const MAX_INPUT_CENTS = 9999999999999999999n;

function decimalCents(value) {
  if (typeof value !== 'string') {
    throw new TypeError('Money must be a decimal string.');
  }
  const match = /^(-?)(\d+)(?:\.(\d{1,2}))?$/.exec(value);
  if (!match || match[0] !== value) {
    throw new TypeError('Enter an amount with at most two decimal places.');
  }
  const cents =
    BigInt(match[2]) * 100n + BigInt((match[3] ?? '').padEnd(2, '0'));
  return match[1] ? -cents : cents;
}

function decimalString(cents, grouped = false) {
  const negative = cents < 0n;
  const absolute = negative ? -cents : cents;
  let whole = (absolute / 100n).toString();
  if (grouped) whole = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const fraction = (absolute % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${whole}.${fraction}`;
}

/** Parse an input amount into exact cents within the DECIMAL(19,2) limit. */
export function parseMoney(value) {
  const cents = decimalCents(value);
  if (cents > MAX_INPUT_CENTS || cents < -MAX_INPUT_CENTS) {
    throw new RangeError('Amount exceeds the maximum of 99999999999999999.99.');
  }
  return cents;
}

/** Format a decimal amount or bigint cents, including larger report totals. */
export function formatMoney(value) {
  return decimalString(
    typeof value === 'bigint' ? value : decimalCents(value),
    true,
  );
}

/** Return an input amount as a canonical decimal string with two places. */
export function normalizeMoney(value) {
  return decimalString(parseMoney(value));
}
