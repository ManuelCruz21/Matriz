export function pageTitle(route, { loading, authenticated, projectName, taskTitle } = {}) {
  if (loading) return 'A carregar… — Matriz';
  if (!authenticated) return 'Iniciar sessão — Matriz';
  const name = { home: 'Painel inicial', projects: 'Projetos',
    project: projectName || `Projeto #${route.id}`, task: taskTitle || `Tarefa #${route.id}` }[route.type];
  return `${name || 'Página ou filtros inválidos'} — Matriz`;
}

// Only navigation presentation lives here; no API data or credentials are retained.
export function createRouteAccessibility(browser = window) {
  const positions = new Map();
  let previous = null;
  return {
    attach() {
      const original = browser.history.scrollRestoration;
      browser.history.scrollRestoration = 'manual';
      const capture = () => {
        const id = browser.document.activeElement?.id;
        if (previous && ['project-search', 'task-status-filter', 'task-priority-filter'].includes(id)) previous.focusId = id;
      };
      browser.document.addEventListener('focusin', capture);
      return () => { browser.history.scrollRestoration = original; browser.document.removeEventListener('focusin', capture); };
    },
    save(key) { positions.set(key, [browser.scrollX, browser.scrollY]); },
    ready(route, { authenticated, sessionVersion, transition, title }) {
      const key = browser.location.pathname + browser.location.search;
      const sameSession = previous?.sessionVersion === sessionVersion;
      if (!sameSession) positions.clear();
      const queryOnly = sameSession && previous.authenticated === authenticated
        && previous.pathname === browser.location.pathname;
      browser.document.title = title;
      // A deep-link fragment (including the skip link) takes precedence over route focus.
      if (browser.location.hash) {
        let id;
        try { id = decodeURIComponent(browser.location.hash.slice(1)); } catch { id = browser.location.hash.slice(1); }
        const anchor = browser.document.getElementById(id);
        if (anchor) { anchor.focus({ preventScroll: true }); anchor.scrollIntoView(); }
      } else {
        const filter = queryOnly && previous.focusId && browser.document.getElementById(previous.focusId);
        const target = filter || browser.document.getElementById(authenticated ? 'main-content' : 'login-heading');
        if (previous) target?.focus({ preventScroll: true });
        const position = transition === 'pop' && sameSession ? positions.get(key) : null;
        browser.scrollTo(...(position || (queryOnly ? positions.get(previous.key) : null) || [0, 0]));
      }
      previous = { key, pathname: browser.location.pathname, authenticated, sessionVersion, focusId: null };
    },
    captureFocus() {
      if (previous) previous.focusId = browser.document.activeElement?.id || null;
    },
  };
}
