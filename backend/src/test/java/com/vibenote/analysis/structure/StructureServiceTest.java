package com.vibenote.analysis.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vibenote.analysis.AnalysisDeletedEvent;
import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** F-21a 5.3절 4: 구조 결과 캐시와 동시 계산 상한 (docs/specs/F-21a-structure-graph.md). */
class StructureServiceTest {

    private static final long USER = 7L;

    private final AnalysisService analysisService = mock(AnalysisService.class);
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private StructureCache cache;

    @AfterEach
    void shutdown() {
        pool.shutdownNow();
    }

    private StructureService service(
            int cacheSize, int maxConcurrent, Duration queueTimeout, long maxWorkSteps) {
        StructureProperties properties =
                new StructureProperties(
                        150,
                        100,
                        maxWorkSteps,
                        Duration.ofSeconds(3),
                        cacheSize,
                        maxConcurrent,
                        queueTimeout);
        cache = new StructureCache(properties);
        return new StructureService(analysisService, properties, cache);
    }

    private StructureService service() {
        return service(64, 2, Duration.ofSeconds(10), 20_000_000);
    }

    private static List<AnalysisFile> files(long id) {
        return List.of(
                StructureFiles.java(
                        id * 10, "com.example.A", "package com.example; class A { B b; }"),
                StructureFiles.java(
                        id * 10 + 1, "com.example.B", "package com.example; class B {}"));
    }

    private void viewable(long id, AnalysisStatus status) {
        AnalysisDetail detail = new AnalysisDetail();
        detail.setId(id);
        detail.setStatus(status);
        detail.setOverview(new AnalysisOverview("요약", List.of(), List.of(), List.of(), List.of()));
        when(analysisService.checkViewableDetail(USER, id)).thenReturn(detail);
        when(analysisService.findFiles(id)).thenReturn(files(id));
    }

    /** findFiles(id)가 release를 기다리게 한다. 들어오면 entered를 센다. */
    private void blockFiles(long id, CountDownLatch entered, CountDownLatch release) {
        when(analysisService.findFiles(id))
                .thenAnswer(
                        inv -> {
                            entered.countDown();
                            release.await(10, TimeUnit.SECONDS);
                            return files(id);
                        });
    }

    @Test
    void AC12_DONE_분석의_구조는_한_번_계산해_다시_쓴다() {
        StructureService service = service();
        viewable(1, AnalysisStatus.DONE);

        AnalysisStructureResponse first = service.get(USER, 1);
        AnalysisStructureResponse second = service.get(USER, 1);

        assertThat(first.edges()).hasSize(1);
        assertThat(second).isEqualTo(first);
        verify(analysisService, times(1)).findFiles(1);
        verify(analysisService, times(2)).checkViewableDetail(USER, 1);
    }

    @Test
    void AC12_진행_중이거나_실패한_분석은_캐시하지_않는다() {
        StructureService service = service();
        viewable(1, AnalysisStatus.EXPLAINING);
        viewable(2, AnalysisStatus.FAILED);

        service.get(USER, 1);
        service.get(USER, 1);
        service.get(USER, 2);
        service.get(USER, 2);

        verify(analysisService, times(2)).findFiles(1);
        verify(analysisService, times(2)).findFiles(2);
    }

    @Test
    void AC13_캐시에_있어도_매_요청_열람_권한을_본다() {
        StructureService service = service();
        viewable(1, AnalysisStatus.DONE);
        service.get(USER, 1);

        when(analysisService.checkViewableDetail(USER, 1))
                .thenThrow(new BusinessException(ErrorCode.ANALYSIS_404));

        assertThatThrownBy(() -> service.get(USER, 1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANALYSIS_404);
    }

    @Test
    void AC12_분석이_삭제되면_캐시를_비운다() {
        StructureService service = service();
        viewable(1, AnalysisStatus.DONE);
        service.get(USER, 1);
        assertThat(cache.contains(1)).isTrue();

        cache.evict(new AnalysisDeletedEvent(1));

        assertThat(cache.contains(1)).isFalse();
        service.get(USER, 1);
        verify(analysisService, times(2)).findFiles(1);
    }

    @Test
    void AC12_작업_예산을_다_쓴_결과는_캐시하지_않는다() {
        StructureService service = service(64, 2, Duration.ofSeconds(10), 1);
        viewable(1, AnalysisStatus.DONE);

        assertThat(service.get(USER, 1).edgesTruncated()).isTrue();
        service.get(USER, 1);

        verify(analysisService, times(2)).findFiles(1);
        assertThat(cache.contains(1)).isFalse();
    }

    @Test
    void AC12_캐시는_크기_상한을_넘으면_가장_오래_안_쓴_것부터_뺀다() {
        StructureService service = service(2, 2, Duration.ofSeconds(10), 20_000_000);
        viewable(1, AnalysisStatus.DONE);
        viewable(2, AnalysisStatus.DONE);
        viewable(3, AnalysisStatus.DONE);

        service.get(USER, 1);
        service.get(USER, 2);
        service.get(USER, 1);
        service.get(USER, 3);

        assertThat(cache.contains(1)).isTrue();
        assertThat(cache.contains(2)).isFalse();
        assertThat(cache.contains(3)).isTrue();
    }

    @Test
    void AC12_같은_분석을_동시에_요청하면_한_번만_계산한다() throws Exception {
        StructureService service = service();
        viewable(1, AnalysisStatus.DONE);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockFiles(1, entered, release);

        Future<AnalysisStructureResponse> first = pool.submit(() -> service.get(USER, 1));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        Future<AnalysisStructureResponse> second = pool.submit(() -> service.get(USER, 1));
        Thread.sleep(200);
        release.countDown();

        assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(first.get(5, TimeUnit.SECONDS));
        verify(analysisService, times(1)).findFiles(1);
    }

    @Test
    void AC12_동시_계산이_상한이면_기다렸다가_자리가_나면_계산하고_시간이_지나면_ANALYSIS_007() throws Exception {
        StructureService service = service(64, 1, Duration.ofMillis(300), 20_000_000);
        viewable(1, AnalysisStatus.DONE);
        viewable(2, AnalysisStatus.DONE);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockFiles(1, entered, release);

        Future<AnalysisStructureResponse> busy = pool.submit(() -> service.get(USER, 1));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> service.get(USER, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANALYSIS_007);
        assertThat(ErrorCode.ANALYSIS_007.status().value()).isEqualTo(503);

        Future<AnalysisStructureResponse> waiting = pool.submit(() -> service.get(USER, 2));
        Thread.sleep(100);
        release.countDown();
        assertThat(busy.get(5, TimeUnit.SECONDS).edges()).hasSize(1);
        assertThat(waiting.get(5, TimeUnit.SECONDS).edges()).hasSize(1);
    }

    @Test
    void AC11_매핑_조건만_다른_같은_API가_여러_파일에_있으면_간선을_확정하지_않는다() {
        // #122: 개요 API 목록은 method+path로 중복을 뺀다. 빠진 다른 파일 매핑도 후보로 넘겨 AMBIGUOUS로 둔다
        List<AnalysisFile> files =
                List.of(
                        StructureFiles.file(
                                2,
                                "src/main/java/web/A.java",
                                "controller",
                                "@RestController class A { @GetMapping(value = \"/todos\","
                                        + " params = \"kind=a\") void a() {} }"),
                        StructureFiles.file(
                                3,
                                "src/main/java/web/B.java",
                                "controller",
                                "@RestController class B { @GetMapping(value = \"/todos\","
                                        + " params = \"kind=b\") void b() {} }"),
                        StructureFiles.file(
                                10, "src/api/todo.js", "js", "fetch('/todos?kind=b')\n"));
        AnalysisDetail detail = new AnalysisDetail();
        detail.setId(9L);
        detail.setStatus(AnalysisStatus.DONE);
        detail.setOverview(
                new AnalysisOverview(
                        "요약", List.of(), List.of(), List.of(), ApiDetector.detect(files)));
        when(analysisService.checkViewableDetail(USER, 9)).thenReturn(detail);
        when(analysisService.findFiles(9)).thenReturn(files);

        AnalysisStructureResponse response = service().get(USER, 9);

        assertThat(response.edges()).noneMatch(e -> e.type() == EdgeType.HTTP);
        assertThat(response.unresolved())
                .filteredOn(u -> u.kind() == UnresolvedKind.HTTP_CALL)
                .extracting(UnresolvedLink::reason)
                .containsExactly(UnresolvedReason.AMBIGUOUS);
    }

    @Test
    void AC11_저장된_개요_목록에_없는_API의_중복은_후보로_넣지_않는다() {
        // #122 리뷰: 다시 훑은 탐지가 남긴 쪽(파일 2)이 저장된 개요에 없으면 중복(파일 3)만 후보가 되어 틀린 간선을 확정할 수 있다
        List<AnalysisFile> files =
                List.of(
                        StructureFiles.file(
                                2,
                                "src/main/java/web/A.java",
                                "controller",
                                "@RestController class A { @GetMapping(\"/todos\") void a() {} }"),
                        StructureFiles.file(
                                3,
                                "src/main/java/web/B.java",
                                "controller",
                                "@RestController class B { @GetMapping(\"/todos\") void b() {} }"),
                        StructureFiles.file(10, "src/api/todo.js", "js", "fetch('/todos')\n"));
        AnalysisDetail detail = new AnalysisDetail();
        detail.setId(8L);
        detail.setStatus(AnalysisStatus.DONE);
        detail.setOverview(new AnalysisOverview("요약", List.of(), List.of(), List.of(), List.of()));
        when(analysisService.checkViewableDetail(USER, 8)).thenReturn(detail);
        when(analysisService.findFiles(8)).thenReturn(files);

        AnalysisStructureResponse response = service().get(USER, 8);

        assertThat(response.edges()).noneMatch(e -> e.type() == EdgeType.HTTP);
        assertThat(response.unresolved())
                .filteredOn(u -> u.kind() == UnresolvedKind.HTTP_CALL)
                .extracting(UnresolvedLink::reason)
                .containsExactly(UnresolvedReason.NO_MATCHING_API);
    }
}
