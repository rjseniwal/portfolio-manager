package com.portfoliomanager.account.services;

import com.google.inject.Inject;
import com.portfoliomanager.ledger.repositories.LedgerQueryRepository;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.account.models.*;
import com.portfoliomanager.account.repositories.AccountRepository;
import com.portfoliomanager.common.LedgerValidation;
import com.portfoliomanager.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class DefaultAccountService implements AccountService {
    private final AccountRepository accounts;
    private final LedgerQueryRepository queries;
    private final TransactionRunner tx;
    private final UserSpaceRepository spaces;
    private final SpaceWriteLock lock;
    @Inject public DefaultAccountService(AccountRepository accounts, TransactionRunner tx, SpaceWriteLock lock, UserSpaceRepository spaces,LedgerQueryRepository queries) {
        this.queries=queries;this.accounts=accounts; this.tx=tx; this.lock=lock; this.spaces=spaces;
    }
    public Account create(UUID spaceId, String name, AccountType type, BigDecimal balance, LocalDate startDate) {
        var validName=LedgerValidation.name(name); var money=LedgerValidation.money(balance);
        if (type == null || startDate == null) throw new IllegalArgumentException("Account type and tracking start date are required");
        return tx.inTransaction(() -> { lock.lock(spaceId); var now=LedgerValidation.now(); return accounts.insert(new Account(UUID.randomUUID(),spaceId,validName,type,money,startDate,now,now)); });
    }
    public Optional<Account> get(UUID spaceId, UUID id) { return tx.inTransaction(() -> accounts.get(spaceId,id)); }
    public List<Account> list(UUID spaceId) { return tx.inTransaction(() -> { requireSpace(spaceId); return accounts.list(spaceId); }); }
    public Account update(UUID spaceId, UUID id, String name, BigDecimal startingBalance, LocalDate startDate) {
        if (name == null && startingBalance == null && startDate == null) throw new IllegalArgumentException("At least one field is required");
        return tx.inTransaction(() -> {
            lock.lock(spaceId);
            var a = require(spaceId, id);
            String newName = name == null ? a.getName() : LedgerValidation.name(name);
            BigDecimal newBalance = startingBalance == null ? a.getStartingBalance() : LedgerValidation.money(startingBalance);
            LocalDate newDate = startDate == null ? a.getTrackingStartDate() : startDate;
            if (queries.accountUsed(spaceId,id,newDate))
                throw new com.portfoliomanager.common.LedgerConflictException("Existing transactions precede tracking date");
            return accounts.update(new Account(id, spaceId, newName, a.getType(), newBalance, newDate, a.getCreatedAt(), LedgerValidation.now()));
        });
    }
    public Account rename(UUID spaceId, UUID id, String name) {
        var validName=LedgerValidation.name(name);
        return tx.inTransaction(() -> { lock.lock(spaceId); var a=require(spaceId,id); return accounts.update(new Account(a.getId(),spaceId,validName,a.getType(),a.getStartingBalance(),a.getTrackingStartDate(),a.getCreatedAt(),LedgerValidation.now())); });
    }
    public Account changeStartingBalance(UUID spaceId, UUID id, BigDecimal amount) {
        var money=LedgerValidation.money(amount);
        return tx.inTransaction(() -> { lock.lock(spaceId); var a=require(spaceId,id); return accounts.update(new Account(a.getId(),spaceId,a.getName(),a.getType(),money,a.getTrackingStartDate(),a.getCreatedAt(),LedgerValidation.now())); });
    }
    public Account changeTrackingStartDate(UUID spaceId, UUID id, LocalDate date) {
        Objects.requireNonNull(date);
        return tx.inTransaction(() -> { lock.lock(spaceId); var a=require(spaceId,id);
            if (queries.accountUsed(spaceId,id,date)) throw new com.portfoliomanager.common.LedgerConflictException("Existing transactions precede tracking date");
            return accounts.update(new Account(a.getId(),spaceId,a.getName(),a.getType(),a.getStartingBalance(),date,a.getCreatedAt(),LedgerValidation.now())); });
    }
    public BigDecimal balance(UUID spaceId, UUID id) {
        return tx.inTransaction(() -> { var a=require(spaceId,id); return a.getStartingBalance().add(queries.accountDelta(spaceId,id)); });
    }
    public void delete(UUID spaceId, UUID id) { tx.inTransaction(() -> { lock.lock(spaceId); var a=require(spaceId,id);
        if (queries.accountUsed(spaceId,id,null)) throw new com.portfoliomanager.common.LedgerConflictException("Account has transactions");
        accounts.delete(a); return null; }); }
    private Account require(UUID spaceId, UUID id) { return accounts.get(spaceId,id).orElseThrow(() -> new NoSuchElementException("Account not found")); }
    private void requireSpace(UUID spaceId) { spaces.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found")); }

}
