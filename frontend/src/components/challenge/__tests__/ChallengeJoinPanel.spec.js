import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChallengeJoinPanel from '@/components/challenge/ChallengeJoinPanel.vue'
import * as challengesApi from '@/api/challenges'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { apiError } from '@/views/__tests__/viewTestUtils'
import { challengeDetail, participation } from './fixtures'

vi.mock('@/api/challenges')

async function mountPanel(challenge, { loggedIn = true } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (loggedIn) {
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
    })
  }
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(`/challenges/${challenge.id}`)
  await router.isReady()
  const wrapper = mount(ChallengeJoinPanel, {
    props: { challenge },
    global: { plugins: [pinia, router] },
    attachTo: document.body,
  })
  return { wrapper, router }
}

const find = (wrapper, action) => wrapper.find(`[data-action="${action}"]`)

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('ChallengeJoinPanel (F-25 AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ''
  })

  it('AC13 비로그인이 "참여하기"를 누르면 로그인 후 이 화면으로 돌아오게 로그인으로 보낸다', async () => {
    const { wrapper, router } = await mountPanel(challengeDetail(7), { loggedIn: false })

    await find(wrapper, 'join').trigger('click')
    await flushPromises()

    expect(challengesApi.joinChallenge).not.toHaveBeenCalled()
    // 로그인 화면 컴포넌트를 지연 로드하므로 이동이 끝날 때까지 기다린다
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('login'))
    expect(router.currentRoute.value.query.redirect).toBe('/challenges/7')
  })

  it('AC13 미참여 회원이 "참여하기"를 누르면 요청 중 버튼을 막고, 결과를 change로 알린다', async () => {
    const result = { joined: true, participantCount: 3, participation: participation() }
    const pending = deferred()
    challengesApi.joinChallenge.mockReturnValue(pending.promise)
    const { wrapper } = await mountPanel(challengeDetail(7))

    await find(wrapper, 'join').trigger('click')
    expect(find(wrapper, 'join').attributes('disabled')).toBeDefined()
    await find(wrapper, 'join').trigger('click')
    expect(challengesApi.joinChallenge).toHaveBeenCalledTimes(1)
    expect(challengesApi.joinChallenge).toHaveBeenCalledWith(7)

    pending.resolve(result)
    await flushPromises()
    expect(wrapper.emitted('change')).toEqual([[result]])
  })

  it('AC13 참여 중이면 달성도 바와 "참여 취소", 확인 모달에서 확정해야 취소한다', async () => {
    const result = { joined: false, participantCount: 1, participation: null }
    challengesApi.cancelChallenge.mockResolvedValue(result)
    const { wrapper } = await mountPanel(challengeDetail(7, { participation: participation() }))

    expect(wrapper.find('[role="progressbar"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('3 / 10회')
    expect(find(wrapper, 'join').exists()).toBe(false)

    await find(wrapper, 'cancel').trigger('click')
    expect(wrapper.find('[role="dialog"]').exists()).toBe(true)
    await find(wrapper, 'close-dialog').trigger('click')
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(challengesApi.cancelChallenge).not.toHaveBeenCalled()

    await find(wrapper, 'cancel').trigger('click')
    await find(wrapper, 'confirm-cancel').trigger('click')
    await flushPromises()

    expect(challengesApi.cancelChallenge).toHaveBeenCalledWith(7)
    expect(wrapper.emitted('change')).toEqual([[result]])
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('AC13 달성했으면 "달성 완료 · 11월 24일" 배지만 있고 취소 버튼이 없다', async () => {
    const { wrapper } = await mountPanel(
      challengeDetail(7, {
        participation: participation({
          progress: 10,
          completedAt: '2026-11-24T10:15:00+09:00',
        }),
      }),
    )

    expect(wrapper.find('[data-badge="completed"]').text()).toBe('달성 완료 · 11월 24일')
    expect(find(wrapper, 'cancel').exists()).toBe(false)
  })

  it('AC13 종료된 챌린지에 참여하지 않았으면 버튼 없이 안내만 보인다', async () => {
    const { wrapper } = await mountPanel(challengeDetail(7, { status: 'ENDED' }))

    expect(wrapper.text()).toContain('종료된 챌린지입니다')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('AC13 409 등 에러는 버튼 아래에 서버 메시지를 보여 주고 버튼을 다시 쓸 수 있다', async () => {
    challengesApi.joinChallenge.mockRejectedValue(
      apiError(409, {
        code: 'CHALLENGE_002',
        message: '종료된 챌린지는 참여하거나 취소할 수 없습니다.',
      }),
    )
    const { wrapper } = await mountPanel(challengeDetail(7))

    await find(wrapper, 'join').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="join-error"]').text()).toBe(
      '종료된 챌린지는 참여하거나 취소할 수 없습니다.',
    )
    expect(find(wrapper, 'join').attributes('disabled')).toBeUndefined()
    expect(wrapper.emitted('change')).toBeUndefined()
  })

  it('AC13 취소 실패(409 CHALLENGE_003)도 모달을 닫고 버튼 아래에 메시지를 보여 준다', async () => {
    challengesApi.cancelChallenge.mockRejectedValue(
      apiError(409, {
        code: 'CHALLENGE_003',
        message: '달성한 챌린지는 참여를 취소할 수 없습니다.',
      }),
    )
    const { wrapper } = await mountPanel(challengeDetail(7, { participation: participation() }))

    await find(wrapper, 'cancel').trigger('click')
    await find(wrapper, 'confirm-cancel').trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="join-error"]').text()).toBe(
      '달성한 챌린지는 참여를 취소할 수 없습니다.',
    )
  })

  it.each([
    ['ANALYSIS_COUNT', '분석 요청하기', '/analyses/new'],
    ['QUIZ_COUNT', '퀴즈 풀러 가기', '/analyses'],
    ['REVIEW_COUNT', '복습하러 가기', '/review-plans'],
  ])(
    '#211 참여 중인 진행 중 %s 챌린지는 목표를 채우러 갈 링크를 보여 준다',
    async (goalType, label, href) => {
      const { wrapper } = await mountPanel(
        challengeDetail(7, { goalType, participation: participation() }),
      )

      const link = find(wrapper, 'go-goal')
      expect(link.text()).toBe(label)
      expect(link.attributes('href')).toBe(href)
    },
  )

  it('#211 참여하지 않았거나, 예정·종료이거나, 달성했으면 목표 링크를 보여 주지 않는다', async () => {
    const cases = [
      challengeDetail(7),
      challengeDetail(7, { status: 'UPCOMING', participation: participation({ progress: 0 }) }),
      challengeDetail(7, { status: 'ENDED', participation: participation() }),
      challengeDetail(7, {
        participation: participation({ progress: 10, completedAt: '2026-11-25T10:00:00+09:00' }),
      }),
    ]
    for (const challenge of cases) {
      const { wrapper } = await mountPanel(challenge)
      expect(find(wrapper, 'go-goal').exists()).toBe(false)
      wrapper.unmount()
    }
  })
})
