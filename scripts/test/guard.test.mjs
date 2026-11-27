// guard-bash hook의 명령 판정 (H-1: 정규식 → 셸 토큰 파서)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { checkCommand, checkTool, parseCommand, resolvePath } from '../lib/guard.mjs';
import { isWithin } from '../lib/protect.mjs';

const blocked = (cmd, opts) => checkCommand(cmd, { cwd: '/repo', ...opts });
const ok = (cmd, opts) => assert.equal(blocked(cmd, opts), null, `차단되면 안 됨: ${cmd}`);
const no = (cmd, opts) => assert.ok(blocked(cmd, opts), `차단되어야 함: ${cmd}`);

test('파서: 따옴표·연산자·리다이렉션·heredoc·명령 치환', () => {
  const words = (cmd) => parseCommand(cmd).map((s) => s.words);
  assert.deepEqual(words(`cd "a b" && git commit -m 'x; y' | cat`), [['cd', 'a b'], ['git', 'commit', '-m', 'x; y'], ['cat']]);
  assert.deepEqual(words('git log 2>&1 > out.txt; echo a\\ b'), [['git', 'log'], ['echo', 'a b']]);
  assert.deepEqual(parseCommand('cat < .env')[0].redirects, [{ op: '<', target: '.env' }]);
  // heredoc 본문은 데이터다
  assert.deepEqual(words("cat <<'EOF' > f\ngit push --force\nEOF\necho done"), [['cat'], ['echo', 'done']]);
  // 명령 치환 안의 명령도 꺼낸다 (따옴표 안이어도)
  assert.deepEqual(words('git commit -m "$(cat <<\'EOF\'\nfix: a)\nEOF\n)"'), [['cat'], ['git', 'commit', '-m', '$()']]);
  assert.deepEqual(words('echo `git status` (cd x; ls)'), [['git', 'status'], ['echo', '$()'], ['cd', 'x'], ['ls']]);
  assert.deepEqual(words('# 주석 git push -f\nls'), [['ls']]);
});

test('hook 우회 금지: --no-verify, commit -n (메시지 안 글자는 무시)', () => {
  no('git commit --no-verify -m "x"');
  no('git push --no-verify');
  no('git commit -nm "x"');
  no('git commit -an -m x');
  ok('git commit -m "-n 옵션 설명: --no-verify 금지"');
  ok('git commit -m -n');
  ok("git commit -F - <<'EOF'\n--no-verify 를 쓰지 않는다\nEOF");
  ok('git log -n 3');
  ok('echo "--no-verify"');
});

test('강제 push·GitLab push·보호 브랜치 삭제', () => {
  no('git push --force origin feature/x');
  no('git push -f');
  no('git push --force-with-lease');
  no('git push origin +feature/x');
  no('git push gitlab main');
  no('git push -u gitlab main');
  no('git push https://lab.ssafy.com/x/y.git main');
  no('git branch -D develop');
  no('git branch --delete --force main');
  no('git push origin --delete develop');
  no('git push origin :main');
  ok('git push -u origin feature/common-harness-revamp');
  ok('git branch -D feature/old');
  ok('git commit -m "git push --force 금지"');
});

test('되돌리기 어려운 git 조작과 hook 경로 변경', () => {
  no('git reset --hard HEAD~1');
  no('git clean -fd');
  no('git checkout .');
  no('git checkout -- .');
  no('git restore .');
  no('git add -f .env.local');
  no('git add --force build/');
  no('git config core.hooksPath /tmp/x');
  no('git config --unset core.hooksPath');
  no('git -c core.hooksPath=/dev/null commit -m x');
  ok('git config core.hooksPath');
  ok('git config core.hooksPath .githooks');
  ok('git reset --soft HEAD~1');
  ok('git restore --staged a.txt');
  ok('git checkout -- src/a.js');
  ok('git clean -n');
});

test('GitHub: 머지·저장소·시크릿 변경은 사람이', () => {
  no('gh pr merge 12 --squash');
  no('gh repo delete x');
  no('gh secret set KEY');
  ok('gh pr view 12');
  ok('gh issue create --title "gh pr merge 금지"');
});

test('.env 읽기·커밋 금지, .env.example은 허용', () => {
  no('cat .env');
  no('cat backend/.env.local');
  no('head -5 .env*');
  no('grep KEY .env');
  no('Get-Content .env');
  no('node x.js < .env');
  no('git add .env');
  ok('cat .env.example');
  ok('git add .env.example');
  ok('echo "cat .env 금지"');
  ok('ls -la');
});

test('셸 래퍼 안의 명령도 본다', () => {
  no('bash -c "git push --force"');
  no('sh -c \'git reset --hard\'');
  no('powershell -Command "git push -f origin x"');
  no('FOO=1 git push --force');
  no('env GIT_TRACE=1 git push -f');
  no('eval "git clean -fdx"');
});

test('보호 브랜치 커밋: cd·git -C로 바꾼 위치 기준', () => {
  const commitBlocked = (cwd) => (cwd.endsWith('main-repo') ? 'develop' : null);
  const exists = () => true;
  no('cd /work/main-repo && git commit -m "fix: a"', { commitBlocked, exists });
  no('git -C /work/main-repo commit -m x', { commitBlocked, exists });
  ok('cd /work/feature-repo && git commit -m "fix: a"', { commitBlocked, exists });
  no('git commit -m x', { cwd: '/work/main-repo', commitBlocked, exists });
  ok('git status', { cwd: '/work/main-repo', commitBlocked, exists });
});

// PR 리뷰(reviewer)가 checkCommand로 직접 재현한 우회 사례. 옛 정규식 가드는 막았던 형태다
test('셸 예약어·래퍼 뒤의 명령도 본다', () => {
  no('if true; then git push --force origin x; fi');
  no('if git diff --quiet; then :; else git reset --hard; fi');
  no('for i in 1; do git reset --hard; done');
  no('until git push -f; do :; done');
  no('{ git push -f; }');
  no('! git push -f');
  no('true && { git clean -fdx; }');
  no('timeout 60 git push --force');
  no('timeout -s KILL 60 git push --force');
  no('nice -n 10 git reset --hard');
  no('echo x | xargs git push --force');
  no('xargs -I {} git push --force origin {}');
  no('env -u X git push -f');
  no('sudo -u me git push -f');
  ok('if git diff --quiet; then echo same; fi');
  ok('timeout 60 git status');
  ok('echo ${HOME} {a,b}');
  // case 분기 패턴의 ) 뒤도 명령이다 (Codex P2)
  no('case "$x" in *) git reset --hard ;; esac');
  no('case $x in a|b) echo; ;; (c) git push -f;; esac');
  ok('case $x in *) git status ;; esac');
});

test('PowerShell: \\는 글자 그대로, 백틱은 다음 글자 이스케이프, { }는 스크립트 블록', () => {
  const ps = (cmd) => checkCommand(cmd, { cwd: 'C:/repo', shell: 'powershell' });
  for (const cmd of [
    'Get-Content .\\.env',
    'type backend\\.env',
    'gc C:\\Users\\me\\final\\.env',
    'git commit -m "a`nb" --no-verify',
    'git push origin `--force',
    'if ($?) { git push --force }',
    'try {git reset --hard} catch {}',
    '1..2 | % { git push -f }',
    'iex "git push -f"',
    "Invoke-Expression 'git reset --hard'",
  ])
    assert.ok(ps(cmd), `차단되어야 함: ${cmd}`);
  assert.equal(ps('git commit -m "줄`n바꿈"'), null);
  assert.equal(ps('Get-Content .\\.env.example'), null);
  assert.equal(ps('Get-ChildItem C:\\repo\\docs'), null);
  // bash에서 부른 powershell -Command 안은 PowerShell 규칙으로 본다
  no('powershell -Command "Get-Content .\\.env"');
});

test('heredoc: 셸이 받는 본문과 따옴표 없는 본문의 명령 치환', () => {
  no('bash <<EOS\ngit push --force\nEOS');
  no("bash <<'EOS'\ngit push --force\nEOS");
  no("cat <<'EOS' | sh\ngit reset --hard\nEOS");
  no('cat <<EOS\n$(git push --force)\nEOS');
  no('cat <<EOS\n`git push --force`\nEOS');
  no('echo "git push -f" | bash');
  ok("cat <<'EOS'\n$(git push --force)\nEOS");
  ok('cat <<EOS\n본문의 git push --force 글자\nEOS');
  ok("git commit -F - <<'EOF'\nfix: git push --force 금지\nEOF");
  ok('echo "git push -f" | cat');
});

test('git 긴 옵션 축약과 $\'…\' 따옴표', () => {
  no('git commit --no-verif -m x');
  no('git push --force-w');
  no('git push --forc');
  no('git reset --har');
  no('git push --rep gitlab');
  no("git push $'--force'");
  no('git push $"--force"');
  ok('git push --dry-run');
  ok('git log --format=%s');
  ok('git commit --amend --no-edit');
});

test('hook 경로를 바꾸는 다른 방법: GIT_CONFIG_*, --config-env, alias', () => {
  no('GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=core.hooksPath GIT_CONFIG_VALUE_0=/tmp git commit -m x');
  no('export GIT_CONFIG_PARAMETERS="\'core.hooksPath\'=\'/tmp\'"');
  no('git --config-env core.hooksPath=X commit -m x');
  no('git --config-env=core.hooksPath=X commit -m x');
  no("git -c alias.ci='commit --no-verify' ci -m x");
  no("git -c alias.p='!git push -f' p");
  ok('git -c alias.st=status st');
  ok('git config --get core.hooksPath');
});

test('push: GitLab --repo, main·develop으로 직접 push', () => {
  no('git push --repo gitlab');
  no('git push --repo=gitlab feature/x');
  no('git push origin HEAD:develop');
  no('git push origin feature/x:main');
  no('git push origin feature/x:refs/heads/develop');
  no('git push origin develop');
  ok('git push origin feature/x:feature/x');
  ok('git push -u origin feature/develop-notes');
});

test('.env를 출력할 수 있는 다른 읽기 명령', () => {
  for (const cmd of ['sort .env', 'uniq .env', 'cut -c1-500 .env', 'diff .env .env.example', 'jq . .env', 'comm .env x', 'rev .env']) no(cmd);
  ok('diff .env.example frontend/.env.example');
});

test('H-3: 작업 트리를 버리는 checkout·switch·restore·stash', () => {
  const isDir = (p) => /[\\/](src|frontend)$/.test(p);
  for (const cmd of ['git checkout -f develop', 'git checkout --force main', 'git switch --discard-changes develop', 'git switch -f main', 'git stash drop', 'git stash drop stash@{1}', 'git stash clear', "git checkout -- '*.js'"])
    no(cmd);
  for (const cmd of ['git restore src', 'git restore --worktree --staged src', 'git checkout -- src', 'git checkout HEAD~1 -- frontend', 'git checkout src', 'git restore --source HEAD~1 frontend']) no(cmd, { isDir });
  for (const cmd of ['git restore src/a.js', 'git restore --staged src', 'git checkout develop', 'git checkout -b feature/x', 'git checkout -- src/a.js']) ok(cmd, { isDir });
  for (const cmd of ['git switch -c feature/x', 'git switch develop', 'git stash', 'git stash push -m x', 'git stash pop', 'git stash list', 'git stash show -p']) ok(cmd);
});

test('H-4: grep·rg의 검색 패턴은 파일이 아니다', () => {
  for (const cmd of ['grep "\\.env" src', 'rg -n "\\.env" scripts', "grep -rn '.env' docs", 'rg .env.local -g "*.md"', 'grep -e .env -r src', 'rg -A 3 .env scripts'])
    ok(cmd);
  for (const cmd of ['grep KEY .env', 'rg KEY backend/.env.local', 'grep -e KEY .env', 'grep -r -- KEY .env', 'rg -f pats.txt .env', 'rg KEY -g .env'])
    no(cmd);
  assert.ok(checkCommand('Select-String -Path .env -Pattern KEY', { cwd: 'C:/repo', shell: 'powershell' }));
  assert.equal(checkCommand('Select-String -Pattern ".env" -Path docs\\a.md', { cwd: 'C:/repo', shell: 'powershell' }), null);
});

test('HX-1: 인라인 코드·인터프리터로 비밀 파일 읽기, 키 파일', () => {
  for (const cmd of [
    `node -e "console.log(require('fs').readFileSync('.env','utf8'))"`,
    `node -p "require('fs').readFileSync('backend/.env.local','utf8')"`,
    `python -c "print(open('.env').read())"`,
    `python3 -c 'import dotenv; print(dotenv.dotenv_values())'`,
    `py -c "print(open('.env').read())"`,
    'node --env-file=.env -e "console.log(process.env)"',
    'node --env-file .env scripts/x.mjs',
    "node <<'EOF'\nconsole.log(require('fs').readFileSync('.env','utf8'))\nEOF",
    `echo "print(open('.env').read())" | python`,
    // 문자열로 넘긴 셸 명령도 본다
    `node -e "require('child_process').execSync('git push --force')"`,
    `python -c "import os; os.system('git reset --hard')"`,
    'cat certs/server.pem',
    'cat ~/.ssh/id_rsa',
  ])
    no(cmd);
  for (const cmd of [`node -e "console.log(require('fs').readFileSync('.env.example','utf8'))"`, 'node -e "console.log(1)"', 'node scripts/verify.mjs harness', `node -e "import('./scripts/lib/owners.mjs').then(console.log)"`, 'python -c "print(1)"'])
    ok(cmd);
});

test('HX-2: 김동환 PC(evalDir 지정)에서 data/raw/eval 접근과 eval 저장소 clone', () => {
  const ev = { evalDir: '/repo/data/raw/eval' };
  for (const cmd of ['cat data/raw/eval/README.md', 'ls data/raw/eval', 'cd data/raw/eval && cat x', 'cd data/raw && cat eval/sets.json', 'git -C data/raw/eval log -p', 'node x.js < data/raw/eval/a.json', 'cp -r data/raw/eval /tmp/x', 'gh repo clone JoonseobKim/vibenote-eval eval', 'git clone https://github.com/JoonseobKim/vibenote-eval.git'])
    no(cmd, ev);
  assert.ok(checkCommand('Get-Content data\\raw\\eval\\a.json', { cwd: '/repo', shell: 'powershell', ...ev }));
  for (const cmd of ['cat data/raw/README.md', 'ls data/raw', 'git commit -m "data/raw/eval/ 읽기 차단"', 'gh pr create --body "data/raw/eval 가드"', 'echo data/raw/eval', 'cat data/raw/eval-notes.md']) ok(cmd, ev);
  // 김준섭 PC(evalDir 없음)는 막지 않는다
  ok('cat data/raw/eval/README.md');
});

test('#130: gh api·curl로 머지·ref·저장소 설정·시크릿을 바꾸는 요청', () => {
  for (const cmd of [
    'gh api -X PUT repos/o/r/pulls/1/merge',
    'gh api --method PUT /repos/o/r/pulls/1/merge -f merge_method=squash',
    'gh api --method=PUT repos/o/r/pulls/1/merge',
    'gh api repos/o/r/pulls/1/merge -f merge_method=merge',
    'gh api -X POST repos/o/r/merges -f base=develop -f head=x',
    'gh api -X PATCH repos/o/r -f visibility=private',
    'gh api -X DELETE repos/o/r',
    'gh api -X PUT repos/o/r/actions/secrets/K -f encrypted_value=x',
    'gh api -X PATCH repos/o/r/git/refs/heads/develop -f sha=x -F force=true',
    'gh api -X PUT repos/o/r/contents/a.md -f message=x -f content=eA==',
    'gh api -X DELETE repos/o/r/branches/develop/protection',
    `gh api graphql -f query='mutation { mergePullRequest(input:{pullRequestId:"x"}) { clientMutationId } }'`,
    'curl -X PUT -H "Authorization: token x" https://api.github.com/repos/o/r/pulls/1/merge',
    'curl -d "{}" https://api.github.com/repos/o/r/merges',
  ])
    no(cmd);
  for (const cmd of [
    'gh api repos/o/r/pulls/1',
    'gh api repos/o/r/pulls/1/merge',
    'gh api --paginate --slurp repos/o/r/pulls/1/comments?per_page=100',
    'gh api -X POST repos/o/r/issues/1/comments -f body=x',
    'gh api -X POST repos/o/r/statuses/abc -f state=success -f context=Codex',
    `gh api graphql -f query='query { viewer { login } }'`,
    'curl https://api.github.com/repos/o/r',
  ])
    ok(cmd);
});

test('#139: 서브셸·명령 치환 안의 cd는 바깥 명령 위치로 이어지지 않는다', () => {
  const commitBlocked = (cwd) => (cwd.endsWith('main-repo') ? 'develop' : null);
  const exists = () => true;
  no('(cd /tmp; git status); git commit -m x', { cwd: '/work/main-repo', commitBlocked, exists });
  no('(cd /work/main-repo && git commit -m x)', { cwd: '/work/feature-repo', commitBlocked, exists });
  ok('(cd /work/main-repo; git status); git commit -m x', { cwd: '/work/feature-repo', commitBlocked, exists });
  ok('echo $(cd /work/main-repo; pwd); git commit -m x', { cwd: '/work/feature-repo', commitBlocked, exists });
  ok('cat <<EOS\n$(cd /work/main-repo)\nEOS\ngit commit -m x', { cwd: '/work/feature-repo', commitBlocked, exists });
  no('cd /work/main-repo; (git status); git commit -m x', { cwd: '/work/feature-repo', commitBlocked, exists });
});

test('MCP 셸 도구(Desktop Commander)도 같은 판정을 받는다 (H-1)', () => {
  const DC = 'mcp__plugin_desktop-commander_desktop-commander__';
  const tool = (name, input) => checkTool(name, input, { cwd: 'C:/repo' });
  assert.ok(tool('Bash', { command: 'git push -f' }));
  assert.ok(tool('PowerShell', { command: 'Get-Content .\\.env' }));
  assert.ok(tool(`${DC}start_process`, { command: 'git push --force origin x', timeout_ms: 1000 }));
  assert.ok(tool(`${DC}start_process`, { command: 'Get-Content .\\.env', timeout_ms: 1000 }));
  assert.ok(tool(`${DC}start_process`, { command: 'cat .env', shell: 'bash', timeout_ms: 1000 }));
  assert.ok(tool(`${DC}start_process`, { command: 'git commit --no-verify -m x', shell: 'cmd', timeout_ms: 1000 }));
  // 셸이든 REPL이든: 셸 명령, 인라인 코드의 비밀 파일 읽기, 문자열 속 셸 명령
  assert.ok(tool(`${DC}interact_with_process`, { pid: 1, input: 'git reset --hard' }));
  assert.ok(tool(`${DC}interact_with_process`, { pid: 1, input: 'gc .\\.env' }));
  assert.ok(tool(`${DC}interact_with_process`, { pid: 1, input: "require('fs').readFileSync('.env','utf8')" }));
  assert.ok(tool(`${DC}interact_with_process`, { pid: 1, input: "import os; os.system('git push -f')" }));
  assert.equal(tool(`${DC}start_process`, { command: 'node -i', timeout_ms: 1000 }), null);
  assert.equal(tool(`${DC}interact_with_process`, { pid: 1, input: 'console.log(1 + 1)' }), null);
  assert.equal(tool(`${DC}start_process`, { command: 'git status', timeout_ms: 1000 }), null);
  assert.equal(tool('Read', { file_path: 'x' }), null);
});

// PR 리뷰(reviewer)가 checkCommand로 재현한 우회·오탐
test('리뷰 반영: PowerShell·cmd 괄호는 서브셸이 아니다, 옵션 붙여 쓰기, 인라인 코드 옵션 변형', () => {
  const commitBlocked = (cwd) => (cwd.replace(/\\/g, '/').endsWith('main-repo') ? 'develop' : null);
  const exists = () => true;
  assert.ok(checkCommand('(cd C:/w/main-repo); git commit -m x', { cwd: 'C:/w/feature-repo', shell: 'powershell', commitBlocked, exists }));
  assert.ok(checkCommand('(cd C:/w/main-repo) & git commit -m x', { cwd: 'C:/w/feature-repo', shell: 'cmd', commitBlocked, exists }));
  assert.ok(checkCommand('(cd data/raw); gc eval/labels.json', { cwd: '/repo', shell: 'powershell', evalDir: '/repo/data/raw/eval', exists }));
  for (const cmd of [
    'gh api -XPUT repos/o/r/pulls/1/merge',
    'gh api repos/o/r/pulls/1/merge -fmerge_method=squash',
    'gh api -X POST repos/o/r/branches/develop/rename -f new_name=x',
    'gh api -X POST repos/o/r/transfer -f new_owner=x',
    'gh api graphql -F query=@m.graphql',
    'gh api graphql --input q.json',
    `node -pe "require('fs').readFileSync('.env','utf8')"`,
    `node --eval="require('fs').readFileSync('.env')"`,
    `node --print="require('fs').readFileSync('.env')"`,
    `python -Ic "print(open('.env').read())"`,
    'git switch --discard develop',
    'cat .ENV',
    'sed -n 1p .env',
    "awk '{print}' .env",
  ])
    no(cmd);
  for (const cmd of ["sed -i 's/foo/.env/' docs/a.md", "awk '/.env/' f.txt", 'sed -e s/a/b/ -i docs/a.md', 'gh api graphql -f query=@viewer']) ok(cmd);
  // graphql 값이 @로 시작하는 것은 -F(파일 읽기)일 때만 막는다
});

// PR #192 Codex P2: 값을 붙인 curl 짧은 옵션, GNU grep 긴 옵션 축약
test('Codex 반영: curl -d{…}·-XPUT 붙여 쓰기, grep --reg= 축약', () => {
  for (const cmd of [
    `curl -d'{"base":"develop","head":"feature/x"}' https://api.github.com/repos/o/r/merges`,
    'curl -XPUT https://api.github.com/repos/o/r/pulls/1/merge',
    'curl -sXPUT https://api.github.com/repos/o/r/pulls/1/merge',
    'curl -sd@body.json https://api.github.com/repos/o/r/merges',
    'grep --reg=KEY .env',
    'grep --rege KEY .env',
    'grep --fil=pats.txt .env',
    'rg --regex=KEY backend/.env.local',
    // Codex 2회차: curl은 URL을 여러 개 순서대로 요청한다, python -c 뒤에 코드를 붙여 쓸 수 있다
    'curl -X DELETE https://api.github.com/zen https://api.github.com/repos/o/r/git/refs/heads/develop',
    `python3 -c'print(open(".env").read())'`,
    `python -Ic"print(open('.env').read())"`,
    `perl -e'open(F, ".env")'`,
  ])
    no(cmd);
  for (const cmd of ['curl -s https://api.github.com/repos/o/r', 'curl -sH "Accept: application/json" https://api.github.com/repos/o/r/pulls/1/merge', 'grep --ignore-case "\\.env" src', 'grep --reg="\\.env" src'])
    ok(cmd);
});

test('리뷰 반영: 김동환 PC에서 메시지 속 eval 글자는 막지 않는다', () => {
  const ev = { evalDir: '/repo/data/raw/eval' };
  for (const cmd of [
    'git commit -m "vibenote-eval 정리"',
    'gh pr create --body "vibenote-eval 언급"',
    'gh issue comment 1 --body "JoonseobKim/vibenote-eval"',
    'node scripts/stamp.mjs --append-log --did "data/raw/eval/ 읽기 차단"',
    'node scripts/handoff.mjs -m "data/raw/eval/ 가드"',
  ])
    ok(cmd, ev);
  for (const cmd of ['git fetch https://github.com/JoonseobKim/vibenote-eval.git', 'git remote add ev https://github.com/JoonseobKim/vibenote-eval', 'gh repo view JoonseobKim/vibenote-eval', 'curl -L https://github.com/JoonseobKim/vibenote-eval/archive/main.zip'])
    no(cmd, ev);
});

test('cd 경로: Windows에서 Git Bash 경로를 드라이브 경로로', () => {
  assert.equal(resolvePath('C:/repo', '/c/Users/SSAFY/final', 'win32'), 'C:/Users/SSAFY/final');
  assert.equal(resolvePath('C:/repo', '/d', 'win32'), 'D:/');
  assert.equal(resolvePath('/repo', '/c/Users', 'linux'), '/c/Users');
});

// #188: 재귀 검색은 인자에 .env가 없어도 그 아래 비밀 파일을 읽는다. ctx.secretFiles는 hook이 git으로 찾은 실제 비밀 파일 목록
const SECRETS = [
  { path: '/repo/.env', ignored: true },
  { path: '/repo/backend/.env.local', ignored: true },
];
const secretFiles = (dir) => SECRETS.filter((f) => isWithin(f.path, dir));

test('#188: grep -r·rg -uu·rg -g 글롭이 그 아래 .env를 읽는다', () => {
  const sf = { secretFiles };
  for (const cmd of [
    'grep -rn GMS_API_KEY .',
    'grep -r KEY',
    'grep -R KEY backend',
    'grep --recursive KEY .',
    'grep -d recurse KEY .',
    'cd backend && grep -rn KEY',
    'grep -rn KEY / ',
    "grep -rn KEY --include='*env*' .",
    'rg -uu GMS_API_KEY',
    'rg --no-ignore --hidden KEY',
    "rg KEY -g '*env*'",
    'rg KEY --glob=.env*',
    'rg -uuu KEY backend',
  ])
    no(cmd, sf);
  for (const cmd of [
    'grep -rn KEY --exclude=".env*" .',
    "grep -rn KEY --include='*.java' .",
    "grep -rn '.env' docs",
    'grep -rn KEY src',
    'rg KEY',
    'rg -u KEY', // gitignore만 무시, 숨김 파일(.env)은 여전히 건너뜀
    'rg --hidden KEY', // 숨김 파일도 보지만 .gitignore는 따른다
    "rg -uu KEY -g '!.env*'",
    'rg -uu KEY -t java',
    "rg KEY -g '*.md'",
    'grep -n KEY scripts/a.mjs',
  ])
    ok(cmd, sf);
  // 비밀 파일 목록을 모르면(hook 밖) 판정하지 않는다
  ok('grep -rn KEY .');
});

test('#188: PowerShell gci 파이프로 넘긴 파일을 읽는 명령', () => {
  const ps = (cmd) => checkCommand(cmd, { cwd: '/repo', shell: 'powershell', secretFiles });
  for (const cmd of ['gci -r | sls KEY', 'Get-ChildItem -Recurse | Select-String KEY', 'gci | gc', 'Get-ChildItem backend -Recurse -File | Select-String KEY', 'gci backend | sls KEY'])
    assert.ok(ps(cmd), `차단되어야 함: ${cmd}`);
  for (const cmd of ['gci -r -Include *.java | sls KEY', 'gci docs | sls KEY', 'gci -r -Exclude .env* | sls KEY', 'gci . *.md | gc'])
    assert.equal(ps(cmd), null, `차단되면 안 됨: ${cmd}`);
});

test('#189: 김동환 PC에서 eval 폴더를 품는 글롭·재귀 명령·git-dir·인라인 코드', () => {
  const ev = { evalDir: '/repo/data/raw/eval', exists: () => true };
  for (const cmd of [
    'cat data/raw/ev*/README.md',
    'ls data/raw/*',
    'cat data/raw/e?al/a.json',
    'grep -r label data/raw',
    'find data/raw -type f',
    'find . -name "*.json"',
    'cp -r data/raw x',
    'tar czf x.tgz data',
    'zip -r x.zip data',
    'ls -R data',
    'rg -uu label data',
    'git --git-dir=data/raw/eval/.git log -p',
    'git --work-tree data/raw/eval status',
    `node -e "console.log(require('fs').readdirSync('data/raw/eval'))"`,
    `python -c "print(open('data/raw/eval/a.json').read())"`,
  ])
    no(cmd, ev);
  for (const cmd of ['gci -Recurse data', 'Copy-Item -Recurse data\\raw x', 'Compress-Archive -Path data -DestinationPath x.zip'])
    assert.ok(checkCommand(cmd, { cwd: '/repo', shell: 'powershell', ...ev }), `차단되어야 함: ${cmd}`);
  for (const cmd of ['cat data/raw/*.md', 'grep -r label scripts', 'find scripts -name x', 'ls data/raw', 'rg label data', 'cp -r scripts x', 'ls -R docs', 'cat data/*.json', 'node -e "console.log(1)"'])
    ok(cmd, ev);
  // eval 폴더가 없으면(평소 김동환 PC) 상위 폴더 재귀는 막지 않는다
  for (const cmd of ['find . -type f', 'grep -r label data/raw', 'tar czf x.tgz data']) ok(cmd, { ...ev, exists: () => false });
});

test('#190: PowerShell 할당·.NET 호출·파이프로 .env 읽기', () => {
  const ps = (cmd) => checkCommand(cmd, { cwd: 'C:/repo', shell: 'powershell' });
  for (const cmd of [
    '$x = Get-Content .env',
    '$x=Get-Content .env',
    '$x += gc .env',
    '[string]$x = gc .env',
    "[IO.File]::ReadAllText('.env')",
    '$t = [System.IO.File]::ReadAllLines("$PWD\\.env")',
    "[IO.File]::ReadAllText( '.env' )",
    "[IO.StreamReader]::new('backend\\.env.local').ReadToEnd()",
    'Get-Item .env | Get-Content',
    'gi backend\\.env | gc',
    '$x = Get-Item .env | Get-Content',
  ])
    assert.ok(ps(cmd), `차단되어야 함: ${cmd}`);
  for (const cmd of ['$x = Get-Content .env.example', "[IO.File]::ReadAllText('README.md')", 'Get-Item README.md | Get-Content', '$x = 1', '$env:LLM_PROVIDER = "mock"'])
    assert.equal(ps(cmd), null, `차단되면 안 됨: ${cmd}`);
});

test('#191: gh 별칭에 숨긴 명령, worktree remove --force, git rm -rf', () => {
  for (const cmd of [
    "gh alias set m 'pr merge'",
    "gh alias set --shell x 'gh pr merge 1'",
    "gh alias set -s x 'git push --force'",
    "gh alias set x '!git reset --hard'",
    "gh alias set --clobber a 'api -X PUT repos/o/r/pulls/1/merge'",
    'gh alias import aliases.yml',
    'gh alias set x -',
    'git worktree remove --force ../wt',
    'git worktree remove -f ../wt',
    'git rm -rf src',
    'git rm -r -f src',
    'git rm -r --force src',
  ])
    no(cmd);
  for (const cmd of ["gh alias set co 'pr checkout'", 'gh alias list', 'gh alias delete co', 'git worktree remove ../wt', 'git worktree add ../wt -b x', 'git rm src/a.js', 'git rm -r src', 'git rm -r --cached build', 'git rm -rf --cached build'])
    ok(cmd);
});

test('#193: 글 전용 명령의 리다이렉션 대상, wget --method', () => {
  const ev = { evalDir: '/repo/data/raw/eval' };
  for (const cmd of ["printf '%s' x > data/raw/eval/labels.json", 'echo x >> data/raw/eval/a.json', 'gh pr view 1 > data/raw/eval/x.md']) no(cmd, ev);
  for (const cmd of ['echo data/raw/eval', 'printf "%s" data/raw/eval > notes.md']) ok(cmd, ev);
  for (const cmd of ['wget --method=DELETE https://api.github.com/repos/o/r/git/refs/heads/develop', 'wget --method DELETE https://api.github.com/repos/o/r/git/refs/heads/develop'])
    no(cmd);
  assert.ok(checkCommand('irm -Method:Delete https://api.github.com/repos/o/r/git/refs/heads/develop', { cwd: 'C:/repo', shell: 'powershell' }));
  ok('wget https://api.github.com/repos/o/r/git/refs/heads/develop');
});

// PR(issue-188) reviewer가 checkCommand로 재현한 우회·오탐
test('리뷰 반영: gh 별칭을 덜 쓴 확장, --shell 확장의 gh $N', () => {
  for (const cmd of ['gh alias set m pr', 'gh alias set a api', "gh alias set x 'pr $1'", "gh alias set -s x 'gh $1 merge'", "gh alias set r 'repo'", "gh alias set --shell y 'gh pr $1 1'"])
    no(cmd);
  for (const cmd of ["gh alias set co 'pr checkout'", "gh alias set prs 'pr list --author @me'", "gh alias set iv 'issue view'", "gh alias set i 'issue $1'"]) ok(cmd);
  // --shell 확장은 gh가 sh -c로 실행한다: PowerShell에서 만들어도 bash 규칙으로 본다
  assert.ok(checkCommand("gh alias set -s x 'git push -f'", { cwd: 'C:/repo', shell: 'powershell' }));
});

test('리뷰 반영: eval — GIT_DIR 환경변수, 글롭은 eval 폴더가 있을 때만', () => {
  const ev = { evalDir: '/repo/data/raw/eval', exists: () => true, home: '/' };
  for (const cmd of ['GIT_DIR=data/raw/eval/.git git log -p', 'GIT_WORK_TREE=data/raw/eval git status', 'export GIT_DIR=data/raw/eval/.git; git log -p', 'find ~ -name "*.json"']) no(cmd, ev);
  assert.ok(checkCommand("$env:GIT_DIR='data/raw/eval/.git'; git log -p", { cwd: '/repo', shell: 'powershell', ...ev }));
  for (const cmd of ['ls data/raw/*', 'cat data/raw/*/README.md', 'wc -l data/raw/*/*']) ok(cmd, { ...ev, exists: () => false });
});

test('리뷰 반영: 재귀 검색 — 알 수 없는 시작 경로, git grep, 이름만 출력, 경로 글롭, 엄격한 제외 글롭', () => {
  const sf = { secretFiles, home: '/' }; // 홈(~)이 저장소를 품는다
  // 같은 명령 안에서 할당한 변수는 펼친다, ~ 아래 다른 폴더는 저장소 밖
  for (const cmd of ['D=.; grep -rn KEY $D', 'export D=/repo; grep -rn KEY "$D"', 'grep -rn KEY $PWD']) no(cmd, sf);
  for (const cmd of ['D=docs; grep -rn KEY $D/a', 'd="/c/tmp/ci"; grep -rh error -A6 $d', 'grep -rn flyway ~/.gradle']) ok(cmd, sf);
  for (const cmd of [
    'grep -rn KEY ~',
    'grep -rn KEY "$HOME"',
    'grep -rn KEY "$(pwd)"',
    'rg -uu KEY ~/',
    'git grep --no-index KEY',
    'git grep --untracked --no-exclude-standard KEY',
    "grep -r KEY --exclude='{x' .",
    'grep -r KEY --exclude=.ENV* .',
    "rg -uu KEY -g '!.ENV*'",
    'rg KEY -g "backend/**"',
  ])
    no(cmd, sf);
  for (const cmd of [
    'grep -rl GMS_API_KEY .',
    'grep -rc KEY .',
    'rg -uu -l KEY',
    'rg -uu --files-with-matches KEY',
    'git grep KEY',
    'git grep --untracked KEY',
    'git grep --no-index --exclude-standard KEY',
    'git grep --no-index KEY -- src',
    'git grep --no-index -l KEY',
    "rg KEY -g 'src/**'",
    "rg KEY -g 'docs/*'",
  ])
    ok(cmd, sf);
  // 목록에 없는 긴 옵션의 =값도 파일로 본다 (develop 동작 유지)
  no('grep --foo=.env KEY src');
  // Windows 경로
  const win = { cwd: 'C:/repo', secretFiles: (dir) => [{ path: 'C:/repo/.env', ignored: true }].filter((f) => isWithin(f.path, dir)) };
  assert.ok(checkCommand('grep -rn KEY .', win));
  assert.ok(checkCommand("grep -rn KEY 'C:\\repo'", win)); // bash에서 따옴표 없는 \는 이스케이프다
  assert.equal(checkCommand('grep -rn KEY scripts', win), null);
});

test('리뷰 반영: PowerShell ForEach-Object 블록으로 넘긴 파일, git rm -f 글롭 pathspec', () => {
  const ps = (cmd) => checkCommand(cmd, { cwd: '/repo', shell: 'powershell', secretFiles });
  for (const cmd of ['gi .env | % { gc $_ }', 'gi .env | ForEach-Object { $_.OpenText().ReadToEnd() }', 'gci -r | % { gc $_ }']) assert.ok(ps(cmd), `차단되어야 함: ${cmd}`);
  assert.equal(ps('gci docs | % { $_.Name }'), null);
  for (const cmd of ["git rm -f 'src/*'", "git rm --force ':(glob)src/**'"]) no(cmd);
  for (const cmd of ['git rm -f src/a.js', "git rm 'src/*.tmp'"]) ok(cmd);
});

// PR #246 Codex 1회차
test('Codex 반영: rg -g + eval, rg -L, grep --exclude 브레이스, 서브셸 변수, rg -t 타입', () => {
  const ev = { evalDir: '/repo/data/raw/eval', exists: () => true, secretFiles };
  assert.doesNotThrow(() => checkCommand("rg KEY -g 'data/**'", { cwd: '/repo', ...ev }));
  no("rg -uu KEY -g 'data/**' data", ev);
  const sf = { secretFiles };
  for (const cmd of [
    'rg -uu -L KEY',
    "grep -r KEY --exclude='{.env,*.pem}' .",
    'D=.; (D=scripts); grep -r KEY "$D"',
    'D=.; echo $(D=scripts); grep -r KEY $D',
    'rg -uu -t sh KEY .',
    'rg -uu -t config KEY .',
    "rg -uu --type-add 'e:.e*' -te KEY",
    "rg -uu --type-add 'e:*.env*' --type e KEY",
  ])
    no(cmd, sf);
  for (const cmd of ['rg -uu -t java KEY .', 'rg -uu -tjs -tts KEY .', "rg -uu --type-add 'j:*.java' -tj KEY", 'D=scripts; (D=.); grep -r KEY $D']) ok(cmd, sf);
});

// PR #246 Codex 2회차
test('Codex 반영: find 제외 -path는 전체 경로, 조건부·재할당 변수는 모름, rg --json은 내용을 낸다', () => {
  const sf = { secretFiles };
  for (const cmd of [
    "find . -type f ! -path '.env' -exec cat {} +",
    'D=.; false && D=scripts; grep -r KEY "$D"',
    'D=.; true || D=scripts; grep -r KEY $D',
    'D=scripts; if true; then D=.; fi; grep -r KEY $D',
    'D=scripts; for D in .; do grep -r KEY $D; done',
    'D=scripts; read D; grep -r KEY $D',
    'rg -uu -l --json KEY .',
    'rg -uu -c --json KEY .',
  ])
    no(cmd, sf);
  for (const cmd of ["find backend -type f ! -path 'backend/.env.local' -exec cat {} +","find . -type f -not -name '.env*' | xargs grep KEY", 'D=scripts; grep -r KEY $D && D=.', 'D=docs; D=scripts; grep -r KEY $D'])
    ok(cmd, sf);
});

// #245: 가드 우회 잔여 (PR #246 reviewer가 재현, 같은 PR에서 처리)
test('#245: 읽기·검색 인자의 셸 글롭 (bash는 * 가 점으로 시작하는 이름에 맞지 않는다)', () => {
  const sf = { secretFiles };
  for (const cmd of ['cat .e*', 'cat .en?', 'head .[e]nv', 'grep KEY .*', 'grep -rn KEY .*', 'grep -rn KEY *', 'cat backend/.env*', 'rg KEY .e*', 'cat */.env.local']) no(cmd, sf);
  for (const cmd of ['cat *.md', 'cat *', 'grep -n KEY src/*', 'grep -rn KEY scripts/*', 'cat .e*.example', 'cat docs/*.md']) ok(cmd, sf);
  // 목록이 없어도(hook 밖) 대표 이름으로 본다
  no('cat .e*');
  ok('cat *.md');
});

test('#245: 파이프·명령 치환·find -exec·findstr /s로 넘긴 파일 이름', () => {
  const sf = { secretFiles };
  for (const cmd of [
    'find . -type f | xargs grep KEY',
    'find . -name ".env*" | xargs cat',
    'find . -type f -exec grep KEY {} +',
    "find . -name '.env' -exec cat {} \\;",
    'grep KEY $(find . -type f)',
    'cat `find . -name ".env*"`',
    'find backend -type f | xargs -I{} grep KEY {}',
  ])
    no(cmd, sf);
  for (const cmd of ['find src -type f | xargs grep KEY', 'find . -name "*.java" | xargs grep KEY', 'find . -name "*.md" -exec grep KEY {} +', 'grep KEY $(find docs -name "*.md")', 'find . -name ".env*"', 'git ls-files | xargs grep KEY'])
    ok(cmd, sf);
  for (const sh of ['cmd', 'powershell']) {
    assert.ok(checkCommand('findstr /s KEY *.*', { cwd: '/repo', shell: sh, secretFiles }), `차단되어야 함 (${sh}): findstr /s`);
    assert.ok(checkCommand('findstr /si key *', { cwd: '/repo', shell: sh, secretFiles }));
    assert.equal(checkCommand('findstr /s KEY *.java', { cwd: '/repo', shell: sh, secretFiles }), null);
  }
});

test('#245: PowerShell 변형 — ${x}, 다중 할당, 괄호로 넘긴 목록, Where-Object 파이프', () => {
  const ps = (cmd) => checkCommand(cmd, { cwd: '/repo', shell: 'powershell', secretFiles });
  for (const cmd of [
    '${x} = gc .env',
    '$x, $y = gc .env',
    '$x,$y = gc .env',
    'Get-Content (gci -Filter .env*)',
    'gc (gi .env)',
    'sls KEY (gci -r)',
    "gci | ? { $_.Name -eq '.env' } | gc",
    'gci -r | Where-Object { $_.Length -gt 0 } | Select-String KEY',
    'gci | sort Name | gc',
  ])
    assert.ok(ps(cmd), `차단되어야 함: ${cmd}`);
  for (const cmd of ['${x} = gc README.md', 'Get-Content (gci docs -Filter *.md)', 'if (Test-Path .env) { echo ok }', 'gci docs | ? { $_.Length -gt 0 } | gc', '$a, $b = 1, 2'])
    assert.equal(ps(cmd), null, `차단되면 안 됨: ${cmd}`);
});

test('#245: eval — 인라인 코드의 경로 조각 이어 붙이기·재귀 탐색 API, 셸 브레이스', () => {
  const ev = { evalDir: '/repo/data/raw/eval', exists: () => true };
  for (const cmd of [
    `node -e "fs.readdirSync(path.join('data','raw','eval'))"`,
    `python -c "import os; print(list(os.walk('data')))"`,
    `node -e "fs.readdirSync('data/raw', { recursive: true })"`,
    `node -e "fs.globSync('data/raw/*/a.json')"`,
    `python -c "from pathlib import Path; print(list(Path('.').rglob('*.json')))"`,
    'cat data/raw/{eval,x}/a.json',
    'ls data/{raw,seed}/eval',
  ])
    no(cmd, ev);
  for (const cmd of [`node -e "fs.readdirSync('data/raw')"`, `python -c "import os; print(list(os.walk('scripts')))"`, `node -e "path.join('data','seed')"`, 'cat data/{seed,x}/a.json']) ok(cmd, ev);
  // eval 폴더가 없으면 재귀 탐색 API는 막지 않는다
  ok(`python -c "import os; print(list(os.walk('data')))"`, { ...ev, exists: () => false });
});

test('#245: 알 수 없는 gh 하위 명령은 미리 만든 별칭·확장일 수 있다', () => {
  for (const cmd of ['gh m 1', 'gh co-merge 12']) no(cmd);
  for (const cmd of ['gh pr view 1', 'gh issue list', 'gh run list', 'gh --version', 'gh auth status', 'gh workflow run CI', 'gh label list', 'gh search issues x', 'gh help']) ok(cmd);
});
