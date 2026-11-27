// 산출물 07 REST API 명세 생성 (HP-5 HX-10). 정본 docs/api/openapi.json을 Markdown으로 옮긴다.
// 순수 함수다: 같은 openapi면 바이트 단위로 같은 결과 (시각·실행 환경을 쓰지 않는다). 파일 읽기·쓰기는 scripts/export-api-doc.mjs.

const METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const COMPOSITE = '(복합 스키마)';
const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

/** 표 칸: 파이프를 이스케이프하고 줄바꿈은 <br>. 비면 - */
export const cell = (v) => {
  const s = String(v ?? '').trim();
  return s ? s.replace(/\|/g, '\\|').replace(/\r?\n/g, '<br>') : '-';
};
const row = (cols) => `| ${cols.map(cell).join(' | ')} |`;
const table = (head, rows) => [row(head), `|${head.map(() => '---').join('|')}|`, ...rows.map(row)].join('\n');
const refName = (ref) => String(ref).split('/').at(-1);
const example = (v) => (v === undefined ? '' : `\`${typeof v === 'string' ? v : JSON.stringify(v)}\``);

/** 스키마 한 줄 표기: $ref는 부록 링크, 배열은 T[], enum은 값 목록, oneOf·anyOf·allOf는 (복합 스키마) */
export function typeOf(s) {
  if (!s || typeof s !== 'object') return 'any';
  if (s.oneOf || s.anyOf || s.allOf) return COMPOSITE;
  if (s.$ref) return `[${refName(s.$ref)}](#${refName(s.$ref).toLowerCase()})`;
  // openapi 3.1의 null 허용 표기 type: ['string', 'null']
  if (Array.isArray(s.type)) {
    const types = s.type.filter((t) => t !== 'null');
    const nullable = types.length < s.type.length ? ' · null 가능' : '';
    const single = types.length === 1 ? typeOf({ ...s, type: types[0], enum: s.enum?.filter((v) => v !== null) }) : types.join(' · ') || 'any';
    return `${single}${nullable}`;
  }
  if (s.type === 'array') return `${typeOf(s.items)}[]`;
  if (s.type === 'object' && s.additionalProperties && typeof s.additionalProperties === 'object') return `map<string, ${typeOf(s.additionalProperties)}>`;
  const base = s.format ? `${s.type ?? 'any'}(${s.format})` : (s.type ?? 'any');
  return s.enum?.length ? `${base}: ${s.enum.join(' · ')}` : base;
}

/** 설명 + 제약 (기본값·길이·범위·항목 수) */
function describe(s = {}, description = s.description) {
  const range = (lo, hi, unit = '') => (lo === undefined && hi === undefined ? null : `${lo ?? ''}~${hi ?? ''}${unit}`);
  const notes = [
    s.default !== undefined ? `기본 ${example(s.default)}` : null,
    range(s.minLength, s.maxLength, '자') && `길이 ${range(s.minLength, s.maxLength, '자')}`,
    range(s.minimum, s.maximum) && `범위 ${range(s.minimum, s.maximum)}`,
    range(s.minItems, s.maxItems, '개') && `항목 ${range(s.minItems, s.maxItems, '개')}`,
  ].filter(Boolean);
  return [description, notes.length ? `(${notes.join(', ')})` : ''].filter(Boolean).join(' ');
}

/** object 스키마의 필드 표 (이름·타입·필수·설명·예). 필드가 없으면 null */
function fieldTable(s) {
  const props = Object.entries(s?.properties ?? {});
  if (!props.length) return null;
  const required = new Set(s.required ?? []);
  return table(
    ['이름', '타입', '필수', '설명', '예'],
    props.map(([name, p]) => [`\`${name}\``, typeOf(p), required.has(name) ? '필수' : '선택', describe(p, p.description), example(p.example)]),
  );
}

/** 권한: security: []면 공개, 403 설명에 ADMIN이 있으면 ADMIN, 그 밖은 로그인 (HP-5 AC7) */
export function accessOf(op) {
  if (Array.isArray(op.security) && op.security.length === 0) return '공개';
  return /ADMIN/.test(op.responses?.['403']?.description ?? '') ? 'ADMIN' : '로그인';
}

/** Tag별 엔드포인트 [{ tag, ops: [{ method, path, op }] }]. openapi tags 순서, 목록에 없는 Tag는 이름 순으로 뒤에. Tag 안은 경로 → 메서드 순 */
export function groupByTag(api) {
  const groups = new Map((api.tags ?? []).map((t) => [t.name, []]));
  const extra = new Map();
  for (const [path, item] of Object.entries(api.paths ?? {})) {
    for (const method of METHODS) {
      const op = item?.[method];
      if (!op) continue;
      const tag = op.tags?.[0] ?? '(태그 없음)';
      const into = groups.has(tag) ? groups : extra;
      if (!into.has(tag)) into.set(tag, []);
      into.get(tag).push({ method, path, op });
    }
  }
  const sorted = [...extra.keys()].sort(cmp).map((k) => [k, extra.get(k)]);
  return [...groups, ...sorted]
    .filter(([, ops]) => ops.length)
    .map(([tag, ops]) => ({ tag, ops: ops.sort((a, b) => cmp(a.path, b.path) || METHODS.indexOf(a.method) - METHODS.indexOf(b.method)) }));
}

const contentLine = (content) =>
  Object.entries(content ?? {})
    .map(([type, c]) => `${typeOf(c.schema)}${type === 'application/json' ? '' : ` (\`${type}\`)`}`)
    .join(', ');

function operationSection({ method, path, op }) {
  const out = [`#### \`${method.toUpperCase()}\` ${path}`, '', `**${op.summary || '(요약 없음)'}** · 권한: ${accessOf(op)}`];
  if (op.description) out.push('', op.description);
  if (op.parameters?.length) {
    out.push(
      '',
      '**파라미터**',
      '',
      table(
        ['이름', '위치', '필수', '타입', '설명', '예'],
        op.parameters.map((p) => [`\`${p.name}\``, p.in, p.required ? '필수' : '선택', typeOf(p.schema), describe(p.schema, p.description ?? p.schema?.description), example(p.example ?? p.schema?.example)]),
      ),
    );
  }
  if (op.requestBody) {
    const [type, c] = Object.entries(op.requestBody.content ?? {})[0] ?? [];
    out.push('', `**요청 본문** (\`${type ?? '?'}\`, ${op.requestBody.required ? '필수' : '선택'}): ${typeOf(c?.schema)}`);
    const inline = c?.schema && !c.schema.$ref ? fieldTable(c.schema) : null;
    if (inline) out.push('', inline);
  }
  const responses = Object.entries(op.responses ?? {}).sort(([a], [b]) => cmp(a, b));
  out.push('', '**응답**', '', table(['상태', '설명', '스키마'], responses.map(([status, r]) => [status, r.description, contentLine(r.content)])));
  return out.join('\n');
}

function schemaSection(name, s) {
  const out = [`### ${name}`];
  if (s.description) out.push('', s.description);
  const fields = s.oneOf || s.anyOf || s.allOf ? null : fieldTable(s);
  out.push('', fields ?? `타입: ${typeOf(s)}`);
  return out.join('\n');
}

/** 산출물 07 전문 (HP-5 AC6~8) */
export function renderApiDoc(api) {
  const groups = groupByTag(api);
  const count = groups.reduce((n, g) => n + g.ops.length, 0);
  const schemas = Object.entries(api.components?.schemas ?? {}).sort(([a], [b]) => cmp(a, b));
  const parts = [
    '# REST API 명세 — 바이브노트',
    '',
    '> **자동 생성 문서다. 손으로 고치지 않는다.** 다시 만들기: `node scripts/export-api-doc.mjs` (주간 `/progress`와 제출 전).',
    '> **정본은 Swagger**다. 실행 중이면 http://localhost:8080/swagger-ui/index.html, 파일은 [`docs/api/openapi.json`](../api/openapi.json) (테스트 `OpenApiExportTest`가 생성, CI가 코드와 일치 여부 검사).',
    `> ${api.info?.title ?? 'API'} ${api.info?.version ?? ''} · 엔드포인트 ${count}개 · 스키마 ${schemas.length}개`,
    '',
    '## 공통 규칙',
    '',
    '- 응답·에러 형식, 에러 코드 체계, 페이지네이션, 인증 헤더: [`docs/conventions.md`](../conventions.md)',
    '- 인증: `Authorization: Bearer <accessToken>` (Swagger의 Authorize 버튼에 Access 토큰 입력)',
    '- 권한: **공개**는 토큰 없이, **로그인**은 Access 토큰, **ADMIN**은 관리자 계정 토큰이 필요하다. 에러 응답의 괄호 안이 에러 코드다',
    '',
    '## 엔드포인트 목록',
    ...groups.flatMap((g) => ['', `### ${g.tag}`, '', table(['메서드', 'URL', '요약', '권한'], g.ops.map(({ method, path, op }) => [`\`${method.toUpperCase()}\``, path, op.summary, accessOf(op)]))]),
    '',
    '## 엔드포인트 상세',
    ...groups.flatMap((g) => ['', `### ${g.tag}`, ...g.ops.flatMap((o) => ['', operationSection(o)])]),
    '',
    '## 스키마 부록',
    ...schemas.flatMap(([name, s]) => ['', schemaSection(name, s)]),
  ];
  return `${parts.join('\n')}\n`;
}
