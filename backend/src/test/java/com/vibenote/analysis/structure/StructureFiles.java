package com.vibenote.analysis.structure;

import com.vibenote.analysis.domain.AnalysisFile;

/** F-21a 테스트용 가짜 프로젝트 파일. 평가(eval) 데이터는 쓰지 않는다. */
public final class StructureFiles {

    private StructureFiles() {}

    public static AnalysisFile file(long id, String path, String layer, String content) {
        AnalysisFile f = new AnalysisFile();
        f.setId(id);
        f.setAnalysisId(1L);
        f.setPath(path);
        f.setLayer(layer);
        f.setContent(content);
        return f;
    }

    /** src/main/java 아래 .java 파일. layer는 Java 참조 계산에 영향이 없어 service로 둔다. */
    public static AnalysisFile java(long id, String fqn, String content) {
        return file(id, "src/main/java/" + fqn.replace('.', '/') + ".java", "service", content);
    }
}
