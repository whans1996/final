#!/usr/bin/env node
// 산출물 07 REST API 명세 생성 (HP-5 HX-10): 정본 docs/api/openapi.json → docs/산출물/07-REST-API-명세.md (통째로 다시 쓴다).
// 주간 /progress docs PR과 제출 전에 실행한다. CI는 최신 여부를 검사하지 않는다 (11-27 결정).
//   node scripts/export-api-doc.mjs [--in <openapi.json>] [--out <md>]   (--in·--out은 테스트용)
import { writeFileSync } from 'node:fs';
import { relative, resolve } from 'node:path';
import { renderApiDoc } from './lib/api-doc.mjs';
import { ROOT, readText } from './lib/common.mjs';

const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : fallback;
};
const input = resolve(ROOT, opt('--in', 'docs/api/openapi.json'));
const out = resolve(ROOT, opt('--out', 'docs/산출물/07-REST-API-명세.md'));

const text = readText(input);
if (text === null) {
  console.error(`❌ openapi.json이 없습니다: ${input} (BE 테스트 OpenApiExportTest가 만든다)`);
  process.exit(1);
}
let api;
try {
  api = JSON.parse(text);
} catch (e) {
  console.error(`❌ openapi.json을 JSON으로 읽을 수 없습니다: ${e.message}`);
  process.exit(1);
}
const md = renderApiDoc(api);
const changed = readText(out) !== md;
if (changed) writeFileSync(out, md, 'utf8');
console.log(`${changed ? '✅ 다시 만들었습니다' : '변경 없음'}: ${relative(ROOT, out).replace(/\\/g, '/')}`);
