// 제출물 DB Schema.sql 추출 (HP-5 HX-10). 정본 Flyway V*.sql을 테스트와 같은 mysql:8.4 임시 컨테이너에 적용하고
// mysqldump --no-data 결과를 정리한다. 만든 파일을 빈 DB에 다시 적용해 SHOW CREATE TABLE이 같아야 성공이다.
// docker 실행기(docker(args, input) → { ok, out, err })를 주입받아 테스트는 가짜 실행기로 돈다. 파일 쓰기는 scripts/export-schema.mjs.
import { compareVersions, versionOf, versionText } from './migrations.mjs';

export const IMAGE = 'mysql:8.4';
const DB = 'vibenote';
const CHECK_DB = 'vibenote_check';
const CHARSET = 'DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci';
// IntegrationTestBase와 같은 서버 설정
const SERVER_FLAGS = ['--character-set-server=utf8mb4', '--collation-server=utf8mb4_0900_ai_ci', '--default-time-zone=+00:00'];
// 클라이언트 문자셋을 정하지 않으면 docker exec(TTY 없음)의 mysql이 latin1로 보내 한글 COMMENT가 이중 인코딩된다 (11-27 실측)
const CLIENT = ['-h127.0.0.1', '-uroot', '--default-character-set=utf8mb4'];
const MYSQL = ['mysql', ...CLIENT];

/** V 파일만 버전 번호 순으로 (V2 < V10). R__ 반복 마이그레이션(시드)과 다른 파일은 뺀다 */
export function orderMigrations(names) {
  return names.filter((n) => versionOf(n)).sort((a, b) => compareVersions(versionOf(a), versionOf(b)));
}

const stripAutoIncrement = (s) => s.replace(/ AUTO_INCREMENT=\d+/g, '');

/**
 * mysqldump 결과 → Schema.sql 전문. 테이블별 CREATE TABLE만 남기고(닫는 줄 ") …;"까지) 그 밖의 주석·세션 변수·DROP 줄은 버린다.
 * 테이블 옵션의 AUTO_INCREMENT=<n>은 지운다 (데이터에 따라 달라진다). 같은 덤프면 같은 결과다.
 */
export function cleanDump(dump, { lastVersion }) {
  const tables = (String(dump).replace(/\r\n/g, '\n').match(/^CREATE TABLE [\s\S]*?^\)[^\n]*;$/gm) ?? []).map((t) => {
    const lines = t.split('\n');
    lines[lines.length - 1] = stripAutoIncrement(lines.at(-1));
    return lines.join('\n');
  });
  return [
    '-- 바이브노트 DB 스키마 (MySQL 8.4)',
    '-- 자동 생성 파일이다. 손으로 고치지 않는다. 다시 만들기: node scripts/export-schema.mjs (Docker 필요, 주간 /progress와 제출 전)',
    `-- 정본: backend/src/main/resources/db/migration/V*.sql (Flyway). 반영한 마지막 버전: ${lastVersion}`,
    '-- 시드 데이터(R__seed_concepts.sql)는 넣지 않는다. 데이터 셋은 docs/산출물/09-데이터셋.md',
    '',
    `CREATE DATABASE IF NOT EXISTS ${DB} ${CHARSET};`,
    `USE ${DB};`,
    '',
    'SET FOREIGN_KEY_CHECKS=0;',
    '',
    ...tables.flatMap((t) => [t, '']),
    'SET FOREIGN_KEY_CHECKS=1;',
    '',
  ].join('\n');
}

/** Schema.sql에서 DB 선언·USE를 뺀 것 (검증 DB에 적용할 때) */
const tablesOnly = (sql) => sql.replace(/^(CREATE DATABASE|USE) [^\n]*\n/gm, '');

/** mysql -N -B "SHOW CREATE TABLE …" 출력(이름\t문장, \n 이스케이프) → Map<테이블, CREATE 문> */
export function parseShowCreate(out) {
  const unescape = (s) => s.replace(/\\(.)/g, (_, c) => ({ n: '\n', t: '\t', 0: '\0' })[c] ?? c);
  const map = new Map();
  for (const line of String(out).split(/\r?\n/)) {
    const i = line.indexOf('\t');
    if (i > 0) map.set(line.slice(0, i), unescape(line.slice(i + 1)));
  }
  return map;
}

/** 두 DB에서 다르거나 한쪽에만 있는 테이블 이름 (이름 순, AUTO_INCREMENT 값은 무시) */
export function tableDiff(a, b) {
  const names = [...new Set([...a.keys(), ...b.keys()])].sort();
  return names.filter((n) => !a.has(n) || !b.has(n) || stripAutoIncrement(a.get(n)) !== stripAutoIncrement(b.get(n)));
}

const firstLine = (s) => String(s || '').split(/\r?\n/).map((l) => l.trim()).find(Boolean) ?? '(메시지 없음)';
const defaultSleep = (ms) => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);

/**
 * migrations: [{ name, sql }] (아무 순서). → { ok: true, sql, lastVersion } | { ok: false, error }.
 * 컨테이너는 빈 root 비밀번호로 외부 포트 없이 띄우고, 성공·실패와 관계없이 지운다.
 */
export function exportSchema({ docker, migrations, sleep = defaultSleep, readyTries = 60, name = `vibenote-schema-${process.pid}` }) {
  if (!docker(['version', '--format', '{{.Server.Version}}']).ok) return { ok: false, error: 'Docker가 꺼져 있습니다. Docker Desktop을 켜고 다시 실행하세요' };
  const byName = new Map(migrations.map((m) => [m.name, m.sql]));
  const ordered = orderMigrations([...byName.keys()]);
  if (!ordered.length) return { ok: false, error: 'V*.sql 파일이 없습니다' };
  const lastVersion = `V${versionText(versionOf(ordered.at(-1)))}`;

  const started = docker(['run', '-d', '--name', name, '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', IMAGE, ...SERVER_FLAGS]);
  if (!started.ok) return { ok: false, error: `${IMAGE} 컨테이너를 띄우지 못했습니다: ${firstLine(started.err)}` };
  try {
    const sqlIn = (db, input) => docker(['exec', '-i', name, ...MYSQL, db], input);
    const query = (db, q) => docker(['exec', name, ...MYSQL, '-N', '-B', ...(db ? [db] : []), '-e', q]);
    // 초기화 중 임시 서버는 TCP를 열지 않으므로 127.0.0.1로 붙으면 최종 서버가 뜬 것이다
    let ready = false;
    for (let i = 0; i < readyTries && !ready; i++) {
      ready = query(null, 'SELECT 1').ok;
      if (!ready) sleep(2000);
    }
    if (!ready) return { ok: false, error: `MySQL이 준비되지 않았습니다 (${readyTries}회 확인)` };
    const created = query(null, `CREATE DATABASE ${DB} ${CHARSET}; CREATE DATABASE ${CHECK_DB} ${CHARSET};`);
    if (!created.ok) return { ok: false, error: `DB 생성 실패: ${firstLine(created.err)}` };

    for (const file of ordered) {
      const r = sqlIn(DB, byName.get(file));
      if (!r.ok) return { ok: false, error: `${file} 적용 실패: ${firstLine(r.err)}` };
    }
    const dump = docker(['exec', name, 'mysqldump', ...CLIENT, '--no-data', '--skip-comments', '--skip-dump-date', DB]);
    if (!dump.ok) return { ok: false, error: `mysqldump 실패: ${firstLine(dump.err)}` };
    const sql = cleanDump(dump.out, { lastVersion });

    const reapplied = sqlIn(CHECK_DB, tablesOnly(sql));
    if (!reapplied.ok) return { ok: false, error: `만든 Schema.sql을 빈 DB에 적용하지 못했습니다: ${firstLine(reapplied.err)}` };
    const showAll = (db) => {
      const tables = query(db, 'SHOW TABLES');
      const names = tables.ok ? tables.out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean) : [];
      return names.length ? parseShowCreate(query(db, names.map((t) => `SHOW CREATE TABLE \`${t}\`;`).join(' ')).out) : new Map();
    };
    const original = showAll(DB);
    if (!original.size) return { ok: false, error: 'V 파일로 만든 DB에 테이블이 없습니다' };
    const diff = tableDiff(original, showAll(CHECK_DB));
    if (diff.length) return { ok: false, error: `만든 Schema.sql로 만든 DB가 V 파일로 만든 DB와 다릅니다: ${diff.join(', ')}` };
    return { ok: true, sql, lastVersion, tables: original.size };
  } finally {
    docker(['rm', '-f', name]);
  }
}
