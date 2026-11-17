#!/usr/bin/env node
// PC마다 최초 1회 실행: git hook 경로 연결 + 개발 환경 점검
// 사용법: node scripts/setup.mjs
import { spawnSync } from 'node:child_process';
import { ROOT, git, isGitRepo } from './lib/common.mjs';

const rows = [];
const add = (ok, item, detail) => rows.push(`${ok === true ? '✅' : ok === 'warn' ? '⚠️ ' : '❌'} ${item.padEnd(18)} ${detail}`);

function cmd(command) {
  const r = spawnSync(command, { shell: true, encoding: 'utf8', windowsHide: true, timeout: 20_000 });
  return { ok: r.status === 0, out: ((r.stdout || '') + (r.stderr || '')).trim() };
}

// 1. git hooks
if (!isGitRepo()) {
  add(false, 'git 저장소', '저장소 루트에서 실행하세요');
} else {
  const set = git(['config', 'core.hooksPath', '.githooks']);
  add(set.ok, 'git hooks', set.ok ? 'core.hooksPath = .githooks (커밋 메시지·테스트 게이트 활성화)' : set.err);
  const name = git(['config', 'user.name']).out;
  const email = git(['config', 'user.email']).out;
  add(!!(name && email) || 'warn', 'git 사용자', name && email ? `${name} <${email}>` : 'git config user.name / user.email 설정 필요');
  const origin = git(['remote', 'get-url', 'origin']);
  add(
    origin.ok ? true : 'warn',
    'origin (GitHub)',
    origin.ok ? origin.out : '없음 → git remote add origin <GitHub private 저장소 URL> && git push -u origin main develop',
  );
  const gitlab = git(['remote', 'get-url', 'gitlab']);
  add(true, 'gitlab (제출)', gitlab.ok ? `${gitlab.out} (push는 제출 때 사람이 직접)` : '아직 없음 (제출 직전에 추가)');
}

// GitHub CLI (PR 생성·CI 확인)
const gh = cmd('gh auth status');
add(gh.ok || 'warn', 'GitHub CLI', gh.ok ? '로그인됨' : 'gh 설치 후 gh auth login (PR 생성·CI 확인에 사용)');

// 2. Node
const nodeMajor = Number(process.versions.node.split('.')[0]);
add(nodeMajor >= 20 || 'warn', 'Node', `v${process.versions.node}${nodeMajor >= 20 ? '' : ' (20 이상 권장)'}`);

// 3. JDK — Gradle·STS가 실제로 쓰는 JAVA_HOME 기준으로 점검한다 (PATH의 java는 참고만)
const javaMajorOf = (out) => {
  const m = /version "(\d+)(?:\.(\d+))?/.exec(out);
  return m ? (m[1] === '1' ? Number(m[2]) : Number(m[1])) : 0;
};
const javaHome = process.env.JAVA_HOME;
const homeJava = javaHome ? cmd(`"${javaHome.replace(/[\\/]$/, '')}${process.platform === 'win32' ? '\\bin\\java.exe' : '/bin/java'}" -version`) : { ok: false, out: '' };
const homeMajor = homeJava.ok ? javaMajorOf(homeJava.out) : 0;
let jdkHint = '';
if (homeMajor < 17 && process.platform === 'win32') {
  // setx 직후에는 현재 프로세스에 반영되지 않는다 → 레지스트리의 사용자 값과 비교해 재시작 필요 여부를 알려 준다
  const reg = cmd('reg query HKCU\\Environment /v JAVA_HOME');
  const regVal = /JAVA_HOME\s+REG_\w+\s+(.+)/.exec(reg.out)?.[1]?.trim();
  if (regVal && regVal !== javaHome) jdkHint = ` (사용자 JAVA_HOME=${regVal} 설정됨 → 터미널·Claude Code를 다시 시작하세요)`;
}
add(
  homeMajor >= 17,
  'JDK (JAVA_HOME)',
  javaHome
    ? `${homeMajor || '확인 실패'} — ${javaHome}${homeMajor >= 17 ? '' : jdkHint || ' → JDK 17 이상을 JAVA_HOME으로 지정 (docs/환경설정.md)'}`
    : '없음 → JDK 17 이상을 JAVA_HOME으로 지정 (docs/환경설정.md)',
);
const pathJava = cmd('java -version');
const pathMajor = pathJava.ok ? javaMajorOf(pathJava.out) : 0;
if (pathMajor && pathMajor !== homeMajor) {
  add(true, 'java (PATH)', `${pathMajor} — 참고: 빌드는 JAVA_HOME을 쓰므로 무관`);
}

// 4. Docker (Testcontainers)
const docker = cmd('docker --version');
if (!docker.ok) add(false, 'Docker', '없음 → Docker Desktop 설치 필요 (Testcontainers)');
else {
  const info = cmd('docker info --format "{{.ServerVersion}}"');
  add(info.ok || 'warn', 'Docker', info.ok ? `엔진 실행 중 (${info.out})` : `${docker.out} — Docker Desktop을 실행하세요`);
}

// 5. MySQL 클라이언트 (선택)
const mysql = cmd('mysql --version');
add(mysql.ok || 'warn', 'MySQL CLI', mysql.ok ? mysql.out.split('\n')[0] : '없음 (선택. Workbench만 써도 됨)');

console.log(`\n바이브노트 개발 환경 점검 (${ROOT})\n`);
console.log(rows.join('\n'));
const failed = rows.filter((r) => r.startsWith('❌')).length;
console.log(
  failed
    ? `\n❌ 항목 ${failed}개를 해결한 뒤 다시 실행하세요. 완료되면 Claude Code에서 /pickup 으로 시작합니다.\n`
    : '\n준비 완료. Claude Code에서 /pickup 으로 시작합니다.\n',
);
process.exitCode = failed ? 1 : 0;
