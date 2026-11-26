// 비밀값·비밀 파일·eval 경로 판정 (H-2, HX-1, HX-2). 테스트 안의 가짜 키는 조각을 이어 붙여 만든다 (커밋 hook·push protection 오탐 방지)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { EVAL_FETCH_EXCLUDE, codeReadsSecret, evalFetchExcluded, findSecrets, isSecretFile, isWithin, scanDiffForSecrets, scanMergeForSecrets } from '../lib/protect.mjs';

const fake = {
  ghp: 'ghp_' + 'a'.repeat(36),
  pat: 'github_pat_' + 'B1'.repeat(15),
  gemini: 'AIza' + 'c'.repeat(35),
  mattermost: 'https://meeting.ssafy.com/hooks/' + 'a1b2c3d4e5f6g7h8i9j0k1l2m3',
  slack: 'https://hooks.slack.com/services/' + 'T0123/B0456/' + 'x'.repeat(24),
  discord: 'https://discord.com/api/webhooks/' + '123456789/' + 'abc-DEF_1',
};

test('비밀 파일: .env 계열과 키 파일, .env.example은 제외', () => {
  // NTFS는 대소문자를 무시하고 `파일::$DATA`(대체 데이터 스트림 표기)로도 같은 파일을 읽는다
  for (const f of ['.env', '.env.local', 'backend/.env.production', '.env*', 'C:\\repo\\.env', 'certs/server.pem', 'a/b.key', 'keystore.p12', '~/.ssh/id_rsa', 'id_ed25519', '.ENV', '.Env.Local', 'C:/repo/.env::$DATA', 'ID_RSA'])
    assert.ok(isSecretFile(f), f);
  for (const f of ['.env.example', 'frontend/.env.example', 'env.md', 'README.md', 'id_rsa.pub', '.envrc.md', 'keys.json']) assert.ok(!isSecretFile(f), f);
});

test('비밀값 패턴: 기존 + github_pat_·웹훅 URL·GMS 키 할당', () => {
  const labels = (t) => findSecrets(t).map((s) => s.label);
  assert.deepEqual(labels(`token=${fake.ghp}`), ['GitHub 토큰']);
  assert.deepEqual(labels(fake.pat), ['GitHub 토큰(fine-grained)']);
  assert.deepEqual(labels(fake.gemini), ['Google(Gemini) API 키']);
  for (const url of [fake.mattermost, fake.slack, fake.discord]) assert.deepEqual(labels(`url: ${url}`), ['웹훅 URL'], url);
  assert.deepEqual(labels('GMS_API_KEY=' + 'S1'.repeat(12)), ['GMS 키']);
  assert.deepEqual(labels("gms_api_key: '" + 'k9'.repeat(10) + "'"), ['GMS 키']);
  // 빈 값·환경변수 참조·설명 글·자리표시자(숫자 없음)는 통과
  for (const t of [
    'GMS_API_KEY=',
    'GMS_API_KEY=${GMS_API_KEY}',
    '# GMS_API_KEY=<팀 키>',
    'GMS_API_KEY=your-gms-api-key-here',
    'const GMS_API_KEY = process.env.GMS_API_KEY',
    'github_pat_[A-Za-z0-9_]{22,}',
    'https://meeting.ssafy.com/hooks/',
    'secret: ${JWT_SECRET:local-dev-only-jwt-secret-do-not-use-in-production}',
  ])
    assert.deepEqual(labels(t), [], t);
});

test('staged diff 검사: 추가된 줄만, 파일 이름과 종류만 알린다', () => {
  const diff = [
    'diff --git a/a.yml b/a.yml',
    '--- a/a.yml',
    '+++ b/a.yml',
    '@@ -1 +1 @@',
    `-old: ${fake.ghp}`,
    `+new: ${fake.pat}`,
    'diff --git a/b.md b/b.md',
    '+++ b/b.md',
    '@@ -0,0 +1 @@',
    '+그냥 글',
  ].join('\n');
  assert.deepEqual(scanDiffForSecrets(diff), [{ file: 'a.yml', label: 'GitHub 토큰(fine-grained)' }]);
  assert.deepEqual(scanDiffForSecrets(''), []);
});

test('merge 커밋: 두 부모 모두에 없던 줄(충돌 해결)만 본다', () => {
  // develop에서 들어온 줄은 ours(HEAD) 대비로는 추가지만 theirs(MERGE_HEAD) 대비로는 추가가 아니다
  const ours = ['+++ b/a.yml', `+from-develop: ${fake.ghp}`, `+resolved: ${fake.pat}`].join('\n');
  const theirs = ['+++ b/a.yml', `+resolved: ${fake.pat}`].join('\n');
  assert.deepEqual(scanMergeForSecrets(ours, theirs), [{ file: 'a.yml', label: 'GitHub 토큰(fine-grained)' }]);
  assert.deepEqual(scanMergeForSecrets(ours, ''), []);
});

test('인라인 코드가 비밀 파일을 읽는가 (node -e, python -c, REPL 입력)', () => {
  for (const code of [
    "console.log(require('fs').readFileSync('.env','utf8'))",
    'print(open(".env.local").read())',
    "fs.readFileSync(path.join(root, 'backend', '.env'))",
    'import dotenv; print(dotenv.dotenv_values())',
    "require('dotenv').config()",
    "open(r'C:\\repo\\.env')",
    'cat(`./certs/server.pem`)',
  ])
    assert.ok(codeReadsSecret(code), code);
  for (const code of ["readFileSync('.env.example')", 'console.log(1)', "import('./scripts/lib/owners.mjs')", "const re = /\\.env$/"]) assert.ok(!codeReadsSecret(code), code);
});

test('경로 포함: 대소문자·구분자 차이를 무시하고, 형제 이름은 제외', () => {
  assert.ok(isWithin('/repo/data/raw/eval/a.json', '/repo/data/raw/eval'));
  assert.ok(isWithin('/repo/data/raw/eval', '/repo/data/raw/eval'));
  assert.ok(isWithin('C:\\Repo\\Data\\raw\\eval\\x', 'c:/repo/data/raw/eval', 'win32'));
  assert.ok(!isWithin('/repo/data/raw/eval-notes/a', '/repo/data/raw/eval'));
  assert.ok(!isWithin('/repo/data/raw', '/repo/data/raw/eval'));
});

test('eval 기록 브랜치 fetch 제외 refspec', () => {
  assert.equal(EVAL_FETCH_EXCLUDE, '^refs/heads/eval/*');
  assert.ok(evalFetchExcluded(['+refs/heads/*:refs/remotes/origin/*', '^refs/heads/eval/*']));
  assert.ok(!evalFetchExcluded(['+refs/heads/*:refs/remotes/origin/*']));
});
