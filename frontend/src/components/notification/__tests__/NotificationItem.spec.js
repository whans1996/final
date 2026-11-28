import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import NotificationItem from '@/components/notification/NotificationItem.vue'
import { notificationIcon } from '@/components/notification/notificationIcons'
import { Bell, MessageSquare, Trophy, UserPlus } from 'lucide-vue-next'

const NOW = new Date('2026-11-29T15:00:00+09:00')

function item(overrides = {}) {
  return {
    id: 41,
    type: 'COMMENT',
    message: '회원님의 글에 댓글을 남겼어요 · Spring 질문',
    link: '/community/12',
    actor: { id: 7, nickname: 'java러버' },
    read: false,
    createdAt: '2026-11-29T14:02:11+09:00',
    ...overrides,
  }
}

describe('NotificationItem (NT-1 AC15)', () => {
  it('AC15 actor가 있으면 닉네임님이 + message, 상대 시각, 안 읽음 점', () => {
    const wrapper = mount(NotificationItem, { props: { notification: item(), now: NOW } })

    expect(wrapper.text()).toContain('java러버님이 회원님의 글에 댓글을 남겼어요 · Spring 질문')
    expect(wrapper.find('strong').text()).toBe('java러버')
    expect(wrapper.text()).toContain('57분 전')
    expect(wrapper.find('[data-testid="unread-dot"]').exists()).toBe(true)
    expect(wrapper.attributes('data-read')).toBe('false')
  })

  it('AC15 actor가 없으면 message만, 읽은 항목은 점이 없다', () => {
    const wrapper = mount(NotificationItem, {
      props: {
        notification: item({
          type: 'CHALLENGE_COMPLETE',
          actor: null,
          message: '챌린지를 달성했어요 · 11월 복습 10회',
          read: true,
        }),
        now: NOW,
      },
    })

    expect(wrapper.text()).not.toContain('님이')
    expect(wrapper.text()).toContain('챌린지를 달성했어요 · 11월 복습 10회')
    expect(wrapper.find('[data-testid="unread-dot"]').exists()).toBe(false)
  })

  it('AC15 type별 아이콘, 모르는 type은 Bell', () => {
    expect(notificationIcon('COMMENT')).toBe(MessageSquare)
    expect(notificationIcon('FOLLOW')).toBe(UserPlus)
    expect(notificationIcon('CHALLENGE_COMPLETE')).toBe(Trophy)
    expect(notificationIcon('ANALYSIS_DONE')).toBe(Bell)
  })

  it('AC16 누르면 select를 올린다', async () => {
    const n = item()
    const wrapper = mount(NotificationItem, { props: { notification: n, now: NOW } })

    await wrapper.trigger('click')

    expect(wrapper.emitted('select')[0]).toEqual([n])
  })
})
