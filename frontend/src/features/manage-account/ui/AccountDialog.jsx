import { useState } from 'react';
import { useLedgerMutation } from '../../../entities/ledger/api.js';
import { Field, Input, Select } from '../../../shared/ui/controls';
import { FormDialog } from '../../../shared/ui/form-dialog';
import { normalizeMoney } from '../../../shared/lib/money.js';
import { localToday, validDate } from '../../../shared/lib/dates.js';

export function AccountDialog({ spaceId, account, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: account?.name || '',
    type: account?.type || 'BANK',
    startingBalance: account?.startingBalance || '0.00',
    trackingStartDate: account?.trackingStartDate || localToday(),
  });
  const [error, setError] = useState('');
  const mutation = useLedgerMutation(spaceId, 'accounts');
  const change = (key) => (e) => {
    setForm({ ...form, [key]: e.target.value });
    setError('');
  };
  async function submit(event) {
    event.preventDefault();
    if (mutation.isPending) return;
    try {
      if (!form.name.trim() || Array.from(form.name.trim()).length > 255)
        throw new Error('Enter an account name of 1–255 characters.');
      if (!validDate(form.trackingStartDate))
        throw new Error('Enter a valid tracking start date.');
      const data = {
        name: form.name.trim(),
        startingBalance: normalizeMoney(form.startingBalance),
        trackingStartDate: form.trackingStartDate,
        ...(account ? {} : { type: form.type }),
      };
      const saved = await mutation.mutateAsync({ id: account?.id, data });
      onSaved?.(saved);
      onClose();
    } catch (e) {
      setError(e.message || 'Could not save the account. Please try again.');
    }
  }
  return (
    <FormDialog
      title={account ? 'Edit account' : 'Add account'}
      description="Track a bank account or cash balance in this space."
      onClose={onClose}
      busy={mutation.isPending}
      error={error}
      onSubmit={submit}
      submitLabel={account ? 'Save account' : 'Add account'}
    >
      <Field label="Account name">
        {(p) => (
          <Input
            {...p}
            autoFocus
            required
            value={form.name}
            onChange={change('name')}
          />
        )}
      </Field>
      <Field
        label="Account type"
        help={
          account ? 'Account type cannot be changed after creation.' : undefined
        }
      >
        {(p) => (
          <Select
            {...p}
            value={form.type}
            disabled={Boolean(account)}
            onChange={change('type')}
          >
            <option value="BANK">Bank</option>
            <option value="CASH">Cash</option>
          </Select>
        )}
      </Field>
      <Field
        label="Starting balance"
        help="Changing this amount also changes the ledger balance. Negative balances are allowed."
      >
        {(p) => (
          <Input
            {...p}
            required
            inputMode="decimal"
            value={form.startingBalance}
            onChange={change('startingBalance')}
          />
        )}
      </Field>
      <Field
        label="Tracking start date"
        help="Transactions cannot be dated before this day."
      >
        {(p) => (
          <Input
            {...p}
            required
            type="date"
            value={form.trackingStartDate}
            onChange={change('trackingStartDate')}
          />
        )}
      </Field>
    </FormDialog>
  );
}
