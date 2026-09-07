# T08 HTTPS 배포 안내

이 저장소는 Render Web Service와 외부 PostgreSQL(Neon 등)을 기준으로 준비되어
있습니다. `render.yaml`은 Docker 빌드, 싱가포르 리전, `/health` 상태 확인과 운영
환경변수를 선언합니다. 비밀값은 저장소에 넣지 않고 배포 화면에서만 입력합니다.

## 1. PostgreSQL 준비

빈 PostgreSQL 데이터베이스를 만들고 다음 세 값을 보관합니다.

- JDBC URL: `jdbc:postgresql://<host>/<database>?sslmode=require`
- 데이터베이스 사용자 이름
- 데이터베이스 비밀번호

제공받은 주소가 `postgresql://`로 시작하면 Spring JDBC용 `jdbc:postgresql://`로
바꿉니다. 첫 실행 때 Flyway가 필요한 표를 자동으로 만듭니다.

## 2. Render Blueprint 연결

1. Render에서 **New Blueprint**를 선택하고 공개 GitHub 저장소
   `myeongjundev/t08-passkey-portfolio`를 연결합니다.
2. `render.yaml`을 읽어 생성될 Web Service 이름과 리전을 확인합니다.
3. 아래 `sync: false` 환경변수를 Render 화면에서 입력합니다.

| 환경변수 | 입력값 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | 위 JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL 사용자 이름 |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL 비밀번호 |
| `T08_WEBAUTHN_RP_ID` | 배포 호스트명만 입력. 예: `t08-passkey-portfolio.onrender.com` |
| `T08_WEBAUTHN_ORIGIN` | 스킴을 포함한 정확한 origin. 예: `https://t08-passkey-portfolio.onrender.com` |

RP ID에는 `https://`와 경로를 넣지 않습니다. Origin에는 마지막 슬래시나 경로를
붙이지 않습니다. 실제 Render 호스트명이 예시와 다르면 두 값 모두 실제 주소를
따라야 합니다. 값이 빠지면 운영 프로필은 의도적으로 시작에 실패합니다.

## 3. 배포 직후 확인

새 시크릿 창과 실제 패스키를 쓸 수 있는 기기에서 다음 순서로 확인합니다.

1. `https://<배포 호스트>/health`가 `{"status":"UP"}`과 200을 반환합니다.
2. `/`가 로그인 없이 열리고 공개 소개가 보입니다.
3. `/private`를 직접 열면 인증 전에는 401로 거절됩니다.
4. `/access`에서 합성 이름으로 첫 패스키를 등록하고 비공개 자료 3건을 봅니다.
5. 로그아웃 뒤 같은 패스키로 다시 로그인합니다.
6. 두 번째 패스키를 등록하고 첫 번째를 지운 뒤 남은 패스키로 로그인합니다.
7. 실제 저장 위치를 `docs/T08-SUBMISSION.md`와 T08-C26에 기록합니다.
8. 결과 HTTPS URL을 제출문과 T08-C01/C03/C10/C11/C52 증거에 반영합니다.

## 4. 로그와 제출 증거

Render나 PostgreSQL 로그를 제출 자료로 옮길 때 연결 문자열, 쿠키, CSRF 값,
challenge, credential ID와 서명은 가립니다. 실제 이름·연락처 대신 합성 데이터만
사용합니다. `/health`는 데이터베이스 연결까지 확인하지만 오류 상세는 응답에
포함하지 않습니다.
