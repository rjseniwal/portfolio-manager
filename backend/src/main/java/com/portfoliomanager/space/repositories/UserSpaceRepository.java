package com.portfoliomanager.space.repositories;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.models.UserSpacePage;

import java.util.Optional;
import java.util.UUID;

public interface UserSpaceRepository {
    UserSpacePage list(int page, int size);
    UserSpace insert(UserSpace space);
    UserSpace update(UserSpace space);
    Optional<UserSpace> findById(UUID id);
}
