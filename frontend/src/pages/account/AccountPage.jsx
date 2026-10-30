import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useBalance, useLedgerItem } from '../../entities/ledger/api.js';
import { formatMoney } from '../../shared/lib/money.js';
import { Button } from '../../shared/ui/button';
import { Badge, PageTitle, Panel, QueryState } from '../../shared/ui/controls';
import { AccountDialog } from '../../features/manage-account/ui/AccountDialog';
import { DeleteDialog } from '../../features/manage-ledger/ui/DeleteDialog';
import { TransactionHistory } from '../../features/transaction-history/ui/TransactionHistory';

export function AccountPage() {
  const { spaceId, accountId } = useParams();
  const navigate = useNavigate();
  const query = useLedgerItem(spaceId, 'accounts', accountId);
  const balance = useBalance(spaceId, accountId);
  const [editing, setEditing] = useState(false);
  const [removing, setRemoving] = useState(false);
  return (
    <>
      <Link
        className="mb-5 inline-block text-sm text-muted-foreground hover:underline"
        to=".."
        relative="path"
      >
        ← Accounts
      </Link>
      <QueryState query={query}>
        {query.data && (
          <>
            <PageTitle
              title={query.data.name}
              description="Account details and activity."
              action={
                <div className="flex gap-2">
                  <Button variant="outline" onClick={() => setEditing(true)}>
                    Edit account
                  </Button>
                  <Button
                    variant="outline"
                    className="text-destructive"
                    onClick={() => setRemoving(true)}
                  >
                    Delete account
                  </Button>
                </div>
              }
            />
            <Panel className="mb-8">
              <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                <div>
                  <dt className="text-sm text-muted-foreground">Type</dt>
                  <dd className="mt-2">
                    <Badge>{query.data.type}</Badge>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-muted-foreground">
                    Tracking start
                  </dt>
                  <dd className="mt-2 font-medium">
                    {query.data.trackingStartDate}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-muted-foreground">
                    Starting balance
                  </dt>
                  <dd className="mt-2 text-xl font-semibold tabular-nums">
                    {formatMoney(query.data.startingBalance)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-muted-foreground">
                    Ledger balance
                  </dt>
                  <dd className="mt-2 text-xl font-semibold tabular-nums">
                    <QueryState query={balance}>
                      {balance.data && formatMoney(balance.data.balance)}
                    </QueryState>
                  </dd>
                </div>
              </dl>
              <p className="mt-5 text-xs text-muted-foreground">
                Ledger balance includes all recorded transactions, including
                future-dated entries.
              </p>
            </Panel>
            <h3 className="mb-4 text-lg font-semibold">Account activity</h3>
            <TransactionHistory spaceId={spaceId} accountId={accountId} />
            {editing && (
              <AccountDialog
                spaceId={spaceId}
                account={query.data}
                onClose={() => setEditing(false)}
              />
            )}
            {removing && (
              <DeleteDialog
                spaceId={spaceId}
                resource="accounts"
                record={query.data}
                onClose={() => setRemoving(false)}
                onDeleted={() => navigate(`/spaces/${spaceId}/accounts`)}
              />
            )}
          </>
        )}
      </QueryState>
    </>
  );
}
