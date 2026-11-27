// verify.mjs 인자 해석과 검증 기록 판정 (HP-4)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { parseVerifyArgs, shouldRecord } from '../lib/verify.mjs';

test('HP-4 AC1: be --tests는 여러 개를 받고, 부분 실행이라 검증 기록을 남기지 않는다', () => {
  const p = parseVerifyArgs(['be', '--tests', 'ConceptExtractorTest', '--tests', 'Eval*']);
  assert.equal(p.error, null);
  assert.deepEqual(p.targets, ['be']);
  assert.deepEqual(p.tests, ['ConceptExtractorTest', 'Eval*']);
  assert.equal(p.record, false);
  // --tests=패턴 형식도, 영역을 생략하면 be로
  assert.deepEqual(parseVerifyArgs(['--tests=AuthServiceTest']), { targets: ['be'], tests: ['AuthServiceTest'], fix: false, staged: false, record: false, error: null });
});

test('HP-4 AC1: be 외 영역·--staged·패턴 없는 --tests는 사용법 오류', () => {
  for (const args of [['fe', '--tests', 'X'], ['all', '--tests', 'X'], ['be', 'harness', '--tests', 'X'], ['be', '--staged', '--tests', 'X'], ['be', '--tests'], ['be', '--tests', '--fix']]) {
    assert.match(parseVerifyArgs(args).error || '', /--tests/, args.join(' '));
  }
});

test('HP-4 AC1: --tests 없는 기존 사용법은 그대로 (기본 be·fe, 전체 검증만 기록)', () => {
  assert.deepEqual(parseVerifyArgs([]), { targets: ['be', 'fe'], tests: [], fix: false, staged: false, record: true, error: null });
  assert.deepEqual(parseVerifyArgs(['all', 'harness']).targets, ['harness', 'be', 'fe']);
  assert.deepEqual(parseVerifyArgs(['fe', '--fix']), { targets: ['fe'], tests: [], fix: true, staged: false, record: false, error: null });
  assert.equal(parseVerifyArgs(['harness', '--staged']).record, false);
  assert.equal(parseVerifyArgs(['harness', '--staged']).staged, true);
});

test('HP-4 AC1: verifier 에이전트는 gradlew --tests를 직접 부르지 않고 verify.mjs be --tests를 쓴다', () => {
  const src = readFileSync(new URL('../../.claude/agents/verifier.md', import.meta.url), 'utf8');
  assert.match(src, /node scripts\/verify\.mjs be --tests/);
  assert.doesNotMatch(src, /gradlew(\.bat)? test[^\n]*--tests/);
});

test('HP-4 AC2: 검증 중 HEAD가 바뀌었으면 기록하지 않는다 (#133)', () => {
  assert.deepEqual(shouldRecord({ startHead: 'a1', endHead: 'a1', dirty: false }), { record: true, reason: null });
  assert.deepEqual(shouldRecord({ startHead: 'a1', endHead: 'b2', dirty: false }), { record: false, reason: 'head-changed' });
  assert.deepEqual(shouldRecord({ startHead: 'a1', endHead: 'a1', dirty: true }), { record: false, reason: 'dirty' });
  assert.deepEqual(shouldRecord({ startHead: '', endHead: '', dirty: false }), { record: false, reason: 'no-head' });
});
