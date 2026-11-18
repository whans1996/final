#!/usr/bin/env node
// PR 본문·댓글에 넣을 스크린샷 Markdown을 만든다.
// GitHub에는 PR 이미지를 올리는 API가 없어서, docs/산출물/screens/에 커밋한 이미지를 커밋 sha 링크로 건다.
// private 저장소도 로그인한 사람에게는 보인다. 링크는 sha 고정이라 브랜치가 지워져도 깨지지 않는다.
//
// 사용법: node scripts/pr-screens.mjs SC-18 SC-19   (접두어에 맞는 파일만, 생략하면 전부)
//   먼저 이미지를 커밋하고 push해야 한다 (HEAD sha를 쓰므로).

import { existsSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, git } from './lib/common.mjs';

const DIR = 'docs/산출물/screens';

const remote = git(['remote', 'get-url', 'origin']).out;
const repo = remote.match(/github\.com[:/](.+?)(\.git)?$/)?.[1];
if (!repo) {
  console.error(`origin이 GitHub 저장소가 아닙니다: ${remote || '(없음)'}`);
  process.exit(1);
}
const sha = git(['rev-parse', 'HEAD']).out;
const onRemote = git(['branch', '-r', '--contains', sha]).out;
if (!onRemote) console.error(`⚠️ HEAD(${sha.slice(0, 7)})가 아직 push되지 않았습니다. push한 뒤에 링크가 열립니다.`);

const dir = join(ROOT, DIR);
if (!existsSync(dir)) {
  console.error(`${DIR}가 없습니다. ui-verifier 스크린샷을 SC-xx-이름-{desktop,mobile}.png로 옮겨 커밋하세요.`);
  process.exit(1);
}
const prefixes = process.argv.slice(2);
const files = readdirSync(dir)
  .filter((f) => f.endsWith('.png'))
  .filter((f) => prefixes.length === 0 || prefixes.some((p) => f.startsWith(p)))
  .sort();
if (files.length === 0) {
  console.error(`${DIR}에 ${prefixes.join(', ') || ''} 스크린샷이 없습니다.`);
  process.exit(1);
}

const url = (f) => `https://github.com/${repo}/blob/${sha}/${encodeURI(`${DIR}/${f}`)}?raw=true`;
const lines = files.map((f) => {
  const width = f.includes('mobile') ? 200 : 480;
  return `**${f.replace(/\.png$/, '')}**<br><img src="${url(f)}" width="${width}">`;
});
console.log(lines.join('\n\n'));
