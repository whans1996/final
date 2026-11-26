// 로드맵 R-1: docs/로드맵.md 파싱, PR·브랜치로 상태 계산, 배너·블록·다음 항목·지난 넘겨주는 시점 (gh 호출 없는 부분)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { sessionBanner } from '../lib/common.mjs';
import {
  ROADMAP_FILE,
  bannerLines,
  branchMatches,
  daysBetween,
  fetchRoadmapPrs,
  newlyOverdue,
  nextItem,
  nextItemLine,
  overdueMessage,
  parseRoadmap,
  roadmapBlock,
  roadmapRefs,
  roadmapStatus,
  roadmapView,
  seoulToday,
} from '../lib/roadmap.mjs';

const MD = `# 로드맵
- 연도: 2026

## 항목

| ID | 담당 | 트랙 | 시작 | 끝 | 의존 | 할 일 | 모델 |
|---|---|---|---|---|---|---|---|
| HP-1 | A | 4 하네스 | 11/26 | 11/26 | - | 가드 | Opus/high |
| HP-2 | A | 4 하네스 | 11/26 | 11/27 | HP-1 | 로드맵 | Opus/high |
| HP-3 | A | 4 하네스 | 11/27 | 11/28 | HP-2 | 표시 | Opus/high |
| E-1 | A | 1 분석 | 11/26 | 11/29 | - | 러너 | Opus/high |
| P-1a | A | 1 분석 | 11/29 | 12/02 | - | 프로젝트 | Opus/high |
| V-2 | B | B | 11/26 | 11/28 | - | 평가 세트 | Opus/medium |
| ST-1 | A·B | 공통 | 12/15 | 12/20 | - | 안정화 | Opus/medium |

## 넘겨주는 시점

| 날짜 | 주는 쪽 | 받는 쪽 | 항목 | 넘기는 것 | 풀리는 작업 |
|---|---|---|---|---|---|
| 11/28 | B | A | V-2 | 평가 세트 고정 | 기준선 |
| 11/29 | A | B | E-1 | 러너 머지 | 기준선 |
| 12/02 | A | A·B | P-1a | API | T-1 |

## 공통 일정

| 날짜 | 일정 |
|---|---|
| 12/14 | 기능 동결 |
`;

const view = (prs = [], branches = [], today = '2026-11-26', opts) => {
  const r = parseRoadmap(MD);
  return roadmapView(r, roadmapStatus(r.items, { prs, branches }), today, opts);
};
const merged = (number, body, headRefName = 'feature/x') => ({ number, state: 'MERGED', body, headRefName });

test('실제 docs/로드맵.md는 형식 오류 없이 읽힌다 (표 머리·열 순서·날짜·의존·넘겨주는 항목)', () => {
  const r = parseRoadmap(readFileSync(ROADMAP_FILE, 'utf8'));
  assert.deepEqual(r.errors, []);
  assert.ok(r.items.length >= 10);
  assert.ok(r.handoffs.length >= 1 && r.events.length >= 1);
  for (const i of r.items) assert.ok(i.owners.length && i.start && i.end && i.model, i.id);
});

test('parseRoadmap은 표를 항목·넘겨주는 시점·공통 일정으로 읽는다', () => {
  const r = parseRoadmap(MD);
  assert.deepEqual(r.errors, []);
  assert.equal(r.year, 2026);
  assert.deepEqual(r.items[1], {
    id: 'HP-2',
    owners: ['dh'],
    owner: 'A',
    track: '4 하네스',
    start: '2026-11-26',
    end: '2026-11-27',
    deps: ['HP-1'],
    task: '로드맵',
    model: 'Opus/high',
  });
  assert.deepEqual(r.items.at(-1).owners, ['dh', 'js']);
  assert.deepEqual(r.handoffs[2].toKeys, ['dh', 'js']);
  assert.deepEqual(r.events, [{ date: '2026-12-14', text: '기능 동결' }]);
});

test('parseRoadmap은 형식 오류를 모아 알린다', () => {
  const bad = MD.replace('| HP-3 | A | 4 하네스 | 11/27 | 11/28 | HP-2 |', '| HP-3 | C | 4 하네스 | 11/31 | 11/28 | HP-9 |')
    .replace('| E-1 | A | 1 분석 | 11/26 | 11/29 |', '| HP-1 | A | 1 분석 | 11/26 | 11/29 |')
    .replace('| 11/29 | A | B | E-1 |', '| 11/29 | A | B | E-9 |')
    .replace('| 12/14 | 기능 동결 |', '| 12-14 | 기능 동결 |');
  const { errors } = parseRoadmap(bad);
  const has = (s) => assert.ok(errors.some((e) => e.includes(s)), `${s} 없음: ${errors.join(' / ')}`);
  has('HP-3: 담당은');
  has('HP-3: 없는 날짜');
  has('의존 HP-9');
  has('HP-1: ID 중복');
  has('항목 E-9이 항목 표에 없음');
  has('날짜 형식 오류 "12-14"');
  assert.ok(parseRoadmap('# 빈 파일').errors.some((e) => e.includes('연도')));
  assert.ok(parseRoadmap(MD.replace('| 4 하네스 | 11/26 | 11/26 | - | 가드 | Opus/high |', '| 4 하네스 | 11/26 | 11/26 | - | 가드 |')).errors.some((e) => e.includes('열 8개')));
});

test('roadmapRefs는 PR 본문의 Roadmap: 줄을 읽고 템플릿 주석은 무시한다', () => {
  assert.deepEqual(roadmapRefs('## 기능\n- Roadmap: HP-2\n'), [{ id: 'HP-2', partial: false }]);
  assert.deepEqual(roadmapRefs('Roadmap: `K-3a`, K-1 (일부)'), [
    { id: 'K-3a', partial: false },
    { id: 'K-1', partial: true },
  ]);
  assert.deepEqual(roadmapRefs('roadmap: T-1 (partial) · I-2'), [
    { id: 'T-1', partial: true },
    { id: 'I-2', partial: false },
  ]);
  // 템플릿 안내 주석, 문장 속 언급, 값 없는 줄은 상태로 치지 않는다
  assert.deepEqual(roadmapRefs('- Roadmap: <!-- 예) Roadmap: HP-2 -->\n본문에서 Roadmap: 줄을 쓴다'), []);
  // 같은 ID가 일부·완료로 둘 다 있으면 완료
  assert.deepEqual(roadmapRefs('Roadmap: P-1a (일부)\nRoadmap: P-1a'), [{ id: 'P-1a', partial: false }]);
  assert.deepEqual(roadmapRefs('Roadmap: HP-2\r\n'), [{ id: 'HP-2', partial: false }]);
});

test('branchMatches는 브랜치 이름의 한 부분이 ID로 시작할 때만 맞는다 (F-26b와 F-26b2 구별)', () => {
  assert.ok(branchMatches('feature/K-3a-seed-check', 'K-3a'));
  assert.ok(branchMatches('origin/feature/hp-2-roadmap', 'HP-2'));
  assert.ok(branchMatches('feature/F-26b', 'F-26b'));
  assert.ok(!branchMatches('feature/F-26b2-edges', 'F-26b'));
  assert.ok(!branchMatches('feature/F-26b-ts', 'F-26b2'));
  assert.ok(!branchMatches('feature/common-hp-2', 'HP-2'));
  assert.ok(!branchMatches(null, 'HP-2'));
});

test('roadmapStatus: 머지된 Roadmap 줄 = ✅, 일부·열린 PR·브랜치 = 🔨, 닫힌 PR은 무시, 조회 실패면 unknown', () => {
  const r = parseRoadmap(MD);
  const s = roadmapStatus(r.items, {
    prs: [
      merged(192, 'Roadmap: HP-1'),
      merged(200, 'Roadmap: E-1 (일부)'),
      { number: 201, state: 'OPEN', body: '', headRefName: 'feature/HP-2-roadmap' },
      { number: 202, state: 'CLOSED', body: 'Roadmap: P-1a', headRefName: 'feature/x' },
    ],
    branches: ['feature/V-2-freeze'],
  });
  assert.deepEqual(s.get('HP-1'), { state: 'done', prs: [192], branches: [] });
  assert.equal(s.get('E-1').state, 'doing');
  assert.deepEqual(s.get('HP-2').prs, [201]);
  assert.equal(s.get('HP-2').state, 'doing');
  assert.equal(s.get('P-1a').state, 'todo');
  assert.equal(s.get('V-2').state, 'doing');
  // 머지(완료)가 남은 브랜치보다 이긴다
  assert.equal(roadmapStatus(r.items, { prs: [merged(1, 'Roadmap: V-2')], branches: ['feature/V-2-x'] }).get('V-2').state, 'done');
  const unknown = roadmapStatus(r.items, { prs: null, branches: ['feature/V-2-x'] });
  assert.equal(unknown.get('HP-1').state, 'unknown');
  assert.equal(unknown.get('V-2').state, 'doing');
});

test('roadmapView: 끝 날짜가 지났는데 ✅가 아니면 late, 상태 조회 실패면 지연 판정을 하지 않는다', () => {
  const v = view([merged(1, 'Roadmap: HP-1')], [], '2026-11-28');
  assert.equal(v.byId.get('HP-1').late, false);
  assert.equal(v.byId.get('HP-2').late, true);
  assert.equal(v.byId.get('HP-3').late, false);
  assert.equal(v.handoffs[0].days, 0);
  const off = view([], [], '2026-11-28', { statusOk: false, reason: 'gh 없음' });
  assert.ok(off.items.every((i) => !i.late));
});

test('nextItem: 지금 브랜치 → 같은 트랙 다음 → 의존이 끝난 가장 이른 항목 → 가장 이른 항목', () => {
  const v = view([merged(1, 'Roadmap: HP-1')]);
  assert.equal(nextItem(v, 'dh', { branch: 'feature/E-1-runner' }).item.id, 'E-1');
  assert.equal(nextItem(v, 'dh', { branch: 'feature/E-1-runner' }).why, 'branch');
  // 방금 HP-1을 끝냈으면 같은 트랙의 HP-2
  assert.deepEqual(
    (({ item, why }) => [item.id, why])(nextItem(v, 'dh', { after: 'HP-1' })),
    ['HP-2', 'track'],
  );
  // 기본은 시작 순: HP-2(11/26~27)가 E-1(11/26~29)보다 앞
  assert.equal(nextItem(v, 'dh').item.id, 'HP-2');
  // HP-1이 안 끝났으면 HP-2는 의존 대기 → 의존 없는 E-1
  const v2 = view([]);
  assert.equal(nextItem(v2, 'dh').item.id, 'HP-1');
  const v3 = view([merged(1, 'Roadmap: HP-1, HP-2, HP-3, E-1, P-1a')]);
  assert.equal(nextItem(v3, 'dh').item.id, 'ST-1');
  assert.equal(nextItem(view([merged(1, 'Roadmap: HP-1, HP-2, HP-3, E-1, P-1a, ST-1')]), 'dh'), null);
  // 다른 사람 항목은 고르지 않는다
  assert.equal(nextItem(v, 'js').item.id, 'V-2');
});

test('nextItemLine은 추천 모델·의존 대기·지연을 함께 보인다', () => {
  const v = view([], [], '2026-11-28');
  const line = nextItemLine(nextItem(v, 'dh', { branch: 'feature/HP-3-banner' }));
  assert.match(line, /^➡️ 다음 항목: HP-3 표시 \(트랙 4 하네스, 11\/27~11\/28\) · 추천 Opus\/high · 지금 브랜치 · ⏳ 의존 대기 HP-2$/);
  assert.match(nextItemLine(nextItem(v, 'dh')), /HP-1 .*⚠️ 끝 날짜 지남/);
  assert.match(nextItemLine(null), /남은 내 항목이 없습니다/);
});

test('bannerLines: 트랙마다 시작한 다음 항목, D-2 안 넘겨주기 🤝, 지난 것 ⚠️, 그 밖에는 가장 가까운 📅', () => {
  const v = view([merged(1, 'Roadmap: HP-1')], ['feature/HP-2-x']);
  assert.deepEqual(bannerLines(v, 'dh'), ['🗺️ 로드맵: 4 하네스 HP-2 🔨 ~11/27 · 1 분석 E-1 ⬜ ~11/29', '🤝 넘겨주기 D-2: 11/28 B→A V-2 평가 세트 고정']);
  // 상대도 같은 넘겨주는 시점을 본다
  assert.ok(bannerLines(v, 'js').includes('🤝 넘겨주기 D-2: 11/28 B→A V-2 평가 세트 고정'));
  const late = view([merged(1, 'Roadmap: HP-1, HP-2, HP-3')], [], '2026-11-29');
  const lines = bannerLines(late, 'dh');
  assert.ok(lines.includes('⚠️ 넘겨주기 1일 지남: 11/28 B→A V-2 평가 세트 고정'), lines.join('\n'));
  assert.ok(lines.includes('🤝 넘겨주기 D-day: 11/29 A→B E-1 러너 머지'));
  // 트랙 4는 다 끝나 빠지고, 트랙 1은 시작 순으로 E-1 (같은 날 시작한 P-1a보다 앞)
  assert.equal(lines[0], '🗺️ 로드맵: 1 분석 E-1 ⬜ ~11/29');
  // D-2 밖이면 가장 가까운 것 하나
  const early = view([], [], '2026-11-20');
  assert.deepEqual(bannerLines(early, 'dh'), ['📅 다음 넘겨주기 D-8: 11/28 B→A V-2 평가 세트 고정']);
  // 상태를 모르면 지남 대신 미확인, 실패 줄
  const off = view([], [], '2026-11-29', { statusOk: false, reason: 'gh 없음' });
  const offLines = bannerLines(off, 'dh');
  assert.ok(offLines.includes('🤝 넘겨주기 1일 지남: 11/28 B→A V-2 평가 세트 고정 (상태 미확인)'), offLines.join('\n'));
  assert.equal(offLines.at(-1), '❔ 로드맵 상태 조회 실패 (gh 없음) → node scripts/roadmap.mjs');
});

test('roadmapBlock은 지금 할 일·내 남은 항목·상대 이번 기간·넘겨주는 시점·공통 일정을 담는다', () => {
  const v = view([merged(1, 'Roadmap: HP-1')], ['feature/HP-2-x']);
  const block = roadmapBlock(v, 'dh', { branch: 'feature/HP-2-x', peerName: '김준섭' });
  assert.match(block, /^## 로드맵 \(docs\/로드맵\.md · 오늘 11\/26/);
  assert.match(block, /- 지금 세션에서 할 일: HP-2 로드맵 .*지금 브랜치/);
  assert.match(block, /- 내 남은 항목 \(시작 순, 5개\):\n {2}- 🔨 HP-2 11\/26~11\/27 \[4 하네스\] 로드맵 \(feature\/HP-2-x\)/);
  assert.ok(!block.includes('HP-1 11/26'), '끝난 항목은 뺀다');
  assert.match(block, /- 김준섭\(B\) 이번 기간:\n {2}- ⬜ V-2/);
  assert.match(block, /🤝 11\/28 \(D-2\) B→A V-2 평가 세트 고정 → 기준선 \(항목 ⬜\)/);
  assert.match(block, /- 공통 일정: 12\/14 \(D-18\) 기능 동결/);
  assert.match(roadmapBlock(view([], [], '2026-11-26', { statusOk: false, reason: 'x' }), 'dh'), /❔ 상태 조회 실패 \(x\)/);
});

test('newlyOverdue: 직전 develop 끝 커밋 날 ≤ D < 오늘인 미완료 넘겨주기만 (push마다 한 번)', () => {
  const v = view([], [], '2026-11-30');
  assert.deepEqual(newlyOverdue(v, '2026-11-28').map((h) => h.item), ['V-2', 'E-1']);
  assert.deepEqual(newlyOverdue(v, '2026-11-29').map((h) => h.item), ['E-1']);
  // 같은 날 두 번째 push: 직전 커밋도 오늘이면 이미 알렸다
  assert.deepEqual(newlyOverdue(v, '2026-11-30'), []);
  assert.deepEqual(newlyOverdue(view([merged(1, 'Roadmap: V-2')], [], '2026-11-30'), '2026-11-28').map((h) => h.item), ['E-1']);
  assert.deepEqual(newlyOverdue(view([], [], '2026-11-30', { statusOk: false }), '2026-11-28'), []);
  const text = overdueMessage(newlyOverdue(v, '2026-11-29'));
  assert.match(text, /로드맵 넘겨주는 시점이 지났습니다/);
  assert.match(text, /- 11\/29 A→B \*\*E-1\*\* 러너 머지 \(항목 ⬜\) → 풀리는 작업: 기준선/);
  assert.equal(overdueMessage([]), null);
});

test('날짜는 서울 기준으로 센다 (Actions는 UTC)', () => {
  assert.equal(seoulToday(new Date('2026-11-27T15:30:00Z')), '2026-11-28');
  assert.equal(seoulToday(new Date('2026-11-27T14:59:00Z')), '2026-11-27');
  assert.equal(daysBetween('2026-11-30', '2026-12-02'), 2);
  assert.equal(daysBetween('2026-12-02', '2026-11-30'), -2);
});

test('fetchRoadmapPrs는 실패를 이유와 함께 돌려준다', async () => {
  const fake = (err, stdout = '', stderr = '') => (f, a, o, cb) => cb(err, stdout, stderr);
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(null, '[{"number":1}]') }), { ok: true, prs: [{ number: 1 }] });
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(null, '[]') }), { ok: true, prs: [] });
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(Object.assign(new Error('x'), { code: 'ENOENT' })) }), { ok: false, reason: 'gh 없음' });
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(Object.assign(new Error('x'), { killed: true })), timeout: 4000 }), { ok: false, reason: '4초 안에 응답 없음' });
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(null, '') }), { ok: false, reason: '응답 형식 오류' });
  assert.deepEqual(await fetchRoadmapPrs({ exec: fake(Object.assign(new Error('x'), { code: 1 }), '', 'HTTP 401\nmore') }), { ok: false, reason: 'gh 실패: HTTP 401' });
});

test('session-start는 로드맵 PR 조회를 autoSync 뒤에 시작하고 배너에 로드맵 줄을 넘긴다 (#67)', () => {
  const src = readFileSync(new URL('../../.claude/hooks/session-start.mjs', import.meta.url), 'utf8');
  const syncAt = src.indexOf('autoSync()');
  assert.ok(syncAt > 0 && src.indexOf('fetchRoadmapPrs(') > syncAt);
  assert.match(src, /roadmapLines,/);
  const banner = sessionBanner({ pc: { id: 'dh-ssafy', owner: '김동환', label: '김동환 교육장' }, branch: 'b', roadmapLines: ['🗺️ 로드맵: x'], myFeature: 'f' });
  assert.deepEqual(banner.split('\n').slice(-2), ['🗺️ 로드맵: x', '내 작업: f']);
});
