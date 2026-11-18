import test from 'node:test';
import assert from 'node:assert/strict';
import { createProjectLoader, loadProjectContext, projectPermissions, updateProject } from './projects.js';

test('project deep-link filters are applied to the first atomic load without an unfiltered request', async () => {
  const paths=[];let view;
  const loader=createProjectLoader(async path=>{paths.push(path);return projectRequest(path);},next=>{view=next;});
  await loader.open(2,1,'DONE','HIGH');
  assert.deepEqual(paths,['/projects/2','/projects/2/eligible-users','/projects/2/tasks?status=DONE&priority=HIGH']);
  assert.equal(view.project.id,2);
  assert.equal(view.tasks[0].id,20);
});

function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
function projectRequest(path) {
  const id = Number(path.split('/')[2]);
  if (path.includes('/tasks?')) return { items: [{ id: id * 10 }], counts: { TODO: id } };
  if (path.endsWith('/eligible-users')) return { items: users };
  return { id, name: `Project ${id}` };
}

test('switching projects immediately clears old context, tasks and counts and commits the new view atomically', async () => {
  let view;
  const pending = deferred();
  const loader = createProjectLoader(path => path === '/projects/2/tasks?' ? pending.promise : Promise.resolve(projectRequest(path)), next => { view = next; });
  await loader.open(1, 1);
  assert.equal(view.tasks[0].id, 10);
  const opening = loader.open(2, 1);
  assert.equal(view.id, 2);
  assert.equal(view.loading, true);
  assert.equal(view.project, null);
  assert.equal(view.role, null);
  assert.deepEqual(view.users, []);
  assert.deepEqual(view.tasks, []);
  assert.deepEqual(view.counts, {});
  await Promise.resolve();
  assert.equal(view.project, null);
  pending.resolve(projectRequest('/projects/2/tasks?'));
  await opening;
  assert.equal(view.project.id, 2);
  assert.equal(view.tasks[0].id, 20);
  assert.deepEqual(view.counts, { TODO: 2 });
  assert.equal(view.loading, false);
});

for (const failedPart of ['tasks?', 'eligible-users', 'context']) {
  test(`failed ${failedPart} load leaves a project-specific error without previous data and can be retried`, async () => {
    let view, fail = false;
    const loader = createProjectLoader(async path => {
      if (fail && (failedPart === 'context' ? path === '/projects/2' : path.endsWith(failedPart))) throw new Error('Unavailable');
      return projectRequest(path);
    }, next => { view = next; });
    await loader.open(1, 1);
    fail = true;
    await assert.rejects(loader.open(2, 1), /Unavailable/);
    assert.equal(view.id, 2);
    assert.equal(view.error, 'Unavailable');
    assert.equal(view.loading, false);
    assert.equal(view.project, null);
    assert.deepEqual(view.tasks, []);
    assert.deepEqual(view.counts, {});
    fail = false;
    await loader.open(2, 1);
    assert.equal(view.error, '');
    assert.equal(view.project.id, 2);
  });
}

for (const lateFailure of [false, true]) {
  test(`late project ${lateFailure ? 'failure' : 'success'} cannot replace the latest project`, async () => {
    let view;
    const pending = deferred();
    const loader = createProjectLoader(path => path === '/projects/1/tasks?' ? pending.promise : Promise.resolve(projectRequest(path)), next => { view = next; });
    const old = loader.open(1, 1);
    const rejected = assert.rejects(old, { name: 'AbortError' });
    await loader.open(2, 1);
    const current = view;
    if (lateFailure) pending.reject(new Error('Old failure'));
    else pending.resolve(projectRequest('/projects/1/tasks?'));
    await rejected;
    assert.equal(view, current);
    assert.equal(view.project.id, 2);
  });
}

test('late filtered task responses cannot overwrite another project', async () => {
  let view;
  const pending = deferred();
  const loader = createProjectLoader(path => path.includes('status=DONE') ? pending.promise : Promise.resolve(projectRequest(path)), next => { view = next; });
  await loader.open(1, 1);
  const old = loader.tasks(1, 'DONE');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  assert.deepEqual(view.tasks, []);
  assert.deepEqual(view.counts, {});
  await loader.open(2, 1);
  pending.resolve({ items: [{ id: 999 }], counts: { DONE: 99 } });
  await rejected;
  assert.equal(view.tasks[0].id, 20);
  await assert.rejects(loader.tasks(1), { name: 'AbortError' });
});

test('failed task filter refresh keeps project identity but clears tasks and counts until retry', async () => {
  let view, fail = false;
  const loader = createProjectLoader(async path => {
    if (fail && path.includes('/tasks?')) throw new Error('Tasks unavailable');
    return projectRequest(path);
  }, next => { view = next; });
  await loader.open(1, 1);
  fail = true;
  await assert.rejects(loader.tasks(1, 'DONE'), /Tasks unavailable/);
  assert.equal(view.project.id, 1);
  assert.equal(view.id, 1);
  assert.equal(view.loading, false);
  assert.equal(view.error, 'Tasks unavailable');
  assert.deepEqual(view.tasks, []);
  assert.deepEqual(view.counts, {});
  fail = false;
  await loader.tasks(1, 'DONE');
  assert.equal(view.error, '');
  assert.equal(view.tasks[0].id, 10);
});

test('newer task filters win and retain contractual query parameters', async () => {
  let view;
  const pending = deferred(), paths = [];
  const loader = createProjectLoader(path => {
    paths.push(path);
    return path.includes('status=TODO') ? pending.promise : Promise.resolve(projectRequest(path));
  }, next => { view = next; });
  await loader.open(1, 1);
  const old = loader.tasks(1, 'TODO');
  const rejected = assert.rejects(old, { name: 'AbortError' });
  await loader.tasks(1, 'DONE', 'HIGH');
  const current = view;
  pending.resolve({ items: [], counts: {} });
  await rejected;
  assert.equal(view, current);
  assert.ok(paths.includes('/projects/1/tasks?status=DONE&priority=HIGH'));
});

test('unmount or session invalidation prevents late publication', async () => {
  const pending = deferred(), published = [];
  const loader = createProjectLoader(path => path.includes('/tasks?') ? pending.promise : Promise.resolve(projectRequest(path)), view => published.push(view));
  const old = loader.open(1, 1);
  const rejected = assert.rejects(old, { name: 'AbortError' });
  loader.invalidate();
  pending.resolve(projectRequest('/projects/1/tasks?'));
  await rejected;
  assert.equal(published.length, 1);
});

test('session abort propagates without publishing a project error or restored data', async () => {
  const published = [];
  const loader = createProjectLoader(async () => { throw new DOMException('Session changed', 'AbortError'); }, view => published.push(view));
  await assert.rejects(loader.open(1, 1), { name: 'AbortError' });
  assert.equal(published.length, 1);
  assert.equal(published[0].project, null);
});

const users = [
  { user: { id: 1, name: 'Creator', email: 'creator@example.test' }, projectRole: 'MEMBRO' },
  { user: { id: 2, name: 'Manager', email: 'manager@example.test' }, projectRole: 'GESTOR' },
];

function contextRequest(project, items = users) {
  return async path => {
    if (path === `/projects/${project.id}`) return project;
    assert.equal(path, `/projects/${project.id}/eligible-users`);
    return { items, totalItems: items.length };
  };
}

test('project role comes from participation, not creator identity or legacy role fields', async () => {
  const project = { id: 10, name: 'Project', description: null, createdByUserId: 1 };
  const creator = await loadProjectContext(contextRequest(project), 10, 1);
  assert.equal(creator.role, 'MEMBRO');
  assert.equal(projectPermissions(creator.role).canManageProject, false);
  const manager = await loadProjectContext(contextRequest(project), 10, 2);
  assert.equal(manager.role, 'GESTOR');
  assert.equal(projectPermissions(manager.role).canManageProject, true);
  assert.deepEqual(manager.project, project);
});

test('null legacy authorship does not prevent a direct manager from managing', async () => {
  const project = { id: 10, name: 'Legacy', description: null, createdByUserId: null };
  const context = await loadProjectContext(contextRequest(project), 10, 2);
  assert.equal(context.role, 'GESTOR');
  assert.equal(projectPermissions(context.role).canManageProject, true);
});

test('nested eligible users become responsible selector options without losing ids or names', async () => {
  const context = await loadProjectContext(contextRequest({ id: 10 }), 10, 1);
  assert.deepEqual(context.users, users.map(item => item.user));
  const responsibleIds = [2];
  assert.deepEqual(context.users.filter(user => responsibleIds.includes(user.id)), [users[1].user]);
});

test('missing participation and unrecognized roles fail closed even for an assigned user', async () => {
  const context = await loadProjectContext(contextRequest({ id: 10, createdByUserId: 3 }), 10, 3);
  assert.equal(context.role, null);
  for (const role of [context.role, undefined, 'gestor', 'ADMIN']) {
    assert.deepEqual(projectPermissions(role, { responsibleUserIds: [3] }, 3), {
      canManageProject: false, canCreateTask: false, canEditTask: false,
    });
  }
});

test('manager can edit all tasks; members edit only their assignments but both can create', () => {
  assert.deepEqual(projectPermissions('GESTOR', { responsibleUserIds: [] }, 2), {
    canManageProject: true, canCreateTask: true, canEditTask: true,
  });
  assert.deepEqual(projectPermissions('MEMBRO', { responsibleUserIds: [1] }, 1), {
    canManageProject: false, canCreateTask: true, canEditTask: true,
  });
  assert.deepEqual(projectPermissions('MEMBRO', { responsibleUserIds: [2] }, 1), {
    canManageProject: false, canCreateTask: true, canEditTask: false,
  });
  assert.equal(projectPermissions('MEMBRO', null, 1).canEditTask, false);
});

test('failed eligibility load never returns a context with inferred privileges', async () => {
  await assert.rejects(loadProjectContext(async path => {
    if (path.endsWith('/eligible-users')) throw new Error('Forbidden');
    return { id: 10, createdByUserId: 1, role: 'gestor' };
  }, 10, 1), { message: 'Forbidden' });
});

test('project edits use PATCH and preserve explicit null versus omitted description', async () => {
  const calls = [];
  const request = async (path, options) => { calls.push({ path, ...options }); return { id: 10, ...options.body }; };
  assert.deepEqual(await updateProject(request, 10, { name: 'Renamed', description: null }), {
    id: 10, name: 'Renamed', description: null,
  });
  await updateProject(request, 10, { name: 'Another name' });
  assert.deepEqual(calls, [
    { path: '/projects/10', method: 'PATCH', body: { name: 'Renamed', description: null } },
    { path: '/projects/10', method: 'PATCH', body: { name: 'Another name' } },
  ]);
});

test('project edit rejection propagates to the existing UI error handling', async () => {
  await assert.rejects(updateProject(async () => { throw new Error('Permission denied'); }, 10, { name: 'Name' }), {
    message: 'Permission denied',
  });
});
