# Frontend helper implementation

Changed files:

- `src/shared/lib/money.js`
- `src/shared/lib/dates.js`
- `src/shared/lib/filters.js`
- `tests/unit/money.test.js`
- `tests/unit/dates.test.js`
- `tests/unit/filters.test.js`
- `docs/implementation/helpers-report.md`

Money parsing uses bigint cents throughout, accepts signed plain decimal strings with at most two decimal places, and enforces the signed `DECIMAL(19,2)` input bound. Normalization emits exactly two decimals. Formatting accepts decimal strings or bigint cents and allows report aggregates above the input bound without losing precision.

Date helpers use local calendar fields, preserve their supplied Date, and support month-to-date, inclusive 30-day, year-to-date, and twelve-month trend ranges. Date validation checks strict `YYYY-MM-DD` input and calendar validity, including leap-year century rules.

Query serialization omits null, undefined, and empty strings while preserving Unicode, zero, and false. Collection query normalization accepts only recognized filters, defaults page/size/sort, rejects partial or unsafe pagination numbers, and allows sizes 20, 50, and 100.

Verification:

- Wrote helper tests before implementation; initial `node --test tests/unit/*.test.js` failed because the helper modules did not exist. The first run also exposed two independently owned API failures, which were reported without changing API files.
- Final `node --test tests/unit/*.test.js`: **28 passed, 0 failed** (26 helper tests and 2 API tests).
- Local timezone rollover checks run in child Node processes using `Asia/Kolkata` and `America/Los_Angeles`, with constructed local dates on either side of UTC midnight.
- No package or application files outside the three assigned helper modules were changed by this task.
