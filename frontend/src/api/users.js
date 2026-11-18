import http from '@/api/http'

// 프로필 조회·수정, 학습 통계, 회원 탈퇴 (F-03, docs/specs/F-03-profile.md 3절)

/** @returns {Promise<{ id: number, email: string, nickname: string, bio: string|null, level: string, role: string, createdAt: string }>} */
export function fetchMe() {
  return http.get('/users/me').then((res) => res.data)
}

/** 보낸 필드만 바뀐다. bio를 ''로 보내면 지운다. 응답은 fetchMe와 같은 형식. */
export function updateMe({ nickname, bio, level }) {
  return http.patch('/users/me', { nickname, bio, level }).then((res) => res.data)
}

/** 비밀번호를 다시 확인하고 탈퇴한다. 서버가 refresh 쿠키와 토큰을 폐기한다 (204). */
export function withdraw(password) {
  return http.delete('/users/me', { data: { password } })
}

/** 공개 프로필 (email 없음). 없거나 탈퇴했으면 404 USER_404. */
export function fetchUser(id) {
  return http.get(`/users/${id}`).then((res) => res.data)
}

/** @returns {Promise<{ analysisCount: number, learnedConceptCount: number, quizAccuracy: number|null }>} */
export function fetchUserStats(id) {
  return http.get(`/users/${id}/stats`).then((res) => res.data)
}
