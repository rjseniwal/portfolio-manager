import { formatMoney } from '../../../shared/lib/money.js';
import { Cell, EmptyState, Panel, Table } from '../../../shared/ui/controls';

export function SummaryCards({ data, showBalance = true }) {
  const values = [
    ...(showBalance
      ? [
          [
            'Ledger balance',
            data.ledgerBalance,
            'Includes all dates, including future entries',
          ],
        ]
      : []),
    ['Income', data.income],
    ['Expenses', data.expense],
    ['Net cash flow', data.netCashFlow],
  ];
  return (
    <div
      className={`grid gap-4 sm:grid-cols-2 ${showBalance ? 'xl:grid-cols-4' : 'lg:grid-cols-3'}`}
    >
      {values.map(([label, amount, help]) => (
        <Panel key={label}>
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-3 break-all text-2xl font-semibold tracking-tight tabular-nums">
            {formatMoney(amount)}
          </p>
          {help && (
            <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
              {help}
            </p>
          )}
        </Panel>
      ))}
    </div>
  );
}
// Chart geometry uses only a bounded ratio. Labels always retain exact decimals.
function cents(value) {
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
}
function width(value, max) {
  return max > 0n ? Number((cents(value) * 10000n) / max) / 100 : 0;
}
export function MonthlyChart({ items }) {
  if (!items.length)
    return (
      <EmptyState title="No monthly data">
        Choose a date range to see monthly cash flow.
      </EmptyState>
    );
  const max = items.reduce(
    (m, item) =>
      [cents(item.income), cents(item.expense), m].reduce((a, b) =>
        a > b ? a : b,
      ),
    0n,
  );
  return (
    <>
      <div aria-hidden="true" className="space-y-4">
        {items.map((item) => (
          <div
            key={item.month}
            className="grid grid-cols-[4.5rem_1fr] items-center gap-3"
          >
            <span className="text-xs text-muted-foreground">{item.month}</span>
            <div className="space-y-1.5">
              <div
                title={`Income ${formatMoney(item.income)}`}
                className="h-3 rounded-sm bg-primary"
                style={{ width: `${width(item.income, max)}%` }}
              />
              <div
                title={`Expenses ${formatMoney(item.expense)}`}
                className="h-3 rounded-sm bg-muted-foreground"
                style={{ width: `${width(item.expense, max)}%` }}
              />
            </div>
          </div>
        ))}
      </div>
      <div className="my-4 flex gap-4 text-xs text-muted-foreground">
        <span className="flex items-center gap-2">
          <span className="size-3 rounded-sm bg-primary" />
          Income
        </span>
        <span className="flex items-center gap-2">
          <span className="size-3 rounded-sm bg-muted-foreground" />
          Expenses
        </span>
      </div>
      <details className="mt-4">
        <summary className="cursor-pointer text-sm font-medium underline-offset-4 hover:underline">
          View monthly values
        </summary>
        <div className="mt-3">
          <Table
            caption="Monthly cash flow"
            headings={['Month', 'Income', 'Expenses', 'Net cash flow']}
          >
            {items.map((item) => (
              <tr key={item.month}>
                <Cell>{item.month}</Cell>
                <Cell>{formatMoney(item.income)}</Cell>
                <Cell>{formatMoney(item.expense)}</Cell>
                <Cell>{formatMoney(item.netCashFlow)}</Cell>
              </tr>
            ))}
          </Table>
        </div>
      </details>
    </>
  );
}
export function CategoryBreakdown({ items }) {
  if (!items.length)
    return (
      <EmptyState title="No categorized activity">
        Income and expense categories will appear here when transactions match
        the selected dates.
      </EmptyState>
    );
  return (
    <Table
      caption="Category breakdown"
      headings={['Category', 'Type', 'Transactions', 'Amount']}
    >
      {items.map((item) => (
        <tr key={item.categoryId}>
          <Cell>{item.name}</Cell>
          <Cell>
            <span className="capitalize">{item.type.toLowerCase()}</span>
          </Cell>
          <Cell>{item.transactionCount}</Cell>
          <Cell className="tabular-nums">{formatMoney(item.amount)}</Cell>
        </tr>
      ))}
    </Table>
  );
}
