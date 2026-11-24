package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.http.UrlEvaluator.Arg;
import com.vibenote.analysis.structure.http.UrlEvaluator.Bindings;
import com.vibenote.analysis.structure.js.JsCall;
import com.vibenote.analysis.structure.js.JsDecl;
import com.vibenote.analysis.structure.js.JsExport;
import com.vibenote.analysis.structure.js.JsFunction;
import com.vibenote.analysis.structure.js.JsImport;
import com.vibenote.analysis.structure.js.JsModule;
import com.vibenote.analysis.structure.js.JsToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FE → API {@code HTTP} 간선 (F-21a AC7~AC11).
 *
 * <p>HTTP 호출은 받는 쪽 R의 {@code R.get/delete/head/options(url, config)}, {@code R.post/put/patch(url,
 * data, config)}, {@code R.request(config)}, {@code R(config)}와 {@code fetch(url, init)}뿐이다. R은
 * axios import 이름, 같은 파일의 {@code const X = axios.create(…)}, 선별 파일에서 import한 이름이 그 파일의 {@code
 * axios.create(…)} 내보내기에 이어질 때만이다. URL을 계산하지 못했고 그 이유가 이름 있는 함수의 매개변수면 그 함수를 부른 곳마다 한 단계 인라인한다.
 */
public final class HttpLinker {

    static final int MAX_DETAIL = 80;

    private static final Set<String> URL_CONFIG_SECOND = Set.of("get", "delete", "head", "options");
    private static final Set<String> URL_CONFIG_THIRD = Set.of("post", "put", "patch");

    private static final int MAX_CONFIG_DEPTH = 16;

    private final Map<Long, JsModule> modules;
    private final UrlEvaluator evaluator;
    private final List<ApiEndpoint> apis;
    private final String contextPath;
    private final StructureLinks out;

    private HttpLinker(
            Map<Long, JsModule> modules,
            List<ApiEndpoint> apis,
            String contextPath,
            StructureLinks out) {
        this.modules = modules;
        this.evaluator = new UrlEvaluator(modules);
        this.apis = apis;
        this.contextPath = contextPath;
        this.out = out;
    }

    public static void link(
            Map<Long, JsModule> modules,
            List<ApiEndpoint> apis,
            String contextPath,
            StructureLinks out) {
        HttpLinker linker = new HttpLinker(modules, apis, contextPath, out);
        modules.values().stream()
                .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                .forEach(
                        module -> {
                            for (JsCall call : module.calls()) {
                                HttpCall http = linker.classify(call, module);
                                if (http != null) {
                                    linker.handle(http);
                                }
                            }
                        });
    }

    // ---- HTTP 호출 판정 (AC7) ----

    private enum Shape {
        URL_THEN_CONFIG,
        CONFIG,
        FETCH
    }

    private enum ReceiverType {
        AXIOS,
        INSTANCE,
        FETCH
    }

    /** createArgs는 INSTANCE의 {@code axios.create(…)} 인자 토큰(없으면 빈 목록)이고 그 모듈이 module이다. */
    private record Receiver(ReceiverType type, JsModule module, List<JsToken> createArgs) {}

    private record HttpCall(
            JsCall call, JsModule module, Receiver receiver, Shape shape, int configIndex) {}

    private HttpCall classify(JsCall call, JsModule module) {
        if (call.member() == null) {
            if (call.receiver().equals("fetch")
                    && module.lookup("fetch", call.start()).isEmpty()
                    && module.importOf("fetch").isEmpty()) {
                return new HttpCall(
                        call,
                        module,
                        new Receiver(ReceiverType.FETCH, module, List.of()),
                        Shape.FETCH,
                        1);
            }
            Receiver receiver = receiver(call.receiver(), call.start(), module);
            return receiver == null ? null : new HttpCall(call, module, receiver, Shape.CONFIG, 0);
        }
        String member = call.member();
        Shape shape;
        int configIndex;
        if (URL_CONFIG_SECOND.contains(member)) {
            shape = Shape.URL_THEN_CONFIG;
            configIndex = 1;
        } else if (URL_CONFIG_THIRD.contains(member)) {
            shape = Shape.URL_THEN_CONFIG;
            configIndex = 2;
        } else if (member.equals("request")) {
            shape = Shape.CONFIG;
            configIndex = 0;
        } else {
            return null;
        }
        Receiver receiver = receiver(call.receiver(), call.start(), module);
        return receiver == null ? null : new HttpCall(call, module, receiver, shape, configIndex);
    }

    private Receiver receiver(String name, int offset, JsModule module) {
        Optional<JsDecl> decl = module.lookup(name, offset);
        if (decl.isEmpty() && module.axiosNames().contains(name)) {
            return new Receiver(ReceiverType.AXIOS, module, List.of());
        }
        if (decl.isPresent()) {
            if (decl.get().kind() != JsDecl.Kind.CONST) {
                return null;
            }
            if (requiresAxios(decl.get().init())) {
                return new Receiver(ReceiverType.AXIOS, module, List.of());
            }
            return instance(decl.get().init(), module);
        }
        Optional<Map.Entry<JsImport, String>> imported = module.importOf(name);
        if (imported.isEmpty() || imported.get().getKey().target() == null) {
            return null;
        }
        JsModule target = modules.get(imported.get().getKey().target());
        JsExport export = target == null ? null : target.exports().get(imported.get().getValue());
        if (export == null) {
            return null;
        }
        if (export.expr() != null) {
            return instance(export.expr(), target);
        }
        return target.lookupTopLevel(export.local())
                .filter(d -> d.kind() == JsDecl.Kind.CONST)
                .map(d -> instance(d.init(), target))
                .orElse(null);
    }

    /** {@code const axios = require('axios')}의 초기값. */
    private static boolean requiresAxios(List<JsToken> init) {
        return init.size() == 4
                && init.get(0).is("require")
                && init.get(1).is("(")
                && init.get(2).kind() == JsToken.Kind.STRING
                && init.get(2).value().equals("axios")
                && init.get(3).is(")");
    }

    /** {@code A.create(…)}(A는 그 모듈의 axios 이름) 식이면 인스턴스. */
    private static Receiver instance(List<JsToken> init, JsModule module) {
        if (init.size() < 5
                || !module.axiosNames().contains(init.get(0).text())
                || !init.get(1).is(".")
                || !init.get(2).is("create")
                || !init.get(3).is("(")
                || !init.get(init.size() - 1).is(")")
                || closingParen(init, 3) != init.size() - 1) {
            return null;
        }
        List<JsToken> args = init.subList(4, init.size() - 1);
        List<JsToken> config = args.isEmpty() ? List.of() : firstArg(args);
        return new Receiver(ReceiverType.INSTANCE, module, config);
    }

    // ---- 요청 계산 (AC8~AC10) ----

    private record Expr(List<JsToken> tokens, JsModule module, Bindings bindings) {}

    /**
     * 객체 리터럴의 속성. props가 null이면 객체 리터럴이 아니거나 값을 모른다. unknown은 {@code ...전개} 뒤에 직접 적지 않은 속성의 값(값을
     * 모르는 식)이다. 전개가 덮어쓸 수 있으므로 없는 속성으로 보지 않는다.
     */
    private record Config(Map<String, Expr> props, JsFunction paramOf, Expr unknown) {
        Config(Map<String, Expr> props, JsFunction paramOf) {
            this(props, paramOf, null);
        }

        Expr prop(String name) {
            if (props == null) {
                return null;
            }
            return props.containsKey(name) ? props.get(name) : unknown;
        }
    }

    private record Outcome(FeRequest request, UnresolvedReason reason, JsFunction paramOf) {
        static Outcome failed(UnresolvedReason reason, JsFunction paramOf) {
            return new Outcome(null, reason, paramOf);
        }
    }

    private void handle(HttpCall http) {
        JsCall call = http.call();
        JsModule module = http.module();
        Outcome outcome = compute(http, Bindings.NONE);
        if (outcome.request() != null) {
            emit(module, call.line(), outcome.request());
            return;
        }
        JsFunction wrapper = outcome.paramOf();
        if (outcome.reason() != UnresolvedReason.URL_UNKNOWN
                || wrapper == null
                || wrapper.name() == null) {
            unresolved(module, call, outcome.reason());
            return;
        }
        List<Site> sites = callSites(wrapper, module);
        if (sites.isEmpty()) {
            unresolved(module, call, UnresolvedReason.URL_UNKNOWN);
            return;
        }
        for (Site site : sites) {
            Map<String, Arg> args = new HashMap<>();
            for (int i = 0; i < wrapper.params().size(); i++) {
                String param = wrapper.params().get(i);
                List<JsToken> arg = site.call().arg(i);
                if (!param.isEmpty() && arg != null) {
                    args.put(param, new Arg(arg, site.module()));
                }
            }
            Outcome inlined = compute(http, new Bindings(wrapper, args));
            if (inlined.request() != null) {
                emit(site.module(), site.call().line(), inlined.request());
            } else {
                unresolved(site.module(), site.call(), inlined.reason());
            }
        }
    }

    private Outcome compute(HttpCall http, Bindings bindings) {
        JsCall call = http.call();
        JsModule module = http.module();
        Expr url;
        String method;
        Config config = null;
        switch (http.shape()) {
            case URL_THEN_CONFIG -> {
                method = call.member().toUpperCase(Locale.ROOT);
                url = expr(call.arg(0), module, bindings);
                config = config(call.arg(http.configIndex()), module, bindings, 0);
            }
            case CONFIG -> {
                config = config(call.arg(0), module, bindings, 0);
                if (config.props() == null) {
                    return Outcome.failed(UnresolvedReason.URL_UNKNOWN, config.paramOf());
                }
                url = config.prop("url");
                method = method(config.prop("method"));
            }
            default -> {
                url = expr(call.arg(0), module, bindings);
                List<JsToken> init = call.arg(1);
                if (init == null) {
                    method = "GET";
                } else {
                    Config fetchInit = config(init, module, bindings, 0);
                    method = fetchInit.props() == null ? null : method(fetchInit.prop("method"));
                }
            }
        }
        if (url == null) {
            return Outcome.failed(UnresolvedReason.URL_UNKNOWN, null);
        }
        UrlValue value = evaluator.eval(url.tokens(), url.module(), url.bindings());
        if (value.startsDynamic()) {
            return Outcome.failed(UnresolvedReason.URL_UNKNOWN, value.paramOf());
        }
        UrlValue.UrlPath path = value.toPath();
        List<PathSegment> segments = path.segments();
        if (!path.absolute() && http.receiver().type() != ReceiverType.FETCH) {
            Expr base = config == null ? null : config.prop("baseURL");
            if (base == null) {
                base = receiverBase(http.receiver());
            }
            if (base != null) {
                UrlValue baseValue = evaluator.eval(base.tokens(), base.module(), base.bindings());
                if (baseValue.startsDynamic()) {
                    return Outcome.failed(UnresolvedReason.BASE_URL_UNKNOWN, null);
                }
                List<PathSegment> joined = new ArrayList<>(baseValue.toPath().segments());
                joined.addAll(segments);
                segments = joined;
            }
        }
        return new Outcome(new FeRequest(method, List.copyOf(segments)), null, null);
    }

    /** 인스턴스면 {@code create} config의 baseURL, axios면 선별 파일 어디든 첫 {@code axios.defaults.baseURL}. */
    private Expr receiverBase(Receiver receiver) {
        if (receiver.type() == ReceiverType.INSTANCE) {
            return config(receiver.createArgs(), receiver.module(), Bindings.NONE, 0)
                    .prop("baseURL");
        }
        return modules.values().stream()
                .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                .filter(m -> !m.defaultsBaseUrls().isEmpty())
                .findFirst()
                .map(m -> new Expr(m.defaultsBaseUrls().get(0), m, Bindings.NONE))
                .orElse(null);
    }

    /** 메서드 식이 문자열로 계산되면 대문자, 속성이 없으면 axios·fetch 기본값 GET, 계산하지 못하면 null. */
    private String method(Expr method) {
        if (method == null) {
            return "GET";
        }
        UrlValue value = evaluator.eval(method.tokens(), method.module(), method.bindings());
        return value.known() ? value.text().toUpperCase(Locale.ROOT) : null;
    }

    private static Expr expr(List<JsToken> tokens, JsModule module, Bindings bindings) {
        return tokens == null ? null : new Expr(tokens, module, bindings);
    }

    /**
     * 객체 리터럴의 url·method·baseURL 등 속성. 식별자면 보이는 const 초기값이나 인라인 중인 인자를 따라간다. {@code const cfg =
     * cfg}처럼 돌고 도는 식별자는 {@link #MAX_CONFIG_DEPTH}에서 끊는다.
     */
    private Config config(List<JsToken> tokens, JsModule module, Bindings bindings, int depth) {
        if (tokens == null || tokens.isEmpty() || depth > MAX_CONFIG_DEPTH) {
            return new Config(null, null);
        }
        if (tokens.size() == 1 && tokens.get(0).kind() == JsToken.Kind.IDENT) {
            JsToken name = tokens.get(0);
            Optional<JsDecl> decl = module.lookup(name.text(), name.start());
            if (decl.isEmpty()) {
                return new Config(null, null);
            }
            JsDecl d = decl.get();
            if (d.kind() == JsDecl.Kind.PARAM) {
                if (d.function().equals(bindings.function())) {
                    Arg arg = bindings.args().get(name.text());
                    return arg == null
                            ? new Config(null, null)
                            : config(arg.tokens(), arg.module(), Bindings.NONE, depth + 1);
                }
                return new Config(null, d.function());
            }
            return d.kind() == JsDecl.Kind.CONST
                    ? config(d.init(), module, bindings, depth + 1)
                    : new Config(null, null);
        }
        if (!tokens.get(0).is("{")
                || !tokens.get(tokens.size() - 1).is("}")
                || closingParen(tokens, 0) != tokens.size() - 1) {
            return new Config(null, null);
        }
        Map<String, Expr> props = new LinkedHashMap<>();
        Expr unknown = null;
        for (List<JsToken> prop : splitTopLevel(tokens.subList(1, tokens.size() - 1))) {
            if (prop.isEmpty()) {
                continue;
            }
            JsToken key = prop.get(0);
            if (key.is("...")) {
                // 전개는 앞의 속성을 덮어쓸 수 있고 무엇이 들어오는지 모른다
                props.clear();
                unknown = new Expr(List.of(), module, bindings);
                continue;
            }
            boolean named = key.kind() == JsToken.Kind.IDENT || key.kind() == JsToken.Kind.STRING;
            if (named && prop.size() == 1 && key.kind() == JsToken.Kind.IDENT) {
                props.put(key.text(), new Expr(prop, module, bindings));
            } else if (named && prop.size() > 2 && prop.get(1).is(":")) {
                props.put(key.value(), new Expr(prop.subList(2, prop.size()), module, bindings));
            }
        }
        return new Config(props, null, unknown);
    }

    // ---- 래퍼 인라인 (AC10) ----

    private record Site(JsModule module, JsCall call) {}

    /** 같은 파일의 {@code f(…)}와, f를 내보낸 이름으로 import한 선별 파일의 호출. */
    private List<Site> callSites(JsFunction wrapper, JsModule module) {
        List<Site> sites = new ArrayList<>();
        // 부른 곳에서 보이는 선언이 래퍼 자신의 선언일 때만 래퍼 호출이다 (매개변수·지역 변수가 이름을 가리면 다른 함수)
        Optional<JsDecl> own = module.lookup(wrapper.name(), wrapper.start());
        for (JsCall call : module.calls()) {
            if (call.member() == null
                    && call.receiver().equals(wrapper.name())
                    && !wrapper.contains(call.start())
                    && module.lookup(call.receiver(), call.start()).equals(own)) {
                sites.add(new Site(module, call));
            }
        }
        Set<String> exportedAs =
                module.exports().entrySet().stream()
                        .filter(e -> wrapper.name().equals(e.getValue().local()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
        if (exportedAs.isEmpty()) {
            return sites;
        }
        modules.values().stream()
                .filter(other -> other != module)
                .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                .forEach(
                        other -> {
                            for (JsImport imp : other.imports()) {
                                if (imp.target() == null || imp.target() != module.fileId()) {
                                    continue;
                                }
                                imp.bindings()
                                        .forEach(
                                                (local, imported) -> {
                                                    for (JsCall call : other.calls()) {
                                                        boolean direct =
                                                                exportedAs.contains(imported)
                                                                        && call.member() == null
                                                                        && call.receiver()
                                                                                .equals(local);
                                                        boolean viaNamespace =
                                                                imported.equals("*")
                                                                        && call.receiver()
                                                                                .equals(local)
                                                                        && exportedAs.contains(
                                                                                call.member());
                                                        boolean shadowed =
                                                                other.lookup(local, call.start())
                                                                        .isPresent();
                                                        if ((direct || viaNamespace) && !shadowed) {
                                                            sites.add(new Site(other, call));
                                                        }
                                                    }
                                                });
                            }
                        });
        return sites;
    }

    // ---- 결과 (AC11) ----

    private void emit(JsModule from, int line, FeRequest request) {
        ApiMatch match = ApiPathMatcher.match(apis, contextPath, request);
        switch (match.status()) {
            case MATCHED -> out.edge(from.fileId(), match.fileId(), EdgeType.HTTP, line);
            case AMBIGUOUS ->
                    out.unresolved(
                            from.fileId(),
                            line,
                            UnresolvedKind.HTTP_CALL,
                            UnresolvedReason.AMBIGUOUS,
                            request.toString());
            case NONE ->
                    out.unresolved(
                            from.fileId(),
                            line,
                            UnresolvedKind.HTTP_CALL,
                            UnresolvedReason.NO_MATCHING_API,
                            request.toString());
        }
    }

    private void unresolved(JsModule module, JsCall call, UnresolvedReason reason) {
        out.unresolved(
                module.fileId(),
                call.line(),
                UnresolvedKind.HTTP_CALL,
                reason,
                module.excerpt(call.start(), call.end(), MAX_DETAIL));
    }

    // ---- 토큰 도우미 ----

    /** open 위치의 여는 괄호와 짝인 닫는 괄호 위치. 없으면 -1. */
    private static int closingParen(List<JsToken> tokens, int open) {
        int depth = 0;
        for (int i = open; i < tokens.size(); i++) {
            JsToken token = tokens.get(i);
            if (token.is("(") || token.is("[") || token.is("{")) {
                depth++;
            } else if (token.is(")") || token.is("]") || token.is("}")) {
                if (--depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<JsToken> firstArg(List<JsToken> args) {
        return splitTopLevel(args).get(0);
    }

    private static List<List<JsToken>> splitTopLevel(List<JsToken> tokens) {
        List<List<JsToken>> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            JsToken token = tokens.get(i);
            if (token.is("(") || token.is("[") || token.is("{")) {
                depth++;
            } else if (token.is(")") || token.is("]") || token.is("}")) {
                depth--;
            } else if (token.is(",") && depth == 0) {
                parts.add(tokens.subList(start, i));
                start = i + 1;
            }
        }
        parts.add(tokens.subList(start, tokens.size()));
        return parts;
    }
}
