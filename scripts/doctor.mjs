#!/usr/bin/env node
// 하네스·개발 환경 한 번에 점검 (H-1). 아무것도 바꾸지 않는다 (setup.mjs는 hook 연결·PC 등록을 한다).
// "커밋이 이상하게 느리다", "hook이 안 돈다", "prettier가 줄바꿈으로 실패한다" 같은 때 먼저 실행한다.
//   1) 개발 환경: setup.mjs와 같은 전체 점검 (PC 등록, git hooks, Node, JDK, gh, Docker …)
//   2) 하네스: hook 파일·실행 권한, settings.json hook 스크립트, 남은 Gradle 잠금, CRLF 파일, FE 의존성 동기화,
//      내 NOW 파일, 자동 동기화 설정, 보호 브랜치의 커밋 안 된 변경
//
// 사용법: node scripts/doctor.mjs   (❌가 있으면 종료 코드 1)
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, currentBranch, git, isGitRepo, localPcId, nowFileOf, nowRelPath, ownerKey, readText } from './lib/common.mjs';
import { REQUIRED_HOOKS, crlfFiles, depsStale, hookModeProblems, settingsHookScripts } from './lib/doctor.mjs';
import { currentPc, icon, runChecks } from './lib/env-check.mjs';
import { STALE_MS, isStale } from './lib/lock.mjs';

const rows = await runChecks();
const add = (status, item, detail) => rows.push({ status, item, detail });

if (isGitRepo()) {
  const hookProblems = hookModeProblems(git(['ls-files', '-s', ...REQUIRED_HOOKS]).out);
  const mjsMissing = REQUIRED_HOOKS.map((h) => `${h}.mjs`).filter((f) => !existsSync(join(ROOT, f)));
  const problems = [...hookProblems, ...mjsMissing.map((f) => `${f} 없음`)];
  add(problems.length ? 'fail' : 'ok', 'hook 파일', problems.length ? `${problems.join(', ')} → git update-index --chmod=+x <파일>` : 'commit-msg·pre-push (실행 권한 있음)');

  const crlf = crlfFiles(git(['ls-files', '--eol']).out);
  add(
    crlf.length ? 'warn' : 'ok',
    '줄바꿈 (LF)',
    crlf.length ? `작업 트리 CRLF ${crlf.length}개 (${crlf.slice(0, 3).join(', ')}${crlf.length > 3 ? ' …' : ''}) → 커밋할 변경이 없으면 rm <파일> && git checkout -- <파일> (checkout만으로는 다시 쓰지 않음)` : '작업 트리 모두 LF',
  );

  const autoSync = git(['config', '--get', 'vibenote.autoSync']).out;
  add('ok', '자동 동기화', autoSync === 'false' ? '꺼짐 (git config vibenote.autoSync false)' : '켜짐 (세션 시작·peer-watch가 origin/develop을 합침)');

  const branch = currentBranch();
  const dirty = git(['status', '--porcelain', '--untracked-files=no']).out.split('\n').filter(Boolean).length;
  if (branch && PROTECTED_BRANCHES.includes(branch) && dirty) add('warn', '브랜치', `${branch}에 커밋 안 된 변경 ${dirty}개 → git switch -c feature/... 로 옮긴 뒤 커밋`);
  else add('ok', '브랜치', `${branch || '(detached)'}${dirty ? ` · 커밋 안 된 변경 ${dirty}개` : ''}`);
}

let settings = null;
try {
  settings = JSON.parse(readFileSync(join(ROOT, '.claude', 'settings.json'), 'utf8'));
} catch (e) {
  add('fail', 'settings.json', `읽기 실패: ${e.message}`);
}
if (settings) {
  const scripts = settingsHookScripts(settings);
  const missing = scripts.filter((p) => !existsSync(join(ROOT, p)));
  add(missing.length ? 'fail' : 'ok', 'Claude hooks', missing.length ? `없는 스크립트: ${missing.join(', ')}` : `${scripts.length}개 스크립트 있음`);
}

const lockPath = join(ROOT, '.harness', 'gradle.lock');
if (existsSync(lockPath)) {
  let info = null;
  try {
    info = JSON.parse(readFileSync(lockPath, 'utf8'));
  } catch {
    info = null;
  }
  add(
    'warn',
    'Gradle 잠금',
    isStale(info)
      ? `버려진 잠금 (다음 verify가 지움): ${info?.label || '?'}`
      : `사용 중: pid ${info.pid} ${info.label || ''} (${Math.round((Date.now() - info.at) / 60000)}분째, ${STALE_MS / 60000}분 넘으면 버려진 것으로 봄)`,
  );
} else add('ok', 'Gradle 잠금', '없음');

const lock = join(ROOT, 'frontend', 'package-lock.json');
const installed = join(ROOT, 'frontend', 'node_modules', '.package-lock.json');
if (existsSync(lock) && existsSync(join(ROOT, 'frontend', 'node_modules'))) {
  const stale = depsStale(statSync(lock).mtimeMs, existsSync(installed) ? statSync(installed).mtimeMs : null);
  add(stale ? 'warn' : 'ok', 'FE 의존성 동기화', stale ? 'package-lock.json이 설치 뒤 바뀜 → cd frontend && npm ci' : 'package-lock.json과 설치 일치');
}

const key = ownerKey(localPcId());
if (key) {
  const now = readText(nowFileOf(key));
  const head = /^갱신: (\d{4}-\d{2}-\d{2} \d{2}:\d{2}).*?PC: ([\w-]+)/m.exec(now || '');
  add(now ? 'ok' : 'warn', '내 NOW 파일', now ? `${nowRelPath(key)} (갱신 ${head ? `${head[1]} @${head[2]}` : '줄 없음'})` : `${nowRelPath(key)} 없음`);
}

const pc = currentPc();
console.log(`\n바이브노트 doctor — ${pc.label || '미등록 PC'} (${pc.id || '?'}) · ${ROOT}\n`);
console.log(rows.map((r) => `${icon(r.status)} ${r.item.padEnd(18)} ${r.detail}`).join('\n'));
const failed = rows.filter((r) => r.status === 'fail').length;
const warned = rows.filter((r) => r.status === 'warn').length;
console.log(`\n${failed ? `❌ ${failed}개` : '❌ 없음'} · ⚠️ ${warned}개${pc.notes?.length ? `\n\n이 PC의 주의 사항 (scripts/lib/pcs.json):\n${pc.notes.map((n) => `  - ${n}`).join('\n')}` : ''}\n`);
process.exitCode = failed ? 1 : 0;
