# Frontend

마이데이터 기반 카드 추천 서비스의 React 프론트엔드입니다.

## 실행

Node.js 22.12 이상이 필요합니다.

```bash
npm install
npm run dev
```

로컬 개발 서버는 `/api` 요청을 `http://127.0.0.1:8080`의 Spring Boot 서버로 전달합니다.

## 검증

```bash
npm test
npm run lint
npm run build
```

배포 환경의 백엔드 주소는 `.env.example`을 참고해 `VITE_API_BASE_URL`로 설정합니다.
