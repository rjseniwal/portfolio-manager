package com.portfoliomanager.ledger;

import com.google.inject.*;
import com.portfoliomanager.guice.*;
import com.portfoliomanager.account.models.*;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.transaction.models.*;
import com.portfoliomanager.account.services.AccountService;
import com.portfoliomanager.space.models.StoredUserSpace;
import com.portfoliomanager.space.services.UserSpaceService;
import com.portfoliomanager.resources.AccountResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LedgerQueryTest {
    @Test void filteredPagesReportsAndExportPreserveLedgerSemantics() throws Exception {
        try (var factory = testFactory()) {
            var injector=Guice.createInjector(new RepositoryModule(factory),new ServiceModule());
            var spaces=injector.getInstance(UserSpaceService.class);
            var accounts=injector.getInstance(AccountService.class);
            var categories=injector.getInstance(com.portfoliomanager.category.services.CategoryService.class);
            var tx=injector.getInstance(com.portfoliomanager.transaction.services.CashTransactionService.class);
            var query=injector.getInstance(com.portfoliomanager.ledger.services.LedgerQueryService.class);
            UUID space=spaces.create("Primary").getId(), other=spaces.create("Other").getId();
            LocalDate jan=LocalDate.of(2026,1,1);
            var bank=accounts.create(space,"Bank_100%",AccountType.BANK,new BigDecimal("100.00"),jan);
            var cash=accounts.create(space,"BankX100Y",AccountType.CASH,new BigDecimal("20.00"),jan);
            var alien=accounts.create(other,"Foreign",AccountType.BANK,BigDecimal.ZERO,jan);
            var salary=categories.create(space,"Salary",CategoryType.INCOME);
            var food=categories.create(space,"Food",CategoryType.EXPENSE);
            tx.create(space,jan,TransactionType.INCOME,new BigDecimal("50.00"),null,bank.getId(),salary.getId(),"Pay_100%");
            tx.create(space,jan.plusDays(2),TransactionType.EXPENSE,new BigDecimal("5.00"),bank.getId(),null,food.getId(),"=SUM(1,2)\n\"quoted\"");
            tx.create(space,jan.plusDays(3),TransactionType.TRANSFER,new BigDecimal("10.00"),bank.getId(),cash.getId(),null,"PayX100Y");
            tx.create(space,LocalDate.of(2026,3,31),TransactionType.INCOME,new BigDecimal("30.00"),null,cash.getId(),salary.getId(),"Future");
            var page=query.accounts(space,filter("1","1",null,null,null,null,null,null,"name_asc"));
            assertEquals(2,page.total()); assertEquals(1,page.items().size());
            assertEquals("135.00",page.items().get(0).balance().toPlainString());
            assertEquals(1,query.accounts(space,filter(null,null,"_100%",null,null,null,null,null,null)).total());
            assertEquals(0,query.accounts(space,filter("4","1",null,null,null,null,null,null,null)).items().size());
            assertEquals(1,query.categories(space,filter(null,null,"SAL", "INCOME",null,null,null,null,null)).total());
            var history=query.transactions(space,filter(null,null,null,null,bank.getId().toString(),null,"2026-01-01","2026-01-04","date_asc"));
            assertEquals(3,history.total()); assertEquals("Bank_100%",history.items().get(0).destinationAccountName());
            assertEquals(1,query.transactions(space,filter(null,null,"pay_100%",null,null,null,null,null,null)).total());
            assertNull(history.items().get(2).categoryName());
            assertEquals(1,query.accounts(space,filter(null,null,null,"CASH",null,null,null,null,null)).total());
            assertEquals(2,query.transactions(space,filter(null,null,null,null,null,salary.getId().toString(),null,null,null)).total());
            assertEquals(1,query.transactions(space,filter(null,null,null,null,null,null,"2026-03-31",null,null)).total());
            assertEquals(1,query.transactions(space,filter(null,null,null,null,null,null,null,"2026-01-01",null)).total());
            assertEquals("Future",query.transactions(space,filter(null,null,null,null,null,null,null,null,null)).items().get(0).transaction().getDescription());
            assertEquals("50.00",query.transactions(space,filter(null,null,null,null,null,null,null,null,"amount_desc")).items().get(0).transaction().getAmount().toPlainString());
            assertEquals("5.00",query.transactions(space,filter(null,null,null,null,null,null,null,null,"amount_asc")).items().get(0).transaction().getAmount().toPlainString());
            assertEquals("BankX100Y",query.accounts(space,filter(null,null,null,null,null,null,null,null,"name_desc")).items().get(0).account().getName());
            var summary=query.summary(space,"2026-01-01","2026-01-31");
            assertEquals("50.00",summary.income().toPlainString()); assertEquals("5.00",summary.expense().toPlainString());
            assertEquals("45.00",summary.netCashFlow().toPlainString()); assertEquals(3,summary.transactionCount());
            assertEquals("195.00",summary.ledgerBalance().toPlainString()); assertEquals(2,summary.accountCount());
            assertEquals(2,summary.categoryTotals().size());
            var empty=query.summary(other,"2026-01-01","2026-01-31");
            assertEquals(0,empty.transactionCount());assertEquals("0.00",empty.income().toPlainString());assertTrue(empty.categoryTotals().isEmpty());
            var jsonSummary=new ObjectMapper().findAndRegisterModules().valueToTree(com.portfoliomanager.resources.models.report.SummaryResponse.from(summary));
            assertTrue(jsonSummary.get("income").isTextual());assertTrue(jsonSummary.get("categoryTotals").get(0).get("amount").isTextual());
            var monthly=query.monthly(space,"2026-01-03","2026-03-10");
            assertEquals(3,monthly.items().size()); assertEquals("2026-02",monthly.items().get(1).month());
            assertEquals("0.00",monthly.items().get(1).income().toPlainString());
            assertEquals("-5.00",monthly.items().get(0).netCashFlow().toPlainString());
            assertEquals("0.00",monthly.items().get(2).income().toPlainString());
            for (var bad : new String[][]{{"0","20"},{"1","101"},{"abc","20"},{"2147483647","100"}})
                assertThrows(IllegalArgumentException.class,()->query.accounts(space,filter(bad[0],bad[1],null,null,null,null,null,null,null)));
            assertThrows(IllegalArgumentException.class,()->query.transactions(space,filter(null,null,null,"INVALID",null,null,null,null,null)));
            assertThrows(IllegalArgumentException.class,()->query.transactions(space,filter(null,null,null,null,null,null,"2026-02-01","2026-01-01",null)));
            assertThrows(IllegalArgumentException.class,()->query.accounts(space,filter(null,null,null,null,null,null,null,null,"date_desc")));
            assertThrows(IllegalArgumentException.class,()->query.categories(space,filter(null,null,null,"BANK",null,null,null,null,null)));
            assertThrows(IllegalArgumentException.class,()->query.transactions(space,filter(null,null,null,null,"not-a-uuid",null,null,null,null)));
            assertThrows(java.util.NoSuchElementException.class,()->query.transactions(space,filter(null,null,null,null,null,UUID.randomUUID().toString(),null,null,null)));
            assertThrows(IllegalArgumentException.class,()->query.summary(space,null,"2026-01-01"));
            assertThrows(IllegalArgumentException.class,()->query.summary(space,"2026-02-30","2026-03-01"));
            assertThrows(java.util.NoSuchElementException.class,()->query.transactions(space,filter(null,null,null,null,alien.getId().toString(),null,null,null,null)));
            assertThrows(java.util.NoSuchElementException.class,()->query.accounts(UUID.randomUUID(),filter(null,null,null,null,null,null,null,null,null)));
            var export=injector.getInstance(com.portfoliomanager.ledger.services.LedgerExportService.class);
            assertThrows(java.util.NoSuchElementException.class,()->export.prepare(space,filter(null,null,null,null,alien.getId().toString(),null,null,null,null)));
            assertThrows(IllegalArgumentException.class,()->export.prepare(space,filter(null,null,null,null,null,null,null,null,"bad")));
            var prepared=export.prepare(space,filter("1","1",null,null,null,null,null,null,"date_asc"));
            var out=new java.io.ByteArrayOutputStream(); export.write(prepared,out);
            String csv=out.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(csv.startsWith("\uFEFFid,spaceId,transactionDate"));
            assertTrue(csv.contains("\"'=SUM(1,2)\n\"\"quoted\"\"\""));
            assertTrue(csv.endsWith("\r\n")); assertTrue(csv.contains("Future"));
            var broken=new java.io.OutputStream(){public void write(int b)throws java.io.IOException{throw new java.io.IOException("disconnected");}};
            assertThrows(java.io.IOException.class,()->export.write(prepared,broken));
            assertEquals(4,query.transactions(space,filter(null,null,null,null,null,null,null,null,null)).total());
            tx.create(space,jan,TransactionType.INCOME,BigDecimal.ONE,null,bank.getId(),salary.getId(),null);
            assertEquals(5,query.transactions(space,filter(null,null,"",null,null,null,null,null,null)).total(),"empty search must retain transactions without descriptions");
            for(int i=0;i<12;i++)tx.create(space,jan,TransactionType.INCOME,BigDecimal.ONE,null,bank.getId(),salary.getId(),"x".repeat(1000));
            assertThrows(java.io.IOException.class,()->export.write(prepared,broken));
            assertEquals(17,query.transactions(space,filter(null,null,null,null,null,null,null,null,null)).total());
        }
    }
    private static org.hibernate.SessionFactory testFactory() throws Exception {
        String url=System.getProperty("ledger.query.test.db.url","jdbc:h2:mem:"+UUID.randomUUID());
        String user=System.getProperty("ledger.query.test.db.user","sa"),password=System.getProperty("ledger.query.test.db.password","");
        boolean maria=url.startsWith("jdbc:mariadb:");
        if(maria)try(var connection=java.sql.DriverManager.getConnection(url,user,password);var statement=connection.createStatement()){
            for(String migration:new String[]{"V1__create_spaces.sql","V2__create_cash_ledger.sql","V3__ledger_query_indexes.sql"})
                try(var input=LedgerQueryTest.class.getResourceAsStream("/db/migration/"+migration)){
                    for(String sql:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())statement.execute(sql);
                }
        }
        var configuration=new Configuration().addAnnotatedClass(StoredUserSpace.class).addAnnotatedClass(StoredAccount.class)
            .addAnnotatedClass(StoredCategory.class).addAnnotatedClass(StoredCashTransaction.class)
            .setProperty("hibernate.connection.url",url).setProperty("hibernate.connection.username",user)
            .setProperty("hibernate.connection.password",password).setProperty("hibernate.jdbc.time_zone","UTC");
        if(!maria)configuration.setProperty("hibernate.hbm2ddl.auto","create-drop");
        return configuration.buildSessionFactory();
    }
    private static com.portfoliomanager.ledger.models.LedgerFilter filter(String page,String size,String q,String type,String account,String category,String from,String to,String sort) {
        return com.portfoliomanager.ledger.models.LedgerFilter.parse(page,size,q,type,account,category,from,to,sort);
    }

    @Test void equalKeysHaveStableIdOrderAcrossPages() throws Exception {
        try(var factory=new Configuration().addAnnotatedClass(StoredUserSpace.class).addAnnotatedClass(StoredAccount.class)
            .addAnnotatedClass(StoredCategory.class).addAnnotatedClass(StoredCashTransaction.class)
            .setProperty("hibernate.connection.url","jdbc:h2:mem:"+UUID.randomUUID())
            .setProperty("hibernate.hbm2ddl.auto","create-drop").buildSessionFactory()) {
            var injector=Guice.createInjector(new RepositoryModule(factory),new ServiceModule());
            var space=injector.getInstance(UserSpaceService.class).create("Tie test").getId();
            var accounts=injector.getInstance(AccountService.class);
            var bank=accounts.create(space,"Bank",AccountType.BANK,BigDecimal.ZERO,LocalDate.of(2026,1,1));
            var category=injector.getInstance(com.portfoliomanager.category.services.CategoryService.class).create(space,"Income",CategoryType.INCOME);
            var tx=injector.getInstance(com.portfoliomanager.persistence.TransactionRunner.class);
            var repository=injector.getInstance(com.portfoliomanager.transaction.repositories.CashTransactionRepository.class);
            var at=java.time.Instant.parse("2026-01-01T00:00:00Z");
            UUID first=UUID.fromString("00000000-0000-0000-0000-000000000001"),second=UUID.fromString("00000000-0000-0000-0000-000000000002");
            tx.inTransaction(()->{
                for(UUID id:new UUID[]{second,first})repository.insert(new CashTransaction(id,space,LocalDate.of(2026,1,1),TransactionType.INCOME,new BigDecimal("1.00"),null,bank.getId(),category.getId(),null,at,at));
                return null;
            });
            var query=injector.getInstance(com.portfoliomanager.ledger.services.LedgerQueryService.class);
            for(String sort:new String[]{"date_desc","date_asc","amount_desc","amount_asc"}) {
                assertEquals(first,query.transactions(space,filter("1","1",null,null,null,null,null,null,sort)).items().get(0).transaction().getId());
                assertEquals(second,query.transactions(space,filter("2","1",null,null,null,null,null,null,sort)).items().get(0).transaction().getId());
            }
        }
    }

    @Test void collectionHasPageEnvelopeAndBalance() throws Exception {
        try (var factory = new Configuration().addAnnotatedClass(StoredUserSpace.class)
                .addAnnotatedClass(StoredAccount.class).addAnnotatedClass(StoredCategory.class).addAnnotatedClass(StoredCashTransaction.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:"+UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory()) {
            var injector=Guice.createInjector(new RepositoryModule(factory),new ServiceModule());
            var space=injector.getInstance(UserSpaceService.class).create("Test");
            injector.getInstance(AccountService.class).create(space.getId(),"Bank",AccountType.BANK,new BigDecimal("25.00"),LocalDate.of(2026,1,1));
            var resource=injector.getInstance(AccountResource.class);
            var json=new ObjectMapper().findAndRegisterModules().valueToTree(resource.list(space.getId().toString(),null,null,null,null,null));
            assertTrue(json.has("items"), "collections must return a page envelope");
            assertEquals(1,json.get("page").asInt()); assertEquals(20,json.get("size").asInt()); assertEquals(1,json.get("total").asInt());
            assertEquals("25.00",json.get("items").get(0).get("balance").asText());
        }
    }
}
