import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { localToday } from '../../src/shared/lib/dates.js';

const origin = process.env.LEDGER_LIVE_ORIGIN;
test('live backend reconciles balances, reports, edits, deletion, and filtered CSV', async ({
  page,
  request,
}) => {
  test.skip(
    !origin,
    'Set LEDGER_LIVE_ORIGIN to a dedicated test backend and BACKEND_ORIGIN to the same URL.',
  );
  const today = localToday();
  async function post(path, data) {
    const response = await request.post(`${origin}${path}`, { data });
    expect(response.status(), await response.text()).toBe(201);
    return response.json();
  }
  const space = await post('/api/spaces', {
    name: `Browser integration ${Date.now()}`,
  });
  const root = `/api/spaces/${space.id}`;
  const bank = await post(`${root}/accounts`, {
    name: 'Bank',
    type: 'BANK',
    startingBalance: '100.00',
    trackingStartDate: today,
  });
  const cash = await post(`${root}/accounts`, {
    name: 'Cash',
    type: 'CASH',
    startingBalance: '20.00',
    trackingStartDate: today,
  });
  const salary = await post(`${root}/categories`, {
    name: 'Salary',
    type: 'INCOME',
  });
  const food = await post(`${root}/categories`, {
    name: 'Food',
    type: 'EXPENSE',
  });
  await page.goto(`/spaces/${space.id}/transactions`);
  for (const values of [
    {
      type: 'INCOME',
      amount: '50',
      destination: bank.id,
      category: salary.id,
      description: 'Payday',
    },
    {
      type: 'EXPENSE',
      amount: '5',
      source: bank.id,
      category: food.id,
      description: 'Lunch',
    },
    {
      type: 'TRANSFER',
      amount: '10',
      source: bank.id,
      destination: cash.id,
      description: 'Cash withdrawal',
    },
  ]) {
    await page
      .getByRole('button', { name: 'Add transaction', exact: true })
      .click();
    const dialog = page.getByRole('dialog');
    await dialog
      .getByLabel('Transaction type', { exact: true })
      .selectOption(values.type);
    await dialog.getByLabel('Amount', { exact: true }).fill(values.amount);
    if (values.source)
      await dialog
        .getByRole('combobox', { name: 'Source account', exact: true })
        .selectOption(values.source);
    if (values.destination)
      await dialog
        .getByRole('combobox', { name: 'Destination account', exact: true })
        .selectOption(values.destination);
    if (values.category)
      await dialog
        .getByRole('combobox', { name: 'Category', exact: true })
        .selectOption(values.category);
    await dialog
      .getByLabel('Description', { exact: true })
      .fill(values.description);
    await dialog
      .getByRole('button', { name: 'Add transaction', exact: true })
      .click();
    await expect(dialog).toHaveCount(0);
    await expect(
      page.getByRole('cell', { name: values.description, exact: true }),
    ).toBeVisible();
  }
  await page.getByRole('link', { name: 'Accounts', exact: true }).click();
  await expect(
    page.getByRole('cell', { name: '135.00', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('cell', { name: '30.00', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Overview', exact: true }).click();
  await expect(page.getByText('165.00', { exact: true })).toBeVisible();
  await expect(page.getByText('45.00', { exact: true }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Transactions', exact: true }).click();
  await page
    .getByRole('row')
    .filter({ has: page.getByRole('cell', { name: 'Lunch', exact: true }) })
    .getByRole('button', { name: 'Edit', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByLabel('Amount', { exact: true })
    .fill('8');
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Save transaction', exact: true })
    .click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('link', { name: 'Accounts', exact: true }).click();
  await expect(
    page.getByRole('cell', { name: '132.00', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Reports', exact: true }).click();
  await expect(page.getByText('42.00', { exact: true }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Transactions', exact: true }).click();
  await page
    .getByRole('row')
    .filter({ has: page.getByRole('cell', { name: 'Lunch', exact: true }) })
    .getByRole('button', { name: 'Delete', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Delete', exact: true })
    .click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(
    page.getByRole('cell', { name: 'Lunch', exact: true }),
  ).toHaveCount(0);
  await page.getByRole('link', { name: 'Accounts', exact: true }).click();
  await expect(
    page.getByRole('cell', { name: '140.00', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Reports', exact: true }).click();
  await expect(page.getByText('50.00', { exact: true }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Transactions', exact: true }).click();
  await page.getByLabel('Search descriptions').fill('Payday');
  await expect(page).toHaveURL(/q=Payday/);
  const downloading = page.waitForEvent('download');
  await page
    .getByRole('link', { name: 'Export matching transactions' })
    .click();
  const download = await downloading;
  expect(download.suggestedFilename()).toMatch(/\.csv$/);
  const csv = await readFile(await download.path(), 'utf8');
  expect(csv).toContain('Payday');
  expect(csv).not.toContain('Cash withdrawal');
  expect(csv).toContain('50.00');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: '/tmp/portfolio-ledger-mobile.png',
    fullPage: true,
  });
  await page.getByRole('button', { name: 'Switch to dark mode' }).click();
  await page.getByRole('link', { name: 'Overview', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Overview', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('link', { name: 'Overview', exact: true }),
  ).toHaveAttribute('aria-current', 'page');
  await page.screenshot({
    path: '/tmp/portfolio-ledger-dark.png',
    fullPage: true,
  });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth),
  ).toBeLessThanOrEqual(390);
});
