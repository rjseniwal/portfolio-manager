package com.portfoliomanager.transaction.repositories;
import com.portfoliomanager.transaction.models.CashTransaction;
import java.util.*;
public interface CashTransactionRepository {
    Optional<CashTransaction> get(UUID spaceId, UUID id);
    List<CashTransaction> list(UUID spaceId);
    CashTransaction insert(CashTransaction value);
    CashTransaction update(CashTransaction value);
    void delete(CashTransaction value);
}
