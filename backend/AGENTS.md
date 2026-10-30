# Project instructions

- Use anemic domain models and separate stored models for all entities. Keep business logic in the service layer.
- Define a repository interface for each entity so storage implementations can be swapped or extended. The current database implementation uses MariaDB. Repository interfaces and implementations return domain models, not stored models.
- Provide a transformation layer for each entity that converts between its domain and stored models. Use these transformations inside the repository implementation.
- Resources must call services, never repositories or the database directly.
- Keep resources free of business logic: their responsibility is to call services and return responses.
- Prefer Lombok for boilerplate where appropriate.
- Define an interface for each service and depend on interfaces wherever applicable.
- Use Google Guice for dependency injection.
- Place Guice modules in `com.portfoliomanager.guice`: `ApplicationModule` binds Dropwizard and application lifecycle objects; `ServiceModule` binds services; `RepositoryModule` binds repositories and database objects; `ClientModule` binds external clients; `ConfigModule` binds application configuration and its derived settings.
- Organize each entity feature package as follows:
  ```text
  <entity>/
  ├── repositories/   # repository interface and implementations
  ├── services/       # service interface and implementations
  └── models/         # domain and stored models
      └── mappers/    # domain/stored model transformations
  ```
- Place API resource classes under `com.portfoliomanager.resources`. Place request and response models under `com.portfoliomanager.resources.models.<entity_name>`.
- API resource methods must return response models rather than `jakarta.ws.rs.core.Response`.
- Annotate all API resources with Swagger annotations and keep documentation concise; avoid redundant descriptions.
