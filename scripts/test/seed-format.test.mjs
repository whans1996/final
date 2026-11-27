// 시드 카드 ①형식 검사 (K-3a AC4~AC6)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { CATEGORY_ORDER, parseCard, renderCard } from '../lib/seed/card.mjs';
import { checkFormat, loadRules } from '../lib/seed/format.mjs';

const SRC = 'data/seed/concepts';
const rules = {
  src: SRC,
  categories: new Set(CATEGORY_ORDER),
  hosts: new Set(['docs.oracle.com', 'docs.spring.io', 'developer.mozilla.org']),
};
const BODY = '## 핵심\n' + '본문 문장이다. '.repeat(20);
const row = (over = {}) => ({
  name: '예외 처리',
  category: 'java',
  difficulty: 'BEGINNER',
  summary: '예외는 정상 흐름을 벗어난 상황을 호출한 쪽에 알리는 방법이다. 커스텀 예외로 원인을 드러낸다.',
  body: BODY,
  reference_url: 'https://docs.oracle.com/javase/tutorial/essential/exceptions/',
  ...over,
});
const JAVA = [{ lang: 'java', code: 'int a = 1;' }];
const card = (slug, over, blocks = JAVA, category = over?.category ?? 'java') =>
  parseCard(renderCard(row(over), blocks), `${SRC}/${category}/${slug}.md`);
const issues = (cards, severity) => checkFormat(Array.isArray(cards) ? cards : [cards], rules).filter((i) => !severity || i.severity === severity);
const items = (list) => list.map((i) => i.item).sort();

test('AC4_정상_카드는_오류도_경고도_없다', () => {
  assert.deepEqual(issues(card('exception')), []);
});

test('AC4_필수_frontmatter·섹션이_없거나_모르는_키·섹션이_있으면_오류', () => {
  const text = [
    '---',
    'name: 예외 처리',
    'category: java',
    'level: BEGINNER',
    '---',
    '',
    '## summary',
    '',
    row().summary,
    '',
    '## notes',
    '',
    '메모',
    '',
    '## example',
    '',
    '```java',
    'int a = 1;',
    '```',
  ].join('\n');
  const list = issues(parseCard(text, `${SRC}/java/exception.md`), 'error');
  assert.deepEqual(items(list), ['body', 'difficulty', 'level', 'notes', 'reference']);
  for (const i of list) {
    assert.equal(i.file, `${SRC}/java/exception.md`);
    assert.equal(i.stage, '①');
    assert.ok(i.message.length > 0);
  }
});

test('AC4_frontmatter가_없으면_오류', () => {
  const list = issues(parseCard('## summary\n\n요약\n', `${SRC}/java/a.md`), 'error');
  assert.ok(list.some((i) => i.item === 'frontmatter'));
});

test('AC4_카테고리가_허용_목록_밖이거나_폴더와_다르면_오류', () => {
  // K-1에서 react가 12종에 들어와 목록 밖 예시를 kotlin으로 바꿨다
  assert.deepEqual(items(issues(card('a', { category: 'kotlin' }, JAVA, 'kotlin'), 'error')), ['category']);
  const moved = card('a', { category: 'spring' }, JAVA, 'java');
  const list = issues(moved, 'error');
  assert.deepEqual(items(list), ['category']);
  assert.match(list[0].message, /폴더/);
});

test('AC4_경로가_category/slug.md가_아니면_오류', () => {
  const bad = parseCard(renderCard(row(), JAVA), `${SRC}/java/Exception_Handling.md`);
  assert.deepEqual(items(issues(bad, 'error')), ['path']);
  const deep = parseCard(renderCard(row(), JAVA), `${SRC}/java/sub/a.md`);
  assert.ok(items(issues(deep, 'error')).includes('path'));
});

test('AC4_난이도가_3단계_밖이면_오류', () => {
  assert.deepEqual(items(issues(card('a', { difficulty: 'EXPERT' }), 'error')), ['difficulty']);
});

test('AC4_name_100자·summary_500자_초과는_오류_(DB_한도, 글자_수_기준)', () => {
  assert.deepEqual(items(issues(card('a', { name: '가'.repeat(100) }), 'error')), []);
  assert.deepEqual(items(issues(card('a', { name: '가'.repeat(101) }), 'error')), ['name']);
  assert.deepEqual(items(issues(card('a', { summary: '나'.repeat(501) }), 'error')), ['summary']);
});

test('AC4_reference_500자·body·예제_TEXT_65535바이트_초과는_오류_(DB_한도)', () => {
  const ref = 'https://docs.oracle.com/'.padEnd(501, 'a');
  assert.deepEqual(items(issues(card('a', { reference_url: ref }), 'error')), ['reference']);
  assert.deepEqual(items(issues(card('a', { reference_url: ref.slice(0, 500) }), 'error')), []);
  // TEXT는 바이트 한도: 한글 1자 = utf8mb4 3바이트
  assert.deepEqual(items(issues(card('a', { body: '가'.repeat(21846) }), 'error')), ['body']);
  assert.deepEqual(items(issues(card('a', { body: '가'.repeat(21845) }), 'error')), []);
  assert.deepEqual(items(issues(card('a', {}, [{ lang: 'java', code: 'a'.repeat(65536) }]), 'error')), ['example']);
});

test('AC4_본문·예제가_비어_있으면_오류', () => {
  assert.deepEqual(items(issues(card('a', { body: '' }), 'error')), ['body']);
  assert.deepEqual(items(issues(card('a', {}, []), 'error')), ['example']);
  assert.deepEqual(items(issues(card('a', {}, [{ lang: 'java', code: '' }]), 'error')), ['example']);
});

test('AC4_예제_블록의_언어_태그가_없거나_허용_목록_밖이면_오류, 블록_밖_텍스트도_오류', () => {
  const list = issues(card('a', {}, [{ lang: '', code: 'a' }, { lang: 'python', code: 'b' }, { lang: 'http', code: 'GET /' }]), 'error');
  assert.deepEqual(items(list), ['example', 'example']);
  assert.match(list.map((i) => i.message).join('\n'), /언어 태그가 없음[\s\S]*python/);
  const text = renderCard(row(), JAVA).replace('```java', '설명 문장\n\n```java');
  assert.deepEqual(items(issues(parseCard(text, `${SRC}/java/a.md`), 'error')), ['example']);
});

test('AC4_reference가_https가_아니거나_DS01_출처_호스트가_아니면_오류', () => {
  assert.deepEqual(items(issues(card('a', { reference_url: 'http://docs.oracle.com/x' }), 'error')), ['reference']);
  assert.deepEqual(items(issues(card('a', { reference_url: 'https://www.baeldung.com/java-exceptions' }), 'error')), ['reference']);
  assert.deepEqual(items(issues(card('a', { reference_url: 'https://docs.spring.io/spring-framework/reference/' }), 'error')), []);
});

test('AC5_이름이_대소문자·악센트만_다르면_두_파일_모두_오류', () => {
  const list = issues([card('a', { name: 'Spring Bean' }), card('b', { name: 'spring béan' }), card('c', { name: '다른 카드' })], 'error');
  assert.deepEqual(
    list.map((i) => [i.file, i.item]),
    [
      [`${SRC}/java/a.md`, 'name'],
      [`${SRC}/java/b.md`, 'name'],
    ],
  );
  assert.match(list[0].message, /b\.md/);
});

test('AC5_공백·기호만_다른_이름은_경고', () => {
  const other = '빈은 스프링 컨테이너가 만들고 관리하는 객체다. 생명주기와 스코프를 컨테이너가 정한다.';
  const cards = [card('a', { name: 'Spring Bean 스코프' }), card('b', { name: 'Spring-Bean·스코프', summary: other })];
  assert.deepEqual(issues(cards, 'error'), []);
  assert.deepEqual(items(issues(cards, 'warn')), ['name', 'name']);
});

test('AC6_권장_범위를_벗어나면_경고', () => {
  assert.deepEqual(items(issues(card('a', { name: '가'.repeat(61) }), 'warn')), ['name']);
  assert.deepEqual(items(issues(card('a', { summary: '짧은 요약' }), 'warn')), ['summary']);
  assert.deepEqual(items(issues(card('a', { summary: '긴'.repeat(201) }), 'warn')), ['summary']);
  assert.deepEqual(items(issues(card('a', { body: '짧은 본문' }), 'warn')), ['body']);
  assert.deepEqual(items(issues(card('a', { body: '긴'.repeat(2001) }), 'warn')), ['body']);
  const long = Array.from({ length: 61 }, (_, i) => `int a${i} = ${i};`).join('\n');
  assert.deepEqual(items(issues(card('a', {}, [{ lang: 'java', code: long }]), 'warn')), ['example']);
  // 예제 줄 수는 블록을 합친 example_code 기준
  const half = Array.from({ length: 30 }, (_, i) => `int a${i} = ${i};`).join('\n');
  assert.deepEqual(items(issues(card('a', {}, [{ lang: 'java', code: half }, { lang: 'java', code: half }]), 'warn')), ['example']);
});

test('AC6_요약의_문자_3-gram_Jaccard가_0.5_이상이면_두_카드와_점수를_경고', () => {
  const s = '예외는 정상 흐름을 벗어난 상황을 호출한 쪽에 알리는 방법이다. 커스텀 예외로 원인을 드러낸다.';
  const cards = [card('a', { name: 'A', summary: s }), card('b', { name: 'B', summary: s.replace('커스텀', '사용자 정의') }), card('c', { name: 'C', summary: '빈은 스프링 컨테이너가 만들고 관리하는 객체다. 생명주기와 스코프를 컨테이너가 정한다.' })];
  const list = issues(cards, 'warn');
  assert.equal(list.length, 1);
  assert.equal(list[0].item, 'summary');
  assert.equal(list[0].file, `${SRC}/java/a.md`);
  assert.match(list[0].message, /b\.md/);
  assert.match(list[0].message, /0\.\d\d/);
});

test('K1_AC5_loadRules는 ConceptCategories.ORDERED 12종과 datasets.json DS-01 호스트를 읽고 CATEGORY_ORDER와 같다', () => {
  const r = loadRules();
  assert.deepEqual([...r.categories], CATEGORY_ORDER);
  assert.deepEqual(CATEGORY_ORDER, ['java', 'spring', 'web', 'security', 'mybatis', 'vue', 'database', 'javascript', 'tooling', 'typescript', 'react', 'cs']);
  assert.ok(r.hosts.has('docs.spring.io'));
  assert.ok(r.hosts.has('ko.vuejs.org'));
  assert.equal(r.src, 'data/seed/concepts');
});

// ── K-1 AC13 수준별 선택 절 ──

test('K1_AC13_수준별_절은_선택이고_요약_300자_본문_10000자까지', () => {
  assert.deepEqual(issues(card('ok', { summary_beginner: '가'.repeat(300), body_senior: '나'.repeat(10000) }), 'error'), []);
  const over = issues(card('over', { summary_junior: '가'.repeat(301), body_beginner: '나'.repeat(10001) }), 'error');
  assert.deepEqual(items(over), ['body-beginner', 'summary-junior']);
});

test('K1_AC13_빈_수준별_절과_모르는_절은_오류', () => {
  const text = renderCard(row(), JAVA).replace('## example', '## summary-senior\n\n## body-expert\n\n본문\n\n## example');
  const list = issues(parseCard(text, `${SRC}/java/empty.md`), 'error');
  assert.deepEqual(items(list), ['body-expert', 'summary-senior']);
});

test('K1_AC5_새_카테고리_폴더의_카드도_통과한다', () => {
  assert.deepEqual(issues(card('hooks', { category: 'react' }), 'error'), []);
});
