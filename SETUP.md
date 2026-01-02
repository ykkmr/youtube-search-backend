# YouTube API 키 설정 가이드

## 1. YouTube Data API v3 키 발급

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속
2. 새 프로젝트 생성 또는 기존 프로젝트 선택
3. "API 및 서비스" > "라이브러리"로 이동
4. "YouTube Data API v3" 검색 후 활성화
5. "사용자 인증 정보" > "사용자 인증 정보 만들기" > "API 키" 선택
6. 생성된 API 키 복사

## 2. API 키 설정 방법

### 방법 1: application.yml 파일에 직접 입력 (개발 환경용)

`src/main/resources/application.yml` 파일을 열고 다음 부분을 수정:

```yaml
youtube:
  api:
    key: 여기에_발급받은_API_키_입력
    base-url: https://www.googleapis.com/youtube/v3
```

예시:
```yaml
youtube:
  api:
    key: AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    base-url: https://www.googleapis.com/youtube/v3
```

### 방법 2: 환경 변수로 설정 (권장, 프로덕션 환경)

#### macOS/Linux:
```bash
export YOUTUBE_API_KEY=여기에_발급받은_API_키_입력
./gradlew bootRun
```

또는 `.env` 파일 생성 (프로젝트 루트):
```
YOUTUBE_API_KEY=여기에_발급받은_API_키_입력
```

#### Windows (PowerShell):
```powershell
$env:YOUTUBE_API_KEY="여기에_발급받은_API_키_입력"
./gradlew bootRun
```

#### Windows (CMD):
```cmd
set YOUTUBE_API_KEY=여기에_발급받은_API_키_입력
./gradlew bootRun
```

## 3. API 키 확인

애플리케이션을 실행한 후, 다음 오류가 나타나지 않으면 정상적으로 설정된 것입니다:
- ❌ "YouTube API key is not configured"
- ✅ 정상적으로 검색이 동작함

## 4. API 할당량 확인

YouTube Data API v3는 일일 할당량이 있습니다:
- 기본 할당량: 10,000 units/일
- 검색 API: 100 units/요청
- 비디오 상세 정보: 1 unit/요청

할당량을 초과하면 403 오류가 발생할 수 있습니다.

## 5. API 키 제한 설정 (선택사항, 보안 강화)

Google Cloud Console에서 API 키에 제한을 설정할 수 있습니다:
- HTTP 리퍼러(웹사이트) 제한
- IP 주소 제한
- API 제한 (YouTube Data API v3만 허용)

## 문제 해결

### "API key not valid" 오류
- API 키가 올바르게 복사되었는지 확인
- YouTube Data API v3가 활성화되었는지 확인
- API 키에 제한이 설정되어 있다면 제한 조건 확인

### "Quota exceeded" 오류
- 일일 할당량을 초과했을 수 있습니다
- 다음 날까지 대기하거나 할당량 증가 요청

### "Forbidden" 오류
- API 키에 IP 주소나 리퍼러 제한이 설정되어 있을 수 있습니다
- Google Cloud Console에서 제한 설정 확인

