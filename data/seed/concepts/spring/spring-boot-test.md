---
name: 스프링 부트 테스트 (@SpringBootTest·@WebMvcTest)
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
---

## summary

@SpringBootTest는 애플리케이션 전체를 띄우는 통합 테스트이고, @WebMvcTest는 컨트롤러 계층만 띄우는 슬라이스 테스트다. MockMvc로 실제 서버 없이 HTTP 요청을 보내 검증한다.

## body

## 핵심
- `@SpringBootTest` + `@AutoConfigureMockMvc`: 컨트롤러부터 DB까지 실제 빈으로 검증한다. 느리지만 믿을 만하다.
- `@WebMvcTest(XxxController.class)`: MVC 관련 빈만 올리고 Service는 가짜로 바꾼다. 빠르다.
- MockMvc: `perform(get(...))` → `andExpect(status().isOk())`, `jsonPath("$.field")`로 응답을 검사한다.

## 최신 문법
- Spring Framework 6.2·Boot 3.4부터 가짜 빈은 `@MockitoBean`으로 만든다. 예전 `@MockBean`은 사용 중단(deprecated)되었다.
- AssertJ 스타일의 `MockMvcTester`도 추가되었다.

## 주의
- 테스트마다 다른 설정(`@MockitoBean` 조합, 프로퍼티)을 쓰면 스프링 컨텍스트를 재사용하지 못해 전체 테스트가 느려진다.
- 테스트끼리 DB 데이터가 섞이지 않게 `@Transactional` 롤백이나 정리 스크립트를 쓴다.

## example

```java
@WebMvcTest(ConceptController.class)
class ConceptControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ConceptService conceptService;   // Boot 3.4+

    @Test
    void 카드_상세를_조회한다() throws Exception {
        given(conceptService.get(1L))
                .willReturn(new ConceptResponse(1L, "AOP", "spring"));

        mockMvc.perform(get("/api/concepts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AOP"));
    }
}
```
