<div align="center">

# T08 · 비밀번호 없이 나만 들어가기

### 소개 페이지는 열어 두고, 나만의 자리는 패스키로 잠급니다

T01에서 만든 개인 포트폴리오 페이지에 **패스키(WebAuthn)** 로 잠긴 비공개 영역을
덧붙이는 SKT ALEPH T08 과제입니다. 비밀번호는 어디에도 없습니다.

[**Task**](docs/T08-TASK.md) ·
[**Requirements**](docs/REQUIREMENTS.md) ·
[**Architecture**](docs/T08-ARCHITECTURE.md) ·
[**Implementation Plan**](docs/T08-IMPLEMENTATION-PLAN.md) ·
[**Security & Network**](docs/T08-SECURITY-NETWORK.md) ·
[**Acceptance Matrix**](docs/T08-ACCEPTANCE-MATRIX.md) ·
[**Decision Log**](docs/DECISIONS.md) ·
[**Status**](docs/STATUS.md)

`Java 25` `Spring Boot 4.1` `webauthn4j` `Thymeleaf` `JPA` `PostgreSQL`

</div>

> ✅ **핵심 구현이 완료되었습니다.** 패스키 등록·로그인·로그아웃, 두 번째
> 패스키 복구, 계정 간 비공개 자료 격리와 보안 증거를 자동 테스트로 확인했습니다.
> 남은 단계는 실제 HTTPS 배포와 물리 기기에서의 최종 확인입니다.

> 공개 화면과 문서의 예시는 합성 데이터입니다. 실제 연락처나 신분증 번호 같은
> 진짜 개인정보는 저장소와 공개 배포에 포함하지 않습니다.

## 무엇을 만드는가

| 영역 | 누가 볼 수 있나 | 내용 |
| --- | --- | --- |
| 공개 소개 페이지 | 누구나. 등록·로그인 없이 | T01 포트폴리오 내용 그대로 |
| 비공개 자리 | 패스키로 들어온 본인만 | 준비 중인 프로젝트 메모 등 (합성 데이터) |

비밀번호 대신 이렇게 동작합니다.

1. **등록** — 기기가 열쇠 한 쌍을 만들고, **공개키만** 서버로 보냅니다. 개인키는
   기기 밖으로 나가지 않습니다.
2. **로그인** — 서버가 **매번 새로운 일회용 질문(challenge)** 을 보내고, 기기가
   개인키로 서명해 답합니다. 서버는 저장해 둔 공개키로 그 서명을 확인합니다.
3. **분실 대비** — 패스키를 두 개 등록해 두고, 하나를 지운 뒤에도 남은 하나로
   들어갈 수 있게 합니다.

## 과제 카드

| 카드 | 내용 | 통과 기준 |
| --- | --- | --- |
| 1 | 무엇을 잠글지 먼저 가른다 | T08-C13 – C18 |
| 2 | 패스키를 등록한다 | T08-C19 – C26 |
| 3 | 패스키로 들어간다 | T08-C27 – C35 |
| 4 | 기기를 잃어버렸을 때 | T08-C42 – C46 |
| 5 | 정말 안 열리는지 확인하고, 어떻게 붙였는지 적는다 | T08-C01 – C12, C36 – C41, C47 – C53 |

전체 과제 원문은 [`docs/T08-TASK.md`](docs/T08-TASK.md)에 있고, 원본 카드 이미지는
[`docs/task-source/`](docs/task-source/)에 보관되어 있습니다.

## 실행

```bash
./gradlew bootRun
```

http://localhost:8080/ 에서 공개 소개 페이지가 열립니다.

> WebAuthn은 origin이 정확히 일치해야 하고 https를 요구합니다. `localhost`만 http
> 예외로 허용됩니다. 배포 환경에서는 `application.properties`의
> `t08.webauthn.rp-id`·`t08.webauthn.origin`을 반드시 덮어써야 합니다.

운영 배포는 저장소의 `Dockerfile.vercel`과 Supabase PostgreSQL을 사용합니다. 필요한 환경변수와
검증 순서는 [`docs/T08-DEPLOYMENT.md`](docs/T08-DEPLOYMENT.md)에 정리했습니다.

## 저장소 구조

```
src/main/java/dev/myeongjun/passkey/
  web/HomeController.java        공개 소개 페이지 라우트
src/main/resources/
  templates/index.html           T01 소개 페이지 (서버 렌더링)
  static/                        T01 css·js·폰트·이미지
  application.properties         WebAuthn RP 설정, 데이터소스
docs/
  T08-TASK.md                    과제 원문 전체
  REQUIREMENTS.md                구현이 만족해야 하는 요구사항
  T08-ACCEPTANCE-MATRIX.md       통과 기준 체크리스트 (수정 금지)
  DECISIONS.md                   결정 기록
  STATUS.md                      현재 상태와 다음 할 일
  task-source/                   과제 카드 원본 이미지
  evidence/                      요청·응답 증거 기록
AGENTS.md                        코딩 에이전트 공통 규칙
CLAUDE.md                        Claude 진입점
```

T01 페이지는 `myeongjundev.github.io` 커밋 `0ec47fc` 기준으로 가져왔습니다
(→ [DECISIONS D-002](docs/DECISIONS.md)).

## 관련 저장소

- [T01 · 개인 포트폴리오](https://github.com/myeongjundev/myeongjundev.github.io) — 이 과제가 이어붙이는 소개 페이지
- [T07 · 플랜두씨 다이어리](https://github.com/myeongjundev/t07-plando-see-diary) — 비밀번호 기반 인증을 다룬 앞 과제
