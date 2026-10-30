package com.portfoliomanager.ledger.services;
import com.portfoliomanager.ledger.models.*;
import com.portfoliomanager.category.models.Category;
import java.util.UUID;
public interface LedgerQueryService {
    Page<AccountBalance> accounts(UUID spaceId,LedgerFilter filter);
    Page<Category> categories(UUID spaceId,LedgerFilter filter);
    Page<TransactionDetails> transactions(UUID spaceId,LedgerFilter filter);
    Summary summary(UUID spaceId,String from,String to);
    MonthlyReport monthly(UUID spaceId,String from,String to);
    void validateTransactions(UUID spaceId,LedgerFilter filter);
}
