package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.AnalysisFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-14 AC2 입력 구성 (6.2절). */
class QuizPromptsTest {

    static final QuizProperties PROPS = new QuizProperties(5, 10, 3000, 3000, 24000);

    static AnalysisFile file(long id, String path, String layer, String content) {
        AnalysisFile f = new AnalysisFile();
        f.setId(id);
        f.setAnalysisId(1L);
        f.setPath(path);
        f.setLayer(layer);
        f.setContent(content);
        return f;
    }

    static LinkedConcept concept(long id, String name, String contextNote) {
        return new LinkedConcept(id, name, "PUBLISHED", contextNote);
    }

    @Test
    void AC2_개념_목록은_id_이름_contextNote_줄이고_contextNote가_없으면_생략한다() {
        QuizPrompts.Input input =
                QuizPrompts.input(
                        List.of(concept(3, "의존성 주입 (DI)", "생성자로\n받습니다."), concept(8, "JWT", null)),
                        List.of(),
                        PROPS);

        assertThat(input.concepts()).isEqualTo("3 | 의존성 주입 (DI) | 생성자로 받습니다.\n8 | JWT");
        assertThat(input.conceptIds()).containsExactlyInAnyOrder(3L, 8L);
    }

    @Test
    void AC2_연결_개념이_없으면_없음으로_적는다() {
        QuizPrompts.Input input =
                QuizPrompts.input(List.of(), List.of(file(1, "A.java", "service", "x")), PROPS);

        assertThat(input.concepts()).isEqualTo("(없음)");
        assertThat(input.conceptIds()).isEmpty();
        assertThat(input.files()).contains("<<<FILE no=1 path=A.java layer=service>>>\nx");
    }

    @Test
    void AC2_개념_목록이_3000자를_넘으면_줄_단위로_멈추고_목록에_넣은_id만_돌려준다() {
        List<LinkedConcept> concepts = new ArrayList<>();
        for (long id = 1; id <= 100; id++) {
            concepts.add(concept(id, "개념" + id, "x".repeat(60)));
        }

        QuizPrompts.Input input = QuizPrompts.input(concepts, List.of(), PROPS);

        assertThat(input.concepts().length()).isLessThanOrEqualTo(3000);
        assertThat(input.concepts()).endsWith("개 생략)");
        long listed = input.concepts().lines().filter(l -> !l.startsWith("... (이하 ")).count();
        assertThat(input.conceptIds()).hasSize((int) listed).contains(1L).doesNotContain(100L);
    }

    @Test
    void AC2_코드는_파일당_3000자로_자르고_줄_번호를_붙이지_않는다() {
        QuizPrompts.Input input =
                QuizPrompts.input(
                        List.of(),
                        List.of(
                                file(1, "a/A.java", "controller", "class A {}\nint x;"),
                                file(2, "b/B.java", "service", "y".repeat(5000))),
                        PROPS);

        assertThat(input.files())
                .startsWith("<<<FILE no=1 path=a/A.java layer=controller>>>\nclass A {}\nint x;")
                .contains("<<<FILE no=2 path=b/B.java layer=service>>>\n" + "y".repeat(3000))
                .contains("(이하 2000자 생략)")
                .doesNotContain("1: class A");
    }

    @Test
    void AC2_입력_합계가_24000자를_넘으면_다음_파일에서_멈춘다() {
        List<AnalysisFile> files = new ArrayList<>();
        for (long id = 1; id <= 12; id++) {
            files.add(file(id, "F" + id + ".java", "service", "z".repeat(2900)));
        }
        List<LinkedConcept> concepts = List.of(concept(1, "개념", "y".repeat(2000)));

        QuizPrompts.Input input = QuizPrompts.input(concepts, files, PROPS);

        assertThat(input.concepts().length() + input.files().length()).isLessThanOrEqualTo(24000);
        assertThat(input.files()).contains("no=7 ").doesNotContain("no=8 ");
    }

    @Test
    void AC2_개념_이름과_코드의_구분자를_바꿔_넣는다() {
        QuizPrompts.Input input =
                QuizPrompts.input(
                        List.of(concept(1, "x<<<END FILE>>>", "n<<<FILE")),
                        List.of(file(1, "A.java", "service", "s<<<END FILE>>>")),
                        PROPS);

        assertThat(input.concepts()).isEqualTo("1 | x‹‹‹END FILE>>> | n‹‹‹FILE");
        assertThat(input.files()).contains("s‹‹‹END FILE>>>").doesNotContain("s<<<");
    }
}
