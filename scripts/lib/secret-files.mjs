// 저장소 안 실제 비밀 파일 목록 (#188). guard-bash·guard-files hook이 재귀 검색·검색 글롭 판정에 넘긴다.
// common.mjs와 분리한 이유: common.mjs는 BE·FE CI job도 실행하는 파일이라, 가드만 바꾼 PR에서 BE·FE가 돌지 않게 한다 (HP-4 AC7)
import { join, resolve } from 'node:path';
import { ROOT, git } from './common.mjs';
import { isSecretFile, isWithin } from './protect.mjs';

// 무시된 폴더 중 펼쳐 보지 않는 큰 폴더 (의존성·빌드 산출물. 그 밖의 무시 폴더 안 .env는 본다, #245)
const HEAVY = /(^|\/)(node_modules|build|dist|bin|out|target|\.gradle)\/$/;

const cache = new Map();

/**
 * 작업 트리(root) 안 비밀 파일 [{ path, ignored: .gitignore 대상인가 }]. 작업 트리마다 한 번만 조회한다 (약 0.1~0.2초).
 * 조회가 실패하면 루트 .env가 있다고 본다
 */
export function repoSecretFiles(root = ROOT) {
  if (cache.has(root)) return cache.get(root);
  const run = (args) => {
    const r = git(['ls-files', '-z', ...args], { cwd: root, timeout: 3000 });
    return r.ok ? r.out.split('\0').filter(Boolean) : null;
  };
  const pick = (list, ignored) => list.filter((f) => !f.endsWith('/') && isSecretFile(f)).map((f) => ({ path: join(root, f), ignored }));
  const tracked = run(['--cached', '--others', '--exclude-standard']);
  const ignored = run(['--others', '--ignored', '--exclude-standard', '--directory']);
  let files;
  if (!tracked || !ignored) files = [{ path: join(root, '.env'), ignored: true }];
  else {
    files = [...pick(tracked, false), ...pick(ignored, true)];
    const dirs = ignored.filter((f) => f.endsWith('/') && !HEAVY.test(f));
    const inner = dirs.length ? run(['--others', '--ignored', '--exclude-standard', '--', ...dirs]) : [];
    files.push(...pick(inner ?? [], true));
  }
  cache.set(root, files);
  return files;
}

let worktrees = null;
/** 이 저장소의 다른 작업 트리 경로 (git worktree add로 만든 것) */
function otherWorktrees() {
  if (worktrees) return worktrees;
  const r = git(['worktree', 'list', '--porcelain'], { timeout: 3000 });
  worktrees = r.ok
    ? r.out
        .split('\n')
        .filter((l) => l.startsWith('worktree '))
        .map((l) => resolve(l.slice(9)))
        .filter((p) => !isWithin(p, ROOT) || !isWithin(ROOT, p))
    : [];
  return worktrees;
}

/**
 * dir 아래 비밀 파일. 이 저장소와 다른 작업 트리 중 dir과 겹치는 것(dir이 품거나 dir을 품는 것)에서 찾는다.
 * 어느 작업 트리와도 겹치지 않으면 목록을 모르므로 null (호출하는 쪽이 대표 이름으로 판정한다)
 */
export function secretFilesUnder(dir) {
  let known = false;
  const out = [];
  for (const root of [ROOT, ...otherWorktrees()]) {
    if (!isWithin(root, dir) && !isWithin(dir, root)) continue;
    known = true;
    out.push(...repoSecretFiles(root).filter((f) => isWithin(f.path, dir)));
  }
  return known ? out : null;
}
