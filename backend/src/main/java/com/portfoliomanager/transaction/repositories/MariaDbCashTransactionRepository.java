package com.portfoliomanager.transaction.repositories;
import com.portfoliomanager.transaction.models.CashTransaction;
import com.portfoliomanager.transaction.models.mappers.CashTransactionMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.*;
public class MariaDbCashTransactionRepository implements CashTransactionRepository {
    private final Provider<StoredCashTransactionDao> dao;
    private final CashTransactionMapper mapper;
    @Inject public MariaDbCashTransactionRepository(Provider<StoredCashTransactionDao> dao, CashTransactionMapper mapper) { this.dao=dao; this.mapper=mapper; }
    public Optional<CashTransaction> get(UUID spaceId, UUID id) { return dao.get().get(spaceId,id).map(mapper::toDomain); }
    public List<CashTransaction> list(UUID spaceId) { return dao.get().list(spaceId).stream().map(mapper::toDomain).toList(); }
    public CashTransaction insert(CashTransaction value) { dao.get().insert(mapper.toStored(value)); return value; }
    public CashTransaction update(CashTransaction value) { dao.get().update(mapper.toStored(value)); return value; }
    public void delete(CashTransaction value) { dao.get().delete(mapper.toStored(value)); }
}
