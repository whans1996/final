package com.vibenote.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 예외가 {@code {code, message, details}} 형식과 올바른 상태 코드로 바뀌는지 확인한다 (Spring 컨텍스트 없이 실행). */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new SampleController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void BusinessException은_ErrorCode의_상태와_메시지로_응답한다() throws Exception {
        mockMvc.perform(get("/sample/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void 본문_검증에_실패하면_400과_필드별_사유() throws Exception {
        mockMvc.perform(
                        post("/sample/body")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.name").value("이름을 입력하세요."));
    }

    @Test
    void 본문이_JSON이_아니면_400() throws Exception {
        mockMvc.perform(post("/sample/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 파라미터_타입이_틀리면_400() throws Exception {
        mockMvc.perform(get("/sample/number").param("n", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 없는_주소면_404() throws Exception {
        mockMvc.perform(get("/sample/none"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void 지원하지_않는_메서드면_405() throws Exception {
        mockMvc.perform(post("/sample/forbidden"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON_405"));
    }

    @Test
    void 예상하지_못한_예외는_500이고_내부_메시지를_노출하지_않는다() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_500"))
                .andExpect(jsonPath("$.message").value("서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    record SampleRequest(@NotBlank(message = "이름을 입력하세요.") String name) {}

    /**
     * 컴포넌트 스캔에 잡히지 않도록 static이 아닌 내부 클래스로 둔다 (스캔은 독립 클래스만 등록한다). static으로 바꾸면 통합 테스트 컨텍스트와
     * openapi.json에 이 주소가 섞인다.
     */
    @RestController
    class SampleController {

        @GetMapping("/sample/forbidden")
        String forbidden() {
            throw new BusinessException(ErrorCode.AUTH_403);
        }

        @PostMapping("/sample/body")
        String body(@Valid @RequestBody SampleRequest request) {
            return request.name();
        }

        @GetMapping("/sample/number")
        int number(@RequestParam int n) {
            return n;
        }

        @GetMapping("/sample/boom")
        String boom() {
            throw new IllegalStateException("DB 비밀번호가 틀렸습니다");
        }
    }
}
