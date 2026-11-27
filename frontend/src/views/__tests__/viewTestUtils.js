import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'

/**
 * 라우터(메모리 히스토리)와 Pinia를 붙여 화면을 띄운다.
 * `beforeNavigate`는 화면을 띄우기 전에 부른다 (예: 로그인 세션을 넣어 mount 시점에 로그인 상태로 시작).
 * 이 라우터에는 인증 가드가 없으므로 가드 동작은 `authGuard`를 직접 테스트한다.
 */
export async function mountAt(component, path, { beforeNavigate } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  beforeNavigate?.()
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

/** axios가 취소된 요청에 던지는 에러 모양 (V-4) */
export function canceledError() {
  return Object.assign(new Error('canceled'), { name: 'CanceledError', code: 'ERR_CANCELED' })
}

/**
 * api mock 구현: 응답하지 않고 기다리다가 두 번째 인자의 signal이 취소되면 canceledError로 reject한다 (V-4 AC4).
 * `fetchX.mockImplementationOnce(untilAborted)`
 */
export function untilAborted(_query, { signal } = {}) {
  return new Promise((_, reject) => {
    signal?.addEventListener('abort', () => reject(canceledError()))
  })
}
