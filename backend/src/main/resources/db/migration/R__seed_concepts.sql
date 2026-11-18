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

## 최신 문법
- Java 16부터 `.toList()`로 바로 리스트를 만든다. 결과는 **수정할 수 없는 리스트**다.
- 예전 코드의 `.collect(Collectors.toList())`는 수정 가능한 `ArrayList`를 돌려준다. 결과에 `add`하는 코드가 있으면 둘을 바꿔 쓸 때 `UnsupportedOperationException`이 난다.

## 주의
- 스트림 안에서 외부 리스트에 `add`하는 식의 부수 효과는 피한다. 결과는 `toList`·`collect`로 만든다.
- 단순한 반복은 for문이 더 읽기 쉬울 때도 있다.',
'List<String> names = concepts.stream()
        .filter(c -> c.status() == Status.PUBLISHED)
        .map(Concept::name)
        .toList();                                   // Java 16+, 불변 리스트

Map<String, Long> countByCategory = concepts.stream()
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

## 실무 패턴
- 요청 DTO: 컴포넌트에 Bean Validation 어노테이션(`@NotBlank` 등)을 단다. Jackson이 record를 바로 역직렬화한다.
- 응답 DTO: DB 행 객체에서 필요한 필드만 골라 담는 정적 팩토리 `from()`을 둔다.
- 값 객체·메서드 반환 묶음(예: 결과 + 개수)에도 쓴다.

## 주의
- setter가 없으므로 값을 바꾸려면 새 객체를 만든다.
- 필드가 final이어도 안에 담긴 `List`는 바뀔 수 있다. 완전히 불변으로 만들려면 compact 생성자에서 `List.copyOf`로 복사한다.
- Lombok `@Data`·`@Getter` DTO와 섞여 있으면 접근자 이름(`name()`과 `getName()`)이 달라 매퍼·템플릿에서 헷갈린다.',
'public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 20) String nickname) { }

public record UserResponse(long id, String nickname, List<String> roles) {
    public UserResponse {
        roles = List.copyOf(roles);              // 방어적 복사로 완전 불변
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getRoles());
    }
}

UserResponse res = UserResponse.from(user);
res.nickname();   // get 없이 필드 이름으로 읽는다',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/language/records.html'),

('switch 식과 instanceof 패턴 매칭', 'java', 'JUNIOR',
 'Java 14의 switch 식은 화살표(->)로 값을 바로 돌려주고 break 누락 실수를 없앤다. Java 16의 instanceof 패턴 매칭은 타입 확인과 형변환을 한 번에 한다.',
'## 핵심
- switch 식: `case A, B -> 값;` 형태로 쓰고, switch 전체가 값이 된다. 위에서 아래로 떨어지는(fall-through) 동작이 없다.
- enum을 switch 식으로 다룰 때 모든 상수를 처리하면 `default`가 없어도 된다. 상수가 새로 추가되면 컴파일 오류로 알려 준다.
- 여러 줄이 필요하면 블록 안에서 `yield 값;`으로 돌려준다.
- `if (obj instanceof String s)`: 확인이 통과하면 `s`를 바로 String으로 쓴다. 따로 `(String) obj` 형변환을 쓰지 않는다.

## AI 코드에서 보이는 모습
상태 enum(예: 분석 단계, 주문 상태)에 따라 메시지·진행률을 정하는 코드에서 자주 보인다. 예전 스타일 `case X: ... break;`와 섞여 있으면 새 문법으로 정리할 수 있다.

## 주의
- 화살표(`->`)와 콜론(`:`) 스타일을 한 switch 안에서 섞을 수 없다.
- `default`를 습관처럼 넣으면 enum 상수가 추가돼도 컴파일러가 알려 주지 못한다.',
'enum Status { FETCHING, EXPLAINING, QUIZ, DONE, FAILED }

int progress = switch (status) {
    case FETCHING -> 10;
    case EXPLAINING, QUIZ -> 60;
    case DONE -> 100;
    case FAILED -> {
        log.warn("분석 실패");
        yield 0;
    }
};   // 모든 상수를 다뤘으므로 default 불필요

// instanceof 패턴 매칭 (Java 16+)
if (ex instanceof BusinessException be) {
    return be.getErrorCode();
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/language/switch-expressions-and-statements.html'),

('텍스트 블록과 var', 'java', 'BEGINNER',
 '텍스트 블록(Java 15)은 큰따옴표 세 개로 여러 줄 문자열을 그대로 쓰는 문법이고, var(Java 10)는 지역 변수 타입을 컴파일러가 추론하게 하는 키워드다.',
'## 핵심
- 텍스트 블록: `"""` 다음 줄부터 내용을 쓴다. 줄바꿈과 따옴표를 이스케이프하지 않아도 되어 JSON·SQL·프롬프트 문자열이 읽기 쉬워진다. 공통 들여쓰기는 자동으로 제거된다.
- `formatted(...)`로 값을 채운다.
- var: `var list = new ArrayList<String>();`처럼 오른쪽에서 타입이 분명할 때 쓴다. 지역 변수에만 쓸 수 있다.

## AI 코드에서 보이는 모습
테스트의 기대 JSON, LLM 프롬프트 템플릿, 긴 SQL 문자열에 텍스트 블록이 자주 쓰인다.

## 주의
- var는 타입이 사라지는 것이 아니라 컴파일 시점에 정해진다. 오른쪽만 봐서 타입이 안 보이면(`var result = service.process();`) 읽는 사람이 헷갈리므로 쓰지 않는다.
- 필드, 메서드 파라미터, 반환 타입에는 var를 쓸 수 없다.',
'String body = """
        {
          "email": "%s",
          "nickname": "vibecoder"
        }
        """.formatted(email);

mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));

var users = new ArrayList<User>();       // 타입이 오른쪽에 드러난다
for (var entry : countByCategory.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/language/text-blocks.html'),

('패턴 매칭 switch와 record 패턴 (Java 21)', 'java', 'SENIOR',
 'Java 21부터 switch에서 값뿐 아니라 타입으로 분기하고(case Circle c), record를 분해(case Point(int x, int y))할 수 있다. sealed 인터페이스와 함께 쓰면 모든 경우를 처리했는지 컴파일러가 확인한다.',
'## 핵심
- **Java 21부터** 쓸 수 있다. 바이브노트 백엔드는 Java 17이라 이 문법이 컴파일되지 않지만, 분석하는 저장소에서는 만날 수 있다.
- `case 타입 변수 ->`: 타입별 분기와 형변환을 한 번에 한다.
- `case Record(타입 a, 타입 b) ->`: record의 구성 요소를 바로 꺼낸다.
- `case X x when 조건 ->`: 가드 조건을 붙인다.
- `sealed interface Shape permits Circle, Square`처럼 하위 타입을 닫아 두면 `default` 없이도 빠진 경우를 컴파일 오류로 알려 준다.
- `case null ->`로 null도 switch 안에서 처리할 수 있다.

## Java 17에서는
같은 일을 `if (shape instanceof Circle c) ... else if ...` 연쇄로 한다.

## 주의
- 위쪽 case가 아래 case를 항상 가리면(더 넓은 타입이 먼저) 컴파일 오류가 난다. 좁은 타입부터 쓴다.',
'sealed interface Result permits Success, Failure { }
record Success(String data) implements Result { }
record Failure(String code, String message) implements Result { }

String render(Result result) {
    return switch (result) {
        case Success(String data) -> "성공: " + data;
        case Failure(String code, String msg) when code.startsWith("LLM") -> "AI 오류: " + msg;
        case Failure(String code, String msg) -> "오류(" + code + "): " + msg;
    };   // sealed라 default 불필요
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/21/language/pattern-matching-switch.html'),

('가상 스레드 (Java 21)', 'java', 'SENIOR',
 '가상 스레드는 Java 21에 정식 도입된 가벼운 스레드다. 운영체제 스레드 하나 위에서 수많은 가상 스레드가 번갈아 실행되어, I/O 대기가 많은 서버에서 요청마다 스레드를 써도 자원이 부족해지지 않는다.',
'## 핵심
- **Java 21부터** 쓸 수 있다. 바이브노트 백엔드는 Java 17이라 쓰지 않는다.
- DB·HTTP 응답을 기다리는 동안 가상 스레드가 운영체제 스레드를 내려놓으므로, 블로킹 코드를 그대로 두고도 동시 처리량이 늘어난다.
- 만들기: `Thread.ofVirtual().start(...)`, `Executors.newVirtualThreadPerTaskExecutor()`.
- Spring Boot 3.2 이상 + Java 21이면 `spring.threads.virtual.enabled=true` 한 줄로 웹 요청 처리와 `@Async`에 가상 스레드를 쓴다.

## 주의
- CPU 계산이 많은 작업은 빨라지지 않는다. 효과는 I/O 대기가 많을 때 난다.
- 가상 스레드는 싸게 만드는 것이 전제라 풀링하지 않는다.
- 동시 요청이 늘어나는 만큼 DB 커넥션 풀이나 외부 API 한도가 먼저 병목이 된다. 동시성 제한은 세마포어 등으로 따로 건다.
- 가상 스레드에서 ThreadLocal에 큰 객체를 담으면 수가 많아 메모리를 크게 쓴다.',
'# application.yml (Spring Boot 3.2+, Java 21)
spring:
  threads:
    virtual:
      enabled: true

// 직접 사용
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<String>> futures = urls.stream()
            .map(url -> executor.submit(() -> httpGet(url)))   // 요청마다 가상 스레드
            .toList();
    for (Future<String> f : futures) {
        System.out.println(f.get().length());
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html'),

('인터페이스와 다형성', 'java', 'BEGINNER',
 '인터페이스는 "무엇을 할 수 있는가"만 약속하고, 실제 동작은 구현 클래스가 정한다. 같은 타입으로 여러 구현을 바꿔 끼울 수 있는 성질이 다형성이다.',
'## 핵심
- 인터페이스는 메서드 시그니처(약속)만 정의하고, 클래스가 `implements`로 그 약속을 지킨다.
- 호출하는 쪽은 인터페이스 타입만 알면 되므로 구현을 바꿔도 코드를 고치지 않는다.
- Java 8부터 `default` 메서드로 구현을 가진 메서드를, `static` 메서드로 도우미를 둘 수 있다. 인터페이스에 메서드를 추가해도 기존 구현 클래스가 깨지지 않는다.

## AI 코드에서 보이는 모습
- `UserService` 인터페이스와 `UserServiceImpl` 구현을 나누는 패턴이 흔하다. 구현이 하나뿐이면 나누지 않는 팀도 많다. 테스트용 가짜 구현이나 교체 가능성(Mock·실제 LLM 클라이언트처럼)이 있을 때 의미가 생긴다.
- 스프링은 같은 인터페이스의 구현 빈을 `List<Notifier>`로 모두 주입받을 수 있다. 전략 패턴을 if 분기 없이 만들 때 쓴다.

## 주의
인터페이스를 만들었다고 다형성이 생기는 것은 아니다. 호출하는 쪽이 구현 클래스 타입을 직접 쓰면 이점이 사라진다.',
'public interface Notifier {
    void send(String to, String message);

    default boolean supports(String channel) {   // Java 8+ 기본 구현
        return false;
    }
}

@Component
public class EmailNotifier implements Notifier {
    public void send(String to, String message) { /* 메일 발송 */ }

    @Override
    public boolean supports(String channel) { return "EMAIL".equals(channel); }
}

@Service
public class NotificationService {
    private final List<Notifier> notifiers;      // 구현 빈이 모두 주입된다

    public NotificationService(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void notify(String channel, String to, String message) {
        notifiers.stream()
                .filter(n -> n.supports(channel))
                .findFirst()
                .orElseThrow()
                .send(to, message);
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/java/IandI/createinterface.html'),

('sealed 클래스와 인터페이스', 'java', 'JUNIOR',
 'sealed(Java 17)는 어떤 클래스가 이 타입을 상속·구현할 수 있는지 permits로 정해 두는 문법이다. 하위 타입이 닫혀 있으므로 "가능한 경우가 이것뿐"임을 컴파일러가 안다.',
'## 핵심
- `sealed interface Shape permits Circle, Square { }`: Circle과 Square만 Shape를 구현할 수 있다.
- 허용된 하위 타입은 `final`, `sealed`, `non-sealed` 중 하나를 선언해야 한다. record는 자동으로 final이라 잘 어울린다.
- 결과 타입(성공·실패), 이벤트 종류, 결제 수단처럼 경우의 수가 정해진 모델에 쓴다.
- Java 21의 패턴 매칭 switch와 함께 쓰면 `default` 없이 모든 경우를 처리했는지 컴파일러가 확인한다.

## 주의
- 같은 패키지(모듈을 쓰면 같은 모듈)에 있어야 permits로 묶을 수 있다.
- 나중에 하위 타입을 자주 추가해야 하는 확장 지점에는 맞지 않는다.',
'public sealed interface PaymentMethod permits Card, BankTransfer, Point { }

public record Card(String number) implements PaymentMethod { }
public record BankTransfer(String bank, String account) implements PaymentMethod { }
public record Point(long amount) implements PaymentMethod { }

// Java 17: instanceof 패턴 매칭으로 분기
String describe(PaymentMethod m) {
    if (m instanceof Card c) return "카드 " + c.number().substring(12);
    if (m instanceof BankTransfer b) return b.bank() + " 이체";
    if (m instanceof Point p) return p.amount() + "포인트";
    throw new IllegalStateException();   // Java 21 switch에서는 불필요
}',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html'),

('제네릭', 'java', 'JUNIOR',
 '제네릭은 클래스나 메서드가 다룰 타입을 파라미터로 받는 문법이다(List<String>). 잘못된 타입을 컴파일 시점에 막고 형변환을 없앤다.',
'## 핵심
- `class Box<T>`, `<T> T first(List<T> list)`처럼 타입 파라미터를 선언한다.
- `List<String>`에는 String만 들어가므로 꺼낼 때 형변환이 필요 없다.
- 와일드카드: `List<? extends Number>`는 읽기 전용(생산자), `List<? super Integer>`는 쓰기용(소비자)으로 쓴다.
- 다이아몬드 `new ArrayList<>()`로 오른쪽 타입을 생략한다.

## AI 코드에서 보이는 모습
공통 응답 래퍼 `ApiResponse<T>`, 페이지 응답 `PageResponse<T>`, `ResponseEntity<T>`, MyBatis 결과 `List<T>`에서 자주 보인다.

## 주의
- 로 타입(`List list`)을 쓰면 타입 검사가 꺼져 실행 중 `ClassCastException`이 날 수 있다.
- 제네릭 타입 정보는 실행 시점에 지워진다(타입 소거). `new T()`나 `instanceof List<String>`은 할 수 없다.',
'public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(items.stream().map(mapper).toList(), total, page, size);
    }
}

PageResponse<User> users = userService.findPage(page);
PageResponse<UserResponse> body = users.map(UserResponse::from);',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/java/generics/'),

('컬렉션과 불변 컬렉션 (List·Set·Map)', 'java', 'BEGINNER',
 'List는 순서가 있는 목록, Set은 중복 없는 집합, Map은 키-값 쌍이다. Java 9부터 List.of·Set.of·Map.of로 수정할 수 없는 컬렉션을 간단히 만든다.',
'## 핵심
- 구현체 선택: 대부분 `ArrayList`, `HashSet`, `HashMap`. 넣은 순서가 필요하면 `LinkedHashMap`, 정렬이 필요하면 `TreeMap`.
- 변수 타입은 인터페이스(`List`, `Map`)로 선언한다.
- `List.of(...)`, `Map.of(...)`, `List.copyOf(...)`: 불변 컬렉션. 수정하면 `UnsupportedOperationException`.
- `map.getOrDefault`, `computeIfAbsent`, `merge`로 "없으면 넣기"를 짧게 쓴다.

## 주의
- `List.of`·`Map.of`는 null을 넣을 수 없다.
- `Map.of`는 순서를 보장하지 않는다. 화면 출력 순서가 중요하면 `LinkedHashMap`을 쓴다.
- 반복문으로 순회하면서 같은 컬렉션에서 삭제하면 `ConcurrentModificationException`이 난다. `removeIf`를 쓴다.',
'List<String> levels = List.of("BEGINNER", "JUNIOR", "SENIOR");   // 불변

Map<String, List<Concept>> byCategory = new HashMap<>();
for (Concept c : concepts) {
    byCategory.computeIfAbsent(c.category(), k -> new ArrayList<>()).add(c);
}

Map<String, Integer> wrongCount = new HashMap<>();
wrongCount.merge(conceptName, 1, Integer::sum);   // 없으면 1, 있으면 +1

concepts.removeIf(c -> c.status() == Status.HIDDEN);',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/core/creating-immutable-lists-sets-and-maps.html'),

('날짜와 시간 API (java.time)', 'java', 'BEGINNER',
 'java.time(Java 8+)은 LocalDate·LocalDateTime·Instant·Duration 같은 불변 타입으로 날짜와 시간을 다룬다. 예전 Date·Calendar의 문제(가변, 0부터 시작하는 월)를 해결했다.',
'## 핵심
- `LocalDate`(날짜), `LocalDateTime`(날짜+시간, 시간대 없음), `ZonedDateTime`(시간대 포함), `Instant`(UTC 기준 시점).
- `Duration`(초·나노초 간격), `Period`(년·월·일 간격).
- 모든 타입이 불변이다. `plusDays(1)`은 새 객체를 돌려준다.
- 스프링·Jackson·MyBatis 모두 java.time을 바로 지원한다. MySQL `DATETIME` ↔ `LocalDateTime`, `DATE` ↔ `LocalDate`.

## 주의
- `LocalDateTime.now()`는 서버 시간대를 따른다. 서버·DB·JVM 시간대(예: Asia/Seoul)를 맞춰 두지 않으면 9시간 차이 버그가 난다.
- 테스트에서 현재 시각에 의존하면 결과가 날마다 달라진다. `Clock`을 주입받아 고정한다.
- `plusDays` 결과를 변수에 다시 담지 않으면 아무 일도 일어나지 않는다.',
'LocalDate today = LocalDate.now(clock);
LocalDate nextReview = today.plusDays(3);              // 새 객체

boolean overdue = plan.dueDate().isBefore(today);
long days = ChronoUnit.DAYS.between(start, today);

Duration latency = Duration.between(startedAt, Instant.now());
log.info("LLM 응답 {}ms", latency.toMillis());

String label = createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/package-summary.html'),

('enum 활용', 'java', 'JUNIOR',
 'enum은 정해진 상수 집합을 타입으로 만든다. 단순 상수를 넘어 필드·생성자·메서드를 가질 수 있어, 상태별 값과 동작을 한곳에 모은다.',
'## 핵심
- 문자열 상수 대신 enum을 쓰면 오타를 컴파일 시점에 잡는다.
- 상수마다 값(HTTP 상태, 메시지, 점수)을 생성자로 넣어 둔다.
- `values()`로 전체 목록, `valueOf("JUNIOR")`로 문자열 변환.
- MyBatis는 enum을 이름 문자열로 저장·조회한다(MySQL `ENUM`·`VARCHAR` 컬럼과 잘 맞는다). Jackson도 이름으로 직렬화한다.

## AI 코드에서 보이는 모습
에러 코드(`ErrorCode.USER_NOT_FOUND(404, "...")`), 상태(`DRAFT·PUBLISHED`), 수준(`BEGINNER·JUNIOR·SENIOR`)에 자주 쓰인다.

## 주의
- `ordinal()`(선언 순서 번호)을 DB에 저장하면 상수 순서를 바꿀 때 데이터가 어긋난다. 이름으로 저장한다.
- 잘못된 문자열을 `valueOf`하면 `IllegalArgumentException`이 난다. 요청 파라미터 변환 실패는 400으로 처리한다.',
'public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}

throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html'),

('CompletableFuture (비동기 조합)', 'java', 'SENIOR',
 'CompletableFuture는 나중에 끝나는 작업의 결과를 나타내고, thenApply·thenCombine·allOf로 여러 비동기 작업을 이어 붙이거나 함께 기다린다.',
'## 핵심
- `supplyAsync(작업, executor)`: 다른 스레드에서 작업을 시작한다.
- `thenApply`(결과 변환), `thenCompose`(이어지는 비동기 작업), `thenCombine`(두 결과 합치기), `allOf`(모두 기다리기).
- `exceptionally`·`handle`로 실패를 처리한다.
- 스프링 `@Async` 메서드는 `CompletableFuture<T>`를 반환할 수 있다.

## 주의
- executor를 지정하지 않으면 공용 ForkJoinPool을 쓴다. 블로킹 I/O(HTTP, DB)는 전용 스레드 풀을 넘긴다.
- `join()`·`get()`을 바로 부르면 결국 기다리게 되어 비동기 이점이 없다. 모아서 마지막에 기다린다.
- 비동기 스레드에서는 요청 스레드의 `SecurityContext`와 트랜잭션이 이어지지 않는다.',
'ExecutorService io = Executors.newFixedThreadPool(8);

CompletableFuture<Profile> profile =
        CompletableFuture.supplyAsync(() -> profileClient.get(userId), io);
CompletableFuture<Stats> stats =
        CompletableFuture.supplyAsync(() -> statsClient.get(userId), io);

Dashboard dashboard = profile
        .thenCombine(stats, Dashboard::new)
        .exceptionally(ex -> Dashboard.empty())
        .join();   // 두 호출을 동시에 보내고 마지막에 한 번 기다린다',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html'),

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
- prototype: 요청할 때마다 새로 만든다. 컨테이너는 만든 뒤 소멸을 관리하지 않는다.
- request·session: 웹 요청·HTTP 세션마다 하나. 요청 추적 ID처럼 요청 단위 정보를 담을 때 쓴다.

## 실무에서
- 대부분은 싱글톤 + 상태 없는(stateless) 빈으로 충분하다. 다른 스코프가 필요해 보이면 먼저 "메서드 파라미터로 넘기면 되지 않나"를 생각한다.
- 싱글톤 빈이 더 짧은 스코프(request·prototype) 빈을 쓰려면 `ObjectProvider`로 꺼내거나 스코프 프록시(`proxyMode`)를 쓴다.

## 주의
- 싱글톤 빈이 prototype 빈을 생성자로 주입받으면 처음 한 번만 만들어진다.
- 싱글톤 빈에 변경 가능한 필드를 두면 요청끼리 값이 섞인다(동시성 문제).',
'@Component
@RequestScope                        // 요청마다 새 인스턴스, 싱글톤에는 프록시가 주입된다
public class RequestContext {
    private final String traceId = UUID.randomUUID().toString();
    public String traceId() { return traceId; }
}

@Service
public class AnalysisService {
    private final RequestContext requestContext;

    public AnalysisService(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public void request() {
        log.info("[{}] 분석 요청", requestContext.traceId());
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

## Boot 3에서 바뀐 점
- 자동 구성 클래스는 `@AutoConfiguration`을 붙이고, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 한 줄씩 등록한다.
- 예전 `spring.factories`의 `EnableAutoConfiguration` 키 방식은 Boot 3.0에서 제거됐다. 블로그·AI 예제가 이 방식을 쓰면 Boot 2 시절 코드다.
- Boot 2 라이브러리를 Boot 3에 넣으면 자동 구성이 조용히 동작하지 않을 수 있다. starter 버전이 Boot 3를 지원하는지 확인한다.

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

('RestClient (외부 API 호출)', 'spring', 'JUNIOR',
 'RestClient는 Spring 6.1(Boot 3.2)에 추가된 동기 HTTP 클라이언트다. 메서드를 이어 붙이는 방식으로 외부 API를 호출하고 응답을 객체로 받는다. 인터페이스만 선언하는 HTTP 인터페이스(@HttpExchange)와 함께 쓸 수 있다.',
'## 핵심
- `restClient.get().uri(...).retrieve().body(타입)` 순서로 쓴다.
- 4xx·5xx 응답은 기본적으로 예외(`RestClientResponseException`)가 된다. `onStatus`로 상태별 처리를 바꾼다.
- Boot는 `RestClient.Builder` 빈을 제공한다. 주입받아 `baseUrl`, 기본 헤더를 정한 뒤 `build()`한다.
- HTTP 인터페이스: `@GetExchange("/users/{id}")`처럼 메서드만 선언하면 구현이 자동으로 만들어진다.

## 최신 문법
- `RestTemplate`은 유지 보수 모드다. 새 코드는 RestClient를 쓴다. `WebClient`는 리액티브(WebFlux) 스택용이다.

## 주의
- **타임아웃을 꼭 설정한다.** 외부 서버가 응답하지 않으면 요청 스레드가 계속 묶인다.
- 외부 호출을 `@Transactional` 안에서 하면 응답을 기다리는 동안 DB 커넥션을 잡는다. 호출과 저장을 나눈다.
- API 키는 환경변수로 받고 로그에 찍지 않는다.',
'@Configuration
public class GithubClientConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .requestFactory(factory)
                .build();
    }
}

Repo repo = githubRestClient.get()
        .uri("/repos/{owner}/{name}", owner, name)
        .retrieve()
        .onStatus(status -> status.value() == 404, (req, res) -> {
            throw new BusinessException(ErrorCode.REPO_NOT_FOUND);
        })
        .body(Repo.class);

// HTTP 인터페이스
public interface GithubApi {
    @GetExchange("/repos/{owner}/{name}")
    Repo getRepo(@PathVariable String owner, @PathVariable String name);
}

GithubApi api = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(githubRestClient))
        .build()
        .createClient(GithubApi.class);',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/integration/rest-clients.html'),

('@Async 비동기 실행', 'spring', 'JUNIOR',
 '@Async를 붙인 메서드는 호출한 스레드가 아니라 별도 스레드 풀에서 실행된다. 오래 걸리는 작업(분석, 메일 발송)을 요청 처리와 분리해 응답을 먼저 돌려줄 때 쓴다.',
'## 핵심
- 설정 클래스에 `@EnableAsync`를 붙인다.
- 반환 타입은 `void` 또는 `CompletableFuture<T>`.
- Boot는 `ThreadPoolTaskExecutor`를 자동 구성한다. `spring.task.execution.pool.*`로 크기를 조정한다.
- "요청 접수 → 202 응답 → 백그라운드 처리 → 상태를 DB에 기록 → 클라이언트가 조회" 패턴과 잘 맞는다.

## 주의
- 프록시 방식이라 **같은 클래스 안에서 호출하면 비동기로 실행되지 않는다.** 다른 빈에서 호출한다.
- `void` 메서드의 예외는 호출자에게 전달되지 않는다. 메서드 안에서 잡아 실패 상태를 기록하거나 `AsyncUncaughtExceptionHandler`를 둔다.
- 호출한 스레드의 트랜잭션과 `SecurityContext`는 이어지지 않는다. 필요한 값(사용자 ID 등)은 파라미터로 넘긴다.
- 커밋 전에 비동기 작업이 시작되면 아직 저장되지 않은 데이터를 읽을 수 있다. 커밋 후에 시작하려면 이벤트(`@TransactionalEventListener`)와 함께 쓴다.',
'@Configuration
@EnableAsync
public class AsyncConfig { }

@Service
public class AnalysisWorker {

    @Async
    public void run(long analysisId) {
        try {
            pipeline.execute(analysisId);
            statusService.markDone(analysisId);
        } catch (Exception e) {
            log.error("분석 실패 id={}", analysisId, e);
            statusService.markFailed(analysisId, e.getMessage());
        }
    }
}

// Controller → Service가 저장 후 다른 빈의 @Async 메서드 호출 → 즉시 202 응답',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/integration/scheduling.html'),

('@Scheduled 스케줄링', 'spring', 'JUNIOR',
 '@Scheduled를 붙인 메서드는 정해진 주기나 cron 표현식에 따라 자동으로 실행된다. 만료 데이터 정리, 집계, 알림 같은 배치 작업에 쓴다.',
'## 핵심
- 설정 클래스에 `@EnableScheduling`을 붙인다.
- `fixedRate`: 시작 시점 기준 주기. `fixedDelay`: 이전 실행이 끝난 뒤 기다리는 시간. `cron`: 달력 기준.
- 스프링 cron은 **6자리**(초 분 시 일 월 요일)다. 리눅스 crontab(5자리)과 다르다.
- `zone = "Asia/Seoul"`로 시간대를 명시한다.

## 주의
- Boot 기본 스케줄러 스레드는 1개다. 작업 하나가 오래 걸리면 다른 작업이 밀린다. `spring.task.scheduling.pool.size`로 늘린다.
- 서버를 여러 대 띄우면 **모든 서버에서 실행된다.** 한 번만 실행해야 하면 DB 락 같은 분산 락이 필요하다.
- 예외가 나도 다음 주기에 다시 실행되지만 로그를 남기지 않으면 실패를 모른다.',
'@Configuration
@EnableScheduling
public class SchedulingConfig { }

@Component
public class CleanupJob {

    // 매일 새벽 4시 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredTokens() {
        int deleted = refreshTokenMapper.deleteExpired(LocalDateTime.now());
        log.info("만료 토큰 {}건 삭제", deleted);
    }

    @Scheduled(fixedDelay = 60_000)   // 끝나고 1분 뒤 다시
    public void retryFailedNotifications() { ... }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled'),

('애플리케이션 이벤트 (@TransactionalEventListener)', 'spring', 'SENIOR',
 '스프링 이벤트는 "어떤 일이 일어났다"를 발행하고 관심 있는 리스너가 받아 처리하게 해 서비스 간 직접 호출을 줄인다. @TransactionalEventListener는 트랜잭션이 커밋된 뒤에만 리스너를 실행한다.',
'## 핵심
- `ApplicationEventPublisher.publishEvent(이벤트)`로 발행한다. 이벤트는 보통 record로 만든다.
- `@EventListener`: 발행 즉시 같은 스레드·같은 트랜잭션 안에서 실행된다.
- `@TransactionalEventListener`: 기본 단계가 `AFTER_COMMIT`이다. 롤백되면 실행되지 않으므로 "저장이 확정된 뒤 알림·비동기 작업 시작"에 맞다.
- `@Async`와 함께 쓰면 커밋 후 별도 스레드에서 처리한다.

## AI 코드에서 보이는 모습
가입 후 환영 메일, 글 작성 후 활동 피드 기록, 분석 접수 후 파이프라인 시작처럼 "주 작업 + 부가 작업"을 나눌 때 쓴다.

## 주의
- 트랜잭션 없이 발행하면 `@TransactionalEventListener`는 실행되지 않는다(`fallbackExecution = true`로 바꿀 수 있다).
- `AFTER_COMMIT` 리스너 안에서 DB에 쓰려면 이미 끝난 트랜잭션에 참여할 수 없으므로 `REQUIRES_NEW` 트랜잭션을 연다.
- 이벤트 흐름이 많아지면 코드만 보고 실행 순서를 추적하기 어렵다. 꼭 필요한 곳에만 쓴다.',
'public record AnalysisRequested(long analysisId) { }

@Service
public class AnalysisService {

    @Transactional
    public long request(AnalysisRequest req, long userId) {
        long id = analysisMapper.insert(req, userId);
        events.publishEvent(new AnalysisRequested(id));
        return id;                        // 커밋되면 리스너가 실행된다
    }
}

@Component
public class AnalysisStarter {

    @Async
    @TransactionalEventListener      // 기본 AFTER_COMMIT
    public void on(AnalysisRequested event) {
        worker.run(event.analysisId());
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html'),

('캐시 추상화 (@Cacheable)', 'spring', 'JUNIOR',
 '@Cacheable을 붙인 메서드는 같은 인자로 다시 호출되면 메서드를 실행하지 않고 저장해 둔 결과를 돌려준다. 자주 읽고 드물게 바뀌는 데이터의 조회 부담을 줄인다.',
'## 핵심
- `@EnableCaching`으로 켠다. Boot는 캐시 라이브러리가 없으면 메모리 맵(`ConcurrentMapCache`)을 쓴다.
- `@Cacheable(cacheNames = "concepts", key = "#id")`: 결과를 저장하고 재사용한다.
- `@CacheEvict`: 데이터가 바뀔 때 캐시를 지운다. `@CachePut`: 항상 실행하고 결과로 캐시를 갱신한다.
- 운영에서는 만료 시간(TTL)을 지원하는 Caffeine이나 Redis를 붙인다.

## 주의
- 기본 메모리 캐시는 만료가 없어 계속 쌓인다.
- 수정·삭제 경로에서 `@CacheEvict`를 빠뜨리면 오래된 데이터가 계속 보인다.
- 프록시 방식이라 같은 클래스 내부 호출에는 캐시가 적용되지 않는다.
- 사용자별로 다른 결과(권한, 찜 여부)를 키 없이 캐시하면 다른 사람의 데이터가 보인다.',
'@Configuration
@EnableCaching
public class CacheConfig { }

@Service
public class ConceptService {

    @Cacheable(cacheNames = "concept", key = "#id")
    public ConceptResponse get(long id) {
        return ConceptResponse.from(conceptMapper.findById(id));
    }

    @CacheEvict(cacheNames = "concept", key = "#id")
    @Transactional
    public void update(long id, ConceptUpdateRequest req) {
        conceptMapper.update(id, req);
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/integration/cache.html'),

('Actuator (헬스 체크·메트릭)', 'spring', 'BEGINNER',
 'Spring Boot Actuator는 애플리케이션 상태 확인(health), 메트릭, 설정 정보 같은 운영용 엔드포인트를 /actuator 아래에 제공한다.',
'## 핵심
- `spring-boot-starter-actuator` 의존성을 추가하면 `/actuator/health`가 열린다. DB 연결 같은 하위 상태도 함께 점검한다.
- 웹에는 기본적으로 health만 노출된다. 나머지는 `management.endpoints.web.exposure.include`로 고른다.
- 로드밸런서·컨테이너 오케스트레이터의 헬스 체크, 배포 후 확인에 쓴다.
- 메트릭은 Micrometer로 수집해 Prometheus 같은 모니터링 도구로 보낸다.

## 주의
- `env`, `heapdump`, `configprops`는 비밀 값이나 메모리 내용이 드러날 수 있다. 공개 포트에 열지 않는다.
- `include: "*"` 설정은 개발용으로만 쓴다.
- Spring Security를 쓰면 actuator 경로의 접근 규칙도 따로 정한다.',
'# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: when-authorized

# 확인
GET /actuator/health   ->  {"status":"UP"}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/actuator/index.html'),

('로깅 (SLF4J·Logback)', 'spring', 'BEGINNER',
 '스프링 부트는 SLF4J 인터페이스와 Logback 구현으로 로그를 남긴다. 로그 레벨(ERROR·WARN·INFO·DEBUG·TRACE)과 패키지별 설정으로 필요한 만큼만 출력한다.',
'## 핵심
- `LoggerFactory.getLogger(클래스)`로 로거를 만든다. Lombok `@Slf4j`도 같은 일을 한다.
- 문자열 더하기 대신 `{}` 자리 표시자를 쓴다. 레벨이 꺼져 있으면 문자열을 만들지 않는다.
- 예외는 마지막 인자로 넘기면 스택 트레이스가 함께 찍힌다.
- `logging.level.패키지=debug`로 패키지별 레벨을 정한다.

## 최신 문법
- Boot 3.4부터 `logging.structured.format.console=ecs`(또는 logstash)로 JSON 구조화 로그를 설정 한 줄로 낸다. 로그 수집 도구에서 필드로 검색할 수 있다.

## 주의
- `System.out.println`은 레벨·시간·스레드 정보가 없고 끌 수 없다. 서버 코드에서는 로거를 쓴다.
- 비밀번호, 토큰, API 키, 개인정보를 로그에 남기지 않는다.
- 반복문 안 INFO 로그는 로그 양을 폭발시킨다.',
'@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    public void login(String email) {
        log.info("로그인 시도 email={}", maskEmail(email));
        try {
            ...
        } catch (Exception e) {
            log.error("로그인 처리 실패 email={}", maskEmail(email), e);  // 예외는 마지막 인자
            throw e;
        }
    }
}

# application.yml
logging:
  level:
    root: info
    com.vibenote: debug
    org.springframework.security: warn',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/features/logging.html'),

('스프링 부트 테스트 (@SpringBootTest·@WebMvcTest)', 'spring', 'JUNIOR',
 '@SpringBootTest는 애플리케이션 전체를 띄우는 통합 테스트이고, @WebMvcTest는 컨트롤러 계층만 띄우는 슬라이스 테스트다. MockMvc로 실제 서버 없이 HTTP 요청을 보내 검증한다.',
'## 핵심
- `@SpringBootTest` + `@AutoConfigureMockMvc`: 컨트롤러부터 DB까지 실제 빈으로 검증한다. 느리지만 믿을 만하다.
- `@WebMvcTest(XxxController.class)`: MVC 관련 빈만 올리고 Service는 가짜로 바꾼다. 빠르다.
- MockMvc: `perform(get(...))` → `andExpect(status().isOk())`, `jsonPath("$.field")`로 응답을 검사한다.

## 최신 문법
- Spring Framework 6.2·Boot 3.4부터 가짜 빈은 `@MockitoBean`으로 만든다. 예전 `@MockBean`은 사용 중단(deprecated)되었다.
- AssertJ 스타일의 `MockMvcTester`도 추가되었다.

## 주의
- 테스트마다 다른 설정(`@MockitoBean` 조합, 프로퍼티)을 쓰면 스프링 컨텍스트를 재사용하지 못해 전체 테스트가 느려진다.
- 테스트끼리 DB 데이터가 섞이지 않게 `@Transactional` 롤백이나 정리 스크립트를 쓴다.',
'@WebMvcTest(ConceptController.class)
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
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html'),

('Testcontainers 통합 테스트', 'spring', 'JUNIOR',
 'Testcontainers는 테스트를 실행할 때 Docker로 실제 DB(MySQL 등)를 띄워 준다. H2 같은 대체 DB가 아니라 운영과 같은 DB로 SQL을 검증한다.',
'## 핵심
- Docker가 실행 중이어야 한다.
- Boot 3.1부터 `@ServiceConnection`을 붙이면 컨테이너 주소·계정이 데이터소스 설정에 자동으로 연결된다. 예전에는 `@DynamicPropertySource`로 직접 넣었다.
- 컨테이너를 `static`으로 두면 테스트 클래스 안에서 한 번만 띄운다. 여러 클래스가 공유하는 싱글톤 컨테이너로 만들면 더 빠르다.
- Flyway 마이그레이션도 테스트 시작 시 그대로 적용된다.

## 주의
- H2로 테스트하면 MySQL 전용 문법(ENUM, JSON, ON DUPLICATE KEY)에서 운영과 다르게 동작할 수 있다.
- 컨테이너 이미지는 운영 DB 버전과 맞춘다(`mysql:8.4`).
- 처음 실행할 때 이미지를 내려받아 오래 걸린다. CI에서는 캐시를 고려한다.',
'@SpringBootTest
@Testcontainers
class ConceptMapperTest {

    @Container
    @ServiceConnection                       // Boot 3.1+: 데이터소스 자동 연결
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired ConceptMapper conceptMapper;

    @Test
    void 공개된_카드만_검색한다() {
        List<Concept> result = conceptMapper.search("트랜잭션", null);
        assertThat(result).allMatch(c -> c.status() == Status.PUBLISHED);
    }
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/reference/testing/testcontainers.html'),

('Flyway DB 마이그레이션', 'spring', 'BEGINNER',
 'Flyway는 DB 스키마 변경을 버전이 붙은 SQL 파일로 관리하고, 애플리케이션 시작 시 아직 적용되지 않은 파일만 순서대로 실행한다. 모든 PC와 서버의 DB 구조를 같게 맞춘다.',
'## 핵심
- `V1__init.sql`, `V2__add_column.sql`: 버전 마이그레이션. 한 번만 실행된다.
- `R__seed.sql`: 반복 마이그레이션. 내용(체크섬)이 바뀔 때마다 다시 실행된다. 시드 데이터·뷰에 쓴다.
- 적용 이력은 `flyway_schema_history` 테이블에 체크섬과 함께 기록된다.
- 스프링 부트는 의존성만 있으면 시작 시 자동으로 마이그레이션한다. Flyway 10 이상에서 MySQL을 쓰려면 `flyway-mysql` 모듈도 추가한다.

## 주의
- **이미 적용된 V 파일을 고치면 체크섬이 달라 애플리케이션이 시작되지 않는다.** 수정할 내용은 새 버전 파일로 추가한다.
- 두 사람이 같은 버전 번호를 동시에 만들면 충돌한다. 새 파일을 만들기 전에 최신 번호를 확인한다.
- 운영 DB에서 `flyway clean`(전체 삭제)을 실행하지 않게 막는다(`spring.flyway.clean-disabled`, Boot 기본값 true).',
'src/main/resources/db/migration/
├─ V1__init.sql
├─ V2__analysis_soft_delete.sql
├─ V3__add_concept_tags.sql      ← 변경은 새 파일로
└─ R__seed_concepts.sql          ← 바뀌면 다시 실행

-- V3__add_concept_tags.sql
ALTER TABLE concepts ADD COLUMN tags VARCHAR(200) NULL;

# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-boot/how-to/data-initialization.html'),

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

## Boot 3에서 바뀐 점
- 패키지가 `javax.validation`에서 **`jakarta.validation`**으로 바뀌었다(Jakarta EE 9+). AI가 `import javax.validation.constraints.NotBlank;`를 쓰면 Boot 3에서 컴파일되지 않는다.
- 같은 이유로 `javax.servlet` → `jakarta.servlet`, `javax.persistence` → `jakarta.persistence`.

## 주의
- `@Valid`를 빼먹으면 어노테이션이 있어도 검증되지 않는다. AI 코드에서 자주 빠진다.
- 중첩 객체·리스트 안의 요소를 검증하려면 그 필드에도 `@Valid`를 붙인다.
- 입력 형식 검증은 DTO에서, "이미 가입된 이메일" 같은 비즈니스 규칙 검증은 Service에서 한다.',
'import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
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

## 응답 형식: ProblemDetail과 자체 형식
- **ProblemDetail**(Spring 6): HTTP API 에러 표준 RFC 9457 형식(`type`, `title`, `status`, `detail`, `instance`)을 만드는 클래스다. 필요한 필드는 `setProperty`로 더한다. 스프링이 던지는 기본 예외(검증 실패, 지원하지 않는 메서드 등)도 같은 형식으로 바꾸려면 `ResponseEntityExceptionHandler`를 상속하거나 `spring.mvc.problemdetails.enabled=true`를 쓴다.
- **자체 형식**: `{code, message, details}`처럼 팀이 정한 구조. 프론트엔드가 `code`로 분기하기 쉽다. 바이브노트가 이 방식이다.
- 어느 쪽이든 **모든 API가 같은 형식**을 쓰는 것이 핵심이다. 섞여 있으면 프론트엔드가 에러를 해석하지 못한다.

## 주의
500 응답에 예외 메시지나 스택 트레이스를 그대로 담으면 내부 정보가 노출된다. 로그에만 남기고 응답은 일반 메시지로 보낸다.',
'@RestControllerAdvice
public class GlobalExceptionHandler {

    // 자체 형식: {"code":"USER_404","message":"회원을 찾을 수 없습니다"}
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
}

// ProblemDetail 방식 (RFC 9457)
@ExceptionHandler(BusinessException.class)
public ProblemDetail handle(BusinessException e) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.getErrorCode().status(), e.getMessage());
    pd.setTitle("요청을 처리할 수 없습니다");
    pd.setProperty("code", e.getErrorCode().name());
    return pd;
}
// {"type":"about:blank","title":"요청을 처리할 수 없습니다","status":404,"detail":"...","code":"USER_404"}',
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

더 읽기: MDN 교차 출처 리소스 공유 https://developer.mozilla.org/ko/docs/Web/HTTP/Guides/CORS',
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
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Web/HTTP/Reference/Methods'),

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

('파일 업로드 (MultipartFile)', 'web', 'JUNIOR',
 '브라우저가 multipart/form-data로 보낸 파일은 스프링에서 MultipartFile로 받는다. 크기·형식 검증과 저장 경로 처리가 보안의 핵심이다.',
'## 핵심
- 컨트롤러 파라미터 `@RequestParam("file") MultipartFile file` 또는 `@RequestPart`로 받는다. 파일과 JSON을 함께 받을 때는 `@RequestPart`를 쓴다.
- 크기 제한: `spring.servlet.multipart.max-file-size`, `max-request-size`. 넘으면 `MaxUploadSizeExceededException`이 난다.
- `file.getOriginalFilename()`, `getContentType()`, `getSize()`, `transferTo(경로)`.

## 주의 (보안)
- **원래 파일 이름을 그대로 저장 경로에 쓰지 않는다.** `../../`가 들어 있으면 의도하지 않은 위치에 써진다(경로 조작). 저장 이름은 UUID로 만든다.
- 확장자와 `Content-Type`은 클라이언트가 바꿀 수 있다. 허용 목록(jpg·png·webp)과 파일 앞부분의 매직 바이트로 함께 확인한다.
- ZIP을 풀 때는 엔트리 경로가 대상 폴더 밖으로 나가는지 검사한다(Zip Slip).
- 업로드 폴더는 실행 권한이 없는 곳에 두고, 웹 루트에 바로 노출하지 않는다.',
'@PostMapping(value = "/api/challenges/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ImageResponse upload(@PathVariable long id, @RequestParam("file") MultipartFile file)
        throws IOException {
    if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) {
        throw new BusinessException(ErrorCode.INVALID_FILE);
    }
    String ext = imageValidator.verifiedExtension(file);   // 확장자 + MIME + 매직 바이트
    String savedName = UUID.randomUUID() + "." + ext;      // 원래 이름은 쓰지 않는다
    Path target = uploadDir.resolve(savedName).normalize();
    if (!target.startsWith(uploadDir)) {
        throw new BusinessException(ErrorCode.INVALID_FILE);
    }
    file.transferTo(target);
    return new ImageResponse("/uploads/" + savedName);
}

# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 3MB',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html'),

('쿠키와 세션', 'web', 'BEGINNER',
 'HTTP는 요청끼리 기억이 없으므로, 서버는 쿠키로 브라우저에 작은 값을 저장하게 하고 다음 요청마다 돌려받는다. 세션은 서버가 사용자 상태를 저장하고 그 식별자만 쿠키로 주고받는 방식이다.',
'## 핵심
- 서버가 `Set-Cookie` 헤더로 쿠키를 주면, 브라우저는 조건이 맞는 요청마다 `Cookie` 헤더로 자동 전송한다.
- 보안 속성:
  - `HttpOnly`: 자바스크립트에서 읽을 수 없다. 토큰 탈취(XSS)를 줄인다.
  - `Secure`: HTTPS에서만 전송한다.
  - `SameSite=Strict|Lax|None`: 다른 사이트에서 시작된 요청에 쿠키를 보낼지 정한다. CSRF를 줄인다.
  - `Max-Age`·`Expires`: 수명. `Path`: 전송할 경로 범위.
- 세션 방식은 서버 메모리·저장소에 상태가 있고, JWT 방식은 토큰 자체에 정보가 있다. Refresh 토큰을 HttpOnly 쿠키에 두는 혼합 방식도 많이 쓴다.

## 주의
- 토큰을 `localStorage`에 두면 XSS 한 번에 탈취된다. 민감한 토큰은 HttpOnly 쿠키가 낫다.
- 쿠키로 인증하면 CSRF 대책(SameSite, CSRF 토큰)이 필요하다.
- 프론트와 백엔드 출처가 다르면 `credentials: "include"`와 CORS `allowCredentials`가 함께 필요하다.

참고: MDN HTTP 쿠키 (링크만)',
'// Spring: Refresh 토큰을 HttpOnly 쿠키로 내려주기
ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api/auth")
        .maxAge(Duration.ofDays(14))
        .build();
return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new TokenResponse(accessToken));

// 응답 헤더
// Set-Cookie: refresh_token=...; Path=/api/auth; Max-Age=1209600; Secure; HttpOnly; SameSite=Strict',
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Web/HTTP/Guides/Cookies'),

('Fetch API', 'web', 'BEGINNER',
 'Fetch API는 브라우저에서 HTTP 요청을 보내는 표준 함수 fetch()다. Promise를 반환하므로 async/await와 함께 쓴다. axios 같은 라이브러리도 내부 개념은 같다.',
'## 핵심
- `fetch(url, { method, headers, body })` → `Response`. 본문은 `await res.json()`으로 꺼낸다.
- JSON을 보낼 때는 `Content-Type: application/json`과 `JSON.stringify(body)`.
- 쿠키를 함께 보내려면 `credentials: "include"`(다른 출처) 또는 `"same-origin"`.
- `AbortController`로 요청을 취소하거나 타임아웃을 건다.

## 주의
- **fetch는 404·500에서도 예외를 던지지 않는다.** 네트워크 실패일 때만 reject된다. `res.ok`를 직접 확인한다. axios는 2xx가 아니면 예외를 던지므로 둘의 동작이 다르다.
- 응답 본문은 한 번만 읽을 수 있다.
- 204 응답에 `res.json()`을 부르면 오류가 난다.

참고: MDN Fetch 사용하기 (링크만)',
'async function createPost(post) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10_000);
  try {
    const res = await fetch("/api/posts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(post),
      credentials: "same-origin",
      signal: controller.signal,
    });
    if (!res.ok) {
      const error = await res.json();          // {code, message}
      throw new Error(error.message);
    }
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}',
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Web/API/Fetch_API/Using_Fetch'),

('SSE (서버 전송 이벤트)', 'web', 'JUNIOR',
 'SSE(Server-Sent Events)는 서버가 하나의 HTTP 연결을 열어 둔 채 클라이언트에게 이벤트를 계속 밀어주는 방식이다. 진행률, 알림처럼 서버 → 클라이언트 단방향 실시간 갱신에 쓴다.',
'## 핵심
- 스프링 MVC는 `SseEmitter`를 반환해 SSE를 보낸다. 응답 타입은 `text/event-stream`.
- 브라우저는 `new EventSource(url)`로 받고, 연결이 끊기면 자동으로 다시 연결한다.
- 양방향 통신이 필요하면 WebSocket, 몇 초 지연이 괜찮으면 폴링(주기적 조회)이 더 단순하다.

## 비교
- 폴링: 구현이 가장 쉽다. 요청이 많고 지연이 있다.
- SSE: HTTP 그대로, 서버 → 클라이언트 단방향. 연결을 오래 유지한다.
- WebSocket: 양방향. 별도 프로토콜과 인프라 설정이 필요하다.

## 주의
- 연결마다 서버 자원을 잡는다. 타임아웃을 정하고, 완료·오류 시 `complete()`로 닫는다.
- 브라우저 `EventSource`는 커스텀 헤더(Authorization)를 넣을 수 없다. 쿠키 인증을 쓰거나 다른 방식을 고른다.
- 프록시(Nginx)가 응답을 버퍼링하면 이벤트가 늦게 도착한다.',
'@GetMapping(value = "/api/analyses/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable long id) {
    SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());
    progressPublisher.register(id, emitter);
    emitter.onCompletion(() -> progressPublisher.remove(id, emitter));
    emitter.onTimeout(emitter::complete);
    return emitter;
}

// 진행률이 바뀔 때
emitter.send(SseEmitter.event().name("progress").data(Map.of("status", "EXPLAINING", "progress", 60)));

// 브라우저
const source = new EventSource("/api/analyses/42/events");
source.addEventListener("progress", (e) => {
  const { status, progress } = JSON.parse(e.data);
});',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html'),

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

## Security 6(Boot 3)에서 바뀐 점
- `WebSecurityConfigurerAdapter`를 상속해 `configure(HttpSecurity)`를 오버라이드하는 방식은 **제거되었다**. `SecurityFilterChain`을 `@Bean`으로 등록한다.
- `authorizeRequests` → `authorizeHttpRequests`, `antMatchers`·`mvcMatchers` → `requestMatchers`.
- `.and()`로 잇는 체인 대신 람다 DSL(`csrf(csrf -> csrf.disable())`)을 쓴다. 예전 방식은 사용 중단되었다.
- AI가 Boot 2 시절 예제를 내면 위 세 가지가 섞여 컴파일 오류가 난다.

## 주의
- `permitAll` 경로 목록이 넓으면 보호해야 할 API가 열린다. 기본은 `authenticated()`로 두고 공개 경로만 연다.
- 규칙은 위에서부터 처음 맞는 것이 적용된다. `anyRequest()`는 마지막에 둔다.
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

('메서드 보안 (@PreAuthorize)', 'security', 'JUNIOR',
 '메서드 보안은 URL 규칙 대신 서비스·컨트롤러 메서드에 @PreAuthorize를 붙여 호출 직전에 권한을 검사한다. SpEL 식으로 역할뿐 아니라 파라미터 값까지 조건으로 쓸 수 있다.',
'## 핵심
- `@EnableMethodSecurity`로 켠다. (예전 `@EnableGlobalMethodSecurity`는 사용 중단되었다.)
- `@PreAuthorize("hasRole(''ADMIN'')")`: 관리자만 호출할 수 있다.
- `@PreAuthorize("#userId == authentication.principal.id")`: 파라미터와 로그인 사용자를 비교한다.
- 실패하면 `AccessDeniedException` → 403.

## URL 규칙과 비교
- URL 규칙(`requestMatchers("/api/admin/**").hasRole("ADMIN")`)은 한눈에 보이고 경로 단위로 막는다.
- 메서드 보안은 같은 서비스가 여러 경로에서 불려도 한곳에서 지킨다.
- 소유자 확인(내 글만 수정)은 DB를 조회해야 하므로 Service 안의 코드로 하는 편이 더 명확할 때가 많다.

## 주의
- 프록시 방식이라 같은 클래스 내부 호출에는 적용되지 않는다.
- SpEL 문자열 오타는 실행 시점에야 드러난다. 테스트로 확인한다.',
'@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig { }

@Service
public class AdminConceptService {

    @PreAuthorize("hasRole(''ADMIN'')")
    public void publish(long conceptId) {
        conceptMapper.updateStatus(conceptId, Status.PUBLISHED);
    }
}

@PreAuthorize("#userId == authentication.principal.id")
public ProfileResponse getMyProfile(long userId) { ... }',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html'),

('CSRF (사이트 간 요청 위조)', 'security', 'JUNIOR',
 'CSRF는 사용자가 로그인한 상태에서 공격자 사이트가 사용자 브라우저를 이용해 우리 서버로 원치 않는 요청(송금, 탈퇴)을 보내게 하는 공격이다. 브라우저가 쿠키를 자동으로 붙이기 때문에 생긴다.',
'## 핵심
- 공격 조건: 인증 정보가 **쿠키로 자동 전송**되고, 서버가 요청 출처를 확인하지 않을 때.
- 방어:
  - CSRF 토큰: 서버가 준 임의 값을 요청 헤더·폼에 담아 보내게 하고 검증한다. 스프링 시큐리티는 기본으로 켜져 있다.
  - `SameSite` 쿠키 속성(Lax·Strict): 다른 사이트에서 시작된 요청에 쿠키를 붙이지 않는다.
  - 상태를 바꾸는 작업은 GET으로 만들지 않는다.

## CSRF를 꺼도 되는 경우
- 인증을 `Authorization: Bearer` 헤더로만 하는 API(JWT를 메모리에 보관)는 브라우저가 자동으로 붙이지 않으므로 CSRF 위험이 낮아 보통 끈다.
- 단, Refresh 토큰처럼 **쿠키로 받는 엔드포인트가 있으면** 그 경로는 SameSite=Strict, 경로 제한(Path) 등으로 따로 지킨다.

## 주의
AI 예제는 습관처럼 `csrf.disable()`을 넣는다. 세션·쿠키 인증을 쓰는 서버에서 끄면 바로 취약해진다.',
'// JWT를 헤더로만 받는 API 서버
http.csrf(csrf -> csrf.disable())
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

// 세션·쿠키 인증 + SPA: 쿠키로 CSRF 토큰을 주고 헤더로 돌려받는다
http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
// 프론트: 요청마다 X-XSRF-TOKEN 헤더에 XSRF-TOKEN 쿠키 값을 담는다',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html'),

('OAuth2 소셜 로그인', 'security', 'SENIOR',
 'OAuth2 로그인은 구글·깃허브·카카오 같은 외부 서비스가 사용자를 인증하고, 우리 서버는 그 결과(사용자 정보)를 받아 로그인 처리하는 방식이다. 비밀번호를 우리 서버가 저장하지 않는다.',
'## 핵심
- 흐름(Authorization Code): 로그인 버튼 → 제공자 로그인·동의 화면 → 우리 서버 콜백으로 인가 코드 전달 → 서버가 코드를 토큰으로 교환 → 사용자 정보 조회 → 우리 회원과 연결.
- 스프링 시큐리티는 `spring-boot-starter-oauth2-client` + `spring.security.oauth2.client.registration.*` 설정 + `oauth2Login()`으로 이 흐름을 처리한다.
- 구글·깃허브 등은 제공자 정보가 내장되어 있다. 카카오·네이버는 `provider` 설정을 직접 적는다.
- 처음 로그인한 사용자는 `OAuth2UserService`를 구현해 우리 DB에 회원으로 저장(또는 기존 계정과 연결)한다.

## 주의
- client secret은 환경변수로 받는다. 저장소에 커밋하지 않는다.
- 제공자 콘솔에 등록한 Redirect URI와 서버 설정이 정확히 같아야 한다.
- SPA + JWT 구조라면 로그인 성공 핸들러에서 우리 토큰을 발급해 프론트로 넘기는 단계가 추가로 필요하다. 토큰을 URL 쿼리로 넘기면 기록에 남으므로 쿠키 등 다른 방법을 고려한다.
- 이메일이 같다는 이유만으로 기존 계정과 자동 연결하면 계정 탈취로 이어질 수 있다. 제공자의 이메일 검증 여부를 확인한다.',
'# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user, user:email

@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login/**").permitAll()
                .anyRequest().authenticated())
        .oauth2Login(oauth -> oauth
                .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                .successHandler(oauth2SuccessHandler));     // 여기서 우리 JWT 발급
    return http.build();
}',
 'PUBLISHED', 'SEED', 'https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html'),

('XSS (교차 사이트 스크립팅) 방어', 'security', 'JUNIOR',
 'XSS는 공격자가 게시글·댓글 같은 입력에 스크립트를 넣어, 다른 사용자의 브라우저에서 실행되게 하는 공격이다. 토큰·쿠키 탈취나 사용자 대신 요청 보내기로 이어진다.',
'## 핵심
- 저장형(DB에 저장된 글), 반사형(URL 파라미터가 그대로 출력), DOM 기반(프론트 코드가 위험하게 삽입)이 있다.
- 기본 방어는 **출력할 때 이스케이프**다. Vue의 `{{ }}`는 자동으로 이스케이프한다.
- 위험한 지점: Vue `v-html`, `innerHTML`, 마크다운을 HTML로 변환해 그대로 넣는 코드.
- HTML을 꼭 보여 줘야 하면 허용 태그만 남기는 sanitizer(예: DOMPurify)를 거친다.
- 추가 방어: 토큰을 HttpOnly 쿠키에 두기, Content-Security-Policy 헤더.

## AI 코드에서 보이는 모습
마크다운 미리보기·AI 설명 렌더링에서 `v-html="marked(text)"`처럼 변환 결과를 그대로 넣는 코드가 흔하다. LLM 출력도 신뢰할 수 없는 입력으로 다룬다.

## 주의
- 입력 시점에 태그를 지우는 것만으로는 부족하다. 출력 위치(HTML 본문, 속성, URL, 스크립트)마다 필요한 처리가 다르다.
- `href`에 사용자 입력을 넣으면 `javascript:` 주소가 실행될 수 있다.

참고: MDN 사이트 간 스크립팅 (링크만)',
'<!-- 안전: 자동 이스케이프 -->
<p>{{ comment.content }}</p>

<!-- 위험: 입력에 <img src=x onerror=alert(1)>이 있으면 실행된다 -->
<div v-html="comment.content"></div>

<!-- 마크다운을 보여 줘야 할 때 -->
<script setup>
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";

const props = defineProps({ markdown: String });
const safeHtml = computed(() => DOMPurify.sanitize(marked.parse(props.markdown ?? "")));
</script>

<template>
  <div v-html="safeHtml"></div>
</template>',
 'PUBLISHED', 'SEED', 'https://developer.mozilla.org/ko/docs/Glossary/Cross-site_scripting'),

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

## 버전 맞추기
- Spring Boot 3(Java 17+)에는 `mybatis-spring-boot-starter` **3.x**를 쓴다. 2.x는 Boot 2용이라 Boot 3에서 자동 구성이 동작하지 않는다.
- AI가 오래된 예제의 버전(2.x)을 그대로 넣는 경우가 많다. 의존성 버전을 먼저 확인한다.

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

('생성 키 받기 (useGeneratedKeys)', 'mybatis', 'BEGINNER',
 'AUTO_INCREMENT로 만든 새 행의 id는 INSERT 후에야 정해진다. MyBatis의 useGeneratedKeys와 keyProperty를 쓰면 INSERT 직후 파라미터 객체에 그 id가 채워진다.',
'## 핵심
- `<insert useGeneratedKeys="true" keyProperty="id">`: DB가 만든 키를 파라미터 객체의 `id` 필드에 넣는다.
- Mapper 메서드의 반환값(int)은 **영향받은 행 수**이지 id가 아니다. id는 파라미터 객체에서 꺼낸다.
- 키를 받을 객체에는 값을 넣을 수 있는 필드(setter나 가변 필드)가 필요하다.

## 주의
- record는 불변이라 keyProperty로 값을 채울 수 없다. INSERT용 파라미터는 가변 클래스로 두거나 `SELECT LAST_INSERT_ID()`를 따로 조회한다.
- 반환값을 id로 착각해 쓰는 실수가 AI 코드에서 흔하다(항상 1이 나온다).
- 여러 행 INSERT에서도 키를 받을 수 있지만 DB·드라이버에 따라 동작이 다르므로 확인한다.',
'public class PostInsert {            // 키를 받으려면 가변 객체
    private Long id;
    private final long userId;
    private final String title;
    // 생성자, getter, setId 생략
}

<insert id="insert" useGeneratedKeys="true" keyProperty="id">
  INSERT INTO posts (user_id, title) VALUES (#{userId}, #{title})
</insert>

PostInsert param = new PostInsert(userId, req.title());
int rows = postMapper.insert(param);   // rows = 1 (행 수)
long newId = param.getId();            // 생성된 id',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/sqlmap-xml.html'),

('페이징 쿼리 (LIMIT·OFFSET과 COUNT)', 'mybatis', 'JUNIOR',
 '목록 API는 한 번에 일부만 돌려준다. MySQL에서는 LIMIT·OFFSET으로 한 페이지를 가져오고, 같은 조건의 COUNT(*)로 전체 개수를 구해 페이지 정보를 만든다.',
'## 핵심
- `OFFSET = (page - 1) * size`. 페이지 번호 계산은 Java(DTO)에서 하고 SQL에는 값만 넘긴다.
- 목록 쿼리와 COUNT 쿼리는 **같은 WHERE 조건**을 써야 한다. `<sql>` 조각으로 공유하면 어긋나지 않는다.
- 정렬(`ORDER BY`)이 없으면 페이지마다 순서가 달라져 중복·누락이 생긴다. 고유 키(id)를 마지막 정렬 기준으로 넣는다.
- size에 상한(예: 100)을 둔다.

## 주의
- OFFSET이 크면(뒤쪽 페이지) DB가 앞의 행을 모두 읽고 버리므로 느려진다. 무한 스크롤처럼 끝없이 넘기는 화면은 "마지막으로 본 id 다음부터"(커서 방식)를 쓴다.
- 정렬 컬럼을 사용자 입력으로 받을 때 `${}`를 쓰지 말고 `<choose>`나 enum 화이트리스트로 고른다.',
'<sql id="searchWhere">
  <where>
    status = ''PUBLISHED''
    <if test="category != null">AND category = #{category}</if>
  </where>
</sql>

<select id="findPage" resultType="Concept">
  SELECT id, name, category, difficulty FROM concepts
  <include refid="searchWhere"/>
  ORDER BY created_at DESC, id DESC
  LIMIT #{size} OFFSET #{offset}
</select>

<select id="count" resultType="long">
  SELECT COUNT(*) FROM concepts
  <include refid="searchWhere"/>
</select>

// Service
long total = conceptMapper.count(query);
List<Concept> items = conceptMapper.findPage(query);   // query.offset() = (page - 1) * size
return new PageResponse<>(items, total, query.page(), query.size());',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/sqlmap-xml.html'),

('TypeHandler (JSON·커스텀 타입 매핑)', 'mybatis', 'SENIOR',
 'TypeHandler는 자바 타입과 DB 컬럼 값을 서로 어떻게 바꿀지 정하는 MyBatis 확장 지점이다. JSON 컬럼을 객체로 읽거나, 값 객체를 문자열로 저장할 때 쓴다.',
'## 핵심
- `BaseTypeHandler<T>`를 상속해 저장(`setNonNullParameter`)과 조회(`getNullableResult` 3종)를 구현한다.
- enum, java.time, 기본 타입은 MyBatis가 이미 처리한다. 직접 만들 일은 JSON·값 객체·암호화 컬럼 정도다.
- 사용 위치: resultMap의 `<result typeHandler="...">`, 파라미터 `#{field, typeHandler=...}`, 또는 패키지 등록(`mybatis.type-handlers-package`)으로 전역 적용.

## 주의
- 전역 등록하면 같은 자바 타입을 쓰는 모든 매핑에 적용된다. 범위가 넓으면 의도하지 않은 컬럼까지 바뀐다. 필요한 매핑에만 지정하는 편이 안전하다.
- 제네릭 타입(`List<FlowStep>`)은 실행 시점에 타입 정보가 지워지므로 배열 타입(`FlowStep[]`)이나 타입 정보를 넘기는 방식으로 처리한다.
- JSON 파싱 실패 처리를 빠뜨리면 조회 전체가 실패한다.',
'public class JsonTypeHandler<T> extends BaseTypeHandler<T> {
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

<result property="steps" column="steps" javaType="com.example.FlowStep[]"
        typeHandler="com.example.common.mybatis.JsonTypeHandler"/>',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/configuration.html#typeHandlers'),

('어노테이션 매퍼 (@Select·@Insert)', 'mybatis', 'BEGINNER',
 'MyBatis는 XML 대신 Mapper 인터페이스 메서드에 @Select·@Insert·@Update·@Delete로 SQL을 직접 적을 수도 있다. 짧은 쿼리는 파일 이동 없이 한눈에 보인다.',
'## 핵심
- `@Select("SELECT ... WHERE id = #{id}")`처럼 쓴다. 파라미터 규칙(`#{}`)은 XML과 같다.
- `@Options(useGeneratedKeys = true, keyProperty = "id")`로 생성 키를 받는다.
- 텍스트 블록(`"""`)을 쓰면 여러 줄 SQL도 읽기 쉽다.
- 같은 Mapper 안에서 어노테이션 메서드와 XML 메서드를 섞어 쓸 수 있다(같은 메서드를 양쪽에 정의하면 오류).

## 언제 XML을 쓰나
- 동적 SQL(`<if>`, `<foreach>`), 긴 조인, resultMap 중첩 매핑은 XML이 낫다. 어노테이션 안의 `<script>` 동적 SQL은 읽기 어렵다.
- 팀 규칙을 정해 두는 것이 중요하다. 예: "한 줄 쿼리만 어노테이션, 나머지는 XML".

## 주의
문자열 안 SQL은 IDE 검사가 약해 오타가 실행 시점에 드러난다. 매퍼 테스트로 확인한다.',
'@Mapper
public interface FollowMapper {

    @Select("SELECT COUNT(*) FROM follows WHERE following_id = #{userId}")
    long countFollowers(long userId);

    @Insert("INSERT INTO follows (follower_id, following_id) VALUES (#{followerId}, #{followingId})")
    int insert(@Param("followerId") long followerId, @Param("followingId") long followingId);

    @Select("""
            SELECT u.id, u.nickname
            FROM follows f JOIN users u ON u.id = f.follower_id
            WHERE f.following_id = #{userId}
            ORDER BY f.created_at DESC
            """)
    List<FollowerRow> findFollowers(long userId);
}',
 'PUBLISHED', 'SEED', 'https://mybatis.org/mybatis-3/ko/java-api.html'),

-- ───────────── vue ─────────────

('반응성 (ref·reactive)', 'vue', 'BEGINNER',
 'Vue의 반응성은 상태가 바뀌면 그 상태를 쓰는 화면이 자동으로 다시 그려지는 성질이다. Composition API에서는 ref와 reactive로 반응형 상태를 만든다.',
'## 핵심
- `ref(값)`: 어떤 타입이든 감싼다. 스크립트에서는 `.value`로 읽고 쓰고, 템플릿에서는 `.value` 없이 쓴다.
- `reactive(객체)`: 객체 자체를 반응형으로 만든다. 객체·배열에만 쓸 수 있다.
- 공식 문서는 기본 선택으로 `ref`를 권한다.

## Options API와 비교
- Vue 2 시절 방식인 Options API는 `data()`, `methods`, `computed` 객체 속성으로 컴포넌트를 나눠 쓰고 `this.count`로 접근한다.
- Vue 3의 `<script setup>` + Composition API는 `ref`·함수를 그냥 선언하고, 관련 로직을 한곳에 모으거나 컴포저블로 빼기 쉽다.
- AI가 두 방식을 한 컴포넌트에 섞어 내는 경우가 있다. 한 프로젝트 안에서는 한 방식으로 통일한다.

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
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/reactivity-fundamentals'),

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
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/computed'),

('props와 emit', 'vue', 'BEGINNER',
 '부모 컴포넌트는 props로 자식에게 데이터를 내려주고, 자식은 emit으로 이벤트를 올려 부모에게 알린다. 데이터는 아래로, 이벤트는 위로 흐른다.',
'## 핵심
- `defineProps`로 받을 값과 타입을 선언한다.
- `defineEmits`로 올릴 이벤트 이름을 선언하고, `emit("이름", 값)`으로 보낸다.
- 부모는 `<Child :item="x" @remove="onRemove" />`처럼 연결한다.

## 최신 문법 (Vue 3.5)
- `const { title, size = "md" } = defineProps({ ... })`처럼 props를 구조 분해해도 반응성이 유지되고, 기본값을 자바스크립트 문법으로 줄 수 있다.
- 단, 구조 분해한 값을 `watch`하려면 `watch(() => title, ...)`처럼 함수로 감싼다.
- 부모와 값을 양방향으로 묶는 경우(입력 컴포넌트)는 props + emit 대신 `defineModel`(3.4+)이 더 간단하다.

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
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/components/props'),

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
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/reusability/composables'),

('v-model과 defineModel', 'vue', 'BEGINNER',
 'v-model은 입력 값과 상태를 양방향으로 묶는다. Vue 3.4부터 자식 컴포넌트는 defineModel() 한 줄로 부모의 v-model을 받을 수 있다.',
'## 핵심
- 기본 요소: `<input v-model="keyword">`는 값 표시(`:value`)와 입력 이벤트(`@input`)를 한 번에 처리한다.
- 수식어: `v-model.trim`(앞뒤 공백 제거), `v-model.number`(숫자 변환), `v-model.lazy`(change 시점에 반영).
- 컴포넌트: 자식에서 `const model = defineModel()`을 선언하면, 부모의 `v-model` 값을 읽고 `model.value = ...`로 바꿀 수 있다.
- 여러 값을 묶으려면 이름을 붙인다: 부모 `v-model:title`, 자식 `defineModel("title")`.

## 최신 문법
3.4 이전에는 자식이 `modelValue` prop과 `update:modelValue` 이벤트를 직접 선언해야 했다. AI 코드에서 이 방식이 보이면 예전 문법이다(지금도 동작한다).

## 주의
- 같은 상태를 여러 컴포넌트가 v-model로 고치면 흐름을 추적하기 어렵다. 공유 상태는 스토어로 옮긴다.',
'<!-- SearchInput.vue -->
<script setup>
const keyword = defineModel({ type: String, default: "" });
</script>

<template>
  <input v-model.trim="keyword" placeholder="개념 검색" />
</template>

<!-- 부모 -->
<script setup>
import { ref } from "vue";
const keyword = ref("");
</script>

<template>
  <SearchInput v-model="keyword" />
  <p>검색어: {{ keyword }}</p>
</template>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/components/v-model'),

('v-if·v-for와 key', 'vue', 'BEGINNER',
 'v-if는 조건에 따라 요소를 만들거나 없애고, v-for는 배열을 반복해 요소를 그린다. v-for에는 항목을 구분하는 고유한 :key를 붙여야 Vue가 바뀐 부분만 정확히 갱신한다.',
'## 핵심
- `v-if` / `v-else-if` / `v-else`: 조건이 거짓이면 DOM에서 제거된다.
- `v-show`: CSS `display`만 바꾼다. 자주 토글하는 요소에 유리하다.
- `v-for="item in items" :key="item.id"`: key는 DB id처럼 바뀌지 않는 고유 값을 쓴다.

## 주의
- **배열 인덱스를 key로 쓰지 않는다.** 항목을 추가·삭제·정렬하면 인덱스가 바뀌어 입력 값이나 상태가 엉뚱한 항목에 붙는다.
- **같은 요소에 v-if와 v-for를 함께 쓰지 않는다.** Vue 3에서는 v-if가 먼저 평가되어 반복 변수를 쓸 수 없다(Vue 2와 반대). 걸러진 목록은 computed로 만들거나 `<template v-for>`로 감싼다.
- 목록이 비었을 때의 화면(빈 상태)을 `v-if`로 따로 보여 준다.',
'<script setup>
import { computed } from "vue";
const props = defineProps({ concepts: Array });
const published = computed(() => props.concepts.filter((c) => c.status === "PUBLISHED"));
</script>

<template>
  <p v-if="published.length === 0">아직 공개된 카드가 없습니다.</p>
  <ul v-else>
    <li v-for="concept in published" :key="concept.id">
      {{ concept.name }}
      <span v-show="concept.wished">★</span>
    </li>
  </ul>
</template>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/list'),

('생명주기 훅 (onMounted·onUnmounted)', 'vue', 'JUNIOR',
 '생명주기 훅은 컴포넌트가 화면에 붙을 때, 갱신될 때, 사라질 때 실행할 코드를 등록하는 함수다. 첫 데이터 로딩과 정리(cleanup)에 쓴다.',
'## 핵심
- `onMounted`: DOM이 그려진 뒤 실행된다. 첫 API 호출, DOM 측정, 외부 라이브러리 초기화.
- `onUnmounted`(`onBeforeUnmount`): 컴포넌트가 사라질 때 실행된다. 타이머, 이벤트 리스너, `EventSource`, 구독을 정리한다.
- `onUpdated`: 반응형 상태 변경으로 다시 그린 뒤 실행된다. 상태를 바꾸면 무한 갱신에 빠질 수 있어 드물게 쓴다.
- `<script setup>` 본문 자체가 예전의 `created` 시점이다.

## 주의
- `setInterval`, `window.addEventListener`를 등록하고 정리하지 않으면 페이지를 떠난 뒤에도 계속 실행된다(메모리 누수, 중복 요청).
- 훅은 setup 시점에 동기적으로 등록해야 한다. `await` 뒤에서 등록하면 연결되지 않는다.',
'<script setup>
import { ref, onMounted, onUnmounted } from "vue";

const props = defineProps({ analysisId: Number });
const status = ref(null);
let timer = null;

async function refresh() {
  status.value = await fetchAnalysisStatus(props.analysisId);
  if (status.value.status === "DONE" || status.value.status === "FAILED") {
    clearInterval(timer);
  }
}

onMounted(() => {
  refresh();
  timer = setInterval(refresh, 3000);   // 3초마다 진행률 조회
});

onUnmounted(() => clearInterval(timer)); // 화면을 떠나면 멈춘다
</script>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/lifecycle'),

('provide/inject', 'vue', 'JUNIOR',
 'provide/inject는 상위 컴포넌트가 제공한 값을 중간 컴포넌트를 거치지 않고 하위 컴포넌트가 바로 꺼내 쓰는 방법이다. props를 여러 단계로 계속 넘기는 문제(prop drilling)를 줄인다.',
'## 핵심
- 상위: `provide(키, 값)`. 하위: `inject(키, 기본값)`.
- ref를 provide하면 하위에서도 반응형으로 갱신된다.
- 키는 문자열보다 `Symbol`을 파일로 export해 쓰면 이름 충돌을 막는다.
- 앱 전체에는 `app.provide`로 제공한다.

## 언제 무엇을
- 부모-자식 한 단계: props/emit.
- 특정 컴포넌트 트리 안에서만 공유(탭 그룹, 폼 컨텍스트): provide/inject.
- 앱 전체에서 공유(로그인 사용자): Pinia 스토어.

## 주의
- 하위에서 값을 마음대로 바꾸면 흐름을 추적하기 어렵다. `readonly`로 제공하고, 바꾸는 함수를 함께 provide한다.
- 어디서 제공된 값인지 코드만 보고 찾기 어렵다. 남용하지 않는다.',
'// keys.js
export const FormKey = Symbol("form");

<!-- ConceptForm.vue (상위) -->
<script setup>
import { provide, reactive, readonly } from "vue";
import { FormKey } from "./keys";

const errors = reactive({});
function setError(field, message) { errors[field] = message; }
provide(FormKey, { errors: readonly(errors), setError });
</script>

<!-- FormField.vue (몇 단계 아래) -->
<script setup>
import { inject } from "vue";
import { FormKey } from "./keys";

const props = defineProps({ name: String });
const { errors } = inject(FormKey);
</script>

<template>
  <slot />
  <p v-if="errors[props.name]" class="error">{{ errors[props.name] }}</p>
</template>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/components/provide-inject'),

('템플릿 ref (useTemplateRef)', 'vue', 'JUNIOR',
 '템플릿 ref는 렌더링된 DOM 요소나 자식 컴포넌트 인스턴스에 직접 접근하는 방법이다. 포커스 주기, 스크롤, 차트 라이브러리 연결처럼 DOM을 직접 다뤄야 할 때 쓴다.',
'## 핵심
- 템플릿에 `ref="이름"`을 붙이고, 스크립트에서 `useTemplateRef("이름")`(Vue 3.5+)으로 가져온다.
- DOM은 마운트 뒤에 생기므로 `onMounted` 이후에 `.value`를 쓴다.
- 자식 컴포넌트는 `<script setup>`이 기본으로 닫혀 있어, 자식이 `defineExpose`로 공개한 것만 접근할 수 있다.

## 최신 문법
3.5 이전에는 `const input = ref(null)`처럼 템플릿의 ref 이름과 **같은 이름의 변수**를 선언하는 방식이었다. 지금도 동작하지만 이름이 우연히 같아서 연결되는 구조라 헷갈리기 쉽다.

## 주의
- 상태로 해결할 수 있는 일(보이기·숨기기, 값 바꾸기)을 DOM 직접 조작으로 하지 않는다. 반응성과 어긋난다.
- `v-if`로 사라진 요소의 ref는 `null`이다.',
'<script setup>
import { useTemplateRef, onMounted } from "vue";

const searchInput = useTemplateRef("search");

onMounted(() => {
  searchInput.value.focus();   // 화면이 열리면 검색창에 포커스
});
</script>

<template>
  <input ref="search" type="search" placeholder="개념 검색" />
</template>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/essentials/template-refs'),

('라우트와 동적 매칭 (Vue Router)', 'vue', 'BEGINNER',
 'Vue Router는 URL 경로와 화면 컴포넌트를 연결한다. /users/:id처럼 경로 일부를 파라미터로 받아 같은 화면에서 다른 데이터를 보여 주고, 화면 코드를 필요할 때 불러오도록(lazy loading) 나눈다.',
'## 핵심
- 라우트 정의: `{ path: "/concepts/:id", name: "concept-detail", component: ... }`.
- 컴포넌트에서 `useRoute().params.id`로 값을 읽는다. **값은 문자열**이다.
- 이동: `<RouterLink :to="{ name: ''concept-detail'', params: { id } }">` 또는 `router.push(...)`. 이름으로 이동하면 경로가 바뀌어도 코드를 고치지 않는다.
- 지연 로딩: `component: () => import("@/views/ConceptDetailView.vue")`. 첫 화면 로딩이 가벼워진다.
- 없는 경로: `{ path: "/:pathMatch(.*)*", component: NotFoundView }`.

## 주의
- `/concepts/1`에서 `/concepts/2`로 이동하면 **같은 컴포넌트가 재사용**되어 `onMounted`가 다시 실행되지 않는다. `watch(() => route.params.id, load)`로 다시 불러온다.
- 숫자로 쓸 때는 `Number(route.params.id)`로 바꾸고, 잘못된 값이면 404 화면으로 보낸다.',
'// router/index.js
const routes = [
  { path: "/", name: "home", component: HomeView },
  {
    path: "/concepts/:id",
    name: "concept-detail",
    component: () => import("@/views/ConceptDetailView.vue"),   // 지연 로딩
  },
  { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundView },
];

<!-- ConceptDetailView.vue -->
<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const concept = ref(null);

watch(
  () => route.params.id,
  async (id) => { concept.value = await fetchConcept(Number(id)); },
  { immediate: true },
);
</script>',
 'PUBLISHED', 'SEED', 'https://router.vuejs.org/guide/essentials/dynamic-matching.html'),

('슬롯 (slot)', 'vue', 'JUNIOR',
 '슬롯은 부모가 자식 컴포넌트 안의 정해진 자리에 원하는 템플릿을 끼워 넣게 하는 기능이다. 모달, 카드, 레이아웃처럼 틀은 같고 내용만 다른 컴포넌트를 만들 때 쓴다.',
'## 핵심
- 기본 슬롯: 자식의 `<slot />` 자리에 부모가 태그 사이에 넣은 내용이 들어간다. `<slot>기본 내용</slot>`으로 대체 내용을 둘 수 있다.
- 이름 있는 슬롯: 자식 `<slot name="footer" />`, 부모 `<template #footer>...</template>`.
- 범위 슬롯: 자식이 `<slot :item="item" />`처럼 데이터를 올려 주면, 부모가 `<template #default="{ item }">`로 받아 그리는 방식을 정한다. 목록 컴포넌트에서 항목 모양을 부모가 정할 때 쓴다.

## 주의
- props로 충분한 단순 텍스트까지 슬롯으로 받으면 사용법이 복잡해진다.
- 슬롯 내용은 부모의 범위에서 컴파일된다. 슬롯 안에서는 부모의 변수만 쓸 수 있고, 자식 데이터는 범위 슬롯으로 받아야 한다.',
'<!-- BaseCard.vue -->
<template>
  <section class="card">
    <header><slot name="header">제목 없음</slot></header>
    <slot />
    <footer v-if="$slots.footer"><slot name="footer" /></footer>
  </section>
</template>

<!-- DataList.vue: 범위 슬롯 -->
<template>
  <ul>
    <li v-for="item in items" :key="item.id"><slot :item="item" /></li>
  </ul>
</template>

<!-- 부모 -->
<BaseCard>
  <template #header>트랜잭션 전파</template>
  <p>REQUIRED와 REQUIRES_NEW의 차이</p>
  <template #footer><button>찜</button></template>
</BaseCard>

<DataList :items="concepts">
  <template #default="{ item }">{{ item.name }} · {{ item.difficulty }}</template>
</DataList>',
 'PUBLISHED', 'SEED', 'https://ko.vuejs.org/guide/components/slots'),

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
