package com.portfoliomanager.resources.models.account;
import com.portfoliomanager.account.models.*;
import com.portfoliomanager.resources.models.CreatedModel;
import java.time.*;
import java.util.UUID;
public record AccountResponse(UUID id, UUID spaceId, String name, AccountType type, String startingBalance, LocalDate trackingStartDate, String createdAt, String updatedAt) implements CreatedModel {
    public static AccountResponse from(Account a) { return new AccountResponse(a.getId(),a.getSpaceId(),a.getName(),a.getType(),a.getStartingBalance().toPlainString(),a.getTrackingStartDate(),a.getCreatedAt().toString(),a.getUpdatedAt().toString()); }
}
