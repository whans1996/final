import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ChallengeDetailView from '@/views/challenge/ChallengeDetailView.vue'
import ChallengeJoinPanel from '@/components/challenge/ChallengeJoinPanel.vue'
import * as challengesApi from '@/api/challenges'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'
import { challengeDetail, participation } from '@/components/challenge/__tests__/fixtures'

vi.mock('@/api/challenges')

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })

async function mountDetail(path = '/challenges/7') {
  const mounted = await mountAt(ChallengeDetailView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

const field = (wrapper, name) => wrapper.find(`[data-field="${name}"]`)

describe('ChallengeDetailView (F-25 AC13 SC-13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // 서울 오늘 = 2026-11-24
    vi.useFakeTimers({ now: new Date('2026-11-24T10:00:00+09:00'), toFake: ['Date'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC13 이미지·제목·상태·기간과 종료까지 D-n·목표·참여자 수·설명을 보여 준다', async () => {
    challengesApi.fetchChallenge.mockResolvedValue(
      challengeDetail(7, {
        title: '11월 퀴즈 챌린지',
        imageUrl: '/uploads/challenges/a.png',
        participantCount: 2,
      }),
    )
    const { wrapper } = await mountDetail()

    expect(challengesApi.fetchChallenge).toHaveBeenCalledWith(7)
    expect(wrapper.find('img').attributes('src')).toBe('/uploads/challenges/a.png')
    expect(wrapper.find('h1').text()).toBe('11월 퀴즈 챌린지')
    expect(field(wrapper, 'status').text()).toBe('진행 중')
    expect(field(wrapper, 'period').text()).toBe('2026.11.20 ~ 11.30')
    expect(field(wrapper, 'dday').text()).toBe('D-6')
    expect(field(wrapper, 'goal').text()).toBe('퀴즈 10회')
    expect(field(wrapper, 'participants').text()).toBe('참여자 2명')
    expect(field(wrapper, 'description').text()).toBe(
      '분석한 프로젝트의 퀴즈를 기간 안에 10번 풀어 보세요.',
    )
  })

  it('AC13 예정은 시작까지 D-n, 종료는 D-n이 없다. 이미지가 없으면 기본 그림', async () => {
    challengesApi.fetchChallenge.mockResolvedValueOnce(
      challengeDetail(7, { status: 'UPCOMING', startDate: '2026-12-01', endDate: '2026-12-31' }),
    )
    const upcoming = await mountDetail()
    expect(field(upcoming.wrapper, 'dday').text()).toBe('D-7')
    expect(upcoming.wrapper.find('[data-thumb="QUIZ_COUNT"]').exists()).toBe(true)

    challengesApi.fetchChallenge.mockResolvedValueOnce(
      challengeDetail(7, { status: 'ENDED', startDate: '2026-11-01', endDate: '2026-11-10' }),
    )
    const ended = await mountDetail()
    expect(field(ended.wrapper, 'dday').exists()).toBe(false)
  })

  it('AC13 D-n은 브라우저 시간대가 아닌 서울 오늘 기준이다 (UTC 11-24 15:00:01 = 서울 11-25)', async () => {
    vi.setSystemTime(new Date('2026-11-24T15:00:01Z'))
    challengesApi.fetchChallenge.mockResolvedValue(challengeDetail(7))

    const { wrapper } = await mountDetail()

    expect(field(wrapper, 'dday').text()).toBe('D-5')
  })

  it('AC13 참여하면 참여자 수와 달성도를 응답으로 바꾼다', async () => {
    challengesApi.fetchChallenge.mockResolvedValue(challengeDetail(7, { participantCount: 2 }))
    const { wrapper } = await mountDetail()

    wrapper.findComponent(ChallengeJoinPanel).vm.$emit('change', {
      joined: true,
      participantCount: 3,
      participation: participation({ progress: 2 }),
    })
    await flushPromises()

    expect(field(wrapper, 'participants').text()).toBe('참여자 3명')
    expect(wrapper.text()).toContain('2 / 10회')
  })

  it('AC13 없는 id는 "챌린지를 찾을 수 없습니다", 그 밖의 에러는 서버 메시지', async () => {
    challengesApi.fetchChallenge.mockRejectedValueOnce(
      apiError(404, { code: 'CHALLENGE_404', message: '챌린지를 찾을 수 없습니다.' }),
    )
    const missing = await mountDetail('/challenges/999')
    expect(missing.wrapper.text()).toContain('챌린지를 찾을 수 없습니다')
    expect(missing.wrapper.findComponent(ChallengeJoinPanel).exists()).toBe(false)

    challengesApi.fetchChallenge.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )
    const failed = await mountDetail()
    expect(failed.wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')
  })
})
