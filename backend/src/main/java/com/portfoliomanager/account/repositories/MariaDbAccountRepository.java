package com.portfoliomanager.account.repositories;
import com.portfoliomanager.account.models.Account;
import com.portfoliomanager.account.models.mappers.AccountMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.*;
public class MariaDbAccountRepository implements AccountRepository {
    private final Provider<StoredAccountDao> dao;
    private final AccountMapper mapper;
    @Inject public MariaDbAccountRepository(Provider<StoredAccountDao> dao, AccountMapper mapper) { this.dao=dao; this.mapper=mapper; }
    public Optional<Account> get(UUID spaceId, UUID id) { return dao.get().get(spaceId,id).map(mapper::toDomain); }
    public List<Account> list(UUID spaceId) { return dao.get().list(spaceId).stream().map(mapper::toDomain).toList(); }
    public Account insert(Account value) { dao.get().insert(mapper.toStored(value)); return value; }
    public Account update(Account value) { dao.get().update(mapper.toStored(value)); return value; }
    public void delete(Account value) { dao.get().delete(mapper.toStored(value)); }
}
