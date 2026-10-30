package com.portfoliomanager.account.services;
import com.portfoliomanager.account.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public interface AccountService {
    Account create(UUID spaceId, String name, AccountType type, BigDecimal startingBalance, LocalDate startDate);
    Optional<Account> get(UUID spaceId, UUID id);
    List<Account> list(UUID spaceId);
    Account update(UUID spaceId, UUID id, String name, BigDecimal startingBalance, LocalDate startDate);
    Account rename(UUID spaceId, UUID id, String name);
    Account changeStartingBalance(UUID spaceId, UUID id, BigDecimal amount);
    Account changeTrackingStartDate(UUID spaceId, UUID id, LocalDate date);
    BigDecimal balance(UUID spaceId, UUID id);
    void delete(UUID spaceId, UUID id);
}
