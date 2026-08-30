# 백엔드 실행 가이드

## 기술 기준

- Java 17
- Spring Boot 4.1.1
- Gradle 9.7.1 Wrapper
- Spring Web MVC
- Spring Boot Actuator
- Jakarta Validation

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
    │   │   └── system/api
    │   │       ├── HealthController.java
    │   │       └── HealthResponse.java
    │   └── resources/application.yml
    └── test
        └── java/com/chiwonpark9/cardrecommendation
            ├── CardRecommendationApplicationTests.java
            └── system/api/HealthControllerTest.java
```

## 테스트

프로젝트에 포함된 Gradle Wrapper를 사용하므로 Gradle을 별도로 설치할 필요가 없다.

```bash
cd backend
./gradlew test
```

현재 테스트는 다음을 검증한다.

- Spring 애플리케이션 컨텍스트가 정상적으로 생성되는가
- Health Controller가 HTTP 200과 약속한 JSON을 반환하는가

## 서버 실행

```bash
cd backend
./gradlew bootRun
```

기본 포트는 8080이며 환경변수로 변경할 수 있다.

```bash
SERVER_PORT=8081 ./gradlew bootRun
```

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

### Actuator Health API

```http
GET /actuator/health
```

Actuator Health는 로드 밸런서와 모니터링 시스템이 애플리케이션의 운영 상태를 확인하기 위한 엔드포인트다. 세부 내부 정보는 외부에 노출하지 않는다.
