# 데이터베이스 실행·설계 가이드

## 구성 목적

로컬 개발자와 자동화 테스트가 같은 MySQL 계열 환경을 사용하면서도 데이터를 서로 간섭하지 않게 한다. 현재 단계는 재현 가능한 연결과 스키마 관리에 집중하며 쿼리 성능 최적화는 합성 거래 데이터가 준비되는 Phase 6에서 직접 측정한다.

## 실행 구조

| 구분 | 개발 환경 | 테스트 환경 |
| --- | --- | --- |
| 실행 도구 | Docker Compose | Testcontainers |
| MySQL 이미지 | `mysql:8.4.11` | `mysql:8.4.11` |
| 생명주기 | 명시적으로 시작·중지 | 테스트가 자동 생성·제거 |
| 데이터 | 이름 있는 Docker 볼륨에 보존 | 테스트마다 격리 |
| 스키마 | Flyway 운영 마이그레이션 | 같은 Flyway 마이그레이션 |

개발용 MySQL은 컨테이너 내부 3306을 사용하고 호스트에는 3307로 노출한다. 이미 설치된 로컬 MySQL의 기본 포트 3306과 충돌하지 않기 위한 선택이다.

## 환경 변수와 비밀값

- `.env.example`은 필요한 변수의 이름과 형식만 Git에 기록한다.
- `.env`는 실제 로컬 비밀번호를 담으며 `.gitignore`로 제외한다.
- 애플리케이션 설정은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 필수로 요구한다.
- 운영 환경에서는 파일 대신 AWS Secrets Manager 등 외부 비밀 저장소를 사용할 예정이다.

```bash
cp .env.example .env
```

복사한 `.env`의 두 비밀번호 예시 값을 로컬 전용 값으로 변경한다.

## 실행과 종료

```bash
docker compose --env-file .env up -d --wait mysql
docker compose --env-file .env ps
docker compose --env-file .env stop mysql
```

`stop`은 컨테이너만 중지하므로 데이터 볼륨이 유지된다. `docker compose down -v`는 볼륨과 데이터를 삭제하므로 초기화가 명확히 필요한 경우에만 사용한다.

## Flyway 마이그레이션 규칙

애플리케이션 시작 시 Flyway가 `backend/src/main/resources/db/migration`을 읽고 아직 적용되지 않은 SQL을 버전 순서대로 실행한다.

- 파일 이름은 `V버전__설명.sql` 형식을 사용한다.
- 이미 공유되거나 적용된 마이그레이션은 수정하지 않는다.
- 변경이 필요하면 `V2`, `V3`처럼 새 파일을 추가한다.
- 애플리케이션 코드와 스키마 변경을 같은 커밋에서 검증한다.
- `flyway_schema_history`로 적용 버전과 성공 여부를 추적한다.

첫 마이그레이션은 `service_metadata` 테이블을 만들고 스키마가 Flyway로 초기화됐다는 확인 데이터를 저장한다.

## 검증 결과

1. Compose MySQL이 `healthy`가 된 뒤 Spring Boot가 연결됐다.
2. Flyway가 빈 스키마에 버전 1을 적용했다.
3. Testcontainers 통합 테스트가 초기 데이터를 실제 MySQL에서 조회했다.
4. 실행 중 MySQL을 중지하자 readiness가 HTTP 503을 반환했다.
5. MySQL 재시작 후 readiness가 HTTP 200으로 복구됐다.

이 단계는 DB 장애를 자동 복구한 것이 아니라, 트래픽을 받을 수 없는 상태를 운영 시스템이 탐지할 수 있게 만든 것이다.

## 문제 해결

### 3306 포트가 이미 사용 중인 경우

이 프로젝트의 기본 호스트 포트는 3307이다. 다른 프로그램이 3307도 사용한다면 `.env`의 `MYSQL_PORT`와 `DB_URL` 포트를 같은 값으로 변경한다.

### 컨테이너가 준비되지 않는 경우

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs mysql
```

상태 검사 결과와 MySQL 초기화 로그를 먼저 확인한다. 비밀번호나 실제 `.env` 내용은 이슈와 문서에 복사하지 않는다.
