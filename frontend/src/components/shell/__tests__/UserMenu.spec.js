import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import UserMenu from '@/components/shell/UserMenu.vue'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({ logout: vi.fn().mockResolvedValue(undefined) }))

function loginAs(role) {
  return () =>
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: 7, nickname: '코딩초보', level: 'JUNIOR', role },
    })
}

const menuItems = () =>
  [...document.querySelectorAll('[role="menuitem"]')].map((el) => el.textContent.trim())

// 닉네임 버튼에서 Enter를 눌러 메뉴를 연 상태로 띄운다 (메뉴는 body 포털에 그려진다)
async function mountOpen(options) {
  const pinia = createPinia()
  setActivePinia(pinia)
  options?.beforeNavigate?.()
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/dashboard')
  await router.isReady()
  const wrapper = mount(UserMenu, { global: { plugins: [pinia, router] }, attachTo: document.body })
  await flushPromises()
  const trigger = wrapper.find('button')
  if (trigger.exists()) {
    await trigger.trigger('keydown', { key: 'Enter' })
    await flushPromises()
  }
  return { wrapper, router }
}

describe('UserMenu (F-29b AC11)', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('AC11 회원은 닉네임 메뉴에서 프로필·설정·로그아웃을 본다 (관리자 없음)', async () => {
    const { wrapper } = await mountOpen({ beforeNavigate: loginAs('USER') })

    expect(wrapper.find('button').text()).toContain('코딩초보')
    // 보이는 닉네임이 접근 가능한 이름에 들어간다 (Label in Name)
    expect(wrapper.find('button').attributes('aria-label')).toBe('코딩초보 메뉴')
    expect(menuItems()).toEqual(['프로필', '설정', '로그아웃'])
    expect(document.querySelector('a[href="/users/7"]').textContent.trim()).toBe('프로필')
    expect(document.querySelector('a[href="/me/settings"]').textContent.trim()).toBe('설정')
    wrapper.unmount()
  })

  it('AC11 ADMIN은 관리자(/admin)가 더해진다', async () => {
    const { wrapper } = await mountOpen({ beforeNavigate: loginAs('ADMIN') })

    expect(menuItems()).toEqual(['프로필', '설정', '관리자', '로그아웃'])
    expect(document.querySelector('a[href="/admin"]')).not.toBeNull()
    wrapper.unmount()
  })

  it('AC11 로그아웃하면 세션을 지우고 home으로 이동한다', async () => {
    const { wrapper, router } = await mountOpen({ beforeNavigate: loginAs('USER') })
    const logout = [...document.querySelectorAll('[role="menuitem"]')].find(
      (el) => el.textContent.trim() === '로그아웃',
    )

    logout.click()
    await flushPromises()

    expect(useAuthStore().isLoggedIn).toBe(false)
    expect(router.currentRoute.value.name).toBe('home')
    wrapper.unmount()
  })

  it('AC11 auth.user가 아직 없으면(복원 중) 닉네임 자리만 둔다', async () => {
    const { wrapper } = await mountOpen()

    expect(wrapper.find('[data-testid="user-menu-placeholder"]').exists()).toBe(true)
    expect(wrapper.find('button').exists()).toBe(false)
    wrapper.unmount()
  })
})
