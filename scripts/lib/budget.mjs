// hook 시간 예산 (H-5). SessionStart hook은 제한(15초)을 넘기면 출력이 통째로 버려져 NOW 주입까지 사라진다.
// 그래서 전체 예산을 정해 두고, 남은 시간이 모자라면 덜 중요한 단계(자동 병합, gh 결과 기다리기, 내 브랜치 조회)를 건너뛴다.
// 동기 git이 이벤트 루프를 막는 동안에는 타이머가 돌지 않으므로, 단계를 시작하기 전에 남은 시간을 보고 고르는 방식이다.

/** 세션 시작 hook의 예산: 제한 15초에서 Node 기동·출력·여유 4초를 뺐다. VIBENOTE_SESSION_BUDGET_MS로 바꿀 수 있다 (점검용) */
export const SESSION_START_BUDGET_MS = 11_000;

export function sessionStartBudgetMs(env = process.env) {
  const v = Number(env.VIBENOTE_SESSION_BUDGET_MS);
  return Number.isFinite(v) && v >= 0 && env.VIBENOTE_SESSION_BUDGET_MS !== '' ? v : SESSION_START_BUDGET_MS;
}

/**
 * totalMs 예산. now는 테스트에서 시계를 바꿀 때만 준다.
 * - left(): 남은 ms (0 이상)
 * - has(ms): ms 이상 남았는가
 * - timeout(max, reserve): 이번 단계에 줄 제한 시간 = min(max, 남은 시간 - reserve), 0 이상
 * - within(promise, fallback, reserve): 남은 시간 - reserve 안에 끝나면 그 결과, 아니면 fallback()의 값
 * - skip(what) / skipped: 예산 때문에 건너뛴 단계 이름 (사람·모델에게 알린다)
 */
export function createBudget(totalMs, { now = Date.now } = {}) {
  const start = now();
  const skipped = [];
  const left = () => Math.max(0, totalMs - (now() - start));
  return {
    left,
    has: (ms) => left() >= ms,
    timeout: (max, reserve = 0) => Math.max(0, Math.min(max, left() - reserve)),
    within(promise, fallback, reserve = 0) {
      const ms = Math.max(0, left() - reserve);
      let timer;
      const late = new Promise((resolve) => {
        timer = setTimeout(() => resolve(fallback()), ms);
      });
      return Promise.race([promise, late]).finally(() => clearTimeout(timer));
    },
    skip(what) {
      if (!skipped.includes(what)) skipped.push(what);
    },
    skipped,
  };
}
