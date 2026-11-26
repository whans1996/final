// 시드 카드 원본(data/seed/concepts/<category>/<slug>.md) 파서와 R__seed_concepts.sql 생성기 (K-3a AC1).
// 형식은 docs/specs/K-3a-seed-check.md 4절. 형식 검사(오류·경고 판정)는 format.mjs가 한다.
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, relative } from 'node:path';

export const FRONT_KEYS = ['name', 'category', 'difficulty', 'reference'];
export const SECTIONS = ['summary', 'body', 'example'];
export const DIFFICULTIES = ['BEGINNER', 'JUNIOR', 'SENIOR'];
/** 예제 실행(②) 대상 언어. 아래 UNCHECKED_LANGS는 허용하되 "검사 안 함"으로 보고한다 */
export const CHECKED_LANGS = ['java', 'sql', 'xml', 'yaml', 'js', 'jsx', 'ts', 'tsx', 'vue', 'json'];
export const UNCHECKED_LANGS = ['http', 'text', 'bash', 'properties', 'gradle', 'kotlin'];
/** 생성 순서. 여기 없는 카테고리는 이 뒤에 이름순 */
export const CATEGORY_ORDER = ['java', 'spring', 'web', 'security', 'mybatis', 'vue'];

const SECTION_RE = /^## ([a-z][a-z0-9-]*)\s*$/;
const FENCE_RE = /^ {0,3}(`{3,}|~{3,})(.*)$/;

/** 펜스 시작 줄이면 {char, len, info}. 닫는 줄 판정은 isFenceClose */
function fenceOpen(line) {
  const m = FENCE_RE.exec(line);
  if (!m) return null;
  if (m[1][0] === '`' && m[2].includes('`')) return null; // 백틱 펜스의 info에는 백틱이 올 수 없다 (CommonMark)
  return { char: m[1][0], len: m[1].length, info: m[2].trim() };
}
const isFenceClose = (line, fence) => new RegExp(`^ {0,3}\\${fence.char}{${fence.len},}\\s*$`).test(line);

const isBlank = (l) => /^\s*$/.test(l);
function trimBlankLines(lines) {
  let s = 0;
  let e = lines.length;
  while (s < e && isBlank(lines[s])) s++;
  while (e > s && isBlank(lines[e - 1])) e--;
  return lines.slice(s, e);
}

function parseFrontValue(raw) {
  const v = raw.trim();
  if (v.startsWith('"')) {
    try {
      return JSON.parse(v);
    } catch {
      return v;
    }
  }
  return v;
}

/**
 * 카드 Markdown을 읽는다. 구조 문제(frontmatter 없음, 같은 키·섹션 두 번, 닫히지 않은 코드 블록 등)는 problems에 모은다.
 * @returns {{file, front, sections, sectionLines, blocks, outside, problems}}
 *   blocks: example 섹션의 코드 블록 [{lang, code, line}] (line은 파일 기준 1부터, 블록 첫 코드 줄)
 *   outside: example 섹션에서 코드 블록 밖에 있는 텍스트 줄
 */
export function parseCard(text, file = '') {
  const lines = text.replace(/\r\n?/g, '\n').split('\n');
  const card = { file, front: {}, sections: {}, sectionLines: {}, blocks: [], outside: [], problems: [] };
  const problem = (item, message) => card.problems.push({ item, message });

  let i = 0;
  if (lines[0] !== '---') {
    problem('frontmatter', '첫 줄이 --- 가 아님 (frontmatter 없음)');
  } else {
    const end = lines.indexOf('---', 1);
    if (end < 0) {
      problem('frontmatter', '닫는 --- 가 없음');
      i = lines.length;
    } else {
      for (let j = 1; j < end; j++) {
        if (isBlank(lines[j])) continue;
        const m = /^([A-Za-z][\w-]*):(.*)$/.exec(lines[j]);
        if (!m) {
          problem('frontmatter', `${j + 1}번 줄을 "키: 값"으로 읽을 수 없음`);
          continue;
        }
        if (Object.hasOwn(card.front, m[1])) problem(m[1], '같은 키가 두 번 나옴');
        card.front[m[1]] = parseFrontValue(m[2]);
      }
      i = end + 1;
    }
  }

  // 섹션 나누기 (코드 블록 안의 "## " 줄은 제목이 아니다)
  let current = null;
  let fence = null;
  const buf = {};
  for (; i < lines.length; i++) {
    const line = lines[i];
    if (fence) {
      if (isFenceClose(line, fence)) fence = null;
    } else {
      const f = fenceOpen(line);
      if (f) fence = f;
      else {
        const m = SECTION_RE.exec(line);
        if (m) {
          current = m[1];
          if (Object.hasOwn(buf, current)) problem(current, '같은 섹션이 두 번 나옴');
          buf[current] = [];
          card.sectionLines[current] = i + 2; // 내용 첫 줄 (1부터)
          continue;
        }
      }
    }
    if (current) buf[current].push({ line, no: i + 1 });
    else if (!isBlank(line)) problem('section', `${i + 1}번 줄이 어떤 섹션에도 속하지 않음`);
  }
  if (fence) problem('section', '닫히지 않은 코드 블록이 있음');

  for (const [name, rows] of Object.entries(buf)) {
    card.sections[name] = trimBlankLines(rows.map((r) => r.line)).join('\n');
  }
  if (buf.example) parseExample(buf.example, card);
  return card;
}

function parseExample(rows, card) {
  let fence = null;
  let code = [];
  let start = 0;
  for (const { line, no } of rows) {
    if (fence) {
      if (isFenceClose(line, fence)) {
        card.blocks.push({ lang: fence.info.split(/\s+/)[0] || '', code: code.join('\n'), line: start });
        fence = null;
      } else code.push(line);
      continue;
    }
    const f = fenceOpen(line);
    if (f) {
      fence = f;
      code = [];
      start = no + 1;
    } else if (!isBlank(line)) card.outside.push(no);
  }
}

/** DB에 들어갈 값. example_code는 블록 내용을 빈 줄 하나로 이어 붙인다 (언어 태그는 저장하지 않음) */
export function toRow(card) {
  const f = card.front;
  return {
    name: f.name ?? '',
    category: f.category ?? '',
    difficulty: f.difficulty ?? '',
    summary: card.sections.summary ?? '',
    body: card.sections.body ?? '',
    example_code: card.blocks.map((b) => b.code).join('\n\n'),
    reference_url: f.reference ?? '',
  };
}

const needsQuote = (v) => v !== v.trim() || v.startsWith('"') || v === '';
const frontValue = (v) => (needsQuote(v) ? JSON.stringify(v) : v);

/** 코드 안에 든 가장 긴 백틱 줄보다 긴 펜스 */
function fenceFor(code) {
  const longest = Math.max(0, ...(code.match(/`+/g) || []).map((s) => s.length));
  return '`'.repeat(Math.max(3, longest + 1));
}

/** 행 값과 예제 블록 [{lang, code}]으로 카드 Markdown을 만든다 (변환·테스트 픽스처용. parseCard와 왕복된다) */
export function renderCard(row, blocks) {
  const out = ['---'];
  out.push(`name: ${frontValue(row.name)}`);
  out.push(`category: ${frontValue(row.category)}`);
  out.push(`difficulty: ${frontValue(row.difficulty)}`);
  out.push(`reference: ${frontValue(row.reference_url)}`);
  out.push('---', '', '## summary', '', row.summary, '', '## body', '', row.body, '', '## example', '');
  blocks.forEach((b, idx) => {
    const fence = fenceFor(b.code);
    if (idx) out.push('');
    out.push(fence + b.lang, b.code, fence);
  });
  out.push('');
  return out.join('\n');
}

/** MySQL 문자열 리터럴 (기본 sql_mode: 역슬래시가 이스케이프 문자다) */
export function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
}

export function categoryRank(category) {
  const i = CATEGORY_ORDER.indexOf(category);
  return i < 0 ? CATEGORY_ORDER.length : i;
}

/** 생성 순서: 카테고리 순 → 파일 이름순 */
export function sortCards(cards) {
  return [...cards].sort((a, b) => {
    const ca = toRow(a).category;
    const cb = toRow(b).category;
    return categoryRank(ca) - categoryRank(cb) || (ca < cb ? -1 : ca > cb ? 1 : 0) || (a.file < b.file ? -1 : a.file > b.file ? 1 : 0);
  });
}

const HEADER = `-- 생성 파일, 직접 고치지 말 것.
-- 원본은 data/seed/concepts/<category>/<slug>.md 카드 파일이고 \`node scripts/seed-build.mjs\`로 다시 만든다.
-- CI가 \`seed-build.mjs --check\`로 원본과 같은지 확인하고, \`seed-check.mjs\`가 형식·예제를 검사한다 (K-3a).
--
-- 시드 개념 카드 (DS-05, WBS 2.5). 반복 마이그레이션이라 내용이 바뀌면 다시 적용된다.
--
-- 출처 규칙 (docs/산출물/09-데이터셋.md DS-01 카드 작성 규칙)
--   - 근거는 DS-01(split content) 공식 문서만 쓴다. 평가(eval) 저장소 코드는 쓰지 않는다.
--   - 본문은 팀이 직접 쓴다. 원문 번역·각색 금지. Oracle·Swagger·MDN은 링크만 건다.
--   - 예제 코드는 개념에 맞게 새로 쓴다. reference_url은 대표 출처 1개.
-- 주의
--   - name(UNIQUE) 기준 upsert라 id는 유지된다(찜·리뷰 FK 보존). 대신 카드 파일을 고치면
--     관리자(F-13)가 화면에서 수정한 시드 카드 내용이 카드 파일 내용으로 덮어써진다.
--   - status는 검수 전 초안(DRAFT)일 때만 파일 값으로 바꾼다(F-13 AC12). 새 카드와 AI가 먼저 초안으로
--     만든 이름은 파일의 PUBLISHED가 되고, 관리자가 정한 PUBLISHED·HIDDEN은 유지한다.
--     시드 카드를 내리려면 관리자 화면에서 숨긴다. (status 줄은 source보다 먼저 둔다: 기존 값을 본다)
--   - 카드 이름을 바꾸면 새 카드가 생긴다. 이름은 바꾸지 말고 필요하면 새 카드를 추가한다.
--   - 작은따옴표와 역슬래시는 생성기가 이스케이프한다 (MySQL 기본 모드에서 역슬래시는 이스케이프 문자).
`;

const UPSERT = `AS seed
ON DUPLICATE KEY UPDATE
    status        = IF(concepts.status = 'DRAFT', seed.status, concepts.status),
    category      = seed.category,
    difficulty    = seed.difficulty,
    summary       = seed.summary,
    body          = seed.body,
    example_code  = seed.example_code,
    source        = seed.source,
    reference_url = seed.reference_url;
`;

/** 카드들로 R__seed_concepts.sql 전체를 만든다. 입력 순서와 관계없이 같은 카드면 같은 출력이다 */
export function buildSql(cards) {
  const out = [HEADER, 'INSERT INTO concepts (name, category, difficulty, summary, body, example_code, status, source, reference_url) VALUES'];
  const sorted = sortCards(cards);
  let lastCategory = null;
  sorted.forEach((card, idx) => {
    const r = toRow(card);
    if (r.category !== lastCategory) {
      out.push('', `-- ───────────── ${r.category} ─────────────`);
      lastCategory = r.category;
    }
    out.push(
      '',
      `-- ${card.file}`,
      `(${sqlString(r.name)}, ${sqlString(r.category)}, ${sqlString(r.difficulty)},`,
      ` ${sqlString(r.summary)},`,
      ` ${sqlString(r.body)},`,
      ` ${sqlString(r.example_code)},`,
      ` 'PUBLISHED', 'SEED', ${sqlString(r.reference_url)})${idx < sorted.length - 1 ? ',' : ''}`,
    );
  });
  out.push('', UPSERT);
  return out.join('\n');
}

/**
 * <dir>/<category>/<slug>.md 카드 파일을 모두 읽는다 (경로 정렬). file은 base 기준 상대 경로(/ 구분).
 * 깊이가 다른 .md 파일도 읽어서 형식 검사가 경로 오류로 알리게 한다.
 */
export function readCards(dir, base = dir) {
  if (!existsSync(dir)) return [];
  const files = [];
  const walk = (d) => {
    for (const e of readdirSync(d, { withFileTypes: true })) {
      const p = join(d, e.name);
      if (e.isDirectory()) walk(p);
      else if (e.name.endsWith('.md')) files.push(p);
    }
  };
  walk(dir);
  return files
    .map((p) => ({ p, rel: relative(base, p).replace(/\\/g, '/') }))
    .sort((a, b) => (a.rel < b.rel ? -1 : a.rel > b.rel ? 1 : 0))
    .map(({ p, rel }) => parseCard(readFileSync(p, 'utf8'), rel));
}
