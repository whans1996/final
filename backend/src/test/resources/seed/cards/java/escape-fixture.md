---
name: 이스케이프 검사 카드 (테스트 전용)
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/javase/tutorial/essential/regex/
---

## summary

It's 테스트 전용 카드다. 정규식 '\d+'와 경로 C:\temp가 그대로 들어가야 한다.

## body

- 작은따옴표: It's, 'quoted'
- 역슬래시: \d+ \\ \n \' 끝\

## example

```java
Pattern digits = Pattern.compile("\\d+");
String path = "C:\\temp\\It's";
```
