package com.portfoliomanager.guice;

import com.google.inject.AbstractModule;
import com.portfoliomanager.ledger.services.*;
import com.portfoliomanager.transaction.services.CashTransactionService;
import com.portfoliomanager.transaction.services.DefaultCashTransactionService;
import com.portfoliomanager.category.services.CategoryService;
import com.portfoliomanager.category.services.DefaultCategoryService;
import com.portfoliomanager.account.services.AccountService;
import com.portfoliomanager.account.services.DefaultAccountService;
import com.portfoliomanager.space.services.DefaultUserSpaceService;
import com.portfoliomanager.space.services.UserSpaceService;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LedgerQueryService.class).to(DefaultLedgerQueryService.class);
        bind(LedgerExportService.class).to(DefaultLedgerExportService.class);
        bind(AccountService.class).to(DefaultAccountService.class);
        bind(CategoryService.class).to(DefaultCategoryService.class);
        bind(CashTransactionService.class).to(DefaultCashTransactionService.class);
        bind(UserSpaceService.class).to(DefaultUserSpaceService.class);
    }
}
