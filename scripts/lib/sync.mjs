// 자동 동기화 (11-19 결정): 세션 시작 hook과 peer-watch hook이 git fetch 직후 부른다.
//   1) 내 브랜치의 원격(@{u})에 다른 PC가 push한 커밋 → 받는다 (같은 사람의 PC 교대)
//   2) 기능 브랜치(feature/·fix/)면 상대가 develop에 머지한 커밋(origin/develop) → git merge (rebase 금지 규칙 그대로)
// 안전 규칙: 커밋 안 된 변경(추적 파일)이 있거나 병합 중이면 건드리지 않고 알리기만 한다. 충돌하면 바로 merge --abort로
// 되돌리고 충돌 파일을 알린다. push는 하지 않는다 (다음 커밋 push 때 같이 올라간다. Codex 재리뷰를 불필요하게 일으키지 않음).
// 끄기: PC마다 `git config vibenote.autoSync false` (다시 켜기: `git config --unset vibenote.autoSync`).
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, currentBranch, git, isMerging } from './common.mjs';

const DEVELOP_TARGET = /^(feature|fix)\//;

// hook 제한(15초) 안에서 fetch와 함께 끝나야 한다. 로컬 병합은 보통 1초 안이다
const MERGE_TIMEOUT = 6000;

export function autoSyncEnabled(cwd = ROOT) {
  return git(['config', '--get', 'vibenote.autoSync'], { cwd }).out.toLowerCase() !== 'false';
}

/**
 * 할 일을 고른다 (순수 함수).
 * state: { branch, merging, dirty: string[], upstream: { ref, ahead, behind } | null, developBehind: number }
 * → { actions: [{ ref, label, ffOnly }], blocked: string | null }
 */
export function syncPlan({ branch, merging, dirty = [], upstream, developBehind = 0 }) {
  if (!branch) return { actions: [], blocked: null };
  const actions = [];
  const isProtected = PROTECTED_BRANCHES.includes(branch);
  if (upstream?.behind > 0) {
    // 보호 브랜치에는 merge 커밋을 만들지 않는다 (fast-forward만)
    actions.push({ ref: upstream.ref, label: `원격 ${upstream.ref}`, ffOnly: isProtected });
  }
  // develop은 기능 브랜치(feature/·fix/)에만 합친다 (실험용 로컬 브랜치 등은 건드리지 않는다)
  if (DEVELOP_TARGET.test(branch) && developBehind > 0 && upstream?.ref !== 'origin/develop') {
    actions.push({ ref: 'origin/develop', label: 'origin/develop', ffOnly: false });
  }
  if (!actions.length) return { actions, blocked: null };
  if (merging) return { actions: [], blocked: '병합이 진행 중이라' };
  if (dirty.length) return { actions: [], blocked: `커밋 안 된 변경 ${dirty.length}개가 있어` };
  return { actions, blocked: null };
}

/** 이번 병합으로 들어온 파일 중 알릴 것 (새 마이그레이션, openapi.json) */
export function mergeNotes(files) {
  const migrations = files.filter((f) => /db\/migration\/V[^/]*\.sql$/.test(f)).map((f) => f.split('/').at(-1));
  return [migrations.length ? `새 마이그레이션 ${migrations.join(', ')}` : '', files.includes('docs/api/openapi.json') ? 'openapi.json 변경' : ''].filter(
    Boolean,
  );
}

/**
 * 양쪽(내 브랜치·합친 대상)이 모두 고친 파일 중 `merge=union`인 것 (#86).
 * 로컬 git은 union으로 충돌 없이 합치지만 GitHub PR 머지는 이 속성을 쓰지 않아, 병합 커밋을 push하기 전까지 PR이 충돌로 보인다.
 */
function unionMergedFiles(before, target, cwd) {
  const base = git(['merge-base', before, target], { cwd });
  if (!base.ok || !base.out) return [];
  const changed = (to) => git(['diff', '--name-only', base.out, to], { cwd }).out.split('\n').filter(Boolean);
  const theirs = new Set(changed(target));
  const both = changed(before).filter((f) => theirs.has(f));
  if (!both.length) return [];
  // 출력: "<경로>: merge: union"
  return git(['check-attr', 'merge', '--', ...both], { cwd })
    .out.split('\n')
    .filter((l) => l.endsWith(': merge: union'))
    .map((l) => l.slice(0, -': merge: union'.length));
}

const firstLine = (r) => (r.status === null ? '시간 초과' : (r.err || r.out || `exit ${r.status}`).split('\n')[0]);

function readState(cwd) {
  const branch = currentBranch(cwd);
  const dirty = git(['status', '--porcelain', '--untracked-files=no'], { cwd }).out.split('\n').filter(Boolean);
  let upstream = null;
  const ref = git(['rev-parse', '--abbrev-ref', '--symbolic-full-name', '@{u}'], { cwd });
  if (ref.ok && ref.out) {
    const ab = git(['rev-list', '--left-right', '--count', `HEAD...${ref.out}`], { cwd });
    if (ab.ok) {
      const [ahead, behind] = ab.out.split(/\s+/).map(Number);
      upstream = { ref: ref.out, ahead, behind };
    }
  }
  const hasDevelop = git(['rev-parse', '--verify', '-q', 'origin/develop'], { cwd }).ok;
  const developBehind = hasDevelop ? Number(git(['rev-list', '--count', 'HEAD..origin/develop'], { cwd }).out) || 0 : 0;
  return { branch, merging: isMerging(cwd), dirty, upstream, developBehind };
}

function mergeOne({ ref, label, ffOnly }, cwd) {
  const before = git(['rev-parse', 'HEAD'], { cwd }).out;
  // 앞 단계(내 원격 받기)에 이미 들어왔으면 할 일이 없다 (다른 PC가 develop을 합쳐 push한 경우)
  const count = Number(git(['rev-list', '--count', `HEAD..${ref}`], { cwd }).out) || 0;
  if (count === 0) return { ok: true, line: null };
  const target = git(['rev-parse', ref], { cwd }).out;
  const r = git(['merge', ffOnly ? '--ff-only' : '--no-edit', ref], { cwd, timeout: MERGE_TIMEOUT });
  if (r.ok) {
    const files = git(['diff', '--name-only', before, 'HEAD'], { cwd }).out.split('\n').filter(Boolean);
    const notes = mergeNotes(files);
    // fast-forward가 아니라 내 쪽에 병합 커밋을 새로 만들었는지 (push가 필요한지)
    const mergeCommit = git(['rev-parse', '-q', '--verify', 'HEAD^2'], { cwd }).ok && git(['rev-parse', 'HEAD^1'], { cwd }).out === before;
    const union = mergeCommit ? unionMergedFiles(before, target, cwd) : [];
    return {
      ok: true,
      mergeCommit,
      union,
      line: `- ✅ ${label}의 새 커밋 ${count}개를 자동으로 합쳤습니다${notes.length ? ` (${notes.join(', ')})` : ''}.`,
    };
  }
  const conflicts = git(['diff', '--name-only', '--diff-filter=U'], { cwd }).out.split('\n').filter(Boolean);
  // 이번에 내가 시작한 병합(MERGE_HEAD가 대상 커밋)만 되돌린다. 같은 PC의 다른 세션·터미널이 시작한 병합은 건드리지 않는다
  const ours = isMerging(cwd) && git(['rev-parse', '-q', '--verify', 'MERGE_HEAD'], { cwd }).out === target;
  if (ours) git(['merge', '--abort'], { cwd });
  if (conflicts.length && ours) {
    const files = `${conflicts.slice(0, 5).join(', ')}${conflicts.length > 5 ? ' 외' : ''}`;
    return { ok: false, line: `- ⚠️ ${label} 자동 병합: 충돌(${files})이 나서 되돌렸습니다. 직접 \`git merge ${ref}\` 후 충돌을 해결하세요.` };
  }
  // 충돌이 아닌 실패(타임아웃, 파일 잠금, index.lock): 시작 전에는 깨끗했으므로 무엇이든 남았으면 분명히 알린다
  const lock = git(['rev-parse', '--git-path', 'index.lock'], { cwd }).out;
  const leftover =
    git(['rev-parse', 'HEAD'], { cwd }).out !== before ||
    isMerging(cwd) ||
    git(['status', '--porcelain', '--untracked-files=no'], { cwd }).out !== '' ||
    (lock !== '' && existsSync(resolve(cwd, lock)));
  if (leftover) {
    return {
      ok: false,
      line: `- ⚠️ ${label} 자동 병합이 도중에 실패해 작업 트리·병합 상태가 남았을 수 있습니다 (${firstLine(r)}). \`git status\`와 \`.git/index.lock\`을 확인하세요.`,
    };
  }
  const why = ffOnly ? 'fast-forward가 안 되어 건너뜀 (로컬에만 있는 커밋이 있음)' : `실패해서 건너뜀 (${firstLine(r)})`;
  return { ok: false, line: `- ⚠️ ${label} 자동 병합: ${why}. 직접 \`git merge ${ref}\`로 합치세요.` };
}

/** 체크아웃하지 않아도 원격을 따라가게 할 로컬 브랜치 (#67: 로컬 develop이 10커밋 뒤처져 있었다) */
export const TRACKED_LOCAL = ['develop', 'main'];

/**
 * 체크아웃 안 된 로컬 develop·main 중 fast-forward할 것과 알릴 것 (순수 함수).
 * refs: [{ name, local, remote, ahead, behind }] (ahead·behind는 로컬 대 origin/<name>), checkedOut: 다른 worktree의 브랜치
 * → { ff: [{ name, behind }], warn: [{ name, ahead }] }. 로컬에만 있는 커밋이 있으면 FF할 수 없어 알리기만 한다.
 */
export function localRefPlan({ current, checkedOut = [], refs }) {
  const ff = [];
  const warn = [];
  for (const r of refs) {
    if (!r.local || !r.remote || r.name === current || checkedOut.includes(r.name)) continue;
    if (r.ahead > 0) warn.push({ name: r.name, ahead: r.ahead });
    else if (r.behind > 0) ff.push({ name: r.name, behind: r.behind });
  }
  return { ff, warn };
}

function readLocalRefs(cwd) {
  const refs = TRACKED_LOCAL.map((name) => {
    const local = git(['rev-parse', '--verify', '-q', `refs/heads/${name}`], { cwd }).ok;
    const remote = git(['rev-parse', '--verify', '-q', `refs/remotes/origin/${name}`], { cwd }).ok;
    const ab = local && remote ? git(['rev-list', '--left-right', '--count', `refs/heads/${name}...refs/remotes/origin/${name}`], { cwd }) : null;
    const [ahead, behind] = ab?.ok ? ab.out.split(/\s+/).map(Number) : [0, 0];
    return { name, local, remote, ahead, behind };
  });
  // 다른 worktree에 체크아웃된 브랜치는 git이 갱신을 거부한다 (그 작업 트리가 따라가지 않으므로)
  const checkedOut = git(['worktree', 'list', '--porcelain'], { cwd })
    .out.split('\n')
    .map((l) => /^branch refs\/heads\/(.+)$/.exec(l)?.[1])
    .filter(Boolean);
  return { current: currentBranch(cwd), checkedOut, refs };
}

/** 로컬 develop·main을 origin으로 fast-forward한다. 작업 트리를 건드리지 않으므로 커밋 안 된 변경이 있어도 한다 */
function syncLocalRefs(cwd) {
  const { ff, warn } = localRefPlan(readLocalRefs(cwd));
  const done = [];
  const lines = [];
  for (const { name, behind } of ff) {
    // '+' 없는 refspec이라 fast-forward가 아니면 git이 거부한다. 사람이 받아 둔 FETCH_HEAD는 덮지 않는다
    const r = git(['fetch', '--quiet', '--no-write-fetch-head', '.', `refs/remotes/origin/${name}:refs/heads/${name}`], { cwd, timeout: MERGE_TIMEOUT });
    if (r.ok) done.push(`${name} +${behind}`);
    else lines.push(`- ⚠️ 로컬 ${name}을 origin/${name}으로 최신화하지 못했습니다 (${firstLine(r)}).`);
  }
  if (done.length) lines.unshift(`- 📥 로컬 ${done.join(' · ')} 커밋을 원격에 맞췄습니다 (fast-forward, 체크아웃 안 된 브랜치).`);
  for (const { name, ahead } of warn) {
    lines.push(`- ⚠️ 로컬 ${name}에 origin/${name}에 없는 커밋 ${ahead}개가 있어 최신화하지 않았습니다. \`git log origin/${name}..${name}\`으로 확인하세요 (${name}에 직접 커밋 금지).`);
  }
  return lines;
}

/**
 * fetch 뒤에 부른다. 알릴 줄 목록을 돌려준다 (할 일이 없으면 빈 배열). 어떤 오류도 던지지 않는다.
 * 지금 브랜치를 합친 뒤 체크아웃 안 된 로컬 develop·main도 fast-forward한다.
 * @returns {string[]}
 */
export function autoSync({ cwd = ROOT } = {}) {
  try {
    if (!autoSyncEnabled(cwd)) return [];
    const plan = syncPlan(readState(cwd));
    const localLines = () => {
      try {
        return syncLocalRefs(cwd);
      } catch {
        return [];
      }
    };
    if (plan.blocked) {
      return [`- ⏸️ 받을 새 커밋이 있지만 ${plan.blocked} 자동 병합을 미뤘습니다. 커밋한 뒤 다음 확인 때 합치거나 직접 \`git merge\` 하세요.`, ...localLines()];
    }
    const lines = [];
    let mergeCommit = false;
    const union = new Set();
    for (const action of plan.actions) {
      const result = mergeOne(action, cwd);
      if (result.line) lines.push(result.line);
      mergeCommit ||= Boolean(result.mergeCommit);
      for (const f of result.union ?? []) union.add(f);
      if (!result.ok) break;
    }
    if (lines.some((l) => l.includes('마이그레이션') || l.includes('openapi.json'))) {
      lines.push('- 마이그레이션·openapi.json이 들어왔으면 `node scripts/verify.mjs be`로 확인하세요.');
    }
    if (union.size) {
      lines.push(
        // 세션 배너는 110자에서 자르므로 할 일(git push)을 앞에 둔다
        `- ⚠️ 열린 PR이 있으면 지금 \`git push\`: 양쪽이 고친 ${[...union].join(', ')}는 로컬에서 merge=union으로 합쳤지만 GitHub PR은 union을 쓰지 않아 push 전까지 충돌로 보입니다 (#86).`,
      );
    } else if (mergeCommit) {
      lines.push('- 병합 커밋은 아직 push하지 않았습니다 (다음 push 때 같이 올라갑니다).');
    }
    return [...lines, ...localLines()];
  } catch {
    return [];
  }
}
