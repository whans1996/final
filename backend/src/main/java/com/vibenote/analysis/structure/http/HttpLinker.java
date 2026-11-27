package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.http.UrlEvaluator.Arg;
import com.vibenote.analysis.structure.http.UrlEvaluator.Bindings;
import com.vibenote.analysis.structure.js.JsCall;
import com.vibenote.analysis.structure.js.JsDecl;
import com.vibenote.analysis.structure.js.JsExport;
import com.vibenote.analysis.structure.js.JsFunction;
import com.vibenote.analysis.structure.js.JsImport;
import com.vibenote.analysis.structure.js.JsModule;
import com.vibenote.analysis.structure.js.JsToken;
import com.vibenote.analysis.structure.js.JsTokens;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * FE → API {@code HTTP} 간선 (F-21a AC7~AC11).
 *
 * <p>HTTP 호출은 받는 쪽 R의 {@code R.get/delete/head/options(url, config)}, {@code R.post/put/patch(url,
 * data, config)}, {@code R.request(config)}, {@code R(config)}와 {@code fetch(url, init)}뿐이다. R은
 * axios import 이름, 같은 파일의 {@code const X = axios.create(…)}, 선별 파일에서 import한 이름이 그 파일의 {@code
 * axios.create(…)} 내보내기에 이어질 때만이다. URL을 계산하지 못했고 그 이유가 이름 있는 함수의 매개변수면 그 함수를 부른 곳마다 한 단계 인라인한다.
 *
 * <p>작업 예산을 다 쓰면 남은 HTTP 호출은 URL_UNKNOWN이다. 호출 하나를 계산하다 예상하지 못한 예외가 나도 그 호출만 URL_UNKNOWN이다.
 */
public final class HttpLinker {

    static final int MAX_DETAIL = 80;

    private static final Set<String> URL_CONFIG_SECOND = Set.of("get", "delete", "head", "options");
    private static final Set<String> URL_CONFIG_THIRD = Set.of("post", "put", "patch");

    private static final int MAX_CONFIG_DEPTH = 16;

    private final Map<Long, JsModule> modules;
    private final UrlEvaluator evaluator;
    private final ApiPathMatcher matcher;
    private final StructureLinks out;
    private final WorkBudget budget;

    private HttpLinker(
            Map<Long, JsModule> modules,
            List<ApiEndpoint> apis,
            String contextPath,
            boolean apisIncomplete,
            StructureLinks out,
            WorkBudget budget) {
        this.modules = modules;
        this.evaluator = new UrlEvaluator(modules, budget);
        this.matcher = new ApiPathMatcher(apis, contextPath, apisIncomplete);
        this.out = out;
        this.budget = budget;
    }

    public static void link(
            Map<Long, JsModule> modules,
            List<ApiEndpoint> apis,
            String contextPath,
            boolean apisIncomplete,
            StructureLinks out,
            WorkBudget budget) {
        HttpLinker linker = new HttpLinker(modules, apis, contextPath, apisIncomplete, out, budget);
        modules.values().stream()
                .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                .forEach(
                        module -> {
                            for (JsCall call : module.calls()) {
                                linker.link(call, module);
                            }
                        });
    }

    /** 호출 하나. 예산을 다 썼거나 계산 중 예외가 나면 HTTP 호출인지만 가려 URL_UNKNOWN으로 남긴다 (분류는 색인 조회라 예산을 쓰지 않는다). */
    private void link(JsCall call, JsModule module) {
        try {
            if (!budget.exhausted()) {
                HttpCall http = classify(call, module);
                if (http != null) {
                    budget.spend();
                    handle(http);
                }
                return;
            }
        } catch (RuntimeException | StackOverflowError e) {
            // 아래에서 HTTP 호출인지 다시 가려 URL_UNKNOWN으로 남긴다
        }
        try {
            // 예산을 다 쓴 뒤의 조회는 예산을 쓰지 않는다
            if (classify(call, module) != null) {
                unresolved(module, call, UnresolvedReason.URL_UNKNOWN);
            }
        } catch (RuntimeException | StackOverflowError e) {
            // HTTP 호출인지 가려낼 수 없으면 남기지 않는다
        }
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
        FETCH,

        /** UNKNOWN 선언({@code with} 포함)이 가린 받는 쪽. 가리지 않았다면 HTTP 호출이었으므로 URL_UNKNOWN이다 (5.3절 1). */
        SHADOWED
    }

    /** createArgs는 INSTANCE의 {@code axios.create(…)} 인자 토큰(없으면 빈 목록)이고 그 모듈이 module이다. */
    private record Receiver(ReceiverType type, JsModule module, List<JsToken> createArgs) {}

    private static final Receiver SHADOWED = new Receiver(ReceiverType.SHADOWED, null, List.of());

    private record HttpCall(
            JsCall call, JsModule module, Receiver receiver, Shape shape, int configIndex) {}

    private HttpCall classify(JsCall call, JsModule module) {
        if (call.member() == null) {
            if (call.receiver().equals("fetch") && module.importOf("fetch").isEmpty()) {
                Optional<JsDecl> decl = module.lookup("fetch", call.start());
                if (decl.isEmpty()) {
                    return new HttpCall(
                            call,
                            module,
                            new Receiver(ReceiverType.FETCH, module, List.of()),
                            Shape.FETCH,
                            1);
                }
                if (unknown(decl) && module.lookupKnown("fetch", call.start()).isEmpty()) {
                    return new HttpCall(call, module, SHADOWED, Shape.FETCH, 1);
                }
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
        if (unknown(decl)) {
            return receiver(name, module.lookupKnown(name, offset), module) == null
                    ? null
                    : SHADOWED;
        }
        return receiver(name, decl, module);
    }

    /** 보이는 선언이 decl인 자리의 name. */
    private Receiver receiver(String name, Optional<JsDecl> decl, JsModule module) {
        if (module.axios(name, decl)) {
            return new Receiver(ReceiverType.AXIOS, module, List.of());
        }
        if (decl.isPresent()) {
            return decl.get().kind() == JsDecl.Kind.CONST
                    ? instance(decl.get().init(), module)
                    : null;
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

    private static boolean unknown(Optional<JsDecl> decl) {
        return decl.isPresent() && decl.get().kind() == JsDecl.Kind.UNKNOWN;
    }

    /** 초기값 토큰 목록(선언·내보내기마다 하나) → 인스턴스 판정. 같은 받는 쪽을 부르는 호출마다 초기값을 다시 훑지 않는다. */
    private final Map<List<JsToken>, Optional<Receiver>> instances = new IdentityHashMap<>();

    private Receiver instance(List<JsToken> init, JsModule module) {
        return instances
                .computeIfAbsent(init, k -> Optional.ofNullable(readInstance(init, module)))
                .orElse(null);
    }

    /** {@code A.create(…)}(A는 그 자리에서 보이는 axios) 식이면 인스턴스. 매개변수·지역 변수가 axios 이름을 가리면 axios가 아니다. */
    private static Receiver readInstance(List<JsToken> init, JsModule module) {
        if (init.size() < 5
                || !module.axiosAt(init.get(0).text(), init.get(0).start())
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
     * 객체 리터럴의 속성. props가 null이면 config가 없거나({@link #NONE}) 객체 리터럴로 계산하지 못했다({@link #unresolved}).
     * unknown은 계산하지 못한 config나 {@code ...전개} 뒤에 직접 적지 않은 속성의 값(값을 모르는 식)이다. 그 속성이 들어 있을 수 있으므로 없는
     * 속성으로 보지 않는다 (예: {@code axios.create(getConfig())}의 baseURL은 BASE_URL_UNKNOWN).
     */
    private record Config(Map<String, Expr> props, JsFunction paramOf, Expr unknown) {

        /** config 인자가 없다. 모든 속성이 없다. */
        static final Config NONE = new Config(null, null, null);

        static Config unresolved(JsFunction paramOf) {
            return new Config(null, paramOf, UNKNOWN);
        }

        Expr prop(String name) {
            if (props == null) {
                return unknown;
            }
            return props.containsKey(name) ? props.get(name) : unknown;
        }
    }

    /** 값을 모르는 식. 계산하면 동적 조각이다. */
    private static final Expr UNKNOWN = new Expr(List.of(), null, Bindings.NONE);

    /** detail은 unresolved에 원문 대신 남길 값이다 (null이면 원문). */
    private record Outcome(
            FeRequest request, UnresolvedReason reason, JsFunction paramOf, String detail) {
        static Outcome failed(UnresolvedReason reason, JsFunction paramOf) {
            return new Outcome(null, reason, paramOf, null);
        }
    }

    private void handle(HttpCall http) {
        JsCall call = http.call();
        JsModule module = http.module();
        if (http.receiver() == SHADOWED || hooked(http.receiver())) {
            unresolved(module, call, UnresolvedReason.URL_UNKNOWN);
            return;
        }
        Outcome outcome = compute(http, Bindings.NONE);
        JsFunction wrapper = outcome.paramOf();
        boolean inlinable =
                wrapper != null
                        && wrapper.name() != null
                        && (outcome.request() != null
                                || outcome.reason() == UnresolvedReason.URL_UNKNOWN);
        List<Site> sites =
                inlinable
                        ? sitesByWrapper.computeIfAbsent(wrapper, w -> callSites(w, module))
                        : List.of();
        if (sites.isEmpty() && outcome.request() != null) {
            // 부른 곳이 없으면 매개변수 자리를 와일드카드로 비교한다
            emit(module, call.line(), outcome.request());
            return;
        }
        if (!inlinable) {
            unresolved(module, call, outcome);
            return;
        }
        if (sites.isEmpty() || inlineBudget <= 0) {
            unresolved(module, call, UnresolvedReason.URL_UNKNOWN);
            return;
        }
        for (Site site : sites) {
            budget.spend();
            // 래퍼 안 호출 수 × 부른 곳 수만큼 계산하므로 분석 하나의 인라인 계산 횟수에 상한을 둔다
            if (--inlineBudget < 0) {
                unresolved(module, call, UnresolvedReason.URL_UNKNOWN);
                return;
            }
            if (site.shadowed()) {
                unresolved(site.module(), site.call(), UnresolvedReason.URL_UNKNOWN);
                continue;
            }
            Map<String, Arg> args = new HashMap<>();
            int spread = firstSpread(site.call());
            for (int i = 0; i < wrapper.params().size(); i++) {
                String param = wrapper.params().get(i);
                // 전개 인자부터는 어느 매개변수에 무엇이 들어갈지 모른다. 전개 식 자체는 계산하면 동적 조각, config로는 값을 모름이다
                List<JsToken> arg = site.call().arg(Math.min(i, spread));
                if (!param.isEmpty() && !param.startsWith("...") && arg != null) {
                    args.put(param, new Arg(arg, site.module()));
                }
            }
            Outcome inlined = compute(http, new Bindings(wrapper, args));
            if (inlined.request() != null) {
                emit(site.module(), site.call().line(), inlined.request());
            } else {
                unresolved(site.module(), site.call(), inlined);
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
                method = method(config.prop("method"), http.receiver());
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
        if (path.hostUnknown()) {
            // 어느 서버로 가는지 모른다 (5.3절 5)
            return Outcome.failed(UnresolvedReason.URL_UNKNOWN, value.paramOf());
        }
        List<PathSegment> segments = path.segments();
        if (path.absolute() && !ourHost(path.host())) {
            // 다른 서버로 가는 요청이다. 경로가 같아도 분석한 컨트롤러가 받지 않는다 (5.3절 5)
            String target = path.host() + new FeRequest(null, normalizeDots(segments));
            return new Outcome(
                    null,
                    UnresolvedReason.NO_MATCHING_API,
                    value.paramOf(),
                    method == null ? target : method + " " + target);
        }
        if (!path.absolute() && http.receiver().type() != ReceiverType.FETCH) {
            Expr base = config == null ? null : config.prop("baseURL");
            if (base == null) {
                base = receiverBase(http.receiver());
            }
            if (base != null) {
                UrlValue baseValue = evaluator.eval(base.tokens(), base.module(), base.bindings());
                UrlValue.UrlPath basePath = baseValue.toPath();
                if (baseValue.startsDynamic() || basePath.hostUnknown()) {
                    return Outcome.failed(UnresolvedReason.BASE_URL_UNKNOWN, null);
                }
                List<PathSegment> joined = new ArrayList<>(basePath.segments());
                joined.addAll(segments);
                segments = joined;
            }
        }
        // 경로에 매개변수가 붙었으면 부른 곳마다 인라인할 수 있게 그 함수도 넘긴다 (5.3절 5)
        return new Outcome(
                new FeRequest(method, normalizeDots(segments)), null, value.paramOf(), null);
    }

    /** 이 앱의 서버로 볼 호스트: 로컬 주소와, 선별 파일에서 계산되는 baseURL(axios.defaults·axios.create)의 호스트. */
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private Set<String> baseHosts;

    private boolean ourHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (LOCAL_HOSTS.contains(h)) {
            return true;
        }
        if (baseHosts == null) {
            baseHosts = collectBaseHosts();
        }
        return baseHosts.contains(h);
    }

    /** 분석 하나에서 한 번, 절대 URL이 로컬이 아닐 때만 모은다. */
    private Set<String> collectBaseHosts() {
        Set<String> hosts = new HashSet<>();
        for (JsModule m :
                modules.values().stream()
                        .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                        .toList()) {
            for (List<JsToken> base : m.defaultsBaseUrls()) {
                addHost(hosts, new Expr(base, m, Bindings.NONE));
            }
            List<List<JsToken>> inits = new ArrayList<>();
            for (JsDecl d : m.decls()) {
                if (d.kind() == JsDecl.Kind.CONST && !d.init().isEmpty()) {
                    inits.add(d.init());
                }
            }
            for (JsExport e : m.exports().values()) {
                if (e.expr() != null) {
                    inits.add(e.expr());
                }
            }
            for (List<JsToken> init : inits) {
                budget.spend();
                Receiver instance = instance(init, m);
                if (instance != null) {
                    addHost(
                            hosts,
                            config(instance.createArgs(), instance.module(), Bindings.NONE, 0)
                                    .prop("baseURL"));
                }
            }
        }
        return hosts;
    }

    private void addHost(Set<String> hosts, Expr base) {
        if (base == null || base.module() == null) {
            return;
        }
        UrlValue value = evaluator.eval(base.tokens(), base.module(), base.bindings());
        UrlValue.UrlPath path = value.startsDynamic() ? null : value.toPath();
        if (path != null && path.absolute() && !path.hostUnknown()) {
            hosts.add(path.host().toLowerCase(Locale.ROOT));
        }
    }

    /** baseURL을 붙인 경로의 {@code .}은 빼고 {@code ..}은 앞 세그먼트를 지운다 (HTTP 클라이언트가 보내기 전에 하는 정규화). */
    private static List<PathSegment> normalizeDots(List<PathSegment> segments) {
        List<PathSegment> out = new ArrayList<>();
        for (PathSegment segment : segments) {
            if (".".equals(segment.literal())) {
                continue;
            }
            if ("..".equals(segment.literal())) {
                if (!out.isEmpty()) {
                    out.remove(out.size() - 1);
                }
                continue;
            }
            out.add(segment);
        }
        return List.copyOf(out);
    }

    /**
     * 인스턴스면 {@code create} config의 baseURL, axios면 선별 파일의 {@code axios.defaults.baseURL}. 기본값을 바꾸는
     * 쓰기가 여럿(값이 다름)이거나 모양이 다르면 값을 모른다 (5.3절 5). baseURL 없이 만든 인스턴스는 같은 파일이 axios 기본값을 바꾸면 그 값을 물려받을
     * 수 있어 모른다.
     */
    private Expr receiverBase(Receiver receiver) {
        Taints taints = taints();
        if (receiver.type() == ReceiverType.INSTANCE) {
            if (taints.instanceBase().contains(receiver)) {
                return UNKNOWN;
            }
            Expr base =
                    config(receiver.createArgs(), receiver.module(), Bindings.NONE, 0)
                            .prop("baseURL");
            return base == null && !receiver.module().defaultsBaseUrls().isEmpty() ? UNKNOWN : base;
        }
        if (taints.axiosBase()) {
            return UNKNOWN;
        }
        if (axiosBase == null) {
            axiosBase = readAxiosBase();
        }
        return axiosBase.orElse(null);
    }

    private Optional<Expr> axiosBase;

    /** 선별 파일의 {@code axios.defaults.baseURL} 쓰기. 계산한 값이 모두 같으면 그 값, 다르면 값을 모른다. */
    private Optional<Expr> readAxiosBase() {
        Expr first = null;
        String value = null;
        for (JsModule m :
                modules.values().stream()
                        .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                        .toList()) {
            for (List<JsToken> base : m.defaultsBaseUrls()) {
                budget.spend();
                Expr expr = new Expr(base, m, Bindings.NONE);
                UrlValue v = evaluator.eval(base, m, Bindings.NONE);
                if (first == null) {
                    first = expr;
                    value = v.text();
                } else if (!v.known() || !v.text().equals(value)) {
                    return Optional.of(UNKNOWN);
                }
            }
        }
        return Optional.ofNullable(first);
    }

    // ---- 요청을 바꾸는 설정 (5.3절 5) ----

    /**
     * 선별 파일 어디서든 요청 주소를 바꿀 수 있는 설정. axios와 fetch는 전역 하나라 파일과 상관없다.
     *
     * @param axiosBase axios 기본 baseURL을 {@code X.defaults.baseURL = 식} 말고 다른 모양으로 바꾼다
     * @param instanceBase 기본 baseURL을 바꾸는 인스턴스 (식별자: 인스턴스 판정 메모의 Receiver)
     * @param axiosMethod axios 기본 method를 바꾼다 (값은 계산하지 않는다, #150)
     * @param instanceMethod 기본 method를 바꾸는 인스턴스
     * @param hooked request 인터셉터를 단 받는 쪽 (axios는 {@link #axiosHooked})
     * @param fetchReplaced {@code fetch}를 바꿔 끼운다
     */
    private record Taints(
            boolean axiosBase,
            Set<Receiver> instanceBase,
            boolean axiosMethod,
            Set<Receiver> instanceMethod,
            boolean axiosHooked,
            Set<Receiver> hooked,
            boolean fetchReplaced) {}

    private Taints taints;

    private boolean hooked(Receiver receiver) {
        Taints t = taints();
        return switch (receiver.type()) {
            case AXIOS -> t.axiosHooked();
            case INSTANCE -> t.hooked().contains(receiver);
            case FETCH -> t.fetchReplaced();
            case SHADOWED -> false;
        };
    }

    private Taints taints() {
        if (taints == null) {
            taints = readTaints();
        }
        return taints;
    }

    /** 요청을 바꾸는 쓰기를 모으는 중간 값. */
    private static final class TaintScan {
        boolean axiosBase;
        boolean axiosMethod;
        boolean axiosHooked;
        boolean fetchReplaced;
        final Set<Receiver> instanceBase = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Receiver> instanceMethod = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Receiver> hooked = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private Taints readTaints() {
        TaintScan scan = new TaintScan();
        for (JsModule m :
                modules.values().stream()
                        .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                        .toList()) {
            readTaints(m.tokens(), m, scan);
        }
        return new Taints(
                scan.axiosBase,
                scan.instanceBase,
                scan.axiosMethod,
                scan.instanceMethod,
                scan.axiosHooked,
                scan.hooked,
                scan.fetchReplaced);
    }

    /** 토큰 목록 하나를 훑는다. 템플릿 ${} 안의 식도 실행되므로 그 토큰 목록도 훑는다 (중첩은 토크나이저가 64단계로 막는다). */
    private void readTaints(List<JsToken> t, JsModule m, TaintScan scan) {
        for (int i = 0; i < t.size(); i++) {
            budget.spend();
            JsToken name = t.get(i);
            if (name.kind() == JsToken.Kind.TEMPLATE) {
                for (List<JsToken> part : name.parts()) {
                    readTaints(part, m, scan);
                }
                continue;
            }
            if (name.kind() != JsToken.Kind.IDENT || JsTokens.isPropertyName(t, i)) {
                continue;
            }
            scan.fetchReplaced |= replacesFetch(t, i, m);
            boolean defaults = is(t, i + 1, ".") && is(t, i + 2, "defaults");
            boolean interceptor =
                    is(t, i + 1, ".")
                            && is(t, i + 2, "interceptors")
                            && is(t, i + 3, ".")
                            && is(t, i + 4, "request");
            boolean assigned =
                    is(t, i - 1, "(")
                            && is(t, i - 2, "assign")
                            && is(t, i - 3, ".")
                            && is(t, i - 4, "Object")
                            && defaults
                            && is(t, i + 3, ",");
            boolean base = defaults && (assigned || writesKey(t, i, "baseURL"));
            boolean method = defaults && (assigned || writesKey(t, i, "method"));
            if (!interceptor && !base && !method) {
                continue;
            }
            Receiver receiver = receiver(name.text(), name.start(), m);
            if (receiver == null || receiver == SHADOWED) {
                continue;
            }
            boolean axios = receiver.type() == ReceiverType.AXIOS;
            if (interceptor) {
                if (axios) {
                    scan.axiosHooked = true;
                } else {
                    scan.hooked.add(receiver);
                }
                continue;
            }
            if (base) {
                if (axios) {
                    // 단순한 axios.defaults.baseURL = 식은 defaultsBaseUrls가 모은다
                    scan.axiosBase |= assigned || !plainBaseWrite(t, i);
                } else {
                    scan.instanceBase.add(receiver);
                }
            }
            if (method) {
                if (axios) {
                    scan.axiosMethod = true;
                } else {
                    scan.instanceMethod.add(receiver);
                }
            }
        }
    }

    /**
     * {@code X.defaults} 뒤가 key(baseURL·method)를 바꾸는 쓰기: {@code X.defaults = …}, {@code
     * X.defaults.key <대입>}, {@code delete X.defaults.key}, 계산된 키 {@code X.defaults[k] = …}(k가 다른
     * 문자열이면 제외).
     */
    private static boolean writesKey(List<JsToken> t, int i, String key) {
        if (is(t, i + 3, null) && ASSIGN_OPS.contains(t.get(i + 3).text())) {
            return true;
        }
        if (is(t, i + 3, ".") && is(t, i + 4, key)) {
            return is(t, i - 1, "delete")
                    || (i + 5 < t.size() && ASSIGN_OPS.contains(t.get(i + 5).text()));
        }
        if (is(t, i + 3, "[")) {
            int close = closingParen(t, i + 3);
            boolean otherKey =
                    close == i + 5
                            && t.get(i + 4).kind() == JsToken.Kind.STRING
                            && !t.get(i + 4).value().equals(key);
            return close > 0
                    && !otherKey
                    && close + 1 < t.size()
                    && ASSIGN_OPS.contains(t.get(close + 1).text());
        }
        return false;
    }

    /** {@code X.defaults.baseURL = 식} (defaultsBaseUrls가 모으는 모양). */
    private static boolean plainBaseWrite(List<JsToken> t, int i) {
        return is(t, i + 3, ".") && is(t, i + 4, "baseURL") && is(t, i + 5, "=");
    }

    /** {@code window.fetch =}·{@code globalThis.fetch =}, 또는 그 자리에서 선언이 보이지 않는 {@code fetch =}. */
    private static boolean replacesFetch(List<JsToken> t, int i, JsModule m) {
        String name = t.get(i).text();
        int at;
        if ((name.equals("window") || name.equals("globalThis"))
                && is(t, i + 1, ".")
                && is(t, i + 2, "fetch")) {
            at = i + 3;
        } else if (name.equals("fetch") && m.lookup("fetch", t.get(i).start()).isEmpty()) {
            at = i + 1;
        } else {
            return false;
        }
        return at < t.size() && ASSIGN_OPS.contains(t.get(at).text());
    }

    /** i 위치 토큰이 text인지 (text가 null이면 있는지만). */
    private static boolean is(List<JsToken> t, int i, String text) {
        return i >= 0 && i < t.size() && (text == null || t.get(i).is(text));
    }

    /**
     * config에 method가 없으면 인스턴스는 {@code create} config의 method를 쓴다 (#123). 그 config를 계산하지 못하면
     * method를 모른다(null). 선별 파일이 그 인스턴스의 기본 method를 바꾸거나, axios 기본 method를 바꾸는데 axios·method 없이 만든
     * 인스턴스로 부르면 method를 모른다 (#150).
     */
    private String method(Expr method, Receiver receiver) {
        if (method == null && receiver.type() == ReceiverType.INSTANCE) {
            if (taints().instanceMethod().contains(receiver)) {
                return null;
            }
            method =
                    config(receiver.createArgs(), receiver.module(), Bindings.NONE, 0)
                            .prop("method");
        }
        if (method == null && receiver.type() != ReceiverType.FETCH && taints().axiosMethod()) {
            return null;
        }
        return method(method);
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
        if (tokens == null || tokens.isEmpty()) {
            return Config.NONE;
        }
        if (depth > MAX_CONFIG_DEPTH || tokens.size() > UrlEvaluator.MAX_TOKENS) {
            // 너무 긴 config는 나누지 않는다 (중첩된 요청 config마다 남은 전체를 훑지 않게)
            return Config.unresolved(null);
        }
        if (tokens.size() == 1 && tokens.get(0).kind() == JsToken.Kind.IDENT) {
            JsToken name = tokens.get(0);
            Optional<JsDecl> decl = module.lookup(name.text(), name.start());
            if (decl.isEmpty()) {
                return Config.unresolved(null);
            }
            JsDecl d = decl.get();
            if (d.kind() == JsDecl.Kind.PARAM) {
                // 본문에서 다시 대입하거나 url·method·baseURL 속성을 바꾸면 부른 곳의 인자가 그 값이 아니다
                if (module.reassigned(d) || mutatedConfigs(module).contains(d)) {
                    return Config.unresolved(null);
                }
                if (d.function().equals(bindings.function())) {
                    // 부른 곳이 그 인자를 넘기지 않았으면 config가 없다. 기본값이 있으면 그 값이라 확정하지 않는다
                    Arg arg = bindings.args().get(name.text());
                    if (arg == null) {
                        return d.function().defaulted().contains(name.text())
                                ? Config.unresolved(null)
                                : Config.NONE;
                    }
                    return config(arg.tokens(), arg.module(), Bindings.NONE, depth + 1);
                }
                return Config.unresolved(d.function());
            }
            if (d.kind() != JsDecl.Kind.CONST || mutatedConfigs(module).contains(d)) {
                return Config.unresolved(null);
            }
            if (depth == 0 && bindings == Bindings.NONE) {
                // 호출마다 같은 const config를 다시 나누지 않는다 (속성 식에 인라인 인자가 없을 때만 같은 결과다)
                return constConfigs.computeIfAbsent(d, k -> config(d.init(), module, bindings, 1));
            }
            return config(d.init(), module, bindings, depth + 1);
        }
        if (!tokens.get(0).is("{")
                || !tokens.get(tokens.size() - 1).is("}")
                || closingParen(tokens, 0) != tokens.size() - 1) {
            return Config.unresolved(null);
        }
        Map<String, Expr> props = new LinkedHashMap<>();
        Expr unknown = null;
        for (List<JsToken> prop : splitTopLevel(tokens.subList(1, tokens.size() - 1))) {
            if (prop.isEmpty()) {
                continue;
            }
            JsToken key = prop.get(0);
            boolean named = key.kind() == JsToken.Kind.IDENT || key.kind() == JsToken.Kind.STRING;
            if (named && prop.size() == 1 && key.kind() == JsToken.Kind.IDENT) {
                props.put(key.text(), new Expr(prop, module, bindings));
            } else if ((named || key.kind() == JsToken.Kind.NUMBER)
                    && prop.size() > 2
                    && prop.get(1).is(":")) {
                props.put(key.value(), new Expr(prop.subList(2, prop.size()), module, bindings));
            } else {
                // 전개(...)·계산된 키([k])·접근자(get k())·메서드(k())는 앞의 속성을 덮어쓸 수 있고 무엇이 되는지 모른다
                props.clear();
                unknown = new Expr(List.of(), module, bindings);
            }
        }
        return new Config(props, null, unknown);
    }

    /** config 값에 쓰는 속성. 이 속성을 바꾸는 쓰기가 있으면 초기값을 믿지 않는다. */
    private static final Set<String> CONFIG_KEYS = Set.of("url", "method", "baseURL");

    private static final Set<String> ASSIGN_OPS = JsModule.ASSIGN_OPS;

    /** 대괄호 안 계산된 키로 쓰는 모양을 찾을 때 볼 토큰 수. */
    private static final int MAX_COMPUTED_KEY_TOKENS = 64;

    private final Map<JsModule, Set<JsDecl>> mutatedConfigs = new HashMap<>();

    private final Map<JsDecl, Config> constConfigs = new IdentityHashMap<>();

    /** 분석 하나에서 래퍼를 부른 곳마다 다시 계산하는 횟수의 상한. 넘으면 남은 래퍼 안 호출은 URL_UNKNOWN이다. */
    static final int MAX_INLINE_EVALUATIONS = 10_000;

    private int inlineBudget = MAX_INLINE_EVALUATIONS;

    private final Map<JsFunction, List<Site>> sitesByWrapper = new IdentityHashMap<>();

    /**
     * 파일에서 속성이 바뀌는 const 선언: {@code cfg.url|method|baseURL = …}, {@code cfg[k] = …}, {@code delete
     * cfg.…}, {@code Object.assign(cfg, …)}. const라도 객체는 바뀔 수 있다. 파일마다 한 번 훑는다.
     */
    private Set<JsDecl> mutatedConfigs(JsModule module) {
        return mutatedConfigs.computeIfAbsent(
                module,
                m -> {
                    Set<JsDecl> out = Collections.newSetFromMap(new IdentityHashMap<>());
                    readConfigWrites(m.tokens(), m, out);
                    return out;
                });
    }

    /** 템플릿 ${} 안의 식도 실행되므로 그 토큰 목록도 훑는다 (#124, 요청을 바꾸는 쓰기를 모으는 readTaints와 같음). */
    private void readConfigWrites(List<JsToken> t, JsModule m, Set<JsDecl> out) {
        for (int i = 0; i < t.size(); i++) {
            budget.spend();
            JsToken name = t.get(i);
            if (name.kind() == JsToken.Kind.TEMPLATE) {
                for (List<JsToken> part : name.parts()) {
                    readConfigWrites(part, m, out);
                }
                continue;
            }
            if (name.kind() != JsToken.Kind.IDENT || JsTokens.isPropertyName(t, i)) {
                continue;
            }
            if (writesConfig(t, i)) {
                m.lookup(name.text(), name.start()).ifPresent(out::add);
            }
        }
    }

    private static boolean writesConfig(List<JsToken> t, int i) {
        boolean keyWrite =
                i + 3 < t.size()
                        && t.get(i + 1).is(".")
                        && CONFIG_KEYS.contains(t.get(i + 2).text())
                        && ASSIGN_OPS.contains(t.get(i + 3).text());
        boolean deleted = i > 0 && t.get(i - 1).is("delete");
        boolean assigned =
                i >= 4
                        && t.get(i - 1).is("(")
                        && t.get(i - 2).is("assign")
                        && t.get(i - 3).is(".")
                        && t.get(i - 4).is("Object");
        return keyWrite || deleted || assigned || computedKeyWrite(t, i);
    }

    private static boolean computedKeyWrite(List<JsToken> t, int i) {
        if (i + 1 >= t.size() || !t.get(i + 1).is("[")) {
            return false;
        }
        int depth = 0;
        for (int j = i + 1; j < t.size() && j <= i + MAX_COMPUTED_KEY_TOKENS; j++) {
            if (t.get(j).is("[")) {
                depth++;
            } else if (t.get(j).is("]") && --depth == 0) {
                return j + 1 < t.size() && ASSIGN_OPS.contains(t.get(j + 1).text());
            }
        }
        return false;
    }

    // ---- 래퍼 인라인 (AC10) ----

    /** shadowed면 UNKNOWN 선언이 그 자리의 래퍼 이름을 가려 래퍼 호출인지 모른다 (URL_UNKNOWN). */
    private record Site(JsModule module, JsCall call, boolean shadowed) {}

    /** 첫 {@code ...전개} 인자 위치. 없으면 Integer.MAX_VALUE. */
    private static int firstSpread(JsCall call) {
        for (int i = 0; i < call.args().size(); i++) {
            List<JsToken> arg = call.args().get(i);
            if (!arg.isEmpty() && arg.get(0).is("...")) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** 다른 선별 파일의 import 이름 하나. seq는 (파일 id, import 순서, 이름 순서)로 매긴 순번이다. */
    private record Binding(JsModule module, String local, String imported, int seq) {}

    /** 대상 모듈 → 그 모듈을 가리키는 import 이름들 (가져온 이름별). 분석 하나에서 한 번 만든다. */
    private Map<JsModule, Map<String, List<Binding>>> importers;

    /** 모듈 → 지역 이름 → 그 이름을 내보낸 이름들. */
    private final Map<JsModule, Map<String, List<String>>> exportNames = new IdentityHashMap<>();

    /**
     * 같은 파일의 {@code f(…)}와, f를 내보낸 이름으로 import한 선별 파일의 호출. 호출은 (receiver, member) 색인으로, import는 가져온
     * 이름별 색인으로 찾는다 (래퍼마다 모든 호출·import를 훑지 않게).
     */
    private List<Site> callSites(JsFunction wrapper, JsModule module) {
        List<Site> sites = new ArrayList<>();
        // 부른 곳에서 보이는 선언이 래퍼 자신의 선언일 때만 래퍼 호출이다 (매개변수·지역 변수가 이름을 가리면 다른 함수)
        Optional<JsDecl> own = module.lookup(wrapper.name(), wrapper.start());
        if (own.isPresent() && module.reassigned(own.get())) {
            // 다시 대입되는 let·var 래퍼는 부른 곳에서 이 함수인지 모른다
            return sites;
        }
        for (JsCall call : module.callsTo(wrapper.name(), null)) {
            budget.spend();
            if (wrapper.contains(call.start())) {
                continue;
            }
            Optional<JsDecl> seen = module.lookup(call.receiver(), call.start());
            if (seen.equals(own)) {
                sites.add(new Site(module, call, false));
            } else if (unknown(seen)
                    && module.lookupKnown(call.receiver(), call.start()).equals(own)) {
                sites.add(new Site(module, call, true));
            }
        }
        // 다른 파일이 import로 부를 수 있는 것은 최상위 래퍼뿐이다 (안쪽 함수가 내보낸 이름과 같아도 다른 함수)
        List<String> exportedAs =
                own.isPresent() && own.get().scopeStart() < 0
                        ? exportedAs(module, wrapper.name())
                        : List.of();
        if (exportedAs.isEmpty()) {
            return sites;
        }
        Map<String, List<Binding>> byImported = importersOf(module);
        List<Binding> bindings = new ArrayList<>(byImported.getOrDefault("*", List.of()));
        exportedAs.forEach(name -> bindings.addAll(byImported.getOrDefault(name, List.of())));
        bindings.sort(Comparator.comparingInt(Binding::seq));
        for (Binding b : bindings) {
            List<JsCall> calls;
            if (b.imported().equals("*")) {
                // 네임스페이스 import: ns.f(…)
                calls = new ArrayList<>();
                for (String name : exportedAs) {
                    calls.addAll(b.module().callsTo(b.local(), name));
                }
                calls.sort(Comparator.comparingInt(JsCall::start));
            } else {
                calls = b.module().callsTo(b.local(), null);
            }
            for (JsCall call : calls) {
                budget.spend();
                Optional<JsDecl> seen = b.module().lookup(b.local(), call.start());
                if (seen.isEmpty()) {
                    sites.add(new Site(b.module(), call, false));
                } else if (unknown(seen)
                        && b.module().lookupKnown(b.local(), call.start()).isEmpty()) {
                    sites.add(new Site(b.module(), call, true));
                }
            }
        }
        return sites;
    }

    private List<String> exportedAs(JsModule module, String local) {
        return exportNames
                .computeIfAbsent(
                        module,
                        m -> {
                            Map<String, List<String>> names = new HashMap<>();
                            m.exports().entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .forEach(
                                            e ->
                                                    names.computeIfAbsent(
                                                                    e.getValue().local(),
                                                                    k -> new ArrayList<>())
                                                            .add(e.getKey()));
                            return names;
                        })
                .getOrDefault(local, List.of());
    }

    private Map<String, List<Binding>> importersOf(JsModule module) {
        if (importers == null) {
            importers = new IdentityHashMap<>();
            int seq = 0;
            for (JsModule other :
                    modules.values().stream()
                            .sorted((a, b) -> Long.compare(a.fileId(), b.fileId()))
                            .toList()) {
                for (JsImport imp : other.imports()) {
                    JsModule target = imp.target() == null ? null : modules.get(imp.target());
                    if (target == null || target == other) {
                        continue;
                    }
                    for (Map.Entry<String, String> e : imp.bindings().entrySet()) {
                        budget.spend();
                        importers
                                .computeIfAbsent(target, k -> new HashMap<>())
                                .computeIfAbsent(e.getValue(), k -> new ArrayList<>())
                                .add(new Binding(other, e.getKey(), e.getValue(), seq++));
                    }
                }
            }
        }
        return importers.getOrDefault(module, Map.of());
    }

    // ---- 결과 (AC11) ----

    private void emit(JsModule from, int line, FeRequest request) {
        ApiMatch match = matcher.match(request, budget);
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

    private void unresolved(JsModule module, JsCall call, Outcome outcome) {
        if (outcome.detail() == null) {
            unresolved(module, call, outcome.reason());
            return;
        }
        out.unresolved(
                module.fileId(),
                call.line(),
                UnresolvedKind.HTTP_CALL,
                outcome.reason(),
                outcome.detail().length() > MAX_DETAIL
                        ? outcome.detail().substring(0, MAX_DETAIL)
                        : outcome.detail());
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
