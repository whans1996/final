#!/usr/bin/env node
// 상태 표시줄 캐시 새로 고침 (HX-6). .claude/hooks/statusline.mjs가 캐시가 오래되면 백그라운드로 띄운다. 사람이 직접 돌려도 된다.
//   - 지금 브랜치의 PR 번호·상태·head·CI (gh pr view)
//   - 로드맵 다음 항목·가장 이른 넘겨주는 시점 (PR 본문 Roadmap: 줄로 계산, scripts/lib/roadmap.mjs)
// 결과는 .harness/status.json에 쓴다. 조회·계산이 실패하면 직전 값을 두고 시각만 바꿔 바로 다시 부르지 않는다 (scripts/lib/status.mjs refreshPatch).
import { execFile } from 'node:child_process';
import { ROOT, localPcId, ownerKey, readText } from './lib/common.mjs';
import { ROADMAP_FILE, fetchRoadmapPrs, loadView } from './lib/roadmap.mjs';
import { ROADMAP_REFRESH_MS, ciState, gitBranchAt, readStatus, refreshPatch, roadmapSnapshot, writeStatus } from './lib/status.mjs';

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

// 표시줄과 같은 값으로 읽는다 (detached면 짧은 sha). 다르면 표시줄이 캐시를 늘 오래됐다고 보고 매번 다시 띄운다
const branch = gitBranchAt(ROOT);
const key = ownerKey(localPcId());
const at = Date.now();
const prev = readStatus();
// 로드맵 조회(PR 500개 본문)는 무거우므로 오래됐거나 브랜치가 바뀌었을 때만 한다
const roadmapStale = !prev.roadmap || prev.roadmap.branch !== branch || at - (prev.roadmap.at ?? 0) > ROADMAP_REFRESH_MS;
const [pr, prsRes] = await Promise.all([
  branch ? currentPr() : Promise.resolve({ number: null }),
  key && roadmapStale ? fetchRoadmapPrs({ timeout: GH_TIMEOUT }) : Promise.resolve(null),
]);

const md = key && prsRes?.ok ? readText(ROADMAP_FILE) : null;
writeStatus(
  refreshPatch({
    prev,
    branch,
    key,
    at,
    pr,
    roadmapStale: Boolean(key && roadmapStale),
    roadmap: md ? () => roadmapSnapshot(loadView(md, prsRes).view, key, branch) : null,
  }),
);
process.exit(0);
