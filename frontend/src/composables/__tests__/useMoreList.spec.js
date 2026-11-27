import { effectScope } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { useMoreList } from '@/composables/useMoreList'
import { deferred } from '@/views/__tests__/viewTestUtils'

const items = (...ids) => ids.map((id) => ({ id }))

describe('useMoreList (V-4 AC10)', () => {
  it('AC10 다음 쪽이 앞 쪽과 겹치면 이미 보인 id는 거른다', async () => {
    const fetchPage = vi
      .fn()
      .mockResolvedValueOnce({ items: items(1, 2), total: 5 })
      .mockResolvedValueOnce({ items: items(2, 3), total: 5 })
    const list = useMoreList(fetchPage, 2)

    await list.reset()
    await list.more()

    expect(list.items.value.map((i) => i.id)).toEqual([1, 2, 3])
    expect(fetchPage).toHaveBeenLastCalledWith(2)
  })

  it('AC10 reset 중 늦게 온 이전 요청의 응답은 버린다', async () => {
    const first = deferred()
    const fetchPage = vi
      .fn()
      .mockReturnValueOnce(first.promise)
      .mockResolvedValueOnce({ items: items(9), total: 1 })
    const list = useMoreList(fetchPage, 20)

    list.reset()
    await list.reset()
    first.resolve({ items: items(1, 2), total: 2 })
    await flushPromises()

    expect(list.items.value.map((i) => i.id)).toEqual([9])
    expect(list.loading.value).toBe(false)
  })

  it('AC10 컴포넌트(effect scope)가 사라진 뒤 온 응답은 반영하지 않는다', async () => {
    const call = deferred()
    const fetchPage = vi.fn().mockReturnValueOnce(call.promise)
    const scope = effectScope()
    const list = scope.run(() => useMoreList(fetchPage, 20))

    list.reset()
    scope.stop()
    call.resolve({ items: items(1), total: 1 })
    await flushPromises()

    expect(list.items.value).toEqual([])
    expect(list.total.value).toBeNull()
    expect(list.loading.value).toBe(true)
  })
})
