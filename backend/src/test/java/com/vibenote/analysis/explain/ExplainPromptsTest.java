package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.ApiDetectorTest.file;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-06 AC3 프롬프트 입력 구성 (자르기, 구분자, 개요 입력 상한). */
class ExplainPromptsTest {

    @Test
    void AC3_6000자를_넘으면_앞부분만_넣고_생략_줄을_붙인다() {
        String content = "a".repeat(6000) + "b".repeat(250);

        String block = ExplainPrompts.fileBlock(2, file(9, "src/A.java", "service", content), 6000);

        assertThat(block)
                .isEqualTo(
                        "<<<FILE no=2 path=src/A.java layer=service>>>\n"
                                + "a".repeat(6000)
                                + "\n// ... (이하 250자 생략)\n"
                                + "<<<END FILE>>>");
    }

    @Test
    void AC3_6000자_이하면_그대로_넣는다() {
        AnalysisFile f = file(9, "src/A.java", "service", "x".repeat(6000));

        assertThat(ExplainPrompts.fileBlock(1, f, 6000))
                .isEqualTo(
                        "<<<FILE no=1 path=src/A.java layer=service>>>\n"
                                + "x".repeat(6000)
                                + "\n<<<END FILE>>>");
        assertThat(f.getContent()).hasSize(6000);
    }

    @Test
    void AC3_묶음은_1부터_번호를_매긴다() {
        String files =
                ExplainPrompts.files(
                        List.of(
                                file(5, "A.java", "controller", "class A {}"),
                                file(6, "B.vue", "vue", "<template/>")),
                        6000);

        assertThat(files)
                .contains(
                        "<<<FILE no=1 path=A.java layer=controller>>>\nclass A {}\n<<<END FILE>>>")
                .contains("<<<FILE no=2 path=B.vue layer=vue>>>\n<template/>\n<<<END FILE>>>");
    }

    @Test
    void AC3_개요_입력은_config_controller_service_순으로_파일당_3000자까지_24000자_안에서_채운다() {
        List<AnalysisFile> files = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 6; i++) {
            files.add(file(id++, "web/C" + i + ".java", "controller", "c".repeat(5000)));
        }
        for (int i = 0; i < 6; i++) {
            files.add(file(id++, "svc/S" + i + ".java", "service", "s".repeat(5000)));
        }
        files.add(file(id++, "map/M.java", "mapper", "m".repeat(100)));
        files.add(file(id, "application.yml", "config", "y".repeat(10)));
        List<ApiEndpoint> apis = List.of(new ApiEndpoint("GET", "/api/x", 1L));

        ExplainPrompts.OverviewInput input = ExplainPrompts.overviewInput(files, apis, 24000);

        assertThat(input.totalLength()).isLessThanOrEqualTo(24000);
        assertThat(input.fileTree())
                .contains(
                        "web/C0.java [controller]",
                        "map/M.java [mapper]",
                        "application.yml [config]");
        assertThat(input.apis()).contains("GET /api/x (web/C0.java)");
        assertThat(input.sources())
                .startsWith("<<<FILE no=1 path=application.yml layer=config>>>")
                .contains("path=web/C0.java", "// ... (이하 2000자 생략)")
                .doesNotContain("map/M.java")
                .doesNotContain("c".repeat(3001));
        // controller 6개 다음 service가 들어갈 자리가 모자라면 멈춘다
        int controllers = input.sources().split("layer=controller").length - 1;
        int services = input.sources().split("layer=service").length - 1;
        assertThat(controllers).isEqualTo(6);
        assertThat(services).isLessThan(6);
    }

    @Test
    void AC3_API가_없으면_없다고_적는다() {
        ExplainPrompts.OverviewInput input =
                ExplainPrompts.overviewInput(List.of(file(1, "a.js", "js", "x")), List.of(), 24000);

        assertThat(input.apis()).isEqualTo("(탐지된 API 없음)");
        assertThat(input.sources()).isEmpty();
    }
}
