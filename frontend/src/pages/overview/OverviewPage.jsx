import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useLedgerList, useReport } from '../../entities/ledger/api.js';
import { monthToDate, trendRange } from '../../shared/lib/dates.js';
import { formatMoney } from '../../shared/lib/money.js';
import { Button } from '../../shared/ui/button';
import {
  Badge,
  EmptyState,
  PageTitle,
  Panel,
  QueryState,
} from '../../shared/ui/controls';
import {
  MonthlyChart,
  SummaryCards,
} from '../../features/reporting/ui/ReportViews';
import { AccountDialog } from '../../features/manage-account/ui/AccountDialog';
import { TransactionDialog } from '../../features/manage-transaction/ui/TransactionDialog';
import { DeleteDialog } from '../../features/manage-ledger/ui/DeleteDialog';
import { TransactionTable } from '../../features/transaction-history/ui/TransactionTable';

export function OverviewPage() {
  const { spaceId } = useParams();
  const range = monthToDate();
  const summary = useReport(spaceId, 'summary', range);
  const monthly = useReport(spaceId, 'monthly', trendRange());
  const recent = useLedgerList(spaceId, 'transactions', {
    page: 1,
    size: 5,
    sort: 'date_desc',
  });
  const accounts = useLedgerList(spaceId, 'accounts', {
    page: 1,
    size: 4,
    sort: 'name_asc',
  });
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [edit, setEdit] = useState(null);
  const [remove, setRemove] = useState(null);
  return (
    <>
      <PageTitle
        title="Overview"
        description={`This month to date · ${range.from} to ${range.to}`}
        action={
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setCreatingAccount(true)}>
              Add account
            </Button>
            <Button onClick={() => setEdit({})}>
              <Plus className="size-4" />
              Add transaction
            </Button>
          </div>
        }
      />
      <QueryState query={summary}>
        {summary.data && (
          <>
            <SummaryCards data={summary.data} />
            <p className="mt-3 text-sm text-muted-foreground">
              {summary.data.accountCount} accounts ·{' '}
              {summary.data.transactionCount} transactions in this period
            </p>
          </>
        )}
      </QueryState>
      <div className="my-8 grid gap-6 lg:grid-cols-2">
        <Panel>
          <h3 className="mb-5 text-lg font-semibold">
            Income and expenses · 12 months
          </h3>
          <QueryState query={monthly}>
            {monthly.data && <MonthlyChart items={monthly.data.items} />}
          </QueryState>
        </Panel>
        <Panel>
          <div className="mb-5 flex items-center justify-between">
            <h3 className="text-lg font-semibold">Your accounts</h3>
            <Link
              to="accounts"
              className="text-sm text-primary hover:underline"
            >
              View all accounts
            </Link>
          </div>
          <QueryState query={accounts}>
            {accounts.data &&
              (accounts.data.items.length ? (
                <ul className="divide-y">
                  {accounts.data.items.map((account) => (
                    <li key={account.id}>
                      <Link
                        to={`accounts/${account.id}`}
                        className="flex items-center justify-between gap-3 py-4"
                      >
                        <div className="min-w-0">
                          <p className="break-words font-medium">
                            {account.name}
                          </p>
                          <Badge>{account.type}</Badge>
                        </div>
                        <span className="whitespace-nowrap font-medium tabular-nums">
                          {formatMoney(account.balance)}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState
                  title="No accounts yet"
                  action={
                    <Button onClick={() => setCreatingAccount(true)}>
                      Add account
                    </Button>
                  }
                >
                  Add a bank or cash account to start tracking.
                </EmptyState>
              ))}
          </QueryState>
        </Panel>
      </div>
      <Panel>
        <div className="mb-5 flex items-center justify-between">
          <h3 className="text-lg font-semibold">Recent transactions</h3>
          <Link
            to="transactions"
            className="text-sm text-primary hover:underline"
          >
            View all transactions
          </Link>
        </div>
        <QueryState query={recent}>
          {recent.data &&
            (recent.data.items.length ? (
              <TransactionTable
                spaceId={spaceId}
                items={recent.data.items}
                onEdit={setEdit}
                onDelete={setRemove}
              />
            ) : (
              <EmptyState title="No transactions yet">
                Add your first transaction to see activity here.
              </EmptyState>
            ))}
        </QueryState>
      </Panel>
      {creatingAccount && (
        <AccountDialog
          spaceId={spaceId}
          onClose={() => setCreatingAccount(false)}
        />
      )}
      {edit && (
        <TransactionDialog
          spaceId={spaceId}
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
