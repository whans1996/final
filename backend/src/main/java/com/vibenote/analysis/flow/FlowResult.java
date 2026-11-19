package com.vibenote.analysis.flow;

import java.util.List;

/** 검증·정리를 마친 흐름 생성 결과 (F-07 AC4). */
public record FlowResult(String summary, List<FlowStep> steps) {}
