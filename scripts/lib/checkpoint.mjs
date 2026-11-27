// Stop hook checkpoint-nudge의 체크포인트 누락 판정 (HP-4 AC5, H-11). git·파일 호출 없이 계산하는 부분이다 (scripts/test/checkpoint.test.mjs).
// 코드를 바꿨는데 내 NOW 파일을 갱신하지 않고 턴을 끝내면 세션이 끊겼을 때 다른 PC가 이어받을 수 없다.
// 미커밋 변경만 보면 "코드를 커밋까지 하고 NOW 없이 멈춤"을 놓친다 (H-11). 그래서 이 세션이 고친 파일을 담은 내 커밋도 본다.
import { basename, relative, resolve } from 'node:path';
import { isWorkFile } from './common.mjs';

// 경로를 받는 편집 도구: Edit·Write·MultiEdit·NotebookEdit, MCP write_file·edit_block 등
const EDIT_TOOL = /edit|write/i;
// 파일 이름 없이 여러 파일을 바꾸는 명령 → 이 세션이 작업 영역 전체를 건드린 것으로 본다
// (git pull·checkout 등은 넣지 않는다: /pickup이 매번 pull하므로 넣으면 오탐 방지가 무력해진다)
// npm run lint·format은 frontend/package.json에서 --fix·prettier --write를 실행한다 (lint:check는 읽기 전용)
const BROAD_COMMAND = /\bnpm\s+(i|install|update|uninstall)\b|\bnpm\s+run\s+(lint|format)(?![:\w-])|\bprettier\b.*--write|--fix\b|spotlessApply/i;
// 명령에 이 이름만 나와서는 어느 파일인지 알 수 없다
const GENERIC_NAMES = new Set(['index.js', 'index.ts', 'package.json', 'package-lock.json', 'build.gradle', 'readme.md', 'claude.md']);
// NOW 파일 mtime과 1초 안쪽은 같은 때로 본다 (파일 시스템 시각 정밀도)
const SLACK_MS = 1000;

/** Stop hook 입력으로 검사할지: 이미 한 번 막힌 뒤의 재시도(루프 방지)와 계획 모드(NOW를 고칠 수 없음)는 검사하지 않는다 */
export function shouldCheck(input) {
  return !input?.stop_hook_active && input?.permission_mode !== 'plan';
}

/**
 * 이 세션이 건드린 파일 판정 함수. texts[0]은 메인 transcript, 나머지는 서브에이전트 transcript 내용.
 * 메인을 못 읽으면(null) null(= 전부 이 세션 것으로 본다). 같은 작업 트리에서 다른 세션이 바꾼 파일로 막지 않기 위해서다 (11-19 오탐).
 * 편집 도구의 경로는 정확히, 셸 명령은 명령 문자열에 경로(뒷부분 두 단계 이상)나 고유한 파일 이름이 있으면 건드린 것으로 본다.
 */
export function sessionTouchedFrom(texts, root) {
  if (!texts?.length || texts[0] == null) return null;
  const norm = (p) => p.replace(/\\/g, '/').toLowerCase();
  const edited = new Set();
  const commands = [];
  let broad = false;
  for (const text of texts) {
    for (const line of (text || '').split('\n')) {
      if (!line.includes('"tool_use"')) continue;
      let o;
      try {
        o = JSON.parse(line);
      } catch {
        continue;
      }
      for (const c of o.message?.content || []) {
        if (c?.type !== 'tool_use' || !c.input) continue;
        const p = c.input.file_path || c.input.notebook_path || c.input.path;
        if (EDIT_TOOL.test(c.name) && typeof p === 'string') edited.add(norm(relative(root, resolve(root, p))));
        else if (typeof c.input.command === 'string') {
          commands.push(norm(c.input.command));
          if (BROAD_COMMAND.test(c.input.command)) broad = true;
        }
      }
    }
  }
  if (broad) return () => true;
  return (path) => {
    const p = norm(path);
    if (edited.has(p)) return true;
    // frontend/src/a/B.vue → "frontend/src/a/b.vue", "src/a/b.vue", "a/b.vue" (cd 후 상대 경로)
    const segs = p.split('/');
    const suffixes = segs.slice(0, -1).map((_, i) => segs.slice(i).join('/'));
    const name = norm(basename(path));
    return commands.some((cmd) => suffixes.some((s) => cmd.includes(s)) || (!GENERIC_NAMES.has(name) && cmd.includes(name)));
  };
}

/**
 * NOW 갱신 뒤 커밋을 읽는 git log 인자 (parseCommitLog 형식).
 * --first-parent: develop 합치기로 들어온 다른 브랜치의 내 커밋(병렬 세션)을 이 브랜치 작업으로 보지 않는다 (reviewer 지적)
 */
export function commitLogArgs(nowMtime, limit) {
  return ['log', '--first-parent', '--no-merges', `-${limit}`, `--since=${new Date(nowMtime).toISOString()}`, '--format=%x1e%H%x1f%ae%x1f%ct%x1f%P%x1f%s', '--name-only', 'HEAD'];
}

/**
 * `git log --format=%x1e%H%x1f%ae%x1f%ct%x1f%P%x1f%s --name-only` 출력 → [{ sha, email, time(ms), merge, subject, files }]
 */
export function parseCommitLog(out) {
  return String(out || '')
    .split('\x1e')
    .filter((chunk) => chunk.trim())
    .map((chunk) => {
      const [head, ...rest] = chunk.split('\n');
      const [sha, email, ct, parents, ...subject] = head.split('\x1f');
      return {
        sha,
        email,
        time: Number(ct) * 1000,
        merge: (parents || '').trim().split(/\s+/).filter(Boolean).length > 1,
        subject: subject.join('\x1f'),
        files: rest.map((l) => l.trim()).filter(Boolean),
      };
    });
}

/**
 * 체크포인트 누락: 이 세션이 건드린 작업 파일(isWorkFile)이 NOW보다 나중에 바뀌었는데 NOW를 갱신하지 않았다.
 * - 미커밋: dirty [{ path, mtime }] 중 mtime이 NOW보다 늦은 것 → { kind: 'uncommitted', path }
 * - 커밋 후 미기록 (H-11): merge가 아니고 작성자가 나(myEmail)이고 커밋 시각이 NOW보다 늦은 커밋의 파일 중,
 *   작업 트리 mtime(mtimeOf, 지운 파일은 null)도 NOW보다 늦은 것 → { kind: 'committed', path, sha, subject }.
 *   파일 mtime도 보는 이유: "코드 수정 → NOW 갱신 → 커밋"(평소 흐름)은 커밋이 NOW보다 늦지만 기록은 이미 했다.
 *   자동 동기화로 들어온 merge·상대 커밋은 작성자가 달라서, 다른 PC·다른 세션의 내 커밋은 touched에서 빠진다.
 * touched가 null이면 전부 이 세션 것으로 본다. 누락이 없으면 null.
 */
export function findMissingCheckpoint({ dirty = [], commits = [], nowMtime = 0, myEmail = '', touched = null, mtimeOf = () => null }) {
  const mine = (p) => isWorkFile(p) && (!touched || touched(p));
  const newer = (t) => Number.isFinite(t) && t > nowMtime + SLACK_MS;
  const d = dirty.find((f) => newer(f.mtime) && mine(f.path));
  if (d) return { kind: 'uncommitted', path: d.path };
  const me = String(myEmail || '').toLowerCase();
  if (!me) return null;
  for (const c of commits) {
    if (c.merge || String(c.email || '').toLowerCase() !== me || !newer(c.time)) continue;
    const path = c.files.find((p) => mine(p) && newer(mtimeOf(p)));
    if (path) return { kind: 'committed', path, sha: c.sha, subject: c.subject };
  }
  return null;
}
