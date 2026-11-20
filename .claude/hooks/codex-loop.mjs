#!/usr/bin/env node
// Stop hook: 현재 feature 브랜치의 열린 PR이 Codex 리뷰를 통과(COMPLETE)할 때까지 루프를 이어 가게 한다
// (11-19 사용자 지시 "Codex 리뷰가 통과될 때까지 계속 리뷰를 받고 고치도록"). 절차의 정본은 /pr-review 8단계다.
//   STALE    push한 head에 리뷰 요청이 없음, 또는 head 리뷰가 실패·정체함 → 막고 `--request --wait --status`(백그라운드)를 지시
//            (실패·정체 재요청은 같은 head의 일반 STALE과 따로 한 번 더 반응한다, #48)
//   RUNNING  요청은 했는데 이 세션에서 --wait를 백그라운드로 돌린 적이 없음 → 막고 --wait를 지시 (끝나도 알림을 못 받으므로)
//   FINDINGS 재리뷰 5회 미만 → 막고 8-3 처리 지시, 5회 이상 → 사람 판단 안내(systemMessage)
//   ERROR    Codex 환경·계정 미설정, 재시도 뒤에도 실패·정체, gh 실패 → 안내(systemMessage)
// (PR, head, 판정)마다 한 번만 반응한다: 지적을 반박하거나 사람에게 넘기고 멈춘 경우 매 턴 다시 막지 않게.
// draft PR은 보지 않는다. 판정 규칙은 scripts/lib/codex.mjs(loopAction·skipRecheck·waitingInTranscript, 테스트 있음)에 있고,
// 판정 자체는 scripts/codex-review.mjs(대기 없음, 읽기 전용)의 첫 줄을 쓴다.
// 네트워크 비용: 판정한 PR head가 로컬 HEAD이고 COMPLETE면 건너뛰고, 그 밖에는 3분에 한 번만 확인한다.
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { NON_INTERACTIVE_GIT_ENV, ROOT, currentBranch, git, isGitRepo, readStdinJson, readText } from '../../scripts/lib/common.mjs';
import { MAX_REVIEW_ROUNDS, loopAction, skipRecheck, waitingInTranscript } from '../../scripts/lib/codex.mjs';

const STATE_FILE = join(ROOT, '.harness', 'codex-loop.json');
const RECHECK_MS = 3 * 60_000;

const input = readStdinJson();
if (input.stop_hook_active || input.permission_mode === 'plan') process.exit(0);
if (!isGitRepo()) process.exit(0);
const branch = currentBranch();
if (!branch?.startsWith('feature/')) process.exit(0);
const head = git(['rev-parse', 'HEAD']).out;

let state = {};
try {
  state = JSON.parse(readText(STATE_FILE) || '{}');
} catch {
  state = {};
}
const prev = state[branch] || {};
if (skipRecheck(prev, head, Date.now(), RECHECK_MS)) process.exit(0);

function save(entry) {
  state[branch] = { handled: prev.handled || [], head, at: Date.now(), prHead: null, ...entry };
  try {
    mkdirSync(join(ROOT, '.harness'), { recursive: true });
    writeFileSync(STATE_FILE, JSON.stringify(state));
  } catch {
    // 상태를 못 남기면 다음 턴에 다시 확인할 뿐이다
  }
}

// 판정 스크립트가 fetch할 때 Windows 인증 창으로 멈추지 않게 한다
const env = { ...process.env, ...NON_INTERACTIVE_GIT_ENV };
const run = (cmd, args, timeout) => spawnSync(cmd, args, { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout, env });

const view = run('gh', ['pr', 'view', '--json', 'number,state,isDraft'], 8_000);
if (view.error || view.status !== 0) {
  save({ verdict: 'NOPR' }); // PR이 아직 없거나 gh가 없다. PR을 만들면 3분 뒤 다시 본다
  process.exit(0);
}
const pr = JSON.parse(view.stdout);
if (pr.state !== 'OPEN' || pr.isDraft) {
  save({ verdict: pr.isDraft ? 'DRAFT' : 'CLOSED' });
  process.exit(0);
}

const judged = run(process.execPath, [join(ROOT, 'scripts', 'codex-review.mjs'), String(pr.number)], 15_000);
const line = (judged.stdout || '').split('\n')[0].trim();
if (judged.error || !line) {
  save({ verdict: 'TIMEOUT' }); // 판정 없이 넘어가고 3분 뒤 다시 본다 (매 턴 15초씩 쓰지 않게)
  process.exit(0);
}

const n = pr.number;
const handled = prev.handled || [];
const waiting = () => waitingInTranscript(readText(input.transcript_path || '') || '', n);
// 실패·정체 재요청(STALE)도 기다리는 프로세스가 있으면 그쪽이 재시도한다 (중복 재시도 방지)
const retry = /^STALE .*(실패|정체)/.test(line);
const act = loopAction({ line, pr: n, handled, waiting: (line.startsWith('RUNNING') || retry) && waiting() });
save({ verdict: act.verdict, prHead: act.prHead, handled: handled.includes(act.key) ? handled : [...handled, act.key].slice(-20) });

const waitCmd = `node scripts/codex-review.mjs ${n} --request --wait --status`;
const REASON = {
  request: `${retry ? 'head의 Codex 리뷰가 실패하거나 멈춰 재요청이 필요합니다' : 'push한 head가 아직 리뷰 요청 전입니다'}. \`${waitCmd}\`를 Bash run_in_background로 실행하고 완료 알림을 기다리세요 (/pr-review 8단계). push하지 않은 커밋이 있으면 먼저 push합니다.`,
  wait: `리뷰가 진행 중인데 이 세션에서 기다리는 프로세스가 없습니다. \`${waitCmd}\`를 Bash run_in_background로 실행하고 완료 알림을 기다리세요.`,
  fix:
    `재리뷰 ${act.rounds}/${MAX_REVIEW_ROUNDS}회. \`node scripts/codex-review.mjs ${n}\`로 지적을 확인하고 /pr-review 8-3대로 처리하세요: ` +
    'P1과 spec 수용 기준·보안 기준을 깨는 지적은 실패하는 테스트부터 써서 고치고 push한 뒤 ' +
    `\`${waitCmd}\`를 백그라운드로 다시 실행합니다. 그 밖의 P2는 follow-up 이슈(area 라벨·담당자 포함)로 넘기고 --resolve합니다(Codex 체크도 ✅로 바뀝니다). ` +
    '지적이 타당하지 않거나 같은 주제로 2회 재지적되면 멈추고 사용자에게 보고합니다.',
};
const NOTICE = {
  limit: `재리뷰 ${act.rounds}회(상한 ${MAX_REVIEW_ROUNDS}회)에도 지적이 남았습니다. 사람이 정하세요: 남은 지적을 follow-up 이슈로 넘기고 --resolve, 또는 설계 변경.`,
  setup: `${line.replace(/^ERROR /, '')} (설정 방법: docs/환경설정.md 8절)`,
  error: line,
};

if (act.action === 'block') {
  process.stdout.write(JSON.stringify({ decision: 'block', reason: `Codex 리뷰 루프 (PR #${n}): ${line}. ${REASON[act.kind]}` }));
} else if (act.action === 'notify') {
  process.stdout.write(JSON.stringify({ systemMessage: `Codex 리뷰 (PR #${n}): ${NOTICE[act.kind]}` }));
}
process.exit(0);
