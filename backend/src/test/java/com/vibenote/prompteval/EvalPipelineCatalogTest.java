package com.vibenote.prompteval;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptDraftService;
import com.vibenote.concept.domain.Concept;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import com.vibenote.llm.MockLlmClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

/** E-1 AC2: Flyway로 시드 카드를 적용한 DB의 카탈로그로 개념·퀴즈를 만든다 (앱과 같은 카탈로그). */
class EvalPipelineCatalogTest extends IntegrationTestBase {

    @Autowired ConceptDraftService conceptDraftService;

    @TempDir Path dir;

    @Test
    void AC2_개념과_퀴즈를_시드_카탈로그로_만든다() throws IOException {
        List<Concept> catalog = conceptDraftService.findAllForMatching();
        // 앱 mock fixture(dev 데이터 기준)를 프롬프트 카드 목록의 id로 연결한다 (테스트 classpath의 concept.json은 빈 목록)
        String conceptFixture =
                Files.readString(Path.of("src/main/resources/llm-fixtures/concept.json"));
        MockLlmClient mock = new MockLlmClient();
        List<String> quizPrompts = new ArrayList<>();
        LlmClient llm =
                request -> {
                    if (request.stage() == LlmStage.CONCEPT) {
                        return new LlmResponse(
                                MockLlmClient.linkConceptNames(conceptFixture, request.prompt()),
                                "mock",
                                1,
                                1);
                    }
                    if (request.stage() == LlmStage.QUIZ) {
                        quizPrompts.add(request.prompt());
                    }
                    return mock.complete(request);
                };
        EvalPipelineTest support = new EvalPipelineTest();
        support.dir = dir;

        Map<String, Object> report =
                new EvalPipeline(
                                new RecordingLlmClient(llm, 0),
                                EvalSettings.defaults(),
                                catalog,
                                "mock")
                        .run(
                                support.fixture(support.todoZip()),
                                new EvalPipeline.Options(
                                        "quiz", List.of(Level.JUNIOR), Integer.MAX_VALUE, 3));

        Map<String, Object> result = EvalPipelineTest.level(report, "junior");
        Concept di =
                catalog.stream()
                        .filter(c -> c.getName().equals("의존성 주입 (DI)"))
                        .findFirst()
                        .orElseThrow();
        List<Map<String, Object>> concepts = EvalPipelineTest.list(result, "concepts");
        assertThat(concepts)
                .anySatisfy(
                        c ->
                                assertThat(c)
                                        .containsEntry("name", "의존성 주입 (DI)")
                                        .containsEntry("conceptId", di.getId())
                                        .containsEntry("draft", false)
                                        .containsEntry("category", di.getCategory())
                                        .containsEntry(
                                                "path", "src/main/java/todo/TodoController.java")
                                        .containsEntry("lineHint", "L20-L30"));
        assertThat(concepts)
                .anySatisfy(
                        c ->
                                assertThat(c)
                                        .containsEntry("name", "GraphQL API (DGS)")
                                        .containsEntry("conceptId", null)
                                        .containsEntry("draft", true)
                                        .containsEntry("category", "web"));

        // 초안은 카탈로그 최대 id 뒤의 임시 id로 퀴즈 개념 목록에 들어간다
        long maxId = catalog.stream().mapToLong(Concept::getId).max().orElseThrow();
        assertThat(quizPrompts).hasSize(1);
        assertThat(quizPrompts.get(0))
                .contains((maxId + 1) + " | GraphQL API (DGS)")
                .contains(di.getId() + " | 의존성 주입 (DI)");
        List<Map<String, Object>> quiz = EvalPipelineTest.list(result, "quiz");
        assertThat(quiz).isNotEmpty();
        assertThat(quiz).anySatisfy(q -> assertThat(q).containsEntry("conceptName", "의존성 주입 (DI)"));
        assertThat(quiz)
                .allSatisfy(
                        q -> {
                            assertThat((List<?>) q.get("choices")).hasSize(4);
                            assertThat((int) q.get("answerIndex")).isBetween(0, 3);
                        });
    }
}
