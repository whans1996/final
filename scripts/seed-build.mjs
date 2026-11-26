#!/usr/bin/env node
// 시드 카드 원본(data/seed/concepts/**.md)으로 R__seed_concepts.sql을 만든다 (K-3a AC1·AC3).
//
// 사용법: node scripts/seed-build.mjs [--check] [--src <카드 폴더>] [--out <SQL 파일>]
//   (기본)   카드 파일을 읽어 SQL을 다시 쓴다
//   --check  쓰지 않고 비교만 한다. 다르면 종료 코드 1 (CI 하네스 잡)
// 형식·예제 검사는 seed-check.mjs가 한다. 여기서는 읽을 수 없는 카드(구조 문제)만 막는다.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { ROOT } from './lib/common.mjs';
import { buildSql, readCards } from './lib/seed/card.mjs';

const DEFAULT_SRC = 'data/seed/concepts';
const DEFAULT_OUT = 'backend/src/main/resources/db/migration/R__seed_concepts.sql';

const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
};
const check = args.includes('--check');
const src = resolve(ROOT, opt('--src', DEFAULT_SRC));
const out = resolve(ROOT, opt('--out', DEFAULT_OUT));
const rel = (p) => relative(ROOT, p).replace(/\\/g, '/') || '.';

const cards = readCards(src, ROOT);
if (!cards.length) {
  console.error(`❌ 카드 파일이 없음: ${rel(src)}`);
  process.exit(1);
}
const broken = cards.filter((c) => c.problems.length);
if (broken.length) {
  for (const c of broken) for (const p of c.problems) console.error(`❌ ${c.file} · ${p.item}: ${p.message}`);
  console.error('카드를 읽을 수 없어 생성하지 않음 (node scripts/seed-check.mjs로 전체 형식 확인)');
  process.exit(1);
}

const sql = buildSql(cards);
if (check) {
  const current = existsSync(out) ? readFileSync(out, 'utf8').replace(/\r\n/g, '\n') : null;
  if (current !== sql) {
    console.error(`❌ 생성본이 원본과 다름: ${rel(out)}`);
    console.error(`   카드 파일(${rel(src)})을 고친 뒤 다시 생성하지 않았거나 SQL을 손으로 고쳤습니다. node scripts/seed-build.mjs로 다시 생성하세요`);
    process.exit(1);
  }
  console.log(`✅ 생성본이 원본과 같음 (카드 ${cards.length}장 → ${rel(out)})`);
} else {
  mkdirSync(dirname(out), { recursive: true });
  writeFileSync(out, sql, 'utf8');
  console.log(`✅ 카드 ${cards.length}장 → ${rel(out)}`);
}
