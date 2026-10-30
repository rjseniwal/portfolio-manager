# Ledger service and repository implementation

1. Add integration tests for space-scoped accounts, categories, transactions, derived balances, validation, and deletion constraints.
2. Add versioned MariaDB schema with space-scoped foreign keys and transaction checks.
3. Add separate domain/stored models, mappers, Jakarta Data DAOs, and repository adapters for each entity.
4. Implement services with atomic, serialized space-scoped writes and explicit validation; register dependencies and entities.
5. Run the Java 17 test suite and verify the schema and service operations against MariaDB; document migration usage and remaining scope.
