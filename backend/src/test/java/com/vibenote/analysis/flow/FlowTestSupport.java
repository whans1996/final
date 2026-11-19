package com.vibenote.analysis.flow;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * F-07 통합 테스트 공통. 흐름 생성은 커밋 뒤에 시작하므로 롤백을 쓰지 않고 {@code @f07.test} 회원을 지워 정리한다 (analyses →
 * analysis_files·analysis_flows는 FK CASCADE).
 *
 * <p>테스트 코드는 직접 만든다 (eval 데이터 금지, CLAUDE.md 규칙 11).
 */
abstract class FlowTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    static final String CONTROLLER =
            """
            @RestController
            @RequestMapping("/api/todos")
            public class TodoController {
                private final TodoService todoService;
                @PostMapping
                public Todo create(@RequestBody TodoRequest req) { return todoService.create(req); }
                @PatchMapping("/{id}")
                public Todo update() { return null; }
            }
            """;

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUpUsers() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f07.test'");
    }

    long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@f07.test", "vibe1234", name, "JUNIOR"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /**
     * 파일 3개(controller·service·mapper)와 개요(apis)가 있는 분석. apis의 fileId는 controller 파일이다.
     *
     * @param apis {@code "POST /api/todos"} 형태
     */
    long analysis(long userId, String status, String level, List<String> apis) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " created_at) VALUES (?, 'ZIP', 'todo-app', 'todo-app', ?, ?, ?)",
                userId,
                level,
                status,
                LocalDateTime.now(KST).withNano(0));
        long id =
                jdbc.queryForObject(
                        "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
        long controller =
                insertFile(id, "src/main/java/todo/TodoController.java", "controller", CONTROLLER);
        insertFile(
                id,
                "src/main/java/todo/TodoService.java",
                "service",
                "@Service class TodoService { TodoMapper mapper; }");
        insertFile(id, "src/main/java/todo/TodoMapper.java", "mapper", "interface TodoMapper {}");
        StringBuilder json = new StringBuilder();
        for (String api : apis) {
            String[] parts = api.split(" ", 2);
            json.append(json.isEmpty() ? "" : ",")
                    .append(
                            "{\"method\":\"%s\",\"path\":\"%s\",\"fileId\":%d}"
                                    .formatted(parts[0], parts[1], controller));
        }
        jdbc.update(
                "UPDATE analyses SET overview = ? WHERE id = ?",
                "{\"summary\":\"할 일"
                        + " 앱\",\"techStack\":[],\"layers\":[],\"readingOrder\":[],\"apis\":["
                        + json
                        + "]}",
                id);
        return id;
    }

    long insertFile(long analysisId, String path, String layer, String content) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?, ?, ?,"
                        + " ?)",
                analysisId,
                path,
                layer,
                content);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", Long.class, analysisId);
    }

    long insertFlow(long analysisId, String endpoint, String status, String errorCode) {
        jdbc.update(
                "INSERT INTO analysis_flows (analysis_id, endpoint, status, summary, steps,"
                        + " error_code, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                analysisId,
                endpoint,
                status,
                status.equals("DONE") ? "요약" : null,
                status.equals("DONE")
                        ? "[{\"no\":1,\"layer\":\"controller\",\"fileId\":null,\"path\":\"p\",\"symbol\":null,\"title\":\"t\",\"description\":\"d\"}]"
                        : null,
                errorCode,
                LocalDateTime.now(KST).withNano(0));
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analysis_flows WHERE analysis_id = ?", Long.class, analysisId);
    }

    Map<String, Object> flowRow(long flowId) {
        return jdbc.queryForMap("SELECT * FROM analysis_flows WHERE id = ?", flowId);
    }

    List<Map<String, Object>> flowRows(long analysisId) {
        return jdbc.queryForList(
                "SELECT * FROM analysis_flows WHERE analysis_id = ? ORDER BY id", analysisId);
    }
}
