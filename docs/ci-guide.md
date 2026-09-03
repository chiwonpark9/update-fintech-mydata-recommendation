# CI 실행 가이드

## 목적

GitHub에 변경 사항이 올라오면 로컬 확인 여부와 관계없이 같은 테스트와 빌드를 다시 실행한다. 문제가 있는 코드가 `main`에 합쳐진 뒤 발견되는 위험을 줄이고, 저장소만으로 프로젝트가 재현되는지 계속 확인한다.

워크플로 파일은 `.github/workflows/ci.yml`이다.

## 실행 시점

| 이벤트 | 실행 조건 | 목적 |
| --- | --- | --- |
| `push` | `main` 브랜치에 커밋이 올라올 때 | 기준 브랜치가 항상 빌드 가능한지 확인 |
| `pull_request` | `main` 대상 Pull Request가 생성·갱신될 때 | 병합 전에 문제 발견 |
| `workflow_dispatch` | GitHub Actions 화면에서 수동 실행 | 설정 변경이나 장애 조사 후 재검증 |

같은 브랜치에 새 커밋이 연속으로 올라오면 이전 실행은 취소하고 최신 커밋만 검사한다. 오래된 결과에 실행 시간을 낭비하지 않기 위해 `concurrency`와 `cancel-in-progress`를 사용한다.

## 작업 구조

백엔드와 프론트엔드는 서로 독립적인 job으로 분리한다. 두 job은 병렬로 실행되며, 한쪽이 실패해도 다른 쪽의 결과를 확인할 수 있다.

### Backend / Test and build

1. 저장소를 내려받는다.
2. Eclipse Temurin Java 17을 준비한다.
3. Gradle 의존성 캐시를 복원한다.
4. Gradle Wrapper로 테스트와 실행 JAR 생성을 검증한다.

```bash
cd backend
./gradlew clean test bootJar --no-daemon
```

전체 컨텍스트 테스트는 Testcontainers가 GitHub의 Linux Docker 환경에서 MySQL 8.4.11 컨테이너를 직접 실행한다. CI 전용 MySQL 비밀번호나 장기 실행 DB 서비스가 필요하지 않으며, Flyway 마이그레이션도 같은 테스트에서 검증한다.

### Frontend / Test, lint and build

1. 저장소를 내려받는다.
2. Node.js 22를 준비한다.
3. npm 다운로드 캐시를 복원한다.
4. lockfile과 정확히 일치하는 의존성을 설치한다.
5. 테스트, 코드 검사, 배포용 빌드를 각각 실행한다.

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

CI에서는 `npm install` 대신 `npm ci`를 사용한다. `package-lock.json`과 설치 결과가 다르면 실패하므로 개발자마다 의존성 버전이 달라지는 문제를 줄일 수 있다.

## 보안과 재현성

### 최소 권한

워크플로의 `GITHUB_TOKEN`에는 `contents: read`만 부여한다. 현재 CI는 소스 읽기와 검사만 수행하므로 코드 쓰기, 패키지 배포, AWS 권한이 필요하지 않다.

### 액션 SHA 고정

외부 액션은 이동 가능한 브랜치나 태그 대신 검증한 전체 커밋 SHA로 고정한다. 옆의 버전 주석은 사람이 릴리스를 식별하기 위한 정보다.

| 액션 | 고정 릴리스 | 역할 |
| --- | --- | --- |
| `actions/checkout` | `v7.0.1` | 저장소 체크아웃 |
| `actions/setup-java` | `v6.0.0` | Java 17 설치와 Gradle 캐시 |
| `actions/setup-node` | `v7.0.0` | Node.js 22 설치와 npm 캐시 |

SHA 고정은 자동 보안 패치가 반영되지 않는 비용이 있다. 액션을 갱신할 때는 공식 저장소의 최신 릴리스와 태그 SHA를 다시 확인하고, 로컬 검사와 GitHub CI를 모두 통과시킨다.

### 비밀값 사용 안 함

현재 CI에는 저장소 Secret이 없다. `.env`도 읽지 않는다. AWS 배포를 추가할 때는 장기 Access Key 대신 GitHub Actions OIDC와 짧은 수명의 AWS 권한을 별도 배포 workflow에 구성한다.

## 캐시와 산출물

- Gradle 캐시는 Wrapper와 빌드 설정이 바뀌면 새 키로 갱신된다.
- npm 캐시는 `package-lock.json` 변경을 기준으로 갱신된다.
- 캐시는 다운로드를 줄이지만 테스트를 생략하지 않는다.
- 현재 CI는 빌드 가능 여부만 검증하며 JAR과 프론트엔드 `dist`를 장기 보관하지 않는다.
- 배포 산출물은 AWS 배포 단계에서 Docker 이미지와 정적 파일로 다시 정의한다.

## 실패 로그 읽는 순서

1. GitHub 저장소의 **Actions** 탭에서 실패한 `CI` 실행을 연다.
2. `Backend / Test and build`와 `Frontend / Test, lint and build` 중 실패한 job을 확인한다.
3. 빨간색으로 표시된 첫 번째 step을 연다.
4. 로그의 마지막 문장만 보지 말고 최초의 컴파일 오류나 실패 테스트부터 읽는다.
5. 같은 명령을 로컬에서 재현하고 수정한다.
6. 수정 후 로컬 전체 검사를 통과시키고 새 커밋을 올린다.

CI 문제와 코드 문제를 구분해야 한다. 의존성 다운로드 장애처럼 외부 원인이 의심되면 수동 재실행으로 일시 장애인지 먼저 확인하고, 같은 지점에서 반복되면 workflow 설정과 버전을 조사한다.

## 완료 기준

- `main` push에서 두 job이 모두 성공한다.
- Pull Request용 실행 조건이 등록되어 있다.
- README의 CI 배지가 성공 상태를 표시한다.
- 로컬과 CI가 같은 테스트·빌드 명령을 사용한다.
- workflow에 비밀번호나 토큰이 직접 작성되어 있지 않다.

## 아직 하지 않은 것

- branch protection의 필수 상태 검사 지정
- 실패 알림 채널 연동
- 테스트 리포트와 빌드 산출물 업로드
- AWS 배포와 배포 승인 환경

저장소 사용자가 늘거나 Pull Request 중심 협업을 시작할 때 branch protection에서 두 CI job을 필수 검사로 지정한다. AWS 배포는 Phase 5에서 별도의 권한과 승인 경계를 가진 workflow로 추가한다.
