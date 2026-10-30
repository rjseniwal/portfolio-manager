import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useLedgerItem } from '../../entities/ledger/api.js';
import { formatMoney } from '../../shared/lib/money.js';
import { Button } from '../../shared/ui/button';
import { Badge, PageTitle, Panel, QueryState } from '../../shared/ui/controls';
import { TransactionDialog } from '../../features/manage-transaction/ui/TransactionDialog';
import { DeleteDialog } from '../../features/manage-ledger/ui/DeleteDialog';

function RelatedRecord({ spaceId, resource, id }) {
  const record = useLedgerItem(spaceId, resource, id);
  if (!id) return '—';
  if (record.isError)
    return (
      <span className="text-destructive">
        Could not load selection{' '}
        <button className="underline" onClick={() => record.refetch()}>
          Retry
        </button>
      </span>
    );
  const name = record.data?.name || 'Loading…';
  return resource === 'accounts' ? (
    <Link className="underline" to={`/spaces/${spaceId}/accounts/${id}`}>
      {name}
    </Link>
  ) : (
    name
  );
}
export function TransactionPage() {
  const { spaceId, transactionId } = useParams();
  const navigate = useNavigate();
  const query = useLedgerItem(spaceId, 'transactions', transactionId);
  const [editing, setEditing] = useState(false);
  const [removing, setRemoving] = useState(false);
  const record = query.data;
  return (
    <>
      <Link
        className="mb-5 inline-block text-sm text-muted-foreground hover:underline"
        to={`/spaces/${spaceId}/transactions`}
      >
        ← Transactions
      </Link>
      <QueryState query={query}>
        {record && (
          <>
            <PageTitle
              title="Transaction details"
              description={record.transactionDate}
              action={
                <div className="flex gap-2">
                  <Button variant="outline" onClick={() => setEditing(true)}>
                    Edit transaction
                  </Button>
                  <Button
                    variant="outline"
                    className="text-destructive"
                    onClick={() => setRemoving(true)}
                  >
                    Delete transaction
                  </Button>
                </div>
              }
            />
            <Panel>
              <div className="mb-6 flex items-center justify-between">
                <Badge>{record.type}</Badge>
                <span className="text-3xl font-semibold tabular-nums">
                  {formatMoney(record.amount)}
                </span>
              </div>
              <dl className="grid gap-5 sm:grid-cols-2">
                {[
                  ['Source account', 'accounts', record.sourceAccountId],
                  [
                    'Destination account',
                    'accounts',
                    record.destinationAccountId,
                  ],
                  ['Category', 'categories', record.categoryId],
                ].map(([label, resource, id]) => (
                  <div key={label}>
                    <dt className="text-sm text-muted-foreground">{label}</dt>
                    <dd className="mt-1">
                      <RelatedRecord
                        spaceId={spaceId}
                        resource={resource}
                        id={id}
                      />
                    </dd>
                  </div>
                ))}
                <div>
                  <dt className="text-sm text-muted-foreground">Description</dt>
                  <dd className="mt-1 whitespace-pre-wrap break-words">
                    {record.description || '—'}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-muted-foreground">Created</dt>
                  <dd className="mt-1">
                    {new Date(record.createdAt).toLocaleString()}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-muted-foreground">Updated</dt>
                  <dd className="mt-1">
                    {new Date(record.updatedAt).toLocaleString()}
                  </dd>
                </div>
              </dl>
            </Panel>
            {editing && (
              <TransactionDialog
                spaceId={spaceId}
                transaction={record}
                onClose={() => setEditing(false)}
              />
            )}
            {removing && (
              <DeleteDialog
                spaceId={spaceId}
                resource="transactions"
                record={record}
                onClose={() => setRemoving(false)}
                onDeleted={() => navigate(`/spaces/${spaceId}/transactions`)}
              />
            )}
          </>
        )}
      </QueryState>
    </>
  );
}
