// 사람별 NOW 파일·작업로그·PC 전환 판정 (11-19 병렬 전환)
import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import {
  lastWorklogEntryOf,
  nowFileOf,
  nowRelPath,
  otherOwnerKey,
  ownerKey,
  previousPc,
  readWorklog,
  recordStamp,
  sessionBanner,
  worklogEntries,
} from '../lib/common.mjs';
import { nowFormatProblems } from '../lib/records.mjs';

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
  // 상대 PC 항목이 들어와도 내 key로 걸러 판정에 쓰지 않는다 (병렬 작업 오탐 방지)
  const theirs = lastWorklogEntryOf(worklogEntries(LOG), 'js');
  assert.equal(previousPc({ nowText: null, entry: theirs, knownIds, key: 'dh' }), null);
  assert.equal(previousPc({ nowText: null, entry: null, knownIds }), null);
});

test('unknown-pc 항목은 작업자 계정이 내 PC 계정일 때만 내 것, 시각이 가장 늦은 항목을 고른다', () => {
  const log = `## 2026-11-19 12:00 · JoonseobKim · unknown-pc · feature/F-09-b
- 미등록 PC
## 2026-11-19 08:00 · whans1996 · dh-home · feature/F-08-a
- 늦게 union 머지됐지만 이른 항목
`;
  const pcs = { 'dh-ssafy': { github: 'whans1996' }, 'js-ssafy': { github: 'JoonseobKim' } };
  const entries = [...worklogEntries(LOG), ...worklogEntries(log)];
  assert.equal(lastWorklogEntryOf(entries, 'dh', pcs).at, '2026-11-19 09:00');
  assert.equal(lastWorklogEntryOf(entries, 'js', pcs).pcId, 'unknown-pc');
  assert.equal(entries.at(-1).author, 'whans1996');
});

test('다른 사람 PC에서 쓴 내 NOW 갱신 줄은 PC 전환으로 보지 않는다', () => {
  const knownIds = ['dh-ssafy', 'js-ssafy'];
  const nowText = '갱신: 2026-11-19 10:26 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: x (초기 생성)\n';
  assert.equal(previousPc({ nowText, entry: null, knownIds, key: 'js' }), null);
  assert.equal(previousPc({ nowText, entry: null, knownIds, key: 'dh' }), 'dh-ssafy');
});

test('세션 배너는 사람·PC·브랜치를 첫 줄에, 환경·주의·내/상대 작업을 다음 줄에 보여 준다', () => {
  const pc = { id: 'dh-ssafy', label: '김동환 SSAFY 교육장', owner: '김동환', hostname: 'h' };
  const b = sessionBanner({
    pc,
    branch: 'develop',
    warnings: ['보호 브랜치(develop)에 있습니다. 코드를 바꾸기 전에 브랜치를 만드세요.'],
    issueLine: '📌 이슈: 내 4 · 공통 3 (#16 설정 마스킹 외)',
    myFeature: '공통 기반 spec',
    peerName: '김준섭',
    peerFeature: '없음 — 착수 전',
  }).split('\n');
  assert.equal(b[0], '[바이브노트] 👤 김동환 · 🖥️ SSAFY 교육장 (dh-ssafy) · 🌿 develop');
  assert.equal(b[1], '환경 ✅');
  assert.equal(b[2], '⚠️ 보호 브랜치(develop)에 있습니다. 코드를 바꾸기 전에 브랜치를 만드세요.');
  assert.equal(b[3], '📌 이슈: 내 4 · 공통 3 (#16 설정 마스킹 외)');
  assert.equal(b[4], '내 작업: 공통 기반 spec');
  assert.equal(b[5], '김준섭: 없음 — 착수 전');
});

test('세션 배너: 미등록 PC는 setup 안내, 환경 문제는 항목을 나열, 긴 기능명은 자른다', () => {
  const b = sessionBanner({ pc: { id: null, hostname: 'PC1' }, branch: 'feature/x', envBad: ['⚠️ Docker'], myFeature: 'a'.repeat(200) }).split('\n');
  assert.ok(b[0].includes('미등록 PC (hostname PC1) → node scripts/setup.mjs --pc <id>'));
  assert.equal(b[1], '환경: ⚠️ Docker');
  assert.ok(b[2].length <= '내 작업: '.length + 90 && b[2].endsWith('…'));
});

test('세션 배너: label·owner가 없으면 id로, 상대 이름이 없으면 "상대"로 표시한다', () => {
  const b = sessionBanner({ pc: { id: 'dh-x' }, branch: 'develop', warnings: ['마침표 없는 경고'], peerFeature: 'F-09' }).split('\n');
  assert.equal(b[0], '[바이브노트] 👤 ? · 🖥️ dh-x (dh-x) · 🌿 develop');
  assert.equal(b[2], '⚠️ 마침표 없는 경고');
  assert.equal(b[3], '상대: F-09');
});

test('NOW 파일마다 맨 앞이 "갱신:"인 줄이 있다 (previousPc·peer-watch·handoff가 이 줄을 읽는다)', () => {
  for (const key of ['dh', 'js']) {
    const text = readFileSync(nowFileOf(key), 'utf8');
    assert.match(text, /^갱신: \d{4}-\d{2}-\d{2} \d{2}:\d{2}/m, nowRelPath(key));
    // 제목·필수 섹션까지 (HP-4 AC3: NOW 파일 커밋은 커밋 hook에서 이 테스트를 돈다)
    assert.deepEqual(nowFormatProblems(text), [], nowRelPath(key));
  }
});

test('세션 배너: 자동 동기화 결과는 경고 다음 줄에 🔄로 보여 준다', () => {
  const b = sessionBanner({
    pc: { id: 'js-home', owner: '김준섭', label: '김준섭 집' },
    branch: 'feature/x',
    warnings: ['경고'],
    syncLines: ['- ✅ origin/develop의 새 커밋 2개를 자동으로 합쳤습니다.'],
  }).split('\n');
  assert.equal(b[2], '⚠️ 경고');
  assert.equal(b[3], '🔄 ✅ origin/develop의 새 커밋 2개를 자동으로 합쳤습니다.');
});

test('HP-5 AC5 readWorklog는 작업로그와 가장 최근 보관 파일만 읽는다 (보관 파일에만 있는 내 항목, 직전 PC)', () => {
  const dir = mkdtempSync(join(tmpdir(), 'common-worklog-'));
  try {
    const file = join(dir, '작업로그.md');
    const archiveDir = join(dir, '기록');
    // 보관 파일도 작업로그도 없으면 빈 문자열
    assert.equal(readWorklog({ file, archiveDir }), '');
    mkdirSync(archiveDir);
    writeFileSync(join(archiveDir, '작업로그-W1.md'), '# 작업로그 W1\n\n## 2026-11-20 09:00 · whans1996 · dh-ssafy · feature/old\n- 한 일: 오래된 주\n');
    writeFileSync(join(archiveDir, '작업로그-W2.md'), '# 작업로그 W2\n\n## 2026-11-30 21:00 · whans1996 · dh-home · feature/a\n- 한 일: 지난 주 마지막\n');
    writeFileSync(file, '# 작업로그\n\n## 2026-12-01 09:00 · JoonseobKim · js-ssafy · feature/b\n- 한 일: 상대 이번 주\n');
    const text = readWorklog({ file, archiveDir });
    assert.doesNotMatch(text, /오래된 주/);
    const mine = lastWorklogEntryOf(worklogEntries(text), 'dh');
    assert.equal(mine.at, '2026-11-30 21:00');
    assert.equal(previousPc({ nowText: null, entry: mine, knownIds: ['dh-ssafy', 'dh-home'], key: 'dh' }), 'dh-home');
    assert.equal(lastWorklogEntryOf(worklogEntries(text), 'js').pcId, 'js-ssafy');
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('HP-5 AC5 session-start·stamp는 readWorklog로 읽고, 항목 추가는 작업로그 파일 끝에 한다', () => {
  const hook = readFileSync(new URL('../../.claude/hooks/session-start.mjs', import.meta.url), 'utf8');
  const stamp = readFileSync(new URL('../stamp.mjs', import.meta.url), 'utf8');
  assert.match(hook, /worklogEntries\(readWorklog\(\)\)/);
  assert.match(stamp, /lastEntryAt\(readWorklog\(\)/);
  assert.match(stamp, /appendFileSync\(WORKLOG_FILE/);
});
