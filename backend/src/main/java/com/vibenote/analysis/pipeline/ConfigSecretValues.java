package com.vibenote.analysis.pipeline;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * 설정 파일을 실제 파서로 읽어 비밀 키 아래의 문자열 값을 모은다 ({@link SecretRedactor}의 2차 마스킹). 줄 단위 처리가 놓치는 형식(다음 줄 값, 공백
 * 구분 properties, 하위 매핑 등)을 덮는다. 해석에 실패하면 모은 데까지만 돌려준다.
 *
 * <p>값은 공백 단위 토큰으로 나눈다 (여러 줄 값은 본문에 그대로 나타나지 않으므로). 기본값 없는 {@code ${NAME}}은 빼고 {@code ${NAME:기본값}}은
 * 기본값만 넣는다. 숫자·불리언은 넣지 않는다 ({@code token-validity: 86400} 때문에 같은 숫자가 파일 전체에서 가려지지 않게). key 규칙으로만
 * 비밀인 키 아래 값에 {@link SecretRedactor#MIN_KEY_SUFFIX_TOKEN}자 미만 토큰이 있으면 그 값은 본문 전체가 아니라 YAML 값이 있는
 * 범위({@link Located})를 통째로 가린다 (#16).
 */
final class ConfigSecretValues {

    /** 값을 모으는 문맥. 아래로 내려가며 더 강한 쪽으로만 바뀐다. */
    private enum Context {
        PLAIN,
        KEY_SUFFIX,
        KEYWORD
    }

    /** 2차 마스킹 대상. tokens는 본문 전체에서 가리고, located는 범위를 통째로 가린다. */
    record Found(Set<String> tokens, List<Located> located) {}

    /** 통째로 가릴 본문 [start, end) 범위 (YAML 값 하나). */
    record Located(int start, int end) {}

    private ConfigSecretValues() {}

    static Found fromYaml(String content) {
        // SafeConstructor: 임의 클래스 생성 금지. LoaderOptions 기본값이 별칭 수·중첩 깊이를 제한한다 (billion laughs)
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        YamlCollector collector = new YamlCollector(content);
        try {
            // 값의 위치가 필요해 객체로 만들지 않고 노드 트리(compose)를 읽는다
            for (Node document : yaml.composeAll(new StringReader(content))) {
                collector.collect(document, Context.PLAIN);
            }
        } catch (RuntimeException e) {
            // 해석할 수 없는 YAML: 줄 단위 결과만 쓴다
        }
        return new Found(collector.tokens, collector.located);
    }

    /** properties는 값이 키와 같은 줄(또는 줄 이음)에만 있어 줄 단위 1차가 원래 자리를 가린다. 그래서 짧은 토큰은 버린다. */
    static Found fromProperties(String content) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException | IllegalArgumentException e) {
            return new Found(Set.of(), List.of());
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String name : properties.stringPropertyNames()) {
            splitTokens(
                    properties.getProperty(name),
                    contextOf(name, Context.PLAIN),
                    tokens,
                    new LinkedHashSet<>());
        }
        return new Found(tokens, List.of());
    }

    /**
     * 가린 뒤의 properties를 다시 읽어 key 규칙 비밀의 짧은 값이 남았는지 본다. 유니코드 이스케이프나 줄 이음으로 쓴 키는 줄 단위 1차가 알아보지 못하므로,
     * 남았으면 파일 전체를 가리게 한다 (#16, Codex 리뷰).
     */
    static boolean hasShortSuffixSecrets(String redactedProperties) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(redactedProperties));
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
        for (String name : properties.stringPropertyNames()) {
            Set<String> local = new LinkedHashSet<>();
            splitTokens(
                    properties.getProperty(name),
                    contextOf(name, Context.PLAIN),
                    new LinkedHashSet<>(),
                    local);
            if (!local.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Context contextOf(String key, Context parent) {
        if (parent == Context.KEYWORD || SecretRedactor.hasSecretKeyword(key)) {
            return Context.KEYWORD;
        }
        if (parent == Context.KEY_SUFFIX || SecretRedactor.endsWithKeyWord(key)) {
            return Context.KEY_SUFFIX;
        }
        return Context.PLAIN;
    }

    /** 값을 토큰으로 나눠 본문 전체에서 가릴 것(global)과 제자리에서만 가릴 것(local)으로 나눈다. */
    private static void splitTokens(
            String value, Context context, Set<String> global, Set<String> local) {
        if (context == Context.PLAIN) {
            return;
        }
        String secret = SecretRedactor.secretPartOf(value);
        if (secret == null) {
            return;
        }
        for (String token : secret.strip().split("\\s+")) {
            if (token.isEmpty() || token.equals(SecretRedactor.REDACTED)) {
                continue;
            }
            if (context == Context.KEYWORD
                    || token.length() >= SecretRedactor.MIN_KEY_SUFFIX_TOKEN) {
                global.add(token);
            } else {
                local.add(token);
            }
        }
    }

    private static final class YamlCollector {
        private final String content;
        private final Set<String> tokens = new LinkedHashSet<>();
        private final List<Located> located = new ArrayList<>();

        /**
         * 문맥별 방문 기록. 비밀 키 밖에서 먼저 본 별칭 노드를 비밀 키 아래에서 다시 만나면 값을 모아야 하므로 문맥마다 따로 센다. 노드당 최대 문맥
         * 수(3)만큼이라 순환해도 끝난다.
         */
        private final Map<Context, Set<Node>> visited = new EnumMap<>(Context.class);

        YamlCollector(String content) {
            this.content = content;
        }

        /** 별칭(&a ... *a)으로 같은 노드가 다시 나오거나 순환하면 문맥마다 한 번만 본다. */
        void collect(Node node, Context context) {
            if (node instanceof MappingNode map) {
                if (!first(map, context)) {
                    return;
                }
                for (NodeTuple entry : map.getValue()) {
                    // !!set은 원소를 키(값은 null)로 담는다. loadAll 때처럼 원소를 값으로 본다 (Codex 리뷰)
                    if (Tag.SET.equals(map.getTag())) {
                        collect(entry.getKeyNode(), context);
                    }
                    collect(entry.getValueNode(), keyContext(entry.getKeyNode(), context));
                }
            } else if (node instanceof SequenceNode sequence) {
                if (!first(sequence, context)) {
                    return;
                }
                for (Node item : sequence.getValue()) {
                    collect(item, context);
                }
            } else if (node instanceof ScalarNode scalar && Tag.STR.equals(scalar.getTag())) {
                Set<String> local = new LinkedHashSet<>();
                splitTokens(scalar.getValue(), context, tokens, local);
                if (!local.isEmpty()) {
                    located.add(
                            new Located(
                                    charIndex(scalar.getStartMark().getIndex()),
                                    charIndex(scalar.getEndMark().getIndex())));
                }
            }
        }

        /** 코드 포인트 위치 → UTF-16 위치. BMP 밖 문자가 없으면 같은 값이다 (Mark 인덱스는 코드 포인트 기준). */
        private int[] charOffsets;

        private boolean offsetsReady;

        private int charIndex(int codePointIndex) {
            if (!offsetsReady) {
                offsetsReady = true;
                int count = content.codePointCount(0, content.length());
                if (count != content.length()) {
                    // 값마다 앞에서부터 다시 세면 값 수 × 본문 길이가 되므로 한 번만 만든다 (Codex 리뷰)
                    charOffsets = new int[count + 1];
                    int at = 0;
                    for (int i = 0; i < count; i++) {
                        charOffsets[i] = at;
                        at += Character.charCount(content.codePointAt(at));
                    }
                    charOffsets[count] = at;
                }
            }
            return charOffsets == null ? codePointIndex : charOffsets[codePointIndex];
        }

        /**
         * 키 노드의 문맥. 목록·매핑을 키로 쓰면({@code ? [password]}) 안의 이름 중 가장 강한 문맥을 쓴다. loadAll 때 키 전체를 문자열로
         * 검사하던 것을 이어 받는다 (Codex 리뷰).
         */
        private static Context keyContext(Node key, Context parent) {
            if (key instanceof ScalarNode scalar) {
                return contextOf(scalar.getValue(), parent);
            }
            Context strongest = parent;
            for (String name : scalarsIn(key, Collections.newSetFromMap(new IdentityHashMap<>()))) {
                Context context = contextOf(name, parent);
                if (context.compareTo(strongest) > 0) {
                    strongest = context;
                }
            }
            return strongest;
        }

        /** 노드 아래 스칼라 값 전부. 별칭이 순환해도 노드마다 한 번만 본다. */
        private static List<String> scalarsIn(Node node, Set<Node> seen) {
            List<String> names = new ArrayList<>();
            if (!seen.add(node)) {
                return names;
            }
            if (node instanceof ScalarNode scalar) {
                names.add(scalar.getValue());
            } else if (node instanceof SequenceNode sequence) {
                sequence.getValue().forEach(item -> names.addAll(scalarsIn(item, seen)));
            } else if (node instanceof MappingNode map) {
                for (NodeTuple entry : map.getValue()) {
                    names.addAll(scalarsIn(entry.getKeyNode(), seen));
                    names.addAll(scalarsIn(entry.getValueNode(), seen));
                }
            }
            return names;
        }

        private boolean first(Node node, Context context) {
            return visited.computeIfAbsent(
                            context, c -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(node);
        }
    }
}
