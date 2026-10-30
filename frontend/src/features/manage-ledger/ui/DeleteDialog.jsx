import { useState } from 'react';
import { useLedgerMutation } from '../../../entities/ledger/api.js';
import { FormDialog } from '../../../shared/ui/form-dialog';

export function DeleteDialog({
  spaceId,
  resource,
  record,
  onClose,
  onDeleted,
}) {
  const [error, setError] = useState('');
  const mutation = useLedgerMutation(spaceId, resource);
  async function submit(event) {
    event.preventDefault();
    if (mutation.isPending) return;
    try {
      await mutation.mutateAsync({ id: record.id, remove: true });
      onDeleted?.();
      onClose();
    } catch (e) {
      setError(e.message || 'Could not delete this record. Please try again.');
    }
  }
  return (
    <FormDialog
      title={`Delete ${resource === 'categories' ? 'category' : resource.slice(0, -1)}?`}
      description={`Delete ${record.name || 'this transaction'}? This action cannot be undone.`}
      busy={mutation.isPending}
      error={error}
      onClose={onClose}
      onSubmit={submit}
      submitLabel="Delete"
    >
      <p className="text-sm text-muted-foreground">
        {resource === 'transactions'
          ? 'Deleting this transaction will update the affected balances and reports.'
          : 'Records with transactions cannot be deleted. Remove their transactions first if you want to delete this record.'}
      </p>
    </FormDialog>
  );
}
