package com.portfoliomanager.space.repositories;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.models.UserSpacePage;
import jakarta.data.page.PageRequest;
import com.portfoliomanager.space.models.mappers.UserSpaceMapper;
import java.util.Optional;
import java.util.UUID;

public class MariaDbUserSpaceRepository implements UserSpaceRepository {
    private final Provider<StoredUserSpaceDao> dao;
    private final UserSpaceMapper mapper;

    @Inject
    public MariaDbUserSpaceRepository(Provider<StoredUserSpaceDao> dao, UserSpaceMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    @Override
    public UserSpacePage list(int page, int size) {
        var result = dao.get().list(PageRequest.ofPage(page, size, true));
        return new UserSpacePage(result.content().stream().map(mapper::toDomain).toList(), page, size, result.totalElements());
    }

    @Override
    public UserSpace insert(UserSpace space) {
        dao.get().insert(mapper.toStored(space));
        return space;
    }

    @Override
    public UserSpace update(UserSpace space) {
        dao.get().update(mapper.toStored(space));
        return space;
    }

    @Override
    public Optional<UserSpace> findById(UUID id) {
        return dao.get().findById(id).map(mapper::toDomain);
    }
}
