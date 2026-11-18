import http from '@/api/http'

// 회원가입(F-01), 로그인·재발급·로그아웃(F-02). Refresh 토큰은 HttpOnly 쿠키라 JS에서 다루지 않는다.

/** @returns {Promise<{ id: number }>} */
export function signup({ email, password, nickname, level }) {
  return http.post('/auth/signup', { email, password, nickname, level }).then((res) => res.data)
}

/** @returns {Promise<{ accessToken: string, expiresIn: number, user: object }>} */
export function login({ email, password }) {
  return http.post('/auth/login', { email, password }).then((res) => res.data)
}

/** refresh_token 쿠키로 새 Access 토큰을 받는다. 응답 형식은 login과 같다. */
export function refresh() {
  return http.post('/auth/refresh').then((res) => res.data)
}

export function logout() {
  return http.post('/auth/logout')
}
