import { describe, expect, it } from 'vitest'
import { identityOrder, reshuffle } from '@/utils/quizShuffle'

/** 미리 정한 값을 차례로 돌려주는 난수 함수 */
function sequence(...values) {
  let i = 0
  return () => values[i++ % values.length]
}

describe('quizShuffle (L-1b AC11)', () => {
  it('AC11 identityOrder는 저장된 순서다', () => {
    expect(identityOrder(4)).toEqual([0, 1, 2, 3])
    expect(identityOrder(0)).toEqual([])
  })

  it('AC11 섞은 결과는 같은 위치들의 순열이고 직전 순서와 다르다', () => {
    const prev = [0, 1, 2, 3]
    for (let k = 0; k < 50; k += 1) {
      const next = reshuffle(prev, Math.random)
      expect([...next].sort()).toEqual([0, 1, 2, 3])
      expect(next).not.toEqual(prev)
    }
  })

  it('AC11 난수가 직전 순서를 그대로 만들면 한 칸 회전한다', () => {
    // random()이 늘 0.999…면 j = i라 아무것도 바꾸지 않는다
    const prev = [2, 0, 3, 1]
    expect(reshuffle(prev, () => 0.9999)).toEqual([0, 3, 1, 2])
  })

  it('AC11 난수에 따라 섞는다 (Fisher–Yates)', () => {
    // i=3: j=0 → [3,1,2,0], i=2: j=0 → [2,1,3,0], i=1: j=1 → 그대로
    expect(reshuffle([0, 1, 2, 3], sequence(0, 0, 0.9))).toEqual([2, 1, 3, 0])
  })

  it('AC11 직전 순서를 바꾸지 않고 선지가 1개 이하면 그대로다', () => {
    const prev = [0, 1, 2, 3]
    reshuffle(prev, () => 0)
    expect(prev).toEqual([0, 1, 2, 3])
    expect(reshuffle([0], () => 0)).toEqual([0])
    expect(reshuffle([], () => 0)).toEqual([])
  })
})
