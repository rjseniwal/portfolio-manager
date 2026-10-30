package com.portfoliomanager.category.repositories;
import com.portfoliomanager.category.models.Category;
import java.util.*;
public interface CategoryRepository {
    Optional<Category> get(UUID spaceId, UUID id);
    List<Category> list(UUID spaceId);
    Category insert(Category value);
    Category update(Category value);
    void delete(Category value);
}
