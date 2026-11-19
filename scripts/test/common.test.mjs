// 사람별 NOW 파일·작업로그·PC 전환 판정 (11-19 병렬 전환)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  lastWorklogEntryOf,
  nowRelPath,
  otherOwnerKey,
  ownerKey,
  previousPc,
  recordStamp,
  worklogEntries,
} from '../lib/common.mjs';

test('PC id 접두로 사람 key를 정한다', () => {
  assert.equal(ownerKey('dh-ssafy'), 'dh');
  assert.equal(ownerKey('js-home'), 'js');
  assert.equal(ownerKey('unknown-pc'), null);
  assert.equal(ownerKey(null), null);
  assert.equal(otherOwnerKey('dh'), 'js');
  assert.equal(nowRelPath('js'), 'docs/handoff/NOW-js.md');
});

test('기록 머리 줄은 시각·작업자·PC·브랜치 순서다', () => {
  const s = recordStamp({ date: new Date(2026, 10, 19, 9, 5), author: 'whans1996', pcId: 'dh-ssafy', branch: 'feature/F-08-x' });
  assert.equal(s, '2026-11-19 09:05 · whans1996 · dh-ssafy · feature/F-08-x');
  assert.match(recordStamp({ author: 'a' }), / · a · unknown-pc · \(detached\)$/);
});

const LOG = `# 작업로그

## 2026-11-19 09:00 · whans1996 · dh-ssafy · feature/F-08-a
- 한 일: A

## 2026-11-19 10:00 · JoonseobKim · js-home · feature/F-09-b
- 한 일: B

## 2026-11-19 11:00 · JoonseobKim · js-ssafy · feature/F-09-b
- 한 일: B2
`;

test('작업로그에서 내 마지막 항목만 고른다 (상대 항목은 건너뜀)', () => {
  const entries = worklogEntries(LOG);
  assert.equal(entries.length, 3);
  assert.equal(lastWorklogEntryOf(entries, 'dh').pcId, 'dh-ssafy');
  assert.equal(lastWorklogEntryOf(entries, 'js').pcId, 'js-ssafy');
});

test('직전 PC는 내 NOW 갱신 줄과 내 작업로그 중 최근 것', () => {
  const knownIds = ['dh-ssafy', 'dh-home', 'js-ssafy', 'js-home'];
  const nowText = '# NOW\n\n갱신: 2026-11-19 12:00 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/F-08-a\n';
  const entry = lastWorklogEntryOf(worklogEntries(LOG), 'dh');
  assert.equal(previousPc({ nowText, entry, knownIds }), 'dh-home');
  // 작업로그가 더 최근이면 작업로그 PC
  const older = nowText.replace('12:00', '08:00');
  assert.equal(previousPc({ nowText: older, entry, knownIds }), 'dh-ssafy');
  // 상대가 develop에 남긴 작업로그는 판정에 쓰지 않는다 (병렬 작업 오탐 방지)
  assert.equal(previousPc({ nowText: null, entry: lastWorklogEntryOf(worklogEntries(LOG), 'dh'), knownIds }), 'dh-ssafy');
  assert.equal(previousPc({ nowText: null, entry: null, knownIds }), null);
});

test('다른 사람 PC에서 쓴 내 NOW 갱신 줄은 PC 전환으로 보지 않는다', () => {
  const knownIds = ['dh-ssafy', 'js-ssafy'];
  const nowText = '갱신: 2026-11-19 10:26 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: x (초기 생성)\n';
  assert.equal(previousPc({ nowText, entry: null, knownIds, key: 'js' }), null);
  assert.equal(previousPc({ nowText, entry: null, knownIds, key: 'dh' }), 'dh-ssafy');
});
