#!/usr/bin/env node
// 로드맵 R-1 (docs/로드맵.md): 상태를 PR·브랜치로 계산해 보여 준다.
//   node scripts/roadmap.mjs                 /pickup 로드맵 블록 (지금 할 일·내 남은 항목·상대·넘겨주는 시점·공통 일정)
//   node scripts/roadmap.mjs --next [--after <ID>]   /handoff·PR 머지 뒤 "다음 항목" 한 줄 (--after: 방금 끝낸 항목과 같은 트랙에서)
//   node scripts/roadmap.mjs --check         표 형식만 검사 (오류면 종료 코드 1)
//   node scripts/roadmap.mjs --check-body <PR 설명 파일>   /pr-review: Roadmap: 줄 형식·없는 ID 검사 (문제면 종료 코드 1)
//   node scripts/roadmap.mjs --overdue-since <sha> [--send]   Actions(roadmap.yml): 그 커밋 날 뒤 새로 지난 넘겨주는 시점 → Mattermost 본문 JSON (없으면 출력 없음)
//        --send: env WEBHOOK으로 보낸다. 상태 조회·전송이 실패하면 종료 코드 1 (Re-run으로 다시 보내기, #196)
//   --who dh|js  사람 지정 (기본: 이 PC 등록)
import { currentBranch, git, localPcId, ownerKey, readText } from './lib/common.mjs';
import { ROADMAP_FILE, bannerLines, fetchRoadmapPrs, loadView, nextItem, nextItemLine, overdueOutcome, overduePayload, parseRoadmap, roadmapBlock, roadmapRefIssues, roadmapRefs } from './lib/roadmap.mjs';

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

if (args.includes('--check-body')) {
  const file = opt('--check-body');
  const body = file ? readText(file) : null;
  if (body === null) {
    console.error(`PR 본문 파일을 읽을 수 없습니다: ${file ?? '(경로 없음)'}`);
    process.exit(1);
  }
  const ids = parseRoadmap(md).items.map((i) => i.id);
  const refs = roadmapRefs(body);
  const issues = roadmapRefIssues(body, ids);
  if (issues.length) {
    console.error(`Roadmap: 줄 문제 ${issues.length}건:\n${issues.map((e) => `- ${e}`).join('\n')}`);
    process.exit(1);
  }
  console.log(refs.length ? `Roadmap ✅ ${refs.map((r) => `${r.id}${r.partial ? ' (일부)' : ''}`).join(', ')}` : 'Roadmap: 줄 없음 (로드맵 밖 작업이면 괜찮다)');
  process.exit(0);
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
  const payload = overduePayload(md, prsRes, at);
  if (!args.includes('--send')) {
    // 알리지 않은 이유는 Actions 로그(stderr)에 남긴다
    if (payload.reason) console.error(`알림 없음: ${payload.reason}`);
    if (payload.text) process.stdout.write(JSON.stringify({ text: payload.text }));
    process.exit(payload.failed ? 1 : 0);
  }
  // --send: 웹훅(env WEBHOOK)으로 보내고, 조회·전송이 실패하면 종료 코드 1로 Re-run을 안내한다 (#196)
  let sent = null;
  let error = '';
  if (payload.text) {
    try {
      const res = await fetch(process.env.WEBHOOK || '', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: payload.text }), signal: AbortSignal.timeout(15000) });
      sent = res.ok;
      if (!res.ok) error = `HTTP ${res.status}`;
    } catch (e) {
      sent = false;
      error = e?.message || String(e);
    }
  }
  const out = overdueOutcome({ ...payload, sent, error });
  (out.exit ? console.error : console.log)(out.message);
  process.exit(out.exit);
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
