package com.portfoliomanager.persistence;

import java.util.function.Supplier;

public interface TransactionRunner {
    <T> T inTransaction(Supplier<T> work);
}
