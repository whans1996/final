// 회원가입·로그인 입력 검증. 규칙과 메시지는 백엔드(SignupRequest, LoginRequest)와 같다 (docs/specs/F-01-signup.md 2절).

export const LEVELS = [
  { value: 'BEGINNER', label: '입문', description: '코딩 용어부터 차근차근 알고 싶어요' },
  {
    value: 'JUNIOR',
    label: '주니어',
    description: '기본은 알고, 실무 코드의 흐름을 이해하고 싶어요',
  },
  { value: 'SENIOR', label: '숙련', description: '설계 의도와 트레이드오프 위주로 보고 싶어요' },
]

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD = /^(?=.*[A-Za-z])(?=.*\d)[\x20-\x7E]{8,64}$/
const NICKNAME = /^[가-힣A-Za-z0-9_]{2,20}$/

export const MESSAGES = {
  email: '올바른 이메일 형식이 아닙니다.',
  password: '비밀번호는 8~64자의 영문·숫자·특수문자이며 영문과 숫자를 모두 포함해야 합니다.',
  nickname: '닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다.',
  level: '학습 수준을 선택하세요.',
  emailRequired: '이메일을 입력하세요.',
  passwordRequired: '비밀번호를 입력하세요.',
}

/** @returns {Record<string, string>} 필드별 사유. 비어 있으면 통과 */
export function validateSignup({ email, password, nickname, level }) {
  const errors = {}
  const normalizedEmail = email.trim()
  if (!EMAIL.test(normalizedEmail) || normalizedEmail.length > 255) errors.email = MESSAGES.email
  if (!PASSWORD.test(password)) errors.password = MESSAGES.password
  if (!NICKNAME.test(nickname.trim())) errors.nickname = MESSAGES.nickname
  if (!LEVELS.some((l) => l.value === level)) errors.level = MESSAGES.level
  return errors
}

export function validateLogin({ email, password }) {
  const errors = {}
  if (!email.trim()) errors.email = MESSAGES.emailRequired
  if (!password) errors.password = MESSAGES.passwordRequired
  return errors
}
