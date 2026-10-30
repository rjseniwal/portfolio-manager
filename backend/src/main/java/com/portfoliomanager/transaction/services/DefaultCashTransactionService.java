package com.portfoliomanager.transaction.services;
import com.google.inject.Inject;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.account.repositories.AccountRepository;
import com.portfoliomanager.category.models.CategoryType;
import com.portfoliomanager.category.repositories.CategoryRepository;
import com.portfoliomanager.common.LedgerValidation;
import com.portfoliomanager.persistence.*;
import com.portfoliomanager.transaction.models.*;
import com.portfoliomanager.transaction.repositories.CashTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public class DefaultCashTransactionService implements CashTransactionService {
    private final CashTransactionRepository ledger;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final TransactionRunner tx;
    private final UserSpaceRepository spaces;
    private final SpaceWriteLock lock;
    @Inject public DefaultCashTransactionService(CashTransactionRepository ledger, AccountRepository accounts, CategoryRepository categories, TransactionRunner tx, SpaceWriteLock lock, UserSpaceRepository spaces) { this.ledger=ledger; this.accounts=accounts; this.categories=categories; this.tx=tx; this.lock=lock; this.spaces=spaces; }
    public CashTransaction create(UUID spaceId, LocalDate date, TransactionType type, BigDecimal amount, UUID source, UUID destination, UUID category, String description) {
        return tx.inTransaction(() -> { lock.lock(spaceId); var money=validate(spaceId,date,type,amount,source,destination,category,description); var now=LedgerValidation.now(); return ledger.insert(new CashTransaction(UUID.randomUUID(),spaceId,date,type,money,source,destination,category,description,now,now)); });
    }
    public CashTransaction update(UUID spaceId, UUID id, LocalDate date, TransactionType type, BigDecimal amount, UUID source, UUID destination, UUID category, String description) {
        return tx.inTransaction(() -> { lock.lock(spaceId); var old=require(spaceId,id); var money=validate(spaceId,date,type,amount,source,destination,category,description); return ledger.update(new CashTransaction(id,spaceId,date,type,money,source,destination,category,description,old.getCreatedAt(),LedgerValidation.now())); });
    }
    public Optional<CashTransaction> get(UUID spaceId, UUID id) { return tx.inTransaction(() -> ledger.get(spaceId,id)); }
    public List<CashTransaction> list(UUID spaceId) { return tx.inTransaction(() -> { requireSpace(spaceId); return ledger.list(spaceId); }); }
    public void delete(UUID spaceId, UUID id) { tx.inTransaction(() -> { lock.lock(spaceId); ledger.delete(require(spaceId,id)); return null; }); }
    private CashTransaction require(UUID spaceId, UUID id) { return ledger.get(spaceId,id).orElseThrow(() -> new NoSuchElementException("Transaction not found")); }
    private BigDecimal validate(UUID spaceId, LocalDate date, TransactionType type, BigDecimal amount, UUID source, UUID destination, UUID category, String description) {
        if (date == null || type == null) throw new IllegalArgumentException("Transaction date and type are required");
        BigDecimal money=LedgerValidation.money(amount);
        if (money.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (description != null && description.codePointCount(0,description.length()) > 1000) throw new IllegalArgumentException("Description exceeds 1000 characters");
        boolean valid = switch(type) {
            case INCOME -> source == null && destination != null && category != null;
            case EXPENSE -> source != null && destination == null && category != null;
            case TRANSFER -> source != null && destination != null && !source.equals(destination) && category == null;
        };
        if (!valid) throw new IllegalArgumentException("Invalid accounts or category for transaction type");
        for (UUID id : new UUID[]{source,destination}) if (id != null) {
            var account=accounts.get(spaceId,id).orElseThrow(() -> new NoSuchElementException("Account not found"));
            if (date.isBefore(account.getTrackingStartDate())) throw new IllegalArgumentException("Transaction precedes tracking start date");
        }
        if (category != null) {
            var c=categories.get(spaceId,category).orElseThrow(() -> new NoSuchElementException("Category not found"));
            if ((type == TransactionType.INCOME && c.getType()!=CategoryType.INCOME) || (type == TransactionType.EXPENSE && c.getType()!=CategoryType.EXPENSE)) throw new IllegalArgumentException("Category type does not match transaction");
        }
        return money;
    }
    private void requireSpace(UUID spaceId) { spaces.findById(spaceId).orElseThrow(() -> new NoSuchElementException("Space not found")); }

}
