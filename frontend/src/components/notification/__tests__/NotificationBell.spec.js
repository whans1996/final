import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import * as api from '@/api/notifications'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import appShellSource from '@/layouts/AppShell.vue?raw'
import guestShellSource from '@/layouts/GuestShell.vue?raw'

vi.mock('@/api/notifications', () => ({
  NOTIFICATION_PAGE_SIZE: 20,
  fetchNotifications: vi.fn(),
  fetchUnreadCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

function notification(id, overrides = {}) {
  return {
    id,
    type: 'COMMENT',
    message: `알림 ${id}`,
    link: `/community/${id}`,
    actor: { id: 7, nickname: 'java러버' },
    read: false,
    createdAt: '2026-11-29T14:00:00+09:00',
    ...overrides,
  }
}

function page(items, total = items.length) {
  return { items, page: 1, size: 20, total }
}

let visibility = 'visible'

async function mountBell({ count = 0 } = {}) {
  api.fetchUnreadCount.mockResolvedValue({ count })
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '나', level: 'JUNIOR', role: 'USER' },
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
  await router.push('/dashboard')
  await router.isReady()
  const wrapper = mount(NotificationBell, {
    global: { plugins: [pinia, router] },
    attachTo: document.body,
  })
  await flushPromises()
  return { wrapper, router }
}

const bell = (wrapper) => wrapper.find('[data-testid="notification-bell"]')
const badge = (wrapper) => wrapper.find('[data-testid="notification-badge"]')
const panel = () => document.querySelector('[data-testid="notification-panel"]')
const items = () => [...document.querySelectorAll('[data-testid="notification-item"]')]

async function openPanel(wrapper) {
  await bell(wrapper).trigger('click')
  await flushPromises()
}

describe('NotificationBell (NT-1 AC13~AC16)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    visibility = 'visible'
    vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => visibility)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    document.body.innerHTML = ''
  })

  // ── AC13 ──

  it('AC13 안 읽은 게 없으면 배지 없이 "알림"', async () => {
    const { wrapper } = await mountBell({ count: 0 })

    expect(bell(wrapper).attributes('aria-label')).toBe('알림')
    expect(badge(wrapper).exists()).toBe(false)
    wrapper.unmount()
  })

  it('AC13 1~9는 숫자, 10 이상은 9+ 배지와 안 읽은 수 라벨', async () => {
    const { wrapper } = await mountBell({ count: 3 })
    expect(badge(wrapper).text()).toBe('3')
    expect(bell(wrapper).attributes('aria-label')).toBe('알림, 안 읽음 3개')

    useNotificationsStore().unreadCount = 12
    await flushPromises()
    expect(badge(wrapper).text()).toBe('9+')
    expect(bell(wrapper).attributes('aria-label')).toBe('알림, 안 읽음 12개')
    wrapper.unmount()
  })

  it('AC13 회원 셸의 #notifications 슬롯에만 들어가고 비회원 셸에는 없다', () => {
    expect(appShellSource).toMatch(/<template #notifications>\s*<NotificationBell \/>/)
    expect(guestShellSource).not.toContain('NotificationBell')
  })

  // ── AC14 ──

  it('AC14 진입 때 1회, 이후 60초마다 다시 센다', async () => {
    vi.useFakeTimers()
    const { wrapper } = await mountBell({ count: 1 })
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(1)

    api.fetchUnreadCount.mockResolvedValue({ count: 4 })
    await vi.advanceTimersByTimeAsync(59_000)
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(1_000)
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(2)
    expect(badge(wrapper).text()).toBe('4')
    wrapper.unmount()
  })

  it('AC14 문서가 숨겨져 있으면 건너뛰고 다시 보이면 바로 1회', async () => {
    vi.useFakeTimers()
    const { wrapper } = await mountBell({ count: 1 })

    visibility = 'hidden'
    await vi.advanceTimersByTimeAsync(60_000)
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(1)

    visibility = 'visible'
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('AC14 조회가 실패하면 배지를 그대로 두고 다음 주기에 다시 한다', async () => {
    vi.useFakeTimers()
    const { wrapper } = await mountBell({ count: 2 })

    api.fetchUnreadCount.mockRejectedValueOnce(new Error('network'))
    await vi.advanceTimersByTimeAsync(60_000)
    expect(badge(wrapper).text()).toBe('2')
    expect(wrapper.text()).not.toContain('network')

    api.fetchUnreadCount.mockResolvedValue({ count: 5 })
    await vi.advanceTimersByTimeAsync(60_000)
    expect(badge(wrapper).text()).toBe('5')
    wrapper.unmount()
  })

  it('AC14 로그아웃하면 멈추고 배지가 0이 된다, 셸을 떠나도 멈춘다', async () => {
    vi.useFakeTimers()
    const { wrapper } = await mountBell({ count: 2 })

    useAuthStore().clear()
    await flushPromises()
    expect(badge(wrapper).exists()).toBe(false)
    await vi.advanceTimersByTimeAsync(120_000)
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(1)

    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(120_000)
    expect(api.fetchUnreadCount).toHaveBeenCalledTimes(1)
  })

  // ── AC15 ──

  it('AC15 열 때마다 첫 20개를 새로 부른다', async () => {
    api.fetchNotifications.mockResolvedValue(
      page([notification(2), notification(1, { read: true })]),
    )
    const { wrapper } = await mountBell({ count: 1 })

    await openPanel(wrapper)
    expect(api.fetchNotifications).toHaveBeenCalledWith({ page: 1, size: 20 })
    expect(items()).toHaveLength(2)
    expect(items()[0].textContent).toContain('java러버님이 알림 2')

    panel().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await flushPromises()
    expect(panel()).toBeNull()

    await openPanel(wrapper)
    expect(api.fetchNotifications).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('AC15 로딩 중 스켈레톤, 0개면 안내', async () => {
    let resolve
    api.fetchNotifications.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountBell()

    await openPanel(wrapper)
    expect(document.querySelectorAll('[data-testid="notification-skeleton"]').length).toBe(3)

    resolve(page([]))
    await flushPromises()
    expect(panel().textContent).toContain('새 알림이 없습니다')
    wrapper.unmount()
  })

  it('AC15 실패하면 서버 message와 다시 시도', async () => {
    api.fetchNotifications.mockRejectedValueOnce({
      response: { data: { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' } },
    })
    const { wrapper } = await mountBell()

    await openPanel(wrapper)
    expect(panel().textContent).toContain('서버 오류가 발생했습니다.')

    api.fetchNotifications.mockResolvedValue(page([notification(1)]))
    const retry = [...panel().querySelectorAll('button')].find((b) =>
      b.textContent.includes('다시 시도'),
    )
    retry.click()
    await flushPromises()
    expect(items()).toHaveLength(1)
    wrapper.unmount()
  })

  it('AC15 20개가 넘으면 더 보기로 이어 붙이고 이미 보인 id는 거른다', async () => {
    const first = Array.from({ length: 20 }, (_, i) => notification(30 - i))
    api.fetchNotifications.mockResolvedValueOnce(page(first, 22)).mockResolvedValueOnce({
      items: [notification(11), notification(10), notification(9)],
      page: 2,
      size: 20,
      total: 22,
    })
    const { wrapper } = await mountBell()

    await openPanel(wrapper)
    expect(items()).toHaveLength(20)
    document.querySelector('[data-testid="notification-more"]').click()
    await flushPromises()

    expect(api.fetchNotifications).toHaveBeenLastCalledWith({ page: 2, size: 20 })
    expect(items()).toHaveLength(22)
    expect(document.querySelector('[data-testid="notification-more"]')).toBeNull()
    wrapper.unmount()
  })

  // ── AC16 ──

  it('AC16 안 읽은 항목을 누르면 응답을 기다리지 않고 이동하고 배지·항목을 읽음으로 바꾼다', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(5)]))
    api.markNotificationRead.mockReturnValue(new Promise(() => {}))
    const { wrapper, router } = await mountBell({ count: 1 })

    await openPanel(wrapper)
    items()[0].click()
    await flushPromises()

    expect(api.markNotificationRead).toHaveBeenCalledWith(5)
    expect(router.currentRoute.value.fullPath).toBe('/community/5')
    expect(panel()).toBeNull()
    expect(badge(wrapper).exists()).toBe(false)
    wrapper.unmount()
  })

  it('AC16 읽음 요청이 실패해도 이동한다', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(5)]))
    api.markNotificationRead.mockRejectedValue(new Error('fail'))
    const { wrapper, router } = await mountBell({ count: 1 })

    await openPanel(wrapper)
    items()[0].click()
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/community/5')
    wrapper.unmount()
  })

  it('AC16 읽은 항목을 누르면 요청 없이 바로 이동한다', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(5, { read: true })]))
    const { wrapper, router } = await mountBell()

    await openPanel(wrapper)
    items()[0].click()
    await flushPromises()

    expect(api.markNotificationRead).not.toHaveBeenCalled()
    expect(router.currentRoute.value.fullPath).toBe('/community/5')
    wrapper.unmount()
  })

  it('AC16 모두 읽음: 요청 중 비활성, 성공하면 항목·배지를 모두 읽음으로', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(2), notification(1)]))
    let resolve
    api.markAllNotificationsRead.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountBell({ count: 2 })

    await openPanel(wrapper)
    const button = document.querySelector('[data-testid="mark-all-read"]')
    expect(button.disabled).toBe(false)
    button.click()
    await flushPromises()
    expect(button.disabled).toBe(true)

    resolve()
    await flushPromises()
    expect(items().every((el) => el.dataset.read === 'true')).toBe(true)
    expect(badge(wrapper).exists()).toBe(false)
    expect(button.disabled).toBe(true)
    wrapper.unmount()
  })

  it('AC16 모두 읽음이 실패하면 버튼 옆에 서버 message', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(1)]))
    api.markAllNotificationsRead.mockRejectedValue({
      response: { data: { code: 'AUTH_401', message: '로그인이 필요합니다.' } },
    })
    const { wrapper } = await mountBell({ count: 1 })

    await openPanel(wrapper)
    document.querySelector('[data-testid="mark-all-read"]').click()
    await flushPromises()

    expect(panel().textContent).toContain('로그인이 필요합니다.')
    expect(badge(wrapper).text()).toBe('1')
    wrapper.unmount()
  })

  it('AC16 안 읽은 게 없으면 모두 읽음은 비활성', async () => {
    api.fetchNotifications.mockResolvedValue(page([notification(1, { read: true })]))
    const { wrapper } = await mountBell({ count: 0 })

    await openPanel(wrapper)
    expect(document.querySelector('[data-testid="mark-all-read"]').disabled).toBe(true)
    wrapper.unmount()
  })
})
