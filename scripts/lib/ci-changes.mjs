// CI changes job의 be·fe 판정 보정 (HP-4 AC8, #224). git·gh 호출 없이 계산하는 부분이다 (scripts/test/ci-changes.test.mjs).
// paths-filter는 PR 전체 diff로 판별하므로, BE 변경이 있는 PR에 NOW·작업로그만 push해도 Backend·Frontend를 처음부터 다시 돌렸다.
// 직전에 성공한 실행(head S) 이후 바뀐 파일이 문서뿐이면 S의 검증 결과가 그대로 유효하므로 건너뛴다 (skipped = 필수 체크 통과).

const OPENAPI = 'docs/api/openapi.json';

/** 문서만 바뀌었나: 하나 이상이고 모두 docs/ 아래이며 openapi.json(BE 테스트가 코드와 비교)은 아니다 */
export function isDocsOnly(files) {
  return files.length > 0 && files.every((f) => f.startsWith('docs/') && f !== OPENAPI);
}

/** `gh run list --json headSha`(최근 것부터)에서 지금 head가 아닌 가장 최근 성공 실행의 head sha. 없으면 null */
export function lastSuccessSha(runs, headSha) {
  return runs.find((r) => r.headSha && r.headSha !== headSha)?.headSha ?? null;
}

/**
 * 최종 be·fe. filter는 paths-filter 결과, compare는 GitHub compare API(S...head)의 { status, files, truncated } (실패면 null).
 * pull_request synchronize에서만, S가 head의 조상(status 'ahead')이고 그 사이가 문서뿐일 때 둘 다 false로 바꾼다.
 * 그 밖(push·opened·reopened, 직전 성공 없음, 브랜치 재작성, 문서 밖 변경, 비교 실패·잘림)은 filter 그대로.
 */
export function decideChanges({ event, action, filter, lastSuccess, compare }) {
  const keep = (reason) => ({ be: !!filter.be, fe: !!filter.fe, reason });
  if (event !== 'pull_request' || action !== 'synchronize') return keep(`${event}${action ? ` ${action}` : ''}: PR 전체 diff로 판별`);
  if (!filter.be && !filter.fe) return keep('BE·FE 변경 없음');
  if (!lastSuccess) return keep('직전 성공 실행 없음: PR 전체 diff로 판별');
  const s = lastSuccess.slice(0, 7);
  if (!compare) return keep(`직전 성공 ${s}와 비교 실패: PR 전체 diff로 판별`);
  if (compare.status !== 'ahead') return keep(`직전 성공 ${s}가 head의 조상이 아님(${compare.status}): PR 전체 diff로 판별`);
  if (compare.truncated || !isDocsOnly(compare.files || [])) return keep(`직전 성공 ${s} 이후 문서 밖 파일도 변경: PR 전체 diff로 판별`);
  return { be: false, fe: false, reason: `직전 성공 ${s} 이후 문서만 변경 (${compare.files.length}개): Backend·Frontend 건너뜀` };
}
