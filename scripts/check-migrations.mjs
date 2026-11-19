#!/usr/bin/env node
// CI: Flyway 버전 번호 검사. 작업 트리(PR이면 base와 합친 결과)의 마이그레이션 버전이 겹치거나,
// PR이 추가한 파일이 base 최신 버전 이하이면 실패한다.
//
// 사용법: node scripts/check-migrations.mjs [--base <ref>]   (기본 origin/develop, ref가 없으면 중복만 검사)
import { readdirSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT } from './lib/common.mjs';
import { MIGRATION_DIR, migrationFilesAt, migrationProblems } from './lib/migrations.mjs';

const args = process.argv.slice(2);
const bi = args.indexOf('--base');
const baseRef = bi >= 0 ? args[bi + 1] : 'origin/develop';

let head;
try {
  head = readdirSync(join(ROOT, MIGRATION_DIR));
} catch {
  console.log('마이그레이션 폴더가 없어 건너뜁니다.');
  process.exit(0);
}
const base = migrationFilesAt(baseRef);
if (!base) console.log(`(base ${baseRef}를 찾지 못해 중복만 검사합니다)`);

const problems = migrationProblems(head, base ?? head);
if (problems.length) {
  for (const p of problems) console.log(`::error::${p}`);
  console.log(
    '\n상대 PR이 먼저 같은 번호를 썼을 가능성이 큽니다. `git merge origin/develop` 후 ' +
      '`node scripts/next-migration.mjs`로 번호를 확인하고 `git mv`로 내 파일 이름만 바꾸세요 (내용 수정 금지).',
  );
  process.exit(1);
}
console.log(`마이그레이션 번호 OK (${head.length}개, base ${base ? baseRef : '없음'})`);
