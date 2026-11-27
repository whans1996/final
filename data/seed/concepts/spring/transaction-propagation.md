---
name: 트랜잭션 전파와 롤백 규칙
category: spring
difficulty: SENIOR
reference: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html
---

## summary

트랜잭션 전파(propagation)는 이미 트랜잭션이 진행 중일 때 새 @Transactional 메서드가 그 트랜잭션에 참여할지, 새로 시작할지를 정한다.

## body

## 핵심
- REQUIRED(기본): 있으면 참여하고 없으면 새로 만든다. 안쪽에서 롤백이 표시되면 바깥 전체가 롤백된다.
- REQUIRES_NEW: 바깥 트랜잭션을 잠시 멈추고 독립 트랜잭션을 연다. 실패 로그처럼 바깥이 롤백돼도 남겨야 하는 기록에 쓴다.
- `rollbackFor`로 checked 예외도 롤백 대상으로 지정할 수 있다.

## 주의
- 안쪽 REQUIRED 메서드에서 예외를 잡아 삼켜도 이미 rollback-only로 표시되어 바깥 커밋 시 `UnexpectedRollbackException`이 난다.
- REQUIRES_NEW는 커넥션을 하나 더 쓰므로 남용하면 커넥션 풀이 고갈될 수 있다.

## example

```java
@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String reason) {
        auditMapper.insert(action, reason);   // 바깥이 롤백돼도 남는다
    }
}
```

```java
@Transactional(rollbackFor = IOException.class)
public void importFile(Path path) throws IOException { }
```
