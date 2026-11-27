---
name: XSS (교차 사이트 스크립팅) 방어
category: security
difficulty: JUNIOR
reference: https://developer.mozilla.org/ko/docs/Glossary/Cross-site_scripting
---

## summary

XSS는 공격자가 게시글·댓글 같은 입력에 스크립트를 넣어, 다른 사용자의 브라우저에서 실행되게 하는 공격이다. 토큰·쿠키 탈취나 사용자 대신 요청 보내기로 이어진다.

## body

## 핵심
- 저장형(DB에 저장된 글), 반사형(URL 파라미터가 그대로 출력), DOM 기반(프론트 코드가 위험하게 삽입)이 있다.
- 기본 방어는 **출력할 때 이스케이프**다. Vue의 `{{ }}`는 자동으로 이스케이프한다.
- 위험한 지점: Vue `v-html`, `innerHTML`, 마크다운을 HTML로 변환해 그대로 넣는 코드.
- HTML을 꼭 보여 줘야 하면 허용 태그만 남기는 sanitizer(예: DOMPurify)를 거친다.
- 추가 방어: 토큰을 HttpOnly 쿠키에 두기, Content-Security-Policy 헤더.

## AI 코드에서 보이는 모습
마크다운 미리보기·AI 설명 렌더링에서 `v-html="marked(text)"`처럼 변환 결과를 그대로 넣는 코드가 흔하다. LLM 출력도 신뢰할 수 없는 입력으로 다룬다.

## 주의
- 입력 시점에 태그를 지우는 것만으로는 부족하다. 출력 위치(HTML 본문, 속성, URL, 스크립트)마다 필요한 처리가 다르다.
- `href`에 사용자 입력을 넣으면 `javascript:` 주소가 실행될 수 있다.

## example

```vue
<!-- 안전: 자동 이스케이프 -->
<p>{{ comment.content }}</p>
```

```vue
<!-- 위험: 입력에 <img src=x onerror=alert(1)>이 있으면 실행된다 -->
<div v-html="comment.content"></div>
```

```vue
<!-- 마크다운을 보여 줘야 할 때 -->
<script setup>
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";

const props = defineProps({ markdown: String });
const safeHtml = computed(() => DOMPurify.sanitize(marked.parse(props.markdown ?? "")));
</script>

<template>
  <div v-html="safeHtml"></div>
</template>
```
