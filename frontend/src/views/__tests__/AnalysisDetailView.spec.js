import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AnalysisDetailView from '@/views/AnalysisDetailView.vue'
import * as analysesApi from '@/api/analyses'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/analyses')

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

function status(partial) {
  return {
    analysisId: 42,
    status: 'FETCHING',
    progress: 5,
    errorCode: null,
    errorMessage: null,
    updatedAt: '2026-11-24T14:20:01+09:00',
    ...partial,
  }
}

async function mountDetail(path = '/analyses/42') {
  const mounted = await mountAt(AnalysisDetailView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

async function advance(ms) {
  await vi.advanceTimersByTimeAsync(ms)
  await flushPromises()
}

describe('AnalysisDetailView (F-05a AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC15 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path.startsWith('/analyses/:id'))
    const to = { meta: route.meta, fullPath: '/analyses/42' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/analyses/42' },
    })
  })

  it('AC15 상태를 바로 1회, 이후 2초마다 조회하며 단계 라벨과 진행률 막대를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(status({ status: 'FETCHING', progress: 5 }))
      .mockResolvedValueOnce(status({ status: 'FILTERING', progress: 15 }))
      .mockResolvedValue(status({ status: 'SELECTING', progress: 25 }))

    const { wrapper } = await mountDetail()

    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledWith(42)
    expect(wrapper.text()).toContain('코드 수집 중')
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('5')

    await advance(1999)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    await advance(1)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('파일 검사 중')
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('15')

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('주요 파일 선별 중')
    wrapper.unmount()
  })

  it('AC15 DONE이면 완료 안내를 보여 주고 폴링을 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(status({ status: 'SELECTING', progress: 25 }))
      .mockResolvedValue(status({ status: 'DONE', progress: 100 }))

    const { wrapper } = await mountDetail()
    await advance(2000)

    expect(wrapper.find('[data-message="done"]').text()).toContain('분석이 끝났습니다')
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
  })

  it('AC15 FAILED면 errorMessage와 다시 요청하기 링크를 보여 주고 폴링을 멈춘다', async () => {
    const message = '지원하는 코드 파일이 312개입니다. 200개 이하로 줄여서 다시 올려 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'FAILED', progress: 15, errorCode: 'ANALYSIS_413', errorMessage: message }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-message="failed"]').text()).toContain(message)
    expect(wrapper.find('a[data-action="retry"]').attributes('href')).toBe('/analyses/new')
    expect(wrapper.text()).toContain('분석 실패')
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 화면을 떠나면 폴링을 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'FILTERING', progress: 15 }))

    const { wrapper } = await mountDetail()
    wrapper.unmount()
    await advance(10000)

    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 404면 "분석을 찾을 수 없습니다."를 보여 주고 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockRejectedValue(
      apiError(404, { code: 'ANALYSIS_404', message: '분석을 찾을 수 없습니다.', details: null }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.text()).toContain('분석을 찾을 수 없습니다.')
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(false)
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 그 밖의 에러는 서버 메시지를 보여 주고 다음 주기에 다시 조회한다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisStatus
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(status({ status: 'FILTERING', progress: 15 }))

    const { wrapper } = await mountDetail()
    expect(wrapper.find('[role="alert"]').text()).toBe(message)

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('파일 검사 중')
    wrapper.unmount()
  })

  it('AC15 다른 분석으로 이동하면 새 id로 다시 폴링하고 이전 폴링은 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockImplementation(async (id) =>
      status({ analysisId: id, status: 'FILTERING', progress: 15 }),
    )

    const { wrapper, router } = await mountDetail('/analyses/42')
    await router.push('/analyses/43')
    await flushPromises()
    await advance(2000)

    const ids = analysesApi.fetchAnalysisStatus.mock.calls.map(([id]) => id)
    expect(ids).toEqual([42, 43, 43])
    wrapper.unmount()
  })
})
