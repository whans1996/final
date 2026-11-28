package com.vibenote.interview;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** I-1a AC6(질문 교체)·AC4(FAILED는 질문 없음). */
class InterviewSaveServiceTest extends InterviewTestSupport {

    @Autowired InterviewSaveService saveService;

    static GeneratedQuestion question(int no, long fileId) {
        return new GeneratedQuestion(
                no,
                InterviewCategory.TECH_CHOICE,
                "새 질문 " + no,
                List.of("새 포인트"),
                List.of(new InterviewEvidence(fileId, "A.java", 3, 4)),
                List.of(12L));
    }

    @Test
    void AC6_성공하면_이전_질문을_새_질문으로_바꾸고_DONE과_generated_at을_기록한다() {
        long userId = createUser("save1");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long fileId = fileIds(analysisId).get(0);
        long interviewId = insertInterview(analysisId, "PENDING", 1, null);
        insertQuestion(interviewId, 1, fileId, "[]");
        insertQuestion(interviewId, 2, fileId, "[]");
        jdbc.update("UPDATE analysis_interviews SET generated_at = NULL WHERE id = ?", interviewId);

        boolean saved =
                saveService.complete(
                        interviewId,
                        List.of(question(1, fileId), question(2, fileId), question(3, fileId)));

        assertThat(saved).isTrue();
        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("generated_at")).isNotNull();
        assertThat(row.get("regenerations")).isEqualTo(1);
        List<Map<String, Object>> questions = questionRows(interviewId);
        assertThat(questions)
                .extracting(q -> q.get("question"))
                .containsExactly("새 질문 1", "새 질문 2", "새 질문 3");
        assertThat(questions.get(0).get("category")).isEqualTo("TECH_CHOICE");
    }

    @Test
    void AC6_이미_끝났으면_바꾸지_않는다() {
        long userId = createUser("save2");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long fileId = fileIds(analysisId).get(0);
        long interviewId = insertInterview(analysisId, "FAILED", 0, "ANALYSIS_500");

        boolean saved = saveService.complete(interviewId, List.of(question(1, fileId)));

        assertThat(saved).isFalse();
        assertThat(interviewRow(analysisId).get("status")).isEqualTo("FAILED");
        assertThat(questionRows(interviewId)).isEmpty();
    }

    @Test
    void AC6_실패하면_이전_질문을_지우고_FAILED다() {
        long userId = createUser("save3");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long fileId = fileIds(analysisId).get(0);
        long interviewId = insertInterview(analysisId, "PENDING", 1, null);
        insertQuestion(interviewId, 1, fileId, "[]");

        saveService.markFailed(interviewId, ErrorCode.LLM_502);

        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_502");
        assertThat(questionRows(interviewId)).isEmpty();
    }
}
