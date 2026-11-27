import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

// 연 버튼과 대화상자를 함께 그리는 화면을 흉내 낸다
function mountHost(props = {}) {
  const events = []
  const open = ref(false)
  const loading = ref(props.loading ?? false)
  const Host = defineComponent({
    setup: () => () =>
      h('div', [
        h('button', { id: 'opener', onClick: () => (open.value = true) }, '삭제'),
        h(ConfirmDialog, {
          open: open.value,
          'onUpdate:open': (v) => (open.value = v),
          title: '카드를 삭제할까요?',
          message: '삭제하면 되돌릴 수 없습니다.',
          confirmLabel: '삭제',
          danger: true,
          ...props,
          loading: loading.value,
          onConfirm: () => events.push('confirm'),
          onCancel: () => events.push('cancel'),
        }),
      ]),
  })
  const wrapper = mount(Host, { attachTo: document.body })
  return { wrapper, open, loading, events }
}

const dialog = () => document.querySelector('[role="dialog"]')
const button = (label) =>
  [...dialog().querySelectorAll('button')].find((b) => b.textContent.trim() === label)

async function openWith() {
  const opener = document.getElementById('opener')
  opener.focus()
  opener.click()
  await flushPromises()
  await nextTick()
}

async function pressEscape() {
  document.activeElement.dispatchEvent(
    new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }),
  )
  await flushPromises()
}

// 대화상자 바깥(연 버튼)을 누른다. Reka DismissableLayer는 열린 뒤 다음 틱부터 document의 pointerdown을 본다
async function pressOutside() {
  await new Promise((resolve) => setTimeout(resolve, 0))
  const target = document.getElementById('opener')
  target.dispatchEvent(new Event('pointerdown', { bubbles: true, cancelable: true }))
  target.dispatchEvent(new Event('pointerup', { bubbles: true, cancelable: true }))
  target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  await flushPromises()
}

let current
afterEach(() => {
  current?.wrapper.unmount()
  document.body.innerHTML = ''
})

// F-29a AC12: Reka UI Dialog를 감싼 확인 대화상자
describe('ConfirmDialog (F-29a AC12)', () => {
  it('AC12 열리면 제목·문구·버튼을 보이고 포커스가 대화상자 안에 있다', async () => {
    current = mountHost()
    await openWith()
    expect(dialog()).not.toBeNull()
    expect(dialog().textContent).toContain('카드를 삭제할까요?')
    expect(dialog().textContent).toContain('삭제하면 되돌릴 수 없습니다.')
    expect(dialog().getAttribute('aria-labelledby')).toBeTruthy()
    expect(dialog().contains(document.activeElement)).toBe(true)
    expect(button('삭제').className).toContain('bg-danger')
  })

  it('AC12 확인을 누르면 confirm을 내보낸다', async () => {
    current = mountHost()
    await openWith()
    button('삭제').click()
    await flushPromises()
    expect(current.events).toEqual(['confirm'])
  })

  it('AC12 취소를 누르면 cancel을 내보내고 닫히며 연 버튼으로 포커스가 돌아간다', async () => {
    current = mountHost()
    await openWith()
    button('취소').click()
    await flushPromises()
    expect(current.events).toEqual(['cancel'])
    expect(current.open.value).toBe(false)
    expect(dialog()).toBeNull()
    expect(document.activeElement.id).toBe('opener')
  })

  it('AC12 Esc를 누르면 cancel을 내보내고 닫힌다', async () => {
    current = mountHost()
    await openWith()
    await pressEscape()
    expect(current.events).toEqual(['cancel'])
    expect(current.open.value).toBe(false)
  })

  it('AC12 바깥을 누르면 cancel을 내보내고 닫힌다', async () => {
    current = mountHost()
    await openWith()
    await pressOutside()
    expect(current.events).toEqual(['cancel'])
    expect(current.open.value).toBe(false)
  })

  it('AC12 loading 동안은 바깥을 눌러도 닫히지 않는다', async () => {
    current = mountHost({ loading: true })
    await openWith()
    await pressOutside()
    expect(current.events).toEqual([])
    expect(current.open.value).toBe(true)
  })

  it('AC12 loading 동안은 두 버튼이 비활성이고 Esc로 닫히지 않는다', async () => {
    current = mountHost({ loading: true })
    await openWith()
    expect(button('취소').disabled).toBe(true)
    expect(button('삭제').disabled).toBe(true)
    await pressEscape()
    expect(current.events).toEqual([])
    expect(current.open.value).toBe(true)
    expect(dialog()).not.toBeNull()
  })
})
