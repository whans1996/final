import { onBeforeUnmount } from 'vue'

/**
 * `task`를 바로 1회 실행하고, 끝날 때마다 `interval` 뒤에 다시 실행한다 (응답이 늦어도 요청이 겹치지 않는다).
 * `task`가 false를 돌려주면 멈추고, 화면을 떠나면(unmount) 자동으로 멈춘다 (frontend/CLAUDE.md).
 *
 * `task`는 `isCurrent()`를 인자로 받는다. 기다리는 동안 stop·start가 다시 불렸으면 false이므로,
 * 늦게 도착한 이전 폴링의 응답을 화면에 반영하지 않을 때 쓴다.
 */
export function usePolling(task, { interval = 2000 } = {}) {
  let generation = 0
  let timer = null

  async function run(gen) {
    const isCurrent = () => gen === generation
    const keepGoing = await task(isCurrent)
    if (isCurrent() && keepGoing !== false) {
      timer = setTimeout(() => run(gen), interval)
    }
  }

  function stop() {
    generation += 1
    clearTimeout(timer)
    timer = null
  }

  /** `delay`(ms)를 주면 첫 실행도 그만큼 기다린다 (방금 받은 응답이 최신일 때). */
  function start({ delay = 0 } = {}) {
    stop()
    const gen = generation
    if (delay > 0) {
      timer = setTimeout(() => run(gen), delay)
    } else {
      run(gen)
    }
  }

  onBeforeUnmount(stop)

  return { start, stop }
}
