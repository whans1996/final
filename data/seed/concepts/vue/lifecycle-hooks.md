---
name: 생명주기 훅 (onMounted·onUnmounted)
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/essentials/lifecycle
---

## summary

생명주기 훅은 컴포넌트가 화면에 붙을 때, 갱신될 때, 사라질 때 실행할 코드를 등록하는 함수다. 첫 데이터 로딩과 정리(cleanup)에 쓴다.

## body

## 핵심
- `onMounted`: DOM이 그려진 뒤 실행된다. 첫 API 호출, DOM 측정, 외부 라이브러리 초기화.
- `onUnmounted`(`onBeforeUnmount`): 컴포넌트가 사라질 때 실행된다. 타이머, 이벤트 리스너, `EventSource`, 구독을 정리한다.
- `onUpdated`: 반응형 상태 변경으로 다시 그린 뒤 실행된다. 상태를 바꾸면 무한 갱신에 빠질 수 있어 드물게 쓴다.
- `<script setup>` 본문 자체가 예전의 `created` 시점이다.

## 주의
- `setInterval`, `window.addEventListener`를 등록하고 정리하지 않으면 페이지를 떠난 뒤에도 계속 실행된다(메모리 누수, 중복 요청).
- 훅은 setup 시점에 동기적으로 등록해야 한다. `await` 뒤에서 등록하면 연결되지 않는다.

## example

```vue
<script setup>
import { ref, onMounted, onUnmounted } from "vue";

const props = defineProps({ analysisId: Number });
const status = ref(null);
let timer = null;

async function refresh() {
  status.value = await fetchAnalysisStatus(props.analysisId);
  if (status.value.status === "DONE" || status.value.status === "FAILED") {
    clearInterval(timer);
  }
}

onMounted(() => {
  refresh();
  timer = setInterval(refresh, 3000);   // 3초마다 진행률 조회
});

onUnmounted(() => clearInterval(timer)); // 화면을 떠나면 멈춘다
</script>
```
