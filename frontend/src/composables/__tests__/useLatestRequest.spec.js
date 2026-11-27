import { effectScope } from 'vue'
import { describe, expect, it } from 'vitest'
import { useLatestRequest } from '@/composables/useLatestRequest'

describe('useLatestRequest', () => {
  it('AC1 다시 begin하면 이전 요청은 현재가 아니고 취소된다', () => {
    const latest = useLatestRequest()
    const first = latest.begin()
    expect(first.isCurrent()).toBe(true)
    expect(first.signal.aborted).toBe(false)

    const second = latest.begin()
    expect(first.isCurrent()).toBe(false)
    expect(first.signal.aborted).toBe(true)
    expect(second.isCurrent()).toBe(true)
    expect(second.signal.aborted).toBe(false)
  })

  it('AC2 invalidate하면 진행 중 요청은 현재가 아니고 취소되며, 그 뒤 begin은 다시 현재다', () => {
    const latest = useLatestRequest()
    const req = latest.begin()
    latest.invalidate()
    expect(req.isCurrent()).toBe(false)
    expect(req.signal.aborted).toBe(true)

    const next = latest.begin()
    expect(next.isCurrent()).toBe(true)
  })

  it('AC2 effect scope가 사라지면 진행 중 요청은 무효이고, 사라진 뒤 begin한 요청도 현재가 아니다', () => {
    const scope = effectScope()
    const latest = scope.run(() => useLatestRequest())
    const req = latest.begin()

    scope.stop()
    expect(req.isCurrent()).toBe(false)
    expect(req.signal.aborted).toBe(true)

    const late = latest.begin()
    expect(late.isCurrent()).toBe(false)
    expect(late.signal.aborted).toBe(true)
  })

  it('AC2 scope 밖에서 쓰면 자동 정리 없이 동작한다', () => {
    const latest = useLatestRequest()
    expect(latest.begin().isCurrent()).toBe(true)
  })
})
