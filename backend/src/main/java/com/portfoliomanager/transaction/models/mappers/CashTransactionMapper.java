package com.portfoliomanager.transaction.models.mappers;
import com.portfoliomanager.transaction.models.CashTransaction;
import com.portfoliomanager.transaction.models.StoredCashTransaction;
public class CashTransactionMapper {
    public StoredCashTransaction toStored(CashTransaction value) { return new StoredCashTransaction(value.getId(), value.getSpaceId(), value.getTransactionDate(), value.getType(), value.getAmount(), value.getSourceAccountId(), value.getDestinationAccountId(), value.getCategoryId(), value.getDescription(), value.getCreatedAt(), value.getUpdatedAt()); }
    public CashTransaction toDomain(StoredCashTransaction value) { return new CashTransaction(value.getId(), value.getSpaceId(), value.getTransactionDate(), value.getType(), value.getAmount(), value.getSourceAccountId(), value.getDestinationAccountId(), value.getCategoryId(), value.getDescription(), value.getCreatedAt(), value.getUpdatedAt()); }
}
