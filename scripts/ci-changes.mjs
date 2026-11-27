#!/usr/bin/env node
// CI changes job 전용: paths-filter 결과(be·fe)를 직전 성공 실행 기준으로 보정해 $GITHUB_OUTPUT에 쓴다 (HP-4 AC8, #224).
// 판정은 scripts/lib/ci-changes.mjs. gh 조회가 실패하면 보정하지 않는다 (filter 그대로 = 기존 동작).
//
// env: EVENT, ACTION, BE, FE(paths-filter 출력 'true'/'false'), HEAD_SHA, HEAD_REF, GITHUB_REPOSITORY, GH_TOKEN, GITHUB_OUTPUT
import { spawnSync } from 'node:child_process';
import { appendFileSync } from 'node:fs';
import { compareFiles, decideChanges, lastSuccessSha } from './lib/ci-changes.mjs';

const env = process.env;
const filter = { be: env.BE === 'true', fe: env.FE === 'true' };

function gh(args) {
  const r = spawnSync('gh', args, { encoding: 'utf8', timeout: 30_000 });
  if (r.status !== 0) {
    console.log(`gh ${args.slice(0, 2).join(' ')} 실패: ${(r.stderr || r.error || '').toString().trim().slice(0, 200)}`);
    return null;
  }
  try {
    return JSON.parse(r.stdout);
  } catch {
    return null;
  }
}

let lastSuccess = null;
let compare = null;
const relevant = env.EVENT === 'pull_request' && env.ACTION === 'synchronize' && (filter.be || filter.fe);
if (relevant && env.HEAD_REF && env.HEAD_SHA) {
  const runs = gh(['run', 'list', '--repo', env.GITHUB_REPOSITORY, '--workflow', 'ci.yml', '--branch', env.HEAD_REF, '--event', 'pull_request', '--status', 'success', '--limit', '20', '--json', 'headSha']);
  lastSuccess = runs ? lastSuccessSha(runs, env.HEAD_SHA) : null;
  if (lastSuccess) {
    const c = gh(['api', `repos/${env.GITHUB_REPOSITORY}/compare/${lastSuccess}...${env.HEAD_SHA}`, '--jq', '{status: .status, files: [.files[]? | {filename, previous_filename}], total: (.files // [] | length)}']);
    // compare API는 파일을 300개까지만 돌려준다
    if (c) compare = { status: c.status, files: compareFiles(c.files), truncated: c.total >= 300 };
  }
}

const r = decideChanges({ event: env.EVENT, action: env.ACTION, filter, lastSuccess, compare });
console.log(`변경 영역: be=${r.be} fe=${r.fe} · ${r.reason}`);
if (env.GITHUB_OUTPUT) appendFileSync(env.GITHUB_OUTPUT, `be=${r.be}\nfe=${r.fe}\n`);
