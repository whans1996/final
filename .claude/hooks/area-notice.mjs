#!/usr/bin/env node
// PostToolUse(Edit|Write|MultiEdit) hook: 상대 영역·공통 파일(.github/CODEOWNERS)을 수정하면 Claude에게 알린다.
// 막지는 않는다 (11-19 결정: 상대 영역 수정은 경고만). 같은 세션에서 파일마다 한 번만 알린다. 항상 exit 0.
import { mkdirSync, writeFileSync } from 'node:fs';
import { isAbsolute, join, relative, resolve } from 'node:path';
import { ROOT, localPcId, otherOwnerKey, ownerKey, readStdinJson, readText } from '../../scripts/lib/common.mjs';

try {
  const { loadPcs } = await import('../../scripts/lib/env-check.mjs');
  const { areaOf, loadOwnerRules, personOf } = await import('../../scripts/lib/owners.mjs');

  const input = readStdinJson();
  const filePath = input?.tool_input?.file_path || input?.tool_input?.path;
  const me = ownerKey(localPcId());
  if (!filePath || !me) process.exit(0);

  const abs = isAbsolute(filePath) ? filePath : resolve(input.cwd || ROOT, filePath);
  const rel = relative(ROOT, abs).replace(/\\/g, '/');
  const pcs = loadPcs();
  const area = areaOf(rel, me, loadOwnerRules(), pcs);
  if (area !== 'peer' && area !== 'common') process.exit(0);

  // 세션마다 알린 파일 목록 (다른 세션의 기록은 버린다)
  const stateFile = join(ROOT, '.harness', 'area-notice.json');
  let state = {};
  try {
    state = JSON.parse(readText(stateFile) || '{}');
  } catch {}
  const session = input.session_id || '?';
  const seen = state.session === session ? state.files || [] : [];
  if (seen.includes(rel)) process.exit(0);
  mkdirSync(join(ROOT, '.harness'), { recursive: true });
  writeFileSync(stateFile, JSON.stringify({ session, files: [...seen, rel] }) + '\n');

  const peer = personOf(otherOwnerKey(me), pcs);
  const mention = `PR 설명 "공통 코드 변경"에 이 파일과 이유를 적고 @${peer?.github ?? '상대'}를 멘션한다`;
  const msg =
    area === 'peer'
      ? `⚠️ ${rel}은 ${peer?.owner ?? '상대'} 영역입니다 (.github/CODEOWNERS). 내 기능에 꼭 필요한 최소 변경인지 확인하고, 아니면 되돌린 뒤 사용자에게 알린다. 유지한다면 ${mention}.`
      : `ℹ️ ${rel}은 공통 파일입니다 (.github/CODEOWNERS). 상대 작업과 겹칠 수 있으니 최소로 바꾸고(enum·routes·nav는 도메인 블록 안에만 추가), ${mention}.`;
  process.stdout.write(JSON.stringify({ hookSpecificOutput: { hookEventName: 'PostToolUse', additionalContext: msg } }));
} catch {
  // 알림 실패는 작업을 막지 않는다
}
process.exit(0);
