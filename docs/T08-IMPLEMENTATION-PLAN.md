# T08 구현 계획

기준: `T08-ARCHITECTURE.md` · `T08-ACCEPTANCE-MATRIX.md`

한 번에 한 단계만 구현한다. 각 단계가 끝날 때 테스트·evidence·`STATUS.md`를 함께 갱신한다.
실제 개인정보, raw session, token, challenge, assertion signature는 커밋하지 않는다.

## 선행 gate

- [x] T08 과제 MD와 저장소 전사본 내용 일치
- [x] 카드 원본 이미지 7장 SHA-256 일치
- [x] Spring Boot 공개 T01 페이지 빌드·실행 확인
- [x] 전체 아키텍처와 계정 bootstrap 방식 결정
- [ ] 공식 과제 페이지에서 빠진 T08-C04~C09 확보
- [x] WebAuthn4J 0.31.8 target compatibility spike 후 버전 확정

빠진 기준은 Card 1~4 구현을 막지는 않지만 최종 완료 선언과 배포 제출을 막는다.

## 모든 slice에 적용하는 SKT ALeph 보안 gate

- [x] 신뢰 경계·위협·네트워크 정책 문서화 → `T08-SECURITY-NETWORK.md`
- [x] 기본 CSP·클릭재킹·MIME·referrer·passkey Permissions-Policy 헤더
- [ ] 모든 상태 변경 route의 JSON·Origin·CSRF 전수 테스트
- [ ] DB 기반 options/finish 속도 제한과 요청 크기 제한
- [ ] session fixation 방지·Secure/HttpOnly/SameSite·서버 logout 검증
- [ ] 중앙 redaction과 security event 저장
- [ ] Vercel HTTPS·Supabase TLS·production HSTS 검증
- [ ] worktree·build·Git evidence secret-pattern 감사

각 기능의 성공 경로는 같은 주소·같은 방식의 거절 또는 재생 경로와 한 쌍으로 구현한다.

## Slice 1 — 공개와 비공개 경계

Acceptance: T08-C13~C18

- [x] 계정 A용 합성 private fixture 세 개 정의
- [ ] 격리 테스트용 계정 B fixture 세 개 정의 (Slice 5 전 완료)
- [x] 공개 페이지에 `/access` 진입 링크 추가
- [x] `/access` 공개 화면 추가
- [x] `/private`와 `/api/private-items` 보호 경로 추가
- [x] `SessionPrincipal`과 `AuthenticationInterceptor` 추가
- [x] 비로그인 보호 요청 401
- [x] 공개 HTML source에 fixture 문자열 0건 검사
- [x] `docs/evidence/01-public-private-boundary.md` 작성

완료 gate:

```text
GET /                         200, T01 공개 내용 유지
GET /private                  401
GET /api/private-items        401
공개 HTML private fixture     0건
```

## Slice 2 — schema와 일회용 ceremony

Acceptance 기반: T08-C19, C20, C27, C28, C31

- [x] Flyway와 schema migration 추가
- [x] account, credential, ceremony, private item, security event 모델 추가
- [x] `SecureRandom` 32-byte challenge 생성
- [x] 5분 만료와 한 번 소비 구현
- [x] 같은 anonymous session·kind의 active ceremony를 하나로 제한
- [x] 동시 소비 테스트
- [x] 만료·kind 불일치·재생 거절 테스트
- [x] challenge 원문을 출력하지 않는 fingerprint helper 추가

완료 gate:

- 두 options 요청의 fingerprint가 다르다.
- 첫 finish lease만 성공하고 같은 ceremony의 두 번째 요청은 거절된다.
- 실패 응답과 로그에 raw challenge가 없다.

## Slice 3 — 최초 passkey와 계정 생성

Acceptance: T08-C19~C26

- [x] creation options DTO와 base64url 변환 구현
- [x] `residentKey=required`, `userVerification=required`, `attestation=none`
  - [x] `navigator.credentials.create()` UI 구현
  - [x] WebAuthn4J registration verification adapter 구현
  - [x] 검증 성공 후 account·credential·fixture 3건 atomic insert
  - [x] public key·credential ID·counter·transport·backup flags 저장
  - [x] nickname 검증과 계정 내 unique 제약
  - [x] 취소 endpoint와 `NotAllowedError` 화면 안내
  - [x] 취소·실패 후 account/credential 0행 검사
  - [x] `docs/evidence/05-registration-finish.md` 작성

증거에는 서로 다른 challenge fingerprint, redacted 등록 request body, 저장된 공개키 형식,
등록 취소 전후 행 개수를 넣는다. 실제 passkey 저장 위치 문장은 사용자가 작성한다.

## Slice 4 — passkey 로그인과 session

Acceptance: T08-C27~C35

  - [x] username-less request options 구현
  - [x] `navigator.credentials.get()` UI 구현
  - [x] credential ID와 userHandle로 account/record 조회
  - [x] assertion signature·origin·RP ID·UP/UV 검증
  - [x] sign counter 정책 구현
  - [x] 성공 시 새 HttpSession과 CSRF 발급
  - [x] POST logout과 session invalidate 구현
  - [x] spent challenge·틀린 signature·다른 origin 거절 테스트
  - [x] password input·password column·hash package 부재 검사
  - [x] `docs/evidence/06-passkey-authentication.md` 작성

완료 gate:

- 검증 성공 전에는 session principal이 생기지 않는다.
- 실패와 성공 request/response가 나란히 존재한다.
- 로그아웃 뒤 보호 route와 이전 assertion 재생이 모두 실패한다.

## Slice 5 — 비공개 데이터 소유권

Acceptance: T08-C36~C41

  - [x] repository query를 account ID 필수 형태로 제한
  - [x] private HTML과 JSON이 session account만 사용
  - [x] 타인 item ID 404
  - [x] `?accountId=<other>`를 보내도 현재 account 데이터만 반환
  - [x] A→B, B→A 양방향 테스트
  - [x] 거절 전후 상대 account 행 개수 불변 검사
  - [x] `docs/evidence/08-cross-account-isolation.md` 작성

## Slice 6 — 두 번째 passkey와 삭제

Acceptance: T08-C42~C46

  - [x] authenticated add-passkey ceremony 구현
  - [x] 기존 credential IDs를 `excludeCredentials`에 포함
  - [x] nickname·registered date 목록 화면
  - [x] `{credentialId, accountId}` scoped delete
  - [x] 두 개 중 하나 삭제 후 남은 credential 로그인 성공
  - [x] 삭제된 credential 로그인 실패
  - [x] 마지막 credential 삭제 409와 복구 불가능성 안내
- [ ] `docs/evidence/05-passkey-management.md` 작성

## Slice 7 — 공통 요청 방어와 운영

- [ ] session CSRF token과 `X-CSRF-Token`
- [ ] JSON content type과 body size 제한
- [ ] configured Origin allowlist
- [ ] session cookie HttpOnly·SameSite=Lax·production Secure
- [ ] cache-control no-store on ceremony/auth/private responses
- [ ] common error envelope과 secret redaction
- [x] DB-backed `/health`
- [ ] production `ddl-auto=validate`
- [x] Dockerfile.vercel·Vercel·Supabase 설정
- [ ] PostgreSQL migration·concurrency test

## Slice 8 — 실제 브라우저와 제출 증거

Acceptance: T08-C01~C12, C47~C53

- [ ] 실제 HTTPS origin에서 새 account A 등록
- [ ] 다른 authenticator로 account A의 두 번째 passkey 등록
- [ ] account B 별도 등록
- [ ] 네 가지 필수 성공/거절 쌍 캡처
  - 로그인 없이 private 열기
  - B passkey session으로 A data 열기
  - spent challenge 재사용
  - 삭제된 passkey로 로그인
- [ ] 모든 cookie/session/CSRF/challenge/signature redaction
- [ ] `docs/T08-AUTH-GUIDE.md` 여섯 절 작성
- [ ] `docs/T08-SUBMISSION.md` 확인 네 줄 작성
- [ ] AI와 본인 판단 세 줄은 사용자가 직접 작성
- [ ] synthetic-only audit
- [ ] 시크릿 창에서 공개 결과물과 source URL 확인
- [ ] 공식 C04~C09를 matrix에 원문 그대로 보충하고 확인

## 마지막 회귀 검사

```powershell
.\gradlew.bat clean test build
git diff --check
git status --short --branch
```

추가로 확인할 것:

- [ ] H2와 PostgreSQL 모두 통과
- [ ] 모든 matrix status에 실제 evidence 경로 존재
- [ ] 공개 HTML·JS·source map에 private fixture 없음
- [ ] password 문자열 검사는 설명 문서의 금지 설명과 실제 input/schema를 구분
- [ ] Git object 전체에서 secret-like 값 0건
- [ ] Vercel 배포 commit과 제출 commit 40자가 일치
