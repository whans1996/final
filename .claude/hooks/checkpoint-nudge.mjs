#!/usr/bin/env node
// Stop hook: 코드를 바꿨는데 NOW.md를 갱신하지 않고 턴을 끝내려 하면 한 번 막고 갱신을 지시한다.
// 세션이 토큰 소진으로 갑자기 끊겨도 상대가 이어받을 수 있게 하기 위한 장치다.
// 루프 방지: stop_hook_active(이미 한 번 막힌 뒤의 재시도)면 통과시킨다.
import { statSync } from 'node:fs';
import { join } from 'node:path';
import { NOW_FILE, ROOT, git, isGitRepo, readStdinJson } from '../../scripts/lib/common.mjs';

const input = readStdinJson();
if (input.stop_hook_active) process.exit(0);
if (!isGitRepo()) process.exit(0);

// 체크포인트 대상: 실제 작업 산출물 (하네스 자체, 기획서는 제외)
const WATCH = /^(backend\/|frontend\/|docs\/specs\/)/;
const IGNORE = /(^|\/)(node_modules|build|dist|\.gradle)\/|(^|\/)CLAUDE\.md$/;

const st = git(['status', '--porcelain', '-z', '--untracked-files=all']);
if (!st.ok || !st.out) process.exit(0);

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
if (!latest) process.exit(0);

let nowMtime = 0;
try {
  nowMtime = statSync(NOW_FILE).mtimeMs;
} catch {
  // NOW.md 없음
}

if (latest > nowMtime + 1000) {
  process.stdout.write(
    JSON.stringify({
      decision: 'block',
      reason:
        `체크포인트 누락: ${latestPath} 등이 docs/handoff/NOW.md보다 나중에 변경되었습니다. ` +
        '세션이 언제 끊길지 모르니 NOW.md의 "현재 기능 / 진행 / 바로 다음 할 일 / 막힌 점 / 주의" 섹션을 지금 상태로 덮어써서 갱신한 뒤 턴을 마치세요. ' +
        '"바로 다음 할 일"에는 파일·메서드·테스트 이름을 구체적으로 적습니다. 다른 설명은 덧붙이지 마세요.',
    }),
  );
}
process.exit(0);
