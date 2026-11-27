import { describe, expect, it } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import BaseButton from '@/components/common/BaseButton.vue'

const mountButton = (props = {}, slot = '저장') =>
  mount(BaseButton, {
    props,
    slots: { default: slot },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })

// F-29a AC7: 공통 버튼
describe('BaseButton (F-29a AC7)', () => {
  it('AC7 기본은 type=button, primary·md이고 클릭하면 click을 내보낸다', async () => {
    const wrapper = mountButton()
    const button = wrapper.find('button')
    expect(button.attributes('type')).toBe('button')
    expect(button.attributes('data-variant')).toBe('primary')
    expect(button.attributes('data-size')).toBe('md')
    expect(button.classes()).toContain('bg-primary')
    await button.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it.each([
    ['primary', 'bg-primary'],
    ['secondary', 'border-border'],
    ['ghost', 'bg-transparent'],
    ['danger', 'bg-danger'],
  ])('AC7 variant %s이면 %s 클래스가 붙는다', (variant, cls) => {
    const button = mountButton({ variant }).find('button')
    expect(button.attributes('data-variant')).toBe(variant)
    expect(button.classes()).toContain(cls)
  })

  it('AC7 size sm은 작은 높이 클래스를 쓴다', () => {
    const button = mountButton({ size: 'sm' }).find('button')
    expect(button.attributes('data-size')).toBe('sm')
    expect(button.classes()).toContain('h-8')
  })

  it('AC7 type=submit을 그대로 넘긴다', () => {
    expect(mountButton({ type: 'submit' }).find('button').attributes('type')).toBe('submit')
  })

  it('AC7 disabled면 disabled가 붙고 click을 내보내지 않는다', async () => {
    const wrapper = mountButton({ disabled: true })
    const button = wrapper.find('button')
    expect(button.attributes('disabled')).toBeDefined()
    await button.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('AC7 loading이면 aria-busy가 붙고 click을 내보내지 않는다', async () => {
    const wrapper = mountButton({ loading: true })
    const button = wrapper.find('button')
    expect(button.attributes('aria-busy')).toBe('true')
    expect(button.attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-spinner]').exists()).toBe(true)
    expect(wrapper.text()).toContain('저장')
    await button.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('AC7 to를 주면 RouterLink로 그린다', () => {
    const wrapper = mountButton({ to: '/concepts', variant: 'secondary' }, '카드 보기')
    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.exists()).toBe(true)
    expect(link.props('to')).toBe('/concepts')
    expect(link.classes()).toContain('border-border')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it.each([[{ disabled: true }], [{ loading: true }]])(
    'AC7 to가 있어도 %o이면 링크 대신 비활성 버튼으로 그려 이동하지 않는다',
    async (state) => {
      const wrapper = mountButton({ to: '/concepts', ...state }, '카드 보기')
      expect(wrapper.findComponent(RouterLinkStub).exists()).toBe(false)
      const button = wrapper.find('button')
      expect(button.attributes('disabled')).toBeDefined()
      await button.trigger('click')
      expect(wrapper.emitted('click')).toBeUndefined()
    },
  )
})
