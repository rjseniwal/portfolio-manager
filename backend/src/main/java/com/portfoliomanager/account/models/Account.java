package com.portfoliomanager.account.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class Account {
    private UUID id;
    private UUID spaceId;
    private String name;
    private AccountType type;
    private BigDecimal startingBalance;
    private LocalDate trackingStartDate;
    private Instant createdAt;
    private Instant updatedAt;
}
