import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowRight, Plus } from 'lucide-react';
import { useListParams } from '../../../shared/hooks/use-list-params.js';
import { Button } from '../../../shared/ui/button';
import {
  EmptyState,
  Pagination,
  QueryState,
} from '../../../shared/ui/controls';
import { listSpaces } from '../api/list-spaces';
import { SpaceNameDialog } from '../../manage-space/ui/SpaceNameDialog';

export function SpaceSelector({ onSelect }) {
  const client = useQueryClient();
  const [params, update] = useListParams('created_asc');
  const [creating, setCreating] = useState(false);
  const query = useQuery({
    queryKey: ['spaces', 'list', params.page],
    queryFn: ({ signal }) => listSpaces(params.page, signal),
  });
  return (
    <div className="space-y-4">
      <Button onClick={() => setCreating(true)}>
        <Plus className="size-4" />
        Add space
      </Button>
      <QueryState query={query}>
        {query.data && (
          <>
            {query.data.items.length ? (
              <ul
                aria-label="Available spaces"
                className="divide-y overflow-hidden rounded-lg border"
              >
                {query.data.items.map((space) => (
                  <li key={space.id}>
                    <button
                      type="button"
                      onClick={() => onSelect(space)}
                      className="flex w-full items-center justify-between gap-4 px-4 py-4 text-left text-sm font-semibold transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"
                    >
                      <span className="break-all">{space.name}</span>
                      <ArrowRight
                        aria-hidden="true"
                        className="size-4 shrink-0 text-muted-foreground"
                      />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState title="No spaces to show">
                {query.data.total === 0
                  ? 'Add your first space to get started.'
                  : 'There are no spaces on this page.'}
              </EmptyState>
            )}
            <Pagination
              data={query.data}
              label="Spaces"
              onChange={(page) => update({ page })}
            />
          </>
        )}
      </QueryState>
      {creating && (
        <SpaceNameDialog
          onClose={() => setCreating(false)}
          onSaved={(space) => {
            setCreating(false);
            void client.invalidateQueries({ queryKey: ['spaces'] });
            onSelect(space);
          }}
        />
      )}
    </div>
  );
}
