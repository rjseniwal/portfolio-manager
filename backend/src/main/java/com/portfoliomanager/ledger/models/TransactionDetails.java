package com.portfoliomanager.ledger.models;
import com.portfoliomanager.transaction.models.CashTransaction;
public record TransactionDetails(CashTransaction transaction,String sourceAccountName,String destinationAccountName,String categoryName) {}
