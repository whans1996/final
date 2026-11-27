import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ThemeToggle from '@/components/shell/ThemeToggle.vue'
import { nextTheme, shownTheme } from '@/components/shell/themeCycle'
import { THEME_KEY, useTheme } from '@/composables/useTheme'

describe('ThemeToggle (F-29b AC10)', () => {
  beforeEach(() => {
    useTheme().setTheme('light')
  })

  it('AC10 누르면 라이트 → 다크 → 라이트로 바뀌고 aria-label이 지금 값을 알린다 (FOLLOW_SYSTEM=false)', async () => {
    const wrapper = mount(ThemeToggle)
    const button = wrapper.find('button')

    expect(button.attributes('aria-label')).toBe('테마: 라이트')
    await button.trigger('click')
    expect(button.attributes('aria-label')).toBe('테마: 다크')
    expect(localStorage.getItem(THEME_KEY)).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    await button.trigger('click')
    expect(button.attributes('aria-label')).toBe('테마: 라이트')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('AC10 저장된 값이 system이면 라이트로 보이고, 누르면 다크가 된다', async () => {
    useTheme().setTheme('system')
    const wrapper = mount(ThemeToggle)

    expect(wrapper.find('button').attributes('aria-label')).toBe('테마: 라이트')
    await wrapper.find('button').trigger('click')
    expect(useTheme().theme.value).toBe('dark')
  })

  it('AC10 FOLLOW_SYSTEM을 켜면 라이트 → 다크 → OS 3단계로 돈다', () => {
    expect(nextTheme('light', true)).toBe('dark')
    expect(nextTheme('dark', true)).toBe('system')
    expect(nextTheme('system', true)).toBe('light')
    expect(shownTheme('system', true)).toBe('system')
    expect(nextTheme('system', false)).toBe('dark')
    expect(nextTheme('dark', false)).toBe('light')
  })
})
