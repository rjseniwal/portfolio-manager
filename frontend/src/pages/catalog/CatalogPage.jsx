import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useLedgerList } from '../../entities/ledger/api.js';
import {
  useListParams,
  useSearchDraft,
} from '../../shared/hooks/use-list-params.js';
import { formatMoney } from '../../shared/lib/money.js';
import { Button } from '../../shared/ui/button';
import {
  Badge,
  Cell,
  EmptyState,
  Field,
  Input,
  PageTitle,
  Pagination,
  Panel,
  QueryState,
  Select,
  Table,
} from '../../shared/ui/controls';
import { AccountDialog } from '../../features/manage-account/ui/AccountDialog';
import { CategoryDialog } from '../../features/manage-category/ui/CategoryDialog';
import { DeleteDialog } from '../../features/manage-ledger/ui/DeleteDialog';

export function CatalogPage({ resource }) {
  const { spaceId } = useParams();
  const accounts = resource === 'accounts';
  const title = accounts ? 'Accounts' : 'Categories';
  const singular = accounts ? 'account' : 'category';
  const [params, update] = useListParams('name_asc');
  const { q, type, page, size, sort } = params;
  const query = useLedgerList(spaceId, resource, { q, type, page, size, sort });
  const searchChange = useCallback(
    (value) => update({ q: value }, true),
    [update],
  );
  const [search, setSearch] = useSearchDraft(q, searchChange);
  const [edit, setEdit] = useState(null);
  const [remove, setRemove] = useState(null);
  useEffect(() => {
    if (query.data && page > 1 && !query.data.items.length)
      update({ page: Math.max(1, Math.ceil(query.data.total / size)) }, true);
  }, [query.data, page, size, update]);
  return (
    <>
      <PageTitle
        title={title}
        description={
          accounts
            ? 'Manage bank and cash accounts and their ledger balances.'
            : 'Organize your income and expenses.'
        }
        action={
          <Button onClick={() => setEdit({})}>
            <Plus className="size-4" />
            Add {singular}
          </Button>
        }
      />
      <Panel>
        <div className="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field label={`Search ${title.toLowerCase()}`}>
            {(p) => (
              <Input
                {...p}
                placeholder="Search by name…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            )}
          </Field>
          <Field label="Type">
            {(p) => (
              <Select
                {...p}
                value={type || ''}
                onChange={(e) => update({ type: e.target.value })}
              >
                <option value="">All types</option>
                {(accounts ? ['BANK', 'CASH'] : ['INCOME', 'EXPENSE']).map(
                  (value) => (
                    <option key={value} value={value}>
                      {value.toLowerCase()}
                    </option>
                  ),
                )}
              </Select>
            )}
          </Field>
          <Field label="Sort">
            {(p) => (
              <Select
                {...p}
                value={sort}
                onChange={(e) => update({ sort: e.target.value })}
              >
                <option value="name_asc">Name A–Z</option>
                <option value="name_desc">Name Z–A</option>
                <option value="created_desc">Newest created</option>
                <option value="created_asc">Oldest created</option>
              </Select>
            )}
          </Field>
          <Field label="Rows per page">
            {(p) => (
              <Select
                {...p}
                value={size}
                onChange={(e) => update({ size: e.target.value })}
              >
                {[20, 50, 100].map((n) => (
                  <option key={n}>{n}</option>
                ))}
              </Select>
            )}
          </Field>
        </div>
        <QueryState query={query}>
          {query.data && (
            <>
              {!query.data.items.length ? (
                <EmptyState
                  title={
                    q || type
                      ? 'No matching results'
                      : `No ${title.toLowerCase()} yet`
                  }
                  action={
                    q || type ? (
                      <Button
                        variant="outline"
                        onClick={() => update({ q: '', type: '' })}
                      >
                        Clear filters
                      </Button>
                    ) : (
                      <Button onClick={() => setEdit({})}>
                        Add {singular}
                      </Button>
                    )
                  }
                >
                  {q || type
                    ? 'Try a different search or clear your filters.'
                    : `Add your first ${singular} to get started.`}
                </EmptyState>
              ) : (
                <Table
                  caption={title}
                  headings={
                    accounts
                      ? [
                          'Name',
                          'Type',
                          'Tracking start',
                          'Ledger balance',
                          'Actions',
                        ]
                      : ['Name', 'Type', 'Actions']
                  }
                >
                  {query.data.items.map((record) => (
                    <tr key={record.id}>
                      <Cell>
                        {accounts ? (
                          <Link
                            className="font-medium underline-offset-4 hover:underline"
                            to={record.id}
                          >
                            {record.name}
                          </Link>
                        ) : (
                          <span className="font-medium">{record.name}</span>
                        )}
                      </Cell>
                      <Cell>
                        <Badge>{record.type}</Badge>
                      </Cell>
                      {accounts && (
                        <>
                          <Cell>{record.trackingStartDate}</Cell>
                          <Cell className="whitespace-nowrap font-medium tabular-nums">
                            {formatMoney(record.balance)}
                          </Cell>
                        </>
                      )}
                      <Cell>
                        <div className="flex flex-wrap gap-1">
                          <Button
                            size="default"
                            variant="ghost"
                            aria-label={`${accounts ? 'Edit' : 'Rename'} ${record.name}`}
                            onClick={() => setEdit(record)}
                          >
                            {accounts ? 'Edit' : 'Rename'}
                          </Button>
                          {!accounts && (
                            <Button asChild variant="ghost">
                              <Link
                                to={`../transactions?categoryId=${record.id}`}
                              >
                                View transactions
                              </Link>
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            className="text-destructive"
                            aria-label={`Delete ${record.name}`}
                            onClick={() => setRemove(record)}
                          >
                            Delete
                          </Button>
                        </div>
                      </Cell>
                    </tr>
                  ))}
                </Table>
              )}
              <Pagination
                data={query.data}
                label={title}
                onChange={(value) => update({ page: value })}
              />
            </>
          )}
        </QueryState>
      </Panel>
      {edit &&
        (accounts ? (
          <AccountDialog
            spaceId={spaceId}
            account={edit.id ? edit : undefined}
            onClose={() => setEdit(null)}
          />
        ) : (
          <CategoryDialog
            spaceId={spaceId}
            category={edit.id ? edit : undefined}
            onClose={() => setEdit(null)}
          />
        ))}
      {remove && (
        <DeleteDialog
          spaceId={spaceId}
          resource={resource}
          record={remove}
          onClose={() => setRemove(null)}
        />
      )}
    </>
  );
}
