// guard-bash hook의 명령 판정 (H-1). 정규식으로 명령 문자열 전체를 훑으면 따옴표·heredoc 안 글자에 오탐하고
// `bash -c "..."`, `git -C <경로>`, 명령 치환 안의 명령을 놓친다. 그래서 셸 토큰으로 나눠 단순 명령(segment)마다 규칙을 본다.
// shell: 'bash'(기본) | 'powershell' | 'cmd'. PowerShell은 \가 글자 그대로이고 백틱이 이스케이프이며 { }가 스크립트 블록이다.
// 완전한 셸 파서가 아니라 가드용 근사다. 모르는 형태는 단어로 남겨 규칙이 보게 한다.
import { basename, isAbsolute, resolve } from 'node:path';

const SUBST = '$()'; // 명령 치환 자리 표시

/**
 * 명령 문자열을 단순 명령 목록으로 나눈다. 각 항목: { words: 따옴표를 푼 단어들, redirects: [{op, target}], stdin?, pipeFrom? }.
 * stdin은 그 명령이 받는 heredoc 본문, pipeFrom은 파이프로 출력을 넘겨준 앞 명령이다 (셸이 받으면 스크립트로 본다).
 * 명령 치환·서브셸 안의 명령은 바깥 명령보다 먼저 나온다. 주석은 버린다.
 */
export function parseCommand(src, shell = 'bash') {
  const out = [];
  parseList(String(src || ''), 0, false, out, shell);
  return out;
}

/** 따옴표 없는 heredoc 본문의 $(…)·`…` 안 명령을 꺼낸다 */
function scanSubstitutions(body, out) {
  for (let i = 0; i < body.length; i++) {
    if (body[i] === '\\') i++;
    else if (body[i] === '$' && body[i + 1] === '(') i = parseList(body, i + 2, true, out, 'bash') - 1;
    else if (body[i] === '`') {
      const end = body.indexOf('`', i + 1);
      parseList(body.slice(i + 1, end === -1 ? body.length : end), 0, false, out, 'bash');
      i = end === -1 ? body.length : end;
    }
  }
}

function parseList(src, start, nested, out, shell) {
  const bash = shell === 'bash';
  const ps = shell === 'powershell';
  let i = start;
  let words = [];
  let redirects = [];
  let word = null; // 만드는 중인 단어 (null = 단어 밖)
  let pendingRedirect = null;
  let pipeFrom = null;
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
    let seg = null;
    if (words.length || redirects.length) {
      seg = { words, redirects };
      if (pipeFrom) seg.pipeFrom = pipeFrom;
      out.push(seg);
      for (const h of heredocs) if (!h.seg) h.seg = seg;
    }
    pipeFrom = null;
    words = [];
    redirects = [];
    return seg;
  };
  const readHeredocBodies = () => {
    // i는 줄바꿈 바로 뒤. 대기 중인 heredoc마다 구분자 줄까지 읽는다
    while (heredocs.length) {
      const h = heredocs.shift();
      const lines = [];
      for (;;) {
        if (i >= src.length) break;
        const nl = src.indexOf('\n', i);
        const line = src.slice(i, nl === -1 ? src.length : nl);
        i = nl === -1 ? src.length : nl + 1;
        const bare = h.strip ? line.replace(/^\t+/, '') : line;
        if (bare.trimEnd() === h.delim) break;
        lines.push(bare);
      }
      const body = lines.join('\n');
      if (h.seg) h.seg.stdin = body;
      if (!h.quoted) scanSubstitutions(body, out);
    }
  };
  const substitution = (close) => {
    // i는 여는 기호 바로 뒤
    if (close === ')') i = parseList(src, i, true, out, shell);
    else {
      const end = src.indexOf('`', i);
      parseList(src.slice(i, end === -1 ? src.length : end), 0, false, out, shell);
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
      readHeredocBodies();
    } else if (c === '#' && word === null) {
      while (i < src.length && src[i] !== '\n') i++;
    } else if (ps && ((c === '{' && !(word ?? '').endsWith('$')) || (c === '}' && !(word ?? '').includes('${')))) {
      // PowerShell 스크립트 블록 경계. ${env:X} 같은 변수 이름 안은 제외
      endSegment();
      i++;
    } else if (c === ';' || c === '|' || (c === '&' && src[i + 1] !== '>')) {
      const seg = endSegment();
      i++;
      if (src[i] === c || (c === '|' && src[i] === '&')) {
        if (c === '|' && src[i] === '&') pipeFrom = seg;
        i++;
      } else if (c === '|') pipeFrom = seg;
    } else if (c === '(' && word === null) {
      endSegment();
      i = parseList(src, i + 1, true, out, shell);
    } else if (bash && (c === '>' || c === '<' || c === '&')) {
      if (word !== null && /^\d+$/.test(word)) word = null; // 2>&1 의 2
      endWord();
      const op = /^(<<<|<<-|<<|>>|>&|<&|>\||&>>|&>|<|>)/.exec(src.slice(i))[0];
      i += op.length;
      if (op === '<<' || op === '<<-') {
        while (src[i] === ' ' || src[i] === '\t') i++;
        const m = /^(?:'([^']*)'|"([^"]*)"|\\?([^\s;|&<>()]+))/.exec(src.slice(i));
        if (m) {
          heredocs.push({ delim: m[1] ?? m[2] ?? m[3], strip: op === '<<-', quoted: m[3] === undefined || m[0].startsWith('\\'), seg: null });
          i += m[0].length;
        }
      } else if ((op === '>&' || op === '<&') && /^[\d-]/.test(src[i] || '')) {
        while (/[\d-]/.test(src[i] || '')) i++; // 2>&1, >&-
      } else pendingRedirect = op;
    } else if (!bash && (c === '>' || c === '<')) {
      if (word !== null && /^\d+$/.test(word)) word = null;
      endWord();
      const op = /^(>>|2>&1|>&1|>|<)/.exec(src.slice(i))[0];
      i += op.length;
      if (!op.includes('&')) pendingRedirect = op;
    } else if (c === "'") {
      const end = src.indexOf("'", i + 1);
      word = (word ?? '') + src.slice(i + 1, end === -1 ? src.length : end);
      i = end === -1 ? src.length : end + 1;
    } else if (c === '"') {
      word = word ?? '';
      i++;
      while (i < src.length && src[i] !== '"') {
        if (bash && src[i] === '\\' && '"\\$`\n'.includes(src[i + 1])) {
          if (src[i + 1] !== '\n') word += src[i + 1];
          i += 2;
        } else if (ps && src[i] === '`') {
          word += src[i + 1] ?? '';
          i += 2;
        } else if (shell !== 'cmd' && src[i] === '$' && src[i + 1] === '(') {
          i += 2;
          substitution(')');
        } else if (bash && src[i] === '`') {
          i++;
          substitution('`');
        } else word += src[i++];
      }
      i++;
    } else if (bash && c === '\\') {
      if (src[i + 1] !== '\n' && i + 1 < src.length) word = (word ?? '') + src[i + 1];
      i += 2;
    } else if (ps && c === '`') {
      if (i + 1 < src.length && src[i + 1] !== '\n') word = (word ?? '') + src[i + 1];
      i += 2;
    } else if (bash && c === '$' && (src[i + 1] === "'" || src[i + 1] === '"')) {
      i++; // $'…' (ANSI-C), $"…" (지역화) 따옴표는 일반 따옴표로 본다
    } else if (shell !== 'cmd' && c === '$' && src[i + 1] === '(') {
      i += 2;
      substitution(')');
    } else if (bash && c === '`') {
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
// 뒤에 오는 단어가 실제 명령인 셸 예약어 (bash·PowerShell). 건너뛰고 다음 단어를 명령으로 본다
const RESERVED = new Set(['if', 'then', 'else', 'elif', 'elseif', 'do', 'while', 'until', '!', '{', '}', 'fi', 'done', 'try', 'catch', 'finally', '%', 'foreach-object', 'where-object']);
// 다른 명령을 실행하는 래퍼: 이름 → { 값을 받는 옵션, 명령 앞의 위치 인자 수 }
const WRAPPERS = {
  sudo: { value: ['-u', '-g', '-h', '-p', '-C', '-U', '--user', '--group'] },
  doas: { value: ['-u'] },
  command: {},
  exec: { value: ['-a'] },
  nohup: {},
  time: {},
  builtin: {},
  setsid: {},
  winpty: {},
  stdbuf: { value: ['-i', '-o', '-e'] },
  nice: { value: ['-n', '--adjustment'] },
  ionice: { value: ['-c', '-n', '-p'] },
  timeout: { value: ['-s', '-k', '--signal', '--kill-after'], positional: 1 },
  xargs: { value: ['-I', '-L', '-n', '-P', '-d', '-E', '-s', '-a', '--max-args', '--max-procs', '--delimiter', '--arg-file', '--replace'] },
  env: { value: ['-u', '-C', '-S', '--unset', '--chdir', '--split-string'] },
};

/** env 할당·예약어·래퍼를 건너뛴 실제 명령과 인자 */
function unwrap(words) {
  let k = 0;
  for (;;) {
    while (k < words.length && (isAssign(words[k]) || RESERVED.has(words[k].toLowerCase()))) k++;
    const w = WRAPPERS[cmdName(words[k])];
    if (!w) break;
    k++;
    while (k < words.length && (words[k].startsWith('-') || (cmdName(words[k - 1]) === 'env' && isAssign(words[k])))) {
      if ((w.value || []).includes(words[k])) k++;
      k++;
    }
    k += w.positional || 0;
  }
  return { name: cmdName(words[k]), args: words.slice(k + 1) };
}

const SHELLS = { bash: 'bash', sh: 'bash', zsh: 'bash', dash: 'bash', powershell: 'powershell', pwsh: 'powershell', cmd: 'cmd' };

/** 셸 래퍼(bash -c, powershell -Command, cmd /c, eval, iex)가 실행하는 문자열과 그 셸. '-'는 표준 입력 */
function innerScript(name, args, shell) {
  const of = (script, sh = SHELLS[name] || shell) => (script == null ? null : { script, shell: sh });
  if (SHELLS[name] === 'bash') {
    const k = args.findIndex((a) => /^-[a-z]*c[a-z]*$/.test(a));
    return of(k === -1 ? null : (args[k + 1] ?? null));
  }
  if (SHELLS[name] === 'powershell') {
    const k = args.findIndex((a) => /^-(c|command)$/i.test(a));
    return of(k === -1 ? null : args.slice(k + 1).join(' '));
  }
  if (name === 'cmd') {
    const k = args.findIndex((a) => /^\/[ck]$/i.test(a));
    return of(k === -1 ? null : args.slice(k + 1).join(' '));
  }
  if (name === 'eval') return of(args.join(' '));
  if (name === 'iex' || name === 'invoke-expression') return of(args.filter((a) => !/^-command$/i.test(a)).join(' '), 'powershell');
  return null;
}

/** 셸이 스크립트 인자 없이 실행되어 표준 입력을 스크립트로 읽는가 */
const readsStdin = (name, args) => Boolean(SHELLS[name]) && !args.some((a) => !a.startsWith('-') && !a.startsWith('/'));

/** 파이프 앞 명령이 넘겨주는 글: heredoc 본문, 또는 echo·printf의 인자 */
function pipedText(seg) {
  if (!seg) return null;
  if (seg.stdin != null) return seg.stdin;
  const { name, args } = unwrap(seg.words);
  return ['echo', 'printf', 'write-output'].includes(name) ? args.filter((a) => !/^-[neE]+$/.test(a)).join(' ') : null;
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
      if (key === '-c') configs.push({ entry: v || '', env: false });
      if (key === '--config-env') configs.push({ entry: v || '', env: true }); // 값은 환경변수라 알 수 없다
    }
    k++;
  }
  return { dir, configs, sub: args[k] || '', rest: args.slice(k + 1) };
}

// git은 모호하지 않은 긴 옵션 축약(--no-verif, --forc)을 받는다. 금지 판정에 쓰는 옵션은 축약도 같은 것으로 본다
const LONG_GUARDED = ['--no-verify', '--force', '--force-with-lease', '--force-if-includes', '--mirror', '--delete', '--hard', '--unset', '--unset-all', '--staged', '--worktree', '--repo'];
const expand = (name, list) => list.filter((full) => full === name || (name.length >= 5 && full.startsWith(name)));

/**
 * 옵션을 펼친다. 짧은 옵션 묶음(-am)은 글자마다, valueShort에 든 글자는 나머지(또는 다음 단어)를 값으로 먹는다.
 * 긴 옵션은 축약을 전체 이름으로도 넣는다 (LONG_GUARDED·valueLong).
 * 반환: { flags: Set('-a','--force',...), values: { '--repo': 'x' }, positional: [...] }
 */
function options(args, valueShort = '', valueLong = []) {
  const flags = new Set();
  const values = {};
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
      expand(name, LONG_GUARDED).forEach((f) => flags.add(f));
      const [full] = expand(name, valueLong);
      if (full) {
        flags.add(full);
        values[full] = a.includes('=') ? a.slice(a.indexOf('=') + 1) : args[++k];
      }
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
  return { flags, values, positional };
}

const PROTECTED = ['main', 'develop'];
const ENV_READERS = new Set(
  'cat type more less head tail gc get-content select-string grep rg sed awk code notepad cp copy bat nl strings base64 xxd od source . tac findstr sort uniq cut diff cmp comm paste fold rev jq yq column expand pr fmt sort-object compare-object import-csv'.split(' '),
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
  pushProtected: 'main·develop에 직접 push할 수 없습니다. feature 브랜치를 push하고 PR로 합치세요.',
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
  for (const { entry, env } of g.configs) {
    const eq = entry.indexOf('=');
    const key = (eq === -1 ? entry : entry.slice(0, eq)).toLowerCase();
    const value = eq === -1 ? '' : entry.slice(eq + 1);
    if (key === 'core.hookspath' && (env || value !== '.githooks')) return R.hooksPath;
    // -c alias.x='commit --no-verify' 처럼 별칭에 숨긴 명령
    if (key.startsWith('alias.') && value && !env) {
      const r = value.startsWith('!') ? checkCommand(value.slice(1), { ...ctx, cwd }, 1) : checkGit(parseCommand(value)[0]?.words || [], cwd, ctx);
      if (r) return r;
    }
  }
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
      if ([remote, o.values['--repo'] || ''].some((x) => x === 'gitlab' || /lab\.ssafy\.com/i.test(x))) return R.gitlab;
      if (refs.some((r) => r.startsWith('+'))) return R.force;
      const deleting = o.flags.has('--delete') || o.flags.has('-d');
      // refspec의 대상(: 뒤, 없으면 그대로)이 보호 브랜치인가. HEAD처럼 지금 브랜치를 가리키는 push는 pre-push가 막는다
      const hits = refs.filter((r) => PROTECTED.includes(r.slice(r.lastIndexOf(':') + 1).replace(/^refs\/heads\//, '')));
      if (hits.length) return deleting || hits.some((r) => r.startsWith(':')) ? R.deleteProtected : R.pushProtected;
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
  const shell = ctx.shell || 'bash';
  const nested = (script, sh) => (depth < 3 && script ? checkCommand(script, { ...ctx, cwd, shell: sh }, depth + 1) : null);
  for (const seg of parseCommand(command, shell)) {
    if (seg.redirects.some((r) => r.op.startsWith('<') && isEnvFile(r.target))) return R.envRead;
    // GIT_CONFIG_KEY_n=core.hooksPath, GIT_CONFIG_PARAMETERS로 hook 경로를 바꾸는 환경변수 (할당·export·$env: 모두)
    if (seg.words.some((w) => /GIT_CONFIG/i.test(w)) && seg.words.some((w) => /core\.hookspath/i.test(w))) return R.hooksPath;
    const { name, args } = unwrap(seg.words);
    if (!name) continue;
    const inner = innerScript(name, args, shell);
    if (inner && inner.script !== '-') {
      const r = nested(inner.script, inner.shell);
      if (r) return r;
      continue;
    }
    // 셸이 표준 입력을 스크립트로 읽는다: bash <<EOF, cat <<EOF | sh, echo "..." | bash
    if (inner || readsStdin(name, args)) {
      const r = nested(seg.stdin ?? pipedText(seg.pipeFrom), SHELLS[name] || shell);
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
