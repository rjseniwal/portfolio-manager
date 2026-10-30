# Final verification

Implementation includes the frontend workspace and matching backend APIs. See [contracts](ledger-contract.md), [frontend review](frontend-review.md), [backend review](backend-review.md), and [browser test report](browser-tests-report.md).

| Check | Result |
| --- | --- |
| Frontend unit tests | 31 passed |
| Full Chromium suite | 19 passed (18 mocked workflows + 1 live-backend workflow) |
| Frontend production build | Passed |
| Applied backend full package/test | 15 passed, BUILD SUCCESS |
| Dedicated MariaDB migrations and queries | Passed |
| Dedicated MariaDB write/locking/FK tests | Passed |
| Independent review | Findings fixed and covered by regression tests |

The live fixture created accounts with100.00/20.00 starting balances and income50.00/expense5.00/transfer10.00. Browser assertions verified balances135.00/30.00, total165.00, net45.00. Editing expense8.00 verified bank132.00 and net42.00. Deleting it verified bank140.00 and net50.00. Filtered CSV contained only matching rows. Mobile pages were inspected in light/dark mode.

After verification the temporary HTTP test backend and its dedicated MariaDB database were removed. No application data or configuration was altered. The existing backend process on9090 still needs to be restarted with the new build. Apply V3__ledger_query_indexes.sql using the existing manual migration process before rollout. Production hosting must fall back to index.html for frontend deep links while retaining the API proxy.

Normal npm run test:e2e skips the live fixture unless LEDGER_LIVE_ORIGIN is explicitly set. To rerun it, point LEDGER_LIVE_ORIGIN and BACKEND_ORIGIN to the same dedicated test backend; it creates a new space and records. Never use a shared production database for live tests.
