package com.portfoliomanager.transaction.repositories;
import com.portfoliomanager.transaction.models.StoredCashTransaction;
import jakarta.data.repository.*;
import java.util.*;
@Repository
public interface StoredCashTransactionDao {
    @Query("from StoredCashTransaction where spaceId = :spaceId and id = :id")
    Optional<StoredCashTransaction> get(UUID spaceId, UUID id);
    @Query("from StoredCashTransaction where spaceId = :spaceId order by createdAt, id")
    List<StoredCashTransaction> list(UUID spaceId);
    @Insert void insert(StoredCashTransaction value);
    @Update void update(StoredCashTransaction value);
    @Delete void delete(StoredCashTransaction value);
}
