import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import HomeView from '@/views/HomeView.vue'

describe('HomeView', () => {
  it('서비스 이름을 보여 준다', () => {
    const wrapper = mount(HomeView)
    expect(wrapper.find('h1').text()).toBe('바이브노트')
  })
})
