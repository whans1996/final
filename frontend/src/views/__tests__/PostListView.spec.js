import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import PostListView from '@/views/community/PostListView.vue'
import * as postsApi from '@/api/posts'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/posts')

function post(partial) {
  return {
    id: 42,
    boardType: 'QNA',
    title: '트랜잭션 질문',
    attachmentType: null,
    commentCount: 0,
    viewCount: 1,
    authorId: 7,
    authorNickname: '코딩초보',
    createdAt: '2026-11-20T14:02:11+09:00',
    ...partial,
  }
}

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 7, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })

async function mountList(path = '/community', options) {
  const mounted = await mountAt(PostListView, path, options)
  await flushPromises()
  return mounted
}

const lastCall = () => postsApi.fetchPosts.mock.calls.at(-1)[0]
const tabs = (wrapper) => wrapper.findAll('[data-board]').map((t) => t.text())

describe('PostListView (F-17 AC13·AC14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC13 게시판 탭과 최신순 글 20개를 보여 주고 행을 누르면 상세로 간다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([post(), post({ id: 41, title: '두 번째' })]))

    const { wrapper } = await mountList()

    expect(tabs(wrapper)).toEqual(['전체', '코드 이해 Q&A', '학습 후기', '자유'])
    expect(lastCall()).toEqual({ boardType: '', keyword: '', page: 1, size: 20 })
    const rows = wrapper.findAll('[data-post-id]')
    expect(rows).toHaveLength(2)
    expect(rows[0].attributes('href')).toBe('/community/42')
    expect(rows[1].text()).toContain('두 번째')
  })

  it('AC13 탭을 누르면 1페이지부터 다시 조회하고 쿼리에 남긴다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([post()]))
    const { wrapper, router } = await mountList('/community?keyword=jwt&page=3')

    expect(lastCall()).toEqual({ boardType: '', keyword: 'jwt', page: 3, size: 20 })
    await wrapper.find('[data-board="REVIEW"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ boardType: 'REVIEW', keyword: 'jwt' })
    expect(lastCall()).toEqual({ boardType: 'REVIEW', keyword: 'jwt', page: 1, size: 20 })
    expect(wrapper.find('[data-board="REVIEW"]').attributes('aria-selected')).toBe('true')
  })

  it('AC13 쿼리의 게시판·검색어·페이지로 시작하고 PageNav로 페이지를 옮긴다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([post()], { page: 1, total: 45 }))
    const { wrapper, router } = await mountList('/community?boardType=QNA')

    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ boardType: 'QNA', page: '2' })
    expect(lastCall()).toEqual({ boardType: 'QNA', keyword: '', page: 2, size: 20 })
  })

  it('AC13 0건이면 안내와 (검색어가 있으면) 검색 초기화 버튼, 실패하면 서버 message를 보여 준다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([]))
    const { wrapper, router } = await mountList('/community?boardType=FREE&keyword=없는글')

    expect(wrapper.text()).toContain('게시글이 없습니다')
    await wrapper.find('[data-action="reset-search"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ boardType: 'FREE' })

    const noKeyword = await mountList('/community')
    expect(noKeyword.wrapper.find('[data-action="reset-search"]').exists()).toBe(false)

    postsApi.fetchPosts.mockRejectedValue(apiError(500, { message: '서버 오류입니다.' }))
    const failed = await mountList('/community')
    expect(failed.wrapper.find('[role="alert"]').text()).toBe('서버 오류입니다.')
  })

  it('AC14 검색어는 Enter나 검색 버튼을 눌러야 1페이지부터 조회하고 50자까지만 입력된다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([post()]))
    const { wrapper, router } = await mountList('/community?boardType=QNA&page=2')
    const input = wrapper.find('input[name="keyword"]')

    expect(input.attributes('maxlength')).toBe('50')
    await input.setValue('트랜잭션')
    await flushPromises()
    expect(postsApi.fetchPosts).toHaveBeenCalledTimes(1)

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ boardType: 'QNA', keyword: '트랜잭션' })
    expect(lastCall()).toEqual({ boardType: 'QNA', keyword: '트랜잭션', page: 1, size: 20 })
  })

  it('AC14 글쓰기는 로그인하면 SC-16으로, 비로그인이면 로그인 화면으로 보낸다', async () => {
    postsApi.fetchPosts.mockResolvedValue(page([]))
    const guest = await mountList()
    await guest.wrapper.find('[data-action="write"]').trigger('click')
    await flushPromises()
    await vi.waitFor(() => expect(guest.router.currentRoute.value.path).toBe('/login'))
    expect(guest.router.currentRoute.value.query).toEqual({ redirect: '/community/write' })

    const member = await mountList('/community', { beforeNavigate: login })
    await member.wrapper.find('[data-action="write"]').trigger('click')
    await flushPromises()
    await vi.waitFor(() => expect(member.router.currentRoute.value.path).toBe('/community/write'))
  })

  it('#46 범위를 넘은 page면 마지막 페이지로 바꿔(replace) 다시 조회하고 빈 목록 안내를 보이지 않는다', async () => {
    postsApi.fetchPosts
      .mockResolvedValueOnce(page([], { page: 7, total: 25 }))
      .mockResolvedValue(page([post()], { page: 2, total: 25 }))

    const { wrapper, router } = await mountList('/community?boardType=QNA&page=7')

    expect(router.currentRoute.value.fullPath).toBe('/community?boardType=QNA&page=2')
    expect(lastCall()).toMatchObject({ boardType: 'QNA', page: 2 })
    expect(wrapper.text()).toContain('트랜잭션 질문')
    expect(wrapper.text()).not.toContain('게시글이 없습니다')
  })

  it('#46 응답 전에 다른 화면으로 떠나면 늦게 온 범위 초과 응답으로 주소를 바꾸지 않는다', async () => {
    let resolve
    postsApi.fetchPosts.mockReturnValue(new Promise((r) => (resolve = r)))
    const { router } = await mountList('/community?boardType=QNA&page=7')

    await router.push('/concepts?page=9')
    resolve(page([], { page: 7, total: 25 }))
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/concepts?page=9')
  })

  it('#46 보정은 push가 아닌 replace로 하고, 다시 조회가 시작될 때까지 불러오는 중을 유지한다', async () => {
    postsApi.fetchPosts.mockResolvedValueOnce(page([post()], { total: 25 }))
    const { wrapper, router } = await mountList('/community')
    const replace = vi.spyOn(router, 'replace').mockResolvedValue(undefined)
    const push = vi.spyOn(router, 'push')
    postsApi.fetchPosts.mockResolvedValueOnce(page([], { page: 7, total: 25 }))

    await router.push('/community?boardType=QNA&page=7')
    await flushPromises()

    expect(replace).toHaveBeenCalledWith({ query: { boardType: 'QNA', page: '2' } })
    expect(push).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('불러오는 중')
    expect(wrapper.text()).not.toContain('게시글이 없습니다')
  })
})
