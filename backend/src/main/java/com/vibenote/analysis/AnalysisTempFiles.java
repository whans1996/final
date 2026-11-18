package com.vibenote.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 ZIP 임시 보관 {@code {app.upload-dir}/analysis-tmp/{id}.zip}. 파일 이름은 분석 id로만 정하므로 업로드 파일명으로 경로를
 * 조작할 수 없다. 파이프라인이 끝나면 지운다 (F-05a AC10).
 */
@Component
public class AnalysisTempFiles {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTempFiles.class);

    private final Path dir;

    public AnalysisTempFiles(@Value("${app.upload-dir}") String uploadDir) {
        this.dir = Path.of(uploadDir, "analysis-tmp").toAbsolutePath().normalize();
    }

    public Path path(long analysisId) {
        return dir.resolve(analysisId + ".zip");
    }

    /** 업로드 파일을 임시 경로에 쓴다. 실패하면 쓰다 만 파일을 지우고 예외를 던진다. */
    public void save(long analysisId, MultipartFile file) {
        Path target = path(analysisId);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dir);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            delete(analysisId);
            throw new UncheckedIOException("분석 임시 파일 저장 실패: " + analysisId, e);
        }
    }

    /** 남은 임시 ZIP을 모두 지운다 (재시작 복구, AC11). */
    public void deleteAll() {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("분석 임시 폴더 정리 실패: {}", dir, e);
        }
    }

    /** 없으면 무시한다. 지우지 못해도 분석 결과에는 영향이 없으므로 경고만 남긴다. */
    public void delete(long analysisId) {
        try {
            Files.deleteIfExists(path(analysisId));
        } catch (IOException e) {
            log.warn("분석 임시 파일 삭제 실패: {}", path(analysisId), e);
        }
    }
}
