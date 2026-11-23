package com.vibenote.analysis.flow;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 실행 흐름 요청 접수·조회와 생성 결과 저장 (F-07). LLM 호출은 FlowGenerator가 트랜잭션 밖에서 한다. */
@Service
public class FlowService {

    static final String DUPLICATE_ENDPOINT = "같은 API를 두 번 고를 수 없습니다.";

    private final FlowMapper flowMapper;
    private final AnalysisService analysisService;
    private final FlowProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public FlowService(
            FlowMapper flowMapper,
            AnalysisService analysisService,
            FlowProperties properties,
            ApplicationEventPublisher events,
            Clock clock) {
        this.flowMapper = flowMapper;
        this.analysisService = analysisService;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * 흐름 생성을 접수한다 (AC1, AC6~AC9). 분석 행을 잠근 채 개수를 세고 행을 만들며, 커밋된 뒤 생성이 시작된다 (FlowLauncher).
     *
     * @return 이 분석의 흐름 전체 (id 오름차순)
     */
    @Transactional
    public List<FlowResponse> request(
            long userId, long analysisId, List<CreateFlowsRequest.Endpoint> endpoints) {
        Set<String> keys = new LinkedHashSet<>();
        for (CreateFlowsRequest.Endpoint e : endpoints) {
            if (!keys.add(e.key())) {
                throw new BusinessException(
                        ErrorCode.COMMON_400, Map.of("endpoints", DUPLICATE_ENDPOINT));
            }
        }
        AnalysisDetail analysis =
                flowMapper
                        .lockOwnedAnalysis(analysisId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
        if (analysis.getStatus() != AnalysisStatus.DONE) {
            throw new BusinessException(ErrorCode.ANALYSIS_409);
        }
        Set<String> apis =
                analysis.getOverview() == null
                        ? Set.of()
                        : analysis.getOverview().apis().stream()
                                .map(FlowService::key)
                                .collect(Collectors.toSet());
        if (!apis.containsAll(keys)) {
            throw new BusinessException(ErrorCode.ANALYSIS_002);
        }

        Map<String, AnalysisFlow> existing =
                flowMapper.findByAnalysis(analysisId).stream()
                        .collect(Collectors.toMap(AnalysisFlow::getEndpoint, Function.identity()));
        long active =
                existing.values().stream().filter(f -> f.getStatus() != FlowStatus.FAILED).count();
        List<String> created = new ArrayList<>();
        List<AnalysisFlow> retried = new ArrayList<>();
        for (String key : keys) {
            AnalysisFlow flow = existing.get(key);
            if (flow == null) {
                created.add(key);
            } else if (flow.getStatus() == FlowStatus.FAILED) {
                retried.add(flow);
            }
            // PENDING·DONE은 캐시: 다시 만들지 않고 그대로 돌려준다 (AC6)
        }
        if (active + created.size() + retried.size() > properties.maxPerAnalysis()) {
            throw new BusinessException(ErrorCode.ANALYSIS_003);
        }

        LocalDateTime now = now();
        List<Long> started = new ArrayList<>();
        for (AnalysisFlow flow : retried) {
            flowMapper.resetFailed(flow.getId(), now);
            started.add(flow.getId());
        }
        for (String key : created) {
            AnalysisFlow flow = AnalysisFlow.pending(analysisId, key, now);
            flowMapper.insert(flow);
            started.add(flow.getId());
        }
        if (!started.isEmpty()) {
            events.publishEvent(new FlowRequestedEvent(started));
        }
        return responses(analysisId);
    }

    /** 볼 수 있는 분석(내 것 또는 공개·완료된 남의 것, F-19)의 흐름 전체 (AC10). 분석이 진행 중이어도 조회는 된다. */
    @Transactional(readOnly = true)
    public List<FlowResponse> list(long userId, long analysisId) {
        analysisService.checkViewable(userId, analysisId);
        return responses(analysisId);
    }

    /** 아직 PENDING인 흐름 (생성기 입력). 이미 끝났거나 없으면 empty. */
    public Optional<AnalysisFlow> findPending(long flowId) {
        return flowMapper.findById(flowId).filter(f -> f.getStatus() == FlowStatus.PENDING);
    }

    /** 생성기 입력용 분석 + overview. */
    public Optional<AnalysisDetail> findAnalysis(long analysisId) {
        return flowMapper.findAnalysis(analysisId);
    }

    /** 생성 결과 저장. LLM 호출과 분리한 짧은 트랜잭션. 그 사이 재시작 복구 등으로 이미 끝났으면 바꾸지 않는다. */
    @Transactional
    public void complete(long flowId, FlowResult result) {
        flowMapper.complete(
                flowId, result.summary(), result.steps().toArray(FlowStep[]::new), now());
    }

    /** PENDING 흐름을 FAILED로. 커밋 이후 콜백(AFTER_COMMIT)에서도 불리므로 새 트랜잭션으로 쓴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long flowId, ErrorCode code) {
        flowMapper.markFailed(flowId, code.name(), now());
    }

    /** PENDING 흐름을 모두 FAILED·ANALYSIS_500으로 (서버 재시작, AC11). 바꾼 수를 돌려준다. */
    @Transactional
    public int failInterrupted() {
        return flowMapper.failAllPending(ErrorCode.ANALYSIS_500.name(), now());
    }

    private List<FlowResponse> responses(long analysisId) {
        return flowMapper.findByAnalysis(analysisId).stream().map(FlowResponse::from).toList();
    }

    static String key(ApiEndpoint api) {
        return api.method() + " " + api.path();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
