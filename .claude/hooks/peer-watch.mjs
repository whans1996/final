#!/usr/bin/env node
// UserPromptSubmit hook: 세션 도중에도 상대 작업 변화를 알린다 (11-19 결정, 병렬 개발).
// 10분에 한 번만 원격을 확인한다: origin/develop 새 커밋(마이그레이션·API), 상대 NOW 갱신(원격 브랜치), 상대 PR 열림·머지·push.
// 확인할 때 받을 커밋이 있으면 자동으로 합친다 (scripts/lib/sync.mjs, 끄기: git config vibenote.autoSync false).
// 변화가 있을 때만 컨텍스트에 주입한다. 어떤 오류도 프롬프트를 막지 않는다 (항상 exit 0).
import { NON_INTERACTIVE_GIT_ENV, git, localPcId, ownerKey, otherOwnerKey } from '../../scripts/lib/common.mjs';

try {
  const { loadPcs } = await import('../../scripts/lib/env-check.mjs');
  const { personOf } = await import('../../scripts/lib/owners.mjs');
  const peer = await import('../../scripts/lib/peer.mjs');

  const me = ownerKey(localPcId());
  const prev = peer.readPeerState();
  if (!me || (prev?.checkedAt && Date.now() - prev.checkedAt < peer.PEER_WATCH_INTERVAL_MS)) process.exit(0);

  const pcs = loadPcs();
  const other = otherOwnerKey(me);
  const peerPerson = personOf(other, pcs);

  // 확인 시각을 먼저 남긴다: 아래가 hook 제한(15초)에 걸려 죽어도 다음 프롬프트마다 다시 느려지지 않고,
  // 같은 PC의 두 세션이 동시에 같은 변화를 알리는 것도 줄인다
  peer.writePeerState({ ...prev, checkedAt: Date.now() });
  git(['fetch', '--quiet', '--prune'], { timeout: 4000, env: NON_INTERACTIVE_GIT_ENV });
  // 상대가 develop에 머지한 커밋·다른 PC의 내 브랜치 커밋을 자동으로 합친다 (scripts/lib/sync.mjs)
  const { autoSync } = await import('../../scripts/lib/sync.mjs');
  const synced = autoSync();
  const developSha = git(['rev-parse', '-q', '--verify', 'origin/develop']).out || null;
  const now = peer.latestPeerNow(other);
  const curr = {
    checkedAt: Date.now(),
    developSha,
    develop: peer.developChanges(prev?.developSha, developSha),
    peerNow: now ? { source: now.source, ...peer.nowHeadline(now.text) } : null,
    // gh가 실패하면 직전 목록을 유지해 다음 확인 때 비교한다
    prs: peer.peerPrs(peerPerson?.github) ?? prev?.prs ?? null,
  };
  const myNames = [git(['config', 'user.name']).out, personOf(me, pcs)?.github].filter(Boolean);
  const lines = peer.peerDigest(prev, curr, { peerName: peerPerson?.owner ?? '상대', myNames });
  delete curr.develop;
  peer.writePeerState(curr);

  // develop을 이미 자동으로 합쳤으면 "git merge origin/develop 하라"는 권고를 결과로 바꾼다
  if (synced.some((l) => l.startsWith('- ✅ origin/develop'))) {
    for (let i = 0; i < lines.length; i++) lines[i] = lines[i].replace(/마이그레이션·API를 추가하거나 PR을 올리기 전에 `git merge origin\/develop`$/, '(이 브랜치에 자동 병합됨)');
  }
  const sections = [];
  if (lines.length) sections.push(`## 📣 상대 작업 변화 (peer-watch hook, 10분 간격)\n${lines.join('\n')}`);
  if (synced.length) sections.push(`## 🔄 자동 동기화 (작업 트리가 바뀌었을 수 있다)\n${synced.join('\n')}`);
  if (sections.length) {
    process.stdout.write(
      JSON.stringify({
        hookSpecificOutput: {
          hookEventName: 'UserPromptSubmit',
          additionalContext: `${sections.join('\n\n')}\n사용자 요청과 관련 있으면 답변에서 짧게 알린다.`,
        },
      }),
    );
  }
} catch {
  // 알림은 부가 기능이라 실패해도 조용히 넘어간다
}
process.exit(0);
