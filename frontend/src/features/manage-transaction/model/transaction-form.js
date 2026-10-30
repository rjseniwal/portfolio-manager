import { normalizeMoney, parseMoney } from '../../../shared/lib/money.js';
import { validDate } from '../../../shared/lib/dates.js';

export function transactionPayload(form, { source, destination } = {}) {
  if (!['INCOME', 'EXPENSE', 'TRANSFER'].includes(form.type))
    throw new Error('Select a transaction type.');
  if (!validDate(form.transactionDate))
    throw new Error('Enter a valid transaction date.');
  const amount = normalizeMoney(form.amount);
  if (parseMoney(amount) <= 0n) throw new Error('Amount must be positive.');
  const income = form.type === 'INCOME';
  const expense = form.type === 'EXPENSE';
  const sourceAccountId = income ? null : form.sourceAccountId || null;
  const destinationAccountId = expense
    ? null
    : form.destinationAccountId || null;
  const categoryId = form.type === 'TRANSFER' ? null : form.categoryId || null;
  if (
    (!income && !sourceAccountId) ||
    (!expense && !destinationAccountId) ||
    (form.type !== 'TRANSFER' && !categoryId)
  )
    throw new Error('Select the accounts and category for this transaction.');
  if (sourceAccountId && sourceAccountId === destinationAccountId)
    throw new Error('Transfers require two different accounts.');
  for (const account of [
    sourceAccountId && source,
    destinationAccountId && destination,
  ]) {
    if (account && form.transactionDate < account.trackingStartDate)
      throw new Error(
        'Transaction date cannot precede an account’s tracking start date.',
      );
  }
  const description = form.description || '';
  if (Array.from(description).length > 1000)
    throw new Error('Description must be at most 1,000 characters.');
  return {
    transactionDate: form.transactionDate,
    type: form.type,
    amount,
    sourceAccountId,
    destinationAccountId,
    categoryId,
    description: description || null,
  };
}
