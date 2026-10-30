package com.portfoliomanager.persistence;

import com.google.inject.Inject;
import com.portfoliomanager.space.models.StoredUserSpace;
import org.hibernate.LockMode;
import java.util.NoSuchElementException;
import java.util.UUID;

public class SpaceWriteLock {
    private final HibernateTransactionRunner transactions;
    @Inject public SpaceWriteLock(HibernateTransactionRunner transactions) { this.transactions = transactions; }
    public void lock(UUID spaceId) {
        if (spaceId == null || transactions.currentSession().get(StoredUserSpace.class, spaceId, LockMode.PESSIMISTIC_WRITE) == null)
            throw new NoSuchElementException("Space not found");
    }
}
