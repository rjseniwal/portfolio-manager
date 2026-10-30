package com.portfoliomanager.guice;

import com.google.inject.AbstractModule;
import com.portfoliomanager.ledger.repositories.*;
import com.google.inject.Provides;
import com.portfoliomanager.persistence.HibernateTransactionRunner;
import com.portfoliomanager.persistence.TransactionRunner;
import com.portfoliomanager.space.repositories.StoredUserSpaceDao;
import com.portfoliomanager.space.repositories.StoredUserSpaceDao_;
import com.portfoliomanager.space.repositories.MariaDbUserSpaceRepository;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.account.repositories.*;
import com.portfoliomanager.category.repositories.*;
import com.portfoliomanager.transaction.repositories.*;
import org.hibernate.SessionFactory;
import java.util.Objects;

public class RepositoryModule extends AbstractModule {
    private final SessionFactory sessionFactory;

    public RepositoryModule(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    protected void configure() {
        bind(LedgerQueryRepository.class).to(MariaDbLedgerQueryRepository.class);
        bind(SessionFactory.class).toInstance(sessionFactory);
        bind(TransactionRunner.class).to(HibernateTransactionRunner.class);
        bind(AccountRepository.class).to(MariaDbAccountRepository.class);
        bind(CategoryRepository.class).to(MariaDbCategoryRepository.class);
        bind(CashTransactionRepository.class).to(MariaDbCashTransactionRepository.class);
        bind(UserSpaceRepository.class).to(MariaDbUserSpaceRepository.class);
    }

    @Provides
    StoredAccountDao storedAccountDao(HibernateTransactionRunner tx) { return new StoredAccountDao_(tx.currentSession()); }

    @Provides
    StoredCategoryDao storedCategoryDao(HibernateTransactionRunner tx) { return new StoredCategoryDao_(tx.currentSession()); }

    @Provides
    StoredCashTransactionDao storedCashTransactionDao(HibernateTransactionRunner tx) { return new StoredCashTransactionDao_(tx.currentSession()); }

    @Provides
    StoredUserSpaceDao storedUserSpaceDao(HibernateTransactionRunner transactions) {
        return new StoredUserSpaceDao_(transactions.currentSession());
    }
}
