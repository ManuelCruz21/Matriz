import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import React from 'react';
import { transformWithOxc } from 'vite';

// Exercise the actual component handlers using the project's JSX/state harness pattern.
const source = await readFile(new URL('./security-center.jsx', import.meta.url), 'utf8');
const { code } = await transformWithOxc(source.replace(/^import .*;\r?\n/gm, '').replace('export default function', 'function'), 'security-center.jsx', { jsx: { runtime: 'classic' } });
function mount() {
  const state = [], downloads = [], revoked = [];
  let index = 0, blob;
  const useState = initial => {
    const slot = index++;
    if (!(slot in state)) state[slot] = initial;
    return [state[slot], value => { state[slot] = value; }];
  };
  const anchor = { click() { downloads.push({ filename: this.download, href: this.href, blob }); }, remove() { this.removed = true; } };
  const document = { createElement: () => anchor, body: { appendChild(node) { assert.equal(node, anchor); } } };
  const url = { createObjectURL(value) { blob = value; return 'blob:local-report'; }, revokeObjectURL(value) { revoked.push(value); } };
  const Component = new Function('React', 'useState', 'useMemo', 'document', 'URL', 'fetch', `${code}; return SecurityCenter;`)(
    React, useState, fn => fn(), document, url, () => assert.fail('Security Center must not send network requests'));
  const nodes = node => !node || typeof node !== 'object' ? [] : Array.isArray(node) ? node.flatMap(nodes) : [node, ...nodes(node.props?.children)];
  const text = node => node == null || typeof node === 'boolean' ? '' : typeof node !== 'object' ? String(node) : Array.isArray(node) ? node.map(text).join('') : text(node.props?.children);
  const render = () => { index = 0; return Component(); };
  const elements = () => nodes(render());
  const click = label => {
    const button = elements().find(node => node.type === 'button' && text(node) === label);
    assert.ok(button, `button exists: ${label}`);
    button.props.onClick();
  };
  return { click, elements, text: () => text(render()), downloads, revoked, anchor,
    report: () => elements().find(node => node.type === 'pre')?.props.children,
    import(value) { click('Event data'); elements().find(node => node.type === 'textarea').props.onChange({ target: { value } }); click('Analyze JSON'); },
  };
}
const event = (type, ip) => ({ time: '12:00', type, source: 'Sanitized test events', detail: 'Test detail', ...(ip === undefined ? {} : { ip }) });
const repeated = (count, ip = '192.0.2.20') => Array.from({ length: count }, () => event('auth_failed', ip));

test('demo source is explicit and daily report counts and Blue Team findings match the sample dataset', () => {
  const page = mount();
  assert.match(page.text(), /DEMO · Fictional sample events/);
  page.click('Daily Report Agent');
  page.click('Generate daily report');
  const report = page.report();
  for (const expected of ['Source: Sample events (demo)', 'Total events: 9', 'API errors: 2', 'Failed logins: 5', 'Access denied: 1', 'Service restored: 1', '[HIGH] Repeated authentication failures', '[MEDIUM] Unauthorized access attempts', '[MEDIUM] Application errors detected', 'no date filtering', 'No live server logs are collected', 'No email is sent']) assert.ok(report.includes(expected), expected);
  page.click('Blue Team Agent');
  assert.match(page.text(), /5 failed logins from 192\.0\.2\.10/);
  assert.match(page.text(), /No time-window correlation is applied/);
});

test('authentication threshold groups by IP and triggers at five, not four or missing IPs', () => {
  const page = mount();
  page.import(JSON.stringify([...repeated(4), ...repeated(4, '192.0.2.21'), ...repeated(6, '')]));
  page.click('Blue Team Agent');
  assert.match(page.text(), /No rule-based alerts/);
  assert.doesNotMatch(page.text(), /Repeated authentication failures/);
  page.import(JSON.stringify([...repeated(5), ...repeated(4, '192.0.2.21')]));
  page.click('Daily Report Agent'); page.click('Generate daily report');
  assert.match(page.report(), /5 failed logins from 192\.0\.2\.20/);
  assert.doesNotMatch(page.report(), /failed logins from 192\.0\.2\.21/);
});

test('empty imported dataset has no alerts and is not presented as fictional demo data', () => {
  const page = mount(); page.import('[]');
  assert.match(page.text(), /LOCAL · Unverified imported events/);
  page.click('Overview');
  assert.match(page.text(), /0 rule-based alert\(s\) available/);
  page.click('Daily Report Agent'); page.click('Generate daily report');
  assert.match(page.report(), /Source: User-provided events \(not independently verified\)/);
  assert.match(page.report(), /Total events: 0/);
  assert.match(page.report(), /\[LOW\] No rule-based alerts/);
  assert.match(page.report(), /does not establish that the system is secure/);
});

test('successful imports invalidate old reports and restoring demo events resets source, input and report', () => {
  const page = mount(); page.click('Daily Report Agent'); page.click('Generate daily report');
  page.import(JSON.stringify([event('service_restored')]));
  page.click('Daily Report Agent'); assert.equal(page.report(), undefined);
  page.click('Generate daily report'); assert.match(page.report(), /Service restored: 1/);
  page.click('Event data'); page.click('Restore demo events');
  assert.match(page.text(), /DEMO · Fictional sample events/);
  assert.equal(page.elements().find(node => node.type === 'textarea').props.value, '');
  page.click('Daily Report Agent'); assert.equal(page.report(), undefined);
  page.click('Generate daily report'); assert.match(page.report(), /Total events: 9/);
});

test('invalid JSON and invalid event fields preserve the current events and generated report', () => {
  const page = mount(); page.click('Daily Report Agent'); page.click('Generate daily report');
  const previous = page.report();
  const invalid = ['{', '{}', '[null]', '[[]]', JSON.stringify([event('unknown')]), JSON.stringify([{ ...event('api_error'), time: 12 }]), JSON.stringify([{ ...event('api_error'), detail: 'x'.repeat(501) }]), JSON.stringify([{ ...event('api_error'), source: 'x'.repeat(101) }]), JSON.stringify([{ ...event('api_error'), time: 'x'.repeat(51) }]), JSON.stringify([{ ...event('auth_failed'), ip: 42 }]), JSON.stringify([{ ...event('auth_failed'), ip: 'x'.repeat(65) }]), JSON.stringify(Array.from({ length: 501 }, () => event('api_error')))];
  for (const value of invalid) {
    page.import(value);
    assert.match(page.text(), /Import failed:/);
    assert.match(page.text(), /DEMO · Fictional sample events/);
    page.click('Daily Report Agent'); assert.equal(page.report(), previous);
  }
});

test('import accepts the documented field and event-count limits', () => {
  const page = mount();
  const boundary = { time: 't'.repeat(50), type: 'api_error', source: 's'.repeat(100), detail: 'd'.repeat(500), ip: 'i'.repeat(64) };
  page.import(JSON.stringify(Array.from({ length: 500 }, () => boundary)));
  assert.match(page.text(), /Events loaded locally/);
  page.click('Daily Report Agent'); page.click('Generate daily report');
  assert.match(page.report(), /Total events: 500/);
  assert.match(page.report(), /API errors: 500/);
});

test('report download contains the generated text and releases its temporary object URL', async () => {
  const page = mount(); page.click('Daily Report Agent'); page.click('Generate daily report');
  const expected = page.report(); page.click('Download .txt report');
  assert.equal(page.downloads.length, 1);
  assert.equal(page.downloads[0].filename, 'matriz-security-report.txt');
  assert.equal(page.downloads[0].blob.type, 'text/plain;charset=utf-8');
  assert.equal(await page.downloads[0].blob.text(), expected);
  assert.deepEqual(page.revoked, ['blob:local-report']);
  assert.equal(page.anchor.removed, true);
});
