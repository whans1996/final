#!/usr/bin/env node
// 상태 표시줄 캐시 새로 고침 (HX-6). .claude/hooks/statusline.mjs가 캐시가 오래되면 백그라운드로 띄운다. 사람이 직접 돌려도 된다.
//   - 지금 브랜치의 PR 번호·상태·head·CI (gh pr view)
//   - 로드맵 다음 항목·가장 이른 넘겨주는 시점 (PR 본문 Roadmap: 줄로 계산, scripts/lib/roadmap.mjs)
// 결과는 .harness/status.json에 쓴다. 조회가 실패하면 직전 값을 두고 시각만 바꿔 2분 동안 다시 부르지 않는다.
import { execFile } from 'node:child_process';
import { ROOT, currentBranch, localPcId, ownerKey, readText } from './lib/common.mjs';
import { ROADMAP_FILE, fetchRoadmapPrs, loadView } from './lib/roadmap.mjs';
import { ROADMAP_REFRESH_MS, ciState, readStatus, roadmapSnapshot, writeStatus } from './lib/status.mjs';

const GH_TIMEOUT = 8000;

/** 지금 브랜치의 PR → { number, state, head, ci } | { number: null } (PR 없음) | null (조회 실패) */
function currentPr() {
  return new Promise((resolve) => {
    execFile(
      'gh',
      ['pr', 'view', '--json', 'number,state,headRefOid,statusCheckRollup'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout: GH_TIMEOUT },
      (err, stdout, stderr) => {
        if (err) return resolve(/no pull requests found/i.test(stderr || '') ? { number: null } : null);
        try {
          const p = JSON.parse(stdout);
          resolve({ number: p.number, state: p.state, head: p.headRefOid, ci: ciState(p.statusCheckRollup) });
        } catch {
          resolve(null);
        }
      },
    );
  });
}

const branch = currentBranch();
const key = ownerKey(localPcId());
const at = Date.now();
const prev = readStatus();
// 로드맵 조회(PR 500개 본문)는 무거우므로 오래됐거나 브랜치가 바뀌었을 때만 한다
const roadmapStale = !prev.roadmap || prev.roadmap.branch !== branch || at - (prev.roadmap.at ?? 0) > ROADMAP_REFRESH_MS;
const [pr, prsRes] = await Promise.all([
  branch ? currentPr() : Promise.resolve({ number: null }),
  key && roadmapStale ? fetchRoadmapPrs({ timeout: GH_TIMEOUT }) : Promise.resolve(null),
]);

const patch = { refreshingAt: null };
// 실패하면 같은 브랜치의 직전 값을 그대로 두고 시각만 바꾼다 (계속 다시 부르지 않게)
patch.pr = pr ? { branch, at, ...pr } : prev.pr?.branch === branch ? { ...prev.pr, at } : { branch, at, number: null };
const md = key ? readText(ROADMAP_FILE) : null;
if (md && prsRes?.ok) {
  patch.roadmap = { at, ...roadmapSnapshot(loadView(md, prsRes).view, key, branch) };
} else if (roadmapStale && prev.roadmap) {
  // 조회 실패: 직전 값을 두고 시각만 바꿔 바로 다시 부르지 않는다
  patch.roadmap = { ...prev.roadmap, at, branch };
}
writeStatus(patch);
process.exit(0);
