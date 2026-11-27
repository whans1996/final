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
]

// 직접 쓴 순번 가드: `let lastRequest = 0` / `++lastHead` / `request !== lastSearch`, 주소 id 비교
const HAND_WRITTEN_GUARDS = [
  /\blet\s+last[A-Z]\w*\s*=\s*0\b/,
  /\+\+\s*last[A-Z]\w*/,
  /\brequest\s*[!=]==\s*last[A-Z]\w*/,
  /\bid\s*[!=]==\s*challengeId\.value/,
]

const source = (path) => readFileSync(resolve('src', path), 'utf8') // vitest root = frontend

describe('useLatestRequest 이관 (V-4 AC11)', () => {
  it.each(ADOPTED)('AC11 %s에는 직접 쓴 순번 가드가 없고 useLatestRequest를 쓴다', (path) => {
    const code = source(path)
    for (const pattern of HAND_WRITTEN_GUARDS) expect(code).not.toMatch(pattern)
    expect(code).toMatch(/useLatestRequest\(\)/)
  })
})
