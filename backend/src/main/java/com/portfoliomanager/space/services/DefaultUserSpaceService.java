package com.portfoliomanager.space.services;
import com.portfoliomanager.space.models.UserSpace;
import com.portfoliomanager.space.models.UserSpacePage;
import com.portfoliomanager.persistence.TransactionRunner;
import com.portfoliomanager.space.repositories.UserSpaceRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

public class DefaultUserSpaceService implements UserSpaceService {
    private final UserSpaceRepository repository;
    private final TransactionRunner transactions;

    @Inject
    public DefaultUserSpaceService(UserSpaceRepository repository, TransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    @Override
    public UserSpacePage list(String page, String size) {
        int pageNumber;
        int pageSize;
        try {
            pageNumber = Integer.parseInt(page);
            pageSize = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new InvalidPaginationException("page and size must be integers");
        }
        if (pageNumber < 1 || pageSize < 1 || pageSize > 100 || (long) (pageNumber - 1) * pageSize > Integer.MAX_VALUE) {
            throw new InvalidPaginationException("page must be positive; size must be between 1 and 100; offset must not exceed 2147483647");
        }
        return transactions.inTransaction(() -> repository.list(pageNumber, pageSize));
    }

    public UserSpace create(String name) {
        String validName = validateName(name);
        Instant now = now();
        return transactions.inTransaction(() -> repository.insert(new UserSpace(UUID.randomUUID(), validName, now, now)));
    }

    public Optional<UserSpace> get(UUID id) {
        return transactions.inTransaction(() -> repository.findById(id));
    }

    public UserSpace rename(UUID id, String name) {
        String validName = validateName(name);
        return transactions.inTransaction(() -> {
            UserSpace existing = repository.findById(id).orElseThrow(() -> new SpaceException(SpaceException.Reason.NOT_FOUND, "Space not found"));
            return repository.update(new UserSpace(existing.getId(), validName, existing.getCreatedAt(), now()));
        });
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new SpaceException(SpaceException.Reason.INVALID_NAME, "Space name must not be blank");
        }
        String trimmed = name.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > 255) {
            throw new SpaceException(SpaceException.Reason.INVALID_NAME, "Space name must be at most 255 characters");
        }
        return trimmed;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
