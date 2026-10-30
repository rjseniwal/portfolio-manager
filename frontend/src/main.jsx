import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './app/App';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (count, error) =>
        count < 1 && (!error.status || error.status >= 500),
    },
    mutations: { retry: false },
  },
});
import './index.css';
import { applyTheme, getInitialTheme } from './shared/theme/theme';

applyTheme(getInitialTheme());

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>,
);
