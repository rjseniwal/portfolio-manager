package com.portfoliomanager.space.services;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.models.UserSpacePage;

import java.util.Optional;
import java.util.UUID;

public interface UserSpaceService {
    UserSpacePage list(String page, String size);
    UserSpace create(String name);
    Optional<UserSpace> get(UUID id);
    UserSpace rename(UUID id, String name);
}
