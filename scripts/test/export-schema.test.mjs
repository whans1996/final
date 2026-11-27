// HP-5 DB Schema.sql 추출 (HX-10): V*.sql을 임시 MySQL 컨테이너에 적용하고 mysqldump --no-data 정리본을 만든다.
// Docker 실행은 가짜 실행기로 주입한다 (실제 실측은 수동 1회, 작업로그에 기록).
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { cleanDump, exportSchema, orderMigrations, parseShowCreate, tableDiff } from '../lib/schema-export.mjs';

test('HP-5 AC9 V 파일은 버전 번호 순(V2 < V10)이고 반복 마이그레이션 R__는 넣지 않는다', () => {
  assert.deepEqual(orderMigrations(['V10__b.sql', 'R__seed_concepts.sql', 'V2__a.sql', 'V1__init.sql', 'V1_1__fix.sql', 'notes.md']), [
    'V1__init.sql',
    'V1_1__fix.sql',
    'V2__a.sql',
    'V10__b.sql',
  ]);
});

const DUMP = `-- MySQL dump 10.13  Distrib 8.4.3, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: vibenote
-- ------------------------------------------------------
-- Server version\t8.4.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

--
-- Table structure for table \`users\`
--

DROP TABLE IF EXISTS \`users\`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE \`users\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`email\` varchar(255) NOT NULL,
  PRIMARY KEY (\`id\`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE TABLE \`posts\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`user_id\` bigint NOT NULL COMMENT '작성자; 탈퇴해도 남는다',
  PRIMARY KEY (\`id\`),
  CONSTRAINT \`fk_posts_user\` FOREIGN KEY (\`user_id\`) REFERENCES \`users\` (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

-- Dump completed on 2026-11-27  3:30:01
`;

test('HP-5 AC10 덤프 정리: 머리 주석·DB 선언·외래 키 끄기 사이에 CREATE TABLE만, AUTO_INCREMENT·덤프 시각·세션 변수 줄은 지운다', () => {
  const sql = cleanDump(DUMP, { lastVersion: 'V14' });
  const lines = sql.split('\n');
  assert.match(lines[0], /^-- /);
  assert.match(sql, /node scripts\/export-schema\.mjs/);
  assert.match(sql, /손으로 고치지 않는다/);
  assert.match(sql, /반영한 마지막 버전: V14/);
  assert.match(sql, /CREATE DATABASE IF NOT EXISTS vibenote [^\n]*;\nUSE vibenote;/);
  const fkOff = sql.indexOf('SET FOREIGN_KEY_CHECKS=0;');
  const fkOn = sql.indexOf('SET FOREIGN_KEY_CHECKS=1;');
  assert.ok(fkOff > 0 && fkOn > fkOff);
  assert.ok(sql.indexOf('CREATE TABLE `users`') > fkOff && sql.indexOf('CREATE TABLE `posts`') < fkOn);
  assert.doesNotMatch(sql, /AUTO_INCREMENT=\d/);
  assert.match(sql, /`id` bigint NOT NULL AUTO_INCREMENT,/, '컬럼의 AUTO_INCREMENT는 남긴다');
  assert.doesNotMatch(sql, /Dump completed|Server version|\/\*!|DROP TABLE/);
  assert.match(sql, /COMMENT '작성자; 탈퇴해도 남는다',/, '문자열 안의 ;에서 자르지 않는다');
  assert.ok(sql.endsWith('SET FOREIGN_KEY_CHECKS=1;\n'));
  assert.equal(cleanDump(DUMP, { lastVersion: 'V14' }), sql, '같은 덤프면 같은 결과');
});

test('HP-5 AC11 SHOW CREATE TABLE 비교: AUTO_INCREMENT 값은 무시하고 다른 테이블·빠진 테이블 이름을 돌려준다', () => {
  const a = parseShowCreate('users\tCREATE TABLE `users` (\\n  `id` bigint\\n) ENGINE=InnoDB AUTO_INCREMENT=3\nposts\tCREATE TABLE `posts` (x)\nonly_a\tCREATE TABLE `only_a` (x)\n');
  const b = parseShowCreate('users\tCREATE TABLE `users` (\\n  `id` bigint\\n) ENGINE=InnoDB\nposts\tCREATE TABLE `posts` (y)\n');
  assert.equal(a.get('users'), 'CREATE TABLE `users` (\n  `id` bigint\n) ENGINE=InnoDB AUTO_INCREMENT=3');
  assert.deepEqual(tableDiff(a, b), ['only_a', 'posts']);
  assert.deepEqual(tableDiff(a, a), []);
});

/** 가짜 docker: 호출을 기록하고 args에 맞는 결과를 돌려준다 */
function fakeDocker(handlers = {}) {
  const calls = [];
  const run = (args, input) => {
    calls.push({ args, input });
    const line = args.join(' ');
    for (const [pattern, result] of Object.entries(handlers)) if (line.includes(pattern)) return typeof result === 'function' ? result(args, input) : result;
    return { ok: true, out: '', err: '' };
  };
  return { run, calls };
}

const migrations = [
  { name: 'V1__init.sql', sql: 'CREATE TABLE users (id BIGINT);' },
  { name: 'V3__bad.sql', sql: 'ALTER TABLE nope ADD x INT;' },
];
const opts = (docker) => ({ docker: docker.run, migrations, sleep: () => {}, readyTries: 3 });

test('HP-5 AC12 Docker가 꺼져 있으면 컨테이너를 띄우지 않고 원인을 알린다', () => {
  const docker = fakeDocker({ version: { ok: false, out: '', err: 'error during connect: open //./pipe/docker_engine' } });
  const r = exportSchema(opts(docker));
  assert.equal(r.ok, false);
  assert.match(r.error, /Docker가 꺼져 있/);
  assert.equal(docker.calls.some((c) => c.args[0] === 'run'), false);
});

test('HP-5 AC12 V 파일 적용이 실패하면 파일 이름과 MySQL 오류 첫 줄을 알리고 컨테이너는 지운다', () => {
  const docker = fakeDocker({
    exec: (args, input) =>
      input?.includes('nope') ? { ok: false, out: '', err: "ERROR 1146 (42S02) at line 1: Table 'vibenote.nope' doesn't exist\nmore" } : { ok: true, out: '1', err: '' },
  });
  const r = exportSchema(opts(docker));
  assert.equal(r.ok, false);
  assert.match(r.error, /V3__bad\.sql/);
  assert.match(r.error, /ERROR 1146 \(42S02\) at line 1: Table 'vibenote\.nope' doesn't exist$/);
  const last = docker.calls.at(-1).args;
  assert.deepEqual(last.slice(0, 3), ['rm', '-f', '-v'], '익명 볼륨(/var/lib/mysql)까지 지운다');
  // 비밀번호 없이, 외부 포트 없이 띄운다
  const run = docker.calls.find((c) => c.args[0] === 'run').args;
  assert.ok(run.includes('MYSQL_ALLOW_EMPTY_PASSWORD=yes'));
  assert.ok(run.includes('mysql:8.4'));
  assert.equal(run.some((a) => a === '-p' || a.startsWith('--publish') || /PASSWORD=(?!yes)/.test(a)), false);
});

test('HP-5 AC12 MySQL이 준비되지 않으면 정해진 횟수 뒤 실패하고 컨테이너를 지운다', () => {
  const docker = fakeDocker({ 'SELECT 1': { ok: false, out: '', err: "Can't connect" } });
  const r = exportSchema(opts(docker));
  assert.equal(r.ok, false);
  assert.match(r.error, /준비되지 않/);
  assert.equal(docker.calls.filter((c) => c.args.includes('SELECT 1')).length, 3);
  assert.deepEqual(docker.calls.at(-1).args.slice(0, 3), ['rm', '-f', '-v']);
});

const SHOW = 'users\tCREATE TABLE `users` (\\n  `id` bigint\\n) ENGINE=InnoDB\n';
const succeed = (checkShow) =>
  fakeDocker({
    mysqldump: { ok: true, out: 'CREATE TABLE `users` (\n  `id` bigint\n) ENGINE=InnoDB AUTO_INCREMENT=5;\n', err: '' },
    'SHOW TABLES': { ok: true, out: 'users\n', err: '' },
    'vibenote_check -e SHOW CREATE': { ok: true, out: checkShow, err: '' },
    'vibenote -e SHOW CREATE': { ok: true, out: SHOW, err: '' },
  });

test('HP-5 AC9·AC11 성공: V 파일을 순서대로 적용하고, 만든 Schema.sql을 빈 DB에 다시 적용해 같으면 SQL을 돌려준다', () => {
  const docker = succeed(SHOW);
  const r = exportSchema({ ...opts(docker), migrations: [migrations[0], { name: 'V2__b.sql', sql: 'ALTER TABLE users ADD y INT;' }] });
  assert.equal(r.ok, true, r.error);
  assert.match(r.sql, /반영한 마지막 버전: V2\b/);
  const applied = docker.calls.filter((c) => c.input !== undefined).map((c) => c.input);
  assert.equal(applied[0], migrations[0].sql);
  assert.match(applied[1], /ADD y INT/);
  // 검증 DB에는 DB 선언·USE 없이 테이블만 적용한다
  assert.match(applied[2], /CREATE TABLE `users`/);
  assert.doesNotMatch(applied[2], /USE vibenote|CREATE DATABASE/);
  // mysql·mysqldump 클라이언트는 utf8mb4로 붙는다 (빠지면 한글 COMMENT가 이중 인코딩된다, 11-27 실측)
  const clients = docker.calls.filter((c) => c.args[0] === 'exec');
  assert.ok(clients.length && clients.every((c) => c.args.includes('--default-character-set=utf8mb4')));
  assert.deepEqual(docker.calls.at(-1).args.slice(0, 3), ['rm', '-f', '-v']);
});

test('HP-5 AC11 다시 적용한 DB와 SHOW CREATE TABLE이 다르면 테이블 이름과 함께 실패한다', () => {
  const docker = succeed('users\tCREATE TABLE `users` (\\n  `id` int\\n) ENGINE=InnoDB\n');
  const r = exportSchema({ ...opts(docker), migrations: [migrations[0]] });
  assert.equal(r.ok, false);
  assert.match(r.error, /users/);
  assert.deepEqual(docker.calls.at(-1).args.slice(0, 3), ['rm', '-f', '-v']);
});

test('HP-5 AC10 트리거·뷰·루틴·이벤트가 있으면 Schema.sql에 빠지므로 이름과 함께 실패한다', () => {
  const docker = succeed(SHOW);
  const run = docker.run;
  const wrapped = { ...docker, run: (args, input) => (args.join(' ').includes('information_schema') ? { ok: true, out: 'TRIGGER\ttrg_users\nVIEW\tv_stats\n', err: '' } : run(args, input)) };
  const r = exportSchema({ ...opts(wrapped), migrations: [migrations[0]] });
  assert.equal(r.ok, false);
  assert.match(r.error, /지원하지 않는 객체/);
  assert.match(r.error, /TRIGGER trg_users/);
  assert.match(r.error, /VIEW v_stats/);
  assert.deepEqual(docker.calls.at(-1).args.slice(0, 3), ['rm', '-f', '-v']);
});
