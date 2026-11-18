import http from '@/api/http'

// 분석 요청(ZIP)·목록·진행 상태 (F-05a, docs/specs/F-05a-analysis-request-zip.md 3절)

// 10MB 업로드는 느린 회선에서 기본 15초를 넘길 수 있다
const UPLOAD_TIMEOUT_MS = 60000

/**
 * multipart로 `file`·`level`·`consent` 파트를 보낸다. level을 생략하면 서버가 내 프로필 수준을 쓴다.
 * @returns {Promise<{ analysisId: number, status: string }>} 202
 */
export function createAnalysis({ file, level, consent }) {
  const form = new FormData()
  form.append('file', file)
  if (level) form.append('level', level)
  form.append('consent', String(consent))
  return http.post('/analyses', form, { timeout: UPLOAD_TIMEOUT_MS }).then((res) => res.data)
}

/** 내 분석 목록 (최신순). @returns {Promise<{ items: object[], page: number, size: number, total: number }>} */
export function fetchAnalyses({ page, size }) {
  return http.get('/analyses', { params: { page, size } }).then((res) => res.data)
}

/**
 * 진행 상태. 없거나 남의 분석이면 404 ANALYSIS_404.
 * @returns {Promise<{ analysisId: number, status: string, progress: number, errorCode: string|null, errorMessage: string|null, updatedAt: string }>}
 */
export function fetchAnalysisStatus(id) {
  return http.get(`/analyses/${id}/status`).then((res) => res.data)
}

// 결과 조회 (F-06, docs/specs/F-06-overview-explanation.md 3절). 없거나 남의 분석이면 404 ANALYSIS_404.

/** 분석 상세 (overview·fileCount·explainedFileCount 포함). 진행 중이면 overview가 null일 수 있다. */
export function fetchAnalysis(id) {
  return http.get(`/analyses/${id}`).then((res) => res.data)
}

/** 선별 파일 목록 (id 오름차순 = 선별 우선순위). @returns {Promise<{ id: number, path: string, layer: string, role: string|null, summary: string|null }[]>} */
export function fetchAnalysisFiles(id) {
  return http.get(`/analyses/${id}/files`).then((res) => res.data)
}

/** 파일 하나 (content 포함). 그 분석의 파일이 아니면 404 ANALYSIS_001. */
export function fetchAnalysisFile(id, fileId) {
  return http.get(`/analyses/${id}/files/${fileId}`).then((res) => res.data)
}
