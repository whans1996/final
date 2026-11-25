// guard-bash hook의 명령 판정 (H-1). 정규식으로 명령 문자열 전체를 훑으면 따옴표·heredoc 안 글자에 오탐하고
// `bash -c "..."`, `git -C <경로>`, 명령 치환 안의 명령을 놓친다. 그래서 셸 토큰으로 나눠 단순 명령(segment)마다 규칙을 본다.
// bash 문법을 기준으로 하되 PowerShell 명령도 같은 연산자(; | &&)로 나뉘는 범위에서 본다. 완전한 셸 파서가 아니라 가드용 근사다.
import { basename, isAbsolute, resolve } from 'node:path';

const SUBST = '$()'; // 명령 치환 자리 표시

/**
 * 명령 문자열을 단순 명령 목록으로 나눈다. 각 항목: { words: 따옴표를 푼 단어들, redirects: [{op, target}] }.
 * 명령 치환·서브셸 안의 명령은 바깥 명령보다 먼저 나온다. heredoc 본문과 주석은 버린다.
 */
export function parseCommand(src) {
  const out = [];
  parseList(String(src || ''), 0, false, out);
  return out;
}

function parseList(src, start, nested, out) {
  let i = start;
  let words = [];
  let redirects = [];
  let word = null; // 만드는 중인 단어 (null = 단어 밖)
  let pendingRedirect = null;
  const heredocs = [];

  const endWord = () => {
    if (word === null) return;
    if (pendingRedirect) {
      redirects.push({ op: pendingRedirect, target: word });
      pendingRedirect = null;
    } else words.push(word);
    word = null;
  };
  const endSegment = () => {
    endWord();
    if (words.length || redirects.length) out.push({ words, redirects });
    words = [];
    redirects = [];
  };
  const skipHeredocBodies = () => {
    // i는 줄바꿈 바로 뒤. 대기 중인 heredoc마다 구분자 줄까지 건너뛴다
    while (heredocs.length) {
      const { delim, strip } = heredocs.shift();
      for (;;) {
        if (i >= src.length) return;
        const nl = src.indexOf('\n', i);
        const line = src.slice(i, nl === -1 ? src.length : nl);
        i = nl === -1 ? src.length : nl + 1;
        if ((strip ? line.replace(/^\t+/, '') : line).trimEnd() === delim) break;
      }
    }
  };
  const substitution = (close) => {
    // i는 여는 기호 바로 뒤
    if (close === ')') i = parseList(src, i, true, out);
    else {
      const end = src.indexOf('`', i);
      const inner = src.slice(i, end === -1 ? src.length : end);
      parseList(inner, 0, false, out);
      i = end === -1 ? src.length : end + 1;
    }
    word = (word ?? '') + SUBST;
  };

  while (i < src.length) {
    const c = src[i];
    if (nested && c === ')') {
      endSegment();
      return i + 1;
    }
    if (c === ' ' || c === '\t' || c === '\r') {
      endWord();
      i++;
    } else if (c === '\n') {
      endSegment();
      i++;
      skipHeredocBodies();
    } else if (c === '#' && word === null) {
      while (i < src.length && src[i] !== '\n') i++;
    } else if (c === ';' || c === '|' || (c === '&' && src[i + 1] !== '>')) {
      endSegment();
      i++;
      if (src[i] === c || (c === '|' && src[i] === '&')) i++;
    } else if (c === '(' && word === null) {
      endSegment();
      i = parseList(src, i + 1, true, out);
    } else if (c === '>' || c === '<' || c === '&') {
      if (word !== null && /^\d+$/.test(word)) word = null; // 2>&1 의 2
      endWord();
      const op = /^(<<<|<<-|<<|>>|>&|<&|>\||&>>|&>|<|>)/.exec(src.slice(i))[0];
      i += op.length;
      if (op === '<<' || op === '<<-') {
        while (src[i] === ' ' || src[i] === '\t') i++;
        const m = /^(?:'([^']*)'|"([^"]*)"|\\?([^\s;|&<>()]+))/.exec(src.slice(i));
        if (m) {
          heredocs.push({ delim: m[1] ?? m[2] ?? m[3], strip: op === '<<-' });
          i += m[0].length;
        }
      } else if ((op === '>&' || op === '<&') && /^[\d-]/.test(src[i] || '')) {
        while (/[\d-]/.test(src[i] || '')) i++; // 2>&1, >&-
      } else pendingRedirect = op;
    } else if (c === "'") {
      const end = src.indexOf("'", i + 1);
      word = (word ?? '') + src.slice(i + 1, end === -1 ? src.length : end);
      i = end === -1 ? src.length : end + 1;
    } else if (c === '"') {
      word = word ?? '';
      i++;
      while (i < src.length && src[i] !== '"') {
        if (src[i] === '\\' && '"\\$`\n'.includes(src[i + 1])) {
          if (src[i + 1] !== '\n') word += src[i + 1];
          i += 2;
        } else if (src[i] === '$' && src[i + 1] === '(') {
          i += 2;
          substitution(')');
        } else if (src[i] === '`') {
          i++;
          substitution('`');
        } else word += src[i++];
      }
      i++;
    } else if (c === '\\') {
      if (src[i + 1] !== '\n' && i + 1 < src.length) word = (word ?? '') + src[i + 1];
      i += 2;
    } else if (c === '$' && src[i + 1] === '(') {
      i += 2;
      substitution(')');
    } else if (c === '`') {
      i++;
      substitution('`');
    } else {
      word = (word ?? '') + c;
      i++;
    }
  }
  endSegment();
  return i;
}

// ── 규칙 ──

const cmdName = (w) => basename(String(w || '').replace(/\\/g, '/')).replace(/\.(exe|cmd|bat)$/i, '').toLowerCase();
const isAssign = (w) => /^[A-Za-z_][A-Za-z0-9_]*=/.test(w);
const WRAPPERS = new Set(['sudo', 'command', 'exec', 'nohup', 'time', 'builtin']);

/** env 할당·래퍼를 건너뛴 실제 명령과 인자 */
function unwrap(words) {
  let k = 0;
  for (;;) {
    while (k < words.length && isAssign(words[k])) k++;
    const name = cmdName(words[k]);
    if (WRAPPERS.has(name)) k++;
    else if (name === 'env') {
      k++;
      while (k < words.length && (isAssign(words[k]) || words[k].startsWith('-'))) k++;
    } else break;
  }
  return { name: cmdName(words[k]), args: words.slice(k + 1) };
}

/** 셸 래퍼(bash -c, powershell -Command, cmd /c, eval)가 실행하는 문자열 */
function innerScript(name, args) {
  if (['bash', 'sh', 'zsh', 'dash'].includes(name)) {
    const k = args.findIndex((a) => /^-[a-z]*c[a-z]*$/.test(a));
    return k === -1 ? null : (args[k + 1] ?? null);
  }
  if (['powershell', 'pwsh'].includes(name)) {
    const k = args.findIndex((a) => /^-(c|command)$/i.test(a));
    return k === -1 ? null : args.slice(k + 1).join(' ');
  }
  if (name === 'cmd') {
    const k = args.findIndex((a) => /^\/[ck]$/i.test(a));
    return k === -1 ? null : args.slice(k + 1).join(' ');
  }
  if (name === 'eval') return args.join(' ');
  return null;
}

// git 전역 옵션 중 값을 받는 것
const GIT_GLOBAL_VALUE = new Set(['-C', '-c', '--git-dir', '--work-tree', '--namespace', '--exec-path', '--config-env']);

function parseGit(args) {
  const configs = [];
  let dir = null;
  let k = 0;
  while (k < args.length && args[k].startsWith('-')) {
    const a = args[k];
    const eq = a.indexOf('=');
    const key = a.startsWith('--') && eq > 0 ? a.slice(0, eq) : a;
    if (GIT_GLOBAL_VALUE.has(key)) {
      const v = key === a ? args[++k] : a.slice(eq + 1);
      if (key === '-C') dir = v;
      if (key === '-c') configs.push(v || '');
    }
    k++;
  }
  return { dir, configs, sub: args[k] || '', rest: args.slice(k + 1) };
}

/**
 * 옵션을 펼친다. 짧은 옵션 묶음(-am)은 글자마다, valueShort에 든 글자는 나머지(또는 다음 단어)를 값으로 먹는다.
 * 반환: { flags: Set('-a','--force',...), positional: [...] }
 */
function options(args, valueShort = '', valueLong = []) {
  const flags = new Set();
  const positional = [];
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    if (a === '--') {
      positional.push(...args.slice(k + 1));
      break;
    }
    if (a.startsWith('--')) {
      const name = a.split('=')[0];
      flags.add(name);
      if (valueLong.includes(name) && !a.includes('=')) k++;
    } else if (a.startsWith('-') && a.length > 1) {
      for (let j = 1; j < a.length; j++) {
        flags.add(`-${a[j]}`);
        if (valueShort.includes(a[j])) {
          if (j === a.length - 1) k++;
          break;
        }
      }
    } else positional.push(a);
  }
  return { flags, positional };
}

const PROTECTED = ['main', 'develop'];
const ENV_READERS = new Set(
  'cat type more less head tail gc get-content select-string grep rg sed awk code notepad cp copy bat nl strings base64 xxd od source . tac findstr'.split(' '),
);
const isEnvFile = (w) => {
  const b = basename(String(w).replace(/\\/g, '/'));
  return /^\.env([.*][\w.*-]*)?$/.test(b) && b !== '.env.example';
};

const R = {
  noVerify: 'git hook 우회(--no-verify)는 금지입니다. 테스트가 실패하면 고치거나, 교대용이면 wip(F-xx): 로 커밋하세요.',
  commitN: 'git commit -n 은 --no-verify와 같습니다. 금지입니다.',
  force: '강제 push는 금지입니다. 다른 PC에서 push한 커밋이 사라질 수 있습니다.',
  gitlab: 'GitLab(lab.ssafy.com)은 최종 제출 전용이며 push는 사람이 직접 합니다. 개발 push는 origin(GitHub)으로 하세요.',
  deleteProtected: '보호 브랜치 삭제는 금지입니다.',
  resetHard: 'git reset --hard는 금지입니다. 미커밋 작업이 사라집니다. 필요하면 사용자에게 직접 실행을 요청하세요.',
  clean: 'git clean -f는 금지입니다. 추적되지 않은 파일이 영구 삭제됩니다.',
  restoreAll: '작업 트리 전체 되돌리기는 금지입니다. 파일을 지정하거나 사용자에게 확인하세요.',
  addForce: 'git add --force는 금지입니다. .gitignore된 비밀 파일이 커밋될 수 있습니다.',
  hooksPath: 'git hook 경로 변경은 금지입니다.',
  merge: 'PR 머지는 영역 오너가 CI 초록을 확인하고 GitHub에서 직접 합니다.',
  repo: '저장소 설정 변경·삭제는 사람이 직접 합니다.',
  secret: '릴리스·시크릿·변수 변경은 사람이 직접 합니다.',
  envRead: '.env 파일 내용을 읽을 수 없습니다. 필요한 변수 이름은 .env.example을 보세요.',
  envAdd: '.env 파일은 커밋할 수 없습니다.',
};

function checkGit(args, cwd, ctx) {
  const g = parseGit(args);
  if (g.configs.some((c) => /^core\.hookspath=/i.test(c) && !/=\.githooks$/i.test(c))) return R.hooksPath;
  const rest = g.rest;
  const where = g.dir ? resolvePath(cwd, g.dir) : cwd;
  switch (g.sub) {
    case 'commit': {
      const o = options(rest, 'mFCct', ['--message', '--file', '--author', '--date', '--reuse-message', '--reedit-message', '--fixup', '--squash', '--cleanup', '--template', '--trailer', '--pathspec-from-file']);
      if (o.flags.has('--no-verify')) return R.noVerify;
      if (o.flags.has('-n')) return R.commitN;
      const branch = ctx.commitBlocked?.(where);
      if (branch) return `보호 브랜치(${branch})에 직접 커밋할 수 없습니다. git switch -c feature/F-xx-slug 로 브랜치를 만드세요.`;
      return null;
    }
    case 'push': {
      const o = options(rest, 'o', ['--push-option', '--receive-pack', '--exec', '--repo']);
      if (o.flags.has('--no-verify')) return R.noVerify;
      if (['--force', '--force-with-lease', '--force-if-includes', '--mirror', '-f'].some((f) => o.flags.has(f))) return R.force;
      const [remote = '', ...refs] = o.positional;
      if (remote === 'gitlab' || /lab\.ssafy\.com/i.test(remote)) return R.gitlab;
      if (refs.some((r) => r.startsWith('+'))) return R.force;
      const deleting = o.flags.has('--delete') || o.flags.has('-d');
      if (refs.some((r) => PROTECTED.includes(r.replace(/^:/, '').replace(/^refs\/heads\//, '')) && (deleting || r.startsWith(':')))) return R.deleteProtected;
      return null;
    }
    case 'merge':
    case 'rebase':
    case 'cherry-pick':
    case 'am':
      return options(rest).flags.has('--no-verify') ? R.noVerify : null;
    case 'reset':
      return options(rest).flags.has('--hard') ? R.resetHard : null;
    case 'clean': {
      const o = options(rest, 'e', ['--exclude']);
      return o.flags.has('-f') || o.flags.has('--force') ? R.clean : null;
    }
    case 'checkout':
    case 'restore': {
      const o = options(rest, 'sb', ['--source', '--conflict', '--pathspec-from-file']);
      if (o.flags.has('--staged') && !o.flags.has('--worktree') && g.sub === 'restore') return null;
      return o.positional.some((p) => p === '.' || p === ':/' || p === '*') ? R.restoreAll : null;
    }
    case 'add': {
      const o = options(rest, '', ['--chmod', '--pathspec-from-file']);
      if (o.flags.has('-f') || o.flags.has('--force')) return R.addForce;
      return o.positional.some(isEnvFile) ? R.envAdd : null;
    }
    case 'config': {
      const o = options(rest, 'f', ['--file', '--blob', '--type', '--default']);
      const keyIdx = o.positional.findIndex((p) => /^core\.hookspath$/i.test(p));
      if (keyIdx === -1) return null;
      if (o.flags.has('--unset') || o.flags.has('--unset-all')) return R.hooksPath;
      const value = o.positional[keyIdx + 1];
      return value !== undefined && value !== '.githooks' ? R.hooksPath : null;
    }
    case 'branch': {
      const o = options(rest);
      const del = o.flags.has('-D') || ((o.flags.has('-d') || o.flags.has('--delete')) && (o.flags.has('-f') || o.flags.has('--force')));
      return del && o.positional.some((b) => PROTECTED.includes(b)) ? R.deleteProtected : null;
    }
    default:
      return null;
  }
}

function checkGh(args) {
  const [group, action] = args;
  if (group === 'pr' && action === 'merge') return R.merge;
  if (group === 'repo' && ['delete', 'edit', 'rename', 'archive'].includes(action)) return R.repo;
  if (['release', 'secret', 'variable'].includes(group) && ['delete', 'remove', 'set'].includes(action)) return R.secret;
  return null;
}

/** cd 대상 경로. Windows의 Git Bash 경로(/c/Users/...)는 C:/Users/...로 바꾼다 (Node는 MSYS 경로를 모른다) */
export function resolvePath(cwd, p, platform = process.platform) {
  const msys = platform === 'win32' ? /^\/([A-Za-z])(\/|$)/.exec(p) : null;
  if (msys) return `${msys[1].toUpperCase()}:/${p.slice(3)}`;
  return isAbsolute(p) || /^[A-Za-z]:[\\/]/.test(p) ? p : resolve(cwd, p);
}

/**
 * 명령을 판정한다. 막을 사유(문자열) 또는 null.
 * ctx.cwd: 시작 위치, ctx.exists(path): cd 대상이 있는지, ctx.commitBlocked(cwd): 그 위치에서 커밋하면 안 되는 보호 브랜치 이름(없으면 null)
 */
export function checkCommand(command, ctx = {}, depth = 0) {
  let cwd = ctx.cwd || process.cwd();
  for (const seg of parseCommand(command)) {
    if (seg.redirects.some((r) => r.op.startsWith('<') && isEnvFile(r.target))) return R.envRead;
    const { name, args } = unwrap(seg.words);
    if (!name) continue;
    const inner = depth < 3 ? innerScript(name, args) : null;
    if (inner) {
      const r = checkCommand(inner, { ...ctx, cwd }, depth + 1);
      if (r) return r;
      continue;
    }
    if (['cd', 'pushd', 'set-location', 'sl', 'chdir'].includes(name) && args[0]) {
      const target = resolvePath(cwd, args.find((a) => !a.startsWith('-')) || args[0]);
      if (!ctx.exists || ctx.exists(target)) cwd = target;
      continue;
    }
    const r = name === 'git' ? checkGit(args, cwd, ctx) : name === 'gh' ? checkGh(args) : null;
    if (r) return r;
    if (ENV_READERS.has(name) && args.some(isEnvFile)) return R.envRead;
  }
  return null;
}
