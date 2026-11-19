package com.vibenote.analysis.flow;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 실행 흐름 요청 (F-07 AC8). 중복 (method, path)는 FlowService가 COMMON_400으로 막는다. */
@Schema(description = "실행 흐름 요청")
public record CreateFlowsRequest(
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "흐름을 만들 API (개요 apis에서 1~3개, 중복 불가)"),
                        minItems = 1,
                        maxItems = 3)
                @NotEmpty(message = "실행 흐름을 만들 API를 하나 이상 고르세요.")
                @Size(min = 1, max = 3, message = "API는 한 번에 1~3개를 고를 수 있습니다.")
                List<@NotNull(message = "API를 입력하세요.") @Valid Endpoint> endpoints) {

    @Schema(description = "API 하나 (개요 apis의 method·path 그대로)")
    public record Endpoint(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "HTTP 메서드 (개요 apis 값, ANY 포함)",
                            example = "POST")
                    @NotBlank(message = "method를 입력하세요.")
                    String method,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "경로",
                            example = "/api/todos")
                    @NotBlank(message = "path를 입력하세요.")
                    String path) {

        String key() {
            return method + " " + path;
        }
    }
}
