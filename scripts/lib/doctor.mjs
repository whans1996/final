// doctor의 하네스 무결성 점검 중 git·파일 호출 없이 계산하는 부분 (scripts/test/doctor.test.mjs).
// 결과 행은 env-check와 같은 { status: 'ok'|'warn'|'fail', item, detail }.

export const REQUIRED_HOOKS = ['.githooks/commit-msg', '.githooks/pre-push'];

/** `git ls-files -s <hooks>` 출력에서 없거나 실행 권한(100755)이 빠진 hook. Linux CI·WSL에서 실행 권한이 없으면 hook이 조용히 건너뛰어진다 */
export function hookModeProblems(lsFilesOut, required = REQUIRED_HOOKS) {
  const modes = new Map(
    (lsFilesOut || '')
      .split('\n')
      .filter(Boolean)
      .map((l) => {
        const [meta, path] = l.split('\t');
        return [path, meta.split(' ')[0]];
      }),
  );
  return required.filter((h) => modes.get(h) !== '100755').map((h) => (modes.has(h) ? `${h} 실행 권한 없음` : `${h} 없음`));
}

/** settings.json의 hook 명령이 부르는 프로젝트 스크립트 경로 ($CLAUDE_PROJECT_DIR 기준 상대 경로) */
export function settingsHookScripts(settings) {
  const paths = new Set();
  for (const groups of Object.values(settings?.hooks || {})) {
    for (const g of groups || []) {
      for (const h of g.hooks || []) {
        const m = /\$CLAUDE_PROJECT_DIR\/([^"'\s]+)/.exec(h.command || '');
        if (m) paths.add(m[1]);
      }
    }
  }
  return [...paths].sort();
}

/** `git ls-files --eol` 출력에서 저장소는 LF인데 작업 트리가 CRLF인 파일 (autocrlf=true PC에서 prettier·spotless가 실패하는 원인) */
export function crlfFiles(eolOut) {
  return (eolOut || '')
    .split('\n')
    .filter((l) => /^i\/lf\s+w\/crlf\b/.test(l) && !/\beol=crlf\b/.test(l)) // .bat처럼 CRLF로 정한 파일은 뺀다
    .map((l) => l.split('\t').pop());
}

/** package-lock.json이 설치 기록(node_modules/.package-lock.json)보다 새로우면 의존성이 바뀐 것이다 */
export function depsStale(lockMtime, installedMtime) {
  if (!installedMtime) return true;
  return lockMtime > installedMtime + 1000;
}
