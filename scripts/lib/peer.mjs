// 병렬 개발 중 상대 작업을 보는 도구. SessionStart hook(상대 NOW 요약)과 UserPromptSubmit hook(peer-watch, 세션 중 변화 알림)이 쓴다.
// 상대 NOW 파일은 상대 PR이 develop에 머지되기 전까지 내 작업 트리에 오지 않으므로, 원격 브랜치마다 읽어 갱신 시각이 가장 늦은 것을 고른다.
// 내 NOW도 PC마다 다른 브랜치에서 갈라질 수 있어(#54), develop에 머지 안 된 브랜치 중 내 NOW를 바꾼 것을 따로 모은다.
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, git, mdSection, nowRelPath, nowStamp, readText } from './common.mjs';
import { mergeNotes } from './sync.mjs';

export const PEER_STATE_FILE = join(ROOT, '.harness', 'peer-watch.json');
export const PEER_WATCH_INTERVAL_MS = 10 * 60 * 1000;
// 로컬 git(log·show·branch·diff)은 보통 수십 ms다. hook 제한(15초) 안에 fetch·gh와 함께 끝나도록 짧게 자른다
const LOCAL_GIT_TIMEOUT = 1500;

/** NOW 본문의 `갱신: YYYY-MM-DD HH:mm` → epoch ms (없으면 null) */
export function nowStampMs(text) {
  const m = /^갱신:\s*(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/m.exec(text || '');
  return m ? new Date(`${m[1]}T${m[2]}:00`).getTime() : null;
}

/**
 * 같은 NOW가 여러 곳에 있을 때 출처로 보일 순위. develop을 합친 내 브랜치·작업 트리에도 상대 NOW가 들어오므로
 * 갱신 줄의 브랜치(상대가 실제로 쓴 곳) > develop·main(그 브랜치가 머지되어 지워졌을 때 거쳐 온 곳) > 그 밖의 원격 브랜치 > 작업 트리 (#29)
 */
function sourceRank(c) {
  const branch = /^갱신:.*?브랜치: (\S+)/m.exec(c.text)?.[1];
  if (branch && c.source === `origin/${branch}`) return 3;
  if (/^origin\/(develop|main)$/.test(c.source)) return 2;
  return c.source.startsWith('origin/') ? 1 : 0;
}

/** 후보 [{ source, text, at }] 중 갱신 시각(없으면 at)이 가장 늦은 것. 같으면 sourceRank가 높은 것, 그래도 같으면 앞의 후보 */
export function pickLatestNow(cands) {
  let best = null;
  let bestAt = -Infinity;
  for (const c of cands) {
    if (!c?.text) continue;
    const at = nowStampMs(c.text) ?? c.at ?? 0;
    if (at > bestAt || (at === bestAt && sourceRank(c) > sourceRank(best))) {
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

/** origin 원격 브랜치 [{ branch: 'origin/x', name: 'x', at }] (최근 커밋 순, HEAD 제외). noMerged를 주면 그 ref에 머지 안 된 것만. 조회 실패면 null */
function remoteBranches(cwd, { noMerged, timeout = LOCAL_GIT_TIMEOUT } = {}) {
  const args = ['for-each-ref', '--sort=-committerdate', '--format=%(refname:lstrip=3)%09%(committerdate:unix)'];
  if (noMerged) args.push(`--no-merged=${noMerged}`);
  const r = git([...args, 'refs/remotes/origin/'], { cwd, timeout });
  if (!r.ok) return null;
  return r.out
    .split('\n')
    .map((l) => l.split('\t'))
    .filter(([name]) => name && name !== 'HEAD')
    .map(([name, ct]) => ({ branch: `origin/${name}`, name, at: Number(ct) * 1000 }));
}

/** `<rev>:<path>` 여러 개를 git 한 번으로 읽는다 (원격 브랜치가 많아도 hook 제한 안에 끝나게) → [{ sha, text }]. 없는 것은 null */
function readBlobs(specs, cwd, timeout = LOCAL_GIT_TIMEOUT) {
  if (!specs.length) return [];
  const r = spawnSync('git', ['cat-file', '--batch'], { cwd, input: specs.join('\n') + '\n', windowsHide: true, timeout });
  if (r.status !== 0 || !r.stdout) return specs.map(() => null);
  const buf = r.stdout;
  let pos = 0;
  return specs.map(() => {
    const nl = buf.indexOf(0x0a, pos);
    if (nl < 0) return null;
    const header = buf.subarray(pos, nl).toString('utf8');
    pos = nl + 1;
    // 없으면 "<spec> missing" 한 줄뿐이다
    const m = /^([0-9a-f]+) (\w+) (\d+)$/.exec(header);
    if (!m) return null;
    const size = Number(m[3]);
    const blob = m[2] === 'blob' ? { sha: m[1], text: buf.subarray(pos, pos + size).toString('utf8') } : null;
    pos += size + 1;
    return blob;
  });
}

/**
 * 사람 key의 NOW 중 가장 최근 것: 작업 트리와 원격 브랜치마다의 NOW를 갱신 시각으로 비교 → { source, text } | null.
 * NOW를 바꾼 커밋 1개만 보면 나중에 머지된 옛 브랜치의 NOW가 다른 브랜치의 새 NOW를 가린다 (#32)
 * timeoutMs: git 두 번(브랜치 목록·NOW 읽기)에 쓸 전체 시간 (H-5, 세션 시작 예산의 남은 시간). 0 이하면 git 없이 작업 트리만 본다
 */
export function latestPeerNow(key, { cwd = ROOT, timeoutMs = LOCAL_GIT_TIMEOUT } = {}) {
  const rel = nowRelPath(key);
  const started = Date.now();
  const branches = timeoutMs > 0 ? (remoteBranches(cwd, { timeout: timeoutMs }) ?? []) : [];
  const blobs = readBlobs(
    branches.map((b) => `${b.branch}:${rel}`),
    cwd,
    Math.max(1, timeoutMs - (Date.now() - started)),
  );
  return pickLatestNow([
    { source: '작업 트리', text: readText(join(cwd, rel)), at: 0 },
    ...branches.map((b, i) => ({ source: b.branch, text: blobs[i]?.text ?? null, at: b.at })),
  ]);
}

/** 세션 시작 hook 제한(15초) 안에서 내 브랜치 조회에 쓰는 시간 */
export const MY_BRANCH_BUDGET_MS = 3000;

/**
 * base(develop)에 머지 안 된 원격 브랜치 중 base 밖의 커밋으로 key의 NOW를 바꾼 것 → { branches: [{ branch, at, text }], truncated } (최근 커밋 순).
 * 같은 사람이 PC마다 다른 브랜치를 진행하면 develop의 NOW가 다른 브랜치의 진행을 가린다 (#54).
 * - 상대 브랜치가 develop을 합쳐 들여온 내 NOW는 develop 쪽 부모와 같아 log에 나오지 않는다
 * - squash 머지 뒤 남은 원격 브랜치는 --no-merged에 걸리지만, 그 NOW 내용(blob)이 develop 이력에 있으므로 뺀다
 * - 브랜치마다 git을 한 번씩 부르므로 budgetMs를 넘기면 남은 브랜치는 보지 않고 truncated로 알린다
 */
export function myBranchNows(key, { cwd = ROOT, base = 'origin/develop', budgetMs = MY_BRANCH_BUDGET_MS } = {}) {
  const rel = nowRelPath(key);
  const started = Date.now();
  // 호출마다 줄 제한 시간: 기본 상한과 남은 budgetMs 중 작은 것 (0이 되면 1ms로 바로 끝나 결과 없음으로 본다)
  const remaining = () => Math.max(1, Math.min(LOCAL_GIT_TIMEOUT, budgetMs - (Date.now() - started)));
  const touched = [];
  let truncated = false;
  for (const b of remoteBranches(cwd, { noMerged: base }) ?? []) {
    if (PROTECTED_BRANCHES.includes(b.name)) continue;
    if (Date.now() - started > budgetMs) {
      truncated = true;
      break;
    }
    // 마지막 호출이 budgetMs를 넘기지 않게 남은 시간만 준다 (H-5, reviewer)
    if (git(['log', '-1', '--format=%H', `${base}..${b.branch}`, '--', rel], { cwd, timeout: remaining() }).out) touched.push(b);
  }
  if (!touched.length) return { branches: [], truncated };
  // 시간이 모자라 develop 이력을 못 읽으면 squash 머지된 브랜치를 거르지 못해 거짓 경고가 나므로 생략으로 알린다
  const onBase = blobsInHistory(base, rel, cwd, remaining());
  if (!onBase) return { branches: [], truncated: true };
  const blobs = readBlobs(
    touched.map((b) => `${b.branch}:${rel}`),
    cwd,
    remaining(),
  );
  return {
    branches: touched
      .map((b, i) => ({ branch: b.branch, at: b.at, blob: blobs[i] }))
      .filter((b) => b.blob && !onBase.has(b.blob.sha))
      .map(({ branch, at, blob }) => ({ branch, at, text: blob.text })),
    truncated,
  };
}

/** `--raw` 줄 ":<old mode> <new mode> <old sha> <new sha> <status>\t<path>" → { sha(새 내용), path }. 지운 파일은 null */
function rawEntry(line) {
  const m = /^:\d+ \d+ [0-9a-f]+ ([0-9a-f]+) ([A-Z])\d*\t(.+)$/.exec(line);
  return m && m[2] !== 'D' ? { sha: m[1], path: m[3] } : null;
}

/** ref의 이력에서 paths(파일·디렉터리)가 가졌던 내용(blob sha) 전체. 조회 실패면 null. 머지로 들어온 쪽 커밋도 보게 --full-history */
function blobsInHistory(ref, paths, cwd, timeout = LOCAL_GIT_TIMEOUT) {
  const r = git(['log', '--full-history', '--no-renames', '--format=', '--raw', '--no-abbrev', ref, '--', paths], { cwd, timeout });
  if (!r.ok) return null;
  return new Set(
    r.out
      .split('\n')
      .map(rawEntry)
      .filter(Boolean)
      .map((e) => e.sha),
  );
}

/**
 * /spec 가드 (#54): base(develop)에 머지 안 된 원격 브랜치 중 기능 fid(F-14, F-05a)를 진행 중인 것 → [{ branch, specs, here, merged }] (최근 커밋 순).
 * 브랜치 이름에 `F-14-`가 있거나, 그 브랜치가 갈라진 뒤 직접 고친(`base...branch`) `docs/specs/F-14-*.md` 중 내용이 base 이력에 없는 것이 있으면 해당한다.
 * 브랜치 끝끼리 비교하면 develop보다 뒤처진 무관한 브랜치가 물려받은 옛 spec도 진행으로 잡힌다 (Codex 리뷰).
 * - here: current(지금 브랜치)와 같은 브랜치. 승인 뒤 같은 브랜치에서 /spec을 다시 실행하는 것은 막지 않는다
 * - merged: 이름만 맞고, 고친 그 기능 spec이 모두 base 이력에 있다 → squash 머지 뒤 남은 원격 브랜치로 본다 (멈출 대상 아님)
 * base나 브랜치 목록·변경을 못 읽으면 null(판정 불가): 빈 목록이면 가드가 거짓 통과(✅)를 낸다
 */
export function featureOnRemotes(fid, { cwd = ROOT, base = 'origin/develop', current = null, listBranches = remoteBranches } = {}) {
  if (!git(['rev-parse', '--verify', '-q', `${base}^{commit}`], { cwd, timeout: LOCAL_GIT_TIMEOUT }).ok) return null;
  const branches = listBranches(cwd, { noMerged: base });
  const onBase = blobsInHistory(base, 'docs/specs/', cwd);
  if (!branches || !onBase) return null;
  const prefix = `${fid}-`.toLowerCase();
  const isFid = (p) => p.split('/').at(-1).toLowerCase().startsWith(prefix);
  const found = [];
  for (const b of branches) {
    if (PROTECTED_BRANCHES.includes(b.name)) continue;
    const d = git(['diff', '--raw', '--no-abbrev', '--no-renames', `${base}...${b.branch}`, '--', 'docs/specs/'], { cwd, timeout: LOCAL_GIT_TIMEOUT });
    if (!d.ok) return null;
    const changed = d.out
      .split('\n')
      .map(rawEntry)
      .filter((e) => e && isFid(e.path));
    const specs = changed.filter((e) => !onBase.has(e.sha)).map((e) => e.path);
    const named = b.name.split('/').some((part) => part.toLowerCase().startsWith(prefix));
    if (named || specs.length) found.push({ branch: b.branch, specs, here: b.name === current, merged: !specs.length && changed.length > 0 });
  }
  return found;
}

/** `갱신: 2026-11-19 17:55 · … · PC: dh-ssafy · …` → `11-19 17:55 dh-ssafy` */
function stampBrief(stamp) {
  const m = /\d{4}-(\d{2}-\d{2})\s+(\d{2}:\d{2})/.exec(stamp);
  const pc = /PC:\s*([^\s·]+)/.exec(stamp)?.[1];
  return [m ? `${m[1]} ${m[2]}` : '갱신 ?', pc].filter(Boolean).join(' ');
}

/** myBranchNows 결과 → 표시 줄과 지금 브랜치가 아닌 것 목록. NOW 갱신 시각과 브랜치 마지막 커밋 시각을 함께 보인다 (NOW 뒤에 코드만 더 push했을 수 있다). 순수 함수 */
export function myBranchLines({ branches, truncated }, current) {
  const cut = (t, n = 100) => (t.length > n ? `${t.slice(0, n - 1)}…` : t);
  const lines = [];
  const others = [];
  for (const b of branches) {
    const h = nowHeadline(b.text);
    const here = b.branch === `origin/${current}`;
    if (!here) others.push(b.branch);
    const commit = b.at ? ` · 커밋 ${nowStamp(new Date(b.at)).slice(5)}` : '';
    lines.push(`- ${here ? '(지금 브랜치)' : '⚠️'} ${b.branch} · NOW ${stampBrief(h.stamp)}${commit} · ${cut(h.feature)}`);
  }
  if (truncated) lines.push('- … 시간 제한으로 일부 생략 → 전체는 node scripts/branches.mjs');
  return { lines, others, truncated: Boolean(truncated) };
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

/**
 * origin/develop의 prev..curr 사이 커밋(전부)·첫 부모 커밋 작성자·변경 파일. 잘라서 보면 앞쪽 상대 커밋이 "전부 내 커밋" 판정에서 빠진다 (#32).
 * firstParent: develop에 직접 쌓인 커밋(= GitHub PR 병합 커밋)의 작성자. GitHub이 머지한 사람의 프로필 Name(비어 있으면 계정)으로 만들므로
 * PC마다 git user.name이 달라도 같은 사람으로 판정할 수 있다 (H-7). 프로필 Name을 계정과 다르게 정하면 내 머지도 알린다(안전한 쪽, conventions 9절)
 */
export function developChanges(prevSha, currSha, { cwd = ROOT } = {}) {
  if (!prevSha || !currSha || prevSha === currSha) return null;
  const log = git(['log', '--format=%h%x09%an%x09%s', `${prevSha}..${currSha}`], { cwd, timeout: LOCAL_GIT_TIMEOUT });
  if (!log.ok) return null;
  return {
    commits: log.out.split('\n').filter(Boolean).map((l) => {
      const [sha, author, subject] = l.split('\t');
      return { sha, author, subject };
    }),
    firstParent: git(['log', '--first-parent', '--format=%an', `${prevSha}..${currSha}`], { cwd, timeout: LOCAL_GIT_TIMEOUT })
      .out.split('\n')
      .filter(Boolean),
    files: git(['diff', '--name-only', prevSha, currSha], { cwd, timeout: LOCAL_GIT_TIMEOUT }).out.split('\n').filter(Boolean),
  };
}

/**
 * 직전 스냅샷과 비교해 알릴 변화(마크다운 줄 목록). 순수 함수.
 * state: { developSha, develop?: developChanges 결과, peerNow: { source, stamp, feature, next }, prs: {...}|null }
 * myNames: 내 커밋 작성자 이름들(내 PR 머지만 있으면 develop 변화를 알리지 않는다). 커밋 전부가 내 이름이거나,
 *   develop에 직접 쌓인 병합 커밋(firstParent)이 전부 내 계정이면 내 것으로 본다 (H-7: 다른 PC의 user.name이 달라도)
 */
export function peerDigest(prev, curr, { peerName = '상대', myNames = [] } = {}) {
  if (!prev) return [];
  const lines = [];

  const dev = curr.develop;
  const mine = (name) => myNames.includes(name);
  const allMine = dev?.commits?.every((c) => mine(c.author)) || (dev?.firstParent?.length > 0 && dev.firstParent.every(mine));
  if (dev?.commits?.length && !allMine) {
    const notes = mergeNotes(dev.files).join(', ');
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

const BANNER_MAX_LINES = 5;

/**
 * peer-watch 결과의 사람용 한 묶음 (HX-7). additionalContext는 모델에만 가므로 사용자 화면에는 systemMessage로 보인다. 순수 함수.
 * lines: peerDigest 줄, synced: autoSync 줄(✅·⚠️·⏸️로 시작하는 결과만 보인다). 보일 것이 없으면 null
 */
export function peerWatchBanner(lines, synced) {
  const cut = (t, n = 110) => (t.length > n ? `${t.slice(0, n - 1)}…` : t);
  const items = [
    ...lines.map((l) => `📣 ${cut(l.replace(/^- /, ''))}`),
    ...synced.filter((l) => /^- (✅|⚠️|⏸️)/.test(l)).map((l) => `🔄 ${cut(l.replace(/^- /, ''))}`),
  ];
  if (!items.length) return null;
  const shown = items.slice(0, BANNER_MAX_LINES);
  const rest = items.length - shown.length;
  return ['[바이브노트] 📣 상대 작업 변화 (10분마다 확인)', ...shown, ...(rest > 0 ? [`… 외 ${rest}건 (Claude에게 전달됨)`] : [])].join('\n');
}
