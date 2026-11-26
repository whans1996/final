package com.vibenote.prompteval;

import com.vibenote.analysis.AnalysisProperties;
import com.vibenote.analysis.concept.ConceptExtractProperties;
import com.vibenote.analysis.explain.ExplainProperties;
import com.vibenote.analysis.flow.FlowProperties;
import com.vibenote.analysis.pipeline.SelectionProperties;
import com.vibenote.quiz.QuizProperties;
import java.util.Map;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** 러너가 앱과 같은 값으로 쓰는 분석 설정 묶음. 실행할 때는 {@code application.yml}, 테스트는 기본값이다. */
record EvalSettings(
        AnalysisProperties analysis,
        SelectionProperties selection,
        ExplainProperties explain,
        FlowProperties flow,
        ConceptExtractProperties concept,
        QuizProperties quiz) {

    static EvalSettings from(Binder binder) {
        return new EvalSettings(
                binder.bindOrCreate("analysis", AnalysisProperties.class),
                binder.bindOrCreate("analysis.selection", SelectionProperties.class),
                binder.bindOrCreate("analysis.explain", ExplainProperties.class),
                binder.bindOrCreate("analysis.flow", FlowProperties.class),
                binder.bindOrCreate("analysis.concept", ConceptExtractProperties.class),
                binder.bindOrCreate("quiz", QuizProperties.class));
    }

    /** {@code @DefaultValue} 기본값. */
    static EvalSettings defaults() {
        return from(new Binder(new MapConfigurationPropertySource(Map.of())));
    }
}
