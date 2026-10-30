package com.portfoliomanager.category.repositories;
import com.portfoliomanager.category.models.StoredCategory;
import jakarta.data.repository.*;
import java.util.*;
@Repository
public interface StoredCategoryDao {
    @Query("from StoredCategory where spaceId = :spaceId and id = :id")
    Optional<StoredCategory> get(UUID spaceId, UUID id);
    @Query("from StoredCategory where spaceId = :spaceId order by createdAt, id")
    List<StoredCategory> list(UUID spaceId);
    @Insert void insert(StoredCategory value);
    @Update void update(StoredCategory value);
    @Delete void delete(StoredCategory value);
}
