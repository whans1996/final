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

test('cd 경로: Windows에서 Git Bash 경로를 드라이브 경로로', () => {
  assert.equal(resolvePath('C:/repo', '/c/Users/SSAFY/final', 'win32'), 'C:/Users/SSAFY/final');
  assert.equal(resolvePath('C:/repo', '/d', 'win32'), 'D:/');
  assert.equal(resolvePath('/repo', '/c/Users', 'linux'), '/c/Users');
});
