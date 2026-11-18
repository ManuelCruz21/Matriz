import { loadProjectContext } from './projects.js';

// Publish detail and its authorization context together, never a previous project's role.
export function createTaskLoader(request, publish, publishPermissions = () => {}) {
  let generation = 0;
  return {
    async load(id, userId) {
      const token = ++generation;
      publishPermissions({ verified: false, loading: true });
      const assertCurrent = () => {
        if (token !== generation) throw new DOMException('Task load superseded', 'AbortError');
      };
      try {
        const detail = await loadTaskDetail(request, id);
        assertCurrent();
        const context = await loadProjectContext(request, detail.task.projectId, userId);
        assertCurrent();
        publish({ ...detail, ...context });
        publishPermissions({ verified: true, loading: false });
      } catch (error) {
        assertCurrent();
        if (error.name !== 'AbortError') publishPermissions({ verified: false, loading: false });
        throw error;
      }
    },
    invalidate: () => { generation++; },
  };
}

export async function loadTaskDetail(request, id) {
  const [task, comments, history] = await Promise.all([
    request(`/tasks/${id}`), request(`/tasks/${id}/comments`), request(`/tasks/${id}/history`),
  ]);
  return { task, comments: comments.items, history: history.items };
}

// A confirmed write must never be retried because its follow-up reads failed.
export async function writeAndRefresh(write, refresh) {
  const result = await write();
  try {
    await refresh(result);
    return { refreshFailed: false };
  } catch (error) {
    // Session changes still invalidate the whole action and its UI feedback.
    if (error.name === 'AbortError') throw error;
    return { refreshFailed: true };
  }
}

export async function saveTask(request, projectId, taskId, data, { openProject, refreshTask, loadDashboard }) {
  return writeAndRefresh(
    () => taskId == null
      ? request(`/projects/${projectId}/tasks`, { method: 'POST', body: data })
      : request(`/tasks/${taskId}`, { method: 'PATCH', body: data }),
    async () => {
      if (taskId == null) await openProject(projectId);
      else await refreshTask(taskId);
      await loadDashboard();
    },
  );
}

// Lock synchronously, before React can render the disabled controls.
export function singleFlight() {
  let pending = false;
  return async action => {
    if (pending) return false;
    pending = true;
    try { await action(); return true; } finally { pending = false; }
  };
}

// Match Java String.strip(): Unicode whitespace, excluding non-breaking spaces.
export function textLengthError(value, max) {
  const stripped = value.replace(/^[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]+|[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]+$/gu, '');
  return Array.from(stripped).length > max ? `O limite é de ${max} caracteres.` : '';
}

export function historyValue(value, users, statuses, priorities) {
  if (Array.isArray(value)) return value.length
    ? value.map(id => users.find(user => user.id === id)?.name || `#${id}`).join(', ')
    : 'Sem responsáveis';
  return statuses[value] || priorities[value] || String(value);
}
