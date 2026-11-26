package com.vibenote.prompteval;

import java.nio.file.Path;

/**
 * 평가 러너가 돌리는 저장소 하나.
 *
 * @param id {@code data/datasets.json}의 저장소 id 또는 ZIP 파일 이름
 * @param zip 실제로 읽는 ZIP (데이터셋이면 {@code paths}만 남긴 캐시)
 * @param name 분석의 프로젝트 이름 (저장소 이름)
 * @param split {@code dev}·{@code eval}·{@code demo}. ZIP 경로로 주면 {@code unknown}
 * @param contamination 오염 그룹 ({@code none}·{@code low}·{@code high}). 모르면 {@code unknown}
 */
record EvalFixture(String id, Path zip, String name, String split, String contamination) {}
