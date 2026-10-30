# Cash ledger API contract

All ledger paths use `/api/spaces/{spaceId}`. UUIDs are space-scoped. Monetary JSON values are plain decimal strings; calendar dates use YYYY-MM-DD and timestamps use UTC ISO-8601.

## Collection envelopes

GET `/accounts`, `/categories`, `/transactions` return `{items,page,size,total}`. `page` defaults to1; `size` defaults to20 and supports1–100. Total reflects the filtered set. Empty and out-of-range pages return empty items. This replaces the previous unpaginated arrays.

Account/category filters: `q` (literal case-insensitive name substring), `type`; sorts `name_asc` (default), `name_desc`, `created_asc`, `created_desc`.

Transaction filters: `q` (description), `type`, `accountId` (source or destination), `categoryId`, `from`, `to` (inclusive bounds). Sorts: `date_desc` (default), `date_asc`, `amount_desc`, `amount_asc`. Filters combine with AND; sorts include unique tie-breakers. Invalid parameters return400; missing spaces and cross-space reference filters return404.

Account items retain all account fields and add `balance`. Transaction items retain all transaction fields and add nullable `sourceAccountName`, `destinationAccountName`, `categoryName`. Category items are unchanged.

CRUD item endpoints retain their existing contracts. Transaction PATCH replaces the complete details. Income has destination account and income category; expense has source account and expense category; transfer has different source/destination accounts and no category. Inapplicable fields must be null.

Inputs: trimmed names1–255 Unicode code points; description at most1000; money fits DECIMAL(19,2) without rounding; transaction amount positive. Account types and category types are immutable. Accounts/categories with transactions cannot be deleted; tracking dates cannot move past existing transactions. Conflicts return409 with `{code,message}`.

## Reports

GET `/reports/summary?from&to` requires valid inclusive dates and returns `{from,to,income,expense,netCashFlow,transactionCount,ledgerBalance,accountCount,categoryTotals}`. Category totals: `{categoryId,name,type,amount,transactionCount}`.

GET `/reports/monthly?from&to` returns `{from,to,items:[{month,income,expense,netCashFlow}]}`. Month is YYYY-MM. Empty months are filled; boundary months respect the exact date bounds.

Cash flow excludes transfers and opening balances. Transaction count includes transfers. Ledger balance sums opening balances and all recorded ledger movements, including future dates. Each transfer cancels across the space's combined balance.

## CSV

GET `/transactions/export` accepts transaction filters/sort without paging. It streams every matching row as a UTF-8 attachment with BOM, CRLF, RFC-style quoting, and spreadsheet formula protection for user text. Empty output contains the header.

Columns: id,spaceId,transactionDate,type,amount,sourceAccountId,sourceAccountName,destinationAccountId,destinationAccountName,categoryId,categoryName,description,createdAt,updatedAt.
