# MySQL 기반 회원 인증

## 목적

JWT가 전달할 사용자 신원을 임의의 메모리 계정이 아니라 MySQL의 제휴사·회원·역할 데이터로 검증한다. 이 단계는 외부 로그인 API를 만들기 전의 내부 인증 기반이며, 운영용 기본 계정이나 평문 비밀번호를 포함하지 않는다.

## 데이터 모델

```text
partners 1 ───── N members 1 ───── N member_roles
   │                  │                    │
   │                  │                    └─ CUSTOMER
   │                  │                       PARTNER_ADMIN
   │                  │                       PLATFORM_ADMIN
   │                  └─ email, password_hash, status
   └─ partner_key, name, status
```

### `partners`

제휴사 데이터 경계의 기준이다.

| 필드 | 의미 |
| --- | --- |
| `id` | 내부 관계에 사용하는 숫자 식별자 |
| `partner_key` | 로그인과 Embed 연동에 사용할 공개 제휴사 키 |
| `name` | 제휴사 표시 이름 |
| `status` | `ACTIVE`, `SUSPENDED` |

### `members`

한 회원은 반드시 하나의 제휴사에 속한다.

| 필드 | 의미 |
| --- | --- |
| `partner_id` | 회원이 속한 제휴사 |
| `email` | 제휴사 내부 로그인 식별자 |
| `password_hash` | 알고리즘 식별자를 포함한 단방향 비밀번호 해시 |
| `display_name` | 화면에 표시할 이름 |
| `status` | `ACTIVE`, `LOCKED`, `WITHDRAWN` |
| `password_changed_at` | 토큰 강제 만료 정책에 사용할 비밀번호 변경 시각 |

`(partner_id, email)`에 유일 제약을 둔다. 같은 제휴사 안에서는 이메일을 중복할 수 없지만, 서로 다른 제휴사는 같은 이메일을 독립 회원으로 가질 수 있다. 기본 collation과 애플리케이션 정규화를 통해 이메일 대소문자를 구분하지 않는다.

### `member_roles`

회원 한 명이 여러 역할을 가질 수 있도록 별도 테이블로 분리했다.

| 역할 | 범위 |
| --- | --- |
| `CUSTOMER` | 자신의 마이데이터와 카드 추천 사용 |
| `PARTNER_ADMIN` | 소속 제휴사 운영 기능 |
| `PLATFORM_ADMIN` | 전체 플랫폼 운영 기능과 보호된 Actuator |

`PARTNER_ADMIN`과 `PLATFORM_ADMIN`을 분리해 제휴사 운영자가 플랫폼 전체 운영 엔드포인트에 접근하지 못하게 한다.

## 인증 흐름

```text
partnerKey + email + raw password
              ↓
PartnerEmailPasswordAuthenticationToken
              ↓
AuthenticationManager (ProviderManager)
              ↓
DatabaseMemberAuthenticationProvider
     ├─ 입력값 정규화
     ├─ JdbcMemberCredentialsRepository 조회
     ├─ PasswordEncoder.matches
     ├─ 제휴사·회원 상태와 역할 확인
     └─ MemberPrincipal + ROLE_* 권한 반환
```

Spring Security의 표준 사용자명 토큰을 그대로 사용하지 않고 제휴사 키를 명시적으로 포함한 인증 토큰과 Provider를 만들었다. 이메일만으로 회원을 조회한 뒤 제휴사를 나중에 검사하는 실수를 구조적으로 줄이기 위한 선택이다.

## 비밀번호 저장

`PasswordEncoderFactories.createDelegatingPasswordEncoder()`를 사용한다. 현재 신규 비밀번호는 BCrypt로 해시되어 `{bcrypt}` 식별자와 함께 저장된다.

- 단방향 변환이므로 해시에서 원문 비밀번호를 복호화할 수 없다.
- BCrypt가 매번 임의 salt를 사용하므로 같은 비밀번호도 서로 다른 해시가 된다.
- 로그인은 원문을 복호화하지 않고 `matches(raw, encoded)`로 비교한다.
- `{bcrypt}` 같은 알고리즘 식별자가 있으므로 앞으로 새 해시 정책을 도입해도 기존 값을 구분해 검증할 수 있다.
- 원문 비밀번호는 인증 결과와 Principal에 넣지 않고 요청 토큰에서도 사용 후 제거한다.

BCrypt 비용 값은 아직 기본값을 사용한다. AWS 실행 환경이 정해지면 로그인 지연과 CPU 사용량을 측정해 조정한다.

## 실패 정보 보호

다음 상황은 모두 `BadCredentialsException("Authentication failed")`로 통일한다.

- 제휴사가 존재하지 않음
- 회원이 존재하지 않음
- 비밀번호 불일치
- 회원이 잠김 또는 탈퇴 상태
- 제휴사가 중지 상태
- 회원에게 역할이 없음

외부 로그인 API에서도 계정 존재 여부를 추측할 수 없도록 같은 401 응답으로 변환할 예정이다. 존재하지 않는 회원도 BCrypt 비교를 한 번 수행하도록 dummy hash를 사용해 큰 시간 차이를 줄였다. 이것은 완전한 시간 동일성을 보장하는 장치가 아니며 로그인 요청 제한과 관측성은 후속 단계에서 추가한다.

## Flyway V2

`V2__create_partner_member_authentication.sql`이 세 테이블과 다음 무결성 규칙을 만든다.

- 제휴사 키 유일 제약
- 제휴사별 이메일 유일 제약
- 제휴사·회원·역할 상태 CHECK 제약
- 회원에서 제휴사로 향하는 외래 키
- 회원 삭제 시 역할을 함께 지우는 외래 키
- 비밀번호 해시의 대소문자를 보존하는 binary collation

운영 마이그레이션에는 제휴사, 회원, 비밀번호 샘플을 넣지 않는다. 합성 마이데이터와 데모 계정은 재현 가능한 별도 생성 과정으로 추가한다.

## 검증 결과

- Phase 2B 단독 검증 23개, Phase 2C JWT 연결 후 전체 백엔드 테스트 41개 통과
- Testcontainers MySQL 8.4에서 Flyway V1·V2 적용 성공
- 개발 DB를 기존 V1에서 V2로 실제 승격
- DB 회원의 제휴사 키·이메일·비밀번호 인증 성공
- 입력 대소문자와 앞뒤 공백 정규화 확인
- 다른 제휴사의 같은 이메일로 인증할 수 없음을 확인
- 잠긴 회원과 중지된 제휴사 인증 거부
- 같은 원문 비밀번호가 서로 다른 BCrypt 해시로 저장됨을 확인
- 같은 제휴사 내 대소문자만 다른 이메일 중복 차단
- 인증 성공 결과와 요청 토큰에서 원문 비밀번호 참조 제거
- Spring Boot 임시 사용자가 생성되지 않고 DB Provider가 전역 등록됨을 확인
- 실행 JAR 생성과 공개 Health 200 확인

## 현재 한계와 다음 단계

- 회원 생성·비밀번호 변경 API는 아직 없다.
- DB 인증은 로그인 HTTP API와 RS256 JWT Access Token 발급에 연결됐다.
- 로그인 시도 횟수 제한과 감사 로그가 아직 없다.
- BCrypt 비용 값은 운영 환경에서 측정하지 않았다.
- 제휴사별 실제 업무 데이터 조회 제한은 아직 없다.

로그인 DTO, 오류 계약과 Access Token 연결은 [로그인 API와 JWT Access Token](jwt-authentication.md)에 기록했다. Refresh Token의 저장·회전·재사용 탐지는 별도 단계로 분리한다.

## 참고 자료

- [Spring Security 인증 아키텍처](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/architecture.html)
- [Spring Security 비밀번호 저장](https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html)
- [Spring Framework JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)
