package com.portfoliomanager.category.models.mappers;
import com.portfoliomanager.category.models.Category;
import com.portfoliomanager.category.models.StoredCategory;
public class CategoryMapper {
    public StoredCategory toStored(Category value) { return new StoredCategory(value.getId(), value.getSpaceId(), value.getName(), value.getType(), value.getCreatedAt(), value.getUpdatedAt()); }
    public Category toDomain(StoredCategory value) { return new Category(value.getId(), value.getSpaceId(), value.getName(), value.getType(), value.getCreatedAt(), value.getUpdatedAt()); }
}
