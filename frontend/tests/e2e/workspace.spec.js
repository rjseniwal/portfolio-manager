import { test, expect } from '@playwright/test';
const space = { id: '11111111-1111-4111-8111-111111111111', name: 'Personal' };
const account = {
  id: '22222222-2222-4222-8222-222222222222',
  spaceId: space.id,
  name: 'Everyday bank',
  type: 'BANK',
  startingBalance: '100.00',
  trackingStartDate: '2026-01-01',
  balance: '135.00',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};
async function mockApi(page) {
  await page.context().route('**/backend/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/backend', '');
    const json = (data, status = 200) => route.fulfill({ status, json: data });
    if (path === '/api/spaces')
      return json({ items: [space], page: 1, size: 20, total: 1 });
    if (path === `/api/spaces/${space.id}`) return json(space);
    if (path.endsWith('/reports/summary'))
      return json({
        income: '50.00',
        expense: '5.00',
        netCashFlow: '45.00',
        ledgerBalance: '165.00',
        accountCount: 2,
        transactionCount: 3,
        categoryTotals: [
          {
            categoryId: 'food',
            name: 'Food',
            type: 'EXPENSE',
            amount: '5.00',
            transactionCount: 1,
          },
        ],
      });
    if (path.endsWith('/reports/monthly'))
      return json({
        items: [
          {
            month: '2026-10',
            income: '50.00',
            expense: '5.00',
            netCashFlow: '45.00',
          },
        ],
      });
    if (path.endsWith('/accounts')) {
      if (route.request().method() === 'POST')
        return json({ ...account, ...route.request().postDataJSON() }, 201);
      return json({ items: [account], page: 1, size: 20, total: 1 });
    }
    if (path.endsWith(`/accounts/${account.id}/balance`))
      return json({ accountId: account.id, balance: '135.00' });
    if (path.endsWith(`/accounts/${account.id}`)) {
      if (route.request().method() === 'DELETE')
        return json(
          { code: 'CONFLICT', message: 'Account has transactions' },
          409,
        );
      return json(account);
    }
    if (path.endsWith('/categories'))
      return json({ items: [], page: 1, size: 20, total: 0 });
    if (path.endsWith('/transactions'))
      return json({
        items: [],
        page: Number(url.searchParams.get('page') || 1),
        size: 20,
        total: 0,
      });
    if (path.endsWith('/transactions/export'))
      return route.fulfill({
        contentType: 'text/csv',
        headers: {
          'Content-Disposition': 'attachment; filename="transactions.csv"',
        },
        body: 'id,type,amount\r\n',
      });
    return json({ message: 'Not found' }, 404);
  });
}

test('routed accounts survive refresh and explain blocked deletion', async ({
  page,
}) => {
  await mockApi(page);
  await page.goto(`/spaces/${space.id}/accounts`);
  await expect(
    page.getByRole('heading', { name: 'Accounts', exact: true }),
  ).toBeVisible();
  await expect(page.getByText('135.00', { exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Delete Everyday bank' }).click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Delete', exact: true })
    .click();
  await expect(page.getByRole('alert')).toContainText(
    'Account has transactions',
  );
  await expect(page.getByRole('dialog')).toBeVisible();
});

test('overview and reports show exact totals; transaction filters persist and export all matches', async ({
  page,
}) => {
  await mockApi(page);
  await page.goto(`/spaces/${space.id}`);
  await expect(page.getByText('165.00', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Reports', exact: true }).click();
  await expect(page.getByText('45.00', { exact: true }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Transactions', exact: true }).click();
  await page.getByLabel('Search descriptions').fill('Lunch');
  await expect(page).toHaveURL(/q=Lunch/);
  await page.reload();
  await expect(page.getByLabel('Search descriptions')).toHaveValue('Lunch');
  const download = page.waitForEvent('download');
  await page
    .getByRole('link', { name: 'Export matching transactions' })
    .click();
  const file = await download;
  await expect(file.suggestedFilename()).toBe('transactions.csv');
  await page.getByRole('button', { name: 'Switch to dark mode' }).click();
  await page.reload();
  await expect(page.locator('html')).toHaveClass(/dark/);
});
