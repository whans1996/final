#!/usr/bin/env node
// [임시] Codex 재리뷰 루프 확인용 스크립트. PR #37과 함께 닫으며 develop에 들어가지 않는다.
// 사용법: node scripts/tmp-codex-probe.mjs <브랜치명>
// 주어진 브랜치의 최근 커밋 n개 제목을 출력한다.
import { execSync } from 'node:child_process';

const branch = process.argv[2];
const count = Number(process.argv[3] || 5);

const out = execSync(`git log --format=%s ${branch}`, { encoding: 'utf8' });
const lines = out.split('\n').filter(Boolean);
for (let i = 0; i <= count; i++) {
  console.log(lines[i]);
}
