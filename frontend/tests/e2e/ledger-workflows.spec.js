import { test, expect } from '@playwright/test';

const PERSONAL = {
  id: '11111111-1111-4111-8111-111111111111',
  name: 'Personal',
};
const BUSINESS = {
  id: '99999999-9999-4999-8999-999999999999',
  name: 'Business',
};
const BANK_ID = '22222222-2222-4222-8222-222222222222';
const CASH_ID = '33333333-3333-4333-8333-333333333333';
const INCOME_ID = '44444444-4444-4444-8444-444444444444';
const EXPENSE_ID = '55555555-5555-4555-8555-555555555555';
const STAMP = '2026-01-01T00:00:00Z';
const base = (space = PERSONAL) => `/api/spaces/${space.id}`;
const screen = (resource, space = PERSONAL) =>
  `/spaces/${space.id}/${resource}`;

function account(id, name, extra = {}) {
  return {
    id,
    spaceId: PERSONAL.id,
    name,
    type: 'BANK',
    startingBalance: '100.00',
    balance: '100.00',
    trackingStartDate: '2026-01-01',
    createdAt: STAMP,
    updatedAt: STAMP,
    ...extra,
  };
}

function category(id, name, type) {
  return {
    id,
    spaceId: PERSONAL.id,
    name,
    type,
    createdAt: STAMP,
    updatedAt: STAMP,
  };
}

function transaction(extra = {}) {
  return {
    id: '66666666-6666-4666-8666-666666666666',
    spaceId: PERSONAL.id,
    transactionDate: '2026-10-01',
    type: 'INCOME',
    amount: '1.00',
    sourceAccountId: null,
    destinationAccountId: BANK_ID,
    categoryId: INCOME_ID,
    description: 'Original salary',
    createdAt: STAMP,
    updatedAt: STAMP,
    ...extra,
  };
}

function defaultLedger(extra = {}) {
  return {
    accounts: [
      account(BANK_ID, 'Everyday bank'),
      account(CASH_ID, 'Cash wallet', { type: 'CASH' }),
    ],
    categories: [
      category(INCOME_ID, 'Salary', 'INCOME'),
      category(EXPENSE_ID, 'Food', 'EXPENSE'),
    ],
    transactions: [],
    ...extra,
  };
}

/** Stateful HTTP fixture: every successful write must become visible through a real refetch. */
async function mockLedger(
  page,
  primary = defaultLedger(),
  secondary = defaultLedger({ accounts: [], categories: [] }),
) {
  const state = {
    ledgers: { [PERSONAL.id]: primary, [BUSINESS.id]: secondary },
    requests: [],
    writes: [],
    failures: new Map(),
    nextFailures: [],
    beforeWrite: null,
  };
  let nextId = 100;
  const enrich = (ledger, item) => ({
    ...item,
    sourceAccountName:
      ledger.accounts.find((a) => a.id === item.sourceAccountId)?.name ?? null,
    destinationAccountName:
      ledger.accounts.find((a) => a.id === item.destinationAccountId)?.name ??
      null,
    categoryName:
      ledger.categories.find((c) => c.id === item.categoryId)?.name ?? null,
  });
  await page.context().route('**/backend/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/backend/, '');
    const method = request.method();
    const body = ['POST', 'PATCH'].includes(method)
      ? request.postDataJSON()
      : null;
    const call = { method, path, body };
    state.requests.push(call);
    const json = (value, status = 200) =>
      route.fulfill({ status, json: value });
    if (method !== 'GET') {
      state.writes.push(call);
      await state.beforeWrite?.(call);
    }
    const failureIndex = state.nextFailures.findIndex(
      (f) => f.method === method && f.path === path,
    );
    const failure =
      failureIndex >= 0
        ? state.nextFailures.splice(failureIndex, 1)[0]
        : state.failures.get(`${method} ${path}`);
    if (failure)
      return json(
        { code: failure.code || 'CONFLICT', message: failure.message },
        failure.status || 409,
      );
    if (path === '/api/spaces')
      return json({ items: [PERSONAL, BUSINESS], page: 1, size: 20, total: 2 });
    const match =
      /^\/api\/spaces\/([^/]+)(?:\/(accounts|categories|transactions|reports)(?:\/([^/]+))?(?:\/([^/]+))?)?$/.exec(
        path,
      );
    if (!match) return json({ message: 'Unmocked route' }, 404);
    const [, spaceId, resource, id, action] = match;
    const space = [PERSONAL, BUSINESS].find((item) => item.id === spaceId);
    const ledger = state.ledgers[spaceId];
    if (!space || !ledger) return json({ message: 'Space not found' }, 404);
    if (!resource) return json(space);
    if (resource === 'reports') {
      if (id === 'monthly') return json({ items: [] });
      return json({
        income: '0.00',
        expense: '0.00',
        netCashFlow: '0.00',
        ledgerBalance: '0.00',
        accountCount: ledger.accounts.length,
        transactionCount: ledger.transactions.length,
        categoryTotals: [],
      });
    }
    const records = ledger[resource];
    const record = records.find((item) => item.id === id);
    if (action === 'balance' && record)
      return json({ accountId: id, balance: record.balance });
    if (method === 'GET' && id) {
      return record
        ? json(resource === 'transactions' ? enrich(ledger, record) : record)
        : json({ message: 'Record not found in this space' }, 404);
    }
    if (method === 'GET') {
      const params = url.searchParams;
      let items = records.filter((item) => {
        if (
          params.get('q') &&
          !(item.name ?? item.description ?? '')
            .toLowerCase()
            .includes(params.get('q').toLowerCase())
        )
          return false;
        if (params.get('type') && item.type !== params.get('type'))
          return false;
        if (
          params.get('categoryId') &&
          item.categoryId !== params.get('categoryId')
        )
          return false;
        if (
          params.get('accountId') &&
          ![item.sourceAccountId, item.destinationAccountId].includes(
            params.get('accountId'),
          )
        )
          return false;
        if (params.get('from') && item.transactionDate < params.get('from'))
          return false;
        if (params.get('to') && item.transactionDate > params.get('to'))
          return false;
        return true;
      });
      const sort = params.get('sort') || '';
      if (sort.startsWith('name_'))
        items.sort(
          (a, b) =>
            a.name.localeCompare(b.name) * (sort === 'name_desc' ? -1 : 1),
        );
      if (sort.startsWith('date_'))
        items.sort(
          (a, b) =>
            a.transactionDate.localeCompare(b.transactionDate) *
            (sort === 'date_desc' ? -1 : 1),
        );
      const pageNumber = Number(params.get('page') || 1);
      const size = Number(params.get('size') || 20);
      const total = items.length;
      items = items.slice((pageNumber - 1) * size, pageNumber * size);
      return json({
        items:
          resource === 'transactions'
            ? items.map((item) => enrich(ledger, item))
            : items,
        page: pageNumber,
        size,
        total,
      });
    }
    if (method === 'POST') {
      const saved = {
        id: `70000000-0000-4000-8000-${String(nextId++).padStart(12, '0')}`,
        spaceId,
        ...body,
        createdAt: STAMP,
        updatedAt: STAMP,
      };
      if (resource === 'accounts') saved.balance = saved.startingBalance;
      records.push(saved);
      return json(
        resource === 'transactions' ? enrich(ledger, saved) : saved,
        201,
      );
    }
    if (!record)
      return json({ message: 'Record not found in this space' }, 404);
    if (method === 'PATCH') {
      Object.assign(record, body, { updatedAt: '2026-10-31T12:00:00Z' });
      if (resource === 'accounts' && Object.hasOwn(body, 'startingBalance'))
        record.balance = body.startingBalance;
      return json(
        resource === 'transactions' ? enrich(ledger, record) : record,
      );
    }
    if (method === 'DELETE') {
      records.splice(records.indexOf(record), 1);
      return route.fulfill({ status: 204 });
    }
    return json({ message: 'Method not mocked' }, 405);
  });
  return state;
}

async function fillAccount(
  dialog,
  { name, balance = '0.00', type = 'BANK', date = '2026-01-01' },
) {
  await dialog.getByLabel('Account name', { exact: true }).fill(name);
  await dialog.getByLabel('Account type', { exact: true }).selectOption(type);
  await dialog.getByLabel('Starting balance', { exact: true }).fill(balance);
  await dialog.getByLabel('Tracking start date', { exact: true }).fill(date);
}

async function fillTransaction(dialog, payload) {
  await dialog
    .getByLabel('Transaction type', { exact: true })
    .selectOption(payload.type);
  await dialog
    .getByLabel('Date', { exact: true })
    .fill(payload.transactionDate);
  await dialog.getByLabel('Amount', { exact: true }).fill(payload.amount);
  if (payload.sourceAccountId)
    await dialog
      .getByLabel('Source account', { exact: true })
      .selectOption(payload.sourceAccountId);
  if (payload.destinationAccountId)
    await dialog
      .getByLabel('Destination account', { exact: true })
      .selectOption(payload.destinationAccountId);
  if (payload.categoryId)
    await dialog
      .getByLabel('Category', { exact: true })
      .selectOption(payload.categoryId);
  await dialog
    .getByLabel('Description', { exact: true })
    .fill(payload.description || '');
}

test('account creation and editing preserve negative exact balances and immutable type', async ({
  page,
}) => {
  const state = await mockLedger(page);
  await page.goto(screen('accounts'));
  await page.getByRole('button', { name: 'Add account', exact: true }).click();
  let dialog = page.getByRole('dialog', { name: 'Add account', exact: true });
  await expect(
    dialog.getByLabel('Account name', { exact: true }),
  ).toBeFocused();
  await fillAccount(dialog, {
    name: '  Café 東京 wallet  ',
    balance: '-90071992547409.93',
    type: 'CASH',
  });
  await dialog
    .getByRole('button', { name: 'Add account', exact: true })
    .click();
  await expect(dialog).toBeHidden();
  await expect(
    page.getByRole('button', { name: 'Add account', exact: true }),
  ).toBeFocused();
  let row = page.getByRole('row').filter({ hasText: 'Café 東京 wallet' });
  await expect(row).toContainText('-90,071,992,547,409.93');
  expect(state.writes[0]).toEqual({
    method: 'POST',
    path: `${base()}/accounts`,
    body: {
      name: 'Café 東京 wallet',
      type: 'CASH',
      startingBalance: '-90071992547409.93',
      trackingStartDate: '2026-01-01',
    },
  });
  await row
    .getByRole('button', { name: 'Edit Café 東京 wallet', exact: true })
    .click();
  dialog = page.getByRole('dialog', { name: 'Edit account', exact: true });
  await expect(
    dialog.getByLabel('Account type', { exact: true }),
  ).toBeDisabled();
  await dialog
    .getByLabel('Account name', { exact: true })
    .fill('Renamed wallet');
  await dialog.getByLabel('Starting balance', { exact: true }).fill('-0.01');
  await dialog
    .getByLabel('Tracking start date', { exact: true })
    .fill('2026-02-03');
  await dialog
    .getByRole('button', { name: 'Save account', exact: true })
    .click();
  await expect(dialog).toBeHidden();
  row = page.getByRole('row').filter({ hasText: 'Renamed wallet' });
  await expect(row).toContainText('-0.01');
  await expect(row).toContainText('2026-02-03');
  await expect(row).toContainText('cash');
  expect(state.writes[1]).toEqual({
    method: 'PATCH',
    path: `${base()}/accounts/${state.ledgers[PERSONAL.id].accounts.at(-1).id}`,
    body: {
      name: 'Renamed wallet',
      startingBalance: '-0.01',
      trackingStartDate: '2026-02-03',
    },
  });
});

test('categories can be created and renamed without changing their type', async ({
  page,
}) => {
  const state = await mockLedger(page);
  await page.goto(screen('categories'));
  await page.getByRole('button', { name: 'Add category', exact: true }).click();
  let dialog = page.getByRole('dialog', { name: 'Add category', exact: true });
  await dialog
    .getByLabel('Category name', { exact: true })
    .fill('  Consulting 東京  ');
  await dialog
    .getByLabel('Category type', { exact: true })
    .selectOption('INCOME');
  await dialog
    .getByRole('button', { name: 'Add category', exact: true })
    .click();
  await expect(dialog).toBeHidden();
  let row = page.getByRole('row').filter({ hasText: 'Consulting 東京' });
  await expect(row).toContainText('income');
  expect(state.writes[0].body).toEqual({
    name: 'Consulting 東京',
    type: 'INCOME',
  });
  await row
    .getByRole('button', { name: 'Rename Consulting 東京', exact: true })
    .click();
  dialog = page.getByRole('dialog', { name: 'Rename category', exact: true });
  await expect(
    dialog.getByLabel('Category type', { exact: true }),
  ).toBeDisabled();
  await dialog
    .getByLabel('Category name', { exact: true })
    .fill('Consulting & teaching');
  await dialog
    .getByRole('button', { name: 'Save category', exact: true })
    .click();
  await expect(dialog).toBeHidden();
  row = page.getByRole('row').filter({ hasText: 'Consulting & teaching' });
  await expect(row).toContainText('income');
  expect(state.writes[1]).toEqual({
    method: 'PATCH',
    path: `${base()}/categories/${state.ledgers[PERSONAL.id].categories.at(-1).id}`,
    body: { name: 'Consulting & teaching' },
  });
});

const creationCases = [
  {
    type: 'INCOME',
    amount: '99999999999999999.99',
    formatted: '99,999,999,999,999,999.99',
    sourceAccountId: null,
    destinationAccountId: BANK_ID,
    categoryId: INCOME_ID,
    description: 'Salary 東京 — exact maximum',
    names: ['Everyday bank', 'Salary'],
  },
  {
    type: 'EXPENSE',
    amount: '90071992547409.93',
    formatted: '90,071,992,547,409.93',
    sourceAccountId: CASH_ID,
    destinationAccountId: null,
    categoryId: EXPENSE_ID,
    description: null,
    names: ['Cash wallet', 'Food'],
  },
  {
    type: 'TRANSFER',
    amount: '0.01',
    formatted: '0.01',
    sourceAccountId: BANK_ID,
    destinationAccountId: CASH_ID,
    categoryId: null,
    description: 'Move one cent',
    names: ['Everyday bank', 'Cash wallet'],
  },
];

for (const scenario of creationCases) {
  test(`${scenario.type} creation sends exact full payload and refetches related names`, async ({
    page,
  }) => {
    const state = await mockLedger(page);
    await page.goto(screen('transactions'));
    await page
      .getByRole('button', { name: 'Add transaction', exact: true })
      .click();
    const dialog = page.getByRole('dialog', {
      name: 'Add transaction',
      exact: true,
    });
    const {
      type,
      amount,
      sourceAccountId,
      destinationAccountId,
      categoryId,
      description,
    } = scenario;
    const payload = {
      transactionDate: '2026-10-15',
      type,
      amount,
      sourceAccountId,
      destinationAccountId,
      categoryId,
      description,
    };
    await fillTransaction(dialog, payload);
    if (type === 'INCOME')
      await expect(
        dialog.getByLabel('Source account', { exact: true }),
      ).toHaveCount(0);
    if (type === 'EXPENSE')
      await expect(
        dialog.getByLabel('Destination account', { exact: true }),
      ).toHaveCount(0);
    if (type === 'TRANSFER') {
      await expect(dialog.getByLabel('Category', { exact: true })).toHaveCount(
        0,
      );
      await expect(
        dialog.getByRole('button', { name: 'Add a category', exact: true }),
      ).toHaveCount(0);
    }
    await dialog
      .getByRole('button', { name: 'Add transaction', exact: true })
      .click();
    await expect(dialog).toBeHidden();
    const row = page
      .getByRole('table', { name: 'Transactions', exact: true })
      .getByRole('row')
      .nth(1);
    await expect(row).toContainText(scenario.formatted);
    await expect(row).toContainText('2026-10-15');
    for (const name of scenario.names) await expect(row).toContainText(name);
    if (description) await expect(row).toContainText(description);
    expect(state.writes).toEqual([
      { method: 'POST', path: `${base()}/transactions`, body: payload },
    ]);
  });
}

test('transaction edits replace the complete shape and null unused account/category/description fields', async ({
  page,
}) => {
  const state = await mockLedger(
    page,
    defaultLedger({ transactions: [transaction()] }),
  );
  await page.goto(screen('transactions'));
  const cases = [
    {
      transactionDate: '2026-10-02',
      type: 'EXPENSE',
      amount: '2.30',
      sourceAccountId: CASH_ID,
      destinationAccountId: null,
      categoryId: EXPENSE_ID,
      description: null,
    },
    {
      transactionDate: '2026-10-03',
      type: 'TRANSFER',
      amount: '90071992547409.93',
      sourceAccountId: BANK_ID,
      destinationAccountId: CASH_ID,
      categoryId: null,
      description: null,
    },
    {
      transactionDate: '2026-10-04',
      type: 'INCOME',
      amount: '0.01',
      sourceAccountId: null,
      destinationAccountId: CASH_ID,
      categoryId: INCOME_ID,
      description: 'Updated salary ✓',
    },
  ];
  for (const [index, payload] of cases.entries()) {
    const row = page
      .getByRole('table', { name: 'Transactions', exact: true })
      .getByRole('row')
      .nth(1);
    await row.getByRole('button', { name: 'Edit', exact: true }).click();
    const dialog = page.getByRole('dialog', {
      name: 'Edit transaction',
      exact: true,
    });
    await fillTransaction(dialog, payload);
    await dialog
      .getByRole('button', { name: 'Save transaction', exact: true })
      .click();
    await expect(dialog).toBeHidden();
    await expect(row).toContainText(payload.transactionDate);
    await expect(row.getByRole('cell').nth(1)).toHaveText(
      payload.type.toLowerCase(),
    );
    await expect(row).toContainText('Cash wallet');
    if (payload.type === 'TRANSFER') {
      await expect(row).toContainText('Everyday bank');
      await expect(row.getByRole('cell').nth(4)).toHaveText('—');
      await expect(row).toContainText('90,071,992,547,409.93');
    }
    if (payload.description)
      await expect(row).toContainText(payload.description);
    else await expect(row.getByRole('cell').nth(5)).toHaveText('—');
    expect(state.writes[index]).toEqual({
      method: 'PATCH',
      path: `${base()}/transactions/66666666-6666-4666-8666-666666666666`,
      body: payload,
    });
  }
});

test('creating missing account and category inside a transaction preserves the complete draft', async ({
  page,
}) => {
  const state = await mockLedger(
    page,
    defaultLedger({ accounts: [], categories: [] }),
  );
  await page.goto(screen('transactions'));
  await page
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  const draft = page.getByRole('dialog', {
    name: 'Add transaction',
    exact: true,
  });
  await draft.getByLabel('Amount', { exact: true }).fill('12345678901234.56');
  await draft.getByLabel('Date', { exact: true }).fill('2026-10-21');
  await draft
    .getByLabel('Description', { exact: true })
    .fill('Draft survives nested forms — 東京');
  await draft
    .getByRole('button', { name: 'Add an account', exact: true })
    .click();
  const addAccount = page.getByRole('dialog', {
    name: 'Add account',
    exact: true,
  });
  await fillAccount(addAccount, {
    name: 'Draft wallet',
    balance: '-12.34',
    type: 'CASH',
  });
  await addAccount
    .getByRole('button', { name: 'Add account', exact: true })
    .click();
  await expect(addAccount).toBeHidden();
  await expect(
    draft.getByRole('button', { name: 'Add an account', exact: true }),
  ).toBeFocused();
  await expect(draft.getByLabel('Amount', { exact: true })).toHaveValue(
    '12345678901234.56',
  );
  await expect(draft.getByLabel('Date', { exact: true })).toHaveValue(
    '2026-10-21',
  );
  await expect(draft.getByLabel('Description', { exact: true })).toHaveValue(
    'Draft survives nested forms — 東京',
  );
  const createdAccount = state.ledgers[PERSONAL.id].accounts[0];
  await expect(draft.getByLabel('Source account', { exact: true })).toHaveValue(
    createdAccount.id,
  );
  await expect(
    draft
      .getByLabel('Source account', { exact: true })
      .locator('option:checked'),
  ).toHaveText('Draft wallet');
  await draft
    .getByRole('button', { name: 'Add a category', exact: true })
    .click();
  const addCategory = page.getByRole('dialog', {
    name: 'Add category',
    exact: true,
  });
  await expect(
    addCategory.getByLabel('Category type', { exact: true }),
  ).toHaveValue('EXPENSE');
  await expect(
    addCategory.getByLabel('Category type', { exact: true }),
  ).toBeDisabled();
  await addCategory
    .getByLabel('Category name', { exact: true })
    .fill('Travel 東京');
  await addCategory
    .getByRole('button', { name: 'Add category', exact: true })
    .click();
  await expect(addCategory).toBeHidden();
  await expect(
    draft.getByRole('button', { name: 'Add a category', exact: true }),
  ).toBeFocused();
  const createdCategory = state.ledgers[PERSONAL.id].categories[0];
  await expect(draft.getByLabel('Category', { exact: true })).toHaveValue(
    createdCategory.id,
  );
  await expect(draft.getByLabel('Amount', { exact: true })).toHaveValue(
    '12345678901234.56',
  );
  await expect(draft.getByLabel('Description', { exact: true })).toHaveValue(
    'Draft survives nested forms — 東京',
  );
  await draft
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  await expect(draft).toBeHidden();
  const row = page
    .getByRole('row')
    .filter({ hasText: 'Draft survives nested forms — 東京' });
  await expect(row).toContainText('12,345,678,901,234.56');
  await expect(row).toContainText('Draft wallet');
  await expect(row).toContainText('Travel 東京');
  expect(state.writes.at(-1).body).toEqual({
    transactionDate: '2026-10-21',
    type: 'EXPENSE',
    amount: '12345678901234.56',
    sourceAccountId: createdAccount.id,
    destinationAccountId: null,
    categoryId: createdCategory.id,
    description: 'Draft survives nested forms — 東京',
  });
});

test('transfer can create both missing accounts without losing the selected source or draft', async ({
  page,
}) => {
  const state = await mockLedger(
    page,
    defaultLedger({ accounts: [], categories: [] }),
  );
  await page.goto(screen('transactions'));
  await page
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  const draft = page.getByRole('dialog', {
    name: 'Add transaction',
    exact: true,
  });
  await draft
    .getByLabel('Transaction type', { exact: true })
    .selectOption('TRANSFER');
  await draft.getByLabel('Amount', { exact: true }).fill('19.99');
  await draft.getByLabel('Date', { exact: true }).fill('2026-10-20');
  await draft
    .getByLabel('Description', { exact: true })
    .fill('Create both sides');
  for (const name of ['Transfer source', 'Transfer destination']) {
    await draft
      .getByRole('button', { name: 'Add an account', exact: true })
      .click();
    const dialog = page.getByRole('dialog', {
      name: 'Add account',
      exact: true,
    });
    await fillAccount(dialog, { name });
    await dialog
      .getByRole('button', { name: 'Add account', exact: true })
      .click();
    await expect(dialog).toBeHidden();
  }
  const [source, destination] = state.ledgers[PERSONAL.id].accounts;
  await expect(draft.getByLabel('Source account', { exact: true })).toHaveValue(
    source.id,
  );
  await expect(
    draft.getByLabel('Destination account', { exact: true }),
  ).toHaveValue(destination.id);
  await expect(draft.getByLabel('Amount', { exact: true })).toHaveValue(
    '19.99',
  );
  await expect(draft.getByLabel('Description', { exact: true })).toHaveValue(
    'Create both sides',
  );
  await draft
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  await expect(draft).toBeHidden();
  const row = page.getByRole('row').filter({ hasText: 'Create both sides' });
  await expect(row).toContainText('Transfer source');
  await expect(row).toContainText('Transfer destination');
  expect(state.writes.at(-1).body).toEqual({
    transactionDate: '2026-10-20',
    type: 'TRANSFER',
    amount: '19.99',
    sourceAccountId: source.id,
    destinationAccountId: destination.id,
    categoryId: null,
    description: 'Create both sides',
  });
});

test('changing transaction type resets category picker pagination to compatible choices', async ({
  page,
}) => {
  const expenses = Array.from({ length: 21 }, (_, index) =>
    category(
      `expense-${index}`,
      `Expense ${String(index + 1).padStart(2, '0')}`,
      'EXPENSE',
    ),
  );
  await mockLedger(
    page,
    defaultLedger({
      categories: [...expenses, category(INCOME_ID, 'Salary', 'INCOME')],
    }),
  );
  await page.goto(screen('transactions'));
  await page
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  const dialog = page.getByRole('dialog', {
    name: 'Add transaction',
    exact: true,
  });
  await dialog
    .getByRole('button', { name: 'More choices', exact: true })
    .click();
  await expect(
    dialog
      .getByLabel('Category', { exact: true })
      .locator('option[value="expense-20"]'),
  ).toHaveText('Expense 21');
  await dialog
    .getByLabel('Transaction type', { exact: true })
    .selectOption('INCOME');
  await expect(dialog.getByLabel('Category', { exact: true })).toHaveValue('');
  await expect(
    dialog
      .getByLabel('Category', { exact: true })
      .locator(`option[value="${INCOME_ID}"]`),
  ).toHaveText('Salary');
  await expect(
    dialog.getByRole('button', { name: 'Previous choices', exact: true }),
  ).toHaveCount(0);
});

test('a failed transaction save keeps every field and can be retried successfully', async ({
  page,
}) => {
  const state = await mockLedger(page);
  state.nextFailures.push({
    method: 'POST',
    path: `${base()}/transactions`,
    status: 409,
    message: 'Concurrent update: review and try again.',
  });
  await page.goto(screen('transactions'));
  await page
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  const dialog = page.getByRole('dialog', {
    name: 'Add transaction',
    exact: true,
  });
  const payload = {
    transactionDate: '2026-10-16',
    type: 'EXPENSE',
    amount: '90071992547409.93',
    sourceAccountId: BANK_ID,
    destinationAccountId: null,
    categoryId: EXPENSE_ID,
    description: 'Retain this draft 東京',
  };
  await fillTransaction(dialog, payload);
  await dialog
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  await expect(dialog.getByRole('alert')).toHaveText(
    'Concurrent update: review and try again.',
  );
  await expect(
    dialog.getByLabel('Transaction type', { exact: true }),
  ).toHaveValue('EXPENSE');
  await expect(dialog.getByLabel('Date', { exact: true })).toHaveValue(
    payload.transactionDate,
  );
  await expect(dialog.getByLabel('Amount', { exact: true })).toHaveValue(
    payload.amount,
  );
  await expect(
    dialog.getByLabel('Source account', { exact: true }),
  ).toHaveValue(BANK_ID);
  await expect(dialog.getByLabel('Category', { exact: true })).toHaveValue(
    EXPENSE_ID,
  );
  await expect(dialog.getByLabel('Description', { exact: true })).toHaveValue(
    payload.description,
  );
  await dialog
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  await expect(dialog).toBeHidden();
  await expect(
    page.getByRole('row').filter({ hasText: payload.description }),
  ).toContainText('90,071,992,547,409.93');
  expect(state.writes.map((call) => call.body)).toEqual([payload, payload]);
});

for (const resource of ['accounts', 'transactions']) {
  test(`deleting the only ${resource} result on page two returns to the preceding page`, async ({
    page,
  }) => {
    const records = Array.from({ length: 21 }, (_, index) =>
      resource === 'accounts'
        ? account(
            `account-${index}`,
            `Account ${String(index + 1).padStart(2, '0')}`,
          )
        : transaction({
            id: `transaction-${index}`,
            transactionDate: `2026-10-${String(index + 1).padStart(2, '0')}`,
            description: `Entry ${String(index + 1).padStart(2, '0')}`,
          }),
    );
    const state = await mockLedger(
      page,
      defaultLedger({ [resource]: records }),
    );
    await page.goto(`${screen(resource)}?page=2`);
    await expect(page.getByText('Page 2 of 2', { exact: true })).toBeVisible();
    const row = page
      .getByRole('table', {
        name: resource === 'accounts' ? 'Accounts' : 'Transactions',
        exact: true,
      })
      .getByRole('row')
      .nth(1);
    const removedText = resource === 'accounts' ? 'Account 21' : 'Entry 01';
    await expect(row).toContainText(removedText);
    await row
      .getByRole('button', {
        name: resource === 'accounts' ? `Delete ${removedText}` : 'Delete',
        exact: true,
      })
      .click();
    const dialog = page.getByRole('dialog', {
      name: resource === 'accounts' ? 'Delete account?' : 'Delete transaction?',
      exact: true,
    });
    await dialog.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(dialog).toBeHidden();
    await expect(page).toHaveURL(/\?page=1$/);
    await expect(page.getByText('Page 1 of 1', { exact: true })).toBeVisible();
    await expect(page.getByRole('table').getByRole('row')).toHaveCount(21);
    await expect(
      page.getByRole('row').filter({ hasText: removedText }),
    ).toHaveCount(0);
    expect(state.writes).toHaveLength(1);
    expect(state.writes[0].method).toBe('DELETE');
    await expect(
      page.getByRole('button', { name: 'Next', exact: true }),
    ).toBeDisabled();
  });
}

test('switching spaces isolates cached records and a failed list can recover', async ({
  page,
}) => {
  const state = await mockLedger(
    page,
    defaultLedger({ accounts: [account(BANK_ID, 'Personal bank')] }),
    defaultLedger({
      accounts: [
        account(CASH_ID, 'Business cash', {
          spaceId: BUSINESS.id,
          type: 'CASH',
          balance: '-25.00',
        }),
      ],
      categories: [],
      transactions: [],
    }),
  );
  await page.goto(screen('accounts'));
  await expect(
    page.getByRole('link', { name: 'Personal bank', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: '← All spaces', exact: true }).click();
  await page.getByRole('button', { name: 'Business', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Business', exact: true }),
  ).toBeVisible();
  state.failures.set(`GET ${base(BUSINESS)}/accounts`, {
    status: 503,
    message: 'Business accounts temporarily unavailable.',
  });
  await page.getByRole('link', { name: 'Accounts', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Request failed (503)');
  await expect(
    page.getByRole('link', { name: 'Personal bank', exact: true }),
  ).toHaveCount(0);
  state.failures.delete(`GET ${base(BUSINESS)}/accounts`);
  await page.getByRole('button', { name: 'Try again', exact: true }).click();
  const row = page.getByRole('row').filter({ hasText: 'Business cash' });
  await expect(row).toContainText('-25.00');
  await expect(page.getByRole('alert')).toHaveCount(0);
  await page.getByRole('link', { name: 'Business cash', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Business cash', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('link', { name: 'Personal bank', exact: true }),
  ).toHaveCount(0);
  await page.getByRole('link', { name: '← All spaces', exact: true }).click();
  await page.getByRole('button', { name: 'Personal', exact: true }).click();
  await page.getByRole('link', { name: 'Accounts', exact: true }).click();
  await expect(
    page.getByRole('link', { name: 'Personal bank', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('link', { name: 'Business cash', exact: true }),
  ).toHaveCount(0);
});

test('keyboard opens and closes dialogs, traps focus, and restores focus to the opener', async ({
  page,
}) => {
  await mockLedger(page);
  await page.goto(screen('accounts'));
  const opener = page.getByRole('button', { name: 'Add account', exact: true });
  await opener.focus();
  await page.keyboard.press('Enter');
  let dialog = page.getByRole('dialog', { name: 'Add account', exact: true });
  await expect(
    dialog.getByLabel('Account name', { exact: true }),
  ).toBeFocused();
  for (let index = 0; index < 9; index += 1) {
    await page.keyboard.press('Tab');
    await expect
      .poll(() =>
        dialog.evaluate((node) => node.contains(document.activeElement)),
      )
      .toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect(opener).toBeFocused();
  await page.keyboard.press('Enter');
  dialog = page.getByRole('dialog', { name: 'Add account', exact: true });
  await dialog.getByRole('button', { name: 'Close', exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect(opener).toBeFocused();
});

test('a pending save prevents Escape, close, cancel, and duplicate submission', async ({
  page,
}) => {
  const state = await mockLedger(page);
  let release;
  state.beforeWrite = () =>
    new Promise((resolve) => {
      release = resolve;
    });
  await page.goto(screen('accounts'));
  await page.getByRole('button', { name: 'Add account', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Add account', exact: true });
  await fillAccount(dialog, { name: 'Pending account', balance: '10.25' });
  await dialog
    .getByRole('button', { name: 'Add account', exact: true })
    .click();
  await expect(
    dialog.getByRole('button', { name: 'Saving…', exact: true }),
  ).toBeDisabled();
  await expect(
    dialog.getByRole('button', { name: 'Cancel', exact: true }),
  ).toBeDisabled();
  await page.keyboard.press('Escape');
  await expect(dialog).toBeVisible();
  await expect(
    dialog.getByRole('button', { name: 'Close', exact: true }),
  ).toBeDisabled();
  await expect(dialog).toBeVisible();
  await page.keyboard.press('Enter');
  expect(state.writes).toHaveLength(1);
  release();
  await expect(dialog).toBeHidden();
  await expect(
    page.getByRole('row').filter({ hasText: 'Pending account' }),
  ).toContainText('10.25');
});

test('mobile pages contain table overflow and keep long transaction forms usable', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  const state = await mockLedger(
    page,
    defaultLedger({
      accounts: [
        account(
          BANK_ID,
          'A very long account name 東京 with exact large balance',
          { balance: '99999999999999999.99' },
        ),
        account(CASH_ID, 'Cash wallet', { type: 'CASH' }),
      ],
    }),
  );
  await page.goto(screen('accounts'));
  const row = page
    .getByRole('row')
    .filter({ hasText: 'A very long account name' });
  await expect(row).toContainText('99,999,999,999,999,999.99');
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    )
    .toBe(true);
  await page.getByRole('link', { name: 'Transactions', exact: true }).click();
  await page
    .getByRole('button', { name: 'Add transaction', exact: true })
    .click();
  const dialog = page.getByRole('dialog', {
    name: 'Add transaction',
    exact: true,
  });
  const bounds = await dialog.boundingBox();
  expect(bounds.x).toBeGreaterThanOrEqual(15);
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(375);
  expect(bounds.y).toBeGreaterThanOrEqual(15);
  expect(bounds.y + bounds.height).toBeLessThanOrEqual(829);
  await fillTransaction(dialog, {
    transactionDate: '2026-10-25',
    type: 'EXPENSE',
    amount: '0.01',
    sourceAccountId: BANK_ID,
    categoryId: EXPENSE_ID,
    description: 'Saved from a narrow screen',
  });
  const save = dialog.getByRole('button', {
    name: 'Add transaction',
    exact: true,
  });
  await save.scrollIntoViewIfNeeded();
  await expect(save).toBeInViewport();
  await save.click();
  await expect(dialog).toBeHidden();
  await expect(
    page.getByRole('row').filter({ hasText: 'Saved from a narrow screen' }),
  ).toContainText('0.01');
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    )
    .toBe(true);
  expect(state.writes.at(-1).body.amount).toBe('0.01');
});
