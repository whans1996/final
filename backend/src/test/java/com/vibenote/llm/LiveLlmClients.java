package com.vibenote.llm;

import org.springframework.web.client.RestClient;

/** 다른 패키지의 수동 실행 하네스(예: {@code prompteval.PromptEvalRunner})가 실제 클라이언트를 앱과 같은 타임아웃으로 만들 때 쓴다. */
public final class LiveLlmClients {

    private LiveLlmClients() {}

    public static LlmClient gemini(LlmProperties.Gemini props) {
        return new GeminiLlmClient(
                LlmConfig.withTimeout(RestClient.builder(), props.timeout()), props);
    }
}
