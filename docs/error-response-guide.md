# 공통 오류 응답 가이드

## 목적

각 컨트롤러가 서로 다른 오류 JSON을 만들지 않도록 Spring MVC의 예외를 한 곳에서 변환한다. HTTP 상태는 오류 범주를 나타내고, 서비스 오류 코드는 프론트엔드와 제휴사 모듈이 안정적으로 분기할 기준을 제공한다.

Spring Framework가 지원하는 RFC 9457 Problem Details를 기본 형식으로 사용하고 프로젝트에 필요한 필드만 확장한다.

## 응답 형식

```json
{
  "type": "urn:mydata-card-recommendation:problem:common-invalid-request",
  "title": "Invalid Request",
  "status": 400,
  "detail": "요청 값을 확인해주세요.",
  "instance": "/api/v1/example",
  "code": "COMMON_INVALID_REQUEST",
  "timestamp": "2026-09-03T15:30:00Z",
  "fieldErrors": [
    {
      "field": "name",
      "reason": "이름은 필수입니다."
    }
  ]
}
```

응답 Content-Type은 `application/problem+json`이다.

## 필드 의미

| 필드 | 표준 | 의미 |
| --- | --- | --- |
| `type` | RFC 9457 | 오류 종류를 식별하는 안정적인 URN |
| `title` | RFC 9457 | 오류 종류의 짧은 이름 |
| `status` | RFC 9457 | HTTP 상태 코드 |
| `detail` | RFC 9457 | 사용자에게 보여줄 수 있는 안전한 설명 |
| `instance` | RFC 9457 | 오류가 발생한 요청 경로 |
| `code` | 프로젝트 확장 | 클라이언트 분기와 문서 검색에 사용하는 서비스 코드 |
| `timestamp` | 프로젝트 확장 | UTC ISO-8601 발생 시각 |
| `fieldErrors` | 프로젝트 확장 | 입력 필드별 안전한 검증 사유 목록 |

클라이언트는 문구 변경이나 다국어 처리에 영향을 받지 않도록 `detail`이 아니라 `code`를 기준으로 동작해야 한다.

## 현재 오류 코드

| 코드 | HTTP | 사용 상황 |
| --- | ---: | --- |
| `COMMON_INVALID_REQUEST` | 400 | DTO 또는 쿼리 파라미터 검증 실패와 기타 잘못된 요청 |
| `COMMON_MALFORMED_JSON` | 400 | JSON 문법 또는 역직렬화 실패 |
| `COMMON_RESOURCE_NOT_FOUND` | 404 | 리소스 또는 API 경로를 찾을 수 없음 |
| `COMMON_METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `COMMON_UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 Content-Type |
| `COMMON_INTERNAL_SERVER_ERROR` | 500 | 예상하지 못한 서버 오류 |
| `AUTH_AUTHENTICATION_REQUIRED` | 401 | 인증 정보가 없거나 유효하지 않음 |
| `AUTH_ACCESS_DENIED` | 403 | 인증됐지만 요청에 필요한 권한이 없음 |

## 구현 역할

### `ApiErrorCode`

HTTP 상태, 서비스 코드, 제목, 안전한 설명을 한 곳에서 관리한다. 오류 응답 문구와 상태를 컨트롤러마다 중복하지 않는다.

### `ApiException`

애플리케이션이 예상하고 있는 오류를 전달한다. 외부 응답에는 등록된 `ApiErrorCode`의 안전한 정보만 사용한다.

### `GlobalExceptionHandler`

`ResponseEntityExceptionHandler`를 확장해 Spring MVC 예외와 애플리케이션 예외를 RFC 9457 형식으로 변환한다. 예상하지 못한 예외는 서버에 stack trace를 기록하지만 응답에는 일반적인 500 설명만 반환한다.

### `ApiProblemDetailFactory`

오류 코드, 요청 경로, 필드 오류를 받아 공통 Problem Details 본문을 만든다. Spring MVC와 Spring Security가 같은 형식을 사용하도록 생성 책임을 공유한다.

### `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`

Spring Security Filter Chain에서 발생한 인증 실패와 권한 부족을 각각 401과 403으로 변환한다. 보안 필터는 MVC보다 먼저 동작하므로 이 오류들은 `GlobalExceptionHandler`에서 처리할 수 없다.

### `SecurityErrorResponseWriter`

보안 실패 본문을 `application/problem+json`으로 직렬화한다. 내부 보안 예외의 메시지를 그대로 노출하지 않고 등록된 `ApiErrorCode`만 사용한다.

### `ApiFieldError`

검증에 실패한 필드 이름과 안전한 사유만 반환한다. 입력한 실제 값은 마이데이터나 개인정보가 될 수 있으므로 오류 응답에 포함하지 않는다.

## 새 업무 오류 추가 방법

1. 현재 공통 범주로 충분한지 먼저 확인한다.
2. 도메인 전용 코드가 필요하면 충돌하지 않는 이름과 HTTP 상태를 정의한다.
3. 서비스 계층에서 등록된 오류 코드를 가진 예외를 발생시킨다.
4. HTTP 상태, `code`, 안전한 `detail`을 테스트한다.
5. 이 문서의 오류 코드 표를 갱신한다.

인증은 `AUTH_` 접두어를 사용한다. 마이데이터 단계부터는 `MYDATA_`, 추천 단계부터는 `RECOMMENDATION_`처럼 도메인 접두어를 분리할 예정이다.

## 보안 원칙

- stack trace, 클래스 이름, SQL, 파일 경로를 HTTP 응답에 포함하지 않는다.
- 예상 밖 오류의 실제 `exception.getMessage()`를 클라이언트에 전달하지 않는다.
- 검증 실패 값 자체를 응답하거나 로그에 무조건 기록하지 않는다.
- 오류 응답에는 비밀번호, 토큰, 금융정보를 포함하지 않는다.
- 필요한 내부 정보는 접근이 통제되는 서버 로그에서만 확인한다.

## 검증 시나리오

- 여러 필드가 실패하면 정렬된 `fieldErrors`가 반환되는가
- 쿼리 파라미터 검증도 같은 오류 코드로 반환되는가
- 깨진 JSON의 파서 정보가 노출되지 않는가
- 404, 405, 415가 각각 안정적인 코드로 반환되는가
- 등록된 `ApiException`이 지정한 HTTP 상태로 변환되는가
- 예상 밖 예외가 일반적인 500 응답으로 가려지는가
- 정상 Health API 응답은 변경되지 않는가
- 인증 정보가 없으면 401과 `AUTH_AUTHENTICATION_REQUIRED`가 반환되는가
- 인증됐지만 권한이 부족하면 403과 `AUTH_ACCESS_DENIED`가 반환되는가
- 보안 오류에도 리다이렉트와 세션 쿠키가 생기지 않는가

Spring Security 적용 후 실제 서버에서 익명 보호 경로는 401, 공개 Health는 200으로 확인했다. 인증된 요청의 없는 경로가 기존 404 계약을 유지하는지는 MockMvc로 검증했다.
