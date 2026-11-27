import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppBottomTabs from '@/components/shell/AppBottomTabs.vue'
import { withoutTabs } from '@/components/shell/bottomTabs'

const Blank = { template: '<div />' }
const paths = ['/dashboard', '/analyses', '/analyses/:id', '/review-plans', '/concepts']

async function mountTabs(path) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: paths.map((p) => ({ path: p, component: Blank })),
  })
  router.push(path)
  await router.isReady()
  const wrapper = mount(AppBottomTabs, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('AppBottomTabs (F-29b AC5)', () => {
  it('AC5 하단 탭 5칸: 대시보드·분석(내 분석)·복습·카드 링크와 더보기 버튼', async () => {
    const wrapper = await mountTabs('/dashboard')

    expect(wrapper.find('nav').attributes('aria-label')).toBe('하단 탭')
    expect(wrapper.findAll('a').map((a) => [a.text(), a.attributes('href')])).toEqual([
      ['대시보드', '/dashboard'],
      ['분석', '/analyses'],
      ['복습', '/review-plans'],
      ['카드', '/concepts'],
    ])
    expect(wrapper.find('button').text()).toBe('더보기')
  })

  it('AC5 하위 경로도 해당 탭이 활성이고, 더보기를 누르면 more를 내보낸다', async () => {
    const wrapper = await mountTabs('/analyses/12')

    expect(wrapper.find('a[aria-current="page"]').text()).toBe('분석')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('more')).toHaveLength(1)
  })

  it('AC5 드로어 메뉴는 하단 탭 경로를 뺀 나머지이고 빈 그룹은 빠진다', () => {
    const groups = [
      { key: 'dashboard', label: '대시보드', items: [{ label: '대시보드', to: '/dashboard' }] },
      {
        key: 'learn',
        label: '학습',
        items: [
          { label: '개념 카드', to: '/concepts' },
          { label: '분석 요청', to: '/analyses/new' },
          { label: '내 분석', to: '/analyses' },
        ],
      },
    ]

    expect(withoutTabs(groups)).toEqual([
      { key: 'learn', label: '학습', items: [{ label: '분석 요청', to: '/analyses/new' }] },
    ])
  })
})
