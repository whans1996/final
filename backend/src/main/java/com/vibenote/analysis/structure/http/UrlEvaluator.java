package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.structure.js.JsDecl;
import com.vibenote.analysis.structure.js.JsExport;
import com.vibenote.analysis.structure.js.JsFunction;
import com.vibenote.analysis.structure.js.JsImport;
import com.vibenote.analysis.structure.js.JsModule;
import com.vibenote.analysis.structure.js.JsToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * URL 식 계산 (F-21a AC8·AC10). 문자열, 템플릿({@code ${}}는 값을 알면 넣고 모르면 동적 조각), {@code +} 연결, 보이는 {@code
 * const} 식별자, 선별 파일에서 import한 {@code export const}·{@code export default}만 값을 안다. 그 밖의 식(멤버 접근, 호출,
 * 조건식 등)은 동적 조각이다. 래퍼 인라인 중이면 그 함수의 매개변수를 부른 곳의 인자로 바꿔 계산한다.
 */
public final class UrlEvaluator {

    private static final int MAX_DEPTH = 16;

    private final Map<Long, JsModule> modules;

    public UrlEvaluator(Map<Long, JsModule> modules) {
        this.modules = modules;
    }

    /** 부른 곳의 인자 하나와 그 인자를 계산할 모듈. */
    public record Arg(List<JsToken> tokens, JsModule module) {}

    /** 인라인 중인 래퍼 함수와 매개변수 이름 → 인자. 인자가 없는 매개변수는 키가 없다. */
    public record Bindings(JsFunction function, Map<String, Arg> args) {
        public static final Bindings NONE = new Bindings(null, Map.of());
    }

    public UrlValue eval(List<JsToken> tokens, JsModule module, Bindings bindings) {
        return eval(tokens, module, bindings, 0);
    }

    private UrlValue eval(List<JsToken> tokens, JsModule module, Bindings bindings, int depth) {
        if (tokens == null || tokens.isEmpty() || depth > MAX_DEPTH) {
            return UrlValue.dynamic();
        }
        UrlValue value = UrlValue.literal("");
        for (List<JsToken> part : splitPlus(tokens)) {
            value = value.plus(operand(part, module, bindings, depth));
        }
        return value;
    }

    private UrlValue operand(List<JsToken> part, JsModule module, Bindings bindings, int depth) {
        if (part.size() == 1) {
            JsToken token = part.get(0);
            return switch (token.kind()) {
                case STRING -> UrlValue.literal(token.value());
                case NUMBER -> UrlValue.literal(token.text());
                case TEMPLATE -> template(token, module, bindings, depth);
                case IDENT -> identifier(token, module, bindings, depth);
                default -> UrlValue.dynamic();
            };
        }
        if (part.size() > 2 && part.get(0).is("(") && part.get(part.size() - 1).is(")")) {
            return eval(part.subList(1, part.size() - 1), module, bindings, depth + 1);
        }
        return UrlValue.dynamic();
    }

    private UrlValue template(JsToken token, JsModule module, Bindings bindings, int depth) {
        UrlValue value = UrlValue.literal(token.quasis().get(0));
        for (int i = 0; i < token.parts().size(); i++) {
            value =
                    value.plus(eval(token.parts().get(i), module, bindings, depth + 1))
                            .plus(UrlValue.literal(token.quasis().get(i + 1)));
        }
        return value;
    }

    private UrlValue identifier(JsToken token, JsModule module, Bindings bindings, int depth) {
        String name = token.text();
        Optional<JsDecl> decl = module.lookup(name, token.start());
        if (decl.isPresent()) {
            JsDecl d = decl.get();
            return switch (d.kind()) {
                case PARAM -> {
                    if (d.function().equals(bindings.function())) {
                        Arg arg = bindings.args().get(name);
                        yield arg == null
                                ? UrlValue.dynamic()
                                : eval(arg.tokens(), arg.module(), Bindings.NONE, depth + 1);
                    }
                    yield UrlValue.dynamic(d.function());
                }
                case CONST -> eval(d.init(), module, bindings, depth + 1);
                default -> UrlValue.dynamic();
            };
        }
        Optional<Map.Entry<JsImport, String>> imported = module.importOf(name);
        if (imported.isPresent() && imported.get().getKey().target() != null) {
            JsModule target = modules.get(imported.get().getKey().target());
            if (target != null) {
                return exported(target, imported.get().getValue(), depth);
            }
        }
        return UrlValue.dynamic();
    }

    private UrlValue exported(JsModule target, String name, int depth) {
        JsExport export = target.exports().get(name);
        if (export == null) {
            return UrlValue.dynamic();
        }
        if (export.expr() != null) {
            return eval(export.expr(), target, Bindings.NONE, depth + 1);
        }
        return target.lookupTopLevel(export.local())
                .filter(d -> d.kind() == JsDecl.Kind.CONST)
                .map(d -> eval(d.init(), target, Bindings.NONE, depth + 1))
                .orElseGet(UrlValue::dynamic);
    }

    /** 괄호 밖의 {@code +}로 나눈다. */
    private static List<List<JsToken>> splitPlus(List<JsToken> tokens) {
        List<List<JsToken>> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            JsToken token = tokens.get(i);
            if (token.is("(") || token.is("[") || token.is("{")) {
                depth++;
            } else if (token.is(")") || token.is("]") || token.is("}")) {
                depth--;
            } else if (token.is("+") && depth == 0) {
                parts.add(tokens.subList(start, i));
                start = i + 1;
            }
        }
        parts.add(tokens.subList(start, tokens.size()));
        return parts;
    }
}
