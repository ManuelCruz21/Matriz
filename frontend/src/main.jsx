import React, { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { createNavigation, createRouteScope, routeHref } from './navigation.js';
import { createTaskLoader, saveTask, writeAndRefresh, singleFlight, historyValue, textLengthError } from './task-flows.js';
import { createRoot } from 'react-dom/client';
import { request as apiRequest, login, logout, getSessionVersion } from './api.js';
import { createProjectLoader, projectPermissions, updateProject } from './projects.js';
import { createRouteAccessibility, pageTitle } from './route-accessibility.js';
import { createTheme } from './theme.js';
import './style.css';
import './visual-refresh.css';
import SecurityCenter from './security-center.jsx';

const theme = createTheme(window);
function ThemeSelector() {
  const [preference,setPreference]=useState(theme.preference);
  useEffect(()=>theme.connect(),[]);
  return <label className="theme-selector">Theme<select value={preference} onChange={event=>{theme.select(event.target.value);setPreference(event.target.value);}}><option value="light">Light</option><option value="dark">Dark</option><option value="system">System</option></select></label>;
}

const statuses = { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Completed' };
const priorities = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' };
function SessionRoot() {
  const [navigation]=useState(createNavigation);
  const [accessibility]=useState(createRouteAccessibility);
  useEffect(()=>accessibility.attach(),[]);
  const route=useSyncExternalStore(navigation.subscribe,navigation.getSnapshot);
  const [session,setSession]=useState({version:getSessionVersion(),user:null,bootstrap:true});
  const [authError,setAuthError]=useState(''),[authBusy,setAuthBusy]=useState(false);
  const [authFlight]=useState(singleFlight);
  const [username,setUsername]=useState(''),[logoutUnconfirmed,setLogoutUnconfirmed]=useState(false);
  useEffect(()=>{
    const changed=e=>{setAuthError('');if(e.detail.user||e.detail.expired){setAuthBusy(false);setLogoutUnconfirmed(false);setUsername('');}setSession({...e.detail,bootstrap:false});};
    window.addEventListener('matriz:session',changed);
    return()=>window.removeEventListener('matriz:session',changed);
  },[]);
  async function authenticate(username,password) {return authFlight(async()=>{
    setAuthBusy(true);
    const pending=login(username,password), version=getSessionVersion();
    try {await pending;} catch(e) {if(e.name!=='AbortError'&&version===getSessionVersion())setAuthError(e.message);} finally {if(version===getSessionVersion())setAuthBusy(false);}
  });}
  async function signOut() {return authFlight(async()=>{
    setAuthBusy(true);setUsername('');navigation.navigate('/',{replace:true});
    const pending=logout(), version=getSessionVersion();
    try {await pending;if(version===getSessionVersion())setLogoutUnconfirmed(false);} catch(e) {if(e.name!=='AbortError'&&version===getSessionVersion())setLogoutUnconfirmed(true);} finally {if(version===getSessionVersion())setAuthBusy(false);}
  });}
  const [actionFlight]=useState(singleFlight);
  return <AppShell key={session.version} route={route} navigation={navigation} accessibility={accessibility} actionFlight={actionFlight} onBootstrap={user=>setSession(current=>({...current,user,bootstrap:false}))} session={session} authError={authError} authBusy={authBusy} username={username} setUsername={setUsername} logoutUnconfirmed={logoutUnconfirmed} authenticate={authenticate} signOut={signOut}/>;
}
function AppShell(props) {
  const {route,navigation,session,authBusy,signOut}=props;
  const routeKey=route.type==='invalid'?window.location.pathname+window.location.search:routeHref(route);
  const [projectContext,setProjectContext]=useState(null);
  const [notice,setNotice]=useState(null);
  const project=projectContext?.routeKey===routeKey?projectContext.project:null;
  const [securityRoute,setSecurityRoute]=useState(null);
  const securityAdmin=session.user?.securityAdmin===true;
  const securityOpen=securityAdmin&&securityRoute===route;
  const navigateAfterSave=(href,message)=>{setNotice({href,message});navigation.navigate(href,{replace:true});};
  const content=<App key={`${session.version}:${routeKey}`} {...props} initialSuccess={notice?.href===routeKey?notice.message:''} navigateAfterSave={navigateAfterSave} active={!securityOpen} onProject={project=>setProjectContext({routeKey,project})}/>;
  useEffect(()=>{if(notice&&notice.href!==routeKey)setNotice(null);},[routeKey]);
  // A new navigation snapshot (including Back/Forward) closes the local view.
  useEffect(()=>{setSecurityRoute(null);},[route]);
  useEffect(()=>{
    if(!securityOpen)return;
    const previousFocus=document.activeElement;
    document.title='Security Center — Matriz';
    document.getElementById('main-content')?.focus({preventScroll:true});
    return()=>{
      if(navigation.getSnapshot()===securityRoute){
        const target=previousFocus?.isConnected?previousFocus:document.getElementById('main-content');
        target?.focus({preventScroll:true});
      }
    };
  },[securityOpen]);
  const setSecurityOpen=open=>setSecurityRoute(open&&securityAdmin?route:null);
  if(!session.user)return content;
  const goHome=()=>{setSecurityOpen(false);navigation.navigate('/');};
  const followLink=event=>{setSecurityOpen(false);navigation.handleClick(event);};
  return <main><a className="skip-link" href="#main-content">Skip to content</a><header><h1><button type="button" className="brand" aria-label="Matriz — return to dashboard" onClick={goHome}>Matriz</button></h1><span className="account-name">{session.user.name||session.user.email}</span><ThemeSelector/><button disabled={authBusy} onClick={signOut}>Log out</button></header>
    <nav aria-label="Main navigation"><button aria-current={!securityOpen&&route.type==='home'?'page':undefined} onClick={goHome}>Dashboard</button><a href="/projects" aria-current={!securityOpen&&route.type==='projects'?'page':undefined} onClick={followLink}>Projects</a>{securityAdmin&&<button type="button" aria-current={securityOpen?'page':undefined} onClick={()=>setSecurityOpen(true)}>Security Center</button>}
    {project&&<a href={routeHref({type:'project',id:project.id})} aria-label={`Open project: ${project.name}`} aria-current={!securityOpen&&route.type==='project'?'page':undefined} onClick={followLink}>{project.name}</a>}</nav>
    <div id="main-content" tabIndex={-1}><div hidden={securityOpen}>{content}</div>{securityOpen&&<SecurityCenter/>}</div></main>;
}
function App({active=true,initialSuccess='',navigateAfterSave,route,navigation,accessibility,actionFlight,onBootstrap,onProject,session,authError,authBusy,username,setUsername,logoutUnconfirmed,authenticate,signOut}) {
  const [scope]=useState(()=>createRouteScope(navigation,route,(path,options={})=>apiRequest(path,{...options,sessionVersion:session.version})));
  const {request,assertActive}=scope;
  const [me,setMe]=useState(session.user), [page,setPage]=useState(route);
  const [loading,setLoading]=useState(session.bootstrap||!!session.user), [busy,setBusy]=useState(false), [error,setError]=useState(session.expired?'Your session has expired. Please log in again.':'');
  const [success,setSuccess]=useState(initialSuccess),[flight]=useState(()=>actionFlight);

  const [projects,setProjects]=useState([]),[search,setSearch]=useState(route.search||'');
  const [projectView,setProjectView]=useState({id:null,project:null,role:null,users:[],tasks:[],counts:{},loading:false,error:''});
  const [projectLoader]=useState(()=>createProjectLoader(request,setProjectView));
  const tasks=projectView.tasks,counts=projectView.counts;
  const [taskView,setTaskView]=useState(null);
  const [responsibleDraft,setResponsibleDraft]=useState(null);
  const task=taskView?.task,comments=taskView?.comments||[];
  const [taskPermissions,setTaskPermissions]=useState({verified:false,loading:false});
  const [taskLoader]=useState(()=>createTaskLoader(request,setTaskView,setTaskPermissions));
  const context=route.type==='task'?taskView:projectView;
  const project=context?.project,projectRole=context?.role,users=context?.users||[],history=taskView?.history||[];
  useEffect(()=>{onProject?.(project);},[project]);
  const [statusFilter,setStatusFilter]=useState(route.status||''),[priorityFilter,setPriorityFilter]=useState(route.priority||'');
  const [dashboard,setDashboard]=useState(null);
  const [password,setPassword]=useState('');
  const [refreshPending,setRefreshPending]=useState(false),[createdProjectId,setCreatedProjectId]=useState(null),[creationVersion,setCreationVersion]=useState(0);
  async function run(action,message='') {if(message&&(refreshPending||route.type==='task'&&!taskPermissions.verified))return false;let saved=false;await flight(async()=>{setBusy(true);setError('');setSuccess('');try {const result=await action();assertActive();setSuccess(result?.refreshFailed?`${message} The displayed data could not be refreshed. The change was saved; do not submit it again. Refresh the data without repeating the action.`:message);setRefreshPending(!!result?.refreshFailed);saved=true;}catch(e){if(e.name!=='AbortError')setError(e.message);}finally{setBusy(false);}});return saved;}
  async function loadProjects(q=search) {const data=await request('/projects?search='+encodeURIComponent(q));setProjects(data.items);}
  async function loadDashboard() {setDashboard(await request('/dashboard'));}
  async function openProject(id) {
    assertActive();
    const destination=routeHref({type:'project',id,status:statusFilter,priority:priorityFilter});
    if(route.type!=='project'||route.id!==id){navigation.navigate(destination);return;}
    setPage({type:'project'});
    await projectLoader.open(id,me.id,statusFilter,priorityFilter);
  }
  async function loadTasks(id=project?.id,s=statusFilter,pr=priorityFilter){if(!id)return;
    await projectLoader.tasks(id,s,pr);}
  async function refreshTask(id=task?.id){if(!id)return;assertActive();await taskLoader.load(id,me.id);}
  async function retryDataRefresh(){await flight(async()=>{setBusy(true);setError('');try{
    if(route.type==='task')await refreshTask();
    else if(route.type==='project')await openProject(route.id);
    else if(route.type==='projects')await loadProjects(route.search);
    await loadDashboard();assertActive();setRefreshPending(false);setCreationVersion(value=>value+1);
  }catch(e){if(e.name!=='AbortError')setError(e.message);}finally{setBusy(false);}});}
  const revalidateTaskPermissions=retryDataRefresh;
  const presented=useRef(false);
  useEffect(()=>{
    if(!active)return;
    const title=pageTitle(route,{loading,authenticated:!!me,projectName:project?.name,taskTitle:task?.title});
    document.title=title;
    if(!loading&&!presented.current){presented.current=true;accessibility.ready(route,{authenticated:!!me,sessionVersion:session.version,transition:navigation.getTransition(),title});}
  },[active,loading,me,project?.name,task?.title]);
  useEffect(()=>{
    const key=window.location.pathname+window.location.search;
    const save=()=>accessibility.save(key);
    window.addEventListener('scroll',save,{passive:true});
    return()=>window.removeEventListener('scroll',save);
  },[]);
  useEffect(()=>{
    scope.activate();
    let active=true;
    async function initialize() {
      try {
        let currentUser=session.user;
        if(session.bootstrap) {const user=await request('/users/me',{anonymousBootstrap:true});if(!active)return;setMe(user);currentUser=user;onBootstrap(user);}
        else if(!session.user)return;
        if(!currentUser)return;
        if(route.type==='home')await loadDashboard();
        else if(route.type==='projects')await loadProjects(route.search);
        else if(route.type==='project')await projectLoader.open(route.id,currentUser.id,route.status,route.priority);
        else if(route.type==='task')await taskLoader.load(route.id,currentUser.id);
      } catch(e) {if(active&&e.name!=='AbortError')setError(e.message);}
      finally {if(active)setLoading(false);}
    }
    initialize();
    return()=>{active=false;scope.invalidate();projectLoader.invalidate();taskLoader.invalidate();};
  },[]);
  const taskControlsVisible=useRef(false);
  if(taskPermissions.verified)taskControlsVisible.current=true;
  else if(!taskPermissions.loading)taskControlsVisible.current=false;
  const {canManageProject,canCreateTask,canEditTask}=projectPermissions(route.type==='task'&&!taskPermissions.verified&&!(taskPermissions.loading&&taskControlsVisible.current)?null:projectRole,task,me?.id);
  const followLink=event=>navigation.handleClick(event);
  const applyFilters=(status,priority)=>navigation.navigate(routeHref({type:'project',id:project.id,status,priority}));
  if(loading)return <section aria-busy="true"><p role="status" aria-live="polite">Loading Matriz…</p></section>;
  if(!me)return <main className="auth"><h1>Matriz</h1>{logoutUnconfirmed&&<section><p role="alert" className="error">Local data was cleared, but the server could not confirm logout. The session may still be active. Do not refresh the page to confirm logout: this could reopen the session. Check your connection and try logging out again. On a shared device, do not leave it unattended until logout is confirmed; if needed, clear this site’s cookies and data.</p><button disabled={authBusy} onClick={signOut}>Try logging out again</button></section>}<h2 id="login-heading" tabIndex={-1}>Log in</h2><form onSubmit={e=>{e.preventDefault();const pending=run(()=>authenticate(username,password));setPassword('');return pending;}}>
    <label>Username<input required disabled={busy||authBusy} value={username} onChange={e=>setUsername(e.target.value)} autoComplete="username" /></label>
    <label>Password<input required disabled={busy||authBusy} type="password" value={password} onChange={e=>setPassword(e.target.value)} autoComplete="current-password" /></label>
    <button disabled={busy||authBusy}>Sign in</button></form>{(authError||error)&&<p role="alert" className="error">{authError||error}</p>}</main>;
  return <>
    {error&&<p role="alert" className="error">{error}</p>}
    {success&&<p role="status" className={refreshPending?"notice-warning":"notice-success"}>{success}</p>}
    {refreshPending&&<aside className="notice-warning"><p>The displayed data may be outdated. Do not repeat the saved action.</p><button disabled={busy||authBusy} onClick={retryDataRefresh}>Refresh data without resubmitting</button></aside>}
    {createdProjectId!=null&&<p><a href={routeHref({type:'project',id:createdProjectId})} onClick={followLink}>Open created project #{createdProjectId}</a></p>}
    {(page.type==='invalid'||page.type==='task'&&!task)&&<section><h2>{page.type==='task'?(busy?'Loading task…':'Unable to load this task'):'Invalid page or filters'}</h2>{page.type==='task'&&!busy&&<button onClick={()=>run(()=>refreshTask(route.id))}>Reload task</button>}<a href="/projects" onClick={followLink}>View projects</a></section>}
    {page.type==='task'&&task&&!taskPermissions.verified&&<aside><p role="status">{taskPermissions.loading?'Revalidating permissions…':'Unable to verify permissions. Data may be outdated; actions are unavailable until verification succeeds.'}</p><button disabled={busy||authBusy||taskPermissions.loading} onClick={revalidateTaskPermissions}>Verify permissions and refresh task</button></aside>}
    <fieldset disabled={busy||authBusy||refreshPending||route.type==='task'&&!taskPermissions.verified} style={{border:0,padding:0,margin:0}}>
    {page.type==='home'&&<section><div className="dashboard-heading"><div><span className="eyebrow">WORKSPACE OVERVIEW</span><h2>Dashboard</h2><p>Track projects and tasks at a glance.</p></div><span className="dashboard-label">Overview</span></div>
      <div className="stats-grid"><article className="stat-card"><span className="stat-label">Projects</span><strong>{dashboard?.totalProjects??0}</strong><span className="stat-foot">Accessible projects</span></article><article className="stat-card"><span className="stat-label">To Do</span><strong>{dashboard?.counts?.TODO??0}</strong><span className="stat-foot">Pending tasks</span></article><article className="stat-card"><span className="stat-label">In Progress</span><strong>{dashboard?.counts?.IN_PROGRESS??0}</strong><span className="stat-foot">Active tasks</span></article><article className="stat-card"><span className="stat-label">Completed</span><strong>{dashboard?.counts?.DONE??0}</strong><span className="stat-foot">Finished tasks</span></article></div><div className="section-heading"><h3>Your projects</h3><span>All accessible projects</span></div>
      <div className="cards">{(dashboard?.projects||[]).map(p=><article key={p.id}><h3>{p.project?.name}</h3><p>To Do: {p.counts?.TODO??0} · In Progress: {p.counts?.IN_PROGRESS??0} · Completed: {p.counts?.DONE??0}</p><a href={routeHref({type:'project',id:p.project.id})} aria-label={`Open project: ${p.project?.name}`} onClick={followLink}>Open</a></article>)}</div></section>}
    {page.type==='projects'&&<section><h2>Projects</h2><form onSubmit={e=>{e.preventDefault();navigation.navigate(routeHref({type:'projects',search}));}} className="inline" role="search" aria-label="Project search"><label>Search projects by name<input id="project-search" type="search" placeholder="Search by name" value={search} onChange={e=>setSearch(e.target.value)}/></label><button>Search</button></form>
      <div className="cards">{projects.map(p=><article key={p.id}><h3>{p.name}</h3><p>{p.description}</p><p><a href={routeHref({type:'project',id:p.id})} aria-label={`Open project: ${p.name}`} onClick={followLink}>Open</a></p></article>)}</div>
      {!projects.length&&<p>No projects match your search.</p>}
      <h3>Create project</h3><ProjectForm key={creationVersion} busy={busy} onSave={data=>run(()=>writeAndRefresh(async()=>{const p=await request('/projects',{method:'POST',body:data});setCreatedProjectId(p.id);return p;},async p=>{await loadProjects();navigateAfterSave(routeHref({type:'project',id:p.id}),'Project created successfully.');}), 'Project created successfully.')}/></section>}
    {page.type==='project'&&(projectView.loading||projectView.error)&&<section aria-busy={projectView.loading}><h2>Project #{projectView.id}</h2>
      {projectView.loading?<p role="status">Loading project and tasks…</p>:<><p role="alert" className="error">Unable to load project data: {projectView.error}</p><button onClick={()=>run(()=>openProject(projectView.id))}>Reload project</button></>}</section>}
    {page.type==='project'&&project&&!projectView.loading&&!projectView.error&&<section><h2>{project.name}</h2><p>{project.description}</p><p>Role: {projectRole||'Unavailable'}</p>
      {canManageProject&&<details><summary>Edit or delete project</summary><ProjectForm key={project.id} initial={project} busy={busy} onSave={data=>run(()=>writeAndRefresh(()=>updateProject(request,project.id,data),()=>openProject(project.id)),'Project updated successfully.')}/>
        <button className="danger" disabled={busy} onClick={()=>{if(window.confirm('Delete this project and all associated data?'))run(()=>writeAndRefresh(()=>request(`/projects/${project.id}`,{method:'DELETE'}),async()=>{projectLoader.invalidate();setProjectView({id:null,project:null,role:null,users:[],tasks:[],counts:{},loading:false,error:''});navigateAfterSave('/projects','Project deleted successfully.');}),'Project deleted successfully.');}}>Delete project</button></details>}
      <h3>Tasks</h3><div className="inline"><label>Status<select id="task-status-filter" value={statusFilter} onChange={e=>{applyFilters(e.target.value,priorityFilter);}}><option value="">All</option>{Object.entries(statuses).map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label>
      <label>Priority<select id="task-priority-filter" value={priorityFilter} onChange={e=>{applyFilters(statusFilter,e.target.value);}}><option value="">All</option>{Object.entries(priorities).map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label></div>
      <p>To Do: {counts.TODO??0} · In Progress: {counts.IN_PROGRESS??0} · Completed: {counts.DONE??0}</p>
      <div className="cards">{tasks.map(t=><article key={t.id}><h3>{t.title}</h3><p>{statuses[t.status]} · {priorities[t.priority]}</p><a href={routeHref({type:'task',id:t.id})} aria-label={`Open task: ${t.title}`} onClick={followLink}>Open task</a></article>)}</div>
      {!tasks.length&&<p>No tasks match the selected filters.</p>}
      {canCreateTask&&<><h3>New task</h3><TaskForm key={creationVersion} busy={busy} onSave={async data=>{let outcome;const saved=await run(async()=>outcome=await saveTask(request,project.id,null,data,{openProject:id=>projectLoader.open(id,me.id,'',''),refreshTask,loadDashboard}),'Task created successfully.');if(saved&&!outcome.refreshFailed&&(statusFilter||priorityFilter))navigateAfterSave(routeHref({type:'project',id:project.id}),'Task created successfully.');return saved;}}/></>}</section>}
    {page.type==='task'&&task&&<section><a href={routeHref({type:'project',id:task.projectId})} aria-label={`Back to project: ${project?.name||task.projectId}`} onClick={followLink}>Back to project</a><h2>{task.title}</h2><p>{task.description||'No description'}</p><p>Status: {statuses[task.status]} · Priority: {priorities[task.priority]}</p>
      {canEditTask&&<><h3>Edit task</h3><TaskForm key={task.id} initial={task} busy={busy} onSave={data=>run(()=>saveTask(request,project.id,task.id,data,{openProject,refreshTask,loadDashboard}),'Task updated successfully.')}/>
      <div className="inline"><label>Status<select value={task.status} disabled={busy} onChange={e=>run(()=>writeAndRefresh(()=>request(`/tasks/${task.id}/status`,{method:'PATCH',body:{status:e.target.value}}),()=>refreshTask(task.id)),'Changes saved successfully.')}>{Object.entries(statuses).map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label>
      <label>Priority<select value={task.priority} disabled={busy} onChange={e=>run(()=>writeAndRefresh(()=>request(`/tasks/${task.id}/priority`,{method:'PATCH',body:{priority:e.target.value}}),()=>refreshTask(task.id)),'Changes saved successfully.')}>{Object.entries(priorities).map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label></div></>}
      <h3>Assignees</h3>{users.filter(u=>task.responsibleUserIds?.includes(u.id)).map(u=><p key={u.id}>{u.name}</p>)}
      {canManageProject&&<Responsibles users={users} selected={responsibleDraft??task.responsibleUserIds??[]} onChange={setResponsibleDraft} busy={busy} onSave={ids=>run(()=>writeAndRefresh(async()=>{const result=await request(`/tasks/${task.id}/responsibles`,{method:'PUT',body:{responsibleUserIds:ids}});setResponsibleDraft(null);return result;},()=>refreshTask(task.id)),'Changes saved successfully.')}/>}
      <h3>Comments</h3>{comments.map(c=><article key={c.id}><p>{c.content}</p><small>Autor #{c.authorId} · {new Date(c.createdAt).toLocaleString('pt-PT')}</small></article>)}
      <fieldset disabled={!taskPermissions.verified} style={{border:0,padding:0,margin:0}}><CommentForm busy={busy||!taskPermissions.verified} onSave={content=>run(()=>writeAndRefresh(()=>request(`/tasks/${task.id}/comments`,{method:'POST',body:{content}}),()=>refreshTask(task.id)),'Changes saved successfully.')}/>
      </fieldset><h3>Activity history</h3>{!history.length&&<p>No changes recorded.</p>}
      <ol>{history.map(entry=><li key={entry.id}><strong>{{status:'Status',priority:'Priority',responsibleUserIds:'Assignees'}[entry.field]}</strong>: {historyValue(entry.previousValue,users,statuses,priorities)} → {historyValue(entry.newValue,users,statuses,priorities)}<br/><small>By {users.find(user=>user.id===entry.changedByUserId)?.name||`#${entry.changedByUserId}`} · <time dateTime={entry.changedAt}>{new Date(entry.changedAt).toLocaleString('pt-PT')}</time></small></li>)}</ol>
      {canManageProject&&<p><button className="danger" disabled={busy} onClick={()=>{if(window.confirm('Delete this task?'))run(()=>writeAndRefresh(()=>request(`/tasks/${task.id}`,{method:'DELETE'}),async()=>{assertActive();navigateAfterSave(routeHref({type:'project',id:task.projectId}),'Task deleted successfully.');}),'Task deleted successfully.');}}>Delete task</button></p>}</section>}
    </fieldset>{busy&&<p role="status">Processing…</p>}</>;
}
// The helper strips Java whitespace and counts code points; exceeding two means at least three.
function requiredTextLengthError(value,max) {return textLengthError(value,max)||(textLengthError(value,2)?'':'Use at least 3 characters.');}
function ProjectForm({initial,busy,onSave}) {const [name,setName]=useState(initial?.name||''),[description,setDescription]=useState(initial?.description||'');
  return <form onSubmit={e=>{e.preventDefault();onSave({name,description:description||null});}}><label>Name<input required ref={element=>{if(element)element.setCustomValidity(requiredTextLengthError(name,100));}} value={name} onChange={e=>setName(e.target.value)}/></label><label>Description<textarea ref={element=>{if(element)element.setCustomValidity(textLengthError(description,1000));}} value={description} onChange={e=>setDescription(e.target.value)}/></label><button disabled={busy}>Save project</button></form>;}
function TaskForm({initial,busy,onSave}) {const [title,setTitle]=useState(initial?.title||''),[description,setDescription]=useState(initial?.description||'');
  return <form onSubmit={e=>{e.preventDefault();onSave({title,description:description||null});}}><label>Title<input required ref={element=>{if(element)element.setCustomValidity(requiredTextLengthError(title,150));}} value={title} onChange={e=>setTitle(e.target.value)}/></label><label>Description<textarea ref={element=>{if(element)element.setCustomValidity(textLengthError(description,2000));}} value={description} onChange={e=>setDescription(e.target.value)}/></label><button disabled={busy}>Save task</button></form>;}
function Responsibles({users,selected,onChange,busy,onSave}) {return <form onSubmit={e=>{e.preventDefault();return onSave(selected);}}><fieldset><legend>Select assignees</legend>{users.map(u=><label key={u.id} className="check"><input type="checkbox" checked={selected.includes(u.id)} onChange={e=>onChange(e.target.checked?[...selected,u.id]:selected.filter(id=>id!==u.id))}/>{u.name}</label>)}</fieldset><button disabled={busy}>Save assignees</button></form>;}
function CommentForm({busy,onSave}) {const [content,setContent]=useState('');return <form onSubmit={async e=>{e.preventDefault();if(!busy&&content.trim()&&await onSave(content))setContent('');}}><label>New comment<textarea required ref={element=>{if(element)element.setCustomValidity(textLengthError(content,2000));}} value={content} onChange={e=>setContent(e.target.value)}/></label><button disabled={busy}>Post comment</button></form>;}
createRoot(document.getElementById('root')).render(<SessionRoot/>);
