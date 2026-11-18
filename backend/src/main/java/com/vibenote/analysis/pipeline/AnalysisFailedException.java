package com.vibenote.analysis.pipeline;

import com.vibenote.common.exception.ErrorCode;

/** 파이프라인을 정해진 사유로 멈춘다. 메시지가 analyses.error_message가 된다 (F-05a 5.3절). */
public class AnalysisFailedException extends RuntimeException {

    private final ErrorCode code;

    public AnalysisFailedException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AnalysisFailedException(ErrorCode code) {
        this(code, code.message());
    }

    public ErrorCode code() {
        return code;
    }
}
