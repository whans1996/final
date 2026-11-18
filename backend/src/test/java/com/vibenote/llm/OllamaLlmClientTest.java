package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vibenote.common.Level;
import java.net.ConnectException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaLlmClientTest {

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final OllamaLlmClient client =
            new OllamaLlmClient(
                    builder,
                    new LlmProperties.Ollama(
                            "http://ollama.test:11434", "qwen-test", Duration.ofSeconds(5)));

    final LlmRequest request = new LlmRequest(LlmStage.FILE, Level.JUNIOR, "설명해 줘");

    @Test
    void AC3_JSON_형식_비스트리밍으로_호출하고_본문과_토큰을_돌려준다() {
        server.expect(requestTo("http://ollama.test:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content()
                                .json(
                                        """
                                        {"model":"qwen-test","prompt":"설명해 줘","format":"json","stream":false}
                                        """,
                                        true))
                .andRespond(
                        withSuccess(
                                """
                                {"model":"qwen-test","response":"{\\"summary\\":\\"요약\\"}","done":true,
                                 "prompt_eval_count":21,"eval_count":43}
                                """,
                                MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request);

        server.verify();
        assertThat(response)
                .isEqualTo(new LlmResponse("{\"summary\":\"요약\"}", "qwen-test", 21, 43));
    }

    @Test
    void AC5_실패는_LlmUnavailableException이다() {
        server.expect(requestTo("http://ollama.test:11434/api/generate"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Ollama 500");

        server.reset();
        server.expect(requestTo("http://ollama.test:11434/api/generate"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Ollama ConnectException");
    }
}
