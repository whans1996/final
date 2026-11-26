// 바뀐 파일에서 검증 영역과 "관련 테스트"를 고른다 (H-1: 커밋 hook은 빠른 검사만, 전체는 pre-push·CI).
// git·파일 시스템 호출 없이 계산하는 부분이다 (scripts/test/related.test.mjs).
import { posix } from 'node:path';

const HARNESS_RE = /^(scripts|\.claude\/hooks|\.githooks)\/.*\.mjs$/;
// 하네스가 읽는 문서: 형식이 깨지면 표시가 틀리므로 하네스 영역으로 보고 그 파일을 파싱하는 테스트를 고른다 (로드맵 R-1)
const HARNESS_DATA = { 'docs/로드맵.md': 'scripts/test/roadmap.test.mjs' };
/** 하네스 영역에 드는 문서 경로 (verify.mjs가 검증 캐시의 미커밋 변경 검사에 쓴다, Codex 리뷰) */
export const HARNESS_DATA_FILES = Object.keys(HARNESS_DATA);
const isHarness = (f) => HARNESS_RE.test(f) || Object.hasOwn(HARNESS_DATA, f);
// 실제 Gemini·Ollama를 부르는 테스트는 `test` 태스크에서 빠진다 (build.gradle 태그 제외). 필터에 넣으면 "No tests found"로 실패한다
const LIVE_TEST_RE = /LiveTest$/;
const BE_TEST_DIR = 'backend/src/test/java/';
const BE_MAIN_DIR = 'backend/src/main/java/';
const FE_CODE_RE = /^frontend\/(src|scripts)\/.*\.(js|mjs|vue|ts|css)$|^frontend\/[^/]+\.(js|mjs|json)$/;

/** 바뀐 파일이 걸친 검증 영역 (verify.mjs 대상 이름). md만 바뀐 영역은 빼고, frontend/scripts의 하네스 래퍼는 fe로 본다 */
export function areasOf(files) {
  const areas = [];
  if (files.some((f) => f.startsWith('backend/') && !f.endsWith('.md'))) areas.push('be');
  if (files.some((f) => f.startsWith('frontend/') && !f.endsWith('.md'))) areas.push('fe');
  if (files.some(isHarness)) areas.push('harness');
  return areas;
}

const className = (path) => posix.basename(path).replace(/\.java$/, '');
const dirOf = (path) => posix.dirname(path);

/**
 * 바뀐 BE 파일과 관련된 테스트 클래스 이름 (Gradle `--tests` 필터용, 정렬됨).
 * - 테스트 클래스(*Test.java): 그 자신
 * - 테스트 지원 파일(FakeXxx, XxxTestSupport 등): 같은 패키지의 테스트
 * - 메인 클래스 X: 이름이 X로 시작하는 테스트(X·XImpl → XTest·XApiTest). 없으면 같은 패키지의 테스트
 * - Flyway 마이그레이션: FlywayMigrationTest
 * - MyBatis XML(XxxMapper.xml): Xxx로 시작하는 테스트
 * 그 밖의 파일(build.gradle, application.yml, 프롬프트 등)은 관련 테스트를 정하지 않는다. 컴파일만 하고 전체는 pre-push가 돈다.
 */
export function relatedBackendTests(files, testFiles) {
  const tests = testFiles.filter((f) => f.startsWith(BE_TEST_DIR) && /Test\.java$/.test(f) && !LIVE_TEST_RE.test(className(f)));
  const picked = new Set();
  const addPrefix = (prefix) => {
    let found = false;
    for (const t of tests) {
      if (className(t).startsWith(prefix)) {
        picked.add(className(t));
        found = true;
      }
    }
    return found;
  };
  const addPackage = (testDir) => {
    for (const t of tests) if (dirOf(t) === testDir) picked.add(className(t));
  };
  for (const f of files) {
    if (f.startsWith(BE_TEST_DIR) && f.endsWith('.java')) {
      if (LIVE_TEST_RE.test(className(f))) continue; // 컴파일만 한다
      if (tests.includes(f)) picked.add(className(f));
      else addPackage(dirOf(f));
    } else if (f.startsWith(BE_MAIN_DIR) && f.endsWith('.java')) {
      const name = className(f).replace(/Impl$/, '');
      if (!addPrefix(name)) addPackage(BE_TEST_DIR + dirOf(f).slice(BE_MAIN_DIR.length));
    } else if (/^backend\/src\/main\/resources\/db\/migration\//.test(f)) {
      addPrefix('FlywayMigrationTest');
    } else if (/^backend\/src\/main\/resources\/mapper\/.*Mapper\.xml$/.test(f)) {
      addPrefix(posix.basename(f).replace(/Mapper\.xml$/, ''));
    }
  }
  return [...picked].sort();
}

/** 린트·관련 테스트 대상 FE 파일 (frontend/ 기준 상대 경로) */
export function frontendCodeFiles(files) {
  return files.filter((f) => FE_CODE_RE.test(f)).map((f) => f.slice('frontend/'.length));
}

/**
 * 바뀐 하네스 파일과 관련된 단위 테스트 파일 (scripts/test/*.test.mjs, 정렬됨).
 * 테스트 파일 자신과, 바뀐 파일 이름(확장자 제외)을 import 문에 쓰는 테스트를 고른다.
 * readTest(path)는 테스트 파일 내용을 돌려준다.
 */
export function relatedHarnessTests(files, testFiles, readTest) {
  const picked = new Set();
  for (const f of files) if (HARNESS_DATA[f] && testFiles.includes(HARNESS_DATA[f])) picked.add(HARNESS_DATA[f]);
  const changed = files.filter((f) => HARNESS_RE.test(f));
  for (const f of changed) if (testFiles.includes(f)) picked.add(f);
  const names = changed.filter((f) => !testFiles.includes(f)).map((f) => posix.basename(f));
  if (!names.length) return [...picked].sort();
  for (const t of testFiles) {
    const imports = (readTest(t) || '').match(/from\s+['"][^'"]+['"]/g) || [];
    if (imports.some((line) => names.some((n) => line.includes(`/${n}'`) || line.includes(`/${n}"`)))) picked.add(t);
  }
  return [...picked].sort();
}

/**
 * pre-push에서 돌릴 verify 대상.
 * - tip 커밋이 wip면 건너뛴다 (교대 push, handoff.mjs)
 * - files: 이번 push로 바뀐 파일 중 develop과 내용이 다른 것 (develop을 합쳐 들어온 상대 변경은 상대 PR·CI가 검사했다)
 * - verified[영역] === sha면 같은 커밋을 깨끗한 작업 트리에서 이미 통과했으므로 다시 돌리지 않는다 (verify.mjs가 남김)
 */
export function prePushPlan({ tipSubject, files, verified = {}, sha }) {
  if (/^wip\(/.test(tipSubject || '')) return { skip: 'wip', targets: [], cached: [] };
  const areas = areasOf(files);
  const cached = areas.filter((a) => sha && verified[a] === sha);
  return { skip: areas.length ? null : 'none', targets: areas.filter((a) => !cached.includes(a)), cached };
}

/**
 * pre-push stdin 줄([local ref, local sha, remote ref, remote sha]) 중 main·develop으로 가는 것(삭제 포함)의 브랜치 이름.
 * guard-bash는 Claude 명령만 보고 `git push`(HEAD)처럼 지금 브랜치로 가는 push는 모른다. 사람의 push도 여기서 막는다 (규칙 1).
 */
export function protectedPushes(updates, protectedBranches = ['main', 'develop']) {
  return updates.map((u) => String(u[2] || '').replace(/^refs\/heads\//, '')).filter((b, k) => /^refs\/heads\//.test(updates[k][2]) && protectedBranches.includes(b));
}

/**
 * pre-push는 작업 트리에서 verify를 돈다. 검증할 커밋이 HEAD가 아니거나 그 영역에 커밋 안 된 변경이 있으면
 * 작업 트리의 결과가 push하는 커밋의 결과가 아니므로 거부 사유를 돌려준다 (PR #138 Codex P1). 문제없으면 null.
 * pushes: [{ sha, targets }] (prePushPlan의 targets), dirtyFiles: `git status --porcelain`의 경로
 */
export function workTreeMismatch({ head, dirtyFiles, pushes }) {
  const need = pushes.filter((p) => p.targets.length);
  const other = need.find((p) => p.sha !== head);
  if (other) return `push하려는 커밋 ${other.sha.slice(0, 7)}이 지금 체크아웃(HEAD ${head.slice(0, 7)})과 달라 검증할 수 없습니다. 그 브랜치로 switch한 뒤 push하세요.`;
  const targets = new Set(need.flatMap((p) => p.targets));
  const dirty = dirtyFiles.filter((f) => areasOf([f]).some((a) => targets.has(a)));
  if (dirty.length) return `검증할 영역에 커밋 안 된 변경이 있어 push하는 커밋을 검증할 수 없습니다: ${dirty.slice(0, 5).join(', ')}${dirty.length > 5 ? ' 외' : ''}. 커밋하거나 되돌린 뒤 push하세요.`;
  return null;
}
