// Non-invasive, same-origin security posture checks. No payloads or state-changing requests.
export async function auditPlatform(fetcher = fetch, origin = window.location.origin, protocol = window.location.protocol) {
  const checks = [];
  const add = (level, title, evidence, recommendation) => checks.push({level,title,evidence,recommendation});
  async function probe(path) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 7000);
    try {
      return await fetcher(new URL(path, origin).href, {method:'GET', credentials:'same-origin', cache:'no-store', redirect:'follow', signal:controller.signal});
    } finally {clearTimeout(timeout);}
  }
  let page;
  try {page = await probe('/');}
  catch (error) {add('unknown','Frontend availability','Could not retrieve the application entry point.','Check connectivity and reverse proxy logs.');}
  if (page) {
    add(page.ok?'pass':'high','Frontend HTTP response',`GET / returned HTTP ${page.status}.`,page.ok?'No action needed.':'Investigate frontend availability.');
    const headers = page.headers;
    const required = [
      ['content-security-policy','Content Security Policy','Define a restrictive Content-Security-Policy header to reduce script injection exposure.'],
      ['x-content-type-options','MIME sniffing protection','Set X-Content-Type-Options: nosniff.'],
      ['referrer-policy','Referrer policy','Set a Referrer-Policy header, e.g. strict-origin-when-cross-origin.'],
      ['x-frame-options','Framing protection','Use CSP frame-ancestors or X-Frame-Options to restrict framing.'],
    ];
    for (const [key,label,recommendation] of required) {
      const value = headers.get(key);
      if (key==='x-frame-options' && !value && /frame-ancestors\s/i.test(headers.get('content-security-policy')||'')) {
        add('pass',label,'CSP frame-ancestors directive is present.','No action needed.');
      } else add(value?'pass':'medium',label,value?`${key}: ${value.slice(0,180)}`:`${key} header not observed on GET /.`,value?'Review policy strength manually; presence alone is not proof of safety.':recommendation);
    }
    if (protocol === 'https:') {
      const hsts = headers.get('strict-transport-security');
      add(hsts?'pass':'medium','HTTPS transport policy',hsts?`HSTS: ${hsts.slice(0,180)}`:'HSTS header not observed on HTTPS.',hsts?'Review max-age and scope.':'Consider Strict-Transport-Security on HTTPS deployments.');
    } else add('info','Local HTTP environment','The application is accessed over HTTP.','Expected for localhost development; use HTTPS and secure cookies for production.');
  }
  try {
    const health = await probe('/api/health');
    add(health.ok?'pass':'medium','Backend health endpoint',`GET /api/health returned HTTP ${health.status}.`,health.ok?'Endpoint responded successfully.':'Check backend health and proxy routing.');
  } catch {add('unknown','Backend health endpoint','Health endpoint could not be reached.','Check backend connectivity.');}
  add('info','Authentication and authorization','Not tested: browser-only passive checks cannot verify role isolation, cookie HttpOnly attributes or injection resistance.','Run dedicated authenticated security tests in an isolated environment.');
  return {origin,generatedAt:new Date().toISOString(),checks};
}
export function auditReport(result) {
  return ['MATRIZ — PLATFORM SECURITY AUDIT','Generated: '+result.generatedAt,'Scope: '+result.origin,'Method: read-only same-origin HTTP checks; no exploitation or destructive testing.','',...result.checks.flatMap(c=>[`[${c.level.toUpperCase()}] ${c.title}`,`Evidence: ${c.evidence}`,`Recommendation: ${c.recommendation}`,'']),'Limitations: Header presence does not prove secure configuration. No active pentest, vulnerability exploitation, external scanning, AI inference, or authorization testing was performed.'].join('\n');
}
