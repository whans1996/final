package com.vibenote.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * MySQL JSON 컬럼 ↔ Java 객체 변환 (analyses.overview, analysis_flows.steps, quiz_questions.choices).
 *
 * <p>MyBatis가 {@code javaType}을 생성자 인자로 넘겨 주므로 매핑에 javaType을 함께 적는다. 목록은 제네릭 정보가 사라지므로 배열 타입(JVM 이름
 * {@code [Lpkg.Type;}, {@code Type[]}은 MyBatis가 못 읽는다)이나 {@code JsonNode}(Jackson 3, {@code
 * tools.jackson.databind})를 쓴다.
 *
 * <p>Jackson 3 기본값을 따른다 (11-18 결정). JSON의 null을 primitive 필드로 읽으면 0이 아니라 예외가 나므로, null이 올 수 있는 필드는
 * 래퍼 타입({@code Integer} 등)으로 선언한다. 날짜는 ISO 문자열로 쓰고 모르는 속성은 무시한다(아래 두 설정은 Jackson 3 기본값과 같지만 의도를
 * 드러내려고 적어 둔다).
 *
 * <pre>{@code
 * <result column="steps" property="steps" javaType="[Lcom.vibenote.analysis.flow.FlowStep;"
 *         typeHandler="com.vibenote.common.mybatis.JsonTypeHandler"/>
 * #{steps, javaType=[Lcom.vibenote.analysis.flow.FlowStep;,
 *          typeHandler=com.vibenote.common.mybatis.JsonTypeHandler}
 * }</pre>
 */
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper MAPPER =
            JsonMapper.builder()
                    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

    private final Class<T> type;

    public JsonTypeHandler(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("JsonTypeHandler에는 javaType이 필요합니다.");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, write(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return read(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return read(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return read(cs.getString(columnIndex));
    }

    private String write(T value) throws SQLException {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new SQLException("JSON 직렬화 실패: " + type.getName(), e);
        }
    }

    private T read(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new SQLException("JSON 역직렬화 실패: " + type.getName(), e);
        }
    }
}
