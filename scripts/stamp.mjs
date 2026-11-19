#!/usr/bin/env node
// 인계 기록의 머리 줄을 시계·git·PC 등록에서 만든다. AI가 시각이나 PC를 짐작해 쓰면
// 작업로그 시각이 커밋보다 늦어지고(11-19 실측 30분) PC 전환 판정이 틀어진다.
//
// 사용법:
//   node scripts/stamp.mjs         2026-11-19 10:20 · whans1996 · dh-ssafy · feature/F-08-concepts
//   node scripts/stamp.mjs --now   갱신: 2026-11-19 10:20 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-08-concepts
//   node scripts/stamp.mjs --log   ## 2026-11-19 10:20 · whans1996 · dh-ssafy · feature/F-08-concepts
//   node scripts/stamp.mjs --file  docs/handoff/NOW-dh.md (이 PC 사용자의 NOW 파일)
//   node scripts/stamp.mjs --area  area:A (이 PC 사용자의 follow-up 이슈 영역 라벨)
import { userInfo } from 'node:os';
import { currentBranch, git, localPcId, nowRelPath, nowStamp, ownerKey, recordStamp } from './lib/common.mjs';
import { AREA_OF } from './lib/issues.mjs';

const args = process.argv.slice(2);
const pcId = localPcId();
const author = git(['config', 'user.name']).out || userInfo().username;
const branch = currentBranch();

if (args.includes('--file') || args.includes('--area')) {
  const key = ownerKey(pcId);
  if (!key) {
    console.error('PC가 등록되지 않았습니다: node scripts/setup.mjs --pc <id>');
    process.exit(1);
  }
  console.log(args.includes('--area') ? AREA_OF[key] : nowRelPath(key));
} else if (args.includes('--now')) {
  console.log(`갱신: ${nowStamp()} · 작업자: ${author} · PC: ${pcId || 'unknown-pc'} · 브랜치: ${branch || '(detached)'}`);
} else if (args.includes('--log')) {
  console.log(`## ${recordStamp({ author, pcId, branch })}`);
} else {
  console.log(recordStamp({ author, pcId, branch }));
}
