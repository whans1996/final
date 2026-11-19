#!/usr/bin/env node
// PR의 Codex 리뷰 상태를 판정하고, 필요하면 재리뷰를 요청해 끝날 때까지 기다린다.
// Codex는 PR마다 요약 댓글 하나를 갱신하며 그 안에 기계용 상태를 넣는다:
//   <!-- codex-review-state:v1 {"code_review":{"commit_sha":"…","has_any_findings":false,"status":"completed"}, …} -->
// 이 상태가 PR head 커밋 기준인지로 판정한다.
// 보조 신호: 수동 요청 뒤 다는 영수증 댓글 "Didn't find any major issues … Reviewed commit: `abc1234`"가
// head 커밋이면 상태 댓글이 늦게 갱신되어도 COMPLETE로 본다. 자동 리뷰 뒤의 👍 반응은 커밋 정보가 없어 쓰지 않는다.
// 문서만 바뀐 커밋: Codex가 마지막으로 지적 없이 끝낸 커밋 이후 docs/ 아래(openapi.json 제외)만 바뀌었으면
// 재리뷰를 요청하지 않고 COMPLETE로 본다 (11-18 사용자 지시). 마지막 리뷰에 지적이 있으면 적용하지 않는다.
// develop 합치기(git merge origin/develop)로 들어온 파일은 이 PR의 변경이 아니므로 문서 판정에서 뺀다 (11-19 병렬 전환).
// 이관 완료: 남은 지적을 follow-up 이슈로 넘긴 뒤 --resolve로 사람 댓글 마커를 달면, 그 시각까지의 지적은 해결된 것으로 본다.
//
// 설정 안내: Codex는 요청한 GitHub 계정(자동 리뷰는 PR 작성자)이 Codex에 연결돼 있어야 리뷰한다.
//   연결이 안 된 계정의 요청에는 "create a Codex account and connect to github" 안내만 단다 (11-19 PR #30, 김준섭 계정).
//   이 안내나 "create an environment" 안내가 마지막 요청보다 늦으면 기다려도 리뷰가 오지 않으므로 ERROR로 끝낸다.
//
// 사용법: node scripts/codex-review.mjs <PR번호> [--request] [--wait] [--resolve] [--status] [--timeout 20]
//   --request  상태가 이전 커밋 기준(STALE)이면 "@codex review" 댓글을 단다.
//              댓글에 head sha 마커를 넣어, 같은 head에 대한 요청이 이미 있으면 생략한다
//   --wait     COMPLETE·FINDINGS가 될 때까지 30초 간격으로 확인한다 (기본 20분). Claude는 run_in_background로 실행한다
//   --resolve  head가 FINDINGS일 때 "남은 지적을 이슈로 이관함" 마커 댓글을 달고 COMPLETE로 끝낸다 (pr-review 8-3)
//   --status   최종 판정을 PR head 커밋의 상태 체크 "Codex"로 남긴다 (COMPLETE ✅, FINDINGS ❌, 그 밖 ⏳·ERROR). --resolve는 항상 남긴다
//              (FINDINGS로 ❌가 된 체크를 이관 완료 뒤 ✅로 바꾸려면, Codex 리뷰)
//              브랜치 보호를 못 쓰는 저장소라 머지를 막지는 않고 PR 화면·gh pr checks에 보이게만 한다 (codex-status.yml도 쓴다)
//
// 출력 첫 줄과 종료 코드:
//   COMPLETE 0  head 커밋 리뷰 완료, 지적 없음 (또는 이관 완료, 문서만 변경)
//   FINDINGS 2  head 커밋 리뷰 완료, 지적 있음 (지적 요약을 함께 출력)
//   RUNNING  3  head 커밋 리뷰 중, 또는 head에 대한 재리뷰를 요청했고 시작을 기다리는 중
//   STALE    4  리뷰 상태가 없거나 이전 커밋 기준
//   TIMEOUT  5  --wait 시간 초과
//   CLOSED   6  PR이 이미 머지·종료됨 (루프 불필요)
//   ERROR    1  gh 실패, 또는 Codex가 마지막 요청 뒤 환경·계정 설정 안내만 남김
//               (환경을 만들거나 계정을 연결한 뒤 --request로 다시 실행하면 요청을 새로 단다)
// 첫 줄 끝에는 "(재리뷰 n회)"를 붙인다 (Stop hook codex-loop.mjs가 상한 5회를 판단한다).

import { spawnSync } from 'node:child_process';
import { NON_INTERACTIVE_GIT_ENV, git } from './lib/common.mjs';
import { CODEX_BOT, REQUEST_MARK_RE, onlyDocsChanged, requestCount, setupNotice, unresolvedFindingAt } from './lib/codex.mjs';

const CODE = { COMPLETE: 0, ERROR: 1, FINDINGS: 2, RUNNING: 3, STALE: 4, TIMEOUT: 5, CLOSED: 6 };
const BOT = CODEX_BOT;
const STATE_RE = /<!--\s*codex-review-state:v1\s*(\{.*?\})\s*-->/s;
const REQUEST_RE = REQUEST_MARK_RE;
const RESOLVED_RE = /<!--\s*codex-findings-resolved:\s*([0-9a-f]{40})\s*-->/;
const CLEAN_RECEIPT_RE = /codex-review-receipt[\s\S]*Didn't find any major issues[\s\S]*?Reviewed commit:\*{0,2}\s*`([0-9a-f]{7,40})`/;

function fail(msg) {
  console.log(`ERROR ${msg}`);
  process.exit(CODE.ERROR);
}

/** soft면 실패해도 종료하지 않고 null을 돌려준다 */
function gh(args, { soft = false } = {}) {
  // 모든 페이지를 한 번에 받으므로 기본 1MiB 버퍼로는 ENOBUFS가 날 수 있다 (Codex 리뷰)
  const r = spawnSync('gh', args, { encoding: 'utf8', windowsHide: true, maxBuffer: 64 * 1024 * 1024 });
  if (soft && (r.error || r.status !== 0)) return null;
  if (r.error) fail(`gh 실행 실패: ${r.error.message}`);
  if (r.status !== 0) fail(`gh ${args.slice(0, 2).join(' ')}: ${(r.stderr || '').trim().split('\n')[0]}`);
  return r.stdout;
}
const ghJson = (args, opts) => JSON.parse(gh(args, opts) || 'null');

const argv = process.argv.slice(2);
const pr = argv.find((a) => /^\d+$/.test(a));
if (!pr) fail('PR 번호가 필요합니다. 예: node scripts/codex-review.mjs 6 --request --wait');
const request = argv.includes('--request');
const wait = argv.includes('--wait');
const resolve = argv.includes('--resolve');
const status = argv.includes('--status');
const ti = argv.indexOf('--timeout');
const timeoutMin = ti >= 0 ? Number(argv[ti + 1]) || 20 : 20;

const remote = git(['remote', 'get-url', 'origin']).out;
const repo = remote.match(/github\.com[:/](.+?)(\.git)?$/)?.[1];
if (!repo) fail(`origin이 GitHub 저장소가 아닙니다: ${remote || '(없음)'}`);

/** PR head 커밋 */
function head() {
  const v = ghJson(['pr', 'view', pr, '--json', 'headRefOid,headRefName,state,author']);
  return { sha: v.headRefOid, ref: v.headRefName, state: v.state, author: v.author?.login };
}

/** 모든 페이지를 읽어 하나의 배열로 (댓글이 100개를 넘어도 영수증·요청 마커를 놓치지 않게, Codex 리뷰) */
const ghList = (path) => (ghJson(['api', '--paginate', '--slurp', `${path}?per_page=100`]) || []).flat();
const issueComments = () => ghList(`repos/${repo}/issues/${pr}/comments`);

/** 요약 댓글의 code_review 상태 + 댓글 갱신 시각(at) */
function codexState(comments) {
  for (const c of comments) {
    if (!c.user?.login?.startsWith(BOT)) continue;
    const m = STATE_RE.exec(c.body || '');
    if (m) {
      try {
        const cr = JSON.parse(m[1]).code_review;
        return cr ? { ...cr, at: c.updated_at || c.created_at } : null;
      } catch {
        return null;
      }
    }
  }
  return null;
}

/** head 커밋에 달린 Codex 인라인 지적 요약 */
function findings(sha) {
  const list = ghList(`repos/${repo}/pulls/${pr}/comments`);
  return list
    .filter((c) => c.user?.login?.startsWith(BOT) && !c.in_reply_to_id && c.original_commit_id === sha)
    .map((c) => {
      const body = c.body || '';
      const pri = /!\[(P\d) Badge\]/.exec(body)?.[1] ?? '?';
      const title = (body.split('\n')[0] || '')
        .replace(/<\/?sub>/g, '')
        .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
        .replace(/\*\*/g, '')
        .trim();
      const line = c.line ?? c.original_line ?? '?';
      return `- [${pri}] ${c.path}:${line} ${title}\n  ${c.html_url}`;
    });
}

/** "지적 없음" 영수증 댓글 목록 { sha(7~40자), at } (댓글 순서) */
function cleanReceipts(comments) {
  return comments
    .filter((c) => c.user?.login?.startsWith(BOT))
    .map((c) => ({ sha: CLEAN_RECEIPT_RE.exec(c.body || '')?.[1], at: c.created_at }))
    .filter((r) => r.sha);
}

/** Codex 인라인 지적(스레드 첫 댓글) 시각 목록 */
function findingTimes() {
  return ghList(`repos/${repo}/pulls/${pr}/comments`)
    .filter((c) => c.user?.login?.startsWith(BOT) && !c.in_reply_to_id)
    .map((c) => c.created_at);
}

/** 사람이 단 "지적을 이슈로 이관함" 마커 [{ sha, at }] */
function resolvedMarks(comments) {
  return comments
    .filter((c) => !c.user?.login?.startsWith(BOT))
    .map((c) => ({ sha: RESOLVED_RE.exec(c.body || '')?.[1], at: c.created_at }))
    .filter((m) => m.sha);
}

/** 문서만 변경 판정의 기준 후보 커밋 (늦은 영수증부터). 마지막 미해결 인라인 지적(findingAt)보다 늦은 "지적 없음"(또는 이관 완료) 결과만 */
function cleanBases(st, stOk, receipts, resolved, findingAt) {
  const fresh = [...receipts, ...resolved]
    .filter((r) => r.at > findingAt)
    .sort((a, b) => (a.at < b.at ? 1 : -1))
    .map((r) => r.sha);
  const stFresh = stOk && st.at > findingAt ? [st.commit_sha] : [];
  return [...new Set([...fresh, ...stFresh])];
}

/** 로컬에 커밋이 없으면 원격에서 받아 온다 (GitHub는 sha로 fetch 가능) */
function ensureCommit(sha, ref) {
  if (git(['cat-file', '-e', `${sha}^{commit}`]).ok) return true;
  git(['fetch', '--quiet', 'origin', ref || sha], { timeout: 30_000, env: NON_INTERACTIVE_GIT_ENV });
  return git(['cat-file', '-e', `${sha}^{commit}`]).ok;
}

/**
 * base 이후 head까지 문서만 바뀌었는가. base가 head의 조상이 아니거나(force push) 커밋을 못 받으면 false.
 * develop 합치기로 들어온 파일(head의 내용이 head·develop 공통 조상의 내용과 같은 파일)은 이 PR의 변경이 아니어서 뺀다.
 */
function docsOnlySince(base, h, mb) {
  // 짧은 sha(영수증)도 받는다
  const full = git(['rev-parse', '--verify', '-q', `${base}^{commit}`]);
  if (!full.ok && !ensureCommit(base)) return false;
  if (!git(['merge-base', '--is-ancestor', base, h.sha]).ok) return false;
  const files = git(['diff', '--no-renames', '--name-only', base, h.sha]).out.split('\n').filter(Boolean);
  const fromDevelop = (f) => !!mb && git(['diff', '--quiet', mb, h.sha, '--', f]).ok;
  return onlyDocsChanged(files, fromDevelop);
}

/** 문서 판정 준비: head 커밋을 받고 develop을 한 번만 fetch해 head·develop 공통 조상을 구한다. head를 못 받으면 null */
function developMergeBase(h) {
  if (!ensureCommit(h.sha, h.ref)) return null;
  git(['fetch', '--quiet', 'origin', 'develop'], { timeout: 30_000, env: NON_INTERACTIVE_GIT_ENV });
  const mb = git(['merge-base', h.sha, 'origin/develop']);
  return { mb: mb.ok ? mb.out : null };
}

function judge() {
  const h = head();
  const comments = issueComments();
  const st = codexState(comments);
  const short = h.sha.slice(0, 7);
  if (h.state !== 'OPEN') return { verdict: 'CLOSED', h, comments, msg: `PR #${pr}이 ${h.state} 상태 (Codex 루프 불필요)` };
  const receipts = cleanReceipts(comments);
  const resolved = resolvedMarks(comments);
  const clean = receipts.some((r) => h.sha.startsWith(r.sha));
  const stClean = st?.status === 'completed' && !st.has_any_findings;
  // 지적이 있었지만 그 커밋의 남은 지적을 모두 이슈로 넘긴 상태 (pr-review 8-3 이관 완료).
  // 마커 뒤에 새 지적이 달렸으면 이관 완료가 아니다. 인라인 지적 조회는 필요할 때만 한다
  let times;
  const allFindings = () => (times ??= findingTimes());
  const lastAny = () => [...allFindings()].sort().at(-1) ?? '';
  const stMarks = resolved.filter((m) => m.sha === st?.commit_sha);
  const stResolved = st?.status === 'completed' && stMarks.length > 0 && stMarks.some((m) => m.at > lastAny());
  if (st?.commit_sha === h.sha && stClean) {
    return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 지적 없음 (상태 댓글)` };
  }
  if (st?.commit_sha === h.sha && stResolved) {
    return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 남은 지적 이관 완료 (--resolve)` };
  }
  if (clean && !(st?.commit_sha === h.sha && st.status === 'completed')) {
    return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 지적 없음 (영수증 댓글)` };
  }
  if (!st || st.commit_sha !== h.sha) {
    // 마지막 리뷰가 지적 없이(또는 이관 완료로) 끝났을 때만(상태 댓글이 없으면 영수증으로) 기준 커밋을 잡는다.
    // 지적이 남았거나 리뷰 중이면 영수증으로 건너뛰어 그 결과를 덮지 않는다.
    // 영수증·상태 댓글 뒤에 미해결 인라인 지적이 달렸으면 그것은 마지막 리뷰가 아니므로 쓰지 않는다.
    // 상태 댓글도 갱신이 늦을 수 있어 같은 기준을 적용한다 (Codex 리뷰)
    if (!st || stClean || stResolved) {
      const findingAt = unresolvedFindingAt(allFindings(), resolved.map((m) => m.at));
      const bases = cleanBases(st, stClean || stResolved, receipts, resolved, findingAt);
      const dev = bases.length ? developMergeBase(h) : null;
      const base = dev && bases.find((b) => docsOnlySince(b, h, dev.mb));
      if (base) {
        return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 문서만 변경 (Codex ✅ ${base.slice(0, 7)} 이후)` };
      }
    }
    // 환경·계정 안내가 이 head에 대한 마지막 요청보다 늦으면 기다려도 리뷰가 오지 않는다.
    // --request로 시작하면 설정을 마친 뒤의 재시도로 보고 요청을 새로 단다 (아래, Codex 리뷰)
    const notice = setupNotice(comments);
    const reqAt = requestAt(comments, h.sha);
    if (notice && (!reqAt || notice.at > reqAt)) {
      const msg =
        notice.kind === 'account'
          ? `Codex 계정 미연결 (@${notice.who ?? h.author ?? '?'}, ${notice.at}): ${notice.url} 에서 GitHub를 연결하고 Code review를 켠 뒤 --request로 다시 요청 (docs/환경설정.md 8절)`
          : `Codex 환경 없음 (${notice.at}): ${notice.url} 에서 만든 뒤 --request로 다시 요청`;
      return { verdict: 'ERROR', envMissing: true, h, comments, msg };
    }
    // 이 head에 대한 요청을 이미 달았으면 Codex가 시작하기를 기다리는 중이다 (요청을 다시 달지 않는다)
    if (reqAt) return { verdict: 'RUNNING', h, comments, msg: `head ${short} 재리뷰 요청함 (${reqAt}), 시작 대기` };
    const seen = st?.commit_sha ? `${st.commit_sha.slice(0, 7)} 기준` : '상태 댓글 없음';
    return { verdict: 'STALE', h, comments, msg: `head ${short}, Codex ${seen}` };
  }
  if (st.status !== 'completed') return { verdict: 'RUNNING', h, comments, msg: `head ${short} 리뷰 ${st.status}` };
  return { verdict: 'FINDINGS', h, comments, msg: `head ${short}` };
}

/** 이 head sha에 대한 재리뷰 요청 중 가장 늦은 시각 (없으면 null).
 *  커밋 시각과 비교하면 "먼저 커밋하고 나중에 push"한 경우 옛 요청을 새 head 것으로 오판하므로 sha 마커로 묶는다 (Codex 리뷰).
 *  봇의 안내문에도 "@codex review"가 들어 있으므로 사람 댓글만 본다. */
function requestAt(comments, sha) {
  const at = comments
    .filter((c) => !c.user?.login?.startsWith(BOT) && REQUEST_RE.exec(c.body || '')?.[1] === sha)
    .map((c) => c.created_at);
  return at.length ? at.sort().at(-1) : null;
}

const STATUS = { COMPLETE: 'success', FINDINGS: 'failure', ERROR: 'error' };

/** --status(또는 --resolve): 판정을 head 커밋의 상태 체크 "Codex"로 남긴다. 실패해도 판정에는 영향이 없다 */
function postStatus(r) {
  if (!(status || resolve) || !r.h?.sha || r.verdict === 'CLOSED') return;
  const state = STATUS[r.verdict] ?? 'pending';
  const description = `${r.verdict} ${r.msg}`.slice(0, 140);
  const fields = [`state=${state}`, 'context=Codex', `description=${description}`, `target_url=https://github.com/${repo}/pull/${pr}`];
  const ok = gh(['api', '-X', 'POST', `repos/${repo}/statuses/${r.h.sha}`, ...fields.flatMap((x) => ['-f', x])], { soft: true });
  if (ok == null) console.error('(Codex 상태 체크를 남기지 못함: statuses 쓰기 권한 확인)');
}

function report(r) {
  const rounds = r.comments && r.verdict !== 'CLOSED' ? ` (재리뷰 ${requestCount(r.comments)}회)` : '';
  console.log(`${r.verdict} ${r.msg}${rounds}`);
  postStatus(r);
  if (r.verdict === 'FINDINGS') {
    const f = findings(r.h.sha);
    console.log(f.length ? f.join('\n') : `(인라인 지적을 찾지 못함: gh api repos/${repo}/pulls/${pr}/reviews 확인)`);
  }
  process.exit(CODE[r.verdict]);
}

let r = judge();
if (r.verdict === 'CLOSED') report(r);

const postRequest = (sha) => gh(['pr', 'comment', pr, '--body', `@codex review\n\n<!-- codex-review-request: ${sha} -->`]);

if (resolve) {
  if (r.verdict !== 'FINDINGS') {
    console.error(`(--resolve는 head가 FINDINGS일 때만 쓴다. 지금은 ${r.verdict})`);
    report(r);
  }
  // 남은 지적을 follow-up 이슈로 넘긴 뒤에만 실행한다 (pr-review 8-3). 이후 문서만 바꾼 커밋은 이 커밋을 기준으로 본다
  gh(['pr', 'comment', pr, '--body', `Codex 남은 지적을 follow-up 이슈로 이관했습니다 (head ${r.h.sha.slice(0, 7)}).\n\n<!-- codex-findings-resolved: ${r.h.sha} -->`]);
  report({ ...r, verdict: 'COMPLETE', msg: `head ${r.h.sha.slice(0, 7)} 남은 지적 이관 완료 (--resolve 마커 댓글)` });
}

if (request && r.envMissing) {
  // 안내가 마지막 요청보다 늦으므로 기존 요청이 있어도 다시 단다. 환경이 아직 없으면 대기 중 같은 안내가 와서 ERROR로 끝난다
  postRequest(r.h.sha);
  console.error(`(Codex 환경·계정 안내 뒤 재시도: @codex review 요청함, head ${r.h.sha.slice(0, 7)})`);
  // 방금 단 댓글이 목록에 바로 안 보일 수 있어 다시 판정하지 않고 대기 상태로 둔다
  r = { ...r, verdict: 'RUNNING', envMissing: false, msg: `head ${r.h.sha.slice(0, 7)} 설정 안내 뒤 재리뷰 요청함, 시작 대기` };
} else if (request && r.verdict === 'STALE') {
  // 같은 head에 대한 요청이 이미 있으면 judge가 RUNNING으로 판정하므로 여기서는 요청이 없다
  postRequest(r.h.sha);
  console.error(`(@codex review 요청함: head ${r.h.sha.slice(0, 7)})`);
  r = { ...r, verdict: 'RUNNING', msg: `head ${r.h.sha.slice(0, 7)} 재리뷰 요청함, 시작 대기` };
}

const done = (v) => v === 'COMPLETE' || v === 'FINDINGS' || v === 'ERROR' || v === 'CLOSED';
if (!wait || done(r.verdict)) report(r);

const deadline = Date.now() + timeoutMin * 60_000;
while (Date.now() < deadline) {
  await new Promise((res) => setTimeout(res, 30_000));
  r = judge();
  if (done(r.verdict)) report(r);
}
console.log(`TIMEOUT ${timeoutMin}분 대기, 마지막 상태 ${r.verdict} (${r.msg})`);
postStatus(r);
process.exit(CODE.TIMEOUT);
