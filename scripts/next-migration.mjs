#!/usr/bin/env node
// 새 Flyway 마이그레이션의 다음 번호를 알려 준다. develop, 원격 feature 브랜치(상대 작업), 내 작업 트리를 모두 본다.
// 상대 PR이 이미 쓰고 있는 번호를 피하려는 것이다. 그래도 겹치면 늦게 머지되는 쪽이 develop을 합친 뒤
// git mv로 번호만 바꾼다 (CI check-migrations.mjs가 잡는다).
//
// 사용법: node scripts/next-migration.mjs   (네트워크가 없으면 로컬 정보만 쓴다)
import { readdirSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, currentBranch, git } from './lib/common.mjs';
import { MIGRATION_DIR, compareVersions, migrationFilesAt, versionOf, versionText } from './lib/migrations.mjs';

git(['fetch', '--quiet', '--prune', 'origin'], { timeout: 15_000 });

/** { file, version, where } */
const seen = [];
const add = (files, where) => {
  for (const f of files || []) {
    const v = versionOf(f);
    if (v) seen.push({ file: f, version: v, where });
  }
};

add(migrationFilesAt('origin/develop'), 'develop');

let local = [];
try {
  local = readdirSync(join(ROOT, MIGRATION_DIR));
} catch {
  // backend 없음
}
add(local, `작업 트리(${currentBranch() || 'detached'})`);

// 원격 feature 브랜치 전부 (PR을 아직 안 연 상대 작업도 포함). 위 fetch로 받은 origin/* 기준
const branches = git(['for-each-ref', '--format=%(refname:strip=3)', 'refs/remotes/origin/feature/'])
  .out.split('\n')
  .filter((b) => b && b !== currentBranch());
for (const b of branches) add(migrationFilesAt(`origin/${b}`), b);

// 같은 파일이 여러 곳에 있으면 한 번만 (develop에 머지된 파일은 develop으로 표시)
const unique = new Map();
for (const s of seen) if (!unique.has(s.file)) unique.set(s.file, s);
const all = [...unique.values()].sort((a, b) => compareVersions(a.version, b.version));

if (!all.length) {
  console.log('마이그레이션 파일이 없습니다. 다음 번호: V1');
  process.exit(0);
}
const max = all.at(-1).version;
const next = max.length === 1 ? `V${max[0] + 1}` : `V${max[0] + 1} (최신이 V${versionText(max)}라 정수 다음 번호를 권장)`;

console.log(`다음 번호: ${next}__설명.sql`);
console.log('최근 버전:');
for (const s of all.slice(-5)) console.log(`  V${versionText(s.version).padEnd(4)} ${s.file}  ← ${s.where}`);
const dup = all.filter((s, i) => all.some((o, j) => j !== i && compareVersions(o.version, s.version) === 0));
if (dup.length) console.log(`⚠️  번호가 겹친 파일: ${dup.map((s) => `${s.file}(${s.where})`).join(', ')}`);
