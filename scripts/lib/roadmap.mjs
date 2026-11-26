// 로드맵 R-1 (docs/기획서.md 7.7절): docs/로드맵.md의 표(항목·넘겨주는 시점·공통 일정)를 읽고,
// 상태(⬜ 🔨 ✅)는 파일에 쓰지 않고 PR 본문의 `Roadmap: <ID>` 줄과 원격 브랜치 이름으로 계산한다.
// develop은 브랜치 보호로 워크플로가 커밋할 수 없고, 상태를 파일에 쓰면 두 사람이 같은 줄을 고치다 충돌한다.
// 보이는 곳: 세션 시작 배너(bannerLines)·모델 컨텍스트(roadmapBlock), `node scripts/roadmap.mjs`(/pickup·/handoff), Mattermost(roadmap.yml).
import { execFile } from 'node:child_process';
import { join } from 'node:path';
import { ROOT, currentBranch, git, mdSection } from './common.mjs';

export const ROADMAP_FILE = join(ROOT, 'docs', '로드맵.md');
export const ROADMAP_REL = 'docs/로드맵.md';
/** 넘겨주는 시점을 배너에 D-n으로 보이기 시작하는 날 수 */
export const HANDOFF_NOTICE_DAYS = 2;
const OWNER_OF = { A: 'dh', B: 'js' };
const LABEL_OF = { dh: 'A', js: 'B' };
const ICON = { done: '✅', doing: '🔨', todo: '⬜', unknown: '❔' };
const DAY_MS = 24 * 60 * 60 * 1000;
/** 항목 ID 형식. 커밋·PR 제목 범위(scripts/lib/commit-msg.mjs SCOPE, ci.yml pr-title)도 이 형식을 받는다 */
export const ROADMAP_ID = '[A-Z]{1,4}-\\d{1,2}[a-z0-9]{0,2}';
const ID_RE = new RegExp(`^${ROADMAP_ID}$`);

/** 서울 날짜 YYYY-MM-DD (Actions는 UTC라 로컬 시간대를 쓰지 않는다) */
export function seoulToday(d = new Date()) {
  return new Date(d.getTime() + 9 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

/** a → b 날짜 차이 (일, YYYY-MM-DD) */
export function daysBetween(a, b) {
  return Math.round((Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`)) / DAY_MS);
}

/** YYYY-MM-DD → MM/DD */
export const shortDate = (d) => (d ? `${d.slice(5, 7)}/${d.slice(8, 10)}` : '?');

/** 마크다운 표 → 머리 줄을 뺀 셀 배열 목록 (구분 줄 제외) */
function tableRows(section) {
  const rows = section
    .split(/\r?\n/)
    .filter((l) => l.trim().startsWith('|'))
    .map((l) =>
      l
        .trim()
        .replace(/^\||\|$/g, '')
        .split('|')
        .map((c) => c.trim()),
    );
  return rows.slice(1).filter((cells) => !cells.every((c) => /^:?-+:?$/.test(c)));
}

/** 'A' → ['dh'], 'A·B' → ['dh', 'js'], 모르는 값 → null */
function ownersOf(cell) {
  const keys = cell.split(/[·,\s]+/).filter(Boolean).map((p) => OWNER_OF[p]);
  return keys.length && keys.every(Boolean) ? keys : null;
}

/**
 * docs/로드맵.md 본문 → { year, items, handoffs, events, errors }. 순수 함수.
 * items: { id, owners, owner(원문), track, start, end, deps, task, model } (날짜는 YYYY-MM-DD)
 * errors: 형식 오류 설명 (빈 배열이면 정상). 하네스 테스트가 실제 파일을 이것으로 검사한다
 */
export function parseRoadmap(md) {
  const errors = [];
  const year = Number(/^-\s*연도:\s*(\d{4})/m.exec(md ?? '')?.[1]);
  if (!year) errors.push('연도 줄(- 연도: 2026)이 없음');
  const date = (cell, where) => {
    const m = /^(\d{1,2})\/(\d{1,2})$/.exec(cell);
    if (!m || !year) {
      errors.push(`${where}: 날짜 형식 오류 "${cell}" (MM/DD)`);
      return null;
    }
    const iso = `${year}-${m[1].padStart(2, '0')}-${m[2].padStart(2, '0')}`;
    const t = Date.parse(`${iso}T00:00:00Z`);
    if (Number.isNaN(t) || new Date(t).toISOString().slice(0, 10) !== iso) errors.push(`${where}: 없는 날짜 "${cell}"`);
    return iso;
  };

  const items = [];
  for (const cells of tableRows(mdSection(md, '항목'))) {
    if (cells.length !== 8) {
      errors.push(`항목 표: 열 8개가 아님 (${cells.join(' | ')})`);
      continue;
    }
    const [id, owner, track, s, e, deps, task, model] = cells;
    const where = `항목 ${id}`;
    if (!ID_RE.test(id)) errors.push(`${where}: ID 형식 오류 (대문자 1~4자-숫자 1~2자리-소문자·숫자 0~2자, 예: P-1a, HP-2, F-26b2)`);
    if (items.some((i) => i.id === id)) errors.push(`${where}: ID 중복`);
    const owners = ownersOf(owner);
    if (!owners) errors.push(`${where}: 담당은 A, B, A·B 중 하나`);
    const start = date(s, where);
    const end = date(e, where);
    if (start && end && start > end) errors.push(`${where}: 시작이 끝보다 늦음`);
    items.push({
      id,
      owners: owners ?? [],
      owner,
      track,
      start,
      end,
      deps: deps === '-' ? [] : deps.split(/[,\s]+/).filter(Boolean),
      task,
      model,
    });
  }
  const ids = new Set(items.map((i) => i.id));
  for (const i of items) for (const d of i.deps) if (!ids.has(d)) errors.push(`항목 ${i.id}: 의존 ${d}가 항목 표에 없음`);

  const handoffs = [];
  for (const cells of tableRows(mdSection(md, '넘겨주는 시점'))) {
    if (cells.length !== 6) {
      errors.push(`넘겨주는 시점 표: 열 6개가 아님 (${cells.join(' | ')})`);
      continue;
    }
    const [d, from, to, item, what, unlocks] = cells;
    const where = `넘겨주는 시점 ${d} ${item}`;
    if (!ids.has(item)) errors.push(`${where}: 항목 ${item}이 항목 표에 없음`);
    const fromKeys = ownersOf(from);
    const toKeys = ownersOf(to);
    if (!fromKeys || !toKeys) errors.push(`${where}: 주는 쪽·받는 쪽은 A, B, A·B 중 하나`);
    handoffs.push({ date: date(d, where), from, to, fromKeys: fromKeys ?? [], toKeys: toKeys ?? [], item, what, unlocks });
  }

  const events = [];
  for (const cells of tableRows(mdSection(md, '공통 일정'))) {
    if (cells.length !== 2) {
      errors.push(`공통 일정 표: 열 2개가 아님 (${cells.join(' | ')})`);
      continue;
    }
    events.push({ date: date(cells[0], `공통 일정 ${cells[0]}`), text: cells[1] });
  }
  if (!items.length) errors.push('항목 표가 비었거나 "## 항목" 절이 없음');
  return { year, items, handoffs, events, errors };
}

// PR 본문 Roadmap: 줄. HTML 주석(PR 템플릿 안내문)과 펜스 코드 블록(사용 예시)은 빼고 본다
const REF_LINE = /^[ \t]*(?:[-*][ \t]+)?Roadmap:[ \t]*(.*)$/gim;
const REF_PART = new RegExp(`^\\s*\`?(${ROADMAP_ID})\`?\\s*(\\((?:일부|partial)\\))?\\s*$`, 'i');
const refLines = (body) =>
  [
    ...String(body ?? '')
      .replace(/<!--[\s\S]*?-->/g, '')
      // 펜스는 ``` 와 ~~~ 둘 다 (Codex 리뷰)
      .replace(/^[ \t]*(```|~~~)[\s\S]*?^[ \t]*\1[^\n]*$/gm, '')
      .matchAll(REF_LINE),
  ]
    .map((m) => m[1].trim())
    .filter(Boolean);

/**
 * PR 본문의 `Roadmap: <ID>[ (일부)], <ID>…` 줄 → [{ id, partial }].
 * 같은 ID가 완료·일부로 둘 다 있으면 완료로 본다. 형식이 틀린 조각은 건너뛴다 (roadmapRefIssues가 알린다)
 */
export function roadmapRefs(body) {
  const refs = new Map();
  for (const line of refLines(body)) {
    for (const part of line.split(/[,·]/)) {
      const r = REF_PART.exec(part);
      if (!r) continue;
      // 대소문자를 무시하고 합친다 (HP-2 (일부) + hp-2 → 완료, Codex 리뷰). id는 처음 적은 표기
      const key = r[1].toLowerCase();
      const partial = Boolean(r[2]);
      const prev = refs.get(key);
      refs.set(key, { id: prev?.id ?? r[1], partial: prev ? prev.partial && partial : partial });
    }
  }
  return [...refs.values()];
}

/**
 * PR 본문 Roadmap: 줄의 문제 (형식 오류, 로드맵에 없는 ID, 대소문자 다름) → 설명 목록. 빈 배열이면 정상.
 * 틀린 줄은 상태 계산에서 조용히 빠져 항목이 ⬜·⚠️로 남으므로 /pr-review가 `node scripts/roadmap.mjs --check-body`로 본다
 */
export function roadmapRefIssues(body, ids) {
  const issues = [];
  for (const line of refLines(body)) {
    for (const part of line.split(/[,·]/)) {
      const r = REF_PART.exec(part);
      if (!r) {
        issues.push(`형식 오류 "${part.trim()}" (예: Roadmap: HP-2, T-1 (일부))`);
        continue;
      }
      const exact = ids.includes(r[1]);
      const loose = ids.find((id) => id.toLowerCase() === r[1].toLowerCase());
      if (!exact && loose) issues.push(`대소문자가 다름 "${r[1]}" → ${loose}`);
      else if (!exact) issues.push(`로드맵에 없는 ID "${r[1]}" (docs/로드맵.md 항목 표)`);
    }
  }
  return issues;
}

/** 브랜치 이름의 한 부분이 `<ID>`이거나 `<ID>-`로 시작하면 그 항목 브랜치다 (feature/K-3a-seed → K-3a, F-26b2와 F-26b는 구별) */
export function branchMatches(branch, id) {
  const lower = id.toLowerCase();
  return String(branch ?? '')
    .split('/')
    .some((part) => {
      const p = part.toLowerCase();
      return p === lower || p.startsWith(`${lower}-`);
    });
}

/**
 * 항목별 상태 → Map(id → { state, prs, branches }). 순수 함수.
 * - done: 머지된 PR에 `Roadmap: <ID>` (일부 아님)
 * - doing: `Roadmap: <ID> (일부)`로 머지됨, 열린 PR이 ID를 적었거나 head가 ID 브랜치, develop에 안 합쳐진 원격 ID 브랜치
 * - todo: 그 밖. prs가 null(조회 실패)이면 브랜치로 doing만 알 수 있고 나머지는 unknown
 * prs: [{ number, state: 'MERGED'|'OPEN'|'CLOSED', body, headRefName }], branches: 원격 브랜치 이름 목록
 */
export function roadmapStatus(items, { prs = [], branches = [] } = {}) {
  const status = new Map();
  for (const item of items) {
    const mine = { state: prs ? 'todo' : 'unknown', prs: [], branches: branches.filter((b) => branchMatches(b, item.id)) };
    for (const pr of prs ?? []) {
      if (pr.state === 'CLOSED') continue;
      const ref = roadmapRefs(pr.body).find((r) => r.id.toLowerCase() === item.id.toLowerCase());
      const named = pr.state === 'OPEN' && branchMatches(pr.headRefName, item.id);
      if (!ref && !named) continue;
      mine.prs.push(pr.number);
      if (pr.state === 'MERGED' && ref && !ref.partial) mine.state = 'done';
      else if (mine.state !== 'done') mine.state = 'doing';
    }
    if (mine.state !== 'done' && mine.branches.length) mine.state = 'doing';
    status.set(item.id, mine);
  }
  return status;
}

/** 로드맵 + 상태 + 오늘 → 표시용 보기 (항목마다 state·late를 붙인다). statusOk=false면 지연 판정을 하지 않는다 */
export function roadmapView(roadmap, status, today, { statusOk = true, reason = null } = {}) {
  const items = roadmap.items.map((i, order) => {
    const s = status.get(i.id) ?? { state: 'unknown', prs: [], branches: [] };
    return { ...i, order, ...s, late: statusOk && s.state !== 'done' && Boolean(i.end) && i.end < today };
  });
  const byId = new Map(items.map((i) => [i.id, i]));
  const handoffs = roadmap.handoffs.map((h) => {
    const state = byId.get(h.item)?.state ?? 'unknown';
    return { ...h, state, days: h.date ? daysBetween(today, h.date) : null };
  });
  return { today, items, byId, handoffs, events: roadmap.events, statusOk, reason };
}

const byStart = (a, b) => (a.start ?? '').localeCompare(b.start ?? '') || (a.end ?? '').localeCompare(b.end ?? '') || a.order - b.order;
const openItems = (view, key) => view.items.filter((i) => i.owners.includes(key) && i.state !== 'done').sort(byStart);
const depsWaiting = (view, item) => (view.statusOk ? item.deps.filter((d) => view.byId.get(d)?.state !== 'done') : []);
const cut = (t, n) => (t.length > n ? `${t.slice(0, n - 1)}…` : t);
const dn = (days) => (days === 0 ? 'D-day' : days > 0 ? `D-${days}` : `${-days}일 지남`);

/**
 * 지금 할 항목: ① 지금 브랜치가 내 미완료 항목이면 그것 ② after(방금 끝낸 항목)가 있으면 같은 트랙의 다음 미완료 항목
 * ③ 의존이 다 끝난 내 미완료 항목 중 가장 이른 것 ④ 없으면 가장 이른 미완료 항목. 없으면 null
 * → { item, why: 'branch'|'track'|'ready'|'waiting', waiting: [의존 ID] }
 */
export function nextItem(view, key, { branch = null, after = null } = {}) {
  // 방금 끝낸 항목(after)은 아직 ✅로 안 보여도(일부 머지·조회 지연) 다시 고르지 않는다
  const open = openItems(view, key).filter((i) => i.id !== after);
  const pick = (item, why) => (item ? { item, why, waiting: depsWaiting(view, item) } : null);
  const onBranch = branch && open.find((i) => branchMatches(branch, i.id));
  if (onBranch) return pick(onBranch, 'branch');
  const track = after && view.byId.get(after)?.track;
  if (track) {
    const same = open.find((i) => i.track === track);
    if (same) return pick(same, 'track');
  }
  return pick(open.find((i) => !depsWaiting(view, i).length), 'ready') ?? pick(open[0], 'waiting');
}

/** 다음 항목 한 줄 (/handoff·PR 머지 뒤 보고, 추천 모델 포함) */
export function nextItemLine(next) {
  if (!next) return '➡️ 로드맵에 남은 내 항목이 없습니다 (docs/로드맵.md에 다음 항목을 추가)';
  const { item, why, waiting } = next;
  const note = { branch: ' · 지금 브랜치', track: ' · 같은 트랙 다음', ready: '', waiting: '' }[why];
  const wait = waiting.length ? ` · ⏳ 의존 대기 ${waiting.join(', ')}` : '';
  const late = item.late ? ' · ⚠️ 끝 날짜 지남' : '';
  return `➡️ 다음 항목: ${item.id} ${item.task} (트랙 ${item.track}, ${shortDate(item.start)}~${shortDate(item.end)}) · 추천 ${item.model}${note}${late}${wait}`;
}

/**
 * 넘겨주는 시점 중 key가 주거나 받는 것, 아직 끝나지 않은 것 (날짜순).
 * 상태를 모르면(조회 실패) 지난 것은 끝났는지 알 수 없어 빼고 stalePast로만 센다 (전부 "지남"으로 보이지 않게)
 */
function myHandoffs(view, key) {
  const mine = view.handoffs
    .filter((h) => h.state !== 'done' && (h.fromKeys.includes(key) || h.toKeys.includes(key)))
    .sort((a, b) => (a.date ?? '').localeCompare(b.date ?? ''));
  if (view.statusOk) return { list: mine, stalePast: 0 };
  const list = mine.filter((h) => h.days === null || h.days >= 0);
  return { list, stalePast: mine.length - list.length };
}

function handoffText(h) {
  return `${shortDate(h.date)} ${h.from}→${h.to} ${h.item} ${h.what}`;
}

/** 내가 주거나 받는 넘겨주는 시점 중 가장 이른 미완료 것 { date, text } | null (상태 표시줄, HX-6). 날짜 없는 것은 뺀다 */
export function nextHandoff(view, key) {
  const h = myHandoffs(view, key).list.find((x) => x.date && x.days !== null);
  return h ? { date: h.date, text: handoffText(h) } : null;
}

/**
 * 세션 시작 배너 줄. 순수 함수.
 * ① 🗺️ 트랙마다 지금 기간의 다음 항목 (시작한 미완료 항목 중 가장 이른 것) ② 넘겨주는 시점: D-2 안이면 🤝, 날짜가 지났으면 ⚠️, 그 밖에는 가장 가까운 것 하나 📅
 */
export function bannerLines(view, key) {
  const lines = [];
  const tracks = new Map();
  for (const i of openItems(view, key)) {
    if (i.start > view.today) continue;
    if (!tracks.has(i.track)) tracks.set(i.track, i);
  }
  if (tracks.size) {
    const parts = [...tracks.values()].map((i) => {
      const tail = i.late ? `⚠️ ${shortDate(i.end)} 지남` : `~${shortDate(i.end)}`;
      return `${cut(i.track, 10)} ${i.id} ${ICON[i.state]} ${tail}`;
    });
    lines.push(`🗺️ 로드맵: ${parts.join(' · ')}`);
  }
  const { list, stalePast } = myHandoffs(view, key);
  const pending = list.filter((h) => h.days !== null);
  const urgent = pending.filter((h) => h.days <= HANDOFF_NOTICE_DAYS);
  const unknown = view.statusOk ? '' : ' (상태 미확인)';
  for (const h of urgent) lines.push(`${h.days < 0 ? '⚠️' : '🤝'} 넘겨주기 ${dn(h.days)}: ${cut(handoffText(h), 70)}${unknown}`);
  if (!urgent.length && pending.length) lines.push(`📅 다음 넘겨주기 ${dn(pending[0].days)}: ${cut(handoffText(pending[0]), 70)}${unknown}`);
  if (!view.statusOk) lines.push(`❔ 로드맵 상태 조회 실패 (${view.reason ?? '?'}${stalePast ? `, 지난 넘겨주기 ${stalePast}건 확인 못 함` : ''}) → node scripts/roadmap.mjs`);
  return lines;
}

function itemLine(view, i) {
  const wait = depsWaiting(view, i);
  const where = i.prs.length ? ` (PR ${i.prs.map((n) => `#${n}`).join(', ')})` : i.branches.length ? ` (${i.branches[0]})` : '';
  const late = i.late ? ' ⚠️ 끝 날짜 지남' : '';
  return `${ICON[i.state]} ${i.id} ${shortDate(i.start)}~${shortDate(i.end)} [${i.track}] ${i.task}${where}${late}${wait.length ? ` · ⏳ ${wait.join(', ')}` : ''}`;
}

/**
 * 모델 컨텍스트·/pickup 보고용 로드맵 블록 (마크다운). 순수 함수.
 * 지금 세션에서 할 일, 내 남은 항목(시작 순), 상대의 이번 기간 항목, 넘겨주는 시점, 공통 일정
 */
export function roadmapBlock(view, key, { branch = null, after = null, limit = 10, peerName = '상대' } = {}) {
  const out = [`## 로드맵 (${ROADMAP_REL} · 오늘 ${shortDate(view.today)} · 상태는 PR 본문 \`Roadmap:\`·브랜치로 계산)`];
  if (!view.statusOk) out.push(`- ❔ 상태 조회 실패 (${view.reason ?? '?'}): 지연(⚠️)·완료 판정을 못 했다. \`node scripts/roadmap.mjs\`로 다시 본다`);
  out.push(`- 지금 세션에서 할 일: ${nextItemLine(nextItem(view, key, { branch, after })).replace(/^➡️ 다음 항목: /, '')}`);
  const mine = openItems(view, key);
  out.push(`- 내 남은 항목 (시작 순, ${mine.length}개${mine.length > limit ? ` 중 ${limit}개` : ''}):`);
  for (const i of mine.slice(0, limit)) out.push(`  - ${itemLine(view, i)}`);
  const other = Object.values(OWNER_OF).find((k) => k !== key);
  const theirs = openItems(view, other).filter((i) => i.start <= view.today && !i.owners.includes(key));
  if (theirs.length) {
    out.push(`- ${peerName}(${LABEL_OF[other]}) 이번 기간:`);
    for (const i of theirs.slice(0, limit)) out.push(`  - ${itemLine(view, i)}`);
  }
  const hand = myHandoffs(view, key).list.slice(0, 5);
  if (hand.length) {
    out.push('- 넘겨주는 시점 (나와 관련, 남은 것):');
    for (const h of hand) out.push(`  - ${h.days !== null && h.days < 0 ? '⚠️' : '🤝'} ${shortDate(h.date)} (${dn(h.days)}) ${h.from}→${h.to} ${h.item} ${h.what} → ${h.unlocks} (항목 ${ICON[h.state]})`);
  }
  const events = view.events.filter((e) => e.date && e.date >= view.today).slice(0, 4);
  if (events.length) out.push(`- 공통 일정: ${events.map((e) => `${shortDate(e.date)} (${dn(daysBetween(view.today, e.date))}) ${e.text}`).join(' · ')}`);
  return out.join('\n');
}

/**
 * develop push 사이에 새로 지난 넘겨주는 시점 (Mattermost 한 번 알림용). 순수 함수.
 * 날짜 D는 D+1일부터 지남이다. 직전 develop 끝 커밋 날(sinceDay) 뒤 오늘까지 지난 것 = sinceDay ≤ D < today.
 * 같은 시점은 그 구간에 든 첫 push에서만 알리게 되어 상태 파일 없이 "한 번"이 된다. 상태 조회 실패면 알리지 않는다.
 */
export function newlyOverdue(view, sinceDay) {
  if (!view.statusOk || !sinceDay) return [];
  return view.handoffs.filter((h) => h.state !== 'done' && h.date && h.date >= sinceDay && h.date < view.today);
}

/** 새로 지난 넘겨주는 시점 → Mattermost 본문 (없으면 null) */
export function overdueMessage(overdue) {
  if (!overdue.length) return null;
  const lines = overdue.map((h) => `- ${shortDate(h.date)} ${h.from}→${h.to} **${h.item}** ${h.what} (항목 ${ICON[h.state]}) → 풀리는 작업: ${h.unlocks}`);
  return `⚠️ **로드맵 넘겨주는 시점이 지났습니다** (끝나지 않음)\n${lines.join('\n')}\n끝냈으면 그 PR 본문에 \`Roadmap: <ID>\`를, 미뤘으면 \`${ROADMAP_REL}\` 날짜를 고칩니다.`;
}

/**
 * Actions(roadmap.yml)용: 직전 develop 끝 커밋 시각(sinceIso) 뒤 새로 지난 넘겨주는 시점 → { text | null, reason | null }.
 * 상태 조회가 실패하면 알리지 않고 이유를 돌려준다 (로그에 남겨 알림이 왜 빠졌는지 보이게)
 */
export function overduePayload(md, prsRes, sinceIso, { today = seoulToday() } = {}) {
  if (!sinceIso) return { text: null, reason: '직전 커밋 시각 없음' };
  const { view } = loadView(md, prsRes, { today, branches: [] });
  if (!view.statusOk) return { text: null, reason: `상태 조회 실패: ${view.reason ?? '?'}` };
  return { text: overdueMessage(newlyOverdue(view, seoulToday(new Date(sinceIso)))), reason: null };
}

/**
 * 상태 계산에 쓸 PR 목록 → { ok: true, prs } | { ok: false, reason }. 세션 시작을 막지 않도록 비동기·제한 시간 (issues.mjs와 같은 방식).
 * `--search`는 GitHub 검색 색인을 써서 방금 머지·수정한 PR이 늦게 보인다(그날 한 번뿐인 Mattermost 알림이 거짓이 된다) → 전체 목록을 받는다.
 * 11-26 기준 192개·약 500KB·1.6초. 동기 git이 끝난 뒤 부른다 (#67).
 */
export function fetchRoadmapPrs({ timeout = 4000, exec = execFile, limit = 500 } = {}) {
  return new Promise((resolve) => {
    exec(
      'gh',
      ['pr', 'list', '--state', 'all', '--limit', String(limit), '--json', 'number,state,body,headRefName'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout, maxBuffer: 32 * 1024 * 1024 },
      (err, stdout, stderr) => {
        if (err?.code === 'ENOENT') return resolve({ ok: false, reason: 'gh 없음' });
        if (err?.killed) return resolve({ ok: false, reason: `${timeout / 1000}초 안에 응답 없음` });
        if (err) {
          const reason = `gh 실패: ${String(stderr || err.message).trim().split('\n')[0]}`;
          return resolve({ ok: false, reason: reason.length > 80 ? `${reason.slice(0, 79)}…` : reason });
        }
        try {
          const prs = JSON.parse(String(stdout ?? '').trim() || 'null');
          resolve(Array.isArray(prs) ? { ok: true, prs } : { ok: false, reason: '응답 형식 오류' });
        } catch {
          resolve({ ok: false, reason: '응답 형식 오류' });
        }
      },
    );
  });
}

/** develop에 아직 안 합쳐진 원격 브랜치 이름 (origin/ 뗌). squash 머지로 남은 브랜치는 PR의 ✅가 이긴다 */
export function unmergedRemoteBranches({ cwd = ROOT, base = 'origin/develop', timeout = 3000 } = {}) {
  const r = git(['for-each-ref', '--format=%(refname:short)', '--no-merged', base, 'refs/remotes/origin'], { cwd, timeout: Math.max(1, timeout) });
  return r.ok ? r.out.split('\n').filter(Boolean).map((b) => b.replace(/^origin\//, '')).filter((b) => b !== 'HEAD' && b !== 'origin') : [];
}

/** 파일·PR·브랜치를 모아 보기를 만든다. prsRes는 fetchRoadmapPrs 결과. 아직 push 안 한 지금 브랜치도 진행 중으로 센다 */
export function loadView(md, prsRes, { today = seoulToday(), branches = [...unmergedRemoteBranches(), currentBranch()].filter(Boolean) } = {}) {
  const roadmap = parseRoadmap(md);
  const prs = prsRes?.ok ? prsRes.prs : null;
  return { roadmap, view: roadmapView(roadmap, roadmapStatus(roadmap.items, { prs, branches }), today, { statusOk: Boolean(prsRes?.ok), reason: prsRes?.reason }) };
}
