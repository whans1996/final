// CODEOWNERS 기반 영역 판정
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { areaOf, ownersOf, parseCodeowners, patternToRegex } from '../lib/owners.mjs';

const PCS = {
  'dh-ssafy': { owner: '김동환', github: 'whans1996' },
  'js-home': { owner: '김준섭', github: 'JoonseobKim' },
};
const RULES = parseCodeowners(`
# 주석
**/vibenote/common/          @whans1996 @JoonseobKim
**/vibenote/analysis/        @whans1996
/frontend/src/views/Analysis*  @whans1996
**/vibenote/follow/          @JoonseobKim   # 끝 주석
/scripts/                    @whans1996
/scripts/lib/pcs.json        @whans1996 @JoonseobKim
`);

test('패턴: 루트 고정, 어느 깊이든, 디렉터리 아래 전부', () => {
  assert.ok(patternToRegex('/scripts/').test('scripts/lib/a.mjs'));
  assert.ok(!patternToRegex('/scripts/').test('frontend/scripts/a.mjs'));
  assert.ok(patternToRegex('**/vibenote/quiz/').test('backend/src/main/java/com/vibenote/quiz/QuizService.java'));
  assert.ok(patternToRegex('**/vibenote/quiz/').test('backend/src/test/java/com/vibenote/quiz/a/B.java'));
  assert.ok(!patternToRegex('**/vibenote/quiz/').test('backend/src/main/java/com/vibenote/quizzes/A.java'));
  assert.ok(patternToRegex('/frontend/src/views/Analysis*').test('frontend/src/views/AnalysisListView.vue'));
  assert.ok(!patternToRegex('/frontend/src/views/Analysis*').test('frontend/src/views/HomeView.vue'));
  assert.ok(patternToRegex('CODEOWNERS').test('.github/CODEOWNERS'));
});

test('뒤에 나온 규칙이 이기고 끝 주석은 무시한다', () => {
  assert.deepEqual(ownersOf('scripts/lib/pcs.json', RULES), ['whans1996', 'JoonseobKim']);
  assert.deepEqual(ownersOf('scripts/lib/common.mjs', RULES), ['whans1996']);
  assert.deepEqual(ownersOf('backend/src/main/java/com/vibenote/follow/FollowService.java', RULES), ['JoonseobKim']);
  assert.deepEqual(ownersOf('docs/기획서.md', RULES), []);
});

test('영역: 내 것, 상대 것, 공통, 지정 없음 (계정 대소문자 무시)', () => {
  const java = (pkg) => `backend/src/main/java/com/vibenote/${pkg}/X.java`;
  assert.equal(areaOf(java('analysis'), 'dh', RULES, PCS), 'mine');
  assert.equal(areaOf(java('analysis'), 'js', RULES, PCS), 'peer');
  assert.equal(areaOf(java('follow'), 'dh', RULES, PCS), 'peer');
  assert.equal(areaOf(java('common'), 'js', RULES, PCS), 'common');
  assert.equal(areaOf('docs/기획서.md', 'dh', RULES, PCS), 'none');
  const lower = parseCodeowners('/a/ @joonseobkim');
  assert.equal(areaOf('a/b.js', 'js', lower, PCS), 'mine');
});
