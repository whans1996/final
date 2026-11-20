// "더 보기" 목록 (F-04 팔로우 목록·피드). 페이지 번호로 다음 쪽을 불러 아래에 붙이고, 이미 보인 id는 거른다.
// offset 페이지라 그사이 새 항목이 생기면 다음 쪽이 앞 쪽과 겹치기 때문이다 (F-12 UserWishes와 같은 방식).
import { computed, ref } from 'vue'

/**
 * @param {(page: number) => Promise<{ items: { id: number }[], total: number }>} fetchPage
 * @param {number} pageSize 서버가 이보다 적게 주면 끝으로 본다
 */
export function useMoreList(fetchPage, pageSize) {
  const items = ref([])
  const total = ref(null)
  const page = ref(0)
  const loading = ref(false)
  const error = ref(null)
  const ended = ref(false)

  const hasMore = computed(
    () => !ended.value && total.value !== null && items.value.length < total.value,
  )

  // 다시 불러오면(탭·id 변경) 늦게 도착한 이전 요청의 응답은 버린다
  let lastRequest = 0

  async function load(nextPage) {
    const request = ++lastRequest
    loading.value = true
    error.value = null
    try {
      const result = await fetchPage(nextPage)
      if (request !== lastRequest) return
      if (nextPage === 1) {
        items.value = result.items
      } else {
        const seen = new Set(items.value.map((item) => item.id))
        items.value = [...items.value, ...result.items.filter((item) => !seen.has(item.id))]
      }
      ended.value = result.items.length < pageSize
      total.value = result.total
      page.value = nextPage
    } catch (e) {
      if (request === lastRequest) error.value = e
    } finally {
      if (request === lastRequest) loading.value = false
    }
  }

  function reset() {
    items.value = []
    total.value = null
    page.value = 0
    ended.value = false
    error.value = null
    return load(1)
  }

  return { items, total, loading, error, hasMore, reset, more: () => load(page.value + 1) }
}
