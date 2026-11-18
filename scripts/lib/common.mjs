// 하네스 스크립트와 Claude Code hooks가 함께 쓰는 유틸리티
import { spawnSync } from 'node:child_process';
import { closeSync, existsSync, fstatSync, openSync, readFileSync, readSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/** 저장소 루트 (scripts/lib 기준 두 단계 위) */
export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const NOW_FILE = join(ROOT, 'docs', 'handoff', 'NOW.md');
export const WORKLOG_FILE = join(ROOT, 'docs', '작업로그.md');
export const PROTECTED_BRANCHES = ['main', 'develop'];
export const isWin = process.platform === 'win32';

/** git 실행. 한글 경로가 escape되지 않도록 core.quotepath=false */
export function git(args, { cwd = ROOT, timeout } = {}) {
  const r = spawnSync('git', ['-c', 'core.quotepath=false', ...args], {
    cwd,
    encoding: 'utf8',
    timeout,
    windowsHide: true,
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

/** 로컬 시간 YYYY-MM-DD HH:mm */
export function nowStamp(d = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
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
