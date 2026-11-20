import { describe, expect, it } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import PostRow from '@/components/community/PostRow.vue'

function post(partial) {
  return {
    id: 42,
    boardType: 'QNA',
    title: '@Transactional이 안 먹는 이유가 뭔가요?',
    attachmentType: null,
    commentCount: 3,
    viewCount: 42,
    authorId: 7,
    authorNickname: '코딩초보',
    createdAt: '2026-11-20T14:02:11+09:00',
    ...partial,
  }
}

const mountRow = (partial) =>
  mount(PostRow, {
    props: { post: post(partial) },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })

describe('PostRow (F-17 AC13)', () => {
  it('AC13 게시판 칩, 제목, 댓글 수, 작성자, MM/DD 작성일, 조회수를 보여 주고 상세로 연결한다', () => {
    const wrapper = mountRow()

    expect(wrapper.findComponent(RouterLinkStub).props('to')).toBe('/community/42')
    expect(wrapper.find('[data-role="board"]').text()).toBe('Q&A')
    expect(wrapper.find('[data-role="title"]').text()).toBe(
      '@Transactional이 안 먹는 이유가 뭔가요?',
    )
    expect(wrapper.find('[data-role="comments"]').text()).toBe('[3]')
    expect(wrapper.find('[data-role="author"]').text()).toBe('코딩초보')
    expect(wrapper.find('[data-role="date"]').text()).toBe('11/20')
    expect(wrapper.find('[data-role="views"]').text()).toContain('42')
    expect(wrapper.find('[data-role="attachment"]').exists()).toBe(false)
  })

  it('AC13 게시판별 칩 이름과 첨부 표시를 보여 준다', () => {
    expect(mountRow({ boardType: 'REVIEW' }).find('[data-role="board"]').text()).toBe('후기')
    expect(mountRow({ boardType: 'FREE' }).find('[data-role="board"]').text()).toBe('자유')
    expect(mountRow({ attachmentType: 'ANALYSIS' }).find('[data-role="attachment"]').text()).toBe(
      '📎 분석',
    )
    expect(mountRow({ attachmentType: 'CONCEPT' }).find('[data-role="attachment"]').text()).toBe(
      '🃏 카드',
    )
  })

  it('AC13 탈퇴한 작성자는 "탈퇴한 사용자"로 보인다', () => {
    const wrapper = mountRow({ authorId: null, authorNickname: '탈퇴한 사용자' })

    expect(wrapper.find('[data-role="author"]').text()).toBe('탈퇴한 사용자')
  })
})
