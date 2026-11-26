// 상태 표시줄 (HX-6): Claude Code 화면 아래 줄에 PC·브랜치·모델·컨텍스트 크기·사용량, 내 PR의 CI·Codex, 다음 로드맵 항목, 넘겨주는 시점 D-n을 보인다.
// 표시줄은 assistant 메시지마다 다시 돌므로(300ms 디바운스) 네트워크·git을 부르지 않는다:
//   - 브랜치는 .git/HEAD를 직접 읽는다
//   - PR·CI와 로드맵은 캐시(.harness/status.json)를 읽는다. 오래되면 scripts/status-refresh.mjs를 백그라운드로 띄워 gh로 채운다
//   - Codex 판정은 codex-loop hook이 남긴 .harness/codex-loop.json을 읽는다
// 세션 시작 hook도 로드맵 캐시를 채운다 (이미 계산한 보기를 재사용).
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { ROOT } from './common.mjs';
import { daysBetween, nextHandoff, nextItem, shortDate } from './roadmap.mjs';

export const STATUS_FILE = join(ROOT, '.harness', 'status.json');
export const CODEX_LOOP_FILE = join(ROOT, '.harness', 'codex-loop.json');
/** PR·CI 캐시를 다시 받는 간격 */
export const PR_REFRESH_MS = 2 * 60_000;
/** 로드맵 캐시를 다시 받는 간격 (상태는 PR 머지 때나 바뀐다) */
export const ROADMAP_REFRESH_MS = 10 * 60_000;
/** 새로 고침을 띄운 뒤 끝나지 않았어도 다시 띄우기까지 기다리는 시간 (gh가 멈춘 경우) */
export const REFRESH_LOCK_MS = 60_000;
/** CLAUDE.md 세션 운영: 200k를 넘으면 /clear를 권한다 (checkpoint-nudge와 같은 값) */
export const CONTEXT_WARN_TOKENS = 200_000;

function readJson(path) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    return null;
  }
}

export const readStatus = (path = STATUS_FILE) => readJson(path) ?? {};

/** 캐시의 일부를 바꿔 쓴다. 다른 프로세스(세션 시작 hook·새로 고침)가 쓴 나머지 키는 그대로 둔다 */
export function writeStatus(patch, path = STATUS_FILE) {
  try {
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, JSON.stringify({ ...readStatus(path), ...patch }) + '\n');
  } catch {
    // 표시용 캐시라 못 써도 다음에 다시 받는다
  }
}

/**
 * .git/HEAD를 직접 읽어 브랜치 이름 (git을 띄우지 않는다). worktree면 .git 파일의 gitdir을 따라간다.
 * detached면 짧은 sha, 읽지 못하면 null
 */
export function gitBranchAt(root = ROOT) {
  try {
    let gitDir = join(root, '.git');
    let head;
    try {
      head = readFileSync(join(gitDir, 'HEAD'), 'utf8');
    } catch {
      const pointer = /^gitdir:\s*(.+)$/m.exec(readFileSync(gitDir, 'utf8'))?.[1]?.trim();
      if (!pointer) return null;
      gitDir = isAbsolute(pointer) ? pointer : resolve(root, pointer);
      head = readFileSync(join(gitDir, 'HEAD'), 'utf8');
    }
    const ref = /^ref:\s*refs\/heads\/(.+)$/m.exec(head)?.[1]?.trim();
    return ref ?? (/^[0-9a-f]{7,}/.exec(head.trim())?.[0].slice(0, 7) || null);
  } catch {
    return null;
  }
}

/** gh pr view의 statusCheckRollup → 'pass' | 'fail' | 'pending' | 'none' */
export function ciState(rollup) {
  if (!Array.isArray(rollup) || !rollup.length) return 'none';
  const FAIL = ['FAILURE', 'CANCELLED', 'TIMED_OUT', 'ACTION_REQUIRED', 'STARTUP_FAILURE', 'ERROR'];
  // CheckRun은 status·conclusion, StatusContext는 state를 쓴다
  const outcome = (c) => (c.__typename === 'StatusContext' || (c.state && !c.status) ? c.state : c.status === 'COMPLETED' ? c.conclusion : 'PENDING');
  const states = rollup.map((c) => String(outcome(c) ?? 'PENDING').toUpperCase());
  if (states.some((s) => FAIL.includes(s))) return 'fail';
  if (states.some((s) => ['PENDING', 'EXPECTED', 'QUEUED', 'IN_PROGRESS', 'WAITING', 'REQUESTED'].includes(s))) return 'pending';
  return 'pass';
}

/** 표시줄 입력(stdin JSON) → { tokens, pct, over }. 첫 응답 전이면 tokens 0 */
export function contextInfo(input) {
  const cw = input?.context_window;
  const u = cw?.current_usage;
  const tokens = u ? (u.input_tokens ?? 0) + (u.cache_creation_input_tokens ?? 0) + (u.cache_read_input_tokens ?? 0) : 0;
  const pct = typeof cw?.used_percentage === 'number' ? Math.round(cw.used_percentage) : null;
  return { tokens, pct, over: Boolean(input?.exceeds_200k_tokens) || tokens >= CONTEXT_WARN_TOKENS };
}

/** 로드맵 보기 → 캐시에 남길 요약 { key, branch, next, handoff } (세션 시작 hook·새로 고침이 쓴다) */
export function roadmapSnapshot(view, key, branch) {
  const next = nextItem(view, key, { branch });
  const h = nextHandoff(view, key);
  return {
    key,
    branch,
    statusOk: view.statusOk,
    next: next ? { id: next.item.id, end: next.item.end, state: next.item.state, model: next.item.model, here: next.why === 'branch' } : null,
    handoff: h,
  };
}

/** 새로 고침이 필요한가 (순수 함수) */
export function needsRefresh(cache, branch, nowMs = Date.now()) {
  if (cache?.refreshingAt && nowMs - cache.refreshingAt < REFRESH_LOCK_MS) return false;
  const pr = cache?.pr;
  const rm = cache?.roadmap;
  return !pr || pr.branch !== branch || nowMs - (pr.at ?? 0) > PR_REFRESH_MS || !rm || rm.branch !== branch || nowMs - (rm.at ?? 0) > ROADMAP_REFRESH_MS;
}

const CODEX_ICON = { COMPLETE: '✅', FINDINGS: '❗', RUNNING: '⏳', STALE: '⏸️', TIMEOUT: '⌛', ERROR: '⚠️' };
const CI_ICON = { pass: '✅', fail: '❌', pending: '⏳' };
const STATE_ICON = { done: '✅', doing: '🔨', todo: '⬜', unknown: '❔' };
const k = (n) => (n >= 1000 ? `${Math.round(n / 1000)}k` : String(n));
const dn = (days) => (days === 0 ? 'D-day' : days > 0 ? `D-${days}` : `⚠️ ${-days}일 지남`);
const pct = (v) => (typeof v === 'number' ? `${Math.round(v)}%` : null);

/**
 * 표시줄 두 줄 (순수 함수).
 * input: Claude Code가 주는 stdin JSON, pc: { id, owner }, branch, cache: .harness/status.json, codex: codex-loop.json의 이 브랜치 항목, today: YYYY-MM-DD
 */
export function statusLines({ input = {}, pc, branch, cache = {}, codex = null, today }) {
  const first = [];
  const model = input.model?.display_name;
  if (model) first.push(`🤖 ${model}${input.effort?.level ? ` ${input.effort.level}` : ''}`);
  first.push(pc?.id ? `👤 ${pc.owner ?? '?'} 🖥️ ${pc.id}` : '🖥️ 미등록 PC → node scripts/setup.mjs --pc <id>');
  first.push(`🌿 ${branch ?? '?'}`);
  const ctx = contextInfo(input);
  if (ctx.tokens || ctx.pct !== null) first.push(`🧠 ${k(ctx.tokens)}${ctx.pct !== null ? ` ${ctx.pct}%` : ''}${ctx.over ? ' ⚠️ /clear 권장' : ''}`);
  const five = pct(input.rate_limits?.five_hour?.used_percentage);
  const week = pct(input.rate_limits?.seven_day?.used_percentage);
  if (five || week) first.push(`⏳ ${[five && `5h ${five}`, week && `7d ${week}`].filter(Boolean).join(' ')}`);

  const second = [];
  const pr = cache.pr?.branch === branch ? cache.pr : null;
  if (pr?.number) {
    const ci = CI_ICON[pr.ci] ? ` CI ${CI_ICON[pr.ci]}` : '';
    // Codex 판정이 PR의 지금 head에 대한 것이 아니면(그 뒤 push) 아직 모른다
    const fresh = codex?.prHead && pr.head ? pr.head.startsWith(codex.prHead) : false;
    const cx = CODEX_ICON[codex?.verdict] ? ` Codex ${fresh ? CODEX_ICON[codex.verdict] : '?'}` : '';
    second.push(`🔀 PR #${pr.number}${pr.state && pr.state !== 'OPEN' ? ` ${pr.state === 'MERGED' ? '머지됨' : '닫힘'}` : ''}${ci}${cx}`);
  } else if (pr && /^(feature|fix)\//.test(branch ?? '')) {
    second.push('🔀 PR 없음');
  }
  // 다른 사람으로 등록했던 캐시(PC 재등록)는 보이지 않는다
  const rm = cache.roadmap && (!pc?.key || cache.roadmap.key === pc.key) ? cache.roadmap : null;
  if (rm?.next) {
    const late = today && rm.next.end && rm.next.end < today ? ' ⚠️ 지남' : '';
    second.push(`🗺️ ${rm.next.here ? '' : '다음 '}${rm.next.id} ${STATE_ICON[rm.next.state] ?? ''} ~${shortDate(rm.next.end)}${late}`.replace(/\s+/g, ' '));
  }
  if (rm?.handoff?.date && today) {
    const days = daysBetween(today, rm.handoff.date);
    const text = rm.handoff.text.length > 40 ? `${rm.handoff.text.slice(0, 39)}…` : rm.handoff.text;
    second.push(`${days <= 2 ? '🤝' : '📅'} ${dn(days)} ${text}`);
  }
  return [first.join(' · '), second.join(' · ')].filter(Boolean);
}
