import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import PostDetailView from '@/views/community/PostDetailView.vue'
import * as postsApi from '@/api/posts'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/posts')

function detail(partial) {
  return {
    id: 42,
    boardType: 'QNA',
    title: '@Transactional이 안 먹어요',
    content: '같은 클래스 안에서\n<b>호출</b>하면 안 걸려요',
    authorId: 7,
    authorNickname: '코딩초보',
    viewCount: 43,
    commentCount: 0,
    analysis: null,
    concept: null,
    createdAt: '2026-11-20T14:02:11+09:00',
    updatedAt: '2026-11-20T14:02:11+09:00',
    ...partial,
  }
}

const loginAs = (id) => () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id, nickname: '회원', level: 'JUNIOR', role: 'USER' },
  })

async function mountDetail(path = '/community/42', options) {
  const mounted = await mountAt(PostDetailView, path, options)
  await flushPromises()
  return mounted
}

describe('PostDetailView (F-17 AC15·AC16)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // F-18 댓글 절이 함께 뜬다. 댓글 테스트는 PostComments.spec.js에 있다
    postsApi.fetchComments.mockResolvedValue({ items: [], total: 0 })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC15 게시판 칩, 제목, 작성자 링크, 작성일, 조회수, 본문을 글자 그대로 보여 준다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())

    const { wrapper } = await mountDetail()

    expect(postsApi.fetchPost).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-role="board"]').text()).toBe('Q&A')
    expect(wrapper.find('h1').text()).toBe('@Transactional이 안 먹어요')
    const author = wrapper.find('[data-role="author"]')
    expect(author.element.tagName).toBe('A')
    expect(author.attributes('href')).toBe('/users/7')
    expect(author.text()).toBe('코딩초보')
    expect(wrapper.find('[data-role="date"]').text()).toBe('2026-11-20 14:02')
    expect(wrapper.find('[data-role="views"]').text()).toContain('43')
    const content = wrapper.find('[data-role="content"]')
    expect(content.text()).toBe('같은 클래스 안에서\n<b>호출</b>하면 안 걸려요')
    expect(content.find('b').exists()).toBe(false)
  })

  it('AC15 탈퇴한 작성자는 링크 없는 "탈퇴한 사용자"로, 첨부는 첨부 상자로 보여 준다', async () => {
    postsApi.fetchPost.mockResolvedValue(
      detail({
        authorId: null,
        authorNickname: '탈퇴한 사용자',
        analysis: { id: 15, viewable: false, title: null, projectName: null },
      }),
    )

    const { wrapper } = await mountDetail()

    const author = wrapper.find('[data-role="author"]')
    expect(author.element.tagName).not.toBe('A')
    expect(author.text()).toBe('탈퇴한 사용자')
    expect(wrapper.text()).toContain('볼 수 없는 분석입니다')
  })

  it('AC15 404면 안내와 목록으로 링크, 그 밖의 실패는 서버 message를 보여 준다', async () => {
    postsApi.fetchPost.mockRejectedValue(
      apiError(404, { code: 'POST_404', message: '게시글을 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountDetail()
    expect(wrapper.text()).toContain('게시글을 찾을 수 없습니다.')
    expect(wrapper.find('a[href="/community"]').exists()).toBe(true)

    postsApi.fetchPost.mockRejectedValue(apiError(500, { message: '서버 오류입니다.' }))
    const failed = await mountDetail()
    expect(failed.wrapper.find('[role="alert"]').text()).toBe('서버 오류입니다.')
  })

  it('AC15 "← 목록으로"는 SC-14로 간다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())
    const { wrapper, router } = await mountDetail()

    await wrapper.find('[data-action="back-to-list"]').trigger('click')
    // /community는 지연 로딩 route라 첫 모듈 로딩이 flushPromises 한 번보다 길 수 있다 (#63)
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community'), {
      timeout: 5000,
    })
  })

  it('AC16 자기 글에만 수정·삭제가 보인다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())

    const guest = await mountDetail()
    expect(guest.wrapper.find('[data-action="edit"]').exists()).toBe(false)
    expect(guest.wrapper.find('[data-action="delete"]').exists()).toBe(false)

    const other = await mountDetail('/community/42', { beforeNavigate: loginAs(8) })
    expect(other.wrapper.find('[data-action="delete"]').exists()).toBe(false)

    const mine = await mountDetail('/community/42', { beforeNavigate: loginAs(7) })
    expect(mine.wrapper.find('[data-action="edit"]').attributes('href')).toBe('/community/42/edit')
    expect(mine.wrapper.find('[data-action="delete"]').exists()).toBe(true)
  })

  it('AC16 삭제를 확인하면 deletePost 후 목록으로, 취소하면 요청하지 않는다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())
    postsApi.deletePost.mockResolvedValue()
    const confirm = vi.spyOn(window, 'confirm')

    const { wrapper, router } = await mountDetail('/community/42', { beforeNavigate: loginAs(7) })
    confirm.mockReturnValue(false)
    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()
    expect(postsApi.deletePost).not.toHaveBeenCalled()

    confirm.mockReturnValue(true)
    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()
    expect(postsApi.deletePost).toHaveBeenCalledWith(42)
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community'), {
      timeout: 5000,
    })
  })

  it('AC16 삭제가 403·404면 서버 message를 보여 준다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())
    postsApi.deletePost.mockRejectedValue(
      apiError(403, { code: 'POST_403', message: '본인이 쓴 글만 수정·삭제할 수 있습니다.' }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper, router } = await mountDetail('/community/42', { beforeNavigate: loginAs(7) })
    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('본인이 쓴 글만 수정·삭제할 수 있습니다.')
    expect(router.currentRoute.value.path).toBe('/community/42')
  })

  it('F-18 AC11 글을 불러오면 본문·첨부 아래에 그 글의 댓글 절을 보여 준다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())
    postsApi.fetchComments.mockResolvedValue({
      items: [
        {
          id: 101,
          content: '프록시 때문이에요',
          authorId: 9,
          authorNickname: '스프링러버',
          edited: false,
          createdAt: '2026-11-20T14:10:05+09:00',
          updatedAt: '2026-11-20T14:10:05+09:00',
        },
      ],
      total: 1,
    })

    const { wrapper } = await mountDetail()

    expect(postsApi.fetchComments).toHaveBeenCalledWith(42)
    const section = wrapper.find('[data-role="comments"]')
    expect(section.find('h2').text()).toBe('댓글 1')
    expect(section.text()).toContain('프록시 때문이에요')
  })

  it('F-18 AC11 글이 404면 댓글 절을 보여 주지 않고 댓글도 부르지 않는다', async () => {
    postsApi.fetchPost.mockRejectedValue(
      apiError(404, { code: 'POST_404', message: '게시글을 찾을 수 없습니다.' }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-role="comments"]').exists()).toBe(false)
    expect(postsApi.fetchComments).not.toHaveBeenCalled()
  })
})
