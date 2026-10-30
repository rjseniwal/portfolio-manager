package com.portfoliomanager.space.models.mappers;

import com.portfoliomanager.space.models.UserSpace;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserSpaceMapperTest {
    @Test
    void roundTripPreservesEveryFieldWithoutSharingModels() {
        UserSpace original = new UserSpace(UUID.randomUUID(), "Family", Instant.parse("2026-01-01T01:02:03.123456Z"), Instant.parse("2026-02-01T01:02:03.123456Z"));
        UserSpaceMapper mapper = new UserSpaceMapper();
        UserSpace restored = mapper.toDomain(mapper.toStored(original));
        assertNotSame(original, restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
        assertEquals(original.getUpdatedAt(), restored.getUpdatedAt());
    }
}
