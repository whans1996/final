package com.vibenote.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JSON 요청 본문을 <b>바이트</b> 기준으로 제한한다 (#73).
 *
 * <p>Jackson 한도({@code max-document-length})는 입력 단위를 세므로 UTF-16·UTF-32 본문은 문자 수로 세어 전송 크기 기준 약 2배까지
 * 통과한다. charset을 밝히지 않아도 Jackson이 바이트를 보고 UTF-16을 감지하므로 charset 검사로는 막을 수 없다. 그래서 JSON 요청의 스트림을 세다가
 * 같은 한도를 넘으면 {@link BodyTooLargeException}을 던지고, {@code GlobalExceptionHandler}가 413 {@code
 * COMMON_413}으로 바꾼다. Jackson 한도는 추가 방어로 그대로 둔다. multipart 업로드는 multipart 한도를 따르므로 감싸지 않는다.
 */
@Component
public class JsonBodyLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public JsonBodyLimitFilter(
            @Value("${spring.jackson.factory.constraints.read.max-document-length}")
                    long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(isJson(request) ? new LimitedRequest(request, maxBytes) : request, response);
    }

    private static boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(type)
                    || type.getSubtype().endsWith("+json");
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    /** 본문이 한도를 넘었다. Jackson·Spring이 읽기 실패로 감싸 원인으로 전달한다. */
    public static class BodyTooLargeException extends IOException {
        BodyTooLargeException(long maxBytes) {
            super("JSON 요청 본문이 " + maxBytes + "바이트를 넘었습니다");
        }
    }

    private static class LimitedRequest extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream stream;

        LimitedRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new CountingInputStream(super.getInputStream(), maxBytes);
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static class CountingInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long count;

        CountingInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                add(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = delegate.read(buf, off, len);
            if (n > 0) {
                add(n);
            }
            return n;
        }

        private void add(int n) throws BodyTooLargeException {
            count += n;
            if (count > maxBytes) {
                throw new BodyTooLargeException(maxBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
