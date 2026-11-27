import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// V-4 spec 5.2절: 손으로 쓴 순번 가드를 useLatestRequest로 옮긴 파일과 새 가드를 단 파일.
// 예외(ConceptReviews.vue의 읽기·쓰기 직렬화 generation, usePolling)는 일부러 넣지 않는다.
// A 영역 파일은 A가 자기 PR에서 옮긴다 (follow-up 이슈).
const ADOPTED = [
  'views/concept/ConceptListView.vue',
  'views/community/PostListView.vue',
  'views/admin/concept/AdminConceptListView.vue',
  'components/community/AttachmentPicker.vue',
  'views/admin/challenge/AdminChallengeListView.vue',
  'views/admin/challenge/AdminChallengeEditView.vue',
  'views/concept/ConceptDetailView.vue',
  'views/community/PostDetailView.vue',
  'views/UserProfileView.vue',
  'views/UserFollowsView.vue',
  'components/follow/FollowSummary.vue',
  'components/concept/UserWishes.vue',
  'components/community/PostComments.vue',
  'composables/useMoreList.js',
  'views/community/PostWriteView.vue',
  'components/concept/WishButton.vue',
  // A 영역 (P-1a, #250)
  'views/AnalysisListView.vue',
  'views/AnalysisDetailView.vue',
  'views/AnalysisRequestView.vue',
  'views/project/ProjectListView.vue',
  'views/project/ProjectDetailView.vue',
  'components/project/LatestCommitBadge.vue',
]

// 직접 쓴 순번 가드: `let lastRequest = 0` / `++lastHead` / `request !== lastSearch`, 주소 id 비교
const HAND_WRITTEN_GUARDS = [
  /\blet\s+last[A-Z]\w*\s*=\s*0\b/,
  /\+\+\s*last[A-Z]\w*/,
  /\brequest\s*[!=]==\s*last[A-Z]\w*/,
  /\bid\s*[!=]==\s*challengeId\.value/,
]

// 한계: 이름이 last로 시작하지 않는 순번(`++seq`)이나 다른 id 비교(`id !== props.userId`)는 잡지 않는다.
// 새 수동 가드를 막는 것은 frontend/CLAUDE.md 규칙과 리뷰이고, 이 검사는 이관한 파일이 되돌아가지 않게 하는 용도다
const source = (path) => readFileSync(resolve('src', path), 'utf8') // vitest root = frontend

describe('useLatestRequest 이관 (V-4 AC11)', () => {
  it.each(ADOPTED)('AC11 %s에는 직접 쓴 순번 가드가 없고 useLatestRequest를 쓴다', (path) => {
    const code = source(path)
    for (const pattern of HAND_WRITTEN_GUARDS) expect(code).not.toMatch(pattern)
    expect(code).toMatch(/useLatestRequest\(\)/)
    // 인스턴스만 만들고 쓰지 않는 경우를 거른다: 요청을 시작하고 응답에서 현재인지 확인해야 한다
    expect(code).toMatch(/\.begin\(\)/)
    expect(code).toMatch(/\.isCurrent\(\)/)
  })
})
