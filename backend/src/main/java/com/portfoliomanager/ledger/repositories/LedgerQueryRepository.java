package com.portfoliomanager.ledger.repositories;
import com.portfoliomanager.ledger.models.*;
import com.portfoliomanager.category.models.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
public interface LedgerQueryRepository {
    Page<AccountBalance> accounts(UUID spaceId,LedgerFilter filter);
    Page<Category> categories(UUID spaceId,LedgerFilter filter);
    Page<TransactionDetails> transactions(UUID spaceId,LedgerFilter filter);
    Summary summary(UUID spaceId,LocalDate from,LocalDate to);
    MonthlyReport monthly(UUID spaceId,LocalDate from,LocalDate to);
    void streamTransactions(UUID spaceId,LedgerFilter filter,Consumer<TransactionDetails> consumer);
    BigDecimal accountDelta(UUID spaceId,UUID accountId);
    boolean accountUsed(UUID spaceId,UUID accountId,LocalDate before);
    boolean categoryUsed(UUID spaceId,UUID categoryId);
}
