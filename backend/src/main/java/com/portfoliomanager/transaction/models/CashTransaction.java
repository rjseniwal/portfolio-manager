package com.portfoliomanager.transaction.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CashTransaction {
    private UUID id;
    private UUID spaceId;
    private LocalDate transactionDate;
    private TransactionType type;
    private BigDecimal amount;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private UUID categoryId;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
