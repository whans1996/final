#!/usr/bin/env node
// 작업로그 주차별 보관 (HP-5 HX-9): 지금 주보다 앞선 주의 항목을 docs/기록/작업로그-<주>.md로 옮긴다.
// 주간 /progress docs PR에서 실행한다. 항목 추가(stamp --append-log, handoff.mjs)는 계속 docs/작업로그.md 끝에 한다.
//   node scripts/archive-worklog.mjs [--dry-run] [--today YYYY-MM-DD] [--docs <docs 폴더>]
//   --dry-run  옮길 주·항목 수만 출력 (파일을 바꾸지 않는다)
//   --today    기준일 (기본: 오늘). 제출 전 정리·테스트용
//   --docs     작업로그.md와 기록/이 있는 폴더 (기본: docs, 테스트용)
import { mkdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { ROOT, nowStamp, readText, worklogArchiveName } from './lib/common.mjs';
import { isDate, mergeArchive, splitByWeek } from './lib/worklog-archive.mjs';

const args = process.argv.slice(2);
const opt = (name) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : null;
};
const fail = (msg) => {
  console.error(`❌ ${msg}`);
  process.exit(1);
};

const today = opt('--today') ?? nowStamp().slice(0, 10);
if (!isDate(today)) fail(`--today는 YYYY-MM-DD 형식이어야 합니다: ${today}`);
const docs = resolve(ROOT, opt('--docs') ?? 'docs');
const logFile = join(docs, '작업로그.md');
const archiveDir = join(docs, '기록');
const dryRun = args.includes('--dry-run');

const text = readText(logFile);
if (text === null) fail(`작업로그를 읽을 수 없습니다: ${logFile}`);

const { log, moved, warnings } = splitByWeek(text, today);
for (const w of warnings) console.warn(`⚠️ ${w}`);
if (!moved.size) {
  console.log(`보관할 항목 없음 (기준일 ${today})`);
  process.exit(0);
}

const plans = [...moved].map(([week, entries]) => {
  const file = join(archiveDir, worklogArchiveName(week));
  const existing = readText(file);
  return { week, file, existing, ...mergeArchive(existing, week, entries), count: entries.length };
});
for (const p of plans) {
  const dup = p.duplicates ? ` (이미 보관된 ${p.duplicates}개는 작업로그에서만 지움)` : '';
  console.log(`${dryRun ? '[dry-run] ' : ''}${p.week}: ${p.count}개 → docs/기록/${worklogArchiveName(p.week)}에 ${p.added}개 추가${dup}`);
}
if (dryRun) process.exit(0);

// 보관 파일을 먼저 쓴다: 중간에 실패해도 항목이 사라지지 않고, 다시 실행하면 중복으로 걸러진다 (AC3)
mkdirSync(archiveDir, { recursive: true });
for (const p of plans) if (p.text !== p.existing) writeFileSync(p.file, p.text, 'utf8');
writeFileSync(logFile, log, 'utf8');
console.log(`✅ 작업로그에서 ${plans.reduce((n, p) => n + p.count, 0)}개 항목을 옮겼습니다 (기준일 ${today})`);
