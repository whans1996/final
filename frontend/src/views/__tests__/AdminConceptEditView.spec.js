import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import AdminConceptEditView from '@/views/admin/concept/AdminConceptEditView.vue'
import * as adminApi from '@/api/adminConcepts'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/adminConcepts')

function detail(partial) {
  return {
    id: 91,
    name: '낙관적 락',
    category: 'spring',
    difficulty: 'SENIOR',
    summary: '버전으로 충돌을 확인한다.',
    body: '## 핵심',
    exampleCode: null,
    referenceUrl: null,
    status: 'DRAFT',
    source: 'AI',
    wishCount: 0,
    avgRating: 0,
    reviewCount: 0,
    linkedAnalysisCount: 2,
    createdAt: '2026-11-20T14:20:00+09:00',
    updatedAt: '2026-11-20T14:20:00+09:00',
    ...partial,
  }
}

async function mountEdit(path) {
  const mounted = await mountAt(AdminConceptEditView, path)
  await flushPromises()
  return mounted
}

const buttons = (wrapper) => wrapper.findAll('.actions button').map((b) => b.text())
const click = async (wrapper, label) => {
  await wrapper
    .findAll('.actions button')
    .find((b) => b.text() === label)
    .trigger('click')
  await flushPromises()
}
const field = (wrapper, name) => wrapper.find(`[name="${name}"]`)
// 목록 route는 lazy import라 flushPromises만으로는 이동이 끝나지 않는다
const arrived = (router) =>
  vi.waitFor(() => expect(router.currentRoute.value.name).toBe('admin-concepts'))
const lastBody = (fn) => fn.mock.calls.at(-1).at(-1)

describe('AdminConceptEditView (F-13 AC14·AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.restoreAllMocks()
  })

  it('AC14 새 카드는 "초안으로 저장"·"공개"로 POST 하고 목록으로 돌아가 안내를 넘긴다', async () => {
    adminApi.createAdminConcept.mockResolvedValue({ id: 92 })
    const { wrapper, router } = await mountEdit('/admin/concepts/new')

    expect(adminApi.fetchAdminConcept).not.toHaveBeenCalled()
    expect(buttons(wrapper)).toEqual(['초안으로 저장', '공개'])

    await field(wrapper, 'name').setValue('JWT 인증')
    await field(wrapper, 'category').setValue('security')
    await field(wrapper, 'difficulty').setValue('JUNIOR')
    await field(wrapper, 'summary').setValue('서명한 토큰')
    await click(wrapper, '공개')

    expect(lastBody(adminApi.createAdminConcept)).toEqual({
      name: 'JWT 인증',
      category: 'security',
      difficulty: 'JUNIOR',
      summary: '서명한 토큰',
      body: null,
      exampleCode: null,
      referenceUrl: null,
      status: 'PUBLISHED',
    })
    await arrived(router)
    expect(router.options.history.state.notice).toBe('공개했습니다.')
  })

  it('AC14 AI 초안은 안내와 연결된 분석 수를 보이고, 초안으로 저장·공개·숨김·삭제 버튼을 둔다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail())
    adminApi.updateAdminConcept.mockResolvedValue(detail({ status: 'HIDDEN' }))
    const { wrapper, router } = await mountEdit('/admin/concepts/91/edit')

    expect(adminApi.fetchAdminConcept).toHaveBeenCalledWith(91)
    expect(wrapper.text()).toContain('AI가 만든 초안입니다. 사실관계·예제를 확인한 뒤 공개하세요.')
    expect(wrapper.text()).toContain('연결된 분석 2개')
    expect(buttons(wrapper)).toEqual(['초안으로 저장', '공개', '숨김', '삭제'])
    expect(field(wrapper, 'name').attributes('readonly')).toBeUndefined()

    await click(wrapper, '숨김')

    expect(adminApi.updateAdminConcept).toHaveBeenCalledWith(91, {
      name: '낙관적 락',
      category: 'spring',
      difficulty: 'SENIOR',
      summary: '버전으로 충돌을 확인한다.',
      body: '## 핵심',
      exampleCode: null,
      referenceUrl: null,
      status: 'HIDDEN',
    })
    await arrived(router)
    expect(router.options.history.state.notice).toBe('숨겼습니다.')
  })

  it('AC14 공개 카드는 "저장"(상태 유지)·"숨김"만 있고 "초안으로 저장"·"삭제"가 없다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail({ status: 'PUBLISHED', source: 'ADMIN' }))
    adminApi.updateAdminConcept.mockResolvedValue(detail({ status: 'PUBLISHED' }))
    const { wrapper, router } = await mountEdit('/admin/concepts/91/edit')

    expect(buttons(wrapper)).toEqual(['저장', '숨김'])
    expect(wrapper.text()).not.toContain('AI가 만든 초안입니다')

    await field(wrapper, 'summary').setValue('고친 요약')
    await click(wrapper, '저장')

    expect(lastBody(adminApi.updateAdminConcept)).toMatchObject({
      summary: '고친 요약',
      status: 'PUBLISHED',
    })
    await arrived(router)
    expect(router.options.history.state.notice).toBe('저장했습니다.')
  })

  it('AC14 숨긴 카드는 "저장"·"공개"만 있다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail({ status: 'HIDDEN', source: 'ADMIN' }))
    const { wrapper } = await mountEdit('/admin/concepts/91/edit')

    expect(buttons(wrapper)).toEqual(['저장', '공개'])
  })

  it('AC14 시드 카드는 안내를 보이고 이름 칸을 읽기 전용으로 둔다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail({ status: 'PUBLISHED', source: 'SEED' }))
    const { wrapper } = await mountEdit('/admin/concepts/91/edit')

    expect(wrapper.text()).toContain(
      '시드 카드입니다. 이름은 바꿀 수 없고, 시드 파일이 바뀌면 내용이 파일 기준으로 다시 덮어써집니다.',
    )
    expect(field(wrapper, 'name').attributes('readonly')).toBeDefined()
  })

  it('AC14 저장 중에는 버튼을 막아 중복 제출하지 않는다', async () => {
    let resolve
    adminApi.createAdminConcept.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountEdit('/admin/concepts/new')

    await wrapper.findAll('.actions button')[0].trigger('click')
    expect(
      wrapper.findAll('.actions button').every((b) => b.attributes('disabled') !== undefined),
    ).toBe(true)
    await wrapper.findAll('.actions button')[0].trigger('click')
    expect(adminApi.createAdminConcept).toHaveBeenCalledTimes(1)
    resolve({ id: 1 })
    await flushPromises()
  })

  it('AC15 400 COMMON_400의 details를 필드 아래에 보이고 입력값을 유지한다', async () => {
    adminApi.createAdminConcept.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: {
          referenceUrl: 'http:// 또는 https://로 시작해야 합니다.',
          category: '허용되지 않은 값',
        },
      }),
    )
    const { wrapper, router } = await mountEdit('/admin/concepts/new')

    await field(wrapper, 'name').setValue('JWT 인증')
    await field(wrapper, 'referenceUrl').setValue('ftp://x')
    await click(wrapper, '초안으로 저장')

    expect(wrapper.find('[data-error-for="referenceUrl"]').text()).toBe(
      'http:// 또는 https://로 시작해야 합니다.',
    )
    expect(wrapper.find('[data-error-for="category"]').text()).toBe('허용되지 않은 값')
    expect(field(wrapper, 'name').element.value).toBe('JWT 인증')
    expect(field(wrapper, 'referenceUrl').element.value).toBe('ftp://x')
    expect(router.currentRoute.value.name).toBe('admin-concept-new')
  })

  it('AC15 409 CONCEPT_002면 이름 아래에 사유를 보인다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail())
    adminApi.updateAdminConcept.mockRejectedValue(
      apiError(409, {
        code: 'CONCEPT_002',
        message: '같은 이름의 개념 카드가 이미 있습니다.',
        details: { name: '같은 이름의 개념 카드가 이미 있습니다.' },
      }),
    )
    const { wrapper } = await mountEdit('/admin/concepts/91/edit')

    await field(wrapper, 'name').setValue('JWT 인증')
    await click(wrapper, '공개')

    expect(wrapper.find('[data-error-for="name"]').text()).toBe(
      '같은 이름의 개념 카드가 이미 있습니다.',
    )
    expect(field(wrapper, 'name').element.value).toBe('JWT 인증')
  })

  it('AC15 details가 없는 실패(409 CONCEPT_003 등)는 서버 메시지를 버튼 위에 보인다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail({ status: 'PUBLISHED' }))
    adminApi.updateAdminConcept.mockRejectedValue(
      apiError(409, { code: 'CONCEPT_003', message: '공개되었거나 숨긴 카드는 …' }),
    )
    const { wrapper } = await mountEdit('/admin/concepts/91/edit')

    await click(wrapper, '저장')

    expect(wrapper.find('[data-testid="submit-error"]').text()).toBe('공개되었거나 숨긴 카드는 …')
  })

  it('AC15 초안 "삭제"는 확인 대화상자 뒤에 DELETE 하고 목록으로 간다 (취소하면 지우지 않는다)', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail())
    adminApi.deleteAdminConcept.mockResolvedValue()
    const confirm = vi.spyOn(window, 'confirm')
    const { wrapper, router } = await mountEdit('/admin/concepts/91/edit')

    confirm.mockReturnValue(false)
    await click(wrapper, '삭제')
    expect(confirm).toHaveBeenCalledWith(
      '이 초안과 분석 2개의 연결을 지웁니다. 다시 추출되지 않게 하려면 숨김을 쓰세요.',
    )
    expect(adminApi.deleteAdminConcept).not.toHaveBeenCalled()

    confirm.mockReturnValue(true)
    await click(wrapper, '삭제')
    expect(adminApi.deleteAdminConcept).toHaveBeenCalledWith(91)
    await arrived(router)
    expect(router.options.history.state.notice).toBe('초안을 삭제했습니다.')
  })

  it('AC15 없는 카드(404)는 안내와 목록 링크를 보인다', async () => {
    adminApi.fetchAdminConcept.mockRejectedValue(
      apiError(404, { code: 'CONCEPT_404', message: '개념 카드를 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountEdit('/admin/concepts/999999/edit')

    expect(wrapper.text()).toContain('카드를 찾을 수 없습니다.')
    expect(wrapper.find('[data-action="back-to-list"]').attributes('href')).toBe('/admin/concepts')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('AC14 목록에서 들어왔으면 저장 후 그 목록 조건으로 돌아간다', async () => {
    adminApi.fetchAdminConcept.mockResolvedValue(detail())
    adminApi.updateAdminConcept.mockResolvedValue(detail({ status: 'PUBLISHED' }))
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/admin/concepts?status=ALL&source=AI')
    // 실제 웹 히스토리처럼 history.state.back을 채운다 (메모리 히스토리는 push의 state를 그대로 보관한다)
    await router.push({
      path: '/admin/concepts/91/edit',
      state: { back: '/admin/concepts?status=ALL&source=AI' },
    })
    const wrapper = mount(AdminConceptEditView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    await click(wrapper, '공개')

    await vi.waitFor(() =>
      expect(router.currentRoute.value.fullPath).toBe('/admin/concepts?status=ALL&source=AI'),
    )
    expect(router.options.history.state.notice).toBe('공개했습니다.')
  })
})
