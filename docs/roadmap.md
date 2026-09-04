# 개발 로드맵

빠르게 기능 수를 늘리기보다, 각 단계에서 동작·테스트·문서·커밋을 완성한 뒤 다음 단계로 이동한다.

## Phase 0. 프로젝트 정의

### 단계 목표

- 요구사항과 구현 기술을 구분한다.
- 시스템 경계와 MVP 범위를 설명할 수 있다.
- ADR로 기술 선택 과정을 기록한다.

### 완료 조건

- [x] 제품 목표와 사용자를 정의한다.
- [x] MVP와 제외 범위를 구분한다.
- [x] 전체 아키텍처 초안을 작성한다.
- [x] 기술 적용 순서를 정한다.
- [x] 문서를 검토하고 첫 Git 커밋을 만든다.

### 커밋 시점

```text
docs: define product scope and initial architecture
```

## Phase 1. 실행 가능한 프로젝트 골격

### 단계 목표

- Spring Boot와 React의 실행 구조를 이해한다.
- Docker Compose로 MySQL을 재현 가능하게 실행한다.
- 환경별 설정과 비밀값을 분리한다.

### 구현

- [x] Spring Boot 프로젝트
- [x] React + TypeScript + Vite 프로젝트
- [x] MySQL Docker Compose
- [x] Health Check API
- [x] 공통 오류 응답
- [x] 백엔드 기본 테스트
- [x] CI

### 커밋 후보

```text
feat: bootstrap Spring Boot health API
feat: connect React frontend to backend health API
chore: add local mysql environment with docker compose
ci: verify frontend and backend on pull requests
```

## Phase 2. 인증과 제휴사 경계

### 단계 목표

- 인증과 인가의 차이를 코드와 HTTP 응답으로 설명한다.
- 사용자 신원과 제휴사 데이터 범위를 신뢰할 수 있게 전달한다.
- 토큰 탈취·재사용·만료 상황의 대응 기준을 만든다.

### 구현

- [x] Spring Security Filter Chain과 기본 보호 정책
- [x] 공개 Health와 관리자 Actuator 접근 규칙
- [x] Stateless 세션 정책
- [x] 401·403 RFC 9457 공통 오류 응답
- [x] 인증·인가 경계 테스트
- [x] MySQL 제휴사·사용자·역할 모델
- [x] BCrypt 비밀번호 해싱과 DB 자격 증명 인증
- [ ] 로그인 HTTP API와 요청 검증
- [ ] JWT Access Token 발급·검증
- [ ] Refresh Token 회전·폐기
- [ ] 제휴사별 데이터 접근 제한
- [ ] 실제 로그인부터 권한 확인까지 통합 테스트

### 커밋 후보

```text
feat: establish Spring Security baseline
feat: add database-backed member authentication
feat: issue and rotate JWT tokens
feat: enforce tenant-scoped authorization
```

## Phase 3. 합성 마이데이터

- 사용자·계좌·카드·거래 모델
- 재현 가능한 합성 데이터 생성기
- 월별·카테고리별 소비 분석
- 개인정보와 동의 상태 표현

## Phase 4. 설명 가능한 카드 추천

- 카드 혜택과 조건 모델
- 예상 혜택 계산
- 추천 점수와 근거
- 경계값과 회귀 테스트
- 추천 결과 화면

## Phase 5. 첫 AWS 배포

- Docker 이미지
- ECR, ECS Fargate, ALB
- RDS MySQL
- S3, CloudFront
- HTTPS, Secrets Manager, CloudWatch
- GitHub Actions OIDC 배포
- 롤백과 운영 절차

작은 기능이라도 먼저 배포해 운영 위험을 일찍 확인하고, 이후 단계마다 같은 환경을 갱신한다.

## Phase 6. MySQL 성능 실험

- 최대 100만 건 합성 거래
- EXPLAIN ANALYZE
- 복합 인덱스
- N+1 제거
- Offset과 Keyset Pagination 비교
- p95와 쿼리 수 기록

## Phase 7. Redis

- 소비 집계 Cache-Aside
- TTL과 무효화
- Cache Stampede 방지
- Redis 장애 fallback
- AI 요청 제한
- 적용 전후 부하 테스트

## Phase 8. AI 상담사

- 텍스트 상담 UI
- 사용자 발화의 구조화
- 추천 엔진 연동
- 안전한 프롬프트와 출력 검증
- AI 장애 시 일반 추천 유지

## Phase 9. RAG와 음성

- 카드 상품·약관 문서 파이프라인
- Chunking, Embedding, 검색
- 출처와 기준 시점 표시
- 검색 품질·환각·No-answer 평가
- 음성 입력과 출력

## Phase 10. Embed SDK와 운영 완성

- 세 가지 독립 위젯
- 제휴사 초기화 API
- Origin 검증과 테마 토큰
- 예제 제휴사 페이지
- 모니터링·알람·백업·복구 훈련
- 최종 README와 기술 성과 정리

## 단계별 공통 완료 기준

모든 Phase는 다음 질문에 답할 수 있어야 한다.

1. 어떤 문제를 해결했는가?
2. 왜 이 방법과 기술을 선택했는가?
3. 정상·실패·경계 상황을 어떻게 테스트했는가?
4. 적용 전후에 무엇이 달라졌는가?
5. 운영 중 문제가 생기면 어떻게 발견하고 복구하는가?
