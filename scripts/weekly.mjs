#!/usr/bin/env node
// 주간 /progress 초안과 하네스 지표 (H-1). /progress가 진행현황을 쓰기 전에 사실을 한 번에 모은다.
//   spec 상태·체크리스트 비율, follow-up 이슈 영역별 집계, 기간 안에 머지된 PR과 PR별 Codex 재리뷰·지적,
//   가드 차단(이 PC의 .harness/metrics/guard.jsonl), 토큰(이 PC의 Claude Code 세션)
// 가드·토큰은 PC마다 따로 쌓이므로 실행한 PC의 값이다. 두 사람 값을 합치려면 각자 실행해 더한다.
//
// 사용법: node scripts/weekly.mjs [--days 7]
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, statSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { ROOT, nowStamp, readText, transcriptUsage, usageSummary } from './lib/common.mjs';
import { AREA_ORDER, codexStats, guardStats, issueWeekStats, specStatus } from './lib/weekly.mjs';

const args = process.argv.slice(2);
const days = Number(args[args.indexOf('--days') + 1]) || 7;
const now = Date.now();
const since = now - days * 86_400_000;
const sinceDay = nowStamp(new Date(since)).slice(0, 10);

const gh = (ghArgs) => {
  const r = spawnSync('gh', ghArgs, { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout: 30_000, maxBuffer: 64 * 1024 * 1024 });
  if (r.status !== 0) return null;
  try {
    return JSON.parse(r.stdout);
  } catch {
    return null;
  }
};
const ghList = (path) => (gh(['api', '--paginate', '--slurp', `${path}?per_page=100`]) || []).flat();
const out = [`# 주간 초안 (${sinceDay} ~ ${nowStamp(new Date(now))}, ${days}일)`];

// ── spec ──
const specDir = join(ROOT, 'docs', 'specs');
const specs = readdirSync(specDir)
  .filter((f) => /^F-.*\.md$/.test(f))
  .sort()
  .map((f) => ({ id: f.replace(/^(F-\d+[a-z]?).*$/, '$1'), ...specStatus(readText(join(specDir, f))) }));
const byStatus = specs.reduce((m, s) => ({ ...m, [s.status]: [...(m[s.status] || []), s] }), {});
out.push('', `## spec (${specs.length}개)`, '| 상태 | 수 | spec (체크 비율) |', '|---|---|---|');
for (const [status, list] of Object.entries(byStatus)) {
  out.push(`| ${status} | ${list.length} | ${list.map((s) => `${s.id}${status === 'DONE' ? '' : ` (${s.checked}/${s.total})`}`).join(', ')} |`);
}

// ── follow-up 이슈 ──
const issues = gh(['issue', 'list', '--label', 'follow-up', '--state', 'all', '--limit', '200', '--json', 'number,state,labels,createdAt,closedAt']);
out.push('', '## follow-up 이슈');
if (!issues) out.push('⚠️ 이슈 조회 실패 (gh issue list --label follow-up)');
else {
  const rows = issueWeekStats(issues, since, now);
  out.push(`| 영역 | 열림 | ${days}일 안 닫힘 | 7일 넘음 |`, '|---|---|---|---|');
  for (const area of AREA_ORDER.filter((a) => rows[a])) {
    const r = rows[area];
    out.push(`| ${area} | ${r.open} | ${r.closed} | ${r.stale.length ? `${r.stale.length} (${r.stale.map((n) => `#${n}`).join(', ')})` : '0'} |`);
  }
}

// ── 머지된 PR과 Codex ──
const repo = gh(['repo', 'view', '--json', 'nameWithOwner'])?.nameWithOwner;
const prs = gh(['pr', 'list', '--state', 'merged', '--limit', '100', '--search', `merged:>=${sinceDay}`, '--json', 'number,title,mergedAt,author']) || [];
out.push('', `## 머지된 PR (${prs.length}개)`);
if (prs.length && repo) {
  out.push('| PR | 작성자 | 머지 | Codex 재리뷰 | 지적 |', '|---|---|---|---|---|');
  let rounds = 0;
  const totals = {};
  for (const p of prs.sort((a, b) => a.number - b.number)) {
    const s = codexStats(ghList(`repos/${repo}/issues/${p.number}/comments`), ghList(`repos/${repo}/pulls/${p.number}/comments`));
    rounds += s.rounds;
    for (const [k, v] of Object.entries(s.findings)) totals[k] = (totals[k] || 0) + v;
    const found = Object.entries(s.findings).map(([k, v]) => `${k} ${v}`).join(' · ') || '-';
    out.push(`| #${p.number} ${p.title.slice(0, 50)} | ${p.author?.login || '?'} | ${p.mergedAt.slice(5, 10)} | ${s.rounds} | ${found} |`);
  }
  const all = Object.entries(totals).map(([k, v]) => `${k} ${v}`).join(' · ') || '없음';
  out.push('', `Codex: PR당 재리뷰 평균 ${(rounds / prs.length).toFixed(1)}회 (합 ${rounds}) · 지적 ${all}`);
}

// ── 하네스 지표 (이 PC) ──
out.push('', '## 하네스 지표 (이 PC)');
const g = guardStats((readText(join(ROOT, '.harness', 'metrics', 'guard.jsonl')) || '').split('\n'), since);
out.push(`- 가드 차단 ${g.total}회${g.byReason.length ? `: ${g.byReason.map(([r, n]) => `${r} ${n}`).join(' · ')}` : ''}`);

const projectDir = join(homedir(), '.claude', 'projects', ROOT.replace(/[^A-Za-z0-9]/g, '-'));
if (existsSync(projectDir)) {
  const sessions = readdirSync(projectDir)
    .filter((f) => f.endsWith('.jsonl'))
    .map((f) => join(projectDir, f))
    .filter((p) => statSync(p).mtimeMs >= since);
  let weighted = 0;
  let heavy = 0;
  for (const path of sessions) {
    const main = usageSummary(transcriptUsage(path));
    weighted += main.weighted;
    if (main.avgCtx > 150_000) heavy++;
    const subDir = path.replace(/\.jsonl$/, '') + '/subagents';
    if (existsSync(subDir)) {
      for (const f of readdirSync(subDir).filter((x) => x.endsWith('.jsonl'))) weighted += usageSummary(transcriptUsage(join(subDir, f))).weighted;
    }
  }
  const m = (n) => `${(n / 1_000_000).toFixed(1)}M`;
  out.push(`- 토큰: 세션 ${sessions.length}개 · 입력 환산 ${m(weighted)} (서브에이전트 포함) · 메인 평균 컨텍스트 150k 넘은 세션 ${heavy}개`);
} else out.push('- 토큰: transcript 폴더 없음');

console.log(out.join('\n'));
