# Ledger browser workflow tests

Added `tests/e2e/ledger-workflows.spec.js` with 16 Chromium workflows. This task changed only that new test file and this report; production fixes were reported to and implemented by the parent task.

The tests use `page.context().route` with mutable records, paginated `{items,page,size,total}` responses, account balances, and transaction account/category names. Successful writes update the fixture and must appear in the UI through the app's query invalidation and refetch. Submitted POST/PATCH/DELETE requests are captured from actual browser HTTP requests and checked alongside visible results.

Coverage:

- Account creation and editing: negative opening balances, values beyond JavaScript's safe integer precision, Unicode names, fixed decimals, immutable type, and the exact PATCH fields.
- Category creation and renaming: Unicode names, immutable type, and POST/PATCH payloads.
- Separate INCOME, EXPENSE, and TRANSFER creation workflows, including the maximum `DECIMAL(19,2)` amount and one-cent transfers.
- Transaction edits across all three types, checking full PATCH bodies and explicit nulls for unused source/destination/category/description fields.
- Nested account/category creation from an expense draft, plus creating both accounts for a transfer. Dates, amounts, descriptions, and selected records survive nested dialogs; focus returns to the appropriate parent button.
- Category picker page reset and empty selection when changing transaction type.
- Failed transaction save with every draft field retained and successful retry.
- Deleting the sole row on page two of both account and transaction lists, then automatic URL/page correction and refreshed rows.
- Space switching, isolated visible records, a 503 list failure, and recovery through Try again.
- Keyboard opening, focus trapping, Escape/Close/save focus restoration, and blocking close/duplicate submission during a pending save.
- A 390×844 viewport: contained table overflow, dialog bounds, scrollable form controls, and a completed transaction save.

Regression findings reproduced before the parent fixes:

1. Top-level dialogs lost opener focus after Escape and successful save.
2. Creating a category from an expense draft allowed changing its type to INCOME.
3. Changing transaction type left the category picker on page two, hiding a smaller compatible result set.

All three now have passing browser assertions. The tests also assert the parent's improved disabled Close control during saves. An initial test expectation for backend text on a 503 response was corrected after confirming the API client intentionally uses its generic retry message for server errors.

Verification command:

```sh
PLAYWRIGHT_BROWSERS_PATH=/tmp/portfolio-playwright npx playwright test tests/e2e/ledger-workflows.spec.js
```

Final result: **16 passed, 0 failed, 5.7 seconds**. Chromium required elevated execution because the macOS sandbox denied its Mach port registration; the approved elevated run completed successfully. Live backend behavior is covered separately by the parent task.
