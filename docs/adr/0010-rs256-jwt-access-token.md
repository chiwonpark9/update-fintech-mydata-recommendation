# ADR-0010: RS256 JWT Access Token을 Spring Security Resource Server로 검증한다

- 상태: 승인
- 날짜: 2026-09-04

## 배경

Phase 2B에서 `partnerKey + email + password`를 MySQL로 검증하는 인증 기반을 만들었다. 이제 브라우저와 Embed 모듈이 매 요청마다 장기 자격 증명인 비밀번호를 다시 보내지 않고, 짧은 수명의 자격 증명으로 보호 API를 호출할 수 있어야 한다.

JWT는 Payload를 암호화하지 않으므로 개인정보를 최소화해야 한다. 또한 여러 제휴사의 사용자가 같은 API를 이용하므로 회원 ID뿐 아니라 제휴사 경계와 역할도 검증된 인증 정보에 포함해야 한다.

## 결정

- `POST /api/v1/auth/login`이 DB 인증 성공 결과로 Access Token을 발급한다.
- Access Token은 JWT이며 RS256으로 서명한다.
- 개인 키는 발급에만 사용하고 공개 키는 검증에 사용한다.
- Spring Security OAuth2 Resource Server가 Bearer Token을 추출하고 JWT 인증을 수행한다.
- Access Token 기본 수명은 15분이고 설정 범위는 1분에서 1시간으로 제한한다.
- `iss`, `aud`, `sub`, `iat`, `nbf`, `exp`, `jti` 표준 claim을 사용한다.
- `partner_id`, `partner_key`, `roles`를 프로젝트 claim으로 사용한다.
- 이메일, 표시 이름, 비밀번호와 해시는 JWT에 넣지 않는다.
- `kid`를 Header에 넣어 키 교체 가능성을 준비한다.
- RS256, JWT type, issuer, audience, 시간, 회원·제휴사·역할 claim을 모두 검증한다.
- `roles`를 Spring Security의 `ROLE_*` 권한으로 변환한다.
- 로그인과 `/me` 응답은 캐시하지 않고 세션을 만들지 않는다.
- 로그인 실패는 `AUTH_INVALID_CREDENTIALS`, Bearer 인증 실패는 `AUTH_AUTHENTICATION_REQUIRED`로 구분한다.
- 로컬 키는 Git에서 제외하고 AWS 키 전달 방식은 배포 단계에서 Secrets Manager 또는 Parameter Store를 기준으로 확정한다.
- Refresh Token은 이번 결정에서 제외한다.

## 이유

- 짧은 Access Token은 의도적으로 느린 BCrypt 검증을 모든 API 요청에서 반복하지 않게 한다.
- RS256은 검증 서비스에 공개 키만 전달할 수 있어 서명 권한의 확산을 줄인다.
- Resource Server를 사용하면 Bearer Token 필터, JWT 인증 Provider, SecurityContext 연결을 직접 다시 구현하지 않아도 된다.
- issuer와 audience를 함께 검증하면 다른 환경이나 다른 대상에 발급된 토큰의 오용을 줄인다.
- 제휴사 claim 검증은 토큰 단계에서 tenant 정보 누락을 조기에 거부한다.
- 표준 오류 계약을 유지하면 프론트엔드와 Embed SDK가 안정적인 코드로 실패를 처리할 수 있다.

## 대안

### HS256 대칭 키

설정은 간단하지만 발급과 검증에 같은 비밀키를 사용한다. 검증 주체가 늘어날수록 서명 권한도 함께 확산되므로 현재 목표 구조에는 RS256이 더 적합하다고 판단했다.

### 직접 구현한 JWT 필터

학습용으로 내부 동작을 이해할 수 있지만 Bearer Token 파싱, 실패 처리, SecurityContext 연결을 중복 구현하게 된다. Spring Security Resource Server의 검증 지점을 테스트하는 방식으로 선택했다.

### 서버 세션

즉시 폐기와 브라우저 관리에는 장점이 있지만 현재 API와 Embed 모듈의 stateless 방향을 변경한다. Refresh Token의 서버 저장으로 필요한 폐기 지점을 제공하는 방향을 유지한다.

### 불투명 Access Token과 introspection

중앙 폐기와 제어에 유리하지만 매 요청의 네트워크 조회와 별도 인증 서버가 필요하다. 현재 모듈형 모놀리스 단계에는 운영 복잡도가 크다.

### 긴 수명의 JWT 하나만 사용

구현은 단순하지만 탈취 시 피해 시간이 길고 정상적인 로그아웃과 회전이 어렵다. 짧은 Access Token과 서버에서 관리하는 Refresh Token으로 분리한다.

## 결과

### 장점

- DB 로그인 성공 이후 실제 보호 API를 호출할 수 있다.
- 공개 키만으로 서명과 표준 claim을 검증할 수 있다.
- 토큰의 제휴사와 역할이 Spring Security 권한으로 복원된다.
- 변조·만료·잘못된 issuer·audience·역할을 자동으로 거부한다.
- 비밀번호와 개인정보가 Access Token에 남지 않는다.

### 비용

- RSA 키 생성, 배포, 교체 절차가 필요하다.
- 서명된 Access Token은 만료 전 즉시 폐기하기 어렵다.
- 브라우저 저장과 Embed 교차 출처 정책은 별도 위협 모델이 필요하다.
- Refresh Token 없이 Access Token이 만료되면 다시 로그인해야 한다.

## 검증 방법

- 로그인 DTO의 정상·검증 실패·자격 증명 실패 응답을 테스트한다.
- RS256 Header와 최소 claim, 15분 만료를 decode해 확인한다.
- 잘못된 issuer, audience, 만료, 역할과 키 쌍을 거부한다.
- Testcontainers MySQL의 실제 회원으로 로그인해 Access Token을 발급한다.
- 발급 토큰으로 `/api/v1/auth/me`에 접근한다.
- 토큰 문자열을 변조해 401 Problem Details를 확인한다.
- 실제 실행 JAR과 로컬 MySQL에서도 같은 흐름을 확인한다.

## 재검토 조건

- 인증 발급과 업무 API를 별도 서비스로 분리할 때
- 무중단 키 교체와 JWK Set 공개가 필요할 때
- 외부 IdP, 소셜 로그인, 기업 SSO를 도입할 때
- Embed 브라우저 저장 방식과 CORS·CSRF 정책을 확정할 때
- 즉시 폐기가 필요한 고위험 API를 정의할 때
- Refresh Token 회전과 Redis 저장을 설계할 때
