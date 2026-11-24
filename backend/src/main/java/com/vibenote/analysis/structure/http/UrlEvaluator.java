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
import java.util.regex.Pattern;

/**
 * URL 식 계산 (F-21a AC8·AC10). 문자열, 템플릿({@code ${}}는 값을 알면 넣고 모르면 동적 조각), {@code +} 연결, 보이는 {@code
 * const} 식별자, 선별 파일에서 import한 {@code export const}·{@code export default}만 값을 안다. 그 밖의 식(멤버 접근, 호출,
 * 조건식 등)은 동적 조각이다. 래퍼 인라인 중이면 그 함수의 매개변수를 부른 곳의 인자로 바꿔 계산한다.
 *
 * <p>업로드한 코드가 계산량을 불리지 못하게, 식 하나를 계산하는 동안 피연산자 수({@link #MAX_OPERANDS})와 값 길이({@link #MAX_LENGTH})에
 * 상한을 둔다. 넘으면 그 식 전체를 동적 조각으로 본다 ({@code const u = u + u}처럼 자기를 가리키는 식 포함).
 */
public final class UrlEvaluator {

    private static final int MAX_DEPTH = 16;

    static final int MAX_OPERANDS = 2_000;

    static final int MAX_LENGTH = 2_048;

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

    /** 식 하나를 계산하는 동안 남은 피연산자 수. 0 아래로 내려가거나 값이 너무 길어지면 그 식은 계산하지 못한 것이다. */
    private int budget;

    public UrlValue eval(List<JsToken> tokens, JsModule module, Bindings bindings) {
        budget = MAX_OPERANDS;
        UrlValue value = eval(tokens, module, bindings, 0);
        // 문자열 하나뿐인 식은 이어 붙이지 않아 capped를 거치지 않는다. 최종 값에도 길이 상한을 둔다
        return budget < 0 || value.text().length() > MAX_LENGTH ? UrlValue.dynamic() : value;
    }

    /**
     * 계산한 값과 그 값이 숫자인지. JS의 {@code +}는 양쪽이 숫자면 더하기라서, 숫자끼리 만나면 문자열로 잇지 않고 동적 조각으로 둔다 ({@code
     * '/todos/' + (1 + 2)}가 {@code /todos/12}가 되지 않게).
     */
    private record Typed(UrlValue value, boolean number) {
        static Typed text(UrlValue value) {
            return new Typed(value, false);
        }
    }

    /** 이 모양의 정수만 JS가 문자열로 바꿔도 원문과 같다 ({@code 1.50}·{@code 0x10}·{@code 1e3}은 다르다). */
    private static final Pattern CANONICAL_INTEGER = Pattern.compile("0|[1-9][0-9]{0,14}");

    private UrlValue eval(List<JsToken> tokens, JsModule module, Bindings bindings, int depth) {
        return typed(tokens, module, bindings, depth).value();
    }

    private Typed typed(List<JsToken> tokens, JsModule module, Bindings bindings, int depth) {
        if (tokens == null || tokens.isEmpty() || depth > MAX_DEPTH || budget < 0) {
            return Typed.text(UrlValue.dynamic());
        }
        Typed acc = null;
        for (List<JsToken> part : splitPlus(tokens)) {
            Typed next = operand(part, module, bindings, depth);
            if (acc == null) {
                acc = next;
            } else if (acc.number() && next.number()) {
                acc = Typed.text(UrlValue.dynamic());
            } else {
                acc = Typed.text(capped(acc.value().plus(next.value())));
            }
        }
        return acc;
    }

    /** 값이 {@link #MAX_LENGTH}를 넘으면 예산을 다 쓴 것으로 본다. */
    private UrlValue capped(UrlValue value) {
        if (value.text().length() > MAX_LENGTH) {
            budget = -1;
            return UrlValue.dynamic();
        }
        return value;
    }

    private Typed operand(List<JsToken> part, JsModule module, Bindings bindings, int depth) {
        if (--budget < 0) {
            return Typed.text(UrlValue.dynamic());
        }
        if (part.size() == 1) {
            JsToken token = part.get(0);
            return switch (token.kind()) {
                case STRING -> Typed.text(UrlValue.literal(token.value()));
                case NUMBER ->
                        new Typed(
                                CANONICAL_INTEGER.matcher(token.text()).matches()
                                        ? UrlValue.literal(token.text())
                                        : UrlValue.dynamic(),
                                true);
                case TEMPLATE -> Typed.text(template(token, module, bindings, depth));
                case IDENT -> identifier(token, module, bindings, depth);
                default -> Typed.text(UrlValue.dynamic());
            };
        }
        if (part.size() > 2 && part.get(0).is("(") && part.get(part.size() - 1).is(")")) {
            return typed(part.subList(1, part.size() - 1), module, bindings, depth + 1);
        }
        return Typed.text(UrlValue.dynamic());
    }

    private UrlValue template(JsToken token, JsModule module, Bindings bindings, int depth) {
        UrlValue value = UrlValue.literal(token.quasis().get(0));
        for (int i = 0; i < token.parts().size(); i++) {
            value =
                    capped(
                            value.plus(eval(token.parts().get(i), module, bindings, depth + 1))
                                    .plus(UrlValue.literal(token.quasis().get(i + 1))));
        }
        return value;
    }

    private Typed identifier(JsToken token, JsModule module, Bindings bindings, int depth) {
        String name = token.text();
        Optional<JsDecl> decl = module.lookup(name, token.start());
        if (decl.isPresent()) {
            JsDecl d = decl.get();
            return switch (d.kind()) {
                case PARAM -> {
                    if (d.function().equals(bindings.function())) {
                        Arg arg = bindings.args().get(name);
                        yield arg == null
                                ? Typed.text(UrlValue.dynamic())
                                : typed(arg.tokens(), arg.module(), Bindings.NONE, depth + 1);
                    }
                    yield Typed.text(UrlValue.dynamic(d.function()));
                }
                case CONST -> typed(d.init(), module, bindings, depth + 1);
                default -> Typed.text(UrlValue.dynamic());
            };
        }
        Optional<Map.Entry<JsImport, String>> imported = module.importOf(name);
        if (imported.isPresent() && imported.get().getKey().target() != null) {
            JsModule target = modules.get(imported.get().getKey().target());
            if (target != null) {
                return exported(target, imported.get().getValue(), depth);
            }
        }
        return Typed.text(UrlValue.dynamic());
    }

    private Typed exported(JsModule target, String name, int depth) {
        JsExport export = target.exports().get(name);
        if (export == null) {
            return Typed.text(UrlValue.dynamic());
        }
        if (export.expr() != null) {
            return typed(export.expr(), target, Bindings.NONE, depth + 1);
        }
        return target.lookupTopLevel(export.local())
                .filter(d -> d.kind() == JsDecl.Kind.CONST)
                .map(d -> typed(d.init(), target, Bindings.NONE, depth + 1))
                .orElseGet(() -> Typed.text(UrlValue.dynamic()));
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
