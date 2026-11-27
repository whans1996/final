// guard-files hook의 파일 도구 판정 (테스트 있음). 내장 도구(Read·Edit·Write·Grep·Glob)와 Desktop Commander MCP 파일 도구(H-1)를 같은 규칙으로 본다.
//  - 쓰기: .env·키 파일, 이미 커밋된 Flyway 버전 마이그레이션, 상대 NOW 파일 / 쓰는 내용의 비밀값 (H-2)
//  - 읽기·검색: .env·키 파일 내용 (HX-1. 이름 목록만 보는 Glob·파일 이름 검색은 통과)
//  - eval: 김동환 PC(ctx.evalDir)에서 data/raw/eval 아래 읽기·쓰기·목록·검색 (HX-2)
// 판정만 한다. git·파일 시스템 조회는 hook이 ctx 함수로 넘긴다.
import { isAbsolute, relative, resolve } from 'node:path';
import { findSecrets, globHitsSecret, isSecretFile, isWithin } from './protect.mjs';

const DC = /desktop-commander__(\w+)$/;

/** 글롭의 고정된 앞부분 (data/raw/eval/** → data/raw/eval/) */
const globBase = (pattern) => String(pattern || '').split(/[*?[{]/)[0];

/** 도구 입력 → { reads, writes, lists, searches: [{ path, globs, content, mcp }], texts } */
export function fileTargets(tool, input = {}) {
  const t = { reads: [], writes: [], lists: [], searches: [], texts: [] };
  const dc = DC.exec(String(tool || ''))?.[1];
  switch (dc ? `dc:${dc}` : tool) {
    case 'Read':
      t.reads.push(input.file_path);
      break;
    case 'Grep':
      t.searches.push({ path: input.path, globs: [input.glob], content: true });
      break;
    case 'Glob': {
      const base = globBase(input.pattern);
      t.lists.push(input.path ?? '.', input.path && base && !isAbsolute(base) ? `${input.path}/${base}` : base || '.');
      break;
    }
    case 'Edit':
    case 'Write':
    case 'MultiEdit':
    case 'NotebookEdit':
      t.writes.push(input.file_path ?? input.notebook_path);
      t.texts.push(input.content, input.new_string, input.new_source, ...(Array.isArray(input.edits) ? input.edits.map((e) => e?.new_string) : []));
      break;
    case 'dc:read_file':
      if (!input.isUrl) t.reads.push(input.path);
      break;
    case 'dc:read_multiple_files':
      t.reads.push(...(Array.isArray(input.paths) ? input.paths : []));
      break;
    case 'dc:get_file_info':
      t.reads.push(input.path);
      break;
    case 'dc:list_directory':
      t.lists.push(input.path);
      break;
    case 'dc:start_search':
      t.searches.push({ path: input.path, globs: String(input.filePattern || '').split('|'), content: input.searchType === 'content', mcp: true, hidden: input.includeHidden === true });
      break;
    case 'dc:write_file':
      t.writes.push(input.path);
      t.texts.push(input.content);
      break;
    case 'dc:edit_block':
      t.writes.push(input.file_path);
      t.texts.push(input.new_string, typeof input.content === 'string' ? input.content : input.content == null ? null : JSON.stringify(input.content));
      break;
    case 'dc:write_pdf':
      t.writes.push(input.path, input.outputPath);
      t.texts.push(typeof input.content === 'string' ? input.content : input.content == null ? null : JSON.stringify(input.content));
      break;
    case 'dc:move_file':
      // 옮기기는 원본을 지우고 대상에 쓴다: 양쪽 모두 쓰기로 본다
      t.writes.push(input.source, input.destination);
      break;
    default:
      break;
  }
  const str = (a) => a.filter((x) => typeof x === 'string' && x !== '');
  return { reads: str(t.reads), writes: str(t.writes), lists: str(t.lists), searches: t.searches.map((s) => ({ ...s, globs: str(s.globs) })), texts: str(t.texts) };
}

const EVAL = '평가(eval) 데이터는 김동환(A) PC에서 열람·수정할 수 없습니다 (블라인드 평가, docs/산출물/09-데이터셋.md 5절). 필요한 것은 김준섭에게 요청하세요.';

/**
 * 막을 사유(문자열) 또는 null.
 * ctx: { cwd, root, evalDir?, exists?(path), committedMigration?(rel): 이미 커밋된 파일인가, peerNowRel?: 상대 NOW 경로(미등록 PC는 null),
 *        secretFiles?(dir): dir 아래 실제 비밀 파일 [{ path }] (검색 글롭 판정) }
 */
export function checkFileTool(tool, input = {}, ctx = {}) {
  const root = ctx.root || process.cwd();
  const abs = (p) => (isAbsolute(p) || /^[A-Za-z]:[\\/]/.test(p) ? p : resolve(ctx.cwd || root, p));
  const rel = (p) => relative(root, abs(p)).replace(/\\/g, '/');
  const t = fileTargets(tool, input);

  if (ctx.evalDir) {
    const paths = [...t.reads, ...t.writes, ...t.lists, ...t.searches.map((s) => s.path || ctx.cwd || root)];
    if (paths.some((p) => isWithin(abs(p), ctx.evalDir))) return EVAL;
    // MCP 검색(Desktop Commander 0.3.x)은 ripgrep 부분은 .gitignore를 따르지만 Excel·DOCX 내용 검색은 폴더를 직접 훑어
    // .gitignore(data/raw/)를 따르지 않는다. 그래서 eval 폴더가 있으면 그 상위에서 시작한 내용 검색은 막는다
    if (t.searches.some((s) => s.mcp && s.content && isWithin(ctx.evalDir, abs(s.path || ctx.cwd || root))) && ctx.exists?.(ctx.evalDir)) return EVAL;
  }
  for (const p of t.writes) {
    const r = rel(p);
    if (isSecretFile(p)) return `${r}: .env·키 파일은 AI가 수정하지 않습니다. 필요한 변수는 .env.example에 이름만 추가하고, 값은 사용자에게 직접 넣도록 안내하세요.`;
    if (/(^|\/)db\/migration\/V[^/]*\.sql$/i.test(r) && ctx.committedMigration?.(r))
      return `${r}: 이미 커밋된 Flyway 마이그레이션은 수정할 수 없습니다 (상대 PC DB와 체크섬이 어긋남). 다음 번호의 V{n}__설명.sql을 새로 만드세요.`;
    // NTFS는 대소문자를 무시한다 (now-js.md도 같은 파일)
    if (ctx.peerNowRel && r.toLowerCase() === ctx.peerNowRel.toLowerCase()) return `${r}: 상대의 NOW 파일입니다. 내 상태는 내 NOW 파일에만 쓰고, 상대에게 전할 말은 PR 댓글이나 Mattermost로 남기세요.`;
  }
  for (const text of t.texts) {
    const [s] = findSecrets(text);
    if (s) return `${s.label}로 보이는 값이 포함되어 있습니다. 키는 환경변수(예: \${GEMINI_API_KEY})로 참조하세요. 테스트용 가짜 값은 문자열을 나눠 이어 붙이세요.`;
  }
  const secretRead = (p) => `${rel(p)}: .env·키 파일 내용은 읽을 수 없습니다. 필요한 변수 이름은 .env.example을 보세요.`;
  for (const p of t.reads) if (isSecretFile(p)) return secretRead(p);
  for (const s of t.searches) {
    if (s.path && isSecretFile(s.path)) return secretRead(s.path);
    if (!s.content) continue;
    // 글롭은 무시 규칙(.gitignore)보다 우선한다: 비밀 파일을 고르는 글롭으로 내용을 검색하면 값이 출력된다 (#188).
    // hook이 넘긴 실제 비밀 파일(검색 폴더 기준 상대 경로)로 보고, 목록이 없으면 대표 이름으로 본다
    const base = abs(s.path || ctx.cwd || root);
    // 목록을 모르는 곳(hook 밖, 저장소 밖: null)과 확인한 빈 목록([])을 구분한다 (Codex)
    const files = ctx.secretFiles?.(base)?.map((f) => relative(base, f.path).replace(/\\/g, '/')) ?? undefined;
    if (s.globs.some((g) => globHitsSecret(g, files))) return '.env·키 파일 내용은 검색할 수 없습니다. 필요한 변수 이름은 .env.example을 보세요.';
    if (s.hidden && !s.globs.length && (!files || files.length)) return '숨김 파일(.env 포함)까지 보는 내용 검색은 filePattern으로 파일 종류를 좁혀 주세요 (#188).';
  }
  return null;
}
