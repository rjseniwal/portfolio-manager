import { useParams } from 'react-router-dom';
import { PageTitle } from '../../shared/ui/controls';
import { TransactionHistory } from '../../features/transaction-history/ui/TransactionHistory';

export function TransactionsPage() {
  const { spaceId } = useParams();
  return (
    <>
      <PageTitle
        title="Transactions"
        description="Your income, expenses, and transfers in one place."
      />
      <TransactionHistory spaceId={spaceId} />
    </>
  );
}
