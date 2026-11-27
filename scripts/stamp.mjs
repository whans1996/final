#!/usr/bin/env node
// 인계 기록의 머리 줄을 시계·git·PC 등록에서 만든다. AI가 시각이나 PC를 짐작해 쓰면
// 작업로그 시각이 커밋보다 늦어지고(11-19 실측 30분) PC 전환 판정이 틀어진다.
// H-1부터는 기록 자체의 초안도 만든다: 사실(커밋, 바뀐 영역, 검증, PR)은 여기서 채우고 사람·AI는 메모 3줄만 쓴다.
//
// 사용법:
//   node scripts/stamp.mjs         2026-11-19 10:20 · whans1996 · dh-ssafy · feature/F-08-concepts
//   node scripts/stamp.mjs --now   갱신: 2026-11-19 10:20 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-08-concepts
//   node scripts/stamp.mjs --log   ## 2026-11-19 10:20 · whans1996 · dh-ssafy · feature/F-08-concepts
//   node scripts/stamp.mjs --file  docs/handoff/NOW-dh.md (이 PC 사용자의 NOW 파일)
//   node scripts/stamp.mjs --area  area:A (이 PC 사용자의 follow-up 이슈 영역 라벨)
//   node scripts/stamp.mjs --draft         NOW·작업로그를 쓸 때 필요한 사실 (내 마지막 기록 뒤 커밋, 바뀐 영역, 검증, PR)
//   node scripts/stamp.mjs --touch-now     내 NOW 파일의 "갱신:" 줄만 지금 값으로 바꾼다
//   node scripts/stamp.mjs --write-now <본문 파일>
//                                          내 NOW 파일의 머리말은 두고 갱신 줄은 지금 값으로, 그 아래는 본문 파일 내용으로 바꾼다 (#166)
//                                          본문에 갱신 줄이 있거나 "## 현재 기능"·"## 바로 다음 할 일"이 없으면 바꾸지 않고 exit 1
//   node scripts/stamp.mjs --append-log --did "한 일" --next "다음 할 일" [--blocked "막힌 점"] [--ai "AI 활용"]
//                                          작업로그 맨 아래에 항목을 붙인다 (머리 줄·커밋 줄은 자동)
import { spawnSync } from 'node:child_process';
import { appendFileSync, readFileSync, writeFileSync } from 'node:fs';
import { userInfo } from 'node:os';
import { join } from 'node:path';
import { ROOT, WORKLOG_FILE, currentBranch, git, localPcId, nowFileOf, nowRelPath, nowStamp, ownerKey, readText, readWorklog, recordStamp } from './lib/common.mjs';
import { AREA_OF } from './lib/issues.mjs';
import { branchChangedFiles, lastEntryAt, logEntry, replaceNowBody, touchNowHeader, undoMsysPathConv, verifiedLine } from './lib/records.mjs';
import { areasOf } from './lib/related.mjs';

const args = process.argv.slice(2);
const has = (f) => args.includes(f);
const value = (f) => {
  const i = args.indexOf(f);
  // Git Bash가 '/pr-review …'를 'C:/Program Files/Git/pr-review …'로 바꿔 넘긴 것을 되돌린다 (#231)
  return i >= 0 ? undoMsysPathConv(args[i + 1] ?? '') : '';
};
const pcId = localPcId();
const author = git(['config', 'user.name']).out || userInfo().username;
const branch = currentBranch();
const key = ownerKey(pcId);

function needKey() {
  if (key) return;
  console.error('PC가 등록되지 않았습니다: node scripts/setup.mjs --pc <id>');
  process.exit(1);
}

const nowLine = () => `갱신: ${nowStamp()} · 작업자: ${author} · PC: ${pcId || 'unknown-pc'} · 브랜치: ${branch || '(detached)'}`;
const develop = () => ['origin/develop', 'develop'].find((b) => git(['rev-parse', '--verify', '-q', b]).ok);

/** 내 마지막 작업로그 항목 뒤에 이 브랜치에 쌓인 커밋 (develop에 이미 있는 것과 merge 커밋 제외) */
function commitsSinceMyLastEntry() {
  const since = lastEntryAt(readWorklog(), key);
  const base = develop();
  const range = base ? [`${base}..HEAD`] : ['-15'];
  const out = git(['log', '--no-merges', '--format=%H%x09%s', ...(since ? [`--since=${since}`] : ['-15']), ...range]).out;
  return {
    since,
    commits: out
      .split('\n')
      .filter(Boolean)
      .map((l) => {
        const [sha, ...rest] = l.split('\t');
        return { sha, subject: rest.join('\t') };
      }),
  };
}

if (has('--file') || has('--area')) {
  needKey();
  console.log(has('--area') ? AREA_OF[key] : nowRelPath(key));
} else if (has('--now')) {
  console.log(nowLine());
} else if (has('--log')) {
  console.log(`## ${recordStamp({ author, pcId, branch })}`);
} else if (has('--touch-now')) {
  needKey();
  const file = nowFileOf(key);
  writeFileSync(file, touchNowHeader(readText(file) || '', nowLine()), 'utf8');
  console.log(`📝 ${nowRelPath(key)} ${nowLine()}`);
} else if (has('--write-now')) {
  needKey();
  const bodyPath = value('--write-now');
  const body = bodyPath ? readText(bodyPath) : null;
  if (body === null) {
    console.error(`본문 파일을 읽을 수 없습니다: ${bodyPath || '(없음)'} — node scripts/stamp.mjs --write-now <본문 파일>`);
    process.exit(1);
  }
  const file = nowFileOf(key);
  const r = replaceNowBody(readText(file) || '', body, nowLine());
  if (r.error) {
    console.error(`❌ ${nowRelPath(key)}을 바꾸지 않았습니다: ${r.error}`);
    process.exit(1);
  }
  writeFileSync(file, r.text, 'utf8');
  console.log(`📝 ${nowRelPath(key)} 본문 교체 · ${nowLine()}`);
  if (r.warning) console.log(`⚠️  ${r.warning}`);
} else if (has('--append-log')) {
  needKey();
  const entry = logEntry({
    header: recordStamp({ author, pcId, branch }),
    did: value('--did'),
    next: value('--next'),
    blocked: value('--blocked'),
    ai: value('--ai'),
    commits: commitsSinceMyLastEntry().commits,
  });
  if (!entry) {
    console.error('--did(한 일)와 --next(다음 할 일)는 필수입니다.');
    process.exit(1);
  }
  const prev = readFileSync(WORKLOG_FILE, 'utf8');
  appendFileSync(WORKLOG_FILE, `${prev.endsWith('\n') ? '' : '\n'}\n${entry}\n`, 'utf8');
  console.log(entry);
} else if (has('--draft')) {
  needKey();
  const { since, commits } = commitsSinceMyLastEntry();
  const base = develop();
  const changed = branchChangedFiles(git, base);
  const areas = areasOf(changed);
  let verified = {};
  try {
    verified = JSON.parse(readFileSync(join(ROOT, '.harness', 'verified.json'), 'utf8'));
  } catch {
    verified = {};
  }
  const dirty = git(['status', '--porcelain']).out.split('\n').filter(Boolean).length;
  const pr = spawnSync('gh', ['pr', 'view', '--json', 'number,state,isDraft,statusCheckRollup'], { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout: 8000 });
  let prLine = 'PR 없음';
  if (pr.status === 0) {
    const p = JSON.parse(pr.stdout);
    const checks = p.statusCheckRollup || [];
    const failed = checks.filter((c) => ['FAILURE', 'ERROR', 'CANCELLED', 'TIMED_OUT'].includes(c.conclusion || c.state)).length;
    const pending = checks.filter((c) => !c.conclusion && c.state !== 'SUCCESS').length;
    prLine = `#${p.number} ${p.isDraft ? 'DRAFT' : p.state} · CI ${failed ? `❌ ${failed}개 실패` : pending ? `⏳ ${pending}개 진행` : '✅'} (Codex: node scripts/codex-review.mjs ${p.number})`;
  }
  console.log(
    [
      nowLine(),
      `NOW 파일: ${nowRelPath(key)}`,
      `내 마지막 작업로그: ${since || '없음'}`,
      `그 뒤 이 브랜치 커밋 ${commits.length}개:`,
      ...commits.slice(0, 12).map((c) => `  - ${c.subject} (${c.sha.slice(0, 7)})`),
      `develop 갈라진 뒤 바뀐 파일 ${changed.length}개 · 영역: ${areas.join(' ') || '없음'} · 커밋 안 된 변경 ${dirty}개`,
      `검증: ${verifiedLine(verified, git(['rev-parse', 'HEAD']).out, areas)}`,
      `PR: ${prLine}`,
      '',
      '메모 3줄만 쓰면 된다: node scripts/stamp.mjs --append-log --did "한 일" --next "다음 할 일" [--blocked "막힌 점"] [--ai "AI 활용"]',
      'NOW 파일은 진행·바로 다음 할 일만 고치고 갱신 줄은 node scripts/stamp.mjs --touch-now',
    ].join('\n'),
  );
} else {
  console.log(recordStamp({ author, pcId, branch }));
}
