import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import MyChallengeList from '@/components/challenge/MyChallengeList.vue'
import { routes } from '@/router'
import { myChallenge } from './fixtures'

async function mountList(props) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  return mount(MyChallengeList, { props, global: { plugins: [router] } })
}

describe('MyChallengeList (F-25 AC14·AC15)', () => {
  it('AC15 항목마다 제목·기간·달성도 바, 달성이면 "달성" 배지, 누르면 상세로 간다', async () => {
    const wrapper = await mountList({
      items: [
        myChallenge(7, { title: '퀴즈 챌린지', progress: 3, goalValue: 10 }),
        myChallenge(8, {
          startDate: '2026-12-01',
          endDate: '2026-12-31',
          progress: 5,
          goalValue: 5,
          completedAt: '2026-11-24T10:15:00+09:00',
        }),
      ],
    })
    const rows = wrapper.findAll('[data-challenge-id]')

    expect(rows).toHaveLength(2)
    expect(rows[0].attributes('href')).toBe('/challenges/7')
    expect(rows[0].find('[data-field="title"]').text()).toBe('퀴즈 챌린지')
    expect(rows[0].find('[data-field="period"]').text()).toBe('2026.11.20 ~ 11.30')
    expect(rows[0].text()).toContain('3 / 10회')
    expect(rows[0].find('[data-badge="completed"]').exists()).toBe(false)
    expect(rows[1].find('[data-field="period"]').text()).toBe('2026.12.01 ~ 12.31')
    expect(rows[1].find('[data-badge="completed"]').text()).toBe('달성')
  })

  it('AC14 today를 주면 기간 대신 종료까지 D-n을 보여 준다', async () => {
    const wrapper = await mountList({
      items: [myChallenge(7, { endDate: '2026-11-30' }), myChallenge(8, { endDate: '2026-11-24' })],
      today: '2026-11-24',
    })
    const ddays = wrapper.findAll('[data-field="dday"]').map((el) => el.text())

    expect(ddays).toEqual(['D-6', 'D-day'])
    expect(wrapper.find('[data-field="period"]').exists()).toBe(false)
  })
})
