import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import ConceptReviews from '@/components/concept/ConceptReviews.vue'
import * as conceptsApi from '@/api/concepts'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/concepts')

function review(id, partial) {
  return {
    id,
    rating: 4,
    content: `리뷰 ${id}`,
    authorId: 100 + id,
    authorNickname: `작성자${id}`,
    createdAt: '2026-12-02T21:14:03+09:00',
    ...partial,
  }
}

const reviews = (from, count) => Array.from({ length: count }, (_, i) => review(from - i))

function page(items, total, pageNo = 1) {
  return { items, page: pageNo, size: 10, total }
}

function apiError(status, data) {
  return Object.assign(new Error(`status ${status}`), { response: { status, data } })
}

async function mountReviews({ loggedIn = false, conceptId = 57 } = {}) {
  setActivePinia(createPinia())
  if (loggedIn) {
    useAuthStore().setSession({
      accessToken: 'token',
      user: { id: 9, nickname: '준섭', level: 'JUNIOR', role: 'USER' },
    })
  }
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(`/concepts/${conceptId}`)
  await router.isReady()
  const wrapper = mount(ConceptReviews, {
    props: { conceptId },
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

const listed = (wrapper) =>
  wrapper.findAll('[data-role="review"]').map((li) => li.attributes('data-id'))
const lastRating = (wrapper) => wrapper.emitted('rating-change')?.at(-1)?.[0]

async function writeReview(wrapper, score, text) {
  await wrapper.find(`[aria-label="별점 ${score}점"]`).trigger('click')
  await wrapper.find('textarea').setValue(text)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('ConceptReviews (F-11 AC13~AC17)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ── AC13 ──

  it('AC13 최신순 10개를 별점·내용·닉네임 링크·작성일로 보여 주고 total을 올린다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page(reviews(12, 10), 12))

    const { wrapper } = await mountReviews()

    expect(conceptsApi.fetchReviews).toHaveBeenCalledWith(57, { page: 1, size: 10 })
    expect(listed(wrapper)).toHaveLength(10)
    const first = wrapper.find('[data-role="review"]')
    expect(first.find('[aria-label="별점 4점"]').exists()).toBe(true)
    expect(first.text()).toContain('리뷰 12')
    expect(first.find('a').attributes('href')).toBe('/users/112')
    expect(first.find('a').text()).toBe('작성자12')
    expect(first.find('time').text()).toBe('2026-12-02')
    expect(lastRating(wrapper)).toEqual({ reviewCount: 12 })
  })

  it('AC13 더 보기를 누르면 다음 10개를 붙이고 다 불러오면 버튼이 사라진다', async () => {
    conceptsApi.fetchReviews
      .mockResolvedValueOnce(page(reviews(12, 10), 12))
      .mockResolvedValueOnce(page(reviews(2, 2), 12, 2))
    const { wrapper } = await mountReviews()

    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    expect(conceptsApi.fetchReviews).toHaveBeenLastCalledWith(57, { page: 2, size: 10 })
    expect(listed(wrapper)).toHaveLength(12)
    expect(lastRating(wrapper)).toEqual({ reviewCount: 12 })
    expect(listed(wrapper).at(-1)).toBe('1')
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 내 리뷰를 지운 뒤 더 보기는 건너뛰는 리뷰 없이 나머지를 붙이고 버튼이 사라진다', async () => {
    // 서버: 최신순 12..1, 내 리뷰는 1쪽의 7번
    let server = reviews(12, 12).map((r) => (r.id === 7 ? { ...r, authorId: 9 } : r))
    conceptsApi.fetchReviews.mockImplementation(async (_id, { page: p, size }) =>
      page(server.slice((p - 1) * size, p * size), server.length, p),
    )
    conceptsApi.fetchMyReview.mockImplementation(async () => server.find((r) => r.id === 7))
    conceptsApi.deleteReview.mockImplementation(async () => {
      server = server.filter((r) => r.id !== 7)
      return { avgRating: 4.0, reviewCount: server.length }
    })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { wrapper } = await mountReviews({ loggedIn: true })

    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    expect(listed(wrapper)).toEqual(['12', '11', '10', '9', '8', '6', '5', '4', '3', '2', '1'])
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 더 보기 사이에 리뷰 수가 바뀌면 헤더용 리뷰 수를 다시 올리고 끝까지 받으면 버튼을 숨긴다', async () => {
    conceptsApi.fetchReviews
      .mockResolvedValueOnce(page(reviews(12, 10), 12))
      .mockResolvedValueOnce(page(reviews(3, 3), 13, 2))
    const { wrapper } = await mountReviews()

    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    // 맨 위에 새로 생긴 리뷰는 아래로 더 불러와도 오지 않으므로 끝까지 받으면 버튼을 숨긴다
    expect(listed(wrapper)).toHaveLength(12)
    expect(lastRating(wrapper)).toEqual({ reviewCount: 13 })
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 다른 회원이 새 리뷰를 쓴 뒤에도 더 보기로 나머지를 모두 불러온다', async () => {
    let server = reviews(25, 25)
    conceptsApi.fetchReviews.mockImplementation(async (_id, { page: p, size }) =>
      page(server.slice((p - 1) * size, p * size), server.length, p),
    )
    const { wrapper } = await mountReviews()
    server = [review(26), ...server] // 첫 10개를 받은 뒤 다른 회원이 썼다

    for (let click = 0; click < 3 && wrapper.find('[data-action="more"]').exists(); click += 1) {
      await wrapper.find('[data-action="more"]').trigger('click')
      await flushPromises()
    }

    expect(listed(wrapper)).toEqual(reviews(25, 25).map((r) => String(r.id)))
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 새 리뷰를 쓴 뒤 더 보기는 밀려난 항목을 한 번만 붙인다', async () => {
    let server = reviews(12, 12)
    conceptsApi.fetchReviews.mockImplementation(async (_id, { page: p, size }) =>
      page(server.slice((p - 1) * size, p * size), server.length, p),
    )
    conceptsApi.fetchMyReview.mockResolvedValue(null)
    conceptsApi.createReview.mockImplementation(async ({ rating, content }) => {
      const mine = review(13, { rating, content, authorId: 9 })
      server = [mine, ...server]
      return { review: mine, avgRating: 4.0, reviewCount: server.length }
    })
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 5, '새 리뷰')
    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    expect(listed(wrapper)).toEqual(reviews(13, 13).map((r) => String(r.id)))
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 0건이면 첫 리뷰 안내를 보여 준다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page([], 0))

    const { wrapper } = await mountReviews()

    expect(wrapper.find('[data-role="empty"]').text()).toBe(
      '아직 리뷰가 없습니다. 첫 리뷰를 남겨 주세요',
    )
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC13 탈퇴한 작성자는 링크 없이 "탈퇴한 사용자"이고 HTML은 글자 그대로다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(
      page(
        [
          review(1, {
            authorId: null,
            authorNickname: '탈퇴한 사용자',
            content: '<img src=x onerror=alert(1)>',
          }),
        ],
        1,
      ),
    )

    const { wrapper } = await mountReviews()

    const item = wrapper.find('[data-role="review"]')
    expect(item.find('a').exists()).toBe(false)
    expect(item.text()).toContain('탈퇴한 사용자')
    expect(item.find('img').exists()).toBe(false)
    expect(item.find('.content').text()).toBe('<img src=x onerror=alert(1)>')
  })

  it('AC13 불러오는 동안 안내를 보여 준다', async () => {
    conceptsApi.fetchReviews.mockReturnValue(new Promise(() => {}))

    const { wrapper } = await mountReviews()

    expect(wrapper.text()).toContain('리뷰를 불러오는 중…')
  })

  // ── AC14 ──

  it('AC14 비로그인이면 폼 대신 로그인 버튼이 보이고 돌아올 주소를 담아 로그인으로 간다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page([], 0))
    const { wrapper, router } = await mountReviews()

    expect(wrapper.find('form').exists()).toBe(false)
    expect(conceptsApi.fetchMyReview).not.toHaveBeenCalled()
    await wrapper.find('[data-action="login-to-review"]').trigger('click')

    // 로그인 화면은 지연 로딩이라 이동이 끝날 때까지 기다린다
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/login'))
    expect(router.currentRoute.value.query.redirect).toBe('/concepts/57')
  })

  // ── AC15 ──

  it('AC15 등록하면 새 리뷰가 맨 위에 붙고 리뷰 수·평점을 올리고 내 리뷰 상자로 바뀐다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page(reviews(3, 3), 3))
    conceptsApi.fetchMyReview.mockResolvedValue(null)
    const mine = review(10, { rating: 5, content: '예제가 명확했어요', authorId: 9 })
    conceptsApi.createReview.mockResolvedValue({ review: mine, avgRating: 4.3, reviewCount: 4 })
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 5, '  예제가 명확했어요 ')

    expect(conceptsApi.createReview).toHaveBeenCalledWith(57, {
      rating: 5,
      content: '예제가 명확했어요',
    })
    expect(listed(wrapper)).toEqual(['10', '3', '2', '1'])
    expect(lastRating(wrapper)).toEqual({ avgRating: 4.3, reviewCount: 4 })
    expect(wrapper.find('[data-role="create-form"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="my-review"]').text()).toContain('예제가 명확했어요')
  })

  it('AC15 400이면 details의 필드 메시지를 보여 준다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page([], 0))
    conceptsApi.fetchMyReview.mockResolvedValue(null)
    conceptsApi.createReview.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '요청 값이 올바르지 않습니다.',
        details: { content: '리뷰는 200자 이하로 입력해 주세요.' },
      }),
    )
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 3, '내용')

    expect(wrapper.find('[data-error="content"]').text()).toBe('리뷰는 200자 이하로 입력해 주세요.')
    expect(wrapper.find('[data-role="create-form"]').exists()).toBe(true)
  })

  it.each(['list', 'mine'])(
    'AC15 처음 %s 조회가 끝나기 전에는 등록하지 않아 늦은 응답이 저장 결과를 덮지 않는다',
    async (slow) => {
      let answer
      const pending = new Promise((resolve) => (answer = resolve))
      if (slow === 'list') {
        conceptsApi.fetchReviews.mockReturnValue(pending)
        conceptsApi.fetchMyReview.mockResolvedValue(null)
      } else {
        conceptsApi.fetchReviews.mockResolvedValue(page([], 0))
        conceptsApi.fetchMyReview.mockReturnValue(pending)
      }
      const mine = review(1, { authorId: 9, rating: 5, content: '새 리뷰' })
      conceptsApi.createReview.mockResolvedValue({ review: mine, avgRating: 5.0, reviewCount: 1 })
      const { wrapper } = await mountReviews({ loggedIn: true })

      await writeReview(wrapper, 5, '새 리뷰')
      expect(conceptsApi.createReview).not.toHaveBeenCalled()
      expect(wrapper.find('[data-role="create-form"] button[type="submit"]').element.disabled).toBe(
        true,
      )

      answer(slow === 'list' ? page([], 0) : null) // 작성 전 기준의 응답
      await flushPromises()
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(conceptsApi.createReview).toHaveBeenCalledTimes(1)
      expect(wrapper.find('[data-role="my-review"]').exists()).toBe(true)
      expect(listed(wrapper)).toEqual(['1'])
      expect(lastRating(wrapper)).toEqual({ avgRating: 5.0, reviewCount: 1 })
    },
  )

  it('AC13 더 보기를 받는 동안에는 수정·삭제를 막는다', async () => {
    let answerMore
    conceptsApi.fetchReviews
      .mockResolvedValueOnce(page(reviews(12, 10), 12))
      .mockReturnValueOnce(new Promise((resolve) => (answerMore = resolve)))
    conceptsApi.fetchMyReview.mockResolvedValue(review(2, { authorId: 9 }))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { wrapper } = await mountReviews({ loggedIn: true })

    await wrapper.find('[data-action="more"]').trigger('click')
    expect(wrapper.find('[data-action="edit"]').element.disabled).toBe(true)
    expect(wrapper.find('[data-action="delete"]').element.disabled).toBe(true)
    await wrapper.find('[data-action="delete"]').trigger('click')
    expect(conceptsApi.deleteReview).not.toHaveBeenCalled()

    answerMore(page(reviews(2, 2), 12, 2))
    await flushPromises()
    expect(wrapper.find('[data-action="delete"]').element.disabled).toBe(false)
  })

  it('AC13 리뷰를 저장하는 동안에는 더 보기를 막는다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page(reviews(12, 10), 12))
    conceptsApi.fetchMyReview.mockResolvedValue(null)
    let answerCreate
    conceptsApi.createReview.mockReturnValue(new Promise((resolve) => (answerCreate = resolve)))
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 5, '새 리뷰')
    expect(conceptsApi.createReview).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-action="more"]').element.disabled).toBe(true)
    await wrapper.find('[data-action="more"]').trigger('click')
    expect(conceptsApi.fetchReviews).toHaveBeenCalledTimes(1)

    answerCreate({ review: review(13, { authorId: 9 }), avgRating: 4.0, reviewCount: 13 })
    await flushPromises()
    expect(wrapper.find('[data-action="more"]').element.disabled).toBe(false)
  })

  it('AC15 409면 서버 message를 보여 주고 내 리뷰를 다시 불러온다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page([], 0))
    conceptsApi.fetchMyReview
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce(review(5, { content: '다른 탭에서 쓴 리뷰' }))
    conceptsApi.createReview.mockRejectedValue(
      apiError(409, { code: 'CONCEPT_409', message: '이미 이 카드에 리뷰를 남겼습니다.' }),
    )
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 3, '내용')

    expect(conceptsApi.fetchMyReview).toHaveBeenCalledTimes(2)
    const box = wrapper.find('[data-role="my-review"]')
    expect(box.text()).toContain('다른 탭에서 쓴 리뷰')
    expect(box.find('[role="alert"]').text()).toBe('이미 이 카드에 리뷰를 남겼습니다.')
  })

  it('AC15 그 밖의 실패는 서버 message를 보여 준다', async () => {
    conceptsApi.fetchReviews.mockResolvedValue(page([], 0))
    conceptsApi.fetchMyReview.mockResolvedValue(null)
    conceptsApi.createReview.mockRejectedValue(
      apiError(404, { code: 'CONCEPT_404', message: '개념 카드를 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountReviews({ loggedIn: true })

    await writeReview(wrapper, 3, '내용')

    expect(wrapper.find('[role="alert"]').text()).toBe('개념 카드를 찾을 수 없습니다.')
  })

  // ── AC16 ──

  it('AC16 내 리뷰가 있으면 상자가 보이고 수정·저장하면 목록과 평점이 바뀐다', async () => {
    const mine = review(2, { rating: 2, content: '처음엔 어려웠음', authorId: 9 })
    conceptsApi.fetchReviews.mockResolvedValue(page([review(3), mine, review(1)], 3))
    conceptsApi.fetchMyReview.mockResolvedValue(mine)
    const changed = { ...mine, rating: 5, content: '다시 읽으니 이해됨' }
    conceptsApi.updateReview.mockResolvedValue({ review: changed, avgRating: 4.3, reviewCount: 3 })
    const { wrapper } = await mountReviews({ loggedIn: true })

    expect(conceptsApi.fetchMyReview).toHaveBeenCalledWith(57)
    const box = wrapper.find('[data-role="my-review"]')
    expect(box.find('[aria-label="별점 2점"]').exists()).toBe(true)
    expect(box.text()).toContain('처음엔 어려웠음')

    await box.find('[data-action="edit"]').trigger('click')
    const form = wrapper.find('[data-role="edit-form"]')
    expect(form.find('textarea').element.value).toBe('처음엔 어려웠음')
    await form.find('[aria-label="별점 5점"]').trigger('click')
    await form.find('textarea').setValue('다시 읽으니 이해됨')
    await form.trigger('submit')
    await flushPromises()

    expect(conceptsApi.updateReview).toHaveBeenCalledWith(57, {
      rating: 5,
      content: '다시 읽으니 이해됨',
    })
    expect(listed(wrapper)).toEqual(['3', '2', '1'])
    expect(wrapper.find('[data-id="2"]').text()).toContain('다시 읽으니 이해됨')
    expect(wrapper.find('[data-role="my-review"]').text()).toContain('다시 읽으니 이해됨')
    expect(lastRating(wrapper)).toEqual({ avgRating: 4.3, reviewCount: 3 })
  })

  it('AC16 취소하면 요청 없이 원래 상자로 돌아간다', async () => {
    const mine = review(2, { authorId: 9 })
    conceptsApi.fetchReviews.mockResolvedValue(page([mine], 1))
    conceptsApi.fetchMyReview.mockResolvedValue(mine)
    const { wrapper } = await mountReviews({ loggedIn: true })

    await wrapper.find('[data-action="edit"]').trigger('click')
    await wrapper.find('[data-action="cancel"]').trigger('click')

    expect(wrapper.find('[data-role="edit-form"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="my-review"]').exists()).toBe(true)
    expect(conceptsApi.updateReview).not.toHaveBeenCalled()
  })

  // ── AC17 ──

  it('AC17 삭제를 확인하면 목록에서 빠지고 리뷰 수·평점을 올리고 빈 폼이 보인다', async () => {
    const mine = review(2, { authorId: 9 })
    conceptsApi.fetchReviews.mockResolvedValue(page([review(3), mine], 2))
    conceptsApi.fetchMyReview.mockResolvedValue(mine)
    conceptsApi.deleteReview.mockResolvedValue({ avgRating: 4.0, reviewCount: 1 })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { wrapper } = await mountReviews({ loggedIn: true })

    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()

    expect(conceptsApi.deleteReview).toHaveBeenCalledWith(57)
    expect(listed(wrapper)).toEqual(['3'])
    expect(lastRating(wrapper)).toEqual({ avgRating: 4.0, reviewCount: 1 })
    const form = wrapper.find('[data-role="create-form"]')
    expect(form.exists()).toBe(true)
    expect(form.find('textarea').element.value).toBe('')
  })

  it('AC17 확인 대화상자에서 취소하면 요청하지 않는다', async () => {
    const mine = review(2, { authorId: 9 })
    conceptsApi.fetchReviews.mockResolvedValue(page([mine], 1))
    conceptsApi.fetchMyReview.mockResolvedValue(mine)
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { wrapper } = await mountReviews({ loggedIn: true })

    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()

    expect(conceptsApi.deleteReview).not.toHaveBeenCalled()
    expect(wrapper.find('[data-role="my-review"]').exists()).toBe(true)
  })

  // ── AC18 (컴포넌트 부분) ──

  it('AC18 목록 조회가 실패하면 리뷰 절에 서버 message를 보여 준다', async () => {
    conceptsApi.fetchReviews.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountReviews()

    expect(wrapper.find('[data-role="list-error"]').text()).toBe('서버 오류가 발생했습니다.')
  })

  it('AC18 conceptId가 바뀌면 상태를 비우고 새 카드 기준으로 다시 불러온다', async () => {
    const mine = review(2, { authorId: 9 })
    conceptsApi.fetchReviews
      .mockResolvedValueOnce(page([mine], 1))
      .mockResolvedValueOnce(page([review(50)], 1))
    conceptsApi.fetchMyReview.mockResolvedValueOnce(mine).mockResolvedValueOnce(null)
    const { wrapper } = await mountReviews({ loggedIn: true })
    await wrapper.find('[data-action="edit"]').trigger('click')

    await wrapper.setProps({ conceptId: 58 })
    await flushPromises()

    expect(conceptsApi.fetchReviews).toHaveBeenLastCalledWith(58, { page: 1, size: 10 })
    expect(conceptsApi.fetchMyReview).toHaveBeenLastCalledWith(58)
    expect(listed(wrapper)).toEqual(['50'])
    expect(wrapper.find('[data-role="edit-form"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="create-form"]').exists()).toBe(true)
  })
})
