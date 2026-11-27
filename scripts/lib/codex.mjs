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

/**
 * codex-loop가 이 PR을 보지 않는 이유 (gh pr view --json state,isDraft,files). 볼 PR이면 null.
 * docs만 바꾼 PR은 CI 초록이면 Codex를 기다리지 않고 머지한다 (docs/conventions.md 7절 예외, #129)
 */
export function loopSkipReason(pr) {
  if (pr.state !== 'OPEN') return 'CLOSED';
  if (pr.isDraft) return 'DRAFT';
  if (onlyDocsChanged((pr.files || []).map((f) => f.path))) return 'DOCS';
  return null;
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

/**
 * 재리뷰 요청 횟수 (사람 댓글의 head sha 마커, 서로 다른 커밋 수).
 * 실패·정체 재시도 댓글은 세지 않는다: 자동 리뷰(마커 없음)가 실패한 head를 재시도하면 1회로 세져 상한이 줄었다 (reviewer)
 */
export function requestCount(comments) {
  const shas = comments
    .filter((c) => !isBot(c) && !RETRY_MARK_RE.test(c.body || ''))
    .map((c) => REQUEST_MARK_RE.exec(c.body || '')?.[1])
    .filter(Boolean);
  return new Set(shas).size;
}

// ── Stop hook codex-loop의 판정 (GitHub 호출 없는 부분) ──
export const MAX_REVIEW_ROUNDS = 5;

/**
 * 같은 head의 실패·정체로 재시도 표시를 붙여 재요청할 판정 줄인가 (judgeReview의 retry: true, "… → 재요청").
 * 이전 커밋 리뷰의 실패·정체는 "… → head 재요청"이라 아니다. codex-loop가 재요청 안내 문구를 고를 때 쓴다
 */
export const isRetryLine = (line) => /^STALE .*(실패|정체).*→ 재요청/.test(line || '');

/**
 * GitHub의 PR head가 방금 push한 커밋을 아직 반영하지 못했는가 (#235).
 * pushed = push로 갱신된 origin/<headRefName>. PR head가 pushed의 조상(더 오래됨)일 때만 true다.
 * PR head가 더 새것(다른 PC가 push함)이거나 pushed를 모르면 기다리지 않는다.
 * @param isAncestor (a, b) => a가 b의 조상인가 (git merge-base --is-ancestor)
 */
export function headLagging(prSha, pushedSha, isAncestor) {
  if (!prSha || !pushedSha || prSha === pushedSha) return false;
  return isAncestor(prSha, pushedSha);
}

/**
 * 다시 확인하지 않아도 되는가. prev = { head(로컬 HEAD), prHead(판정한 PR head 7자), verdict, at, remote(판정 때 origin/<브랜치> sha) }.
 * COMPLETE는 그 판정의 PR head가 지금 로컬 HEAD일 때만 확정으로 본다: 커밋만 하고 push 전에 판정하면
 * PR head는 이전 커밋이라, push 뒤의 STALE을 놓치지 않게 한다 (reviewer). 그 밖에는 recheckMs마다 다시 본다.
 * remote(지금 origin/<브랜치> sha)가 판정 때와 다르면 push가 있었으므로 바로 다시 본다: 커밋한 턴 뒤 3분 안에 push하고 멈추면 STALE을 놓쳤다 (#178)
 */
export function skipRecheck(prev, head, now, recheckMs, remote = null) {
  if (!prev || prev.head !== head) return false;
  if (remote && prev.remote !== remote) return false;
  if (prev.verdict === 'COMPLETE' && prev.prHead && head.startsWith(prev.prHead)) return true;
  return now - (prev.at || 0) < recheckMs;
}

/**
 * transcript(JSONL)에 아직 끝나지 않은, 이 PR의 --wait 백그라운드 Bash 호출이 있는가.
 * hook이 남긴 차단 문구에도 같은 명령이 들어 있으므로 tool_use의 command만 본다 (reviewer).
 * 끝난 호출(완료 알림 <tool-use-id>가 transcript에 있음)은 세지 않는다: 이전 head의 --wait가 남아 있어도
 * 새 리뷰를 기다리는 프로세스로 오인하지 않게 한다 (Codex 리뷰).
 */
export function waitingInTranscript(text, pr) {
  const re = new RegExp(`codex-review\\.mjs\\s+${pr}\\b.*--wait`);
  const raw = text || '';
  const ids = [];
  for (const line of raw.split('\n')) {
    if (!line.includes('"tool_use"')) continue;
    let o;
    try {
      o = JSON.parse(line);
    } catch {
      continue;
    }
    for (const c of o.message?.content || []) {
      if (c?.type === 'tool_use' && c.input?.run_in_background && re.test(c.input.command || '')) ids.push(c.id);
    }
  }
  return ids.some((id) => id && !raw.includes(`<tool-use-id>${id}</tool-use-id>`));
}

/**
 * codex-review.mjs 첫 줄로 다음 동작을 정한다. (PR, head, 판정)마다 한 번만 반응한다 (handled에 key가 있으면 none).
 * action: 'block'(Claude가 이어서 할 일, kind: request|wait|fix) | 'notify'(사람에게, kind: limit|setup|error) | 'none'
 */
export function loopAction({ line, pr, handled = [], waiting = false }) {
  const verdict = (line || '').split(' ')[0];
  const rounds = Number(/재리뷰 (\d+)회/.exec(line)?.[1] ?? 0);
  const prHead = /head ([0-9a-f]{7})/.exec(line)?.[1] ?? null;
  // 실패·정체 재요청(STALE)은 같은 head의 일반 STALE에 이미 반응했어도 한 번 더 반응한다 (#48)
  const key = `${pr}:${prHead}:${verdict}${verdict === 'STALE' && /실패|정체/.test(line) ? ':retry' : ''}`;
  const out = (action, kind) => ({ verdict, rounds, prHead, key, action, kind });
  if (handled.includes(key)) return out('none');
  // 실패·정체 재요청도 기다리는 프로세스와 무관하게 막는다. 그 프로세스가 재시도할 수 있는지(--request, 같은 head) hook이
  // 확실히 알 수 없고, 맡겼다가 못 하면 key가 처리됨으로 남아 루프가 멈춘다 (PR #69 Codex 리뷰 2회).
  // 대가: 두 대기 프로세스가 같은 30초 안에 판정하면 재시도 댓글이 겹칠 수 있다 (판정에는 영향 없음)
  if (verdict === 'STALE') return out('block', 'request');
  if (verdict === 'RUNNING') return waiting ? out('none') : out('block', 'wait');
  if (verdict === 'FINDINGS') return rounds >= MAX_REVIEW_ROUNDS ? out('notify', 'limit') : out('block', 'fix');
  if (verdict === 'ERROR') return out('notify', /계정 미연결|환경 없음/.test(line) ? 'setup' : 'error');
  return out('none');
}

// ── codex-review.mjs의 판정 (GitHub·git 호출은 인자로 받는다, #67) ──
const STATE_RE = /<!--\s*codex-review-state:v1\s*(\{.*?\})\s*-->/s;
// 요약 댓글 표의 Code Review 행 시각: 그 상태가 된 시각 (진행 중이면 시작 시각)
const STATUS_AT_RE = /Code Review[^\n]*?<relative-time datetime="([^"]+)"/;
const RESOLVED_RE = /<!--\s*codex-findings-resolved:\s*([0-9a-f]{40})\s*-->/;
const CLEAN_RECEIPT_RE = /codex-review-receipt[\s\S]*Didn't find any major issues[\s\S]*?Reviewed commit:\*{0,2}\s*`([0-9a-f]{7,40})`/;
/** 실패·정체 뒤 재요청 표시. 같은 head의 재시도는 재리뷰 횟수(requestCount)를 늘리지 않는다 */
export const RETRY_MARK_RE = /<!--\s*codex-review-retry:\s*([0-9a-f]{40})\s*-->/;
/** 이만큼 지나도 리뷰가 끝나지 않거나 요청 뒤 시작하지 않으면 정체로 본다 (PR #66: 21분 running, --wait 20분 TIMEOUT) */
export const STUCK_MS = 15 * 60_000;
/** 끝났지만 결과가 없는 상태 (PR #47 failed). 그 밖의 completed 아닌 값은 진행 중으로 본다 */
const FAILED_RE = /^(failed|error|errored|cancell?ed|timed_out|skipped)$/;

/** 요약 댓글의 code_review 상태 + 댓글 갱신 시각(at) + 그 상태가 된 시각(since, 없으면 at) */
export function codexState(comments) {
  for (const c of comments) {
    if (!isBot(c)) continue;
    const m = STATE_RE.exec(c.body || '');
    if (m) {
      try {
        const cr = JSON.parse(m[1]).code_review;
        const at = c.updated_at || c.created_at;
        const statusAt = STATUS_AT_RE.exec(c.body || '')?.[1];
        const since = statusAt && !Number.isNaN(Date.parse(statusAt)) ? new Date(statusAt).toISOString() : at;
        return cr ? { ...cr, at, since } : null;
      } catch {
        return null;
      }
    }
  }
  return null;
}

/** "지적 없음" 영수증 댓글 목록 { sha(7~40자), at } (댓글 순서) */
export const cleanReceipts = (comments) =>
  comments
    .filter(isBot)
    .map((c) => ({ sha: CLEAN_RECEIPT_RE.exec(c.body || '')?.[1], at: c.created_at }))
    .filter((r) => r.sha);

/** 사람이 단 "지적을 이슈로 이관함" 마커 [{ sha, at }] */
export const resolvedMarks = (comments) =>
  comments
    .filter((c) => !isBot(c))
    .map((c) => ({ sha: RESOLVED_RE.exec(c.body || '')?.[1], at: c.created_at }))
    .filter((m) => m.sha);

/** 사람이 단 재리뷰 요청 [{ sha, at, retry }] (봇의 안내문에도 "@codex review"가 들어 있으므로 사람 댓글만) */
const requests = (comments) =>
  comments
    .filter((c) => !isBot(c))
    .map((c) => ({ sha: REQUEST_MARK_RE.exec(c.body || '')?.[1], at: c.created_at, retry: RETRY_MARK_RE.test(c.body || '') }))
    .filter((r) => r.sha);

/** 문서만 변경 판정의 기준 후보 커밋 (늦은 영수증부터). 마지막 미해결 인라인 지적(findingAt)보다 늦은 "지적 없음"(또는 이관 완료) 결과만 */
function cleanBases(st, stOk, receipts, resolved, findingAt) {
  const fresh = [...receipts, ...resolved]
    .filter((r) => r.at > findingAt)
    .sort((a, b) => (a.at < b.at ? 1 : -1))
    .map((r) => r.sha);
  const stFresh = stOk && st.at > findingAt ? [st.commit_sha] : [];
  return [...new Set([...fresh, ...stFresh])];
}

const minutes = (ms) => Math.floor(ms / 60_000);

/**
 * PR의 Codex 리뷰 판정 → { verdict, msg, retry?, envMissing? }. msg는 "head <7자>"로 시작한다 (codex-loop가 읽는다).
 * h = { sha, state, author }, comments = 이슈 댓글(작성 순서), findingTimes() = 봇 인라인 지적 시각(필요할 때만 조회),
 * docsOnlySince(base) = base 이후 head까지 이 PR 자신의 변경이 문서뿐인가(git, 필요할 때만).
 * prDocsOnly() = PR 전체(develop merge-base 대비)가 문서뿐인가(git, 필요할 때만, #165).
 * retry: 같은 head의 리뷰가 실패·정체해 재시도 표시를 붙여 다시 요청해야 한다. 재시도 뒤 또 실패·정체하면 ERROR (#48).
 */
export function judgeReview({ pr, h, comments, findingTimes = () => [], docsOnlySince = () => false, prDocsOnly = () => false, now = Date.now() }) {
  if (h.state !== 'OPEN') return { verdict: 'CLOSED', msg: `PR #${pr}이 ${h.state} 상태 (Codex 루프 불필요)` };
  const short = h.sha.slice(0, 7);
  const st = codexState(comments);
  const receipts = cleanReceipts(comments);
  const resolved = resolvedMarks(comments);
  const reqs = requests(comments);
  const clean = receipts.some((r) => h.sha.startsWith(r.sha));
  // 인라인 지적 조회는 필요할 때만 한다
  let times;
  const allFindings = () => (times ??= findingTimes());
  const lastAny = () => [...allFindings()].sort().at(-1) ?? '';
  // 상태 댓글의 has_any_findings는 이전 커밋의 미해결 스레드까지 누적한다. Codex는 한 리뷰에 인라인 지적과
  // "지적 없음" 영수증 중 하나만 남기므로, 모든 인라인 지적보다 늦은 그 커밋의 영수증이 있으면 지적 없음이다 (PR #97)
  const receiptAfterFindings = (sha) => receipts.some((r) => sha.startsWith(r.sha) && r.at > lastAny());
  const stClean =
    st?.status === 'completed' && (!st.has_any_findings || receiptAfterFindings(st.commit_sha));
  // 지적이 있었지만 그 커밋의 남은 지적을 모두 이슈로 넘긴 상태 (pr-review 8-3 이관 완료).
  // 마커 뒤에 새 지적이 달렸으면 이관 완료가 아니다
  const stMarks = resolved.filter((m) => m.sha === st?.commit_sha);
  const stResolved = st?.status === 'completed' && stMarks.length > 0 && stMarks.some((m) => m.at > lastAny());
  if (st?.commit_sha === h.sha && stClean) return { verdict: 'COMPLETE', msg: `head ${short} 지적 없음 (상태 댓글)` };
  if (st?.commit_sha === h.sha && stResolved) return { verdict: 'COMPLETE', msg: `head ${short} 남은 지적 이관 완료 (--resolve)` };
  if (clean && !(st?.commit_sha === h.sha && st.status === 'completed')) {
    return { verdict: 'COMPLETE', msg: `head ${short} 지적 없음 (영수증 댓글)` };
  }
  // 문서만 바꾼 PR은 Codex 루프를 생략하고 CI 초록이면 머지한다 (conventions 7절, #129). 리뷰가 없거나 실패해도
  // "Codex" 체크가 ⏳로 남아 오류처럼 보이지 않게 COMPLETE로 둔다 (#165). head 리뷰가 지적을 남겼으면 사람이 읽고 판단하므로 FINDINGS 그대로
  const headReviewed = st?.commit_sha === h.sha && st.status === 'completed';
  if (!headReviewed && prDocsOnly()) return { verdict: 'COMPLETE', msg: `head ${short} 문서만 PR, Codex 생략 (conventions 7절)` };

  /**
   * target 커밋의 리뷰가 진행 중이거나 요청을 기다리는 중일 때의 판정 (요청도 진행도 없으면 null).
   * 실패하거나 STUCK_MS를 넘기면 재요청(STALE), 이미 재시도했으면 ERROR.
   * target이 head가 아니면(#39 문서만 push) 재시도 대신 head로 새로 요청한다.
   */
  const inFlight = (target, label = '') => {
    const onHead = target === h.sha;
    const prefix = onHead ? `head ${short}` : `head ${short} ${label}, ${target.slice(0, 7)}`;
    const mine = reqs.filter((r) => r.sha === target);
    const lastReq = mine.map((r) => r.at).sort().at(-1) ?? null;
    const retried = onHead && mine.some((r) => r.retry);
    // kind: 실패|정체 (codex-loop가 이 낱말로 일반 STALE과 구분한다), detail: 사람이 읽을 근거
    const stuck = (kind, detail) => {
      if (retried) return { verdict: 'ERROR', msg: `${prefix} Codex 리뷰 ${kind}가 재시도 뒤에도 반복 (${detail}) → 사람이 Codex 상태를 확인` };
      return onHead
        ? { verdict: 'STALE', retry: true, msg: `${prefix} Codex 리뷰 ${detail} → 재요청` }
        : { verdict: 'STALE', msg: `${prefix} 리뷰 ${detail} → head 재요청` };
    };
    const waitReq = () => {
      const age = now - Date.parse(lastReq);
      if (age > STUCK_MS) return stuck('정체', `요청 ${lastReq} 뒤 ${minutes(age)}분 지나도 시작 안 함 (정체)`);
      return { verdict: 'RUNNING', msg: onHead ? `${prefix} 재리뷰 요청함 (${lastReq}), 시작 대기` : `${prefix} 리뷰 대기 (요청 ${lastReq})` };
    };
    if (st?.commit_sha === target && st.status !== 'completed') {
      // 실패·정체한 뒤에 단 요청이면 다시 시작하기를 기다린다
      // 시각 형식이 달라(…:46Z 대 …:46.657Z) 문자열이 아니라 시각으로 비교한다 (reviewer)
      if (lastReq && Date.parse(lastReq) > Date.parse(st.since)) return waitReq();
      if (FAILED_RE.test(st.status)) return stuck('실패', `실패(${st.status})`);
      const age = now - Date.parse(st.since);
      if (age > STUCK_MS) return stuck('정체', `${minutes(age)}분째 ${st.status} (정체)`);
      return { verdict: 'RUNNING', msg: `${prefix} 리뷰 ${st.status}` };
    }
    return lastReq ? waitReq() : null;
  };

  if (!st || st.commit_sha !== h.sha) {
    // 마지막 리뷰가 지적 없이(또는 이관 완료로) 끝났을 때만(상태 댓글이 없으면 영수증으로) 기준 커밋을 잡는다.
    // 지적이 남았거나 리뷰 중이면 영수증으로 건너뛰어 그 결과를 덮지 않는다.
    // 영수증·상태 댓글 뒤에 미해결 인라인 지적이 달렸으면 그것은 마지막 리뷰가 아니므로 쓰지 않는다.
    // 상태 댓글도 갱신이 늦을 수 있어 같은 기준을 적용한다 (Codex 리뷰)
    // 상태 댓글이 지적 있던 이전 커밋에 머물러 있어도 그보다 늦은 영수증은 더 뒤의 리뷰 결과다 (#198, cleanBases가 지적 시각으로 거른다)
    const receiptAfterState = !!st && receipts.some((r) => r.at > st.at);
    if (!st || stClean || stResolved || receiptAfterState) {
      const findingAt = unresolvedFindingAt(allFindings(), resolved.map((m) => m.at));
      const base = cleanBases(st, stClean || stResolved, receipts, resolved, findingAt).find((b) => docsOnlySince(b));
      if (base) return { verdict: 'COMPLETE', msg: `head ${short} 문서만 변경 (Codex ✅ ${base.slice(0, 7)} 이후)` };
    }
    // 환경·계정 안내가 이 head에 대한 마지막 요청보다 늦으면 기다려도 리뷰가 오지 않는다.
    // --request로 시작하면 설정을 마친 뒤의 재시도로 보고 요청을 새로 단다 (Codex 리뷰)
    const notice = setupNotice(comments);
    const reqAt = reqs.filter((r) => r.sha === h.sha).map((r) => r.at).sort().at(-1) ?? null;
    if (notice && (!reqAt || notice.at > reqAt)) {
      const msg =
        notice.kind === 'account'
          ? `Codex 계정 미연결 (@${notice.who ?? h.author ?? '?'}, ${notice.at}): ${notice.url} 에서 GitHub를 연결하고 Code review를 켠 뒤 --request로 다시 요청 (docs/환경설정.md 8절)`
          : `Codex 환경 없음 (${notice.at}): ${notice.url} 에서 만든 뒤 --request로 다시 요청`;
      return { verdict: 'ERROR', envMissing: true, msg };
    }
    // 이 head에 대한 요청을 이미 달았으면 Codex가 시작하기를 기다리는 중이다 (요청을 다시 달지 않는다)
    if (reqAt) return inFlight(h.sha);
    // #39: 결과를 기다리는 이전 커밋(리뷰 중이거나 요청만 함) 이후 문서만 바뀌었으면 재요청하지 않고 그 리뷰를 기다린다.
    // 그 리뷰가 지적 없이 끝나면 위 문서만 변경 규칙으로 COMPLETE, 지적이 있으면 아래 규칙으로 그 커밋의 FINDINGS가 된다
    const done = (sha) => (st?.commit_sha === sha && st.status === 'completed') || receipts.some((r) => sha.startsWith(r.sha));
    // 후보: 진행 중(또는 실패로 남은) 리뷰 커밋과 결과 없는 요청 커밋, 최근 것부터. 실패로 남은 이전 커밋 때문에
    // 더 최근 요청을 놓치지 않게 문서만 바뀐 첫 후보를 고른다 (reviewer)
    const candidates = [
      ...reqs.filter((r) => r.sha !== h.sha && !done(r.sha)),
      ...(st && st.status !== 'completed' ? [{ sha: st.commit_sha, at: st.since }] : []),
    ].sort((a, b) => Date.parse(b.at) - Date.parse(a.at));
    const pending = [...new Set(candidates.map((c) => c.sha))].find((sha) => docsOnlySince(sha));
    if (pending) {
      const r = inFlight(pending, '문서만 변경');
      if (r) return r;
    }
    // 마지막 리뷰가 지적을 남긴 커밋 뒤에 문서만 바뀌었으면 재리뷰해도 같은 코드라 그 지적이 판정이다.
    // STALE로 두면 지적을 보여 주지 않고 문서 커밋에 재리뷰를 쓴다 (PR #78)
    if (st?.status === 'completed' && st.has_any_findings && docsOnlySince(st.commit_sha)) {
      return { verdict: 'FINDINGS', findingsSha: st.commit_sha, msg: `head ${short} 문서만 변경, ${st.commit_sha.slice(0, 7)} 지적 있음` };
    }
    const seen = st?.commit_sha ? `${st.commit_sha.slice(0, 7)} 기준` : '상태 댓글 없음';
    return { verdict: 'STALE', msg: `head ${short}, Codex ${seen}` };
  }
  if (st.status !== 'completed') return inFlight(h.sha);
  return { verdict: 'FINDINGS', msg: `head ${short}` };
}
