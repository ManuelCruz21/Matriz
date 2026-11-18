import test from 'node:test';
import assert from 'node:assert/strict';
import { fetchHealth } from './health.js';

const response = (status, body) => new Response(JSON.stringify(body), {
  status,
  headers: { 'Content-Type': 'application/json' },
});
const errorMessage = 'Não foi possível consultar o estado do serviço.';

test('requests the same-origin health endpoint and returns UP', async () => {
  const controller = new AbortController();
  const result = await fetchHealth({
    signal: controller.signal,
    fetchImpl: async (url, options) => {
      assert.equal(url, '/api/health');
      assert.equal(options.method, 'GET');
      assert.deepEqual(options.headers, { Accept: 'application/json' });
      assert.equal(options.cache, 'no-store');
      assert.equal(options.signal, controller.signal);
      return response(200, { status: 'UP', details: 'not exposed' });
    },
  });
  assert.deepEqual(result, { status: 'UP' });
});

test('treats a contractual 503 DOWN as an unavailable service, not a fetch error', async () => {
  assert.deepEqual(await fetchHealth({
    fetchImpl: async () => response(503, { status: 'DOWN' }),
  }), { status: 'DOWN' });
});

for (const [status, body] of [
  [200, { status: 'DOWN' }],
  [503, { status: 'UP' }],
  [500, { status: 'DOWN', detail: 'secret' }],
  [401, { message: 'secret' }],
  [200, {}],
  [200, null],
  [200, { status: 'UNKNOWN' }],
]) {
  test(`rejects unexpected response ${status} ${JSON.stringify(body)}`, async () => {
    await assert.rejects(fetchHealth({
      fetchImpl: async () => response(status, body),
    }), { message: errorMessage });
  });
}

test('rejects invalid JSON without exposing the response', async () => {
  await assert.rejects(fetchHealth({
    fetchImpl: async () => new Response('sensitive diagnostic', { status: 200 }),
  }), { message: errorMessage });
});

test('sanitizes network errors', async () => {
  await assert.rejects(fetchHealth({
    fetchImpl: async () => { throw new Error('sensitive diagnostic'); },
  }), { message: errorMessage });
});

test('preserves cancellation for callers', async () => {
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(fetchHealth({
    signal: controller.signal,
    fetchImpl: async (_url, { signal }) => { signal.throwIfAborted(); },
  }), { name: 'AbortError' });
});
