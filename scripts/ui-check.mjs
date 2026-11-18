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
import { mkdirSync, existsSync, writeFileSync } from 'node:fs';
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
// Git Bash(MSYS)는 '/concepts' 같은 인자를 'C:/Program Files/Git/concepts'로 바꿔 넘긴다 → 되돌린다
const undoMsysPath = (r) => r.replace(/^[A-Za-z]:[\\/].*?[\\/]Git[\\/]/i, '/');
const routes = args.filter((a) => !a.startsWith('--') && !optionValues.has(a)).map(undoMsysPath);
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

// 비로그인 상태에서 앱 시작 시 세션 복원(POST /api/auth/refresh)이 401 AUTH_002를 받는 것은 정상이다 (F-02 AC8).
// 이 응답과 브라우저가 남기는 "Failed to load resource ... 401" 콘솔 로그는 문제로 세지 않는다.
function isExpectedRefresh401(url = '', text = '') {
  return String(url).includes('/api/auth/refresh') && String(text).includes('401');
}

const browser = await chromium.launch();
const viewports = [{ name: 'desktop', width, height: 900 }];
if (mobile) viewports.push({ name: 'mobile', width: 375, height: 812 });

let problems = 0;
const report = [];
// Refresh 토큰은 1회용(재발급마다 회전)이다. 앞 viewport가 쿠키를 쓰고 나면 파일의 쿠키는 무효가 되므로
// context가 끝날 때의 쿠키를 다음 viewport에 넘기고 파일에도 다시 저장한다 (다음 실행도 로그인 유지).
let state = storage && existsSync(storage) ? storage : undefined;
for (const vp of viewports) {
  const context = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
    storageState: state,
  });
  for (const route of routes) {
    const page = await context.newPage();
    const issues = [];
    page.on('console', (m) => m.type() === 'error' && !isExpectedRefresh401(m.location()?.url, m.text()) && issues.push(`console: ${m.text().slice(0, 200)}`));
    page.on('pageerror', (e) => issues.push(`exception: ${String(e.message).slice(0, 200)}`));
    page.on('response', (r) => r.status() >= 400 && !isExpectedRefresh401(r.url(), String(r.status())) && issues.push(`HTTP ${r.status()} ${r.request().method()} ${r.url().replace(base, '')}`));
    page.on('requestfailed', (r) => issues.push(`요청 실패: ${r.url().replace(base, '')} (${r.failure()?.errorText})`));

    let finalUrl = '';
    try {
      await page.goto(base + route, { waitUntil: 'networkidle', timeout: 20_000 });
      finalUrl = page.url().replace(base, '');
      if (state && finalUrl.startsWith('/login') && !route.startsWith('/login')) {
        issues.push(`로그인 화면으로 이동됨: --storage의 세션이 만료되었거나 이미 사용된 refresh 쿠키입니다`);
      }
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
  if (state) {
    state = await context.storageState();
    writeFileSync(storage, JSON.stringify(state, null, 2));
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
