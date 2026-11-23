package com.vibenote.analysis;

import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.concept.ConceptExtractor;
import com.vibenote.analysis.concept.ConceptLinkService;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.analysis.explain.ExplainProperties;
import com.vibenote.analysis.explain.FileExplainer;
import com.vibenote.analysis.explain.OverviewExplainer;
import com.vibenote.analysis.fetch.RepositoryArchiveFetcher;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.pipeline.FileSelector;
import com.vibenote.analysis.pipeline.SecretRedactor;
import com.vibenote.analysis.pipeline.SelectedFile;
import com.vibenote.analysis.pipeline.SourceFile;
import com.vibenote.analysis.pipeline.ZipArchiveReader;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptDraftService;
import com.vibenote.quiz.QuizGenerator;
import com.vibenote.quiz.QuizSaveService;
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
 * <p>단계 목록은 {@link #stages} 한 곳에 둔다. F-06이 SELECTING 뒤에 EXPLAINING(40)을, F-08이 그 뒤에 EXTRACTING(70)을
 * 넣었고, F-14가 그 뒤에 QUIZ(85)를 넣었다.
 */
@Component
public class AnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final AnalysisService analysisService;
    private final AnalysisTempFiles tempFiles;
    private final ZipArchiveReader zipReader;
    private final RepositoryArchiveFetcher archiveFetcher;
    private final AnalysisProperties properties;
    private final List<Stage> stages;
    private final OverviewExplainer overviewExplainer;
    private final FileExplainer fileExplainer;
    private final ExplainProperties explainProperties;
    private final ConceptDraftService conceptDraftService;
    private final ConceptExtractor conceptExtractor;
    private final ConceptLinkService conceptLinkService;
    private final AnalysisConceptService analysisConceptService;
    private final QuizGenerator quizGenerator;
    private final QuizSaveService quizSaveService;

    /** EXPLAINING 진행률: 개요 뒤 45, 파일 묶음 k/n 뒤 45 + floor(24·k/n) (F-06 AC2). */
    static final int EXPLAIN_OVERVIEW_DONE = 45;

    static final int EXPLAIN_FILES_SPAN = 24;

    public AnalysisPipeline(
            AnalysisService analysisService,
            AnalysisTempFiles tempFiles,
            ZipArchiveReader zipReader,
            RepositoryArchiveFetcher archiveFetcher,
            AnalysisProperties properties,
            OverviewExplainer overviewExplainer,
            FileExplainer fileExplainer,
            ExplainProperties explainProperties,
            ConceptDraftService conceptDraftService,
            ConceptExtractor conceptExtractor,
            ConceptLinkService conceptLinkService,
            AnalysisConceptService analysisConceptService,
            QuizGenerator quizGenerator,
            QuizSaveService quizSaveService) {
        this.analysisService = analysisService;
        this.tempFiles = tempFiles;
        this.zipReader = zipReader;
        this.archiveFetcher = archiveFetcher;
        this.properties = properties;
        this.overviewExplainer = overviewExplainer;
        this.fileExplainer = fileExplainer;
        this.explainProperties = explainProperties;
        this.conceptDraftService = conceptDraftService;
        this.conceptExtractor = conceptExtractor;
        this.conceptLinkService = conceptLinkService;
        this.analysisConceptService = analysisConceptService;
        this.quizGenerator = quizGenerator;
        this.quizSaveService = quizSaveService;
        this.stages =
                List.of(
                        new Stage(AnalysisStatus.FETCHING, 5, this::fetch),
                        new Stage(AnalysisStatus.FILTERING, 15, this::filter),
                        new Stage(AnalysisStatus.SELECTING, 25, this::select),
                        new Stage(AnalysisStatus.EXPLAINING, 40, this::explain),
                        new Stage(AnalysisStatus.EXTRACTING, 70, this::extract),
                        new Stage(AnalysisStatus.QUIZ, 85, this::quiz));
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

    /**
     * ZIP은 요청 때 이미 받아 두었다. 저장소 URL(F-05b)은 여기서 기본 브랜치 아카이브를 같은 임시 경로로 받으므로, 이후 단계·{@code finally}
     * 삭제·재시작 복구가 그대로 적용된다.
     */
    private void fetch(Context context) {
        Path archive = tempFiles.path(context.analysisId);
        Analysis analysis = analysisService.findForPipeline(context.analysisId);
        if (analysis.getSourceType() == SourceType.URL) {
            archiveFetcher.download(RepositoryUrl.parse(analysis.getSourceUrl()), archive);
        } else if (!Files.isRegularFile(archive)) {
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

    /**
     * 개요 1회 + 파일 묶음(6개) 설명 (F-06). 개요가 실패하면 분석을 멈추고(AnalysisFailedException), 묶음 실패는 그 묶음만 비워 두고
     * 계속한다. LLM 호출은 트랜잭션 밖에서 하고 결과 저장만 짧은 트랜잭션으로 한다.
     */
    private void explain(Context context) {
        long id = context.analysisId;
        Analysis analysis = analysisService.findForPipeline(id);
        List<AnalysisFile> files = analysisService.findFiles(id);
        AnalysisOverview overview =
                overviewExplainer.explain(analysis, files, ApiDetector.detect(files));
        analysisService.saveOverview(id, overview);
        if (!analysisService.advance(id, AnalysisStatus.EXPLAINING, EXPLAIN_OVERVIEW_DONE)) {
            return;
        }
        int size = explainProperties.batchSize();
        int batches = (files.size() + size - 1) / size;
        for (int k = 1; k <= batches; k++) {
            List<AnalysisFile> batch =
                    files.subList((k - 1) * size, Math.min(k * size, files.size()));
            analysisService.saveFileExplanations(
                    id, fileExplainer.explain(analysis, overview.summary(), batch));
            int progress = EXPLAIN_OVERVIEW_DONE + EXPLAIN_FILES_SPAN * k / batches;
            if (!analysisService.advance(id, AnalysisStatus.EXPLAINING, progress)) {
                return;
            }
        }
    }

    /**
     * 개념 추출 1회 (F-08). 추출이 실패해도 연결 없이 계속한다 (ConceptExtractor). LLM은 트랜잭션 밖에서 부르고, 새 초안과 연결은 한
     * 트랜잭션으로 저장한다. 저장 중 예외는 ANALYSIS_500이다.
     */
    private void extract(Context context) {
        long id = context.analysisId;
        Analysis analysis = analysisService.findForPipeline(id);
        conceptLinkService.save(
                id,
                conceptExtractor.extract(
                        analysis,
                        conceptDraftService.findAllForMatching(),
                        analysisService.findFiles(id)));
    }

    /**
     * 퀴즈 생성 1회 (F-14). 생성이 실패하면 퀴즈 없이 계속한다 (QuizGenerator). LLM은 트랜잭션 밖에서 부르고 퀴즈와 문항은 한 트랜잭션으로
     * 저장한다. 저장 중 예외는 ANALYSIS_500이다.
     */
    private void quiz(Context context) {
        long id = context.analysisId;
        quizSaveService.save(
                id,
                quizGenerator.generate(
                        analysisService.findForPipeline(id),
                        analysisConceptService.findForQuiz(id),
                        analysisService.findFiles(id)));
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
