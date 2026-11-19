// Flyway 번호 검사 (두 사람이 각자 V6을 만드는 경우)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { compareVersions, maxVersion, migrationProblems, versionOf } from '../lib/migrations.mjs';

test('버전 파일 이름을 해석한다', () => {
  assert.deepEqual(versionOf('V12__desc.sql'), [12]);
  assert.deepEqual(versionOf('backend/x/V1_2__a.sql'), [1, 2]);
  assert.equal(versionOf('R__seed_concepts.sql'), null);
  assert.ok(compareVersions([10], [9]) > 0);
  assert.equal(compareVersions([1], [1, 0]), 0);
  assert.deepEqual(maxVersion(['V1__a.sql', 'V10__b.sql', 'V9__c.sql', 'R__s.sql']), [10]);
});

const BASE = ['R__seed.sql', 'V1__init.sql', 'V5__flow.sql'];

test('base 다음 번호를 추가하면 통과', () => {
  assert.deepEqual(migrationProblems([...BASE, 'V6__card.sql'], BASE), []);
});

test('같은 번호가 둘이면 실패 (상대 PR이 먼저 V6을 머지)', () => {
  const base = [...BASE, 'V6__quiz.sql'];
  const problems = migrationProblems([...base, 'V6__card.sql'], base);
  assert.equal(problems.length, 2);
  assert.match(problems[0], /버전 6 중복/);
  assert.match(problems[1], /V6__card\.sql: base의 최신 버전 V6 이하/);
});

test('base보다 낮은 번호를 새로 추가하면 실패 (V7 머지 뒤 V6 추가)', () => {
  const base = [...BASE, 'V6__a.sql', 'V7__b.sql'];
  assert.match(migrationProblems([...base, 'V6_1__c.sql'], base)[0], /V6_1__c\.sql: base의 최신 버전 V7 이하/);
});
