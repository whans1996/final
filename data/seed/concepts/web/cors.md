---
name: CORS
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html
---

## summary

CORS는 브라우저가 다른 출처(도메인·포트)로 보내는 요청을 서버가 허용하는지 확인하는 규칙이다. 프론트(5173)와 백엔드(8080)를 따로 띄우면 바로 만난다.

## body

## 핵심
- 출처는 프로토콜 + 호스트 + 포트다. 포트만 달라도 다른 출처다.
- 브라우저는 응답의 `Access-Control-Allow-Origin` 등 헤더를 보고 허용 여부를 판단한다. 서버가 아니라 브라우저가 막는 것이다.
- JSON 요청이나 Authorization 헤더가 있으면 먼저 OPTIONS 사전 요청(preflight)을 보낸다.

## 주의
- 스프링 시큐리티를 쓰면 시큐리티 설정에서도 CORS를 켜야 preflight가 인증에 막히지 않는다.
- `allowedOrigins("*")`와 자격 증명 허용을 같이 쓸 수 없다. 허용할 출처를 명시한다.

더 읽기: MDN 교차 출처 리소스 공유 https://developer.mozilla.org/ko/docs/Web/HTTP/Guides/CORS

## example

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .allowCredentials(true);
    }
}
```
