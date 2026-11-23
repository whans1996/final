import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ConceptListView from '@/views/concept/ConceptListView.vue'
import * as conceptsApi from '@/api/concepts'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/concepts')

function concept(partial) {
  return {
    id: 57,
    name: 'JWT 기반 인증',
    category: 'security',
    difficulty: 'SENIOR',
    summary: 'JWT는 서명한 토큰이다.',
    wishCount: 12,
    avgRating: 4.5,
    ...partial,
  }
}

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function mountList(path = '/concepts') {
  const mounted = await mountAt(ConceptListView, path)
  await flushPromises()
  return mounted
}

const lastCall = () => conceptsApi.fetchConcepts.mock.calls.at(-1)[0]
const options = (wrapper, name) =>
  wrapper.findAll(`select[name="${name}"] option`).map((o) => [o.element.value, o.text()])

describe('ConceptListView (F-09 AC9~AC11)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC9 검색창, 카테고리·난이도·정렬 선택, 카드 그리드를 보여 준다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(
      page([concept(), concept({ id: 58, name: '세션' })]),
    )

    const { wrapper } = await mountList()

    expect(wrapper.find('input[name="keyword"]').exists()).toBe(true)
    expect(options(wrapper, 'category')).toEqual([
      ['', '전체'],
      ['java', 'Java'],
      ['spring', 'Spring'],
      ['web', 'Web'],
      ['security', 'Security'],
      ['mybatis', 'MyBatis'],
      ['vue', 'Vue'],
    ])
    expect(options(wrapper, 'difficulty')).toEqual([
      ['', '전체'],
      ['BEGINNER', '입문'],
      ['JUNIOR', '주니어'],
      ['SENIOR', '숙련'],
    ])
    expect(options(wrapper, 'sort')).toEqual([
      ['POPULAR', '인기순'],
      ['LATEST', '최신순'],
    ])

    expect(wrapper.find('[data-testid="concept-total"]').text()).toBe('2개')
    const cards = wrapper.findAll('[data-concept-id]')
    expect(cards).toHaveLength(2)
    expect(cards[0].attributes('href')).toBe('/concepts/57')
    expect(cards[0].text()).toContain('JWT 기반 인증')
    expect(cards[0].text()).toContain('security')
    expect(cards[0].text()).toContain('숙련')
    expect(cards[0].text()).toContain('4.5')
    expect(cards[0].text()).toContain('12')
  })

  it('AC9 결과가 한 페이지를 넘으면 이전·다음 페이지 버튼을 보여 준다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()], { total: 45 }))

    const { wrapper } = await mountList()

    expect(wrapper.find('[data-action="prev"]').exists()).toBe(true)
    expect(wrapper.find('[data-action="next"]').exists()).toBe(true)
  })

  it('AC10 쿼리가 달린 URL을 열면 그 조건으로 조회하고 입력란에 복원한다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()], { page: 2, total: 45 }))

    const { wrapper } = await mountList(
      '/concepts?keyword=jwt&category=security&difficulty=JUNIOR&sort=LATEST&page=2',
    )

    expect(lastCall()).toEqual({
      keyword: 'jwt',
      category: 'security',
      difficulty: 'JUNIOR',
      sort: 'LATEST',
      page: 2,
      size: 20,
    })
    expect(wrapper.find('input[name="keyword"]').element.value).toBe('jwt')
    expect(wrapper.find('select[name="category"]').element.value).toBe('security')
    expect(wrapper.find('select[name="difficulty"]').element.value).toBe('JUNIOR')
    expect(wrapper.find('select[name="sort"]').element.value).toBe('LATEST')
  })

  it('AC10 쿼리가 없으면 기본값(인기순, 1페이지, 조건 없음)으로 조회한다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()]))

    await mountList()

    expect(lastCall()).toEqual({
      keyword: '',
      category: '',
      difficulty: '',
      sort: 'POPULAR',
      page: 1,
      size: 20,
    })
  })

  it('AC10 검색어를 입력하고 검색하면 URL에 반영하고 page를 1로 돌린 뒤 다시 조회한다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()], { total: 45 }))
    const { wrapper, router } = await mountList('/concepts?category=security&page=3')

    await wrapper.find('input[name="keyword"]').setValue('  jwt  ')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ keyword: 'jwt', category: 'security' })
    expect(lastCall()).toMatchObject({ keyword: 'jwt', category: 'security', page: 1 })
  })

  it('AC10 필터·정렬을 바꾸면 URL에 반영하고 page를 1로 돌린다. 기본값은 쿼리에 넣지 않는다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()], { total: 45 }))
    const { wrapper, router } = await mountList('/concepts?sort=LATEST&page=2')

    await wrapper.find('select[name="difficulty"]').setValue('JUNIOR')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ difficulty: 'JUNIOR', sort: 'LATEST' })

    await wrapper.find('select[name="sort"]').setValue('POPULAR')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ difficulty: 'JUNIOR' })

    await wrapper.find('select[name="category"]').setValue('vue')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ category: 'vue', difficulty: 'JUNIOR' })
    expect(lastCall()).toMatchObject({ category: 'vue', difficulty: 'JUNIOR', page: 1 })
  })

  it('AC10 페이지를 넘기면 조건을 유지한 채 page만 바꾼다', async () => {
    conceptsApi.fetchConcepts.mockImplementation(async ({ page: p }) =>
      page([concept()], { page: p, total: 45 }),
    )
    const { wrapper, router } = await mountList('/concepts?keyword=jwt')

    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ keyword: 'jwt', page: '2' })
    expect(lastCall()).toMatchObject({ keyword: 'jwt', page: 2 })
  })

  it('AC10 뒤로 가기를 하면 이전 조건이 입력란과 목록에 복원된다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([concept()]))
    const { wrapper, router } = await mountList('/concepts?keyword=jwt')

    await wrapper.find('select[name="category"]').setValue('web')
    await flushPromises()
    await wrapper.find('input[name="keyword"]').setValue('세션')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ keyword: '세션', category: 'web' })

    router.back()
    await vi.waitFor(() =>
      expect(router.currentRoute.value.query).toEqual({ keyword: 'jwt', category: 'web' }),
    )
    await flushPromises()

    expect(wrapper.find('input[name="keyword"]').element.value).toBe('jwt')
    expect(wrapper.find('select[name="category"]').element.value).toBe('web')
    expect(lastCall()).toMatchObject({ keyword: 'jwt', category: 'web' })
  })

  it('AC11 불러오는 동안 로딩 문구를 보여 준다', async () => {
    conceptsApi.fetchConcepts.mockReturnValue(new Promise(() => {}))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('불러오는 중')
  })

  it('AC11 결과가 0건이면 안내 문구와 필터 초기화 버튼을 보여 주고, 누르면 /concepts로 간다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([]))
    const { wrapper, router } = await mountList('/concepts?keyword=없는말&category=vue')

    expect(wrapper.text()).toContain('조건에 맞는 개념 카드가 없습니다')

    await wrapper.find('[data-action="reset-filters"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/concepts')
    expect(wrapper.find('input[name="keyword"]').element.value).toBe('')
    expect(wrapper.find('select[name="category"]').element.value).toBe('')
  })

  it('AC11 요청이 실패하면 서버 message를 보여 준다', async () => {
    conceptsApi.fetchConcepts.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { sort: '정렬은 POPULAR, LATEST 중 하나여야 합니다.' },
      }),
    )

    const { wrapper } = await mountList('/concepts?sort=NAME')

    expect(wrapper.find('[role="alert"]').text()).toContain('입력값이 올바르지 않습니다.')
  })

  it('#46 범위를 넘은 page면 마지막 페이지로 바꿔(replace) 다시 조회하고 빈 목록 안내를 보이지 않는다', async () => {
    conceptsApi.fetchConcepts
      .mockResolvedValueOnce(page([], { page: 99, total: 41 }))
      .mockResolvedValue(page([concept()], { page: 3, total: 41 }))

    const { wrapper, router } = await mountList('/concepts?category=vue&page=99')

    expect(router.currentRoute.value.fullPath).toBe('/concepts?category=vue&page=3')
    expect(lastCall()).toMatchObject({ category: 'vue', page: 3 })
    expect(wrapper.findAll('.cards li')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('조건에 맞는 개념 카드가 없습니다')
    expect(wrapper.text()).toContain('3 / 3')
  })

  it('#46 결과가 0건이면 page가 커도 다시 조회하지 않고 빈 목록 안내를 보여 준다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue(page([], { page: 5, total: 0 }))

    const { wrapper, router } = await mountList('/concepts?keyword=없는말&page=5')

    expect(conceptsApi.fetchConcepts).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.query.page).toBe('5')
    expect(wrapper.text()).toContain('조건에 맞는 개념 카드가 없습니다')
  })

  it('#46 응답 전에 다른 화면으로 떠나면 늦게 온 범위 초과 응답으로 주소를 바꾸지 않는다', async () => {
    let resolve
    conceptsApi.fetchConcepts.mockReturnValue(new Promise((r) => (resolve = r)))
    const { router } = await mountList('/concepts?category=vue&page=99')

    await router.push('/community?page=9')
    resolve(page([], { page: 99, total: 41 }))
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/community?page=9')
  })
})
