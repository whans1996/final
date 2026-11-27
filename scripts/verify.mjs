#!/usr/bin/env node
// 검증 실행기: BE/FE/하네스의 포맷·린트·테스트·빌드를 돌리고 "실패한 부분만" 짧게 요약한다.
// 전체 로그는 .harness/logs/ 에 저장한다. AI 컨텍스트(토큰)를 아끼기 위한 도구다.
//
// 사용법: node scripts/verify.mjs [be|fe|harness|all] [--fix] [--staged]
//        node scripts/verify.mjs be --tests <패턴> [--tests <패턴>…]
//   be       backend: (spotlessCheck | --fix면 spotlessApply) → test
//   fe       frontend: lint → test:unit/test (--run) → build
//   harness  scripts/test 단위 테스트
//   all      be·fe (기본값)
//   --staged 커밋 hook용 빠른 검사 (H-1): 스테이징된 파일의 영역만, 포맷·린트와 관련 테스트만 돈다
//            (scripts/lib/related.mjs). 빌드와 전체 테스트는 pre-push·CI가 한다
//   --tests  BE 테스트 일부만 gradle `test --tests <패턴>`으로 돈다 (H-9, be 전용). verifier가 gradlew를 직접 불러 잠금을 우회하지 않게 한다
// BE 단계는 .harness/gradle.lock으로 직렬화한다 (같은 PC에서 Gradle 동시 실행 방지).
// --staged·--fix·--tests 없이 통과하고, 그 영역에 커밋 안 된 변경이 없고, 검증하는 동안 HEAD가 그대로면
// .harness/verified.json에 HEAD를 남긴다 (pre-push가 같은 커밋을 다시 돌리지 않게, #133).
// 종료 코드: 0 = 모두 통과 또는 건너뜀, 1 = 실패, 2 = 사용법 오류
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { basename, join, relative } from 'node:path';
import { ROOT, git, isWin } from './lib/common.mjs';
import { acquireLock } from './lib/lock.mjs';
import { HARNESS_DATA_FILES, SEED_CATEGORIES, SEED_DATA_PATHS, areasOf, isSeedData, frontendCodeFiles, relatedBackendTests, relatedHarnessTests } from './lib/related.mjs';
import { logsToPrune, parseVerifyArgs, shouldRecord } from './lib/verify.mjs';

const args = process.argv.slice(2);
const parsed = parseVerifyArgs(args);
if (parsed.error) {
  console.error(parsed.error);
  process.exit(2);
}
const { fix, staged, tests: testFilters } = parsed;
let targets = parsed.targets;
const stagedFiles = staged ? git(['diff', '--cached', '--name-only', '--diff-filter=ACMR']).out.split('\n').filter(Boolean) : [];
if (staged) targets = targets.length ? areasOf(stagedFiles).filter((a) => targets.includes(a)) : areasOf(stagedFiles);
// 검증 기록은 시작 때의 HEAD로 판정한다: 검증 중에 생긴 커밋을 통과로 남기지 않는다 (#133)
const startHead = git(['rev-parse', 'HEAD']).out;

const MAX_LINES_PER_STEP = 15;
const harnessDir = join(ROOT, '.harness');
const logDir = join(harnessDir, 'logs');
mkdirSync(logDir, { recursive: true });
const logFile = join(logDir, `verify-${new Date().toISOString().replace(/[:.]/g, '-')}.log`);
let fullLog = '';
const results = [];

const stripAnsi = (s) => s.replace(/\x1b\[[0-9;]*[A-Za-z]/g, '');
const quote = (a) => (/^[\w./:@=-]+$/.test(a) ? a : `"${a}"`);

const FAIL_PATTERN =
  /FAILED|FAIL\b|✗|×|Exception|Error\b|error:|AssertionError|expected|but was|Caused by|\.(java|vue|js|ts|mjs):\d+|BUILD FAILED|violations|✖|not ok/;

function summarize(out) {
  const lines = stripAnsi(out)
    .split(/\r?\n/)
    .map((l) => l.trimEnd())
    .filter((l) => l.trim());
  const picked = [];
  const seen = new Set();
  for (const l of lines) {
    if (!FAIL_PATTERN.test(l)) continue;
    if (/^\s+at (org\.junit|org\.gradle|java\.base|jdk\.|sun\.|node:internal)/.test(l)) continue; // 프레임워크 스택 제외
    const key = l.trim();
    if (seen.has(key)) continue;
    seen.add(key);
    picked.push(l.length > 220 ? l.slice(0, 220) + '…' : l);
    if (picked.length >= MAX_LINES_PER_STEP) break;
  }
  return picked.length ? picked : lines.slice(-MAX_LINES_PER_STEP);
}

function run(area, step, cmd, cwd) {
  const started = Date.now();
  const r = spawnSync(cmd, {
    cwd,
    shell: true,
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
    windowsHide: true,
    // GRADLE_OPTS: Gradle 클라이언트 출력도 UTF-8로 (Windows 기본 cp949면 한글 테스트 이름이 깨진다)
    env: {
      ...process.env,
      FORCE_COLOR: '0',
      CI: 'true',
      GRADLE_OPTS: [process.env.GRADLE_OPTS, '-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8']
        .filter(Boolean)
        .join(' '),
    },
  });
  const out = (r.stdout || '') + (r.stderr || '') + (r.error ? String(r.error) : '');
  fullLog += `\n===== [${area}] ${step}: ${cmd} (cwd=${relative(ROOT, cwd) || '.'}) exit=${r.status} =====\n${out}\n`;
  const ok = r.status === 0;
  results.push({ area, step, ok, secs: ((Date.now() - started) / 1000).toFixed(1), summary: ok ? [] : summarize(out) });
  return ok;
}

function skip(area, reason) {
  results.push({ area, step: '건너뜀', ok: true, skipped: true, reason });
}

function listFiles(dir, re) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name);
    if (e.isDirectory()) return listFiles(p, re);
    return re.test(e.name) ? [relative(ROOT, p).replace(/\\/g, '/')] : [];
  });
}

function verifyBackend() {
  const dir = join(ROOT, 'backend');
  const buildFile = ['build.gradle', 'build.gradle.kts'].map((f) => join(dir, f)).find(existsSync);
  if (!buildFile) return skip('be', 'backend 프로젝트가 아직 생성되지 않음');
  // 경로를 명시한다: NoDefaultCurrentDirectoryInExePath가 켜진 환경(Claude Code 등)에서는 cmd가 현재 폴더를 찾지 않는다
  const gradlew = isWin ? '.\\gradlew.bat' : './gradlew';
  const base = `${gradlew} --console=plain`;
  const release = acquireLock(join(harnessDir, 'gradle.lock'), {
    label: `verify ${args.join(' ')}`.trim(),
    onWait: (info) => console.log(`⏳ 다른 Gradle 실행이 끝나기를 기다립니다 (pid ${info?.pid}, ${info?.label || '?'})…`),
  });
  if (!release) {
    results.push({ area: 'be', step: 'gradle 잠금', ok: false, secs: '0', summary: ['.harness/gradle.lock 대기 시간 초과. 멈춘 Gradle이 있는지 확인하세요'] });
    return;
  }
  try {
    if (testFilters.length) return void run('be', `test ${testFilters.join(' ')}`, `${base} test ${testFilters.map((t) => `--tests ${quote(t)}`).join(' ')}`, dir);
    const usesSpotless = /spotless/.test(readFileSync(buildFile, 'utf8'));
    if (usesSpotless) {
      if (!run('be', fix ? 'spotlessApply' : 'spotlessCheck', `${base} ${fix ? 'spotlessApply' : 'spotlessCheck'}`, dir)) {
        if (!fix) return; // 포맷 실패면 테스트까지 갈 필요 없음
      }
    }
    if (!staged) return void run('be', 'test', `${base} test`, dir);
    const tests = relatedBackendTests(stagedFiles, listFiles(join(dir, 'src', 'test'), /\.java$/));
    if (!tests.length) return void run('be', 'compileTestJava (관련 테스트 없음)', `${base} compileTestJava`, dir);
    run('be', `관련 테스트 ${tests.length}개`, `${base} test ${tests.map((t) => `--tests ${t}`).join(' ')}`, dir);
  } finally {
    release();
  }
}

function verifyFrontend() {
  const dir = join(ROOT, 'frontend');
  const pkgPath = join(dir, 'package.json');
  if (!existsSync(pkgPath)) return skip('fe', 'frontend 프로젝트가 아직 생성되지 않음');
  if (!existsSync(join(dir, 'node_modules'))) {
    results.push({ area: 'fe', step: 'install', ok: false, secs: '0', summary: ['node_modules 없음 → cd frontend && npm install'] });
    return;
  }
  if (staged) return verifyFrontendStaged(dir);
  const scripts = JSON.parse(readFileSync(pkgPath, 'utf8')).scripts || {};
  if (scripts.lint) {
    const lintCmd = fix ? 'npm run lint' : scripts['lint:check'] ? 'npm run lint:check' : 'npm run lint';
    run('fe', 'lint', lintCmd, dir);
  }
  const testScript = scripts['test:unit'] ? 'test:unit' : scripts.test ? 'test' : null;
  if (testScript) run('fe', 'test', `npm run ${testScript} -- --run`, dir);
  if (scripts.build) run('fe', 'build', 'npm run build', dir);
  verifySeedCards();
}

// 시드 카드 ①형식·②FE 예제 검사 (K-3a). FE 의존성(typescript·@vue/compiler-sfc)을 쓰므로 fe 영역에서 돈다
function verifySeedCards() {
  if (!existsSync(join(ROOT, 'data', 'seed'))) return;
  run('fe', '시드 카드 검사 (seed-check)', 'node scripts/seed-check.mjs', ROOT);
  if (!staged) run('fe', '시드 예제 검사 단위 테스트', 'node --test scripts/test/seed-examples.test.mjs', ROOT);
}

// 스테이징된 파일만 린트하고, 그 파일에 닿는 단위 테스트만 돈다 (vitest related)
function verifyFrontendStaged(dir) {
  const seed = stagedFiles.some(isSeedData) || stagedFiles.includes(SEED_CATEGORIES);
  if (seed) verifySeedCards();
  const files = frontendCodeFiles(stagedFiles).filter((f) => existsSync(join(dir, f)));
  if (!files.length) return seed ? undefined : skip('fe', '스테이징된 코드 파일 없음');
  const scriptFiles = files.filter((f) => /\.(js|mjs|vue|ts)$/.test(f)).map(quote);
  const srcFiles = files.filter((f) => f.startsWith('src/')).map(quote);
  if (scriptFiles.length) {
    run('fe', 'oxlint', `node scripts/run-oxlint.mjs ${scriptFiles.join(' ')}`, dir);
    run('fe', 'eslint', `npx eslint --no-warn-ignored ${scriptFiles.join(' ')}`, dir);
  }
  if (srcFiles.length) run('fe', 'prettier', `npx prettier --check ${srcFiles.join(' ')}`, dir);
  // 파일 없이 부르면 vitest related가 전체를 돈다
  const testable = srcFiles.filter((f) => !/\.css"?$/.test(f));
  if (testable.length) run('fe', '관련 테스트', `npx vitest related ${testable.join(' ')} --run --passWithNoTests`, dir);
}

function verifyHarness() {
  const testFiles = listFiles(join(ROOT, 'scripts', 'test'), /\.test\.mjs$/);
  if (!testFiles.length) return skip('harness', 'scripts/test 없음');
  if (!staged) return void run('harness', 'test', `node --test "scripts/test/*.test.mjs"`, ROOT);
  const picked = relatedHarnessTests(stagedFiles, testFiles, (p) => readFileSync(join(ROOT, p), 'utf8'));
  if (!picked.length) return skip('harness', '관련 단위 테스트 없음 (전체는 pre-push)');
  run('harness', `관련 테스트 ${picked.length}개`, `node --test ${picked.join(' ')}`, ROOT);
}

// 하네스가 읽는 문서(로드맵)도 넣는다: 작업 트리에서만 고친 로드맵으로 통과한 결과를 HEAD에 남기지 않게 (Codex 리뷰)
const AREA_PATHS = { be: ['backend', ...SEED_DATA_PATHS], fe: ['frontend', ...SEED_DATA_PATHS, SEED_CATEGORIES], harness: ['scripts', '.claude/hooks', '.githooks', ...HARNESS_DATA_FILES] };

// 전체 검증을 통과한 커밋을 남긴다. 검증 중 HEAD가 바뀌었거나 그 영역에 커밋 안 된 변경이 있으면 HEAD와 검증한 내용이 달라 남기지 않는다
function recordVerified(areas) {
  const head = git(['rev-parse', 'HEAD']).out;
  const { reason } = shouldRecord({ startHead, endHead: head, dirty: false });
  if (reason === 'head-changed') console.log(`ℹ️  검증 중 HEAD가 바뀌어(${startHead.slice(0, 7)} → ${head.slice(0, 7)}) 검증 기록을 남기지 않음`);
  if (reason) return;
  const path = join(harnessDir, 'verified.json');
  let data = {};
  try {
    data = JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    data = {};
  }
  for (const a of areas) {
    const dirty = !!git(['status', '--porcelain', '--', ...AREA_PATHS[a]]).out;
    if (shouldRecord({ startHead, endHead: head, dirty }).record) data[a] = head;
  }
  writeFileSync(path, JSON.stringify(data, null, 2));
}

if (staged && targets.length === 0) {
  console.log('⏭  스테이징된 파일에 검증할 영역이 없습니다');
  process.exit(0);
}
if (targets.includes('be')) verifyBackend();
if (targets.includes('fe')) verifyFrontend();
if (targets.includes('harness')) verifyHarness();

writeFileSync(logFile, fullLog || '(실행된 단계 없음)\n', 'utf8');
// 오래된 verify 로그 정리 (#180). 부가 작업이라 실패해도(다른 프로세스가 잡은 파일 등) 검증 결과에 영향을 주지 않는다
for (const name of logsToPrune(readdirSync(logDir), undefined, basename(logFile))) {
  try {
    rmSync(join(logDir, name), { force: true });
  } catch {}
}

const failed = results.filter((r) => !r.ok);
if (parsed.record) {
  const ran = targets.filter((t) => results.some((r) => r.area === t && !r.skipped));
  const passed = ran.filter((t) => !failed.some((r) => r.area === t));
  if (passed.length) recordVerified(passed);
}
const lines = [];
if (staged) lines.push(`⚡ 빠른 검사 (스테이징된 파일 ${stagedFiles.length}개, 전체는 pre-push·CI)`);
for (const r of results) {
  if (r.skipped) lines.push(`⏭  [${r.area}] ${r.reason}`);
  else lines.push(`${r.ok ? '✅' : '❌'} [${r.area}] ${r.step} (${r.secs}s)`);
  for (const s of r.summary || []) lines.push(`     ${s}`);
}
lines.push(failed.length ? `\n결과: 실패 ${failed.length}개` : '\n결과: 통과');
if (fullLog) lines.push(`전체 로그: ${relative(ROOT, logFile).replace(/\\/g, '/')}`);
console.log(lines.join('\n'));
process.exit(failed.length ? 1 : 0);
