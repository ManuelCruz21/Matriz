// Project authorship is metadata, never an authorization signal.
export async function loadProjectContext(request, projectId, userId) {
  const [project, eligible] = await Promise.all([
    request(`/projects/${projectId}`),
    request(`/projects/${projectId}/eligible-users`),
  ]);
  return {
    project,
    role: eligible.items.find(item => item.user.id === userId)?.projectRole ?? null,
    users: eligible.items.map(item => item.user),
  };
}

export function createProjectLoader(request, publish) {
  let generation = 0;
  let view = { id: null, project: null, role: null, users: [], tasks: [], counts: {}, loading: false, error: '' };
  function assertCurrent(token) {
    if (token !== generation) throw new DOMException('Project load superseded', 'AbortError');
  }
  async function load(id, userId, status, priority, contextRequired) {
    const token = ++generation;
    view = { ...(contextRequired ? { project: null, role: null, users: [] } : view),
      id, tasks: [], counts: {}, loading: true, error: '' };
    publish(view);
    try {
      const params = new URLSearchParams();
      if (status) params.set('status', status);
      if (priority) params.set('priority', priority);
      const [context, data] = await Promise.all([
        contextRequired ? loadProjectContext(request, id, userId) : Promise.resolve(null),
        request(`/projects/${id}/tasks?${params}`),
      ]);
      assertCurrent(token);
      view = { ...view, ...(context || {}), tasks: data.items || [], counts: data.counts || {}, loading: false };
      publish(view);
    } catch (error) {
      assertCurrent(token);
      if (error.name !== 'AbortError') {
        view = { ...view, loading: false, error: error.message };
        publish(view);
      }
      throw error;
    }
  }
  return {
    open: (id, userId, status = '', priority = '') => load(id, userId, status, priority, true),
    tasks: (id, status = '', priority = '') => {
      if (id !== view.id || !view.project) return Promise.reject(new DOMException('Project changed', 'AbortError'));
      return load(id, null, status, priority, false);
    },
    invalidate: () => { generation++; },
  };
}

export function projectPermissions(role, task, userId) {
  const canManageProject = role === 'GESTOR';
  return {
    canManageProject,
    canCreateTask: canManageProject || role === 'MEMBRO',
    canEditTask: canManageProject || (role === 'MEMBRO' && !!task?.responsibleUserIds?.includes(userId)),
  };
}

export function updateProject(request, projectId, data) {
  return request(`/projects/${projectId}`, { method: 'PATCH', body: data });
}
