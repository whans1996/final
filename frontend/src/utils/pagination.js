/**
 * 페이지 번호 목록에서 요청한 page가 결과 범위를 넘었으면 돌아갈 마지막 page를, 아니면 null을 준다 (#46·#76).
 * BE는 범위를 넘은 page에 `items: []`와 실제 `total`을 주므로, 그대로 그리면 빈 목록 안내만 보이고
 * PageNav가 숨어 앞 페이지로 돌아갈 길이 없다. 결과가 0건(total 0)이면 빈 목록이 맞으므로 null이다.
 */
export function overflowLastPage({ page, size, total, itemCount }) {
  if (itemCount > 0 || total <= 0) return null
  const lastPage = Math.ceil(total / size)
  return page > lastPage ? lastPage : null
}
