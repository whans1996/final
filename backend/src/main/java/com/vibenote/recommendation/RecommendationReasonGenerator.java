package com.vibenote.recommendation;

import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 카드 여러 장의 추천 이유를 LLM 1회로 만든다 (F-16 AC5·AC6). 형식 오류는 1회 재시도하고, 그래도 틀리거나 LLM이 응답하지 못하면 빈 맵을 돌려준다
 * (호출자가 규칙 문장으로 채운다).
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석).
 */
@Component
class RecommendationReasonGenerator {

    private static final Logger log = LoggerFactory.getLogger(RecommendationReasonGenerator.class);

    private final LlmClient llm;
    private final PromptTemplates templates;

    RecommendationReasonGenerator(LlmClient llm, PromptTemplates templates) {
        this.llm = llm;
        this.templates = templates;
    }

    /**
     * @param cards 이유가 필요한 카드 (최대 10장, 추천 순서)
     * @return 카드 id → 정리한 이유. 못 받은 카드는 없다
     */
    Map<Long, String> generate(long userId, Level level, List<ScoredConcept> cards) {
        List<ScoredConcept> asked = cards.stream().limit(RecommendationPrompts.MAX_CARDS).toList();
        Set<Long> ids = asked.stream().map(c -> c.concept().id()).collect(Collectors.toSet());
        String prompt =
                templates.render(
                        LlmStage.RECOMMEND,
                        level,
                        Map.of(
                                "level", RuleReasons.label(level),
                                "cards", RecommendationPrompts.cards(asked, level)));
        LlmRequest request = new LlmRequest(LlmStage.RECOMMEND, level, prompt, userId, null);
        try {
            return LlmJsonCall.call(
                            llm, request, root -> RecommendationResponseParser.parse(root, ids))
                    .orElseGet(
                            () -> {
                                log.warn("회원 {} 추천 이유: 형식 오류로 규칙 문장 사용", userId);
                                return Map.of();
                            });
        } catch (BusinessException e) {
            log.warn("회원 {} 추천 이유: LLM 실패로 규칙 문장 사용 ({})", userId, e.getErrorCode());
            return Map.of();
        }
    }
}
