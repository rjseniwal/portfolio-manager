package com.portfoliomanager.resources.models.account;
import com.portfoliomanager.account.models.AccountType;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.portfoliomanager.resources.DecimalStringDeserializer;
public record AccountRequest(String name, AccountType type, @JsonDeserialize(using = DecimalStringDeserializer.class) String startingBalance, LocalDate trackingStartDate) {}
