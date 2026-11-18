import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parseRoute, routeHref, createNavigation, createRouteScope } from './navigation.js';

for (const failure of [false, true]) {
  test(`route scope discards late ${failure ? 'failures' : 'results'} and prevents follow-up requests`, async () => {
    const browser = browserAt('/projects?search=old'), nav = createNavigation(browser);
    let finish, calls = 0;
    const scope = createRouteScope(nav,nav.getSnapshot(),()=>{
      calls++;
      return new Promise((resolve,reject)=>{finish=()=>failure?reject(new Error('Unavailable')):resolve({items:['old']});});
    });
    scope.activate();
    const pending = scope.request('/projects?search=old');
    const rejected = assert.rejects(pending,{name:'AbortError'});
    nav.navigate('/projects?search=new');
    finish();
    await rejected;
    await assert.rejects(scope.request('/dashboard'),{name:'AbortError'});
    assert.equal(calls,1);
  });
}

test('task history restores deep links and old scopes stay invalid even before React cleanup',async()=>{
  const browser=browserAt('/projects/1?status=DONE'),nav=createNavigation(browser);
  nav.subscribe(()=>{});
  nav.navigate('/tasks/10');
  let finish,writes=0,redirects=0;
  const scope=createRouteScope(nav,nav.getSnapshot(),()=>new Promise(resolve=>{writes++;finish=resolve;}));
  scope.activate();
  const pending=scope.request('/tasks/10',{method:'DELETE'}).then(()=>{scope.assertActive();redirects++;nav.navigate('/projects/1');});
  const rejected=assert.rejects(pending,{name:'AbortError'});
  nav.navigate('/tasks/20');
  browser.history.back();
  assert.equal(nav.getSnapshot().id,10);
  finish(null);await rejected;
  assert.equal(writes,1);assert.equal(redirects,0);
  browser.history.back();assert.equal(nav.getSnapshot().status,'DONE');
  browser.history.forward();assert.equal(nav.getSnapshot().id,10);
  scope.invalidate();
});

test('invalidated route scope stays invalid after Back returns to the same URL', async () => {
  const browser=browserAt('/projects/1'),nav=createNavigation(browser);
  const scope=createRouteScope(nav,nav.getSnapshot(),async()=>({id:1}));
  scope.activate();
  scope.invalidate();
  nav.navigate('/');
  browser.history.back();
  await assert.rejects(scope.request('/projects/1'),{name:'AbortError'});
});

test('active route scope preserves session aborts and mutation request options without retries', async () => {
  const nav=createNavigation(browserAt('/projects/1'));
  const options={method:'POST',body:{title:'Once'}};
  let calls=0;
  const scope=createRouteScope(nav,nav.getSnapshot(),async(path,received)=>{
    calls++;assert.equal(received.method,options.method);assert.equal(received.body,options.body);received.beforeDispatch();throw new DOMException('Session changed','AbortError');
  });
  scope.activate();
  await assert.rejects(scope.request('/projects/1/tasks',options),{name:'AbortError'});
  assert.equal(calls,1);
});

test('render-time scope construction has no subscriptions or request side effects', async () => {
  let subscriptions=0, calls=0;
  const nav={getSnapshot:()=>parseRoute('/'),subscribe:()=>{subscriptions++;return()=>{subscriptions--;};}};
  // Includes a discarded initializer, as in an abandoned or StrictMode render.
  createRouteScope(nav,nav.getSnapshot(),()=>{calls++;});
  const scope=createRouteScope(nav,nav.getSnapshot(),()=>{calls++;});
  assert.equal(subscriptions,0);
  await assert.rejects(scope.request('/dashboard'),{name:'AbortError'});
  assert.equal(calls,0);
  scope.activate();scope.activate();
  assert.equal(subscriptions,1);
  scope.invalidate();scope.invalidate();
  assert.equal(subscriptions,0);
  scope.activate();
  assert.equal(subscriptions,1);
  scope.invalidate();
  assert.equal(subscriptions,0);
});

for (const failure of [false,true]) {
  test(`effect replay rejects old ${failure?'failures':'results'} while allowing new requests`,async()=>{
    const nav=createNavigation(browserAt('/tasks/10'));
    let finish;
    const scope=createRouteScope(nav,nav.getSnapshot(),path=>path==='/old'
      ?new Promise((resolve,reject)=>{finish=()=>failure?reject(new Error('Old failure')):resolve('old');})
      :Promise.resolve('new'));
    scope.activate();
    const rejected=assert.rejects(scope.request('/old'),{name:'AbortError'});
    scope.invalidate();scope.activate();
    assert.equal(await scope.request('/new'),'new');
    finish();await rejected;
    scope.invalidate();
  });
}

test('effect replay prevents an old deferred mutation dispatch without retrying',async()=>{
  const nav=createNavigation(browserAt('/tasks/10'));
  let dispatch, writes=0, hooks=0;
  const scope=createRouteScope(nav,nav.getSnapshot(),async(path,options)=>{
    await new Promise(resolve=>{dispatch=resolve;});
    options.beforeDispatch();writes++;
  });
  scope.activate();
  const rejected=assert.rejects(scope.request('/tasks/10',{method:'DELETE',beforeDispatch:()=>{hooks++;}}),{name:'AbortError'});
  scope.invalidate();scope.activate();
  dispatch();await rejected;
  assert.equal(writes,0);assert.equal(hooks,0);
  scope.invalidate();
});

test('activation checks a route changed between render and effect setup',async()=>{
  const nav=createNavigation(browserAt('/tasks/10'));
  let calls=0;
  const scope=createRouteScope(nav,nav.getSnapshot(),async()=>{calls++;});
  nav.navigate('/tasks/20');
  scope.activate();
  nav.navigate('/tasks/10');
  await assert.rejects(scope.request('/tasks/10'),{name:'AbortError'});
  assert.equal(calls,0);
  scope.invalidate();
});

test('React integration gates route reads behind authentication and loads task deep links with matching project context', async () => {
  const source=await readFile(new URL('./main.jsx',import.meta.url),'utf8');
  assert.match(source,/useSyncExternalStore\(navigation.subscribe,navigation.getSnapshot\)/);
  assert.match(source,/key=\{`\$\{session.version\}:/);
  assert.match(source,/else if\(!session.user\)return;\s*if\(!currentUser\)return;\s*if\(route.type==='home'\)await loadDashboard\(\)/);
  assert.match(source,/route.type==='projects'\)await loadProjects\(route.search\)/);
  assert.match(source,/projectLoader.open\(route.id,currentUser.id,route.status,route.priority\)/);
  assert.match(source,/onBootstrap\(user\)/);
  assert.match(source,/useEffect\(\(\)=>\{\s*scope.activate\(\);\s*let active=true;/);
  assert.match(source,/scope.invalidate\(\);projectLoader.invalidate\(\)/);
  assert.match(source,/sessionVersion:session.version/);
  assert.match(source,/navigation.navigate\(routeHref\(\{type:'projects',search\}\)\)/);
  assert.match(source,/const applyFilters=.*navigation.navigate\(routeHref/);
  assert.match(source,/href=\{routeHref\(\{type:'project',id:p.id\}\)\}/);
  assert.match(source,/onClick=\{followLink\}/);
  assert.match(source,/route.type==='task'\)await taskLoader.load\(route.id,currentUser.id\)/);
  assert.match(source,/const context=route.type==='task'\?taskView:projectView/);
  assert.match(source,/assertActive\(\);navigateAfterSave\(routeHref\(\{type:'project',id:task.projectId\}\),'Task deleted successfully.'\)/);
  assert.doesNotMatch(source,/Ligação de tarefa ainda não suportada/);
  assert.match(source,/Invalid page or filters/i);
});

function browserAt(path = '/') {
  const events = new EventTarget(), entries = [new URL(path, 'https://matriz.test')];
  let index = 0;
  const browser = {
    get location() { return entries[index]; },
    addEventListener: events.addEventListener.bind(events),
    removeEventListener: events.removeEventListener.bind(events),
    history: {
      pushState(state, unused, href) {
        assert.equal(state, null);
        entries.splice(index + 1);
        entries.push(new URL(href, browser.location)); index++;
      },
      replaceState(state, unused, href) {
        assert.equal(state, null);
        entries[index] = new URL(href, browser.location);
      },
      back() { if (index) { index--; events.dispatchEvent(new Event('popstate')); } },
      forward() { if (index + 1 < entries.length) { index++; events.dispatchEvent(new Event('popstate')); } },
      get length() { return entries.length; },
    },
  };
  return browser;
}
function click(href, overrides = {}, attributes = {}) {
  const attrs = { href, ...attributes };
  const anchor = {
    rel: attrs.rel || '',
    getAttribute: key => attrs[key],
    hasAttribute: key => Object.hasOwn(attrs, key),
  };
  return {
    button: 0, defaultPrevented: false,
    target: { closest: selector => { assert.equal(selector, 'a[href]'); return anchor; } },
    preventDefault() { this.defaultPrevented = true; }, ...overrides,
  };
}

for (const [href, route] of [
  ['/', { type: 'home' }],
  ['/projects', { type: 'projects', search: '' }],
  ['/projects?search=a%26b+%C3%A1', { type: 'projects', search: 'a&b á' }],
  ['/projects/42', { type: 'project', id: 42, status: '', priority: '' }],
  ['/projects/42?status=TODO&priority=HIGH', { type: 'project', id: 42, status: 'TODO', priority: 'HIGH' }],
  ['/tasks/123', { type: 'task', id: 123 }],
  ['/#main-content', { type: 'home' }],
  ['/tasks/123#main-content', { type: 'task', id: 123 }],
]) {
  test(`parses ${href}`, () => assert.deepEqual(parseRoute(href), route));
}

for (const href of ['/unknown', '/dashboard', '/projects/', '/tasks', '/tasks/1/extra',
  '/projects/0', '/projects/-1', '/projects/+1', '/projects/01', '/tasks/1.5',
  '/tasks/42abc', '/tasks/1e3', '/tasks/%31', '/tasks/9007199254740992']) {
  test(`rejects unknown route or malformed ID: ${href}`, () => {
    assert.deepEqual(parseRoute(href), { type: 'invalid', reason: 'route' });
  });
}
for (const href of ['/projects?search=a&search=b', '/projects?search=&search=',
  '/projects/1?status=TODO&status=DONE', '/projects/1?priority=&priority=HIGH',
  '/projects/1?status=todo', '/projects/1?priority=URGENT', '/projects/1?search=a',
  '/projects?status=TODO', '/?search=a', '/tasks/1?priority=LOW', '/projects?unknown=x',
  '/projects?%73earch=a&search=b']) {
  test(`rejects invalid or duplicate parameters: ${href}`, () => {
    assert.deepEqual(parseRoute(href), { type: 'invalid', reason: 'query' });
  });
}

test('canonical serialization omits empty filters and safely encodes search', () => {
  assert.equal(routeHref(parseRoute('/projects/1?priority=&status=')), '/projects/1');
  assert.equal(routeHref(parseRoute('/projects?search=++hello++')), '/projects?search=hello');
  const route = { type: 'projects', search: 'a&b / ? # á' };
  assert.deepEqual(parseRoute(routeHref(route)), route);
  assert.equal(routeHref(parseRoute('/projects/1?priority=LOW&status=DONE')), '/projects/1?status=DONE&priority=LOW');
});

test('serializer rejects invalid IDs, route types and filters', () => {
  for (const route of [{ type: 'invalid' }, { type: 'task', id: 0 },
    { type: 'project', id: Number.MAX_SAFE_INTEGER + 1 },
    { type: 'project', id: 1, status: 'bad' }, { type: 'project', id: 1, priority: 'bad' }]) {
    assert.throws(() => routeHref(route), TypeError);
  }
});

test('initial deep link, push, replace, Back and Forward publish stable snapshots', () => {
  const browser = browserAt('/projects/7?status=DONE');
  const nav = createNavigation(browser), published = [];
  assert.equal(nav.getSnapshot().id, 7);
  assert.equal(nav.getSnapshot(), nav.getSnapshot());
  const stop = nav.subscribe(() => published.push(nav.getSnapshot()));
  assert.equal(nav.navigate('/tasks/2'), true);
  assert.equal(browser.history.length, 2);
  nav.navigate('/projects?search=foo', { replace: true });
  assert.equal(browser.history.length, 2);
  browser.history.back();
  assert.equal(nav.getSnapshot().status, 'DONE');
  browser.history.forward();
  assert.equal(nav.getSnapshot().search, 'foo');
  assert.equal(published.length, 4);
  stop();
  browser.history.back();
  assert.equal(published.length, 4);
  nav.subscribe(() => {});
  assert.equal(nav.getSnapshot().id, 7);
});

test('same destination and canonicalization never create duplicate history entries', () => {
  const browser = browserAt('/projects/1?priority=HIGH&status=TODO');
  const nav = createNavigation(browser);
  nav.navigate('/projects/1?status=TODO&priority=HIGH');
  assert.equal(browser.location.search, '?status=TODO&priority=HIGH');
  nav.navigate('/projects/1?status=TODO&priority=HIGH');
  assert.equal(browser.history.length, 1);
});

test('query-only navigation is restored by Back without pushing another entry', () => {
  const browser = browserAt('/projects/1'), nav = createNavigation(browser);
  nav.subscribe(() => {});
  nav.navigate('?status=DONE');
  assert.equal(nav.getSnapshot().status, 'DONE');
  browser.history.back();
  assert.equal(nav.getSnapshot().status, '');
  assert.equal(browser.history.length, 2);
});

test('unsafe or unrecognized destinations do not change location', () => {
  const browser = browserAt('/'), nav = createNavigation(browser);
  for (const href of ['https://other.test/projects', '//other.test/projects', 'javascript:alert(1)',
    'https://user:password@matriz.test/projects', '/bad', '/tasks/0', '/projects?bad=1', '#main-content']) {
    assert.equal(nav.navigate(href), false);
  }
  assert.equal(browser.history.length, 1);
  assert.equal(browser.location.pathname, '/');
});

test('ordinary nested anchor clicks navigate and prevent default', () => {
  const browser = browserAt('/'), nav = createNavigation(browser);
  const event = click('/tasks/12');
  const handler = nav.handleClick;
  assert.equal(handler(event), true);
  assert.equal(event.defaultPrevented, true);
  assert.equal(browser.location.pathname, '/tasks/12');
});

test('modified clicks, non-primary clicks and already-handled events stay native', () => {
  const browser = browserAt('/'), nav = createNavigation(browser);
  for (const override of [{ ctrlKey: true }, { metaKey: true }, { shiftKey: true },
    { altKey: true }, { button: 1 }, { button: 2 }, { defaultPrevented: true }]) {
    assert.equal(nav.handleClick(click('/projects', override)), false);
  }
  assert.equal(nav.handleClick({ button: 0, target: {} }), false);
  assert.equal(browser.history.length, 1);
});

test('fragment, external, targeted and download links stay native', () => {
  const browser = browserAt('/'), nav = createNavigation(browser);
  for (const [href, attrs] of [
    ['#main-content', {}], ['#', {}], ['/projects#main-content', {}],
    ['https://other.test/projects', {}], ['/projects', { target: '_blank' }],
    ['/projects', { target: '_self' }], ['/projects', { download: '' }],
    ['/projects', { rel: 'external noopener' }],
  ]) {
    const event = click(href, {}, attrs);
    assert.equal(nav.handleClick(event), false);
    assert.equal(event.defaultPrevented, false);
  }
  assert.equal(browser.history.length, 1);
});

test('skip-link fragment history does not publish an application route transition', () => {
  const browser = browserAt('/projects'), nav = createNavigation(browser);
  let transitions = 0;
  nav.subscribe(() => { transitions++; });
  browser.history.pushState(null, '', '#main-content');
  browser.history.back();
  browser.history.forward();
  assert.equal(transitions, 0);
});
