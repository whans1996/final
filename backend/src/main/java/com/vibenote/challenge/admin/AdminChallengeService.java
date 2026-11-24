package com.vibenote.challenge.admin;

import com.vibenote.challenge.ChallengeImageStorage;
import com.vibenote.challenge.ChallengeMapper;
import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.challenge.admin.dto.AdminChallengeRequest;
import com.vibenote.challenge.admin.dto.AdminChallengeResponse;
import com.vibenote.challenge.admin.dto.AdminChallengeSearchRequest;
import com.vibenote.challenge.admin.dto.AdminChallengeSummaryResponse;
import com.vibenote.challenge.domain.Challenge;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자 챌린지 관리 (F-24). 권한은 SecurityConfig의 {@code /api/admin/**} 규칙이 막는다. 판단 순서는 404 → 400 → 409다.
 *
 * <p>이미지 파일은 DB와 한 트랜잭션으로 묶을 수 없으므로, 새 파일은 DB 저장 전에 쓰고 롤백되면 지우며, 이전 파일은 커밋된 뒤에 지운다.
 */
@Service
@Transactional(readOnly = true)
public class AdminChallengeService {

    static final int MAX_PERIOD_DAYS = 90;
    static final String END_BEFORE_START = "종료일은 시작일과 같거나 뒤여야 합니다.";
    static final String END_IN_PAST = "종료일은 오늘이거나 오늘 이후여야 합니다.";
    static final String PERIOD_TOO_LONG = "기간은 시작일과 종료일을 포함해 90일 이하로 정하세요.";
    static final String IMAGE_AND_REMOVE = "새 이미지와 이미지 제거를 함께 보낼 수 없습니다.";

    private final ChallengeMapper challengeMapper;
    private final ChallengeImageStorage imageStorage;
    private final Validator validator;
    private final Clock clock;

    public AdminChallengeService(
            ChallengeMapper challengeMapper,
            ChallengeImageStorage imageStorage,
            Validator validator,
            Clock clock) {
        this.challengeMapper = challengeMapper;
        this.imageStorage = imageStorage;
        this.validator = validator;
        this.clock = clock;
    }

    public PageResponse<AdminChallengeSummaryResponse> search(
            AdminChallengeSearchRequest search, PageQuery page) {
        LocalDate today = today();
        ChallengeStatus status = search.statusValue();
        long total = challengeMapper.countForAdmin(status, today);
        List<AdminChallengeSummaryResponse> items =
                total <= page.offset()
                        ? List.of()
                        : challengeMapper
                                .findForAdmin(status, today, page.size(), page.offset())
                                .stream()
                                .map(c -> AdminChallengeSummaryResponse.of(c, today))
                                .toList();
        return PageResponse.of(items, page, total);
    }

    public AdminChallengeResponse getDetail(long id) {
        Challenge challenge =
                challengeMapper
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_404));
        return AdminChallengeResponse.of(challenge, today());
    }

    /** 등록 (AC1~AC3). 종료일은 오늘 이후여야 하고 시작일은 과거여도 된다. */
    @Transactional
    public long create(long adminId, AdminChallengeRequest request) {
        Period period = validate(request, null);
        Challenge challenge = new Challenge();
        apply(challenge, request, period);
        challenge.setCreatedBy(adminId);
        if (request.image() != null) {
            challenge.setImageUrl(saveImage(request.image(), null));
        }
        challengeMapper.insert(challenge);
        return challenge.getId();
    }

    /**
     * 전체 교체 (AC6·AC7). 시작한 챌린지는 목표 유형·목표값·시작일을 바꿀 수 없다. 챌린지를 잠그고 읽어 확인과 변경 사이에 삭제·다른 수정이 끼지 않게 한다.
     */
    @Transactional
    public AdminChallengeResponse update(long id, AdminChallengeRequest request) {
        Challenge challenge = lockOrThrow(id);
        Period period = validate(request, challenge.getEndDate());
        if (challenge.statusOn(today()) != ChallengeStatus.UPCOMING
                && (challenge.getGoalType() != request.goalTypeValue()
                        || challenge.getGoalValue() != request.goalValueAsInt()
                        || !challenge.getStartDate().equals(period.start()))) {
            throw new BusinessException(ErrorCode.CHALLENGE_001);
        }
        String oldImageUrl = challenge.getImageUrl();
        apply(challenge, request, period);
        if (request.image() != null) {
            challenge.setImageUrl(saveImage(request.image(), oldImageUrl));
        } else if (request.removeImageRequested()) {
            challenge.setImageUrl(null);
            deleteAfterCommit(oldImageUrl);
        }
        challengeMapper.update(challenge);
        return getDetail(id);
    }

    /** 실제 삭제 (AC8). 참여 기록은 FK CASCADE로 함께 지워지고, 이미지 파일은 커밋 뒤에 지운다. */
    @Transactional
    public void delete(long id) {
        Challenge challenge = lockOrThrow(id);
        challengeMapper.deleteById(id);
        deleteAfterCommit(challenge.getImageUrl());
    }

    private Challenge lockOrThrow(long id) {
        return challengeMapper
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_404));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private record Period(LocalDate start, LocalDate end) {}

    /**
     * 필드 검증과 기간·이미지 교차 검증 (AC2·AC3·AC6). 문제를 모두 모아 COMMON_400 {@code details}로 알린다. 종료일 ≥ 오늘은 등록할
     * 때와 종료일이 바뀔 때만 본다.
     *
     * @param currentEnd 수정 전 종료일. 등록이면 null
     */
    private Period validate(AdminChallengeRequest request, LocalDate currentEnd) {
        Map<String, String> details = new LinkedHashMap<>();
        validator
                .validate(request)
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        LocalDate start = parseDate(request.startDate(), "startDate", details);
        LocalDate end = parseDate(request.endDate(), "endDate", details);
        if (start != null && end != null && !details.containsKey("endDate")) {
            String problem = periodProblem(start, end, currentEnd);
            if (problem != null) details.put("endDate", problem);
        }
        if (request.image() != null) {
            String problem =
                    request.removeImageRequested()
                            ? IMAGE_AND_REMOVE
                            : imageStorage.problem(request.image());
            if (problem != null) details.put("image", problem);
        }
        if (!details.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_400, details);
        }
        return new Period(start, end);
    }

    private String periodProblem(LocalDate start, LocalDate end, LocalDate currentEnd) {
        if (end.isBefore(start)) return END_BEFORE_START;
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_PERIOD_DAYS) return PERIOD_TOO_LONG;
        if (!end.equals(currentEnd) && end.isBefore(today())) return END_IN_PAST;
        return null;
    }

    /** 형식 검증을 통과한 값만 달력 날짜로 읽는다 (2026-13-01 등은 사유를 남기고 null). */
    private static LocalDate parseDate(String value, String field, Map<String, String> details) {
        if (value == null || details.containsKey(field)) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            details.put(field, AdminChallengeRequest.DATE_MESSAGE);
            return null;
        }
    }

    private static void apply(Challenge challenge, AdminChallengeRequest request, Period period) {
        challenge.setTitle(request.title());
        challenge.setDescription(request.description());
        challenge.setStartDate(period.start());
        challenge.setEndDate(period.end());
        challenge.setGoalType(request.goalTypeValue());
        challenge.setGoalValue(request.goalValueAsInt());
    }

    /**
     * 새 이미지를 쓰고, 커밋되면 이전 이미지를, 롤백되면 새 이미지를 지운다. 결과를 모르면(STATUS_UNKNOWN) DB가 어느 쪽을 가리키는지 알 수 없으므로 둘
     * 다 남긴다 (깨진 이미지보다 남는 파일이 낫다).
     */
    private String saveImage(MultipartFile image, String oldImageUrl) {
        String newImageUrl = imageStorage.save(image);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            imageStorage.delete(oldImageUrl);
                        } else if (status == STATUS_ROLLED_BACK) {
                            imageStorage.delete(newImageUrl);
                        }
                    }
                });
        return newImageUrl;
    }

    private void deleteAfterCommit(String imageUrl) {
        if (imageUrl == null) return;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        imageStorage.delete(imageUrl);
                    }
                });
    }
}
