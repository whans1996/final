// codex-review.mjs의 판정 규칙 중 GitHub 호출 없이 계산하는 부분 (scripts/test에서 검사한다)

/** 재리뷰 없이 넘어가도 되는 문서 경로. openapi.json은 API 계약이라 제외 */
export const isDocPath = (p) => !!p && p.startsWith('docs/') && p !== 'docs/api/openapi.json';

/**
 * 리뷰 기준 커밋 이후 바뀐 파일이 "문서만"인가.
 * fromDevelop(path): head의 그 파일 내용이 develop에서 그대로 들어온 것이면 true.
 * develop 합치기로 들어온 상대 코드는 이 PR의 변경이 아니어서 재리뷰 대상이 아니다 (11-19 병렬 전환).
 */
export function onlyDocsChanged(changedFiles, fromDevelop = () => false) {
  if (!changedFiles.length) return false;
  return changedFiles.every((f) => isDocPath(f) || fromDevelop(f));
}

/** 아직 해결되지 않은 마지막 인라인 지적 시각(ISO). 그보다 늦은 이관 마커가 있으면 '' (모두 이슈로 이관됨) */
export function unresolvedFindingAt(findingTimes, resolvedTimes) {
  const last = [...findingTimes].sort().at(-1) ?? '';
  const resolved = [...resolvedTimes].sort().at(-1) ?? '';
  return last && resolved > last ? '' : last;
}

export const CODEX_BOT = 'chatgpt-codex-connector';
const isBot = (c) => !!c.user?.login?.startsWith(CODEX_BOT);
/** 리뷰 대신 설정 안내만 남기는 봇 댓글: 저장소 환경 없음, 요청한 GitHub 계정이 Codex에 연결되지 않음 (11-19 PR #30) */
const NOTICES = [
  { kind: 'env', re: /create an environment for this repo\]\((https?:[^)\s]+)\)/ },
  { kind: 'account', re: /create a Codex account and connect to github\]\((https?:[^)\s]+)\)/i },
];
const noticeOf = (body = '') => NOTICES.map((n) => ({ kind: n.kind, url: n.re.exec(body)?.[1] })).find((n) => n.url);
export const REQUEST_MARK_RE = /<!--\s*codex-review-request:\s*([0-9a-f]{40})\s*-->/;

/**
 * 마지막 봇 활동이 설정 안내이면 { kind: 'env'|'account', url, at, who }. 그 뒤 다른 봇 댓글이 달리거나 갱신됐으면 null.
 * who: 안내를 부른 "@codex review" 작성자 (계정 안내는 그 사람에게 해당한다). 그 요청과 안내 사이에 다른 봇 댓글이 있으면
 *      요청이 계기가 아니므로 null이다 (PR 작성자의 자동 리뷰가 부른 안내 → 호출하는 쪽이 PR 작성자로 채운다, reviewer).
 * comments는 이슈 댓글(작성 순서).
 */
export function setupNotice(comments) {
  const bot = comments.filter(isBot);
  const notices = bot.filter((c) => noticeOf(c.body));
  if (!notices.length) return null;
  const last = notices.at(-1);
  const otherAt = bot
    .filter((c) => !noticeOf(c.body))
    .map((c) => c.updated_at || c.created_at)
    .sort()
    .at(-1);
  if (otherAt && otherAt >= last.created_at) return null;
  const floor = bot.filter((c) => c !== last && c.created_at <= last.created_at).map((c) => c.created_at).sort().at(-1) ?? '';
  const who = comments
    .filter((c) => !isBot(c) && /@codex\s+review/i.test(c.body || '') && c.created_at <= last.created_at && c.created_at > floor)
    .at(-1)?.user?.login;
  return { ...noticeOf(last.body), at: last.created_at, who: who ?? null };
}

/** 재리뷰 요청 횟수 (사람 댓글의 head sha 마커, 서로 다른 커밋 수) */
export function requestCount(comments) {
  const shas = comments.filter((c) => !isBot(c)).map((c) => REQUEST_MARK_RE.exec(c.body || '')?.[1]).filter(Boolean);
  return new Set(shas).size;
}

// ── Stop hook codex-loop의 판정 (GitHub 호출 없는 부분) ──
export const MAX_REVIEW_ROUNDS = 5;

/**
 * 다시 확인하지 않아도 되는가. prev = { head(로컬 HEAD), prHead(판정한 PR head 7자), verdict, at }.
 * COMPLETE는 그 판정의 PR head가 지금 로컬 HEAD일 때만 확정으로 본다: 커밋만 하고 push 전에 판정하면
 * PR head는 이전 커밋이라, push 뒤의 STALE을 놓치지 않게 한다 (reviewer). 그 밖에는 recheckMs마다 다시 본다.
 */
export function skipRecheck(prev, head, now, recheckMs) {
  if (!prev || prev.head !== head) return false;
  if (prev.verdict === 'COMPLETE' && prev.prHead && head.startsWith(prev.prHead)) return true;
  return now - (prev.at || 0) < recheckMs;
}

/**
 * transcript(JSONL)에 이 PR의 --wait를 백그라운드로 돌린 Bash 호출이 있는가.
 * hook이 남긴 차단 문구에도 같은 명령이 들어 있으므로 tool_use의 command만 본다 (reviewer).
 */
export function waitingInTranscript(text, pr) {
  const re = new RegExp(`codex-review\\.mjs\\s+${pr}\\b.*--wait`);
  for (const line of (text || '').split('\n')) {
    if (!line.includes('"tool_use"')) continue;
    let o;
    try {
      o = JSON.parse(line);
    } catch {
      continue;
    }
    for (const c of o.message?.content || []) {
      if (c?.type === 'tool_use' && c.input?.run_in_background && re.test(c.input.command || '')) return true;
    }
  }
  return false;
}

/**
 * codex-review.mjs 첫 줄로 다음 동작을 정한다. (PR, head, 판정)마다 한 번만 반응한다 (handled에 key가 있으면 none).
 * action: 'block'(Claude가 이어서 할 일, kind: request|wait|fix) | 'notify'(사람에게, kind: limit|setup|error) | 'none'
 */
export function loopAction({ line, pr, handled = [], waiting = false }) {
  const verdict = (line || '').split(' ')[0];
  const rounds = Number(/재리뷰 (\d+)회/.exec(line)?.[1] ?? 0);
  const prHead = /head ([0-9a-f]{7})/.exec(line)?.[1] ?? null;
  const key = `${pr}:${prHead}:${verdict}`;
  const out = (action, kind) => ({ verdict, rounds, prHead, key, action, kind });
  if (handled.includes(key)) return out('none');
  if (verdict === 'STALE') return out('block', 'request');
  if (verdict === 'RUNNING') return waiting ? out('none') : out('block', 'wait');
  if (verdict === 'FINDINGS') return rounds >= MAX_REVIEW_ROUNDS ? out('notify', 'limit') : out('block', 'fix');
  if (verdict === 'ERROR') return out('notify', /계정 미연결|환경 없음/.test(line) ? 'setup' : 'error');
  return out('none');
}
