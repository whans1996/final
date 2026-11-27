---
name: 슬롯 (slot)
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/components/slots
---

## summary

슬롯은 부모가 자식 컴포넌트 안의 정해진 자리에 원하는 템플릿을 끼워 넣게 하는 기능이다. 모달, 카드, 레이아웃처럼 틀은 같고 내용만 다른 컴포넌트를 만들 때 쓴다.

## body

## 핵심
- 기본 슬롯: 자식의 `<slot />` 자리에 부모가 태그 사이에 넣은 내용이 들어간다. `<slot>기본 내용</slot>`으로 대체 내용을 둘 수 있다.
- 이름 있는 슬롯: 자식 `<slot name="footer" />`, 부모 `<template #footer>...</template>`.
- 범위 슬롯: 자식이 `<slot :item="item" />`처럼 데이터를 올려 주면, 부모가 `<template #default="{ item }">`로 받아 그리는 방식을 정한다. 목록 컴포넌트에서 항목 모양을 부모가 정할 때 쓴다.

## 주의
- props로 충분한 단순 텍스트까지 슬롯으로 받으면 사용법이 복잡해진다.
- 슬롯 내용은 부모의 범위에서 컴파일된다. 슬롯 안에서는 부모의 변수만 쓸 수 있고, 자식 데이터는 범위 슬롯으로 받아야 한다.

## example

```vue
<!-- BaseCard.vue -->
<template>
  <section class="card">
    <header><slot name="header">제목 없음</slot></header>
    <slot />
    <footer v-if="$slots.footer"><slot name="footer" /></footer>
  </section>
</template>
```

```vue
<!-- DataList.vue: 범위 슬롯 -->
<template>
  <ul>
    <li v-for="item in items" :key="item.id"><slot :item="item" /></li>
  </ul>
</template>
```

```vue
<!-- 부모 -->
<BaseCard>
  <template #header>트랜잭션 전파</template>
  <p>REQUIRED와 REQUIRES_NEW의 차이</p>
  <template #footer><button>찜</button></template>
</BaseCard>
```

```vue
<DataList :items="concepts">
  <template #default="{ item }">{{ item.name }} · {{ item.difficulty }}</template>
</DataList>
```
