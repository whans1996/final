import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import PostWriteView from '@/views/community/PostWriteView.vue'
import * as postsApi from '@/api/posts'
import * as analysesApi from '@/api/analyses'
import * as conceptsApi from '@/api/concepts'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/posts')
vi.mock('@/api/analyses')
vi.mock('@/api/concepts')

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 7, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })

function detail(partial) {
  return {
    id: 42,
    boardType: 'REVIEW',
    title: '원래 제목',
    content: '원래 본문',
    authorId: 7,
    authorNickname: '코딩초보',
    viewCount: 3,
    commentCount: 0,
    analysis: null,
    concept: {
      id: 12,
      viewable: true,
      name: 'JWT',
      category: 'security',
      difficulty: 'JUNIOR',
      summary: '요약',
    },
    createdAt: '2026-11-20T14:02:11+09:00',
    updatedAt: '2026-11-20T14:02:11+09:00',
    ...partial,
  }
}

async function mountWrite(path = '/community/write') {
  const mounted = await mountAt(PostWriteView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

async function fillForm(wrapper, { title = '제목', content = '본문' } = {}) {
  await wrapper.find('input[name="title"]').setValue(title)
  await wrapper.find('textarea[name="content"]').setValue(content)
}

const submitButton = (wrapper) => wrapper.find('button[type="submit"]')
const submitForm = async (wrapper) => {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('PostWriteView (F-17 AC17·AC18)', () => {
  let confirm

  beforeEach(() => {
    vi.resetAllMocks()
    confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC17 게시판(기본 Q&A)·제목·본문을 입력해 등록하면 createPost 후 그 글로 간다', async () => {
    postsApi.createPost.mockResolvedValue({ id: 42 })
    const { wrapper, router } = await mountWrite()

    expect(wrapper.find('input[name="boardType"][value="QNA"]').element.checked).toBe(true)
    await wrapper.find('input[name="boardType"][value="FREE"]').setValue(true)
    await fillForm(wrapper, { title: '스터디 하실 분', content: '같이 해요' })
    await submitForm(wrapper)

    expect(postsApi.createPost).toHaveBeenCalledWith({
      boardType: 'FREE',
      title: '스터디 하실 분',
      content: '같이 해요',
      analysisId: null,
      conceptId: null,
    })
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community/42'), {
      timeout: 5000,
    })
    expect(confirm).not.toHaveBeenCalled()
  })

  it('AC17 제목·본문이 공백뿐이면 등록이 비활성이고 길이 제한과 본문 글자 수를 보여 준다', async () => {
    const { wrapper } = await mountWrite()

    expect(wrapper.find('input[name="title"]').attributes('maxlength')).toBe('100')
    expect(wrapper.find('textarea[name="content"]').attributes('maxlength')).toBe('5000')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await fillForm(wrapper, { title: '   ', content: '본문' })
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await fillForm(wrapper, { title: '제목', content: ' \n ' })
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    await fillForm(wrapper, { title: '제목', content: '본문입니다' })
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-role="content-count"]').text()).toBe('5/5000')
  })

  it('AC17 고른 첨부를 보내고 저장 중에는 버튼이 비활성이다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [{ id: 15, title: '쇼핑몰 과제', projectName: 'shop', status: 'DONE' }],
      page: 1,
      size: 100,
      total: 1,
    })
    let resolve
    postsApi.createPost.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper, router } = await mountWrite()

    await wrapper.find('input[name="attachmentMode"][value="analysis"]').setValue(true)
    await flushPromises()
    await wrapper.find('select[name="analysisId"]').setValue('15')
    await fillForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    expect(postsApi.createPost.mock.calls[0][0]).toMatchObject({ analysisId: 15, conceptId: null })
    resolve({ id: 43 })
    await flushPromises()
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community/43'), {
      timeout: 5000,
    })
  })

  it('AC17 400이면 details를 필드 아래에, 첨부 오류는 첨부 아래에, 그 밖은 서버 message를 보여 준다', async () => {
    const { wrapper } = await mountWrite()
    await fillForm(wrapper)

    postsApi.createPost.mockRejectedValueOnce(
      apiError(400, {
        code: 'COMMON_400',
        message: '요청 값이 올바르지 않습니다.',
        details: {
          title: '제목은 100자 이하로 입력해 주세요.',
          conceptId: '하나만 첨부할 수 있습니다.',
        },
      }),
    )
    await submitForm(wrapper)
    expect(wrapper.find('[data-error="title"]').text()).toBe('제목은 100자 이하로 입력해 주세요.')
    expect(wrapper.find('[data-error="attachment"]').text()).toBe('하나만 첨부할 수 있습니다.')

    postsApi.createPost.mockRejectedValueOnce(
      apiError(400, { code: 'POST_002', message: '첨부할 수 없는 개념 카드입니다.' }),
    )
    await submitForm(wrapper)
    expect(wrapper.find('[data-error="title"]').exists()).toBe(false)
    expect(wrapper.find('[data-error="attachment"]').text()).toBe('첨부할 수 없는 개념 카드입니다.')

    postsApi.createPost.mockRejectedValueOnce(apiError(500, { message: '서버 오류입니다.' }))
    await submitForm(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류입니다.')
  })

  it('AC17 입력이 있는 채로 떠나려 하면 확인을 띄우고, 입력이 없으면 띄우지 않는다', async () => {
    const empty = await mountWrite()
    await empty.router.push('/community')
    expect(confirm).not.toHaveBeenCalled()

    const { wrapper, router } = await mountWrite()
    await fillForm(wrapper, { title: '쓰던 글', content: '' })
    confirm.mockReturnValue(false)
    await router.push('/community')
    expect(confirm).toHaveBeenCalledWith('작성 중인 내용이 사라집니다. 이 화면을 나갈까요?')
    expect(router.currentRoute.value.path).toBe('/community/write')

    confirm.mockReturnValue(true)
    await router.push('/community')
    expect(router.currentRoute.value.path).toBe('/community')
  })

  it('AC18 ?conceptId=·?analysisId=로 들어오면 첨부를 미리 채운다', async () => {
    conceptsApi.fetchConcept.mockResolvedValue({ id: 12, name: 'JWT', category: 'security' })
    postsApi.createPost.mockResolvedValue({ id: 50 })
    const { wrapper } = await mountWrite('/community/write?conceptId=12')

    expect(conceptsApi.fetchConcept).toHaveBeenCalledWith(12)
    expect(wrapper.find('[data-role="attachment-chip"]').text()).toContain('JWT')
    await fillForm(wrapper)
    await submitForm(wrapper)
    expect(postsApi.createPost.mock.calls[0][0]).toMatchObject({ analysisId: null, conceptId: 12 })

    analysesApi.fetchAnalysis.mockResolvedValue({
      id: 15,
      title: '쇼핑몰 과제',
      projectName: 'shop',
      status: 'DONE',
      mine: true,
    })
    const withAnalysis = await mountWrite('/community/write?analysisId=15')
    expect(analysesApi.fetchAnalysis).toHaveBeenCalledWith(15)
    expect(withAnalysis.wrapper.find('[data-role="attachment-chip"]').text()).toContain(
      '쇼핑몰 과제',
    )
  })

  it('AC18 #207 ?analysisId=가 남의 분석이면 공개여도 첨부하지 않고 안내한다', async () => {
    analysesApi.fetchAnalysis.mockResolvedValue({
      id: 15,
      title: '남의 과제',
      projectName: 'shop',
      status: 'DONE',
      isPublic: true,
      mine: false,
    })
    postsApi.createPost.mockResolvedValue({ id: 50 })
    const { wrapper } = await mountWrite('/community/write?analysisId=15')

    expect(wrapper.text()).toContain('첨부할 수 없는 항목이라 빼고 열었습니다')
    expect(wrapper.find('[data-role="attachment-chip"]').exists()).toBe(false)
    await fillForm(wrapper)
    await submitForm(wrapper)
    expect(postsApi.createPost.mock.calls[0][0]).toMatchObject({ analysisId: null })
  })

  it('AC17·AC18 미리 채울 첨부를 불러오는 동안은 폼을 열지 않아 입력이 기준 상태에 묻히지 않는다', async () => {
    let resolve
    conceptsApi.fetchConcept.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountWrite('/community/write?conceptId=12')

    expect(wrapper.text()).toContain('불러오는 중')
    expect(wrapper.find('form').exists()).toBe(false)

    resolve({ id: 12, name: 'JWT', category: 'security' })
    await flushPromises()
    expect(wrapper.find('[data-role="attachment-chip"]').text()).toContain('JWT')
    await fillForm(wrapper)
    expect(submitButton(wrapper).element.disabled).toBe(false)
  })

  it('AC18 미리 채울 첨부를 불러오지 못하면 첨부 없이 열고 안내한다', async () => {
    conceptsApi.fetchConcept.mockRejectedValue(apiError(404, { code: 'CONCEPT_404' }))
    const { wrapper, router } = await mountWrite('/community/write?conceptId=99')

    expect(wrapper.text()).toContain('첨부할 수 없는 항목이라 빼고 열었습니다')
    expect(wrapper.find('[data-role="attachment-chip"]').exists()).toBe(false)

    analysesApi.fetchAnalysis.mockResolvedValue({ id: 16, title: '진행 중', status: 'EXPLAINING' })
    const running = await mountWrite('/community/write?analysisId=16')
    expect(running.wrapper.text()).toContain('첨부할 수 없는 항목이라 빼고 열었습니다')

    // 미리 채우기만 한 상태는 입력으로 보지 않는다
    await router.push('/community')
    expect(confirm).not.toHaveBeenCalled()
  })

  it('AC18 자기 글의 edit은 글 값으로 폼을 채우고 "저장"하면 updatePost 후 그 글로 간다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail())
    postsApi.updatePost.mockResolvedValue(detail({ title: '바뀐 제목' }))
    const { wrapper, router } = await mountWrite('/community/42/edit')

    expect(postsApi.fetchPost).toHaveBeenCalledWith(42)
    expect(wrapper.find('input[name="boardType"][value="REVIEW"]').element.checked).toBe(true)
    expect(wrapper.find('input[name="title"]').element.value).toBe('원래 제목')
    expect(wrapper.find('textarea[name="content"]').element.value).toBe('원래 본문')
    expect(wrapper.find('[data-role="attachment-chip"]').text()).toContain('JWT')
    expect(submitButton(wrapper).text()).toBe('저장')

    await wrapper.find('input[name="title"]').setValue('바뀐 제목')
    await submitForm(wrapper)

    expect(postsApi.updatePost).toHaveBeenCalledWith(42, {
      boardType: 'REVIEW',
      title: '바뀐 제목',
      content: '원래 본문',
      analysisId: null,
      conceptId: 12,
    })
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community/42'), {
      timeout: 5000,
    })
    expect(confirm).not.toHaveBeenCalled()
  })

  it('AC18 자기 글의 edit에서 볼 수 없게 된 첨부(숨긴 카드·삭제한 분석)는 빼고 열고 안내한다', async () => {
    postsApi.fetchPost.mockResolvedValue(
      detail({
        concept: {
          id: 12,
          viewable: false,
          name: null,
          category: null,
          difficulty: null,
          summary: null,
        },
      }),
    )
    postsApi.updatePost.mockResolvedValue(detail({ concept: null }))
    const { wrapper } = await mountWrite('/community/42/edit')

    expect(wrapper.text()).toContain('첨부할 수 없는 항목이라 빼고 열었습니다')
    expect(wrapper.find('[data-role="attachment-chip"]').exists()).toBe(false)
    await submitForm(wrapper)
    expect(postsApi.updatePost).toHaveBeenCalledWith(42, {
      boardType: 'REVIEW',
      title: '원래 제목',
      content: '원래 본문',
      analysisId: null,
      conceptId: null,
    })

    postsApi.fetchPost.mockResolvedValue(
      detail({
        concept: null,
        analysis: { id: 15, viewable: false, title: null, projectName: null },
      }),
    )
    const deleted = await mountWrite('/community/42/edit')
    expect(deleted.wrapper.text()).toContain('첨부할 수 없는 항목이라 빼고 열었습니다')
    expect(deleted.wrapper.find('[data-role="attachment-chip"]').exists()).toBe(false)
  })

  it('AC18 남의 글의 edit은 안내를 보여 주고 그 글로 보낸다', async () => {
    postsApi.fetchPost.mockResolvedValue(detail({ authorId: 8 }))
    const { wrapper, router } = await mountWrite('/community/42/edit')

    expect(wrapper.text()).toContain('본인이 쓴 글만 수정할 수 있습니다.')
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/community/42'), {
      timeout: 5000,
    })
    expect(postsApi.updatePost).not.toHaveBeenCalled()
  })
})
