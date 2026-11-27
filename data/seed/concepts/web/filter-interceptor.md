---
name: 필터와 인터셉터
category: web
difficulty: SENIOR
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html
---

## summary

필터는 서블릿 컨테이너 단계에서, 인터셉터는 스프링 MVC 단계에서 요청 앞뒤에 공통 처리를 끼워 넣는다. 둘 다 여러 요청에 공통으로 필요한 일을 한곳에 모은다.

## body

## 핵심
- 요청 흐름: 필터 → DispatcherServlet → 인터셉터 → 컨트롤러.
- 필터: 인코딩, 보안(스프링 시큐리티 필터 체인), 요청 로깅처럼 스프링 MVC 바깥에서도 필요한 일.
- 인터셉터(`HandlerInterceptor`): 어떤 컨트롤러가 호출될지 알 수 있어 핸들러별 처리에 적합하다.

## 주의
- 필터에서 던진 예외는 `@RestControllerAdvice`가 잡지 못한다. 필터 안에서 직접 응답을 써야 한다.
- 인터셉터 `preHandle`이 false를 반환하면 컨트롤러가 실행되지 않는다.

## example

```java
public class RequestTimeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute("start", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        long ms = System.currentTimeMillis() - (long) req.getAttribute("start");
        log.info("{} {} {}ms", req.getMethod(), req.getRequestURI(), ms);
    }
}
```

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestTimeInterceptor()).addPathPatterns("/api/**");
    }
}
```
