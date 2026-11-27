import http from '@/api/http'

// 프로젝트·버전 (P-1a, docs/specs/P-1a-project-versions.md 3절). 모두 로그인 필요, 주인만.
// 없거나 남의 프로젝트, 분석을 모두 삭제한 프로젝트는 404 PROJECT_404.

export const PROJECT_NAME_MAX = 100

/**
 * @typedef {{ id: number, number: number, commitSha: string|null, commitMessage: string|null,
 *   committedAt: string|null, createdAt: string }} ProjectVersion
 * @typedef {{ id: number, name: string, sourceType: 'URL'|'ZIP', sourceUrl: string|null,
 *   versionCount: number, latestVersion: ProjectVersion|null, levels: string[], lastAnalyzedAt: string }} ProjectSummary
 * @typedef {{ id: number, versionId: number|null, level: string, status: string,
 *   quizStatus: string|null, title: string|null, createdAt: string }} ProjectAnalysis
 * @typedef {{ id: number, name: string, sourceType: 'URL'|'ZIP', sourceUrl: string|null, createdAt: string,
 *   versions: ProjectVersion[], analyses: ProjectAnalysis[] }} ProjectDetail
 */

/** 내 프로젝트 (마지막 분석 요청 최신순). @returns {Promise<{ items: ProjectSummary[], page: number, size: number, total: number }>} */
export function fetchProjects({ page, size }, { signal } = {}) {
  return http.get('/projects', { params: { page, size }, signal }).then((res) => res.data)
}

/** 프로젝트 상세 (버전 번호 내림차순, 삭제하지 않은 분석). @returns {Promise<ProjectDetail>} */
export function fetchProject(id, { signal } = {}) {
  return http.get(`/projects/${id}`, { signal }).then((res) => res.data)
}

/**
 * 기본 브랜치 최신 커밋과 분석한 최신 커밋 비교 (서버가 5분 저장). ZIP 프로젝트는 NOT_SUPPORTED.
 * @returns {Promise<{ status: 'NEW_COMMIT'|'UP_TO_DATE'|'UNKNOWN'|'NOT_SUPPORTED',
 *   latest: { sha: string, message: string|null, committedAt: string|null }|null,
 *   analyzedSha: string|null, checkedAt: string|null }>}
 */
export function fetchLatestCommit(id, { signal } = {}) {
  return http.get(`/projects/${id}/latest-commit`, { signal }).then((res) => res.data)
}

/** 이름 바꾸기 (앞뒤 공백을 뗀 1~100자, 아니면 400 COMMON_400 details.name). @returns {Promise<{ id: number, name: string }>} */
export function updateProject(id, { name }) {
  return http.patch(`/projects/${id}`, { name }).then((res) => res.data)
}
