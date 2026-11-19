// 병렬 개발 중 상대 작업을 보는 도구. SessionStart hook(상대 NOW 요약)과 UserPromptSubmit hook(peer-watch, 세션 중 변화 알림)이 쓴다.
// 상대 NOW 파일은 상대 PR이 develop에 머지되기 전까지 내 작업 트리에 오지 않으므로, 원격 브랜치 중 가장 최근 것을 읽는다.
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, git, mdSection, nowFileOf, nowRelPath, readText } from './common.mjs';

export const PEER_STATE_FILE = join(ROOT, '.harness', 'peer-watch.json');
export const PEER_WATCH_INTERVAL_MS = 10 * 60 * 1000;

/** NOW 본문의 `갱신: YYYY-MM-DD HH:mm` → epoch ms (없으면 null) */
export function nowStampMs(text) {
  const m = /^갱신:\s*(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/m.exec(text || '');
  return m ? new Date(`${m[1]}T${m[2]}:00`).getTime() : null;
}

/** 후보 [{ source, text, at }] 중 갱신 시각(없으면 at)이 가장 늦은 것. 같으면 앞의 후보 */
export function pickLatestNow(cands) {
  let best = null;
  let bestAt = -Infinity;
  for (const c of cands) {
    if (!c?.text) continue;
    const at = nowStampMs(c.text) ?? c.at ?? 0;
    if (at > bestAt) {
      best = c;
      bestAt = at;
    }
  }
  return best;
}

/** NOW 요약: 갱신 줄, 현재 기능 첫 줄, 바로 다음 할 일 1번 */
export function nowHeadline(text) {
  const stamp = /^갱신:.*$/m.exec(text || '')?.[0] ?? '갱신: ?';
  const feature = mdSection(text, '현재 기능').split(/\r?\n/)[0] || '(없음)';
  const next =
    mdSection(text, '바로 다음 할 일')
      .split(/\r?\n/)
      .find((l) => /^\s*(1\.|-)\s+/.test(l))
      ?.replace(/^\s*(1\.|-)\s+/, '') || '(없음)';
  return { stamp, feature, next };
}

/** 사람 key의 NOW 중 가장 최근 것: 원격 브랜치(가장 최근 커밋)와 작업 트리 비교 → { source, text } | null */
export function latestPeerNow(key) {
  const rel = nowRelPath(key);
  const cands = [{ source: '작업 트리', text: readText(nowFileOf(key)), at: 0 }];
  const last = git(['log', '--remotes=origin', '-1', '--format=%H%x09%ct', '--', rel], { timeout: 4000 });
  const [sha, ct] = last.ok ? last.out.split('\t') : [];
  if (sha) {
    const shown = git(['show', `${sha}:${rel}`], { timeout: 4000 });
    const branch = git(['branch', '-r', '--contains', sha, '--format=%(refname:short)'], { timeout: 4000 })
      .out.split('\n')
      .find((b) => b && !b.endsWith('/HEAD'));
    if (shown.ok) cands.push({ source: branch || sha.slice(0, 7), text: shown.out, at: Number(ct) * 1000 });
  }
  return pickLatestNow(cands);
}

export function readPeerState() {
  try {
    return JSON.parse(readText(PEER_STATE_FILE) || 'null');
  } catch {
    return null;
  }
}

export function writePeerState(state) {
  try {
    mkdirSync(join(ROOT, '.harness'), { recursive: true });
    writeFileSync(PEER_STATE_FILE, JSON.stringify(state, null, 2) + '\n');
  } catch {
    // 상태 저장 실패는 알림을 한 번 더 받는 정도라 무시한다
  }
}

/** 상대의 PR 목록 { 번호: { title, state, head, sha, url } }. gh가 없거나 느리면 null */
export function peerPrs(peerGithub, { timeout = 3000 } = {}) {
  if (!peerGithub) return null;
  const r = spawnSync(
    'gh',
    ['pr', 'list', '--state', 'all', '--limit', '15', '--author', peerGithub, '--json', 'number,title,state,headRefName,headRefOid,url'],
    { cwd: ROOT, encoding: 'utf8', windowsHide: true, timeout },
  );
  if (r.status !== 0) return null;
  try {
    const prs = {};
    for (const p of JSON.parse(r.stdout)) prs[p.number] = { title: p.title, state: p.state, head: p.headRefName, sha: p.headRefOid, url: p.url };
    return prs;
  } catch {
    return null;
  }
}

/** origin/develop의 prev..curr 사이 커밋·변경 파일 */
export function developChanges(prevSha, currSha) {
  if (!prevSha || !currSha || prevSha === currSha) return null;
  const log = git(['log', '--format=%h%x09%an%x09%s', '-10', `${prevSha}..${currSha}`]);
  if (!log.ok) return null;
  return {
    commits: log.out.split('\n').filter(Boolean).map((l) => {
      const [sha, author, subject] = l.split('\t');
      return { sha, author, subject };
    }),
    files: git(['diff', '--name-only', prevSha, currSha]).out.split('\n').filter(Boolean),
  };
}

/**
 * 직전 스냅샷과 비교해 알릴 변화(마크다운 줄 목록). 순수 함수.
 * state: { developSha, develop?: developChanges 결과, peerNow: { source, stamp, feature, next }, prs: {...}|null }
 * myNames: 내 커밋 작성자 이름들(내 PR 머지만 있으면 develop 변화를 알리지 않는다)
 */
export function peerDigest(prev, curr, { peerName = '상대', myNames = [] } = {}) {
  if (!prev) return [];
  const lines = [];

  const dev = curr.develop;
  if (dev?.commits?.length && !dev.commits.every((c) => myNames.includes(c.author))) {
    const migrations = dev.files.filter((f) => /db\/migration\/V[^/]*\.sql$/.test(f)).map((f) => f.split('/').at(-1));
    const notes = [migrations.length ? `새 마이그레이션 ${migrations.join(', ')}` : '', dev.files.includes('docs/api/openapi.json') ? 'openapi.json 변경' : '']
      .filter(Boolean)
      .join(', ');
    lines.push(
      `- develop에 새 커밋 ${dev.commits.length}개${notes ? ` (${notes})` : ''}: ${dev.commits
        .slice(0, 3)
        .map((c) => c.subject)
        .join(' / ')}. 마이그레이션·API를 추가하거나 PR을 올리기 전에 \`git merge origin/develop\``,
    );
  }

  if (curr.peerNow && curr.peerNow.stamp !== prev.peerNow?.stamp) {
    lines.push(`- ${peerName} NOW 갱신 (${curr.peerNow.source}): ${curr.peerNow.feature} / 다음: ${curr.peerNow.next}`);
  }

  if (prev.prs && curr.prs) {
    for (const [n, p] of Object.entries(curr.prs)) {
      const before = prev.prs[n];
      const link = `PR #${n} ${p.title}`;
      if (!before) lines.push(`- ${peerName} ${link} ${p.state === 'MERGED' ? '머지됨' : p.state === 'OPEN' ? '열림' : '닫힘'} (${p.head})`);
      else if (before.state !== p.state) lines.push(`- ${peerName} ${link} ${p.state === 'MERGED' ? '머지됨 → develop 합치기 대상' : p.state === 'OPEN' ? '다시 열림' : '닫힘'}`);
      else if (p.state === 'OPEN' && before.sha !== p.sha) lines.push(`- ${peerName} ${link}에 새 커밋 push`);
    }
  }
  return lines;
}
