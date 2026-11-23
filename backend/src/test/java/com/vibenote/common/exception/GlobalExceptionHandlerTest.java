package com.vibenote.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;

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
    void 본문이_Jackson_읽기_한도를_넘으면_413() throws Exception {
        JsonMapper limited =
                JsonMapper.builder(
                                JsonFactory.builder()
                                        .streamReadConstraints(
                                                StreamReadConstraints.builder()
                                                        .maxDocumentLength(1000)
                                                        .build())
                                        .build())
                        .build();
        MockMvc limitedMvc =
                MockMvcBuilders.standaloneSetup(new SampleController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new JacksonJsonHttpMessageConverter(limited))
                        .build();

        limitedMvc
                .perform(
                        post("/sample/body")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"" + "a".repeat(20_000) + "\"}"))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("COMMON_413"));
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
    void 본문_Content_Type이_JSON이_아니면_415() throws Exception {
        mockMvc.perform(post("/sample/body").contentType(MediaType.TEXT_PLAIN).content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("COMMON_415"));
    }

    @Test
    void 필수_헤더가_없으면_400() throws Exception {
        mockMvc.perform(get("/sample/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void ResponseStatusException은_그_상태에_맞는_코드() throws Exception {
        mockMvc.perform(get("/sample/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void multipart가_아닌_업로드_요청은_400() throws Exception {
        mockMvc.perform(
                        post("/sample/upload")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
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

        @GetMapping("/sample/header")
        String header(@RequestHeader("X-Sample") String value) {
            return value;
        }

        @GetMapping("/sample/status")
        String responseStatus() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @PostMapping("/sample/upload")
        String upload(@RequestPart("file") MultipartFile file) {
            return file.getOriginalFilename();
        }

        @GetMapping("/sample/boom")
        String boom() {
            throw new IllegalStateException("DB 비밀번호가 틀렸습니다");
        }
    }
}
