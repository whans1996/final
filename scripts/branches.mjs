#!/usr/bin/env node
// develop에 머지 안 된 원격 브랜치에서 진행 중인 일을 보여 준다 (#54).
// 같은 사람이 PC마다 다른 브랜치를 진행하면 develop의 내 NOW가 다른 브랜치의 진행을 가려, 이미 한 일(/spec 등)을 다시 할 수 있다.
//
// 사용법:
//   node scripts/branches.mjs          내 NOW 파일을 바꾼 원격 브랜치 (세션 시작 배너와 같은 목록)
//   node scripts/branches.mjs F-14     F-14를 진행 중인 원격 브랜치 (/spec 1단계 가드). 있으면 새 spec을 쓰지 않는다
import { NON_INTERACTIVE_GIT_ENV, currentBranch, git, localPcId, nowRelPath, ownerKey } from './lib/common.mjs';
import { featureOnRemotes, myBranchLines, myBranchNows } from './lib/peer.mjs';

const arg = process.argv[2];
git(['fetch', '--quiet', '--prune', 'origin'], { timeout: 15_000, env: NON_INTERACTIVE_GIT_ENV });

if (arg) {
  const fid = /^f-\d+[a-z]?$/i.test(arg) ? arg.replace(/^f/, 'F') : null;
  if (!fid) {
    console.error(`기능 ID 형식이 아닙니다: ${arg} (예: F-14, F-05a)`);
    process.exit(2);
  }
  const found = featureOnRemotes(fid);
  if (!found.length) {
    console.log(`✅ develop 밖에서 ${fid}를 진행 중인 원격 브랜치 없음`);
  } else {
    console.log(`⚠️ ${fid}를 이미 진행 중인 원격 브랜치 (develop 미머지):`);
    for (const f of found) console.log(`- ${f.branch}${f.specs.length ? ` · spec ${f.specs.join(', ')}` : ''}`);
    console.log(`→ 새 spec을 쓰지 말고 그 브랜치로 switch해 이어 간다 (git switch ${found[0].branch.replace(/^origin\//, '')}). 상대 브랜치면 사용자에게 묻는다`);
  }
} else {
  const key = ownerKey(localPcId());
  if (!key) {
    console.error('이 PC가 등록되지 않아 내 NOW 파일을 정할 수 없습니다 → node scripts/setup.mjs --pc <id>');
    process.exit(2);
  }
  const { lines } = myBranchLines(myBranchNows(key), currentBranch());
  console.log(lines.length ? `${nowRelPath(key)}을 바꾼 원격 브랜치 (develop 미머지):\n${lines.join('\n')}` : `✅ develop 밖에서 ${nowRelPath(key)}을 바꾼 원격 브랜치 없음`);
}
