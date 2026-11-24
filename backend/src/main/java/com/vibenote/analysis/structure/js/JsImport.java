package com.vibenote.analysis.structure.js;

import java.util.Map;

/**
 * import·export from·import()·require() 하나 (F-21a AC6).
 *
 * @param bindings 지역 이름 → 가져온 이름 ({@code default}, {@code *}, 또는 내보낸 이름)
 */
public final class JsImport {

    private final String specifier;
    private final int line;
    private final Map<String, String> bindings;
    private Long target;

    public JsImport(String specifier, int line, Map<String, String> bindings) {
        this.specifier = specifier;
        this.line = line;
        this.bindings = bindings;
    }

    public String specifier() {
        return specifier;
    }

    public int line() {
        return line;
    }

    public Map<String, String> bindings() {
        return bindings;
    }

    /** 풀린 선별 파일 ID. 패키지이거나 선별되지 않았으면 null. */
    public Long target() {
        return target;
    }

    void setTarget(Long target) {
        this.target = target;
    }
}
