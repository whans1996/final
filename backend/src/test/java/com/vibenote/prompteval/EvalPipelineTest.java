package com.vibenote.prompteval;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import com.vibenote.llm.MockLlmClient;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** E-1 AC1·AC3·AC4: 평가 러너 파이프라인을 mock LLM과 코드로 만든 작은 ZIP으로 돌린다. */
class EvalPipelineTest {

    @TempDir Path dir;

    static final String CONTROLLER =
            """
            package todo;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/todos")
            public class TodoController {
                private final TodoService todoService;

                public TodoController(TodoService todoService) {
                    this.todoService = todoService;
                }

                @GetMapping
                public Object list() {
                    return todoService.list();
                }

                @PostMapping
                public Object create(@RequestBody String title) {
                    return todoService.create(title);
                }

                @DeleteMapping("/{id}")
                public void delete(@PathVariable long id) {
                    todoService.delete(id);
                }
            }
            """;

    static final String SERVICE =
            """
            package todo;

            import org.springframework.stereotype.Service;

            @Service
            public class TodoService {
                private final TodoMapper todoMapper;

                public TodoService(TodoMapper todoMapper) {
                    this.todoMapper = todoMapper;
                }

                public Object list() {
                    return todoMapper.findAll();
                }

                public Object create(String title) {
                    return todoMapper.insert(title);
                }

                public void delete(long id) {
                    todoMapper.delete(id);
                }
            }
            """;

    static final String MAPPER =
            """
            package todo;

            import org.apache.ibatis.annotations.Mapper;

            @Mapper
            public interface TodoMapper {
                Object findAll();

                Object insert(String title);

                void delete(long id);
            }
            """;

    Path zip(Map<String, String> files) throws IOException {
        Path zip = dir.resolve("todo.zip");
        try (OutputStream os = Files.newOutputStream(zip);
                ZipOutputStream out = new ZipOutputStream(os)) {
            for (Map.Entry<String, String> f : files.entrySet()) {
                out.putNextEntry(new ZipEntry("todo-main/" + f.getKey()));
                out.write(f.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }

    Path todoZip() throws IOException {
        return zip(
                Map.of(
                        "src/main/java/todo/TodoController.java", CONTROLLER,
                        "src/main/java/todo/TodoService.java", SERVICE,
                        "src/main/java/todo/TodoMapper.java", MAPPER));
    }

    EvalFixture fixture(Path zip) {
        return new EvalFixture("todo-test", zip, "todo", "dev", "none");
    }

    /** mock fixture의 기존 카드 이름 두 개 (나머지 이름은 카탈로그에 없어 연결되지 않는다). */
    static List<Concept> catalog() {
        List<Concept> catalog = new ArrayList<>();
        String[][] rows = {
            {"@RestController와 요청 매핑", "spring-web"}, {"의존성 주입 (DI)", "spring-core"}
        };
        for (int i = 0; i < rows.length; i++) {
            Concept c = new Concept();
            c.setId(10L + i);
            c.setName(rows[i][0]);
            c.setCategory(rows[i][1]);
            c.setDifficulty(Level.BEGINNER);
            c.setStatus(ConceptStatus.PUBLISHED);
            catalog.add(c);
        }
        return catalog;
    }

    /** 카탈로그 카드 하나(11) 연결 + 초안 하나. 테스트 classpath의 {@code llm-fixtures/concept.json}은 빈 목록이다. */
    static final String CONCEPT_RESPONSE =
            """
            {"concepts": [
              {"conceptId": 11, "fileNo": 1, "lineHint": "L8-L12",
               "contextNote": "TodoController가 생성자로 TodoService를 받습니다."},
              {"new": {"name": "요청 본문 바인딩 (@RequestBody)", "category": "spring",
                       "difficulty": "BEGINNER", "summary": "요청 본문 JSON을 메서드 인자로 바꿉니다.",
                       "body": "## 핵심\\n본문을 읽습니다.\\n## 주의\\n형식이 틀리면 400입니다."},
               "fileNo": 1, "lineHint": "L20", "contextNote": "create가 title을 본문으로 받습니다."}
            ]}
            """;

    /** 개념만 {@link #CONCEPT_RESPONSE}, 나머지는 앱 mock fixture. */
    static LlmClient fake() {
        MockLlmClient mock = new MockLlmClient();
        return request ->
                request.stage() == LlmStage.CONCEPT
                        ? new LlmResponse(CONCEPT_RESPONSE, "mock", 1, 1)
                        : mock.complete(request);
    }

    static RecordingLlmClient recording(LlmClient delegate) {
        return new RecordingLlmClient(delegate, 0);
    }

    static EvalPipeline pipeline(LlmClient delegate) {
        return new EvalPipeline(recording(delegate), EvalSettings.defaults(), catalog(), "mock");
    }

    static EvalPipeline.Options options(String stage, List<Level> levels) {
        return new EvalPipeline.Options(stage, levels, Integer.MAX_VALUE, 3);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> level(Map<String, Object> report, String level) {
        return (Map<String, Object>) ((Map<String, Object>) report.get("results")).get(level);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> list(Map<String, Object> map, String key) {
        return (List<Map<String, Object>>) map.get(key);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Map<String, Object> map, String key) {
        return (Map<String, Object>) map.get(key);
    }

    @Test
    void AC1_흐름_단계는_탐지한_API_앞_N개를_수준마다_만든다() throws IOException {
        List<String> flowPrompts = new ArrayList<>();
        LlmClient mock = fake();
        LlmClient llm =
                request -> {
                    if (request.stage() == LlmStage.FLOW) {
                        flowPrompts.add(request.prompt());
                    }
                    return mock.complete(request);
                };

        Map<String, Object> report =
                pipeline(llm)
                        .run(
                                fixture(todoZip()),
                                new EvalPipeline.Options(
                                        "flow", List.of(Level.BEGINNER, Level.SENIOR), 1, 2));

        for (String level : List.of("beginner", "senior")) {
            Map<String, Object> result = level(report, level);
            assertThat(result).containsKey("overview");
            List<Map<String, Object>> flows = list(result, "flows");
            assertThat(flows).hasSize(2);
            assertThat(flows.get(0))
                    .containsKeys("endpoint", "summary", "steps", "error")
                    .containsEntry("error", null);
            assertThat((String) flows.get(0).get("endpoint")).matches("[A-Z]+ /api/todos.*");
            assertThat(list(flows.get(0), "steps")).isNotEmpty();
            // flow 단계는 파일·개념·퀴즈를 돌리지 않는다
            assertThat(result).doesNotContainKeys("files", "concepts", "quiz");
        }
        assertThat(report.get("stage")).isEqualTo("flow");
        // 앱과 같은 흐름 프롬프트 (수준 2 × 흐름 2)
        assertThat(flowPrompts).hasSize(4);
        assertThat(flowPrompts.get(0))
                .contains("path=src/main/java/todo/TodoController.java layer=controller")
                .contains("path=src/main/java/todo/TodoService.java layer=service");
    }

    @Test
    void AC1_API가_없으면_흐름을_건너뛴다() throws IOException {
        Map<String, Object> report =
                pipeline(fake())
                        .run(
                                fixture(
                                        zip(
                                                Map.of(
                                                        "src/main/java/todo/TodoService.java",
                                                        SERVICE))),
                                options("flow", List.of(Level.JUNIOR)));

        Map<String, Object> result = level(report, "junior");
        assertThat(list(result, "flows")).isEmpty();
        assertThat(map(result, "skipped")).containsEntry("flow", "NO_API");
    }

    @Test
    void AC3_all은_다섯_단계를_한_보고서에_남긴다() throws IOException {
        EvalPipeline pipeline = pipeline(fake());

        Map<String, Object> report =
                pipeline.run(fixture(todoZip()), options("all", List.of(Level.values())));

        // 수준 3 × (개요 1 + 파일 묶음 1 + 흐름 3 + 개념 1 + 퀴즈 1)
        assertThat(report.get("expectedCalls")).isEqualTo(21);
        for (String level : List.of("beginner", "junior", "senior")) {
            Map<String, Object> result = level(report, level);
            assertThat(result).containsKeys("overview", "files", "flows", "concepts", "quiz");
            assertThat(list(result, "files")).hasSize(3);
            assertThat(list(result, "flows")).hasSize(3);
            List<Map<String, Object>> concepts = list(result, "concepts");
            assertThat(concepts)
                    .isNotEmpty()
                    .allSatisfy(
                            c ->
                                    assertThat(c)
                                            .containsKeys(
                                                    "name",
                                                    "conceptId",
                                                    "draft",
                                                    "category",
                                                    "path",
                                                    "lineHint",
                                                    "contextNote"));
            // 카탈로그에 있는 이름은 그 id로, 없는 이름은 초안(conceptId null)
            assertThat(concepts)
                    .anySatisfy(
                            c ->
                                    assertThat(c)
                                            .containsEntry("name", "의존성 주입 (DI)")
                                            .containsEntry("conceptId", 11L)
                                            .containsEntry("draft", false));
            assertThat(concepts)
                    .anySatisfy(
                            c ->
                                    assertThat(c)
                                            .containsEntry("conceptId", null)
                                            .containsEntry("draft", true));
            List<Map<String, Object>> quiz = list(result, "quiz");
            assertThat(quiz)
                    .isNotEmpty()
                    .allSatisfy(
                            q ->
                                    assertThat(q)
                                            .containsKeys(
                                                    "conceptName",
                                                    "question",
                                                    "choices",
                                                    "answerIndex",
                                                    "explanation"));
            assertThat(quiz).anySatisfy(q -> assertThat(q.get("conceptName")).isNotNull());
            assertThat(map(result, "skipped")).isEmpty();
        }
    }

    @Test
    void AC3_개요가_실패하면_의존_단계만_건너뛴다() throws IOException {
        LlmClient mock = fake();
        LlmClient llm =
                request ->
                        request.stage() == LlmStage.OVERVIEW
                                ? new LlmResponse("JSON 아님", "fake", 1, 1)
                                : mock.complete(request);

        Map<String, Object> report =
                pipeline(llm).run(fixture(todoZip()), options("all", List.of(Level.JUNIOR)));

        Map<String, Object> result = level(report, "junior");
        assertThat(map(result, "errors")).containsKey("overview");
        assertThat(map(result, "skipped"))
                .containsEntry("file", "overview 실패")
                .containsEntry("flow", "overview 실패")
                .doesNotContainKeys("concept", "quiz");
        assertThat(list(result, "concepts")).isNotEmpty();
        assertThat(list(result, "quiz")).isNotEmpty();
    }

    @Test
    void AC3_개념이_실패하면_퀴즈만_건너뛴다() throws IOException {
        LlmClient mock = fake();
        LlmClient llm =
                request ->
                        request.stage() == LlmStage.CONCEPT
                                ? new LlmResponse("{\"x\":1}", "fake", 1, 1)
                                : mock.complete(request);

        Map<String, Object> report =
                pipeline(llm).run(fixture(todoZip()), options("quiz", List.of(Level.JUNIOR)));

        Map<String, Object> result = level(report, "junior");
        assertThat(map(result, "errors")).containsKey("concept");
        assertThat(map(result, "skipped")).containsEntry("quiz", "concept 실패");
        assertThat(list(result, "quiz")).isEmpty();
    }

    @Test
    void AC4_보고서에_index와_units가_있다() throws IOException {
        LlmClient mock = fake();
        int[] fileCalls = {0};
        // 파일 설명 첫 응답만 형식 오류 → 재시도 성공 (firstPass false, ok true)
        LlmClient llm =
                request -> {
                    if (request.stage() == LlmStage.FILE && fileCalls[0]++ == 0) {
                        return new LlmResponse("JSON 아님", "fake", 1, 1);
                    }
                    return mock.complete(request);
                };
        Path zip = todoZip();

        Map<String, Object> report =
                pipeline(llm).run(fixture(zip), options("all", List.of(Level.BEGINNER)));

        assertThat(report)
                .containsEntry("fixture", "todo-test")
                .containsEntry("fixtureZip", zip.toAbsolutePath().normalize().toString())
                .containsEntry("provider", "mock")
                .containsEntry("model", "mock, fake")
                .containsEntry("split", "dev")
                .containsEntry("contamination", "none");
        List<Map<String, Object>> index = list(report, "index");
        assertThat(index).hasSize(3);
        assertThat(index)
                .anySatisfy(
                        f ->
                                assertThat(f)
                                        .containsEntry(
                                                "path", "src/main/java/todo/TodoController.java")
                                        .containsEntry("layer", "controller")
                                        .containsEntry("selected", true)
                                        .containsEntry("lines", (int) CONTROLLER.lines().count()));

        List<Map<String, Object>> units = list(report, "units");
        assertThat(units)
                .extracting(u -> u.get("stage") + ":" + u.get("key"))
                .contains(
                        "overview:overview",
                        "flow:GET /api/todos",
                        "flow:POST /api/todos",
                        "concept:concept",
                        "quiz:quiz");
        assertThat(units)
                .allSatisfy(
                        u ->
                                assertThat(u)
                                        .containsEntry("level", "beginner")
                                        .containsKeys("calls", "firstPass", "ok"));
        Map<String, Object> file =
                units.stream().filter(u -> u.get("stage").equals("file")).findFirst().orElseThrow();
        assertThat(file)
                .containsEntry("calls", 2)
                .containsEntry("firstPass", false)
                .containsEntry("ok", true);
        assertThat((String) file.get("key")).contains("src/main/java/todo/TodoController.java");
        Map<String, Object> overview =
                units.stream()
                        .filter(u -> u.get("stage").equals("overview"))
                        .findFirst()
                        .orElseThrow();
        assertThat(overview)
                .containsEntry("calls", 1)
                .containsEntry("firstPass", true)
                .containsEntry("ok", true);
    }
}
