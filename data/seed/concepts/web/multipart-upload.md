---
name: 파일 업로드 (MultipartFile)
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html
---

## summary

브라우저가 multipart/form-data로 보낸 파일은 스프링에서 MultipartFile로 받는다. 크기·형식 검증과 저장 경로 처리가 보안의 핵심이다.

## body

## 핵심
- 컨트롤러 파라미터 `@RequestParam("file") MultipartFile file` 또는 `@RequestPart`로 받는다. 파일과 JSON을 함께 받을 때는 `@RequestPart`를 쓴다.
- 크기 제한: `spring.servlet.multipart.max-file-size`, `max-request-size`. 넘으면 `MaxUploadSizeExceededException`이 난다.
- `file.getOriginalFilename()`, `getContentType()`, `getSize()`, `transferTo(경로)`.

## 주의 (보안)
- **원래 파일 이름을 그대로 저장 경로에 쓰지 않는다.** `../../`가 들어 있으면 의도하지 않은 위치에 써진다(경로 조작). 저장 이름은 UUID로 만든다.
- 확장자와 `Content-Type`은 클라이언트가 바꿀 수 있다. 허용 목록(jpg·png·webp)과 파일 앞부분의 매직 바이트로 함께 확인한다.
- ZIP을 풀 때는 엔트리 경로가 대상 폴더 밖으로 나가는지 검사한다(Zip Slip).
- 업로드 폴더는 실행 권한이 없는 곳에 두고, 웹 루트에 바로 노출하지 않는다.

## example

```java
@PostMapping(value = "/api/challenges/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
```

```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 3MB
```
