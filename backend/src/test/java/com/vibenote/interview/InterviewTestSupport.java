package com.vibenote.interview;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
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
 * I-1a 통합 테스트 공통. 생성은 커밋 뒤에 시작하므로 롤백을 쓰지 않고 {@code @i1a.test} 회원을 지워 정리한다 (analyses →
 * analysis_interviews → interview_questions는 FK CASCADE). 카드는 이름 접두사 {@code I1A-}로 지운다.
 *
 * <p>테스트 코드는 직접 만든다 (eval 데이터 금지, CLAUDE.md 규칙 11).
 */
abstract class InterviewTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    static final String CONTROLLER =
            """
            @RestController
            @RequestMapping("/api/todos")
            public class TodoController {
                @PostMapping
                public Todo create(@RequestBody TodoRequest req) { return todoService.create(req); }
            }\
            """;

    static final String SERVICE =
            """
            @Service
            public class TodoService {
                @Transactional
                public Todo create(TodoRequest req) { return mapper.insert(req); }
            }\
            """;

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@i1a.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'I1A-%'");
    }

    long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@i1a.test", "vibe1234", name, "JUNIOR"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /** 선별 파일 2개(controller 6줄·service 5줄, 중요도 순)와 개요가 있는 분석. 선별 안 된 파일 1개도 넣는다. */
    long analysis(long userId, String status, String level) {
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, source_type, project_name, title,"
                    + " level, status, created_at) VALUES (?, ?, 'ZIP', 'todo-app', 'todo-app', ?,"
                    + " ?, ?)",
                userId,
                TestProjects.zip(jdbc, userId),
                level,
                status,
                LocalDateTime.now(KST).withNano(0));
        long id =
                jdbc.queryForObject(
                        "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
        long controller =
                insertFile(
                        id,
                        "src/main/java/todo/TodoController.java",
                        "controller",
                        90,
                        true,
                        CONTROLLER);
        insertFile(id, "src/main/java/todo/TodoService.java", "service", 80, true, SERVICE);
        insertFile(id, "src/main/java/todo/Unselected.java", "other", 99, false, "class X {}");
        jdbc.update(
                "UPDATE analyses SET overview = ? WHERE id = ?",
                ("{\"summary\":\"할 일 앱\",\"techStack\":[\"Spring Boot\"],\"layers\":[],"
                                + "\"readingOrder\":[],\"apis\":[{\"method\":\"POST\","
                                + "\"path\":\"/api/todos\",\"fileId\":%d}]}")
                        .formatted(controller),
                id);
        return id;
    }

    long insertFile(
            long analysisId,
            String path,
            String layer,
            int importance,
            boolean selected,
            String content) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, importance, selected,"
                        + " content) VALUES (?, ?, ?, ?, ?, ?)",
                analysisId,
                path,
                layer,
                importance,
                selected,
                content);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", Long.class, analysisId);
    }

    List<Long> fileIds(long analysisId) {
        return jdbc.queryForList(
                "SELECT id FROM analysis_files WHERE analysis_id = ? ORDER BY id",
                Long.class,
                analysisId);
    }

    long insertInterview(long analysisId, String status, int regenerations, String errorCode) {
        jdbc.update(
                "INSERT INTO analysis_interviews (analysis_id, status, error_code, regenerations,"
                        + " generated_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                analysisId,
                status,
                errorCode,
                regenerations,
                status.equals("DONE") ? LocalDateTime.now(KST).withNano(0) : null,
                LocalDateTime.now(KST).withNano(0),
                LocalDateTime.now(KST).withNano(0));
        return jdbc.queryForObject(
                "SELECT id FROM analysis_interviews WHERE analysis_id = ?", Long.class, analysisId);
    }

    void insertQuestion(long interviewId, int no, long fileId, String conceptIdsJson) {
        jdbc.update(
                "INSERT INTO interview_questions (interview_id, no, category, question, key_points,"
                        + " evidence, concept_ids) VALUES (?, ?, 'FLOW', ?, '[\"포인트\"]', ?, ?)",
                interviewId,
                no,
                "질문 " + no,
                "[{\"fileId\":%d,\"path\":\"p\",\"startLine\":1,\"endLine\":2}]".formatted(fileId),
                conceptIdsJson);
    }

    /** 이름이 {@code I1A-}로 시작하는 카드. */
    long concept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source) VALUES"
                        + " (?, 'spring', 'JUNIOR', '요약', ?, 'SEED')",
                "I1A-" + name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    void link(long analysisId, long conceptId, String contextNote) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, context_note) VALUES (?,"
                        + " ?, ?)",
                analysisId,
                conceptId,
                contextNote);
    }

    Map<String, Object> interviewRow(long analysisId) {
        return jdbc.queryForMap(
                "SELECT * FROM analysis_interviews WHERE analysis_id = ?", analysisId);
    }

    int interviewCount(long analysisId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM analysis_interviews WHERE analysis_id = ?",
                Integer.class,
                analysisId);
    }

    List<Map<String, Object>> questionRows(long interviewId) {
        return jdbc.queryForList(
                "SELECT * FROM interview_questions WHERE interview_id = ? ORDER BY no",
                interviewId);
    }
}
