#!/usr/bin/env node
// Stop hook
// 1. 코드를 바꿨는데 NOW.md를 갱신하지 않고 턴을 끝내려 하면 한 번 막고 갱신을 지시한다.
//    세션이 토큰 소진으로 갑자기 끊겨도 상대가 이어받을 수 있게 하기 위한 장치다.
//    루프 방지: stop_hook_active(이미 한 번 막힌 뒤의 재시도)면 통과시킨다.
//    이 세션(서브에이전트 포함)이 건드린 파일만 본다: 같은 작업 트리에서 다른 세션이 바꾼 파일로 막지 않는다 (11-19 오탐).
//    이전 세션이 남긴 미기록 변경은 SessionStart hook이 경고한다.
//    계획 모드에서는 NOW.md를 고칠 수 없으므로 검사하지 않는다.
// 2. 체크포인트가 최신인데 컨텍스트가 200k를 넘었으면 사용자에게 /clear를 권한다 (100k마다 한 번).
//    턴마다 컨텍스트 전체를 다시 읽으므로 비용의 대부분이 여기서 나온다 (11-18 실측: 메인 cache read 78%).
//    NOW.md가 최신이면 /clear 후 SessionStart hook이 다시 주입하므로 이어받기 비용이 작다.
import { mkdirSync, readdirSync, writeFileSync } from 'node:fs';
import { basename, dirname, join, relative, resolve } from 'node:path';
import {
  ROOT,
  isGitRepo,
  lastContextTokens,
  readStdinJson,
  readText,
  workFilesNewerThanNow,
} from '../../scripts/lib/common.mjs';

const CONTEXT_NUDGE_TOKENS = 200_000;
const NUDGE_STEP = 100_000;
const NUDGE_STATE = join(ROOT, '.harness', 'context-nudge.json');

// 경로를 받는 편집 도구: Edit·Write·MultiEdit·NotebookEdit, MCP write_file·edit_block 등
const EDIT_TOOL = /edit|write/i;
// 파일 이름 없이 여러 파일을 바꾸는 명령 → 이 세션이 작업 영역 전체를 건드린 것으로 본다
// (git pull·checkout 등은 넣지 않는다: /pickup이 매번 pull하므로 넣으면 오탐 방지가 무력해진다)
const BROAD_COMMAND = /\bnpm\s+(i|install|update|uninstall)\b|\bprettier\b.*--write|--fix\b|spotlessApply/i;
// 명령에 이 이름만 나와서는 어느 파일인지 알 수 없다
const GENERIC_NAMES = new Set(['index.js', 'index.ts', 'package.json', 'package-lock.json', 'build.gradle', 'readme.md', 'claude.md']);

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

/**
 * 이 세션이 건드린 파일 판정 함수. 메인 transcript를 못 읽으면 null(= 전부 이 세션 것으로 본다).
 * 편집 도구의 경로는 정확히, 셸 명령은 명령 문자열에 경로(뒷부분 두 단계 이상)나 고유한 파일 이름이 있으면 건드린 것으로 본다.
 */
function sessionTouched(transcriptPath, sessionId) {
  if (!transcriptPath || readText(transcriptPath) == null) return null;
  const norm = (p) => p.replace(/\\/g, '/').toLowerCase();
  const edited = new Set();
  const commands = [];
  let broad = false;
  for (const file of transcriptsOf(transcriptPath, sessionId)) {
    for (const line of (readText(file) || '').split('\n')) {
      if (!line.includes('"tool_use"')) continue;
      let o;
      try {
        o = JSON.parse(line);
      } catch {
        continue;
      }
      for (const c of o.message?.content || []) {
        if (c?.type !== 'tool_use' || !c.input) continue;
        const p = c.input.file_path || c.input.notebook_path || c.input.path;
        if (EDIT_TOOL.test(c.name) && typeof p === 'string') edited.add(norm(relative(ROOT, resolve(ROOT, p))));
        else if (typeof c.input.command === 'string') {
          commands.push(norm(c.input.command));
          if (BROAD_COMMAND.test(c.input.command)) broad = true;
        }
      }
    }
  }
  if (broad) return () => true;
  return (path) => {
    const p = norm(path);
    if (edited.has(p)) return true;
    // frontend/src/a/B.vue → "frontend/src/a/b.vue", "src/a/b.vue", "a/b.vue" (cd 후 상대 경로)
    const segs = p.split('/');
    const suffixes = segs.slice(0, -1).map((_, i) => segs.slice(i).join('/'));
    const name = norm(basename(path));
    return commands.some((cmd) => suffixes.some((s) => cmd.includes(s)) || (!GENERIC_NAMES.has(name) && cmd.includes(name)));
  };
}

/** NOW.md보다 나중에 바뀐, 이 세션이 건드린 작업 파일 경로. 없으면 null */
function missingCheckpoint() {
  const touched = sessionTouched(input.transcript_path, input.session_id);
  return workFilesNewerThanNow().find((p) => !touched || touched(p)) || null;
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
    '작업 단위(spec 승인, PR 생성, 머지 후 정리)가 끝났다면 /clear로 새로 시작하세요. NOW.md는 최신이고 새 세션에 자동으로 주입됩니다.'
  );
}

const input = readStdinJson();
if (input.stop_hook_active || input.permission_mode === 'plan') process.exit(0);
if (!isGitRepo()) process.exit(0);

const missing = missingCheckpoint();
if (missing) {
  process.stdout.write(
    JSON.stringify({
      decision: 'block',
      reason:
        `체크포인트 누락: ${missing} 등이 docs/handoff/NOW.md보다 나중에 변경되었습니다. ` +
        '세션이 언제 끊길지 모르니 NOW.md의 "현재 기능 / 진행 / 바로 다음 할 일 / 막힌 점 / 주의" 섹션을 지금 상태로 덮어써서 갱신한 뒤 턴을 마치세요. ' +
        '"갱신:" 줄의 시각·작업자·PC는 지금 값으로 바꿉니다(PC는 SessionStart의 "PC·환경" id). ' +
        '"바로 다음 할 일"에는 파일·메서드·테스트 이름을 구체적으로 적습니다. 다른 설명은 덧붙이지 마세요.',
    }),
  );
  process.exit(0);
}

const nudge = contextNudge();
if (nudge) process.stdout.write(JSON.stringify({ systemMessage: nudge }));
process.exit(0);
