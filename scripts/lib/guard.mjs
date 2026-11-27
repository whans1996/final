// guard-bash hook의 명령 판정 (H-1). 정규식으로 명령 문자열 전체를 훑으면 따옴표·heredoc 안 글자에 오탐하고
// `bash -c "..."`, `git -C <경로>`, 명령 치환 안의 명령을 놓친다. 그래서 셸 토큰으로 나눠 단순 명령(segment)마다 규칙을 본다.
// shell: 'bash'(기본) | 'powershell' | 'cmd'. PowerShell은 \가 글자 그대로이고 백틱이 이스케이프이며 { }가 스크립트 블록이다.
// 완전한 셸 파서가 아니라 가드용 근사다. 모르는 형태는 단어로 남겨 규칙이 보게 한다.
import { homedir } from 'node:os';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { SECRET_SAMPLES, codeReadsSecret, globHitsSecret, globMatch, globMatchPath, isSecretFile, isWithin, stringLiterals } from './protect.mjs';

const SUBST = '$()'; // 명령 치환 자리 표시
// 서브셸·명령 치환의 시작과 끝 표시. 그 안의 cd는 바깥 명령의 위치를 바꾸지 않는다 (#139). parseCommand는 걸러 낸다
const PUSH = { scope: 'push' };
const POP = { scope: 'pop' };

/**
 * 명령 문자열을 단순 명령 목록으로 나눈다. 각 항목: { words: 따옴표를 푼 단어들, redirects: [{op, target}], stdin?, pipeFrom? }.
 * stdin은 그 명령이 받는 heredoc 본문, pipeFrom은 파이프로 출력을 넘겨준 앞 명령이다 (셸이 받으면 스크립트로 본다).
 * 명령 치환·서브셸 안의 명령은 바깥 명령보다 먼저 나온다. 주석은 버린다.
 */
export function parseCommand(src, shell = 'bash') {
  return parseSegments(src, shell).filter((s) => !s.scope);
}

/** parseCommand + 범위 표시(PUSH·POP) */
function parseSegments(src, shell) {
  const out = [];
  parseList(String(src || ''), 0, false, out, shell);
  return out;
}

/** 따옴표 없는 heredoc 본문의 $(…)·`…` 안 명령을 꺼낸다 */
function scanSubstitutions(body, out) {
  for (let i = 0; i < body.length; i++) {
    if (body[i] === '\\') i++;
    else if (body[i] === '$' && body[i + 1] === '(') {
      out.push(PUSH);
      i = parseList(body, i + 2, true, out, 'bash') - 1;
      out.push(POP);
    } else if (body[i] === '`') {
      const end = body.indexOf('`', i + 1);
      out.push(PUSH);
      parseList(body.slice(i + 1, end === -1 ? body.length : end), 0, false, out, 'bash');
      out.push(POP);
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
  const blockOpeners = []; // PowerShell { 를 연 명령
  let lastClosed = null; // 방금 닫힌 PowerShell 블록·괄호의 명령 (그 뒤 파이프가 이어받는다)
  let cond = false; // 지금 명령이 && || 뒤라 실행되지 않을 수 있는가 (seg.cond)

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
      if (cond) seg.cond = true;
      out.push(seg);
      for (const h of heredocs) if (!h.seg) h.seg = seg;
      lastClosed = null;
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
    // i는 여는 기호 바로 뒤. 범위 표시는 bash만: PowerShell의 $( )는 같은 프로세스라 그 안의 cd가 바깥에 남는다
    if (bash) out.push(PUSH);
    if (close === ')') i = parseList(src, i, true, out, shell);
    else {
      const end = src.indexOf('`', i);
      parseList(src.slice(i, end === -1 ? src.length : end), 0, false, out, shell);
      i = end === -1 ? src.length : end + 1;
    }
    if (bash) out.push(POP);
    word = (word ?? '') + SUBST;
  };

  while (i < src.length) {
    const c = src[i];
    if (nested && c === ')') {
      endSegment();
      return i + 1;
    }
    if (bash && c === ')') {
      // 서브셸 밖의 )는 case 분기 패턴의 끝이다: case $x in *) git reset --hard ;; esac
      endSegment();
      i++;
      continue;
    }
    if (c === ' ' || c === '\t' || c === '\r') {
      endWord();
      i++;
    } else if (c === '\n') {
      endSegment();
      cond = false;
      i++;
      readHeredocBodies();
    } else if (c === '#' && word === null) {
      while (i < src.length && src[i] !== '\n') i++;
    } else if (ps && ((c === '{' && !(word ?? '').endsWith('$')) || (c === '}' && !(word ?? '').includes('${')))) {
      // PowerShell 스크립트 블록 경계. ${env:X} 같은 변수 이름 안은 제외.
      // 블록 뒤의 파이프는 블록을 연 명령에서 이어진다: gci | ? { … } | gc (#245)
      if (c === '{') blockOpeners.push(endSegment());
      else {
        endSegment();
        lastClosed = blockOpeners.pop() ?? null;
      }
      i++;
    } else if (c === ';' || c === '|' || (c === '&' && src[i + 1] !== '>')) {
      const seg = endSegment() ?? (c === '|' ? lastClosed : null);
      lastClosed = null;
      i++;
      if (src[i] === c || (c === '|' && src[i] === '&')) {
        if (c === '|' && src[i] === '&') pipeFrom = seg;
        else cond = c !== ';'; // && || 뒤는 조건부로 실행된다 (;; 는 case 분기 끝)
        i++;
      } else if (c === '|') pipeFrom = seg;
      else cond = false;
    } else if (c === '(' && word === null) {
      // bash의 ( )만 서브셸이다. PowerShell·cmd의 ( )는 묶음일 뿐이라 cd가 바깥에 남는다
      const before = endSegment();
      if (bash) out.push(PUSH);
      const from = out.length;
      i = parseList(src, i + 1, true, out, shell);
      if (bash) out.push(POP);
      // PowerShell: 괄호 안 결과는 앞 명령의 인자다 (Get-Content (gci -Filter .env*), #245)
      const group = out.slice(from).filter((s) => !s.scope);
      if (ps && before) for (const s of group) s.argOf = before;
      lastClosed = ps ? (group[group.length - 1] ?? null) : null;
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

// PowerShell 변수 할당: `$x = gc .env`, `[string]$x += …`, `$x=gc .env` (#190). 오른쪽이 명령이다
// ${x}·$x,$y 다중 할당도 변수로 본다 (#245)
const PS_VAR = /^(?:(?:\[[\w.[\],]+\])*\$(?:[\w:]+|\{[^}]+\}),?)+$/;
const PS_ASSIGN_OP = /^(?:[-+*/%]|\?\?)?=$/;
const PS_VAR_EQ = /^(?:\[[\w.[\],]+\])*\$[\w:]+(?:[-+*/%]|\?\?)?=(.*)$/;

/** env 할당·PowerShell 변수 할당·예약어·래퍼를 건너뛴 실제 명령과 인자 */
function unwrap(input) {
  const words = [...input];
  let k = 0;
  for (;;) {
    for (;;) {
      const w = words[k];
      if (w === undefined) break;
      if (isAssign(w) || RESERVED.has(w.toLowerCase())) k++;
      else if (PS_VAR.test(w) && w.endsWith(',') && PS_VAR.test(words[k + 1] ?? '')) k++; // $x, $y = …
      else if (PS_VAR.test(w) && PS_ASSIGN_OP.test(words[k + 1] ?? '')) k += 2;
      else if (PS_VAR_EQ.test(w)) {
        const rest = PS_VAR_EQ.exec(w)[1];
        if (rest) words[k] = rest;
        else k++;
      } else break;
    }
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
  const paths = []; // --git-dir·--work-tree 값 (eval 경로 판정, #189)
  let dir = null;
  let k = 0;
  while (k < args.length && args[k].startsWith('-')) {
    const a = args[k];
    const eq = a.indexOf('=');
    const key = a.startsWith('--') && eq > 0 ? a.slice(0, eq) : a;
    if (GIT_GLOBAL_VALUE.has(key)) {
      const v = key === a ? args[++k] : a.slice(eq + 1);
      if (key === '-C') dir = v;
      if ((key === '--git-dir' || key === '--work-tree') && v) paths.push(v);
      if (key === '-c') configs.push({ entry: v || '', env: false });
      if (key === '--config-env') configs.push({ entry: v || '', env: true }); // 값은 환경변수라 알 수 없다
    }
    k++;
  }
  return { dir, configs, paths, sub: args[k] || '', rest: args.slice(k + 1) };
}

// git은 모호하지 않은 긴 옵션 축약(--no-verif, --forc)을 받는다. 금지 판정에 쓰는 옵션은 축약도 같은 것으로 본다
const LONG_GUARDED = ['--no-verify', '--force', '--force-with-lease', '--force-if-includes', '--mirror', '--delete', '--hard', '--unset', '--unset-all', '--staged', '--worktree', '--repo', '--discard-changes'];
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
// 파일 내용을 출력하는 명령. 인자 중 비밀 파일이 있으면 막는다
const READERS = new Set(
  'cat type more less head tail gc get-content select-string sls grep egrep fgrep rg sed awk code notepad cp copy bat nl strings base64 xxd od source . tac findstr sort uniq cut diff cmp comm paste fold rev jq yq column expand pr fmt sort-object compare-object import-csv'.split(' '),
);
// 첫 위치 인자가 파일이 아니라 검색 패턴인 명령 (H-4). value: 값을 받는 짧은 옵션, pattern: 패턴을 주는 옵션(있으면 첫 위치 인자도 파일)
const GREP_LIKE = {
  grep: { value: 'efmABCdD', pattern: 'ef' },
  egrep: { value: 'efmABCdD', pattern: 'ef' },
  fgrep: { value: 'efmABCdD', pattern: 'ef' },
  rg: { value: 'efmABCgtTjMEr', pattern: 'ef' },
  // sed·awk도 첫 위치 인자가 스크립트다 (`sed -i 's/a/.env/' f`). -e·-f로 주면 위치 인자는 모두 파일
  sed: { value: 'ef', pattern: 'ef' },
  awk: { value: 'fvF', pattern: 'f' },
  gawk: { value: 'fvF', pattern: 'f' },
};
const GREP_LONG_VALUE = new Set(
  '--regexp --expression --file --max-count --after-context --before-context --context --include --exclude --exclude-dir --exclude-from --label --binary-files --devices --directories --glob --iglob --type --type-not --threads --replace --max-depth --encoding --type-add --pre'.split(' '),
);

/** 읽기 명령의 파일 인자. grep류는 검색 패턴을 뺀다 (`grep "\.env" src`는 .env를 읽지 않는다) */
function readTargets(name, args) {
  if (name === 'select-string' || name === 'sls') {
    // PowerShell: -Pattern 값은 패턴, -Path·-LiteralPath 값과 나머지 위치 인자는 파일 (첫 위치 인자는 -Pattern 없을 때 패턴)
    const files = [];
    const positional = [];
    let pattern = false;
    for (let k = 0; k < args.length; k++) {
      const a = args[k].toLowerCase();
      if (a === '-pattern') (pattern = true), k++;
      else if (a === '-path' || a === '-literalpath') files.push(args[++k] ?? '');
      else if (!a.startsWith('-')) positional.push(args[k]);
    }
    return [...files, ...(pattern ? positional : positional.slice(1))];
  }
  if (name === 'findstr') {
    const positional = args.filter((a) => !a.startsWith('/'));
    return args.some((a) => /^\/[cg]:/i.test(a)) ? positional : positional.slice(1);
  }
  const spec = GREP_LIKE[name];
  if (!spec) return args;
  const files = [];
  const positional = [];
  let pattern = false;
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    if (a === '--') {
      positional.push(...args.slice(k + 1));
      break;
    }
    if (a.startsWith('--')) {
      const [raw, ...v] = a.split('=');
      // GNU grep은 모호하지 않은 긴 옵션 축약(--reg=)을 받는다. 후보 중 하나라도 패턴·값 옵션이면 그렇게 본다 (보수적으로)
      const candidates = GREP_LONG_VALUE.has(raw) ? [raw] : [...GREP_LONG_VALUE].filter((o) => raw.length >= 4 && o.startsWith(raw));
      const takesValue = candidates.length > 0;
      const value = v.length ? v.join('=') : takesValue ? args[++k] : undefined;
      const patternOpt = candidates.some((o) => o === '--regexp' || o === '--expression');
      if (patternOpt) pattern = true;
      // --file(패턴 파일)·--glob 등. 값도 파일 이름일 수 있다. 빼는 글롭(--exclude=".env*")은 읽지 않는다
      else if (value !== undefined && !(candidates.length && candidates.every((o) => o === '--exclude' || o === '--exclude-dir'))) files.push(value);
      if (candidates.includes('--file')) pattern = true;
    } else if (a.startsWith('-') && a.length > 1) {
      for (let j = 1; j < a.length; j++) {
        if (!spec.value.includes(a[j])) continue;
        const value = a.slice(j + 1) || args[++k] || '';
        if (spec.pattern.includes(a[j])) pattern = true;
        if (a[j] !== 'e') files.push(value);
        break;
      }
    } else positional.push(a);
  }
  return [...files, ...(pattern ? positional : positional.slice(1))];
}

// ── 재귀 검색 (#188) ── grep -r는 .gitignore를 따르지 않고, rg는 -uu·--no-ignore·-g 글롭이 무시 규칙을 넘는다.
// 인자에 .env가 없어도 검색 폴더 아래 비밀 파일이 출력된다. 그래서 hook이 넘긴 실제 비밀 파일 목록(ctx.secretFiles)을 이 검색이 읽는지 본다
const SEARCHERS = new Set(['grep', 'egrep', 'fgrep', 'rg']);
// 파일 이름·개수만 출력하는 옵션 (내용이 나오지 않는다: "이 변수가 어디서 쓰이나" 찾기). rg의 -L은 --follow라 뺀다 (Codex)
const NAMES_ONLY_SHORT = { grep: 'lLcq', egrep: 'lLcq', fgrep: 'lLcq', rg: 'lcq' };
const NAMES_ONLY_LONG = new Set(['--files-with-matches', '--files-without-match', '--count', '--count-matches', '--quiet', '--silent', '--files']);
// 비밀 파일(.env*·키 파일)을 고르지 않는 rg 내장 타입. 그 밖의 타입(sh, config 등)은 모르므로 고를 수 있다고 본다 (Codex)
const RG_SAFE_TYPES = new Set(
  'java js ts vue jsx tsx md markdown json yaml toml html css scss less py python kotlin kt xml sql gradle txt csv svg go rust c cpp h swift ruby php lua'.split(' '),
);

/**
 * 검색 시작 경로. ~는 홈 폴더(ctx.home), $PWD와 같은 명령 안에서 할당한 변수(ctx.vars: `D=…; grep -r x $D`)는 그 값으로 펼친다.
 * 그 밖에 셸이 펼치는 경로($HOME, "$(pwd)")는 알 수 없어 / 로 본다 (저장소를 품을 수 있다)
 */
function searchRoot(cwd, r, ctx = {}) {
  let p = String(r).replace(/\$\{?(\w+)\}?/g, (m, n) => (n === 'PWD' ? cwd : (ctx.vars?.[n] ?? m)));
  if (/^~(?=[\\/]|$)/.test(p)) p = (ctx.home ?? homedir()) + p.slice(1);
  return /^~|\$/.test(p) ? '/' : resolvePath(cwd, p);
}

/** dir 아래 비밀 파일. 목록을 모르면(hook 밖, 저장소 밖) 대표 이름이 dir 바로 아래 있다고 본다 */
const secretsIn = (dir, ctx) => ctx.secretFiles?.(dir) ?? SECRET_SAMPLES.map((n) => ({ path: join(dir, n), ignored: true }));

const normParts = (p) => {
  const s = resolve(String(p)).replace(/\\/g, '/').replace(/\/+$/, '');
  return (process.platform === 'win32' ? s.toLowerCase() : s).split('/');
};

/**
 * 셸이 펼칠 글롭 인자(`cat .e*`, `grep -rn KEY *`)가 고르는 비밀 파일 [{ f, exact }] (#245).
 * bash처럼 * ? [ ]는 점으로 시작하는 이름에 맞지 않는다. exact: 글롭이 그 파일 자체, 아니면 글롭이 고른 폴더 안의 파일
 */
function shellGlobFiles(word, cwd, ctx) {
  const abs = resolvePath(cwd, word);
  const gs = normParts(abs);
  const fixed = gs.findIndex((g) => /[*?[]/.test(g));
  if (fixed === -1) return [];
  const base = gs.slice(0, fixed).join('/') || '/';
  const out = [];
  for (const f of secretsIn(base, ctx)) {
    const ps = normParts(f.path);
    let ok = ps.length >= gs.length;
    for (let i = fixed; ok && i < gs.length; i++) ok = globMatch(gs[i], ps[i], { shell: true });
    if (ok) out.push({ f, exact: ps.length === gs.length, base });
  }
  return out;
}

/** grep류·rg의 재귀 여부, 포함·제외 글롭, 타입, 무시 규칙을 넘는 옵션, 검색 시작 경로 */
function searchSpec(name, args) {
  const spec = GREP_LIKE[name];
  const rg = name === 'rg';
  // globs: rg -g·--glob·--iglob [{ glob, icase }] (명령줄 순서. 뒤의 것이 이긴다). types: -t로 고른 타입, typeAdds: --type-add 정의
  // filters: grep --include·--exclude [{ include, glob }] (명령줄 순서. 충돌하면 마지막에 맞은 것이 이긴다)
  const s = { recursive: rg, filters: [], globs: [], types: [], typeAdds: {}, noIgnore: false, hidden: false, namesOnly: false, roots: [] };
  const positional = [];
  let pattern = false;
  let unrestricted = 0;
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    if (a === '--') {
      positional.push(...args.slice(k + 1));
      break;
    }
    if (a.startsWith('--')) {
      const [raw, ...v] = a.split('=');
      const candidates = GREP_LONG_VALUE.has(raw) ? [raw] : [...GREP_LONG_VALUE].filter((o) => raw.length >= 4 && o.startsWith(raw));
      const value = v.length ? v.join('=') : candidates.length ? args[++k] : undefined;
      const full = candidates.length === 1 ? candidates[0] : raw; // 모호한 축약(--excl)은 제외 옵션으로 보지 않는다 (보수적으로)
      if (candidates.some((o) => o === '--regexp' || o === '--expression' || o === '--file')) pattern = true;
      if (full === '--include' || full === '--exclude') s.filters.push({ include: full === '--include', glob: value ?? '' });
      else if (full === '--glob' || full === '--iglob') s.globs.push({ glob: value ?? '', icase: full === '--iglob' });
      else if (full === '--type') s.types.push(value ?? '');
      else if (full === '--type-add') {
        const [t, ...globs] = String(value ?? '').split(':');
        (s.typeAdds[t] ??= []).push(...globs.join(':').split(','));
      } else if (full === '--directories' && value === 'recurse') s.recursive = true;
      if (raw.length >= 5 && ['--recursive', '--dereference-recursive'].some((o) => o.startsWith(raw))) s.recursive = true;
      if (raw.startsWith('--no-ignore')) s.noIgnore = true;
      if (raw === '--unrestricted') unrestricted++;
      if (raw === '--hidden') s.hidden = true;
      if (NAMES_ONLY_LONG.has(raw)) s.namesOnly = true;
      if (raw === '--json') s.json = true;
    } else if (a.startsWith('-') && a.length > 1) {
      for (let j = 1; j < a.length; j++) {
        const c = a[j];
        if (!rg && (c === 'r' || c === 'R')) s.recursive = true; // rg의 -r은 --replace다
        if (rg && c === 'u') unrestricted++;
        if (rg && c === '.') s.hidden = true;
        if (NAMES_ONLY_SHORT[name]?.includes(c)) s.namesOnly = true;
        if (!spec.value.includes(c)) continue;
        const value = a.slice(j + 1) || args[++k] || '';
        if (spec.pattern.includes(c)) pattern = true;
        if (rg && c === 'g') s.globs.push({ glob: value, icase: false });
        if (rg && c === 't') s.types.push(value);
        if (!rg && c === 'd' && value === 'recurse') s.recursive = true;
        break;
      }
    } else positional.push(a);
  }
  // rg -u: .gitignore 무시, -uu: 숨김 파일까지
  if (unrestricted >= 1) s.noIgnore = true;
  if (unrestricted >= 2) s.hidden = true;
  // rg --json은 -l·-c와 함께여도 일치한 줄 내용을 낸다 (Codex)
  if (s.json) s.namesOnly = false;
  s.roots = pattern ? positional : positional.slice(1);
  if (!s.roots.length) s.roots = ['.'];
  return s;
}

/**
 * 이 검색이 비밀 파일 f({ path, ignored })를 읽는가. rel: 검색 시작 폴더 기준 상대 경로.
 * 고르는 글롭은 보수적으로(대소문자 무시), 빼는 글롭은 엄격하게(grep·rg처럼 대소문자 구분, 해석 실패면 빼지 않음) 맞춘다.
 * grep --include·--exclude는 브레이스를 글자 그대로 본다 (Codex)
 */
function searchReads(name, s, f, rel) {
  const base = basename(String(f.path).replace(/\\/g, '/'));
  if (name !== 'rg') {
    // GNU grep: 마지막에 맞은 --include·--exclude가 이긴다. 아무것도 맞지 않으면 첫 규칙이 --include일 때만 뺀다 (Codex)
    let take = null;
    for (const { include, glob } of s.filters) if (globMatch(glob, base, include ? { braces: false } : { strict: true, braces: false })) take = include;
    return s.recursive && (take ?? !s.filters[0]?.include);
  }
  // rg: 뒤에 준 글롭이 이긴다. 고르는 글롭(-g x)에 맞으면 무시 규칙·숨김을 넘고, 고르는 글롭이 있는데 맞지 않으면 건너뛴다
  let picked = null;
  for (const { glob, icase } of s.globs) {
    const neg = glob.startsWith('!');
    if (neg ? globMatchPath(glob.slice(1), rel, { strict: true, icase }) : globMatchPath(glob, rel)) picked = !neg;
  }
  if (picked !== null) return picked;
  if (s.globs.some((g) => !g.glob.startsWith('!'))) return false;
  // -t 타입은 고르기만 하고 무시 규칙은 넘지 않는다. --type-add 정의는 그 글롭으로, 내장 타입은 안전한 것만 비밀 파일을 고르지 않는다고 본다
  // --type-add 'x:include:sh,…'는 다른 타입의 규칙을 가져온다 (Codex)
  const typeHits = (t, depth = 0) =>
    s.typeAdds[t]
      ? s.typeAdds[t].some((g) => (g.startsWith('include:') ? depth < 5 && g.slice(8).split(',').some((u) => typeHits(u, depth + 1)) : globMatchPath(g, rel)))
      : !RG_SAFE_TYPES.has(t);
  if (s.types.length && !s.types.some((t) => typeHits(t))) return false;
  return (!f.ignored || s.noIgnore) && (!base.startsWith('.') || s.hidden);
}

/** 재귀 검색이 검색 폴더 아래 비밀 파일 내용을 출력하는가. 비밀 파일 목록을 모르면(hook 밖) 판정하지 않는다 */
function searchReadsSecret(name, args, cwd, ctx) {
  if (!ctx.secretFiles) return false;
  const s = searchSpec(name, args);
  if (s.namesOnly) return false;
  return s.roots.some((r) => {
    // 셸 글롭 인자: 그 파일 자체를 고르면 이름을 준 것과 같고(rg도 무시 규칙과 상관없이 읽는다), 폴더를 고르면 그 안을 검색한다 (#245)
    if (/[*?[]/.test(r)) return shellGlobFiles(r, cwd, ctx).some(({ f, exact, base }) => exact || searchReads(name, s, f, relative(base, f.path)));
    if (!s.recursive) return false;
    const root = searchRoot(cwd, r, ctx);
    return (ctx.secretFiles(root) ?? []).some((f) => searchReads(name, s, f, relative(root, f.path)));
  });
}

/** find가 비밀 파일 이름을 내보내는가 (`find . -type f | xargs grep`, `find … -exec cat {} +`, `grep x $(find …)`, #245) */
function findEmitsSecret(seg, cwd, ctx) {
  const { name, args } = unwrap(seg.words);
  if (name !== 'find') return false;
  const roots = recursiveRoots('find', args);
  const tests = [];
  let neg = false;
  // -o(OR)로 나눈 분기마다 조건을 모은다. 괄호·쉼표로 묶으면 범위를 따지지 않고 모든 파일을 낸다고 본다 (보수적으로, Codex)
  const branches = [tests];
  let grouped = false;
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    if (a === '!' || a === '-not') neg = true;
    else if (/^-(i?name|i?path|i?wholename)$/.test(a)) {
      branches[branches.length - 1].push({ kind: a.includes('name') && !a.includes('whole') ? 'name' : 'path', glob: args[++k] ?? '', icase: a.startsWith('-i'), neg });
      neg = false;
    } else if (a === '-type') {
      // 부정 없는 -type d가 있는 분기는 파일을 내지 않는다 (! -type d, 다른 분기는 그대로, Codex)
      if (args[++k] === 'd' && !neg) branches[branches.length - 1].dirs = true;
      neg = false;
    }
    else if (a === '-exec' || a === '-execdir' || a === '-ok' || a === '-okdir') break;
    else if (a === '-o' || a === '-or') (branches.push([]), (neg = false));
    else if (a === ',' || a === '(' || a === ')') (grouped = true), (neg = false);
    else neg = false;
  }
  return roots.some((r) => {
    const root = searchRoot(cwd, r, ctx);
    return (ctx.secretFiles?.(root) ?? []).some((f) => {
      if (grouped) return true;
      // -name은 이름, -path는 find가 출력하는 경로(./backend/.env.local)로 맞춘다. 고르는 쪽은 보수적으로, 빼는 쪽은 엄격하게 (Codex)
      const subject = (t) => (t.kind === 'name' ? basename(f.path.replace(/\\/g, '/')) : `${r.replace(/\/+$/, '')}/${relative(root, f.path).replace(/\\/g, '/')}`);
      return branches.some((list) => {
        if (list.dirs) return false;
        const pos = list.filter((t) => !t.neg);
        return (
          (!pos.length || pos.some((t) => globMatch(t.glob, subject(t), { icase: true }))) &&
          !list.filter((t) => t.neg).some((t) => globMatch(t.glob, subject(t), { strict: true, icase: t.icase }))
        );
      });
    });
  });
}

/** findstr /s(하위 폴더까지)가 고르는 비밀 파일이 있는가 (cmd·PowerShell, #245). 와일드카드는 Windows처럼 대소문자를 무시한다 */
function findstrReadsSecret(args, cwd, ctx) {
  const opts = args.filter((a) => a.startsWith('/'));
  if (!opts.some((o) => !o.includes(':') && /s/i.test(o))) return false;
  const positional = args.filter((a) => !a.startsWith('/'));
  const patterns = opts.some((o) => /^\/[cg]:/i.test(o)) ? positional : positional.slice(1);
  return patterns.some((p) => {
    const slash = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
    const dir = resolvePath(cwd, slash === -1 ? '.' : p.slice(0, slash) || '/');
    const glob = (slash === -1 ? p : p.slice(slash + 1)).replace(/^\*\.\*$/, '*');
    return secretsIn(dir, ctx).some((f) => globMatch(glob, basename(f.path.replace(/\\/g, '/')), { icase: true }));
  });
}

// PowerShell은 파이프로 파일 객체를 넘긴다: `gi .env | gc`, `gci -r | sls KEY` (#188·#190)
const PS_LIST = new Set(['get-childitem', 'gci', 'dir', 'ls', 'get-item', 'gi']);
// 받은 객체를 그대로(걸러서·정렬해서) 넘기는 명령: `gci | ? {…} | gc` (#245)
const PS_PASS = new Set(['?', 'where-object', 'where', 'sort-object', 'sort', 'select-object', 'select', '%', 'foreach-object', 'foreach', 'tee-object', 'tee']);
const PS_RECURSE = /^-r(e(c(u(r(se?)?)?)?)?)?$/i;

/** 파이프 앞(또는 괄호로 넘긴) PowerShell 목록 명령이 비밀 파일을 넘기는가 */
function psPipeReadsSecret(from, cwd, ctx, depth = 0) {
  if (PS_PASS.has(String(from.words[0] ?? '').toLowerCase())) return Boolean(from.pipeFrom) && depth < 5 && psPipeReadsSecret(from.pipeFrom, cwd, ctx, depth + 1);
  const { name, args } = unwrap(from.words);
  if (!PS_LIST.has(name)) return false;
  const paths = [];
  const positional = [];
  const include = [];
  const exclude = [];
  let recurse = false;
  for (let k = 0; k < args.length; k++) {
    const a = args[k].toLowerCase();
    if (PS_RECURSE.test(a)) recurse = true;
    else if (a === '-depth') (recurse = true), k++;
    else if (/^-(path|literalpath|lp)$/.test(a)) paths.push(...String(args[++k] ?? '').split(','));
    else if (/^-(filter|include)$/.test(a)) include.push(...String(args[++k] ?? '').split(','));
    else if (a === '-exclude') exclude.push(...String(args[++k] ?? '').split(','));
    else if (!a.startsWith('-')) positional.push(args[k]);
  }
  // 첫 위치 인자는 경로, 둘째는 -Filter
  if (positional[0]) paths.push(...positional[0].split(','));
  if (positional[1]) include.push(positional[1]);
  const roots = paths.length ? paths : ['.'];
  if (roots.some((r) => isSecretFile(r) || (/[*?[]/.test(r) && globHitsSecret(r)))) return true;
  if (!ctx.secretFiles) return include.some((g) => globHitsSecret(g));
  return roots.some((r) => {
    const root = resolvePath(cwd, r);
    return (ctx.secretFiles(root) ?? []).some((f) => {
      const base = basename(String(f.path).replace(/\\/g, '/'));
      if (!recurse && !isWithin(root, dirname(f.path))) return false; // 재귀가 아니면 바로 아래 파일만
      return (!include.length || include.some((g) => globMatch(g, base))) && !exclude.some((g) => globMatch(g, base, { strict: true, icase: true }));
    });
  });
}

// 인라인 코드를 실행하는 인터프리터: 이름 → 코드를 받는 옵션 (HX-1)
const INTERPRETERS = {
  node: ['-e', '--eval', '-p', '--print'],
  bun: ['-e', '--eval', '-p', '--print'],
  deno: ['eval'],
  python: ['-c'],
  python3: ['-c'],
  py: ['-c'],
  pythonw: ['-c'],
  ruby: ['-e'],
  perl: ['-e', '-E'],
  php: ['-r'],
};

// GitHub API로 머지·ref·저장소 설정·시크릿을 바꾸는 경로 (#130). GET이 아닐 때만 막는다
const GITHUB_WRITE_PATHS = [
  /(^|\/)pulls\/\d+\/merge$/,
  /^repos\/[^/]+\/[^/]+$/,
  /^repos\/[^/]+\/[^/]+\/merges$/,
  /\/(actions|dependabot|codespaces)\/(secrets|variables)(\/|$)/,
  /\/environments\/[^/]+\/(secrets|variables)(\/|$)/,
  /\/releases(\/|$)/,
  /\/git\/refs(\/|$)/,
  /\/branches\/[^/]+\/(protection|rename)/,
  /^repos\/[^/]+\/[^/]+\/transfer$/,
  /\/rulesets(\/|$)/,
  /\/contents\//,
  /\/collaborators(\/|$)/,
  /\/hooks(\/|$)/,
  /\/keys(\/|$)/,
];
const GRAPHQL_WRITE =
  /\b(mergePullRequest|enablePullRequestAutoMerge|mergeBranch|createRef|updateRef|updateRefs|deleteRef|createCommitOnBranch|updateRepository|deleteRepository|archiveRepository|createBranchProtectionRule|updateBranchProtectionRule|deleteBranchProtectionRule)\b/;
const apiPath = (p) =>
  String(p || '')
    .replace(/^https?:\/\/api\.github\.com/i, '')
    .replace(/^\/+/, '')
    .replace(/[?#].*$/, '');
const githubWrite = (path, method, text) => (path === 'graphql' ? GRAPHQL_WRITE.test(text) : method !== 'GET' && GITHUB_WRITE_PATHS.some((re) => re.test(path)));

const R = {
  noVerify: 'git hook 우회(--no-verify)는 금지입니다. 테스트가 실패하면 고치거나, 교대용이면 wip(F-xx): 로 커밋하세요.',
  commitN: 'git commit -n 은 --no-verify와 같습니다. 금지입니다.',
  force: '강제 push는 금지입니다. 다른 PC에서 push한 커밋이 사라질 수 있습니다.',
  gitlab: 'GitLab(lab.ssafy.com)은 최종 제출 전용이며 push는 사람이 직접 합니다. 개발 push는 origin(GitHub)으로 하세요.',
  deleteProtected: '보호 브랜치 삭제는 금지입니다.',
  pushProtected: 'main·develop에 직접 push할 수 없습니다. feature 브랜치를 push하고 PR로 합치세요.',
  resetHard: 'git reset --hard는 금지입니다. 미커밋 작업이 사라집니다. 필요하면 사용자에게 직접 실행을 요청하세요.',
  clean: 'git clean -f는 금지입니다. 추적되지 않은 파일이 영구 삭제됩니다.',
  restoreAll: '작업 트리를 디렉터리·와일드카드 단위로 되돌리는 것은 금지입니다. 파일을 하나씩 지정하거나 사용자에게 확인하세요.',
  discard: '미커밋 변경을 버리는 브랜치 전환(checkout -f, switch -f·--discard-changes)은 금지입니다. 먼저 커밋(교대용이면 wip)하거나 사용자에게 확인하세요.',
  stashDrop: 'git stash drop·clear는 금지입니다. 보관한 변경이 영구 삭제됩니다.',
  ghUnknown: '모르는 gh 하위 명령은 미리 만든 별칭·확장일 수 있어 막습니다. gh alias list로 확인하고 원래 명령(gh pr view …)을 쓰세요.',
  worktreeForce:'git worktree remove --force는 금지입니다. 그 작업 트리의 미커밋 변경이 사라집니다. 사용자에게 확인하세요.',
  rmForce: 'git rm -r -f는 금지입니다. 폴더의 미커밋 변경까지 지웁니다. 파일을 하나씩 지정하거나(--cached는 허용) 사용자에게 확인하세요.',
  ghAlias: 'gh 별칭으로 막힌 명령을 숨길 수 있어 막습니다 (import·표준 입력, 하위 명령을 덜 쓴 확장 pr·api·\'pr $1\', 셸 별칭의 gh $N). 확장에 하위 명령까지 모두 쓰세요.',
  addForce: 'git add --force는 금지입니다. .gitignore된 비밀 파일이 커밋될 수 있습니다.',
  hooksPath: 'git hook 경로 변경은 금지입니다.',
  merge: 'PR 머지는 영역 오너가 CI 초록을 확인하고 GitHub에서 직접 합니다.',
  ghApi: 'GitHub API로 머지·브랜치(ref)·저장소 설정·시크릿을 바꾸는 요청은 금지입니다 (gh pr merge 차단 우회, #130). 사람이 GitHub에서 직접 합니다.',
  repo: '저장소 설정 변경·삭제는 사람이 직접 합니다.',
  secret: '릴리스·시크릿·변수 변경은 사람이 직접 합니다.',
  envRead: '.env·키 파일 내용을 읽을 수 없습니다. 필요한 변수 이름은 .env.example을 보세요.',
  envSearch: '이 재귀 검색은 .env·키 파일 내용까지 출력합니다. 검색 경로를 좁히거나 --exclude=".env*"(rg는 -g "!.env*")로 빼세요 (#188).',
  envAdd: '.env·키 파일은 커밋할 수 없습니다.',
  evalRead: '평가(eval) 데이터는 김동환(A) PC에서 열람할 수 없습니다 (블라인드 평가, docs/산출물/09-데이터셋.md 5절). 필요한 것은 김준섭에게 요청하세요.',
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
  // 되돌릴 범위가 넓은 경로: 전체(. :/), 와일드카드·매직 pathspec, 디렉터리 (H-3)
  const broad = (p) => p === '.' || p === ':/' || /[*?[]/.test(p) || p.startsWith(':(') || Boolean(ctx.isDir?.(resolvePath(where, p)));
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
      const o = options(rest, 'sbB', ['--source', '--conflict', '--pathspec-from-file']);
      if (g.sub === 'checkout' && (o.flags.has('-f') || o.flags.has('--force'))) return R.discard;
      const staged = o.flags.has('--staged') || o.flags.has('-S');
      const worktree = o.flags.has('--worktree') || o.flags.has('-W');
      if (g.sub === 'restore' && staged && !worktree) return null;
      return o.positional.some(broad) ? R.restoreAll : null;
    }
    case 'switch': {
      const o = options(rest, 'cC', ['--create', '--force-create', '--orphan']);
      return o.flags.has('--discard-changes') || o.flags.has('-f') || o.flags.has('--force') ? R.discard : null;
    }
    case 'stash':
      return ['drop', 'clear'].includes(rest[0]) ? R.stashDrop : null;
    case 'worktree': {
      if (rest[0] !== 'remove') return null;
      const o = options(rest.slice(1));
      return o.flags.has('-f') || o.flags.has('--force') ? R.worktreeForce : null;
    }
    case 'rm': {
      const o = options(rest, '', ['--pathspec-from-file']);
      if (o.flags.has('--cached')) return null; // 색인에서만 빼고 파일은 남긴다
      // 글롭 pathspec('src/*')은 디렉터리 경계를 넘어 맞아 -r 없이도 하위 전체를 지운다
      const wide = o.flags.has('-r') || o.positional.some((p) => /[*?[]/.test(p) || p.startsWith(':('));
      return wide && (o.flags.has('-f') || o.flags.has('--force')) ? R.rmForce : null;
    }
    case 'grep': {
      // 무시 파일까지 보는 git grep (#188): --no-index(기본으로 .gitignore를 따르지 않음), --untracked --no-exclude-standard
      const o = options(rest, 'efABCmO', ['--max-depth', '--threads', '--open-files-in-pager', '--context', '--after-context', '--before-context', '--max-count']);
      const noIndex = o.flags.has('--no-index') && !o.flags.has('--exclude-standard');
      const ignored = o.flags.has('--untracked') && o.flags.has('--no-exclude-standard');
      if (!(noIndex || ignored) || !ctx.secretFiles) return null;
      if (['-l', '-L', '-c', '-q', '--name-only', '--files-with-matches', '--files-without-match', '--count', '--quiet'].some((f) => o.flags.has(f))) return null;
      // pathspec 글롭('*', ':(glob)**')은 git이 펼친다: 글롭 앞의 고정된 폴더로 본다 (Codex)
      const paths = (o.flags.has('-e') || o.flags.has('-f') ? o.positional : o.positional.slice(1)).map((p) => {
        const spec = p.replace(/^:\([^)]*\)/, '');
        if (!/[*?[]/.test(spec) && spec === p) return p;
        const fixed = spec.split(/[*?[]/)[0];
        return fixed.includes('/') ? fixed.slice(0, fixed.lastIndexOf('/')) || '.' : '.';
      });
      return (paths.length ? paths : ['.']).some((p) => (ctx.secretFiles(searchRoot(where, p, ctx)) ?? []).length) ? R.envSearch : null;
    }
    case 'add': {
      const o = options(rest, '', ['--chmod', '--pathspec-from-file']);
      if (o.flags.has('-f') || o.flags.has('--force')) return R.addForce;
      return o.positional.some(isSecretFile) ? R.envAdd : null;
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

// gh api의 값을 받는 옵션
const GH_API_VALUE = new Set(['-X', '--method', '-f', '--raw-field', '-F', '--field', '-H', '--header', '--input', '-q', '--jq', '-t', '--template', '--hostname', '--cache', '-p', '--preview']);

function checkGhApi(args) {
  let method = null;
  let body = false;
  let path = null;
  let hidden = false; // graphql 본문을 파일에서 읽어 내용을 볼 수 없음 (-F query=@파일, --input)
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    let n;
    let value;
    if (a.startsWith('--')) {
      const eq = a.indexOf('=');
      n = eq === -1 ? a : a.slice(0, eq);
      if (GH_API_VALUE.has(n)) value = eq === -1 ? args[++k] : a.slice(eq + 1);
    } else if (/^-[A-Za-z]/.test(a)) {
      // pflag는 짧은 옵션에 값을 붙여 쓰는 것(-XPUT, -fkey=val)을 받는다
      n = a.slice(0, 2);
      if (GH_API_VALUE.has(n)) value = a.length > 2 ? a.slice(2) : args[++k];
    } else {
      if (path === null) path = apiPath(a);
      continue;
    }
    if (!GH_API_VALUE.has(n)) continue;
    if (n === '-X' || n === '--method') method = String(value || '').toUpperCase();
    if (['-f', '--raw-field', '-F', '--field', '--input'].includes(n)) body = true;
    if (n === '--input' || ((n === '-F' || n === '--field') && /^[^=]*=@/.test(String(value || '')))) hidden = true;
  }
  if (path === 'graphql' && hidden) return R.ghApi;
  return githubWrite(path, method || (body ? 'POST' : 'GET'), args.join(' ')) ? R.ghApi : null;
}

// 하위 명령에 따라 막는 gh 그룹 (별칭 판정용)
const GH_GUARDED = new Set(['pr', 'repo', 'release', 'secret', 'variable']);
// gh 내장 명령 (gh 2.x help 기준, co는 내장 별칭)
const GH_COMMANDS = new Set(
  'agent-task alias api attestation auth browse cache co codespace completion config copilot extension gist gpg-key help issue label org pr preview project release repo ruleset run search secret ssh-key status variable version workflow'.split(
    ' ',
  ),
);

function checkGh(args, ctx = {}, depth = 0) {
  const [group, action] = args;
  if (group === 'alias') {
    // 별칭에 숨긴 명령 (#191): `gh alias set m 'pr merge'` 뒤 `gh m 1`. 확장 내용을 같은 규칙으로 본다
    if (action === 'import') return R.ghAlias;
    if (action !== 'set') return null;
    const o = options(args.slice(2));
    const expansion = o.positional[1];
    if (expansion === undefined || expansion === '-') return R.ghAlias;
    if (depth >= 3) return null;
    if (o.flags.has('-s') || o.flags.has('--shell') || expansion.startsWith('!')) {
      // gh는 셸 별칭을 sh -c로 실행하고 뒤 인자를 $1…로 넘긴다: gh 다음에 $N이 오면 어떤 하위 명령이든 될 수 있다
      if (/\bgh(?:\.exe)?\s+(?:\S+\s+)?["']?\$/.test(expansion)) return R.ghAlias;
      return checkCommand(expansion.replace(/^!/, ''), { ...ctx, shell: 'bash' }, depth + 1);
    }
    // 별칭 뒤에 붙인 인자는 확장 뒤에 이어 붙는다: 하위 명령을 덜 쓴 확장(pr, api, 'pr $1')은 무엇이든 될 수 있다
    const words = parseCommand(expansion)[0]?.words || [];
    if (words[0] === 'api' || (GH_GUARDED.has(words[0]) && (!words[1] || /^[$-]/.test(words[1])))) return R.ghAlias;
    return checkGh(words, ctx, depth + 1);
  }
  // 모르는 하위 명령은 이미 만들어 둔 별칭이나 확장일 수 있다 (gh alias list를 hook에서 읽지 않는다, #245)
  if (group && !group.startsWith('-') && !GH_COMMANDS.has(group)) return R.ghUnknown;
  if (group === 'pr' && action === 'merge') return R.merge;
  if (group === 'api') return checkGhApi(args.slice(1));
  if (group === 'repo' && ['delete', 'edit', 'rename', 'archive'].includes(action)) return R.repo;
  if (['release', 'secret', 'variable'].includes(group) && ['delete', 'remove', 'set'].includes(action)) return R.secret;
  return null;
}

// curl의 값을 받는 짧은 옵션 (-d 본문, -F 폼, -T 업로드, -X 메서드, -H 헤더, -u 계정 등)
const CURL_SHORT_VALUE = 'dFTXHuoAebcKmrwxECQyYz';

/** curl·wget·Invoke-RestMethod로 GitHub API를 직접 부르는 경우 (#130) */
function checkHttp(args) {
  // curl은 URL을 여러 개 받아 같은 메서드로 차례로 요청한다: 하나라도 쓰기 경로면 막는다
  const urls = args.filter((a) => /^https?:\/\/api\.github\.com\//i.test(a));
  if (!urls.length) return null;
  let method = null;
  let body = false;
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    // curl --request X, wget --method=X·--method X (#193), PowerShell -Method X·-Method:X
    const m = /^(?:--request|--method|-method)(?:[=:](.*))?$/i.exec(a);
    if (m) method = String(m[1] ?? args[++k] ?? '').toUpperCase();
    else if (/^(--data.*|--json|--form.*|--upload-file|-body|--post-data|--body-data)$/i.test(a.split('=')[0])) body = true;
    else if (/^-[A-Za-z]/.test(a) && !a.startsWith('--')) {
      // curl 짧은 옵션 묶음: 값을 받는 글자에서 나머지(없으면 다음 단어)가 값이다 (-sXPUT, -d'{…}', -sd@f)
      for (let j = 1; j < a.length; j++) {
        if (!CURL_SHORT_VALUE.includes(a[j])) continue;
        const value = a.slice(j + 1) || args[++k] || '';
        if (a[j] === 'X') method = value.toUpperCase();
        if ('dFT'.includes(a[j])) body = true;
        break;
      }
    }
  }
  return urls.some((url) => githubWrite(apiPath(url), method || (body ? 'POST' : 'GET'), args.join(' '))) ? R.ghApi : null;
}

// 경로가 아니라 글을 받는 명령 (eval 경로 검사에서 뺀다)
const TEXT_ONLY = new Set(['echo', 'printf', 'write-output', 'write-host']);

// eval 저장소를 받아 오거나 여는 하위 명령 (저장소 이름 검사는 여기에만. 메시지 속 이름은 막지 않는다)
const EVAL_REPO_GIT = new Set(['clone', 'fetch', 'pull', 'remote', 'submodule', 'ls-remote']);
const EVAL_REPO_GH = new Set(['clone', 'view', 'fork', 'sync']);

/**
 * 와일드카드 경로가 dir(또는 그 안)에 닿는가: 앞 조각들이 dir의 조각마다 맞으면 (`data/raw/ev*`, `data/raw/*`, #189).
 * bash는 globstar가 기본으로 꺼져 있어 `**`도 한 조각으로 본다 (코드 속 `/**` 주석 오탐 방지)
 */
function globReaches(pattern, dir) {
  const win = process.platform === 'win32';
  const parts = (p) => {
    const s = resolve(String(p)).replace(/\\/g, '/').replace(/\/+$/, '');
    return (win ? s.toLowerCase() : s).split('/');
  };
  const g = parts(pattern);
  const d = parts(dir);
  for (let i = 0; i < d.length; i++) {
    if (i >= g.length) return false;
    if (!globMatch(g[i], d[i], { icase: false })) return false;
  }
  return true;
}

/**
 * 단어가 eval 폴더나 그 안을 가리키는가. 공백이 든 단어는 메시지로 본다 (경로에 공백을 쓰지 않는다).
 * glob: 셸이 와일드카드를 펼치는가. 인라인 코드 문자열은 fs가 펼치지 않으므로 false (`'**\/config'` 오탐 방지)
 */
/** 브레이스를 펼친 단어들 (a{b,c}d → abd, acd). 많아도 32개까지 */
function expandBraces(w) {
  const m = /\{([^{}]*,[^{}]*)\}/.exec(w);
  if (!m) return [w];
  return m[1]
    .split(',')
    .flatMap((alt) => expandBraces(w.slice(0, m.index) + alt + w.slice(m.index + m[0].length)))
    .slice(0, 32);
}

function evalHit(w, cwd, ctx, glob = true) {
  const p = String(w ?? '').replace(/^(?:--?[\w-]+|[A-Za-z_]\w*)=/, '');
  if (!p || /\s/.test(p)) return false;
  // 셸 브레이스 펼치기: data/raw/{eval,x}/a (#245)
  if (glob && /\{[^{}]*,[^{}]*\}/.test(p)) return expandBraces(p).some((v) => evalHit(v, cwd, ctx, true));
  // 셸 글롭은 있는 경로로만 펼쳐진다: eval 폴더가 없으면(평소 김동환 PC) 막아도 얻는 것이 없다
  if (glob && /[*?[]/.test(p)) return Boolean(ctx.exists?.(ctx.evalDir)) && globReaches(resolvePath(cwd, p), ctx.evalDir);
  return /eval/i.test(p) && isWithin(resolvePath(cwd, p), ctx.evalDir);
}

/** 폴더를 통째로 훑거나 옮기는 명령의 대상 폴더 (#189). 재귀가 아니면 [] */
function recursiveRoots(name, args) {
  const positional = args.filter((a) => !a.startsWith('-') && !/^\/[A-Za-z]$/.test(a));
  const has = (re) => args.some((a) => re.test(a));
  const short = (c) => args.some((a) => /^-[A-Za-z]+$/.test(a) && a.includes(c));
  const orCwd = (list) => (list.length ? list : ['.']);
  switch (name) {
    case 'grep':
    case 'egrep':
    case 'fgrep':
    case 'rg': {
      const s = searchSpec(name, args);
      // rg는 기본으로 .gitignore(data/raw/)를 따른다
      if (name === 'rg' && !s.noIgnore && !s.globs.some((g) => !g.glob.startsWith('!'))) return [];
      return s.recursive ? s.roots : [];
    }
    case 'find': {
      const roots = [];
      for (const a of args) {
        if (/^-([HLP]|O\d)$/.test(a)) continue;
        if (a.startsWith('-') || a === '(' || a === '!') break;
        roots.push(a);
      }
      return orCwd(roots);
    }
    case 'cp':
    case 'scp':
    case 'rsync':
    case 'copy-item':
    case 'cpi':
    case 'copy':
      return short('r') || short('R') || short('a') || has(/^--(recursive|archive)$/) ? positional : [];
    case 'zip':
      return short('r') || has(/^--recurse-paths$/) ? positional : [];
    case 'ls':
    case 'get-childitem':
    case 'gci':
    case 'dir':
      return short('R') || has(PS_RECURSE) || has(/^-depth$/i) || has(/^--recursive$/) || has(/^\/s$/i) ? orCwd(positional) : [];
    case 'tree':
    case 'du':
      return orCwd(positional);
    case 'tar':
    case '7z':
    case 'robocopy':
    case 'xcopy':
    case 'compress-archive':
      return positional;
    default:
      return [];
  }
}

/** 김동환 PC에서 eval 경로를 건드리는가 (HX-2). 메시지 인자(git commit -m, gh pr create --body, --did "…")는 경로로 보지 않는다 */
function checkEval(name, args, redirects, cwd, ctx) {
  const repo = (list) => list.some((a) => /vibenote-eval/i.test(a));
  if (name === 'git' && EVAL_REPO_GIT.has(parseGit(args).sub) && repo(args)) return R.evalRead;
  if (name === 'gh' && ((args[0] === 'repo' && EVAL_REPO_GH.has(args[1])) || args[0] === 'api') && repo(args)) return R.evalRead;
  if (['curl', 'wget', 'invoke-webrequest', 'iwr', 'invoke-restmethod', 'irm'].includes(name) && repo(args)) return R.evalRead;
  // 글 전용 명령·gh는 인자를 경로로 보지 않지만 리다이렉션 대상은 본다 (`printf x > data/raw/eval/a.json`, #193)
  let words = TEXT_ONLY.has(name) || name === 'gh' ? [] : args;
  if (name === 'git') {
    const g = parseGit(args);
    words = [...(g.dir ? [g.dir] : []), ...g.paths, ...(['commit', 'tag', 'notes'].includes(g.sub) ? [] : g.rest)];
  }
  if ([...words, ...redirects.map((r) => r.target)].some((w) => evalHit(w, cwd, ctx))) return R.evalRead;
  // eval 폴더를 품는 상위 폴더를 재귀로 훑기 (eval 폴더가 있을 때만. 평소 김동환 PC에는 없다)
  if (ctx.exists?.(ctx.evalDir) && recursiveRoots(name, args).some((r) => isWithin(ctx.evalDir, searchRoot(cwd, r, ctx)))) return R.evalRead;
  return null;
}

/** 인라인 코드: 비밀 파일 읽기, eval 경로 문자열, 문자열로 넘긴 셸 명령 (HX-1, #189) */
// 폴더를 재귀로 훑는 코드 API (os.walk, Path.rglob, glob, readdirSync(…, { recursive: true }), copytree 등)
const CODE_WALK = /\bos\.walk\b|\.rglob\b|\bglob(?:Sync)?\s*\(|\bglob\.i?glob\b|\brecursive\s*[:=]\s*(?:true|True)\b|\bcopytree\b|\bwalkSync\b|\bcpSync\b/;

/**
 * 인라인 코드가 eval 폴더를 건드리는가 (#189, #245): eval 경로 문자열, 이어 붙인 경로 조각(path.join('data','raw','eval')),
 * eval 폴더가 있을 때 그 상위 폴더를 재귀로 훑는 API (글롭 문자열 포함)
 */
function codeTouchesEval(code, cwd, ctx) {
  const literals = stringLiterals(code);
  if (literals.some((l) => evalHit(l, cwd, ctx, false))) return true;
  // 쉼표로 이어진 문자열 조각: 'data', 'raw', 'eval' → data/raw/eval
  for (const run of code.match(/(?:(['"`])[^'"`\s]*\1\s*,\s*)+(['"`])[^'"`\s]*\2/g) ?? []) {
    const parts = [...run.matchAll(/(['"`])([^'"`\s]*)\1/g)].map((m) => m[2]);
    if (evalHit(parts.join('/'), cwd, ctx, false)) return true;
  }
  if (!ctx.exists?.(ctx.evalDir) || !CODE_WALK.test(code)) return false;
  return literals.some((l) => l && !/\s/.test(l) && (isWithin(ctx.evalDir, resolvePath(cwd, l)) || (/[*?[]/.test(l) && globReaches(resolvePath(cwd, l), ctx.evalDir))));
}

function checkCode(code, ctx, depth) {
  if (codeReadsSecret(code)) return R.envRead;
  if (ctx.evalDir && codeTouchesEval(String(code ?? ''), ctx.cwd || process.cwd(), ctx)) return R.evalRead;
  if (depth >= 3) return null;
  for (const literal of stringLiterals(code)) {
    const r = checkCommand(literal, { ...ctx, shell: 'bash' }, depth + 1);
    if (r) return r;
  }
  return null;
}

/** 인터프리터(node·python…) 실행: --env-file, -e/-c 코드, 표준 입력으로 받는 코드 */
function checkInterpreter(name, args, seg, ctx, depth) {
  for (let k = 0; k < args.length; k++) {
    const m = /^--env-file(?:-if-exists)?(?:=(.*))?$/.exec(args[k]);
    if (m && isSecretFile(m[1] ?? args[k + 1] ?? '')) return R.envRead;
  }
  // 코드 옵션: 정확한 이름(-e, --eval), 값을 붙인 긴 옵션(--eval=…), 끝 글자가 코드 옵션인 묶음(node -pe, python -Ic)
  const short = INTERPRETERS[name].filter((o) => /^-[A-Za-z]$/.test(o)).map((o) => o[1]);
  const bundle = short.length ? new RegExp(`^-[A-Za-z]*[${short.join('')}]$`) : null;
  for (let k = 0; k < args.length; k++) {
    const a = args[k];
    const eq = a.indexOf('=');
    if (a.startsWith('--') && eq !== -1 && INTERPRETERS[name].includes(a.slice(0, eq))) return checkCode(a.slice(eq + 1), ctx, depth);
    if (INTERPRETERS[name].includes(a) || bundle?.test(a)) return checkCode(args[k + 1] ?? '', ctx, depth);
    // python·perl·ruby·php는 코드를 옵션에 붙여 쓸 수 있다 (python3 -c'…', python -Ic"…"). node는 붙여 쓰기를 받지 않는다
    const attached = short.length && !['node', 'bun', 'deno'].includes(name) ? new RegExp(`^-[A-Za-z]*?[${short.join('')}](.+)$`, 's').exec(a) : null;
    if (attached) return checkCode(attached[1], ctx, depth);
  }
  // 스크립트 파일 없이 실행(node, python -)하면 표준 입력을 코드로 읽는다
  if (!args.some((a) => !a.startsWith('-')) || args.includes('-')) {
    const code = seg.stdin ?? pipedText(seg.pipeFrom);
    if (code) return checkCode(code, ctx, depth);
  }
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
 * ctx.cwd: 시작 위치, ctx.exists(path): cd 대상이 있는지, ctx.commitBlocked(cwd): 그 위치에서 커밋하면 안 되는 보호 브랜치 이름(없으면 null),
 * ctx.isDir(path): 디렉터리인지 (되돌리기 범위 판정), ctx.evalDir: 열람을 막을 eval 폴더 절대 경로 (김동환 PC만),
 * ctx.secretFiles(dir): dir 아래 비밀 파일 [{ path, ignored: .gitignore 대상인가 }] (재귀 검색 판정, 없으면 판정하지 않는다)
 */
// 값을 알 수 없는 변수 (searchRoot가 $가 남은 경로로 보고 / 로 판정한다)
const UNKNOWN_VAR = '$?';

export function checkCommand(command, context = {}, depth = 0) {
  const ctx = { ...context, vars: { ...context.vars } }; // vars: 이 명령 안에서 할당한 셸 변수 (searchRoot가 펼친다)
  let cwd = ctx.cwd || process.cwd();
  const shell = ctx.shell || 'bash';
  const nested = (script, sh) => (depth < 3 && script ? checkCommand(script, { ...ctx, cwd, shell: sh }, depth + 1) : null);
  const stack = [];
  const scopes = []; // 열린 서브셸·명령 치환 안의 명령들
  let lastScope = null; // 방금 닫힌 명령 치환 안의 명령들 (`grep x $(find …)`가 받는 목록, #245)
  for (const seg of parseSegments(command, shell)) {
    if (seg === PUSH) {
      // 서브셸·명령 치환 안의 cd와 변수 할당은 바깥에 남지 않는다 (#139, Codex)
      stack.push({ cwd, vars: { ...ctx.vars } });
      scopes.push([]);
      continue;
    }
    if (seg === POP) {
      const saved = stack.pop();
      if (saved) ({ cwd } = saved), (ctx.vars = saved.vars);
      lastScope = scopes.pop() ?? null;
      continue;
    }
    scopes[scopes.length - 1]?.push(seg);
    if (seg.redirects.some((r) => r.op.startsWith('<') && isSecretFile(r.target))) return R.envRead;
    // GIT_CONFIG_KEY_n=core.hooksPath, GIT_CONFIG_PARAMETERS로 hook 경로를 바꾸는 환경변수 (할당·export·$env: 모두)
    if (seg.words.some((w) => /GIT_CONFIG/i.test(w)) && seg.words.some((w) => /core\.hookspath/i.test(w))) return R.hooksPath;
    // 명령 없는 할당(D=…)·export는 뒤 명령의 검색 경로를 펼칠 때 쓴다 (`D=docs; grep -rn x $D`).
    // 실행되지 않을 수 있는 할당(&& || 뒤, if·while 안), 명령 앞 할당, for·read로 바뀌는 변수는 값을 모르는 것으로 둔다 (Codex)
    const assigns = seg.words[0] === 'export' ? seg.words.slice(1) : seg.words;
    const simple = assigns.length > 0 && assigns.every(isAssign) && !seg.cond;
    for (const w of seg.words) if (isAssign(w)) ctx.vars[w.slice(0, w.indexOf('='))] = simple ? w.slice(w.indexOf('=') + 1) : UNKNOWN_VAR;
    if (/^(for|select)$/.test(seg.words[0] ?? '') && seg.words[1]) ctx.vars[seg.words[1]] = UNKNOWN_VAR;
    const head = unwrap(seg.words);
    if (head.name === 'read') for (const a of head.args) if (!a.startsWith('-')) ctx.vars[a] = UNKNOWN_VAR;
    // 환경변수 할당 값도 경로로 본다: GIT_DIR=data/raw/eval/.git git log, export GIT_DIR=…, $env:GIT_DIR='…' (#189)
    const assigned = (w, i) => isAssign(w) || PS_VAR_EQ.test(w) || (PS_ASSIGN_OP.test(seg.words[i - 1] ?? '') && PS_VAR.test(seg.words[i - 2] ?? ''));
    if (ctx.evalDir && seg.words.some((w, i) => assigned(w, i) && evalHit(w.includes('=') ? w.slice(w.indexOf('=') + 1) : w, cwd, ctx))) return R.evalRead;
    // ForEach-Object 블록은 파이프로 받은 파일을 연다: gi .env | % { gc $_ } (#190). 블록 내용과 상관없이 넘긴 파일로 본다
    if (shell === 'powershell' && seg.pipeFrom && ['%', 'foreach-object', 'foreach'].includes(String(seg.words[0] ?? '').toLowerCase()) && psPipeReadsSecret(seg.pipeFrom, cwd, ctx))
      return R.envRead;
    const { name, args } = unwrap(seg.words);
    if (!name) continue;
    if (ctx.evalDir) {
      const r = checkEval(name, args, seg.redirects, cwd, ctx);
      if (r) return r;
    }
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
    if (INTERPRETERS[name]) {
      const r = checkInterpreter(name, args, seg, { ...ctx, cwd }, depth);
      if (r) return r;
      continue;
    }
    const r =
      name === 'git'
        ? checkGit(args, cwd, ctx)
        : name === 'gh'
          ? checkGh(args, { ...ctx, cwd }, depth)
          : ['curl', 'wget', 'invoke-restmethod', 'irm', 'invoke-webrequest', 'iwr'].includes(name)
            ? checkHttp(args)
            : null;
    if (r) return r;
    if (READERS.has(name) && readTargets(name, args).some(isSecretFile)) return R.envRead;
    // 셸이 펼칠 글롭 인자가 비밀 파일 자체를 고른다: cat .e*, grep KEY .* (#245)
    if (READERS.has(name) && readTargets(name, args).some((t) => /[*?[]/.test(t) && shellGlobFiles(t, cwd, ctx).some((x) => x.exact))) return R.envRead;
    if (SEARCHERS.has(name) && searchReadsSecret(name, args, cwd, ctx)) return R.envSearch;
    // 파일 이름을 넘겨받아 읽는다: find … | xargs grep, find … -exec cat {} +, grep x $(find …) (#245)
    const reads = (n) => READERS.has(n) || SEARCHERS.has(n);
    if (reads(name) && seg.pipeFrom && seg.words.some((w) => cmdName(w) === 'xargs') && findEmitsSecret(seg.pipeFrom, cwd, ctx)) return R.envRead;
    if (reads(name) && args.some((a) => a.includes(SUBST)) && lastScope?.some((s) => findEmitsSecret(s, cwd, ctx))) return R.envRead;
    if (name === 'find') {
      const x = args.findIndex((a) => /^-(exec|execdir|ok|okdir)$/.test(a));
      if (x !== -1 && reads(cmdName(args[x + 1])) && findEmitsSecret(seg, cwd, ctx)) return R.envRead;
    }
    if (name === 'findstr' && findstrReadsSecret(args, cwd, ctx)) return R.envSearch;
    // PowerShell 괄호로 넘긴 목록을 읽는다: Get-Content (gci -Filter .env*), sls KEY (gci -r) (#245)
    if (shell === 'powershell' && seg.argOf && READERS.has(unwrap(seg.argOf.words).name) && psPipeReadsSecret(seg, cwd, ctx)) return R.envRead;
    // .NET 호출: [IO.File]::ReadAllText('.env'), [IO.StreamReader]::new(…), New-Object IO.StreamReader(…) (#190)
    if ((seg.words.some((w) => /\]::\w/.test(w)) || name === 'new-object') && seg.words.some((w) => w.split(/[(),;\s]+/).some(isSecretFile))) return R.envRead;
    if (shell === 'powershell' && seg.pipeFrom && READERS.has(name) && psPipeReadsSecret(seg.pipeFrom, cwd, ctx)) return R.envRead;
  }
  return null;
}

// ── 도구별 입력 (H-1) ── 내장 Bash·PowerShell과 Desktop Commander MCP의 셸 도구
const DC_START = /desktop-commander__start_process$/;
const DC_INPUT = /desktop-commander__interact_with_process$/;

/** MCP 셸 이름 → 판정할 셸 목록. 지정이 없으면(Windows 기본 powershell, 그 밖 기본 셸) 둘 다 본다 */
function shellsOf(sh) {
  const s = String(sh || '').toLowerCase();
  if (!s) return ['bash', 'powershell'];
  if (/cmd(\.exe)?$/.test(s)) return ['cmd'];
  if (/pwsh|powershell/.test(s)) return ['powershell'];
  return ['bash'];
}

/** PreToolUse 입력(tool_name, tool_input)을 판정한다. 셸 도구가 아니면 null */
export function checkTool(toolName, input = {}, ctx = {}) {
  const t = String(toolName || '');
  const run = (command, shells) => {
    for (const shell of shells) {
      const r = checkCommand(command, { ...ctx, shell });
      if (r) return r;
    }
    return null;
  };
  if (t === 'Bash') return run(String(input.command || ''), ['bash']);
  if (t === 'PowerShell') return run(String(input.command || ''), ['powershell']);
  if (DC_START.test(t)) return run(String(input.command || ''), shellsOf(input.shell));
  // 실행 중인 셸이나 REPL(node -i, python -i)에 보내는 입력: 셸 명령이든 코드든 본다
  if (DC_INPUT.test(t)) {
    const text = String(input.input || '');
    return run(text, ['bash', 'powershell']) || checkCode(text, ctx, 0);
  }
  return null;
}
