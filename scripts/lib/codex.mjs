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

/** 아직 해결되지 않은 마지막 인라인 지적 시각(ISO). 그보다 늦은 이관 마커가 있으면 '' (모두 이슈로 이관됨) */
export function unresolvedFindingAt(findingTimes, resolvedTimes) {
  const last = [...findingTimes].sort().at(-1) ?? '';
  const resolved = [...resolvedTimes].sort().at(-1) ?? '';
  return last && resolved > last ? '' : last;
}
