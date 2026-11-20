package com.vibenote.reviewplan;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.reviewplan.domain.ReviewPlan;
import com.vibenote.reviewplan.dto.ReviewPlanItemResponse;
import com.vibenote.reviewplan.dto.ReviewPlanListResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 복습 계획 (F-14 오답 자동 추가, F-15 목록·직접 추가·완료·삭제, F-12 찜 자동 추가). 한 회원의 한 카드에 대한 TODO는 날짜와 상관없이 하나만 둔다.
 * 카드가 PUBLISHED인 계획만 존재하는 것으로 본다.
 */
@Service
public class ReviewPlanService {

    /** 기본 조회 기간: 오늘 ~ 오늘+6일 (AC1). */
    static final int DEFAULT_DAYS = 7;

    /** 조회 기간 상한: {@code to - from ≤ 30일} (AC3). */
    static final int MAX_RANGE_DAYS = 30;

    /** 직접 추가할 수 있는 가장 먼 날짜: 오늘+365일 (AC5). */
    static final int MAX_PLAN_AHEAD_DAYS = 365;

    private final ReviewPlanMapper mapper;
    private final ConceptQueryService conceptQueryService;
    private final Clock clock;

    public ReviewPlanService(
            ReviewPlanMapper mapper, ConceptQueryService conceptQueryService, Clock clock) {
        this.mapper = mapper;
        this.conceptQueryService = conceptQueryService;
        this.clock = clock;
    }

    /** 새로 넣은 계획 하나. */
    public record Added(long conceptId, LocalDate planDate) {}

    // ── 목록 (AC1~AC3) ──

    /**
     * 기간 안의 내 계획과 밀린 복습. {@code from}·{@code to}는 둘 다 주거나 둘 다 생략한다 (생략하면 오늘 ~ 오늘+6일).
     *
     * @throws BusinessException COMMON_400 (details에 from·to) 형식 오류, 한쪽만 줌, from &gt; to, 31일 초과
     */
    @Transactional(readOnly = true)
    public ReviewPlanListResponse list(long userId, String fromText, String toText) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from;
        LocalDate to;
        if (fromText == null && toText == null) {
            from = today;
            to = today.plusDays(DEFAULT_DAYS - 1);
        } else {
            Map<String, String> details = new LinkedHashMap<>();
            from = parseDate("from", fromText, details);
            to = parseDate("to", toText, details);
            if (details.isEmpty() && from.isAfter(to)) {
                details.put("to", "to는 from과 같거나 뒤의 날짜여야 합니다.");
            } else if (details.isEmpty() && ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
                details.put("to", "한 번에 31일까지 조회할 수 있습니다.");
            }
            if (!details.isEmpty()) {
                throw new BusinessException(ErrorCode.COMMON_400, details);
            }
        }
        List<ReviewPlan> overdue = mapper.findOverdue(userId, today);
        List<ReviewPlan> items = mapper.findInRange(userId, from, to);
        Map<Long, ConceptSummaryResponse> concepts =
                publishedSummaries(Stream.concat(overdue.stream(), items.stream()).toList());
        return new ReviewPlanListResponse(
                today, from, to, toItems(overdue, concepts), toItems(items, concepts));
    }

    private static LocalDate parseDate(String field, String text, Map<String, String> details) {
        if (text == null) {
            details.put(field, "from과 to를 함께 보내 주세요.");
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            details.put(field, "날짜는 YYYY-MM-DD 형식이어야 합니다.");
            return null;
        }
    }

    private Map<Long, ConceptSummaryResponse> publishedSummaries(List<ReviewPlan> plans) {
        List<Long> ids = plans.stream().map(ReviewPlan::getConceptId).distinct().toList();
        return conceptQueryService.findPublishedSummaries(ids).stream()
                .collect(Collectors.toMap(ConceptSummaryResponse::id, Function.identity()));
    }

    /** 요약이 없는(PUBLISHED가 아닌) 카드의 계획은 뺀다. */
    private static List<ReviewPlanItemResponse> toItems(
            List<ReviewPlan> plans, Map<Long, ConceptSummaryResponse> concepts) {
        return plans.stream()
                .filter(plan -> concepts.containsKey(plan.getConceptId()))
                .map(plan -> ReviewPlanItemResponse.of(plan, concepts.get(plan.getConceptId())))
                .toList();
    }

    // ── 직접 추가·완료·삭제 (AC4~AC7) ──

    /**
     * PUBLISHED 카드를 {@code planDate}(생략하면 내일)의 TODO 계획(reason MANUAL)으로 넣는다 (AC4·AC5).
     *
     * @throws BusinessException COMMON_400 planDate가 오늘 이전·오늘+365일 뒤, CONCEPT_404 없는·비공개 카드,
     *     REVIEW_PLAN_409 그 카드의 TODO가 이미 있거나 같은 날짜의 계획이 있음
     */
    @Transactional
    public ReviewPlanItemResponse addManual(long userId, long conceptId, LocalDate planDate) {
        LocalDate today = today();
        LocalDate date = planDate == null ? today.plusDays(1) : planDate;
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_PLAN_AHEAD_DAYS))) {
            throw new BusinessException(
                    ErrorCode.COMMON_400, Map.of("planDate", "오늘부터 1년 안의 날짜를 골라 주세요."));
        }
        ConceptSummaryResponse concept =
                ConceptSummaryResponse.from(conceptQueryService.getPublished(conceptId));
        if (!addIfNoTodo(userId, conceptId, date, ReviewPlanReason.MANUAL, now())) {
            throw new BusinessException(ErrorCode.REVIEW_PLAN_409);
        }
        ReviewPlan plan = mapper.findByDate(userId, conceptId, date).orElseThrow();
        return ReviewPlanItemResponse.of(plan, concept);
    }

    /**
     * 완료(DONE, doneAt = 지금)·되돌림(TODO, doneAt = null). 이미 그 상태면 바꾸지 않는다 (AC6).
     *
     * @throws BusinessException REVIEW_PLAN_404 없는·남의·비공개 카드의 계획, REVIEW_PLAN_409 되돌릴 때 그 카드의 다른
     *     TODO가 있음
     */
    @Transactional
    public ReviewPlanItemResponse changeStatus(long userId, long planId, ReviewPlanStatus status) {
        Visible visible = findVisible(userId, planId);
        ReviewPlan plan = visible.plan();
        ConceptSummaryResponse concept = visible.concept();
        if (plan.getStatus() == status) {
            return ReviewPlanItemResponse.of(plan, concept);
        }
        if (status == ReviewPlanStatus.TODO
                && mapper.existsOtherTodo(userId, plan.getConceptId(), planId)) {
            throw new BusinessException(ErrorCode.REVIEW_PLAN_409);
        }
        LocalDateTime doneAt = status == ReviewPlanStatus.DONE ? now() : null;
        mapper.updateStatus(planId, status, doneAt);
        plan.setStatus(status);
        plan.setDoneAt(doneAt);
        return ReviewPlanItemResponse.of(plan, concept);
    }

    /**
     * 내 계획을 지운다 (AC7).
     *
     * @throws BusinessException REVIEW_PLAN_404 없는·남의·비공개 카드의 계획
     */
    @Transactional
    public void delete(long userId, long planId) {
        findVisible(userId, planId);
        mapper.deleteOwned(userId, planId);
    }

    private record Visible(ReviewPlan plan, ConceptSummaryResponse concept) {}

    /** 내 계획이고 카드가 PUBLISHED일 때만. 아니면 존재 여부를 드러내지 않고 REVIEW_PLAN_404. */
    private Visible findVisible(long userId, long planId) {
        ReviewPlan plan =
                mapper.findOwned(userId, planId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_PLAN_404));
        ConceptSummaryResponse concept = publishedSummaries(List.of(plan)).get(plan.getConceptId());
        if (concept == null) {
            throw new BusinessException(ErrorCode.REVIEW_PLAN_404);
        }
        return new Visible(plan, concept);
    }

    // ── 자동 추가 (F-14 WRONG, F-12 WISH) ──

    /**
     * 퀴즈에서 틀린 개념을 {@code planDate}의 TODO 계획(reason WRONG)으로 넣는다 (F-14 AC7). 그 개념의 TODO가 날짜와 상관없이 이미
     * 있거나, 같은 날짜의 계획(DONE 포함)이 있으면 건너뛴다. 호출자의 트랜잭션에 참여한다.
     *
     * @param conceptIds 공개 카드 id (중복은 한 번만 본다)
     * @return 새로 넣은 계획 ({@code conceptId} 오름차순)
     */
    @Transactional
    public List<Added> addWrongAnswers(
            long userId, Collection<Long> conceptIds, LocalDate planDate) {
        LocalDateTime now = now();
        List<Added> added = new ArrayList<>();
        for (long conceptId : new TreeSet<>(conceptIds)) {
            if (addIfNoTodo(userId, conceptId, planDate, ReviewPlanReason.WRONG, now)) {
                added.add(new Added(conceptId, planDate));
            }
        }
        return added;
    }

    /**
     * 찜한 카드를 내일의 TODO 계획(reason WISH)으로 넣는다 (F-15 AC8, F-12가 부른다). 그 카드의 TODO가 이미 있거나 내일 날짜의 계획이
     * 있으면 넣지 않는다. 호출자의 트랜잭션에 참여한다. 카드가 PUBLISHED인지는 호출자가 확인한다.
     *
     * @return 넣었으면 그 계획, 아니면 비어 있다
     */
    @Transactional
    public Optional<Added> addWish(long userId, long conceptId) {
        LocalDate tomorrow = today().plusDays(1);
        if (addIfNoTodo(userId, conceptId, tomorrow, ReviewPlanReason.WISH, now())) {
            return Optional.of(new Added(conceptId, tomorrow));
        }
        return Optional.empty();
    }

    /** 그 카드의 TODO가 없고 같은 날짜의 계획도 없으면 넣는다. @return 넣었으면 true */
    private boolean addIfNoTodo(
            long userId,
            long conceptId,
            LocalDate planDate,
            ReviewPlanReason reason,
            LocalDateTime now) {
        if (mapper.existsTodo(userId, conceptId)) {
            return false;
        }
        return mapper.insertIgnoringDuplicate(userId, conceptId, planDate, reason.name(), now) == 1;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
