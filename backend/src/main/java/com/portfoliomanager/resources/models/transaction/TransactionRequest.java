package com.portfoliomanager.resources.models.transaction;
import com.portfoliomanager.transaction.models.TransactionType;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.portfoliomanager.resources.DecimalStringDeserializer;
import java.util.UUID;
public record TransactionRequest(LocalDate transactionDate, TransactionType type, @JsonDeserialize(using = DecimalStringDeserializer.class) String amount, UUID sourceAccountId, UUID destinationAccountId, UUID categoryId, String description) {}
