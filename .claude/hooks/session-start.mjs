#!/usr/bin/env node
// SessionStart hook: 릴레이 교대 직후에도 AI가 바로 상태를 알도록
// 브랜치, git 상태, 최근 커밋, NOW.md, 작업로그 마지막 항목을 컨텍스트에 주입한다.
// PC 4대의 환경이 서로 달라서 어느 PC인지, 빠른 환경 점검 결과, 그 PC의 함정, 직전 작업 PC와 다른지도 알린다.
import {
  NOW_FILE,
  PROTECTED_BRANCHES,
  WORKLOG_FILE,
  currentBranch,
  git,
  hasCommits,
  isGitRepo,
  readStdinJson,
  readText,
  workFilesNewerThanNow,
} from '../../scripts/lib/common.mjs';

// env-check가 깨져도 NOW.md 주입은 살린다
let envCheck;
try {
  envCheck = await import('../../scripts/lib/env-check.mjs');
} catch (e) {
  envCheck = { error: e.message };
}

const input = readStdinJson();
const parts = ['# 바이브노트 세션 컨텍스트 (SessionStart hook 자동 주입)'];
const warnings = [];
// 외부 명령(gh·docker)은 병렬·3초 제한이라 아래 git fetch와 겹쳐 돈다
const checksFailed = (e) => [{ status: 'warn', item: '환경 점검', detail: `실패: ${e?.message ?? e}` }];
const checksP = envCheck.runChecks ? envCheck.runChecks({ fast: true }).catch(checksFailed) : Promise.resolve(checksFailed(envCheck.error));

if (!isGitRepo()) {
  warnings.push('git 저장소가 아닙니다.');
} else {
  const branch = currentBranch() || '(detached)';
  parts.push(`## Git\n- 브랜치: ${branch}`);

  // 원격과 비교 (네트워크가 느리면 건너뜀)
  if (git(['remote']).out) {
    git(['fetch', '--quiet'], { timeout: 5000 });
    const ab = git(['rev-list', '--left-right', '--count', 'HEAD...@{u}']);
    if (ab.ok) {
      const [ahead, behind] = ab.out.split(/\s+/).map(Number);
      if (behind > 0) warnings.push(`원격보다 ${behind}커밋 뒤처져 있습니다. 작업 전에 git pull 하세요 (상대가 교대 후 push했을 수 있음).`);
      if (ahead > 0) parts.push(`- push 안 된 커밋: ${ahead}개`);
    }
  }

  const status = git(['status', '--short']).out.split('\n').filter(Boolean);
  parts.push(`- 미커밋 변경: ${status.length}개${status.length ? '\n' + status.slice(0, 20).map((s) => `  ${s}`).join('\n') : ''}`);
  if (hasCommits()) parts.push(`- 최근 커밋:\n${git(['log', '--oneline', '-5']).out.split('\n').map((l) => `  ${l}`).join('\n')}`);

  if (PROTECTED_BRANCHES.includes(branch) && hasCommits()) {
    warnings.push(`보호 브랜치(${branch})에 있습니다. 코드를 바꾸기 전에 feature/F-xx-slug 브랜치를 만드세요.`);
  }
}

const now = readText(NOW_FILE);
const log = readText(WORKLOG_FILE);
const logEntries = log ? log.split(/\r?\n(?=## )/) : [];
const lastLog = logEntries.at(-1)?.startsWith('## ') ? logEntries.at(-1) : null;

// 이전 세션이 체크포인트 없이 남긴 변경 (Stop hook은 이 세션이 건드린 파일만 보므로 여기서 알린다)
const unrecorded = isGitRepo() ? workFilesNewerThanNow() : [];
if (unrecorded.length) {
  warnings.push(
    `NOW.md보다 나중에 바뀐 미커밋 작업 파일 ${unrecorded.length}개 (${unrecorded.slice(0, 3).join(', ')}${unrecorded.length > 3 ? ' …' : ''}). ` +
      '이전 세션이 체크포인트 없이 끝났거나 같은 작업 트리에서 다른 세션이 작업 중일 수 있습니다. /pickup에서 git diff로 실제 진행을 확인하고, 다른 세션의 파일은 건드리지 않습니다.',
  );
}

// PC·환경 (git hooks 연결 여부도 여기서 본다)
const {
  currentPc = () => ({ id: null, hostname: '?' }),
  icon = (s) => s,
  lastFullCheck = () => null,
  loadPcs = () => ({}),
} = envCheck;
const pc = currentPc();
const rows = await checksP;
const env = [pc.id ? `- PC: ${pc.label} (${pc.id}) · hostname ${pc.hostname}` : `- PC: 미등록 (hostname ${pc.hostname})`];
const bad = rows.filter((r) => r.status !== 'ok');
env.push(
  bad.length
    ? bad.map((r) => `- ${icon(r.status)} ${r.item}: ${r.detail}`).join('\n')
    : '- 환경 ✅ (빠른 점검: PC 등록·git hooks·git 사용자·Node·JDK·로컬 설정·FE 의존성·gh 계정·Docker)',
);
const full = lastFullCheck();
if (pc.id && full?.pcId !== pc.id) env.push('- 전체 점검(setup.mjs) 기록 없음 → 사용자에게 `node scripts/setup.mjs` 실행을 요청');
if (pc.notes?.length) env.push(`- 이 PC의 주의 사항 (scripts/lib/pcs.json):\n${pc.notes.map((n) => `  - ${n}`).join('\n')}`);
parts.splice(1, 0, `## PC·환경\n${env.join('\n')}`);
if (!pc.id) warnings.push('이 PC가 등록되지 않아 인계 기록에 PC를 적을 수 없습니다. 사용자에게 `node scripts/setup.mjs --pc <id>` 실행을 요청하세요.');
if (rows.some((r) => r.status === 'fail')) warnings.push('환경 점검에 ❌ 항목이 있습니다. 작업 전에 사용자와 해결하세요 (PC·환경 섹션).');

// 직전 작업 PC: NOW.md "갱신:" 줄과 작업로그 마지막 헤더 중 더 최근 것 (자동 handoff.mjs는 작업로그에만 PC를 남긴다)
const nowHead = /^갱신: (\d{4}-\d{2}-\d{2})(?: (\d{2}:\d{2}))?.*?\bPC: ([\w-]+)/m.exec(now || '');
const logHead = /^## (\d{4}-\d{2}-\d{2})(?: (\d{2}:\d{2}))? · [^·\n]+ · ([\w-]+) · /.exec(lastLog || '');
const pcIds = loadPcs();
const prevPc = [nowHead, logHead]
  .filter((m) => m && (m[3] === 'unknown-pc' || pcIds[m[3]]))
  .map((m) => ({ at: `${m[1]} ${m[2] || '00:00'}`, id: m[3] }))
  .sort((a, b) => (a.at < b.at ? 1 : -1))[0]?.id;
if (prevPc === 'unknown-pc' && pc.id) {
  warnings.push('직전 작업 PC를 알 수 없습니다(미등록 PC에서 인계). git pull로 최신화하고, 이 PC에서 verify.mjs가 통과하는지 먼저 봅니다.');
} else if (prevPc && pc.id && prevPc !== pc.id) {
  warnings.push(
    `직전 작업은 다른 PC(${prevPc})에서 했습니다. git pull로 최신화했는지, 그 PC에만 있던 것(push 안 한 커밋, 로컬 DB 데이터, 환경변수 값, 설치한 도구)이 필요한지 확인하고, 이 PC에서 verify.mjs가 통과하는지 먼저 봅니다.`,
  );
}
parts.push(`## docs/handoff/NOW.md\n${now ? now.trim() : '(없음)'}`);

if (lastLog) parts.push(`## 작업로그 마지막 항목\n${lastLog.trim()}`);

if (warnings.length) parts.splice(1, 0, `## ⚠️ 주의\n${warnings.map((w) => `- ${w}`).join('\n')}`);
parts.push(
  input.source === 'compact'
    ? '컨텍스트가 압축되었다. 위 NOW.md와 현재 spec을 기준으로 이어서 작업한다.'
    : '이어서 작업하려면 /pickup 을 사용한다. 작업 단위가 끝날 때마다 NOW.md를 갱신한다.',
);

process.stdout.write(
  JSON.stringify({
    hookSpecificOutput: { hookEventName: 'SessionStart', additionalContext: parts.join('\n\n') },
  }),
);
