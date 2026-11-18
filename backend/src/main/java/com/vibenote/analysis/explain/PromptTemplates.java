package com.vibenote.analysis.explain;

import com.vibenote.common.Level;
import com.vibenote.llm.LlmStage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 수준별 프롬프트 템플릿 ({@code classpath:prompts/<stage>/<level>.md}). {@code {{name}}} 자리를 값으로 한 번에 바꾼다. 값
 * 안의 {@code {{...}}}(예: Vue 템플릿)는 다시 바꾸지 않는다. 읽은 템플릿은 캐시한다.
 */
@Component
public class PromptTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String render(LlmStage stage, Level level, Map<String, String> values) {
        String path = path(stage, level);
        String template = cache.computeIfAbsent(path, PromptTemplates::load);
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder(template.length());
        while (m.find()) {
            String value = values.get(m.group(1));
            if (value == null) {
                throw new IllegalStateException(path + "의 {{" + m.group(1) + "}} 값이 없습니다.");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    static String path(LlmStage stage, Level level) {
        return "prompts/" + stage.key() + "/" + level.name().toLowerCase(Locale.ROOT) + ".md";
    }

    private static String load(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("프롬프트 템플릿이 없습니다: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
