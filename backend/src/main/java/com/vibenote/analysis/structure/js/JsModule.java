package com.vibenote.analysis.structure.js;

import java.util.ArrayList;
import java.util.List;

/** 토큰으로 읽은 JS·Vue 파일 하나 (F-21a AC6~AC10). */
public final class JsModule {

    private final long fileId;
    private final String path;
    private final List<JsToken> tokens;
    private final List<JsImport> imports = new ArrayList<>();

    public JsModule(long fileId, String path, List<JsToken> tokens) {
        this.fileId = fileId;
        this.path = path;
        this.tokens = tokens;
    }

    public long fileId() {
        return fileId;
    }

    public String path() {
        return path;
    }

    public List<JsToken> tokens() {
        return tokens;
    }

    public List<JsImport> imports() {
        return imports;
    }
}
