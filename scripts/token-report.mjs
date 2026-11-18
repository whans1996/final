#!/usr/bin/env node
// 세션별 토큰 사용량 집계: 메인 에이전트와 서브에이전트(종류·모델별)의 컨텍스트 크기, cache read·write, output.
// 컨텍스트·모델·effort 운영 규칙(CLAUDE.md "세션 운영")이 효과가 있는지 확인하는 용도다.
//
// 사용법: node scripts/token-report.mjs [latest|<세션 ID 앞부분>] [--list]
//   latest   이 프로젝트에서 가장 최근 세션 (기본값)
//   --list   최근 세션 10개의 요약만
// 입력 환산 비중: 캐시 쓰기 1.25배, 캐시 읽기 0.1배, 출력 5배로 가중한다 (모델 간 단가 차이는 무시한 대략치).
import { existsSync, readdirSync, statSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { ROOT, nowStamp, readText, transcriptUsage } from './lib/common.mjs';

// Claude Code는 프로젝트 경로의 영문·숫자 외 문자를 '-'로 바꾼 폴더에 transcript를 둔다
const projectDir = join(homedir(), '.claude', 'projects', ROOT.replace(/[^A-Za-z0-9]/g, '-'));
if (!existsSync(projectDir)) {
  console.error(`transcript 폴더가 없습니다: ${projectDir}`);
  process.exit(1);
}

const sessions = readdirSync(projectDir)
  .filter((f) => f.endsWith('.jsonl'))
  .map((f) => ({ id: f.replace(/\.jsonl$/, ''), path: join(projectDir, f), mtime: statSync(join(projectDir, f)).mtimeMs }))
  .sort((a, b) => b.mtime - a.mtime);

const k = (n) => `${Math.round(n / 1000).toLocaleString()}k`;

function summarize(turns) {
  const s = { turns: turns.length, input: 0, cacheWrite: 0, cacheRead: 0, output: 0, maxCtx: 0, models: new Set() };
  for (const t of turns) {
    s.input += t.input;
    s.cacheWrite += t.cacheWrite;
    s.cacheRead += t.cacheRead;
    s.output += t.output;
    s.maxCtx = Math.max(s.maxCtx, t.ctx);
    if (t.model) s.models.add(t.model.replace(/^claude-/, ''));
  }
  s.avgCtx = s.turns ? (s.input + s.cacheWrite + s.cacheRead) / s.turns : 0;
  s.weighted = s.input + s.cacheWrite * 1.25 + s.cacheRead * 0.1 + s.output * 5;
  return s;
}

const args = process.argv.slice(2);
if (args.includes('--list')) {
  for (const s of sessions.slice(0, 10)) {
    const m = summarize(transcriptUsage(s.path));
    const date = nowStamp(new Date(s.mtime)).slice(5);
    console.log(`${s.id.slice(0, 8)}  ${date}  턴 ${String(m.turns).padStart(3)}  평균 ${k(m.avgCtx).padStart(5)}  최대 ${k(m.maxCtx).padStart(5)}`);
  }
  process.exit(0);
}

const want = args.find((a) => !a.startsWith('--')) || 'latest';
const session = want === 'latest' ? sessions[0] : sessions.find((s) => s.id.startsWith(want));
if (!session) {
  console.error(`세션을 찾지 못했습니다: ${want} (node scripts/token-report.mjs --list)`);
  process.exit(1);
}

const rows = [['메인', summarize(transcriptUsage(session.path))]];
const subDir = join(projectDir, session.id, 'subagents');
if (existsSync(subDir)) {
  const groups = new Map();
  for (const f of readdirSync(subDir).filter((x) => x.endsWith('.jsonl'))) {
    let type = '?';
    try {
      type = JSON.parse(readText(join(subDir, f.replace(/\.jsonl$/, '.meta.json'))) || '{}').agentType || '?';
    } catch {
      // meta 없음
    }
    const g = groups.get(type) || { runs: 0, turns: [] };
    g.runs += 1;
    g.turns.push(...transcriptUsage(join(subDir, f)));
    groups.set(type, g);
  }
  for (const [type, g] of groups) rows.push([`${type} ×${g.runs}`, summarize(g.turns)]);
}

const total = rows.reduce((sum, [, s]) => sum + s.weighted, 0) || 1;
console.log(`세션 ${session.id} (마지막 기록 ${nowStamp(new Date(session.mtime))})`);
console.log('주체                  모델                 턴   평균ctx  최대ctx  cacheRead  cacheWrite  output  비중');
for (const [name, s] of rows.sort((a, b) => b[1].weighted - a[1].weighted)) {
  console.log(
    [
      name.padEnd(20),
      [...s.models].join(',').padEnd(20),
      String(s.turns).padStart(4),
      k(s.avgCtx).padStart(8),
      k(s.maxCtx).padStart(8),
      k(s.cacheRead).padStart(10),
      k(s.cacheWrite).padStart(11),
      k(s.output).padStart(7),
      `${Math.round((s.weighted / total) * 100)}%`.padStart(5),
    ].join(' '),
  );
}
const main = rows.find(([n]) => n === '메인')[1];
if (main.avgCtx > 150_000) {
  console.log(`\n⚠️ 메인 평균 컨텍스트 ${k(main.avgCtx)}: 작업 단위마다 /clear하면 크게 줄어듭니다 (CLAUDE.md "세션 운영").`);
}
