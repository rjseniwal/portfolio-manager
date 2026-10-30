# Cash ledger implementation progress

Approved scope: routed React frontend plus paginated backend collections, SQL reports and streamed CSV export. Full plan is in the conversation (2026-10-31).

## Contracts
- Existing /accounts, /categories, /transactions GET collections change to {items,page,size,total}; page 1 default, size 20, range 1..100.
- All routes scoped /api/spaces/{spaceId}; accounts list includes balance; transaction list includes related names.
- Query filters q,type,accountId,categoryId,from,to,sort. Transaction sort date_desc/date_asc/amount_desc/amount_asc; accounts/categories name_asc/name_desc/created_asc/created_desc.
- Reports /reports/summary and /reports/monthly require from/to. Summary income,expense,netCashFlow,transactionCount,ledgerBalance,accountCount,categoryTotals; monthly items month,income,expense,netCashFlow.
- CSV /transactions/export filters match transaction listing, no paging.
- All monetary JSON fields decimal strings. Balance includes future transactions; range cash flow excludes transfers/openings.

## Milestones
- [x] Backend pagination, SQL balances/conflicts, migration
- [x] Backend SQL reports and streaming CSV
- [x] Frontend routing/query infrastructure
- [x] Account/category management
- [x] Transaction management/history/activity/export
- [x] Dashboard/reports
- [x] Automated tests, review, documentation, integration

## Rulings
- No Git metadata exists; edit current frontend and stage backend in /tmp/portfolio-ledger-backend. Apply backend files after verification with escalation.
- Backend and frontend implementation can proceed independently against the fixed contracts above.

Frontend verification so far: 31 Node tests pass, 2 Playwright smoke scenarios pass, production build passes. Extended workflow tests and backend integration still in progress.

## Final verification
- Frontend: npm test — 31 tests passed; npm run build — passed.
- Chromium: full Playwright suite with live backend enabled — 19 passed, including real MariaDB/HTTP reconciliation, filtered CSV, mobile layouts and both themes.
- Actual backend project: JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn -o -DisolatedBuild package — 15 tests passed, BUILD SUCCESS.
- Dedicated MariaDB migrations/query/write/lock/FK runs passed; application database untouched.
- Independent frontend/backend reviews complete. All 3 frontend P2 findings reproduced in failing browser tests and fixed; ISO date mismatch reproduced and fixed in HTTP tests.
- All 38 backend files applied with before/after hash checks; original-file backups in /tmp/portfolio-backend-before-ledger.
- Temporary backend server19090 and dedicated test DB removed after checks. Original9090 server remains running the old build; restart it to use the new APIs.
- V3 index migration remains an explicit application rollout step. No application DB DDL was executed.
