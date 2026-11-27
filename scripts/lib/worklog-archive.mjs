// 작업로그 주차별 보관 (HP-5 HX-9). 두 사람 공용 docs/작업로그.md가 하루 약 95줄씩 늘어나므로
// 지금 주보다 앞선 주의 항목을 docs/기록/작업로그-<주>.md로 옮겨 AI가 읽는 파일과 union 머지 범위를 작게 둔다.
// 판정은 순수 함수이고 파일 읽기·쓰기는 scripts/archive-worklog.mjs가 한다.
import { WORKLOG_WEEKS } from './common.mjs';

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
export const isDate = (s) => DATE_RE.test(String(s || ''));

/** 날짜 'YYYY-MM-DD'가 속한 주 { id, start, label } (W1 이전이면 null) */
export function weekOf(date) {
  return WORKLOG_WEEKS.filter((w) => w.start <= date).at(-1) ?? null;
}

/**
 * 머리말(첫 항목 앞)과 항목 [{ date, header, text }]. 항목은 코드 블록 밖의 "## " 줄에서 시작한다
 * (머리말의 형식 예시 "## YYYY-MM-DD …"는 ``` 안에 있다). date는 헤더가 "## YYYY-MM-DD"로 시작할 때만, 아니면 null.
 * text는 헤더 줄부터 다음 항목 전까지이고 끝의 빈 줄을 뺀다.
 */
export function parseWorklog(text) {
  const lines = String(text || '').split(/\r?\n/);
  const blocks = [[]];
  let fenced = false;
  for (const line of lines) {
    if (/^\s*```/.test(line)) fenced = !fenced;
    if (!fenced && line.startsWith('## ')) blocks.push([]);
    blocks.at(-1).push(line);
  }
  const join = (b) => b.join('\n').replace(/\s+$/, '');
  return {
    preamble: join(blocks[0]),
    entries: blocks.slice(1).map((b) => {
      const date = /^## (\d{4}-\d{2}-\d{2})\b/.exec(b[0])?.[1] ?? null;
      return { date, header: b[0], text: join(b) };
    }),
  };
}

const render = (preamble, entries) => [preamble, ...entries.map((e) => e.text)].filter(Boolean).join('\n\n') + '\n';

/**
 * 오늘(today)이 속한 주보다 앞선 주의 항목을 떼어 낸다.
 * → { log: 남길 작업로그 전문, moved: Map<주 id, 항목[]> (주 순서, 항목은 문서 순서), warnings: string[] }
 * 날짜를 읽을 수 없거나 W1 이전인 항목은 남기고 경고한다 (자료를 잃지 않는다). 옮길 것이 없으면 log는 입력 그대로다.
 */
export function splitByWeek(text, today) {
  const { preamble, entries } = parseWorklog(text);
  const current = weekOf(today);
  const currentIdx = current ? WORKLOG_WEEKS.indexOf(current) : -1;
  const moved = new Map();
  const kept = [];
  const warnings = [];
  for (const e of entries) {
    const w = e.date ? weekOf(e.date) : null;
    if (!w) {
      warnings.push(`${e.date ? 'W1 이전 날짜' : '날짜를 읽을 수 없는 헤더'}라 남겨 둡니다: ${e.header}`);
      kept.push(e);
    } else if (WORKLOG_WEEKS.indexOf(w) < currentIdx) {
      if (!moved.has(w.id)) moved.set(w.id, []);
      moved.get(w.id).push(e);
    } else kept.push(e);
  }
  const ordered = new Map(WORKLOG_WEEKS.filter((w) => moved.has(w.id)).map((w) => [w.id, moved.get(w.id)]));
  return { log: moved.size ? render(preamble, kept) : String(text), moved: ordered, warnings };
}

/** 보관 파일 머리 (제목 + 원래 파일 링크) */
export function archiveHeader(weekId) {
  const w = WORKLOG_WEEKS.find((x) => x.id === weekId);
  return [
    `# 작업로그 ${weekId} (${w?.label ?? '?'})`,
    '',
    '> 지난 주 항목을 `docs/작업로그.md`에서 옮긴 보관본이다 (`node scripts/archive-worklog.mjs`, 주간 `/progress`). 고치지 않는다.',
    '> 이번 주 기록과 형식: [작업로그](../작업로그.md)',
  ].join('\n');
}

/**
 * 보관 파일(없으면 null)에 항목을 합친다 → { text, added, duplicates }.
 * 이미 있는 항목(앞뒤 공백 제외 동일)은 붙이지 않는다: union 머지로 보관한 항목이 작업로그에 다시 나타난 경우다.
 * 새 항목은 날짜 순으로(같은 날은 들어온 순서) 끝에 붙인다.
 */
export function mergeArchive(existing, weekId, entries) {
  const base = existing === null ? { preamble: archiveHeader(weekId), entries: [] } : parseWorklog(existing);
  const seen = new Set(base.entries.map((e) => e.text.trim()));
  const fresh = [];
  for (const e of entries) {
    if (seen.has(e.text.trim())) continue;
    seen.add(e.text.trim());
    fresh.push(e);
  }
  fresh.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
  return { text: render(base.preamble, [...base.entries, ...fresh]), added: fresh.length, duplicates: entries.length - fresh.length };
}
