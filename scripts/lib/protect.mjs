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
  // GMS 키는 형식이 공개되지 않아 변수 할당으로 잡는다. 빈 값·${…} 참조·<설명>은 통과
  [/\bGMS_API_KEY\s*[=:]\s*['"]?(?![$<'"\s])[^\s'"#]{16,}/i, 'GMS 키'],
];

/** 글 안의 비밀값 종류 목록 [{ label }] (같은 종류는 한 번) */
export function findSecrets(text) {
  const s = String(text ?? '');
  return SECRET_PATTERNS.filter(([re]) => re.test(s)).map(([, label]) => ({ label }));
}

/** `git diff --cached` 출력에서 추가된 줄의 비밀값 → [{ file, label }] (파일·종류마다 한 번) */
export function scanDiffForSecrets(diff) {
  const found = [];
  const seen = new Set();
  let file = null;
  for (const line of String(diff || '').split('\n')) {
    if (line.startsWith('+++ ')) file = line.slice(4).replace(/^b\//, '');
    else if (line.startsWith('+') && file)
      for (const { label } of findSecrets(line.slice(1))) {
        const key = `${file}\0${label}`;
        if (seen.has(key)) continue;
        seen.add(key);
        found.push({ file, label });
      }
  }
  return found;
}

/** 비밀 파일 이름인가: .env 계열(.env.example 제외)과 키 파일. 경로의 \와 /를 모두 구분자로 본다 */
export function isSecretFile(path) {
  const b = basename(String(path ?? '').replace(/\\/g, '/'));
  if (/^\.env([.*][\w.*-]*)?$/.test(b)) return b !== '.env.example';
  return /\.(pem|key|p12|pfx|jks|keystore)$/i.test(b) || /^id_(rsa|dsa|ecdsa|ed25519)$/.test(b);
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
