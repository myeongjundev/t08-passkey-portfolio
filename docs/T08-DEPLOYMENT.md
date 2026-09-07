# T08 Vercel + Supabase HTTPS 배포 안내

이 저장소는 Vercel container function과 Supabase PostgreSQL을 기준으로 준비되어
있습니다. Vercel은 루트의 `Dockerfile.vercel`을 빌드하고, Supabase는 애플리케이션
데이터·WebAuthn 일회용 challenge·서버 HttpSession을 함께 보관합니다. 비밀값은
저장소에 넣지 않고 배포 서비스의 암호화된 환경변수로만 전달합니다.

## 1. Supabase 프로젝트 준비

1. Supabase Dashboard에서 서울과 가까운 리전에 빈 프로젝트를 만듭니다.
2. 상단 **Connect**에서 **Session pooler**, port `5432`, JDBC 정보를 확인합니다.
3. Transaction pooler의 port `6543`은 Hibernate prepared statement와 맞지 않으므로
   이 애플리케이션의 주 데이터 연결로 사용하지 않습니다.
4. JDBC URL에 `sslmode=require`를 넣습니다.

환경변수는 연결 문자열 하나에 비밀번호를 합치지 않고 아래처럼 나누는 편이 기록을
가리기 쉽습니다.

| 환경변수 | 형식 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<session-pooler-host>:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Supabase가 표시한 `postgres.<project-ref>` 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | 프로젝트 데이터베이스 비밀번호 |

첫 실행 때 Flyway가 도메인 표와 `spring_session` 표를 만듭니다. 애플리케이션은
스키마를 임의 갱신하지 않고 `ddl-auto=validate`로 확인만 합니다.

## 2. Vercel 프로젝트 연결

1. Vercel에서 **Add New → Project**를 선택합니다.
2. 공개 GitHub 저장소 `myeongjundev/t08-passkey-portfolio`를 가져옵니다.
3. Framework Preset은 자동 감지를 사용합니다. 루트의 `Dockerfile.vercel`이 있으면
   전체 요청을 Java container로 전달합니다.
4. Production 환경에 아래 값을 입력합니다.

5. **Settings → Functions → Function Region**에서 Supabase와 같은 서울
   (`icn1`)을 선택합니다. 2026-09-07 확인 당시 기본값은 미국 동부(`iad1`)였고,
   Supabase 연결 후 Flyway 초기화 중 15초 container startup timeout이 발생했습니다.
   리전 변경은 새 배포부터 적용됩니다.

| 환경변수 | 입력값 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | 위 Supabase Session pooler JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Supabase pooler 사용자 이름 |
| `SPRING_DATASOURCE_PASSWORD` | Supabase DB 비밀번호 |
| `T08_WEBAUTHN_RP_ID` | production 호스트명만. 예: `t08-passkey-portfolio.vercel.app` |
| `T08_WEBAUTHN_ORIGIN` | 정확한 HTTPS origin. 예: `https://t08-passkey-portfolio.vercel.app` |
| `DB_POOL_MAX` | `3` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1 -Dspring.main.lazy-initialization=true -Dspring.data.jpa.repositories.bootstrap-mode=lazy` |
| `PORT` | `80` — 2026-09-07 Production에서 기존 `8080`을 수정 |

`TieredStopAtLevel=1`은 JVM의 고단계 JIT 컴파일을 제한해 기동 부담을 줄입니다.
장시간 실행 시 최대 처리 성능과 맞바꾸는 선택이며, 이 저트래픽 과제의 짧은
container 기동 제한에 대응하기 위해 적용했습니다. 인증 검증이나 Flyway/JPA
스키마 검증은 그대로 실행합니다.

지연 초기화는 일부 오류가 첫 요청에서 드러날 수 있으므로 배포 직후
`pwsh -File scripts/Verify-Deployment.ps1`로 등록 옵션 API까지 확인합니다.
이 스크립트는 비밀값을 출력하지 않고 실제 패스키·계정을 만들지 않습니다.

RP ID에는 `https://`와 경로를 넣지 않습니다. Origin에는 마지막 슬래시나 경로를
붙이지 않습니다. 실제 배포 호스트명이 예시와 다르면 두 값 모두 실제 주소로 바꿉니다.
Preview URL은 매번 달라질 수 있으므로 패스키 제출 검증은 production URL 하나에서만
진행합니다.

## 3. 무상태 container의 세션 처리

Vercel container는 유휴 시 종료되고 여러 instance로 확장될 수 있습니다. 운영 프로필의
`PersistentSessionConfiguration`은 `HttpSession`을 Spring Session JDBC로 바꿉니다.
브라우저 쿠키에는 불투명 session ID만 남고 다음 값은 Supabase에 직렬화됩니다.

- 인증된 합성 계정의 UUID와 표시 이름
- 익명 ceremony 소유자 난수
- CSRF 난수

따라서 instance가 바뀌어도 패스키 등록 흐름과 로그인 상태가 유지됩니다. Supabase 연결이
끊기면 세션을 추측하거나 우회하지 않고 요청이 실패하는 방향으로 동작합니다.

## 4. 배포 직후 확인

새 시크릿 창과 실제 패스키를 쓸 수 있는 기기에서 다음 순서로 확인합니다.

1. `https://<production-host>/health`가 `{"status":"UP"}`과 200을 반환합니다.
2. `/`가 로그인 없이 열리고 공개 소개가 보입니다.
3. `/private`를 직접 열면 인증 전에는 401로 거절됩니다.
4. `/access`에서 합성 이름으로 첫 패스키를 등록하고 비공개 자료 3건을 봅니다.
5. 로그아웃 뒤 같은 패스키로 다시 로그인합니다.
6. 두 번째 패스키를 등록하고 첫 번째를 지운 뒤 남은 패스키로 로그인합니다.
7. 실제 저장 위치를 `docs/T08-SUBMISSION.md`와 T08-C26에 기록합니다.
8. 결과 HTTPS URL을 제출문과 T08-C01/C03/C10/C11/C52 증거에 반영합니다.

## 5. 로그와 제출 증거

Vercel이나 Supabase 로그를 제출 자료로 옮길 때 연결 문자열, 쿠키, CSRF 값,
challenge, credential ID와 서명은 가립니다. 실제 이름·연락처 대신 합성 데이터만
사용합니다. `/health`는 데이터베이스 연결까지 확인하지만 오류 상세는 응답에
포함하지 않습니다.
