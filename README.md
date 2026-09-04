# MyData Card Recommendation

[![CI](https://github.com/chiwonpark9/update-fintech-mydata-recommendation/actions/workflows/ci.yml/badge.svg)](https://github.com/chiwonpark9/update-fintech-mydata-recommendation/actions/workflows/ci.yml)

마이데이터 기반 소비 분석을 이용해 사용자에게 적합한 카드를 추천하고, 추천 근거를 대화형으로 설명하는 B2B2C 금융 모듈 프로젝트입니다.

이 프로젝트는 하나의 독립 서비스인 동시에 제휴사 홈페이지에 삽입할 수 있는 컴포넌트 형태를 목표로 합니다.

## 해결하려는 문제

카드 상품은 혜택 조건이 복잡하고, 사용자는 자신의 실제 소비 패턴에 어떤 카드가 유리한지 판단하기 어렵습니다. 서비스는 사용자의 합성 마이데이터를 분석해 다음 질문에 답합니다.

- 나는 어디에 가장 많이 지출하는가?
- 내 소비 패턴과 잘 맞는 카드는 무엇인가?
- 왜 이 카드가 추천되었는가?
- 특정 생활 변화나 계획을 고려하면 어떤 카드가 적합한가?

## 핵심 모듈

1. **MyData Summary**: 로그인 사용자의 자산·카드·소비 요약
2. **Card Recommendation**: 소비 패턴과 카드 혜택을 비교한 설명 가능한 추천
3. **AI Advisor**: 채팅과 음성으로 상황을 파악하고 근거와 출처를 제시하는 상담
4. **Embed SDK**: 제휴사 서비스에 각 모듈을 삽입하기 위한 연동 인터페이스

## 기술 방향

| 영역 | 기술 | 적용 목적 |
| --- | --- | --- |
| Frontend | React 19.2.8, TypeScript 6.0.2, Vite 8.2.2 | 독립 화면과 삽입형 컴포넌트 구현 |
| Backend | Java 17, Spring Boot 4.1.1 | 도메인 API와 추천 로직 구현 |
| Security | Spring Security, JWT | 인증·인가와 제휴사별 접근 제어 |
| Database | MySQL 8.4.11, Flyway, Testcontainers | 관계형 데이터 관리와 스키마 변경 검증 |
| Cache | Redis | 소비 집계 캐시와 AI 요청 제한 |
| AI | RAG, Embedding | 카드 혜택과 약관에 근거한 상담 답변 |
| Infrastructure | Docker, AWS | 재현 가능한 실행 환경과 운영 배포 |
| CI/CD | GitHub Actions | 테스트·빌드·배포 자동화 |

기술은 한 번에 모두 도입하지 않습니다. 먼저 동작하는 수직 기능을 만든 후 측정 가능한 문제가 확인될 때 MySQL 최적화, Redis, RAG를 차례로 적용합니다.

## 설계 원칙

- 추천 결과와 추천 이유를 함께 제공한다.
- AI가 추천 점수를 임의로 결정하지 않도록 추천 엔진과 설명 역할을 분리한다.
- 실제 금융정보 대신 재현 가능한 합성 마이데이터를 사용한다.
- 제휴사별 데이터와 권한을 분리한다.
- 정상 동작뿐 아니라 장애·동시성·복구 시나리오를 테스트한다.
- 선택한 기술의 이유와 결과를 문서와 수치로 남긴다.

## 문서

- [제품 요구사항](docs/product-requirements.md)
- [아키텍처](docs/architecture.md)
- [개발 로드맵](docs/roadmap.md)
- [ADR-0001: 모듈형 모놀리스로 시작](docs/adr/0001-modular-monolith.md)
- [ADR-0002: 핵심 기술 스택](docs/adr/0002-technology-stack.md)
- [ADR-0003: Spring Boot 백엔드 기준](docs/adr/0003-spring-boot-baseline.md)
- [ADR-0004: React 프론트엔드 기준](docs/adr/0004-react-vite-baseline.md)
- [ADR-0005: MySQL 개발·테스트 환경](docs/adr/0005-mysql-flyway-testcontainers.md)
- [ADR-0006: RFC 9457 공통 오류 응답](docs/adr/0006-rfc9457-error-response.md)
- [ADR-0007: GitHub Actions CI](docs/adr/0007-github-actions-ci.md)
- [ADR-0008: Spring Security 요청 경계](docs/adr/0008-spring-security-baseline.md)
- [ADR-0009: 제휴사 범위를 포함한 DB 인증](docs/adr/0009-database-backed-partner-authentication.md)
- [ADR-0010: RS256 JWT Access Token](docs/adr/0010-rs256-jwt-access-token.md)
- [백엔드 실행 가이드](docs/backend-guide.md)
- [데이터베이스 실행·설계 가이드](docs/database-guide.md)
- [MySQL 기반 회원 인증](docs/database-authentication.md)
- [로그인 API와 JWT Access Token](docs/jwt-authentication.md)
- [공통 오류 응답 가이드](docs/error-response-guide.md)
- [Spring Security 기준선](docs/security-baseline.md)
- [프론트엔드 실행·구조 가이드](docs/frontend-guide.md)
- [CI 실행 가이드](docs/ci-guide.md)

## 현재 상태

`Phase 2 — 인증과 제휴사 경계 진행 중`

Phase 1의 실행 가능한 Spring Boot·React·MySQL 골격과 CI를 완료했습니다. Phase 2에서는 Spring Security 요청 경계, MySQL 제휴사·회원·역할 인증, 로그인 API와 RS256 JWT Access Token 발급·검증까지 연결했습니다. 토큰은 15분 동안 유효하며 회원 ID, 제휴사 경계와 역할만 포함합니다. 로그인부터 Bearer 보호 API, 만료·변조·잘못된 issuer·audience 검증을 포함한 전체 백엔드 테스트 41개가 통과했습니다. 다음 작업은 Refresh Token 회전·폐기와 제휴사별 업무 데이터 접근 제한입니다.

## 로컬 실행

필수 환경은 Java 17, Node.js 22.12 이상, Docker Desktop입니다. 별도의 Gradle 설치 없이 프로젝트에 포함된 Gradle Wrapper를 사용합니다.

최초 한 번 로컬 환경 파일을 만들고 비밀번호 예시 값을 자신만의 값으로 변경합니다. 실제 `.env`는 Git에 포함되지 않습니다.

```bash
cp .env.example .env
./scripts/generate-local-jwt-keys.sh
docker compose --env-file .env up -d --wait mysql
```

호스트의 기존 MySQL과 충돌하지 않도록 프로젝트 DB는 `localhost:3307`로 노출되고, 컨테이너 내부에서는 표준 포트 3306을 사용합니다.

```bash
cd backend
set -a
source ../.env
source ../.env.jwt.local
set +a
./gradlew test
./gradlew bootRun
```

서버 실행 후 다음 주소에서 상태를 확인할 수 있습니다.

| 용도 | 주소 |
| --- | --- |
| 애플리케이션 API 확인 | `GET http://localhost:8080/api/v1/health` |
| 운영 상태 확인 | `GET http://localhost:8080/actuator/health` |
| DB 포함 준비 상태 확인 | `GET http://localhost:8080/actuator/health/readiness` |

새 터미널에서 프론트엔드를 실행합니다.

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 열면 됩니다. 개발 중 `/api` 요청은 Vite 프록시를 통해 로컬 Spring 서버로 전달됩니다.

작업을 마치면 데이터 볼륨을 보존한 채 MySQL만 중지합니다.

```bash
docker compose --env-file .env stop mysql
```

## 기록 원칙

각 구현 단계는 다음 조건을 만족할 때 완료합니다.

- 코드가 로컬에서 재현 가능하게 실행된다.
- 필요한 테스트가 통과한다.
- 설계 선택과 시행착오가 문서에 반영된다.
- 적용 전후를 비교할 수 있는 근거가 남는다.
- 하나의 완결된 변경 단위로 Git 커밋한다.
