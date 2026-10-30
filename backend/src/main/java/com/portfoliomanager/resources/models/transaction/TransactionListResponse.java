package com.portfoliomanager.resources.models.transaction;
import com.portfoliomanager.transaction.models.TransactionType;
import com.portfoliomanager.ledger.models.TransactionDetails;
import java.time.LocalDate;
import java.util.UUID;
public record TransactionListResponse(UUID id,UUID spaceId,LocalDate transactionDate,TransactionType type,String amount,UUID sourceAccountId,UUID destinationAccountId,UUID categoryId,String description,String createdAt,String updatedAt,String sourceAccountName,String destinationAccountName,String categoryName) {
    public static TransactionListResponse from(TransactionDetails item){var t=item.transaction();return new TransactionListResponse(t.getId(),t.getSpaceId(),t.getTransactionDate(),t.getType(),t.getAmount().toPlainString(),t.getSourceAccountId(),t.getDestinationAccountId(),t.getCategoryId(),t.getDescription(),t.getCreatedAt().toString(),t.getUpdatedAt().toString(),item.sourceAccountName(),item.destinationAccountName(),item.categoryName());}
}
