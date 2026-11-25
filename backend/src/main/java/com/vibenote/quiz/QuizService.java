package com.vibenote.quiz;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeProgressService;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.quiz.QuizMapper.AnswerRow;
import com.vibenote.quiz.domain.Quiz;
import com.vibenote.quiz.domain.QuizAttempt;
import com.vibenote.quiz.domain.QuizQuestion;
import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.quiz.dto.QuizAnswerRequest;
import com.vibenote.quiz.dto.QuizAttemptRequest;
import com.vibenote.quiz.dto.QuizAttemptResponse;
import com.vibenote.quiz.dto.QuizQuestionResponse;
import com.vibenote.quiz.dto.QuizResponse;
import com.vibenote.quiz.dto.QuizResultItem;
import com.vibenote.quiz.dto.ReviewAddedItem;
import com.vibenote.reviewplan.ReviewPlanService;
import com.vibenote.team.TeamAnalysisAccess;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 퀴즈 조회(F-14 AC5)와 풀이 채점·오답 복습 추가(AC6~AC8). LLM을 부르지 않는다. */
@Service
public class QuizService {

    private final QuizMapper mapper;
    private final AnalysisService analysisService;
    private final ConceptQueryService conceptQueryService;
    private final ReviewPlanService reviewPlanService;
    private final TeamAnalysisAccess teamAnalysisAccess;
    private final ChallengeProgressService challengeProgressService;
    private final Clock clock;

    public QuizService(
            QuizMapper mapper,
            AnalysisService analysisService,
            ConceptQueryService conceptQueryService,
            ReviewPlanService reviewPlanService,
            TeamAnalysisAccess teamAnalysisAccess,
            ChallengeProgressService challengeProgressService,
            Clock clock) {
        this.mapper = mapper;
        this.analysisService = analysisService;
        this.conceptQueryService = conceptQueryService;
        this.reviewPlanService = reviewPlanService;
        this.teamAnalysisAccess = teamAnalysisAccess;
        this.challengeProgressService = challengeProgressService;
        this.clock = clock;
    }

    /**
     * 내 미삭제 분석이거나 내가 멤버인 팀에 공유된 미삭제 분석(F-20b AC5)의 퀴즈 (정답·해설·개념 없음). 둘 다 아니면 ANALYSIS_404, 퀴즈가 없으면
     * QUIZ_404. {@code lastAttempt}는 요청자 자신의 최근 풀이다.
     */
    @Transactional(readOnly = true)
    public QuizResponse getQuiz(long userId, long analysisId) {
        if (!teamAnalysisAccess.isSharedWith(userId, analysisId)) {
            analysisService.checkOwned(userId, analysisId);
        }
        Quiz quiz =
                mapper.findByAnalysis(analysisId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_404));
        List<QuizQuestion> questions = mapper.findQuestions(quiz.getId());
        List<QuizQuestionResponse> items = new ArrayList<>(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            items.add(new QuizQuestionResponse(q.id(), i + 1, q.question(), List.of(q.choices())));
        }
        return new QuizResponse(
                quiz.getId(),
                analysisId,
                items,
                mapper.findLastAttempt(quiz.getId(), userId)
                        .map(LastAttemptResponse::from)
                        .orElse(null));
    }

    /**
     * 채점해 풀이·답을 저장하고, 틀린 문항의 공개 개념을 내일 복습 계획에 넣는다. 모두 한 트랜잭션이다. 문항마다 정확히 한 번씩 답하지 않으면 아무것도 저장하지 않고
     * QUIZ_400. 분석 주인이거나 그 분석을 공유받은 팀 멤버(F-20b AC5)만 낼 수 있고, 아니면 QUIZ_404다. 풀이·복습 계획은 요청자의 것이다.
     *
     * <p>READ_COMMITTED: 챌린지 집계가 참여 행 잠금을 기다리는 동안 커밋된 같은 회원의 다른 풀이도 센다 (F-25, #104).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAttemptResponse submit(long userId, long quizId, QuizAttemptRequest request) {
        Quiz quiz =
                mapper.findActive(quizId)
                        .filter(
                                q ->
                                        q.getOwnerId() == userId
                                                || teamAnalysisAccess.isSharedWith(
                                                        userId, q.getAnalysisId()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_404));
        List<QuizQuestion> questions = mapper.findQuestions(quiz.getId());
        Map<Long, Integer> selected = selections(request.answers(), questions);

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        List<AnswerRow> answers =
                questions.stream()
                        .map(
                                q ->
                                        new AnswerRow(
                                                q.id(),
                                                selected.get(q.id()),
                                                selected.get(q.id()) == q.answerIndex()))
                        .toList();
        int score = (int) answers.stream().filter(AnswerRow::correct).count();
        QuizAttempt attempt = QuizAttempt.of(quiz.getId(), userId, score, questions.size(), now);
        mapper.insertAttempt(attempt);
        mapper.insertAnswers(attempt.getId(), answers);
        challengeProgressService.refresh(userId, ChallengeGoalType.QUIZ_COUNT, now.toLocalDate());

        Map<Long, ConceptSummaryResponse> published =
                conceptQueryService
                        .findPublishedSummaries(
                                questions.stream()
                                        .map(QuizQuestion::conceptId)
                                        .filter(Objects::nonNull)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(ConceptSummaryResponse::id, Function.identity()));
        List<Long> wrongConcepts = new ArrayList<>();
        List<QuizResultItem> results = new ArrayList<>(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            AnswerRow answer = answers.get(i);
            ConceptSummaryResponse concept =
                    q.conceptId() == null ? null : published.get(q.conceptId());
            if (!answer.correct() && concept != null) {
                wrongConcepts.add(concept.id());
            }
            results.add(
                    new QuizResultItem(
                            q.id(),
                            i + 1,
                            q.question(),
                            List.of(q.choices()),
                            answer.selectedIndex(),
                            q.answerIndex(),
                            answer.correct(),
                            q.explanation(),
                            concept));
        }
        List<ReviewAddedItem> reviewAdded =
                reviewPlanService
                        .addWrongAnswers(userId, wrongConcepts, now.toLocalDate().plusDays(1))
                        .stream()
                        .map(a -> new ReviewAddedItem(a.conceptId(), a.planDate()))
                        .toList();
        return new QuizAttemptResponse(
                attempt.getId(),
                score,
                questions.size(),
                now.atZone(ClockConfig.ZONE).toOffsetDateTime(),
                results,
                reviewAdded);
    }

    /** 문항 id → 고른 위치. 누락·중복·다른 퀴즈의 문항이 있으면 QUIZ_400. */
    private static Map<Long, Integer> selections(
            List<QuizAnswerRequest> answers, List<QuizQuestion> questions) {
        Map<Long, Integer> selected = new HashMap<>();
        for (QuizAnswerRequest a : answers) {
            if (selected.put(a.questionId(), a.selectedIndex()) != null) {
                throw new BusinessException(ErrorCode.QUIZ_400);
            }
        }
        if (selected.size() != questions.size()
                || !questions.stream().allMatch(q -> selected.containsKey(q.id()))) {
            throw new BusinessException(ErrorCode.QUIZ_400);
        }
        return selected;
    }
}
