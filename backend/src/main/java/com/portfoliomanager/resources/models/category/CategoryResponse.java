package com.portfoliomanager.resources.models.category;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.resources.models.CreatedModel;
import java.time.Instant;
import java.util.UUID;
public record CategoryResponse(UUID id, UUID spaceId, String name, CategoryType type, String createdAt, String updatedAt) implements CreatedModel {
    public static CategoryResponse from(Category c) { return new CategoryResponse(c.getId(),c.getSpaceId(),c.getName(),c.getType(),c.getCreatedAt().toString(),c.getUpdatedAt().toString()); }
}
