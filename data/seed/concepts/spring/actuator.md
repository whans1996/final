---
name: Actuator (헬스 체크·메트릭)
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-boot/reference/actuator/index.html
---

## summary

Spring Boot Actuator는 애플리케이션 상태 확인(health), 메트릭, 설정 정보 같은 운영용 엔드포인트를 /actuator 아래에 제공한다.

## body

## 핵심
- `spring-boot-starter-actuator` 의존성을 추가하면 `/actuator/health`가 열린다. DB 연결 같은 하위 상태도 함께 점검한다.
- 웹에는 기본적으로 health만 노출된다. 나머지는 `management.endpoints.web.exposure.include`로 고른다.
- 로드밸런서·컨테이너 오케스트레이터의 헬스 체크, 배포 후 확인에 쓴다.
- 메트릭은 Micrometer로 수집해 Prometheus 같은 모니터링 도구로 보낸다.

## 주의
- `env`, `heapdump`, `configprops`는 비밀 값이나 메모리 내용이 드러날 수 있다. 공개 포트에 열지 않는다.
- `include: "*"` 설정은 개발용으로만 쓴다.
- Spring Security를 쓰면 actuator 경로의 접근 규칙도 따로 정한다.

## example

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: when-authorized
```

```bash
# 확인
GET /actuator/health   ->  {"status":"UP"}
```
