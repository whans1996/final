import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import * as reviewPlansApi from '@/api/reviewPlans'
import * as analysesApi from '@/api/analyses'
import * as recommendationsApi from '@/api/recommendations'
import { recommendation } from '@/components/recommendation/__tests__/fixtures'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/reviewPlans')
vi.mock('@/api/analyses')
vi.mock('@/api/recommendations')

function concept(id) {
  return {
    id,
    name: `복습${id}`,
    category: 'spring',
    difficulty: 'JUNIOR',
    summary: '요약',
    avgRating: 0,
    wishCount: 0,
  }
}

function plan(id, planDate, status = 'TODO') {
  return { id, planDate, status, reason: 'WRONG', doneAt: null, concept: concept(100 + id) }
}

function plans(partial = {}) {
  return {
    today: '2026-11-20',
    from: '2026-11-20',
    to: '2026-11-26',
    overdue: [plan(1, '2026-11-18')],
    items: [plan(2, '2026-11-20'), plan(3, '2026-11-20', 'DONE'), plan(4, '2026-11-21')],
    ...partial,
  }
}

function analysis(id, partial = {}) {
  return {
    id,
    title: `분석${id}`,
    projectName: `project${id}`,
    status: 'DONE',
    progress: 100,
    level: 'JUNIOR',
    sourceType: 'ZIP',
    isPublic: false,
    createdAt: '2026-11-19T15:30:00+09:00',
    ...partial,
  }
}

async function mountView() {
  const mounted = await mountAt(DashboardView, '/dashboard')
  await flushPromises()
  return mounted
}

const section = (wrapper, name) => wrapper.find(`[data-section="${name}"]`)

describe('DashboardView (F-16 SC-04)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(plans())
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [analysis(9), analysis(8, { title: null, status: 'EXPLAINING' })],
      page: 1,
      size: 3,
      total: 2,
    })
    recommendationsApi.fetchRecommendations.mockResolvedValue({
      items: [recommendation(51), recommendation(52)],
    })
  })

  it('AC8 제목과 세 절을 각각 불러온다', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.find('h1').text()).toBe('대시보드')
    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenCalledWith()
    expect(analysesApi.fetchAnalyses).toHaveBeenCalledWith({ page: 1, size: 3 })
    expect(recommendationsApi.fetchRecommendations).toHaveBeenCalledWith({ size: 5 })
    expect(section(wrapper, 'today').exists()).toBe(true)
    expect(section(wrapper, 'recommendations').exists()).toBe(true)
    expect(section(wrapper, 'recent').exists()).toBe(true)
  })

  it('AC8 오늘 복습은 밀린 복습과 오늘 날짜의 TODO만 보여 준다', async () => {
    const { wrapper } = await mountView()
    const today = section(wrapper, 'today')

    expect(today.find('h2').text()).toBe('오늘 복습할 카드 2개')
    expect(today.findAll('[data-concept-id]').map((c) => c.attributes('data-concept-id'))).toEqual([
      '101',
      '102',
    ])
    expect(today.find('a[href="/review-plans"]').text()).toBe('복습 계획 보기')
  })

  it('AC8 오늘 복습이 5개를 넘으면 5개만 보이고 외 k개를 알린다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(
      plans({
        overdue: [1, 2, 3, 4].map((id) => plan(id, '2026-11-19')),
        items: [5, 6, 7].map((id) => plan(id, '2026-11-20')),
      }),
    )
    const { wrapper } = await mountView()
    const today = section(wrapper, 'today')

    expect(today.find('h2').text()).toBe('오늘 복습할 카드 7개')
    expect(today.findAll('[data-concept-id]')).toHaveLength(5)
    expect(today.find('[data-message="more"]').text()).toBe('외 2개')
  })

  it('AC8 오늘 복습이 없으면 안내 문구를 보여 준다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(
      plans({ overdue: [], items: [plan(3, '2026-11-20', 'DONE')] }),
    )
    const { wrapper } = await mountView()

    expect(section(wrapper, 'today').text()).toContain('오늘 복습할 카드가 없습니다.')
  })

  it('AC8 맞춤 추천은 추천 카드를 보여 준다', async () => {
    const { wrapper } = await mountView()
    const recs = section(wrapper, 'recommendations')

    expect(recs.find('h2').text()).toBe('맞춤 추천')
    expect(recs.findAll('[data-concept-id]').map((c) => c.attributes('data-concept-id'))).toEqual([
      '51',
      '52',
    ])
  })

  it('AC8 최근 분석은 제목(없으면 프로젝트 이름)·상태·요청일과 링크를 보여 준다', async () => {
    const { wrapper } = await mountView()
    const recent = section(wrapper, 'recent')
    const rows = recent.findAll('[data-analysis-id]')

    expect(rows).toHaveLength(2)
    expect(rows[0].attributes('href')).toBe('/analyses/9')
    expect(rows[0].text()).toContain('분석9')
    expect(rows[0].text()).toContain('분석 완료')
    expect(rows[0].text()).toContain('11월 19일')
    expect(rows[1].text()).toContain('project8')
    expect(rows[1].text()).toContain('설명 생성 중')
    expect(recent.find('a[href="/analyses"]').text()).toBe('전체 보기')
  })

  it('AC8 최근 분석이 없으면 안내 문구와 분석 요청 링크를 보여 준다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({ items: [], page: 1, size: 3, total: 0 })
    const { wrapper } = await mountView()
    const recent = section(wrapper, 'recent')

    expect(recent.text()).toContain('아직 분석한 코드가 없습니다.')
    expect(recent.find('a[href="/analyses/new"]').text()).toBe('분석 요청')
  })

  it('AC8 한 절이 실패해도 다른 절은 보이고, 그 절만 다시 시도한다', async () => {
    reviewPlansApi.fetchReviewPlans
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '복습 오류' }))
      .mockResolvedValueOnce(plans())
    analysesApi.fetchAnalyses.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '분석 오류' }),
    )
    const { wrapper } = await mountView()

    expect(section(wrapper, 'today').find('[role="alert"]').text()).toBe('복습 오류')
    expect(section(wrapper, 'recent').find('[role="alert"]').text()).toBe('분석 오류')
    expect(section(wrapper, 'recommendations').findAll('[data-concept-id]')).toHaveLength(2)

    await section(wrapper, 'today').find('[data-action="retry-today"]').trigger('click')
    await flushPromises()

    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenCalledTimes(2)
    expect(analysesApi.fetchAnalyses).toHaveBeenCalledTimes(1)
    expect(section(wrapper, 'today').findAll('[data-concept-id]')).toHaveLength(2)
  })

  it('AC8 불러오는 동안 절마다 로딩 문구를 보인다', async () => {
    reviewPlansApi.fetchReviewPlans.mockReturnValue(new Promise(() => {}))
    analysesApi.fetchAnalyses.mockReturnValue(new Promise(() => {}))
    recommendationsApi.fetchRecommendations.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountView()

    expect(section(wrapper, 'today').text()).toContain('불러오는 중…')
    expect(section(wrapper, 'recent').text()).toContain('불러오는 중…')
    expect(section(wrapper, 'recommendations').text()).toContain('추천을 만드는 중…')
  })
})
