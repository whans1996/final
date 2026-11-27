// NOW·작업로그 자동 초안 (H-1)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { commitsLine, lastEntryAt, logEntry, nowFormatProblems, replaceNowBody, touchNowHeader, undoMsysPathConv, verifiedLine } from '../lib/records.mjs';

test('NOW 갱신 줄만 바꾸고, 없으면 맨 위에 넣는다', () => {
  assert.equal(touchNowHeader('# NOW\n갱신: 옛날\n본문', '갱신: 새것'), '# NOW\n갱신: 새것\n본문');
  assert.equal(touchNowHeader('본문', '갱신: 새것'), '갱신: 새것\n\n본문');
});

const NOW_HEAD = '# NOW — 김동환 현재 작업 스냅샷\n\n> 안내 1. 머리 줄은 `stamp --now` 출력으로 쓴다. 갱신: 줄을 짐작해 쓰지 않는다\n> 안내 2';
const BODY = '## 현재 기능\n\nHP-4\n\n## 바로 다음 할 일\n\n1. AC5\n';

test('HP-4 AC4: NOW 본문 교체는 머리말을 지키고 갱신 줄을 새로 쓴다 (#166)', () => {
  const current = `${NOW_HEAD}\n갱신: 2026-11-26 10:00 · 옛날\n\n## 현재 기능\n\n옛 본문\n`;
  const r = replaceNowBody(current, BODY, '갱신: 2026-11-27 11:00 · 새것');
  assert.equal(r.error, null);
  assert.equal(r.text, `${NOW_HEAD}\n갱신: 2026-11-27 11:00 · 새것\n\n${BODY}`);
  assert.equal(r.warning, null);
  // 갱신 줄이 없어진(깨진) 파일도 머리말 뒤에 갱신 줄을 넣는다
  assert.equal(replaceNowBody(`${NOW_HEAD}\n\n## 현재 기능\n옛\n`, BODY, '갱신: 새것').text, `${NOW_HEAD}\n갱신: 새것\n\n${BODY}`);
  assert.deepEqual(nowFormatProblems(r.text), []);
});

test('HP-4 AC4: 갱신 줄이 든 본문, 필수 섹션이 없는 본문은 거부하고 30줄을 넘으면 경고만 한다', () => {
  const current = `${NOW_HEAD}\n갱신: 옛날\n\n${BODY}`;
  assert.match(replaceNowBody(current, `갱신: 짐작\n\n${BODY}`, '갱신: 새것').error, /갱신:/);
  assert.match(replaceNowBody(current, '## 현재 기능\n\nx\n', '갱신: 새것').error, /## 바로 다음 할 일/);
  assert.match(replaceNowBody(current, '## 바로 다음 할 일\n\nx\n', '갱신: 새것').error, /## 현재 기능/);
  assert.equal(replaceNowBody(current, '갱신: x', '갱신: 새것').text, null);
  const long = replaceNowBody(current, `${BODY}${'- 줄\n'.repeat(30)}`, '갱신: 새것');
  assert.equal(long.error, null);
  assert.match(long.warning, /30줄/);
});

test('HP-4 AC4: NOW 형식 검사 (제목·갱신 줄·필수 섹션)', () => {
  assert.deepEqual(nowFormatProblems(`${NOW_HEAD}\n갱신: 2026-11-27 11:00 · x\n\n${BODY}`), []);
  // #166 실제 사례: 머리말 안내 줄의 "갱신:" 지점에서 잘려 갱신 줄이 사라짐
  assert.ok(nowFormatProblems(`${NOW_HEAD}\n\n${BODY}`).some((p) => /갱신/.test(p)));
  assert.ok(nowFormatProblems(`갱신: 2026-11-27 11:00 · x\n\n${BODY}`).some((p) => /제목/.test(p)));
  assert.ok(nowFormatProblems(`${NOW_HEAD}\n갱신: 2026-11-27 11:00 · x\n\n## 현재 기능\n`).some((p) => /바로 다음 할 일/.test(p)));
});

test('HP-4 AC4: 스킬·CLAUDE.md는 NOW 갱신에 stamp --write-now를 쓴다', () => {
  for (const f of ['.claude/skills/handoff/SKILL.md', '.claude/skills/implement/SKILL.md', 'CLAUDE.md']) {
    const src = readFileSync(new URL(`../../${f}`, import.meta.url), 'utf8');
    assert.match(src, /stamp\.mjs --write-now/, f);
  }
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
