package com.portfoliomanager.ledger;

import com.google.inject.Guice;
import com.portfoliomanager.guice.RepositoryModule;
import com.portfoliomanager.guice.ServiceModule;
import com.portfoliomanager.space.models.StoredUserSpace;
import com.portfoliomanager.space.services.UserSpaceService;
import com.portfoliomanager.account.models.*;
import com.portfoliomanager.account.services.AccountService;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.category.services.CategoryService;
import com.portfoliomanager.transaction.models.*;
import com.portfoliomanager.transaction.services.CashTransactionService;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LedgerServiceTest {
    @Test
    void ledgerOperationsMaintainIsolationAndBalances() {
        String url = System.getProperty("ledger.test.db.url", "jdbc:h2:mem:ledger_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        String user = System.getProperty("ledger.test.db.user", "sa");
        String password = System.getProperty("ledger.test.db.password", "");
        boolean maria = url.startsWith("jdbc:mariadb:");
        if (maria) {
            try (var connection = java.sql.DriverManager.getConnection(url, user, password); var statement = connection.createStatement()) {
                for (String migration : new String[]{"V1__create_spaces.sql", "V2__create_cash_ledger.sql", "V3__ledger_query_indexes.sql"}) {
                    try (var input = getClass().getResourceAsStream("/db/migration/" + migration)) {
                        for (String sql : new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split(";")) {
                            if (!sql.isBlank()) statement.execute(sql);
                        }
                    }
                }
            } catch (java.io.IOException | java.sql.SQLException e) { throw new RuntimeException(e); }
        }
        var cfg = new Configuration().addAnnotatedClass(StoredUserSpace.class)
            .addAnnotatedClass(StoredAccount.class).addAnnotatedClass(StoredCategory.class).addAnnotatedClass(StoredCashTransaction.class)
            .setProperty("hibernate.connection.url", url)
            .setProperty("hibernate.connection.username", user)
            .setProperty("hibernate.connection.password", password)
            .setProperty("hibernate.jdbc.time_zone", "UTC");
        if (!maria) cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = cfg.buildSessionFactory()) {
            var injector = Guice.createInjector(new RepositoryModule(factory), new ServiceModule());
            var spaces = injector.getInstance(UserSpaceService.class);
            var accounts = injector.getInstance(AccountService.class);
            var categories = injector.getInstance(CategoryService.class);
            var transactions = injector.getInstance(CashTransactionService.class);
            UUID a = spaces.create("A").getId(), b = spaces.create("B").getId();
            LocalDate day = LocalDate.of(2026, 1, 1);
            var bank = accounts.create(a,"Bank",AccountType.BANK,new BigDecimal("100.00"),day);
            var cash = accounts.create(a,"Cash",AccountType.CASH,new BigDecimal("20.00"),day);
            var other = accounts.create(b,"Other",AccountType.BANK,BigDecimal.ZERO,day);
            var income = categories.create(a,"Salary",CategoryType.INCOME);
            var expense = categories.create(a,"Food",CategoryType.EXPENSE);
            transactions.create(a,day,TransactionType.INCOME,new BigDecimal("50.00"),null,bank.getId(),income.getId(),null);
            var spend = transactions.create(a,day,TransactionType.EXPENSE,new BigDecimal("5.00"),bank.getId(),null,expense.getId(),null);
            transactions.create(a,day,TransactionType.TRANSFER,new BigDecimal("10.00"),bank.getId(),cash.getId(),null,null);
            assertEquals(new BigDecimal("135.00"), accounts.balance(a, bank.getId()));
            assertEquals(new BigDecimal("30.00"), accounts.balance(a, cash.getId()));
            assertThrows(RuntimeException.class, () -> transactions.create(a,day,TransactionType.TRANSFER,BigDecimal.ONE,bank.getId(),other.getId(),null,null));
            assertThrows(RuntimeException.class, () -> transactions.create(a,day,TransactionType.EXPENSE,BigDecimal.ONE,bank.getId(),null,income.getId(),null));
            assertThrows(RuntimeException.class, () -> transactions.create(a,day.minusDays(1),TransactionType.INCOME,BigDecimal.ONE,null,bank.getId(),income.getId(),null));
            assertThrows(RuntimeException.class, () -> accounts.delete(a,bank.getId()));
            assertThrows(RuntimeException.class, () -> categories.delete(a,expense.getId()));
            assertThrows(RuntimeException.class, () -> accounts.changeTrackingStartDate(a, bank.getId(), day.plusDays(1)));
            assertThrows(RuntimeException.class, () -> accounts.update(a, bank.getId(), "Changed", new BigDecimal("999.00"), day.plusDays(1)));
            assertEquals("Bank", accounts.get(a, bank.getId()).orElseThrow().getName());
            assertEquals(new BigDecimal("100.00"), accounts.get(a, bank.getId()).orElseThrow().getStartingBalance());
            var changedAccount = accounts.update(a, cash.getId(), "Wallet", new BigDecimal("25.00"), null);
            assertEquals("Wallet", changedAccount.getName());
            assertEquals(new BigDecimal("35.00"), accounts.balance(a, cash.getId()));
            UUID missing = UUID.randomUUID();
            assertThrows(java.util.NoSuchElementException.class, () -> accounts.list(missing));
            assertThrows(java.util.NoSuchElementException.class, () -> categories.list(missing));
            assertThrows(java.util.NoSuchElementException.class, () -> transactions.list(missing));
            transactions.update(a,spend.getId(),day,TransactionType.EXPENSE,new BigDecimal("8.00"),bank.getId(),null,expense.getId(),"updated");
            assertEquals(new BigDecimal("132.00"),accounts.balance(a,bank.getId()));
            assertThrows(RuntimeException.class, () -> transactions.update(a,spend.getId(),day,TransactionType.EXPENSE,new BigDecimal("0.001"),bank.getId(),null,expense.getId(),null));
            assertEquals(new BigDecimal("132.00"),accounts.balance(a,bank.getId()));
            transactions.delete(a,spend.getId());
            assertEquals(new BigDecimal("140.00"),accounts.balance(a,bank.getId()));
            categories.delete(a,expense.getId());
            assertTrue(categories.get(a,expense.getId()).isEmpty());
            assertEquals(2, transactions.list(a).size());
            assertEquals(0, transactions.list(b).size());
            assertThrows(RuntimeException.class, () -> transactions.delete(b, transactions.list(a).get(0).getId()));
            assertTrue(accounts.get(b,bank.getId()).isEmpty());
            var raceAccount = accounts.create(a, "Race", AccountType.BANK, BigDecimal.ZERO, day);
            var start = new java.util.concurrent.CountDownLatch(1);
            var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var dateChange = executor.submit(() -> { start.await(); try { accounts.changeTrackingStartDate(a, raceAccount.getId(), day.plusDays(1)); return true; } catch (RuntimeException e) { return false; } });
                var insertion = executor.submit(() -> { start.await(); try { transactions.create(a, day, TransactionType.INCOME, BigDecimal.ONE, null, raceAccount.getId(), income.getId(), null); return true; } catch (RuntimeException e) { return false; } });
                start.countDown();
                assertNotEquals(dateChange.get(), insertion.get());
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) { throw new RuntimeException(e); }
            finally { executor.shutdownNow(); }
            if (maria) {
                try (var connection = java.sql.DriverManager.getConnection(url, user, password);
                     var statement = connection.prepareStatement("INSERT INTO cash_transactions (id, space_id, transaction_date, type, amount, source_account_id, destination_account_id, category_id, created_at, updated_at) VALUES (?, ?, ?, 'TRANSFER', 1, ?, ?, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))")) {
                    statement.setString(1, UUID.randomUUID().toString());
                    statement.setString(2, a.toString());
                    statement.setDate(3, java.sql.Date.valueOf(day));
                    statement.setString(4, bank.getId().toString());
                    statement.setString(5, other.getId().toString());
                    assertThrows(java.sql.SQLException.class, statement::executeUpdate);
                } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
            }
        }
    }
}
