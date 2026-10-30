package com.portfoliomanager.resources.models.account;
import com.portfoliomanager.account.models.AccountType;
import com.portfoliomanager.ledger.models.AccountBalance;
import java.time.LocalDate;
import java.util.UUID;
public record AccountListResponse(UUID id,UUID spaceId,String name,AccountType type,String startingBalance,LocalDate trackingStartDate,String createdAt,String updatedAt,String balance) {
    public static AccountListResponse from(AccountBalance item){var a=item.account();return new AccountListResponse(a.getId(),a.getSpaceId(),a.getName(),a.getType(),a.getStartingBalance().toPlainString(),a.getTrackingStartDate(),a.getCreatedAt().toString(),a.getUpdatedAt().toString(),item.balance().toPlainString());}
}
