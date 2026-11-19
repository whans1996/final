import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AdminHomeView from '@/views/admin/AdminHomeView.vue'

const Stub = { template: '<p>stub</p>' }

async function mountAdmin(children) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: Stub },
      { path: '/admin', component: AdminLayout, children },
    ],
  })
  await router.push('/admin')
  await router.isReady()
  const wrapper = mount({ template: '<RouterView />' }, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

// F-00b AC9: 관리자 레이아웃은 /admin 자식 route의 meta.adminNav로 사이드 메뉴를 만든다
describe('AdminLayout (F-00b AC9)', () => {
  it('AC9 관리자 홈은 사이드 메뉴와 안내 문구를 보인다', async () => {
    const wrapper = await mountAdmin([
      { path: '', name: 'admin-home', component: AdminHomeView },
      {
        path: 'concepts',
        name: 'admin-concepts',
        component: Stub,
        meta: { adminNav: { label: '카드 관리', order: 10 } },
      },
    ])

    const link = wrapper.find('nav[aria-label="관리 메뉴"] a')
    expect(link.text()).toBe('카드 관리')
    expect(link.attributes('href')).toBe('/admin/concepts')
    expect(wrapper.text()).toContain('관리할 항목을 고르세요')
  })

  it('AC9 관리 메뉴가 붙은 자식 route가 없으면 안내를 보인다', async () => {
    const wrapper = await mountAdmin([{ path: '', name: 'admin-home', component: AdminHomeView }])

    expect(wrapper.find('nav[aria-label="관리 메뉴"]').text()).toBe('아직 관리 메뉴가 없습니다.')
    expect(wrapper.text()).toContain('관리할 항목을 고르세요')
  })
})
