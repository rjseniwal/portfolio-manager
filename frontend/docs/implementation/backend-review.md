# Independent staged backend review

Reviewed `/tmp/portfolio-ledger-backend` against its `AGENTS.md` and the ledger API contract. This was a static source review; no application files were edited and no existing tests were rerun.

## Findings

No additional important, reproducible correctness or security defects identified in the new backend implementation.

The already identified LocalDate serialization issue is addressed in the reviewed staging source: `PortfolioManagerApplication.initialize` disables `WRITE_DATES_AS_TIMESTAMPS`, and `LedgerHttpTest` now asserts ISO date strings for create/detail/list/report responses. Execution of those updated assertions belongs to the implementation verification pass.

## Reviewed behavior

- Collection and export queries constrain transaction roots and related-record joins to the requested space. Cross-space account/category filters are rejected through space-scoped lookups before query/export execution.
- SQL filter values use prepared parameters; sort expressions come from explicit allowlists. Literal search escapes `%`, `_`, and the escape character. Pagination is bounded and checked for offset overflow; ordering ends in unique IDs.
- Items and counts use the same filter builder inside the service transaction. On the intended MariaDB default repeatable-read isolation, the queries share a consistent snapshot.
- Account balances aggregate each direction before joining, avoiding multiplication. Summary balances aggregate starting balances and ledger movements separately. BigDecimal/JDBC decimal values remain exact through response string conversion.
- Cash flow and category totals exclude transfers; transaction counts include them. Ledger balance includes all dates. Monthly queries apply exact inclusive date bounds before aggregation and fill missing months with zero values.
- Account/category deletion and tracking-date checks use scoped existence queries inside the existing space write lock. Balance calculation uses an aggregate instead of loading all transactions.
- Export reuses transaction filter/sort definitions and omits LIMIT/OFFSET. CSV output uses UTF-8 BOM, CRLF record endings, field quoting, embedded-quote escaping, and formula-prefix protection after leading whitespace.
- Streaming owns its JDBC statement/result set through try-with-resources. Output IOExceptions are wrapped during row consumption, unwind these resources, and cause transaction rollback/session closure; the HTTP-owned output stream is not closed by the service.
- The page/report DTOs include the contracted fields, with monetary values represented as strings. Services, repository, report resource, and CSV writer are registered through the application/Guice wiring. V3 indexes use the expected space-scoped columns.

## Limits

This review does not establish real-world memory or latency bounds for very large exports, nor does it execute browser/server integration. The existing tests and live MariaDB/HTTP verification were inspected as context, not rerun or independently certified here.
