// SC-08 "다시 풀기"의 선지 다시 섞기 (L-1b AC11). 표시 순서(order)는 저장된 선지 위치의 순열이다:
// order[화면 위치] = 저장된 위치. 제출할 때는 고른 화면 위치를 order로 저장된 위치로 바꿔 보낸다.

/** 저장된 순서 그대로 [0, 1, …, n-1] */
export function identityOrder(n) {
  return Array.from({ length: n }, (_, i) => i)
}

/**
 * 직전 순서(prevOrder)와 다른 순열을 만든다 (Fisher–Yates). 섞은 결과가 직전과 같으면 한 칸 회전한다.
 * 선지가 1개 이하면 그대로 돌려준다. random은 [0, 1) 난수 함수로, 테스트에서 주입한다.
 * @param {number[]} prevOrder
 * @param {() => number} [random]
 * @returns {number[]}
 */
export function reshuffle(prevOrder, random = Math.random) {
  const next = [...prevOrder]
  if (next.length < 2) return next
  for (let i = next.length - 1; i > 0; i -= 1) {
    const j = Math.floor(random() * (i + 1))
    ;[next[i], next[j]] = [next[j], next[i]]
  }
  if (next.every((value, i) => value === prevOrder[i])) next.push(next.shift())
  return next
}
