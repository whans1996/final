#!/usr/bin/env node
// SessionStart hook: 새 세션·교대 직후에도 AI가 바로 상태를 알도록
// 브랜치, git 상태, 최근 커밋, 내 NOW 파일(docs/handoff/NOW-<dh|js>.md), 내 작업로그 마지막 항목을 컨텍스트에 주입한다.
// 두 사람이 영역별로 병렬 개발하므로 상대 NOW는 원격 브랜치까지 보고 가장 최근 것의 요약만 보여 주고, develop에 새로 들어온 상대 변경(마이그레이션·API)을 알린다.
// 세션 도중의 상대 변화는 peer-watch hook(UserPromptSubmit)이 여기서 남긴 스냅샷(.harness/peer-watch.json)과 비교해 알린다.
// PC 4대의 환경이 서로 달라서 어느 PC인지, 빠른 환경 점검 결과, 그 PC의 함정, 직전 작업 PC와 다른지도 알린다.
// hook 제한(15초)을 넘기면 출력이 통째로 버려지므로 시간 예산(scripts/lib/budget.mjs, H-5) 안에서 덜 중요한 단계부터 건너뛴다.
import { createBudget, sessionStartBudgetMs } from '../../scripts/lib/budget.mjs';
import {
  NON_INTERACTIVE_GIT_ENV,
  PROTECTED_BRANCHES,
  ROOT,
  WORKLOG_FILE,
  currentBranch,
  git,
  hasCommits,
  isGitRepo,
  lastWorklogEntryOf,
  localPcId,
  nowFileOf,
  nowRelPath,
  otherOwnerKey,
  ownerKey,
  previousPc,
  readStdinJson,
  readText,
  sessionBanner,
  workFilesNewerThanNow,
  worklogEntries,
} from '../../scripts/lib/common.mjs';
import { followUpIssues, issueBannerLine, issueContext, issueFailureLine, summarizeIssues } from '../../scripts/lib/issues.mjs';
import { MY_BRANCH_BUDGET_MS, latestPeerNow, myBranchLines, myBranchNows, nowHeadline, readPeerState, writePeerState } from '../../scripts/lib/peer.mjs';
import { ROADMAP_FILE, ROADMAP_REL, bannerLines, fetchRoadmapPrs, loadView, roadmapBlock } from '../../scripts/lib/roadmap.mjs';
import { roadmapSnapshot, writeStatus } from '../../scripts/lib/status.mjs';
import { autoSync, mergeNotes } from '../../scripts/lib/sync.mjs';

// 예산은 import가 끝난 시점부터 잰다 (Node 기동·import 시간은 예산 밖 여유 4초에 든다)
const budget = createBudget(sessionStartBudgetMs());
// gh·docker 결과를 기다린 뒤에도 남겨 둘 시간: 상대 NOW·내 브랜치·작업 파일·로드맵 조회 (git 1회 0.1초인 PC에서 약 3초)
const LOCAL_RESERVE_MS = 3500;
const OVER_BUDGET = '세션 시작 시간 예산 초과';

// env-check가 깨져도 NOW 주입은 살린다
let envCheck;
try {
  envCheck = await import('../../scripts/lib/env-check.mjs');
} catch (e) {
  envCheck = { error: e.message };
}

const input = readStdinJson();
const parts = ['# 바이브노트 세션 컨텍스트 (SessionStart hook 자동 주입)'];
const warnings = [];
let synced = [];

// 같은 값을 여러 번 묻지 않는다 (git 1회가 0.1초 걸리는 PC가 있다)
const inRepo = isGitRepo();
const branchName = inRepo ? currentBranch() : null;

if (!inRepo) {
  warnings.push('git 저장소가 아닙니다.');
} else {
  const branch = branchName || '(detached)';
  parts.push(`## Git\n- 브랜치: ${branch}`);

  // 원격과 비교 (네트워크가 느리면 건너뜀)
  if (git(['remote']).out) {
    const fetchMs = budget.timeout(5000, LOCAL_RESERVE_MS);
    if (fetchMs >= 1000) git(['fetch', '--quiet', '--prune'], { timeout: fetchMs, env: NON_INTERACTIVE_GIT_ENV });
    else budget.skip('원격 확인(fetch)');
    // 다른 PC가 push한 내 브랜치 커밋과 상대가 develop에 머지한 커밋을 자동으로 합친다 (scripts/lib/sync.mjs). 예산이 모자라면 건너뛴다
    synced = autoSync({ timeLeft: budget.left });
    if (synced.some((l) => l.includes('시간 예산'))) budget.skip('자동 병합');
    const ab = git(['rev-list', '--left-right', '--count', 'HEAD...@{u}']);
    if (ab.ok) {
      const [ahead, behind] = ab.out.split(/\s+/).map(Number);
      if (behind > 0) warnings.push(`원격보다 ${behind}커밋 뒤처져 있습니다. 작업 전에 git pull 하세요 (다른 PC에서 이 브랜치에 push했을 수 있음).`);
      if (ahead > 0) parts.push(`- push 안 된 커밋: ${ahead}개`);
    }
    // 병렬 개발: 상대가 develop에 머지한 것 중 내 브랜치에 없는 것 (마이그레이션 번호·openapi.json 충돌의 원인)
    if (!PROTECTED_BRANCHES.includes(branch) && git(['rev-parse', '--verify', '-q', 'origin/develop']).ok) {
      const n = Number(git(['rev-list', '--count', 'HEAD..origin/develop']).out) || 0;
      if (n > 0) {
        const changed = git(['diff', '--name-only', 'HEAD...origin/develop']).out.split('\n').filter(Boolean);
        const notes = mergeNotes(changed);
        parts.push(
          `- develop에 이 브랜치에 없는 커밋 ${n}개${notes.length ? ` (${notes.join(', ')})` : ''}. ` +
            'PR 전이나 마이그레이션·API를 추가하기 전에 `git merge origin/develop`',
        );
      }
    }
  }

  const status = git(['status', '--short']).out.split('\n').filter(Boolean);
  parts.push(`- 미커밋 변경: ${status.length}개${status.length ? '\n' + status.slice(0, 20).map((s) => `  ${s}`).join('\n') : ''}`);
  if (hasCommits()) parts.push(`- 최근 커밋:\n${git(['log', '--oneline', '-5']).out.split('\n').map((l) => `  ${l}`).join('\n')}`);

  if (PROTECTED_BRANCHES.includes(branch) && hasCommits()) {
    warnings.push(`보호 브랜치(${branch})에 있습니다. 코드를 바꾸기 전에 feature/F-xx-slug 브랜치를 만드세요.`);
  }
  // Git 블록의 "- 미커밋 변경"·"- 최근 커밋" 줄이 이 헤딩 아래 묶여 보이지 않게 Git 블록 뒤에 둔다 (#44)
  if (synced.length) parts.push(`## 자동 동기화\n${synced.join('\n')}`);
}

// 외부 명령(gh·docker)은 위 동기 git(fetch·autoSync)이 끝난 뒤 띄운다 (#67).
// 먼저 띄우면 동기 git이 이벤트 루프를 막는 사이 제한 시간 타이머가 먼저 터져 출력이 버려지고, 이슈가 경고 없이 빠지거나 점검이 거짓 ✅가 된다
const checksFailed = (e) => [{ status: 'warn', item: '환경 점검', detail: `실패: ${e?.message ?? e}` }];
// 예산 안에 안 끝난 외부 조회는 기다리지 않고 실패로 보인다 (출력이 통째로 버려지는 것보다 낫다)
const late = (what, value) => () => {
  budget.skip(what);
  return value;
};
const checksP = envCheck.runChecks ? envCheck.runChecks({ fast: true }).catch(checksFailed) : Promise.resolve(checksFailed(envCheck.error));
// 만들기만 하고 다시 안 보던 follow-up 이슈를 세션마다 보여 준다 (실패하면 실패 줄을 낸다)
const issuesP = followUpIssues().catch((e) => ({ ok: false, reason: e?.message ?? String(e) }));
// 로드맵 상태(PR 본문 Roadmap: 줄)도 같은 방식으로 받는다 (docs/로드맵.md, R-1)
const roadmapPrsP = fetchRoadmapPrs().catch((e) => ({ ok: false, reason: e?.message ?? String(e) }));

// PC·환경 (git hooks 연결 여부도 여기서 본다)
const {
  currentPc = () => ({ id: null, hostname: '?' }),
  icon = (s) => s,
  lastFullCheck = () => null,
  loadPcs = () => ({}),
} = envCheck;
const pc = currentPc();
// 둘 다 여기서 받는다. 이슈를 아래 동기 git(상대 NOW·작업 파일·peer 상태) 뒤에 받으면 같은 경합으로 사라진다 (reviewer)
const [rows, issuesRes, roadmapPrsRes] = await Promise.all([
  budget.within(checksP, late('환경 점검', [{ status: 'warn', item: '환경 점검', detail: `${OVER_BUDGET}로 결과를 기다리지 않음 → node scripts/doctor.mjs` }]), LOCAL_RESERVE_MS),
  budget.within(issuesP, late('이슈 조회', { ok: false, reason: OVER_BUDGET }), LOCAL_RESERVE_MS),
  budget.within(roadmapPrsP, late('로드맵 상태 조회', { ok: false, reason: OVER_BUDGET }), LOCAL_RESERVE_MS),
]);
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
if (!pc.id) warnings.push('이 PC가 등록되지 않아 내 NOW 파일을 정할 수 없습니다 (NOW 주입·Stop hook 체크포인트 검사가 꺼지고 인계 기록에 PC를 적을 수 없음). 작업 전에 사용자에게 `node scripts/setup.mjs --pc <id>` 실행을 요청하세요.');
if (rows.some((r) => r.status === 'fail')) warnings.push('환경 점검에 ❌ 항목이 있습니다. 작업 전에 사용자와 해결하세요 (PC·환경 섹션).');

// 사람별 NOW 파일: 내 것은 전문, 상대 것은 원격 브랜치·작업 트리 중 가장 최근 것의 요약만
// env-check를 못 불러와도(pc.id null) 로컬 등록(.harness/pc.json)으로 NOW 주입은 살린다
const me = ownerKey(pc.id ?? localPcId());
const now = me ? readText(nowFileOf(me)) : null;
const other = me ? otherOwnerKey(me) : null;
const otherNow = other ? latestPeerNow(other) : null;
// 같은 사람의 다른 PC가 진행 중인 브랜치 (#54): develop의 내 NOW가 그 진행을 가려 이미 한 일(/spec 등)을 다시 할 수 있다
const myBranchMs = budget.timeout(MY_BRANCH_BUDGET_MS, 2000);
if (me && inRepo && myBranchMs < 300) budget.skip('내 원격 브랜치 조회');
const myBranches =
  me && inRepo && myBranchMs >= 300
    ? myBranchLines(myBranchNows(me, { budgetMs: myBranchMs }), branchName)
    : { lines: [], others: [], truncated: Boolean(me && inRepo) };
if (myBranches.others.length) {
  warnings.push(
    `develop에 머지 안 된 내 다른 브랜치에 진행이 있습니다: ${myBranches.others.map((b) => b.replace(/^origin\//, '')).join(', ')}. ` +
      '내 NOW와 다른 일이면 /pickup에서 그 브랜치로 switch할지 확인합니다 (같은 기능을 새로 시작하지 않는다).',
  );
}
const myEntry = me ? lastWorklogEntryOf(worklogEntries(readText(WORKLOG_FILE)), me, loadPcs()) : null;

// 이전 세션이 체크포인트 없이 남긴 변경 (Stop hook은 이 세션이 건드린 파일만 보므로 여기서 알린다)
const unrecorded = inRepo && me ? workFilesNewerThanNow(ROOT, nowFileOf(me)) : [];
if (unrecorded.length) {
  warnings.push(
    `${nowRelPath(me)}보다 나중에 바뀐 미커밋 작업 파일 ${unrecorded.length}개 (${unrecorded.slice(0, 3).join(', ')}${unrecorded.length > 3 ? ' …' : ''}). ` +
      '이전 세션이 체크포인트 없이 끝났거나 같은 작업 트리에서 다른 세션이 작업 중일 수 있습니다. /pickup에서 git diff로 실제 진행을 확인하고, 다른 세션의 파일은 건드리지 않습니다.',
  );
}

// 직전 작업 PC: 내 NOW 갱신 줄과 내 마지막 작업로그 항목 중 더 최근 것 (상대 기록은 보지 않는다)
const prevPc = me ? previousPc({ nowText: now, entry: myEntry, knownIds: Object.keys(loadPcs()), key: me }) : null;
if (prevPc === 'unknown-pc' && pc.id) {
  warnings.push('직전 작업 PC를 알 수 없습니다(미등록 PC에서 인계). git pull로 최신화하고, 이 PC에서 verify.mjs가 통과하는지 먼저 봅니다.');
} else if (prevPc && pc.id && prevPc !== pc.id) {
  warnings.push(
    `직전 작업은 다른 PC(${prevPc})에서 했습니다. git pull로 최신화했는지, 그 PC에만 있던 것(push 안 한 커밋, 로컬 DB 데이터, 환경변수 값, 설치한 도구)이 필요한지 확인하고, 이 PC에서 verify.mjs가 통과하는지 먼저 봅니다.`,
  );
}
if (me) {
  parts.push(`## ${nowRelPath(me)} (내 작업)\n${now ? now.trim() : '(없음)'}`);
  // 시간 제한으로 일부를 못 봤으면 다른 브랜치가 0개여도 생략 사실을 보인다
  if (myBranches.others.length || myBranches.truncated) {
    parts.push(
      `## 내 원격 브랜치 (develop 미머지 · ${nowRelPath(me)}을 바꾼 것)\n${myBranches.lines.join('\n')}\n` +
        '- ⚠️ 브랜치의 NOW가 위 내 NOW와 엇갈리면 /pickup 보고에 넣고, 어느 쪽을 이어 갈지 사용자에게 묻는다',
    );
  }
  if (otherNow) {
    const h = nowHeadline(otherNow.text);
    parts.push(
      `## 상대 작업 (${nowRelPath(other)} · 출처 ${otherNow.source}, 수정하지 않는다)\n- ${h.stamp}\n- 현재 기능: ${h.feature}\n- 다음: ${h.next}`,
    );
  }
  // peer-watch hook의 비교 기준 (PR 목록은 직전 것을 유지해 자리를 비운 사이의 변화를 알린다)
  if (inRepo) {
    const prevState = readPeerState();
    writePeerState({
      checkedAt: Date.now(),
      developSha: git(['rev-parse', '-q', '--verify', 'origin/develop']).out || null,
      peerNow: otherNow ? { source: otherNow.source, ...nowHeadline(otherNow.text) } : null,
      prs: prevState?.prs ?? null,
    });
  }
}
if (myEntry) parts.push(`## 내 작업로그 마지막 항목\n${myEntry.text.trim()}`);

// 이슈 표시가 깨져도 NOW 주입은 살린다
let issueSummary = null;
let issueList = null;
let issueFailure = null;
try {
  const res = me ? issuesRes : null;
  if (res && !res.ok) issueFailure = issueFailureLine(res.reason);
  issueSummary = res?.ok ? summarizeIssues(res.issues, { key: me, github: pc.github }) : null;
  issueList = issueSummary ? issueContext(issueSummary) : null;
} catch (e) {
  issueSummary = null;
  issueFailure = issueFailureLine(`표시 오류: ${e?.message ?? e}`);
}
if (issueFailure) {
  parts.push(`## 열린 follow-up 이슈\n- ${issueFailure}\n- /pickup에서 위 명령으로 직접 확인한다 ("이슈 없음"으로 보고하지 않는다)`);
} else if (issueList) {
  parts.push(
    `## 열린 follow-up 이슈 (차단·내 영역·공통)\n${issueList}\n` +
      '- 기능 사이 남는 시간에 내 영역 이슈를 처리하고, /spec 때 이 기능에 흡수할 이슈를 spec "관련 이슈"에 적는다 (docs/conventions.md 7절)',
  );
}

// 로드맵이 깨져도 NOW 주입은 살린다 (형식 오류는 블록에 적어 /pickup에서 고치게 한다)
let roadmapLines = [];
try {
  const md = me ? readText(ROADMAP_FILE) : null;
  if (md) {
    const { roadmap, view } = loadView(md, roadmapPrsRes);
    const peerName = other ? Object.entries(loadPcs()).find(([id]) => ownerKey(id) === other)?.[1].owner : undefined;
    const errors = roadmap.errors.length ? `\n- ⚠️ ${ROADMAP_REL} 형식 오류 ${roadmap.errors.length}건 → \`node scripts/roadmap.mjs --check\`` : '';
    parts.push(`${roadmapBlock(view, me, { branch: branchName, peerName })}${errors}\n- /pickup 보고에 🗺️ 줄로 넣는다`);
    roadmapLines = bannerLines(view, me);
    // 상태 표시줄(HX-6)이 세션 시작부터 다음 항목·넘겨주는 시점을 보이게 캐시를 채운다 (상태를 모르면 표시줄의 새로 고침에 맡긴다)
    if (view.statusOk) writeStatus({ roadmap: { at: Date.now(), ...roadmapSnapshot(view, me, branchName) } });
  }
} catch (e) {
  parts.push(`## 로드맵\n- ⚠️ 표시 오류: ${e?.message ?? e} → \`node scripts/roadmap.mjs\``);
}

if (budget.skipped.length) {
  warnings.push(
    `세션 시작 시간 예산(${sessionStartBudgetMs() / 1000}초)이 모자라 건너뛴 것: ${budget.skipped.join(', ')} (H-5). /pickup에서 필요한 것을 직접 확인합니다 (git fetch·gh pr status·node scripts/branches.mjs).`,
  );
}
if (warnings.length) parts.splice(1, 0, `## ⚠️ 주의\n${warnings.map((w) => `- ${w}`).join('\n')}`);
parts.push(
  input.source === 'compact'
    ? '컨텍스트가 압축되었다. 위 내 NOW 파일과 현재 spec을 기준으로 이어서 작업한다.'
    : `이어서 작업하려면 /pickup 을 사용한다. 작업 단위가 끝날 때마다 ${me ? nowRelPath(me) : '내 NOW 파일'}을 갱신한다.`,
);

// additionalContext는 모델에만 간다. 사람에게는 systemMessage로 누구·어느 PC인지 바로 보여 준다
// 배너가 깨져도 NOW 주입은 살린다
let banner;
try {
  // env-check를 못 불러와도 로컬 등록이 있으면 미등록으로 보이지 않게 한다
  const shownPc = pc.id || !localPcId() ? pc : { id: localPcId(), label: '(env-check 로드 실패)' };
  banner = sessionBanner({
    pc: shownPc,
    branch: inRepo ? branchName || '(detached)' : null,
    envBad: bad.map((r) => `${icon(r.status)} ${r.item}`),
    warnings,
    syncLines: synced.filter((l) => /^- (✅|⚠️|⏸️)/.test(l)),
    issueLine: issueFailure ?? (issueSummary ? issueBannerLine(issueSummary) : null),
    roadmapLines,
    myFeature: me && now ? nowHeadline(now).feature : null,
    peerName: other ? Object.entries(loadPcs()).find(([id]) => ownerKey(id) === other)?.[1].owner : null,
    peerFeature: otherNow ? nowHeadline(otherNow.text).feature : null,
  });
} catch {
  banner = undefined;
}

// 기다리지 않은 외부 명령(gh 등)이 프로세스를 붙잡지 않게 출력을 다 쓴 뒤 바로 끝낸다 (H-5)
process.stdout.write(
  JSON.stringify({
    ...(banner ? { systemMessage: banner } : {}),
    hookSpecificOutput: { hookEventName: 'SessionStart', additionalContext: parts.join('\n\n') },
  }),
  () => process.exit(0),
);
