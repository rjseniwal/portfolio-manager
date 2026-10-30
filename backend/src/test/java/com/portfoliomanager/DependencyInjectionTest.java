package com.portfoliomanager;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.portfoliomanager.guice.*;
import com.portfoliomanager.resources.StatusResource;
import com.portfoliomanager.space.repositories.UserSpaceRepository;
import com.portfoliomanager.space.services.UserSpaceService;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class DependencyInjectionTest {
    @Test
    void resolvesInterfacesAndSharesApplicationInstances() {
        SessionFactory factory = (SessionFactory) Proxy.newProxyInstance(
                SessionFactory.class.getClassLoader(), new Class<?>[]{SessionFactory.class},
                (proxy, method, args) -> null);
        Environment environment = new Environment("test");
        PortfolioManagerConfiguration config = new PortfolioManagerConfiguration();
        Injector injector = Guice.createInjector(new ApplicationModule(environment), new ServiceModule(),
                new RepositoryModule(factory), new ClientModule(), new ConfigModule(config));
        assertNotNull(injector.getInstance(UserSpaceService.class));
        assertNotNull(injector.getInstance(UserSpaceRepository.class));
        assertNotNull(injector.getInstance(StatusResource.class));
        assertSame(factory, injector.getInstance(SessionFactory.class));
        assertSame(environment, injector.getInstance(Environment.class));
        assertSame(environment.getObjectMapper(), injector.getInstance(ObjectMapper.class));
        assertSame(environment.metrics(), injector.getInstance(MetricRegistry.class));
        assertSame(config, injector.getInstance(PortfolioManagerConfiguration.class));
        assertSame(config.getDatabase(), injector.getInstance(DataSourceFactory.class));
    }
}
