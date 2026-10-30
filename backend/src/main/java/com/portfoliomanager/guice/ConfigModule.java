package com.portfoliomanager.guice;

import com.google.inject.AbstractModule;
import com.portfoliomanager.PortfolioManagerConfiguration;
import io.dropwizard.db.DataSourceFactory;
import java.util.Objects;

public class ConfigModule extends AbstractModule {
    private final PortfolioManagerConfiguration configuration;

    public ConfigModule(PortfolioManagerConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    protected void configure() {
        bind(PortfolioManagerConfiguration.class).toInstance(configuration);
        bind(DataSourceFactory.class).toInstance(configuration.getDatabase());
    }
}
