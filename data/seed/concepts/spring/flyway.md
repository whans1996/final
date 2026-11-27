---
name: Flyway DB 마이그레이션
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-boot/how-to/data-initialization.html
---

## summary

Flyway는 DB 스키마 변경을 버전이 붙은 SQL 파일로 관리하고, 애플리케이션 시작 시 아직 적용되지 않은 파일만 순서대로 실행한다. 모든 PC와 서버의 DB 구조를 같게 맞춘다.

## body

## 핵심
- `V1__init.sql`, `V2__add_column.sql`: 버전 마이그레이션. 한 번만 실행된다.
- `R__seed.sql`: 반복 마이그레이션. 내용(체크섬)이 바뀔 때마다 다시 실행된다. 시드 데이터·뷰에 쓴다.
- 적용 이력은 `flyway_schema_history` 테이블에 체크섬과 함께 기록된다.
- 스프링 부트는 의존성만 있으면 시작 시 자동으로 마이그레이션한다. Flyway 10 이상에서 MySQL을 쓰려면 `flyway-mysql` 모듈도 추가한다.

## 주의
- **이미 적용된 V 파일을 고치면 체크섬이 달라 애플리케이션이 시작되지 않는다.** 수정할 내용은 새 버전 파일로 추가한다.
- 두 사람이 같은 버전 번호를 동시에 만들면 충돌한다. 새 파일을 만들기 전에 최신 번호를 확인한다.
- 운영 DB에서 `flyway clean`(전체 삭제)을 실행하지 않게 막는다(`spring.flyway.clean-disabled`, Boot 기본값 true).

## example

```text
src/main/resources/db/migration/
├─ V1__init.sql
├─ V2__analysis_soft_delete.sql
├─ V3__add_concept_tags.sql      ← 변경은 새 파일로
└─ R__seed_concepts.sql          ← 바뀌면 다시 실행
```

```sql
-- V3__add_concept_tags.sql
ALTER TABLE concepts ADD COLUMN tags VARCHAR(200) NULL;
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```
