// HP-5 산출물 07 REST API 명세 생성 (HX-10): docs/api/openapi.json → docs/산출물/07-REST-API-명세.md
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { ROOT } from '../lib/common.mjs';
import { accessOf, groupByTag, renderApiDoc, typeOf } from '../lib/api-doc.mjs';

const err = (d) => ({ description: d, content: { 'application/json': { schema: { $ref: '#/components/schemas/ErrorResponse' } } } });
const API = {
  openapi: '3.1.0',
  info: { title: 'VibeNote API', version: 'v1' },
  security: [{ bearerAuth: [] }],
  tags: [{ name: '인증' }, { name: '관리자' }],
  paths: {
    '/api/admin/items/{id}': {
      delete: { tags: ['관리자'], summary: '항목 삭제', parameters: [{ name: 'id', in: 'path', required: true, description: '항목 ID', example: 7, schema: { type: 'integer', format: 'int64' } }], responses: { 204: { description: '삭제 성공' }, 403: err('ADMIN이 아님 (AUTH_403)') } },
      get: { tags: ['관리자'], summary: '항목 상세', responses: { 200: { description: '성공', content: { 'application/json': { schema: { $ref: '#/components/schemas/Item' } } } }, 403: err('ADMIN이 아님 (AUTH_403)') } },
    },
    '/api/admin/items': {
      get: {
        tags: ['관리자'],
        summary: '항목 목록',
        parameters: [{ name: 'level', in: 'query', required: false, description: '수준', schema: { type: 'string', enum: ['BEGINNER', 'JUNIOR'], default: 'JUNIOR' } }],
        responses: { 200: { description: '성공', content: { 'application/json': { schema: { type: 'array', items: { $ref: '#/components/schemas/Item' } } } } } },
      },
    },
    '/api/auth/login': {
      post: {
        tags: ['인증'],
        summary: '로그인',
        description: '이메일과 비밀번호로 로그인한다.',
        security: [],
        requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/LoginRequest' } } } },
        responses: { 200: { description: '로그인 성공' }, 401: err('비밀번호 틀림 (AUTH_401)') },
      },
    },
    '/api/me': {
      post: {
        tags: ['회원'],
        summary: '내 정보 업로드',
        requestBody: { content: { 'multipart/form-data': { schema: { type: 'object', properties: { file: { type: 'string', format: 'binary', description: 'ZIP' } } } } } },
        responses: { 403: err('본인이 아님 (USER_403)'), 200: { description: '성공', content: { 'application/json': { schema: { oneOf: [{ type: 'string' }, { type: 'integer' }] } } } } },
      },
    },
  },
  components: {
    schemas: {
      LoginRequest: { type: 'object', required: ['email'], properties: { email: { type: 'string', description: '이메일', example: 'a@b.c', maxLength: 100 }, password: { type: 'string', minLength: 8 } } },
      Item: { type: 'object', description: '항목', properties: { tags: { type: 'array', items: { type: 'string' } }, owner: { $ref: '#/components/schemas/LoginRequest' }, meta: { type: 'object', additionalProperties: { type: 'string' } } } },
      Mixed: { oneOf: [{ $ref: '#/components/schemas/Item' }] },
    },
  },
};

test('HP-5 AC6 타입 표기: $ref는 부록 링크, 배열 T[], enum 값, map, format, 복합 스키마', () => {
  assert.equal(typeOf({ $ref: '#/components/schemas/Item' }), '[Item](#item)');
  assert.equal(typeOf({ type: 'array', items: { $ref: '#/components/schemas/Item' } }), '[Item](#item)[]');
  assert.equal(typeOf({ type: 'string', enum: ['A', 'B'] }), 'string: A · B');
  assert.equal(typeOf({ type: 'integer', format: 'int64' }), 'integer(int64)');
  assert.equal(typeOf({ type: 'object', additionalProperties: { type: 'string' } }), 'map<string, string>');
  assert.equal(typeOf({ oneOf: [{ type: 'string' }] }), '(복합 스키마)');
});

test('HP-5 AC6 openapi tags 순서로 묶고 Tag 안은 경로 → 메서드 순, 목록에 없는 Tag는 뒤에', () => {
  const groups = groupByTag(API);
  assert.deepEqual(groups.map((g) => g.tag), ['인증', '관리자', '회원']);
  assert.deepEqual(groups[1].ops.map((o) => `${o.method} ${o.path}`), ['get /api/admin/items', 'get /api/admin/items/{id}', 'delete /api/admin/items/{id}']);
});

test('HP-5 AC7 권한: security []면 공개, 403 설명에 ADMIN이면 ADMIN, 그 밖은 로그인', () => {
  assert.equal(accessOf(API.paths['/api/auth/login'].post), '공개');
  assert.equal(accessOf(API.paths['/api/admin/items/{id}'].delete), 'ADMIN');
  assert.equal(accessOf(API.paths['/api/me'].post), '로그인');
  // 403이 없는 관리자 경로도 로그인으로 본다 (판정은 응답 설명으로만)
  assert.equal(accessOf(API.paths['/api/admin/items'].get), '로그인');
});

test('HP-5 AC6·AC7 문서 구성: 머리말 → 공통 규칙 → 목록 → 상세 → 스키마 부록, 에러 코드는 응답 표에 그대로', () => {
  const md = renderApiDoc(API);
  const order = ['# REST API 명세', '손으로 고치지 않는다', 'node scripts/export-api-doc.mjs', '## 공통 규칙', '## 엔드포인트 목록', '## 엔드포인트 상세', '## 스키마 부록'].map((s) => md.indexOf(s));
  assert.ok(order.every((i) => i >= 0), String(order));
  assert.deepEqual([...order].sort((a, b) => a - b), order);
  assert.match(md, /VibeNote API v1 · 엔드포인트 5개 · 스키마 3개/);
  assert.match(md, /응답·에러 형식, 에러 코드 체계, 페이지네이션, 인증 헤더/);
  assert.match(md, /\| `POST` \| \/api\/auth\/login \| 로그인 \| 공개 \|/);
  assert.match(md, /\| `DELETE` \| \/api\/admin\/items\/\{id\} \| 항목 삭제 \| ADMIN \|/);
  // 상세: 설명, 파라미터 표, 요청 본문, 응답 표 (상태 순)
  assert.match(md, /이메일과 비밀번호로 로그인한다\./);
  assert.match(md, /\| `id` \| path \| 필수 \| integer\(int64\) \| 항목 ID \| `7` \|/);
  assert.match(md, /\| `level` \| query \| 선택 \| string: BEGINNER · JUNIOR \| 수준 \(기본 `JUNIOR`\) \| - \|/);
  assert.match(md, /\*\*요청 본문\*\* \(`application\/json`, 필수\): \[LoginRequest\]\(#loginrequest\)/);
  assert.match(md, /\*\*요청 본문\*\* \(`multipart\/form-data`, 선택\): object\n\n\| 이름 .*\n.*\n\| `file` \| string\(binary\) \| 선택 \| ZIP \| - \|/);
  assert.match(md, /\| 401 \| 비밀번호 틀림 \(AUTH_401\) \| \[ErrorResponse\]\(#errorresponse\) \|/);
  assert.match(md, /\| 200 \| 성공 \| \[Item\]\(#item\)\[\] \|/);
  assert.ok(md.indexOf('| 200 | 성공 | (복합 스키마) |') < md.indexOf('| 403 | 본인이 아님 (USER_403)'));
  // 스키마 부록: 이름 순, 필드 표, 필수·제약, 복합 스키마
  assert.ok(md.indexOf('### Item') < md.indexOf('### LoginRequest'));
  assert.match(md, /\| `email` \| string \| 필수 \| 이메일 \(길이 ~100자\) \| `a@b.c` \|/);
  assert.match(md, /\| `password` \| string \| 선택 \| \(길이 8~자\) \| - \|/);
  assert.match(md, /\| `owner` \| \[LoginRequest\]\(#loginrequest\) \| 선택 \|/);
  assert.match(md, /\| `meta` \| map<string, string> \|/);
  assert.match(md, /### Mixed\n\n타입: \(복합 스키마\)/);
});

test('HP-5 AC8 같은 openapi로 두 번 렌더하면 같고, 표 칸의 파이프·줄바꿈은 깨지지 않는다', () => {
  assert.equal(renderApiDoc(API), renderApiDoc(structuredClone(API)));
  const api = structuredClone(API);
  api.paths['/api/auth/login'].post.summary = 'a|b\nc';
  assert.match(renderApiDoc(api), /\| a\\\|b<br>c \| 공개 \|/);
});

const cli = (...args) => spawnSync(process.execPath, [join(ROOT, 'scripts/export-api-doc.mjs'), ...args], { encoding: 'utf8' });

test('HP-5 AC8 CLI: 생성 → 다시 실행해도 바이트 단위로 같다, 없거나 깨진 openapi는 파일을 바꾸지 않고 exit 1', () => {
  const dir = mkdtempSync(join(tmpdir(), 'export-api-doc-'));
  try {
    const input = join(dir, 'openapi.json');
    const out = join(dir, '07.md');
    writeFileSync(input, JSON.stringify(API));
    const first = cli('--in', input, '--out', out);
    assert.equal(first.status, 0, first.stderr);
    const once = readFileSync(out);
    assert.equal(cli('--in', input, '--out', out).status, 0);
    assert.ok(readFileSync(out).equals(once));

    assert.equal(cli('--in', join(dir, 'none.json'), '--out', out).status, 1);
    writeFileSync(input, '{ not json');
    assert.equal(cli('--in', input, '--out', out).status, 1);
    assert.ok(readFileSync(out).equals(once));
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
