package com.portfoliomanager.resources.models.transaction;
import com.portfoliomanager.transaction.models.*;
import com.portfoliomanager.resources.models.CreatedModel;
import java.time.*;
import java.util.UUID;
public record TransactionResponse(UUID id, UUID spaceId, LocalDate transactionDate, TransactionType type, String amount, UUID sourceAccountId, UUID destinationAccountId, UUID categoryId, String description, String createdAt, String updatedAt) implements CreatedModel {
    public static TransactionResponse from(CashTransaction t) { return new TransactionResponse(t.getId(),t.getSpaceId(),t.getTransactionDate(),t.getType(),t.getAmount().toPlainString(),t.getSourceAccountId(),t.getDestinationAccountId(),t.getCategoryId(),t.getDescription(),t.getCreatedAt().toString(),t.getUpdatedAt().toString()); }
}
