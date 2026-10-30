package com.portfoliomanager.ledger.repositories;

import com.google.inject.Inject;
import com.portfoliomanager.ledger.models.*;
import com.portfoliomanager.account.models.*;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.transaction.models.*;
import com.portfoliomanager.persistence.HibernateTransactionRunner;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/** JDBC projections share the service's Hibernate StatelessSession transaction. */
public class MariaDbLedgerQueryRepository implements LedgerQueryRepository {
    private final HibernateTransactionRunner tx;
    @Inject public MariaDbLedgerQueryRepository(HibernateTransactionRunner tx){this.tx=tx;}
    @FunctionalInterface private interface Row<T>{T map(ResultSet r)throws SQLException;}
    private PreparedStatement statement(Connection c,String sql,List<Object> args)throws SQLException{
        var p=c.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
        try { for(int i=0;i<args.size();i++) {var a=args.get(i); if(a instanceof UUID)p.setString(i+1,a.toString());else if(a instanceof LocalDate d)p.setDate(i+1,java.sql.Date.valueOf(d));else p.setObject(i+1,a);} return p; }
        catch(SQLException e){p.close();throw e;}
    }
    private <T> List<T> rows(String sql,List<Object> args,Row<T> mapper){return tx.currentSession().doReturningWork(c->{try(var p=statement(c,sql,args);var r=p.executeQuery()){var values=new ArrayList<T>();while(r.next())values.add(mapper.map(r));return values;}});}
    private long count(String sql,List<Object> args){return rows(sql,args,r->r.getLong(1)).get(0);}
    private static UUID id(ResultSet r,String column)throws SQLException{var s=r.getString(column);return s==null?null:UUID.fromString(s);}
    private static Instant instant(ResultSet r,String column)throws SQLException{return r.getTimestamp(column,Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant();}
    private static BigDecimal money(BigDecimal value){return value==null?new BigDecimal("0.00"):value.setScale(2);}
    private static Account account(ResultSet r)throws SQLException{return new Account(id(r,"id"),id(r,"space_id"),r.getString("name"),AccountType.valueOf(r.getString("type")),money(r.getBigDecimal("starting_balance")),r.getDate("tracking_start_date").toLocalDate(),instant(r,"created_at"),instant(r,"updated_at"));}
    private static Category category(ResultSet r)throws SQLException{return new Category(id(r,"id"),id(r,"space_id"),r.getString("name"),CategoryType.valueOf(r.getString("type")),instant(r,"created_at"),instant(r,"updated_at"));}
    private static TransactionDetails details(ResultSet r)throws SQLException{return new TransactionDetails(new CashTransaction(id(r,"id"),id(r,"space_id"),r.getDate("transaction_date").toLocalDate(),TransactionType.valueOf(r.getString("type")),money(r.getBigDecimal("amount")),id(r,"source_account_id"),id(r,"destination_account_id"),id(r,"category_id"),r.getString("description"),instant(r,"created_at"),instant(r,"updated_at")),r.getString("source_name"),r.getString("destination_name"),r.getString("category_name"));}
    private record Where(String sql,List<Object> args){}
    private Where where(UUID space,LedgerFilter f,String alias,boolean transactions){
        var sql=new StringBuilder(" WHERE "+alias+".space_id = ?");var args=new ArrayList<Object>();args.add(space);
        if(f.q()!=null && !f.q().isEmpty()){sql.append(" AND LOWER("+alias+(transactions?".description":".name")+") LIKE ? ESCAPE '!'");args.add("%"+f.q().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        if(f.type()!=null){sql.append(" AND "+alias+".type = ?");args.add(f.type());}
        if(transactions){
            if(f.accountId()!=null){sql.append(" AND (t.source_account_id = ? OR t.destination_account_id = ?)");args.add(f.accountId());args.add(f.accountId());}
            if(f.categoryId()!=null){sql.append(" AND t.category_id = ?");args.add(f.categoryId());}
            if(f.from()!=null){sql.append(" AND t.transaction_date >= ?");args.add(f.from());}
            if(f.to()!=null){sql.append(" AND t.transaction_date <= ?");args.add(f.to());}
        }
        return new Where(sql.toString(),args);
    }
    private String namedSort(LedgerFilter f,String a){String sort=f.sort()==null?"name_asc":f.sort();return " ORDER BY "+switch(sort){case "name_asc"->"LOWER("+a+".name) ASC, "+a+".created_at ASC";case "name_desc"->"LOWER("+a+".name) DESC, "+a+".created_at ASC";case "created_asc"->a+".created_at ASC";case "created_desc"->a+".created_at DESC";default->throw new IllegalArgumentException("Invalid sort");}+", "+a+".id ASC";}
    private String transactionSort(LedgerFilter f){String sort=f.sort()==null?"date_desc":f.sort();return " ORDER BY "+switch(sort){case "date_desc"->"t.transaction_date DESC";case "date_asc"->"t.transaction_date ASC";case "amount_desc"->"t.amount DESC";case "amount_asc"->"t.amount ASC";default->throw new IllegalArgumentException("Invalid sort");}+", t.created_at ASC, t.id ASC";}
    private List<Object> paged(List<Object> args,LedgerFilter f){var p=new ArrayList<>(args);p.add(f.size());p.add(f.offset());return p;}
    public Page<AccountBalance> accounts(UUID space,LedgerFilter f){
        var w=where(space,f,"a",false);long total=count("SELECT COUNT(*) FROM accounts a"+w.sql,w.args);
        String sql="SELECT a.*, a.starting_balance + COALESCE(d.total,0) - COALESCE(s.total,0) AS balance FROM accounts a "+
            "LEFT JOIN (SELECT destination_account_id AS account_id,SUM(amount) AS total FROM cash_transactions WHERE space_id = ? GROUP BY destination_account_id) d ON d.account_id=a.id "+
            "LEFT JOIN (SELECT source_account_id AS account_id,SUM(amount) AS total FROM cash_transactions WHERE space_id = ? GROUP BY source_account_id) s ON s.account_id=a.id"+w.sql+namedSort(f,"a")+" LIMIT ? OFFSET ?";
        var args=new ArrayList<Object>();args.add(space);args.add(space);args.addAll(w.args);
        return new Page<>(rows(sql,paged(args,f),r->new AccountBalance(account(r),money(r.getBigDecimal("balance")))),f.page(),f.size(),total);
    }
    public Page<Category> categories(UUID space,LedgerFilter f){var w=where(space,f,"c",false);return new Page<>(rows("SELECT c.* FROM categories c"+w.sql+namedSort(f,"c")+" LIMIT ? OFFSET ?",paged(w.args,f),MariaDbLedgerQueryRepository::category),f.page(),f.size(),count("SELECT COUNT(*) FROM categories c"+w.sql,w.args));}
    private static final String TRANSACTION_SELECT="SELECT t.*, s.name AS source_name,d.name AS destination_name,c.name AS category_name FROM cash_transactions t LEFT JOIN accounts s ON s.space_id=t.space_id AND s.id=t.source_account_id LEFT JOIN accounts d ON d.space_id=t.space_id AND d.id=t.destination_account_id LEFT JOIN categories c ON c.space_id=t.space_id AND c.id=t.category_id";
    public Page<TransactionDetails> transactions(UUID space,LedgerFilter f){var w=where(space,f,"t",true);return new Page<>(rows(TRANSACTION_SELECT+w.sql+transactionSort(f)+" LIMIT ? OFFSET ?",paged(w.args,f),MariaDbLedgerQueryRepository::details),f.page(),f.size(),count("SELECT COUNT(*) FROM cash_transactions t"+w.sql,w.args));}
    public void streamTransactions(UUID space,LedgerFilter f,Consumer<TransactionDetails> consumer){var w=where(space,f,"t",true);tx.currentSession().doWork(c->{try(var p=statement(c,TRANSACTION_SELECT+w.sql+transactionSort(f),w.args)){p.setFetchSize(100);try(var r=p.executeQuery()){while(r.next())consumer.accept(details(r));}}});}
    public BigDecimal accountDelta(UUID space,UUID account){return rows("SELECT COALESCE(SUM(CASE WHEN destination_account_id = ? THEN amount ELSE 0 END),0)-COALESCE(SUM(CASE WHEN source_account_id = ? THEN amount ELSE 0 END),0) FROM cash_transactions WHERE space_id = ? AND (source_account_id = ? OR destination_account_id = ?)",List.of(account,account,space,account,account),r->money(r.getBigDecimal(1))).get(0);}
    public boolean accountUsed(UUID space,UUID account,LocalDate before){var args=new ArrayList<Object>(List.of(space,account,account));String suffix="";if(before!=null){suffix=" AND transaction_date < ?";args.add(before);}return !rows("SELECT id FROM cash_transactions WHERE space_id = ? AND (source_account_id = ? OR destination_account_id = ?)"+suffix+" LIMIT 1",args,r->r.getString(1)).isEmpty();}
    public boolean categoryUsed(UUID space,UUID category){return !rows("SELECT id FROM cash_transactions WHERE space_id = ? AND category_id = ? LIMIT 1",List.of(space,category),r->r.getString(1)).isEmpty();}
    public Summary summary(UUID space,LocalDate from,LocalDate to){
        var totals=rows("SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END),0),COUNT(*) FROM cash_transactions WHERE space_id = ? AND transaction_date >= ? AND transaction_date <= ?",List.of(space,from,to),r->new Object[]{money(r.getBigDecimal(1)),money(r.getBigDecimal(2)),r.getLong(3)}).get(0);
        var income=(BigDecimal)totals[0];var expense=(BigDecimal)totals[1];
        var balance=rows("SELECT COALESCE((SELECT SUM(starting_balance) FROM accounts WHERE space_id = ?),0)+COALESCE((SELECT SUM(CASE WHEN type='INCOME' THEN amount WHEN type='EXPENSE' THEN -amount ELSE 0 END) FROM cash_transactions WHERE space_id = ?),0)",List.of(space,space),r->money(r.getBigDecimal(1))).get(0);
        var categoryTotals=rows("SELECT c.id,c.name,c.type,SUM(t.amount) AS amount,COUNT(*) AS transaction_count FROM cash_transactions t JOIN categories c ON c.space_id=t.space_id AND c.id=t.category_id WHERE t.space_id = ? AND t.transaction_date >= ? AND t.transaction_date <= ? AND t.type IN ('INCOME','EXPENSE') GROUP BY c.id,c.name,c.type ORDER BY c.type,LOWER(c.name),c.id",List.of(space,from,to),r->new CategoryTotal(id(r,"id"),r.getString("name"),CategoryType.valueOf(r.getString("type")),money(r.getBigDecimal("amount")),r.getLong("transaction_count")));
        return new Summary(from,to,income,expense,money(income.subtract(expense)),(Long)totals[2],balance,count("SELECT COUNT(*) FROM accounts WHERE space_id = ?",List.of(space)),categoryTotals);
    }
    public MonthlyReport monthly(UUID space,LocalDate from,LocalDate to){
        var found=rows("SELECT YEAR(transaction_date) AS yr,MONTH(transaction_date) AS mon,COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END),0) AS income,COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END),0) AS expense FROM cash_transactions WHERE space_id = ? AND transaction_date >= ? AND transaction_date <= ? GROUP BY YEAR(transaction_date),MONTH(transaction_date)",List.of(space,from,to),r->{var income=money(r.getBigDecimal("income"));var expense=money(r.getBigDecimal("expense"));return new MonthlyReport.MonthTotal(YearMonth.of(r.getInt("yr"),r.getInt("mon")).toString(),income,expense,money(income.subtract(expense)));});
        var byMonth=new HashMap<String,MonthlyReport.MonthTotal>();found.forEach(m->byMonth.put(m.month(),m));var items=new ArrayList<MonthlyReport.MonthTotal>();
        for(var month=YearMonth.from(from);!month.isAfter(YearMonth.from(to));month=month.plusMonths(1)){var key=month.toString();items.add(byMonth.getOrDefault(key,new MonthlyReport.MonthTotal(key,money(null),money(null),money(null))));}
        return new MonthlyReport(from,to,items);
    }
}
