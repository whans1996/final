package com.vibenote.analysis.explain;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 정해 둔 응답을 차례로 돌려주고 요청을 기록하는 가짜 LLM. 응답 대신 예외를 넣을 수 있다. */
class FakeLlmClient implements LlmClient {

    final List<LlmRequest> requests = new ArrayList<>();
    private final Deque<Object> answers = new ArrayDeque<>();

    FakeLlmClient answer(String content) {
        answers.add(content);
        return this;
    }

    FakeLlmClient fail(BusinessException e) {
        answers.add(e);
        return this;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        requests.add(request);
        Object next = answers.poll();
        if (next == null) {
            throw new IllegalStateException("준비한 응답이 없습니다");
        }
        if (next instanceof BusinessException e) {
            throw e;
        }
        return new LlmResponse((String) next, "fake", 10, 20);
    }
}
