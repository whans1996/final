#!/usr/bin/env node
// oxlint 실행 래퍼. `npm run lint`·`lint:check`가 oxlint 대신 이 파일을 부른다.
//
// Windows 11 스마트 앱 컨트롤(SAC)이 켜진 PC는 서명 없는 oxlint 네이티브 바이너리
// (@oxlint/binding-win32-x64-msvc)를 막는다 ("An Application Control policy has blocked this file").
// 파일 단위 예외가 없고 wasm 대체 패키지도 npm에 없어서 다음 순서로 실행한다.
//   1. 네이티브가 로드되면 그대로 실행 (CI·Linux·SAC 꺼진 PC)
//   2. 막혔으면 Docker(Linux 컨테이너)에서 같은 버전의 oxlint를 실행 (--fix도 마운트로 반영)
//   3. Docker도 없거나 꺼져 있으면 경고만 내고 건너뛴다. PR CI(Linux)가 oxlint를 강제한다
//
// 사용법: node scripts/run-oxlint.mjs [oxlint 인자...]   (예: . --fix)
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(resolve(frontendDir, 'package.json'))
const args = process.argv.slice(2)

/** SAC 같은 애플리케이션 제어 정책이 네이티브 바인딩을 막았는지 확인한다. 다른 오류는 oxlint가 직접 보고하게 둔다. */
function nativeBlocked() {
  if (process.platform !== 'win32') return false
  try {
    require(`@oxlint/binding-win32-${process.arch}-msvc`)
    return false
  } catch (e) {
    return /Application Control policy/i.test(String(e?.message))
  }
}

function run(command, commandArgs) {
  const result = spawnSync(command, commandArgs, { cwd: frontendDir, stdio: 'inherit' })
  if (result.error) throw result.error
  return result.status ?? 1
}

const oxlintPkgPath = require.resolve('oxlint/package.json')
const oxlintPkg = require(oxlintPkgPath)

if (!nativeBlocked()) {
  // oxlint의 exports에 './bin/oxlint'가 없어 require.resolve로 못 찾는다. package.json의 bin으로 경로를 만든다
  const bin = resolve(dirname(oxlintPkgPath), oxlintPkg.bin.oxlint)
  process.exit(run(process.execPath, [bin, ...args]))
}

const version = oxlintPkg.version
const docker = spawnSync('docker', ['info', '--format', '{{.ServerVersion}}'], {
  encoding: 'utf8',
})
if (docker.status !== 0) {
  console.warn(
    `⚠️  oxlint 건너뜀: 스마트 앱 컨트롤이 네이티브 바이너리를 막았고 Docker도 실행 중이 아닙니다.\n` +
      `   Docker Desktop을 켜면 컨테이너로 검사합니다. PR CI는 oxlint를 그대로 검사합니다.`,
  )
  process.exit(0)
}

console.warn(
  `ℹ️  스마트 앱 컨트롤이 oxlint 네이티브를 막아 Docker로 실행합니다 (oxlint@${version}).`,
)
// 마운트한 node_modules에는 Windows 바인딩만 있으므로 컨테이너 안 별도 폴더에 Linux용을 설치해 쓴다.
// npm 캐시는 이름 있는 볼륨에 둬서 두 번째부터는 내려받지 않는다.
// 저장소 루트를 마운트해야 루트 .gitignore(node_modules·dist 등)가 네이티브 실행과 똑같이 적용된다.
// 루트에는 .env(키)가 있으므로 노출을 줄인다: 설치 스크립트는 돌리지 않고(--ignore-scripts),
// --fix가 아니면 읽기 전용으로 마운트한다.
const repoDir = resolve(frontendDir, '..')
const writable = args.some((a) => a === '--fix' || a.startsWith('--fix-'))
const script =
  `npm install --prefix /opt/oxlint --no-save --no-audit --no-fund --ignore-scripts --loglevel=error oxlint@${version}` +
  ` && /opt/oxlint/node_modules/.bin/oxlint "$@"`
process.exit(
  run('docker', [
    'run',
    '--rm',
    '-v',
    `${repoDir}:/repo${writable ? '' : ':ro'}`,
    '-v',
    'vibenote-oxlint-npm:/root/.npm',
    '-w',
    '/repo/frontend',
    'node:24-alpine',
    'sh',
    '-c',
    script,
    'oxlint',
    ...args,
  ]),
)
