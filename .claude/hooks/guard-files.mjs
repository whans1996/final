#!/usr/bin/env node
// PreToolUse(Edit|Write|MultiEdit) hook
//  - .env 파일 수정 금지 (.env.example은 허용)
//  - 이미 커밋된 Flyway 버전 마이그레이션(V*.sql) 수정 금지 → 새 버전 파일을 만들어야 함
//  - 상대의 NOW 파일(docs/handoff/NOW-<상대>.md) 수정 금지
//  - 작성 내용에 실제 키로 보이는 문자열이 있으면 차단
//  (상대 영역 코드는 막지 않고 PostToolUse area-notice hook이 알린다)
import { basename, isAbsolute, relative, resolve } from 'node:path';
import { ROOT, git, hasCommits, localPcId, nowRelPath, otherOwnerKey, ownerKey, readStdinJson } from '../../scripts/lib/common.mjs';

const input = readStdinJson();
const ti = input?.tool_input || {};
const filePath = ti.file_path || ti.path;
if (!filePath) process.exit(0);

const abs = isAbsolute(filePath) ? filePath : resolve(input.cwd || ROOT, filePath);
const rel = relative(ROOT, abs).replace(/\\/g, '/');
const name = basename(abs);

const block = (reason) => {
  console.error(`🚫 하네스 가드가 파일 변경을 차단했습니다 (${rel}): ${reason}`);
  process.exit(2);
};

// 1) .env
if (/^\.env(\..+)?$/.test(name) && name !== '.env.example') {
  block('.env 파일은 AI가 수정하지 않습니다. 필요한 변수는 .env.example에 이름만 추가하고, 값은 사용자에게 직접 넣도록 안내하세요.');
}

// 2) 커밋된 Flyway 버전 마이그레이션
if (/(^|\/)db\/migration\/V[^/]*\.sql$/i.test(rel) && hasCommits()) {
  if (git(['cat-file', '-e', `HEAD:${rel}`]).ok) {
    block('이미 커밋된 Flyway 마이그레이션은 수정할 수 없습니다 (상대 PC DB와 체크섬이 어긋남). 다음 번호의 V{n}__설명.sql을 새로 만드세요.');
  }
}

// 3) 상대 NOW 파일 (사람별 스냅샷은 본인만 쓴다, 11-19 결정). 미등록 PC는 판정할 수 없어 건너뛴다
const me = ownerKey(localPcId());
if (me && rel === nowRelPath(otherOwnerKey(me))) {
  block(`상대의 NOW 파일입니다. 내 상태는 ${nowRelPath(me)}에만 쓰고, 상대에게 전할 말은 PR 댓글이나 Mattermost로 남기세요.`);
}

// 4) 비밀 키 패턴
const texts = [ti.content, ti.new_string, ...(Array.isArray(ti.edits) ? ti.edits.map((e) => e?.new_string) : [])].filter(
  (t) => typeof t === 'string',
);
const SECRETS = [
  [/AIza[0-9A-Za-z_\-]{35}/, 'Google(Gemini) API 키'],
  [/\bsk-(?:ant-|proj-)?[A-Za-z0-9_\-]{32,}/, 'API 비밀 키(sk-)'],
  [/-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----/, '개인 키'],
  [/\bglpat-[0-9A-Za-z_\-]{20,}/, 'GitLab 토큰'],
  [/\bgh[pousr]_[A-Za-z0-9]{36,}/, 'GitHub 토큰'],
  [/\bAKIA[0-9A-Z]{16}\b/, 'AWS 액세스 키'],
];
for (const text of texts) {
  for (const [re, label] of SECRETS) {
    if (re.test(text)) block(`${label}로 보이는 값이 포함되어 있습니다. 키는 환경변수(예: \${GEMINI_API_KEY})로 참조하세요.`);
  }
}

process.exit(0);
