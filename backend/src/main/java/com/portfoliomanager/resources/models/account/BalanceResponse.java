package com.portfoliomanager.resources.models.account;
import java.util.UUID;
public record BalanceResponse(UUID accountId, String balance) {}
