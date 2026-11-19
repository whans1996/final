package com.vibenote.analysis.explain;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * LLM 응답을 JSON으로 읽고 검증한다. JSON이 아니거나 검증에 실패하면 <b>같은 요청을 1회만</b> 다시 보낸다 (F-06 AC5, NF-04). 호출 자체의
 * 실패({@code BusinessException(LLM_503)} 등)는 재시도하지 않고 그대로 던진다.
 */
public final class LlmJsonCall {

    private static final Logger log = LoggerFactory.getLogger(LlmJsonCall.class);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    static final int MAX_ATTEMPTS = 2;

    private LlmJsonCall() {}

    /**
     * @param parse 검증·정리. 형식이 틀리면 {@link InvalidResponseException}을 던진다
     * @return 두 번 다 틀리면 empty
     */
    public static <T> Optional<T> call(
            LlmClient llm, LlmRequest request, Function<JsonNode, T> parse) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String content = llm.complete(request).content();
            try {
                if (content == null) {
                    throw new InvalidResponseException("응답 없음");
                }
                return Optional.of(parse.apply(JSON.readTree(content)));
            } catch (JacksonException | InvalidResponseException e) {
                log.warn(
                        "LLM 응답 형식 오류 (stage={}, analysis={}, 시도 {}/{}): {}",
                        request.stage().key(),
                        request.analysisId(),
                        attempt,
                        MAX_ATTEMPTS,
                        e instanceof InvalidResponseException ? e.getMessage() : "JSON 아님");
            }
        }
        return Optional.empty();
    }

    /** 응답이 5.3절 스키마와 맞지 않음. */
    public static final class InvalidResponseException extends RuntimeException {
        public InvalidResponseException(String message) {
            super(message, null, false, false);
        }
    }

    /** 필수 문자열 필드. 없거나 문자열이 아니면 형식 오류. */
    public static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            throw new InvalidResponseException(field + "가 문자열이 아님");
        }
        return value.asString();
    }

    public static String cut(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
