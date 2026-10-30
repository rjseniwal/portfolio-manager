import { useState } from 'react';
import {
  BrowserRouter,
  Link,
  Route,
  Routes,
  useNavigate,
} from 'react-router-dom';
import { Moon, Sun } from 'lucide-react';
import { Button } from '../shared/ui/button';
import { applyTheme, getInitialTheme, saveTheme } from '../shared/theme/theme';
import { HomePage } from '../pages/home/HomePage';
import { SpacePage } from '../pages/space/SpacePage';
import { OverviewPage } from '../pages/overview/OverviewPage';
import { CatalogPage } from '../pages/catalog/CatalogPage';
import { AccountPage } from '../pages/account/AccountPage';
import { TransactionsPage } from '../pages/transactions/TransactionsPage';
import { TransactionPage } from '../pages/transactions/TransactionPage';
import { ReportsPage } from '../pages/reports/ReportsPage';

function HomeRoute() {
  const navigate = useNavigate();
  return (
    <HomePage onSelectSpace={(space) => navigate(`/spaces/${space.id}`)} />
  );
}
function NotFound() {
  return (
    <main id="main" tabIndex={-1} className="mx-auto max-w-6xl px-6 py-12">
      <h1 className="text-3xl font-bold">Page not found</h1>
      <Link className="mt-4 inline-block text-primary underline" to="/">
        Return to your spaces
      </Link>
    </main>
  );
}
function Shell() {
  const [theme, setTheme] = useState(getInitialTheme);
  function toggleTheme() {
    const next = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    saveTheme(next);
    setTheme(next);
  }
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:rounded focus:bg-card focus:p-3"
      >
        Skip to content
      </a>
      <header className="border-b bg-card">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <Link to="/" className="font-bold tracking-tight">
            Portfolio Manager
          </Link>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={toggleTheme}
            aria-label={
              theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'
            }
          >
            {theme === 'dark' ? (
              <Sun aria-hidden="true" className="size-5" />
            ) : (
              <Moon aria-hidden="true" className="size-5" />
            )}
          </Button>
        </div>
      </header>
      <Routes>
        <Route path="/" element={<HomeRoute />} />
        <Route path="/spaces/:spaceId" element={<SpacePage />}>
          <Route index element={<OverviewPage />} />
          <Route
            path="accounts"
            element={<CatalogPage key="accounts" resource="accounts" />}
          />
          <Route path="accounts/:accountId" element={<AccountPage />} />
          <Route
            path="categories"
            element={<CatalogPage key="categories" resource="categories" />}
          />
          <Route path="transactions" element={<TransactionsPage />} />
          <Route
            path="transactions/:transactionId"
            element={<TransactionPage />}
          />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}
export function App() {
  return (
    <BrowserRouter>
      <Shell />
    </BrowserRouter>
  );
}
