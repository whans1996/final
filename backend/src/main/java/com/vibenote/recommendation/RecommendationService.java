package com.vibenote.recommendation;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.recommendation.domain.ConceptCount;
import com.vibenote.recommendation.domain.RecommendationReason;
import com.vibenote.recommendation.dto.ReasonSource;
import com.vibenote.recommendation.dto.RecommendationItemResponse;
import com.vibenote.recommendation.dto.RecommendationListResponse;
import com.vibenote.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 맞춤 개념 카드 추천 (docs/specs/F-16-recommendation.md). 규칙 점수로 고르고(1.1절), 이유는 캐시가 없거나 신호 키가 바뀐 카드만 모아
 * LLM 1회로 만든다(AC5). LLM이 못 준 카드는 규칙 문장(AC6).
 *
 * <p><b>{@code @Transactional}을 붙이지 않는다.</b> LLM 호출이 트랜잭션 밖에 있어야 한다 ({@code LlmClient} 주석). 조회는 각각
 * auto-commit이고, 이유 저장은 INSERT 한 문장이다.
 */
@Service
public class RecommendationService {

    private final RecommendationMapper mapper;
    private final ConceptQueryService conceptQueryService;
    private final UserService userService;
    private final RecommendationReasonGenerator generator;
    private final Clock clock;

    public RecommendationService(
            RecommendationMapper mapper,
            ConceptQueryService conceptQueryService,
            UserService userService,
            RecommendationReasonGenerator generator,
            Clock clock) {
        this.mapper = mapper;
        this.conceptQueryService = conceptQueryService;
        this.userService = userService;
        this.generator = generator;
        this.clock = clock;
    }

    public RecommendationListResponse recommend(long userId, int size) {
        Level level = userService.getActiveMember(userId).getLevel();
        List<ScoredConcept> ranked = rank(userId, level, size);
        if (ranked.isEmpty()) {
            return new RecommendationListResponse(List.of());
        }

        Map<Long, RecommendationReason> cached =
                mapper
                        .findReasons(userId, ranked.stream().map(s -> s.concept().id()).toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        RecommendationReason::conceptId, Function.identity()));
        List<ScoredConcept> misses =
                ranked.stream()
                        .filter(
                                s -> {
                                    RecommendationReason hit = cached.get(s.concept().id());
                                    return hit == null || !hit.signalKey().equals(s.signalKey());
                                })
                        .toList();
        Map<Long, String> generated =
                misses.isEmpty() ? Map.of() : generator.generate(userId, level, misses);
        List<RecommendationReason> fresh =
                misses.stream()
                        .filter(s -> generated.containsKey(s.concept().id()))
                        .map(
                                s ->
                                        new RecommendationReason(
                                                s.concept().id(),
                                                generated.get(s.concept().id()),
                                                s.signalKey()))
                        .toList();
        if (!fresh.isEmpty()) {
            mapper.upsertReasons(userId, fresh, LocalDateTime.now(clock));
        }

        List<RecommendationItemResponse> items =
                ranked.stream().map(s -> item(s, level, cached, generated, misses)).toList();
        return new RecommendationListResponse(items);
    }

    /** 후보(제외 규칙 적용)에 점수를 매겨 상위 {@code size}개. 공개 카드가 없으면 빈 목록. */
    private List<ScoredConcept> rank(long userId, Level level, int size) {
        List<ConceptSummaryResponse> candidates = conceptQueryService.findAllPublishedSummaries();
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Concept> wished = conceptQueryService.findWishedByUser(userId);
        Set<Long> excluded = new HashSet<>(mapper.findReviewedConceptIds(userId));
        excluded.addAll(mapper.findDueTodoConceptIds(userId, LocalDate.now(clock)));
        wished.forEach(c -> excluded.add(c.getId()));

        Map<Long, Integer> wrongCounts =
                mapper.countWrongByConcept(userId).stream()
                        .collect(Collectors.toMap(ConceptCount::conceptId, ConceptCount::count));
        return RecommendationScorer.rank(
                new RecommendationScorer.Input(
                        candidates,
                        wrongCounts,
                        Set.copyOf(mapper.findMyConceptIds(userId)),
                        wished.stream().map(Concept::getCategory).collect(Collectors.toSet()),
                        excluded,
                        level),
                size);
    }

    private static RecommendationItemResponse item(
            ScoredConcept s,
            Level level,
            Map<Long, RecommendationReason> cached,
            Map<Long, String> generated,
            List<ScoredConcept> misses) {
        long id = s.concept().id();
        String reason;
        ReasonSource source = ReasonSource.AI;
        if (!misses.contains(s)) {
            reason = cached.get(id).reason();
        } else if (generated.containsKey(id)) {
            reason = generated.get(id);
        } else {
            reason = RuleReasons.of(s.signals(), s.concept(), level);
            source = ReasonSource.RULE;
        }
        return new RecommendationItemResponse(s.concept(), s.score(), s.signals(), reason, source);
    }
}
