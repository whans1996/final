package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class LlmUsageLogMapperTest extends IntegrationTestBase {

    @Autowired LlmUsageLogMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void AC6_insert() {
        mapper.insert(
                new LlmUsageLog(
                        null, 42L, "overview", "gemini-test", 10, 20, 1234, false, "Gemini 429"));

        Map<String, Object> row =
                jdbc.queryForMap(
                        "SELECT * FROM llm_usage_logs WHERE analysis_id = 42 ORDER BY id DESC"
                                + " LIMIT 1");
        assertThat(row)
                .containsEntry("user_id", null)
                .containsEntry("stage", "overview")
                .containsEntry("model", "gemini-test")
                .containsEntry("input_tokens", 10)
                .containsEntry("output_tokens", 20)
                .containsEntry("latency_ms", 1234)
                .containsEntry("success", false)
                .containsEntry("error_message", "Gemini 429");
        assertThat(row.get("created_at")).isNotNull();
    }
}
