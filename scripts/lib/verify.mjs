// verify.mjs의 인자 해석과 검증 기록 판정. git·파일 호출 없이 계산하는 부분이다 (scripts/test/verify.test.mjs).

const USAGE_TESTS = '--tests는 be에만 쓰고 --staged와 함께 쓰지 않는다: node scripts/verify.mjs be --tests <패턴> [--tests <패턴>…]';

/**
 * verify.mjs 인자 → { targets, tests, fix, staged, record, error }.
 * - 영역이 없거나 all이면 be·fe를 더한다. --tests만 주면 be
 * - --tests <패턴>(또는 --tests=<패턴>)은 여러 번 줄 수 있고 be 전용이다 (H-9: verifier가 gradlew를 직접 불러 잠금을 우회하지 않게)
 * - record: 전체 검증만 .harness/verified.json에 남긴다 (--staged·--fix·--tests는 일부만 본다)
 */
export function parseVerifyArgs(args) {
  const tests = [];
  const targets = [];
  let fix = false;
  let staged = false;
  let error = null;
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--fix') fix = true;
    else if (a === '--staged') staged = true;
    else if (a === '--tests' || a.startsWith('--tests=')) {
      const pattern = a === '--tests' ? args[++i] : a.slice('--tests='.length);
      if (!pattern || pattern.startsWith('--')) error = USAGE_TESTS;
      else tests.push(pattern);
    } else if (!a.startsWith('--')) targets.push(a);
  }
  if (tests.length || error) {
    if (!targets.length) targets.push('be');
    if (targets.some((t) => t !== 'be') || staged) error = USAGE_TESTS;
    return { targets, tests, fix, staged, record: false, error };
  }
  const resolved = staged || (targets.length && !targets.includes('all')) ? targets : [...new Set([...targets.filter((t) => t !== 'all'), 'be', 'fe'])];
  return { targets: resolved, tests, fix, staged, record: !staged && !fix, error };
}

/**
 * 검증을 통과한 영역을 verified.json에 남길지 (#133).
 * 검증하는 동안 HEAD가 바뀌었으면(다른 커밋이 생김) 검증한 커밋이 아니므로, 그 영역에 커밋 안 된 변경이 있으면 HEAD와 내용이 달라 남기지 않는다.
 */
export function shouldRecord({ startHead, endHead, dirty }) {
  if (!startHead || !endHead) return { record: false, reason: 'no-head' };
  if (startHead !== endHead) return { record: false, reason: 'head-changed' };
  if (dirty) return { record: false, reason: 'dirty' };
  return { record: true, reason: null };
}
