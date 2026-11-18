import test, { beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { request, login, logout, refreshCsrf, getSessionVersion } from './api.js';
import { singleFlight } from './task-flows.js';
import { createNavigation, createRouteScope } from './navigation.js';

const originalFetch = globalThis.fetch;
const originalWindow = globalThis.window;
let events;
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { 'Content-Type': 'application/json' },
});
const deferred = () => {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
};
beforeEach(() => {
  globalThis.window = new EventTarget();
  events = [];
  window.addEventListener('matriz:session', event => events.push(event.detail));
});
afterEach(() => {
  globalThis.fetch = originalFetch;
  globalThis.window = originalWindow;
});

async function signIn(user = { id: 2, name: 'Next account' }) {
  globalThis.fetch = async url => json(url.endsWith('/auth/login') ? user : { token: 'csrf-new' });
  await login('next', 'password');
}

test('login resets the session before authentication and again for the new account', async () => {
  await signIn();
  assert.equal(events.length, 2);
  assert.equal(events[0].user, null);
  assert.deepEqual(events[1].user, { id: 2, name: 'Next account' });
  assert.equal(events[1].version, events[0].version + 1);
});

for (const failure of ['http', 'network', 'json']) {
  test(`confirmed login survives ${failure} failure initializing CSRF for the next write and recovers without repeating login`, async () => {
    const user = { id: 7, name: 'Confirmed account' }, calls = [];
    let csrfCalls = 0, recovered = false;
    globalThis.fetch = async (url, options) => {
      calls.push([url, options]);
      if (url.endsWith('/auth/login')) return json(user);
      if (url.endsWith('/auth/csrf')) {
        csrfCalls++;
        if (csrfCalls === 1) return json({ token: 'pre-login' });
        if (recovered) return json({ token: 'authenticated-token' });
        if (failure === 'network') throw new TypeError('Offline');
        if (failure === 'json') return new Response('not-json', { status: 200 });
        return new Response(null, { status: 503 });
      }
      if (url.endsWith('/dashboard')) return json({ totalProjects: 1 });
      if (url.endsWith('/comments')) return json({ id: 10 });
      assert.fail(`Unexpected request: ${url}`);
    };
    assert.deepEqual(await login('confirmed', 'temporary-password'), user);
    assert.equal(csrfCalls, 1, 'login must not depend on post-login CSRF availability');
    assert.deepEqual(events.at(-1).user, user);
    const version = getSessionVersion(), eventCount = events.length;
    assert.deepEqual(await request('/dashboard'), { totalProjects: 1 });
    const options = { method: 'POST', body: { content: 'Once' } };
    await assert.rejects(request('/tasks/1/comments', options));
    assert.equal(calls.filter(([url]) => url.endsWith('/comments')).length, 0);
    assert.equal(getSessionVersion(), version);
    assert.equal(events.length, eventCount);
    // Recovery is an explicit later action, never an automatic replay of login or the write.
    recovered = true;
    assert.deepEqual(await request('/tasks/1/comments', options), { id: 10 });
    const writes = calls.filter(([url]) => url.endsWith('/comments'));
    assert.equal(writes.length, 1);
    assert.equal(writes[0][1].headers['X-CSRF-TOKEN'], 'authenticated-token');
    assert.equal(writes[0][1].credentials, 'same-origin');
    assert.equal(calls.filter(([url]) => url.endsWith('/auth/login')).length, 1);
  });
}

test('late authenticated CSRF initialization cannot cross logout or send its pending write', async () => {
  await signIn();
  const pending = deferred(), started = deferred();
  let writes = 0;
  globalThis.fetch = async url => {
    if (url.endsWith('/auth/csrf')) { started.resolve(); return pending.promise; }
    writes++;
    return json({});
  };
  const old = request('/tasks/1/comments', { method: 'POST', body: { content: 'Stale' } });
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await started.promise;
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'logout-token' }) : new Response(null, { status: 204 });
  await logout();
  pending.resolve(json({ token: 'old-authenticated-token' }));
  await rejected;
  assert.equal(writes, 0);
  assert.equal(events.at(-1).user, null);
});

for (const method of ['POST', 'PATCH', 'PUT', 'DELETE']) {
  test(`${method} is not dispatched if its route is abandoned while waiting for CSRF`, async () => {
    await signIn();
    const browser = new EventTarget();
    browser.location = new URL('https://matriz.test/tasks/1');
    browser.history = { pushState: (state, title, href) => { browser.location = new URL(href, browser.location); } };
    const nav = createNavigation(browser), version = getSessionVersion();
    const scope = createRouteScope(nav, nav.getSnapshot(), (path, options) => request(path, { ...options, sessionVersion: version }));
    scope.activate();
    const token = deferred(), started = deferred();
    let writes = 0, csrfReads = 0;
    globalThis.fetch = async url => {
      if (url.endsWith('/auth/csrf')) { csrfReads++; started.resolve(); return token.promise; }
      writes++;
      return json({ id: 1 });
    };
    const pending = scope.request('/tasks/1', { method, body: { title: 'Once' } });
    const rejected = assert.rejects(pending, { name: 'AbortError' });
    await started.promise;
    nav.navigate('/projects');
    token.resolve(json({ token: 'fresh' }));
    await rejected;
    assert.equal(writes, 0);
    assert.equal(csrfReads, 1);
    assert.equal(getSessionVersion(), version);
    scope.invalidate();
  });
}

test('active route dispatches exactly once after CSRF with unchanged payload and no internal hook in fetch options', async () => {
  await signIn();
  const nav = createNavigation({ location: new URL('https://matriz.test/tasks/1'), addEventListener() {}, removeEventListener() {} });
  const scope = createRouteScope(nav, nav.getSnapshot(), request);
  scope.activate();
  const calls = [];
  let checked = 0;
  globalThis.fetch = async (url, options) => {
    calls.push([url, options]);
    if (url.endsWith('/auth/csrf')) return json({ token: 'fresh' });
    assert.equal(checked, 1);
    assert.equal(Object.hasOwn(options, 'beforeDispatch'), false);
    assert.equal(options.credentials, 'same-origin');
    assert.equal(options.headers['X-CSRF-TOKEN'], 'fresh');
    assert.equal(options.body, JSON.stringify({ content: 'Once' }));
    return json({ id: 5 });
  };
  assert.deepEqual(await scope.request('/tasks/1/comments', {
    method: 'POST', body: { content: 'Once' }, beforeDispatch: () => { checked++; },
  }), { id: 5 });
  assert.equal(calls.length, 2);
  scope.invalidate();
});

test('navigation after dispatch does not abort or repeat a write, but suppresses its stale completion', async () => {
  await signIn();
  const browser = new EventTarget();
  browser.location = new URL('https://matriz.test/tasks/1');
  browser.history = { pushState: (state, title, href) => { browser.location = new URL(href, browser.location); } };
  const nav = createNavigation(browser), scope = createRouteScope(nav, nav.getSnapshot(), request);
  scope.activate();
  const response = deferred(), started = deferred();
  let writes = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/auth/csrf')) return json({ token: 'fresh' });
    writes++;assert.equal(options.signal, undefined);started.resolve();return response.promise;
  };
  const pending = scope.request('/tasks/1', { method: 'PATCH', body: { title: 'Once' } });
  const rejected = assert.rejects(pending, { name: 'AbortError' });
  await started.promise;
  nav.navigate('/projects');
  response.resolve(json({ id: 1 }));
  await rejected;
  assert.equal(writes, 1);
  scope.invalidate();
});

test('logout clears user state immediately even when the network fails', async () => {
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const result = logout();
  assert.equal(events.length, 1);
  assert.equal(events[0].user, null);
  pending.resolve(new Response(null, { status: 503 }));
  await assert.rejects(result);
  assert.equal(events.length, 1);
});

for (const logoutFails of [false, true]) {
  test(`logout bypasses a pending mutation after navigation, clears immediately and ${logoutFails ? 'allows safe retry after failure' : 'completes with fresh CSRF'}`, async () => {
    await signIn();
    const browser = new EventTarget();
    browser.location = new URL('https://matriz.test/tasks/1');
    browser.history = { pushState: (state, title, href) => { browser.location = new URL(href, browser.location); } };
    const navigation = createNavigation(browser);
    const version = getSessionVersion();
    const scope = createRouteScope(navigation, navigation.getSnapshot(), (path, options) => request(path, { ...options, sessionVersion: version }));
    scope.activate();
    const mutation = deferred(), started = deferred(), logoutResponse = deferred();
    const calls = [];
    globalThis.fetch = async (url, options) => {
      calls.push([url, options]);
      if (url.endsWith('/auth/csrf')) return json({ token: 'logout-fresh' });
      if (url.endsWith('/comments')) { started.resolve(); return mutation.promise; }
      if (url.endsWith('/auth/logout')) return logoutResponse.promise;
      assert.fail(`Unexpected request: ${url}`);
    };
    const actionFlight = singleFlight(), authFlight = singleFlight();
    const pendingWrite = actionFlight(async () => {
      await assert.rejects(scope.request('/tasks/1/comments', { method: 'POST', body: { content: 'Once' } }), { name: 'AbortError' });
    });
    await started.promise;
    navigation.navigate('/projects');
    // The original shared mutation lock is still occupied on the new route.
    assert.equal(await actionFlight(async () => assert.fail('mutation lock must remain held')), false);
    events.length = 0;
    const signingOut = authFlight(logout);
    assert.equal(events.length, 1);
    assert.equal(events[0].user, null);
    assert.ok(getSessionVersion() > version);
    const completion = logoutFails ? assert.rejects(signingOut) : signingOut;
    logoutResponse.resolve(new Response(null, { status: logoutFails ? 503 : 204 }));
    await completion;
    const logoutCall = calls.find(([url]) => url.endsWith('/auth/logout'));
    assert.equal(logoutCall[1].credentials, 'same-origin');
    assert.equal(logoutCall[1].headers['X-CSRF-TOKEN'], 'logout-fresh');
    assert.equal(logoutCall[1].method, 'POST');
    if (logoutFails) {
      globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'retry-fresh' }) : new Response(null, { status: 204 });
      assert.equal(await authFlight(logout), true);
      assert.equal(events.at(-1).user, null);
    }
    // A late successful write response cannot restore local state or issue follow-up reads.
    mutation.resolve(json({ id: 10 }));
    assert.equal(await pendingWrite, true);
    assert.equal(calls.filter(([url]) => url.endsWith('/comments')).length, 1);
    scope.invalidate();
  });
}

test('both logout controls bypass mutation busy state and retain the separate auth lock', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /async function signOut\(\) \{return authFlight\(async\(\)=>/);
  assert.equal((source.match(/disabled=\{authBusy\} onClick=\{signOut\}/g) || []).length, 2);
  assert.doesNotMatch(source, /run\(signOut\)/);
  assert.match(source, /onClick=\{signOut\}>Log out/);
});

test('late project response cannot restore data after logout', async () => {
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const old = request('/projects');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'logout' }) : new Response(null, { status: 204 });
  await logout();
  pending.resolve(json([{ id: 1, name: 'Private project' }]));
  await rejected;
});

test('late decoded task body cannot restore data after account switch', async () => {
  const body = deferred();
  const decoding = deferred();
  globalThis.fetch = async () => ({ ok: true, status: 200, json: () => { decoding.resolve(); return body.promise; } });
  const old = request('/tasks/1');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await decoding.promise;
  await signIn();
  body.resolve({ title: 'Old private task', responsibleUserIds: [1] });
  await rejected;
});

test('stale action cannot issue a follow-up request using the new session', async () => {
  const oldVersion = getSessionVersion();
  await signIn();
  let calls = 0;
  globalThis.fetch = async () => { calls++; return json({}); };
  await assert.rejects(request('/projects/1/tasks', { sessionVersion: oldVersion }), { name: 'AbortError' });
  assert.equal(calls, 0);
});

test('401 clears the current session and invalidates other pending reads', async () => {
  const pending = deferred();
  globalThis.fetch = url => url.endsWith('/dashboard') ? pending.promise : Promise.resolve(json({}, 401));
  const old = request('/dashboard');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await assert.rejects(request('/users/me'), { name: 'AbortError' });
  assert.equal(events.length, 1);
  assert.equal(events[0].user, null);
  assert.equal(events[0].expired, true);
  pending.resolve(json({ projects: [{ name: 'Previous account' }] }));
  await rejected;
});

test('late 401 from an old account cannot expire the new account', async () => {
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const old = request('/projects');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await signIn();
  pending.resolve(json({}, 401));
  await rejected;
  assert.equal(events.length, 2);
  assert.equal(events[1].user.id, 2);
});

test('late CSRF response cannot overwrite the new account token', async () => {
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const old = refreshCsrf();
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await signIn();
  pending.resolve(json({ token: 'old-secret' }));
  await rejected;
  // Session transition clears CSRF; the next write must bootstrap its own token.
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push([url, options]);
    return url.endsWith('/auth/csrf') ? json({ token: 'current-token' }) : json({});
  };
  await request('/projects', { method: 'POST', body: { name: 'New' } });
  assert.equal(calls[0][0], '/api/v1/auth/csrf');
  assert.equal(calls[1][1].headers['X-CSRF-TOKEN'], 'current-token');
});

test('invalid credentials remain a normal error, not a session expiry', async () => {
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'token' }) : json({ error: 'Invalid credentials' }, 401);
  await assert.rejects(login('wrong', 'password'), { message: 'Não foi possível iniciar sessão. Verifica os dados e tenta novamente.' });
  assert.equal(events.length, 1);
  assert.equal(events[0].expired, false);
});

test('Problem Details preserve summary and field validation messages', async () => {
  globalThis.fetch = async () => json({ detail: 'There are validation errors.', errors: [
    { field: 'name', message: 'Use at least 3 characters.' }, { field: 'description', message: 'Too long.' },
  ] }, 422);
  await assert.rejects(request('/projects'), {
    message: 'There are validation errors. name: Use at least 3 characters. description: Too long.',
  });
  assert.equal(events.length, 0);
});

test('login Problem Details 401 remains generic and never signals expiry', async () => {
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'token' })
    : json({ detail: 'Internal account-specific detail' }, 401);
  await assert.rejects(login('wrong', 'password'), {
    message: 'Não foi possível iniciar sessão. Verifica os dados e tenta novamente.',
  });
  assert.equal(events.length, 1);
  assert.equal(events[0].expired, false);
});

test('empty error response has a safe HTTP fallback', async () => {
  globalThis.fetch = async () => new Response(null, { status: 503 });
  await assert.rejects(request('/projects'), { message: 'Pedido recusado (HTTP 503)' });
});

test('401 on logout is also treated as session expiry', async () => {
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ token: 'token' }) : json({}, 401);
  await assert.rejects(logout(), { name: 'AbortError' });
  assert.equal(events.at(-1).expired, true);
  assert.equal(events.at(-1).user, null);
});

test('stale login response cannot replace a newer account', async () => {
  const pending = deferred();
  const started = deferred();
  globalThis.fetch = async url => {
    if (url.endsWith('/auth/csrf')) return json({ token: 'first' });
    started.resolve();
    return pending.promise;
  };
  const old = login('first', 'password');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await started.promise;
  await signIn({ id: 3, name: 'Current account' });
  pending.resolve(json({ id: 1, name: 'Previous account' }));
  await rejected;
  assert.equal(events.at(-1).user.id, 3);
});

for (const failure of ['csrf', 'network', 'http']) {
  test(`logout ${failure} failure clears local session and a manual retry uses fresh CSRF and cookies`, async () => {
    await signIn();
    events.length = 0;
    let posts = 0;
    globalThis.fetch = async url => {
      if (url.endsWith('/auth/csrf')) return failure === 'csrf' ? new Response(null, { status: 503 }) : json({ token: 'first' });
      posts++;
      if (failure === 'network') throw new TypeError('Network unavailable');
      return new Response(null, { status: 503 });
    };
    const oldVersion = getSessionVersion();
    await assert.rejects(logout());
    assert.equal(events.length, 1);
    assert.equal(events[0].user, null);
    assert.ok(getSessionVersion() > oldVersion);
    assert.equal(posts, failure === 'csrf' ? 0 : 1);
    await assert.rejects(request('/projects', { sessionVersion: oldVersion }), { name: 'AbortError' });
    const calls = [];
    globalThis.fetch = async (url, options) => {
      calls.push([url, options]);
      return url.endsWith('/auth/csrf') ? json({ token: 'retry-token' }) : new Response(null, { status: 204 });
    };
    await logout();
    assert.equal(calls.length, 2);
    assert.equal(calls[0][0], '/api/v1/auth/csrf');
    assert.equal(calls[1][0], '/api/v1/auth/logout');
    assert.equal(calls[1][1].method, 'POST');
    assert.equal(calls[1][1].credentials, 'same-origin');
    assert.equal(calls[1][1].headers['X-CSRF-TOKEN'], 'retry-token');
    assert.equal(events.at(-1).user, null);
  });
}

test('late logout failure cannot clear or expire a newer authenticated session', async () => {
  const pending = deferred(), started = deferred();
  globalThis.fetch = async url => {
    if (url.endsWith('/auth/csrf')) return json({ token: 'logout-token' });
    started.resolve();
    return pending.promise;
  };
  const old = logout();
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await started.promise;
  await signIn({ id: 99, name: 'New session' });
  const version = getSessionVersion(), count = events.length;
  pending.resolve(new Response(null, { status: 503 }));
  await rejected;
  assert.equal(getSessionVersion(), version);
  assert.equal(events.length, count);
  assert.equal(events.at(-1).user.id, 99);
});

test('login credentials are only sent in the login body, never in session events', async () => {
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push([url, options]);
    return url.endsWith('/auth/csrf') ? json({ token: 'csrf' }) : json({}, 401);
  };
  await assert.rejects(login('preserved-user', 'temporary-password'));
  assert.deepEqual(JSON.parse(calls[1][1].body), { username: 'preserved-user', password: 'temporary-password' });
  assert.equal(calls[1][0], '/api/v1/auth/login');
  assert.equal(calls[1][1].credentials, 'same-origin');
  assert.equal(calls[1][1].headers['X-CSRF-TOKEN'], 'csrf');
  assert.equal(JSON.stringify(events).includes('temporary-password'), false);
});

test('auth UI retains only username across remounts and clears password on submission', async () => {
  // Wiring guard: Node tests do not render React or exercise browser autofill.
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  const root = source.slice(source.indexOf('function SessionRoot()'), source.indexOf('function App('));
  const app = source.slice(source.indexOf('function App('));
  assert.match(root, /\[username,setUsername\]=useState\(''\)/);
  assert.doesNotMatch(root, /\[password,setPassword\]/);
  assert.match(root, /<AppShell key=\{session.version\}.*username=\{username\}/);
  assert.match(root, /<App key=\{`\$\{session.version\}:\$\{routeKey\}`\}/);
  assert.doesNotMatch(app, /\[username,setUsername\]=useState/);
  assert.match(app, /run\(\(\)=>authenticate\(username,password\)\);setPassword\(''\)/);
  assert.match(app, /disabled=\{busy\|\|authBusy\} type="password"/);
  assert.doesNotMatch(source, /localStorage|sessionStorage/);
});

test('logout warning is session-scoped, remains separate from login errors and offers manual recovery', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /e.name!=='AbortError'&&version===getSessionVersion\(\)\)setLogoutUnconfirmed\(true\)/);
  assert.match(source, /await pending;if\(version===getSessionVersion\(\)\)setLogoutUnconfirmed\(false\)/);
  assert.match(source, /if\(e.detail.user\|\|e.detail.expired\).*setLogoutUnconfirmed\(false\);setUsername\(''\)/);
  assert.match(source, /setAuthBusy\(true\);setUsername\(''\);/);
  assert.match(source, /logoutUnconfirmed&&<section><p role="alert"/);
  assert.match(source, /the server could not confirm logout/);
  assert.match(source, /Do not refresh the page/);
  assert.match(source, /clear this site’s cookies and data/);
  assert.match(source, /disabled=\{authBusy\} onClick=\{signOut\}>Try logging out again/);
});

test('current requests preserve body, CSRF, cookies and returned data', async () => {
  globalThis.fetch = async () => json({ token: 'token' });
  await refreshCsrf();
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/v1/tasks/1/comments');
    assert.equal(options.credentials, 'same-origin');
    assert.equal(options.headers['X-CSRF-TOKEN'], 'token');
    assert.equal(options.body, JSON.stringify({ content: 'Comment' }));
    return json({ id: 3 });
  };
  assert.deepEqual(await request('/tasks/1/comments', { method: 'POST', body: { content: 'Comment' } }), { id: 3 });
});


test('anonymous initial bootstrap 401 is not a session expiry', async () => {
  const version = getSessionVersion();
  globalThis.fetch = async url => {
    assert.ok(url.endsWith('/users/me'));
    return new Response(null, { status: 401 });
  };
  assert.equal(await request('/users/me', { anonymousBootstrap: true }), null);
  assert.equal(getSessionVersion(), version);
  assert.equal(events.length, 0);
});

test('authenticated users/me 401 still expires the session', async () => {
  await signIn();
  globalThis.fetch = async () => new Response(null, { status: 401 });
  await assert.rejects(request('/users/me'), { name: 'AbortError' });
  assert.equal(events.at(-1).expired, true);
});
