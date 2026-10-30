package com.portfoliomanager.guice;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.dropwizard.core.setup.Environment;
import java.util.Objects;

public class ApplicationModule extends AbstractModule {
    private final Environment environment;

    public ApplicationModule(Environment environment) {
        this.environment = Objects.requireNonNull(environment);
    }

    @Override
    protected void configure() {
        bind(Environment.class).toInstance(environment);
        bind(ObjectMapper.class).toInstance(environment.getObjectMapper());
        bind(MetricRegistry.class).toInstance(environment.metrics());
    }
}
