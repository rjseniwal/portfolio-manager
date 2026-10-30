package com.portfoliomanager.ledger.models;
import com.portfoliomanager.account.models.Account;
import java.math.BigDecimal;
public record AccountBalance(Account account,BigDecimal balance) {}
