# 백엔드 실행 가이드

## 기술 기준

- Java 17
- Spring Boot 4.1.1
- Gradle 9.7.1 Wrapper
- Spring Web MVC
- Spring Security
- Spring Boot Actuator
- Jakarta Validation
- Spring JDBC와 HikariCP
- MySQL 8.4.11
- Flyway
- Testcontainers

## 프로젝트 구조

```text
backend/
├── build.gradle
├── gradlew
├── gradle/wrapper
└── src
    ├── main
    │   ├── java/com/chiwonpark9/cardrecommendation
    │   │   ├── CardRecommendationApplication.java
    │   │   ├── auth
    │   │   │   ├── application/port/MemberCredentialsRepository.java
    │   │   │   ├── config
    │   │   │   │   ├── AuthenticationConfig.java
    │   │   │   │   └── SecurityConfig.java
    │   │   │   ├── domain
    │   │   │   │   ├── MemberCredentials.java
    │   │   │   │   ├── MemberRole.java
    │   │   │   │   └── MemberStatus.java
    │   │   │   ├── infrastructure/JdbcMemberCredentialsRepository.java
    │   │   │   └── security
    │   │   │       ├── DatabaseMemberAuthenticationProvider.java
    │   │   │       ├── MemberPrincipal.java
    │   │   │       ├── PartnerEmailPasswordAuthenticationToken.java
    │   │   │       ├── RestAuthenticationEntryPoint.java
    │   │   │       ├── RestAccessDeniedHandler.java
    │   │   │       └── SecurityErrorResponseWriter.java
    │   │   ├── common/error
    │   │   │   ├── ApiErrorCode.java
    │   │   │   ├── ApiException.java
    │   │   │   ├── ApiFieldError.java
    │   │   │   ├── ApiProblemDetailFactory.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── tenant/domain/PartnerStatus.java
    │   │   └── system/api
    │   │       ├── HealthController.java
    │   │       └── HealthResponse.java
    │   └── resources
    │       ├── application.yml
    │       └── db/migration
    │           ├── V1__initialize_service_metadata.sql
    │           └── V2__create_partner_member_authentication.sql
    └── test
        └── java/com/chiwonpark9/cardrecommendation
            ├── CardRecommendationApplicationTests.java
            ├── auth/config/SecurityConfigTest.java
            ├── auth/security/DatabaseMemberAuthenticationProviderTest.java
            ├── common/error/GlobalExceptionHandlerTest.java
            └── system/api/HealthControllerTest.java
```

## 테스트

프로젝트에 포함된 Gradle Wrapper를 사용하므로 Gradle을 별도로 설치할 필요가 없다.

```bash
cd backend
./gradlew test
```

전체 테스트를 실행하려면 Docker Desktop이 실행 중이어야 한다. Testcontainers는 테스트 전용 MySQL을 만들고 테스트가 끝나면 제거한다.

현재 전체 테스트 23개는 다음을 검증한다.

- 실제 MySQL에서 Spring 애플리케이션 컨텍스트가 생성되는가
- Flyway 마이그레이션이 실행되고 초기 데이터가 조회되는가
- Health Controller가 HTTP 200과 약속한 JSON을 반환하는가
- 검증 실패, 잘못된 JSON, 404, 405, 415와 예상 밖 오류가 같은 계약으로 반환되는가
- 내부 예외 정보가 HTTP 500 응답에 노출되지 않는가
- 공개 Health는 인증 없이 접근할 수 있는가
- 미인증 보호 요청은 401 공통 오류를 반환하는가
- 인증된 일반 사용자의 관리자 경로 접근은 403 공통 오류를 반환하는가
- 보안 실패가 세션 쿠키나 로그인 리다이렉트를 만들지 않는가
- DB 회원이 제휴사 범위와 BCrypt 비밀번호로 인증되는가
- 다른 제휴사, 잠긴 회원, 중지된 제휴사가 인증되지 않는가
- 같은 제휴사의 이메일 중복을 DB가 차단하는가
- 인증 이후 원문 비밀번호 참조가 제거되는가

## 빌드 산출물 위치

일반 환경과 CI에서는 기본 `backend/build`를 사용한다. macOS에서 프로젝트가 `/Volumes/` 아래의 외장 드라이브에 있으면 AppleDouble 보조 파일이 클래스 스캔을 방해할 수 있어, Gradle이 사용자 내부 캐시의 `<프로젝트명>/backend-build`로 산출물을 자동 분리한다.

필요하면 `BACKEND_BUILD_DIR` 환경 변수로 다른 생성물 경로를 지정할 수 있다. 소스와 Git 이력에는 영향을 주지 않는다.

## 서버 실행

프로젝트 루트에서 환경 파일과 MySQL을 먼저 준비한다.

```bash
cp .env.example .env
docker compose --env-file .env up -d --wait mysql
```

그다음 같은 환경 변수를 Spring Boot에 전달해 실행한다.

```bash
cd backend
set -a
source ../.env
set +a
./gradlew bootRun
```

기본 포트는 8080이며 환경변수로 변경할 수 있다.

```bash
SERVER_PORT=8081 ./gradlew bootRun
```

DB 기반 회원 인증은 구현됐지만 로그인 HTTP API와 JWT는 아직 없다. 따라서 Health 외 요청에 사용할 외부 인증 수단은 없으며, 다음 단계에서 인증 성공 결과를 Access Token으로 연결한다. Spring Boot 임시 사용자는 생성되지 않는다.

## API

### 애플리케이션 Health API

```http
GET /api/v1/health
```

```json
{
  "status": "UP",
  "service": "mydata-card-recommendation-api"
}
```

이 API는 프론트엔드와 백엔드가 정상적으로 통신하는지 확인하는 애플리케이션 계약이다.

## 보안 접근 규칙

| 경로 | 접근 조건 |
| --- | --- |
| `/api/v1/health` | 공개 |
| `/actuator/health`, `/actuator/health/**` | 공개 |
| `/actuator/**` | `PLATFORM_ADMIN` 역할 |
| 나머지 모든 요청 | 인증 필요 |

세션, 폼 로그인, HTTP Basic은 사용하지 않는다. 미인증 요청은 로그인 화면으로 이동하지 않고 401 Problem Details를 반환하며, 인증됐지만 권한이 부족한 요청은 403을 반환한다. 자세한 설계와 현재 제한은 [Spring Security 기준선](security-baseline.md)을 참고한다.

### Actuator Health API

```http
GET /actuator/health
```

Actuator Health는 로드 밸런서와 모니터링 시스템이 애플리케이션의 운영 상태를 확인하기 위한 엔드포인트다. 세부 내부 정보는 외부에 노출하지 않는다.

## 공통 오류 응답

API 오류는 `application/problem+json`으로 반환한다. RFC 9457 표준 필드에 서비스 오류 코드, 발생 시각, 필드별 검증 사유를 추가했다.

```json
{
  "type": "urn:mydata-card-recommendation:problem:common-resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "요청한 리소스를 찾을 수 없습니다.",
  "instance": "/api/v1/missing",
  "code": "COMMON_RESOURCE_NOT_FOUND",
  "timestamp": "2026-09-03T15:33:30.860763Z",
  "fieldErrors": []
}
```

클라이언트는 변경될 수 있는 `detail` 문장보다 안정적인 `code`를 기준으로 분기한다. 전체 규칙과 확장 방법은 [공통 오류 응답 가이드](error-response-guide.md)를 따른다.

### Readiness API

```http
GET /actuator/health/readiness
```

DB 연결을 포함해 현재 요청을 처리할 준비가 됐는지 판단한다. 실제 검증에서 MySQL 중지 시 HTTP 503, 재시작 후 HTTP 200으로 복구됐다. 향후 AWS의 로드 밸런서와 컨테이너 상태 검사에 연결할 수 있다.
