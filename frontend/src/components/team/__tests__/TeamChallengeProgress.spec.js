import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TeamChallengeProgress from '@/components/team/TeamChallengeProgress.vue'

function challenge(partial) {
  return {
    challengeId: 7,
    analysisId: 42,
    title: 'spring-realworld',
    projectName: 'spring-realworld',
    startDate: '2026-11-24',
    endDate: '2026-11-30',
    targetRate: 80,
    status: 'ONGOING',
    memberCount: 3,
    solvedMemberCount: 2,
    achievedMemberCount: 1,
    achieved: false,
    members: [
      {
        userId: 3,
        nickname: 'vibecoder',
        role: 'OWNER',
        attempt: { attemptId: 16, score: 3, total: 5, createdAt: '2026-11-24T15:02:00+09:00' },
        achieved: false,
      },
      {
        userId: 5,
        nickname: 'jsk',
        role: 'MEMBER',
        attempt: { attemptId: 18, score: 2, total: 3, createdAt: '2026-11-24T16:40:00+09:00' },
        achieved: true,
      },
      { userId: 9, nickname: 'newbie', role: 'MEMBER', attempt: null, achieved: false },
    ],
    createdAt: '2026-11-24T10:00:00+09:00',
    ...partial,
  }
}

describe('TeamChallengeProgress SC-24 (F-20c AC12)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 24, 10, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC12 목표·기간·상태·달성 수와 멤버별 점수·풀이 없음·달성 표시를 보여 준다', () => {
    const wrapper = mount(TeamChallengeProgress, { props: { challenge: challenge() } })

    expect(wrapper.find('[data-role="goal"]').text()).toBe('정답률 80% 이상 · 11-24 ~ 11-30')
    expect(wrapper.find('[data-role="status"]').text()).toBe('6일 남음')
    expect(wrapper.find('[data-role="progress"]').text()).toBe('1/3명 달성')
    const [owner, member, newbie] = wrapper.findAll('[data-challenge-member]')
    expect(owner.text()).toContain('vibecoder')
    expect(owner.find('[data-role="result"]').text()).toBe('3/5 (60%)')
    expect(owner.find('[data-role="member-achieved"]').exists()).toBe(false)
    expect(member.find('[data-role="result"]').text()).toBe('2/3 (67%)')
    expect(member.find('[data-role="member-achieved"]').text()).toBe('달성')
    expect(newbie.find('[data-role="result"]').text()).toBe('기간 안 풀이 없음')
  })

  it('AC12 예정은 D-n, 종료는 팀 달성·미달성 칩을 보여 준다', () => {
    const upcoming = mount(TeamChallengeProgress, {
      props: { challenge: challenge({ status: 'UPCOMING', startDate: '2026-11-25' }) },
    })
    expect(upcoming.find('[data-role="status"]').text()).toBe('D-1')

    const ended = mount(TeamChallengeProgress, {
      props: { challenge: challenge({ status: 'ENDED', endDate: '2026-11-23' }) },
    })
    expect(ended.find('[data-role="status"]').text()).toBe('종료')
    expect(ended.find('[data-role="missed"]').text()).toBe('미달성')

    const done = mount(TeamChallengeProgress, {
      props: { challenge: challenge({ status: 'ENDED', achieved: true }) },
    })
    expect(done.find('[data-role="achieved"]').text()).toBe('달성')
  })
})
