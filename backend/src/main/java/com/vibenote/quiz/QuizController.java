package com.vibenote.quiz;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.quiz.dto.QuizAttemptRequest;
import com.vibenote.quiz.dto.QuizAttemptResponse;
import com.vibenote.quiz.dto.QuizRegenerationResponse;
import com.vibenote.quiz.dto.QuizResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 코드 이해 퀴즈 (docs/specs/F-14-code-quiz.md 3절)와 재생성 (L-1a). 로그인 필요 · 본인 분석만. */
@Tag(name = "퀴즈", description = "분석한 코드로 만든 객관식 퀴즈 조회, 다시 만들기, 풀이 제출·채점")
@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService quizService;
    private final QuizRegenerationService quizRegenerationService;

    public QuizController(
            QuizService quizService, QuizRegenerationService quizRegenerationService) {
        this.quizService = quizService;
        this.quizRegenerationService = quizRegenerationService;
    }

    @Operation(
            summary = "분석의 퀴즈 조회",
            description =
                    "문항(정답·해설·개념 제외)과 요청자의 가장 최근 풀이 요약. 분석 주인과 그 분석을 공유받은 팀 멤버(F-20b)가"
                            + " 볼 수 있다. LLM을 부르지 않는다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "없는·삭제된 분석, 내 분석도 내가 멤버인 팀에 공유된 분석도 아님 (ANALYSIS_404), 퀴즈가 없음" + " (QUIZ_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/analyses/{id:\\d+}/quiz")
    public QuizResponse getQuiz(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return quizService.getQuiz(user.id(), id);
    }

    @Operation(
            summary = "퀴즈 다시 만들기",
            description =
                    "생성에 실패한(quizStatus = FAILED) 내 완료 분석의 퀴즈를 같은 입력으로 다시 만든다. 바로 202를 주고 생성은"
                        + " 백그라운드에서 하며, 결과는 /api/analyses/{id}/status의 quizStatus(READY·FAILED)로"
                        + " 확인한다. 분석당 3회까지이고 실패해도 횟수는 돌아오지 않는다.")
    @ApiResponse(responseCode = "202", description = "접수됨 (quizStatus = GENERATING)")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는·삭제된 분석, 남의 분석(팀 공유 멤버 포함) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "분석이 DONE이 아니거나 퀴즈가 이미 있거나 만드는 중 (QUIZ_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "429",
            description = "재생성 횟수를 다 씀 (QUIZ_429)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/analyses/{id:\\d+}/quiz")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public QuizRegenerationResponse regenerate(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return quizRegenerationService.request(user.id(), id);
    }

    @Operation(
            summary = "퀴즈 풀이 제출·채점",
            description =
                    "문항마다 한 번씩 답하면 채점해 저장하고, 틀린 문항의 공개 개념을 내일 복습 계획에 넣는다."
                            + " 같은 퀴즈를 여러 번 풀 수 있다. 분석 주인과 그 분석을 공유받은 팀 멤버(F-20b)가 낼 수 있고,"
                            + " 풀이·복습 계획은 요청자의 것이다.")
    @ApiResponse(responseCode = "201", description = "채점 성공")
    @ApiResponse(
            responseCode = "400",
            description = "answers 검증 실패 (COMMON_400), 문항마다 한 번씩 답하지 않음 (QUIZ_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 퀴즈, 삭제된 분석의 퀴즈, 내 분석도 내가 멤버인 팀에 공유된 분석도 아닌 퀴즈 (QUIZ_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/quizzes/{quizId:\\d+}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizAttemptResponse submit(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long quizId,
            @Valid @RequestBody QuizAttemptRequest request) {
        return quizService.submit(user.id(), quizId, request);
    }
}
