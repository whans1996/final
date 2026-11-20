// follow-up 이슈 순환: 만든 이슈가 세션 시작·/pickup·/progress에 다시 보이고, PR이 develop에 머지되면 닫히게 한다.
// 영역은 area:A(김동환)·area:B(김준섭)·area:common 라벨로 나눈다 (docs/conventions.md 9절).
import { execFile } from 'node:child_process';
import { ROOT } from './common.mjs';

export const AREA_OF = { dh: 'area:A', js: 'area:B' };
export const COMMON_AREA = 'area:common';
/** 누군가의 작업(커밋·머지·다음 기능)을 막는 이슈. 영역과 무관하게 두 사람 모두에게 먼저 보인다 (#67) */
export const BLOCKER = 'blocker';
export const STALE_DAYS = 7;
const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * 열린 follow-up 이슈 목록 → { ok: true, issues } | { ok: false, reason }.
 * 세션 시작을 막지 않도록 비동기·제한 시간. 실패를 조용히 삼키지 않고 이유를 돌려준다 (#67: 이슈 16개가 경고 없이 빠졌다).
 * 주의: 띄운 뒤 동기 작업으로 이벤트 루프를 막으면 제한 시간 타이머가 먼저 터져 출력이 버려진다 → 동기 git이 끝난 뒤 부른다.
 */
export function followUpIssues({ timeout = 4000, exec = execFile } = {}) {
  return new Promise((resolve) => {
    exec(
      'gh',
      ['issue', 'list', '--label', 'follow-up', '--state', 'open', '--limit', '50', '--json', 'number,title,labels,assignees,createdAt'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout },
      (err, stdout, stderr) => {
        if (err?.code === 'ENOENT') return resolve({ ok: false, reason: 'gh 없음' });
        if (err?.killed) return resolve({ ok: false, reason: `${timeout / 1000}초 안에 응답 없음` });
        if (err) {
          const reason = `gh 실패: ${String(stderr || err.message).trim().split('\n')[0]}`;
          return resolve({ ok: false, reason: reason.length > 80 ? `${reason.slice(0, 79)}…` : reason });
        }
        if (!String(stdout ?? '').trim()) return resolve({ ok: false, reason: '빈 응답' });
        try {
          const issues = JSON.parse(stdout);
          resolve(Array.isArray(issues) ? { ok: true, issues } : { ok: false, reason: '응답 형식 오류' });
        } catch {
          resolve({ ok: false, reason: '응답 형식 오류' });
        }
      },
    );
  });
}

/** 이슈 조회 실패 줄 (배너·모델 컨텍스트 공용). 빠진 줄과 "이슈 없음"을 구분하려고 실패하면 항상 낸다 */
export function issueFailureLine(reason) {
  return `⚠️ 이슈 조회 실패 (${reason}) → gh issue list --label follow-up`;
}

/**
 * 이슈를 차단·내 것·공통·분류 필요로 나눈다 (상대 영역은 차단이 아니면 세지 않는다).
 * 차단 = blocker 라벨(영역 무관, 다른 그룹에서 뺀다), 공통 = area:common, 내 것 = (공통이 아닌 것 중) 내 area 라벨이거나 담당자가 나.
 * stale = 차단·내 것·공통 중 STALE_DAYS일 넘게 열린 것.
 */
export function summarizeIssues(issues, { key, github } = {}, now = Date.now()) {
  const blockers = [];
  const mine = [];
  const common = [];
  const untriaged = [];
  const myArea = AREA_OF[key];
  for (const i of issues ?? []) {
    const labels = (i.labels ?? []).map((l) => l.name);
    const assignees = (i.assignees ?? []).map((a) => a.login);
    if (labels.includes(BLOCKER)) blockers.push(i);
    // 공통은 담당자가 나여도 공통으로 센다 (공통 이슈 담당은 만든 사람이라 대부분 나)
    else if (labels.includes(COMMON_AREA)) common.push(i);
    else if ((myArea && labels.includes(myArea)) || (github && assignees.includes(github))) mine.push(i);
    else if (!labels.some((l) => l.startsWith('area:'))) untriaged.push(i);
  }
  const byNumber = (a, b) => a.number - b.number;
  const stale = [...blockers, ...mine, ...common].filter((i) => now - Date.parse(i.createdAt) > STALE_DAYS * DAY_MS).sort(byNumber);
  return { blockers: blockers.sort(byNumber), mine: mine.sort(byNumber), common: common.sort(byNumber), untriaged: untriaged.sort(byNumber), stale };
}

/** 세션 시작 배너 한 줄. 차단 이슈가 있으면 🔴로 시작하고 첫 이슈로 보여 준다. 볼 이슈가 없으면 null */
export function issueBannerLine({ blockers = [], mine, common, untriaged, stale }) {
  const all = [...blockers, ...mine, ...common, ...untriaged];
  if (!all.length) return null;
  const counts = [`내 ${mine.length}`, `공통 ${common.length}`];
  if (blockers.length) counts.unshift(`차단 ${blockers.length}`);
  if (untriaged.length) counts.push(`분류 필요 ${untriaged.length}`);
  if (stale.length) counts.push(`${STALE_DAYS}일 넘음 ${stale.length}`);
  const first = all[0];
  return `${blockers.length ? '🔴' : '📌'} 이슈: ${counts.join(' · ')} (#${first.number} ${first.title.length > 40 ? `${first.title.slice(0, 39)}…` : first.title}${all.length > 1 ? ' 외' : ''})`;
}

/** 모델에 주는 이슈 목록 (그룹별 최대 limit개) */
export function issueContext({ blockers = [], mine, common, untriaged }, limit = 10) {
  const list = (items) => items.slice(0, limit).map((i) => `  - #${i.number} ${i.title}`).join('\n') + (items.length > limit ? `\n  - … 외 ${items.length - limit}개` : '');
  const groups = [
    ['🔴 차단 (blocker: 작업 전에 멈추고 사용자에게 해결·우회 여부를 묻는다)', blockers],
    ['내 영역', mine],
    ['공통', common],
    ['분류 필요 (area 라벨 없음 → 라벨·담당자 지정 제안)', untriaged],
  ].filter(([, items]) => items.length);
  if (!groups.length) return null;
  return groups.map(([name, items]) => `- ${name} ${items.length}개\n${list(items)}`).join('\n');
}

/** PR 본문에서 닫을 이슈 번호 (GitHub 키워드: close·closes·closed·fix·fixes·fixed·resolve·resolves·resolved) */
export function closingRefs(body) {
  const refs = new Set();
  // 키워드와 번호는 같은 줄에 있어야 한다 (줄 끝 "fix" + 다음 줄 "#5" 오탐 방지)
  for (const m of String(body ?? '').matchAll(/\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)[ \t]*:?[ \t]+#(\d+)(?![\w-])/gi)) refs.add(Number(m[1]));
  return [...refs].sort((a, b) => a - b);
}
