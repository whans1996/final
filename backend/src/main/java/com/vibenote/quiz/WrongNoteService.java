package com.vibenote.quiz;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.quiz.domain.WrongNoteRow;
import com.vibenote.quiz.dto.WrongNoteGroupResponse;
import com.vibenote.quiz.dto.WrongNoteItemResponse;
import com.vibenote.quiz.dto.WrongNoteResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오답 노트 (L-1b AC5~AC8). 지금 열 수 있는 분석(내 분석·공유받은 팀 분석)에서 내 마지막 답이 틀린 문항을 개념별로 묶는다. 오답만 모은 새 퀴즈는 만들지
 * 않는다.
 */
@Service
public class WrongNoteService {

    /** 묶는 문항 수 상한. 넘으면 마지막으로 답한 시각이 최근인 것만 담고 totalCount는 전체 수다 (AC8). */
    static final int LIMIT = 200;

    static final String UNTITLED = "제목 없는 분석";

    private final QuizMapper mapper;
    private final ConceptQueryService conceptQueryService;

    public WrongNoteService(QuizMapper mapper, ConceptQueryService conceptQueryService) {
        this.mapper = mapper;
        this.conceptQueryService = conceptQueryService;
    }

    @Transactional(readOnly = true)
    public WrongNoteResponse getWrongNotes(long userId) {
        int total = mapper.countWrongNotes(userId);
        if (total == 0) {
            return new WrongNoteResponse(0, List.of());
        }
        List<WrongNoteRow> rows = mapper.findWrongNotes(userId, LIMIT);
        Map<Long, ConceptSummaryResponse> published =
                conceptQueryService
                        .findPublishedSummaries(
                                rows.stream()
                                        .map(WrongNoteRow::conceptId)
                                        .filter(Objects::nonNull)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(ConceptSummaryResponse::id, Function.identity()));

        // 행이 lastAnsweredAt 내림차순 → questionId 오름차순이라 묶음 안 순서가 그대로 맞고, 묶음의 첫 행이 가장 최근 시각이다
        Map<Long, Group> byConcept = new LinkedHashMap<>();
        Group none = new Group(null);
        for (WrongNoteRow row : rows) {
            ConceptSummaryResponse concept =
                    row.conceptId() == null ? null : published.get(row.conceptId());
            Group group =
                    concept == null
                            ? none
                            : byConcept.computeIfAbsent(concept.id(), id -> new Group(concept));
            group.add(row);
        }
        List<Group> groups = new ArrayList<>(byConcept.values());
        groups.sort(
                Comparator.comparingInt((Group g) -> g.items.size())
                        .reversed()
                        .thenComparing((Group g) -> g.latest, Comparator.reverseOrder())
                        .thenComparingLong(g -> g.concept.id()));
        if (!none.items.isEmpty()) {
            groups.add(none);
        }
        return new WrongNoteResponse(
                total,
                groups.stream()
                        .map(g -> new WrongNoteGroupResponse(g.concept, List.copyOf(g.items)))
                        .toList());
    }

    private static WrongNoteItemResponse item(WrongNoteRow row) {
        return new WrongNoteItemResponse(
                row.questionId(),
                row.quizId(),
                row.analysisId(),
                row.analysisTitle() == null ? UNTITLED : row.analysisTitle(),
                row.question(),
                List.of(row.choices()),
                row.selectedIndex(),
                row.answerIndex(),
                row.explanation(),
                row.wrongCount(),
                row.lastAnsweredAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }

    /** 개념 묶음 하나. concept가 null이면 개념 없음·비공개 개념 묶음이다. */
    private static final class Group {
        final ConceptSummaryResponse concept;
        final List<WrongNoteItemResponse> items = new ArrayList<>();
        LocalDateTime latest;

        Group(ConceptSummaryResponse concept) {
            this.concept = concept;
        }

        void add(WrongNoteRow row) {
            if (latest == null) {
                latest = row.lastAnsweredAt();
            }
            items.add(item(row));
        }
    }
}
