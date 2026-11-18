import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { pageTitle, createRouteAccessibility } from './route-accessibility.js';

function fixture() {
  const focuses=[],scrolls=[],document=new EventTarget();
  const nodes=Object.fromEntries(['main-content','login-heading','task-status-filter','task-priority-filter','project-search'].map(id=>[id,{id,focus:options=>focuses.push([id,options]),scrollIntoView:()=>scrolls.push('anchor')}]));
  document.getElementById=id=>nodes[id];
  const browser={document,history:{scrollRestoration:'auto'},location:new URL('https://matriz.test/projects/1'),scrollX:0,scrollY:200,scrollTo:(...args)=>scrolls.push(args)};
  const controller=createRouteAccessibility(browser);
  const ready=(extra={})=>controller.ready({type:'project',id:1},{authenticated:true,sessionVersion:1,transition:'push',title:'Project — Matriz',...extra});
  return {browser,controller,ready,focuses,scrolls,nodes};
}

test('titles distinguish authentication, loading, route types and loaded names',()=>{
  assert.equal(pageTitle({type:'task',id:1},{loading:true}),'A carregar… — Matriz');
  assert.equal(pageTitle({type:'task',id:1},{}),'Iniciar sessão — Matriz');
  for(const [route,extra,title] of [[{type:'home'},{},'Painel inicial'],[{type:'projects'},{},'Projetos'],[{type:'project',id:1},{},'Projeto #1'],[{type:'task',id:2},{taskTitle:'Task'},'Task'],[{type:'project',id:1},{projectName:'Project'},'Project'],[{type:'invalid'},{},'Página ou filtros inválidos']])
    assert.equal(pageTitle(route,{authenticated:true,...extra}),`${title} — Matriz`);
});

test('initial page does not force keyboard focus, but scrolls to top',()=>{
  const f=fixture();f.ready();
  assert.deepEqual(f.focuses,[]);
  assert.deepEqual(f.scrolls,[[0,0]]);
  assert.equal(f.browser.document.title,'Project — Matriz');
});

for(const id of ['task-status-filter','task-priority-filter','project-search']) {
  test(`query navigation restores ${id} focus and previous scroll`,()=>{
    const f=fixture(),detach=f.controller.attach();f.ready();
    f.browser.document.activeElement=f.nodes[id];
    f.browser.document.dispatchEvent(new Event('focusin'));
    f.controller.save('/projects/1');
    f.browser.location=new URL('https://matriz.test/projects/1?status=DONE');f.ready();
    assert.equal(f.focuses.at(-1)[0],id);
    assert.deepEqual(f.scrolls.at(-1),[0,200]);
    detach();assert.equal(f.browser.history.scrollRestoration,'auto');
  });
}

test('Back restores saved position, while ordinary screen navigation starts at top',()=>{
  const f=fixture();f.ready();f.controller.save('/projects/1');
  f.browser.location=new URL('https://matriz.test/tasks/2');f.ready();
  assert.deepEqual(f.scrolls.at(-1),[0,0]);
  f.browser.location=new URL('https://matriz.test/projects/1');f.ready({transition:'pop'});
  assert.deepEqual(f.scrolls.at(-1),[0,200]);
});

test('account changes discard private scroll state and focus login rather than old filters',()=>{
  const f=fixture();f.ready();f.controller.save('/projects/1');
  f.ready({authenticated:false,sessionVersion:2,transition:'pop'});
  assert.equal(f.focuses.at(-1)[0],'login-heading');
  assert.deepEqual(f.scrolls.at(-1),[0,0]);
});

test('skip fragment takes precedence over automatic route scrolling',()=>{
  const f=fixture();f.browser.location.hash='#main-content';f.ready();
  assert.deepEqual(f.focuses,[['main-content',{preventScroll:true}]]);
  assert.deepEqual(f.scrolls,['anchor']);
});

test('search submit-button focus does not lose the search focus destination',()=>{
  const f=fixture(),detach=f.controller.attach();f.ready();
  f.browser.document.activeElement=f.nodes['project-search'];f.browser.document.dispatchEvent(new Event('focusin'));
  f.browser.document.activeElement={};f.browser.document.dispatchEvent(new Event('focusin'));
  f.browser.location=new URL('https://matriz.test/projects/1?search=new');f.ready();
  assert.equal(f.focuses.at(-1)[0],'project-search');detach();
});

test('React announces loading, marks current routes and only applies initial route focus once',async()=>{
  const source=await readFile(new URL('./main.jsx',import.meta.url),'utf8');
  assert.match(source,/aria-busy="true"><p role="status" aria-live="polite"/);
  assert.match(source,/!loading&&!presented.current/);
  for(const type of ['home','projects','project'])assert.ok(source.includes(`aria-current={!securityOpen&&route.type==='${type}'?'page':undefined}`));
  assert.match(source,/id="main-content" tabIndex=\{-1\}/);
  assert.match(source,/href="#main-content"/);
});

test('malformed fragments do not throw',()=>{
  const f=fixture();f.browser.location.hash='#%xx';assert.doesNotThrow(()=>f.ready());
});
