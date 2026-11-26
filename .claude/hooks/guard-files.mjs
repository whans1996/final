#!/usr/bin/env node
// PreToolUse(파일 쓰기·읽기 도구, Desktop Commander MCP 파일 도구 포함) hook. 판정은 scripts/lib/file-guard.mjs(테스트 있음)
//  - .env·키 파일 수정·읽기 금지 (.env.example은 허용)
//  - 이미 커밋된 Flyway 버전 마이그레이션(V*.sql) 수정 금지 → 새 버전 파일을 만들어야 함
//  - 상대의 NOW 파일(docs/handoff/NOW-<상대>.md) 수정 금지
//  - 작성 내용에 실제 키로 보이는 문자열이 있으면 차단 (패턴은 scripts/lib/protect.mjs)
//  - 김동환 PC에서 data/raw/eval 읽기·쓰기·검색 금지 (eval 블라인드)
//  (상대 영역 코드는 막지 않고 PostToolUse area-notice hook이 알린다)
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, git, hasCommits, localPcId, nowRelPath, otherOwnerKey, ownerKey, readStdinJson } from '../../scripts/lib/common.mjs';
import { checkFileTool } from '../../scripts/lib/file-guard.mjs';
import { EVAL_DIR_REL, evalBlockedFor } from '../../scripts/lib/protect.mjs';

const input = readStdinJson();
const me = ownerKey(localPcId());
const reason = checkFileTool(input?.tool_name, input?.tool_input || {}, {
  cwd: input?.cwd || ROOT,
  root: ROOT,
  exists: existsSync,
  evalDir: evalBlockedFor(me) ? join(ROOT, EVAL_DIR_REL) : null,
  committedMigration: (rel) => hasCommits() && git(['cat-file', '-e', `HEAD:${rel}`]).ok,
  // 사람별 스냅샷은 본인만 쓴다 (11-19 결정). 미등록 PC는 판정할 수 없어 건너뛴다
  peerNowRel: me ? nowRelPath(otherOwnerKey(me)) : null,
});
if (!reason) process.exit(0);
console.error(`🚫 하네스 가드가 파일 접근을 차단했습니다: ${reason}`);
process.exit(2);
