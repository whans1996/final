import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import ConceptDetailView from '@/views/concept/ConceptDetailView.vue'
import * as conceptsApi from '@/api/concepts'
import { useAuthStore } from '@/stores/auth'
import { apiError, deferred, mountAt } from './viewTestUtils'

vi.mock('@/api/concepts')

function summary(partial) {
  return {
    id: 58,
    name: 'Spring Security 필터 체인',
    category: 'security',
    difficulty: 'JUNIOR',
    summary: '요청은 보안 필터들을 차례로 지난다.',
    wishCount: 9,
    avgRating: 4.2,
    ...partial,
  }
}

function detail(partial) {
  return {
    id: 57,
    name: 'JWT 기반 인증',
    category: 'security',
    difficulty: 'SENIOR',
    summary: 'JWT는 서명한 토큰이다.',
    body: '## 핵심\n- 서버는 **서명**만 검증한다.',
    exampleCode: 'String token = Jwts.builder()\n    .compact();',
    referenceUrl: 'https://docs.spring.io/jwt.html',
    wishCount: 12,
    wished: false,
    avgRating: 4.25,
    appearances: [],
    related: [],
    ...partial,
  }
}

async function mountDetail(path = '/concepts/57') {
  const mounted = await mountAt(ConceptDetailView, path)
  await flushPromises()
  return mounted
}

/** 실제 웹 히스토리처럼 history.state.back을 채워 상세로 들어간다 (메모리 히스토리는 push의 state를 그대로 보관한다). */
async function mountWithBack(back, detailPath = '/concepts/57') {
  setActivePinia(createPinia())
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(back ?? '/')
  await router.push({ path: detailPath, state: back ? { back } : {} })
  await router.isReady()
  const wrapper = mount(ConceptDetailView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

const section = (wrapper, name) => wrapper.find(`[data-section="${name}"]`)

describe('ConceptDetailView (F-10 AC9~AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.scrollTo = vi.fn()
    // F-11 리뷰 절 기본값 (리뷰 0건)
    conceptsApi.fetchReviews.mockResolvedValue({ items: [], page: 1, size: 10, total: 0 })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // ── AC9 ──

  it('AC9 헤더·요약·쉬운 설명·예제 코드·출처를 보여 준다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())

    const { wrapper } = await mountDetail()

    expect(conceptsApi.fetchConcept).toHaveBeenCalledWith(57)
    expect(wrapper.find('h1').text()).toBe('JWT 기반 인증')
    const header = wrapper.find('header')
    expect(header.text()).toContain('security')
    expect(header.text()).toContain('숙련')
    expect(header.text()).toContain('♡ 12') // F-12 AC12: 찜하지 않은 카드는 빈 하트
    expect(header.text()).toContain('★ 4.3')
    expect(wrapper.text()).toContain('JWT는 서명한 토큰이다.')

    const body = section(wrapper, 'body')
    expect(body.find('h2').text()).toBe('쉬운 설명')
    expect(body.find('h3').text()).toBe('핵심')
    expect(body.find('li strong').text()).toBe('서명')

    const example = section(wrapper, 'example')
    expect(example.find('h2').text()).toBe('예제 코드')
    expect(example.find('pre code').text()).toBe('String token = Jwts.builder()\n    .compact();')

    const link = section(wrapper, 'reference').find('a')
    expect(link.attributes('href')).toBe('https://docs.spring.io/jwt.html')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })

  it('AC9 본문·예제·출처가 null이면 그 절을 제목째 숨긴다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(
      detail({ body: null, exampleCode: null, referenceUrl: null }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('h1').text()).toBe('JWT 기반 인증')
    expect(section(wrapper, 'body').exists()).toBe(false)
    expect(section(wrapper, 'example').exists()).toBe(false)
    expect(section(wrapper, 'reference').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('쉬운 설명')
  })

  it('AC9 출처가 http(s) URL이 아니면 링크로 만들지 않고 글자로 보여 준다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail({ referenceUrl: 'javascript:alert(1)' }))

    const { wrapper } = await mountDetail()

    const reference = section(wrapper, 'reference')
    expect(reference.find('a').exists()).toBe(false)
    expect(reference.text()).toContain('javascript:alert(1)')
  })

  it('AC9 복사를 누르면 예제 코드를 클립보드에 넣고 2초간 "복사됨"이 된다', async () => {
    const writeText = vi.fn().mockResolvedValue()
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    const { wrapper } = await mountDetail()
    vi.useFakeTimers()

    const button = section(wrapper, 'example').find('button')
    expect(button.text()).toBe('복사')
    await button.trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith('String token = Jwts.builder()\n    .compact();')
    expect(button.text()).toBe('복사됨')
    await vi.advanceTimersByTimeAsync(2000)
    expect(button.text()).toBe('복사')
  })

  it('AC9 복사에 실패하면 "복사하지 못했습니다."를 보여 준다', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denied'))
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    const { wrapper } = await mountDetail()

    await section(wrapper, 'example').find('button').trigger('click')
    await flushPromises()

    expect(section(wrapper, 'example').text()).toContain('복사하지 못했습니다.')
  })

  // ── AC10 ──

  it('AC10 내 코드 등장 위치를 보여 주고 파일이 있으면 파일 탭으로, 없으면 분석 상세로 간다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(
      detail({
        appearances: [
          {
            analysisId: 31,
            analysisTitle: '쇼핑몰 과제',
            fileId: 412,
            filePath: 'src/JwtProvider.java',
            lineHint: 'L24-L40',
            contextNote: '로그인 성공 시 토큰을 만든다',
          },
          {
            analysisId: 28,
            analysisTitle: 'todo-app',
            fileId: null,
            filePath: null,
            lineHint: null,
            contextNote: null,
          },
        ],
      }),
    )

    const { wrapper } = await mountDetail()

    const appearances = section(wrapper, 'appearances')
    expect(appearances.find('h2').text()).toBe('내 코드에서 등장한 위치')
    const links = appearances.findAll('a')
    expect(links).toHaveLength(2)
    expect(links[0].attributes('href')).toBe('/analyses/31?tab=files&file=412')
    expect(links[0].text()).toBe(
      '쇼핑몰 과제 · src/JwtProvider.java · L24-L40 · 로그인 성공 시 토큰을 만든다',
    )
    expect(links[1].attributes('href')).toBe('/analyses/28')
    expect(links[1].text()).toBe('todo-app')
  })

  it('AC10 등장 위치가 비어 있으면 절 전체를 숨긴다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail({ appearances: [] }))

    const { wrapper } = await mountDetail()

    expect(section(wrapper, 'appearances').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('내 코드에서 등장한 위치')
  })

  // ── AC11 ──

  it('AC11 관련 카드를 ConceptCard로 보여 주고 비어 있으면 절을 숨긴다', async () => {
    conceptsApi.fetchConcept.mockResolvedValueOnce(
      detail({ related: [summary(), summary({ id: 59, name: 'CORS' })] }),
    )
    const { wrapper } = await mountDetail()

    const cards = section(wrapper, 'related').findAll('[data-concept-id]')
    expect(section(wrapper, 'related').find('h2').text()).toBe('관련 카드')
    expect(cards).toHaveLength(2)
    expect(cards[0].attributes('href')).toBe('/concepts/58')
    expect(cards[1].text()).toContain('CORS')

    conceptsApi.fetchConcept.mockResolvedValueOnce(detail({ related: [] }))
    const { wrapper: empty } = await mountDetail()
    expect(section(empty, 'related').exists()).toBe(false)
  })

  it('AC11 같은 화면에서 :id만 바뀌어도 새 카드를 조회하고 맨 위로 스크롤한다', async () => {
    conceptsApi.fetchConcept
      .mockResolvedValueOnce(detail({ related: [summary()] }))
      .mockResolvedValueOnce(detail({ id: 58, name: 'Spring Security 필터 체인' }))
    const { wrapper, router } = await mountDetail()
    window.scrollTo.mockClear()

    await router.push('/concepts/58')
    await flushPromises()

    expect(conceptsApi.fetchConcept).toHaveBeenLastCalledWith(58)
    expect(wrapper.find('h1').text()).toBe('Spring Security 필터 체인')
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0)
  })

  // ── AC12 ──

  it('AC12 직전 화면이 목록(쿼리 포함)이면 뒤로 가서 검색 조건을 복원한다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    conceptsApi.fetchConcepts.mockResolvedValue({ items: [], page: 1, size: 20, total: 0 })
    const { wrapper, router } = await mountWithBack('/concepts?keyword=jwt&page=2')

    await wrapper.find('[data-action="back-to-list"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/concepts?keyword=jwt&page=2')
  })

  it.each([
    ['직접 진입', null],
    ['다른 카드 상세에서 진입', '/concepts/58'],
    ['다른 화면에서 진입', '/analyses/3'],
  ])('AC12 %s이면 /concepts로 간다', async (_, back) => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    conceptsApi.fetchConcepts.mockResolvedValue({ items: [], page: 1, size: 20, total: 0 })
    const { wrapper, router } = await mountWithBack(back)

    await wrapper.find('[data-action="back-to-list"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/concepts')
  })

  // ── AC13 ──

  it('AC13 불러오는 동안 "불러오는 중…"을 보여 준다', async () => {
    conceptsApi.fetchConcept.mockReturnValue(new Promise(() => {}))

    const { wrapper } = await mountDetail()

    expect(wrapper.text()).toContain('불러오는 중…')
    expect(wrapper.find('h1').exists()).toBe(false)
  })

  it('AC13 404 CONCEPT_404면 안내와 목록으로 버튼을 보여 준다', async () => {
    conceptsApi.fetchConcept.mockRejectedValue(
      apiError(404, { code: 'CONCEPT_404', message: '개념 카드를 찾을 수 없습니다.' }),
    )
    conceptsApi.fetchConcepts.mockResolvedValue({ items: [], page: 1, size: 20, total: 0 })

    const { wrapper, router } = await mountDetail()

    expect(wrapper.text()).toContain('개념 카드를 찾을 수 없습니다.')
    await wrapper.find('[data-action="not-found-to-list"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/concepts')
  })

  it('AC13 그 밖의 실패는 서버 message를 보여 준다', async () => {
    conceptsApi.fetchConcept.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')
    expect(wrapper.find('[data-action="not-found-to-list"]').exists()).toBe(false)
  })

  it('AC13 숫자가 아닌 id는 상세 라우트에 걸리지 않는다', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/concepts/abc')
    expect(router.currentRoute.value.name).toBe('not-found')
    await router.push('/concepts/57')
    expect(router.currentRoute.value.name).toBe('concept-detail')
  })

  // ── F-11 AC13·AC18 (리뷰 절) ──

  it('F-11 AC13 헤더 평점 옆에 리뷰 목록 total을 "리뷰 N"으로 보여 주고 리뷰 절이 붙는다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    conceptsApi.fetchReviews.mockResolvedValue({ items: [], page: 1, size: 10, total: 12 })

    const { wrapper } = await mountDetail()

    expect(conceptsApi.fetchReviews).toHaveBeenCalledWith(57, { page: 1, size: 10 })
    expect(wrapper.find('[data-role="review-count"]').text()).toBe('· 리뷰 12')
    expect(section(wrapper, 'reviews').find('h2').text()).toBe('리뷰')
  })

  it('F-11 AC18 리뷰 절이 평점을 바꾸면 헤더 평점과 리뷰 수가 바뀐다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail({ avgRating: 4.25 }))
    const { wrapper } = await mountDetail()

    wrapper
      .findComponent({ name: 'ConceptReviews' })
      .vm.$emit('rating-change', { avgRating: 3.7, reviewCount: 5 })
    await flushPromises()

    expect(wrapper.find('.rating').text()).toBe('★ 3.7')
    expect(wrapper.find('[data-role="review-count"]').text()).toBe('· 리뷰 5')
  })

  it('F-11 AC18 리뷰 목록 조회가 실패해도 카드 본문은 그대로 보이고 리뷰 절에만 message가 보인다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())
    conceptsApi.fetchReviews.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('h1').text()).toBe('JWT 기반 인증')
    expect(section(wrapper, 'body').exists()).toBe(true)
    expect(section(wrapper, 'reviews').find('[role="alert"]').text()).toBe(
      '서버 오류가 발생했습니다.',
    )
  })

  it('F-11 AC18 관련 카드로 :id만 바뀌면 새 카드 기준으로 리뷰를 다시 불러온다', async () => {
    conceptsApi.fetchConcept
      .mockResolvedValueOnce(detail({ related: [summary()] }))
      .mockResolvedValueOnce(detail({ id: 58, name: 'Spring Security 필터 체인' }))
    conceptsApi.fetchReviews
      .mockResolvedValueOnce({ items: [], page: 1, size: 10, total: 3 })
      .mockResolvedValueOnce({ items: [], page: 1, size: 10, total: 0 })
    const { wrapper, router } = await mountDetail()
    expect(wrapper.find('[data-role="review-count"]').text()).toBe('· 리뷰 3')

    await router.push('/concepts/58')
    await flushPromises()

    expect(conceptsApi.fetchReviews).toHaveBeenLastCalledWith(58, { page: 1, size: 10 })
    expect(wrapper.find('[data-role="review-count"]').text()).toBe('· 리뷰 0')
  })
})

describe('ConceptDetailView 글쓰기 버튼 (F-17 AC18)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.scrollTo = vi.fn()
    conceptsApi.fetchReviews.mockResolvedValue({ items: [], page: 1, size: 10, total: 0 })
  })

  it('AC18 "이 카드로 글쓰기"는 로그인하면 SC-16에 카드를 붙여, 비로그인이면 로그인 후 돌아오게 보낸다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail())

    const guest = await mountDetail()
    await guest.wrapper.find('[data-action="write-post"]').trigger('click')
    await flushPromises()
    await vi.waitFor(() => expect(guest.router.currentRoute.value.path).toBe('/login'))
    expect(guest.router.currentRoute.value.query).toEqual({
      redirect: '/community/write?conceptId=57',
    })

    const member = await mountAt(ConceptDetailView, '/concepts/57', {
      beforeNavigate: () =>
        useAuthStore().setSession({
          accessToken: 'access-1',
          user: { id: 7, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
        }),
    })
    await flushPromises()
    await member.wrapper.find('[data-action="write-post"]').trigger('click')
    await flushPromises()
    await vi.waitFor(() =>
      expect(member.router.currentRoute.value.fullPath).toBe('/community/write?conceptId=57'),
    )
  })
})

describe('ConceptDetailView 찜 버튼 (F-12 AC12)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.scrollTo = vi.fn()
    conceptsApi.fetchReviews.mockResolvedValue({ items: [], page: 1, size: 10, total: 0 })
  })

  it('AC12 헤더 찜 수 자리에 WishButton이 카드의 wished·wishCount로 보인다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail({ wished: true, wishCount: 13 }))

    const { wrapper } = await mountDetail()

    const button = wrapper.find('header [data-action="toggle-wish"]')
    expect(button.text()).toBe('♥ 13')
    expect(button.attributes('aria-pressed')).toBe('true')
  })

  it('AC12 관련 카드로 :id가 바뀌면 새 카드의 찜 여부와 수로 바뀐다', async () => {
    conceptsApi.fetchConcept
      .mockResolvedValueOnce(detail({ wished: true, wishCount: 13, related: [summary()] }))
      .mockResolvedValueOnce(detail({ id: 58, wished: false, wishCount: 3 }))
    const { wrapper, router } = await mountDetail()

    await router.push('/concepts/58')
    await flushPromises()

    const button = wrapper.find('[data-action="toggle-wish"]')
    expect(button.text()).toBe('♡ 3')
    expect(button.attributes('aria-pressed')).toBe('false')
  })

  it('AC13 찜 버튼이 change를 올리면 카드의 wished·wishCount를 바꾼다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue(detail({ wished: false, wishCount: 12 }))
    const { wrapper } = await mountDetail()

    wrapper
      .findComponent({ name: 'WishButton' })
      .vm.$emit('change', { wished: true, wishCount: 20 })
    await flushPromises()

    const button = wrapper.find('[data-action="toggle-wish"]')
    expect(button.text()).toBe('♥ 20')
    expect(button.attributes('aria-pressed')).toBe('true')
  })
})

describe('ConceptDetailView 늦은 응답 (V-4 AC6·AC7)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.scrollTo = vi.fn()
    conceptsApi.fetchReviews.mockResolvedValue({ items: [], page: 1, size: 10, total: 0 })
  })

  it('AC6·AC7 이전 id의 늦은 실패와 화면을 떠난 뒤 온 응답은 화면 상태를 바꾸지 않는다', async () => {
    const first = deferred()
    const second = deferred()
    conceptsApi.fetchConcept.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const { wrapper, router } = await mountDetail('/concepts/57')

    await router.push('/concepts/58')
    first.reject(apiError(500, { code: 'COMMON_500', message: '옛 카드 실패' }))
    await flushPromises()
    const vm = wrapper.vm
    expect(vm.loadError).toBe('')
    expect(vm.loading).toBe(true)

    wrapper.unmount()
    second.resolve(detail({ id: 58 }))
    await flushPromises()
    expect(vm.concept).toBeNull()
    expect(vm.loading).toBe(true)
  })
})
