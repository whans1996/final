import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChallengeCard from '@/components/challenge/ChallengeCard.vue'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'
import { routes } from '@/router'
import { challengeSummary, myChallenge } from './fixtures'

async function mountCard(challenge) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  return mount(ChallengeCard, { props: { challenge }, global: { plugins: [router] } })
}

describe('ChallengeCard (F-25 AC12)', () => {
  it('AC12 이미지·제목·기간·목표·참여자 수를 보여 주고 상세로 이동한다', async () => {
    const wrapper = await mountCard(
      challengeSummary(7, {
        title: '12월 퀴즈 챌린지',
        imageUrl: '/uploads/challenges/a.png',
        startDate: '2026-12-01',
        endDate: '2026-12-31',
        participantCount: 2,
      }),
    )

    expect(wrapper.find('a').attributes('href')).toBe('/challenges/7')
    expect(wrapper.find('img').attributes('src')).toBe('/uploads/challenges/a.png')
    expect(wrapper.find('[data-field="title"]').text()).toBe('12월 퀴즈 챌린지')
    expect(wrapper.find('[data-field="period"]').text()).toBe('2026.12.01 ~ 12.31')
    expect(wrapper.find('[data-field="goal"]').text()).toBe('퀴즈 10회')
    expect(wrapper.find('[data-field="participants"]').text()).toBe('참여자 2명')
    expect(wrapper.find('[data-badge="joined"]').exists()).toBe(false)
  })

  it('AC12 이미지가 없으면 목표 유형별 기본 그림, 참여 중이면 "참여 중" 배지', async () => {
    const wrapper = await mountCard(
      challengeSummary(7, { goalType: 'REVIEW_COUNT', imageUrl: null, joined: true }),
    )

    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('[data-thumb="REVIEW_COUNT"]').text()).toBe('📚')
    expect(wrapper.find('[data-badge="joined"]').text()).toBe('참여 중')
  })

  it('AC12 내 챌린지 항목은 참여자 수 대신 달성도 바, 달성했으면 "달성" 배지', async () => {
    const ongoing = await mountCard(myChallenge(7, { progress: 3, goalValue: 10 }))
    expect(ongoing.find('[data-field="participants"]').exists()).toBe(false)
    expect(ongoing.findComponent(ChallengeProgressBar).text()).toContain('3 / 10회')
    expect(ongoing.find('[data-badge="completed"]').exists()).toBe(false)

    const done = await mountCard(
      myChallenge(8, { progress: 10, goalValue: 10, completedAt: '2026-11-24T10:15:00+09:00' }),
    )
    expect(done.find('[data-badge="completed"]').text()).toBe('달성')
  })
})

describe('ChallengeProgressBar (F-25 AC12·AC13)', () => {
  it('AC13 "3 / 10회"와 progressbar 속성, 넘치지 않는 너비', () => {
    const wrapper = mount(ChallengeProgressBar, { props: { progress: 3, goalValue: 10 } })
    const bar = wrapper.find('[role="progressbar"]')

    expect(wrapper.text()).toContain('3 / 10회')
    expect(bar.attributes('aria-valuenow')).toBe('3')
    expect(bar.attributes('aria-valuemax')).toBe('10')
    expect(wrapper.find('.fill').attributes('style')).toContain('width: 30%')
  })
})
