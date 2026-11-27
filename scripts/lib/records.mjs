// NOW·작업로그 자동 초안 (H-1). 사실(커밋, 바뀐 영역, 검증, PR)은 git·GitHub에서 채우고
// 사람·AI는 메모 3줄(한 일, 다음 할 일, 막힌 점)만 쓴다. 기록 부담을 줄이고 AI가 큰 작업로그 파일을 읽지 않게 한다.
// 파일·git 호출 없는 부분이다 (scripts/test/records.test.mjs).
import { lastWorklogEntryOf, worklogEntries } from './common.mjs';

/**
 * Git Bash(MSYS)가 경로로 보고 바꾼 메모 인자를 되돌린다 (#231).
 * `--next "/pr-review → …"`처럼 `/`로 시작하는 인자를 MSYS가 `C:/Program Files/Git/pr-review → …`로 바꿔 넘긴다.
 * MSYS 아래(env.MSYSTEM)에서만, Git 설치 경로(EXEPATH의 상위, 없으면 기본 설치 경로)로 시작할 때만 그 접두사를 `/`로 되돌린다.
 */
export function undoMsysPathConv(value, env = process.env) {
  if (typeof value !== 'string' || !env?.MSYSTEM) return value;
  const roots = [];
  if (env.EXEPATH) roots.push(env.EXEPATH.replace(/\\/g, '/').replace(/\/+$/, '').replace(/\/(usr\/)?bin$/i, ''));
  roots.push('C:/Program Files/Git');
  for (const root of roots) {
    const prefix = `${root}/`;
    if (value.slice(0, prefix.length).toLowerCase() === prefix.toLowerCase()) return `/${value.slice(prefix.length)}`;
  }
  return value;
}

/** NOW 파일의 "갱신:" 줄을 바꾼다. 없으면 맨 위에 넣는다 */
export function touchNowHeader(md, head) {
  return /^갱신:.*$/m.test(md) ? md.replace(/^갱신:.*$/m, head) : `${head}\n\n${md}`;
}

export const NOW_MAX_LINES = 30;
const NOW_REQUIRED_SECTIONS = ['현재 기능', '바로 다음 할 일'];
const hasSection = (md, title) => new RegExp(`^## ${title}\\s*$`, 'm').test(md);

/**
 * NOW 파일 형식 문제 목록 (없으면 []). previousPc·peer-watch·handoff가 갱신 줄을, 세션 시작·상대 요약이 두 섹션을 읽는다.
 * #166: 덮어쓰다 머리말이 깨져 갱신 줄이 사라진 파일이 CI에서만 잡혔다
 */
export function nowFormatProblems(md) {
  const problems = [];
  const first = String(md || '').split(/\r?\n/).find((l) => l.trim());
  if (!/^# NOW\b/.test(first || '')) problems.push('제목 줄(# NOW …)이 맨 앞에 없음');
  if (!/^갱신: \d{4}-\d{2}-\d{2} \d{2}:\d{2}/m.test(md || '')) problems.push('맨 앞이 "갱신: YYYY-MM-DD HH:mm"인 줄이 없음');
  for (const s of NOW_REQUIRED_SECTIONS) if (!hasSection(md || '', s)) problems.push(`## ${s} 섹션이 없음`);
  return problems;
}

/**
 * NOW 본문만 바꾼다 (stamp --write-now, HP-4 AC4). 지금 파일의 머리말(맨 앞의 제목·`>` 안내·빈 줄)은 그대로 두고
 * 그 뒤에 갱신 줄(head)과 본문을 쓴다. 본문에 갱신 줄이 있거나 필수 섹션이 없으면 { text: null, error }.
 * 결과가 NOW_MAX_LINES줄을 넘으면 warning만 준다.
 */
export function replaceNowBody(current, body, head) {
  const cleanBody = String(body || '').replace(/\r\n/g, '\n').trim();
  if (/^갱신:/m.test(cleanBody)) return { text: null, error: '본문에 "갱신:"으로 시작하는 줄이 있습니다. 갱신 줄은 stamp가 씁니다 (본문에서 빼세요)', warning: null };
  const missing = NOW_REQUIRED_SECTIONS.filter((s) => !hasSection(cleanBody, s));
  if (missing.length) return { text: null, error: `본문에 ${missing.map((s) => `## ${s}`).join('·')} 섹션이 없습니다`, warning: null };
  const lines = String(current || '').replace(/\r\n/g, '\n').split('\n');
  const end = lines.findIndex((l) => !(/^# /.test(l) || l.startsWith('>') || !l.trim()));
  const header = (end < 0 ? lines : lines.slice(0, end)).join('\n').replace(/\s+$/, '');
  const text = `${header ? `${header}\n` : ''}${head}\n\n${cleanBody}\n`;
  const count = text.trimEnd().split('\n').length;
  return { text, error: null, warning: count > NOW_MAX_LINES ? `NOW 파일이 ${count}줄입니다 (${NOW_MAX_LINES}줄 이내로 줄이세요)` : null };
}

/** 이 사람(key)의 마지막 작업로그 항목 시각 'YYYY-MM-DD HH:mm' (없으면 null) */
export function lastEntryAt(log, key, pcs = {}) {
  return lastWorklogEntryOf(worklogEntries(log), key, pcs)?.at ?? null;
}

const oneLine = (s) =>
  String(s || '')
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
    .join(' / ');

/** 커밋 줄: "3개 — fix(harness): a (3d57f6b) · …". 많으면 앞 maxShown개만 */
export function commitsLine(commits, maxShown = 6) {
  if (!commits.length) return '없음';
  const shown = commits.slice(0, maxShown).map(({ sha, subject }) => `${subject} (${sha.slice(0, 7)})`);
  return `${commits.length}개 — ${shown.join(' · ')}${commits.length > maxShown ? ` · 외 ${commits.length - maxShown}개` : ''}`;
}

/**
 * 작업로그 항목. did·next·blocked가 메모 3줄이고, 커밋 줄은 자동이다. ai는 있을 때만 (기록할 만한 AI 활용 사례).
 * 메모가 비면 만들지 않는다 (null): 사실만 있는 항목은 다음 사람에게 쓸모가 없다.
 */
export function logEntry({ header, did, next, blocked, ai, commits = [] }) {
  if (!oneLine(did) || !oneLine(next)) return null;
  return [
    `## ${header}`,
    `- 한 일: ${oneLine(did)}`,
    `- 커밋: ${commitsLine(commits)}`,
    `- 다음 할 일: ${oneLine(next)}`,
    `- 막힌 점: ${oneLine(blocked) || '없음'}`,
    ...(oneLine(ai) ? [`- AI 활용: ${oneLine(ai)}`] : []),
  ].join('\n');
}

/** 검증 상태 한 줄: verified.json(영역 → 통과한 sha)과 HEAD로 */
export function verifiedLine(verified, head, areas) {
  if (!areas.length) return '검증할 코드 변경 없음';
  return areas.map((a) => `${a} ${head && verified?.[a] === head ? '✅ HEAD 통과' : '⬜ HEAD 미검증'}`).join(' · ');
}
