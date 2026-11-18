// 분석 단계의 화면 라벨 (docs/specs/F-05a-analysis-request-zip.md 5.1절). 단계가 늘면 여기에 더한다.

export const STATUS_LABELS = {
  FETCHING: '코드 수집 중',
  FILTERING: '파일 검사 중',
  SELECTING: '주요 파일 선별 중',
  EXPLAINING: '설명 생성 중',
  EXTRACTING: '개념 추출 중',
  QUIZ: '퀴즈 생성 중',
  DONE: '분석 완료',
  FAILED: '분석 실패',
}

/** DONE·FAILED가 아니면 진행 중 (폴링·진행률 막대 표시 기준). */
export function isInProgress(status) {
  return status !== 'DONE' && status !== 'FAILED'
}
