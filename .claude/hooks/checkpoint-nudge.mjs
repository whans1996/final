#!/usr/bin/env node
// Stop hook
// 1. 코드를 바꿨는데 NOW.md를 갱신하지 않고 턴을 끝내려 하면 한 번 막고 갱신을 지시한다.
//    세션이 토큰 소진으로 갑자기 끊겨도 상대가 이어받을 수 있게 하기 위한 장치다.
//    루프 방지: stop_hook_active(이미 한 번 막힌 뒤의 재시도)면 통과시킨다.
// 2. 체크포인트가 최신인데 컨텍스트가 200k를 넘었으면 사용자에게 /clear를 권한다 (100k마다 한 번).
//    턴마다 컨텍스트 전체를 다시 읽으므로 비용의 대부분이 여기서 나온다 (11-18 실측: 메인 cache read 78%).
//    NOW.md가 최신이면 /clear 후 SessionStart hook이 다시 주입하므로 이어받기 비용이 작다.
import { mkdirSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import {
  NOW_FILE,
  ROOT,
  git,
  isGitRepo,
  lastContextTokens,
  readStdinJson,
  readText,
} from '../../scripts/lib/common.mjs';

const CONTEXT_NUDGE_TOKENS = 200_000;
const NUDGE_STEP = 100_000;
const NUDGE_STATE = join(ROOT, '.harness', 'context-nudge.json');

// 체크포인트 대상: 실제 작업 산출물 (하네스 자체, 기획서는 제외)
const WATCH = /^(backend\/|frontend\/|docs\/specs\/)/;
const IGNORE = /(^|\/)(node_modules|build|dist|\.gradle)\/|(^|\/)CLAUDE\.md$/;

/** NOW.md보다 나중에 바뀐 작업 파일 경로. 없으면 null */
function missingCheckpoint() {
  const st = git(['status', '--porcelain', '-z', '--untracked-files=all']);
  if (!st.ok || !st.out) return null;

  // -z 형식: "XY path\0" (rename/copy는 뒤에 원래 경로가 하나 더 붙음)
  const tokens = st.out.split('\0').filter(Boolean);
  const paths = [];
  for (let i = 0; i < tokens.length; i++) {
    const t = tokens[i];
    const xy = t.slice(0, 2);
    paths.push(t.slice(3));
    if (/[RC]/.test(xy)) i++;
  }

  let latest = 0;
  let latestPath = '';
  for (const p of paths) {
    if (!WATCH.test(p) || IGNORE.test(p)) continue;
    try {
      const m = statSync(join(ROOT, p)).mtimeMs;
      if (m > latest) {
        latest = m;
        latestPath = p;
      }
    } catch {
      // 삭제된 파일
    }
  }
  if (!latest) return null;

  let nowMtime = 0;
  try {
    nowMtime = statSync(NOW_FILE).mtimeMs;
  } catch {
    // NOW.md 없음
  }
  return latest > nowMtime + 1000 ? latestPath : null;
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
  try {
    mkdirSync(join(ROOT, '.harness'), { recursive: true });
    writeFileSync(NUDGE_STATE, JSON.stringify({ [key]: step }));
  } catch {
    // 상태를 못 남기면 다음 턴에 다시 안내될 뿐이다
  }
  return (
    `컨텍스트 ${Math.round(ctx / 1000)}k 토큰: 턴마다 이 전체를 다시 읽습니다. ` +
    '작업 단위(spec 승인, PR 생성, 머지 후 정리)가 끝났다면 /clear로 새로 시작하세요. NOW.md는 최신이고 새 세션에 자동으로 주입됩니다.'
  );
}

const input = readStdinJson();
if (input.stop_hook_active) process.exit(0);
if (!isGitRepo()) process.exit(0);

const missing = missingCheckpoint();
if (missing) {
  process.stdout.write(
    JSON.stringify({
      decision: 'block',
      reason:
        `체크포인트 누락: ${missing} 등이 docs/handoff/NOW.md보다 나중에 변경되었습니다. ` +
        '세션이 언제 끊길지 모르니 NOW.md의 "현재 기능 / 진행 / 바로 다음 할 일 / 막힌 점 / 주의" 섹션을 지금 상태로 덮어써서 갱신한 뒤 턴을 마치세요. ' +
        '"바로 다음 할 일"에는 파일·메서드·테스트 이름을 구체적으로 적습니다. 다른 설명은 덧붙이지 마세요.',
    }),
  );
  process.exit(0);
}

const nudge = contextNudge();
if (nudge) process.stdout.write(JSON.stringify({ systemMessage: nudge }));
process.exit(0);
