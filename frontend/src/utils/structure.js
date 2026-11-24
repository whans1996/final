// 구조 그래프 화면 문구 (docs/specs/F-21a-structure-graph.md AC15·3.1절).

/** 확인하지 못한 연결의 사유 (reason 값이 kind마다 겹치지 않아 reason만으로 고른다). */
export const UNRESOLVED_REASON_LABELS = {
  NOT_SELECTED: '분석에 포함되지 않은 프로젝트 파일을 참조합니다',
  PARSE_ERROR: '코드를 해석하지 못했습니다',
  URL_UNKNOWN: '요청 주소가 실행 중에 정해져 코드만으로 확정할 수 없습니다',
  BASE_URL_UNKNOWN: '기본 주소(baseURL)가 환경 설정에서 와 확정할 수 없습니다',
  NO_MATCHING_API: '분석한 컨트롤러에서 이 요청을 받는 API를 찾지 못했습니다',
  AMBIGUOUS: '이 요청을 받을 수 있는 API가 여러 파일에 있습니다',
}

/** 연결 목록의 간선 종류. 그림에서 IMPORT·MYBATIS는 실선, HTTP는 점선이다. */
export const EDGE_TYPE_LABELS = {
  IMPORT: '코드 참조',
  MYBATIS: 'MyBatis 매핑',
  HTTP: 'API 호출',
}

export function unresolvedReasonLabel(reason) {
  return UNRESOLVED_REASON_LABELS[reason] ?? reason
}

export function edgeTypeLabel(type) {
  return EDGE_TYPE_LABELS[type] ?? type
}
