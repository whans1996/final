import { afterEach, beforeEach, describe, expect, it, onTestFinished, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AdminChallengeEditView from '@/views/admin/challenge/AdminChallengeEditView.vue'
import * as adminApi from '@/api/adminChallenges'
import { apiError, deferred, mountAt } from './viewTestUtils'

vi.mock('@/api/adminChallenges')

const LOCK_MESSAGE = '이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다'

function detail(partial) {
  return {
    id: 7,
    title: '12월 퀴즈 챌린지',
    description: '퀴즈를 10번 풀어 보세요.',
    imageUrl: '/uploads/challenges/old.png',
    startDate: '2026-12-01',
    endDate: '2026-12-31',
    goalType: 'QUIZ_COUNT',
    goalValue: 10,
    status: 'UPCOMING',
    participantCount: 0,
    createdAt: '2026-11-24T08:50:00+09:00',
    updatedAt: '2026-11-24T08:50:00+09:00',
    ...partial,
  }
}

async function mountEdit(path) {
  const mounted = await mountAt(AdminChallengeEditView, path)
  await flushPromises()
  return mounted
}

async function fillForm(wrapper, values) {
  for (const [name, value] of Object.entries(values)) {
    const field = wrapper.find(`[name="${name}"]`)
    await field.setValue(value)
  }
}

const save = async (wrapper) => {
  await wrapper.find('[data-action="save"]').trigger('click')
  await flushPromises()
}

const errorFor = (wrapper, name) => wrapper.find(`[data-error-for="${name}"]`)

describe('AdminChallengeEditView (F-24 AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // 오늘 = 2026-11-24 (KST 오전)
    vi.useFakeTimers({ now: new Date('2026-11-24T10:00:00+09:00'), toFake: ['Date'] })
    URL.createObjectURL = vi.fn(() => 'blob:preview')
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC13 새 챌린지는 클라이언트 검증을 통과해야 요청하고, 성공하면 목록으로 돌아간다', async () => {
    adminApi.createAdminChallenge.mockResolvedValue({ id: 9 })
    const { wrapper, router } = await mountEdit('/admin/challenges/new')

    await save(wrapper)
    expect(adminApi.createAdminChallenge).not.toHaveBeenCalled()
    expect(errorFor(wrapper, 'title').text()).toBe('제목을 입력하세요.')
    expect(errorFor(wrapper, 'startDate').exists()).toBe(true)
    expect(errorFor(wrapper, 'goalType').exists()).toBe(true)

    await fillForm(wrapper, {
      title: '  12월 퀴즈 챌린지 ',
      startDate: '2026-12-01',
      endDate: '2027-03-01', // 91일
      goalType: 'QUIZ_COUNT',
      goalValue: '1001',
    })
    await save(wrapper)
    expect(adminApi.createAdminChallenge).not.toHaveBeenCalled()
    expect(errorFor(wrapper, 'endDate').text()).toContain('90일')
    expect(errorFor(wrapper, 'goalValue').text()).toContain('1~1,000')

    await fillForm(wrapper, { endDate: '2026-12-31', goalValue: '10' })
    await save(wrapper)

    expect(adminApi.createAdminChallenge).toHaveBeenCalledWith({
      title: '12월 퀴즈 챌린지',
      description: '',
      startDate: '2026-12-01',
      endDate: '2026-12-31',
      goalType: 'QUIZ_COUNT',
      goalValue: '10',
      image: null,
      removeImage: false,
    })
    // 목록 route는 lazy import라 이동이 flushPromises보다 늦게 끝난다
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('admin-challenges'))
  })

  it('F-25 AC16 오늘은 브라우저 시간대가 아닌 서울 날짜다 (UTC 11-24 15:00:01 = 서울 11-25)', async () => {
    // 브라우저가 UTC에 있는 것처럼 (교육장 PC는 KST라 그대로면 차이가 드러나지 않는다)
    const env = globalThis.process.env
    const originalTz = env.TZ
    env.TZ = 'UTC'
    onTestFinished(() => {
      // undefined를 넣으면 문자열 'undefined'가 되므로 지운다
      if (originalTz === undefined) delete env.TZ
      else env.TZ = originalTz
    })
    vi.setSystemTime(new Date('2026-11-24T15:00:01Z'))
    const { wrapper } = await mountEdit('/admin/challenges/new')
    await fillForm(wrapper, {
      title: '어제 끝난 챌린지',
      startDate: '2026-11-20',
      endDate: '2026-11-24',
      goalType: 'REVIEW_COUNT',
      goalValue: '5',
    })

    await save(wrapper)

    expect(adminApi.createAdminChallenge).not.toHaveBeenCalled()
    expect(errorFor(wrapper, 'endDate').text()).toBe('종료일은 오늘이거나 오늘 이후여야 합니다.')
  })

  it('AC13 종료일이 오늘보다 앞이거나 이미지 형식·크기가 맞지 않으면 요청하지 않는다', async () => {
    const { wrapper } = await mountEdit('/admin/challenges/new')
    await fillForm(wrapper, {
      title: '지난 챌린지',
      startDate: '2026-11-01',
      endDate: '2026-11-23',
      goalType: 'REVIEW_COUNT',
      goalValue: '5',
    })
    const gif = new File(['GIF89a'], 'a.gif', { type: 'image/gif' })
    const input = wrapper.find('input[name="image"]')
    Object.defineProperty(input.element, 'files', { value: [gif], configurable: true })
    await input.trigger('change')

    await save(wrapper)

    expect(adminApi.createAdminChallenge).not.toHaveBeenCalled()
    expect(errorFor(wrapper, 'endDate').text()).toBe('종료일은 오늘이거나 오늘 이후여야 합니다.')
    expect(errorFor(wrapper, 'image').text()).toBe('jpg·png·webp 이미지만 올릴 수 있습니다.')

    const big = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'big.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [big], configurable: true })
    await input.trigger('change')
    await save(wrapper)
    expect(errorFor(wrapper, 'image').text()).toBe('이미지는 2MB 이하만 올릴 수 있습니다.')
  })

  it('AC13 서버 400 details는 해당 필드 아래에 보여 준다', async () => {
    adminApi.createAdminChallenge.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '요청 값이 올바르지 않습니다.',
        details: { image: '서버가 본 이미지 사유', title: '서버가 본 제목 사유' },
      }),
    )
    const { wrapper, router } = await mountEdit('/admin/challenges/new')
    await fillForm(wrapper, {
      title: '챌린지',
      startDate: '2026-12-01',
      endDate: '2026-12-31',
      goalType: 'QUIZ_COUNT',
      goalValue: '10',
    })

    await save(wrapper)

    expect(errorFor(wrapper, 'image').text()).toBe('서버가 본 이미지 사유')
    expect(errorFor(wrapper, 'title').text()).toBe('서버가 본 제목 사유')
    expect(router.currentRoute.value.name).toBe('admin-challenge-new')
  })

  it('AC13 예정 챌린지 수정 화면은 값을 채우고 모든 입력을 바꿀 수 있으며 PUT으로 저장한다', async () => {
    adminApi.fetchAdminChallenge.mockResolvedValue(detail())
    adminApi.updateAdminChallenge.mockResolvedValue(detail())
    const { wrapper, router } = await mountEdit('/admin/challenges/7/edit')

    expect(adminApi.fetchAdminChallenge).toHaveBeenCalledWith(7)
    expect(wrapper.find('[name="title"]').element.value).toBe('12월 퀴즈 챌린지')
    expect(wrapper.find('[name="goalType"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.text()).not.toContain(LOCK_MESSAGE)

    await fillForm(wrapper, { goalValue: '20', startDate: '2026-12-05' })
    await save(wrapper)

    expect(adminApi.updateAdminChallenge).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ goalValue: '20', startDate: '2026-12-05', removeImage: false }),
    )
    // 목록 route는 lazy import라 이동이 flushPromises보다 늦게 끝난다
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('admin-challenges'))
  })

  it('AC13 시작한 챌린지는 목표 유형·목표값·시작일이 비활성이고 안내를 보여 준다. 지난 종료일을 그대로 두면 저장할 수 있다', async () => {
    adminApi.fetchAdminChallenge.mockResolvedValue(
      detail({ startDate: '2026-11-01', endDate: '2026-11-10', status: 'ENDED' }),
    )
    adminApi.updateAdminChallenge.mockResolvedValue(detail())
    const { wrapper } = await mountEdit('/admin/challenges/7/edit')

    for (const name of ['goalType', 'goalValue', 'startDate']) {
      expect(wrapper.find(`[name="${name}"]`).attributes('disabled')).toBeDefined()
    }
    expect(wrapper.find('[name="endDate"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="lock-notice"]').text()).toContain(LOCK_MESSAGE)

    await fillForm(wrapper, { title: '제목만 수정' })
    await save(wrapper)

    expect(adminApi.updateAdminChallenge).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ title: '제목만 수정', endDate: '2026-11-10' }),
    )
  })

  it('AC13 409 CHALLENGE_001을 받으면 잠금 안내를 보여 준다', async () => {
    adminApi.fetchAdminChallenge.mockResolvedValue(detail())
    adminApi.updateAdminChallenge.mockRejectedValue(
      apiError(409, {
        code: 'CHALLENGE_001',
        message: '이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다.',
      }),
    )
    const { wrapper, router } = await mountEdit('/admin/challenges/7/edit')

    await save(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toContain(LOCK_MESSAGE)
    expect(router.currentRoute.value.name).toBe('admin-challenge-edit')
  })

  it('AC13 이미지를 제거하면 removeImage=true로 보낸다', async () => {
    adminApi.fetchAdminChallenge.mockResolvedValue(detail())
    adminApi.updateAdminChallenge.mockResolvedValue(detail({ imageUrl: null }))
    const { wrapper } = await mountEdit('/admin/challenges/7/edit')

    await wrapper.find('[data-action="remove-image"]').trigger('click')
    await save(wrapper)

    expect(adminApi.updateAdminChallenge).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ image: null, removeImage: true }),
    )
  })

  it('AC13 없는 챌린지는 "챌린지를 찾을 수 없습니다"를 보여 준다', async () => {
    adminApi.fetchAdminChallenge.mockRejectedValue(
      apiError(404, { code: 'CHALLENGE_404', message: '챌린지를 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountEdit('/admin/challenges/999/edit')

    expect(wrapper.text()).toContain('챌린지를 찾을 수 없습니다')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('AC13 같은 화면에서 다른 챌린지 주소로 바뀌면 그 챌린지를 다시 불러와 그 id로 저장한다', async () => {
    let resolveFirst
    adminApi.fetchAdminChallenge
      .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
      .mockResolvedValueOnce(detail({ id: 8, title: '8번 챌린지' }))
    adminApi.updateAdminChallenge.mockResolvedValue(detail({ id: 8 }))
    const { wrapper, router } = await mountEdit('/admin/challenges/7/edit')

    await router.push('/admin/challenges/8/edit')
    await flushPromises()
    // 7번 응답이 늦게 와도 8번 화면을 덮어쓰지 않는다
    resolveFirst(detail())
    await flushPromises()

    expect(adminApi.fetchAdminChallenge).toHaveBeenLastCalledWith(8)
    expect(wrapper.find('[name="title"]').element.value).toBe('8번 챌린지')

    await save(wrapper)
    expect(adminApi.updateAdminChallenge).toHaveBeenCalledWith(8, expect.anything())

    await router.push('/admin/challenges/new')
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('새 챌린지')
    expect(wrapper.find('[name="title"]').element.value).toBe('')
  })

  it('AC13 불러오기에 실패하면 에러를 보여 주고, 다시 시도하면 같은 챌린지를 불러와 폼을 보여 준다', async () => {
    adminApi.fetchAdminChallenge
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce(detail())
    const { wrapper } = await mountEdit('/admin/challenges/7/edit')

    expect(wrapper.find('[role="alert"]').text()).toContain('서버 오류입니다.')

    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()

    expect(adminApi.fetchAdminChallenge).toHaveBeenLastCalledWith(7)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('[name="title"]').element.value).toBe('12월 퀴즈 챌린지')
  })

  it('AC13 저장 중에는 버튼이 비활성이다', async () => {
    let resolve
    adminApi.createAdminChallenge.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountEdit('/admin/challenges/new')
    await fillForm(wrapper, {
      title: '챌린지',
      startDate: '2026-12-01',
      endDate: '2026-12-31',
      goalType: 'QUIZ_COUNT',
      goalValue: '10',
    })

    await wrapper.find('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-action="save"]').attributes('disabled')).toBeDefined()

    resolve({ id: 9 })
    await flushPromises()
  })
})

describe('AdminChallengeEditView 늦은 응답 (V-4 AC6·AC7)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC6·AC7 이전 id의 늦은 실패와 화면을 떠난 뒤 온 응답은 화면 상태를 바꾸지 않는다', async () => {
    const first = deferred()
    const second = deferred()
    adminApi.fetchAdminChallenge
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)
    const { wrapper, router } = await mountEdit('/admin/challenges/7/edit')

    await router.push('/admin/challenges/8/edit')
    first.reject(apiError(500, { code: 'COMMON_500', message: '옛 챌린지 실패' }))
    await flushPromises()
    const vm = wrapper.vm
    expect(vm.loadError).toBe('')
    expect(vm.loading).toBe(true)

    wrapper.unmount()
    second.resolve(detail({ id: 8 }))
    await flushPromises()
    expect(vm.challenge).toBeNull()
    expect(vm.loading).toBe(true)
  })
})
