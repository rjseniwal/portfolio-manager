package com.portfoliomanager.space.services;

import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.persistence.TransactionRunner;
import java.util.function.Supplier;
import com.portfoliomanager.space.repositories.UserSpaceRepository;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UserSpaceServiceTest {
    @Test
    void creationAndRenameMaintainIdentityAndAuditFields() {
        UserSpaceService service = new DefaultUserSpaceService(new MemoryRepository(), new InlineTransactions());
        UserSpace created = service.create("  Personal  ");
        assertNotNull(created.getId());
        assertEquals("Personal", created.getName());
        assertNotNull(created.getCreatedAt());
        assertEquals(created.getCreatedAt(), created.getUpdatedAt());
        UserSpace renamed = service.rename(created.getId(), "  Family ");
        assertEquals("Family", renamed.getName());
        assertEquals(created.getId(), renamed.getId());
        assertEquals(created.getCreatedAt(), renamed.getCreatedAt());
        assertFalse(renamed.getUpdatedAt().isBefore(created.getUpdatedAt()));
    }

    @Test
    void invalidNamesDoNotMutateSavedSpace() {
        UserSpaceService service = new DefaultUserSpaceService(new MemoryRepository(), new InlineTransactions());
        UserSpace created = service.create("Personal");
        for (String invalid : Arrays.asList(null, "  ", "a".repeat(256))) {
            assertThrows(IllegalArgumentException.class, () -> service.create(invalid));
            assertThrows(IllegalArgumentException.class, () -> service.rename(created.getId(), invalid));
        }
        assertEquals("Personal", service.get(created.getId()).orElseThrow().getName());
        assertDoesNotThrow(() -> service.create("a".repeat(255)));
    }

    @Test
    void renameRejectsMissingSpace() {
        UserSpaceService service = new DefaultUserSpaceService(new MemoryRepository(), new InlineTransactions());
        assertThrows(SpaceException.class, () -> service.rename(UUID.randomUUID(), "Name"));
    }

    @Test
    void validatesPaginationBeforeAccessingRepository() {
        UserSpaceService service = new DefaultUserSpaceService(new MemoryRepository(), new InlineTransactions());
        for (String invalid : Arrays.asList("0", "-1", "abc", "9999999999999999999")) {
            assertThrows(IllegalArgumentException.class, () -> service.list(invalid, "10"));
        }
        for (String invalid : Arrays.asList("0", "-1", "101", "abc")) {
            assertThrows(IllegalArgumentException.class, () -> service.list("1", invalid));
        }
    }

    private static class InlineTransactions implements TransactionRunner {
        public <T> T inTransaction(Supplier<T> work) { return work.get(); }
    }

    private static class MemoryRepository implements UserSpaceRepository {
        private final Map<UUID, UserSpace> spaces = new HashMap<>();
        public UserSpace insert(UserSpace space) { spaces.put(space.getId(), space); return space; }
        public UserSpace update(UserSpace space) { spaces.put(space.getId(), space); return space; }
        public com.portfoliomanager.space.models.UserSpacePage list(int page, int size) {
            return new com.portfoliomanager.space.models.UserSpacePage(List.of(), page, size, 0);
        }
        public Optional<UserSpace> findById(UUID id) { return Optional.ofNullable(spaces.get(id)); }
    }
}
