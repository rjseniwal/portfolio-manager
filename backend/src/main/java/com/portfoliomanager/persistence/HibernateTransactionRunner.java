package com.portfoliomanager.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import java.util.function.Supplier;

@Singleton
public class HibernateTransactionRunner implements TransactionRunner {
    private final SessionFactory sessionFactory;
    private final ThreadLocal<StatelessSession> current = new ThreadLocal<>();

    @Inject
    public HibernateTransactionRunner(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public StatelessSession currentSession() {
        StatelessSession session = current.get();
        if (session == null) throw new IllegalStateException("An active transaction is required");
        return session;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            try {
                return work.get();
            } catch (RuntimeException | Error failure) {
                current.get().getTransaction().markRollbackOnly();
                throw failure;
            }
        }
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            Transaction transaction = session.beginTransaction();
            current.set(session);
            try {
                T result = work.get();
                if (transaction.getRollbackOnly()) {
                    throw new IllegalStateException("Transaction marked for rollback");
                }
                transaction.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) {
                    try { transaction.rollback(); }
                    catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                }
                throw failure;
            } finally {
                current.remove();
            }
        }
    }
}
