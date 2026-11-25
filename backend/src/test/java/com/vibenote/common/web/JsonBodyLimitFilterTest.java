package com.vibenote.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.common.web.JsonBodyLimitFilter.BodyTooLargeException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * JSON 본문 바이트 상한 필터가 감싸는 대상을 확인한다 (#73). multipart 업로드는 MockMvc가 본문 스트림을 거치지 않아 통합 테스트로는 확인할 수 없으므로
 * 필터를 직접 부른다.
 */
class JsonBodyLimitFilterTest {

    private static final int LIMIT = 16;

    private final JsonBodyLimitFilter filter = new JsonBodyLimitFilter(LIMIT);

    /** 필터를 거친 요청의 본문을 끝까지 읽는다. 한도를 넘으면 필터가 던진 예외가 그대로 나온다. */
    private byte[] readThroughFilter(String contentType, byte[] body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/x");
        request.setContentType(contentType);
        request.setContent(body);
        byte[][] read = new byte[1][];
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest req, ServletResponse res)
                            throws IOException {
                        read[0] = req.getInputStream().readAllBytes();
                    }
                };
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return read[0];
    }

    @Test
    void multipart_본문은_한도를_넘어도_감싸지_않는다() throws Exception {
        byte[] body = new byte[LIMIT * 4];

        assertThat(readThroughFilter("multipart/form-data; boundary=x", body)).hasSize(body.length);
    }

    @Test
    void JSON이_아닌_본문은_감싸지_않는다() throws Exception {
        byte[] body = new byte[LIMIT * 4];

        assertThat(readThroughFilter("text/plain", body)).hasSize(body.length);
    }

    @Test
    void JSON_본문은_한도까지_읽히고_넘으면_BodyTooLargeException() throws Exception {
        assertThat(readThroughFilter("application/json", new byte[LIMIT])).hasSize(LIMIT);
        assertThatThrownBy(() -> readThroughFilter("application/json", new byte[LIMIT + 1]))
                .isInstanceOf(BodyTooLargeException.class);
    }

    @Test
    void plus_json_본문도_한도를_넘으면_BodyTooLargeException() {
        assertThatThrownBy(
                        () ->
                                readThroughFilter(
                                        "application/merge-patch+json", new byte[LIMIT + 1]))
                .isInstanceOf(BodyTooLargeException.class);
    }
}
