# Portfolio Manager frontend

React and Vite workspace for space-scoped bank/cash accounts, income/expense categories, transactions, reports, and CSV export. Uses shadcn-style shared components, Radix dialogs, Tailwind CSS, React Router, and TanStack Query. Light/dark mode follows the system initially and remembers manual selection.

## Run locally

Use Node.js 20.19+ or 22.12+ and npm.

```sh
npm install
cp .env.example .env
npm run dev
```

Start the backend separately (see `../backend/README.md`). The Vite server proxies `/backend/*` to `BACKEND_ORIGIN`, default `http://localhost:9090`, removing the `/backend` prefix. Configure the address in `.env` and restart Vite. `VITE_API_BASE_URL` defaults to `/backend`.

This frontend requires the new **paginated** backend ledger APIs. Account, category, and transaction lists return `{items,page,size,total}`, replacing the former array responses. Apply the backend V3 index migration before deployment. See [API contracts](docs/implementation/ledger-contract.md).

## Features

- Space chooser, creation, and renaming.
- Account creation, editing, deletion, ledger balances, and account-specific activity.
- Income/expense category creation, renaming, deletion, and transaction links.
- Income, expense, and transfer entry with type-specific fields; editing and deletion.
- Searchable paginated account/category pickers, including adding prerequisites while keeping transaction drafts.
- URL-backed transaction filters, sorting, pagination, and full filtered CSV downloads.
- Month-to-date overview, 12-month trends, date-range reports, and accessible chart tables.

Amounts have two decimal places without a currency symbol. API money stays in decimal strings; frontend helpers use exact bigint cents. A ledger balance includes all recorded transactions, including future dates. Cash flow uses transaction dates and excludes transfers and opening balances. Dates are local calendar dates; no currency conversion or historical balance snapshots are implied.

## Verification

```sh
npm test
npx playwright install chromium
npm run test:e2e
npm run build
```

Unit tests cover precision, date boundaries, URL filters, HTTP errors, and transaction payloads. Playwright starts an isolated Vite server on `127.0.0.1:5174`; browser tests exercise workflows using contract-shaped API fixtures. Set `PLAYWRIGHT_BROWSERS_PATH` consistently for installation and execution if using a custom cache directory. Live tests, when enabled with `LEDGER_LIVE_ORIGIN`, use a dedicated test backend and create a new space; never point them at shared production data.

## Production

Build with `npm run build` and serve `dist/`. Configure the web server to serve `index.html` for frontend routes such as `/spaces/ID/accounts`. Preserve `/backend/*` as an API proxy with the prefix removed. Vite's development proxy is not provided by the production build or `npm run preview`.

Alternatively configure `VITE_API_BASE_URL` at build time and configure backend CORS and cross-origin CSV attachment downloads. Environment variables are embedded in browser code; never put secrets in them.

Deploy the backend and frontend together: the collection-envelope change is breaking for old API consumers. Existing create/get/update/delete endpoints are retained.

## Structure

- `app`: query provider, application shell, routes.
- `pages`: routed chooser, space layout, overview, account/category screens, transaction details, and reports.
- `features`: forms, mutations, transaction history, and reporting views.
- `entities/ledger`: shared domain queries, cache keys, and HTTP interfaces.
- `shared`: API/configuration, exact money/date/filter helpers, theme, and reusable UI.

Keep feature behavior out of shared controls. Pages compose features; features consume entity APIs and shared infrastructure. Use existing theme tokens for new UI so both modes remain readable.
