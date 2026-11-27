// 시드 카드 ②예제 실행 검사 중 FE 쪽 (K-3a AC9): js·jsx 문법, ts·tsx 문법·타입, vue SFC, json.
// typescript·@vue/compiler-sfc는 frontend/node_modules에서 읽는다 (하네스 잡처럼 없으면 null → 건너뜀).
// java·sql·xml·yaml은 BE 테스트(SeedExampleTest)가 맡는다.
import { createRequire } from 'node:module';
import { existsSync } from 'node:fs';
import { join, posix } from 'node:path';
import { ROOT } from '../common.mjs';
import { UNCHECKED_LANGS } from './card.mjs';

export const FE_LANGS = ['js', 'jsx', 'ts', 'tsx', 'vue', 'json'];
export const BE_LANGS = ['java', 'sql', 'xml', 'yaml'];
/** 맥락이 없어서 나는 진단 (이름·모듈을 못 찾음) → 경고 */
export const TS_CONTEXT_CODES = new Set([2304, 2307, 2552, 2582, 7016]);

/** frontend/node_modules의 typescript·@vue/compiler-sfc. 없으면 null */
export function loadFeTools(root = ROOT) {
  const pkg = join(root, 'frontend', 'package.json');
  if (!existsSync(join(root, 'frontend', 'node_modules'))) return null;
  try {
    const req = createRequire(pkg);
    return { ts: req('typescript'), sfc: req('@vue/compiler-sfc'), root };
  } catch {
    return null;
  }
}

/** <script setup> 컴파일러 매크로. vue가 내보내는 실제 타입으로 검사한다 (any로 선언하면 타입을 준 emit·model 오용을 놓친다, Codex 리뷰) */
const MACROS = ['defineProps', 'defineEmits', 'defineModel', 'withDefaults', 'defineExpose', 'defineOptions', 'defineSlots'];
/** 매크로 import 한 줄. 조각이 이미 import한 이름은 뺀다 (이름이 겹치지 않게) */
const setupMacros = (content) => {
  const imported = new Set(
    [...content.matchAll(/import\s*(?:type\s*)?\{([^}]*)\}/g)].flatMap((m) => m[1].split(',').map((x) => x.trim().split(/\s+as\s+/).pop())),
  );
  const names = MACROS.filter((n) => !imported.has(n));
  return `${names.length ? `import { ${names.join(', ')} } from "vue";` : ''}\n`;
};
const slugOf = (file) => posix.basename(file).replace(/\.md$/, '');

/**
 * ts·js 가상 파일 묶음을 한 프로그램으로 검사한다 (lib.d.ts를 한 번만 읽게).
 * 파일은 frontend/ 아래 가상 경로에 두어 vue·pinia·vue-router 타입이 실제로 해석된다.
 * @param units [{path, text, semantic, report(diag)}] semantic: true면 타입까지, 'grammar'면 checker의 문법 진단(TS1xxx)만 (js)
 */
function runTs(ts, root, units) {
  if (!units.length) return;
  const options = {
    noEmit: true,
    target: ts.ScriptTarget.ES2022,
    module: ts.ModuleKind.ESNext,
    moduleResolution: ts.ModuleResolutionKind.Bundler,
    jsx: ts.JsxEmit.Preserve,
    allowJs: true,
    // js도 checker를 돌려야 루프 밖 break·최상위 return 같은 문법 오류가 나온다. js는 그중 TS1xxx만 쓴다
    checkJs: true,
    strict: false,
    noImplicitAny: false,
    skipLibCheck: true,
    resolveJsonModule: true,
    types: [],
    lib: ['lib.es2023.d.ts', 'lib.dom.d.ts', 'lib.dom.iterable.d.ts'],
  };
  const files = new Map(units.map((u) => [u.path, u.text]));
  const host = ts.createCompilerHost(options, true);
  const norm = (p) => p.replace(/\\/g, '/');
  const getSourceFile = host.getSourceFile.bind(host);
  host.getSourceFile = (name, lang, onError, create) => {
    const text = files.get(norm(name));
    return text != null ? ts.createSourceFile(name, text, lang, true) : getSourceFile(name, lang, onError, create);
  };
  const fileExists = host.fileExists.bind(host);
  host.fileExists = (name) => files.has(norm(name)) || fileExists(name);
  const readFile = host.readFile.bind(host);
  host.readFile = (name) => files.get(norm(name)) ?? readFile(name);
  const program = ts.createProgram({ rootNames: [...files.keys()], options, host });
  for (const u of units) {
    const sf = program.getSourceFile(u.path);
    // 문법 오류는 연쇄로 여러 개 나오므로 첫 오류만 본다
    const diags = program.getSyntacticDiagnostics(sf).slice(0, 1);
    if (u.semantic && !diags.length) {
      const sem = program.getSemanticDiagnostics(sf);
      diags.push(...(u.semantic === 'grammar' ? sem.filter((d) => d.code >= 1000 && d.code < 2000).slice(0, 1) : sem));
    }
    for (const d of diags) {
      const line = d.file && d.start != null ? d.file.getLineAndCharacterOfPosition(d.start).line + 1 : 1;
      u.report({ code: d.code, line, message: ts.flattenDiagnosticMessageText(d.messageText, ' ') });
    }
  }
}

/** 첫 컴파일 오류만 (뒤 오류는 대개 연쇄) */
const compilerErrors = (errors) =>
  (errors || []).slice(0, 1).map((e) => (typeof e === 'string' ? { message: e, line: 1 } : { message: e.message, line: e.loc?.start?.line ?? 1 }));

/**
 * 카드들의 FE 언어 블록을 검사한다.
 * @returns {{issues, unchecked, checked}} issues: {file, stage: '②', severity, item, line, message}
 *   unchecked: "검사 안 함" 블록 {file, lang, line} · checked: 언어별 검사한 블록 수
 */
export function checkFeExamples(cards, tools) {
  const { ts, sfc, root } = tools;
  const issues = [];
  const unchecked = [];
  const checked = {};
  const units = [];
  const base = `${root.replace(/\\/g, '/')}/frontend/__seed_check__`;

  for (const card of cards) {
    card.blocks.forEach((block, idx) => {
      const { lang, code } = block;
      const add = (severity, line, message) =>
        issues.push({ file: card.file, stage: '②', severity, item: lang, line: block.line + line - 1, message });
      if (UNCHECKED_LANGS.includes(lang)) return void unchecked.push({ file: card.file, lang, line: block.line });
      if (!FE_LANGS.includes(lang)) return;
      checked[lang] = (checked[lang] ?? 0) + 1;
      const tsUnit = (ext, text, offset, semantic, part = '') =>
        units.push({
          // part: vue의 <script>·<script setup>을 서로 다른 파일로 (경로가 같으면 한쪽이 버려진다)
          path: `${base}/${card.file.split('/').at(-2)}-${slugOf(card.file)}-${idx + 1}${part}.${ext}`,
          // 블록끼리 전역 이름이 겹치지 않게 모듈로 만든다 (끝에 붙여 줄 번호는 그대로)
          text: `${text}\nexport {};\n`,
          semantic,
          report: (d) => {
            const contextual = TS_CONTEXT_CODES.has(d.code);
            add(contextual ? 'warn' : 'error', d.line + offset, `TS${d.code} ${d.message}`);
          },
        });

      if (lang === 'json') {
        try {
          JSON.parse(code);
        } catch (e) {
          add('error', 1, `JSON 파싱 실패: ${e.message}`);
        }
      } else if (lang === 'js' || lang === 'jsx') tsUnit(lang, code, 0, 'grammar');
      else if (lang === 'ts' || lang === 'tsx') tsUnit(lang, code, 0, true);
      else if (lang === 'vue') {
        const filename = `${slugOf(card.file)}-${idx + 1}.vue`;
        const id = `seed${idx}`;
        const { descriptor, errors } = sfc.parse(code, { filename });
        const { template, script, scriptSetup } = descriptor;
        if (!template && !script && !scriptSetup && !descriptor.styles.length) {
          // 템플릿 조각 (<template> 없이 마크업만): SFC가 아니라 템플릿으로 컴파일한다
          const r = sfc.compileTemplate({ source: code, filename, id });
          for (const e of compilerErrors(r.errors)) add('error', e.line, `템플릿: ${e.message}`);
          return;
        }
        for (const e of compilerErrors(errors)) add('error', e.line, `SFC 파싱: ${e.message}`);
        if (errors?.length) return;
        if (template) {
          const r = sfc.compileTemplate({ source: template.content, filename, id });
          const offset = template.loc.start.line - 1;
          for (const e of compilerErrors(r.errors)) add('error', e.line + offset, `템플릿: ${e.message}`);
        }
        if (script || scriptSetup) {
          try {
            sfc.compileScript(descriptor, { id });
          } catch (e) {
            add('error', (e.loc?.start?.line ?? 1), `스크립트: ${String(e.message).split('\n')[0]}`);
          }
          for (const s of [script, scriptSetup]) {
            if (!s || (s.lang !== 'ts' && s.lang !== 'tsx')) continue;
            // <script setup>의 컴파일러 매크로는 import 없이 쓴다 → 한 줄로 vue에서 import하고 줄 번호를 하나 당긴다
            const text = s === scriptSetup ? `${setupMacros(s.content)}${s.content}` : s.content;
            tsUnit(s.lang, text, s.loc.start.line - 1 - (s === scriptSetup ? 1 : 0), true, s === scriptSetup ? '-setup' : '-script');
          }
        }
      }
    });
  }
  runTs(ts, root, units);
  return { issues, unchecked, checked };
}
