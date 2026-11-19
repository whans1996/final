// 하네스 스크립트와 Claude Code hooks가 함께 쓰는 유틸리티
import { spawnSync } from 'node:child_process';
import { closeSync, existsSync, fstatSync, openSync, readFileSync, readSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/** 저장소 루트 (scripts/lib 기준 두 단계 위) */
export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const WORKLOG_FILE = join(ROOT, 'docs', '작업로그.md');

// 두 사람이 영역별로 병렬 개발한다 (11-19 결정). 현재 작업 스냅샷은 사람별 파일 docs/handoff/NOW-<key>.md이고,
// key는 PC id 접두다 (dh-ssafy → dh). 같은 사람의 PC 사이에서는 이 파일로 교대한다.
export const OWNER_KEYS = ['dh', 'js'];

/** PC id → 사람 key (dh/js). 모르면 null */
export function ownerKey(pcId) {
  const k = String(pcId || '').split('-')[0];
  return OWNER_KEYS.includes(k) ? k : null;
}

export const nowRelPath = (key) => `docs/handoff/NOW-${key}.md`;
export const nowFileOf = (key) => join(ROOT, nowRelPath(key));
export const otherOwnerKey = (key) => OWNER_KEYS.find((k) => k !== key) ?? null;

/** 이 PC에 등록된 PC id (.harness/pc.json). 등록부 검증은 env-check.currentPc가 한다 */
export function localPcId() {
  try {
    return JSON.parse(readFileSync(join(ROOT, '.harness', 'pc.json'), 'utf8')).id || null;
  } catch {
    return null;
  }
}

/** 이 PC 사용자의 NOW 파일 경로. PC 미등록이면 null */
export function myNowFile() {
  const key = ownerKey(localPcId());
  return key ? nowFileOf(key) : null;
}
export const PROTECTED_BRANCHES = ['main', 'develop'];
export const isWin = process.platform === 'win32';

/** hook 안의 fetch가 인증 창(Windows Git Credential Manager)이나 터미널 프롬프트로 멈추지 않게 하는 env */
export const NON_INTERACTIVE_GIT_ENV = { GIT_TERMINAL_PROMPT: '0', GCM_INTERACTIVE: 'never' };

/** git 실행. 한글 경로가 escape되지 않도록 core.quotepath=false. env는 process.env에 덧붙인다 */
export function git(args, { cwd = ROOT, timeout, env } = {}) {
  const r = spawnSync('git', ['-c', 'core.quotepath=false', ...args], {
    cwd,
    encoding: 'utf8',
    timeout,
    windowsHide: true,
    env: env ? { ...process.env, ...env } : undefined,
  });
  return { ok: r.status === 0, out: (r.stdout || '').trim(), err: (r.stderr || '').trim(), status: r.status };
}

export function isGitRepo(cwd = ROOT) {
  return git(['rev-parse', '--is-inside-work-tree'], { cwd }).ok;
}

/** 현재 브랜치 이름. 커밋이 없는 브랜치에서도 동작. detached면 null */
export function currentBranch(cwd = ROOT) {
  const r = git(['symbolic-ref', '--short', '-q', 'HEAD'], { cwd });
  return r.ok ? r.out : null;
}

export function hasCommits(cwd = ROOT) {
  return git(['rev-parse', '--verify', '-q', 'HEAD'], { cwd }).ok;
}

export function isMerging(cwd = ROOT) {
  const r = git(['rev-parse', '--git-path', 'MERGE_HEAD'], { cwd });
  return r.ok && existsSync(resolve(cwd, r.out));
}

/** 기능 ID 추출: feature/F-05-analysis → F-05, feature/F-05a-analysis-zip → F-05a */
export function featureIdFrom(text) {
  const m = /F-\d{2}(?:[a-z](?![a-z]))?/.exec(text || '');
  return m ? m[0] : null;
}

/** stdin 전체를 JSON으로 읽는다 (Claude Code hook 입력) */
export function readStdinJson() {
  try {
    const raw = readFileSync(0, 'utf8');
    return raw.trim() ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

export function readText(path) {
  try {
    return readFileSync(path, 'utf8');
  } catch {
    return null;
  }
}

/** 마크다운에서 `## 제목` 섹션 본문을 뽑는다 */
export function mdSection(md, title) {
  if (!md) return '';
  const lines = md.split(/\r?\n/);
  const start = lines.findIndex((l) => l.replace(/^##\s+/, '').trim().startsWith(title) && l.startsWith('## '));
  if (start < 0) return '';
  const out = [];
  for (let i = start + 1; i < lines.length; i++) {
    if (lines[i].startsWith('## ')) break;
    out.push(lines[i]);
  }
  return out.join('\n').trim();
}

// 체크포인트 대상: 실제 작업 산출물 (하네스 자체, 기획서는 제외)
const WORK_WATCH = /^(backend\/|frontend\/|docs\/specs\/)/;
const WORK_IGNORE = /(^|\/)(node_modules|build|dist|\.gradle)\/|(^|\/)CLAUDE\.md$/;

/** 내 NOW 파일보다 1초 넘게 나중에 바뀐 미커밋 작업 파일 경로. 최근 것부터. PC 미등록(nowFile null)이면 판정하지 않는다 */
export function workFilesNewerThanNow(cwd = ROOT, nowFile = myNowFile()) {
  if (!nowFile) return [];
  const st = git(['status', '--porcelain', '-z', '--untracked-files=all'], { cwd });
  if (!st.ok || !st.out) return [];

  // -z 형식: "XY path\0" (rename/copy는 뒤에 원래 경로가 하나 더 붙음)
  const tokens = st.out.split('\0').filter(Boolean);
  const paths = [];
  for (let i = 0; i < tokens.length; i++) {
    const t = tokens[i];
    paths.push(t.slice(3));
    if (/[RC]/.test(t.slice(0, 2))) i++;
  }

  let nowMtime = 0;
  try {
    nowMtime = statSync(nowFile).mtimeMs;
  } catch {
    // NOW 파일 없음
  }
  const out = [];
  for (const p of paths) {
    if (!WORK_WATCH.test(p) || WORK_IGNORE.test(p)) continue;
    try {
      const m = statSync(join(cwd, p)).mtimeMs;
      if (m > nowMtime + 1000) out.push({ path: p, mtime: m });
    } catch {
      // 삭제된 파일
    }
  }
  return out.sort((a, b) => b.mtime - a.mtime).map((f) => f.path);
}

/** 로컬 시간 YYYY-MM-DD HH:mm */
export function nowStamp(d = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** 작업로그 헤더·NOW "갱신:" 줄에 쓰는 기록 머리 "YYYY-MM-DD HH:mm · 작업자 · PC id · 브랜치" (AI가 시각을 짐작해 쓰지 않게) */
export function recordStamp({ date = new Date(), author, pcId, branch }) {
  return `${nowStamp(date)} · ${author || '?'} · ${pcId || 'unknown-pc'} · ${branch || '(detached)'}`;
}

/** 작업로그 항목 [{ at: 'YYYY-MM-DD HH:mm', author, pcId, text }] (문서 순서). 헤더 "## 날짜 [시각] · 작업자 · PC id · 브랜치" */
export function worklogEntries(log) {
  if (!log) return [];
  return log
    .split(/\r?\n(?=## )/)
    .filter((e) => e.startsWith('## '))
    .map((text) => {
      const m = /^## (\d{4}-\d{2}-\d{2})(?: (\d{2}:\d{2}))? · ([^·\n]+?) · ([\w-]+) · /.exec(text);
      return m ? { at: `${m[1]} ${m[2] || '00:00'}`, author: m[3], pcId: m[4], text } : { at: null, author: null, pcId: null, text };
    });
}

/**
 * 이 사람(key)의 가장 최근 작업로그 항목. 병렬 작업이라 상대 항목은 건너뛴다.
 * unknown-pc 항목(미등록 PC의 handoff.mjs)은 작업자가 이 사람 PC의 github 계정일 때만 넣는다 (pcs: pcs.json의 pcs).
 * union 머지 뒤에는 문서 순서가 시간순이 아닐 수 있어 시각으로 고른다 (같으면 뒤의 것).
 */
export function lastWorklogEntryOf(entries, key, pcs = {}) {
  const accounts = new Set(
    Object.entries(pcs)
      .filter(([id, p]) => ownerKey(id) === key && p?.github)
      .map(([, p]) => p.github),
  );
  const mine = entries.filter((e) => e.pcId && (ownerKey(e.pcId) === key || (e.pcId === 'unknown-pc' && accounts.has(e.author))));
  return mine.reduce((best, e) => (!best || e.at >= best.at ? e : best), null);
}

/**
 * 직전 작업 PC: 내 NOW "갱신:" 줄과 내 마지막 작업로그 항목 중 더 최근 것.
 * (자동 handoff.mjs는 NOW의 갱신 줄만 바꾸고 본문은 그대로 둔다)
 * knownIds: pcs.json의 id 목록. 모르는 id와 다른 사람의 PC는 무시한다
 * (상대가 내 NOW 파일을 처음 만들어 준 경우처럼, 다른 사람 PC에서 쓴 기록은 내 PC 전환이 아니다)
 */
export function previousPc({ nowText, entry, knownIds, key }) {
  const nowHead = /^갱신: (\d{4}-\d{2}-\d{2})(?: (\d{2}:\d{2}))?.*?\bPC: ([\w-]+)/m.exec(nowText || '');
  const cands = [];
  if (nowHead) cands.push({ at: `${nowHead[1]} ${nowHead[2] || '00:00'}`, id: nowHead[3] });
  if (entry?.pcId) cands.push({ at: entry.at, id: entry.pcId });
  return (
    cands
      .filter((c) => c.id === 'unknown-pc' || (knownIds.includes(c.id) && (!key || ownerKey(c.id) === key)))
      .sort((a, b) => (a.at < b.at ? 1 : -1))[0]?.id ?? null
  );
}

/** assistant 메시지 한 줄의 usage를 { ctx, input, cacheWrite, cacheRead, output, model }로. usage가 없으면 null */
function usageOf(line) {
  let o;
  try {
    o = JSON.parse(line);
  } catch {
    return null;
  }
  const u = o.type === 'assistant' ? o.message?.usage : null;
  if (!u) return null;
  const input = u.input_tokens || 0;
  const cacheWrite = u.cache_creation_input_tokens || 0;
  const cacheRead = u.cache_read_input_tokens || 0;
  return {
    id: o.message.id,
    model: o.message.model || '',
    input,
    cacheWrite,
    cacheRead,
    output: u.output_tokens || 0,
    ctx: input + cacheWrite + cacheRead,
  };
}

/** Claude Code transcript(JSONL)의 턴별 usage. 스트리밍으로 같은 message.id가 여러 줄 남으므로 마지막 줄만 쓴다 */
export function transcriptUsage(path) {
  const byId = new Map();
  const text = readText(path) || '';
  for (const line of text.split('\n')) {
    const u = line && usageOf(line);
    if (u) byId.set(u.id, u);
  }
  return [...byId.values()];
}

/** 마지막 턴의 컨텍스트 크기(토큰). 큰 transcript도 빠르게 읽도록 끝부분만 본다. 모르면 0 */
export function lastContextTokens(path, tailBytes = 512 * 1024) {
  try {
    const fd = openSync(path, 'r');
    try {
      const size = fstatSync(fd).size;
      const len = Math.min(size, tailBytes);
      const buf = Buffer.alloc(len);
      readSync(fd, buf, 0, len, size - len);
      const lines = buf.toString('utf8').split('\n');
      for (let i = lines.length - 1; i >= 0; i--) {
        const u = usageOf(lines[i]);
        if (u) return u.ctx;
      }
    } finally {
      closeSync(fd);
    }
  } catch {
    // transcript 없음
  }
  return 0;
}

/**
 * SessionStart 화면 배너 (systemMessage). additionalContext는 모델에만 가므로 사람에게는 이것만 보인다.
 * 누구·어느 PC·브랜치·환경·주의·follow-up 이슈·내/상대 현재 기능을 몇 줄로 요약한다.
 */
export function sessionBanner({ pc, branch, envBad = [], warnings = [], syncLines = [], issueLine, myFeature, peerName, peerFeature }) {
  const cut = (t, n = 90) => (t.length > n ? `${t.slice(0, n - 1)}…` : t);
  let who = `👤 ? · 🖥️ 미등록 PC (hostname ${pc?.hostname ?? '?'}) → node scripts/setup.mjs --pc <id>`;
  if (pc?.id) {
    // label은 "김동환 SSAFY 교육장"처럼 사람 이름으로 시작한다
    const label = String(pc.label ?? '');
    const place = pc.owner && label.startsWith(pc.owner) ? label.slice(pc.owner.length).trim() : label;
    who = `👤 ${pc.owner ?? '?'} · 🖥️ ${place || pc.id} (${pc.id})`;
  }
  const lines = [`[바이브노트] ${who} · 🌿 ${branch ?? '?'}`];
  lines.push(envBad.length ? `환경: ${envBad.join(', ')}` : '환경 ✅');
  // 경고는 대개 "상태. 할 일." 순서라 첫 문장만 자르면 할 일이 빠진다 → 길이로만 자른다
  for (const w of warnings) lines.push(`⚠️ ${cut(w, 110)}`);
  // 자동 동기화 결과 (scripts/lib/sync.mjs): 줄마다 ✅·⚠️·⏸️로 시작한다
  for (const s of syncLines) lines.push(`🔄 ${cut(s.replace(/^- /, ''), 110)}`);
  if (issueLine) lines.push(issueLine);
  if (myFeature) lines.push(`내 작업: ${cut(myFeature)}`);
  if (peerFeature) lines.push(`${peerName ?? '상대'}: ${cut(peerFeature)}`);
  return lines.join('\n');
}
