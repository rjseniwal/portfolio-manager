package com.portfoliomanager.space.repositories;

import com.portfoliomanager.space.models.StoredUserSpace;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Update;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoredUserSpaceDao {
    @Find
    Optional<StoredUserSpace> findById(UUID id);

    @Query("from StoredUserSpace order by createdAt asc, id asc")
    Page<StoredUserSpace> list(PageRequest pageRequest);

    @Insert
    void insert(StoredUserSpace space);

    @Update
    void update(StoredUserSpace space);
}
