// PC 식별과 개발 환경 점검. setup.mjs(전체 점검)와 SessionStart hook(fast 점검)이 함께 쓴다.
// PC 4대(scripts/lib/pcs.json)는 환경이 서로 달라서, 세션마다 어느 PC인지와 그 PC의 함정을 Claude에게 알린다.
import { exec } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { hostname } from 'node:os';
import { join } from 'node:path';
import { ROOT, git, isGitRepo, isWin, ownerKey, readText } from './common.mjs';
import { EVAL_FETCH_EXCLUDE, evalBlockedFor, evalFetchExcluded } from './protect.mjs';

const HARNESS_DIR = join(ROOT, '.harness');
export const PC_FILE = join(HARNESS_DIR, 'pc.json');
export const ENV_CHECK_FILE = join(HARNESS_DIR, 'env-check.json');

function readJson(path) {
  try {
    return JSON.parse(readText(path) || 'null');
  } catch {
    return null;
  }
}

function writeJson(path, data) {
  mkdirSync(HARNESS_DIR, { recursive: true });
  writeFileSync(path, JSON.stringify(data, null, 2) + '\n');
}

/** 등록부의 PC 목록 { id: { label, owner, github, notes } } */
export function loadPcs() {
  return readJson(join(ROOT, 'scripts', 'lib', 'pcs.json'))?.pcs || {};
}

/**
 * 이 PC의 등록 정보. 미등록이면 { id: null }.
 * hostnameChanged: 등록할 때와 hostname이 다르다 (폴더째 복사했거나 다른 PC의 .harness를 가져옴)
 */
export function currentPc() {
  const local = readJson(PC_FILE);
  const pcs = loadPcs();
  if (!local?.id || !pcs[local.id]) return { id: null, hostname: hostname() };
  return { id: local.id, ...pcs[local.id], hostname: hostname(), hostnameChanged: !!local.hostname && local.hostname !== hostname() };
}

export function registerPc(id) {
  writeJson(PC_FILE, { id, hostname: hostname(), registeredAt: new Date().toISOString() });
}

/** 마지막 전체 점검 결과 { at, pcId, rows } 또는 null */
export function lastFullCheck() {
  return readJson(ENV_CHECK_FILE);
}

export function saveFullCheck(pcId, rows) {
  writeJson(ENV_CHECK_FILE, { at: new Date().toISOString(), pcId, rows });
}

/**
 * 외부 명령 실행 → { ok, out, timedOut }. timedOut이면 "꺼짐·로그인 안 됨"이 아니라 "응답 없음"으로 알린다.
 * 주의: 띄운 뒤 동기 작업으로 이벤트 루프를 막으면 타이머가 먼저 터져 출력이 버려진 채 err 없이 끝난다 (#67) → 동기 git이 끝난 뒤 부른다.
 * 그래도 콜백이 제한 시간 뒤에 오면 출력을 믿을 수 없으므로 시간 초과로 본다 (거짓 ✅ 방지, reviewer).
 */
export function run(command, timeout) {
  const start = Date.now();
  return new Promise((resolve) => {
    exec(command, { encoding: 'utf8', windowsHide: true, timeout }, (err, stdout, stderr) => {
      const timedOut = !!err?.killed || (!!timeout && Date.now() - start >= timeout);
      resolve({ ok: !err && !timedOut, out: ((stdout || '') + (stderr || '')).trim(), timedOut });
    });
  });
}

const javaMajorOf = (version) => {
  const m = /^(\d+)(?:\.(\d+))?/.exec(version || '');
  return m ? (m[1] === '1' ? Number(m[2]) : Number(m[1])) : 0;
};

/**
 * git 사용자 점검 → { status, detail }. 순수 함수.
 * 같은 사람의 PC마다 user.name이 다르면 작업로그·NOW의 작업자와 커밋 작성자가 PC마다 달라진다 (H-7: dh-home만 이메일이 이름이었다).
 * 이름은 그 PC의 GitHub 계정(pcs.json github)으로 맞춘다. 이메일은 PC마다 달라도 된다 (저장소가 public이라 등록부에 적지 않는다)
 */
export function gitUserRow({ name, email, github }) {
  if (!name || !email) return { status: 'warn', detail: 'git config user.name / user.email 설정 필요' };
  if (github && name !== github) return { status: 'warn', detail: `이름 ${name} ≠ 이 PC 계정 ${github} → git config user.name ${github} (PC마다 같게, H-7)` };
  return { status: 'ok', detail: `${name} <${email}>` };
}

/**
 * 환경 점검. 결과 행은 { status: 'ok'|'warn'|'fail', item, detail }.
 * fast: 세션 시작용. 프로세스를 거의 띄우지 않고 외부 명령은 병렬·3초 제한 (SessionStart hook timeout 15초 안)
 */
export async function runChecks({ fast = false } = {}) {
  const timeout = fast ? 3000 : 20_000;
  const rows = [];
  const add = (status, item, detail) => rows.push({ status, item, detail });
  const pc = currentPc();

  // 외부 명령은 먼저 모두 띄워 놓고 기다린다
  const ghP = run('gh auth status --active', timeout);
  const dockerP = run('docker info --format "{{.ServerVersion}}"', timeout);
  const dockerVerP = fast ? null : run('docker --version', timeout);
  const mysqlP = fast ? null : run('mysql --version', timeout);

  // PC
  if (!pc.id) add('fail', 'PC 등록', `미등록 → node scripts/setup.mjs --pc <${Object.keys(loadPcs()).join('|')}>`);
  else if (pc.hostnameChanged) add('warn', 'PC 등록', `${pc.id}로 등록됐지만 hostname이 다름(${pc.hostname}) → 다른 PC면 setup.mjs --pc로 다시 등록`);
  else add('ok', 'PC 등록', `${pc.label} (${pc.id})`);

  // git
  if (!isGitRepo()) {
    add('fail', 'git 저장소', '저장소 루트에서 실행하세요');
  } else {
    const hooks = git(['config', 'core.hooksPath']).out === '.githooks';
    add(hooks ? 'ok' : 'fail', 'git hooks', hooks ? 'core.hooksPath = .githooks' : '미연결 → node scripts/setup.mjs');
    const u = gitUserRow({ name: git(['config', 'user.name']).out, email: git(['config', 'user.email']).out, github: pc.github });
    add(u.status, 'git 사용자', u.detail);
    // eval 블라인드 (HX-2): 김동환 PC의 fetch는 eval 기록 브랜치(eval/…)를 받지 않는다. setup.mjs가 refspec을 넣는다
    if (evalBlockedFor(ownerKey(pc.id))) {
      const excluded = evalFetchExcluded(git(['config', '--get-all', 'remote.origin.fetch']).out.split(/\r?\n/));
      add(excluded ? 'ok' : 'warn', 'eval 브랜치 제외', excluded ? `fetch에서 ${EVAL_FETCH_EXCLUDE} 제외` : '없음 → node scripts/setup.mjs (eval 블라인드, HX-2)');
    }
    if (!fast) {
      const origin = git(['remote', 'get-url', 'origin']);
      add(origin.ok ? 'ok' : 'warn', 'origin (GitHub)', origin.ok ? origin.out : '없음 → git remote add origin <GitHub private 저장소 URL>');
      const gitlab = git(['remote', 'get-url', 'gitlab']);
      add('ok', 'gitlab (제출)', gitlab.ok ? `${gitlab.out} (push는 제출 때 사람이 직접)` : '아직 없음 (제출 직전에 추가)');
    }
  }

  // Node — FE(jsdom 31·vitest 5) 요건: ^22.22.2 || ^24.15.0 || >=26 (frontend/package.json engines)
  const [nodeMajor, nodeMinor, nodePatch] = process.versions.node.split('.').map(Number);
  const atLeast = (mi, pa) => nodeMinor > mi || (nodeMinor === mi && nodePatch >= pa);
  const nodeOk = nodeMajor >= 26 || (nodeMajor === 24 && atLeast(15, 0)) || (nodeMajor === 22 && atLeast(22, 2));
  add(nodeOk ? 'ok' : 'warn', 'Node', `v${process.versions.node}${nodeOk ? '' : ' (FE 테스트는 24.15 이상 필요)'}`);

  // JDK — Gradle이 쓰는 JAVA_HOME 기준(17 이상). 컴파일 JDK 21은 Gradle toolchain이 자동으로 받는다.
  //       release 파일을 읽어 java 프로세스를 띄우지 않는다. 없으면(전체 점검만) java -version
  const javaHome = (process.env.JAVA_HOME || '').replace(/[\\/]$/, '');
  let javaMajor = javaHome ? javaMajorOf(/JAVA_VERSION="([^"]+)"/.exec(readText(join(javaHome, 'release')) || '')?.[1]) : 0;
  if (javaHome && !javaMajor && !fast) {
    const v = await run(`"${join(javaHome, 'bin', isWin ? 'java.exe' : 'java')}" -version`, timeout);
    javaMajor = javaMajorOf(/version "([^"]+)"/.exec(v.out)?.[1]);
  }
  let jdkHint = '';
  if (javaMajor < 17 && isWin && !fast) {
    // setx 직후에는 현재 프로세스에 반영되지 않는다 → 레지스트리 사용자 값과 비교해 재시작 필요 여부를 알린다
    const reg = await run('reg query HKCU\\Environment /v JAVA_HOME', timeout);
    const regVal = /JAVA_HOME\s+REG_\w+\s+(.+)/.exec(reg.out)?.[1]?.trim();
    if (regVal && regVal !== process.env.JAVA_HOME) jdkHint = ` (사용자 JAVA_HOME=${regVal} 설정됨 → 터미널·Claude Code를 다시 시작하세요)`;
  }
  add(
    javaMajor >= 17 ? 'ok' : 'fail',
    'JDK (JAVA_HOME)',
    javaHome
      ? `${javaMajor || '확인 실패'} — ${javaHome}${javaMajor >= 17 ? (javaMajor >= 21 || fast ? '' : ' (컴파일 JDK 21은 첫 빌드 때 Gradle이 자동으로 받음. STS는 JDK 21 필요)') : jdkHint || ' → JDK 17 이상을 JAVA_HOME으로 지정 (docs/환경설정.md 1절)'}`
      : '없음 → JDK 17 이상을 JAVA_HOME으로 지정 (docs/환경설정.md 1절)',
  );

  // 로컬 파일 (존재만 본다. .env 내용은 읽지 않는다)
  add(existsSync(join(ROOT, '.env')) ? 'ok' : 'warn', '.env', existsSync(join(ROOT, '.env')) ? '있음' : '없음 → .env.example을 복사해 채움 (bootRun에 필요)');
  const nm = existsSync(join(ROOT, 'frontend', 'node_modules'));
  add(nm ? 'ok' : 'warn', 'FE 의존성', nm ? 'frontend/node_modules 있음' : '없음 → cd frontend && npm ci');

  // GitHub CLI — 활성 계정이 등록부의 이 PC 계정과 같은지
  const gh = await ghP;
  const ghAccount = /account (\S+)/.exec(gh.out)?.[1];
  if (gh.timedOut) add('warn', 'GitHub CLI', `${timeout / 1000}초 안에 응답 없음 (네트워크 확인) → gh auth status`);
  else if (!gh.ok) add('warn', 'GitHub CLI', fast ? '로그인 안 됨 → gh auth status' : 'gh 설치 후 gh auth login (PR 생성·CI 확인에 사용)');
  else if (pc.github && ghAccount && ghAccount !== pc.github) add('warn', 'GitHub CLI', `활성 계정 ${ghAccount} ≠ 이 PC 계정 ${pc.github} → gh auth switch -u ${pc.github}`);
  else add('ok', 'GitHub CLI', `${ghAccount || '로그인됨'}${pc.id && !pc.github ? ' (scripts/lib/pcs.json에 github 미기입)' : ''}`);

  // Docker (Testcontainers, 일부 PC의 oxlint)
  const docker = await dockerP;
  if (docker.ok) add('ok', 'Docker', `엔진 실행 중${docker.out ? ` (${docker.out})` : ''}`);
  else if (docker.timedOut) add('warn', 'Docker', `${timeout / 1000}초 안에 응답 없음 (엔진 시작 중일 수 있음) → docker info`);
  else if (dockerVerP && !(await dockerVerP).ok) add('fail', 'Docker', '없음 → Docker Desktop 설치 필요 (Testcontainers)');
  else add('warn', 'Docker', '엔진 꺼짐 → Docker Desktop 실행 (BE 통합 테스트·커밋 게이트에 필요)');

  if (mysqlP) {
    const mysql = await mysqlP;
    add(mysql.ok ? 'ok' : 'warn', 'MySQL CLI', mysql.ok ? mysql.out.split('\n')[0] : '없음 (선택. Workbench만 써도 됨)');
  }
  return rows;
}

export const icon = (status) => (status === 'ok' ? '✅' : status === 'warn' ? '⚠️ ' : '❌');
