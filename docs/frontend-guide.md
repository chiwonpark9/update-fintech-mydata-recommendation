# 프론트엔드 실행·구조 가이드

## 역할

`frontend`는 마이데이터 기반 카드 추천 서비스를 보여주는 React 애플리케이션입니다. 현재 단계에서는 서비스의 세 가지 모듈과 구현 방향을 소개하고, Spring Boot Health API와의 실제 연결 상태를 표시합니다.

## 요구 환경

- Node.js 22.12 이상
- npm 10 이상
- Health API 확인 시 `http://127.0.0.1:8080`에서 실행 중인 백엔드

## 실행과 검증

```bash
cd frontend
npm install
npm run dev
```

```bash
npm test
npm run lint
npm run build
```

| 명령 | 목적 |
| --- | --- |
| `npm run dev` | 개발 서버와 빠른 화면 갱신 |
| `npm test` | API 계약과 UI 상태 테스트 |
| `npm run lint` | 코드 규칙과 잠재 오류 검사 |
| `npm run build` | 타입 검사 후 배포 파일 생성 |

## 현재 요청 흐름

```text
BackendStatus 컴포넌트
  → getBackendHealth 함수
  → GET /api/v1/health
  → Vite 개발 프록시
  → Spring Boot :8080
```

배포 환경에서는 `.env.example`을 복사해 `VITE_API_BASE_URL`을 백엔드 공개 주소로 설정합니다. 브라우저에 포함되는 `VITE_` 환경 변수에는 비밀값을 넣지 않습니다.

## 폴더 구조

```text
frontend/src
├── features
│   └── system
│       ├── api
│       │   ├── health.ts
│       │   └── health.test.ts
│       └── components
│           ├── BackendStatus.tsx
│           └── BackendStatus.test.tsx
├── test
│   └── setup.ts
├── App.tsx
├── App.css
├── index.css
└── main.tsx
```

화면 기능을 기술 종류가 아니라 업무 기능 단위인 `features`로 나눕니다. 이후 `auth`, `mydata`, `recommendation`, `advisor` 기능도 같은 방식으로 추가합니다.

## Health API 처리 원칙

TypeScript 타입 선언은 서버 응답을 자동으로 보장하지 않습니다. `health.ts`는 네트워크에서 받은 값을 `unknown`으로 취급하고 `status`, `service`가 문자열인지 확인한 뒤 화면에 전달합니다.

컴포넌트는 다음 상태를 명시적으로 구분합니다.

- `loading`: 응답을 기다리는 상태
- `connected`: 검증된 응답을 받은 상태
- `error`: HTTP 오류, 네트워크 오류, 잘못된 응답 형식
- `retry`: 오류 화면에서 사용자가 다시 요청하는 동작

화면을 벗어날 때는 `AbortController`로 진행 중인 요청을 취소해 이미 사라진 컴포넌트의 상태를 바꾸지 않도록 합니다.

## 테스트 범위

- 정상 Health 응답 반환
- 응답 계약 불일치 차단
- 로딩에서 연결 성공으로 전환
- 연결 실패 후 재시도 성공

테스트는 백엔드 서버 없이도 빠르게 실행되도록 `fetch`를 가짜 응답으로 교체합니다. 별도로 실제 로컬 서버를 함께 실행해 Vite 프록시까지 포함한 연결을 확인합니다.

## 외장 드라이브 개발 참고

macOS가 외장 파일 시스템에 만드는 `._*` AppleDouble 파일은 TypeScript와 테스트 도구가 소스 파일로 오인할 수 있습니다. 이 프로젝트는 Git에서 해당 파일을 제외하며, 로컬에서 문제가 생기면 숨김 파일을 정리합니다. `node_modules`는 Git에 포함하지 않습니다.
