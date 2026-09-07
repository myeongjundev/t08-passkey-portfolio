# T08 학원 작업 인수인계 — 2026-09-08

학원 PC에서 T08을 이어서 마감하기 위한 최신 문서입니다.

> **한 줄 요약:** 코드와 Vercel/Supabase 배포는 정상입니다. 캡처된 기준 47개 중
> 46개가 완료됐고, 실제 기기 패스키 저장 위치(C26), 공식 페이지의 누락 기준
> C04–C09 확인, 제출문 개인 판단 문장 확인만 남았습니다.

---

## 1. 시작하자마자 할 일

```bash
git clone https://github.com/myeongjundev/t08-passkey-portfolio.git
cd t08-passkey-portfolio
git pull --ff-only origin main
git log -3 --oneline
```

이미 clone되어 있으면 두 번째 줄부터 실행합니다. 다음 커밋이 보여야 합니다.

```text
31c366d docs: record healthy T08 production deployment
eebe09e fix: expose guarded startup port on Vercel
```

다른 수정이 남아 있으면 덮어쓰지 말고 먼저 `git status --short`를 확인합니다.

---

## 2. 현재 확정 상태

| 항목 | 상태 |
| --- | --- |
| 결과물 | https://t08-passkey-portfolio.vercel.app |
| 소스 | https://github.com/myeongjundev/t08-passkey-portfolio |
| Vercel | 정상 · 서울 `icn1` · Production `PORT=80` |
| Supabase | 정상 · 서울 · Session pooler 5432 · Data API 비활성 |
| 자동 테스트 | JDK 25 · 38 tests · 실패/오류/스킵 0 |
| 운영 HTTP 검사 | 전체 통과 |
| 고정된 기준 | 47개 중 done 46 · todo 1(C26) |
| 아직 원문이 없는 기준 | C04–C09 |

첫 cold start에는 다음 순서가 정상입니다.

1. `503 Service is starting` + `Retry-After: 2`
2. 잠시 뒤 `GET /health` → `200 {"status":"UP"}`

503 준비 응답은 공개·비공개 데이터를 포함하지 않으며 `Cache-Control: no-store`입니다.
Vercel의 빌드 표시가 `Ready`인 것만으로 판단하지 말고 아래 검사를 실행합니다.

```powershell
pwsh -File scripts/Verify-Deployment.ps1
```

마지막 줄이 아래와 같으면 정상입니다.

```text
PASS: deployed HTTP and registration-option boundaries (no passkey/account created)
```

이 검사는 challenge·세션·CSRF 값을 출력하지 않고 실제 계정이나 패스키도 만들지 않습니다.
고정된 기동 중 503만 최대 10회 재시도하며, 다른 오류 응답은 즉시 실패로 처리합니다.

---

## 3. 학원에서 끝낼 작업 — 권장 순서

### A. 공식 과제 페이지에서 C04–C09 확인

현재 카드 이미지에는 C01–C03 다음이 C10으로 이어져 C04–C09 원문이 없습니다.

1. 공식 T08 과제 페이지를 엽니다.
2. C04–C09 문장을 그대로 확인합니다.
3. `docs/T08-ACCEPTANCE-MATRIX.md`에 기준을 추가합니다.
4. 이미 충족된 항목이라도 근거 문서를 연결한 뒤에만 `done`으로 표시합니다.
5. 기존 기준을 약화하거나 추측해서 채우지 않습니다.

원문을 찾지 못하면 `D-003`을 열어 둔 채 제출 전에 담당자에게 확인합니다.

### B. 실제 기기 패스키 확인 — C26

Chrome 또는 Edge의 새 시크릿 창에서 진행합니다. 모든 이름과 내용은 합성값을 씁니다.

1. https://t08-passkey-portfolio.vercel.app/ 접속
2. 공개 소개 페이지가 로그인 없이 열리는지 확인
3. `나만의 공간` → `/access`
4. 합성 표시 이름과 첫 패스키 이름 입력 → 패스키 생성
5. 비공개 합성 자료 3건과 패스키 이름·등록일 확인
6. 로그아웃 → 같은 패스키로 다시 로그인
7. 두 번째 패스키를 **다른 이름**으로 등록
8. 첫 패스키 삭제 → 남은 패스키로 로그인
9. 브라우저/OS가 표시한 실제 저장 위치를 기록

저장 위치 예: `Google 비밀번호 관리자`, `이 Windows 기기`, `보안 키`.
확인한 실제 문구만 `docs/T08-SUBMISSION.md`의 `패스키 저장 위치`에 적고
`T08-ACCEPTANCE-MATRIX.md`의 C26을 `done`으로 바꿉니다.

도메인을 바꾸면 RP ID가 달라져 기존 패스키를 다시 등록해야 합니다. 현재 canonical
도메인 `t08-passkey-portfolio.vercel.app`을 유지합니다.

### C. 제출문 개인 문장 확인 — C53

`docs/T08-SUBMISSION.md`의 `AI와 내 판단` 세 줄을 읽고 본인이 실제로 한 판단에 맞게
고칩니다. 특히 `내가 직접 판단한 일`은 AI가 대신 확정할 수 없습니다.

최종 제출문에 필요한 URL은 이미 입력되어 있습니다.

---

## 4. 최종 검증

JDK 25가 설치돼 있으면:

```powershell
$env:JAVA_HOME='JDK 25 설치 경로'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test build --console=plain
pwsh -File scripts/Verify-Deployment.ps1
git diff --check
git status --short
```

Windows가 아니면 `./gradlew clean test build`를 사용합니다. 예상 결과:

- 38개 이상 테스트, 실패·오류·스킵 0
- 운영 검사 PASS
- `git diff --check` 출력 없음
- 마지막 문서 수정 외 예상하지 않은 변경 없음

마감한 문서는 커밋하고 `main`에 푸시합니다. 문서 커밋도 Vercel 재배포를 만들 수 있으므로
배포 완료 뒤 운영 검사를 한 번 더 실행합니다.

---

## 5. 건드리지 말아야 할 설정

- Vercel `PORT=80`을 8080으로 바꾸지 않습니다.
- Function Region은 서울 `icn1`을 유지합니다.
- `JAVA_TOOL_OPTIONS`의 기동 옵션을 제거하지 않습니다.
- Supabase Data API를 켜지 않습니다.
- Transaction pooler 6543으로 바꾸지 않습니다.
- 비밀번호 필드·비밀번호 로그인·개인키 저장을 추가하지 않습니다.
- 실제 개인정보를 테스트나 제출 자료에 넣지 않습니다.

배포 설정 전체는 `docs/T08-DEPLOYMENT.md`, 시행착오와 성공 증거는
`docs/evidence/11-production-startup.md`, 설계 이유는 `docs/DECISIONS.md`의
D-015~D-017에 있습니다.

---

## 6. 문제가 생겼을 때

### 결과물이 503

첫 cold start라면 2~10초 뒤 다시 시도합니다. 응답에 고정 문구
`Service is starting. Please retry shortly.`가 있으면 시작 보호 장치가 작동 중입니다.

### 결과물이 500

Vercel Runtime Logs에서 해당 요청을 열고 다음 순서로 봅니다.

1. `Picked up JAVA_TOOL_OPTIONS`
2. Java 25와 `prod` profile 시작
3. Hikari connection 성공
4. Flyway/PostgreSQL 확인
5. `Tomcat started on port 80`

로그나 화면을 문서로 옮길 때 DB 비밀번호·연결 문자열의 비밀번호·쿠키·CSRF·challenge·
credential ID·서명을 가립니다.

### 패스키가 작동하지 않음

- 주소가 정확히 canonical HTTPS 도메인인지 확인
- Vercel의 `T08_WEBAUTHN_RP_ID=t08-passkey-portfolio.vercel.app` 확인
- `T08_WEBAUTHN_ORIGIN=https://t08-passkey-portfolio.vercel.app` 확인
- 끝 슬래시나 preview 도메인을 사용하지 않았는지 확인

---

## 7. 완료 조건

- C04–C09 원문과 상태가 acceptance matrix에 있음
- 실제 패스키 저장 위치가 제출문에 있음
- C26이 done
- C53 문장이 본인의 실제 판단과 일치
- 전체 테스트와 운영 검사가 통과
- 최종 `main`이 원격에 push됨
- 제출한 두 URL이 새 시크릿 창에서 열림

이 조건까지 끝나면 T08 제출 준비 완료입니다.
