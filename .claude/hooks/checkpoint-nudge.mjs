#!/usr/bin/env node
// Stop hook
// 1. 코드를 바꿨는데 내 NOW 파일(docs/handoff/NOW-<dh|js>.md)을 갱신하지 않고 턴을 끝내려 하면 한 번 막고 갱신을 지시한다.
//    세션이 토큰 소진으로 갑자기 끊겨도 내 다른 PC(또는 상대)가 이어받을 수 있게 하기 위한 장치다.
//    루프 방지: stop_hook_active(이미 한 번 막힌 뒤의 재시도)면 통과시킨다.
//    이 세션(서브에이전트 포함)이 건드린 파일만 본다: 같은 작업 트리에서 다른 세션이 바꾼 파일로 막지 않는다 (11-19 오탐).
//    미커밋 변경과, 그 파일을 커밋까지 하고 NOW 없이 멈춘 경우(H-11)를 본다. 판정은 scripts/lib/checkpoint.mjs (HP-4 AC5).
//    이전 세션이 남긴 미기록 변경은 SessionStart hook이 경고한다.
//    계획 모드에서는 NOW 파일을 고칠 수 없으므로 검사하지 않는다.
// 2. 체크포인트가 최신인데 컨텍스트가 200k를 넘었으면 사용자에게 /clear를 권한다 (100k마다 한 번).
//    턴마다 컨텍스트 전체를 다시 읽으므로 비용의 대부분이 여기서 나온다 (11-18 실측: 메인 cache read 78%).
//    NOW 파일이 최신이면 /clear 후 SessionStart hook이 다시 주입하므로 이어받기 비용이 작다.
import { mkdirSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { findMissingCheckpoint, parseCommitLog, sessionTouchedFrom, shouldCheck } from '../../scripts/lib/checkpoint.mjs';
import {
  ROOT,
  dirtyWorkFiles,
  git,
  isGitRepo,
  lastContextTokens,
  myNowFile,
  mtimeOf,
  nowRelPath,
  ownerKey,
  localPcId,
  readStdinJson,
  readText,
} from '../../scripts/lib/common.mjs';

const CONTEXT_NUDGE_TOKENS = 200_000;
const NUDGE_STEP = 100_000;
const NUDGE_STATE = join(ROOT, '.harness', 'context-nudge.json');
// 커밋 후 미기록 판정에서 볼 최근 커밋 수 (NOW보다 늦은 커밋만 쓴다)
const RECENT_COMMITS = 30;

/** 메인 transcript와 같은 세션의 서브에이전트 transcript들 */
function transcriptsOf(transcriptPath, sessionId) {
  const files = [transcriptPath];
  if (sessionId) {
    const dir = join(dirname(transcriptPath), sessionId, 'subagents');
    try {
      for (const f of readdirSync(dir)) if (f.endsWith('.jsonl')) files.push(join(dir, f));
    } catch {
      // 서브에이전트 없음
    }
  }
  return files;
}

/** 체크포인트 누락 (scripts/lib/checkpoint.mjs). 없거나 PC 미등록이면 null */
function missingCheckpoint() {
  const nowFile = myNowFile();
  if (!nowFile) return null;
  const nowMtime = mtimeOf(nowFile) ?? 0;
  const texts = input.transcript_path ? transcriptsOf(input.transcript_path, input.session_id).map(readText) : [null];
  const log = git(['log', '--no-merges', `-${RECENT_COMMITS}`, `--since=${new Date(nowMtime).toISOString()}`, '--format=%x1e%H%x1f%ae%x1f%ct%x1f%P%x1f%s', '--name-only', 'HEAD']);
  return findMissingCheckpoint({
    dirty: dirtyWorkFiles(),
    commits: log.ok ? parseCommitLog(log.out) : [],
    nowMtime,
    myEmail: git(['config', 'user.email']).out,
    touched: sessionTouchedFrom(texts, ROOT),
    mtimeOf: (p) => mtimeOf(join(ROOT, p)),
  });
}

/** 컨텍스트가 기준을 새로 넘었으면 안내 문구, 아니면 null. 세션별로 100k 구간마다 한 번만 */
function contextNudge() {
  if (!input.transcript_path) return null;
  const ctx = lastContextTokens(input.transcript_path);
  if (ctx < CONTEXT_NUDGE_TOKENS) return null;

  const step = Math.floor(ctx / NUDGE_STEP);
  let state = {};
  try {
    state = JSON.parse(readText(NUDGE_STATE) || '{}');
  } catch {
    state = {};
  }
  const key = input.session_id || input.transcript_path;
  if ((state[key] || 0) >= step) return null;
  // 다른 세션 기록은 남긴다 (덮어쓰면 세션을 오갈 때마다 다시 안내된다). 최근 20개만 유지
  delete state[key];
  state[key] = step;
  const keep = Object.entries(state).slice(-20);
  try {
    mkdirSync(join(ROOT, '.harness'), { recursive: true });
    writeFileSync(NUDGE_STATE, JSON.stringify(Object.fromEntries(keep)));
  } catch {
    // 상태를 못 남기면 다음 턴에 다시 안내될 뿐이다
  }
  return (
    `컨텍스트 ${Math.round(ctx / 1000)}k 토큰: 턴마다 이 전체를 다시 읽습니다. ` +
    '작업 단위(spec 승인, PR 생성, 머지 후 정리)가 끝났다면 /clear로 새로 시작하세요. 내 NOW 파일은 최신이고 새 세션에 자동으로 주입됩니다.'
  );
}

const input = readStdinJson();
if (!shouldCheck(input)) process.exit(0);
if (!isGitRepo()) process.exit(0);

const missing = missingCheckpoint();
if (missing) {
  const nowFile = nowRelPath(ownerKey(localPcId()));
  process.stdout.write(
    JSON.stringify({
      decision: 'block',
      reason:
        (missing.kind === 'committed'
          ? `체크포인트 누락: ${missing.path}을 바꾼 커밋 ${missing.sha.slice(0, 7)}(${missing.subject})이 ${nowFile}보다 나중입니다. `
          : `체크포인트 누락: ${missing.path} 등이 ${nowFile}보다 나중에 변경되었습니다. `) +
        `세션이 언제 끊길지 모르니 ${nowFile}의 "현재 기능 / 진행 / 바로 다음 할 일 / 막힌 점 / 주의" 섹션을 지금 상태로 갱신한 뒤 턴을 마치세요. ` +
        '본문(## 현재 기능부터, 갱신 줄 없이)을 .harness/now-body.md에 쓰고 `node scripts/stamp.mjs --write-now .harness/now-body.md`로 바꿉니다(머리말·갱신 줄은 stamp가 씁니다). 상대의 NOW 파일은 건드리지 않습니다. ' +
        '"바로 다음 할 일"에는 파일·메서드·테스트 이름을 구체적으로 적습니다. 다른 설명은 덧붙이지 마세요.',
    }),
  );
  process.exit(0);
}

const nudge = contextNudge();
if (nudge) process.stdout.write(JSON.stringify({ systemMessage: nudge }));
process.exit(0);
