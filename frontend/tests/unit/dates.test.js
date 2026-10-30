import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import {
  dateRange,
  localToday,
  monthToDate,
  trendRange,
  validDate,
} from '../../src/shared/lib/dates.js';

test('localToday formats a constructed local calendar date with padding', () => {
  assert.equal(localToday(new Date(2026, 0, 2, 7, 30)), '2026-01-02');
});

test('localToday keeps local dates at UTC day rollover in both directions', () => {
  const moduleUrl = new URL('../../src/shared/lib/dates.js', import.meta.url)
    .href;
  for (const [timezone, year, month, day, hour, expected] of [
    ['Asia/Kolkata', 2026, 0, 1, 0, '2026-01-01'],
    ['America/Los_Angeles', 2026, 11, 31, 23, '2026-12-31'],
  ]) {
    const result = spawnSync(
      process.execPath,
      [
        '--input-type=module',
        '-e',
        `import { localToday } from ${JSON.stringify(moduleUrl)}; process.stdout.write(localToday(new Date(${year}, ${month}, ${day}, ${hour}, 5)));`,
      ],
      { env: { ...process.env, TZ: timezone }, encoding: 'utf8' },
    );
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout, expected, timezone);
  }
});

test('monthToDate starts at the first local calendar day', () => {
  assert.deepEqual(monthToDate(new Date(2026, 9, 31, 23, 59)), {
    from: '2026-10-01',
    to: '2026-10-31',
  });
});

test('dateRange returns month-to-date and year-to-date ranges', () => {
  const date = new Date(2026, 9, 31);
  assert.deepEqual(dateRange('month', date), {
    from: '2026-10-01',
    to: '2026-10-31',
  });
  assert.deepEqual(dateRange('year', date), {
    from: '2026-01-01',
    to: '2026-10-31',
  });
});

test('dateRange includes exactly 30 calendar days across year and leap boundaries', () => {
  assert.deepEqual(dateRange('30days', new Date(2026, 0, 5)), {
    from: '2025-12-07',
    to: '2026-01-05',
  });
  assert.deepEqual(dateRange('30days', new Date(2024, 2, 1)), {
    from: '2024-02-01',
    to: '2024-03-01',
  });
});

test('trendRange starts on the first day eleven months before today', () => {
  assert.deepEqual(trendRange(new Date(2026, 9, 31)), {
    from: '2025-11-01',
    to: '2026-10-31',
  });
  assert.deepEqual(trendRange(new Date(2024, 1, 29)), {
    from: '2023-03-01',
    to: '2024-02-29',
  });
});

test('range helpers do not mutate the supplied date', () => {
  const date = new Date(2026, 0, 31, 23, 15);
  const original = date.getTime();
  monthToDate(date);
  dateRange('30days', date);
  dateRange('year', date);
  trendRange(date);
  assert.equal(date.getTime(), original);
});

test('validDate accepts real strict calendar dates including leap days', () => {
  for (const value of [
    '2026-01-31',
    '2024-02-29',
    '2000-02-29',
    '0001-01-01',
  ]) {
    assert.equal(validDate(value), true, value);
  }
});

test('validDate rejects impossible dates, loose formats, and date-time strings', () => {
  for (const value of [
    '2025-02-29',
    '1900-02-29',
    '2026-04-31',
    '2026-00-01',
    '2026-13-01',
    '2026-01-00',
    '2026-01-32',
    '2026-1-01',
    '26-01-01',
    '2026-01-01T00:00:00Z',
    ' 2026-01-01',
    '2026-01-01 ',
    '0000-01-01',
    '',
    null,
    undefined,
  ])
    assert.equal(validDate(value), false, String(value));
});
