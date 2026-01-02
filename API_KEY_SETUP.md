# YouTube API 키 설정 방법

## 빠른 설정

터미널에서 다음 명령어를 실행하세요:

```bash
export YOUTUBE_API_KEY=AIzaSyAeEEaL04Y1AVIQ5lF_JUuwY1payY6dXhY
```

그 다음 애플리케이션을 실행:
```bash
cd /Users/ileuleu/IdeaProjects/youtube-search-backend
./gradlew bootRun
```

## 영구적으로 설정하기 (macOS/Linux)

### 방법 1: ~/.zshrc 또는 ~/.bashrc에 추가

```bash
echo 'export YOUTUBE_API_KEY=AIzaSyAeEEaL04Y1AVIQ5lF_JUuwY1payY6dXhY' >> ~/.zshrc
source ~/.zshrc
```

### 방법 2: application.yml에 직접 입력

`src/main/resources/application.yml` 파일을 열고:

```yaml
youtube:
  api:
    key: AIzaSyAeEEaL04Y1AVIQ5lF_JUuwY1payY6dXhY
```

**주의**: 이 방법은 Git에 커밋하지 마세요! `.gitignore`에 추가하거나 별도의 `application-local.yml` 파일을 사용하세요.

## 확인

애플리케이션을 실행한 후, 다음 오류가 나타나지 않으면 정상적으로 설정된 것입니다:
- ❌ "YouTube API key is not configured"

