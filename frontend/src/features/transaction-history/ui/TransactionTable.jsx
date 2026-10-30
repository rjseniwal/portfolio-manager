import { Link } from 'react-router-dom';
import { Badge, Cell, Table } from '../../../shared/ui/controls';
import { Button } from '../../../shared/ui/button';
import { formatMoney } from '../../../shared/lib/money.js';

export function TransactionTable({ spaceId, items, onEdit, onDelete }) {
  return (
    <Table
      caption="Transactions"
      headings={[
        'Date',
        'Type',
        'Amount',
        'Accounts',
        'Category',
        'Description',
        'Actions',
      ]}
    >
      {items.map((record) => (
        <tr key={record.id}>
          <Cell className="whitespace-nowrap">
            <Link
              className="font-medium underline-offset-4 hover:underline"
              to={`/spaces/${spaceId}/transactions/${record.id}`}
            >
              {record.transactionDate}
            </Link>
          </Cell>
          <Cell>
            <Badge>{record.type}</Badge>
          </Cell>
          <Cell className="whitespace-nowrap font-medium tabular-nums">
            {formatMoney(record.amount)}
          </Cell>
          <Cell>
            <div className="min-w-28 space-y-1">
              {record.sourceAccountId && (
                <div>
                  <span className="text-xs text-muted-foreground">From </span>
                  <Link
                    className="hover:underline"
                    to={`/spaces/${spaceId}/accounts/${record.sourceAccountId}`}
                  >
                    {record.sourceAccountName || 'Source account'}
                  </Link>
                </div>
              )}
              {record.destinationAccountId && (
                <div>
                  <span className="text-xs text-muted-foreground">To </span>
                  <Link
                    className="hover:underline"
                    to={`/spaces/${spaceId}/accounts/${record.destinationAccountId}`}
                  >
                    {record.destinationAccountName || 'Destination account'}
                  </Link>
                </div>
              )}
            </div>
          </Cell>
          <Cell>{record.categoryName || '—'}</Cell>
          <Cell className="min-w-32 max-w-64 whitespace-pre-wrap break-words">
            {record.description || '—'}
          </Cell>
          <Cell>
            <div className="flex gap-1">
              <Button variant="ghost" onClick={() => onEdit(record)}>
                Edit
              </Button>
              <Button
                variant="ghost"
                className="text-destructive"
                onClick={() => onDelete(record)}
              >
                Delete
              </Button>
            </div>
          </Cell>
        </tr>
      ))}
    </Table>
  );
}
