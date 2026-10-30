package com.portfoliomanager.category.services;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.repositories.LedgerQueryRepository;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.category.models.*;
import com.portfoliomanager.category.repositories.CategoryRepository;
import com.portfoliomanager.common.LedgerValidation;
import com.portfoliomanager.persistence.*;
import java.util.*;
public class DefaultCategoryService implements CategoryService {
    private final CategoryRepository categories;
    private final LedgerQueryRepository queries;
    private final TransactionRunner tx;
    private final UserSpaceRepository spaces;
    private final SpaceWriteLock lock;
    @Inject public DefaultCategoryService(CategoryRepository categories, TransactionRunner tx, SpaceWriteLock lock, UserSpaceRepository spaces,LedgerQueryRepository queries) { this.queries=queries;this.categories=categories; this.tx=tx; this.lock=lock; this.spaces=spaces; }
    public Category create(UUID spaceId, String name, CategoryType type) {
        var valid=LedgerValidation.name(name); if (type == null) throw new IllegalArgumentException("Category type is required");
        return tx.inTransaction(() -> { lock.lock(spaceId); var now=LedgerValidation.now(); return categories.insert(new Category(UUID.randomUUID(),spaceId,valid,type,now,now)); });
    }
    public Optional<Category> get(UUID spaceId, UUID id) { return tx.inTransaction(() -> categories.get(spaceId,id)); }
    public List<Category> list(UUID spaceId) { return tx.inTransaction(() -> { requireSpace(spaceId); return categories.list(spaceId); }); }
    public Category rename(UUID spaceId, UUID id, String name) {
        var valid=LedgerValidation.name(name);
        return tx.inTransaction(() -> { lock.lock(spaceId); var c=require(spaceId,id); return categories.update(new Category(c.getId(),spaceId,valid,c.getType(),c.getCreatedAt(),LedgerValidation.now())); });
    }
    public void delete(UUID spaceId, UUID id) { tx.inTransaction(() -> { lock.lock(spaceId); var c=require(spaceId,id);
        if (queries.categoryUsed(spaceId,id)) throw new com.portfoliomanager.common.LedgerConflictException("Category has transactions");
        categories.delete(c); return null; }); }
    private Category require(UUID spaceId, UUID id) { return categories.get(spaceId,id).orElseThrow(() -> new NoSuchElementException("Category not found")); }
    private void requireSpace(UUID spaceId) { spaces.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found")); }

}
