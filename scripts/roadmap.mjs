#!/usr/bin/env node
// 로드맵 R-1 (docs/로드맵.md): 상태를 PR·브랜치로 계산해 보여 준다.
//   node scripts/roadmap.mjs                 /pickup 로드맵 블록 (지금 할 일·내 남은 항목·상대·넘겨주는 시점·공통 일정)
//   node scripts/roadmap.mjs --next [--after <ID>]   /handoff·PR 머지 뒤 "다음 항목" 한 줄 (--after: 방금 끝낸 항목과 같은 트랙에서)
//   node scripts/roadmap.mjs --check         표 형식만 검사 (오류면 종료 코드 1)
//   node scripts/roadmap.mjs --overdue-since <sha>   Actions(roadmap.yml): 그 커밋 날 뒤 새로 지난 넘겨주는 시점 → Mattermost 본문 JSON (없으면 출력 없음)
//   --who dh|js  사람 지정 (기본: 이 PC 등록)
import { currentBranch, git, localPcId, ownerKey, readText } from './lib/common.mjs';
import { ROADMAP_FILE, bannerLines, fetchRoadmapPrs, loadView, newlyOverdue, nextItem, nextItemLine, overdueMessage, parseRoadmap, roadmapBlock, seoulToday } from './lib/roadmap.mjs';

const args = process.argv.slice(2);
const opt = (name) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : null;
};
const md = readText(ROADMAP_FILE);
if (!md) {
  console.error(`로드맵 파일이 없습니다: ${ROADMAP_FILE}`);
  process.exit(1);
}

if (args.includes('--check')) {
  const { errors, items, handoffs } = parseRoadmap(md);
  if (errors.length) {
    console.error(`로드맵 형식 오류 ${errors.length}건:\n${errors.map((e) => `- ${e}`).join('\n')}`);
    process.exit(1);
  }
  console.log(`로드맵 형식 ✅ (항목 ${items.length}개, 넘겨주는 시점 ${handoffs.length}개)`);
  process.exit(0);
}

const prsRes = await fetchRoadmapPrs({ timeout: 15000 });

if (args.includes('--overdue-since')) {
  const sha = opt('--overdue-since');
  // 새 브랜치 push(before가 0)이거나 커밋을 못 읽으면 알리지 않는다
  const at = sha && !/^0+$/.test(sha) ? git(['show', '-s', '--format=%cI', sha]).out : '';
  if (!at) process.exit(0);
  const { view } = loadView(md, prsRes, { branches: [] });
  const text = overdueMessage(newlyOverdue(view, seoulToday(new Date(at))));
  if (text) process.stdout.write(JSON.stringify({ text }));
  process.exit(0);
}

const key = opt('--who') ?? ownerKey(localPcId());
if (!key) {
  console.error('이 PC가 등록되지 않았습니다. --who dh|js로 지정하거나 node scripts/setup.mjs --pc <id>');
  process.exit(1);
}
const { roadmap, view } = loadView(md, prsRes);
if (roadmap.errors.length) console.error(`⚠️ 로드맵 형식 오류 ${roadmap.errors.length}건 (node scripts/roadmap.mjs --check)`);
const branch = currentBranch();
const after = opt('--after');

if (args.includes('--next')) {
  console.log(nextItemLine(nextItem(view, key, { branch, after })));
} else {
  console.log(roadmapBlock(view, key, { branch, after }));
  const banner = bannerLines(view, key);
  if (banner.length) console.log(`\n${banner.join('\n')}`);
}
