#!/usr/bin/env node
// [임시] Codex 재리뷰 루프 확인용 스크립트. PR #37과 함께 닫으며 develop에 들어가지 않는다.
// 사용법: node scripts/tmp-codex-probe.mjs <브랜치명> [개수]
// 주어진 브랜치의 최근 커밋 n개 제목을 출력한다.
import { execFileSync } from 'node:child_process';

const branch = process.argv[2];
if (!branch) {
  console.error('브랜치명이 필요합니다');
  process.exit(1);
}
const count = Math.max(0, Number(process.argv[3] || 5) || 0);

// 셸을 거치지 않고, "--"로 브랜치명이 옵션으로 해석되지 않게 한다
const out = execFileSync('git', ['log', `--max-count=${count}`, '--format=%s', branch, '--'], { encoding: 'utf8' });
for (const line of out.split('\n').filter(Boolean)) {
  console.log(line);
}
