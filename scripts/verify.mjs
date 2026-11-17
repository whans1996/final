#!/usr/bin/env node
// 검증 실행기: BE/FE의 포맷·린트·테스트·빌드를 돌리고 "실패한 부분만" 짧게 요약한다.
// 전체 로그는 .harness/logs/ 에 저장한다. AI 컨텍스트(토큰)를 아끼기 위한 도구다.
//
// 사용법: node scripts/verify.mjs [be|fe|all] [--fix]
//   be    backend: (spotlessCheck | --fix면 spotlessApply) → test
//   fe    frontend: lint → test:unit/test (--run) → build
//   all   둘 다 (기본값)
// 종료 코드: 0 = 모두 통과 또는 건너뜀, 1 = 실패
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { ROOT, isWin } from './lib/common.mjs';

const args = process.argv.slice(2);
const fix = args.includes('--fix');
let targets = args.filter((a) => !a.startsWith('--'));
if (targets.length === 0 || targets.includes('all')) targets = ['be', 'fe'];

const MAX_LINES_PER_STEP = 15;
const logDir = join(ROOT, '.harness', 'logs');
mkdirSync(logDir, { recursive: true });
const logFile = join(logDir, `verify-${new Date().toISOString().replace(/[:.]/g, '-')}.log`);
let fullLog = '';
const results = [];

const stripAnsi = (s) => s.replace(/\x1b\[[0-9;]*[A-Za-z]/g, '');

const FAIL_PATTERN =
  /FAILED|FAIL\b|✗|×|Exception|Error\b|error:|AssertionError|expected|but was|Caused by|\.(java|vue|js|ts):\d+|BUILD FAILED|violations|✖/;

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

function verifyBackend() {
  const dir = join(ROOT, 'backend');
  const buildFile = ['build.gradle', 'build.gradle.kts'].map((f) => join(dir, f)).find(existsSync);
  if (!buildFile) return skip('be', 'backend 프로젝트가 아직 생성되지 않음');
  // 경로를 명시한다: NoDefaultCurrentDirectoryInExePath가 켜진 환경(Claude Code 등)에서는 cmd가 현재 폴더를 찾지 않는다
  const gradlew = isWin ? '.\\gradlew.bat' : './gradlew';
  const base = `${gradlew} --console=plain`;
  const usesSpotless = /spotless/.test(readFileSync(buildFile, 'utf8'));
  if (usesSpotless) {
    if (!run('be', fix ? 'spotlessApply' : 'spotlessCheck', `${base} ${fix ? 'spotlessApply' : 'spotlessCheck'}`, dir)) {
      if (!fix) return; // 포맷 실패면 테스트까지 갈 필요 없음
    }
  }
  run('be', 'test', `${base} test`, dir);
}

function verifyFrontend() {
  const dir = join(ROOT, 'frontend');
  const pkgPath = join(dir, 'package.json');
  if (!existsSync(pkgPath)) return skip('fe', 'frontend 프로젝트가 아직 생성되지 않음');
  if (!existsSync(join(dir, 'node_modules'))) {
    results.push({ area: 'fe', step: 'install', ok: false, secs: '0', summary: ['node_modules 없음 → cd frontend && npm install'] });
    return;
  }
  const scripts = JSON.parse(readFileSync(pkgPath, 'utf8')).scripts || {};
  if (scripts.lint) {
    const lintCmd = fix ? 'npm run lint' : scripts['lint:check'] ? 'npm run lint:check' : 'npm run lint';
    run('fe', 'lint', lintCmd, dir);
  }
  const testScript = scripts['test:unit'] ? 'test:unit' : scripts.test ? 'test' : null;
  if (testScript) run('fe', 'test', `npm run ${testScript} -- --run`, dir);
  if (scripts.build) run('fe', 'build', 'npm run build', dir);
}

if (targets.includes('be')) verifyBackend();
if (targets.includes('fe')) verifyFrontend();

writeFileSync(logFile, fullLog || '(실행된 단계 없음)\n', 'utf8');

const failed = results.filter((r) => !r.ok);
const lines = [];
for (const r of results) {
  if (r.skipped) lines.push(`⏭  [${r.area}] ${r.reason}`);
  else lines.push(`${r.ok ? '✅' : '❌'} [${r.area}] ${r.step} (${r.secs}s)`);
  for (const s of r.summary || []) lines.push(`     ${s}`);
}
lines.push(failed.length ? `\n결과: 실패 ${failed.length}개` : '\n결과: 통과');
if (fullLog) lines.push(`전체 로그: ${relative(ROOT, logFile).replace(/\\/g, '/')}`);
console.log(lines.join('\n'));
process.exit(failed.length ? 1 : 0);
