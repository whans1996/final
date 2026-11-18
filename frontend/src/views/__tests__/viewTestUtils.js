import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'

/** 라우터(메모리 히스토리)와 Pinia를 붙여 화면을 띄운다. */
export async function mountAt(component, path) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  return { wrapper, router }
}

export async function fill(wrapper, values) {
  for (const [name, value] of Object.entries(values)) {
    await wrapper.find(`input[name="${name}"]`).setValue(value)
  }
}

export async function submit(wrapper) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

export function apiError(status, data) {
  return Object.assign(new Error(`status ${status}`), { response: { status, data } })
}
