// 프로세스 사이 잠금 파일. 같은 PC에서 Gradle이 동시에 돌면(커밋 hook + pre-push + verify를 부른 에이전트)
// 같은 build 폴더를 두고 다투어 테스트 결과가 섞이거나 "Timeout waiting to lock" 으로 실패한다. verify.mjs가 BE 단계를 이 잠금으로 직렬화한다.
// verify.mjs를 거치지 않는 Gradle 실행(gradlew --tests 직접 호출, bootRun)은 이 잠금 밖이다.
import { closeSync, mkdirSync, openSync, readFileSync, rmSync, statSync, writeSync } from 'node:fs';
import { dirname } from 'node:path';

export const STALE_MS = 30 * 60_000;

function pidAlive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch (e) {
    return e.code === 'EPERM'; // 다른 사용자의 살아 있는 프로세스
  }
}

/** 잠금 정보가 버려진 것인가: 주인 프로세스가 죽었거나 STALE_MS를 넘겼거나 내용이 깨졌다 */
export function isStale(info, now = Date.now(), alive = pidAlive) {
  if (!info || !Number.isInteger(info.pid) || !Number.isFinite(info.at)) return true;
  return now - info.at > STALE_MS || !alive(info.pid);
}

function readInfo(path) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    return null;
  }
}

function justCreated(path) {
  try {
    return Date.now() - statSync(path).mtimeMs < 5000;
  } catch {
    return false;
  }
}

const sleep = (ms) => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);

/**
 * 잠금을 얻는다. 이미 있으면 풀릴 때까지 pollMs마다 기다리고, 처음 기다릴 때 onWait(주인 정보)를 한 번 부른다.
 * 버려진 잠금은 지우고 가져간다. timeoutMs를 넘기면 false. 얻으면 해제 함수를 돌려준다.
 */
export function acquireLock(path, { label = '', pollMs = 2000, timeoutMs = STALE_MS, onWait = () => {} } = {}) {
  mkdirSync(dirname(path), { recursive: true });
  const started = Date.now();
  let waited = false;
  for (;;) {
    try {
      const fd = openSync(path, 'wx');
      writeSync(fd, JSON.stringify({ pid: process.pid, at: Date.now(), label }));
      closeSync(fd);
      return () => {
        if (readInfo(path)?.pid === process.pid) rmSync(path, { force: true });
      };
    } catch (e) {
      if (e.code !== 'EEXIST') throw e;
    }
    const info = readInfo(path);
    // 막 만들어져 아직 내용을 쓰는 중인 잠금은 깨진 것으로 보지 않는다
    if (isStale(info) && !(info === null && justCreated(path))) {
      rmSync(path, { force: true });
      continue;
    }
    if (!waited) {
      onWait(info);
      waited = true;
    }
    if (Date.now() - started > timeoutMs) return false;
    sleep(pollMs);
  }
}
