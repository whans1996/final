// .env 위치 (#249). 비밀을 AI가 검색하는 저장소 폴더에 두지 않으려고 PC마다 저장소 밖 ~/.vibenote/.env에 둔다.
// BE는 application.yml spring.config.import로 루트 .env 다음에 ~/.vibenote/.env를 읽는다 (키 단위로 합쳐지고 같은 키는 뒤가 이김, EnvFileImportTest).
// 여기서는 파일이 있는지만 보고 내용은 읽지 않는다. 옮기기는 사람이 `node scripts/setup.mjs --move-env`로 한다.
import { chmodSync, copyFileSync, constants, existsSync, mkdirSync, renameSync, unlinkSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';
import { ROOT, isWin } from './common.mjs';

export const ENV_PATHS = { root: join(ROOT, '.env'), home: join(homedir(), '.vibenote', '.env') };

/** 환경 점검 한 줄. home·root는 파일이 있는지 */
export function envFileRow({ home, root }) {
  const item = '.env';
  if (home && !root) return { status: 'ok', item, detail: '~/.vibenote/.env 있음' };
  if (home) {
    return {
      status: 'warn',
      item,
      detail: '루트 .env가 남아 있음 → 필요한 줄을 ~/.vibenote/.env로 옮긴 뒤 루트 .env를 지움 (키 단위로 합쳐져 루트에만 있는 키가 계속 쓰임, #249)',
    };
  }
  if (root) return { status: 'warn', item, detail: '저장소 루트에 있음 → node scripts/setup.mjs --move-env 로 ~/.vibenote/.env로 옮김 (#249)' };
  return { status: 'warn', item, detail: '없음 → .env.example을 ~/.vibenote/.env로 복사해 채움 (bootRun에 필요)' };
}

export const envFileCheck = (paths = ENV_PATHS) => envFileRow({ home: existsSync(paths.home), root: existsSync(paths.root) });

/**
 * 루트 .env를 ~/.vibenote/.env로 옮긴다. 이미 있으면 덮어쓰지 않는다. 반환 { ok, message }.
 * 실패해도 예외로 끝내지 않고 사람이 할 일을 알린다 (파일이 편집기·백신에 잠김 등). fs는 테스트에서 바꿔 끼운다
 */
export function moveEnv(paths = ENV_PATHS, fs = {}) {
  const { renameSync: rename = renameSync, copyFileSync: copy = copyFileSync, unlinkSync: unlink = unlinkSync } = fs;
  if (!existsSync(paths.root)) {
    return existsSync(paths.home) ? { ok: true, message: `이미 옮겼습니다: ${paths.home}` } : { ok: false, message: `옮길 파일이 없습니다: ${paths.root}` };
  }
  if (existsSync(paths.home)) {
    return { ok: false, message: `${paths.home}가 이미 있어 덮어쓰지 않습니다. 필요한 줄을 직접 옮긴 뒤 루트 .env를 지우세요 (두 파일은 키 단위로 합쳐져 루트에만 있는 키가 계속 쓰입니다).` };
  }
  try {
    mkdirSync(dirname(paths.home), { recursive: true });
    try {
      rename(paths.root, paths.home);
    } catch (e) {
      if (e.code !== 'EXDEV') throw e;
      // 저장소와 홈이 다른 드라이브면 rename이 안 된다
      copy(paths.root, paths.home, constants.COPYFILE_EXCL);
      try {
        unlink(paths.root);
      } catch (u) {
        return { ok: false, message: `${paths.home}로 복사했지만 원본을 지우지 못했습니다 (${u.code}). ${paths.root}를 직접 지우세요.` };
      }
    }
  } catch (e) {
    return { ok: false, message: `옮기지 못했습니다 (${e.code || e.message}). 편집기 등에서 .env를 닫고 다시 실행하세요.` };
  }
  if (!isWin) chmodSync(paths.home, 0o600);
  return { ok: true, message: `옮겼습니다: ${paths.root} → ${paths.home}` };
}
