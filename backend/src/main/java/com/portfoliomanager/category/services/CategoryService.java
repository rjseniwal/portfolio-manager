package com.portfoliomanager.category.services;
import com.portfoliomanager.category.models.*;
import java.util.*;
public interface CategoryService {
    Category create(UUID spaceId, String name, CategoryType type);
    Optional<Category> get(UUID spaceId, UUID id);
    List<Category> list(UUID spaceId);
    Category rename(UUID spaceId, UUID id, String name);
    void delete(UUID spaceId, UUID id);
}
