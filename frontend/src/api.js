const base = '/api/v1';
let csrf = null;
let sessionVersion = 0;
export const getSessionVersion = () => sessionVersion;

function assertCurrent(version) {
  if (version !== sessionVersion) throw new DOMException('Session changed', 'AbortError');
}
function sessionChanged(user = null, expired = false) {
  sessionVersion++;
  csrf = null;
  window.dispatchEvent(Object.assign(new Event('matriz:session'), {
    detail: { user, version: sessionVersion, expired },
  }));
}
export async function request(path, { method = 'GET', body, sessionVersion: version = sessionVersion, beforeDispatch, anonymousBootstrap = false, ...options } = {}) {
  assertCurrent(version);
  if (method !== 'GET' && !csrf) await refreshCsrf(version);
  assertCurrent(version);
  // Route ownership can change while obtaining CSRF. Check before dispatch,
  // without cancelling or replaying writes that have already reached fetch.
  if (method !== 'GET') beforeDispatch?.();
  const response = await fetch(base + path, {
    method, credentials: 'same-origin',
    headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(method !== 'GET' && csrf ? { 'X-CSRF-TOKEN': csrf } : {}) },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}), ...options,
  });
  assertCurrent(version);
  if (response.status === 401 && anonymousBootstrap && path === '/users/me') return null;
  if (response.status === 401 && path !== '/auth/login') {
    sessionChanged(null, true);
    window.dispatchEvent(new Event('matriz:expired'));
    throw new DOMException('Session expired', 'AbortError');
  }
  if (!response.ok) {
    let detail; try { detail = await response.json(); } catch { /* empty response */ }
    assertCurrent(version);
    if (response.status === 401 && path === '/auth/login')
      throw new Error('Não foi possível iniciar sessão. Verifica os dados e tenta novamente.');
    const fields = Array.isArray(detail?.errors) ? detail.errors
      .filter(item => typeof item?.message === 'string')
      .map(item => `${item.field ? item.field + ': ' : ''}${item.message}`).join(' ') : '';
    throw new Error([detail?.detail || detail?.error, fields].filter(Boolean).join(' ') || `Pedido recusado (HTTP ${response.status})`);
  }
  const result = response.status === 204 ? null : await response.json();
  assertCurrent(version);
  return result;
}
export async function refreshCsrf(version = sessionVersion) {
  assertCurrent(version);
  const r = await fetch(base + '/auth/csrf', { credentials: 'same-origin' });
  assertCurrent(version);
  if (!r.ok) throw new Error('Não foi possível obter CSRF');
  const data = await r.json();
  assertCurrent(version);
  csrf = data.token;
}
export async function login(username, password) {
  sessionChanged();
  const version = sessionVersion;
  await refreshCsrf(version);
  const result = await request('/auth/login', { method: 'POST', body: { username, password }, sessionVersion: version });
  assertCurrent(version);
  // Authentication is confirmed. Invalidate the pre-login token and let the next
  // write initialize CSRF; a token-fetch failure must not undo login success.
  sessionChanged(result);
  return result;
}
export async function logout() {
  sessionChanged();
  const version = sessionVersion;
  await request('/auth/logout', { method: 'POST', sessionVersion: version });
  assertCurrent(version);
  csrf = null;
}
