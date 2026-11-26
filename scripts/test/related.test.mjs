// 커밋 hook·pre-push의 검증 대상 선택 (H-1)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { HARNESS_DATA_FILES, areasOf, frontendCodeFiles, prePushPlan, protectedPushes, relatedBackendTests, workTreeMismatch, relatedHarnessTests } from '../lib/related.mjs';

const T = 'backend/src/test/java/com/vibenote/';
const M = 'backend/src/main/java/com/vibenote/';
const TESTS = [
  `${T}analysis/ConceptExtractorTest.java`,
  `${T}analysis/ConceptLinkServiceTest.java`,
  `${T}analysis/AnalysisControllerTest.java`,
  `${T}analysis/FakeLlmClient.java`,
  `${T}analysis/RepositoryArchiveFetcherLiveTest.java`,
  `${T}analysis/RepositoryArchiveFetcherTest.java`,
  `${T}auth/AuthServiceTest.java`,
  `${T}auth/LoginApiTest.java`,
  `${T}common/FlywayMigrationTest.java`,
  `${T}concept/ConceptListApiTest.java`,
];

test('영역: md만 바뀐 곳은 빼고 하네스 스크립트를 따로 본다', () => {
  assert.deepEqual(areasOf(['backend/CLAUDE.md', 'docs/a.md']), []);
  assert.deepEqual(areasOf(['backend/build.gradle', 'frontend/src/App.vue', 'scripts/lib/common.mjs']), ['be', 'fe', 'harness']);
  assert.deepEqual(areasOf(['.claude/hooks/guard-bash.mjs', '.githooks/pre-push']), ['harness']);
  // frontend/scripts의 래퍼는 fe 영역이다
  assert.deepEqual(areasOf(['frontend/scripts/run-oxlint.mjs']), ['fe']);
});

test('BE: 메인 클래스는 이름이 같은 접두어인 테스트를 고른다', () => {
  assert.deepEqual(relatedBackendTests([`${M}analysis/ConceptExtractor.java`], TESTS), ['ConceptExtractorTest']);
  // Impl 접미사는 떼고 찾는다
  assert.deepEqual(relatedBackendTests([`${M}auth/AuthServiceImpl.java`], TESTS), ['AuthServiceTest']);
  // 접두어 테스트가 없으면 같은 패키지의 테스트
  assert.deepEqual(relatedBackendTests([`${M}auth/JwtFilter.java`], TESTS), ['AuthServiceTest', 'LoginApiTest']);
});

test('BE: 테스트 지원 파일은 같은 패키지, 실제 LLM을 부르는 LiveTest는 고르지 않는다', () => {
  assert.deepEqual(relatedBackendTests([`${T}analysis/FakeLlmClient.java`], TESTS), [
    'AnalysisControllerTest',
    'ConceptExtractorTest',
    'ConceptLinkServiceTest',
    'RepositoryArchiveFetcherTest',
  ]);
  assert.deepEqual(relatedBackendTests([`${M}analysis/RepositoryArchiveFetcher.java`], TESTS), ['RepositoryArchiveFetcherTest']);
  assert.deepEqual(relatedBackendTests([`${T}analysis/RepositoryArchiveFetcherLiveTest.java`], TESTS), []);
});

test('BE: 마이그레이션·Mapper XML·설정 파일', () => {
  assert.deepEqual(relatedBackendTests(['backend/src/main/resources/db/migration/V30__x.sql'], TESTS), ['FlywayMigrationTest']);
  assert.deepEqual(relatedBackendTests(['backend/src/main/resources/mapper/ConceptMapper.xml'], TESTS), [
    'ConceptExtractorTest',
    'ConceptLinkServiceTest',
    'ConceptListApiTest',
  ]);
  // 빌드·설정 변경은 관련 테스트를 정하지 않는다 (컴파일만, 전체는 pre-push)
  assert.deepEqual(relatedBackendTests(['backend/build.gradle', 'backend/src/main/resources/application.yml'], TESTS), []);
});

test('FE: 코드 파일만 frontend/ 기준 경로로', () => {
  assert.deepEqual(
    frontendCodeFiles(['frontend/src/views/A.vue', 'frontend/src/api/x.js', 'frontend/README.md', 'frontend/public/logo.png', 'frontend/vite.config.js']),
    ['src/views/A.vue', 'src/api/x.js', 'vite.config.js'],
  );
});

test('하네스: 테스트 자신과, 바뀐 모듈을 import하는 테스트', () => {
  const files = { 'scripts/test/a.test.mjs': "import { x } from '../lib/common.mjs';", 'scripts/test/b.test.mjs': "import { y } from '../lib/sync.mjs';" };
  const testFiles = Object.keys(files);
  const read = (p) => files[p];
  assert.deepEqual(relatedHarnessTests(['scripts/lib/common.mjs'], testFiles, read), ['scripts/test/a.test.mjs']);
  assert.deepEqual(relatedHarnessTests(['scripts/test/b.test.mjs'], testFiles, read), ['scripts/test/b.test.mjs']);
  // 이름이 겹치는 다른 파일(lib/common.mjs ↔ x-common.mjs)은 고르지 않는다
  assert.deepEqual(relatedHarnessTests(['scripts/lib/x-common.mjs'], testFiles, read), []);
  // 테스트가 없는 hook은 고를 것이 없다 (전체는 pre-push)
  assert.deepEqual(relatedHarnessTests(['.claude/hooks/guard-bash.mjs'], testFiles, read), []);
  // 하네스가 읽는 문서(로드맵)는 그 파일을 파싱하는 테스트를 고른다
  assert.deepEqual(relatedHarnessTests(['docs/로드맵.md'], [...testFiles, 'scripts/test/roadmap.test.mjs'], read), ['scripts/test/roadmap.test.mjs']);
  assert.deepEqual(areasOf(['docs/로드맵.md']), ['harness']);
  assert.deepEqual(areasOf(['docs/기획서.md']), []);
  // 검증 캐시(verify.mjs recordVerified)의 미커밋 변경 검사도 같은 문서를 본다 (Codex 리뷰)
  assert.deepEqual(HARNESS_DATA_FILES, ['docs/로드맵.md']);
  const verifySrc = readFileSync(new URL('../verify.mjs', import.meta.url), 'utf8');
  assert.match(verifySrc, /harness: \[[^\]]*\.\.\.HARNESS_DATA_FILES\]/);
});

test('pre-push: wip는 건너뛰고, 같은 커밋을 이미 통과한 영역은 다시 돌리지 않는다', () => {
  const files = ['backend/src/main/java/A.java', 'frontend/src/A.vue'];
  assert.equal(prePushPlan({ tipSubject: 'wip(F-08): 교대', files, sha: 'abc' }).skip, 'wip');
  assert.equal(prePushPlan({ tipSubject: 'docs: 진행현황', files: ['docs/a.md'], sha: 'abc' }).skip, 'none');
  assert.deepEqual(prePushPlan({ tipSubject: 'feat(F-08): x', files, sha: 'abc', verified: { be: 'abc', fe: 'old' } }), {
    skip: null,
    targets: ['fe'],
    cached: ['be'],
  });
});

test('pre-push: main·develop으로 가는 push(삭제 포함)는 사람이든 Claude든 거부한다', () => {
  const Z = '0'.repeat(40);
  const line = (local, remote, sha = 'a'.repeat(40)) => [local, sha, remote, Z];
  assert.deepEqual(protectedPushes([line('refs/heads/feature/x', 'refs/heads/feature/x')]), []);
  assert.deepEqual(protectedPushes([line('HEAD', 'refs/heads/develop'), line('refs/heads/x', 'refs/heads/main')]), ['develop', 'main']);
  assert.deepEqual(protectedPushes([line('(delete)', 'refs/heads/develop', Z)]), ['develop']);
  assert.deepEqual(protectedPushes([line('refs/tags/v1', 'refs/tags/develop')]), []);
});

test('pre-push: 검증할 커밋이 HEAD가 아니거나 그 영역에 커밋 안 된 변경이 있으면 작업 트리로 검증할 수 없다 (Codex P1)', () => {
  const head = 'h'.repeat(40);
  assert.equal(workTreeMismatch({ head, dirtyFiles: [], pushes: [{ sha: head, targets: ['be'] }] }), null);
  assert.match(workTreeMismatch({ head, dirtyFiles: [], pushes: [{ sha: 'o'.repeat(40), targets: ['be'] }] }), /HEAD/);
  assert.match(workTreeMismatch({ head, dirtyFiles: ['backend/src/main/java/A.java'], pushes: [{ sha: head, targets: ['be'] }] }), /backend\/src\/main\/java\/A\.java/);
  // 다른 영역·문서의 미커밋 변경, 검증할 것이 없는 push는 상관없다
  assert.equal(workTreeMismatch({ head, dirtyFiles: ['frontend/src/A.vue', 'docs/handoff/NOW-dh.md'], pushes: [{ sha: head, targets: ['be'] }] }), null);
  assert.equal(workTreeMismatch({ head, dirtyFiles: ['backend/A.java'], pushes: [{ sha: 'o'.repeat(40), targets: [] }] }), null);
});
