// NOW·작업로그 자동 초안 (H-1)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { commitsLine, lastEntryAt, logEntry, touchNowHeader, undoMsysPathConv, verifiedLine } from '../lib/records.mjs';

test('NOW 갱신 줄만 바꾸고, 없으면 맨 위에 넣는다', () => {
  assert.equal(touchNowHeader('# NOW\n갱신: 옛날\n본문', '갱신: 새것'), '# NOW\n갱신: 새것\n본문');
  assert.equal(touchNowHeader('본문', '갱신: 새것'), '갱신: 새것\n\n본문');
});

test('내 마지막 작업로그 시각 (상대 항목은 건너뜀)', () => {
  const log = [
    '# 작업로그',
    '## 2026-11-25 09:30 · whans1996 · dh-ssafy · feature/a\n- 한 일: a',
    '## 2026-11-25 10:00 · JoonseobKim · js-home · feature/b\n- 한 일: b',
  ].join('\n\n');
  assert.equal(lastEntryAt(log, 'dh'), '2026-11-25 09:30');
  assert.equal(lastEntryAt(log, 'js'), '2026-11-25 10:00');
  assert.equal(lastEntryAt('', 'dh'), null);
});

test('작업로그 항목: 메모 3줄 + 자동 커밋 줄, 여러 줄 메모는 한 줄로', () => {
  const commits = [
    { sha: '3d57f6baaaa', subject: 'fix(harness): a' },
    { sha: 'd78a032bbbb', subject: 'feat(harness): b' },
  ];
  assert.equal(
    logEntry({ header: '2026-11-25 11:00 · whans1996 · dh-ssafy · feature/x', did: 'H-1 ①②\n③', next: '④', blocked: '', commits }),
    [
      '## 2026-11-25 11:00 · whans1996 · dh-ssafy · feature/x',
      '- 한 일: H-1 ①② / ③',
      '- 커밋: 2개 — fix(harness): a (3d57f6b) · feat(harness): b (d78a032)',
      '- 다음 할 일: ④',
      '- 막힌 점: 없음',
    ].join('\n'),
  );
  assert.match(logEntry({ header: 'h', did: 'a', next: 'b', ai: 'Codex가 P1을 잡음' }), /- AI 활용: Codex가 P1을 잡음$/);
  // 한 일·다음 할 일이 없으면 만들지 않는다
  assert.equal(logEntry({ header: 'h', did: ' ', next: 'b' }), null);
  assert.equal(logEntry({ header: 'h', did: 'a', next: '' }), null);
});

test('커밋 줄은 많으면 줄인다', () => {
  const commits = Array.from({ length: 8 }, (_, i) => ({ sha: `${i}`.repeat(7), subject: `c${i}` }));
  assert.match(commitsLine(commits, 2), /^8개 — c0 \(0000000\) · c1 \(1111111\) · 외 6개$/);
  assert.equal(commitsLine([]), '없음');
});

test('Git Bash가 Git 설치 경로로 바꾼 "/명령" 메모를 되돌린다 (#231)', () => {
  const env = { MSYSTEM: 'UCRT64', EXEPATH: 'C:\\Program Files\\Git\\bin' };
  // 11-26 실제로 작업로그에 남은 값
  assert.equal(undoMsysPathConv('C:/Program Files/Git/pr-review → PR(Closes #179)', env), '/pr-review → PR(Closes #179)');
  assert.equal(undoMsysPathConv('C:/Program Files/Git/', env), '/');
  // 설치 경로가 달라도 EXEPATH 기준 (usr/bin·끝 역슬래시도)
  assert.equal(undoMsysPathConv('D:/Tools/Git/spec K-3a', { MSYSTEM: 'MINGW64', EXEPATH: 'D:\\Tools\\Git\\usr\\bin\\' }), '/spec K-3a');
  // EXEPATH가 없으면 기본 설치 경로, 대소문자 무시
  assert.equal(undoMsysPathConv('c:/program files/Git/handoff', { MSYSTEM: 'MINGW64' }), '/handoff');
  // 바뀌지 않은 값·중간의 /명령·다른 경로는 그대로
  assert.equal(undoMsysPathConv('다음: /spec K-3a', env), '다음: /spec K-3a');
  assert.equal(undoMsysPathConv('C:/Users/me/x', env), 'C:/Users/me/x');
  assert.equal(undoMsysPathConv('', env), '');
  // MSYS 밖(PowerShell·macOS·CI)에서는 손대지 않는다
  assert.equal(undoMsysPathConv('C:/Program Files/Git/pr-review', {}), 'C:/Program Files/Git/pr-review');
});

test('stamp·handoff는 메모 인자에 MSYS 되돌림을 쓴다 (#231)', () => {
  for (const f of ['stamp.mjs', 'handoff.mjs']) {
    const src = readFileSync(new URL(`../${f}`, import.meta.url), 'utf8');
    assert.match(src, /undoMsysPathConv\(args\[/, f);
  }
});

test('검증 상태: HEAD가 verified.json에 있는 영역만 통과', () => {
  assert.equal(verifiedLine({ be: 'h1', harness: 'old' }, 'h1', ['be', 'harness']), 'be ✅ HEAD 통과 · harness ⬜ HEAD 미검증');
  assert.equal(verifiedLine({}, 'h1', []), '검증할 코드 변경 없음');
});
