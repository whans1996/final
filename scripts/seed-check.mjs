#!/usr/bin/env node
// 시드 카드 검수 ①형식 ②예제 실행 (K-3a AC10). 보고서: .harness/seed-check/report.md·report.json
//
// 사용법: node scripts/seed-check.mjs [--be] [--card <slug|category/slug>]
//   (기본)  ①형식(+ 생성본이 원본과 같은지) · ②FE(js·jsx·ts·tsx·vue·json, frontend/node_modules 필요)
//   --be    ②BE(java·sql·xml·yaml)도: Gradle로 SeedExampleTest를 돌려 backend/build/seed-check/result.json을 합친다.
//           Docker(Testcontainers)가 없으면 "BE 검사 건너뜀"을 알리고 종료 코드는 바꾸지 않는다
//   --card  그 카드만 보고한다 (이름 충돌·비슷한 요약은 전체 카드와 비교)
// 종료 코드: 오류가 있으면 1, 경고만 있으면 0. 오류는 CI 실패, 경고는 K-3c 플래그 목록 재료다.
// 테스트용: --src <카드 폴더> --sql <생성 SQL> --report-dir <보고서 폴더>
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { ROOT, isWin } from './lib/common.mjs';
import { acquireLock } from './lib/lock.mjs';
import { buildSql, readCards } from './lib/seed/card.mjs';
import { BE_LANGS, checkFeExamples, loadFeTools } from './lib/seed/examples.mjs';
import { checkFormat, loadRules } from './lib/seed/format.mjs';

const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
};
const rel = (p) => relative(ROOT, p).replace(/\\/g, '/');
const src = resolve(ROOT, opt('--src', 'data/seed/concepts'));
const sqlPath = resolve(ROOT, opt('--sql', 'backend/src/main/resources/db/migration/R__seed_concepts.sql'));
const reportDir = resolve(ROOT, opt('--report-dir', '.harness/seed-check'));
const cardArg = opt('--card', null);
const withBe = args.includes('--be');

const all = readCards(src, ROOT);
const rules = { ...loadRules(), src: rel(src) };
const matches = (file) => !cardArg || file.endsWith(`/${cardArg}.md`);
const cards = all.filter((c) => matches(c.file));
if (cardArg && !cards.length) {
  console.error(`❌ 카드를 찾을 수 없음: ${cardArg}`);
  process.exit(1);
}

const issues = checkFormat(all, rules).filter((i) => matches(i.file));
const skipped = [];
let unchecked = [];
const checked = {};

// 생성본이 원본과 같은지 (CI 하네스 잡의 seed-build --check와 같은 판정. 로컬 verify fe에서도 잡히게)
if (!all.some((c) => c.problems.length)) {
  const current = existsSync(sqlPath) ? readFileSync(sqlPath, 'utf8').replace(/\r\n/g, '\n') : null;
  if (current !== buildSql(all)) {
    issues.push({ file: rel(sqlPath), stage: '①', severity: 'error', item: 'generated', message: '생성본이 원본과 다름 → node scripts/seed-build.mjs' });
  }
}

const tools = loadFeTools();
if (tools) {
  const fe = checkFeExamples(cards, tools);
  issues.push(...fe.issues);
  unchecked = fe.unchecked;
  Object.assign(checked, fe.checked);
} else skipped.push('FE 검사(js·jsx·ts·tsx·vue·json): frontend/node_modules 없음 → cd frontend && npm ci');

if (!withBe) skipped.push(`BE 검사(${BE_LANGS.join('·')}): --be로 실행 (CI backend 잡의 SeedExampleTest)`);
else runBackend();

function runBackend() {
  const docker = spawnSync('docker', ['info', '--format', '{{.ServerVersion}}'], { encoding: 'utf8', timeout: 20000, windowsHide: true });
  if (docker.status !== 0) return void skipped.push('BE 검사 건너뜀: Docker 응답 없음 (Testcontainers 필요)');
  const resultFile = join(ROOT, 'backend', 'build', 'seed-check', 'result.json');
  rmSync(resultFile, { force: true });
  const release = acquireLock(join(ROOT, '.harness', 'gradle.lock'), {
    label: 'seed-check --be',
    onWait: (info) => console.log(`⏳ 다른 Gradle 실행이 끝나기를 기다립니다 (pid ${info?.pid}, ${info?.label || '?'})…`),
  });
  if (!release) return void issues.push({ file: '-', stage: '②', severity: 'error', item: 'be', message: '.harness/gradle.lock 대기 시간 초과' });
  const logFile = join(ROOT, '.harness', 'seed-check', 'gradle.log');
  let run;
  try {
    console.log('⏳ BE 검사 (SeedExampleTest)…');
    const gradlew = isWin ? '.\\gradlew.bat' : './gradlew';
    // --rerun: 카드·환경변수가 그대로면 test가 UP-TO-DATE로 건너뛰어 result.json(위에서 지움)이 다시 생기지 않는다
    run = spawnSync(`${gradlew} --console=plain test --tests "*SeedExampleTest" --rerun`, {
      cwd: join(ROOT, 'backend'),
      shell: true,
      encoding: 'utf8',
      maxBuffer: 256 * 1024 * 1024,
      windowsHide: true,
      env: { ...process.env, SEED_SRC: src, ...(cardArg ? { SEED_CARD: cardArg } : {}) },
    });
  } finally {
    release();
  }
  mkdirSync(join(ROOT, '.harness', 'seed-check'), { recursive: true });
  writeFileSync(logFile, `${run?.stdout ?? ''}${run?.stderr ?? ''}${run?.error ? `\n${run.error}` : ''}`);
  if (!existsSync(resultFile)) {
    const tail = `${run?.stdout ?? ''}${run?.stderr ?? ''}`.trim().split(/\r?\n/).slice(-15).join('\n');
    if (tail) console.error(tail);
    return void issues.push({ file: '-', stage: '②', severity: 'error', item: 'be', message: `BE 검사 결과 파일이 없음 (Gradle 종료 코드 ${run?.status ?? '?'}, 로그 ${rel(logFile)})` });
  }
  const be = JSON.parse(readFileSync(resultFile, 'utf8'));
  issues.push(...be.issues.map((i) => ({ ...i, file: rel(resolve(ROOT, i.file)) })).filter((i) => matches(i.file)));
  for (const [k, v] of Object.entries(be.checked ?? {})) checked[k] = (checked[k] ?? 0) + v;
}

const order = (a, b) => (a.file < b.file ? -1 : a.file > b.file ? 1 : 0) || a.stage.localeCompare(b.stage) || (a.line ?? 0) - (b.line ?? 0);
issues.sort(order);
const errors = issues.filter((i) => i.severity === 'error');
const warns = issues.filter((i) => i.severity === 'warn');
const totals = { cards: cards.length, errors: errors.length, warnings: warns.length, unchecked: unchecked.length };

const cell = (s) => String(s ?? '').replace(/\|/g, '\\|').replace(/\n/g, ' ');
const table = (list) =>
  list.length
    ? ['| 파일 | 단계 | 항목 | 줄 | 내용 |', '|---|---|---|---|---|', ...list.map((i) => `| ${cell(i.file)} | ${i.stage} | ${cell(i.item)} | ${i.line ?? ''} | ${cell(i.message)} |`)]
    : ['없음'];
const md = [
  '# 시드 카드 검사 보고서 (①형식 ②예제 실행)',
  '',
  `- 카드 ${totals.cards}장 · 오류 ${totals.errors} · 경고 ${totals.warnings} · 검사 안 한 블록 ${totals.unchecked}`,
  `- 검사한 블록: ${Object.entries(checked).map(([k, v]) => `${k} ${v}`).join(' · ') || '없음'}`,
  ...skipped.map((s) => `- 건너뜀: ${s}`),
  '',
  '## 오류 (CI 실패)',
  '',
  ...table(errors),
  '',
  '## 경고 (플래그 목록 재료)',
  '',
  ...table(warns),
  '',
  '## 검사 안 한 블록',
  '',
  ...(unchecked.length ? ['| 파일 | 언어 | 줄 |', '|---|---|---|', ...unchecked.map((u) => `| ${u.file} | ${u.lang} | ${u.line} |`)] : ['없음']),
  '',
].join('\n');
mkdirSync(reportDir, { recursive: true });
writeFileSync(join(reportDir, 'report.md'), md, 'utf8');
writeFileSync(join(reportDir, 'report.json'), JSON.stringify({ totals, checked, skipped, issues, unchecked }, null, 2), 'utf8');

console.log(`${errors.length ? '❌' : '✅'} 시드 카드 ${totals.cards}장 · 오류 ${totals.errors} · 경고 ${totals.warnings} · 검사 안 한 블록 ${totals.unchecked}`);
for (const s of skipped) console.log(`⏭  ${s}`);
for (const i of errors.slice(0, 30)) console.log(`  ❌ ${i.file}${i.line ? `:${i.line}` : ''} [${i.stage} ${i.item}] ${i.message}`);
if (errors.length > 30) console.log(`  … 외 ${errors.length - 30}건`);
console.log(`보고서: ${rel(join(reportDir, 'report.md'))}`);
process.exit(errors.length ? 1 : 0);
