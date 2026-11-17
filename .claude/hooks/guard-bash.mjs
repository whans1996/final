#!/usr/bin/env node
// PreToolUse(Bash|PowerShell) hook: 되돌리기 어려운 git 조작과 비밀 파일 접근을 막는다.
// 차단 시 exit 2 + stderr 메시지 → Claude에게 사유가 전달된다.
import { existsSync } from 'node:fs';
import { PROTECTED_BRANCHES, currentBranch, hasCommits, isMerging, readStdinJson } from '../../scripts/lib/common.mjs';

const input = readStdinJson();
const command = String(input?.tool_input?.command || '');
if (!command) process.exit(0);
// "cd <경로> && git commit ..." 이면 그 경로 기준으로 브랜치를 판단한다
const cdMatch = /^\s*cd\s+(?:"([^"]+)"|'([^']+)'|(\S+))\s*(&&|;)/.exec(command);
const cdTarget = cdMatch ? cdMatch[1] || cdMatch[2] || cdMatch[3] : null;
const cwd = cdTarget && existsSync(cdTarget) ? cdTarget : input.cwd || process.cwd();

// 따옴표 안(커밋 메시지 등)의 내용은 검사에서 제외해 오탐을 줄인다
const bare = command.replace(/"(?:\\.|[^"\\])*"|'[^']*'/g, '""');

const block = (reason) => {
  console.error(`🚫 하네스 가드가 명령을 차단했습니다: ${reason}`);
  process.exit(2);
};

const ENV_FILE = /(^|[\s/\\"'=])\.env(\.(?!example\b)[\w.-]+)?(?=$|[\s"';|&)])/;

const rules = [
  [/--no-verify\b/, 'git hook 우회(--no-verify)는 금지입니다. 테스트가 실패하면 고치거나, 교대용이면 wip(F-xx): 로 커밋하세요.'],
  [/\bgit\s+commit\b[^;&|]*\s-[a-zA-Z]*n[a-zA-Z]*\b/, 'git commit -n 은 --no-verify와 같습니다. 금지입니다.'],
  [/\bgit\s+push\b[^;&|]*(\s--force(-with-lease)?\b|\s-[a-zA-Z]*f\b|\s\+\S)/, '강제 push는 금지입니다. 릴레이 중인 상대의 커밋이 사라질 수 있습니다.'],
  [/\bgit\s+reset\b[^;&|]*--hard\b/, 'git reset --hard는 금지입니다. 미커밋 작업이 사라집니다. 필요하면 사용자에게 직접 실행을 요청하세요.'],
  [/\bgit\s+clean\b[^;&|]*\s-[a-zA-Z]*f/, 'git clean -f는 금지입니다. 추적되지 않은 파일이 영구 삭제됩니다.'],
  [/\bgit\s+(checkout|restore)\s+(--\s+)?\.(\s|$)/, '작업 트리 전체 되돌리기는 금지입니다. 파일을 지정하거나 사용자에게 확인하세요.'],
  [/\bgit\s+add\b[^;&|]*\s(-f|--force)\b/, 'git add --force는 금지입니다. .gitignore된 비밀 파일이 커밋될 수 있습니다.'],
  // 값 설정(`core.hooksPath x`, `-c core.hooksPath=x`)과 해제만 막고, 조회(`git config core.hooksPath`)는 허용한다
  [
    /--unset(-all)?\s+core\.hooksPath\b|\bcore\.hooksPath(\s*=\s*|\s+)(?!\.githooks\b)[^\s;&|)]/,
    'git hook 경로 변경은 금지입니다.',
  ],
  [/\bbranch\s+-D\s+(main|develop)\b/, '보호 브랜치 삭제는 금지입니다.'],
  // 최종 제출 저장소(lab.ssafy.com GitLab)는 사람이 직접 push한다
  [
    /\bgit\s+push\b(\s+-\S+)*\s+gitlab(\s|$)|\bgit\s+push\b[^;&|]*lab\.ssafy\.com/i,
    'GitLab(lab.ssafy.com)은 최종 제출 전용이며 push는 사람이 직접 합니다. 개발 push는 origin(GitHub)으로 하세요.',
  ],
  // GitHub: 머지와 저장소 설정 변경은 사람이 CI를 확인하고 한다 (무료 private 플랜은 브랜치 보호가 없음)
  [/\bgh\s+pr\s+merge\b/, 'PR 머지는 영역 오너가 CI 초록을 확인하고 GitHub에서 직접 합니다.'],
  [/\bgh\s+repo\s+(delete|edit|rename|archive)\b/, '저장소 설정 변경·삭제는 사람이 직접 합니다.'],
  [/\bgh\s+(release|secret|variable)\s+(delete|remove|set)\b/, '릴리스·시크릿·변수 변경은 사람이 직접 합니다.'],
];
for (const [re, reason] of rules) if (re.test(bare)) block(reason);

// 비밀 파일 읽기·커밋 (.env.example은 허용)
if (/\b(cat|type|more|less|head|tail|gc|Get-Content|Select-String|grep|rg|sed|awk|code|notepad)\b/i.test(bare) && ENV_FILE.test(command)) {
  block('.env 파일 내용을 읽을 수 없습니다. 필요한 변수 이름은 .env.example을 보세요.');
}
if (/\bgit\s+add\b/.test(bare) && ENV_FILE.test(command)) block('.env 파일은 커밋할 수 없습니다.');

// 보호 브랜치 직접 커밋 (첫 커밋, merge 진행 중은 예외)
if (/\bgit\s+(-C\s+\S+\s+)?commit\b/.test(bare)) {
  const branch = currentBranch(cwd);
  if (branch && PROTECTED_BRANCHES.includes(branch) && hasCommits(cwd) && !isMerging(cwd)) {
    block(`보호 브랜치(${branch})에 직접 커밋할 수 없습니다. git switch -c feature/F-xx-slug 로 브랜치를 만드세요.`);
  }
}

process.exit(0);
