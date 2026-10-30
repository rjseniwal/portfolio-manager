package com.portfoliomanager.space.resources;

import com.portfoliomanager.persistence.TransactionRunner;
import com.portfoliomanager.resources.UserSpaceResource;
import com.portfoliomanager.resources.SpaceExceptionMapper;
import com.portfoliomanager.resources.models.space.NameRequest;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.models.UserSpacePage;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.space.services.DefaultUserSpaceService;
import com.portfoliomanager.space.services.SpaceException;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

class UserSpaceResourceTest {
    @Test
    void createsReadsAndRenamesSpace() {
        var resource = resource();
        var created = resource.create(new NameRequest(" Personal "));
        assertEquals("Personal", created.name());
        assertEquals(created, resource.get(created.id().toString()));
        var renamed = resource.rename(created.id().toString(), new NameRequest("Family"));
        assertEquals("Family", renamed.name());
        assertEquals(created.createdAt(), renamed.createdAt());
        assertEquals(renamed, resource.get(created.id().toString()));
    }

    @Test
    void errorsMapToCorrectStatuses() {
        var resource = resource();
        var mapper = new SpaceExceptionMapper();
        assertEquals(400, mapper.toResponse(assertThrows(SpaceException.class,
                () -> resource.create(new NameRequest(" ")))).getStatus());
        assertEquals(400, mapper.toResponse(assertThrows(SpaceException.class,
                () -> resource.get("bad-uuid"))).getStatus());
        assertEquals(404, mapper.toResponse(assertThrows(SpaceException.class,
                () -> resource.get(UUID.randomUUID().toString()))).getStatus());
        assertEquals(404, mapper.toResponse(assertThrows(SpaceException.class,
                () -> resource.rename(UUID.randomUUID().toString(), new NameRequest("Name")))).getStatus());
    }

    private UserSpaceResource resource() {
        UserSpaceRepository repository = new UserSpaceRepository() {
            private final Map<UUID, UserSpace> spaces = new HashMap<>();
            public UserSpace insert(UserSpace space) { spaces.put(space.getId(), space); return space; }
            public UserSpace update(UserSpace space) { spaces.put(space.getId(), space); return space; }
            public Optional<UserSpace> findById(UUID id) { return Optional.ofNullable(spaces.get(id)); }
            public UserSpacePage list(int page, int size) { return new UserSpacePage(List.of(), page, size, 0); }
        };
        TransactionRunner tx = new TransactionRunner() {
            public <T> T inTransaction(Supplier<T> work) { return work.get(); }
        };
        return new UserSpaceResource(new DefaultUserSpaceService(repository, tx));
    }
}
