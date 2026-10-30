import { useCallback, useEffect, useState } from 'react';
import { Download, Plus } from 'lucide-react';
import { useLedgerList, resourcePath } from '../../../entities/ledger/api.js';
import { API_BASE_URL } from '../../../shared/config/env.js';
import {
  useListParams,
  useSearchDraft,
} from '../../../shared/hooks/use-list-params.js';
import { toSearchParams } from '../../../shared/lib/filters.js';
import { Button } from '../../../shared/ui/button';
import { EntityPicker } from '../../../shared/ui/entity-picker';
import {
  EmptyState,
  Field,
  Input,
  Pagination,
  Panel,
  QueryState,
  Select,
} from '../../../shared/ui/controls';
import { TransactionDialog } from '../../manage-transaction/ui/TransactionDialog';
import { DeleteDialog } from '../../manage-ledger/ui/DeleteDialog';
import { TransactionTable } from './TransactionTable';

export function TransactionHistory({ spaceId, accountId }) {
  const [params, update] = useListParams();
  const effective = { ...params, ...(accountId ? { accountId } : {}) };
  const query = useLedgerList(spaceId, 'transactions', effective);
  const [edit, setEdit] = useState(null);
  const [remove, setRemove] = useState(null);
  const searchChange = useCallback(
    (value) => update({ q: value }, true),
    [update],
  );
  const [search, setSearch] = useSearchDraft(params.q, searchChange);
  const { page, size, ...exportParams } = effective;
  const filtered = [
    'q',
    'type',
    'categoryId',
    'from',
    'to',
    ...(!accountId ? ['accountId'] : []),
  ].some((key) => Boolean(params[key]));
  useEffect(() => {
    if (query.data && page > 1 && !query.data.items.length)
      update({ page: Math.max(1, Math.ceil(query.data.total / size)) }, true);
  }, [query.data, page, size, update]);
  return (
    <>
      <div className="mb-5 flex flex-wrap justify-end gap-2">
        <Button asChild variant="outline">
          <a
            href={`${API_BASE_URL}${resourcePath(spaceId, 'transactions')}/export?${toSearchParams(exportParams)}`}
          >
            <Download className="size-4" />
            Export matching transactions
          </a>
        </Button>
        <Button onClick={() => setEdit({})}>
          <Plus className="size-4" />
          Add transaction
        </Button>
      </div>
      <Panel>
        <div className="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Search descriptions">
            {(p) => (
              <Input
                {...p}
                value={search}
                placeholder="Search descriptions…"
                onChange={(e) => setSearch(e.target.value)}
              />
            )}
          </Field>
          <Field label="Transaction type">
            {(p) => (
              <Select
                {...p}
                value={params.type || ''}
                onChange={(e) => update({ type: e.target.value })}
              >
                <option value="">All types</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
                <option value="TRANSFER">Transfer</option>
              </Select>
            )}
          </Field>
          <Field label="From date">
            {(p) => (
              <Input
                {...p}
                type="date"
                value={params.from || ''}
                onChange={(e) => update({ from: e.target.value })}
              />
            )}
          </Field>
          <Field label="To date">
            {(p) => (
              <Input
                {...p}
                type="date"
                value={params.to || ''}
                onChange={(e) => update({ to: e.target.value })}
              />
            )}
          </Field>
          {!accountId && (
            <Field label="Account">
              {(p) => (
                <EntityPicker
                  {...p}
                  label="Account"
                  spaceId={spaceId}
                  resource="accounts"
                  allLabel="All accounts"
                  value={params.accountId}
                  onChange={(v) => update({ accountId: v })}
                />
              )}
            </Field>
          )}
          <Field label="Category">
            {(p) => (
              <EntityPicker
                {...p}
                label="Category"
                spaceId={spaceId}
                resource="categories"
                allLabel="All categories"
                value={params.categoryId}
                onChange={(v) => update({ categoryId: v })}
              />
            )}
          </Field>
          <Field label="Sort transactions">
            {(p) => (
              <Select
                {...p}
                value={params.sort}
                onChange={(e) => update({ sort: e.target.value })}
              >
                <option value="date_desc">Newest date</option>
                <option value="date_asc">Oldest date</option>
                <option value="amount_desc">Highest amount</option>
                <option value="amount_asc">Lowest amount</option>
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
        {filtered && (
          <Button
            className="mb-4"
            variant="ghost"
            onClick={() =>
              update({
                q: '',
                type: '',
                accountId: '',
                categoryId: '',
                from: '',
                to: '',
              })
            }
          >
            Clear filters
          </Button>
        )}
        <QueryState query={query}>
          {query.data && (
            <>
              {query.data.items.length ? (
                <TransactionTable
                  spaceId={spaceId}
                  items={query.data.items}
                  onEdit={setEdit}
                  onDelete={setRemove}
                />
              ) : (
                <EmptyState
                  title={
                    filtered
                      ? 'No matching transactions'
                      : 'No transactions yet'
                  }
                >
                  {filtered
                    ? 'Try different filters to find your transactions.'
                    : 'Record income, expenses, and transfers to start your ledger.'}
                </EmptyState>
              )}
              <Pagination
                data={query.data}
                label="Transactions"
                onChange={(value) => update({ page: value })}
              />
            </>
          )}
        </QueryState>
      </Panel>
      {edit && (
        <TransactionDialog
          spaceId={spaceId}
          accountId={accountId}
          transaction={edit.id ? edit : undefined}
          onClose={() => setEdit(null)}
        />
      )}
      {remove && (
        <DeleteDialog
          spaceId={spaceId}
          resource="transactions"
          record={remove}
          onClose={() => setRemove(null)}
        />
      )}
    </>
  );
}
