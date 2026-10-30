package com.portfoliomanager.account.models.mappers;
import com.portfoliomanager.account.models.Account;
import com.portfoliomanager.account.models.StoredAccount;
public class AccountMapper {
    public StoredAccount toStored(Account value) { return new StoredAccount(value.getId(), value.getSpaceId(), value.getName(), value.getType(), value.getStartingBalance(), value.getTrackingStartDate(), value.getCreatedAt(), value.getUpdatedAt()); }
    public Account toDomain(StoredAccount value) { return new Account(value.getId(), value.getSpaceId(), value.getName(), value.getType(), value.getStartingBalance(), value.getTrackingStartDate(), value.getCreatedAt(), value.getUpdatedAt()); }
}
