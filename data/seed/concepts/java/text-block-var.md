---
name: 텍스트 블록과 var
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/en/java/javase/17/language/text-blocks.html
---

## summary

텍스트 블록(Java 15)은 큰따옴표 세 개로 여러 줄 문자열을 그대로 쓰는 문법이고, var(Java 10)는 지역 변수 타입을 컴파일러가 추론하게 하는 키워드다.

## body

## 핵심
- 텍스트 블록: `"""` 다음 줄부터 내용을 쓴다. 줄바꿈과 따옴표를 이스케이프하지 않아도 되어 JSON·SQL·프롬프트 문자열이 읽기 쉬워진다. 공통 들여쓰기는 자동으로 제거된다.
- `formatted(...)`로 값을 채운다.
- var: `var list = new ArrayList<String>();`처럼 오른쪽에서 타입이 분명할 때 쓴다. 지역 변수에만 쓸 수 있다.

## AI 코드에서 보이는 모습
테스트의 기대 JSON, LLM 프롬프트 템플릿, 긴 SQL 문자열에 텍스트 블록이 자주 쓰인다.

## 주의
- var는 타입이 사라지는 것이 아니라 컴파일 시점에 정해진다. 오른쪽만 봐서 타입이 안 보이면(`var result = service.process();`) 읽는 사람이 헷갈리므로 쓰지 않는다.
- 필드, 메서드 파라미터, 반환 타입에는 var를 쓸 수 없다.

## example

```java
String body = """
        {
          "email": "%s",
          "nickname": "vibecoder"
        }
        """.formatted(email);
```

```java
mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
```

```java
var users = new ArrayList<User>();       // 타입이 오른쪽에 드러난다
for (var entry : countByCategory.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}
```
