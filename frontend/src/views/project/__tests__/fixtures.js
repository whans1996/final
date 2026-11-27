// P-1a 프로젝트 화면 테스트용 응답 (docs/specs/P-1a-project-versions.md 3절 예시)

export const SHA_2 = '9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182'

export function projectSummary(partial) {
  return {
    id: 12,
    name: 'todo-app',
    sourceType: 'URL',
    sourceUrl: 'https://github.com/octo/todo-app',
    versionCount: 2,
    latestVersion: {
      id: 31,
      number: 2,
      commitSha: SHA_2,
      commitMessage: 'feat: JWT 로그인',
      committedAt: '2026-11-28T14:02:11+09:00',
      createdAt: '2026-11-28T17:30:00+09:00',
    },
    levels: ['BEGINNER', 'JUNIOR'],
    lastAnalyzedAt: '2026-11-28T17:30:00+09:00',
    ...partial,
  }
}

export function projectDetail(partial) {
  return {
    id: 12,
    name: 'todo-app',
    sourceType: 'URL',
    sourceUrl: 'https://github.com/octo/todo-app',
    createdAt: '2026-11-27T10:00:00+09:00',
    versions: [
      {
        id: 31,
        number: 2,
        commitSha: SHA_2,
        commitMessage: 'feat: JWT 로그인',
        committedAt: '2026-11-28T14:02:11+09:00',
        createdAt: '2026-11-28T17:30:05+09:00',
      },
      {
        id: 30,
        number: 1,
        commitSha: null,
        commitMessage: null,
        committedAt: null,
        createdAt: '2026-11-27T10:00:04+09:00',
      },
    ],
    analyses: [
      {
        id: 128,
        versionId: 31,
        level: 'BEGINNER',
        status: 'DONE',
        quizStatus: 'READY',
        title: 'todo-app',
        createdAt: '2026-11-28T17:30:00+09:00',
      },
      {
        id: 129,
        versionId: 31,
        level: 'SENIOR',
        status: 'EXPLAINING',
        quizStatus: null,
        title: 'todo-app',
        createdAt: '2026-11-28T17:40:00+09:00',
      },
      {
        id: 120,
        versionId: 30,
        level: 'JUNIOR',
        status: 'DONE',
        quizStatus: 'READY',
        title: 'todo-app',
        createdAt: '2026-11-27T10:00:00+09:00',
      },
    ],
    ...partial,
  }
}
