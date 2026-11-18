import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import React from 'react';
import { projectPermissions } from './projects.js';
import { transformWithOxc } from 'vite';
import { createTaskLoader, loadTaskDetail, saveTask, writeAndRefresh, singleFlight, historyValue, textLengthError } from './task-flows.js';

function taskResponse(path) {
  const id=Number(path.split('/')[2]);
  if(path.endsWith('/eligible-users'))return {items:[{user:{id:8,name:'Member'},projectRole:id===20?'MEMBRO':'GESTOR'}]};
  if(path.startsWith('/projects/'))return {id};
  if(path.endsWith('/comments')||path.endsWith('/history'))return {items:[]};
  return {id,projectId:id*10,responsibleUserIds:[8]};
}

test('task deep-link detail waits for matching project permissions before atomic publication',async()=>{
  let resolveContext;const publications=[],paths=[];
  const loader=createTaskLoader(async path=>{
    paths.push(path);
    if(path==='/projects/20/eligible-users')return new Promise(resolve=>{resolveContext=resolve;});
    return taskResponse(path);
  },view=>publications.push(view));
  const pending=loader.load(2,8);
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(publications.length,0);
  resolveContext(taskResponse('/projects/20/eligible-users'));
  await pending;
  assert.equal(publications.length,1);
  assert.equal(publications[0].task.projectId,publications[0].project.id);
  assert.equal(publications[0].role,'MEMBRO');
  assert.deepEqual(publications[0].users,[{id:8,name:'Member'}]);
  assert.ok(!paths.some(path=>path.includes('/projects/20/tasks')));
});

for(const fail of [false,true]){
  test(`superseded task context ${fail?'failure':'success'} cannot overwrite a newer task`,async()=>{
    let finish;const publications=[];
    const loader=createTaskLoader(async path=>{
      if(path==='/projects/10/eligible-users')return new Promise((resolve,reject)=>{finish=()=>fail?reject(new Error('Old error')):resolve(taskResponse(path));});
      return taskResponse(path);
    },view=>publications.push(view));
    const old=loader.load(1,8),rejected=assert.rejects(old,{name:'AbortError'});
    await new Promise(resolve=>setImmediate(resolve));
    await loader.load(2,8);
    finish();await rejected;
    assert.equal(publications.length,1);
    assert.equal(publications[0].task.id,2);
    assert.equal(publications[0].role,'MEMBRO');
  });
}

test('failed project permission fetch never publishes task detail or inferred privileges',async()=>{
  const publications=[];
  const loader=createTaskLoader(async path=>{
    if(path.endsWith('/eligible-users'))throw new Error('Forbidden');
    return taskResponse(path);
  },view=>publications.push(view));
  await assert.rejects(loader.load(1,8),/Forbidden/);
  assert.deepEqual(publications,[]);
});

test('invalidating task loading prevents late publication after logout or unmount',async()=>{
  let finish;const publications=[];
  const loader=createTaskLoader(async path=>{
    if(path==='/tasks/1')return new Promise(resolve=>{finish=()=>resolve(taskResponse(path));});
    return taskResponse(path);
  },view=>publications.push(view));
  const pending=loader.load(1,8),rejected=assert.rejects(pending,{name:'AbortError'});
  loader.invalidate();finish();await rejected;
  assert.deepEqual(publications,[]);
});

for(const failedPart of ['/tasks/1','/projects/10','/projects/10/eligible-users']) {
  test(`failed refresh at ${failedPart} revokes cached permissions, preserves read-only data and confirmed-write outcome`,async()=>{
    let view,permissions,fail=false,writes=0;
    const paths=[];
    const loader=createTaskLoader(async path=>{
      paths.push(path);
      if(fail&&path===failedPart)throw new Error('Unavailable');
      return taskResponse(path);
    },next=>{view=next;},next=>{permissions=next;});
    await loader.load(1,8);
    const original=view;
    assert.equal(permissions.verified,true);
    fail=true;
    const outcome=await writeAndRefresh(async()=>{writes++;},()=>loader.load(1,8));
    assert.deepEqual(outcome,{refreshFailed:true});
    assert.deepEqual(permissions,{verified:false,loading:false});
    assert.equal(view,original);
    fail=false;
    await loader.load(1,8);
    assert.deepEqual(permissions,{verified:true,loading:false});
    assert.equal(writes,1);
    assert.ok(paths.every(path=>path.startsWith('/tasks/')||path.startsWith('/projects/')));
  });
}

test('cached permissions become unverified immediately and a superseded failure cannot revoke newly validated context',async()=>{
  let permissions,finish,delay=false;
  const loader=createTaskLoader(async path=>{
    if(delay&&path==='/projects/10/eligible-users')return new Promise((resolve,reject)=>{finish=()=>reject(new Error('Old failure'));});
    return taskResponse(path);
  },()=>{},next=>{permissions=next;});
  await loader.load(1,8);
  delay=true;
  const old=loader.load(1,8),rejected=assert.rejects(old,{name:'AbortError'});
  assert.deepEqual(permissions,{verified:false,loading:true});
  await new Promise(resolve=>setImmediate(resolve));
  await loader.load(2,8);
  finish();await rejected;
  assert.deepEqual(permissions,{verified:true,loading:false});
});

test('permission recovery wiring fails closed, disables comments and preserves saved-write feedback during read-only retry',async()=>{
  const source=await readFile(new URL('./main.jsx',import.meta.url),'utf8');
  assert.match(source,/createTaskLoader\(request,setTaskView,setTaskPermissions\)/);
  assert.match(source,/projectPermissions\(route.type==='task'&&!taskPermissions.verified&&!\(taskPermissions.loading&&taskControlsVisible.current\)\?null:projectRole/);
  assert.match(source,/<fieldset disabled=\{!taskPermissions.verified\}/);
  assert.match(source,/<CommentForm busy=\{busy\|\|!taskPermissions.verified\}/);
  const retry=source.slice(source.indexOf('async function retryDataRefresh'),source.indexOf('useEffect(()=>{',source.indexOf('async function retryDataRefresh')));
  assert.match(retry,/await refreshTask\(\)/);
  assert.doesNotMatch(retry,/setSuccess|method:|writeAndRefresh/);
  assert.match(source,/onClick=\{revalidateTaskPermissions\}/);
});

test('confirmed project ID survives list failure and manual read recovery never repeats creation',async()=>{
  let createdId=null,writes=0,reads=0,fail=true;
  const refresh=async()=>{reads++;if(fail)throw new Error('List unavailable');};
  const result=await writeAndRefresh(async()=>{writes++;const project={id:42};createdId=project.id;return project;},refresh);
  assert.deepEqual(result,{refreshFailed:true});
  assert.equal(createdId,42);
  await assert.rejects(refresh(),/List unavailable/);
  fail=false;await refresh();
  assert.equal(writes,1);
  assert.equal(reads,3);
  assert.equal(createdId,42);
});

test('saved-write recovery retains warning, has read-only actions and blocks resubmission until refreshed',async()=>{
  const source=await readFile(new URL('./main.jsx',import.meta.url),'utf8');
  const retry=source.slice(source.indexOf('async function retryDataRefresh'),source.indexOf('const revalidateTaskPermissions'));
  assert.doesNotMatch(retry,/setSuccess|method:|writeAndRefresh|saveTask/);
  assert.match(retry,/route.type==='task'\)await refreshTask\(\)/);
  assert.match(retry,/route.type==='project'\)await openProject\(route.id\)/);
  assert.match(retry,/route.type==='projects'\)await loadProjects\(route.search\)/);
  assert.match(retry,/await loadDashboard\(\);assertActive\(\);setRefreshPending\(false\)/);
  assert.match(source,/if\(message&&\(refreshPending\|\|route.type==='task'&&!taskPermissions.verified\)\)return false/);
  assert.match(source,/<fieldset disabled=\{busy\|\|authBusy\|\|refreshPending\|\|route.type==='task'&&!taskPermissions.verified\}/);
  assert.match(source,/onClick=\{retryDataRefresh\}>Refresh data without resubmitting/);
  assert.match(source,/setCreatedProjectId\(p.id\);return p;\},async p=>\{await loadProjects\(\)/);
  assert.match(source,/href=\{routeHref\(\{type:'project',id:createdProjectId\}\)\}/);
  assert.match(source,/<ProjectForm key=\{creationVersion\}/);
});

for (const taskId of [null, 7]) {
  for (const failedRead of ['detail', 'dashboard']) {
    test(`confirmed task ${taskId == null ? 'creation' : 'edit'} remains successful after ${failedRead} refresh failure`, async () => {
      const calls = [];
      const refreshDetail = async () => {
        calls.push('detail');
        if (failedRead === 'detail') throw new Error('Read unavailable');
      };
      const result = await saveTask(async (path, options) => {
        calls.push([path, options]);
        return { id: 7 };
      }, 3, taskId, { title: 'Saved' }, {
        openProject: refreshDetail, refreshTask: refreshDetail,
        loadDashboard: async () => { calls.push('dashboard'); throw new Error('Dashboard unavailable'); },
      });
      assert.deepEqual(result, { refreshFailed: true });
      assert.deepEqual(calls, [
        [taskId == null ? '/projects/3/tasks' : '/tasks/7',
          { method: taskId == null ? 'POST' : 'PATCH', body: { title: 'Saved' } }],
        'detail', ...(failedRead === 'dashboard' ? ['dashboard'] : []),
      ]);
    });
  }
}

for (const mutation of ['project creation', 'project update', 'comment', 'task status', 'task priority', 'responsibles', 'deletion']) {
  test(`${mutation}: refresh failure preserves confirmed success without retrying the write`, async () => {
    let writes = 0, reads = 0;
    const response = { id: 42 };
    const result = await writeAndRefresh(async () => { writes++; return response; }, async received => {
      assert.equal(received, response);
      reads++;
      throw new Error('Read failed');
    });
    assert.deepEqual(result, { refreshFailed: true });
    assert.equal(writes, 1);
    assert.equal(reads, 1);
  });
}

test('successful write and refresh report no stale-data warning', async () => {
  assert.deepEqual(await writeAndRefresh(async () => null, async () => {}), { refreshFailed: false });
});

test('failed write propagates unchanged without refreshing or retrying', async () => {
  const error = new Error('Permission denied');
  let writes = 0;
  await assert.rejects(writeAndRefresh(async () => { writes++; throw error; },
    async () => assert.fail('must not refresh a rejected write')), received => received === error);
  assert.equal(writes, 1);
});

test('session abort during a post-write refresh is not converted into success feedback', async () => {
  const error = new DOMException('Session changed', 'AbortError');
  let writes = 0;
  await assert.rejects(writeAndRefresh(async () => { writes++; }, async () => { throw error; }),
    received => received === error);
  assert.equal(writes, 1);
});

test('duplicate-action lock covers the post-write refresh and releases after refresh failure', async () => {
  const flight = singleFlight();
  let release, writes = 0, result;
  const pending = flight(async () => {
    result = await writeAndRefresh(async () => { writes++; },
      () => new Promise((resolve, reject) => { release = () => reject(new Error('Read failed')); }));
  });
  // Let the confirmed write enter its deferred refresh.
  await Promise.resolve();
  assert.equal(await flight(async () => { writes++; }), false);
  release();
  assert.equal(await pending, true);
  assert.deepEqual(result, { refreshFailed: true });
  assert.equal(writes, 1);
  assert.equal(await flight(async () => {}), true);
});

test('mutation handlers return refresh outcomes and confirmed comments still clear their drafts', async () => {
  // Wiring guard alongside behavioral helper tests; this does not render React.
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /const result=await action\(\);assertActive\(\);setSuccess\(result\?\.refreshFailed/);
  assert.match(source, /The change was saved; do not submit it again/);
  assert.match(source, /saved=true;/);
  assert.match(source, /await onSave\(content\)\)setContent\(''\)/);
  assert.equal((source.match(/run\(\(\)=>writeAndRefresh\(/g) || []).length, 8);
  assert.match(source, /run\(async\(\)=>outcome=await saveTask\(request,project.id,null,data/);
  assert.match(source, /run\(\(\)=>saveTask\(request,project.id,task.id,data/);
});

test('home navigation is a native named button rather than a clickable heading', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.doesNotMatch(source, /<h[1-6][^>]*onClick=/);
  assert.match(source, /<h1><button type="button" className="brand" aria-label="Matriz — return to dashboard" onClick=\{goHome\}>Matriz<\/button><\/h1>/);
  assert.match(source, /<nav aria-label="Main navigation">/);
});

test('project search has a persistent visible label and named search landmark', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /role="search" aria-label="Project search"><label>Search projects by name<input id="project-search" type="search".*?value=\{search\}.*?\/><\/label>/);
});

test('project and task navigation actions include entity names without changing their handlers', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source,/href=\{routeHref\(\{type:'task',id:t.id\}\)\} aria-label=\{`Open task: \$\{t.title\}`\} onClick=\{followLink\}/);
  assert.match(source,/href=\{routeHref\(\{type:'project',id:task.projectId\}\)\} aria-label=\{`Back to project:/);
});

test('skip link precedes navigation and targets focusable content outside the disabled fieldset', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  const link = source.indexOf('<a className="skip-link" href="#main-content">Skip to content</a>');
  const header = source.indexOf('<header>');
  const target = source.indexOf('<div id="main-content" tabIndex={-1}>');
  const fieldset = source.indexOf('<fieldset disabled={busy||authBusy||refreshPending||');
  assert.ok(link > 0 && link < header && header < target && target < fieldset);
  assert.equal((source.match(/id="main-content"/g) || []).length, 1);
  assert.match(source, /<\/fieldset>\{busy&&<p role="status">Processing…<\/p>\}<\/>/);
  assert.match(source, /<div id="main-content" tabIndex=\{-1\}><div hidden=\{securityOpen\}>\{content\}<\/div>\{securityOpen&&<SecurityCenter\/>\}<\/div><\/main>/);
});

test('keyboard focus is visible on interactive controls and reveals the skip link', async () => {
  const css = await readFile(new URL('./style.css', import.meta.url), 'utf8');
  assert.match(css, /\.skip-link\{[^}]*position:fixed;[^}]*transform:translateY/);
  assert.match(css, /\.skip-link:focus\{transform:translateY\(0\)\}/);
  assert.match(css, /:where\(a,button,input,textarea,select,summary,\[tabindex\]\):focus-visible\{outline:3px solid var\(--focus\);outline-offset:3px;box-shadow:0 0 0 3px var\(--surface\)\}/);
  assert.doesNotMatch(css, /outline\s*:\s*(none|0)(?:;|\})/);
});

test('description/comment limits count stripped Unicode code points', () => {
  for (const max of [1000, 2000]) {
    for (const value of ['', ' ', '😀', '😀'.repeat(max), 'a' + '😀'.repeat(max - 1)]) {
      assert.equal(textLengthError(` \t\u2003${value}\u2003\n `, max), '');
    }
    assert.notEqual(textLengthError('😀'.repeat(max + 1), max), '');
    assert.notEqual(textLengthError('a'.repeat(max + 1), max), '');
    // Java strip does not remove non-breaking spaces or BOM characters.
    assert.notEqual(textLengthError('\u00a0' + '😀'.repeat(max), max), '');
    assert.notEqual(textLengthError('\ufeff' + '😀'.repeat(max), max), '');
  }
});

test('all three textareas use code-point custom validity instead of UTF-16 maxlength', async () => {
  // Structural wiring guard, not a rendered-browser test.
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  const textareas = source.match(/<textarea.*?\/>/g);
  assert.equal(textareas.length, 3);
  assert.ok(textareas.every(tag => !tag.includes('maxLength=')));
  assert.match(textareas[0], /setCustomValidity\(textLengthError\(description,1000\)\)/);
  assert.match(textareas[1], /setCustomValidity\(textLengthError\(description,2000\)\)/);
  assert.match(textareas[2], /required.*setCustomValidity\(textLengthError\(content,2000\)\)/);
});

test('task detail loads comments and typed history from the same task', async () => {
  const calls = [];
  const history = [{ changedByUserId: 2, field: 'responsibleUserIds', previousValue: [], newValue: [2] }];
  const result = await loadTaskDetail(async path => {
    calls.push(path);
    return path.endsWith('/history') ? { items: history } : path.endsWith('/comments') ? { items: [] } : { id: 7 };
  }, 7);
  assert.deepEqual(calls, ['/tasks/7', '/tasks/7/comments', '/tasks/7/history']);
  assert.deepEqual(result, { task: { id: 7 }, comments: [], history });
});

test('task creation reopens the relevant project before refreshing dashboard', async () => {
  const calls = [];
  await saveTask(async (path, options) => calls.push([path, options]), 3, null, { title: 'New' }, {
    openProject: async id => calls.push(['project', id]),
    refreshTask: async () => assert.fail('must not open task detail after creation'),
    loadDashboard: async () => calls.push(['dashboard']),
  });
  assert.deepEqual(calls, [['/projects/3/tasks', { method: 'POST', body: { title: 'New' } }], ['project', 3], ['dashboard']]);
});

test('task editing stays on detail and refreshes dashboard', async () => {
  const calls = [];
  await saveTask(async (path, options) => calls.push([path, options]), 3, 7, { title: 'Edited' }, {
    openProject: async () => assert.fail('must not leave detail'),
    refreshTask: async id => calls.push(['detail', id]), loadDashboard: async () => calls.push(['dashboard']),
  });
  assert.deepEqual(calls, [['/tasks/7', { method: 'PATCH', body: { title: 'Edited' } }], ['detail', 7], ['dashboard']]);
});

test('rejected creation or editing preserves screen and propagates validation and permission errors', async () => {
  const unexpected = async () => assert.fail('must not navigate or refresh after rejection');
  for (const taskId of [null, 7]) {
    for (const message of ['Invalid title', 'Permission denied']) {
      const error = new Error(message);
      await assert.rejects(saveTask(async () => { throw error; }, 3, taskId, {}, {
        openProject: unexpected, refreshTask: unexpected, loadDashboard: unexpected,
      }), received => received === error);
    }
  }
});

test('creation is wired to project navigation which clears filters and reloads the task list', async () => {
  // Structural guard for React wiring; these Node tests do not render a browser UI.
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /saveTask\(request,project.id,null,data,\{openProject:id=>projectLoader.open\(id,me.id,'',''\),refreshTask,loadDashboard\}\)/);
  assert.match(source, /if\(saved&&!outcome.refreshFailed&&\(statusFilter\|\|priorityFilter\)\)navigateAfterSave\(routeHref\(\{type:'project',id:project.id\}\),'Task created successfully.'\)/);
  assert.match(source, /setNotice\(\{href,message\}\);navigation.navigate\(href,\{replace:true\}\)/);
  const openProject = source.slice(source.indexOf('async function openProject(id)'), source.indexOf('async function loadTasks('));
  assert.match(openProject, /navigation.navigate\(destination\)/);
  assert.match(openProject, /setPage\(\{type:'project'\}\)/);
  assert.match(openProject, /await projectLoader.open\(id,me.id,statusFilter,priorityFilter\)/);
  assert.match(source, /await projectLoader.tasks\(id,s,pr\)/);
  assert.match(source, /project&&!projectView.loading&&!projectView.error&&<section>/);
  assert.match(source, /aria-busy=\{projectView.loading\}/);
  assert.match(source, /Unable to load project data/);
  assert.match(source, /return\(\)=>\{active=false;scope.invalidate\(\);projectLoader.invalidate\(\);taskLoader.invalidate\(\);\}/);
});

test('synchronous submission lock rejects duplicates and releases after success or failure', async () => {
  const run = singleFlight();
  let resolve, calls = 0;
  const pending = run(async () => { calls++; await new Promise(done => { resolve = done; }); });
  assert.equal(await run(async () => { calls++; }), false);
  resolve();
  assert.equal(await pending, true);
  assert.equal(calls, 1);
  await assert.rejects(run(async () => { throw new Error('failed'); }), /failed/);
  assert.equal(await run(async () => { calls++; }), true);
  assert.equal(calls, 2);
});

test('history formats empty/multiple assignments, absent users and enums', () => {
  const users = [{ id: 2, name: 'Member' }];
  assert.equal(historyValue([], users, {}, {}), 'Sem responsáveis');
  assert.equal(historyValue([2, 3], users, {}, {}), 'Member, #3');
  assert.equal(historyValue('TODO', users, { TODO: 'Por fazer' }, {}), 'Por fazer');
  assert.equal(historyValue('HIGH', users, {}, { HIGH: 'Alta' }), 'Alta');
});

test('priority refresh preserves unsaved responsible checkboxes; assignment save and navigation use server data', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  // Exercise the real JSX/event handlers with route-local hook state, without a DOM.
  const appSource = source.slice(source.indexOf('function App('), source.indexOf('createRoot(document'))
    .replace(/\[(\w+)(,\w+)?\]=useState\(/g, '[$1$2]=useState("$1",')
    .replace(/const (\w+)=useRef\(/g, 'const $1=useRef("$1",');
  const { code } = await transformWithOxc(appSource, 'main.jsx', { jsx: { runtime: 'classic' } });
  let server = { id: 1, projectId: 10, title: 'Task', status: 'TODO', priority: 'LOW', responsibleUserIds: [8] };
  let rejectAssignment = false, pauseRefresh, role = 'GESTOR', failRefresh = false;
  const users = [{ id: 8, name: 'First' }, { id: 9, name: 'Second' }];
  const view = () => ({ task: { ...server }, project: { id: 10 }, role: 'GESTOR', users, comments: [], history: [] });
  const request = async (path, options = {}) => {
    if (path.endsWith('/priority') || path.endsWith('/status')) { server = { ...server, ...options.body }; return server; }
    if (path.endsWith('/responsibles')) {
      if (rejectAssignment) throw new Error('Assignment rejected');
      // Deliberately return canonical server assignments, not the submitted draft.
      server = { ...server, responsibleUserIds: [9] }; return server;
    }
    if (path === '/tasks/1') return { ...server };
    if (path.endsWith('/comments') || path.endsWith('/history')) return { items: [] };
    if (path.endsWith('/eligible-users')) {
      if (pauseRefresh) await pauseRefresh;
      if (failRefresh) throw new Error('Permission refresh failed');
      return { items: users.map(user => ({ user, projectRole: role })) };
    }
    if (path === '/projects/10') return { id: 10 };
    if (path === '/dashboard') return {};
    assert.fail(`Unexpected request: ${path}`);
  };
  function mount() {
    const state = new Map([['loading', false], ['taskView', view()], ['taskPermissions', { verified: true, loading: false }]]);
    const useState = (name, initial) => {
      if (!state.has(name)) state.set(name, typeof initial === 'function' ? initial() : initial);
      return [state.get(name), value => state.set(name, typeof value === 'function' ? value(state.get(name)) : value)];
    };
    const App = new Function('React', 'useState', 'useEffect', 'useRef', 'createRouteScope', 'apiRequest', 'createProjectLoader', 'createTaskLoader', 'singleFlight', 'projectPermissions', 'writeAndRefresh', 'statuses', 'priorities', 'routeHref', 'historyValue', `${code}; return App;`)(
      React, useState, () => {}, (name, initial) => {
        if (!state.has(name)) state.set(name, { current: initial });
        return state.get(name);
      }, () => ({ request, assertActive() {} }), request,
      () => ({}), createTaskLoader, singleFlight, projectPermissions, writeAndRefresh,
      { TODO: 'Todo' }, { LOW: 'Low', HIGH: 'High' }, () => '/projects/10', historyValue);
    const render = () => App({ route: { type: 'task', id: 1 }, session: { user: { id: 8 } }, actionFlight: singleFlight() });
    const nodes = node => !node || typeof node !== 'object' ? [] : Array.isArray(node)
      ? node.flatMap(nodes) : [node, ...nodes(node.props?.children)];
    const form = () => {
      const element = nodes(render()).find(node => node.type?.name === 'Responsibles');
      return element.type(element.props);
    };
    return {
      elements: () => nodes(render()),
      permissions: value => state.set('taskPermissions', value),
      status: () => nodes(render()).find(node => node.type === 'select' && node.props.value === server.status).props.onChange({ target: { value: 'DONE' } }),
      checked: () => nodes(form()).filter(node => node.type === 'input').map(node => node.props.checked),
      toggle: (index, checked) => nodes(form()).filter(node => node.type === 'input')[index].props.onChange({ target: { checked } }),
      priority: () => nodes(render()).find(node => node.type === 'select' && node.props.value === server.priority).props.onChange({ target: { value: 'HIGH' } }),
      save: () => form().props.onSubmit({ preventDefault() {} }),
    };
  }
  const page = mount();
  page.toggle(0, false); page.toggle(1, true);
  assert.deepEqual(page.checked(), [false, true]);
  await page.priority();
  assert.deepEqual(server.responsibleUserIds, [8]);
  assert.deepEqual(page.checked(), [false, true]);
  page.toggle(1, false);
  await page.priority();
  assert.deepEqual(page.checked(), [false, false], 'an empty unsaved selection survives refresh');
  rejectAssignment = true;
  await page.save();
  assert.deepEqual(page.checked(), [false, false], 'failed saves preserve the draft');
  rejectAssignment = false;
  await page.save();
  assert.deepEqual(page.checked(), [false, true], 'successful saves use canonical server assignments');
  page.toggle(0, true);
  assert.deepEqual(mount().checked(), [false, true], 'navigation discards the previous route draft');
  assert.match(source, /<App key=\{`\$\{session.version\}:\$\{routeKey\}`\}/);
  assert.match(source, /const routeKey=.*routeHref\(route\)/);

  // Inspect intermediate refresh renders, not just the final refreshed view.
  for (const mutate of ['priority', 'status']) {
    const before = page.elements().filter(node => ['TaskForm', 'Responsibles', 'CommentForm'].includes(node.type?.name));
    let release;
    pauseRefresh = new Promise(resolve => { release = resolve; });
    const pending = page[mutate]();
    await new Promise(resolve => setImmediate(resolve));
    const during = page.elements();
    const forms = during.filter(node => ['TaskForm', 'Responsibles', 'CommentForm'].includes(node.type?.name));
    assert.deepEqual(forms.map(node => [node.type, node.key]), before.map(node => [node.type, node.key]), 'refresh retains component identities and keys');
    assert.ok(during.some(node => node.type === 'fieldset' && node.props.disabled && forms.every(form => nodesIn(node).includes(form))), 'all mutation controls are disabled while permissions are unverified');
    assert.equal(await forms.find(node => node.type.name === 'Responsibles').props.onSave([8]), false, 'unverified handlers cannot dispatch writes');
    assert.deepEqual(page.checked(), [true, true], 'draft remains visible during refresh');
    release(); await pending; pauseRefresh = null;
    assert.equal(page.elements().filter(node => node.type === 'select').length, 2);
  }

  function nodesIn(node) {
    return !node || typeof node !== 'object' ? [] : Array.isArray(node)
      ? node.flatMap(nodesIn) : [node, ...nodesIn(node.props?.children)];
  }
  // Confirmed role/assignment revocation must remove controls in the very next render.
  role = 'MEMBRO';
  await page.priority();
  assert.ok(!page.elements().some(node => ['TaskForm', 'Responsibles'].includes(node.type?.name)));
  assert.equal(page.elements().filter(node => node.type === 'select').length, 0);
  assert.ok(!page.elements().some(node => node.type === 'button' && node.props.children === 'Delete task'));

  const failedPage = mount();
  failRefresh = true;
  await failedPage.priority();
  assert.ok(!failedPage.elements().some(node => ['TaskForm', 'Responsibles'].includes(node.type?.name)), 'failed revalidation hides protected controls');
  const failedElements = failedPage.elements();
  const retry = failedElements.find(node => node.type === 'button' && node.props.children === 'Verify permissions and refresh task');
  assert.equal(retry.props.disabled, false, 'permission recovery remains available after verification fails');
  assert.ok(!failedElements.some(node => node.type === 'fieldset' && node.props.disabled && nodesIn(node).includes(retry)), 'recovery is outside every disabled fieldset');
  assert.ok(failedElements.some(node => node.type === 'fieldset' && node.props.disabled), 'mutation controls remain disabled');
  failRefresh = false;
  await retry.props.onClick();
  assert.ok(!failedPage.elements().some(node => node.type === 'button' && node.props.children === 'Verify permissions and refresh task'), 'retry successfully verifies permissions');
  failedPage.permissions({ verified: false, loading: false });
  failedPage.elements(); // Commit the failed-permission presentation before retry starts.
  failedPage.permissions({ verified: false, loading: true });
  assert.ok(!failedPage.elements().some(node => node.type?.name === 'Responsibles'), 'retry cannot resurrect controls hidden after failed validation');
});

test('authenticated shell stays unkeyed by route while content and private context reset at route/session boundaries', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  const shellSource = source.slice(source.indexOf('function AppShell('), source.indexOf('function App('));
  const { code } = await transformWithOxc(shellSource, 'shell.jsx', { jsx: { runtime: 'classic' } });
  const state = [], effects = [];
  let hookIndex = 0, effectIndex = 0, pendingEffects = [];
  function RouteContent() {}
  function SecurityCenter() {}
  const useShellState = initial => {
    const index = hookIndex++;
    if (!(index in state)) state[index] = initial;
    return [state[index], value => { state[index] = value; }];
  };
  const useShellEffect = (callback, deps) => {
    const index = effectIndex++, previous = effects[index];
    if (!previous || deps.some((value, i) => value !== previous.deps[i])) {
      pendingEffects.push(() => {
        previous?.cleanup?.();
        effects[index] = { deps, cleanup: callback() };
      });
    }
  };
  let restored = 0, focused = 0;
  const document = { title: 'Task — Matriz', activeElement: { isConnected: true, focus() { restored++; } }, getElementById: () => ({ focus() { focused++; } }) };
  const Shell = new Function('React', 'useState', 'useEffect', 'document', 'routeHref', 'App', 'ThemeSelector', 'SecurityCenter', `${code}; return AppShell;`)(
    React, useShellState, useShellEffect, document, route => route.type === 'task' ? `/tasks/${route.id}` : route.type === 'projects' ? `/projects?search=${route.search || ''}` : '/', RouteContent, function ThemeSelector() {}, SecurityCenter);
  const destinations = [];
  const followedLinks = [];
  let snapshot;
  const navigation = { getSnapshot: () => snapshot, navigate(path) { destinations.push(path); }, handleClick(event) { followedLinks.push(event); } };
  const session = {
  version: 1,
  user: {
    name: 'First account',
    securityAdmin: true
  }
};
  const render = (route, currentSession = session) => {
    if (JSON.stringify(snapshot) !== JSON.stringify(route)) snapshot = route;
    hookIndex = 0; effectIndex = 0; pendingEffects = [];
    const tree = Shell({ route: snapshot, navigation, session: currentSession });
    pendingEffects.forEach(effect => effect());
    return tree;
  };
  const nodes = node => !node || typeof node !== 'object' ? [] : Array.isArray(node)
    ? node.flatMap(nodes) : [node, ...nodes(node.props?.children)];
  const content = tree => nodes(tree).find(node => node.type === RouteContent);
  const layout = tree => nodes(tree).filter(node => ['main', 'header', 'nav'].includes(node.type) || node.props?.id === 'main-content');
  const first = render({ type: 'task', id: 1 });
  assert.equal(hookIndex, 3, 'authenticated renders retain context, mutation notice and local view state');
  nodes(first).find(node => node.type === 'button' && node.props.children === 'Security Center').props.onClick();
  const security = render({ type: 'task', id: 1 });
  assert.ok(nodes(security).some(node => node.type === SecurityCenter));
  assert.equal(content(security).type, content(first).type);
  assert.equal(content(security).key, content(first).key, 'Security Center preserves the mounted route and pending writes');
  assert.equal(content(security).props.active, false);
  assert.ok(nodes(security).some(node => node.type === 'div' && node.props.hidden && node.props.children === content(security)));
  assert.equal(nodes(security).filter(node => node.props?.['aria-current'] === 'page').length, 1);
  assert.equal(document.title, 'Security Center — Matriz');
  assert.equal(focused, 1, 'opening Security Center focuses the persistent content target');
  assert.deepEqual(layout(security).map(node => [node.type, node.key]), layout(first).map(node => [node.type, node.key]));
  nodes(security).find(node => node.type === 'button' && node.props.children === 'Dashboard').props.onClick();
  assert.deepEqual(destinations, ['/']);
  assert.ok(content(render({ type: 'home' })), 'dashboard navigation closes Security Center');
  nodes(first).find(node => node.type === 'button' && node.props.children === 'Security Center').props.onClick();
  const securityAgain = render({ type: 'task', id: 1 });
  const linkEvent = { button: 0 };
  nodes(securityAgain).find(node => node.type === 'a' && node.props.children === 'Projects').props.onClick(linkEvent);
  assert.deepEqual(followedLinks, [linkEvent]);
  assert.ok(content(render({ type: 'projects' })), 'project navigation closes Security Center');
  // Returning to the same route does not remount App, but restores focus.
  nodes(render({ type: 'home' })).find(node => node.type === 'button' && node.props.children === 'Security Center').props.onClick();
  const homeSecurity = render({ type: 'home' });
  nodes(homeSecurity).find(node => node.type === 'button' && node.props.children === 'Dashboard').props.onClick();
  const restoredHome = render({ type: 'home' });
  assert.equal(restored, 1);
  assert.equal(content(restoredHome).props.active, true);
  nodes(restoredHome).find(node => node.type === 'button' && node.props.children === 'Security Center').props.onClick();
  render({ type: 'home' });
  // A changed snapshot models navigation publication, including popstate, with no click handler.
  const popped = render({ type: 'task', id: 1 });
  assert.ok(!nodes(popped).some(node => node.type === SecurityCenter), 'Back/Forward closes Security Center immediately');
  assert.equal(content(popped).props.active, true);
  assert.equal(restored, 1, 'route navigation must not restore focus to the old view');
  content(first).props.onProject({ id: 10, name: 'Private project' });
  assert.ok(nodes(render({ type: 'task', id: 1 })).some(node => node.props?.children === 'Private project'));
  const second = render({ type: 'task', id: 2 });
  assert.deepEqual(layout(second).map(node => [node.type, node.key]), layout(first).map(node => [node.type, node.key]));
  assert.notEqual(content(first).key, content(second).key, 'route content remounts to reset drafts and scopes');
  const frame = layout(second).find(node => node.props?.id === 'main-content');
  assert.ok(nodes(frame).includes(content(second)), 'the loading route remains inside the persistent focus target');
  assert.ok(!nodes(second).some(node => node.props?.children === 'Private project'), 'old project context is hidden immediately');
  assert.notEqual(content(render({ type: 'projects', search: 'a' })).key, content(render({ type: 'projects', search: 'b' })).key);
  const logout = render({ type: 'home' }, { version: 2, user: null });
  assert.equal(hookIndex, 3, 'unauthenticated renders retain the same hook order');
  assert.equal(logout.type, RouteContent);
  assert.equal(layout(logout).length, 0, 'logout immediately removes authenticated shell');
  const account = render({ type: 'task', id: 1 }, { version: 3, user: { name: 'Other account' } });
  assert.notEqual(content(account).key, content(first).key);
  assert.ok(nodes(account).some(node => node.props?.children === 'Other account'));
  // Real session-keyed ownership clears the shell context, including same-route account switches.
  assert.match(source, /<AppShell key=\{session.version\}/);
  assert.match(source, /if\(loading\)return <section aria-busy="true"><p role="status" aria-live="polite">/);
  assert.doesNotMatch(shellSource, /if\(loading\)|createRouteScope|taskLoader/);
  assert.match(source, /scope.activate\(\)/);
  assert.match(source, /scope.invalidate\(\);projectLoader.invalidate\(\);taskLoader.invalidate\(\)/);
});

test('hidden App does not overwrite the Security Center title or move focus, and restores its title on return', async () => {
  const source = (await readFile(new URL('./main.jsx', import.meta.url), 'utf8')).replace(/\r\n/g, '\n');
  const effectStart = '  useEffect(()=>{\n    if(!active)return;';
  const start = source.indexOf(effectStart);
  assert.notEqual(start, -1, 'found the active route title effect');
  const end = source.indexOf('  useEffect(()=>{', start + effectStart.length);
  assert.ok(end > start, 'found the following effect');
  const effect = source.slice(start, end);
  assert.match(effect, /\[active,loading,me,project\?\.name,task\?\.title\]/);
  let present;
  const document = { title: 'Security Center — Matriz' };
  const presented = { current: false };
  let ready = 0;
  const run = new Function('useEffect', 'active', 'document', 'pageTitle', 'route', 'loading', 'me', 'project', 'task', 'presented', 'accessibility', 'session', 'navigation', effect);
  const render = active => run(callback => { present = callback; }, active, document, () => 'Task — Matriz', { type: 'task', id: 1 }, false, { id: 8 }, null, { title: 'Task' }, presented, { ready() { ready++; } }, { version: 1 }, { getTransition: () => 'push' });
  render(false); present();
  assert.equal(document.title, 'Security Center — Matriz');
  assert.equal(ready, 0);
  render(true); present();
  assert.equal(document.title, 'Task — Matriz');
  assert.equal(ready, 1, 'a route that finished loading while hidden is presented on return');
  render(false); present(); render(true); present();
  assert.equal(ready, 1, 'returning to an already presented route does not override restored focus');
});

test('project names and task titles validate stripped code points at both API boundaries', async () => {
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  const helper = source.slice(source.indexOf('function requiredTextLengthError('), source.indexOf('function ProjectForm('));
  const validate = new Function('textLengthError', `${helper}; return requiredTextLengthError;`)(textLengthError);
  for (const [field, max] of [['name', 100], ['title', 150]]) {
    for (const value of ['', '  ', 'ab', '😀😀', ' \t😀😀\u2003', '😀'.repeat(max + 1)]) assert.notEqual(validate(value, max), '');
    for (const value of ['abc', '😀😀😀', ' \t😀😀😀\u2003', '😀'.repeat(max), '\u00a0ab', '\ufeffab']) assert.equal(validate(value, max), '');
    const input = source.match(new RegExp(`<input required ref=.*?value=\\{${field}\\}.*?/>`))?.[0];
    assert.ok(input, `found ${field} input`);
    assert.ok(input.includes(`setCustomValidity(requiredTextLengthError(${field},${max}))`));
    assert.doesNotMatch(input, /minLength|maxLength/);
  }
});

test('entity forms are keyed by identity to discard another entity’s draft', async () => {
  // Structural guard; browser interaction is not covered by the Node-only test setup.
  const source = await readFile(new URL('./main.jsx', import.meta.url), 'utf8');
  assert.match(source, /<ProjectForm key=\{project.id\} initial=\{project\}/);
  assert.match(source, /<TaskForm key=\{task.id\} initial=\{task\}/);
});
