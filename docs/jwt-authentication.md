# 로그인 API와 JWT Access Token

## 목적

MySQL에서 검증한 회원 신원을 짧은 수명의 서명된 Access Token으로 발급하고, 후속 API 요청에서 Spring Security가 토큰을 검증해 인증과 권한을 복원한다.

이 단계에서는 Refresh Token을 발급하지 않는다. Access Token 발급·검증과 Refresh Token의 저장·회전·재사용 탐지를 분리해 각 보안 책임을 독립적으로 검증하기 위해서다.

## 전체 흐름

```text
POST /api/v1/auth/login
  ↓
LoginRequest 검증
  ↓
AuthenticationManager
  ↓
DatabaseMemberAuthenticationProvider
  ├─ partnerKey + email로 MySQL 조회
  ├─ BCrypt 비밀번호 비교
  └─ 회원·제휴사 상태와 역할 확인
  ↓
JwtAccessTokenService
  └─ RS256 개인 키로 Access Token 서명
  ↓
Authorization: Bearer <access-token>
  ↓
Spring Security Resource Server
  ├─ RS256 공개 키로 서명 검증
  ├─ typ, iss, aud, exp, nbf 검증
  ├─ 회원·제휴사·역할 claim 검증
  └─ roles를 ROLE_* 권한으로 변환
  ↓
SecurityContext 인증 등록 → 보호 API
```

Spring Security의 Resource Server 기능을 사용해 Bearer Token 추출, JWT 검증, `SecurityContext` 등록을 표준 필터 체인에 맡긴다. 직접 만든 JWT 필터에서 인증 흐름을 다시 구현하지 않는다.

## API

### 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "partnerKey": "woori-card",
  "email": "user@example.com",
  "password": "correct-password"
}
```

입력 규칙은 다음과 같다.

| 필드 | 규칙 |
| --- | --- |
| `partnerKey` | 필수, 최대 64자, 영문·숫자로 시작하고 `_`, `-` 허용 |
| `email` | 필수, 이메일 형식, 최대 254자 |
| `password` | 필수, 8자 이상 128자 이하 |

개인정보와 비밀번호가 포함된 `LoginRequest`와 `LoginCommand`, 토큰을 가진 응답 객체는 자동 생성되는 `toString()`을 재정의해 이메일·비밀번호·Access Token 원문이 로그에 나타나지 않게 했다.

성공 응답:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresIn": 900,
  "expiresAt": "2026-09-04T14:01:06Z",
  "member": {
    "memberId": 1,
    "partnerId": 1,
    "partnerKey": "woori-card",
    "email": "user@example.com",
    "displayName": "테스트 사용자",
    "roles": ["CUSTOMER"]
  }
}
```

로그인 응답에는 `Cache-Control: no-store`, `Pragma: no-cache`를 설정하며 세션 쿠키를 만들지 않는다.

계정 없음, 잘못된 비밀번호, 잠긴 회원, 중지된 제휴사는 모두 같은 외부 오류로 변환한다.

```json
{
  "status": 401,
  "code": "AUTH_INVALID_CREDENTIALS",
  "detail": "로그인 정보가 올바르지 않습니다."
}
```

### 현재 인증 정보 확인

```http
GET /api/v1/auth/me
Authorization: Bearer <access-token>
```

```json
{
  "memberId": 1,
  "partnerId": 1,
  "partnerKey": "woori-card",
  "roles": ["CUSTOMER"]
}
```

이 API는 DB 사용자 상세 조회가 아니라, 검증된 Access Token으로 복원된 최소 인증 컨텍스트를 확인한다. 사용자 상세 정보와 마이데이터 요약은 별도 기능에서 DB 접근과 제휴사 범위를 다시 적용한다.

## Access Token 설계

서명 알고리즘은 RS256을 사용한다. 발급 서버는 개인 키로 서명하고 검증 측은 공개 키만 사용한다. 향후 검증 역할을 다른 서비스로 분리해도 개인 키를 공유하지 않아도 된다는 점을 HMAC 방식보다 우선했다.

기본 수명은 15분이며 설정 가능한 범위를 1분에서 1시간으로 제한한다.

| Header/Claim | 값과 목적 |
| --- | --- |
| `alg` | `RS256`, 허용 알고리즘 고정 |
| `typ` | `JWT`, 토큰 유형 확인 |
| `kid` | 서명 키 식별자, 향후 키 교체 준비 |
| `iss` | 환경별 발급자 URL |
| `aud` | 이 토큰을 사용할 클라이언트·API 범위 |
| `sub` | 회원 숫자 ID의 문자열 표현 |
| `iat`, `nbf`, `exp` | 발급·사용 시작·만료 시각 |
| `jti` | 토큰별 UUID |
| `partner_id`, `partner_key` | 제휴사 데이터 경계 |
| `roles` | `CUSTOMER`, `PARTNER_ADMIN`, `PLATFORM_ADMIN` |

이메일, 표시 이름, 비밀번호, 비밀번호 해시는 토큰에 넣지 않는다. JWT Payload는 암호화된 비밀 공간이 아니라 Base64URL로 인코딩된 데이터이므로 노출돼도 되는 최소 정보만 넣는다.

## 검증 규칙

`JwtDecoder`는 다음을 모두 통과시켜야 인증을 만든다.

- 서명 알고리즘이 RS256인가
- RSA 공개 키로 서명이 유효한가
- `typ`이 JWT인가
- `iss`가 서버 설정과 같은가
- `aud`에 요구한 대상이 포함되는가
- 현재 시간이 `nbf` 이후이고 `exp` 이전인가
- `sub`와 `partner_id`가 양수인가
- `partner_key`가 비어 있지 않고 64자 이하인가
- `roles`가 비어 있지 않고 허용된 역할만 포함하는가

토큰 없음, 만료, 변조, 잘못된 claim은 모두 `AUTH_AUTHENTICATION_REQUIRED` 401 Problem Details로 변환한다. 내부 JWT 오류 메시지와 토큰 값은 응답에 포함하지 않는다. 보호된 리소스의 401 응답에는 `WWW-Authenticate: Bearer`도 포함한다.

## 로컬 RSA 키 생성

실제 개인 키는 Git에 커밋하지 않는다. 프로젝트 루트에서 다음 스크립트를 최초 한 번 실행한다.

```bash
./scripts/generate-local-jwt-keys.sh
```

스크립트는 다음 로컬 전용 파일을 만든다.

- `.local/jwt/private.pem`: RS256 서명용 개인 키
- `.local/jwt/public.pem`: 검증용 공개 키
- `.env.jwt.local`: 애플리케이션이 읽을 Base64 DER 키와 JWT 설정

세 파일은 `.gitignore` 대상이며 `umask 077`로 다른 로컬 사용자가 읽지 못하게 생성한다. 이미 파일이 있으면 키를 자동으로 덮어쓰지 않는다.

서버 실행 시 DB 환경과 JWT 환경을 함께 불러온다.

```bash
cd backend
set -a
source ../.env
source ../.env.jwt.local
set +a
./gradlew bootRun
```

AWS에서는 개인 키를 이미지나 저장소에 포함하지 않고 Secrets Manager 또는 Parameter Store와 태스크 역할을 사용해 전달할 예정이다. 실제 도메인이 정해지면 `JWT_ISSUER`도 배포 주소로 변경한다.

## 테스트와 실제 실행 결과

- 전체 백엔드 테스트 41개 통과, 실패 0개
- 실행 JAR 생성 성공
- Testcontainers MySQL 8.4에서 DB 로그인부터 Bearer 보호 API까지 통합 검증
- RS256 Header와 `kid` 확인
- `iss`, `aud`, 만료, 허용 역할과 필수 제휴사 claim 검증
- 공개 키와 개인 키 불일치, 2048비트 미만 키 거부
- 로그인 DTO 검증과 이메일·비밀번호·Access Token `toString()` 마스킹 확인
- 잘못된 자격 증명은 토큰을 발급하지 않고 같은 401 반환
- 실제 로컬 MySQL과 실행 JAR에서 로그인 200, `/me` 200 확인
- 실제 Access Token 마지막 문자를 변경했을 때 `/me` 401 확인
- 로그인과 `/me` 응답에 캐시 방지 헤더, 로그인 응답에 세션 쿠키 없음 확인

실행 점검에 사용한 임시 회원과 제휴사 데이터는 확인 후 삭제했다.

## 현재 한계와 다음 단계

- 회원 생성·비밀번호 변경·계정 복구 API는 아직 없다.
- Access Token을 브라우저 어디에 보관할지는 Embed 위협 모델과 함께 확정해야 한다.
- Refresh Token 발급·저장·회전·재사용 탐지는 아직 없다.
- 로그아웃과 강제 토큰 폐기 정책이 아직 없다.
- 키 무중단 교체와 공개 JWK Set 엔드포인트는 아직 없다.
- 로그인 요청 제한과 감사 로그가 아직 없다.

다음 인증 단계에서는 Refresh Token을 Redis에 원문이 아닌 해시로 저장하고, 회전과 재사용 탐지, 로그아웃, 만료 정책을 구현한다. 그 전에 제휴사별 실제 업무 데이터 접근을 시작할 경우 모든 쿼리에 `partner_id` 범위를 강제한다.

## 참고 자료

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring Security Custom JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Spring Security JWT API](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/oauth2/jwt/package-summary.html)
- [Spring Security Bearer Token](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/bearer-tokens.html)
