// F-25 챌린지 화면 테스트용 응답 (spec 3절 형식)

/** GET /api/challenges 항목 */
export function challengeSummary(id, partial = {}) {
  return {
    id,
    title: `챌린지${id}`,
    imageUrl: null,
    startDate: '2026-11-20',
    endDate: '2026-11-30',
    goalType: 'QUIZ_COUNT',
    goalValue: 10,
    status: 'ONGOING',
    participantCount: 2,
    joined: false,
    ...partial,
  }
}

/** GET /api/challenges/{id} */
export function challengeDetail(id, partial = {}) {
  return {
    id,
    title: `챌린지${id}`,
    description: '분석한 프로젝트의 퀴즈를 기간 안에 10번 풀어 보세요.',
    imageUrl: null,
    startDate: '2026-11-20',
    endDate: '2026-11-30',
    goalType: 'QUIZ_COUNT',
    goalValue: 10,
    status: 'ONGOING',
    participantCount: 2,
    participation: null,
    ...partial,
  }
}

export function participation(partial = {}) {
  return {
    progress: 3,
    goalValue: 10,
    completedAt: null,
    joinedAt: '2026-11-22T21:05:00+09:00',
    ...partial,
  }
}

/** GET /api/users/{me|id}/challenges 항목 */
export function myChallenge(id, partial = {}) {
  return {
    id,
    title: `챌린지${id}`,
    imageUrl: null,
    startDate: '2026-11-20',
    endDate: '2026-11-30',
    goalType: 'QUIZ_COUNT',
    goalValue: 10,
    status: 'ONGOING',
    progress: 3,
    completedAt: null,
    joinedAt: '2026-11-22T21:05:00+09:00',
    ...partial,
  }
}

export function page(items, partial = {}) {
  return { items, page: 1, size: 12, total: items.length, ...partial }
}
