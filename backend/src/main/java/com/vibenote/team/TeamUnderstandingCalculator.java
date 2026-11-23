package com.vibenote.team;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.team.domain.MemberAnswer;
import com.vibenote.team.domain.QuestionLayer;
import com.vibenote.team.dto.MemberModuleResponse;
import com.vibenote.team.dto.MemberUnderstandingResponse;
import com.vibenote.team.dto.ModuleUnderstandingResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 팀 이해도 계산 (F-20b AC6·AC7). DB를 보지 않는 순수 계산이다.
 *
 * <p>모듈 = 문항의 레이어. 레이어가 없거나 알 수 없는 값이면 {@code other}다. 모듈은 {@link #LAYER_ORDER} 순서로 문항이 있는 것만 준다.
 * 정답률은 푼 멤버들의 그 레이어 맞힌 수 합 ÷ 문항 수 합 × 100을 반올림한 정수이고, 아무도 풀지 않았으면 null이다.
 */
final class TeamUnderstandingCalculator {

    static final List<String> LAYER_ORDER =
            List.of("controller", "service", "mapper", "vue", "js", "config", "domain", "other");

    private static final String OTHER = "other";

    /** 계산에 필요한 멤버 정보. 순서가 곧 응답 순서다. */
    record Member(long userId, String nickname, TeamRole role) {}

    record Result(
            int solvedMemberCount,
            List<ModuleUnderstandingResponse> modules,
            List<MemberUnderstandingResponse> members) {}

    private TeamUnderstandingCalculator() {}

    static Result calculate(
            List<QuestionLayer> questions, List<Member> members, List<MemberAnswer> answers) {
        Map<Long, String> layerOf = new HashMap<>();
        Map<String, Integer> questionCount = new LinkedHashMap<>();
        LAYER_ORDER.forEach(layer -> questionCount.put(layer, 0));
        for (QuestionLayer q : questions) {
            String layer = normalize(q.layer());
            layerOf.put(q.questionId(), layer);
            questionCount.merge(layer, 1, Integer::sum);
        }
        List<String> layers =
                LAYER_ORDER.stream().filter(layer -> questionCount.get(layer) > 0).toList();

        Map<Long, List<MemberAnswer>> answersByUser = new HashMap<>();
        for (MemberAnswer a : answers) {
            answersByUser.computeIfAbsent(a.userId(), k -> new ArrayList<>()).add(a);
        }

        Map<String, Integer> teamCorrect = new HashMap<>();
        Map<String, Integer> teamTotal = new HashMap<>();
        List<MemberUnderstandingResponse> memberResults = new ArrayList<>(members.size());
        int solved = 0;
        for (Member member : members) {
            List<MemberAnswer> rows = answersByUser.get(member.userId());
            if (rows == null) {
                memberResults.add(
                        new MemberUnderstandingResponse(
                                member.userId(),
                                member.nickname(),
                                member.role(),
                                null,
                                List.of()));
                continue;
            }
            solved++;
            Map<String, Integer> correct = new HashMap<>();
            for (MemberAnswer a : rows) {
                if (a.questionId() != null
                        && Boolean.TRUE.equals(a.correct())
                        && layerOf.containsKey(a.questionId())) {
                    correct.merge(layerOf.get(a.questionId()), 1, Integer::sum);
                }
            }
            List<MemberModuleResponse> modules = new ArrayList<>(layers.size());
            for (String layer : layers) {
                int c = correct.getOrDefault(layer, 0);
                int t = questionCount.get(layer);
                modules.add(new MemberModuleResponse(layer, c, t));
                teamCorrect.merge(layer, c, Integer::sum);
                teamTotal.merge(layer, t, Integer::sum);
            }
            MemberAnswer first = rows.get(0);
            memberResults.add(
                    new MemberUnderstandingResponse(
                            member.userId(),
                            member.nickname(),
                            member.role(),
                            new LastAttemptResponse(
                                    first.attemptId(),
                                    first.score(),
                                    first.total(),
                                    first.createdAt().atZone(ClockConfig.ZONE).toOffsetDateTime()),
                            modules));
        }

        List<ModuleUnderstandingResponse> modules = new ArrayList<>(layers.size());
        for (String layer : layers) {
            int total = teamTotal.getOrDefault(layer, 0);
            Integer rate =
                    total == 0
                            ? null
                            : (int) Math.round(teamCorrect.getOrDefault(layer, 0) * 100.0 / total);
            modules.add(new ModuleUnderstandingResponse(layer, questionCount.get(layer), rate));
        }
        return new Result(solved, modules, memberResults);
    }

    private static String normalize(String layer) {
        return layer != null && LAYER_ORDER.contains(layer) ? layer : OTHER;
    }
}
