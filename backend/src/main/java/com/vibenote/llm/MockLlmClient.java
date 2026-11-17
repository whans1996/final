package com.vibenote.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 LLM. 외부 호출 없이 {@code classpath:llm-fixtures/<stage>.json}을 그대로 돌려준다.
 *
 * <p>결정적이어야 하므로 랜덤을 쓰지 않는다. fixture는 dev·demo 데이터로만 만든다 (eval 데이터 금지, CLAUDE.md 11번).
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    static final String MODEL = "mock";

    @Override
    public LlmResponse complete(LlmRequest request) {
        String path = "llm-fixtures/" + request.stage().key() + ".json";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Mock LLM fixture가 없습니다: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            return new LlmResponse(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8), MODEL, 0, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
