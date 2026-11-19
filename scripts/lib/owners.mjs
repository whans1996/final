// 영역 경계: .github/CODEOWNERS가 정본이다 (11-19 결정).
// 두 사람이 병렬로 개발하므로 파일마다 내 영역·상대 영역·공통인지 판정해 guard·hook·PR 설명에서 쓴다.
// GitHub CODEOWNERS 문법의 하위 집합(gitignore식 패턴, 뒤에 나온 규칙이 이긴다)만 지원한다.
import { join } from 'node:path';
import { ROOT, ownerKey, readText } from './common.mjs';

export const CODEOWNERS_FILE = join(ROOT, '.github', 'CODEOWNERS');

/** 패턴 → 정규식. `/`로 시작하면 루트 기준, 중간에 `/`가 없으면 어느 깊이든, 끝이 `/`이면 그 아래 전부 */
export function patternToRegex(pattern) {
  let p = pattern.trim();
  const dirOnly = p.endsWith('/');
  if (dirOnly) p = p.slice(0, -1);
  const anchored = p.includes('/');
  if (p.startsWith('/')) p = p.slice(1);
  let re = '';
  for (let i = 0; i < p.length; i++) {
    const c = p[i];
    if (c === '*' && p[i + 1] === '*') {
      // `**/` = 0개 이상의 디렉터리, 끝의 `**` = 그 아래 전부
      if (p[i + 2] === '/') {
        re += '(?:.*/)?';
        i += 2;
      } else {
        re += '.*';
        i += 1;
      }
    } else if (c === '*') re += '[^/]*';
    else if (c === '?') re += '[^/]';
    else re += c.replace(/[.+^${}()|[\]\\]/g, '\\$&');
  }
  return new RegExp(`${anchored ? '^' : '(?:^|/)'}${re}${dirOnly ? '/' : '(?:$|/)'}`);
}

/** CODEOWNERS 본문 → [{ pattern, re, owners: ['whans1996', ...] }] (주석·빈 줄 제외) */
export function parseCodeowners(text) {
  return String(text || '')
    .split(/\r?\n/)
    .map((l) => l.replace(/\s#.*$/, '').trim())
    .filter((l) => l && !l.startsWith('#'))
    .map((l) => {
      const [pattern, ...owners] = l.split(/\s+/);
      return { pattern, re: patternToRegex(pattern), owners: owners.map((o) => o.replace(/^@/, '')) };
    });
}

/** 저장소 기준 경로의 소유자 github 계정 목록. 마지막으로 일치한 규칙이 이긴다. 없으면 [] */
export function ownersOf(path, rules) {
  const p = path.replace(/\\/g, '/').replace(/^\.\//, '');
  let owners = [];
  for (const r of rules) if (r.re.test(p)) owners = r.owners;
  return owners;
}

/** pcs.json → { github(소문자): { key, owner } } */
export function accountsOf(pcs) {
  const map = {};
  for (const [id, pc] of Object.entries(pcs || {})) {
    if (pc?.github && ownerKey(id)) map[pc.github.toLowerCase()] = { key: ownerKey(id), owner: pc.owner, github: pc.github };
  }
  return map;
}

/** 사람 key(dh/js) → { owner, github } */
export function personOf(key, pcs) {
  return Object.values(accountsOf(pcs)).find((a) => a.key === key) || null;
}

/** 파일 영역: mine(나만) | peer(상대만) | common(둘 다) | none(지정 없음) */
export function areaOf(path, myKey, rules, pcs) {
  const accounts = accountsOf(pcs);
  const keys = new Set(ownersOf(path, rules).map((o) => accounts[o.toLowerCase()]?.key).filter(Boolean));
  if (!keys.size) return 'none';
  if (keys.size > 1) return 'common';
  return keys.has(myKey) ? 'mine' : 'peer';
}

/** 저장소의 CODEOWNERS 규칙 (없으면 []) */
export function loadOwnerRules() {
  return parseCodeowners(readText(CODEOWNERS_FILE));
}
