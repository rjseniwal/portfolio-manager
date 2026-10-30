package com.portfoliomanager.space.repositories;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.portfoliomanager.guice.RepositoryModule;
import com.portfoliomanager.guice.ServiceModule;
import com.portfoliomanager.persistence.TransactionRunner;
import com.portfoliomanager.space.models.StoredUserSpace;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.services.UserSpaceService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserSpacePersistenceTest {
    @Test
    void persistsRenamesAndRollsBackAcrossRepositoryOperations() throws Exception {
        String url = System.getProperty("test.db.url", "jdbc:h2:mem:spaces_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        String user = System.getProperty("test.db.user", "sa");
        String password = System.getProperty("test.db.password", "");
        boolean maria = url.startsWith("jdbc:mariadb:");
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            if (maria) {
                try (var stream = getClass().getResourceAsStream("/db/migration/V1__create_spaces.sql")) {
                    statement.execute(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            } else {
                statement.execute("CREATE TABLE spaces (id CHAR(36) PRIMARY KEY, name VARCHAR(255) NOT NULL, created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL)");
            }
        }
        Configuration config = new Configuration().addAnnotatedClass(StoredUserSpace.class)
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", user)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.jdbc.time_zone", "UTC");
        try (SessionFactory sessions = config.buildSessionFactory()) {
            Injector injector = Guice.createInjector(new RepositoryModule(sessions), new ServiceModule());
            UserSpaceService service = injector.getInstance(UserSpaceService.class);
            UserSpaceRepository repository = injector.getInstance(UserSpaceRepository.class);
            TransactionRunner transactions = injector.getInstance(TransactionRunner.class);
            assertEquals(0, service.list("1", "2").total());
            UserSpace created = service.create("Personal");
            UserSpace found = service.get(created.getId()).orElseThrow();
            assertEquals(created.getName(), found.getName());
            assertEquals(created.getCreatedAt(), found.getCreatedAt());
            service.rename(created.getId(), "Family");
            assertEquals("Family", service.get(created.getId()).orElseThrow().getName());
            assertTrue(service.get(UUID.randomUUID()).isEmpty());
            service.create("Second");
            service.create("Third");
            var first = service.list("1", "2");
            var second = service.list("2", "2");
            assertEquals(3, first.total());
            assertEquals(2, first.items().size());
            assertEquals(1, second.items().size());
            assertTrue(first.items().stream().noneMatch(s -> s.getId().equals(second.items().get(0).getId())));
            assertTrue(service.list("3", "2").items().isEmpty());
            Instant tie = Instant.parse("2020-01-01T00:00:00Z");
            UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            transactions.inTransaction(() -> {
                repository.insert(new UserSpace(secondId, "Tie B", tie, tie));
                repository.insert(new UserSpace(firstId, "Tie A", tie, tie));
                return null;
            });
            assertEquals(firstId, service.list("1", "1").items().get(0).getId());
            assertEquals(secondId, service.list("2", "1").items().get(0).getId());
            UUID rolledBack = UUID.randomUUID();
            assertThrows(IllegalStateException.class, () -> transactions.inTransaction(() -> {
                repository.insert(new UserSpace(rolledBack, "Rollback", Instant.now(), Instant.now()));
                assertTrue(repository.findById(rolledBack).isPresent());
                throw new IllegalStateException("abort");
            }));
            assertTrue(service.get(rolledBack).isEmpty());
            UUID nestedRollback = UUID.randomUUID();
            assertThrows(IllegalStateException.class, () -> transactions.inTransaction(() -> {
                repository.insert(new UserSpace(nestedRollback, "Nested", Instant.now(), Instant.now()));
                try {
                    transactions.inTransaction(() -> { throw new IllegalArgumentException("abort nested"); });
                } catch (IllegalArgumentException expected) {
                    // Even a caught nested failure must prevent the outer transaction from committing.
                }
                return null;
            }));
            assertTrue(service.get(nestedRollback).isEmpty());
            var missingTransaction = assertThrows(com.google.inject.ProvisionException.class, () -> repository.findById(created.getId()));
            assertInstanceOf(IllegalStateException.class, missingTransaction.getCause());
        }
    }
}
