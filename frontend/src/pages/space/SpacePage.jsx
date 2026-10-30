import { useState } from 'react';
import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useSpace } from '../../entities/ledger/api.js';
import { Button } from '../../shared/ui/button';
import { QueryState } from '../../shared/ui/controls';
import { SpaceNameDialog } from '../../features/manage-space/ui/SpaceNameDialog';

export function SpacePage() {
  const { spaceId } = useParams();
  const query = useSpace(spaceId);
  const client = useQueryClient();
  const [renaming, setRenaming] = useState(false);
  return (
    <main
      id="main"
      tabIndex={-1}
      className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10"
    >
      <Link
        to="/"
        className="mb-6 inline-block text-sm text-muted-foreground hover:underline"
      >
        ← All spaces
      </Link>
      <QueryState query={query}>
        {query.data && (
          <>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs font-bold tracking-[0.12em] text-primary">
                  YOUR SPACE
                </p>
                <h1 className="mt-2 break-words text-3xl font-bold tracking-tight sm:text-4xl">
                  {query.data.name}
                </h1>
              </div>
              <Button variant="outline" onClick={() => setRenaming(true)}>
                Rename space
              </Button>
            </div>
            <nav
              aria-label="Space sections"
              className="my-7 flex gap-1 overflow-x-auto border-b pb-2"
            >
              {[
                ['', 'Overview'],
                ['accounts', 'Accounts'],
                ['transactions', 'Transactions'],
                ['categories', 'Categories'],
                ['reports', 'Reports'],
              ].map(([path, label]) => (
                <NavLink
                  key={path}
                  end={!path}
                  to={path || '.'}
                  className={({ isActive }) =>
                    `whitespace-nowrap rounded-md px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'}`
                  }
                >
                  {label}
                </NavLink>
              ))}
            </nav>
            <Outlet />
            {renaming && (
              <SpaceNameDialog
                space={query.data}
                onClose={() => setRenaming(false)}
                onSaved={(space) => {
                  client.setQueryData(['space', spaceId, 'detail'], space);
                  void client.invalidateQueries({ queryKey: ['spaces'] });
                  setRenaming(false);
                }}
              />
            )}
          </>
        )}
      </QueryState>
    </main>
  );
}
