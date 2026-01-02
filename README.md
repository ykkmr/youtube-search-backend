# YouTube Search Backend

YouTube 영상 검색 백엔드 애플리케이션 - 키워드, 조회수, 구독자수, 영상 길이 등 다양한 조건으로 YouTube 영상을 검색할 수 있는 REST API 서버입니다.

## 📋 목차

- [기능](#기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 엔드포인트](#api-엔드포인트)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능 설명](#주요-기능-설명)

## ✨ 기능

### 검색 기능
- **키워드 검색**: YouTube 영상을 키워드로 검색
- **제목 필터링**: 검색어가 제목에 포함된 영상만 표시
  - 1단어: 정확히 포함되어야 함
  - 2단어: 모든 단어가 포함되어야 함
  - 3단어 이상: 검색어 전체가 포함되거나 2/3 이상의 단어가 포함되어야 함
- **정렬 옵션**: 관련도순, 최신순, 조회수순, 평점순, 제목순

### 필터링 기능
- **영상 길이**: 전체, 1분 미만 (Shorts), 4분 미만, 4-20분, 20분 이상
- **조회수 범위**: 최소/최대 조회수로 필터링
- **구독자수 범위**: 최소/최대 구독자수로 필터링

### 페이지네이션
- YouTube API의 페이지 토큰을 활용한 페이지네이션
- 자동 결과 보충: 필터링 후 결과가 부족하면 자동으로 추가 페이지를 가져옴
- 페이지 히스토리 관리로 이전 페이지로 정확히 이동 가능

## 🛠 기술 스택

- **언어**: Kotlin 2.0.0
- **프레임워크**: Spring Boot 3.3.3
- **빌드 도구**: Gradle
- **HTTP 클라이언트**: WebClient (Reactive)
- **데이터베이스**: H2 (로컬 개발용)
- **외부 API**: YouTube Data API v3

## 🚀 시작하기

### 1. YouTube Data API v3 키 발급

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속
2. 새 프로젝트 생성 또는 기존 프로젝트 선택
3. "API 및 서비스" > "라이브러리"로 이동
4. "YouTube Data API v3" 검색 후 활성화
5. "사용자 인증 정보" > "사용자 인증 정보 만들기" > "API 키" 선택
6. 생성된 API 키 복사

### 2. 환경 변수 설정

#### 방법 1: 환경 변수로 설정 (권장)

```bash
export YOUTUBE_API_KEY=your-youtube-api-key-here
```

영구적으로 설정하려면 `~/.zshrc` 또는 `~/.bashrc`에 추가:

```bash
echo 'export YOUTUBE_API_KEY=your-youtube-api-key-here' >> ~/.zshrc
source ~/.zshrc
```

#### 방법 2: application.yml에 직접 입력

`src/main/resources/application.yml` 파일을 열고:

```yaml
youtube:
  api:
    key: your-youtube-api-key-here
```

**주의**: API 키는 Git에 커밋하지 마세요!

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/youtube-search-backend.jar
```

서버가 `http://localhost:8080`에서 실행됩니다.

## 📡 API 엔드포인트

### 영상 검색

**POST** `/api/youtube/search`

```json
{
  "keyword": "검색 키워드",
  "maxResults": 25,
  "order": "relevance",
  "videoDuration": "any",
  "pageToken": "다음_페이지_토큰",
  "minViewCount": 1000,
  "maxViewCount": 1000000
}
```

**GET** `/api/youtube/search?keyword=검색어&maxResults=25&order=relevance`

#### 요청 파라미터

| 파라미터 | 타입 | 설명 | 기본값 |
|---------|------|------|--------|
| `keyword` | String | 검색 키워드 (필수) | - |
| `maxResults` | Int | 최대 결과 수 | 25 |
| `order` | String | 정렬 기준 | relevance |
| `videoDuration` | String | 영상 길이 필터 | any |
| `pageToken` | String | 페이지네이션 토큰 | null |
| `minViewCount` | Long | 최소 조회수 | null |
| `maxViewCount` | Long | 최대 조회수 | null |

#### 정렬 옵션 (order)
- `relevance`: 관련도순 (기본값)
- `date`: 최신순
- `viewCount`: 조회수순
- `rating`: 평점순
- `title`: 제목순

#### 영상 길이 (videoDuration)
- `any`: 전체
- `shorts`: 1분 미만 (Shorts)
- `short`: 4분 미만
- `medium`: 4-20분
- `long`: 20분 이상

#### 응답 예시

```json
{
  "videos": [
    {
      "videoId": "dQw4w9WgXcQ",
      "title": "영상 제목",
      "description": "영상 설명",
      "thumbnailUrl": "https://...",
      "channelId": "UC...",
      "channelTitle": "채널명",
      "publishedAt": "2024-01-01T00:00:00Z",
      "viewCount": 1000000,
      "likeCount": 50000,
      "commentCount": 1000,
      "duration": "PT3M30S",
      "subscriberCount": 100000
    }
  ],
  "totalResults": 1000000,
  "nextPageToken": "CAoQAA",
  "prevPageToken": null
}
```

## 📁 프로젝트 구조

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
│   └── YoutubeService.kt           # YouTube API 호출 및 비즈니스 로직
└── dto/
    ├── VideoSearchRequest.kt       # 검색 요청 DTO
    └── VideoSearchResponse.kt      # 검색 응답 DTO
```

## 🔍 주요 기능 설명

### 제목 필터링

검색어가 제목에 포함된 영상만 반환합니다. 필터링 로직은 다음과 같습니다:

- **1단어 검색**: 제목에 정확히 포함되어야 함
- **2단어 검색**: 모든 단어가 제목에 포함되어야 함
- **3단어 이상**: 검색어 전체가 포함되거나, 2/3 이상의 단어가 포함되어야 함

제목 필터링으로 인해 많은 결과가 필터링될 수 있으므로, 자동으로 더 많은 결과를 가져와서 필터링합니다.

### 자동 결과 보충

필터링 후 결과가 부족하면 자동으로 다음 페이지를 가져와 결과를 채웁니다:

- 결과가 없을 때: 최대 5번까지 자동으로 다음 페이지를 가져옴
- 결과가 부족할 때: 최대 3번까지 추가로 가져옴

### 조회수/구독자수 필터링

조회수나 구독자수 필터링이 필요한 경우:
1. 더 많은 결과를 가져옴 (최대 50개)
2. 영상 상세 정보와 채널 정보를 병렬로 가져옴
3. 필터링 조건에 맞는 영상만 반환

### Shorts 필터링 (1분 미만)

YouTube API는 1분 미만을 직접 지원하지 않으므로:
1. "short" (4분 미만)로 검색
2. 영상 상세 정보에서 duration을 가져옴
3. ISO 8601 형식 (예: PT1M30S)을 파싱하여 초 단위로 변환
4. 60초 미만인 영상만 필터링

## 🔧 개발 환경

- Java 21
- Kotlin 2.0.0
- Spring Boot 3.3.3
- Gradle 8.5+

## 📝 참고사항

### API 할당량

YouTube Data API v3는 일일 할당량이 있습니다:
- 기본 할당량: 10,000 units/일
- 검색 API: 100 units/요청
- 비디오 상세 정보: 1 unit/요청
- 채널 정보: 1 unit/요청

할당량을 초과하면 403 오류가 발생할 수 있습니다.

### 에러 처리

- API 키 미설정: 400 Bad Request
- YouTube API 오류: 에러 메시지와 함께 400 Bad Request
- 네트워크 오류: 500 Internal Server Error

## 📄 라이선스

이 프로젝트는 개인 사용 목적으로 제작되었습니다.
