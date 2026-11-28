import { describe, expect, it } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import PostAttachment from '@/components/community/PostAttachment.vue'

const mountAttachment = (props) =>
  mount(PostAttachment, {
    props: { analysis: null, concept: null, ...props },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })

describe('PostAttachment (F-17 AC15)', () => {
  it('AC15 볼 수 있는 분석은 제목·프로젝트명과 분석 보기 링크를 보여 준다', () => {
    const wrapper = mountAttachment({
      analysis: { id: 15, viewable: true, title: '쇼핑몰 과제', projectName: 'shop-backend' },
    })

    expect(wrapper.text()).toContain('쇼핑몰 과제')
    expect(wrapper.text()).toContain('shop-backend')
    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.props('to')).toBe('/analyses/15')
    expect(link.text()).toBe('분석 보기')
  })

  it('AC15 볼 수 없는 분석은 안내만 보여 준다', () => {
    const wrapper = mountAttachment({
      analysis: { id: 15, viewable: false, title: null, projectName: null },
    })

    expect(wrapper.text()).toContain('볼 수 없는 분석입니다')
    expect(wrapper.findComponent(RouterLinkStub).exists()).toBe(false)
  })

  it('AC15 볼 수 있는 카드는 이름·카테고리·난이도·요약 상자를 카드 상세로 연결한다', () => {
    const wrapper = mountAttachment({
      concept: {
        id: 12,
        viewable: true,
        name: 'JWT',
        category: 'security',
        difficulty: 'JUNIOR',
        summary: '서버가 서명한 토큰',
      },
    })

    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.props('to')).toBe('/concepts/12')
    expect(link.text()).toContain('JWT')
    expect(link.text()).toContain('Security') // K-1 AC12: 원문 대신 라벨
    expect(link.text()).toContain('주니어')
    expect(link.text()).toContain('서버가 서명한 토큰')
  })

  it('AC15 볼 수 없는 카드는 안내만, 첨부가 없으면 아무것도 보여 주지 않는다', () => {
    const hidden = mountAttachment({
      concept: {
        id: 12,
        viewable: false,
        name: null,
        category: null,
        difficulty: null,
        summary: null,
      },
    })
    expect(hidden.text()).toContain('볼 수 없는 개념 카드입니다')
    expect(hidden.findComponent(RouterLinkStub).exists()).toBe(false)

    expect(mountAttachment({}).text()).toBe('')
  })
})
