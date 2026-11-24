import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AdminChallengeListView from '@/views/admin/challenge/AdminChallengeListView.vue'
import * as adminApi from '@/api/adminChallenges'
import { routes } from '@/router'
import { adminNavItems } from '@/utils/navItems'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/adminChallenges')

function challenge(partial) {
  return {
    id: 7,
    title: '12월 퀴즈 챌린지',
    imageUrl: '/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png',
    startDate: '2026-12-01',
    endDate: '2026-12-31',
    goalType: 'QUIZ_COUNT',
    goalValue: 10,
    status: 'UPCOMING',
    participantCount: 0,
    ...partial,
  }
}

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function mountList(path = '/admin/challenges') {
  const mounted = await mountAt(AdminChallengeListView, path)
  await flushPromises()
  return mounted
}

const lastCall = () => adminApi.fetchAdminChallenges.mock.calls.at(-1)[0]
const tabs = (wrapper) => wrapper.findAll('[data-status-tab]')

describe('AdminChallengeListView (F-24 AC11·AC12)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    adminApi.fetchAdminChallenges.mockResolvedValue(page([challenge()]))
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC11 관리자 사이드 메뉴에 개념 카드 다음으로 "챌린지"가 있다', () => {
    const admin = routes.find((r) => r.path === '/admin')
    const labels = adminNavItems(admin).map((item) => item.label)

    expect(labels.slice(labels.indexOf('개념 카드'))).toEqual(['개념 카드', '챌린지'])
    expect(adminNavItems(admin).find((item) => item.label === '챌린지').to).toEqual({
      name: 'admin-challenges',
    })
  })

  it('AC11 쿼리가 없으면 전체 탭으로 조회하고 상태 탭 4개를 보여 준다', async () => {
    const { wrapper } = await mountList()

    expect(lastCall()).toEqual({ status: '', page: 1, size: 20 })
    expect(tabs(wrapper).map((t) => t.text())).toEqual(['전체', '예정', '진행 중', '종료'])
    expect(tabs(wrapper)[0].attributes('aria-selected')).toBe('true')
  })

  it('AC11 탭을 누르면 쿼리에 상태를 넣고 1페이지부터 다시 조회한다', async () => {
    const { wrapper, router } = await mountList('/admin/challenges?page=2')

    await tabs(wrapper)[2].trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ status: 'ONGOING' })
    expect(lastCall()).toEqual({ status: 'ONGOING', page: 1, size: 20 })
    expect(tabs(wrapper)[2].attributes('aria-selected')).toBe('true')
  })

  it('AC11 표에 썸네일·제목·기간·목표·상태·참여자 수와 수정·삭제 버튼을 보여 준다', async () => {
    adminApi.fetchAdminChallenges.mockResolvedValue(
      page([
        challenge(),
        challenge({
          id: 3,
          title: '복습 챌린지',
          imageUrl: null,
          startDate: '2026-11-20',
          endDate: '2027-01-10',
          goalType: 'REVIEW_COUNT',
          goalValue: 1000,
          status: 'ONGOING',
          participantCount: 2,
        }),
      ]),
    )

    const { wrapper } = await mountList()

    const headers = wrapper.findAll('thead th').map((th) => th.text())
    expect(headers).toEqual(['이미지', '제목', '기간', '목표', '상태', '참여자', '작업'])
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    const first = rows[0].findAll('td').map((td) => td.text())
    expect(first.slice(1, 6)).toEqual([
      '12월 퀴즈 챌린지',
      '2026.12.01 ~ 12.31',
      '퀴즈 10회',
      '예정',
      '0',
    ])
    expect(rows[0].find('img').attributes('src')).toBe(challenge().imageUrl)
    expect(rows[0].find('[data-action="edit"]').attributes('href')).toBe('/admin/challenges/7/edit')
    expect(rows[0].find('[data-action="delete"]').exists()).toBe(true)

    const second = rows[1].findAll('td').map((td) => td.text())
    expect(second.slice(2, 6)).toEqual([
      '2026.11.20 ~ 2027.01.10',
      '개념 복습 1,000회',
      '진행 중',
      '2',
    ])
    // 이미지가 없으면 목표 유형별 기본 그림
    expect(rows[1].find('img').exists()).toBe(false)
    expect(rows[1].find('[data-thumb="REVIEW_COUNT"]').exists()).toBe(true)
  })

  it('AC11 목록이 비면 안내와 등록 버튼을 보여 준다', async () => {
    adminApi.fetchAdminChallenges.mockResolvedValue(page([]))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('등록된 챌린지가 없습니다')
    expect(wrapper.find('.empty [data-action="new"]').attributes('href')).toBe(
      '/admin/challenges/new',
    )
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('AC11 20개를 넘으면 PageNav로 다음 페이지를 쿼리에 넣어 조회한다', async () => {
    adminApi.fetchAdminChallenges.mockResolvedValue(page([challenge()], { total: 45 }))
    const { wrapper, router } = await mountList()

    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ page: '2' })
    expect(lastCall()).toEqual({ status: '', page: 2, size: 20 })
  })

  it('AC11 불러오는 중과 에러를 보여 주고 다시 시도할 수 있다', async () => {
    let reject
    adminApi.fetchAdminChallenges.mockReturnValue(new Promise((_, r) => (reject = r)))
    const { wrapper } = await mountAt(AdminChallengeListView, '/admin/challenges')

    expect(wrapper.text()).toContain('불러오는 중')

    reject(apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }))
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('서버에서 오류가 발생했습니다.')

    adminApi.fetchAdminChallenges.mockResolvedValue(page([challenge()]))
    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
  })

  it('AC12 삭제를 누르면 화면 안 대화상자에 참여자 수를 알리고, 확인하면 지운 뒤 목록을 다시 부른다', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm')
    adminApi.fetchAdminChallenges.mockResolvedValue(
      page([challenge({ id: 3, participantCount: 2, status: 'ONGOING' })]),
    )
    adminApi.deleteAdminChallenge.mockResolvedValue()
    const { wrapper } = await mountList()
    const callsBefore = adminApi.fetchAdminChallenges.mock.calls.length

    await wrapper.find('[data-action="delete"]').trigger('click')

    const dialog = wrapper.find('[role="dialog"]')
    expect(dialog.exists()).toBe(true)
    expect(dialog.text()).toContain('참여자 2명의 참여 기록도 함께 지워집니다')
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(adminApi.deleteAdminChallenge).not.toHaveBeenCalled()

    await dialog.find('[data-action="confirm-delete"]').trigger('click')
    await flushPromises()

    expect(adminApi.deleteAdminChallenge).toHaveBeenCalledWith(3)
    expect(adminApi.fetchAdminChallenges.mock.calls.length).toBe(callsBefore + 1)
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(wrapper.find('[role="status"]').text()).toContain('삭제했습니다')
  })

  it('AC12 대화상자에서 취소하면 지우지 않는다', async () => {
    const { wrapper } = await mountList()

    await wrapper.find('[data-action="delete"]').trigger('click')
    await wrapper.find('[data-action="cancel-delete"]').trigger('click')

    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(adminApi.deleteAdminChallenge).not.toHaveBeenCalled()
  })

  it('AC12 삭제에 실패하면 목록 위에 메시지를 띄운다', async () => {
    adminApi.deleteAdminChallenge.mockRejectedValue(
      apiError(404, { code: 'CHALLENGE_404', message: '챌린지를 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountList()

    await wrapper.find('[data-action="delete"]').trigger('click')
    await wrapper.find('[data-action="confirm-delete"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="action-error"]').text()).toContain(
      '챌린지를 찾을 수 없습니다.',
    )
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(wrapper.findAll('tbody tr')).toHaveLength(1)
  })
})
