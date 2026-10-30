package com.portfoliomanager.ledger.services;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.models.*;
import com.portfoliomanager.ledger.repositories.LedgerQueryRepository;
import com.portfoliomanager.account.models.AccountType;
import com.portfoliomanager.account.repositories.AccountRepository;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.category.repositories.CategoryRepository;
import com.portfoliomanager.transaction.models.TransactionType;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.persistence.TransactionRunner;
import java.time.LocalDate;
import java.util.*;
public class DefaultLedgerQueryService implements LedgerQueryService {
    private final LedgerQueryRepository queries; private final TransactionRunner tx;
    private final UserSpaceRepository spaces; private final AccountRepository accounts; private final CategoryRepository categories;
    @Inject public DefaultLedgerQueryService(LedgerQueryRepository queries,TransactionRunner tx,UserSpaceRepository spaces,AccountRepository accounts,CategoryRepository categories){this.queries=queries;this.tx=tx;this.spaces=spaces;this.accounts=accounts;this.categories=categories;}
    private void space(UUID id){spaces.findById(id).orElseThrow(()->new NoSuchElementException("Space not found"));}
    private <E extends Enum<E>> void named(LedgerFilter f,Class<E> type){
        if(f.sort()!=null && !Set.of("name_asc","name_desc","created_asc","created_desc").contains(f.sort()))throw new IllegalArgumentException("Invalid sort");
        if(f.type()!=null)try{Enum.valueOf(type,f.type());}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid type");}
    }
    public Page<AccountBalance> accounts(UUID s,LedgerFilter f){named(f,AccountType.class);return tx.inTransaction(()->{space(s);return queries.accounts(s,f);});}
    public Page<Category> categories(UUID s,LedgerFilter f){named(f,CategoryType.class);return tx.inTransaction(()->{space(s);return queries.categories(s,f);});}
    private void transactionFilters(UUID s,LedgerFilter f){
        if(f.sort()!=null && !Set.of("date_desc","date_asc","amount_desc","amount_asc").contains(f.sort()))throw new IllegalArgumentException("Invalid sort");
        if(f.type()!=null)try{TransactionType.valueOf(f.type());}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid type");}
        space(s);
        if(f.accountId()!=null)accounts.get(s,f.accountId()).orElseThrow(()->new NoSuchElementException("Account not found"));
        if(f.categoryId()!=null)categories.get(s,f.categoryId()).orElseThrow(()->new NoSuchElementException("Category not found"));
    }
    public Page<TransactionDetails> transactions(UUID s,LedgerFilter f){return tx.inTransaction(()->{transactionFilters(s,f);return queries.transactions(s,f);});}
    public void validateTransactions(UUID s,LedgerFilter f){tx.inTransaction(()->{transactionFilters(s,f);return null;});}
    private LocalDate[] range(String from,String to){if(from==null||to==null)throw new IllegalArgumentException("from and to are required");var f=LedgerFilter.date(from);var t=LedgerFilter.date(to);if(f.isAfter(t))throw new IllegalArgumentException("from must not follow to");return new LocalDate[]{f,t};}
    public Summary summary(UUID s,String from,String to){var r=range(from,to);return tx.inTransaction(()->{space(s);return queries.summary(s,r[0],r[1]);});}
    public MonthlyReport monthly(UUID s,String from,String to){var r=range(from,to);return tx.inTransaction(()->{space(s);return queries.monthly(s,r[0],r[1]);});}
}
