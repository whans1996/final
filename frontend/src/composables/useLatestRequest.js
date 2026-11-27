// 최신 요청만 반영하기 (V-4). 대상(id·검색어·쪽)이 바뀌거나 화면을 떠날 수 있는 조회에 쓴다.
// begin()이 이전 요청을 취소(abort)하고 순번을 올린다. 응답 뒤 isCurrent()가 false면 버린다.
// effect scope(컴포넌트·composable) 안에서 부르면 scope가 사라질 때 진행 중 요청을 무효로 하고,
// 그 뒤의 begin()도 처음부터 무효다 (늦은 watch가 다시 불러도 반영하지 않는다).
import { getCurrentScope, onScopeDispose } from 'vue'

/**
 * @returns {{
 *   begin: () => { signal: AbortSignal, isCurrent: () => boolean },
 *   invalidate: () => void,
 * }}
 */
export function useLatestRequest() {
  let sequence = 0
  let controller = null
  let disposed = false

  function abortCurrent() {
    controller?.abort()
    controller = null
  }

  function begin() {
    abortCurrent()
    const id = ++sequence
    const own = new AbortController()
    if (disposed) own.abort()
    else controller = own
    return {
      signal: own.signal,
      isCurrent: () => !disposed && id === sequence,
    }
  }

  function invalidate() {
    sequence++
    abortCurrent()
  }

  if (getCurrentScope()) {
    onScopeDispose(() => {
      disposed = true
      invalidate()
    })
  }

  return { begin, invalidate }
}
