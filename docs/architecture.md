# 아키텍처

## 1. 시스템 컨텍스트

```text
최종 사용자 ── 제휴사 홈페이지 ── Embed SDK
                                │
                                ▼
                         Spring Boot API
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
                  MySQL       Redis      AI Provider
                    │                       │
                    └──── 카드·약관 문서 ───┘

제휴사 운영자 ── 운영 화면 ────────────────┘
```

## 2. 애플리케이션 구조

초기에는 배포 단위가 하나인 모듈형 모놀리스로 구성한다. 각 도메인은 코드와 의존성 경계를 분리하고, 실제 운영상의 필요가 확인되기 전에는 마이크로서비스로 나누지 않는다.

```text
backend
├── auth             인증과 토큰
├── tenant           제휴사와 접근 범위
├── member           사용자
├── mydata           합성 마이데이터와 소비 분석
├── card             카드 상품과 혜택
├── recommendation   추천 계산과 근거
├── advisor          AI 상담과 문서 검색
└── common           공통 오류·보안·관측성

frontend
├── app              독립 웹 애플리케이션
├── features         도메인별 UI
├── widgets          삽입 가능한 세 가지 모듈
├── sdk              제휴사 초기화·통신 인터페이스
└── shared           디자인 토큰과 공통 기능
```

## 3. 추천과 AI의 역할 분리

```text
소비 거래
  ↓
소비 프로필 생성
  ↓
결정적 추천 엔진 ── 점수·예상 혜택·추천 근거
  ↓
AI 상담사 ── 사용자 상황 해석·문서 검색·자연어 설명
```

- 추천 엔진이 카드 후보와 점수를 계산한다.
- AI 상담사는 카드 점수를 임의로 만들거나 변경하지 않는다.
- AI가 필요한 경우 카드 상품·약관 문서를 검색한다.
- 답변에는 근거 문서와 기준 시점을 표시한다.

## 4. 인증 구조

Spring Security는 요청을 가로채 인증·인가 규칙을 실행하는 보안 프레임워크다. JWT는 로그인 이후 사용자의 신원과 권한을 전달하는 토큰 형식이다.

현재 구현은 아래 목표 흐름 중 **MySQL 제휴사·회원·역할 조회와 비밀번호 검증**까지다. Health만 공개하고 나머지 요청은 인증을 요구하며, DB AuthenticationProvider가 `partnerKey + email + password`를 검증한다. 로그인 HTTP API와 JWT는 다음 구현 단계다.

```text
로그인 요청
  ↓
Spring Security가 계정과 비밀번호 검증
  ↓
Access Token + Refresh Token 발급
  ↓
후속 API 요청에 Access Token 전달
  ↓
JWT 검증 필터가 서명·만료·클레임 확인
  ↓
SecurityContext에 인증 정보 등록
  ↓
Spring Security가 URL·메서드 권한 확인
```

- 짧은 수명의 Access Token을 사용한다.
- Refresh Token은 회전과 폐기가 가능하도록 서버에서 관리한다.
- 브라우저 저장 방식과 CSRF 대응은 상세 위협 모델 작성 후 확정한다.
- Redis는 Access Token의 일반 저장소로 사용하지 않는다.
- 현재 서버 세션, 폼 로그인, HTTP Basic은 사용하지 않는다.
- 쿠키 기반 토큰이나 교차 출처 Embed 정책을 도입하기 전에 CSRF·CORS를 재검토한다.
- 회원 조회는 이메일만 사용하지 않고 제휴사 키를 함께 조건으로 사용한다.
- `PARTNER_ADMIN`과 `PLATFORM_ADMIN`을 분리해 제휴사 운영자가 플랫폼 운영 경로에 접근하지 못하게 한다.

## 5. 데이터 원칙

- MySQL이 사용자, 거래, 카드 상품, 추천 기록의 원본이다.
- Redis는 재생성할 수 있는 소비 집계 결과만 캐시한다.
- 합성 데이터는 Seed와 생성 규칙을 함께 버전 관리한다.
- 모든 핵심 테이블은 제휴사 데이터 경계를 확인할 수 있어야 한다.

## 6. 목표 AWS 구조

```text
Route 53 + ACM
       │
   CloudFront
    ├── S3: React
    └── ALB
         └── ECS Fargate: Spring Boot
              ├── RDS MySQL
              ├── ElastiCache Redis
              ├── Secrets Manager
              └── CloudWatch

GitHub Actions ── OIDC ── ECR / ECS
```

- 애플리케이션과 데이터 저장소는 분리한다.
- RDS와 Redis는 외부 인터넷에서 직접 접근하지 못하게 한다.
- 로그, 헬스 체크, 알람, 백업, 롤백 절차를 함께 구축한다.
- 세부 AWS 구성은 로컬 수직 기능이 완성된 뒤 ADR로 확정한다.

## 7. 검증 전략

- 단위 테스트: 추천 공식, 도메인 규칙, 토큰 로직
- 통합 테스트: MySQL, Redis, 보안 필터와 API
- 계약 테스트: Embed SDK와 API 요청·응답 규격
- E2E 테스트: 로그인부터 추천 확인까지의 주요 흐름
- 부하 테스트: 대용량 거래 조회와 캐시 적용 전후 비교
- 장애 테스트: Redis·AI Provider 장애와 재시도·fallback
