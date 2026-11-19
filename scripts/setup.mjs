#!/usr/bin/env node
// PC마다 최초 1회 실행: PC 등록 + git hook 경로 연결 + 개발 환경 점검
// 사용법: node scripts/setup.mjs --pc <dh-ssafy|dh-home|js-ssafy|js-home>   (등록 뒤에는 --pc 생략 가능)
// 결과는 .harness/env-check.json에 남고, SessionStart hook이 세션마다 빠른 점검을 다시 한다.
import { ROOT, git, isGitRepo } from './lib/common.mjs';
import { currentPc, icon, loadPcs, registerPc, runChecks, saveFullCheck } from './lib/env-check.mjs';

// git hook 연결 (유일하게 상태를 바꾸는 단계). PC 등록과 무관하므로 먼저 한다
const hookSet = isGitRepo() ? git(['config', 'core.hooksPath', '.githooks']) : null;
if (hookSet && !hookSet.ok) console.error(`⚠️  git hook 연결 실패: ${hookSet.err}`);

const args = process.argv.slice(2);
let pcArg;
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--pc') pcArg = args[++i] ?? '';
  else if (args[i].startsWith('--pc=')) pcArg = args[i].slice(5);
  else {
    console.error(`❌ 알 수 없는 인자: ${args[i]}\n   사용법: node scripts/setup.mjs [--pc <id>]`);
    process.exit(1);
  }
}
const pcs = loadPcs();
const pcList = Object.entries(pcs)
  .map(([id, p]) => `  ${id.padEnd(9)} ${p.label}`)
  .join('\n');

if (pcArg !== undefined) {
  if (!pcs[pcArg]) {
    console.error(`❌ 알 수 없는 PC id: ${pcArg || '(없음)'}\n\n등록된 PC (scripts/lib/pcs.json):\n${pcList}\n`);
    process.exit(1);
  }
  registerPc(pcArg);
} else if (!currentPc().id) {
  console.error(`❌ 이 PC가 아직 등록되지 않았습니다. 어느 PC인지 지정해 다시 실행하세요.\n   node scripts/setup.mjs --pc <id>\n\n${pcList}\n`);
  process.exit(1);
}

const pc = currentPc();
const rows = await runChecks();
saveFullCheck(pc.id, rows);

console.log(`\n바이브노트 개발 환경 점검 — ${pc.label} (${pc.id}) · ${ROOT}\n`);
console.log(rows.map((r) => `${icon(r.status)} ${r.item.padEnd(18)} ${r.detail}`).join('\n'));
if (pc.notes?.length) console.log(`\n이 PC의 주의 사항 (scripts/lib/pcs.json):\n${pc.notes.map((n) => `  - ${n}`).join('\n')}`);
const failed = rows.filter((r) => r.status === 'fail').length;
console.log(
  failed
    ? `\n❌ 항목 ${failed}개를 해결한 뒤 다시 실행하세요. 완료되면 Claude Code에서 /pickup 으로 시작합니다.\n`
    : '\n준비 완료. Claude Code에서 /pickup 으로 시작합니다.\n',
);
process.exitCode = failed ? 1 : 0;
