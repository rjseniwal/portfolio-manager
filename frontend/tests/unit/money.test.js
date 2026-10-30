import assert from 'node:assert/strict';
import test from 'node:test';
import {
  formatMoney,
  normalizeMoney,
  parseMoney,
} from '../../src/shared/lib/money.js';

test('parseMoney preserves cents beyond floating-point precision', () => {
  assert.equal(parseMoney('90071992547409.93'), 9007199254740993n);
  assert.equal(parseMoney('99999999999999999.99'), 9999999999999999999n);
  assert.equal(parseMoney('-99999999999999999.99'), -9999999999999999999n);
});

test('parseMoney handles whole amounts, subunits, negatives, and zero', () => {
  for (const [value, expected] of [
    ['12', 1200n],
    ['12.3', 1230n],
    ['12.34', 1234n],
    ['-0.01', -1n],
    ['-0.50', -50n],
    ['0', 0n],
    ['-0.00', 0n],
  ])
    assert.equal(parseMoney(value), expected, value);
});

test('parseMoney rejects malformed, nondecimal, and overprecision amounts', () => {
  for (const value of [
    '',
    ' ',
    ' 1.00',
    '1.00 ',
    '1.00\n',
    '1e2',
    '1E+2',
    '1.001',
    '1.',
    '.50',
    '+1.00',
    '1,000.00',
    '$1.00',
    'NaN',
    'Infinity',
    '--1',
    '१२.३४',
  ])
    assert.throws(() => parseMoney(value), undefined, value);
});

test('parseMoney enforces the DECIMAL(19,2) limit on both signs', () => {
  assert.throws(() => parseMoney('100000000000000000.00'), RangeError);
  assert.throws(() => parseMoney('-100000000000000000.00'), RangeError);
});

test('parseMoney requires a string so precision cannot be lost before parsing', () => {
  for (const value of [1.23, 123n, null, undefined]) {
    assert.throws(() => parseMoney(value), TypeError);
  }
});

test('normalizeMoney emits canonical fixed two-decimal strings', () => {
  assert.equal(normalizeMoney('0012.3'), '12.30');
  assert.equal(normalizeMoney('-0.01'), '-0.01');
  assert.equal(normalizeMoney('-0.00'), '0.00');
  assert.equal(normalizeMoney('0'), '0.00');
  assert.throws(() => normalizeMoney('1.234'));
  assert.throws(() => normalizeMoney('100000000000000000.00'), RangeError);
});

test('formatMoney groups exact decimal strings and bigint cents', () => {
  assert.equal(formatMoney('1234567.8'), '1,234,567.80');
  assert.equal(formatMoney(123456789n), '1,234,567.89');
  assert.equal(formatMoney('-0.01'), '-0.01');
  assert.equal(formatMoney(-1n), '-0.01');
  assert.equal(formatMoney(0n), '0.00');
  assert.equal(formatMoney('-0.00'), '0.00');
});

test('formatMoney accepts report aggregates beyond the input amount limit', () => {
  assert.equal(
    formatMoney('100000000000000000.01'),
    '100,000,000,000,000,000.01',
  );
  assert.equal(
    formatMoney(1234567890123456789012345678912n),
    '12,345,678,901,234,567,890,123,456,789.12',
  );
});

test('formatMoney rejects exponent strings and numeric inputs', () => {
  assert.throws(() => formatMoney('1e6'));
  assert.throws(() => formatMoney('1.001'));
  assert.throws(() => formatMoney(1.23), TypeError);
});
