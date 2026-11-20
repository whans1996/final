import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import FeedItem from '@/components/feed/FeedItem.vue'

const NOW = new Date('2026-11-20T15:00:00')

function item(type, target, extra = {}) {
  return {
    id: 91,
    type,
    createdAt: '2026-11-20T14:55:00',
    actor: { id: 7, nickname: 'java러버' },
    target: { rating: null, ...target },
    ...extra,
  }
}

async function render(feedItem) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  return mount(FeedItem, { props: { item: feedItem, now: NOW }, global: { plugins: [router] } })
}

describe('FeedItem (F-04 AC11)', () => {
  it('AC11 이니셜·닉네임(SC-18 링크)·상대 시각을 보여 준다', async () => {
    const wrapper = await render(item('POST', { id: 40, title: '@Transactional이 안 먹어요' }))

    expect(wrapper.find('.avatar').text()).toBe('J')
    const actor = wrapper.find('[data-link="actor"]')
    expect(actor.text()).toBe('java러버')
    expect(actor.attributes('href')).toBe('/users/7')
    expect(wrapper.find('time').text()).toBe('5분 전')
    expect(wrapper.find('time').attributes('datetime')).toBe('2026-11-20T14:55:00')
  })

  it.each([
    [
      'ANALYSIS',
      { id: 5, title: '내 첫 분석' },
      '새 분석을 공개했어요',
      '내 첫 분석',
      '/analyses/5',
    ],
    [
      'REVIEW',
      { id: 12, title: 'JWT 기반 인증', rating: 5 },
      '카드에 리뷰를 남겼어요',
      'JWT 기반 인증',
      '/concepts/12',
    ],
    [
      'WISH',
      { id: 12, title: 'JWT 기반 인증' },
      '카드를 찜했어요',
      'JWT 기반 인증',
      '/concepts/12',
    ],
    ['POST', { id: 40, title: '질문 있어요' }, '글을 썼어요', '질문 있어요', '/community/40'],
    [
      'CHALLENGE_JOIN',
      { id: 3, title: '리뷰 3개' },
      '챌린지에 참여했어요',
      '리뷰 3개',
      '/challenges/3',
    ],
    [
      'CHALLENGE_COMPLETE',
      { id: 3, title: '리뷰 3개' },
      '챌린지를 달성했어요',
      '리뷰 3개',
      '/challenges/3',
    ],
  ])(
    'AC11 %s는 종류별 문장과 대상 링크를 보여 준다',
    async (type, target, sentence, title, href) => {
      const wrapper = await render(item(type, target))

      expect(wrapper.find('.sentence').text()).toBe(sentence)
      const link = wrapper.find('[data-link="target"]')
      expect(link.text()).toContain(title)
      expect(link.attributes('href')).toBe(href)
    },
  )

  it('AC11 리뷰는 별점을 함께 보여 주고 다른 종류는 보여 주지 않는다', async () => {
    const review = await render(item('REVIEW', { id: 12, title: 'JWT', rating: 4 }))
    expect(review.find('.rating').text()).toBe('★4')

    const wish = await render(item('WISH', { id: 12, title: 'JWT' }))
    expect(wish.find('.rating').exists()).toBe(false)
  })

  it('AC11 모르는 type이면 항목을 그리지 않는다', async () => {
    const wrapper = await render(item('FOLLOW', { id: 1, title: '?' }))

    expect(wrapper.find('li').exists()).toBe(false)
  })
})
