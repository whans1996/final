-- 시드 개념 카드 (DS-05, WBS 2.5). 반복 마이그레이션이라 내용이 바뀌면 다시 적용된다.
--
-- 출처 규칙 (docs/산출물/09-데이터셋.md DS-01 카드 작성 규칙)
--   - 근거는 DS-01(split content) 공식 문서만 쓴다. 평가(eval) 저장소 코드는 쓰지 않는다.
--   - 본문은 팀이 직접 쓴다. 원문 번역·각색 금지. Oracle·Swagger·MDN은 링크만 건다.
--   - 예제 코드는 개념에 맞게 새로 쓴다. reference_url은 대표 출처 1개.
-- 주의
--   - name(UNIQUE) 기준 upsert라 id는 유지된다(찜·리뷰 FK 보존). 대신 이 파일을 고치면
--     관리자(F-13)가 화면에서 수정한 시드 카드 내용이 이 파일 내용으로 덮어써진다.
--   - 카드 이름을 바꾸면 새 카드가 생긴다. 이름은 바꾸지 말고 필요하면 새 카드를 추가한다.
--   - MySQL 기본 모드에서 백슬래시는 이스케이프 문자다. 본문·예제에 백슬래시를 쓰지 않는다.

INSERT INTO concepts (name, category, difficulty, summary, body, example_code, status, source, reference_url) VALUES

-- ───────────── java ─────────────

('인터페이스와 다형성', 'java', 'BEGINNER',
 '인터페이스는 "무엇을 할 수 있는가"만 약속하고, 실제 동작은 구현 클래스가 정한다. 같은 타입으로 여러 구현을 바꿔 끼울 수 있는 성질이 다형성이다.',
'## 핵심
- 인터페이스는 메서드 시그니처(약속)만 정의하고, 클래스가 `implements`로 그 약속을 지킨다.
- 호출하는 쪽은 인터페이스 타입만 알면 되므로 구현을 바꿔도 코드를 고치지 않는다.

## AI 코드에서 보이는 모습
`UserService` 인터페이스와 `UserServiceImpl` 구현을 나누는 패턴이 흔하다. 구현이 하나뿐이면 굳이 나누지 않아도 되지만, 테스트용 가짜 구현이나 교체 가능성이 있으면 의미가 생긴다.

## 주의
인터페이스를 만들었다고 다형성이 생기는 것은 아니다. 호출하는 쪽이 구현 클래스 타입을 직접 쓰면 이점이 사라진다.',
'public interface Notifier {
    void send(String to, String message);
}

public class EmailNotifier implements Notifier {
    public void send(String to, String message) {
        System.out.println("메일 -> " + to + ": " + message);
    }
}

// 호출하는 쪽은 Notifier만 안다
Notifier notifier = new EmailNotifier();
notifier.send("dev@example.com", "가입을 환영합니다");',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/java/IandI/createinterface.html'),

('예외 처리와 커스텀 예외', 'java', 'BEGINNER',
 '예외는 정상 흐름을 벗어난 상황을 호출한 쪽에 알리는 방법이다. try-catch로 처리하거나 위로 던지고, 의미가 분명한 커스텀 예외로 원인을 드러낸다.',
'## 핵심
- checked 예외(`Exception` 계열)는 컴파일러가 처리를 강제하고, unchecked 예외(`RuntimeException` 계열)는 강제하지 않는다.
- 스프링 애플리케이션은 보통 `RuntimeException`을 상속한 비즈니스 예외를 만들고, 전역 핸들러가 응답으로 바꾼다.

## 주의
- `catch (Exception e) {}`처럼 예외를 삼키면 문제가 조용히 사라진다. 최소한 로그를 남기거나 다시 던진다.
- 다시 던질 때는 원래 예외를 cause로 넘겨야 스택 트레이스가 이어진다.',
'public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

public User findUser(long id) {
    User user = userMapper.findById(id);
    if (user == null) {
        throw new NotFoundException("회원이 없습니다: " + id);
    }
    return user;
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/essential/exceptions/'),

('람다와 함수형 인터페이스', 'java', 'JUNIOR',
 '람다는 메서드 하나짜리 인터페이스(함수형 인터페이스)의 구현을 짧게 쓰는 문법이다. 동작 자체를 값처럼 넘길 수 있게 해 준다.',
'## 핵심
- 추상 메서드가 하나인 인터페이스면 람다로 구현할 수 있다. `Function`, `Predicate`, `Supplier`, `Consumer`가 대표적이다.
- `(파라미터) -> 식` 형태이며, 메서드 참조(`String::length`)로 더 줄일 수 있다.

## AI 코드에서 보이는 모습
스트림, `Optional`, 정렬 기준(`Comparator`), 콜백에서 자주 나온다.

## 주의
람다 안에서 바깥 지역 변수는 읽기만 할 수 있다(사실상 final). 람다가 길어지면 이름 있는 메서드로 빼는 편이 읽기 쉽다.',
'List<String> names = List.of("kim", "lee", "park");

// 길이가 3보다 큰 이름만, 대문자로
Predicate<String> longName = name -> name.length() > 3;
Function<String, String> upper = String::toUpperCase;

List<String> result = names.stream()
        .filter(longName)
        .map(upper)
        .toList();   // [PARK]',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html'),

('스트림 API', 'java', 'JUNIOR',
 '스트림은 컬렉션 데이터를 filter·map·collect 같은 연산의 연결로 처리하는 방법이다. 반복문 대신 "무엇을 할지"를 선언적으로 쓴다.',
'## 핵심
- 생성(`list.stream()`) → 중간 연산(`filter`, `map`, `sorted`) → 최종 연산(`toList`, `collect`, `count`) 순서로 쓴다.
- 중간 연산은 최종 연산이 호출될 때 실행된다(지연 실행).
- 스트림은 한 번만 소비할 수 있다.

## 주의
- 스트림 안에서 외부 리스트에 `add`하는 식의 부수 효과는 피한다. 결과는 `collect`로 만든다.
- 단순한 반복은 for문이 더 읽기 쉬울 때도 있다.',
'Map<String, Long> countByCategory = concepts.stream()
        .filter(c -> c.status() == Status.PUBLISHED)
        .collect(Collectors.groupingBy(Concept::category, Collectors.counting()));',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/package-summary.html'),

('Optional', 'java', 'JUNIOR',
 'Optional은 값이 있을 수도 없을 수도 있음을 타입으로 드러내는 컨테이너다. null 확인을 잊어 생기는 NullPointerException을 줄인다.',
'## 핵심
- 반환 타입으로 쓰는 것이 원래 목적이다. "없을 수 있는 조회 결과"에 어울린다.
- `orElse`, `orElseThrow`, `map`, `ifPresent`로 값을 꺼내거나 변환한다.

## 주의
- `get()`을 바로 부르면 null 확인을 안 한 것과 같다. `orElseThrow()`를 쓴다.
- 필드나 메서드 파라미터 타입으로는 권장되지 않는다.
- `orElse(무거운호출())`은 값이 있어도 호출된다. 지연이 필요하면 `orElseGet`을 쓴다.',
'public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(userMapper.findByEmail(email));
}

User user = findByEmail(email)
        .orElseThrow(() -> new NotFoundException("가입되지 않은 이메일"));',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Optional.html'),

('record', 'java', 'JUNIOR',
 'record는 값을 담기만 하는 불변 클래스를 한 줄로 선언하는 문법(Java 16+)이다. 생성자, getter, equals·hashCode·toString을 자동으로 만든다.',
'## 핵심
- `record Point(int x, int y) {}`만으로 필드, 생성자, 접근자 `x()`, `y()`가 생긴다. 접근자 이름에 `get`이 붙지 않는다.
- 필드는 final이라 만든 뒤 바꿀 수 없다.
- 요청·응답 DTO처럼 데이터를 나르는 객체에 잘 맞는다.

## 주의
- setter가 없으므로 값을 바꾸려면 새 객체를 만든다.
- 생성자에서 검증하려면 compact 생성자(`record X(...) { 검증 }`)를 쓴다.',
'public record SignupRequest(String email, String nickname) {
    public SignupRequest {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다");
        }
    }
}

SignupRequest req = new SignupRequest("a@b.com", "vibecoder");
req.nickname();   // "vibecoder"',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/language/records.html'),

-- ───────────── spring ─────────────

('IoC 컨테이너와 빈', 'spring', 'BEGINNER',
 '스프링은 객체를 직접 new 하지 않고 컨테이너가 만들어 관리한다. 컨테이너가 관리하는 객체를 빈(Bean)이라 부르고, 객체 생성의 제어권이 개발자에서 프레임워크로 넘어간 것을 제어의 역전(IoC)이라 한다.',
'## 핵심
- `ApplicationContext`가 빈을 만들고, 의존 관계를 연결하고, 생명주기를 관리한다.
- 기본적으로 빈은 애플리케이션에 하나만 만들어지는 싱글톤이다.

## AI 코드에서 보이는 모습
`@Service`, `@Repository`, `@Controller`, `@Configuration`의 `@Bean` 메서드가 모두 빈을 등록하는 방법이다.

## 주의
싱글톤 빈은 여러 요청이 동시에 쓴다. 빈 필드에 요청별 상태(현재 사용자 등)를 저장하면 요청끼리 섞인다.',
'@Service
public class GreetingService {
    public String greet(String name) {
        return "안녕하세요, " + name;
    }
}

// 직접 new 하지 않고 컨테이너에서 받는다
@RestController
public class GreetingController {
    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/introduction.html'),

('의존성 주입 (DI)', 'spring', 'BEGINNER',
 '의존성 주입은 객체가 필요한 다른 객체를 스스로 만들지 않고 외부(스프링 컨테이너)로부터 받는 방식이다. 생성자 주입이 권장된다.',
'## 핵심
- 생성자 주입: 필드를 `final`로 둘 수 있고, 필요한 의존성이 빠지면 객체가 만들어지지 않는다.
- 생성자가 하나면 `@Autowired`를 생략해도 된다.
- 테스트에서 가짜 객체를 생성자로 넣기 쉽다.

## AI 코드에서 보이는 모습
Lombok `@RequiredArgsConstructor`와 `private final` 필드 조합이 자주 보인다. 이것도 생성자 주입이다.

## 주의
필드에 `@Autowired`를 붙이는 필드 주입은 테스트와 불변성에 불리하다.',
'@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;

    public OrderService(OrderMapper orderMapper, PaymentClient paymentClient) {
        this.orderMapper = orderMapper;
        this.paymentClient = paymentClient;
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html'),

('컴포넌트 스캔과 스테레오타입', 'spring', 'BEGINNER',
 '컴포넌트 스캔은 지정한 패키지 아래에서 @Component 계열 어노테이션이 붙은 클래스를 찾아 자동으로 빈으로 등록하는 기능이다.',
'## 핵심
- `@Component`와 그 특수화인 `@Service`, `@Repository`, `@Controller`(`@RestController`)를 스테레오타입 어노테이션이라 한다.
- 스프링 부트는 `@SpringBootApplication`이 붙은 클래스의 패키지와 하위 패키지를 스캔한다.
- 역할별 어노테이션은 의도를 드러내고, `@Repository`는 DB 예외 변환 같은 추가 기능도 받는다.

## 주의
메인 클래스보다 바깥 패키지에 둔 클래스는 스캔되지 않아 "빈을 찾을 수 없음" 오류가 난다.',
'package com.example.app;

@SpringBootApplication   // com.example.app 이하를 스캔
public class App { }

package com.example.app.user;

@Service                 // 자동으로 빈 등록
public class UserService { }',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html'),

('@Configuration과 @Bean', 'spring', 'JUNIOR',
 '@Configuration 클래스 안의 @Bean 메서드는 반환한 객체를 빈으로 등록한다. 직접 만든 클래스가 아니라 라이브러리 객체를 빈으로 쓰거나 생성 과정을 코드로 제어할 때 쓴다.',
'## 핵심
- 컴포넌트 스캔은 내가 만든 클래스에, `@Bean`은 외부 라이브러리 객체(예: `PasswordEncoder`, `RestClient`)에 주로 쓴다.
- 메서드 이름이 기본 빈 이름이 된다.
- `@Configuration` 클래스에서 다른 `@Bean` 메서드를 호출해도 같은 싱글톤 빈이 반환된다.

## 주의
같은 타입 빈이 둘 이상이면 주입할 때 모호해진다. `@Primary`나 `@Qualifier`로 고른다.',
'@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/java.html'),

('빈 스코프', 'spring', 'JUNIOR',
 '빈 스코프는 빈 인스턴스가 얼마나 오래, 몇 개 존재하는지를 정한다. 기본은 싱글톤이고, 필요하면 prototype·request·session 스코프를 쓴다.',
'## 핵심
- singleton: 컨테이너당 하나. 대부분의 서비스·매퍼가 여기에 해당한다.
- prototype: 요청할 때마다 새로 만든다. 컨테이너는 만든 뒤 관리하지 않는다.
- request·session: 웹 요청·세션마다 하나.

## 주의
- 싱글톤 빈이 prototype 빈을 생성자로 주입받으면 처음 한 번만 만들어진다. 매번 새 객체가 필요하면 `ObjectProvider`로 꺼낸다.
- 싱글톤 빈에 변경 가능한 필드를 두면 동시성 문제가 생긴다.',
'@Component
@Scope("prototype")
public class ReportBuilder { }

@Service
public class ReportService {
    private final ObjectProvider<ReportBuilder> builders;

    public ReportService(ObjectProvider<ReportBuilder> builders) {
        this.builders = builders;
    }

    public Report make() {
        ReportBuilder builder = builders.getObject(); // 호출마다 새 인스턴스
        return builder.build();
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html'),

('프로파일 (@Profile)', 'spring', 'JUNIOR',
 '프로파일은 실행 환경(local, test, prod 등)에 따라 다른 빈과 설정을 활성화하는 기능이다.',
'## 핵심
- `@Profile("local")`이 붙은 빈은 그 프로파일이 활성일 때만 등록된다.
- 스프링 부트는 `application-{프로파일}.yml`을 함께 읽는다.
- `spring.profiles.active`(실행 인자, 환경변수)로 켠다.

## AI 코드에서 보이는 모습
개발용 Mock 구현과 운영용 실제 구현을 프로파일로 나누는 패턴이 많다.

## 주의
운영 설정 파일에 비밀번호나 API 키를 직접 쓰지 말고 환경변수로 받는다.',
'@Configuration
public class MailConfig {

    @Bean
    @Profile("local")
    public MailSender fakeMailSender() {
        return new LoggingMailSender();
    }

    @Bean
    @Profile("prod")
    public MailSender smtpMailSender() {
        return new SmtpMailSender();
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/environment.html'),

('AOP (관점 지향 프로그래밍)', 'spring', 'SENIOR',
 'AOP는 로깅·트랜잭션·권한 확인처럼 여러 곳에 반복되는 부가 기능을 핵심 로직과 분리해 한곳에서 적용하는 기법이다. 스프링은 프록시 객체로 이를 구현한다.',
'## 핵심
- Aspect(부가 기능 모듈), Advice(언제 무엇을 할지: `@Before`, `@Around` 등), Pointcut(어디에 적용할지)으로 구성된다.
- 스프링 AOP는 빈을 프록시로 감싸 메서드 호출 앞뒤에 끼어든다. `@Transactional`도 이 방식이다.

## 주의
- 같은 클래스 안에서 자기 메서드를 호출(self-invocation)하면 프록시를 거치지 않아 AOP가 적용되지 않는다.
- private 메서드에는 적용되지 않는다.
- 너무 넓은 Pointcut은 어디서 무엇이 실행되는지 추적하기 어렵게 만든다.',
'@Aspect
@Component
public class TimingAspect {

    @Around("execution(* com.example..service.*.*(..))")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long ms = System.currentTimeMillis() - start;
            System.out.println(pjp.getSignature() + " " + ms + "ms");
        }
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/aop.html'),

('@Transactional 선언적 트랜잭션', 'spring', 'JUNIOR',
 '@Transactional을 메서드나 클래스에 붙이면 스프링이 메서드 시작 시 트랜잭션을 열고, 정상 종료 시 커밋, 예외 시 롤백한다.',
'## 핵심
- 여러 SQL이 "모두 성공하거나 모두 실패"해야 할 때 쓴다. 예: 주문 저장 + 재고 차감.
- 보통 Service 계층 메서드에 붙인다.
- 조회만 하는 메서드는 `@Transactional(readOnly = true)`로 의도를 드러낸다.

## 주의
- 기본 설정은 unchecked 예외(`RuntimeException`)에서만 롤백한다. checked 예외는 커밋된다.
- 프록시 방식이라 같은 클래스 내부 호출에는 적용되지 않는다.
- 트랜잭션 안에서 외부 API처럼 오래 걸리는 호출을 하면 그동안 DB 커넥션을 잡고 있게 된다.',
'@Service
public class OrderService {

    @Transactional
    public void placeOrder(OrderRequest req) {
        orderMapper.insert(req);
        stockMapper.decrease(req.productId(), req.quantity());
        // 여기서 예외가 나면 insert도 롤백된다
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html'),

('트랜잭션 전파와 롤백 규칙', 'spring', 'SENIOR',
 '트랜잭션 전파(propagation)는 이미 트랜잭션이 진행 중일 때 새 @Transactional 메서드가 그 트랜잭션에 참여할지, 새로 시작할지를 정한다.',
'## 핵심
- REQUIRED(기본): 있으면 참여하고 없으면 새로 만든다. 안쪽에서 롤백이 표시되면 바깥 전체가 롤백된다.
- REQUIRES_NEW: 바깥 트랜잭션을 잠시 멈추고 독립 트랜잭션을 연다. 실패 로그처럼 바깥이 롤백돼도 남겨야 하는 기록에 쓴다.
- `rollbackFor`로 checked 예외도 롤백 대상으로 지정할 수 있다.

## 주의
- 안쪽 REQUIRED 메서드에서 예외를 잡아 삼켜도 이미 rollback-only로 표시되어 바깥 커밋 시 `UnexpectedRollbackException`이 난다.
- REQUIRES_NEW는 커넥션을 하나 더 쓰므로 남용하면 커넥션 풀이 고갈될 수 있다.',
'@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String reason) {
        auditMapper.insert(action, reason);   // 바깥이 롤백돼도 남는다
    }
}

@Transactional(rollbackFor = IOException.class)
public void importFile(Path path) throws IOException { }',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html'),

('스프링 부트 자동 구성', 'spring', 'JUNIOR',
 '자동 구성(Auto-configuration)은 클래스패스에 있는 라이브러리와 설정값을 보고 필요한 빈을 스프링 부트가 알아서 등록해 주는 기능이다.',
'## 핵심
- 예: MySQL 드라이버와 `spring.datasource.*` 설정이 있으면 `DataSource` 빈이 자동으로 생긴다.
- 자동 구성은 "내가 같은 빈을 직접 정의하지 않았을 때만" 동작한다(조건부 등록). 직접 정의하면 그쪽이 우선한다.
- starter 의존성(`spring-boot-starter-web` 등)이 필요한 라이브러리와 자동 구성을 함께 가져온다.

## 주의
무엇이 자동으로 등록됐는지 모르면 디버깅이 어렵다. `--debug`로 실행하면 조건 평가 보고서를 볼 수 있다.',
'# application.yml 만으로 DataSource와 MyBatis SqlSessionFactory가 자동 구성된다
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/app
    username: app
    password: change-me
mybatis:
  mapper-locations: classpath:mapper/**/*.xml',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/using/auto-configuration.html'),

('외부 설정과 @ConfigurationProperties', 'spring', 'JUNIOR',
 '스프링 부트는 application.yml, 환경변수, 실행 인자에서 설정값을 읽는다. @ConfigurationProperties는 관련 설정을 타입이 있는 객체 하나로 묶어 준다.',
'## 핵심
- 우선순위가 있다. 실행 인자와 환경변수가 설정 파일보다 우선하므로, 운영 값은 환경변수로 덮어쓴다.
- `@Value("${...}")`는 값 하나, `@ConfigurationProperties(prefix = "...")`는 묶음에 적합하다.
- record로 선언하면 불변 설정 객체가 된다.

## 주의
비밀 값(API 키, DB 비밀번호)은 설정 파일에 쓰지 말고 환경변수로 받는다. 저장소에 커밋되면 되돌리기 어렵다.',
'@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(String dir, long maxBytes) { }

@SpringBootApplication
@ConfigurationPropertiesScan
public class App { }

# application.yml
app:
  upload:
    dir: ./uploads
    max-bytes: 2097152',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/features/external-config.html'),

('계층형 구조 (Controller·Service·Mapper)', 'spring', 'BEGINNER',
 '웹 애플리케이션을 요청 처리(Controller), 비즈니스 로직(Service), 데이터 접근(Mapper·Repository) 계층으로 나누는 구조다. 각 계층은 아래 계층만 호출한다.',
'## 핵심
- Controller: 요청을 받고 검증한 뒤 Service를 호출하고 응답을 돌려준다. 로직을 두지 않는다.
- Service: 규칙과 트랜잭션을 담당한다.
- Mapper: SQL을 실행한다.

## AI 코드에서 보이는 모습
AI가 만든 코드는 Controller에서 바로 Mapper를 부르거나, Service에 HTTP 관련 코드가 섞이는 경우가 있다. 계층을 건너뛰면 트랜잭션과 테스트 경계가 흐려진다.

## 주의
DB 행 객체를 그대로 응답으로 내보내면 비밀번호 같은 필드가 노출될 수 있다. 응답 DTO로 바꿔서 보낸다.',
'@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable long id) {
        return userService.getUser(id);          // Controller -> Service
    }
}

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getUser(long id) {
        User user = userMapper.findById(id);     // Service -> Mapper
        return UserResponse.from(user);
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html#beans-stereotype-annotations'),

-- ───────────── web ─────────────

('@RestController와 요청 매핑', 'web', 'BEGINNER',
 '@RestController는 메서드 반환값을 JSON 같은 응답 본문으로 바로 쓰는 컨트롤러다. @GetMapping·@PostMapping 등으로 URL과 HTTP 메서드를 메서드에 연결한다.',
'## 핵심
- `@RestController` = `@Controller` + `@ResponseBody`.
- 클래스의 `@RequestMapping("/api/posts")`가 공통 경로, 메서드의 `@GetMapping("/{id}")`가 세부 경로다.
- 반환한 객체는 Jackson이 JSON으로 바꾼다.

## 주의
같은 URL과 메서드 조합이 두 군데 매핑되면 애플리케이션이 시작되지 않는다.',
'@RestController
@RequestMapping("/api/posts")
public class PostController {

    @GetMapping
    public List<PostResponse> list() { return postService.findAll(); }

    @PostMapping
    public PostResponse create(@RequestBody PostRequest req) { return postService.create(req); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) { postService.delete(id); }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html'),

('요청 파라미터 바인딩 (@PathVariable·@RequestParam·@RequestBody)', 'web', 'BEGINNER',
 '컨트롤러 메서드 파라미터에 어노테이션을 붙여 URL 경로, 쿼리 문자열, 요청 본문(JSON)의 값을 자바 객체로 받는다.',
'## 핵심
- `@PathVariable`: `/users/{id}`의 `id`처럼 경로의 일부.
- `@RequestParam`: `?page=2&keyword=spring` 같은 쿼리 문자열. `required = false`, `defaultValue`로 선택값을 지정한다.
- `@RequestBody`: 요청 본문 JSON을 객체(주로 record DTO)로 변환한다.

## 주의
- 식별자는 경로, 검색·필터·정렬 조건은 쿼리, 생성·수정 데이터는 본문에 두는 것이 일반적이다.
- 본문 JSON 형식이 틀리면 400 응답이 난다. 전역 예외 처리에서 일관된 에러 형식으로 바꿔 준다.',
'@GetMapping("/api/concepts")
public PageResponse<ConceptResponse> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int page) { ... }

@PatchMapping("/api/posts/{id}")
public PostResponse update(@PathVariable long id, @RequestBody PostUpdateRequest req) { ... }',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/arguments.html'),

('ResponseEntity와 HTTP 상태 코드', 'web', 'BEGINNER',
 'ResponseEntity는 응답 본문과 함께 HTTP 상태 코드와 헤더를 직접 정할 수 있는 반환 타입이다. 상태 코드는 요청 결과를 클라이언트에게 알리는 약속이다.',
'## 핵심
- 2xx 성공(200 OK, 201 Created, 202 Accepted, 204 No Content), 4xx 클라이언트 오류(400, 401, 403, 404, 409), 5xx 서버 오류.
- 생성은 201과 `Location` 헤더, 비동기 접수는 202, 본문 없는 삭제는 204가 자주 쓰인다.
- 객체만 반환하면 기본은 200이다.

## 주의
에러인데 200과 `{"success": false}`를 보내면 클라이언트와 모니터링이 실패를 알아채기 어렵다. 상태 코드로 표현한다.',
'@PostMapping("/api/posts")
public ResponseEntity<PostResponse> create(@RequestBody PostRequest req) {
    PostResponse created = postService.create(req);
    return ResponseEntity
            .created(URI.create("/api/posts/" + created.id()))
            .body(created);
}

@DeleteMapping("/api/posts/{id}")
public ResponseEntity<Void> delete(@PathVariable long id) {
    postService.delete(id);
    return ResponseEntity.noContent().build();
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html'),

('Bean Validation (@Valid)', 'web', 'JUNIOR',
 'Bean Validation은 DTO 필드에 @NotBlank·@Size·@Email 같은 제약 어노테이션을 달고, 컨트롤러 파라미터에 @Valid를 붙여 요청을 자동 검증하는 표준이다.',
'## 핵심
- 검증에 실패하면 컨트롤러 메서드가 실행되지 않고 `MethodArgumentNotValidException`이 난다.
- 전역 예외 처리에서 필드별 오류 메시지를 모아 400 응답으로 바꾼다.
- `spring-boot-starter-validation` 의존성이 필요하다.

## 주의
- `@Valid`를 빼먹으면 어노테이션이 있어도 검증되지 않는다. AI 코드에서 자주 빠진다.
- 입력 형식 검증은 DTO에서, "이미 가입된 이메일" 같은 비즈니스 규칙 검증은 Service에서 한다.',
'public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 20) String nickname) { }

@PostMapping("/api/auth/signup")
public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest req) { ... }',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html'),

('전역 예외 처리 (@RestControllerAdvice)', 'web', 'JUNIOR',
 '@RestControllerAdvice 클래스의 @ExceptionHandler 메서드는 모든 컨트롤러에서 던진 예외를 한곳에서 잡아 일관된 에러 응답으로 바꾼다.',
'## 핵심
- 컨트롤러마다 try-catch를 쓰지 않아도 된다.
- 비즈니스 예외는 에러 코드·HTTP 상태·메시지로, 검증 예외는 필드 오류 목록으로, 예상 못한 예외는 500으로 변환한다.
- 더 구체적인 예외 타입의 핸들러가 우선한다.

## 주의
500 응답에 예외 메시지나 스택 트레이스를 그대로 담으면 내부 정보가 노출된다. 로그에만 남기고 응답은 일반 메시지로 보낸다.',
'@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), code.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("SERVER_500", "서버 오류가 발생했습니다"));
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html'),

('필터와 인터셉터', 'web', 'SENIOR',
 '필터는 서블릿 컨테이너 단계에서, 인터셉터는 스프링 MVC 단계에서 요청 앞뒤에 공통 처리를 끼워 넣는다. 둘 다 여러 요청에 공통으로 필요한 일을 한곳에 모은다.',
'## 핵심
- 요청 흐름: 필터 → DispatcherServlet → 인터셉터 → 컨트롤러.
- 필터: 인코딩, 보안(스프링 시큐리티 필터 체인), 요청 로깅처럼 스프링 MVC 바깥에서도 필요한 일.
- 인터셉터(`HandlerInterceptor`): 어떤 컨트롤러가 호출될지 알 수 있어 핸들러별 처리에 적합하다.

## 주의
- 필터에서 던진 예외는 `@RestControllerAdvice`가 잡지 못한다. 필터 안에서 직접 응답을 써야 한다.
- 인터셉터 `preHandle`이 false를 반환하면 컨트롤러가 실행되지 않는다.',
'public class RequestTimeInterceptor implements HandlerInterceptor {

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

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestTimeInterceptor()).addPathPatterns("/api/**");
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html'),

('CORS', 'web', 'JUNIOR',
 'CORS는 브라우저가 다른 출처(도메인·포트)로 보내는 요청을 서버가 허용하는지 확인하는 규칙이다. 프론트(5173)와 백엔드(8080)를 따로 띄우면 바로 만난다.',
'## 핵심
- 출처는 프로토콜 + 호스트 + 포트다. 포트만 달라도 다른 출처다.
- 브라우저는 응답의 `Access-Control-Allow-Origin` 등 헤더를 보고 허용 여부를 판단한다. 서버가 아니라 브라우저가 막는 것이다.
- JSON 요청이나 Authorization 헤더가 있으면 먼저 OPTIONS 사전 요청(preflight)을 보낸다.

## 주의
- 스프링 시큐리티를 쓰면 시큐리티 설정에서도 CORS를 켜야 preflight가 인증에 막히지 않는다.
- `allowedOrigins("*")`와 자격 증명 허용을 같이 쓸 수 없다. 허용할 출처를 명시한다.

더 읽기: MDN 교차 출처 리소스 공유 https://developer.mozilla.org/ko/docs/Web/HTTP/CORS',
'@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .allowCredentials(true);
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html'),

('REST API와 HTTP 메서드', 'web', 'BEGINNER',
 'REST API는 자원을 URL로 표현하고, 그 자원에 대한 행동을 HTTP 메서드(GET·POST·PUT·PATCH·DELETE)로 나타내는 설계 방식이다.',
'## 핵심
- URL은 명사(자원), 행동은 메서드로 표현한다. `POST /api/posts`(생성), `GET /api/posts/3`(조회), `DELETE /api/posts/3`(삭제).
- GET은 데이터를 바꾸지 않아야 한다.
- PUT은 전체 교체, PATCH는 일부 수정에 쓴다.
- 같은 요청을 여러 번 보내도 결과가 같은 성질(멱등성): GET·PUT·DELETE는 멱등, POST는 아니다.

## 주의
`GET /api/deletePost?id=3`처럼 동사를 URL에 넣는 설계는 캐시·크롤러가 데이터를 지우는 사고로 이어질 수 있다.

참고: MDN HTTP 요청 메서드 (링크만)',
'GET    /api/posts?page=1        목록
GET    /api/posts/42            상세
POST   /api/posts               생성 (201 + Location)
PATCH  /api/posts/42            일부 수정
DELETE /api/posts/42            삭제 (204)
POST   /api/posts/42/likes      하위 자원 생성 (좋아요)',
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Web/HTTP/Methods'),

('JSON 직렬화 (Jackson)', 'web', 'JUNIOR',
 '스프링 부트는 Jackson 라이브러리로 자바 객체와 JSON을 서로 변환한다. 요청 본문은 객체로(역직렬화), 반환값은 JSON으로(직렬화) 바뀐다.',
'## 핵심
- 기본적으로 getter(record는 접근자) 이름이 JSON 필드 이름이 된다.
- 날짜(`LocalDateTime`)는 ISO-8601 문자열로 나간다.
- `@JsonProperty`, `@JsonIgnore`로 이름과 포함 여부를 조정한다.

## 주의
- 엔티티를 그대로 반환하면 원치 않는 필드(비밀번호 해시 등)까지 직렬화된다. 응답 DTO를 쓴다.
- 서로를 참조하는 객체는 무한 재귀로 직렬화가 실패할 수 있다.',
'public record UserResponse(
        long id,
        String nickname,
        @JsonProperty("joinedAt") LocalDateTime createdAt) { }

// 응답: {"id":1,"nickname":"vibecoder","joinedAt":"2026-11-18T10:00:00"}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/features/json.html'),

('OpenAPI 문서화 (springdoc)', 'web', 'BEGINNER',
 'springdoc-openapi는 스프링 컨트롤러를 분석해 OpenAPI 명세(JSON)와 Swagger UI 화면을 자동으로 만들어 준다. 어노테이션으로 설명과 예시를 보강한다.',
'## 핵심
- 의존성만 추가하면 `/v3/api-docs`(명세)와 `/swagger-ui/index.html`(화면)이 생긴다.
- `@Tag`(그룹), `@Operation`(요약), `@ApiResponse`(응답 코드별 설명), `@Schema`(필드 설명·예시)를 단다.
- 프론트엔드와 API 계약을 맞추는 기준 문서로 쓸 수 있다.

## 주의
운영 환경에서 Swagger UI를 공개할지 결정해야 한다. 내부 API 구조가 그대로 노출된다.',
'@Tag(name = "게시판", description = "게시글 CRUD")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Operation(summary = "게시글 상세 조회")
    @ApiResponse(responseCode = "404", description = "게시글 없음")
    @GetMapping("/{id}")
    public PostResponse get(@PathVariable long id) { ... }
}

public record PostResponse(
        @Schema(description = "게시글 ID", example = "42") long id,
        @Schema(description = "제목", example = "MyBatis 질문") String title) { }',
 'PUBLISHED', 'SEED', 'https://springdoc.org/'),

('async/await와 Promise', 'web', 'BEGINNER',
 'Promise는 나중에 끝나는 작업(서버 요청 등)의 결과를 나타내는 객체이고, async/await는 그 결과를 동기 코드처럼 순서대로 기다리게 해 주는 문법이다.',
'## 핵심
- `async` 함수는 항상 Promise를 반환한다.
- `await`는 Promise가 끝날 때까지 그 함수의 실행을 멈추고 결과값을 꺼낸다.
- 실패(reject)는 `try/catch`로 잡는다.

## AI 코드에서 보이는 모습
Vue 컴포넌트와 Pinia 액션에서 API를 호출할 때 거의 항상 쓰인다.

## 주의
- `await`를 빼먹으면 값 대신 Promise 객체를 쓰게 된다.
- 서로 독립된 요청을 하나씩 await하면 느리다. `Promise.all`로 동시에 보낸다.

참고: MDN async function (링크만)',
'async function loadDashboard(userId) {
  try {
    const [profile, stats] = await Promise.all([
      api.get("/api/users/" + userId),
      api.get("/api/users/" + userId + "/stats"),
    ]);
    return { profile: profile.data, stats: stats.data };
  } catch (error) {
    console.error("대시보드 로딩 실패", error);
    throw error;
  }
}',
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Web/JavaScript/Reference/Statements/async_function'),

-- ───────────── security ─────────────

('인증과 인가', 'security', 'BEGINNER',
 '인증(Authentication)은 "누구인가"를 확인하는 것이고, 인가(Authorization)는 "그 사람이 이 일을 해도 되는가"를 판단하는 것이다.',
'## 핵심
- 인증 실패는 401 Unauthorized, 인증은 됐지만 권한이 없으면 403 Forbidden이다.
- 스프링 시큐리티는 인증 결과를 `SecurityContext`에 담고, URL 규칙이나 `@PreAuthorize`로 인가를 판단한다.

## 주의
- "로그인했는가"만 확인하고 "내 글인가"를 확인하지 않으면 남의 글을 수정·삭제할 수 있다. 소유자 확인은 Service에서 한다.
- 관리자 API는 URL 규칙으로 역할을 강제한다.',
'http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()      // 인증 불필요
        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 인가: 관리자만
        .anyRequest().authenticated());                    // 나머지는 인증 필요

// 소유자 확인 (인가)
if (post.userId() != loginUserId) {
    throw new BusinessException(ErrorCode.FORBIDDEN);
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html'),

('SecurityFilterChain', 'security', 'JUNIOR',
 '스프링 시큐리티는 요청이 컨트롤러에 닿기 전에 여러 보안 필터를 순서대로 통과시킨다. SecurityFilterChain 빈으로 어떤 필터를 어떤 규칙으로 쓸지 설정한다.',
'## 핵심
- `HttpSecurity`로 CSRF, 세션 정책, URL별 권한, 커스텀 필터 위치를 정한다.
- JWT 방식 API 서버는 보통 세션을 쓰지 않고(STATELESS), CSRF를 끄고, JWT 검증 필터를 `UsernamePasswordAuthenticationFilter` 앞에 둔다.
- 401·403 응답 형식은 `AuthenticationEntryPoint`, `AccessDeniedHandler`로 바꾼다.

## 주의
- `permitAll` 경로 목록이 넓으면 보호해야 할 API가 열린다. 기본은 `authenticated()`로 두고 공개 경로만 연다.
- 세션 쿠키 기반 인증이라면 CSRF를 끄면 안 된다.',
'@Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
        throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/architecture.html'),

('비밀번호 해싱 (PasswordEncoder)', 'security', 'BEGINNER',
 '비밀번호는 원문이나 되돌릴 수 있는 암호문으로 저장하지 않고, BCrypt 같은 느린 단방향 해시로 저장한다. 로그인 시에는 입력값을 같은 방식으로 비교한다.',
'## 핵심
- `PasswordEncoder.encode()`로 저장하고 `matches(원문, 해시)`로 비교한다. 해시를 다시 원문으로 바꾸지 않는다.
- BCrypt는 매번 다른 salt를 섞으므로 같은 비밀번호도 해시값이 다르다. 그래서 문자열 `equals`로 비교하면 안 된다.
- 일부러 느리게 만들어 무차별 대입 공격 비용을 높인다.

## 주의
- MD5·SHA-256 한 번 적용은 너무 빨라서 비밀번호 저장에 부적합하다.
- 로그·응답·예외 메시지에 비밀번호가 찍히지 않게 한다.',
'@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;

    public void signup(SignupRequest req) {
        String hash = passwordEncoder.encode(req.password());
        userMapper.insert(req.email(), hash, req.nickname());
    }

    public User login(String email, String rawPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return user;
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html'),

('JWT 기반 인증', 'security', 'SENIOR',
 'JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다. 서버는 세션을 저장하지 않고, 요청마다 Authorization 헤더의 토큰 서명과 만료를 검증해 사용자를 식별한다.',
'## 핵심
- 구조: header.payload.signature (Base64URL). payload는 암호화가 아니라 인코딩이라 누구나 읽을 수 있다.
- Access 토큰은 짧게(예: 30분), Refresh 토큰은 길게 두고 Refresh로 Access를 재발급한다.
- 서버는 서명 키로 위조 여부를, `exp`로 만료를 확인한다.

## 주의
- payload에 비밀번호나 개인정보를 넣지 않는다.
- 서명 키는 충분히 길고 환경변수로 관리한다.
- 발급한 JWT는 만료 전에 서버가 취소하기 어렵다. 로그아웃·탈취 대응은 Refresh 토큰 저장·폐기로 보완한다.
- 서명 알고리즘을 고정하고 `alg: none` 토큰을 받지 않는다.',
'// 요청
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3OTk5OTk5OTl9.서명

// 필터에서 검증
String token = resolveBearer(request);
if (token != null && jwtTokenProvider.isValid(token)) {
    long userId = jwtTokenProvider.getUserId(token);
    Authentication auth = new UsernamePasswordAuthenticationToken(
            new AuthUser(userId), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html'),

-- ───────────── mybatis ─────────────

('Mapper 인터페이스와 XML 매퍼', 'mybatis', 'BEGINNER',
 'MyBatis는 자바 인터페이스(Mapper)의 메서드와 XML에 적은 SQL을 연결한다. 메서드를 호출하면 연결된 SQL이 실행되고 결과가 객체로 매핑된다.',
'## 핵심
- XML의 `namespace`는 Mapper 인터페이스의 전체 이름, 각 SQL의 `id`는 메서드 이름과 같아야 한다.
- 파라미터는 `#{이름}`으로 받는다. 파라미터가 여러 개면 `@Param`으로 이름을 붙인다.
- `resultType`은 결과를 담을 클래스다. `map-underscore-to-camel-case`를 켜면 `created_at` 컬럼이 `createdAt` 필드로 들어간다.

## 주의
namespace나 id 오타는 실행 시점에 `Invalid bound statement` 오류로 드러난다.',
'@Mapper
public interface PostMapper {
    Post findById(long id);
    List<Post> findByUser(@Param("userId") long userId, @Param("size") int size);
}

<!-- resources/mapper/post/PostMapper.xml -->
<mapper namespace="com.example.post.PostMapper">
  <select id="findById" resultType="com.example.post.domain.Post">
    SELECT id, user_id, title, created_at FROM posts WHERE id = #{id}
  </select>
</mapper>',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/getting-started.html'),

('#{}와 ${}의 차이 (SQL 인젝션)', 'mybatis', 'JUNIOR',
 'MyBatis에서 #{}는 PreparedStatement의 파라미터(?)로 안전하게 바인딩되고, ${}는 값을 SQL 문자열에 그대로 끼워 넣는다. 사용자 입력을 ${}로 넣으면 SQL 인젝션이 생긴다.',
'## 핵심
- `#{keyword}` → `WHERE title LIKE ?` 로 바뀌고 값은 따로 전달된다. 입력에 따옴표가 있어도 SQL 구조가 바뀌지 않는다.
- `${column}` → 값이 SQL 텍스트 자체가 된다.
- `${}`가 필요한 경우는 정렬 컬럼명처럼 파라미터로 바인딩할 수 없는 자리뿐이다.

## AI 코드에서 보이는 모습
정렬·검색 기능을 만들 때 AI가 `ORDER BY ${sort}`나 `LIKE ''%${keyword}%''`를 쓰는 경우가 있다. 리뷰에서 꼭 확인할 지점이다.

## 주의
`${}`를 써야 한다면 enum이나 화이트리스트로 검증된 값만 넘긴다. LIKE 검색은 `CONCAT(''%'', #{keyword}, ''%'')`로 쓴다.',
'<!-- 위험: keyword에 따옴표와 OR 1=1을 넣으면 전체 조회 -->
<select id="searchUnsafe" resultType="Post">
  SELECT * FROM posts WHERE title LIKE ''%${keyword}%''
</select>

<!-- 안전 -->
<select id="search" resultType="Post">
  SELECT * FROM posts
  WHERE title LIKE CONCAT(''%'', #{keyword}, ''%'')
  ORDER BY
  <choose>
    <when test="sort == ''POPULAR''">like_count DESC</when>
    <otherwise>created_at DESC</otherwise>
  </choose>
</select>',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/sqlmap-xml.html'),

('동적 SQL', 'mybatis', 'JUNIOR',
 '동적 SQL은 조건에 따라 SQL 일부를 넣거나 빼는 MyBatis 기능이다. <if>, <where>, <choose>, <foreach>, <set> 태그를 쓴다.',
'## 핵심
- `<where>`는 안쪽 조건이 하나라도 있으면 WHERE를 붙이고, 맨 앞의 AND·OR를 정리한다.
- `<foreach>`는 `IN (...)` 목록이나 여러 행 INSERT에 쓴다.
- `<set>`은 수정할 필드만 골라 UPDATE할 때 끝의 쉼표를 정리한다.

## 주의
- `<foreach>`에 빈 목록이 들어오면 `IN ()`이 되어 SQL 오류가 난다. 호출 전에 빈 목록을 처리한다.
- 조건이 모두 빠지면 전체 조회·전체 수정이 될 수 있다. 특히 UPDATE·DELETE에서 조심한다.',
'<select id="search" resultType="Concept">
  SELECT id, name, category, difficulty FROM concepts
  <where>
    status = ''PUBLISHED''
    <if test="keyword != null and keyword != ''''">
      AND name LIKE CONCAT(''%'', #{keyword}, ''%'')
    </if>
    <if test="category != null">AND category = #{category}</if>
    <if test="ids != null and ids.size() > 0">
      AND id IN
      <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </if>
  </where>
</select>',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/dynamic-sql.html'),

('resultMap과 연관 매핑', 'mybatis', 'JUNIOR',
 'resultMap은 조회 결과 컬럼을 객체 필드에 어떻게 넣을지 직접 정의하는 MyBatis 설정이다. 조인 결과를 객체 안의 객체(association)나 목록(collection)으로 묶을 때 쓴다.',
'## 핵심
- `<id>`는 행을 구분하는 키다. collection 매핑에서 같은 부모 행을 하나로 묶는 기준이 된다.
- `<association>`: 1:1 (게시글 → 작성자), `<collection>`: 1:N (게시글 → 댓글 목록).
- 단순 이름 차이는 `map-underscore-to-camel-case`나 SQL 별칭으로 해결하고, 중첩 구조일 때 resultMap을 쓴다.

## 주의
- 조인한 테이블 컬럼 이름이 겹치면(`id`) 별칭으로 구분해야 값이 섞이지 않는다.
- `<id>`를 빠뜨리면 collection이 부모 행마다 따로 만들어질 수 있다.',
'<resultMap id="postWithComments" type="PostDetail">
  <id property="id" column="post_id"/>
  <result property="title" column="title"/>
  <association property="author" javaType="Author">
    <id property="id" column="author_id"/>
    <result property="nickname" column="author_nickname"/>
  </association>
  <collection property="comments" ofType="Comment">
    <id property="id" column="comment_id"/>
    <result property="content" column="comment_content"/>
  </collection>
</resultMap>

<select id="findDetail" resultMap="postWithComments">
  SELECT p.id AS post_id, p.title,
         u.id AS author_id, u.nickname AS author_nickname,
         c.id AS comment_id, c.content AS comment_content
  FROM posts p
  JOIN users u ON u.id = p.user_id
  LEFT JOIN comments c ON c.post_id = p.id
  WHERE p.id = #{id}
</select>',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/sqlmap-xml.html#Result_Maps'),

('N+1 쿼리 문제', 'mybatis', 'SENIOR',
 'N+1 문제는 목록 1번 조회 후 각 항목의 연관 데이터를 항목마다 따로 조회해 쿼리가 N+1번 실행되는 성능 문제다. 데이터가 적을 땐 안 보이다가 늘어나면 급격히 느려진다.',
'## 핵심
- MyBatis에서는 `<association select="...">`·`<collection select="...">`(중첩 select)나, 반복문 안에서 Mapper를 호출할 때 생긴다.
- 해결: 조인 한 번 + resultMap 중첩 매핑, 또는 ID 목록을 모아 `IN (...)`으로 한 번에 조회한 뒤 메모리에서 합친다.
- 지연 로딩은 쿼리 시점을 미룰 뿐 횟수를 줄이지 않는다.

## AI 코드에서 보이는 모습
"게시글 목록 + 작성자 닉네임"을 만들 때 for문 안에서 `userMapper.findById()`를 부르는 코드가 흔하다.

## 주의
조인으로 1:N을 가져오면 행이 부모 × 자식 수만큼 늘어나고 페이지네이션(LIMIT)이 자식 행 기준으로 잘린다. 목록 페이지에서는 IN 조회 방식이 안전하다.',
'// 문제: 게시글 20개면 쿼리 21번
List<Post> posts = postMapper.findPage(page);
for (Post post : posts) {
    post.setAuthor(userMapper.findById(post.getUserId()));
}

// 개선: 쿼리 2번
List<Post> posts = postMapper.findPage(page);
List<Long> userIds = posts.stream().map(Post::getUserId).distinct().toList();
Map<Long, User> users = userMapper.findByIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));
posts.forEach(p -> p.setAuthor(users.get(p.getUserId())));',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/sqlmap-xml.html'),

('MyBatis 스프링 부트 설정', 'mybatis', 'BEGINNER',
 'mybatis-spring-boot-starter는 DataSource를 보고 SqlSessionFactory를 자동으로 만들고, @Mapper 인터페이스를 빈으로 등록해 준다. 설정은 application.yml의 mybatis.* 항목으로 한다.',
'## 핵심
- `mybatis.mapper-locations`: XML 매퍼 파일 위치.
- `mybatis.configuration.map-underscore-to-camel-case: true`: `user_id` → `userId` 자동 매핑.
- `mybatis.type-aliases-package`: resultType에 패키지 없이 클래스 이름만 쓸 수 있게 한다.
- `@Mapper`를 붙이거나 `@MapperScan`으로 패키지를 지정해 Mapper를 등록한다.

## 주의
XML 위치 설정이 틀리면 앱은 뜨지만 호출 시점에 `Invalid bound statement (not found)` 오류가 난다.',
'# application.yml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.domain
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100',
 'PUBLISHED', 'SEED', 'https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/'),

-- ───────────── vue ─────────────

('반응성 (ref·reactive)', 'vue', 'BEGINNER',
 'Vue의 반응성은 상태가 바뀌면 그 상태를 쓰는 화면이 자동으로 다시 그려지는 성질이다. Composition API에서는 ref와 reactive로 반응형 상태를 만든다.',
'## 핵심
- `ref(값)`: 어떤 타입이든 감싼다. 스크립트에서는 `.value`로 읽고 쓰고, 템플릿에서는 `.value` 없이 쓴다.
- `reactive(객체)`: 객체 자체를 반응형으로 만든다. 객체·배열에만 쓸 수 있다.
- 공식 문서는 기본 선택으로 `ref`를 권한다.

## 주의
- `reactive` 객체를 구조 분해하거나 통째로 새 객체를 대입하면 반응성이 끊긴다.
- 스크립트에서 `.value`를 빼먹는 실수가 흔하다.',
'<script setup>
import { ref } from "vue";

const count = ref(0);
const todos = ref([]);

function add(text) {
  todos.value.push({ id: Date.now(), text });
  count.value++;
}
</script>

<template>
  <p>할 일 {{ count }}개</p>
  <button @click="add(''새 할 일'')">추가</button>
</template>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/reactivity-fundamentals.html'),

('computed와 watch', 'vue', 'JUNIOR',
 'computed는 다른 상태로부터 계산되는 값을 캐시해 두는 반응형 값이고, watch는 상태가 바뀔 때 API 호출 같은 부수 효과를 실행한다.',
'## 핵심
- computed: 의존하는 값이 바뀔 때만 다시 계산한다. 템플릿의 복잡한 식을 옮기기 좋다. 계산만 하고 상태를 바꾸지 않는다.
- watch: 특정 값을 지켜보다가 바뀌면 콜백을 실행한다. 검색어 변경 → 목록 재조회 같은 경우.
- `watch(..., { immediate: true })`로 처음에도 한 번 실행할 수 있다.

## 주의
- computed 안에서 API를 부르거나 다른 상태를 바꾸지 않는다.
- 계산으로 충분한 것을 watch + 별도 상태로 만들면 동기화 버그가 생긴다.',
'<script setup>
import { ref, computed, watch } from "vue";

const keyword = ref("");
const concepts = ref([]);

const count = computed(() => concepts.value.length);

watch(keyword, async (value) => {
  concepts.value = await fetchConcepts({ keyword: value });
});
</script>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/computed.html'),

('props와 emit', 'vue', 'BEGINNER',
 '부모 컴포넌트는 props로 자식에게 데이터를 내려주고, 자식은 emit으로 이벤트를 올려 부모에게 알린다. 데이터는 아래로, 이벤트는 위로 흐른다.',
'## 핵심
- `defineProps`로 받을 값과 타입을 선언한다.
- `defineEmits`로 올릴 이벤트 이름을 선언하고, `emit("이름", 값)`으로 보낸다.
- 부모는 `<Child :item="x" @remove="onRemove" />`처럼 연결한다.

## 주의
- 자식이 props를 직접 바꾸면 안 된다(읽기 전용). 바꾸고 싶으면 이벤트를 올려 부모가 바꾸게 한다.
- 여러 단계 아래로 계속 전달해야 하면 provide/inject나 Pinia 스토어를 고려한다.',
'<!-- ConceptItem.vue -->
<script setup>
const props = defineProps({ concept: { type: Object, required: true } });
const emit = defineEmits(["wish"]);
</script>

<template>
  <li>
    {{ props.concept.name }}
    <button @click="emit(''wish'', props.concept.id)">찜</button>
  </li>
</template>

<!-- 부모 -->
<ConceptItem v-for="c in concepts" :key="c.id" :concept="c" @wish="toggleWish" />',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/components/props.html'),

('컴포저블', 'vue', 'JUNIOR',
 '컴포저블은 상태와 로직을 묶어 여러 컴포넌트에서 재사용하는 함수다. 이름은 보통 use로 시작한다(useFetch, useAuth).',
'## 핵심
- 안에서 `ref`, `computed`, `watch`, 생명주기 훅을 써서 상태 로직을 캡슐화하고 필요한 값을 반환한다.
- 호출할 때마다 새 상태가 만들어진다. 전역으로 공유할 상태는 Pinia 스토어가 더 적합하다.
- 반환할 때 ref를 그대로 돌려주면 구조 분해해도 반응성이 유지된다.

## 주의
- `<script setup>` 안이나 setup 시점에 동기적으로 호출해야 생명주기 훅이 올바르게 연결된다.',
'// composables/useFetch.js
import { ref } from "vue";

export function useFetch(loader) {
  const data = ref(null);
  const error = ref(null);
  const loading = ref(false);

  async function run(...args) {
    loading.value = true;
    error.value = null;
    try {
      data.value = await loader(...args);
    } catch (e) {
      error.value = e;
    } finally {
      loading.value = false;
    }
  }

  return { data, error, loading, run };
}',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/reusability/composables.html'),

('Pinia 스토어', 'vue', 'BEGINNER',
 'Pinia는 Vue의 공식 상태 관리 라이브러리다. 로그인 사용자처럼 여러 화면이 함께 쓰는 상태를 스토어 하나에 모아 두고 어디서나 읽고 바꾼다.',
'## 핵심
- `defineStore("이름", ...)`로 정의한다. state(상태), getters(계산 값), actions(상태를 바꾸는 함수, API 호출)로 구성된다.
- setup 문법을 쓰면 `ref`가 state, `computed`가 getter, 함수가 action이 된다.
- 컴포넌트에서 `useXxxStore()`로 꺼내 쓴다.

## 주의
- 한 화면에서만 쓰는 상태까지 스토어에 넣으면 흐름이 복잡해진다.
- 새로고침하면 스토어 상태는 사라진다. 유지가 필요한 값(토큰 등)은 저장 위치와 보안을 따로 결정한다.',
'// stores/auth.js
import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useAuthStore = defineStore("auth", () => {
  const user = ref(null);
  const isLoggedIn = computed(() => user.value !== null);

  async function login(email, password) {
    const res = await api.post("/api/auth/login", { email, password });
    user.value = res.data.user;
  }

  function logout() {
    user.value = null;
  }

  return { user, isLoggedIn, login, logout };
});',
 'PUBLISHED', 'SEED', 'https://pinia.vuejs.org/core-concepts/'),

('storeToRefs', 'vue', 'JUNIOR',
 'Pinia 스토어를 구조 분해하면 반응성이 끊긴다. storeToRefs로 state와 getter를 ref로 꺼내면 반응성을 유지한 채 구조 분해할 수 있다.',
'## 핵심
- `const { user } = useAuthStore()`는 그 순간의 값만 복사한다. 이후 변경이 화면에 반영되지 않는다.
- `const { user } = storeToRefs(store)`는 ref를 돌려주므로 반응성이 유지된다.
- action(함수)은 `storeToRefs`로 꺼내지 않고 스토어에서 직접 구조 분해한다.

## 주의
"로그인했는데 헤더가 안 바뀐다" 같은 버그의 흔한 원인이다.',
'<script setup>
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";

const store = useAuthStore();
const { user, isLoggedIn } = storeToRefs(store); // 반응성 유지
const { logout } = store;                         // 액션은 그대로
</script>

<template>
  <span v-if="isLoggedIn">{{ user.nickname }}</span>
  <button v-if="isLoggedIn" @click="logout">로그아웃</button>
</template>',
 'PUBLISHED', 'SEED', 'https://pinia.vuejs.org/core-concepts/#Destructuring-from-a-Store'),

('네비게이션 가드 (Vue Router)', 'vue', 'JUNIOR',
 '네비게이션 가드는 라우트 이동 직전에 실행되는 함수다. 로그인하지 않은 사용자를 로그인 화면으로 보내는 등 이동을 허용·취소·변경할 수 있다.',
'## 핵심
- 전역 가드 `router.beforeEach((to, from) => ...)`에서 `false`를 반환하면 이동을 취소하고, 경로를 반환하면 그곳으로 보낸다.
- 라우트에 `meta: { requiresAuth: true }`를 달아 가드에서 확인하는 방식이 일반적이다.
- 가드 안에서 Pinia 스토어를 쓸 수 있다(앱에 Pinia가 설치된 뒤 호출).

## 주의
프론트엔드 가드는 화면 이동만 막는다. 보안은 반드시 백엔드 API가 인증·인가로 지켜야 한다.',
'// router/index.js
router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
});

// 라우트 정의
{ path: "/analyses", component: AnalysisList, meta: { requiresAuth: true } }',
 'PUBLISHED', 'SEED', 'https://router.vuejs.org/guide/advanced/navigation-guards.html')

AS seed
ON DUPLICATE KEY UPDATE
    category      = seed.category,
    difficulty    = seed.difficulty,
    summary       = seed.summary,
    body          = seed.body,
    example_code  = seed.example_code,
    status        = seed.status,
    source        = seed.source,
    reference_url = seed.reference_url;
