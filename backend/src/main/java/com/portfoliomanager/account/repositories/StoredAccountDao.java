package com.portfoliomanager.account.repositories;
import com.portfoliomanager.account.models.StoredAccount;
import jakarta.data.repository.*;
import java.util.*;
@Repository
public interface StoredAccountDao {
    @Query("from StoredAccount where spaceId = :spaceId and id = :id")
    Optional<StoredAccount> get(UUID spaceId, UUID id);
    @Query("from StoredAccount where spaceId = :spaceId order by createdAt, id")
    List<StoredAccount> list(UUID spaceId);
    @Insert void insert(StoredAccount value);
    @Update void update(StoredAccount value);
    @Delete void delete(StoredAccount value);
}
