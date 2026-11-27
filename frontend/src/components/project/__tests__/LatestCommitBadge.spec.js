import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import LatestCommitBadge from '@/components/project/LatestCommitBadge.vue'
import * as projectsApi from '@/api/projects'
import { apiError, deferred } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/projects')

const LATEST = {
  sha: 'a1b2c3d4e5f60718293a4b5c6d7e8f9012345678',
  message: 'feat: 비밀번호 재설정',
  committedAt: '2026-11-29T09:12:40+09:00',
}

function result(partial) {
  return {
    status: 'NEW_COMMIT',
    latest: LATEST,
    analyzedSha: '9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182',
    checkedAt: '2026-11-29T10:00:00+09:00',
    ...partial,
  }
}

async function mountBadge(projectId = 12) {
  const wrapper = mount(LatestCommitBadge, { props: { projectId } })
  await flushPromises()
  return wrapper
}

describe('LatestCommitBadge (P-1a AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-29T12:12:40+09:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC15 조회 중에는 확인 중 문구를 보여 준다', async () => {
    const pending = deferred()
    projectsApi.fetchLatestCommit.mockReturnValue(pending.promise)

    const wrapper = mount(LatestCommitBadge, { props: { projectId: 12 } })
    await flushPromises()

    expect(wrapper.find('[data-commit="checking"]').text()).toContain('최신 커밋 확인 중')
    pending.resolve(result())
    await flushPromises()
    expect(wrapper.find('[data-commit="checking"]').exists()).toBe(false)
  })

  it('AC15 NEW_COMMIT이면 "새 커밋이 있어요 · 메시지 · 상대 시각"을 보여 준다', async () => {
    projectsApi.fetchLatestCommit.mockResolvedValue(result())

    const wrapper = await mountBadge()

    expect(projectsApi.fetchLatestCommit).toHaveBeenCalledWith(12, expect.anything())
    const badge = wrapper.find('[data-commit="NEW_COMMIT"]')
    expect(badge.text()).toContain('새 커밋이 있어요')
    expect(badge.text()).toContain('feat: 비밀번호 재설정')
    expect(badge.text()).toContain('3시간 전')
  })

  it('AC15 UP_TO_DATE면 "최신 커밋을 분석했어요"를 보여 준다', async () => {
    projectsApi.fetchLatestCommit.mockResolvedValue(
      result({ status: 'UP_TO_DATE', analyzedSha: LATEST.sha }),
    )

    const wrapper = await mountBadge()

    expect(wrapper.find('[data-commit="UP_TO_DATE"]').text()).toContain('최신 커밋을 분석했어요')
  })

  it('AC15 UNKNOWN이고 최신 커밋 정보가 있으면 그 커밋만 보여 준다', async () => {
    projectsApi.fetchLatestCommit.mockResolvedValue(
      result({ status: 'UNKNOWN', analyzedSha: null }),
    )

    const wrapper = await mountBadge()

    const badge = wrapper.find('[data-commit="UNKNOWN"]')
    expect(badge.text()).toContain('feat: 비밀번호 재설정')
    expect(badge.text()).not.toContain('새 커밋')
    expect(badge.text()).not.toContain('최신 커밋을 분석했어요')
  })

  it('AC15 UNKNOWN이고 최신 커밋 정보가 없거나 NOT_SUPPORTED면 아무것도 그리지 않는다', async () => {
    projectsApi.fetchLatestCommit.mockResolvedValueOnce(
      result({ status: 'UNKNOWN', latest: null, analyzedSha: null }),
    )
    const unknown = await mountBadge()
    expect(unknown.find('[data-commit]').exists()).toBe(false)

    projectsApi.fetchLatestCommit.mockResolvedValueOnce(
      result({ status: 'NOT_SUPPORTED', latest: null, analyzedSha: null, checkedAt: null }),
    )
    const zip = await mountBadge()
    expect(zip.find('[data-commit]').exists()).toBe(false)
  })

  it('AC15 조회가 실패해도 에러를 크게 띄우지 않는다 (상세를 가리지 않는다)', async () => {
    projectsApi.fetchLatestCommit.mockRejectedValue(apiError(500, { message: '서버 오류' }))

    const wrapper = await mountBadge()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('[data-commit]').exists()).toBe(false)
  })

  it('AC15 projectId가 바뀌면 새 프로젝트로 다시 조회하고 이전 응답은 버린다', async () => {
    const first = deferred()
    projectsApi.fetchLatestCommit
      .mockReturnValueOnce(first.promise)
      .mockResolvedValueOnce(result({ status: 'UP_TO_DATE' }))

    const wrapper = mount(LatestCommitBadge, { props: { projectId: 12 } })
    await wrapper.setProps({ projectId: 13 })
    await flushPromises()
    first.resolve(result())
    await flushPromises()

    expect(projectsApi.fetchLatestCommit).toHaveBeenLastCalledWith(13, expect.anything())
    expect(wrapper.find('[data-commit="UP_TO_DATE"]').exists()).toBe(true)
    expect(wrapper.find('[data-commit="NEW_COMMIT"]').exists()).toBe(false)
  })
})
