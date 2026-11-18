import React, { useMemo, useState } from 'react';
import './security-center.css';
import { request } from './api.js';
import { auditPlatform, auditReport } from './platform-audit.js';

const DEMO_EVENTS = [
  {time:'09:02',type:'api_error',source:'API /projects',detail:'HTTP 500 on project listing',severity:'high'},
  {time:'09:08',type:'auth_failed',source:'Authentication',detail:'Failed login from 192.0.2.10',severity:'medium',ip:'192.0.2.10'},
  {time:'09:09',type:'auth_failed',source:'Authentication',detail:'Failed login from 192.0.2.10',severity:'medium',ip:'192.0.2.10'},
  {time:'09:10',type:'auth_failed',source:'Authentication',detail:'Failed login from 192.0.2.10',severity:'medium',ip:'192.0.2.10'},
  {time:'09:12',type:'auth_failed',source:'Authentication',detail:'Failed login from 192.0.2.10',severity:'medium',ip:'192.0.2.10'},
  {time:'09:13',type:'auth_failed',source:'Authentication',detail:'Failed login from 192.0.2.10',severity:'medium',ip:'192.0.2.10'},
  {time:'10:24',type:'access_denied',source:'API /admin',detail:'Unauthorized access attempt (403)',severity:'high'},
  {time:'11:01',type:'api_error',source:'API /tasks',detail:'Request timeout',severity:'medium'},
  {time:'11:30',type:'service_restored',source:'API /projects',detail:'Endpoint responding normally',severity:'low'},
];
const ALLOWED = new Set(['api_error','auth_failed','access_denied','service_restored']);
function analyze(events) {
  const errors=events.filter(e=>e.type==='api_error');
  const denied=events.filter(e=>e.type==='access_denied');
  const failed=events.filter(e=>e.type==='auth_failed');
  const restored=events.filter(e=>e.type==='service_restored');
  const ipCounts=new Map();
  for(const e of failed) if(e.ip)ipCounts.set(e.ip,(ipCounts.get(e.ip)||0)+1);
  const findings=[];
  for(const [ip,count] of ipCounts) if(count>=5)findings.push({level:'high',title:'Repeated authentication failures',detail:`${count} failed logins from ${ip}. Review authentication logs, rate limiting and account protection.`});
  if(denied.length)findings.push({level:'medium',title:'Unauthorized access attempts',detail:`${denied.length} access-denied event(s). Verify permissions and investigate unexpected access patterns.`});
  if(errors.length)findings.push({level:'medium',title:'Application errors detected',detail:`${errors.length} API error(s). Check server health and error logs before considering remediation.`});
  if(!findings.length)findings.push({level:'low',title:'No rule-based alerts',detail:'No suspicious patterns matched the available events. This does not establish that the system is secure.'});
  return {errors,denied,failed,restored,findings};
}
function reportText(events,result) {
  const date=new Date().toLocaleDateString('en-GB');
  return [`MATRIZ — DAILY RELIABILITY REPORT`, `Generated: ${date}`,`Source: ${events===DEMO_EVENTS?'Sample events (demo)':'User-provided events (not independently verified)'}`,'',`Total events: ${events.length}`,`API errors: ${result.errors.length}`,`Failed logins: ${result.failed.length}`,`Access denied: ${result.denied.length}`,`Service restored: ${result.restored.length}`,'','BLUE TEAM FINDINGS',...result.findings.map(f=>`[${f.level.toUpperCase()}] ${f.title}\n${f.detail}`),'','Scope: All loaded events; no date filtering. Generated date is the report creation date.','Notes: This is a local, rule-based demonstration. No live server logs are collected. No email is sent.'].join('\n');
}
function downloadReport(text) {
  const blob=new Blob([text],{type:'text/plain;charset=utf-8'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a');a.href=url;a.download='matriz-security-report.txt';document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);
}
export default function SecurityCenter() {
  const [events,setEvents]=useState(DEMO_EVENTS);
  const [input,setInput]=useState('');
  const [message,setMessage]=useState('');
  const [tab,setTab]=useState('overview');
  const [report,setReport]=useState('');
  const [audit,setAudit]=useState(null);
  const [auditBusy,setAuditBusy]=useState(false);
  const result=useMemo(()=>analyze(events),[events]);
  async function runAudit() {
    if(auditBusy)return;
    setAuditBusy(true);setMessage('');
    try {await request('/security/access');setAudit(await auditPlatform());}
    catch {setMessage('Security audit could not finish. Check application connectivity.');}
    finally {setAuditBusy(false);}
  }
  function importEvents() {
    try {
      const data=JSON.parse(input);
      if(!Array.isArray(data)||data.length>500||data.some(e=>!e||typeof e!=='object'||!ALLOWED.has(e.type)||typeof e.detail!=='string'||e.detail.length>500||typeof e.source!=='string'||e.source.length>100||typeof e.time!=='string'||e.time.length>50||(e.ip!==undefined&&(typeof e.ip!=='string'||e.ip.length>64))))throw Error('Expected an array (max 500) with time, type, source and detail.');
      setEvents(data.map(e=>({time:e.time,type:e.type,source:e.source,detail:e.detail,ip:e.ip||''})));
      setReport('');setMessage('Events loaded locally. No data was sent to a server.');
    }catch(e){setMessage(`Import failed: ${e.message}`);}
  }
  return <section className="security-center" aria-label="Security Center">
    <div className="sc-heading"><div><p className="sc-eyebrow">SECURITY & RELIABILITY</p><h2>Security Center</h2><p>Daily reliability reporting and defensive event analysis.</p></div><span className="sc-demo">{events===DEMO_EVENTS?'DEMO · Fictional sample events':'LOCAL · Unverified imported events'}</span></div>
    <div className="sc-stats"><div><small>Events analyzed</small><strong>{events.length}</strong></div><div><small>API errors</small><strong>{result.errors.length}</strong></div><div><small>Failed logins</small><strong>{result.failed.length}</strong></div><div><small>Security findings</small><strong>{result.findings.filter(f=>f.level!=='low').length}</strong></div></div>
    <div className="sc-tabs" role="group" aria-label="Security Center views"><button type="button" aria-pressed={tab==='overview'} onClick={()=>setTab('overview')}>Overview</button><button type="button" aria-pressed={tab==='report'} onClick={()=>setTab('report')}>Daily Report Agent</button><button type="button" aria-pressed={tab==='blue'} onClick={()=>setTab('blue')}>Blue Team Agent</button><button type="button" aria-pressed={tab==='data'} onClick={()=>setTab('data')}>Event data</button><button type="button" aria-pressed={tab==='audit'} onClick={()=>setTab('audit')}>Platform Pentesting Agent</button></div>
    {tab==='audit'&&<div className="sc-panel"><h3>Platform Pentesting Agent — Safe Audit</h3><p>Runs real, read-only checks against this Matriz installation: frontend response, HTTP security headers, and backend health. No brute force, injection payloads, or destructive operations. This is a limited security posture audit, not a full penetration test.</p><button type="button" disabled={auditBusy} onClick={runAudit}>{auditBusy?'Auditing…':'Run security audit'}</button>{audit&&<><p className="sc-note">Last scan: {new Date(audit.generatedAt).toLocaleString('pt-PT')} · Scope: {audit.origin}</p><div className="sc-findings">{audit.checks.map((check,i)=><div className="sc-finding" key={i}><span className={`sc-severity sc-${check.level}`}>{check.level.toUpperCase()}</span><div><b>{check.title}</b><p><strong>Evidence:</strong> {check.evidence}</p><p><strong>Recommendation:</strong> {check.recommendation}</p></div></div>)}</div><div className="sc-actions"><button type="button" className="sc-secondary" onClick={()=>downloadReport(auditReport(audit))}>Download audit report (.txt)</button></div></>}</div>}
    {tab==='overview'&&<div className="sc-panel"><h3>Agent status</h3><p><b>Daily Report Agent</b> — Ready to generate a reliability summary.</p><p><b>Blue Team Agent</b> — {result.findings.filter(f=>f.level!=='low').length} rule-based alert(s) available.</p><p className="sc-note">Both agents run entirely in your browser on sample events by default. No live monitoring, autonomous actions or AI model calls are claimed.</p></div>}
    {tab==='report'&&<div className="sc-panel"><h3>Daily Report Agent</h3><p>Summarizes all loaded events, not a date-filtered day. The generated date is the report creation date. No backend logs are collected or email sent.</p><button type="button" onClick={()=>{setReport(reportText(events,result));setMessage('Report generated successfully.');}}>Generate daily report</button>{report&&<><pre className="sc-report">{report}</pre><button type="button" className="sc-secondary" onClick={()=>downloadReport(report)}>Download .txt report</button></>}</div>}
    {tab==='blue'&&<div className="sc-panel"><h3>Blue Team Agent</h3><p>Defensive, rule-based detection. Five or more failed logins from the same IP across all loaded events trigger a high-severity finding. No time-window correlation is applied.</p><div className="sc-findings">{result.findings.map((f,i)=><div className="sc-finding" key={i}><span className={`sc-severity sc-${f.level}`}>{f.level.toUpperCase()}</span><div><b>{f.title}</b><p>{f.detail}</p></div></div>)}</div><p className="sc-note">Recommendations require human review. No IP blocking, account changes or server access are performed.</p></div>}
    {tab==='data'&&<div className="sc-panel"><h3>Event data</h3><p>Sample events are fictional. Import a JSON array to demonstrate analysis with your own sanitized events.</p><div className="sc-table-wrap"><table className="sc-table"><thead><tr><th>Time</th><th>Type</th><th>Source</th><th>Details</th></tr></thead><tbody>{events.map((e,i)=><tr key={i}><td>{e.time}</td><td>{e.type}</td><td>{e.source}</td><td>{e.detail}</td></tr>)}</tbody></table></div><label>Import JSON events<textarea rows={5} value={input} onChange={e=>setInput(e.target.value)} placeholder='[{"time":"12:00","type":"api_error","source":"API /tasks","detail":"HTTP 500"}]'/></label><div className="sc-actions"><button type="button" onClick={importEvents}>Analyze JSON</button><button type="button" className="sc-secondary" onClick={()=>{setEvents(DEMO_EVENTS);setInput('');setReport('');setMessage('Sample events restored.');}}>Restore demo events</button></div></div>}
    {message&&<p role="status" className={message.startsWith('Import failed')?'sc-feedback sc-feedback-error':'sc-feedback'}>{message}</p>}
  </section>;
}
