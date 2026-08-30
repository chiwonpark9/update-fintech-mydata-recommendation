# ADR-0002: 핵심 기술 스택과 도입 순서를 정한다

- 상태: 승인
- 날짜: 2026-08-30

## 배경

프로젝트는 기술적 깊이와 실제 실행 가능성을 모두 갖춰야 한다. 여러 기술을 이름만 나열하지 않고 문제, 선택, 검증 결과를 연결해야 한다.

## 결정

- Frontend: React, TypeScript, Vite
- Backend: Java, Spring Boot
- Security: Spring Security, JWT
- Primary Database: MySQL
- Cache: Redis
- AI: 추천 엔진과 분리된 RAG 기반 상담사
- Infrastructure: Docker, AWS
- Automation: GitHub Actions

## 도입 순서

1. React, Spring Boot, MySQL로 첫 수직 기능을 완성한다.
2. MySQL 기준 성능을 측정하고 쿼리와 인덱스를 개선한다.
3. 반복 비용이 큰 소비 집계에 Redis를 도입한다.
4. 추천 엔진을 완성한 뒤 RAG 기반 AI 설명을 추가한다.
5. 첫 수직 기능부터 AWS에 배포하고 단계적으로 운영 구성을 강화한다.

## 이유

- MySQL은 관계와 정합성이 중요한 금융 도메인 데이터를 표현하기 적합하다.
- Redis는 측정된 반복 조회와 요청 제한 문제에 한정해 사용할 수 있다.
- 추천 엔진과 AI를 분리하면 결과 재현성과 자연어 설명 능력을 함께 확보할 수 있다.
- Docker와 AWS를 사용하면 개발 환경의 재현성과 운영 경험을 함께 증명할 수 있다.

## 보류한 선택

- NestJS를 두 번째 운영 백엔드로 추가하지 않는다.
- 카드 검색 규모가 작을 때 Elasticsearch를 미리 도입하지 않는다.
- 검색 동기화 요구가 생기기 전에는 메시지 큐와 Outbox를 도입하지 않는다.
- 실제 사업 요구가 없는 결제·구독·정산 기능은 구현하지 않는다.

## 검증 방법

- 자동화 테스트와 실행 가능한 로컬 환경
- MySQL·Redis 적용 전후 부하 테스트
- RAG 검색 품질과 근거 정확성 평가
- AWS 배포, 관측, 장애 대응, 롤백 기록
- 단계별 ADR와 회고 문서
