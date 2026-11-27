// 저장소 안 실제 비밀 파일 목록 (#188). guard-bash·guard-files hook이 재귀 검색·검색 글롭 판정에 넘긴다.
// common.mjs와 분리한 이유: common.mjs는 BE·FE CI job도 실행하는 파일이라, 가드만 바꾼 PR에서 BE·FE가 돌지 않게 한다 (HP-4 AC7)
import { join } from 'node:path';
import { ROOT, git } from './common.mjs';
import { isSecretFile, isWithin } from './protect.mjs';

let repoSecrets = null;

/**
 * 저장소 안 비밀 파일 [{ path, ignored: .gitignore 대상인가 }]. 프로세스마다 한 번만 조회한다 (약 0.1초).
 * 무시된 폴더(node_modules, data/raw 등)는 --directory로 접어 그 안은 보지 않고, 다른 worktree의 파일도 모른다.
 * 조회가 실패하면 루트 .env가 있다고 본다
 */
export function repoSecretFiles() {
  if (repoSecrets) return repoSecrets;
  const list = (args, ignored) => {
    const r = git(['ls-files', '-z', ...args], { timeout: 3000 });
    if (!r.ok) return null;
    return r.out
      .split('\0')
      .filter((f) => f && !f.endsWith('/') && isSecretFile(f))
      .map((f) => ({ path: join(ROOT, f), ignored }));
  };
  const parts = [list(['--cached', '--others', '--exclude-standard'], false), list(['--others', '--ignored', '--exclude-standard', '--directory'], true)];
  repoSecrets = parts.some((x) => x === null) ? [{ path: join(ROOT, '.env'), ignored: true }] : parts.flat();
  return repoSecrets;
}

/** dir 아래 비밀 파일. 저장소를 품는 폴더(홈·드라이브 루트)면 전부, 저장소 밖은 모른다 ([]) */
export const secretFilesUnder = (dir) => (isWithin(ROOT, dir) ? repoSecretFiles() : repoSecretFiles().filter((f) => isWithin(f.path, dir)));
