---
name: 비밀번호 해싱 (PasswordEncoder)
category: security
difficulty: BEGINNER
reference: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
---

## summary

비밀번호는 원문이나 되돌릴 수 있는 암호문으로 저장하지 않고, BCrypt 같은 느린 단방향 해시로 저장한다. 로그인 시에는 입력값을 같은 방식으로 비교한다.

## body

## 핵심
- `PasswordEncoder.encode()`로 저장하고 `matches(원문, 해시)`로 비교한다. 해시를 다시 원문으로 바꾸지 않는다.
- BCrypt는 매번 다른 salt를 섞으므로 같은 비밀번호도 해시값이 다르다. 그래서 문자열 `equals`로 비교하면 안 된다.
- 일부러 느리게 만들어 무차별 대입 공격 비용을 높인다.

## 주의
- MD5·SHA-256 한 번 적용은 너무 빨라서 비밀번호 저장에 부적합하다.
- 로그·응답·예외 메시지에 비밀번호가 찍히지 않게 한다.

## example

```java
@Service
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
}
```
