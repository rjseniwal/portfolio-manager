package com.portfoliomanager.category.repositories;
import com.portfoliomanager.category.models.Category;
import com.portfoliomanager.category.models.mappers.CategoryMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.*;
public class MariaDbCategoryRepository implements CategoryRepository {
    private final Provider<StoredCategoryDao> dao;
    private final CategoryMapper mapper;
    @Inject public MariaDbCategoryRepository(Provider<StoredCategoryDao> dao, CategoryMapper mapper) { this.dao=dao; this.mapper=mapper; }
    public Optional<Category> get(UUID spaceId, UUID id) { return dao.get().get(spaceId,id).map(mapper::toDomain); }
    public List<Category> list(UUID spaceId) { return dao.get().list(spaceId).stream().map(mapper::toDomain).toList(); }
    public Category insert(Category value) { dao.get().insert(mapper.toStored(value)); return value; }
    public Category update(Category value) { dao.get().update(mapper.toStored(value)); return value; }
    public void delete(Category value) { dao.get().delete(mapper.toStored(value)); }
}
