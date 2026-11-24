import http from '@/api/http'

// 분석 요청(ZIP F-05a·저장소 URL F-05b)·목록·진행 상태 (docs/specs/F-05a-analysis-request-zip.md·F-05b 3절)

// 10MB 업로드는 느린 회선에서 기본 15초를 넘길 수 있다
const UPLOAD_TIMEOUT_MS = 60000

/**
 * multipart로 `file` 또는 `sourceUrl` 파트 하나와 `level`·`consent` 파트를 보낸다. file이 있으면 file을,
 * 없으면 sourceUrl을 보낸다. level을 생략하면 서버가 내 프로필 수준을 쓴다.
 * @returns {Promise<{ analysisId: number, status: string }>} 202
 */
export function createAnalysis({ file, sourceUrl, level, consent }) {
  const form = new FormData()
  if (file) form.append('file', file)
  else form.append('sourceUrl', sourceUrl)
  if (level) form.append('level', level)
  form.append('consent', String(consent))
  return http.post('/analyses', form, { timeout: UPLOAD_TIMEOUT_MS }).then((res) => res.data)
}

/** 내 분석 목록 (최신순). 항목에 memo(F-19)가 있다. @returns {Promise<{ items: object[], page: number, size: number, total: number }>} */
export function fetchAnalyses({ page, size }) {
  return http.get('/analyses', { params: { page, size } }).then((res) => res.data)
}

/**
 * 진행 상태. 없거나 볼 수 없는 분석이면 404 ANALYSIS_404.
 * sourceUrl은 URL 분석의 저장소 주소이고 ZIP 분석이면 null이다 (FAILED 화면의 "저장소 보기", F-19 AC12).
 * @returns {Promise<{ analysisId: number, status: string, progress: number, errorCode: string|null, errorMessage: string|null, sourceUrl: string|null, updatedAt: string }>}
 */
export function fetchAnalysisStatus(id) {
  return http.get(`/analyses/${id}/status`).then((res) => res.data)
}

// 결과 조회 (F-06, docs/specs/F-06-overview-explanation.md 3절). 내 분석과 공개·완료된 남의 분석을 볼 수 있고 (F-19),
// 없거나 볼 수 없으면 404 ANALYSIS_404.

/**
 * 분석 상세 (overview·fileCount·explainedFileCount 포함). 진행 중이면 overview가 null일 수 있다.
 * sourceUrl은 URL 분석의 정규화한 저장소 주소이고 ZIP 분석이면 null이다 (F-05b AC12).
 * mine은 내 분석인지, owner는 주인 `{ id, nickname }`, memo는 주인에게만 온다 (남이면 null, F-19 AC9·AC11).
 * @returns {Promise<{ id: number, title: string|null, memo: string|null, mine: boolean, owner: { id: number, nickname: string }, projectName: string|null, sourceType: string, sourceUrl: string|null, level: string, status: string, progress: number, isPublic: boolean, errorCode: string|null, errorMessage: string|null, createdAt: string, fileCount: number, explainedFileCount: number, overview: object|null }>}
 */
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

// 실행 흐름 (F-07, docs/specs/F-07-execution-flow.md 3절). 생성은 비동기라 PENDING이 있으면 목록을 다시 불러 확인한다.

/**
 * 이 분석의 흐름 전체 (id 오름차순). DONE이 아니면 summary·steps가 null.
 * @returns {Promise<{ id: number, method: string, path: string, status: 'PENDING'|'DONE'|'FAILED', errorCode: string|null, errorMessage: string|null, summary: string|null, steps: object[]|null, createdAt: string }[]>}
 */
export function fetchAnalysisFlows(id) {
  return http.get(`/analyses/${id}/flows`).then((res) => res.data)
}

/**
 * 개요 apis에서 고른 API(1~3개)의 흐름 생성을 요청한다. 202로 흐름 전체를 돌려준다.
 * 400 COMMON_400·ANALYSIS_002·ANALYSIS_003, 409 ANALYSIS_409, 404 ANALYSIS_404.
 * @param {{ method: string, path: string }[]} endpoints
 */
export function requestAnalysisFlows(id, endpoints) {
  return http.post(`/analyses/${id}/flows`, { endpoints }).then((res) => res.data)
}

// 개념 카드 (F-08, docs/specs/F-08-concept-extraction.md 3절). 없거나 남의 분석이면 404 ANALYSIS_404.

/**
 * 분석 코드에 쓰인 공개 카드와 위치. 검수 전 AI 초안은 수만 준다. 추출 전이면 빈 목록.
 * @returns {Promise<{ items: { concept: object, fileId: number|null, filePath: string|null, lineHint: string|null, contextNote: string|null }[], pendingCount: number }>}
 */
export function fetchAnalysisConcepts(id) {
  return http.get(`/analyses/${id}/concepts`).then((res) => res.data)
}

// 분석 관리 (F-19, docs/specs/F-19-analysis-manage.md 3절). 주인만 되고, 남의 분석·삭제·없음은 404 ANALYSIS_404.

/**
 * 제목·메모를 보낸 값으로 바꾼다 (빈 문자열은 null). 400 COMMON_400(title 100자·memo 1000자 초과).
 * @returns {Promise<object>} 바뀐 상세 (fetchAnalysis와 같은 형식)
 */
export function updateAnalysis(id, { title, memo }) {
  return http.patch(`/analyses/${id}`, { title, memo }).then((res) => res.data)
}

/**
 * 공개 여부. 공개는 DONE일 때만 되고 아니면 409 ANALYSIS_006.
 * @returns {Promise<{ id: number, isPublic: boolean }>}
 */
export function updateVisibility(id, isPublic) {
  return http.patch(`/analyses/${id}/visibility`, { isPublic }).then((res) => res.data)
}

/** 삭제 (204). 진행 중이면 409 ANALYSIS_005. 퀴즈 풀이 기록은 남는다. */
export function deleteAnalysis(id) {
  return http.delete(`/analyses/${id}`).then(() => undefined)
}

// 구조 시각화 (F-21a, docs/specs/F-21a-structure-graph.md 3절). 없거나 볼 수 없는 분석이면 404 ANALYSIS_404.

/**
 * 파일 의존 그래프. 간선은 코드에서 확정한 연결만 오고, 확정하지 못한 연결은 unresolved에 사유와 함께 온다.
 * @returns {Promise<{ nodes: { fileId: number, path: string, name: string, layer: string, role: string|null }[], edges: { from: number, to: number, type: 'IMPORT'|'MYBATIS'|'HTTP', line: number }[], unresolved: { fileId: number, line: number, kind: string, reason: string, detail: string }[], edgesTruncated: boolean, unresolvedTruncated: boolean }>}
 */
export function fetchAnalysisStructure(id) {
  return http.get(`/analyses/${id}/structure`).then((res) => res.data)
}
