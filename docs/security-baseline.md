# Spring Security 기준선

## 목적

JWT를 발급하기 전에 먼저 모든 HTTP 요청이 통과할 보안 경계를 만든다. 이 단계에서는 공개 경로를 최소화하고, 인증·인가 실패도 다른 API 오류와 같은 RFC 9457 계약으로 응답한다.

현재는 사용자 계정과 로그인 API가 아직 없으므로 토큰 인증을 구현하지 않았다. 따라서 공개 Health 경로를 제외한 실제 요청은 인증할 방법이 없으며 `401 Unauthorized`가 정상 동작이다.

## 현재 접근 규칙

규칙은 위에서 아래 순서로 평가한다.

| 요청 | 규칙 | 현재 결과 |
| --- | --- | --- |
| `GET /api/v1/health` | 공개 | 인증 없이 200 |
| `/actuator/health`, `/actuator/health/**` | 공개 | 로드 밸런서와 상태 검사에서 사용 |
| `/actuator/**` | `ADMIN` 역할 필요 | 미인증 401, 일반 사용자 403 |
| 그 외 모든 요청 | 인증 필요 | 현재 미인증 요청은 401 |

ERROR와 FORWARD 디스패치는 오류 처리 과정이 다시 인증에 막히지 않도록 허용한다. 공개 경로도 Security Filter Chain 자체를 우회시키지 않고 `permitAll`로 통과시켜 기본 보안 헤더를 유지한다.

## 요청 처리 흐름

```text
HTTP 요청
   ↓
SecurityFilterChain
   ├─ 공개 경로 ──────────────────────→ Controller
   ├─ 인증 정보 없음 ─→ AuthenticationEntryPoint ─→ 401 Problem Details
   └─ 인증됨
        ├─ 권한 부족 ─→ AccessDeniedHandler ──────→ 403 Problem Details
        └─ 권한 충족 ────────────────────────────→ Controller
```

보안 필터는 Spring MVC의 `DispatcherServlet`과 `@RestControllerAdvice`보다 먼저 동작한다. 따라서 인증·인가 오류는 `GlobalExceptionHandler`가 아니라 보안 전용 진입점과 처리기가 변환한다. 두 경로가 서로 다른 JSON을 만들지 않도록 `ApiProblemDetailFactory`를 공유한다.

## Spring Security와 JWT의 역할

- Spring Security는 요청 필터, 인증 정보 보관, URL·역할 기반 인가, 실패 처리 같은 전체 보안 실행 틀이다.
- JWT는 로그인 이후 사용자 식별자와 권한을 전달할 토큰 형식이다.
- 다음 단계에서 JWT 검증 필터가 토큰의 서명·만료·클레임을 확인하고 인증 객체를 `SecurityContext`에 넣는다.
- 그 이후의 접근 허용 여부와 401·403 처리는 지금 만든 Spring Security 경계가 담당한다.

둘을 단순히 섞는 것이 아니라, JWT가 인증 정보를 제공하고 Spring Security가 그 정보를 이용해 요청 전체의 인증·인가를 수행하는 관계다.

## 구현 구성요소

| 구성요소 | 책임 |
| --- | --- |
| `SecurityConfig` | 세션·로그인 방식과 URL 접근 규칙 정의 |
| `RestAuthenticationEntryPoint` | 인증되지 않은 요청을 401로 변환 |
| `RestAccessDeniedHandler` | 인증됐지만 권한이 부족한 요청을 403으로 변환 |
| `SecurityErrorResponseWriter` | 보안 실패를 `application/problem+json`으로 직렬화 |
| `ApiProblemDetailFactory` | MVC와 Security에서 동일한 오류 본문 생성 |
| `emptyUserDetailsService` | 계정 구현 전 Boot의 임시 기본 사용자 자동 생성을 막는 과도기 구성 |

## 상태 저장과 브라우저 보안 결정

서버 세션을 인증 상태 저장소로 사용하지 않도록 `SessionCreationPolicy.STATELESS`를 적용했다. 폼 로그인, HTTP Basic, 로그아웃 엔드포인트, 요청 캐시는 현재 API 구조에 필요하지 않아 비활성화했다.

CSRF는 **현재 단계의 전제** 아래 비활성화했다. 앞으로 Access Token을 쿠키가 아니라 `Authorization: Bearer ...` 헤더로 명시적으로 전달하는 API를 계획하고 있기 때문이다. 다만 Refresh Token을 쿠키로 전달하거나 제휴사 페이지의 교차 출처 삽입 구조가 확정되면 SameSite, Origin, CORS와 함께 위협 모델을 다시 작성하고 CSRF 보호 여부를 재결정한다.

CORS도 아직 전체 허용하지 않는다. Embed SDK 단계에서 등록된 제휴사 Origin만 허용하는 정책을 별도로 구현한다.

## 오류 계약

미인증 요청 예시:

```json
{
  "type": "urn:mydata-card-recommendation:problem:auth-authentication-required",
  "title": "Authentication Required",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/v1/missing",
  "code": "AUTH_AUTHENTICATION_REQUIRED",
  "timestamp": "2026-09-03T17:34:06.659293Z",
  "fieldErrors": []
}
```

권한 부족은 같은 구조로 `403`과 `AUTH_ACCESS_DENIED`를 반환한다. 비밀번호, 토큰 값, 내부 예외 메시지는 응답에 포함하지 않는다.

## 검증 결과

- 백엔드 전체 테스트 14개 통과
- 실제 MySQL 8.4 연결과 Flyway 스키마 검증 통과
- 실행 JAR 생성 성공
- 실제 서버에서 애플리케이션 Health와 Actuator Health가 인증 없이 200
- 실제 서버에서 일반 보호 API와 관리자 Actuator가 미인증 시 401
- MockMvc에서 인증된 일반 사용자의 관리자 Actuator 접근이 403
- 401·403 응답이 `application/problem+json`과 공통 확장 필드를 유지
- 공개 요청에도 `X-Content-Type-Options`, `X-Frame-Options`, 캐시 방지 헤더 적용 확인
- 세션 쿠키와 로그인 리다이렉트가 생성되지 않음을 확인

## 현재 한계와 다음 단계

- 실제 사용자 계정과 비밀번호 검증이 없다.
- JWT 발급·검증·갱신·폐기가 없다.
- 실제 인증으로 403을 재현하는 E2E 흐름은 아직 없다.
- 제휴사별 권한과 데이터 범위가 아직 없다.

다음 단계에서는 MySQL에 사용자와 역할 모델을 만들고 BCrypt로 비밀번호를 안전하게 저장한 뒤 로그인 인증을 구현한다. 그 다음 JWT Access·Refresh Token을 연결한다.

## 참고 자료

- [Spring Security 요청 인가](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Security 세션 관리](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security Servlet 아키텍처](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html)
