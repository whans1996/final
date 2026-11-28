// 시드 카드 ①형식 검사 (K-3a AC4~AC6). 심각도: error(CI 실패) · warn(보고서에만, K-3c 플래그 목록 재료)
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT } from '../common.mjs';
import { CHECKED_LANGS, DIFFICULTIES, FRONT_KEYS, LEVEL_SECTIONS, SECTIONS, UNCHECKED_LANGS, toRow } from './card.mjs';

export const SEED_SRC = 'data/seed/concepts';
const SLUG_RE = /^[a-z0-9]+(-[a-z0-9]+)*\.md$/;
const LANGS = new Set([...CHECKED_LANGS, ...UNCHECKED_LANGS]);

/** DB 한도(오류)와 권장 범위(경고). 글자 수는 코드 포인트 기준 (VARCHAR(n)은 문자 수) */
export const LIMITS = {
  nameMax: 100,
  nameWarn: 60,
  summaryMax: 500,
  summaryWarn: [40, 200],
  levelSummaryMax: 300,
  levelBodyMax: 10000,
  referenceMax: 500,
  textMaxBytes: 65535,
  bodyWarn: [150, 2000],
  exampleLinesWarn: 60,
  similarSummary: 0.5,
};

/** 형식 규칙: 카테고리는 BE ConceptCategories.ORDERED(12종, K-1), 출처 호스트는 datasets.json DS-01 */
export function loadRules(root = ROOT) {
  const java = readFileSync(join(root, 'backend/src/main/java/com/vibenote/analysis/concept/ConceptCategories.java'), 'utf8');
  const set = /ORDERED\s*=\s*List\.of\(([^)]*)\)/.exec(java);
  if (!set) throw new Error('ConceptCategories.ORDERED를 읽을 수 없음');
  const categories = new Set([...set[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]));
  const datasets = JSON.parse(readFileSync(join(root, 'data/datasets.json'), 'utf8')).datasets;
  const ds01 = datasets.find((d) => d.id === 'DS-01');
  const hosts = new Set((ds01?.sources ?? []).map((s) => new URL(s.url).host));
  return { src: SEED_SRC, categories, hosts };
}

const len = (s) => [...s].length;
const bytes = (s) => Buffer.byteLength(s ?? '', 'utf8');
/** MySQL utf8mb4_0900_ai_ci에서 같은 값인지 (대소문자·악센트 무시. NO PAD라 공백은 그대로) */
export const aiCiKey = (s) => s.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().normalize('NFC');
/** 공백·기호까지 무시한 이름 */
const looseKey = (s) => aiCiKey(s).replace(/[^\p{L}\p{N}]/gu, '');

function trigrams(s) {
  const t = s.replace(/\s+/g, ' ').trim();
  const chars = [...t];
  const set = new Set();
  if (chars.length < 3) set.add(t);
  for (let i = 0; i + 3 <= chars.length; i++) set.add(chars.slice(i, i + 3).join(''));
  return set;
}
export function jaccard(a, b) {
  let inter = 0;
  for (const x of a) if (b.has(x)) inter++;
  const union = a.size + b.size - inter;
  return union ? inter / union : 0;
}

function checkCard(card, rules, add) {
  const err = (item, message) => add('error', item, message);
  const warn = (item, message) => add('warn', item, message);
  for (const p of card.problems) err(p.item, p.message);

  // 경로: <src>/<category>/<slug>.md
  const prefix = `${rules.src}/`;
  const rel = card.file.startsWith(prefix) ? card.file.slice(prefix.length) : null;
  const parts = rel ? rel.split('/') : [];
  if (parts.length !== 2) err('path', `경로가 ${rules.src}/<category>/<slug>.md 형식이 아님`);
  else if (!SLUG_RE.test(parts[1])) err('path', `파일 이름 "${parts[1]}"이 영문 소문자 kebab(.md)이 아님`);
  const folder = parts.length >= 2 ? parts[0] : null;

  const f = card.front;
  for (const k of FRONT_KEYS) if (!Object.hasOwn(f, k)) err(k, `frontmatter에 ${k}가 없음`);
  for (const k of Object.keys(f)) if (!FRONT_KEYS.includes(k)) err(k, `모르는 frontmatter 키 (허용: ${FRONT_KEYS.join('·')})`);
  for (const s of SECTIONS) if (!Object.hasOwn(card.sections, s)) err(s, `## ${s} 섹션이 없음`);
  const allowed = [...SECTIONS, ...Object.keys(LEVEL_SECTIONS)];
  for (const s of Object.keys(card.sections)) if (!allowed.includes(s)) err(s, `모르는 섹션 ## ${s} (허용: ${allowed.join('·')})`);

  const r = toRow(card);
  if (Object.hasOwn(f, 'category')) {
    if (!rules.categories.has(r.category)) err('category', `"${r.category}"는 허용 카테고리가 아님 (ConceptCategories.ALL: ${[...rules.categories].join('·')})`);
    else if (folder && folder !== r.category) err('category', `category "${r.category}"와 폴더 "${folder}"가 다름`);
  }
  if (Object.hasOwn(f, 'difficulty') && !DIFFICULTIES.includes(r.difficulty)) err('difficulty', `"${r.difficulty}"는 난이도가 아님 (${DIFFICULTIES.join('·')})`);

  if (Object.hasOwn(f, 'name')) {
    if (!r.name.trim()) err('name', 'name이 비어 있음');
    else if (len(r.name) > LIMITS.nameMax) err('name', `name ${len(r.name)}자 > ${LIMITS.nameMax}자 (DB 한도)`);
    else if (len(r.name) > LIMITS.nameWarn) warn('name', `name ${len(r.name)}자 > 권장 ${LIMITS.nameWarn}자`);
  }
  if (Object.hasOwn(card.sections, 'summary')) {
    const n = len(r.summary);
    const [lo, hi] = LIMITS.summaryWarn;
    if (!r.summary.trim()) err('summary', 'summary가 비어 있음');
    else if (n > LIMITS.summaryMax) err('summary', `summary ${n}자 > ${LIMITS.summaryMax}자 (DB 한도)`);
    else if (n < lo || n > hi) warn('summary', `summary ${n}자, 권장 ${lo}~${hi}자 밖`);
  }
  if (Object.hasOwn(card.sections, 'body')) {
    const n = len(r.body);
    const [lo, hi] = LIMITS.bodyWarn;
    if (!r.body.trim()) err('body', 'body가 비어 있음');
    else if (bytes(r.body) > LIMITS.textMaxBytes) err('body', `body ${bytes(r.body)}바이트 > ${LIMITS.textMaxBytes}바이트 (DB 한도 TEXT)`);
    else if (n < lo || n > hi) warn('body', `body ${n}자, 권장 ${lo}~${hi}자 밖`);
  }
  // 수준별 선택 절 (K-1 AC13): 쓰면 비어 있지 않아야 하고 DB 한도(요약 VARCHAR(300), 본문은 관리자 API와 같은 10,000자) 안이어야 한다
  for (const [section, column] of Object.entries(LEVEL_SECTIONS)) {
    if (!Object.hasOwn(card.sections, section)) continue;
    const v = r[column];
    const max = column.startsWith('summary') ? LIMITS.levelSummaryMax : LIMITS.levelBodyMax;
    if (v == null || !v.trim()) err(section, `## ${section} 절이 비어 있음 (쓰지 않을 거면 절을 지운다)`);
    else if (len(v) > max) err(section, `${section} ${len(v)}자 > ${max}자`);
  }
  if (Object.hasOwn(card.sections, 'example')) {
    if (!card.blocks.length || !r.example_code.trim()) err('example', '예제 코드 블록이 없거나 비어 있음');
    for (const b of card.blocks) {
      if (!b.lang) err('example', `${b.line}번 줄 코드 블록에 언어 태그가 없음`);
      else if (!LANGS.has(b.lang)) err('example', `${b.line}번 줄 언어 태그 "${b.lang}"는 허용 목록 밖 (${[...LANGS].join(' ')})`);
    }
    if (card.outside.length) err('example', `example 섹션의 코드 블록 밖에 텍스트가 있음 (${card.outside.join(', ')}번 줄, DB에 들어가지 않는다)`);
    if (bytes(r.example_code) > LIMITS.textMaxBytes) err('example', `예제 ${bytes(r.example_code)}바이트 > ${LIMITS.textMaxBytes}바이트 (DB 한도 TEXT)`);
    const lines = r.example_code ? r.example_code.split('\n').length : 0;
    if (lines > LIMITS.exampleLinesWarn) warn('example', `예제 ${lines}줄 > 권장 ${LIMITS.exampleLinesWarn}줄`);
  }
  if (Object.hasOwn(f, 'reference')) {
    let url = null;
    try {
      url = new URL(r.reference_url);
    } catch {
      url = null;
    }
    if (len(r.reference_url) > LIMITS.referenceMax) err('reference', `reference ${len(r.reference_url)}자 > ${LIMITS.referenceMax}자 (DB 한도)`);
    else if (!url || url.protocol !== 'https:') err('reference', `reference "${r.reference_url}"가 https:// 주소가 아님`);
    else if (!rules.hosts.has(url.host)) err('reference', `reference 호스트 ${url.host}는 DS-01 출처가 아님 (data/datasets.json)`);
  }
}

/**
 * 카드 전체의 ①형식 검사. file은 저장소 기준 상대 경로여야 한다 (rules.src 아래).
 * @returns {{file, stage: '①', severity: 'error'|'warn', item, message}[]} 파일 경로순
 */
export function checkFormat(cards, rules) {
  const out = [];
  const adder = (file) => (severity, item, message) => out.push({ file, stage: '①', severity, item, message });
  for (const card of cards) checkCard(card, rules, adder(card.file));

  // 이름 충돌: 콜레이션상 같으면 한 INSERT 안에서 앞 행이 조용히 덮어써진다 (AC5)
  const named = cards.map((c) => ({ c, name: toRow(c).name })).filter((x) => x.name.trim());
  for (let i = 0; i < named.length; i++) {
    for (let j = i + 1; j < named.length; j++) {
      const a = named[i];
      const b = named[j];
      if (aiCiKey(a.name) === aiCiKey(b.name)) {
        adder(a.c.file)('error', 'name', `이름 "${a.name}"이 ${b.c.file}의 "${b.name}"과 DB에서 같은 값 (대소문자·악센트 무시)`);
        adder(b.c.file)('error', 'name', `이름 "${b.name}"이 ${a.c.file}의 "${a.name}"과 DB에서 같은 값 (대소문자·악센트 무시)`);
      } else if (looseKey(a.name) && looseKey(a.name) === looseKey(b.name)) {
        adder(a.c.file)('warn', 'name', `이름 "${a.name}"이 ${b.c.file}의 "${b.name}"과 공백·기호만 다름`);
        adder(b.c.file)('warn', 'name', `이름 "${b.name}"이 ${a.c.file}의 "${a.name}"과 공백·기호만 다름`);
      }
    }
  }

  // 비슷한 요약 (AC6): 쌍마다 한 번, 앞 파일에 남긴다
  const grams = cards.map((c) => ({ c, g: trigrams(toRow(c).summary) })).filter((x) => toRow(x.c).summary.trim());
  for (let i = 0; i < grams.length; i++) {
    for (let j = i + 1; j < grams.length; j++) {
      const score = jaccard(grams[i].g, grams[j].g);
      if (score >= LIMITS.similarSummary) {
        adder(grams[i].c.file)('warn', 'summary', `요약이 ${grams[j].c.file}과 비슷함 (문자 3-gram Jaccard ${score.toFixed(2)})`);
      }
    }
  }

  return out.sort((a, b) => (a.file < b.file ? -1 : a.file > b.file ? 1 : 0));
}
