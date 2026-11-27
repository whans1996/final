// 시드 카드 ②예제 실행 검사 FE 쪽 (K-3a AC9). frontend/node_modules가 없으면(하네스 잡) 건너뛴다 → frontend 잡에서 돈다
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseCard, renderCard } from '../lib/seed/card.mjs';
import { checkFeExamples, loadFeTools } from '../lib/seed/examples.mjs';

const tools = loadFeTools();
const skip = tools ? false : 'frontend/node_modules 없음 (frontend CI 잡에서 실행)';

const row = {
  name: '예제',
  category: 'vue',
  difficulty: 'BEGINNER',
  summary: '요약',
  body: '본문',
  reference_url: 'https://ko.vuejs.org/',
};
const card = (blocks, slug = 'a') => parseCard(renderCard(row, blocks), `data/seed/concepts/vue/${slug}.md`);
const run = (blocks) => checkFeExamples([card(blocks)], tools);
const sev = (r) => r.issues.map((i) => `${i.item}:${i.severity}`).sort();

test('AC9_js·jsx는_문법만_본다 (모르는 이름은 문제 아님, 문법 오류는 오류)', { skip }, () => {
  assert.deepEqual(sev(run([{ lang: 'js', code: 'const a = useAuthStore();\nawait load(a);' }])), []);
  const r = run([{ lang: 'js', code: '// 라우트 정의\n{ path: "/a", component: A, meta: { requiresAuth: true } }' }]);
  assert.deepEqual(sev(r), ['js:error']);
  // 카드 파일 기준 줄 번호
  assert.equal(r.issues[0].line, 20); // 둘째 코드 줄
  assert.deepEqual(sev(run([{ lang: 'jsx', code: 'const el = <div className="a">{name}</div>;' }])), []);
  assert.deepEqual(sev(run([{ lang: 'jsx', code: 'const el = <div>;' }])), ['jsx:error']);
});

test('AC9_ts·tsx는_문법과_타입을_보고_이름·모듈_못_찾음은_경고', { skip }, () => {
  assert.deepEqual(sev(run([{ lang: 'ts', code: 'const n: number = 1;\nconst s: string = n.toFixed(1);' }])), []);
  assert.deepEqual(sev(run([{ lang: 'ts', code: 'const n: number = "문자열";' }])), ['ts:error']);
  const r = run([{ lang: 'ts', code: 'import { x } from "없는-모듈";\nconst y: Foo = x;\nunknownFn();' }]);
  assert.deepEqual(sev(r), ['ts:warn', 'ts:warn', 'ts:warn']);
  assert.match(r.issues.map((i) => i.message).join('\n'), /TS2307[\s\S]*TS2304/);
  // frontend 의존성의 타입을 실제로 읽는다 (vue)
  assert.deepEqual(sev(run([{ lang: 'ts', code: 'import { ref } from "vue";\nconst c = ref(0);\nc.value = "x";' }])), ['ts:error']);
  // 블록끼리 이름이 겹쳐도 각자 모듈이다
  assert.deepEqual(sev(run([{ lang: 'ts', code: 'const a = 1;' }, { lang: 'ts', code: 'const a = 2;' }])), []);
  assert.deepEqual(sev(run([{ lang: 'tsx', code: 'const el = <div>{1 + 1}</div>;' }])), []);
});

test('AC9_vue는_SFC_파싱·템플릿·스크립트_컴파일_오류가_오류', { skip }, () => {
  const ok = '<script setup>\nimport { ref } from "vue";\nconst n = ref(0);\n</script>\n\n<template>\n  <button @click="n++">{{ n }}</button>\n</template>';
  assert.deepEqual(sev(run([{ lang: 'vue', code: ok }])), []);
  assert.deepEqual(sev(run([{ lang: 'vue', code: '<template>\n  <div>{{ a </div>\n</template>' }])), ['vue:error']);
  assert.deepEqual(sev(run([{ lang: 'vue', code: '<script setup>\nconst = 1;\n</script>' }])), ['vue:error']);
  // <template> 없는 마크업 조각은 템플릿으로 컴파일한다
  assert.deepEqual(sev(run([{ lang: 'vue', code: '<!-- 안전 -->\n<p>{{ comment.body }}</p>' }])), []);
  assert.deepEqual(sev(run([{ lang: 'vue', code: '<li v-for="x in" :key="x">{{ x }}</li>' }])), ['vue:error']);
});

test('AC9_vue_script_setup_lang_ts면_ts_규칙도_적용', { skip }, () => {
  const sfc = (body) => `<script setup lang="ts">\n${body}\n</script>\n\n<template>\n  <p>{{ title }}</p>\n</template>`;
  assert.deepEqual(sev(run([{ lang: 'vue', code: sfc('const props = defineProps<{ title: string }>();') }])), []);
  const r = run([{ lang: 'vue', code: sfc('const count: number = "1";') }]);
  assert.deepEqual(sev(r), ['vue:error']);
  assert.match(r.issues[0].message, /TS2322/);
});

test('AC9_json은_JSON.parse_실패가_오류이고_검사_안_하는_언어는_따로_센다', { skip }, () => {
  assert.deepEqual(sev(run([{ lang: 'json', code: '{"a": 1}' }])), []);
  assert.deepEqual(sev(run([{ lang: 'json', code: "{a: 1}" }])), ['json:error']);
  const r = run([{ lang: 'http', code: 'GET /api' }, { lang: 'bash', code: 'npm ci' }, { lang: 'java', code: 'int a;' }]);
  assert.deepEqual(r.issues, []);
  assert.deepEqual(
    r.unchecked.map((u) => u.lang),
    ['http', 'bash'],
  );
  assert.deepEqual(r.checked, {});
});
