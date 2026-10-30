import assert from 'node:assert/strict';
import test from 'node:test';
import {
  normalizeListParams,
  toSearchParams,
} from '../../src/shared/lib/filters.js';

test('toSearchParams omits absent and empty values while retaining zero and false', () => {
  const source = {
    q: '',
    type: null,
    accountId: undefined,
    page: 0,
    active: false,
    size: 20,
  };
  assert.deepEqual(
    [...toSearchParams(source)],
    [
      ['page', '0'],
      ['active', 'false'],
      ['size', '20'],
    ],
  );
  assert.equal(source.q, '');
});

test('query serialization preserves Unicode text and reserved characters', () => {
  const text = 'Café 東京 — खर्च & notes=✓';
  const params = toSearchParams({ q: text });
  assert.equal(new URLSearchParams(params.toString()).get('q'), text);
  assert.deepEqual(normalizeListParams(params), {
    q: text,
    page: 1,
    size: 20,
    sort: 'date_desc',
  });
});

test('normalizeListParams supplies paging and sort defaults', () => {
  assert.deepEqual(normalizeListParams(new URLSearchParams()), {
    page: 1,
    size: 20,
    sort: 'date_desc',
  });
  assert.deepEqual(
    normalizeListParams(new URLSearchParams(), { sort: 'name_asc' }),
    {
      page: 1,
      size: 20,
      sort: 'name_asc',
    },
  );
});

test('normalizeListParams retains supported filters and ignores unknown values', () => {
  const params = new URLSearchParams({
    q: 'rent',
    type: 'EXPENSE',
    accountId: 'account-1',
    categoryId: 'category-2',
    from: '2026-10-01',
    to: '2026-10-31',
    page: '3',
    size: '50',
    sort: 'amount_asc',
    ignored: 'value',
  });
  assert.deepEqual(normalizeListParams(params), {
    q: 'rent',
    type: 'EXPENSE',
    accountId: 'account-1',
    categoryId: 'category-2',
    from: '2026-10-01',
    to: '2026-10-31',
    page: 3,
    size: 50,
    sort: 'amount_asc',
  });
  assert.equal(params.get('ignored'), 'value');
});

test('normalizeListParams omits empty filters and defaults empty sorting', () => {
  const params = new URLSearchParams({
    q: '',
    type: '',
    accountId: '',
    categoryId: '',
    from: '',
    to: '',
    sort: '',
  });
  assert.deepEqual(normalizeListParams(params), {
    page: 1,
    size: 20,
    sort: 'date_desc',
  });
});

test('page parsing rejects partial integers, exponent forms, and unsafe numbers', () => {
  for (const page of [
    '0',
    '-1',
    '1.5',
    '2rows',
    '1e2',
    '+2',
    ' 2',
    '2 ',
    '2\n',
    '',
    'Infinity',
    '9007199254740992',
  ]) {
    assert.equal(
      normalizeListParams(new URLSearchParams({ page })).page,
      1,
      page,
    );
  }
  assert.equal(
    normalizeListParams(new URLSearchParams({ page: '12' })).page,
    12,
  );
});

test('size parsing accepts only the supported strict integer values', () => {
  for (const size of ['20', '50', '100']) {
    assert.equal(
      normalizeListParams(new URLSearchParams({ size })).size,
      Number(size),
    );
  }
  for (const size of [
    '10',
    '0',
    '-20',
    '20.0',
    '20rows',
    '1e2',
    ' 50',
    '50 ',
    '+50',
    '',
  ]) {
    assert.equal(
      normalizeListParams(new URLSearchParams({ size })).size,
      20,
      size,
    );
  }
});

test('normalizeListParams preserves sort strings for collection-specific validation', () => {
  assert.equal(
    normalizeListParams(new URLSearchParams({ sort: 'custom_sort' })).sort,
    'custom_sort',
  );
});
