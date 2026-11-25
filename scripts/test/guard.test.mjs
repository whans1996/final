// guard-bash hook의 명령 판정 (H-1: 정규식 → 셸 토큰 파서)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { checkCommand, parseCommand, resolvePath } from '../lib/guard.mjs';

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

test('cd 경로: Windows에서 Git Bash 경로를 드라이브 경로로', () => {
  assert.equal(resolvePath('C:/repo', '/c/Users/SSAFY/final', 'win32'), 'C:/Users/SSAFY/final');
  assert.equal(resolvePath('C:/repo', '/d', 'win32'), 'D:/');
  assert.equal(resolvePath('/repo', '/c/Users', 'linux'), '/c/Users');
});
