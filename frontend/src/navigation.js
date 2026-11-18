const statuses = new Set(['TODO', 'IN_PROGRESS', 'DONE']);
const priorities = new Set(['LOW', 'MEDIUM', 'HIGH']);
const parseBase = 'http://localhost';

function validId(value) {
  return /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value));
}

// Pure parser: fragments are document anchors, never application routes.
export function parseRoute(input) {
  let url;
  try { url = new URL(input, parseBase); } catch { return { type: 'invalid', reason: 'route' }; }
  const path = url.pathname;
  let route, allowed;
  if (path === '/') {
    route = { type: 'home' }; allowed = [];
  } else if (path === '/projects') {
    route = { type: 'projects', search: '' }; allowed = ['search'];
  } else {
    const match = /^\/(projects|tasks)\/([^/]+)$/.exec(path);
    if (!match || !validId(match[2])) return { type: 'invalid', reason: 'route' };
    route = match[1] === 'projects'
      ? { type: 'project', id: Number(match[2]), status: '', priority: '' }
      : { type: 'task', id: Number(match[2]) };
    allowed = route.type === 'project' ? ['status', 'priority'] : [];
  }
  const seen = new Set();
  for (const [key, value] of url.searchParams) {
    if (!allowed.includes(key) || seen.has(key)) return { type: 'invalid', reason: 'query' };
    seen.add(key);
    if (key === 'status' && value && !statuses.has(value)) return { type: 'invalid', reason: 'query' };
    if (key === 'priority' && value && !priorities.has(value)) return { type: 'invalid', reason: 'query' };
    route[key] = key === 'search' ? value.trim() : value;
  }
  return route;
}

export function routeHref(route) {
  let path;
  const params = new URLSearchParams();
  switch (route.type) {
    case 'home': path = '/'; break;
    case 'projects':
      path = '/projects';
      if (route.search?.trim()) params.set('search', route.search.trim());
      break;
    case 'project':
      if (!validId(String(route.id))) throw new TypeError('Invalid project ID');
      if (route.status && !statuses.has(route.status)) throw new TypeError('Invalid status');
      if (route.priority && !priorities.has(route.priority)) throw new TypeError('Invalid priority');
      path = `/projects/${route.id}`;
      if (route.status) params.set('status', route.status);
      if (route.priority) params.set('priority', route.priority);
      break;
    case 'task':
      if (!validId(String(route.id))) throw new TypeError('Invalid task ID');
      path = `/tasks/${route.id}`;
      break;
    default: throw new TypeError('Invalid route');
  }
  return path + (params.size ? `?${params}` : '');
}

// Route ownership complements (but never replaces) the API's session-version checks.
export function createRouteScope(navigation, route, request) {
  let active = false, generation = 0, unsubscribe;
  const currentKey = value => value.type === 'invalid' ? value : routeHref(value);
  function assertActive(token = generation) {
    if (!active || token !== generation || currentKey(navigation.getSnapshot()) !== currentKey(route))
      throw new DOMException('Route changed', 'AbortError');
  }
  return {
    // Construction is render-safe; only a committed effect may subscribe.
    activate() {
      if (unsubscribe) return;
      generation++;
      active = true;
      unsubscribe = navigation.subscribe(() => {
        if (currentKey(navigation.getSnapshot()) !== currentKey(route)) active = false;
      });
      if (currentKey(navigation.getSnapshot()) !== currentKey(route)) active = false;
    },
    assertActive,
    invalidate() {
      active = false;
      generation++;
      unsubscribe?.();
      unsubscribe = undefined;
    },
    async request(path, options) {
      const token = generation;
      assertActive(token);
      try {
        const result = await request(path, { ...options, beforeDispatch: () => {
          assertActive(token);
          options?.beforeDispatch?.();
        } });
        assertActive(token);
        return result;
      } catch (error) {
        assertActive(token);
        throw error;
      }
    },
  };
}

// No fetching, authentication, or React state lives here. Subscribe before navigating.
export function createNavigation(browser = window) {
  const listeners = new Set();
  const locationKey = () => browser.location.pathname + browser.location.search;
  let key = locationKey();
  let snapshot = parseRoute(browser.location.href);
  let transition = 'initial';
  function publish(kind = 'push') {
    transition = kind;
    key = locationKey();
    snapshot = parseRoute(browser.location.href);
    for (const listener of listeners) listener();
  }
  function onPopState() {
    // Anchor-only history changes (e.g. the skip link) are native browser behavior.
    if (key !== locationKey()) publish('pop');
  }
  const navigation = {
    getSnapshot: () => snapshot,
    getTransition: () => transition,
    subscribe(listener) {
      if (!listeners.size) {
        browser.addEventListener('popstate', onPopState);
        if (key !== locationKey()) publish('pop');
      }
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
        if (!listeners.size) browser.removeEventListener('popstate', onPopState);
      };
    },
    navigate(href, { replace = false } = {}) {
      let url;
      try { url = new URL(href, browser.location.href); } catch { return false; }
      if (url.origin !== browser.location.origin || url.username || url.password || url.hash) return false;
      const route = parseRoute(url.href);
      if (route.type === 'invalid') return false;
      const target = routeHref(route);
      const current = parseRoute(browser.location.href);
      const sameRoute = current.type !== 'invalid' && routeHref(current) === target;
      // Canonicalization and repeated destinations must not add history entries.
      if (sameRoute && locationKey() === target && !browser.location.hash) return true;
      browser.history[replace || sameRoute ? 'replaceState' : 'pushState'](null, '', target);
      publish();
      return true;
    },
    handleClick(event) {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return false;
      const anchor = event.target?.closest?.('a[href]');
      if (!anchor || anchor.hasAttribute('download') || anchor.hasAttribute('target') || anchor.hasAttribute('rel') && anchor.rel.split(/\s+/).includes('external')) return false;
      const href = anchor.getAttribute('href');
      // Never hijack fragments, including an empty fragment (#).
      if (!href || href.includes('#')) return false;
      if (!navigation.navigate(href)) return false;
      event.preventDefault();
      return true;
    },
  };
  return navigation;
}
