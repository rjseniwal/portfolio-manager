import test from 'node:test';
import assert from 'node:assert/strict';
import { apiRequest, ApiError } from '../../src/shared/api/client.js';

test('conflicts retain backend explanation and error code', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({ code: 'CONFLICT', message: 'Account has transactions' }),
      { status: 409 },
    );
  try {
    await assert.rejects(
      apiRequest('/api/test'),
      (error) =>
        error instanceof ApiError &&
        error.status === 409 &&
        error.code === 'CONFLICT' &&
        error.message === 'Account has transactions',
    );
  } finally {
    globalThis.fetch = original;
  }
});

test('successful JSON requests forward cancellation and include JSON headers', async () => {
  const original = globalThis.fetch;
  const controller = new AbortController();
  globalThis.fetch = async (url, options) => {
    assert.equal(options.signal, controller.signal);
    assert.equal(options.headers['Content-Type'], 'application/json');
    return new Response(JSON.stringify({ id: 'saved' }));
  };
  try {
    assert.deepEqual(
      await apiRequest('/api/test', {
        method: 'POST',
        json: { name: 'Cash' },
        signal: controller.signal,
      }),
      { id: 'saved' },
    );
  } finally {
    globalThis.fetch = original;
  }
});
