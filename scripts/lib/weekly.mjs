// 주간 /progress 초안과 하네스 지표 (H-1). GitHub·파일 호출 없이 계산하는 부분이다 (scripts/test/weekly.test.mjs).
//  - 진행현황 초안: spec 상태·체크리스트 비율, follow-up 이슈 영역별 집계
//  - 주간 지표: PR별 Codex 재리뷰 횟수·지적 우선순위, 가드 차단, 토큰(이 PC의 세션)
import { CODEX_BOT, requestCount } from './codex.mjs';
import { COMMON_AREA, STALE_DAYS } from './issues.mjs';

const DAY_MS = 86_400_000;

/** spec 머리 표의 상태와 구현 체크리스트 체크 수 */
export function specStatus(md) {
  const text = md || '';
  const m = /^\|\s*상태\s*\|\s*([A-Z]+)(?:\s*\(PR #(\d+)\))?/m.exec(text);
  const checked = (text.match(/^\s*- \[x\]/gim) || []).length;
  const total = checked + (text.match(/^\s*- \[ \]/gm) || []).length;
  return { status: m?.[1] ?? '?', pr: m?.[2] ? Number(m[2]) : null, checked, total };
}

/**
 * follow-up 이슈(--state all) 영역별 집계: 열림, 기간 안에 닫힘, STALE_DAYS일 넘게 열린 번호.
 * 영역 = area:A·area:B·area:common, 라벨이 없으면 '분류 필요'
 */
export function issueWeekStats(issues, since, now = Date.now()) {
  const rows = {};
  for (const i of issues || []) {
    const labels = (i.labels || []).map((l) => l.name);
    const area = labels.find((l) => /^area:/.test(l)) || '분류 필요';
    const r = (rows[area] ||= { open: 0, closed: 0, stale: [] });
    if (i.state === 'OPEN') {
      r.open++;
      if (now - Date.parse(i.createdAt) > STALE_DAYS * DAY_MS) r.stale.push(i.number);
    } else if (i.closedAt && Date.parse(i.closedAt) >= since) r.closed++;
  }
  for (const r of Object.values(rows)) r.stale.sort((a, b) => a - b);
  return rows;
}

export const AREA_ORDER = ['area:A', 'area:B', COMMON_AREA, '분류 필요'];

/** PR 하나의 Codex 지표: 재리뷰 요청 횟수와 인라인 지적(스레드 첫 댓글) 우선순위별 수 */
export function codexStats(issueComments, reviewComments) {
  const findings = {};
  for (const c of reviewComments || []) {
    if (!c.user?.login?.startsWith(CODEX_BOT) || c.in_reply_to_id) continue;
    const pri = /!\[(P\d) Badge\]/.exec(c.body || '')?.[1] ?? '?';
    findings[pri] = (findings[pri] || 0) + 1;
  }
  return { rounds: requestCount(issueComments || []), findings };
}

/** 가드 차단 기록(.harness/metrics/guard.jsonl 줄들) 중 기간 안의 것을 사유별로 */
export function guardStats(lines, since) {
  const byReason = new Map();
  let total = 0;
  for (const line of lines || []) {
    let o;
    try {
      o = JSON.parse(line);
    } catch {
      continue;
    }
    if (!o?.at || Date.parse(o.at) < since) continue;
    total++;
    const key = String(o.reason || '?').split(/\.(?:\s|$)/)[0]; // 첫 문장 (.env의 점에서 자르지 않게)
    byReason.set(key, (byReason.get(key) || 0) + 1);
  }
  return { total, byReason: [...byReason].sort((a, b) => b[1] - a[1]) };
}
