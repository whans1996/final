import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AnalysisRequestView from '@/views/AnalysisRequestView.vue'
import * as analysesApi from '@/api/analyses'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/analyses')

const MB = 1024 * 1024

function login(level = 'JUNIOR') {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level, role: 'USER' },
  })
}

function zipFile(name = 'todo-app.zip', size) {
  const file = new File(['PK'], name, { type: 'application/zip' })
  if (size !== undefined) Object.defineProperty(file, 'size', { value: size })
  return file
}

async function chooseFile(wrapper, file) {
  const input = wrapper.find('input[name="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
}

async function mountRequest(level) {
  const mounted = await mountAt(AnalysisRequestView, '/analyses/new', {
    beforeNavigate: () => login(level),
  })
  await flushPromises()
  return mounted
}

const submitButton = (wrapper) => wrapper.find('button[type="submit"]')

async function submitForm(wrapper) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('AnalysisRequestView (F-05a AC14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC14 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path === '/analyses/new')
    const to = { meta: route.meta, fullPath: '/analyses/new' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/analyses/new' },
    })
  })

  it('AC14 수준 라디오는 내 수준으로 선택되어 있고 고지·제한 안내를 보여 준다 (F-27 AC12 GMS 고지)', async () => {
    const { wrapper } = await mountRequest('SENIOR')

    expect(wrapper.find('input[name="level"][value="SENIOR"]').element.checked).toBe(true)
    expect(wrapper.text()).toContain('SSAFY GMS(교육용 AI 게이트웨이)를 거쳐 외부 AI 모델로 전송')
    expect(wrapper.text()).toContain('서버의 로컬 AI로 간이 분석')
    expect(wrapper.text()).not.toContain('Gemini')
    expect(wrapper.text()).toContain('하루 5회')
    // F-26a부터 코드 파일 수로 실패하지 않는다 (#155)
    expect(wrapper.text()).toContain(
      'ZIP 10MB 이하 · 큰 프로젝트는 중요한 파일 30개를 골라 설명합니다',
    )
    expect(wrapper.text()).not.toContain('코드 파일 200개')
  })

  it('AC14 파일과 동의가 모두 있어야 분석 요청 버튼이 활성화된다', async () => {
    const { wrapper } = await mountRequest()

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await chooseFile(wrapper, zipFile())
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="consent"]').setValue(true)
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
    await wrapper.find('input[name="consent"]').setValue(false)
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('AC14 10MB 넘는 파일을 고르면 업로드 전에 필드 아래 안내를 보여 주고 버튼을 막는다', async () => {
    const { wrapper } = await mountRequest()

    await chooseFile(wrapper, zipFile('big.zip', 10 * MB + 1))
    await wrapper.find('input[name="consent"]').setValue(true)

    expect(wrapper.find('[data-error="file"]').text()).toContain('10MB')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await submitForm(wrapper)
    expect(analysesApi.createAnalysis).not.toHaveBeenCalled()

    await chooseFile(wrapper, zipFile('ok.zip', 10 * MB))
    expect(wrapper.find('[data-error="file"]').exists()).toBe(false)
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('AC14 202면 고른 수준으로 요청하고 /analyses/{analysisId}로 이동한다', async () => {
    const { wrapper, router } = await mountRequest()
    analysesApi.createAnalysis.mockResolvedValue({ analysisId: 42, status: 'FETCHING' })
    const file = zipFile()

    await chooseFile(wrapper, file)
    await wrapper.find('input[name="level"][value="BEGINNER"]').setValue(true)
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(analysesApi.createAnalysis).toHaveBeenCalledWith({
      file,
      level: 'BEGINNER',
      consent: true,
    })
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/analyses/42'))
  })

  it('AC14 요청 중에는 버튼이 비활성화된다', async () => {
    const { wrapper } = await mountRequest()
    let resolve
    analysesApi.createAnalysis.mockReturnValue(new Promise((r) => (resolve = r)))

    await chooseFile(wrapper, zipFile())
    await wrapper.find('input[name="consent"]').setValue(true)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    resolve({ analysisId: 1, status: 'FETCHING' })
    await flushPromises()
  })

  it('AC14 429면 서버 message를 보여 주고 화면에 머문다', async () => {
    const { wrapper, router } = await mountRequest()
    const message = '오늘 분석 요청 횟수(5회)를 모두 사용했습니다. 내일 다시 시도해 주세요.'
    analysesApi.createAnalysis.mockRejectedValue(
      apiError(429, { code: 'ANALYSIS_429', message, details: null }),
    )

    await chooseFile(wrapper, zipFile())
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toBe(message)
    expect(router.currentRoute.value.path).toBe('/analyses/new')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('AC14 413이면 서버 message를 보여 준다', async () => {
    const { wrapper } = await mountRequest()
    const message = '업로드할 수 있는 파일 크기를 넘었습니다.'
    analysesApi.createAnalysis.mockRejectedValue(
      apiError(413, { code: 'COMMON_413', message, details: null }),
    )

    await chooseFile(wrapper, zipFile())
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toBe(message)
  })

  it('AC14 400이면 message와 함께 details를 각 필드 아래에 보여 준다', async () => {
    const { wrapper } = await mountRequest()
    analysesApi.createAnalysis.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { file: 'ZIP 파일만 올릴 수 있습니다.', level: '허용되지 않은 수준입니다.' },
      }),
    )

    await chooseFile(wrapper, zipFile('fake.zip'))
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toBe('입력값이 올바르지 않습니다.')
    expect(wrapper.find('[data-error="file"]').text()).toBe('ZIP 파일만 올릴 수 있습니다.')
    expect(wrapper.find('[data-error="level"]').text()).toBe('허용되지 않은 수준입니다.')
  })
})

describe('AnalysisRequestView 저장소 주소 (F-05b AC13)', () => {
  const REPO = 'https://github.com/octo/todo-app'

  beforeEach(() => {
    vi.resetAllMocks()
  })

  async function chooseSource(wrapper, value) {
    await wrapper.find(`input[name="source"][value="${value}"]`).setValue(true)
  }

  it('AC13 입력 방식은 ZIP 업로드가 기본이고, 저장소 주소를 고르면 주소 칸과 안내를 보여 준다', async () => {
    const { wrapper } = await mountRequest()

    expect(wrapper.find('input[name="source"][value="ZIP"]').element.checked).toBe(true)
    expect(wrapper.find('input[name="sourceUrl"]').exists()).toBe(false)
    expect(wrapper.find('input[name="file"]').exists()).toBe(true)

    await chooseSource(wrapper, 'URL')

    const input = wrapper.find('input[name="sourceUrl"]')
    expect(input.exists()).toBe(true)
    expect(input.attributes('placeholder')).toBe('https://github.com/owner/repo')
    expect(wrapper.text()).toContain(
      '공개 GitHub·GitLab(gitlab.com) 저장소의 기본 브랜치를 분석합니다. 저장소 주소만 입력하세요.',
    )
    expect(wrapper.text()).toContain(
      '저장소 아카이브 30MB 이하 · 큰 저장소는 중요한 파일 30개를 골라 설명합니다',
    )
    expect(wrapper.text()).not.toContain('코드 파일 200개')
    expect(wrapper.find('input[name="file"]').exists()).toBe(false)
  })

  it('AC13 주소(공백 제외)와 동의가 모두 있어야 버튼이 활성화된다', async () => {
    const { wrapper } = await mountRequest()
    await chooseSource(wrapper, 'URL')
    await wrapper.find('input[name="consent"]').setValue(true)

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="sourceUrl"]').setValue('   ')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="sourceUrl"]').setValue(REPO)
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
    await wrapper.find('input[name="consent"]').setValue(false)
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('AC13 202면 sourceUrl만 보내고 /analyses/{analysisId}로 이동한다', async () => {
    const { wrapper, router } = await mountRequest()
    analysesApi.createAnalysis.mockResolvedValue({ analysisId: 57, status: 'FETCHING' })

    await chooseSource(wrapper, 'URL')
    await wrapper.find('input[name="sourceUrl"]').setValue(`  ${REPO}  `)
    await wrapper.find('input[name="level"][value="SENIOR"]').setValue(true)
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(analysesApi.createAnalysis).toHaveBeenCalledWith({
      sourceUrl: REPO,
      level: 'SENIOR',
      consent: true,
    })
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/analyses/57'))
  })

  it('AC13 방식을 바꾸면 다른 방식의 입력값을 비운다', async () => {
    const { wrapper } = await mountRequest()
    await wrapper.find('input[name="consent"]').setValue(true)

    await chooseFile(wrapper, zipFile())
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
    await chooseSource(wrapper, 'URL')
    await wrapper.find('input[name="sourceUrl"]').setValue(REPO)
    await chooseSource(wrapper, 'ZIP')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await chooseSource(wrapper, 'URL')
    expect(wrapper.find('input[name="sourceUrl"]').element.value).toBe('')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('AC13 400의 details.sourceUrl을 주소 칸 아래에 보여 준다', async () => {
    const { wrapper } = await mountRequest()
    const message =
      '저장소 주소만 입력하세요. 브랜치·파일 경로(/tree/…, /blob/…)는 지원하지 않습니다.'
    analysesApi.createAnalysis.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { sourceUrl: message },
      }),
    )

    await chooseSource(wrapper, 'URL')
    await wrapper.find('input[name="sourceUrl"]').setValue(`${REPO}/tree/dev`)
    await wrapper.find('input[name="consent"]').setValue(true)
    await submitForm(wrapper)

    expect(wrapper.find('[data-error="sourceUrl"]').text()).toBe(message)
    expect(wrapper.find('input[name="sourceUrl"]').attributes('aria-invalid')).toBe('true')
  })
})
