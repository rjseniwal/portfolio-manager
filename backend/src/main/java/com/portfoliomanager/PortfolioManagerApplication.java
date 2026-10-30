package com.portfoliomanager;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.portfoliomanager.space.models.StoredUserSpace;
import com.portfoliomanager.account.models.StoredAccount;
import com.portfoliomanager.category.models.StoredCategory;
import com.portfoliomanager.transaction.models.StoredCashTransaction;
import com.portfoliomanager.resources.StatusResource;
import com.portfoliomanager.resources.ReportResource;
import com.portfoliomanager.resources.CsvExportWriter;
import com.portfoliomanager.resources.AccountResource;
import com.portfoliomanager.resources.CategoryResource;
import com.portfoliomanager.resources.CashTransactionResource;
import com.portfoliomanager.resources.LedgerExceptionMappers;
import com.portfoliomanager.resources.CreatedSpaceResponseFilter;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import com.portfoliomanager.resources.UserSpaceResource;
import com.portfoliomanager.resources.InvalidPaginationMapper;
import com.portfoliomanager.resources.SpaceExceptionMapper;
import com.portfoliomanager.guice.ApplicationModule;
import com.portfoliomanager.guice.ClientModule;
import com.portfoliomanager.guice.ConfigModule;
import com.portfoliomanager.guice.RepositoryModule;
import com.portfoliomanager.guice.ServiceModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;


public class PortfolioManagerApplication extends Application<PortfolioManagerConfiguration> {
    private final HibernateBundle<PortfolioManagerConfiguration> hibernate =
            new HibernateBundle<PortfolioManagerConfiguration>(StoredUserSpace.class, StoredAccount.class, StoredCategory.class, StoredCashTransaction.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(PortfolioManagerConfiguration configuration) {
                    return configuration.getDatabase();
                }
            };

    public static void main(String[] args) throws Exception {
        new PortfolioManagerApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<PortfolioManagerConfiguration> bootstrap) {
        bootstrap.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(new AssetsBundle("/META-INF/resources/webjars/swagger-ui/5.30.2", "/swagger-ui", null, "swagger-assets"));
        bootstrap.addBundle(new AssetsBundle("/swagger-docs", "/docs", "index.html", "swagger-docs"));
    }

    @Override
    public void run(PortfolioManagerConfiguration configuration, Environment environment) {
        Injector injector = Guice.createInjector(new ApplicationModule(environment), new ServiceModule(),
                new RepositoryModule(hibernate.getSessionFactory()), new ClientModule(),
                new ConfigModule(configuration));
        environment.jersey().register(injector.getInstance(StatusResource.class));
        environment.jersey().register(injector.getInstance(UserSpaceResource.class));
        environment.jersey().register(injector.getInstance(AccountResource.class));
        environment.jersey().register(injector.getInstance(CategoryResource.class));
        environment.jersey().register(injector.getInstance(CashTransactionResource.class));
        environment.jersey().register(injector.getInstance(ReportResource.class));
        environment.jersey().register(injector.getInstance(CsvExportWriter.class));
        environment.jersey().register(LedgerExceptionMappers.Invalid.class);
        environment.jersey().register(LedgerExceptionMappers.Missing.class);
        environment.jersey().register(LedgerExceptionMappers.Conflict.class);
        environment.jersey().register(InvalidPaginationMapper.class);
        environment.jersey().register(SpaceExceptionMapper.class);
        environment.jersey().register(CreatedSpaceResponseFilter.class);
        environment.jersey().register(new OpenApiResource().resourcePackages(java.util.Set.of("com.portfoliomanager.resources")));
    }
}
