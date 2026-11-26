package com.vibenote.llm;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 공급자를 순서대로 시도한다. 앞 공급자가 {@link LlmUnavailableException}이면 재시도 없이 다음으로 넘어가고, 모두 실패하면 {@code
 * BusinessException(LLM_503)}을 던진다 (F-00 AC4·AC5).
 *
 * <p>provider=gms는 {@code [GMS, Ollama]}, provider=ollama는 {@code [Ollama]}, provider=gemini는 폴백 없이
 * {@code [Gemini]}로 조립한다 ({@link LlmConfig}, F-27). 그 밖의 예외(버그)는 폴백하지 않고 그대로 던진다.
 */
public class FallbackLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmClient.class);

    private final List<LlmClient> chain;

    public FallbackLlmClient(List<LlmClient> chain) {
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("공급자가 하나 이상 있어야 합니다");
        }
        this.chain = List.copyOf(chain);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        for (int i = 0; i < chain.size(); i++) {
            try {
                return chain.get(i).complete(request);
            } catch (LlmUnavailableException e) {
                // 메시지는 "<공급자> <상태>"뿐이라 키·프롬프트가 없다
                boolean last = i == chain.size() - 1;
                log.warn(
                        "LLM 호출 실패 ({}, stage={}){}",
                        e.getMessage(),
                        request.stage().key(),
                        last ? "" : " → 다음 공급자로 전환");
            }
        }
        throw new BusinessException(ErrorCode.LLM_503);
    }

    List<LlmClient> chain() {
        return chain;
    }
}
