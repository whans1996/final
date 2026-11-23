package com.vibenote.analysis.fetch;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * 테스트용 가짜 GitHub·GitLab 서버 (F-05b 8절). 경로마다 응답을 정하고 받은 요청을 기록한다. 외부 네트워크를 쓰지 않는다.
 *
 * <p>{@code localhost}(루프백)에만 바인딩한다. 요청 기록의 경로는 인코딩된 그대로(raw)다.
 */
public final class FakeRepositoryServer implements AutoCloseable {

    /** 받은 요청 하나. */
    public record Recorded(String method, String rawPath, Headers headers) {}

    /** 응답을 만든다. */
    @FunctionalInterface
    public interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private final HttpServer server;
    private final Map<String, Handler> handlers = new ConcurrentHashMap<>();
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    public FakeRepositoryServer() {
        try {
            server =
                    HttpServer.create(
                            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext(
                "/",
                exchange -> {
                    String rawPath = exchange.getRequestURI().getRawPath();
                    requests.add(
                            new Recorded(
                                    exchange.getRequestMethod(),
                                    rawPath,
                                    exchange.getRequestHeaders()));
                    Handler handler = handlers.get(rawPath);
                    try (exchange) {
                        if (handler == null) {
                            exchange.sendResponseHeaders(404, -1);
                        } else {
                            handler.handle(exchange);
                        }
                    } catch (IOException e) {
                        // 클라이언트가 먼저 끊은 경우(시간 초과·상한 초과 테스트)
                    }
                });
        server.start();
    }

    /** {@code http://localhost:{port}}. */
    public String base() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public FakeRepositoryServer on(String rawPath, Handler handler) {
        handlers.put(rawPath, handler);
        return this;
    }

    public List<Recorded> requests() {
        return List.copyOf(requests);
    }

    public void reset() {
        handlers.clear();
        requests.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ───── 자주 쓰는 응답 ─────

    public static Handler body(int status, byte[] body) {
        return exchange -> {
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        };
    }

    public static Handler zip(byte[] body) {
        return exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            body(200, body).handle(exchange);
        };
    }

    /** Content-Length 없이(chunked) 보낸다. */
    public static Handler chunked(byte[] body) {
        return exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        };
    }

    public static Handler status(int status) {
        return status(status, Map.of());
    }

    public static Handler status(int status, Map<String, String> headers) {
        return exchange -> {
            headers.forEach(exchange.getResponseHeaders()::set);
            exchange.sendResponseHeaders(status, -1);
        };
    }

    public static Handler redirect(int status, String location) {
        return status(status, location == null ? Map.of() : Map.of("Location", location));
    }
}
