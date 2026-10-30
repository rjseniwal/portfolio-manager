package com.portfoliomanager.transaction.services;
import com.portfoliomanager.transaction.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public interface CashTransactionService {
    CashTransaction create(UUID spaceId, LocalDate date, TransactionType type, BigDecimal amount, UUID source, UUID destination, UUID category, String description);
    CashTransaction update(UUID spaceId, UUID id, LocalDate date, TransactionType type, BigDecimal amount, UUID source, UUID destination, UUID category, String description);
    Optional<CashTransaction> get(UUID spaceId, UUID id);
    List<CashTransaction> list(UUID spaceId);
    void delete(UUID spaceId, UUID id);
}
