import test from 'node:test';
import assert from 'node:assert/strict';
import {auditPlatform,auditReport} from './platform-audit.js';
const headers = new Headers({'x-content-type-options':'nosniff','content-security-policy':"default-src 'self'; frame-ancestors 'none'"});
test('same-origin read-only scan detects missing headers and reports evidence',async()=>{
 const urls=[];
 const fake=async(url,opts)=>{urls.push([url,opts.method,opts.credentials]);return {ok:true,status:200,headers};};
 const result=await auditPlatform(fake,'http://localhost:8080','http:');
 assert.deepEqual(urls.map(x=>x[0]),['http://localhost:8080/','http://localhost:8080/api/health']);
 assert.ok(urls.every(x=>x[1]==='GET'&&x[2]==='same-origin'));
 assert.ok(result.checks.some(x=>x.title==='Referrer policy'&&x.level==='medium'));
 assert.ok(result.checks.some(x=>x.title==='Framing protection'&&x.level==='pass'));
 assert.match(auditReport(result),/No active pentest/);
});
test('unavailable backend is unknown rather than confirmed vulnerability',async()=>{
 const result=await auditPlatform(async url=>{if(url.endsWith('/api/health'))throw Error('network');return {ok:true,status:200,headers:new Headers()};},'http://localhost:8080','http:');
 assert.equal(result.checks.find(x=>x.title==='Backend health endpoint').level,'unknown');
});
