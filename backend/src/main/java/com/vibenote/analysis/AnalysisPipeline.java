package com.vibenote.analysis;

import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.pipeline.FileSelector;
import com.vibenote.analysis.pipeline.SecretRedactor;
import com.vibenote.analysis.pipeline.SelectedFile;
import com.vibenote.analysis.pipeline.SourceFile;
import com.vibenote.analysis.pipeline.ZipArchiveReader;
import com.vibenote.common.exception.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 분석 파이프라인 (docs/specs/F-05a-analysis-request-zip.md 5절). 단계마다 status·progress를 기록하고, 정해진 사유로 멈추면 그
 * 사유로, 예기치 못한 예외면 ANALYSIS_500으로 FAILED 처리한다. 결과와 상관없이 임시 ZIP을 지운다 (AC10).
 *
 * <p>단계 목록은 {@link #stages} 한 곳에 둔다. F-06·F-08·F-14가 SELECTING 뒤에
 * EXPLAINING(40)·EXTRACTING(70)·QUIZ(85)를 끼워 넣는다.
 */
@Component
public class AnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final AnalysisService analysisService;
    private final AnalysisTempFiles tempFiles;
    private final ZipArchiveReader zipReader;
    private final AnalysisProperties properties;
    private final List<Stage> stages;

    public AnalysisPipeline(
            AnalysisService analysisService,
            AnalysisTempFiles tempFiles,
            ZipArchiveReader zipReader,
            AnalysisProperties properties) {
        this.analysisService = analysisService;
        this.tempFiles = tempFiles;
        this.zipReader = zipReader;
        this.properties = properties;
        this.stages =
                List.of(
                        new Stage(AnalysisStatus.FETCHING, 5, this::fetch),
                        new Stage(AnalysisStatus.FILTERING, 15, this::filter),
                        new Stage(AnalysisStatus.SELECTING, 25, this::select));
    }

    /** 분석 하나를 끝까지 실행한다. 분석 전용 실행기 스레드에서 부른다. */
    public void run(long analysisId) {
        Context context = new Context(analysisId);
        try {
            for (Stage stage : stages) {
                if (!analysisService.advance(analysisId, stage.status(), stage.progress())) {
                    log.info("분석 {}이 없거나 이미 끝나 파이프라인을 멈춤", analysisId);
                    return;
                }
                stage.work().accept(context);
            }
            analysisService.complete(analysisId);
        } catch (AnalysisFailedException e) {
            analysisService.markFailed(analysisId, e.code(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("분석 {} 파이프라인 오류", analysisId, e);
            analysisService.markFailed(
                    analysisId, ErrorCode.ANALYSIS_500, ErrorCode.ANALYSIS_500.message());
        } catch (Error e) {
            // 진행 중 상태로 남으면 화면이 계속 폴링하므로 FAILED로 바꾸고 스레드에는 그대로 알린다
            log.error("분석 {} 파이프라인 치명적 오류", analysisId, e);
            analysisService.markFailed(
                    analysisId, ErrorCode.ANALYSIS_500, ErrorCode.ANALYSIS_500.message());
            throw e;
        } finally {
            tempFiles.delete(analysisId);
        }
    }

    /** ZIP은 요청 때 이미 받아 두었다. URL 수집(F-05b)은 여기서 아카이브를 내려받는다. */
    private void fetch(Context context) {
        Path archive = tempFiles.path(context.analysisId);
        if (!Files.isRegularFile(archive)) {
            throw new IllegalStateException("분석 임시 파일 없음: " + archive);
        }
        context.archive = archive;
    }

    private void filter(Context context) {
        context.files = zipReader.read(context.archive);
    }

    /** 선별한 파일만 키를 가려 저장한다. 이후 단계는 저장된 analysis_files를 입력으로 쓴다. */
    private void select(Context context) {
        List<SelectedFile> selected =
                FileSelector.select(context.files, properties.maxSelectedFiles()).stream()
                        .map(
                                f ->
                                        new SelectedFile(
                                                f.path(),
                                                f.layer(),
                                                SecretRedactor.redact(f.path(), f.content())))
                        .toList();
        analysisService.saveFiles(context.analysisId, selected);
    }

    /** 진입할 때 status·progress를 기록하는 단계 하나. */
    private record Stage(AnalysisStatus status, int progress, Consumer<Context> work) {}

    /** 단계 사이에 넘기는 값. */
    private static final class Context {
        private final long analysisId;
        private Path archive;
        private List<SourceFile> files;

        private Context(long analysisId) {
            this.analysisId = analysisId;
        }
    }
}
