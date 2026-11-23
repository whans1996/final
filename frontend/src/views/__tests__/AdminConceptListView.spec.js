import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AdminConceptListView from '@/views/admin/concept/AdminConceptListView.vue'
import * as adminApi from '@/api/adminConcepts'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/adminConcepts')

function concept(partial) {
  return {
    id: 91,
    name: '낙관적 락',
    category: 'spring',
    difficulty: 'SENIOR',
    summary: '버전으로 충돌을 확인한다.',
    status: 'DRAFT',
    source: 'AI',
    wishCount: 3,
    avgRating: 4.5,
    updatedAt: '2026-11-20T14:20:00+09:00',
    ...partial,
  }
}

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function mountList(path = '/admin/concepts') {
  const mounted = await mountAt(AdminConceptListView, path)
  await flushPromises()
  return mounted
}

const lastCall = () => adminApi.fetchAdminConcepts.mock.calls.at(-1)[0]
const tabs = (wrapper) => wrapper.findAll('[data-status-tab]')

describe('AdminConceptListView (F-13 AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    adminApi.fetchAdminConceptCounts.mockResolvedValue({ DRAFT: 2, PUBLISHED: 85, HIDDEN: 1 })
    adminApi.fetchAdminConcepts.mockResolvedValue(page([concept()]))
  })

  it('AC13 쿼리가 없으면 검수 대기(DRAFT) 탭으로 조회하고 탭에 상태별 개수를 보여 준다', async () => {
    const { wrapper } = await mountList()

    expect(lastCall()).toEqual({ status: 'DRAFT', source: '', keyword: '', page: 1, size: 20 })
    expect(tabs(wrapper).map((t) => t.text().replace(/\s+/g, ' '))).toEqual([
      '검수 대기 2',
      '공개 85',
      '숨김 1',
      '전체',
    ])
    expect(tabs(wrapper)[0].attributes('aria-selected')).toBe('true')
  })

  it('AC13 표에 이름·카테고리·난이도·출처·상태·찜·평점·수정일과 상태별 버튼을 보여 준다', async () => {
    adminApi.fetchAdminConcepts.mockResolvedValue(
      page([concept(), concept({ id: 7, name: 'JWT', status: 'PUBLISHED', source: 'SEED' })]),
    )

    const { wrapper } = await mountList('/admin/concepts?status=ALL')

    const headers = wrapper.findAll('thead th').map((th) => th.text())
    expect(headers).toEqual([
      '이름',
      '카테고리',
      '난이도',
      '출처',
      '상태',
      '찜',
      '평점',
      '수정일',
      '작업',
    ])
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    const first = rows[0].findAll('td').map((td) => td.text())
    expect(first.slice(0, 8)).toEqual([
      '낙관적 락',
      'Spring',
      '숙련',
      'AI 초안',
      '검수 대기',
      '3',
      '4.5',
      '2026-11-20',
    ])
    const review = rows[0].find('[data-action="edit"]')
    expect(review.text()).toBe('검수')
    expect(review.attributes('href')).toBe('/admin/concepts/91/edit')
    const edit = rows[1].find('[data-action="edit"]')
    expect(edit.text()).toBe('수정')
    expect(rows[1].text()).toContain('시드')
    expect(rows[1].text()).toContain('공개')
  })

  it('AC13 탭을 누르면 status 쿼리가 바뀌고 1페이지로 다시 조회한다 (전체는 ALL, 조회에는 status 없음)', async () => {
    const { wrapper, router } = await mountList('/admin/concepts?keyword=jwt&page=3')

    await tabs(wrapper)[1].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ status: 'PUBLISHED', keyword: 'jwt' })
    expect(lastCall()).toEqual({
      status: 'PUBLISHED',
      source: '',
      keyword: 'jwt',
      page: 1,
      size: 20,
    })

    await tabs(wrapper)[3].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ status: 'ALL', keyword: 'jwt' })
    expect(lastCall()).toEqual({ status: '', source: '', keyword: 'jwt', page: 1, size: 20 })

    await tabs(wrapper)[0].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ keyword: 'jwt' })
  })

  it('AC13 출처 필터와 검색어를 쿼리에 반영한다', async () => {
    const { wrapper, router } = await mountList('/admin/concepts?status=HIDDEN')

    const options = wrapper
      .findAll('select[name="source"] option')
      .map((o) => [o.element.value, o.text()])
    expect(options).toEqual([
      ['', '전체 출처'],
      ['SEED', '시드'],
      ['AI', 'AI 초안'],
      ['ADMIN', '직접 작성'],
    ])

    await wrapper.find('select[name="source"]').setValue('SEED')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ status: 'HIDDEN', source: 'SEED' })

    await wrapper.find('input[name="keyword"]').setValue('  락 ')
    await wrapper.find('form[role="search"]').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({
      status: 'HIDDEN',
      source: 'SEED',
      keyword: '락',
    })
    expect(lastCall()).toEqual({
      status: 'HIDDEN',
      source: 'SEED',
      keyword: '락',
      page: 1,
      size: 20,
    })
  })

  it('AC13 새로고침(쿼리로 진입)·뒤로 가기에 조건이 유지된다', async () => {
    adminApi.fetchAdminConcepts.mockResolvedValue(page([concept()], { page: 2, total: 45 }))
    const { wrapper, router } = await mountList('/admin/concepts?status=PUBLISHED&source=AI&page=2')

    expect(lastCall()).toEqual({
      status: 'PUBLISHED',
      source: 'AI',
      keyword: '',
      page: 2,
      size: 20,
    })
    expect(wrapper.find('select[name="source"]').element.value).toBe('AI')
    expect(tabs(wrapper)[1].attributes('aria-selected')).toBe('true')

    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({
      status: 'PUBLISHED',
      source: 'AI',
      page: '3',
    })

    router.back()
    await flushPromises()
    expect(lastCall()).toEqual({
      status: 'PUBLISHED',
      source: 'AI',
      keyword: '',
      page: 2,
      size: 20,
    })
  })

  it.each([
    ['', '검수할 초안이 없습니다.'],
    ['?status=PUBLISHED', '공개된 카드가 없습니다.'],
    ['?status=HIDDEN', '숨긴 카드가 없습니다.'],
    ['?status=ALL', '개념 카드가 없습니다.'],
  ])('AC13 탭에 카드가 없으면 탭별 문구를 보여 준다 (%s)', async (query, message) => {
    adminApi.fetchAdminConcepts.mockResolvedValue(page([]))

    const { wrapper } = await mountList(`/admin/concepts${query}`)

    expect(wrapper.text()).toContain(message)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('AC13 검색·출처 조건으로 비었으면 조건 문구를 보여 준다', async () => {
    adminApi.fetchAdminConcepts.mockResolvedValue(page([]))

    const { wrapper } = await mountList('/admin/concepts?keyword=zzz')

    expect(wrapper.text()).toContain('조건에 맞는 카드가 없습니다.')
  })

  it('AC13 조회에 실패하면 서버 메시지와 다시 시도 버튼을 보여 준다', async () => {
    adminApi.fetchAdminConcepts.mockRejectedValueOnce(
      apiError(400, { code: 'COMMON_400', message: '요청 값이 올바르지 않습니다.' }),
    )

    const { wrapper } = await mountList()

    expect(wrapper.find('[role="alert"]').text()).toContain('요청 값이 올바르지 않습니다.')
    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
  })

  it('AC13 새 카드 링크와 편집 화면에서 넘긴 성공 안내를 보여 준다', async () => {
    const { wrapper, router } = await mountList()

    expect(wrapper.find('[data-action="new"]').attributes('href')).toBe('/admin/concepts/new')

    await router.push({
      name: 'admin-concepts',
      query: { status: 'ALL' },
      state: { notice: '저장했습니다.' },
    })
    await flushPromises()
    expect(wrapper.find('[role="status"]').text()).toBe('저장했습니다.')
  })

  it('AC13 성공 안내는 한 번만 보이고 history state에서 지워 새로고침 때 다시 뜨지 않는다', async () => {
    const { router } = await mountList()
    await router.push({
      name: 'admin-concepts',
      query: { status: 'ALL' },
      state: { notice: '공개했습니다.' },
    })
    await flushPromises()

    expect(router.options.history.state.notice).toBeFalsy()
    expect(router.options.history.location).toBe('/admin/concepts?status=ALL')

    // 새로고침: 같은 history 항목에서 화면을 다시 띄운다
    const reloaded = await mountAt(AdminConceptListView, '/admin/concepts?status=ALL')
    await flushPromises()
    expect(reloaded.wrapper.find('[role="status"]').exists()).toBe(false)
  })

  it('#76 마지막 페이지의 카드를 처리하고 돌아와 page가 범위를 넘으면 마지막 페이지로 바꾸고 안내를 유지한다', async () => {
    const { wrapper, router } = await mountList()
    adminApi.fetchAdminConcepts
      .mockResolvedValueOnce(page([], { page: 3, total: 40 }))
      .mockResolvedValue(page([concept()], { page: 2, total: 40 }))

    await router.push({
      name: 'admin-concepts',
      query: { page: '3' },
      state: { notice: '공개했습니다.' },
    })
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/admin/concepts?page=2')
    expect(lastCall()).toMatchObject({ status: 'DRAFT', page: 2 })
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
    expect(wrapper.find('[role="status"]').text()).toBe('공개했습니다.')
    expect(router.options.history.state.notice).toBeFalsy()
  })

  it('#76 결과가 0건이면 page가 커도 다시 조회하지 않고 빈 목록 안내를 보여 준다', async () => {
    adminApi.fetchAdminConcepts.mockResolvedValue(page([], { page: 2, total: 0 }))

    const { wrapper, router } = await mountList('/admin/concepts?page=2')

    expect(adminApi.fetchAdminConcepts).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.query.page).toBe('2')
    expect(wrapper.text()).toContain('검수할 초안이 없습니다.')
  })
})
