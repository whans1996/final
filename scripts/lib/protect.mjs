// 비밀값·비밀 파일·eval 경로 판정 (H-2, HX-1, HX-2). guard-bash·guard-files hook과 commit-msg hook이 함께 쓴다.
// 판정만 하고 값은 돌려주지 않는다: 알릴 때는 파일 이름과 종류만 쓴다 (기록·로그에 비밀값이 남지 않게, #135).
import { basename, resolve } from 'node:path';

/** [정규식, 종류]. 새 형식은 여기에만 추가한다 */
export const SECRET_PATTERNS = [
  [/AIza[0-9A-Za-z_-]{35}/, 'Google(Gemini) API 키'],
  [/\bsk-(?:ant-|proj-)?[A-Za-z0-9_-]{32,}/, 'API 비밀 키(sk-)'],
  [/-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----/, '개인 키'],
  [/\bglpat-[0-9A-Za-z_-]{20,}/, 'GitLab 토큰'],
  [/\bgh[pousr]_[A-Za-z0-9]{36,}/, 'GitHub 토큰'],
  [/\bgithub_pat_[A-Za-z0-9_]{22,}/, 'GitHub 토큰(fine-grained)'],
  [/\bAKIA[0-9A-Z]{16}\b/, 'AWS 액세스 키'],
  // Mattermost(…/hooks/<26자>), Slack, Discord 웹훅. 주소만 알면 누구나 채널에 글을 쓸 수 있다
  [/https?:\/\/[^\s'"<>]+\/hooks\/[a-z0-9]{26}\b|https:\/\/hooks\.slack\.com\/services\/[A-Za-z0-9/_-]+|https:\/\/(?:canary\.|ptb\.)?discord(?:app)?\.com\/api\/webhooks\/\d+\/[\w-]+/, '웹훅 URL'],
  // GMS 키는 형식이 공개되지 않아 변수 할당으로 잡는다. 빈 값·${…} 참조·<설명>, 숫자가 없는 값(자리표시자·process.env.X)은 통과
  [/\bGMS_API_KEY\s*[=:]\s*['"]?(?![$<'"\s])(?=[^\s'"#]*\d)[^\s'"#]{16,}/i, 'GMS 키'],
];

/** 글 안의 비밀값 종류 목록 [{ label }] (같은 종류는 한 번) */
export function findSecrets(text) {
  const s = String(text ?? '');
  return SECRET_PATTERNS.filter(([re]) => re.test(s)).map(([, label]) => ({ label }));
}

/** diff 출력의 추가된 줄 → [{ file, text }] */
function addedLines(diff) {
  const out = [];
  let file = null;
  for (const line of String(diff || '').split('\n')) {
    if (line.startsWith('+++ ')) file = line.slice(4).replace(/^b\//, '');
    else if (line.startsWith('+') && file) out.push({ file, text: line.slice(1) });
  }
  return out;
}

/** 줄 목록의 비밀값 → [{ file, label }] (파일·종류마다 한 번) */
function secretsOf(lines) {
  const found = [];
  const seen = new Set();
  for (const { file, text } of lines)
    for (const { label } of findSecrets(text)) {
      const key = `${file}\0${label}`;
      if (seen.has(key)) continue;
      seen.add(key);
      found.push({ file, label });
    }
  return found;
}

/** `git diff --cached` 출력에서 추가된 줄의 비밀값 → [{ file, label }] */
export const scanDiffForSecrets = (diff) => secretsOf(addedLines(diff));

/**
 * merge 커밋: 두 부모(HEAD·MERGE_HEAD) 모두에 대해 추가인 줄(충돌 해결로 새로 쓴 줄)만 본다.
 * develop에서 들어온 줄까지 보면 develop에 걸리는 줄이 하나만 생겨도 모든 병합이 막힌다 (풀 방법이 --no-verify뿐)
 */
export function scanMergeForSecrets(diffOurs, diffTheirs) {
  const theirs = new Set(addedLines(diffTheirs).map((l) => `${l.file}\0${l.text}`));
  return secretsOf(addedLines(diffOurs).filter((l) => theirs.has(`${l.file}\0${l.text}`)));
}

/**
 * 비밀 파일 이름인가: .env 계열(.env.example 제외)과 키 파일. 경로의 \와 /를 모두 구분자로 본다.
 * NTFS는 대소문자를 무시하고 `파일::$DATA`(대체 데이터 스트림)로도 같은 파일을 읽으므로 대소문자·`:` 뒤를 무시한다
 */
export function isSecretFile(path) {
  const b = basename(String(path ?? '').replace(/\\/g, '/'))
    .replace(/:.*$/, '')
    .toLowerCase();
  if (/^\.env([.*][\w.*-]*)?$/.test(b)) return b !== '.env.example';
  return /\.(pem|key|p12|pfx|jks|keystore)$/.test(b) || /^id_(rsa|dsa|ecdsa|ed25519)$/.test(b);
}

/** 글롭 한 조각(구분자 없는 이름)의 정규식. *·?는 점으로 시작하는 이름에도 맞는다 (grep --include·rg -g·bash dotglob과 같게 보수적으로) */
function segmentRegex(glob, flags) {
  let re = '';
  let braces = 0;
  for (let i = 0; i < glob.length; i++) {
    const c = glob[i];
    if (c === '*') re += '.*';
    else if (c === '?') re += '.';
    else if (c === '[') {
      // [abc], [!abc]·[^abc]. 여는 괄호 바로 뒤의 ]는 글자 그대로다 ([]a], [!]])
      let j = i + 1;
      const neg = glob[j] === '!' || glob[j] === '^';
      if (neg) j++;
      const end = glob.indexOf(']', j + 1);
      if (end === -1) re += '\\[';
      else {
        re += `[${neg ? '^' : ''}${glob.slice(j, end).replace(/[\\\]^]/g, '\\$&')}]`;
        i = end;
      }
    } else if (c === '{') (re += '(?:'), braces++;
    else if (c === '}' && braces) (re += ')'), braces--;
    else if (c === ',' && braces) re += '|';
    else re += c.replace(/[.+^$()|\\{}]/g, '\\$&');
  }
  try {
    return new RegExp(`^${re}$`, flags);
  } catch {
    return null;
  }
}

/**
 * 이름(경로 구분자 없음)이 글롭 조각에 맞는가. 고르는 쪽(포함 글롭)은 보수적으로 대소문자를 무시하고 해석할 수 없으면 맞는다고 본다.
 * 빼는 쪽(--exclude, rg !글롭)은 strict: grep·rg처럼 대소문자를 구분하고 해석할 수 없으면 맞지 않는다고 본다 (리뷰 반영)
 */
export function globMatch(glob, name, { strict = false, icase = !strict } = {}) {
  const re = segmentRegex(String(glob ?? ''), icase ? 'i' : '');
  return re ? re.test(String(name ?? '')) : !strict;
}

/** 상대 경로가 글롭에 맞는가. /가 없는 글롭은 어느 깊이든 이름(basename)으로, 있으면 경로 전체로 맞춘다 (** 는 0개 이상의 조각) */
export function globMatchPath(glob, relPath, opts) {
  const g = String(glob ?? '').replace(/\\/g, '/');
  const parts = String(relPath ?? '')
    .replace(/\\/g, '/')
    .split('/')
    .filter((p) => p && p !== '.');
  const gs = g.split('/').filter(Boolean);
  if (gs.length <= 1 && !g.startsWith('/')) return globMatch(gs[0] ?? '', parts[parts.length - 1] ?? '', opts);
  const match = (a, b) => {
    if (!a.length) return !b.length;
    if (a[0] === '**') return match(a.slice(1), b) || (b.length > 0 && match(a, b.slice(1)));
    return b.length > 0 && globMatch(a[0], b[0], opts) && match(a.slice(1), b.slice(1));
  };
  return match(gs, parts);
}

// 실제 파일 목록이 없을 때 글롭이 비밀 파일을 고르는지 볼 대표 경로 (#188. 저장소 루트의 이름들)
const SECRET_SAMPLES = ['.env', '.env.local', '.env.production', 'server.pem', 'server.key', 'cert.p12', 'id_rsa'];

/** 검색 글롭이 비밀 파일을 고르는가. files: 검색 폴더 기준 상대 경로 목록(없으면 대표 경로). !로 시작하는 제외 글롭은 고르지 않는다 */
export function globHitsSecret(glob, files = SECRET_SAMPLES) {
  const g = String(glob ?? '').replace(/\\/g, '/');
  if (!g || g.startsWith('!')) return false;
  if (files === SECRET_SAMPLES && isSecretFile(g.split('/').filter(Boolean).pop() ?? '')) return true;
  return files.some((rel) => globMatchPath(g, rel));
}

/** 코드 안의 문자열 리터럴 ('…', "…", `…`). 인라인 코드(node -e·python -c·REPL 입력) 판정에 쓴다 */
export function stringLiterals(code) {
  const out = [];
  const re = /(['"`])((?:\\.|(?!\1)[^\\])*)\1/gs;
  for (let m; (m = re.exec(String(code ?? ''))); ) out.push(m[2].replace(/\\\\/g, '\\'));
  return out;
}

/** 인라인 코드가 비밀 파일을 읽는가: 비밀 파일 이름의 문자열, 또는 dotenv 사용 */
export function codeReadsSecret(code) {
  return /\bdotenv\b/.test(String(code ?? '')) || stringLiterals(code).some(isSecretFile);
}

/** path가 dir 안(같은 경로 포함)인가. Windows는 대소문자를 무시한다 */
export function isWithin(path, dir, platform = process.platform) {
  const norm = (p) => {
    const s = resolve(String(p)).replace(/\\/g, '/').replace(/\/+$/, '');
    return platform === 'win32' ? s.toLowerCase() : s;
  };
  const p = norm(path);
  const d = norm(dir);
  return p === d || p.startsWith(`${d}/`);
}

// ── eval 분할 (HX-2) ── 평가 데이터는 김준섭 private 저장소를 data/raw/eval/에 clone한다. 김동환(A)은 블라인드라 보지 않는다
export const EVAL_DIR_REL = 'data/raw/eval';
/** eval 기록 브랜치(`eval/…`)를 김동환 PC의 fetch에서 빼는 negative refspec (setup.mjs가 remote.origin.fetch에 추가) */
export const EVAL_FETCH_EXCLUDE = '^refs/heads/eval/*';
/** eval을 보면 안 되는 사람인가 (dh = 김동환) */
export const evalBlockedFor = (ownerKey) => ownerKey === 'dh';
/** remote.origin.fetch 값들에 eval 제외 refspec이 있는가 */
export const evalFetchExcluded = (refspecs) => (refspecs || []).includes(EVAL_FETCH_EXCLUDE);
