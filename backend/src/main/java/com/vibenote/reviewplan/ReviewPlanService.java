package com.vibenote.reviewplan;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 복습 계획 (F-14 오답 자동 추가. 목록·완료는 F-15가 이 패키지에 더한다). */
@Service
public class ReviewPlanService {

    static final String REASON_WRONG = "WRONG";

    private final ReviewPlanMapper mapper;
    private final Clock clock;

    public ReviewPlanService(ReviewPlanMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 새로 넣은 계획 하나. */
    public record Added(long conceptId, LocalDate planDate) {}

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
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        List<Added> added = new ArrayList<>();
        for (long conceptId : new TreeSet<>(conceptIds)) {
            if (mapper.existsTodo(userId, conceptId)) {
                continue;
            }
            if (mapper.insertIgnoringDuplicate(userId, conceptId, planDate, REASON_WRONG, now)
                    == 1) {
                added.add(new Added(conceptId, planDate));
            }
        }
        return added;
    }
}
