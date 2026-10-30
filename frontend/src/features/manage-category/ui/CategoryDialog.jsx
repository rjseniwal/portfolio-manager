import { useState } from 'react';
import { useLedgerMutation } from '../../../entities/ledger/api.js';
import { Field, Input, Select } from '../../../shared/ui/controls';
import { FormDialog } from '../../../shared/ui/form-dialog';

export function CategoryDialog({
  spaceId,
  category,
  defaultType = 'EXPENSE',
  fixedType,
  onClose,
  onSaved,
}) {
  const [name, setName] = useState(category?.name || '');
  const [type, setType] = useState(category?.type || fixedType || defaultType);
  const [error, setError] = useState('');
  const mutation = useLedgerMutation(spaceId, 'categories');
  async function submit(event) {
    event.preventDefault();
    if (mutation.isPending) return;
    if (!name.trim() || Array.from(name.trim()).length > 255) {
      setError('Enter a category name of 1–255 characters.');
      return;
    }
    try {
      const saved = await mutation.mutateAsync({
        id: category?.id,
        data: { name: name.trim(), ...(category ? {} : { type }) },
      });
      onSaved?.(saved);
      onClose();
    } catch (e) {
      setError(e.message || 'Could not save the category. Please try again.');
    }
  }
  return (
    <FormDialog
      title={category ? 'Rename category' : 'Add category'}
      description="Organize income and expenses with categories."
      onClose={onClose}
      busy={mutation.isPending}
      error={error}
      onSubmit={submit}
      submitLabel={category ? 'Save category' : 'Add category'}
    >
      <Field label="Category name">
        {(p) => (
          <Input
            {...p}
            autoFocus
            required
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        )}
      </Field>
      <Field
        label="Category type"
        help={
          category
            ? 'Category type cannot be changed after creation.'
            : fixedType
              ? 'Category type matches this transaction.'
              : undefined
        }
      >
        {(p) => (
          <Select
            {...p}
            disabled={Boolean(category || fixedType)}
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </Select>
        )}
      </Field>
    </FormDialog>
  );
}
