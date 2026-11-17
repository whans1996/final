#!/usr/bin/env node
// 실제 화면 점검: Playwright(headless Chromium)로 라우트를 열어
// 콘솔 에러, 페이지 예외, 4xx/5xx 응답, 빈 화면 의심을 수집하고 스크린샷을 남긴다.
//
// 사전 조건: FE dev 서버 실행 중 (cd frontend && npm run dev), BE는 local 프로필(mock LLM)
//           frontend에 playwright 설치: npm i -D playwright && npx playwright install chromium
//
// 사용법: node scripts/ui-check.mjs [route ...] [--base http://localhost:5173] [--storage .harness/auth.json] [--width 1280]
//   --storage  로그인 상태(storageState JSON). 로그인 필요한 화면을 볼 때 사용
//   --mobile   375px 폭으로도 한 번 더 확인
import { mkdirSync, existsSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join, relative } from 'node:path';
import { ROOT } from './lib/common.mjs';

const args = process.argv.slice(2);
const opt = (name, def) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : def;
};
const base = opt('--base', 'http://localhost:5173').replace(/\/$/, '');
const storage = opt('--storage', null);
const width = Number(opt('--width', 1280));
const mobile = args.includes('--mobile');
const optionValues = new Set(['--base', '--storage', '--width'].map((n) => opt(n, null)).filter(Boolean));
const routes = args.filter((a) => !a.startsWith('--') && !optionValues.has(a));
if (routes.length === 0) routes.push('/');

let chromium;
try {
  const req = createRequire(join(ROOT, 'frontend', 'package.json'));
  ({ chromium } = req('playwright'));
} catch {
  console.log('⏭  playwright가 frontend에 설치되어 있지 않습니다.\n   cd frontend && npm i -D playwright && npx playwright install chromium');
  process.exit(2);
}

const outDir = join(ROOT, '.harness', 'screens');
mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch();
const viewports = [{ name: 'desktop', width, height: 900 }];
if (mobile) viewports.push({ name: 'mobile', width: 375, height: 812 });

let problems = 0;
const report = [];
for (const vp of viewports) {
  const context = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
    storageState: storage && existsSync(storage) ? storage : undefined,
  });
  for (const route of routes) {
    const page = await context.newPage();
    const issues = [];
    page.on('console', (m) => m.type() === 'error' && issues.push(`console: ${m.text().slice(0, 200)}`));
    page.on('pageerror', (e) => issues.push(`exception: ${String(e.message).slice(0, 200)}`));
    page.on('response', (r) => r.status() >= 400 && issues.push(`HTTP ${r.status()} ${r.request().method()} ${r.url().replace(base, '')}`));
    page.on('requestfailed', (r) => issues.push(`요청 실패: ${r.url().replace(base, '')} (${r.failure()?.errorText})`));

    let finalUrl = '';
    try {
      await page.goto(base + route, { waitUntil: 'networkidle', timeout: 20_000 });
      finalUrl = page.url().replace(base, '');
      const text = (await page.locator('body').innerText()).trim();
      if (text.length < 20) issues.push(`빈 화면 의심 (본문 텍스트 ${text.length}자)`);
    } catch (e) {
      issues.push(`로드 실패: ${String(e.message).split('\n')[0]}`);
    }
    const file = join(outDir, `${vp.name}${route.replace(/[^\w-]+/g, '_') || '_root'}.png`);
    await page.screenshot({ path: file, fullPage: true }).catch(() => {});
    problems += issues.length;
    report.push({
      route,
      vp: vp.name,
      redirected: finalUrl && finalUrl !== route ? finalUrl : null,
      issues,
      shot: relative(ROOT, file).replace(/\\/g, '/'),
    });
    await page.close();
  }
  await context.close();
}
await browser.close();

for (const r of report) {
  console.log(`${r.issues.length ? '❌' : '✅'} [${r.vp}] ${r.route}${r.redirected ? ` → ${r.redirected}` : ''}  (스크린샷: ${r.shot})`);
  for (const i of r.issues.slice(0, 8)) console.log(`     ${i}`);
  if (r.issues.length > 8) console.log(`     … 외 ${r.issues.length - 8}개`);
}
console.log(problems ? `\n결과: 문제 ${problems}건` : '\n결과: 통과');
process.exit(problems ? 1 : 0);
