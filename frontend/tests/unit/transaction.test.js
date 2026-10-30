import test from 'node:test';
import assert from 'node:assert/strict';
import { transactionPayload } from '../../src/features/manage-transaction/model/transaction-form.js';
const form = {
  type: 'EXPENSE',
  transactionDate: '2026-01-05',
  amount: '5',
  sourceAccountId: 'bank',
  destinationAccountId: '',
  categoryId: 'food',
  description: 'Lunch',
};
test('transaction edits send the complete applicable payload', () => {
  assert.deepEqual(transactionPayload(form), {
    ...form,
    amount: '5.00',
    destinationAccountId: null,
  });
});
test('transfers require two different accounts and clear category', () => {
  assert.throws(
    () =>
      transactionPayload({
        ...form,
        type: 'TRANSFER',
        destinationAccountId: 'bank',
      }),
    /different/,
  );
  const result = transactionPayload({
    ...form,
    type: 'TRANSFER',
    destinationAccountId: 'cash',
  });
  assert.equal(result.categoryId, null);
});
test('transaction validates positive exact amounts, tracking date, and Unicode descriptions', () => {
  assert.throws(() => transactionPayload({ ...form, amount: '0' }), /positive/);
  assert.throws(
    () =>
      transactionPayload(form, { source: { trackingStartDate: '2026-02-01' } }),
    /tracking/,
  );
  assert.equal(
    transactionPayload({ ...form, description: '😀'.repeat(1000) }).description
      .length,
    2000,
  );
  assert.throws(
    () => transactionPayload({ ...form, description: '😀'.repeat(1001) }),
    /1,000/,
  );
});
