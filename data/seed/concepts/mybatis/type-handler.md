---
name: TypeHandler (JSON·커스텀 타입 매핑)
category: mybatis
difficulty: SENIOR
reference: https://mybatis.org/mybatis-3/ko/configuration.html#typeHandlers
---

## summary

TypeHandler는 자바 타입과 DB 컬럼 값을 서로 어떻게 바꿀지 정하는 MyBatis 확장 지점이다. JSON 컬럼을 객체로 읽거나, 값 객체를 문자열로 저장할 때 쓴다.

## body

## 핵심
- `BaseTypeHandler<T>`를 상속해 저장(`setNonNullParameter`)과 조회(`getNullableResult` 3종)를 구현한다.
- enum, java.time, 기본 타입은 MyBatis가 이미 처리한다. 직접 만들 일은 JSON·값 객체·암호화 컬럼 정도다.
- 사용 위치: resultMap의 `<result typeHandler="...">`, 파라미터 `#{field, typeHandler=...}`, 또는 패키지 등록(`mybatis.type-handlers-package`)으로 전역 적용.

## 주의
- 전역 등록하면 같은 자바 타입을 쓰는 모든 매핑에 적용된다. 범위가 넓으면 의도하지 않은 컬럼까지 바뀐다. 필요한 매핑에만 지정하는 편이 안전하다.
- 제네릭 타입(`List<FlowStep>`)은 실행 시점에 타입 정보가 지워지므로 배열 타입(`FlowStep[]`)이나 타입 정보를 넘기는 방식으로 처리한다.
- JSON 파싱 실패 처리를 빠뜨리면 조회 전체가 실패한다.

## example

```java
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Class<T> type;

    public JsonTypeHandler(Class<T> type) { this.type = type; }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T value, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 변환 실패", e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String column) throws SQLException {
        return parse(rs.getString(column));
    }
    // getNullableResult(ResultSet, int), getNullableResult(CallableStatement, int), parse 생략
}
```

```xml
<result property="steps" column="steps" javaType="com.example.FlowStep[]"
        typeHandler="com.example.common.mybatis.JsonTypeHandler"/>
```
