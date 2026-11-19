// follow-up 이슈 순환: 만든 이슈가 세션 시작·/pickup·/progress에 다시 보이고, PR이 develop에 머지되면 닫히게 한다.
// 영역은 area:A(김동환)·area:B(김준섭)·area:common 라벨로 나눈다 (docs/conventions.md 9절).
import { execFile } from 'node:child_process';
import { ROOT } from './common.mjs';

export const AREA_OF = { dh: 'area:A', js: 'area:B' };
export const COMMON_AREA = 'area:common';
export const STALE_DAYS = 7;
const DAY_MS = 24 * 60 * 60 * 1000;

/** 열린 follow-up 이슈 목록. gh가 없거나 느리면 null (세션 시작을 막지 않도록 비동기·제한 시간) */
export function followUpIssues({ timeout = 3000 } = {}) {
  return new Promise((resolve) => {
    execFile(
      'gh',
      ['issue', 'list', '--label', 'follow-up', '--state', 'open', '--limit', '50', '--json', 'number,title,labels,assignees,createdAt'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout },
      (err, stdout) => {
        if (err) return resolve(null);
        try {
          const issues = JSON.parse(stdout);
          resolve(Array.isArray(issues) ? issues : null);
        } catch {
          resolve(null);
        }
      },
    );
  });
}

/**
 * 이슈를 내 것·공통·분류 필요로 나눈다 (상대 영역은 세지 않는다).
 * 공통 = area:common, 내 것 = (공통이 아닌 것 중) 내 area 라벨이거나 담당자가 나. stale = 그중 STALE_DAYS일 넘게 열린 것.
 */
export function summarizeIssues(issues, { key, github } = {}, now = Date.now()) {
  const mine = [];
  const common = [];
  const untriaged = [];
  const myArea = AREA_OF[key];
  for (const i of issues ?? []) {
    const labels = (i.labels ?? []).map((l) => l.name);
    const assignees = (i.assignees ?? []).map((a) => a.login);
    // 공통은 담당자가 나여도 공통으로 센다 (공통 이슈 담당은 만든 사람이라 대부분 나)
    if (labels.includes(COMMON_AREA)) common.push(i);
    else if ((myArea && labels.includes(myArea)) || (github && assignees.includes(github))) mine.push(i);
    else if (!labels.some((l) => l.startsWith('area:'))) untriaged.push(i);
  }
  const byNumber = (a, b) => a.number - b.number;
  const stale = [...mine, ...common].filter((i) => now - Date.parse(i.createdAt) > STALE_DAYS * DAY_MS).sort(byNumber);
  return { mine: mine.sort(byNumber), common: common.sort(byNumber), untriaged: untriaged.sort(byNumber), stale };
}

/** 세션 시작 배너 한 줄. 볼 이슈가 없으면 null */
export function issueBannerLine({ mine, common, untriaged, stale }) {
  if (!mine.length && !common.length && !untriaged.length) return null;
  const counts = [`내 ${mine.length}`, `공통 ${common.length}`];
  if (untriaged.length) counts.push(`분류 필요 ${untriaged.length}`);
  if (stale.length) counts.push(`${STALE_DAYS}일 넘음 ${stale.length}`);
  const first = [...mine, ...common, ...untriaged][0];
  return `📌 이슈: ${counts.join(' · ')} (#${first.number} ${first.title.length > 40 ? `${first.title.slice(0, 39)}…` : first.title}${mine.length + common.length + untriaged.length > 1 ? ' 외' : ''})`;
}

/** 모델에 주는 이슈 목록 (그룹별 최대 limit개) */
export function issueContext({ mine, common, untriaged }, limit = 10) {
  const list = (items) => items.slice(0, limit).map((i) => `  - #${i.number} ${i.title}`).join('\n') + (items.length > limit ? `\n  - … 외 ${items.length - limit}개` : '');
  const groups = [
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
