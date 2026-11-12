# JobFit

신입 개발 공고(사람인·잡코리아·워크넷)에서 헤드헌팅·파견을 제외하고 유명기업 공고를 선별해, 요구 기술과 내 기술의 적합도를 보여주는 백엔드.

- `backend/` Spring Boot 4.1 · Java 21 (`./gradlew bootRun`, `./gradlew test`)
- `docs/채용공고_분석.xlsx` 수집·분류 결과 (유명기업 추천 / 이름 모를 기업 / 제외 목록)
- `docs/01_요구사항분석서.md` · `02_요구사항명세서.md` · `03_데이터정의서.md`(ERD·순서도) · `04_개발일정표.xlsx` · `05_포트폴리오.md`

- `ai-service/` FastAPI RAG(BM25 검색, 정형 답변) · 백엔드 `static/index.html` 웹 화면

현재 범위: 백엔드 테스트 10건 + AI 서비스 테스트 5건 통과, 웹 화면 수동 확인. 의미(임베딩) 검색과 운영 프로필(MySQL·Redis) 실행 검증은 미완.
