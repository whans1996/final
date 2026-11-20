import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import CommentItem from '@/components/community/CommentItem.vue'
import * as postsApi from '@/api/posts'

vi.mock('@/api/posts')

function comment(partial) {
  return {
    id: 101,
    content: '첫 줄\n<b>둘째</b> 줄',
    authorId: 9,
    authorNickname: '스프링러버',
    edited: false,
    createdAt: '2026-11-20T14:10:05+09:00',
    updatedAt: '2026-11-20T14:10:05+09:00',
    ...partial,
  }
}

const apiError = (status, data) =>
  Object.assign(new Error(`status ${status}`), { response: { status, data } })

const mountItem = (props) =>
  mount(CommentItem, {
    props: { comment: comment(), postId: 42, mine: false, editing: false, ...props },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })

describe('CommentItem (F-18 AC11·AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC11 닉네임 링크, 작성일, 내용을 글자 그대로 보여 준다', () => {
    const wrapper = mountItem()

    const author = wrapper.findComponent(RouterLinkStub)
    expect(author.props('to')).toBe('/users/9')
    expect(author.text()).toBe('스프링러버')
    expect(wrapper.find('[data-role="comment-date"]').text()).toBe('2026-11-20 14:10')
    expect(wrapper.find('[data-role="edited"]').exists()).toBe(false)
    const content = wrapper.find('[data-role="comment-content"]')
    expect(content.text()).toBe('첫 줄\n<b>둘째</b> 줄')
    expect(content.find('b').exists()).toBe(false)
  })

  it('AC11 탈퇴한 작성자는 링크 없는 "탈퇴한 사용자"이고, 수정한 댓글은 "(수정됨)"을 붙인다', () => {
    const wrapper = mountItem({
      comment: comment({ authorId: null, authorNickname: '탈퇴한 사용자', edited: true }),
    })

    expect(wrapper.findComponent(RouterLinkStub).exists()).toBe(false)
    expect(wrapper.find('[data-role="comment-author"]').text()).toBe('탈퇴한 사용자')
    expect(wrapper.find('[data-role="edited"]').text()).toBe('(수정됨)')
  })

  it('AC13 내 댓글에만 "수정"·"삭제"가 보인다', () => {
    expect(mountItem().find('[data-action="edit-comment"]').exists()).toBe(false)
    expect(mountItem().find('[data-action="delete-comment"]').exists()).toBe(false)

    const mine = mountItem({ mine: true })
    expect(mine.find('[data-action="edit-comment"]').text()).toBe('수정')
    expect(mine.find('[data-action="delete-comment"]').text()).toBe('삭제')
  })

  it('AC13 "수정"을 누르면 edit을 보내고, 수정 중이면 값이 채워진 입력창으로 바뀐다', async () => {
    const wrapper = mountItem({ mine: true })
    await wrapper.find('[data-action="edit-comment"]').trigger('click')
    expect(wrapper.emitted('edit')).toHaveLength(1)

    await wrapper.setProps({ editing: true })
    expect(wrapper.find('textarea').element.value).toBe('첫 줄\n<b>둘째</b> 줄')
    expect(wrapper.find('[data-action="submit"]').text()).toBe('저장')
    expect(wrapper.find('[data-role="counter"]').text()).toBe('15/500')
    expect(wrapper.find('[data-role="comment-content"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="edit-comment"]').exists()).toBe(false)
  })

  it('AC13 "저장"은 공백뿐이면 비활성이고, 누르면 updateComment 후 changed를 보낸다', async () => {
    postsApi.updateComment.mockResolvedValue(comment({ content: '고친 내용', edited: true }))
    const wrapper = mountItem({ mine: true, editing: true })

    await wrapper.find('textarea').setValue('   ')
    expect(wrapper.find('[data-action="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.find('textarea').setValue('고친 내용')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(postsApi.updateComment).toHaveBeenCalledWith(42, 101, { content: '고친 내용' })
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('AC13 "취소"는 요청 없이 cancel을 보내고, 다시 열면 원래 내용으로 채운다', async () => {
    const wrapper = mountItem({ mine: true, editing: true })
    await wrapper.find('textarea').setValue('고치다 만 내용')

    await wrapper.find('[data-action="cancel"]').trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(postsApi.updateComment).not.toHaveBeenCalled()

    await wrapper.setProps({ editing: false })
    expect(wrapper.find('[data-role="comment-content"]').text()).toBe('첫 줄\n<b>둘째</b> 줄')
    await wrapper.setProps({ editing: true })
    expect(wrapper.find('textarea').element.value).toBe('첫 줄\n<b>둘째</b> 줄')
  })

  it('AC13 수정이 400이면 입력창 아래 details.content, 403이면 댓글 아래 서버 message를 보여 준다', async () => {
    postsApi.updateComment.mockRejectedValueOnce(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { content: '댓글은 500자 이하로 입력해 주세요.' },
      }),
    )
    postsApi.updateComment.mockRejectedValueOnce(
      apiError(403, { code: 'POST_004', message: '본인이 쓴 댓글만 수정·삭제할 수 있습니다.' }),
    )
    const wrapper = mountItem({ mine: true, editing: true })

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-role="field-error"]').text()).toBe(
      '댓글은 500자 이하로 입력해 주세요.',
    )

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-role="field-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="comment-error"]').text()).toBe(
      '본인이 쓴 댓글만 수정·삭제할 수 있습니다.',
    )
    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.emitted('changed')).toBeUndefined()
  })

  it('AC13 "삭제"는 확인하면 deleteComment 후 changed를 보내고, 취소하면 요청하지 않는다', async () => {
    postsApi.deleteComment.mockResolvedValue(undefined)
    const confirm = vi.spyOn(window, 'confirm')
    const wrapper = mountItem({ mine: true })

    confirm.mockReturnValue(false)
    await wrapper.find('[data-action="delete-comment"]').trigger('click')
    expect(postsApi.deleteComment).not.toHaveBeenCalled()

    confirm.mockReturnValue(true)
    await wrapper.find('[data-action="delete-comment"]').trigger('click')
    await flushPromises()
    expect(postsApi.deleteComment).toHaveBeenCalledWith(42, 101)
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('AC13 삭제가 404(POST_003)면 서버 message를 보여 주고 changed를 보내 목록을 다시 받는다', async () => {
    postsApi.deleteComment.mockRejectedValue(
      apiError(404, { code: 'POST_003', message: '댓글을 찾을 수 없습니다.' }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mountItem({ mine: true })

    await wrapper.find('[data-action="delete-comment"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-role="comment-error"]').text()).toBe('댓글을 찾을 수 없습니다.')
    // 이미 지워진 댓글이면 목록을 다시 받아 화면에서도 빠지게 한다
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('AC13 삭제가 403이면 댓글 아래에 서버 message만 보여 주고 목록은 그대로다', async () => {
    postsApi.deleteComment.mockRejectedValue(
      apiError(403, { code: 'POST_004', message: '본인이 쓴 댓글만 수정·삭제할 수 있습니다.' }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mountItem({ mine: true })

    await wrapper.find('[data-action="delete-comment"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-role="comment-error"]').text()).toBe(
      '본인이 쓴 댓글만 수정·삭제할 수 있습니다.',
    )
    expect(wrapper.emitted('changed')).toBeUndefined()
  })
})
