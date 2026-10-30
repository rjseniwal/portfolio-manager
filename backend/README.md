# Portfolio Manager backend

A minimal Java 17+ Dropwizard 5.0.2 server generated with the official
`io.dropwizard.archetypes:java-simple:5.0.2` Maven archetype. It uses
Dropwizard's Hibernate integration and MariaDB Connector/J.

## Run

Start MariaDB on `localhost:3306` with the local development credentials
`root` / `root`, and create the database first:

```sql
CREATE DATABASE portfolio_manager;
```

Then build and start the server:

```sh
mvn -DisolatedBuild clean package
java -jar target/standalone/portfolio-manager-1.0-SNAPSHOT.jar server config.yml
```

The isolated build keeps Maven's output separate from IDE-compiled classes. Use
Java 17 or later for both Maven and the server. If an IDE run reports `Unresolved
compilation problems`, refresh its Maven project and Java workspace; run the
isolated JAR above while resolving any IDE build errors.

`GET http://localhost:9090/status` returns `{"status":"ok"}`. The admin
endpoint `http://localhost:9091/healthcheck` includes the Hibernate database
health check. The server requires a reachable database at startup.

Database settings are in `config.yml`; these credentials are intended only for
local development. The Hibernate bundle registers the `StoredUserSpace` entity. The versioned SQL schema
is in `src/main/resources/db/migration`; migration execution is not wired up yet.
Apply `V1__create_spaces.sql`, `V2__create_cash_ledger.sql`, and
`V3__ledger_query_indexes.sql` in order before using the entities. V3 adds indexes
for space/name and space/type/date/creation queries. Existing migrations are immutable.
Hibernate does not create or alter tables.

## Spaces persistence

`UserSpaceRepository` exposes domain models and maps to the stored model. Its
MariaDB implementation delegates to a Hibernate Data Repository generated at
build time. The service wraps its repository operations in a transaction; nested
calls share the transaction and a failure marks it for rollback. Direct repository
calls require an active `TransactionRunner` transaction.

Run the automated tests with `mvn test` (the persistence test uses an isolated H2
in-memory database). To run that test against MariaDB instead, use a **dedicated
empty test database** and set `test.db.url`, `test.db.user`, and
`test.db.password` Maven system properties. The test creates the `spaces` table
in that database and leaves it there; do not point it at an application database.

## List spaces

`GET /api/spaces?page=1&size=20` returns an object containing `items`, `page`,
`size`, and `total`. Pages are numbered from 1; the default page size is 20
and the allowed range is 1–100. Results are ordered by creation time, then ID.
An out-of-range page returns an empty `items` array. Invalid pagination values
return HTTP 400 with code `INVALID_PAGINATION`. Timestamps are UTC ISO-8601
strings. As with offset pagination generally, inserts or deletes between requests
may shift results across pages.

## Create, retrieve, and rename spaces

```sh
curl -i -X POST http://localhost:9090/api/spaces \
  -H 'Content-Type: application/json' -d '{"name":"Personal"}'
curl http://localhost:9090/api/spaces/SPACE_ID
curl -X PATCH http://localhost:9090/api/spaces/SPACE_ID \
  -H 'Content-Type: application/json' -d '{"name":"Family"}'
```

Create returns HTTP 201 with the new space and a `Location` header. Retrieve and
rename return HTTP 200 with the space. The name is trimmed, must not be blank,
and may contain at most 255 Unicode characters; duplicate names are allowed.
Malformed IDs and invalid names return HTTP 400; a nonexistent space returns
HTTP 404. Application validation errors contain a `code` and `message`.

## API documentation

Swagger UI is available at `http://localhost:9090/docs/`. The OpenAPI
definition is available at `http://localhost:9090/openapi.json` (or
`/openapi.yaml`). The UI assets are served locally.

## Ledger services

The account, category, and cash transaction service interfaces provide space-scoped
operations. Account balances are derived from the starting balance and ledger;
no balance is stored. Transactions support income, expense, and transfer. Ledger
writes acquire a space-row lock so validation and mutation occur in one database
transaction. Monetary inputs must fit `DECIMAL(19,2)` without rounding.

Collection reads use SQL pagination, aggregate balances, and joined transaction
name projections. Balance reads and account/category conflict checks use aggregate
or existence SQL without loading the ledger. Report aggregates also run in SQL.
The read repository returns domain projections and shares the Hibernate
StatelessSession transaction with the service layer. Default categories are not automatically seeded. Migration execution is
not yet wired up.

The ledger integration test uses H2 by default. To test the MariaDB migrations and
constraints, provide `ledger.test.db.url`, `ledger.test.db.user`, and
`ledger.test.db.password` as Maven system properties pointing to a dedicated,
empty test database. The test creates tables and records in that database.

## Account, category, and transaction APIs

All paths are scoped under `/api/spaces/{spaceId}`:

| Resource | Operations |
| --- | --- |
| `/accounts` | GET list, POST create |
| `/accounts/{id}` | GET, PATCH, DELETE |
| `/accounts/{id}/balance` | GET derived balance |
| `/categories` | GET list, POST create |
| `/categories/{id}` | GET, PATCH name, DELETE |
| `/transactions` | GET list, POST create |
| `/transactions/{id}` | GET, PATCH, DELETE |

Create returns 201 with a `Location` header; delete returns 200 with the deleted
ID. Monetary values are decimal strings, such as `"100.00"`; dates use
`YYYY-MM-DD`. The application disables Jackson timestamp-style date serialization
so LocalDate fields are always ISO strings in create, item, list, and report responses. Account PATCH accepts any of `name`, `startingBalance`, and
`trackingStartDate` together, atomically. Transaction PATCH requires the complete
transaction details, including the account and category fields applicable to
the chosen type. Collections use the page envelope and filters documented below. Invalid inputs return 400, missing or cross-space entities return
404, and conflicting deletes or date changes return 409. See Swagger UI for the
request and response shapes.


## Breaking collection API change

`GET /accounts`, `/categories`, and `/transactions` now return
`{"items":[],"page":1,"size":20,"total":0}` instead of arrays. Clients must read
`items`. `total` counts all matching rows before pagination. `page` starts at 1;
`size` defaults to 20 and accepts 1 through 100. Pages beyond the end are empty.
Malformed, nonpositive, oversized, or overflowing pagination values return 400.

All three collections accept `q`, `type`, and `sort`. `q` is a case-insensitive
literal substring (name for accounts/categories, description for transactions);
`%`, `_`, and `!` are treated as literal characters. An empty `q` applies no search filter. Account types are BANK/CASH;
category types are INCOME/EXPENSE; transaction types are INCOME/EXPENSE/TRANSFER.
Unknown type and sort values return 400.

Accounts and categories support `name_asc` (default), `name_desc`, `created_asc`,
and `created_desc`. Name ordering compares lowercase names; ties use creation
time and ID. Creation ordering ties use ID. Account collection items include
all existing account fields and `balance`, calculated from the opening balance
and every transaction, including future dates. Item and mutation response shapes
and `/accounts/{id}/balance` remain available.

Transactions additionally accept `accountId` (matches either source or
destination), `categoryId`, and optional inclusive ISO dates `from`/`to`.
Malformed IDs/dates and a reversed range return 400. Missing or cross-space
filter references return 404. Sorts are `date_desc` (default), `date_asc`,
`amount_desc`, and `amount_asc`, with ascending creation time and ID as stable
tie breakers. Transaction collection items include all existing fields plus
nullable `sourceAccountName`, `destinationAccountName`, and `categoryName`.
Offset pages can shift if writes occur between requests.

## Reports

`GET /api/spaces/{spaceId}/reports/summary?from=2026-01-01&to=2026-01-31`
requires both dates and returns:

```json
{
  "from": "2026-01-01", "to": "2026-01-31",
  "income": "50.00", "expense": "5.00", "netCashFlow": "45.00",
  "transactionCount": 3, "ledgerBalance": "165.00", "accountCount": 2,
  "categoryTotals": [
    {"categoryId": "UUID", "name": "Salary", "type": "INCOME", "amount": "50.00", "transactionCount": 1},
    {"categoryId": "UUID", "name": "Food", "type": "EXPENSE", "amount": "5.00", "transactionCount": 1}
  ]
}
```

Range cash flow includes income and expense only; it excludes transfers and
opening balances. `transactionCount` includes transfers. Category totals contain
only categories used by income/expense in the range, ordered by type/name/ID.
`ledgerBalance` includes all account opening balances and all transaction dates,
including dates outside the report range and future dates. `accountCount` counts
all accounts in the space. Monetary JSON fields are decimal strings.

`GET /api/spaces/{spaceId}/reports/monthly?from=2026-01-15&to=2026-03-10`
returns `{from,to,items:[{month:"2026-01",income:"0.00",expense:"0.00",netCashFlow:"0.00"}]}`
with every inclusive calendar month, zero-filled and ordered chronologically.
The first and last months honor the exact date boundaries. Transfers and opening
balances are excluded. Missing, invalid, or reversed dates return 400; a missing
space returns 404.

## CSV export

`GET /api/spaces/{spaceId}/transactions/export` accepts the transaction collection's
filters and sorts and exports every matching row without pagination. Validation
runs before the CSV response is returned. A response model and registered
`MessageBodyWriter` delegate to the export service, which opens one transaction
and streams a forward-only JDBC result with bounded fetch size through the
StatelessSession connection. Result sets, statements, and transactions are closed
on completion or client disconnect; no full ledger list is materialized.

The response is `text/csv; charset=UTF-8` with a download filename, UTF-8 BOM,
CRLF record endings, RFC-style quote/comma/newline escaping, and an apostrophe
prefix on spreadsheet formula-like fields (including formulas after whitespace).
The exact columns are:

```text
id,spaceId,transactionDate,type,amount,sourceAccountId,sourceAccountName,destinationAccountId,destinationAccountName,categoryId,categoryName,description,createdAt,updatedAt
```

Null references and related names are empty cells. Money uses decimal strings;
dates and UTC timestamps use ISO formats.

## Verification

```sh
mvn -DisolatedBuild test
mvn -DisolatedBuild package
```

Tests cover H2 persistence/services, filtered pages, literal search, balances,
reports, zero-filled partial months, CSV encoding/escaping/disconnects, and actual
Dropwizard HTTP resource/provider registration. The HTTP test launches a temporary
H2 server subprocess on available ports and stops it after completion.

For MariaDB, use **separate empty test databases**, never the application database.
The following commands apply V1/V2/V3 and leave test data in their supplied databases:

```sh
mvn -DisolatedBuild -Dtest=LedgerServiceTest \
  -Dledger.test.db.url=jdbc:mariadb://localhost:3306/ledger_writes_test \
  -Dledger.test.db.user=root -Dledger.test.db.password=root test
mvn -DisolatedBuild -Dtest=LedgerQueryTest#filteredPagesReportsAndExportPreserveLedgerSemantics \
  -Dledger.query.test.db.url=jdbc:mariadb://localhost:3306/ledger_queries_test \
  -Dledger.query.test.db.user=root -Dledger.query.test.db.password=root test
```

Recreate those dedicated databases before repeating the migration tests. A failed
stream can result in a partial CSV download, since headers may already have been
sent; the database cursor and transaction are still closed.
