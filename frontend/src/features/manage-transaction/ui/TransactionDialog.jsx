import { useState } from 'react';
import {
  useLedgerItem,
  useLedgerMutation,
} from '../../../entities/ledger/api.js';
import { transactionPayload } from '../model/transaction-form.js';
import { localToday } from '../../../shared/lib/dates.js';
import { FormDialog } from '../../../shared/ui/form-dialog';
import { Field, Input, Select } from '../../../shared/ui/controls';
import { Button } from '../../../shared/ui/button';
import { EntityPicker } from '../../../shared/ui/entity-picker';
import { AccountDialog } from '../../manage-account/ui/AccountDialog';
import { CategoryDialog } from '../../manage-category/ui/CategoryDialog';

export function TransactionDialog({
  spaceId,
  transaction,
  accountId,
  onClose,
}) {
  const [form, setForm] = useState({
    transactionDate: transaction?.transactionDate || localToday(),
    type: transaction?.type || 'EXPENSE',
    amount: transaction?.amount || '',
    sourceAccountId: transaction?.sourceAccountId || accountId || '',
    destinationAccountId: transaction?.destinationAccountId || '',
    categoryId: transaction?.categoryId || '',
    description: transaction?.description || '',
  });
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(null);
  const source = useLedgerItem(spaceId, 'accounts', form.sourceAccountId);
  const destination = useLedgerItem(
    spaceId,
    'accounts',
    form.destinationAccountId,
  );
  const mutation = useLedgerMutation(spaceId, 'transactions');
  const change = (key, value) => {
    setForm((previous) => ({ ...previous, [key]: value }));
    setError('');
  };
  async function submit(event) {
    event.preventDefault();
    if (mutation.isPending) return;
    try {
      const data = transactionPayload(form, {
        source: source.data,
        destination: destination.data,
      });
      await mutation.mutateAsync({ id: transaction?.id, data });
      onClose();
    } catch (e) {
      setError(
        e.message || 'Could not save this transaction. Please try again.',
      );
    }
  }
  return (
    <>
      <FormDialog
        title={transaction ? 'Edit transaction' : 'Add transaction'}
        description="Record income, expenses, or a transfer between accounts."
        onClose={onClose}
        onSubmit={submit}
        busy={mutation.isPending}
        error={error}
        submitLabel={transaction ? 'Save transaction' : 'Add transaction'}
      >
        <Field label="Transaction type">
          {(p) => (
            <Select
              {...p}
              value={form.type}
              onChange={(e) => {
                setForm({
                  ...form,
                  type: e.target.value,
                  sourceAccountId: '',
                  destinationAccountId: '',
                  categoryId: '',
                });
                setError('');
              }}
            >
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
              <option value="TRANSFER">Transfer</option>
            </Select>
          )}
        </Field>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Date">
            {(p) => (
              <Input
                {...p}
                type="date"
                required
                value={form.transactionDate}
                onChange={(e) => change('transactionDate', e.target.value)}
              />
            )}
          </Field>
          <Field label="Amount">
            {(p) => (
              <Input
                {...p}
                autoFocus
                required
                inputMode="decimal"
                placeholder="0.00"
                value={form.amount}
                onChange={(e) => change('amount', e.target.value)}
              />
            )}
          </Field>
        </div>
        {form.type !== 'INCOME' && (
          <Field label="Source account">
            {(p) => (
              <EntityPicker
                {...p}
                spaceId={spaceId}
                resource="accounts"
                label="Source account"
                required
                value={form.sourceAccountId}
                onChange={(v) => change('sourceAccountId', v)}
              />
            )}
          </Field>
        )}
        {form.type !== 'EXPENSE' && (
          <Field label="Destination account">
            {(p) => (
              <EntityPicker
                {...p}
                spaceId={spaceId}
                resource="accounts"
                label="Destination account"
                required
                value={form.destinationAccountId}
                onChange={(v) => change('destinationAccountId', v)}
              />
            )}
          </Field>
        )}
        <Button
          type="button"
          variant="ghost"
          onClick={() => setCreating('account')}
        >
          Add an account
        </Button>
        {form.type !== 'TRANSFER' && (
          <>
            <Field label="Category">
              {(p) => (
                <EntityPicker
                  {...p}
                  spaceId={spaceId}
                  resource="categories"
                  label="Category"
                  type={form.type}
                  required
                  value={form.categoryId}
                  onChange={(v) => change('categoryId', v)}
                />
              )}
            </Field>
            <Button
              type="button"
              variant="ghost"
              onClick={() => setCreating('category')}
            >
              Add a category
            </Button>
          </>
        )}
        <Field label="Description" help="Optional, up to 1,000 characters.">
          {(p) => (
            <textarea
              {...p}
              className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              value={form.description}
              onChange={(e) => change('description', e.target.value)}
            />
          )}
        </Field>
      </FormDialog>
      {creating === 'account' && (
        <AccountDialog
          spaceId={spaceId}
          onClose={() => setCreating(null)}
          onSaved={(a) =>
            change(
              form.type === 'INCOME' ||
                (form.type === 'TRANSFER' && form.sourceAccountId)
                ? 'destinationAccountId'
                : 'sourceAccountId',
              a.id,
            )
          }
        />
      )}
      {creating === 'category' && (
        <CategoryDialog
          spaceId={spaceId}
          defaultType={form.type}
          fixedType={form.type}
          onClose={() => setCreating(null)}
          onSaved={(c) => change('categoryId', c.id)}
        />
      )}
    </>
  );
}
