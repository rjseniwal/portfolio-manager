package com.portfoliomanager.space.models.mappers;
import com.portfoliomanager.space.models.StoredUserSpace;
import com.portfoliomanager.space.models.UserSpace;

public class UserSpaceMapper {
    public StoredUserSpace toStored(UserSpace space) {
        return new StoredUserSpace(space.getId(), space.getName(), space.getCreatedAt(), space.getUpdatedAt());
    }

    public UserSpace toDomain(StoredUserSpace space) {
        return new UserSpace(space.getId(), space.getName(), space.getCreatedAt(), space.getUpdatedAt());
    }
}
