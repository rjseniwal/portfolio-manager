package com.portfoliomanager.resources.models.space;

import com.portfoliomanager.space.models.UserSpace;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record SpaceResponse(UUID id, String name,
                            @Schema(type = "string", format = "date-time") String createdAt,
                            @Schema(type = "string", format = "date-time") String updatedAt) implements com.portfoliomanager.resources.models.CreatedModel {
    public static SpaceResponse from(UserSpace space) {
        return new SpaceResponse(space.getId(), space.getName(), space.getCreatedAt().toString(), space.getUpdatedAt().toString());
    }
}
