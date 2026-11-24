import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TeamChallengeForm from '@/components/team/TeamChallengeForm.vue'

describe('TeamChallengeForm SC-24 팀장 목표 만들기 (F-20c AC12)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 28, 10, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC12 기본값은 시작일 오늘, 종료일 오늘+6일, 목표 80이고 제출하면 값을 보낸다', async () => {
    const wrapper = mount(TeamChallengeForm)

    expect(wrapper.find('input[name="startDate"]').element.value).toBe('2026-11-28')
    expect(wrapper.find('input[name="endDate"]').element.value).toBe('2026-12-04')
    expect(wrapper.find('input[name="targetRate"]').element.value).toBe('80')
    expect(wrapper.find('input[name="targetRate"]').attributes('min')).toBe('1')
    expect(wrapper.find('input[name="targetRate"]').attributes('max')).toBe('100')

    await wrapper.find('input[name="targetRate"]').setValue('90')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')[0][0]).toEqual({
      startDate: '2026-11-28',
      endDate: '2026-12-04',
      targetRate: 90,
    })
  })

  it('AC12 요청 중에는 제출 버튼이 비활성이고 필드·폼 에러를 보여 준다', () => {
    const wrapper = mount(TeamChallengeForm, {
      props: {
        busy: true,
        fieldErrors: { endDate: '기간은 최대 30일입니다.', targetRate: '1~100' },
        formError: '이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다.',
      },
    })

    expect(wrapper.find('[data-action="create-challenge"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-error="endDate"]').text()).toBe('기간은 최대 30일입니다.')
    expect(wrapper.find('[data-error="targetRate"]').text()).toBe('1~100')
    expect(wrapper.find('[data-error="challenge-form"]').text()).toBe(
      '이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다.',
    )
  })

  it('AC12 목표 정답률을 비우면 null을 보낸다 (서버가 400 details로 알린다)', async () => {
    const wrapper = mount(TeamChallengeForm)
    await wrapper.find('input[name="targetRate"]').setValue('')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')[0][0].targetRate).toBeNull()
  })
})
