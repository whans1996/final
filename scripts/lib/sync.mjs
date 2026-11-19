// 자동 동기화 (11-19 결정): 세션 시작 hook과 peer-watch hook이 git fetch 직후 부른다.
//   1) 내 브랜치의 원격(@{u})에 다른 PC가 push한 커밋 → 받는다 (같은 사람의 PC 교대)
//   2) feature 브랜치면 상대가 develop에 머지한 커밋(origin/develop) → git merge (rebase 금지 규칙 그대로)
// 안전 규칙: 커밋 안 된 변경(추적 파일)이 있거나 병합 중이면 건드리지 않고 알리기만 한다. 충돌하면 바로 merge --abort로
// 되돌리고 충돌 파일을 알린다. push는 하지 않는다 (다음 커밋 push 때 같이 올라간다. Codex 재리뷰를 불필요하게 일으키지 않음).
// 끄기: PC마다 `git config vibenote.autoSync false` (다시 켜기: `git config --unset vibenote.autoSync`).
import { PROTECTED_BRANCHES, ROOT, currentBranch, git, isMerging } from './common.mjs';

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
  if (!isProtected && developBehind > 0 && upstream?.ref !== 'origin/develop') {
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
  const count = Number(git(['rev-list', '--count', `HEAD..${ref}`], { cwd }).out) || 0;
  const r = git(['merge', ffOnly ? '--ff-only' : '--no-edit', ref], { cwd, timeout: MERGE_TIMEOUT });
  if (r.ok) {
    const files = git(['diff', '--name-only', before, 'HEAD'], { cwd }).out.split('\n').filter(Boolean);
    const notes = mergeNotes(files);
    // fast-forward가 아니라 내 쪽에 병합 커밋을 새로 만들었는지 (push가 필요한지)
    const mergeCommit = git(['rev-parse', '-q', '--verify', 'HEAD^2'], { cwd }).ok && git(['rev-parse', 'HEAD^1'], { cwd }).out === before;
    return { ok: true, mergeCommit, line: `- ✅ ${label}의 새 커밋 ${count}개를 자동으로 합쳤습니다${notes.length ? ` (${notes.join(', ')})` : ''}.` };
  }
  const conflicts = git(['diff', '--name-only', '--diff-filter=U'], { cwd }).out.split('\n').filter(Boolean);
  if (isMerging(cwd)) git(['merge', '--abort'], { cwd });
  const why = conflicts.length
    ? `충돌(${conflicts.slice(0, 5).join(', ')}${conflicts.length > 5 ? ' 외' : ''})이 나서 되돌렸습니다`
    : ffOnly
      ? 'fast-forward가 안 되어 건너뜀 (로컬에만 있는 커밋이 있음)'
      : `실패해서 건너뜀 (${(r.err || 'timeout').split('\n')[0]})`;
  return { ok: false, line: `- ⚠️ ${label} 자동 병합: ${why}. 직접 \`git merge ${ref}\` 후 충돌을 해결하세요.` };
}

/**
 * fetch 뒤에 부른다. 알릴 줄 목록을 돌려준다 (할 일이 없으면 빈 배열). 어떤 오류도 던지지 않는다.
 * @returns {string[]}
 */
export function autoSync({ cwd = ROOT } = {}) {
  try {
    if (!autoSyncEnabled(cwd)) return [];
    const plan = syncPlan(readState(cwd));
    if (plan.blocked) {
      return [`- ⏸️ 받을 새 커밋이 있지만 ${plan.blocked} 자동 병합을 미뤘습니다. 커밋한 뒤 다음 확인 때 합치거나 직접 \`git merge\` 하세요.`];
    }
    const lines = [];
    let mergeCommit = false;
    for (const action of plan.actions) {
      const result = mergeOne(action, cwd);
      lines.push(result.line);
      mergeCommit ||= Boolean(result.mergeCommit);
      if (!result.ok) break;
    }
    if (lines.some((l) => l.includes('마이그레이션') || l.includes('openapi.json'))) {
      lines.push('- 마이그레이션·openapi.json이 들어왔으면 `node scripts/verify.mjs be`로 확인하세요.');
    }
    if (mergeCommit) lines.push('- 병합 커밋은 아직 push하지 않았습니다 (다음 push 때 같이 올라갑니다).');
    return lines;
  } catch {
    return [];
  }
}
