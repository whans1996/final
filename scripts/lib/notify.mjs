// 사람에게 가는 알림(Mattermost 웹훅) 메시지. GitHub Actions(.github/workflows/notify.yml)가 scripts/notify-message.mjs로 부른다.
// Claude를 켜지 않은 상대도 PR 열림·머지와 NOW 갱신(= 지금 무엇을 하는지)을 바로 알 수 있게 한다.
import { ownerKey } from './common.mjs';
import { accountsOf, personOf } from './owners.mjs';
import { nowHeadline } from './peer.mjs';

const AREA = { dh: 'A', js: 'B' };
const NOW_PATH = /^docs\/handoff\/NOW-(\w+)\.md$/;

/** github 계정 → "A 김동환" (모르면 계정 그대로) */
export function whoOf(login, pcs) {
  const a = accountsOf(pcs)[String(login || '').toLowerCase()];
  return a ? `${AREA[a.key] ?? ''} ${a.owner}`.trim() : login || '?';
}

/**
 * push 이벤트에서 바뀐 NOW 파일의 사람 key 목록.
 * develop 합치기로 딸려 온 커밋(distinct:false)은 빼고, push한 사람을 알면 그 사람의 NOW만 남긴다
 * (머지 직전 develop을 합쳐 push하면 상대의 옛 NOW 커밋이 payload.commits에 섞인다).
 * filesOf(commit): 커밋의 변경 파일. GitHub Actions의 push payload에는 added·modified가 없으므로
 * notify-message.mjs가 커밋 id로 git에서 조회해 넘긴다. 기본값은 webhook payload의 added·modified.
 */
export function changedNowKeys(payload, pcs = {}, filesOf = (c) => [...(c.added || []), ...(c.modified || [])]) {
  const pusher = accountsOf(pcs)[String(payload?.sender?.login || '').toLowerCase()]?.key;
  const keys = new Set();
  for (const c of payload?.commits || []) {
    if (c.distinct === false) continue;
    for (const f of filesOf(c) || []) {
      const m = NOW_PATH.exec(f);
      if (m && ownerKey(`${m[1]}-x`) && (!pusher || pusher === m[1])) keys.add(m[1]);
    }
  }
  return [...keys];
}

/**
 * 이벤트 → 메시지 본문 (알릴 것이 없으면 null). 순수 함수.
 * nowTexts: push 이벤트에서 바뀐 NOW 파일 본문 { dh: '...', js: '...' }
 */
export function buildMessage(eventName, payload, { pcs = {}, nowTexts = {}, filesOf } = {}) {
  if (eventName === 'pull_request') {
    const pr = payload?.pull_request;
    if (!pr) return null;
    const verb =
      {
        opened: pr.draft ? '초안 열림' : '열림',
        reopened: '다시 열림',
        ready_for_review: '리뷰 준비됨',
        closed: pr.merged ? '✅ 머지됨' : '닫힘',
      }[payload.action] ?? null;
    if (!verb) return null;
    const tail = payload.action === 'closed' && pr.merged ? ` → 상대는 \`git merge origin/${pr.base.ref}\`` : '';
    return `🔀 **${whoOf(pr.user?.login, pcs)}** [PR #${pr.number} ${pr.title}](${pr.html_url}) ${verb} (\`${pr.head.ref}\` → \`${pr.base.ref}\`)${tail}`;
  }

  if (eventName === 'push') {
    const branch = String(payload?.ref || '').replace(/^refs\/heads\//, '');
    const lines = changedNowKeys(payload, pcs, filesOf)
      .filter((k) => nowTexts[k])
      .map((k) => {
        const h = nowHeadline(nowTexts[k]);
        const who = personOf(k, pcs);
        return `📝 **${AREA[k] ?? ''} ${who?.owner ?? k}** 작업 상태 (\`${branch}\`)\n- 현재: ${h.feature}\n- 다음: ${h.next}`;
      });
    return lines.length ? lines.join('\n') : null;
  }
  return null;
}
