package com.vibenote.analysis.pipeline;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * 설정 파일을 실제 파서로 읽어 비밀 키 아래의 문자열 값을 모은다 ({@link SecretRedactor}의 2차 마스킹). 줄 단위 처리가 놓치는 형식(다음 줄 값, 공백
 * 구분 properties, 하위 매핑 등)을 덮는다. 해석에 실패하면 모은 데까지만 돌려준다.
 *
 * <p>값은 공백 단위 토큰으로 나눈다 (여러 줄 값은 본문에 그대로 나타나지 않으므로). 기본값 없는 {@code ${NAME}}은 빼고 {@code ${NAME:기본값}}은
 * 기본값만 넣는다. 숫자·불리언은 넣지 않는다 ({@code token-validity: 86400} 때문에 같은 숫자가 파일 전체에서 가려지지 않게).
 */
final class ConfigSecretValues {

    private ConfigSecretValues() {}

    static Set<String> fromYaml(String content) {
        Set<String> values = new LinkedHashSet<>();
        // SafeConstructor: 임의 클래스 생성 금지. LoaderOptions 기본값이 별칭 수·중첩 깊이를 제한한다 (billion laughs)
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Visited visited = new Visited();
        try {
            for (Object document : yaml.loadAll(content)) {
                collect(document, false, values, visited);
            }
        } catch (RuntimeException e) {
            // 해석할 수 없는 YAML: 줄 단위 결과만 쓴다
        }
        return values;
    }

    static Set<String> fromProperties(String content) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException | IllegalArgumentException e) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String name : properties.stringPropertyNames()) {
            if (SecretRedactor.isSecretKey(name)) {
                addTokens(properties.getProperty(name), values);
            }
        }
        return values;
    }

    /** 별칭(&a ... *a)으로 같은 노드가 다시 나오거나 순환하면 문맥(비밀 키 아래인지)마다 한 번만 본다. */
    private static void collect(
            Object node, boolean underSecret, Set<String> values, Visited visited) {
        if (node instanceof Map<?, ?> map) {
            if (!visited.first(map, underSecret)) {
                return;
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                boolean secret =
                        underSecret
                                || (entry.getKey() != null
                                        && SecretRedactor.isSecretKey(
                                                String.valueOf(entry.getKey())));
                collect(entry.getValue(), secret, values, visited);
            }
        } else if (node instanceof Collection<?> collection) {
            if (!visited.first(collection, underSecret)) {
                return;
            }
            for (Object item : collection) {
                collect(item, underSecret, values, visited);
            }
        } else if (underSecret && node instanceof String value) {
            addTokens(value, values);
        }
    }

    private static void addTokens(String value, Set<String> values) {
        String secret = SecretRedactor.secretPartOf(value);
        if (secret == null) {
            return;
        }
        for (String token : secret.strip().split("\\s+")) {
            if (!token.isEmpty() && !token.equals(SecretRedactor.REDACTED)) {
                values.add(token);
            }
        }
    }

    /**
     * 비밀 키 밖·안 문맥별 방문 기록. 비밀 키 밖에서 먼저 본 별칭 노드를 비밀 키 아래에서 다시 만나면 값을 모아야 하므로 문맥마다 따로 센다. 노드당 최대 두 번이라
     * 순환해도 끝난다.
     */
    private static final class Visited {
        private final Set<Object> plain = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Object> secret = Collections.newSetFromMap(new IdentityHashMap<>());

        boolean first(Object node, boolean underSecret) {
            return (underSecret ? secret : plain).add(node);
        }
    }
}
