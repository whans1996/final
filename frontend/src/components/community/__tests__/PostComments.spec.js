import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PostComments from '@/components/community/PostComments.vue'
import * as postsApi from '@/api/posts'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/posts')

function comment(id, partial) {
  return {
    id,
    content: `댓글 ${id}`,
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

function loginAs(id) {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id, nickname: '회원', level: 'JUNIOR', role: 'USER' },
  })
}

async function mountComments({ userId } = {}) {
  setActivePinia(createPinia())
  if (userId) loginAs(userId)
  const wrapper = mount(PostComments, {
    props: { postId: 42 },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

const ids = (wrapper) =>
  wrapper.findAll('[data-comment-id]').map((li) => Number(li.attributes('data-comment-id')))

describe('PostComments (F-18 AC11~AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC11 "댓글 N"과 댓글을 서버 순서(오래된순) 그대로 보여 준다', async () => {
    postsApi.fetchComments.mockResolvedValue({
      items: [comment(1), comment(2), comment(3)],
      total: 3,
    })

    const wrapper = await mountComments()

    expect(postsApi.fetchComments).toHaveBeenCalledWith(42)
    expect(wrapper.find('h2').text()).toBe('댓글 3')
    expect(ids(wrapper)).toEqual([1, 2, 3])
    expect(wrapper.find('[data-role="comments-truncated"]').exists()).toBe(false)
  })

  it('AC11 0건이면 "첫 댓글을 남겨 주세요"를 보여 준다', async () => {
    postsApi.fetchComments.mockResolvedValue({ items: [], total: 0 })

    const wrapper = await mountComments()

    expect(wrapper.find('h2').text()).toBe('댓글 0')
    expect(wrapper.find('[data-role="comments-empty"]').text()).toBe('첫 댓글을 남겨 주세요')
  })

  it('AC11 불러오기에 실패하면 댓글 절 안에 서버 message를 보여 준다', async () => {
    postsApi.fetchComments.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const wrapper = await mountComments()

    expect(wrapper.find('[data-role="comments-error"]').text()).toBe('서버 오류가 발생했습니다.')
    expect(wrapper.find('[data-comment-id]').exists()).toBe(false)
  })

  it('AC11 #208 total이 items 수보다 크면 목록 위에 "이전 댓글 n개는 보이지 않습니다"를 붙인다', async () => {
    postsApi.fetchComments.mockResolvedValue({ items: [comment(13), comment(14)], total: 14 })

    const wrapper = await mountComments()

    expect(wrapper.find('h2').text()).toBe('댓글 14')
    const notice = wrapper.find('[data-role="comments-truncated"]')
    expect(notice.text()).toBe('이전 댓글 12개는 보이지 않습니다')
    // 숨은 것은 오래된 댓글이라 목록 앞에 둔다
    const list = wrapper.find('ul.list')
    expect(
      notice.element.compareDocumentPosition(list.element) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
  })

  it('AC12 비로그인은 입력창 대신 "로그인하고 댓글 남기기" 링크를 본다', async () => {
    postsApi.fetchComments.mockResolvedValue({ items: [comment(1)], total: 1 })

    const wrapper = await mountComments()

    expect(wrapper.find('textarea').exists()).toBe(false)
    const login = wrapper
      .findAllComponents(RouterLinkStub)
      .find((link) => link.attributes('data-action') === 'login-to-comment')
    expect(login.text()).toBe('로그인하고 댓글 남기기')
    expect(login.props('to')).toEqual({ path: '/login', query: { redirect: '/community/42' } })
    expect(wrapper.find('[data-action="edit-comment"]').exists()).toBe(false)
  })

  it('AC12 회원이 "등록"하면 createComment 후 입력창을 비우고 목록만 다시 불러온다', async () => {
    postsApi.fetchComments
      .mockResolvedValueOnce({ items: [], total: 0 })
      .mockResolvedValueOnce({ items: [comment(5, { content: '답' })], total: 1 })
    let resolveCreate
    postsApi.createComment.mockReturnValue(new Promise((resolve) => (resolveCreate = resolve)))
    const wrapper = await mountComments({ userId: 9 })

    expect(wrapper.find('[data-action="login-to-comment"]').exists()).toBe(false)
    await wrapper.find('textarea').setValue('답')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('[data-action="submit"]').attributes('disabled')).toBeDefined()

    resolveCreate(comment(5, { content: '답' }))
    await flushPromises()

    expect(postsApi.createComment).toHaveBeenCalledWith(42, { content: '답' })
    expect(postsApi.fetchComments).toHaveBeenCalledTimes(2)
    expect(wrapper.find('textarea').element.value).toBe('')
    expect(wrapper.find('h2').text()).toBe('댓글 1')
    expect(ids(wrapper)).toEqual([5])
  })

  it('AC12 #210 목록을 받을 때마다 댓글 수를 count로 알리고, 실패하면 알리지 않는다', async () => {
    postsApi.fetchComments
      .mockResolvedValueOnce({ items: [], total: 0 })
      .mockResolvedValueOnce({ items: [comment(5)], total: 1 })
      .mockRejectedValueOnce(apiError(500, { message: '서버 오류입니다.' }))
    postsApi.createComment.mockResolvedValue(comment(5))
    const wrapper = await mountComments({ userId: 9 })
    expect(wrapper.emitted('count')).toEqual([[0]])

    await wrapper.find('textarea').setValue('답')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.emitted('count')).toEqual([[0], [1]])

    await wrapper.find('textarea').setValue('또')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.emitted('count')).toEqual([[0], [1]])
  })

  it('AC12 작성이 400이면 details.content를, 그 밖의 실패는 서버 message를 보여 주고 입력은 남긴다', async () => {
    postsApi.fetchComments.mockResolvedValue({ items: [], total: 0 })
    postsApi.createComment
      .mockRejectedValueOnce(
        apiError(400, {
          code: 'COMMON_400',
          message: '입력값이 올바르지 않습니다.',
          details: { content: '댓글 내용을 입력해 주세요.' },
        }),
      )
      .mockRejectedValueOnce(
        apiError(404, { code: 'POST_404', message: '게시글을 찾을 수 없습니다.' }),
      )
    const wrapper = await mountComments({ userId: 9 })
    await wrapper.find('textarea').setValue('답')

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-role="field-error"]').text()).toBe('댓글 내용을 입력해 주세요.')

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-role="field-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="comment-form-error"]').text()).toBe(
      '게시글을 찾을 수 없습니다.',
    )
    expect(wrapper.find('textarea').element.value).toBe('답')
    expect(postsApi.fetchComments).toHaveBeenCalledTimes(1)
  })

  it('AC13 내 댓글에만 수정·삭제가 보이고, 한 번에 한 댓글만 수정 중이다', async () => {
    postsApi.fetchComments.mockResolvedValue({
      items: [comment(1, { authorId: 7 }), comment(2), comment(3, { authorId: 7 })],
      total: 3,
    })
    const wrapper = await mountComments({ userId: 7 })

    const editButtons = () => wrapper.findAll('[data-action="edit-comment"]')
    expect(editButtons()).toHaveLength(2)
    expect(wrapper.find('[data-comment-id="2"] [data-action="delete-comment"]').exists()).toBe(
      false,
    )

    await wrapper.find('[data-comment-id="1"] [data-action="edit-comment"]').trigger('click')
    expect(wrapper.find('[data-comment-id="1"] textarea').exists()).toBe(true)

    await wrapper.find('[data-comment-id="3"] [data-action="edit-comment"]').trigger('click')
    expect(wrapper.find('[data-comment-id="1"] textarea').exists()).toBe(false)
    expect(wrapper.find('[data-comment-id="3"] textarea').exists()).toBe(true)

    await wrapper.find('[data-comment-id="3"] [data-action="cancel"]').trigger('click')
    expect(wrapper.find('[data-comment-id="3"] textarea').exists()).toBe(false)
  })

  it('AC13 수정을 저장하면 수정 상태를 끝내고 목록을 다시 불러온다', async () => {
    postsApi.fetchComments
      .mockResolvedValueOnce({ items: [comment(1, { authorId: 7 })], total: 1 })
      .mockResolvedValueOnce({
        items: [comment(1, { authorId: 7, content: '고침', edited: true })],
        total: 1,
      })
    postsApi.updateComment.mockResolvedValue(comment(1, { authorId: 7, content: '고침' }))
    const wrapper = await mountComments({ userId: 7 })

    await wrapper.find('[data-action="edit-comment"]').trigger('click')
    await wrapper.find('[data-comment-id="1"] textarea').setValue('고침')
    await wrapper.find('[data-comment-id="1"] form').trigger('submit')
    await flushPromises()

    expect(postsApi.updateComment).toHaveBeenCalledWith(42, 1, { content: '고침' })
    expect(postsApi.fetchComments).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-comment-id="1"] textarea').exists()).toBe(false)
    expect(wrapper.find('[data-role="comment-content"]').text()).toBe('고침')
    expect(wrapper.find('[data-role="edited"]').exists()).toBe(true)
  })

  it('AC13 삭제하면 목록을 다시 불러와 "댓글 N"이 줄어든다', async () => {
    postsApi.fetchComments
      .mockResolvedValueOnce({ items: [comment(1, { authorId: 7 }), comment(2)], total: 2 })
      .mockResolvedValueOnce({ items: [comment(2)], total: 1 })
    postsApi.deleteComment.mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = await mountComments({ userId: 7 })

    await wrapper.find('[data-comment-id="1"] [data-action="delete-comment"]').trigger('click')
    await flushPromises()

    expect(postsApi.deleteComment).toHaveBeenCalledWith(42, 1)
    expect(wrapper.find('h2').text()).toBe('댓글 1')
    expect(ids(wrapper)).toEqual([2])
  })
})
