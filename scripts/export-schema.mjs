#!/usr/bin/env node
// 제출물 DB Schema.sql 추출 (HP-5 HX-10): Flyway V*.sql → mysql:8.4 임시 컨테이너 → mysqldump --no-data 정리본 → docs/산출물/Schema.sql.
// 만든 파일을 빈 DB에 다시 적용해 SHOW CREATE TABLE이 같을 때만 쓴다. 실패하면 기존 파일을 그대로 두고 종료 코드 1.
// 주간 /progress docs PR과 제출 전에 실행한다 (Docker 필요, 처음에는 이미지 받기 포함 1분 안팎).
//   node scripts/export-schema.mjs
import { spawnSync } from 'node:child_process';
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, readText } from './lib/common.mjs';
import { MIGRATION_DIR } from './lib/migrations.mjs';
import { exportSchema, orderMigrations } from './lib/schema-export.mjs';

const OUT = join(ROOT, 'docs', '산출물', 'Schema.sql');
const dir = join(ROOT, MIGRATION_DIR);

const docker = (args, input) => {
  const r = spawnSync('docker', args, { input, encoding: 'utf8', timeout: 300_000, maxBuffer: 64 * 1024 * 1024, windowsHide: true });
  return { ok: r.status === 0, out: r.stdout ?? '', err: r.stderr || r.error?.message || '' };
};

const migrations = orderMigrations(readdirSync(dir)).map((name) => ({ name, sql: readFileSync(join(dir, name), 'utf8') }));
console.log(`V 파일 ${migrations.length}개를 임시 MySQL 컨테이너에 적용합니다…`);
const r = exportSchema({ docker, migrations });
if (!r.ok) {
  console.error(`❌ ${r.error}\n   docs/산출물/Schema.sql은 바꾸지 않았습니다.`);
  process.exit(1);
}
const changed = readText(OUT) !== r.sql;
if (changed) writeFileSync(OUT, r.sql, 'utf8');
console.log(`${changed ? '✅ 다시 만들었습니다' : '변경 없음'}: docs/산출물/Schema.sql (테이블 ${r.tables}개, ${r.lastVersion}까지, 빈 DB 재적용 검증 통과)`);
