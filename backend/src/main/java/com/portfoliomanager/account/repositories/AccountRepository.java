package com.portfoliomanager.account.repositories;
import com.portfoliomanager.account.models.Account;
import java.util.*;
public interface AccountRepository {
    Optional<Account> get(UUID spaceId, UUID id);
    List<Account> list(UUID spaceId);
    Account insert(Account value);
    Account update(Account value);
    void delete(Account value);
}
