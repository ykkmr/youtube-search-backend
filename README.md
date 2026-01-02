# YouTube Search Backend

YouTube 영상 검색 백엔드 애플리케이션

## 시작하기

### 1. YouTube Data API v3 키 발급

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속
2. 새 프로젝트 생성 또는 기존 프로젝트 선택
3. "API 및 서비스" > "라이브러리"에서 "YouTube Data API v3" 활성화
4. "사용자 인증 정보"에서 API 키 생성

### 2. 환경 변수 설정

`application.yml` 파일을 수정하거나 환경 변수로 설정:

```yaml
youtube:
  api:
    key: YOUR_YOUTUBE_API_KEY_HERE
```

또는 환경 변수:

```bash
export YOUTUBE_API_KEY=your-api-key-here
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/youtube-search-backend.jar
```

## API 엔드포인트

### 영상 검색

**POST** `/api/youtube/search`

```json
{
  "keyword": "검색 키워드",
  "maxResults": 25,
  "order": "relevance",
  "videoDuration": "any",
  "minViewCount": 1000,
  "maxViewCount": 1000000
}
```

**GET** `/api/youtube/search?keyword=검색어&maxResults=25&order=relevance`

#### 정렬 옵션 (order)
- `relevance`: 관련도순 (기본값)
- `date`: 최신순
- `viewCount`: 조회수순
- `rating`: 평점순
- `title`: 제목순

#### 영상 길이 (videoDuration)
- `any`: 전체
- `short`: 4분 미만
- `medium`: 4-20분
- `long`: 20분 이상

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.3.3
- **빌드 도구**: Gradle
- **HTTP 클라이언트**: WebClient (Reactive)
- **데이터베이스**: H2 (로컬 개발용)

## 프로젝트 구조

```
src/main/kotlin/com/youtube/search/
├── YoutubeSearchApplication.kt    # 메인 애플리케이션
├── config/
│   ├── WebConfig.kt                # CORS 설정
│   ├── WebClientConfig.kt          # WebClient 설정
│   └── YoutubeApiProperties.kt    # YouTube API 설정
├── controller/
│   └── YoutubeController.kt        # REST API 컨트롤러
├── service/
│   └── YoutubeService.kt           # YouTube API 호출 서비스
└── dto/
    ├── VideoSearchRequest.kt       # 검색 요청 DTO
    └── VideoSearchResponse.kt      # 검색 응답 DTO
```

## 개발 환경

- Java 21
- Kotlin 2.0.0
- Spring Boot 3.3.3

