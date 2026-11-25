package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.pipeline.Layer;
import com.vibenote.analysis.pipeline.SelectedFile;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 인덱스 저장 묶음 (F-26a 5.5절). 행 수와 내용 길이 중 먼저 닿는 쪽에서 끊는다. */
class AnalysisServiceChunkTest {

    private static List<SelectedFile> files(int count, int chars) {
        return IntStream.range(0, count)
                .mapToObj(i -> new SelectedFile("F" + i + ".java", Layer.OTHER, "x".repeat(chars)))
                .toList();
    }

    private static List<Integer> sizes(List<List<SelectedFile>> chunks) {
        return chunks.stream().map(List::size).toList();
    }

    @Test
    void F26a_AC8_작은_파일은_100행씩_나눈다() {
        assertThat(sizes(AnalysisService.chunks(files(250, 10)))).containsExactly(100, 100, 50);
    }

    @Test
    void F26a_AC8_내용_합계가_상한을_넘기_전에_묶음을_끊는다() {
        int third = AnalysisService.INSERT_CHUNK_CHARS / 3 + 1;

        assertThat(sizes(AnalysisService.chunks(files(5, third)))).containsExactly(2, 2, 1);
    }

    @Test
    void F26a_AC8_상한보다_큰_파일_하나는_혼자_묶음이_된다() {
        List<SelectedFile> files =
                List.of(
                        files(1, 10).getFirst(),
                        files(1, AnalysisService.INSERT_CHUNK_CHARS + 1).getFirst(),
                        files(1, 10).getFirst());

        assertThat(sizes(AnalysisService.chunks(files))).containsExactly(1, 1, 1);
        assertThat(AnalysisService.chunks(List.of())).isEmpty();
    }
}
