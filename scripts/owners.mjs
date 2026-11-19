#!/usr/bin/env node
// 파일별 영역(내 것·상대 것·공통)을 .github/CODEOWNERS로 판정한다. /pr-review가 PR 설명의 @멘션 대상을 뽑을 때 쓴다.
// 사용법: node scripts/owners.mjs --diff origin/develop   (이 브랜치에서 바뀐 파일)
//         node scripts/owners.mjs <경로...>
import { git, localPcId, ownerKey, otherOwnerKey } from './lib/common.mjs';
import { loadPcs } from './lib/env-check.mjs';
import { areaOf, loadOwnerRules, personOf } from './lib/owners.mjs';

const args = process.argv.slice(2);
const diffAt = args.indexOf('--diff');
const files =
  diffAt >= 0
    ? git(['diff', '--name-only', `${args[diffAt + 1] || 'origin/develop'}...HEAD`]).out.split('\n').filter(Boolean)
    : args;
if (!files.length) {
  console.log('바뀐 파일 없음');
  process.exit(0);
}

const me = ownerKey(localPcId());
if (!me) {
  console.error('❌ 이 PC가 등록되지 않아 내 영역을 정할 수 없습니다 → node scripts/setup.mjs --pc <id>');
  process.exit(1);
}
const pcs = loadPcs();
const rules = loadOwnerRules();
const peer = personOf(otherOwnerKey(me), pcs);

const groups = { peer: [], common: [], mine: [], none: [] };
for (const f of files) groups[areaOf(f, me, rules, pcs)].push(f);

const label = { peer: `상대 영역 (${peer?.owner ?? '?'})`, common: '공통', mine: '내 영역', none: '지정 없음 (문서 등)' };
for (const k of ['peer', 'common', 'mine', 'none']) {
  if (groups[k].length) console.log(`## ${label[k]} ${groups[k].length}개\n${groups[k].map((f) => `- ${f}`).join('\n')}\n`);
}
if (groups.peer.length || groups.common.length) {
  console.log(`멘션: @${peer?.github ?? '?'} — PR 설명 "공통 코드 변경"에 위 상대 영역·공통 파일과 이유를 적는다`);
} else {
  console.log('멘션 필요 없음 (내 영역과 지정 없는 파일만 바뀜)');
}
