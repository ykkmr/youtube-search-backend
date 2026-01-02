# GitHub 저장소 설정 가이드

## 1. GitHub에서 새 저장소 생성

1. [GitHub](https://github.com)에 로그인
2. 우측 상단의 "+" 버튼 클릭 → "New repository" 선택
3. 저장소 이름 입력 (예: `youtube-search-backend`)
4. Public 또는 Private 선택
5. "Initialize this repository with a README" 체크 해제 (이미 로컬에 파일이 있으므로)
6. "Create repository" 클릭

## 2. 원격 저장소 연결 및 푸시

```bash
cd /Users/ileuleu/IdeaProjects/youtube-search-backend

# 원격 저장소 추가 (YOUR_USERNAME과 REPO_NAME을 실제 값으로 변경)
git remote add origin https://github.com/YOUR_USERNAME/youtube-search-backend.git

# 또는 SSH 사용 시
git remote add origin git@github.com:YOUR_USERNAME/youtube-search-backend.git

# 메인 브랜치를 main으로 설정
git branch -M main

# GitHub에 푸시
git push -u origin main
```

## 3. 프론트엔드도 동일하게 설정

```bash
cd /Users/ileuleu/IdeaProjects/youtube-search-frontend

# 원격 저장소 추가
git remote add origin https://github.com/YOUR_USERNAME/youtube-search-frontend.git

# 푸시
git branch -M main
git push -u origin main
```

## 참고사항

- API 키는 환경 변수로 관리하거나 GitHub Secrets를 사용하세요
- `.gitignore`에 민감한 정보가 제외되어 있는지 확인하세요

