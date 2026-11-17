#!/usr/bin/env node
// PC마다 최초 1회 실행: git hook 경로 연결 + 개발 환경 점검
// 사용법: node scripts/setup.mjs
import { spawnSync } from 'node:child_process';
import { ROOT, git, isGitRepo } from './lib/common.mjs';

const rows = [];
const add = (ok, item, detail) => rows.push(`${ok === true ? '✅' : ok === 'warn' ? '⚠️ ' : '❌'} ${item.padEnd(16)} ${detail}`);

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
  const remote = git(['remote', '-v']).out;
  add(remote ? true : 'warn', 'git 원격', remote ? remote.split('\n')[0] : '없음 → git remote add origin <GitLab URL>');
}

// 2. Node
const nodeMajor = Number(process.versions.node.split('.')[0]);
add(nodeMajor >= 20 || 'warn', 'Node', `v${process.versions.node}${nodeMajor >= 20 ? '' : ' (20 이상 권장)'}`);

// 3. JDK
const java = cmd('java -version');
const m = /version "(\d+)(?:\.(\d+))?/.exec(java.out);
const javaMajor = m ? (m[1] === '1' ? Number(m[2]) : Number(m[1])) : 0;
add(javaMajor >= 17, 'JDK', java.ok ? `${javaMajor}${javaMajor >= 17 ? '' : ' → JDK 17 이상 설치 필요 (Spring Boot 3)'}` : '없음 → JDK 17 이상 설치 필요');

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
console.log('\n❌ 항목을 해결한 뒤 다시 실행하세요. 완료되면 Claude Code에서 /pickup 으로 시작합니다.\n');
