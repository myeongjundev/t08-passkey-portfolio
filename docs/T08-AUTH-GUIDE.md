# T08 인증 구현 설명서

이 문서의 계정과 비공개 내용은 모두 과제 확인용 합성 데이터입니다. 원본 challenge,
서명, 쿠키, 세션 ID, CSRF 값과 개인키는 기록하지 않았습니다.

## ① 무엇으로 붙였나

직접 만든 비밀번호 인증이나 외부 인증 서비스가 아니라 Java용 WebAuthn 라이브러리
`WebAuthn4J 0.31.8.RELEASE`를 Spring Boot 4.1.1 애플리케이션에 연결했습니다. 브라우저는
표준 `navigator.credentials.create()`와 `navigator.credentials.get()`을 사용합니다.
로그인 뒤 사용자를 알아보는 값은 JWT가 아니라 서버의 `HttpSession`입니다.

잠금을 여는 수단은 비밀번호가 아니라 **기기의 지문 인증**입니다. 등록과 로그인 모두
`userVerification: "required"`로 요청하므로, 기기가 사용자 확인을 마쳐야만 서명이
만들어집니다. Windows Hello에서는 지문, 맥에서는 Touch ID, 휴대폰에서는 지문·얼굴이
이 자리를 담당합니다. 지문 데이터 자체는 기기 밖으로 나오지 않습니다. 지문은 기기 안에
있는 개인키를 꺼내는 열쇠로만 쓰이고, 서버가 받아서 저장하는 것은 공개키와 서명뿐입니다.
서버에는 생체정보가 저장되지 않습니다.

## ② 왜 이것을 골랐나

WebAuthn의 challenge, RP ID hash, Origin, 사용자 확인, 공개키 서명을 직접 구현하면 작은
실수도 인증 우회로 이어질 수 있습니다. 검증은 WebAuthn4J에 맡기고, 애플리케이션은
challenge 수명·일회성 소비·계정 소유권·세션·증거 가림을 담당하도록 경계를 나눴습니다.
Spring Boot와 PostgreSQL은 포트폴리오의 주력 서버 기술과 배포 구성을 이어가기 위해
선택했습니다.

## ③ 어디를 어떻게 고쳤나

- 등록: `WebAuthnController`가 등록 options/finish를 받고 `CeremonyService`가 서버
  challenge를 보관·소비합니다. `WebAuthnVerificationAdapter`가 응답을 검증한 뒤
  `AccountRegistrationService`가 계정·공개키·합성 자료 3건을 한 트랜잭션으로 저장합니다.
- 로그인: `WebAuthnController`가 username-less options를 만들고
  `AccountAuthenticationService`가 credential ID와 userHandle을 연결합니다.
  `WebAuthnVerificationAdapter`가 저장된 COSE 공개키로 서명을 검증한 뒤에만 새 세션을
  발급합니다.
- 로그아웃: `SessionController`가 서버 세션을 즉시 무효화합니다.
- 비공개 자료: `AuthenticationInterceptor`가 미인증 요청을 먼저 막고,
  `PrivateAreaController` → `PrivateAreaService` → `PrivateItemRepository`가 세션의 account
  ID만 사용해 조회합니다. URL의 accountId는 소유권 입력으로 사용하지 않습니다.
- 복구: `PasskeyController`와 `PasskeyManagementService`가 두 번째 패스키 등록·목록·삭제를
  처리하며 마지막 하나의 삭제는 409로 막습니다.
- 사용자 확인(지문): 등록 options의 `authenticatorSelection`을
  `userVerification=required`로 고정했습니다
  (`WebAuthnController:141`, `PasskeyController:76`). 로그인 options도 같은 값을 내려보내
  (`AuthenticationOptionsResponse`) 두 번째 패스키를 쓸 때도 지문을 건너뛸 수 없습니다.
  검증 단계에서도 `WebAuthnVerificationAdapter`가 WebAuthn4J에 `userVerificationRequired=true`를
  넘기므로(`:77` 등록, `:147` 로그인), 지문을 거치지 않아 UV 플래그가 꺼진 응답은 서명이
  맞더라도 거절됩니다. 옵션에서 요구하는 데 그치지 않고 서버 검증에서 다시 강제합니다.

## ④ 안 열리는 것을 확인한 기록

| 확인 | 정상 요청 | 차단 요청 | 증거 |
| --- | --- | --- | --- |
| 로그인 없이 열기 | 인증 세션의 `/api/private-items` → 200, 3건 | 무세션 요청 → 401 | `evidence/01-public-private-boundary.md` |
| 남의 패스키로 열기 | A 패스키로 A 자료 → 200 | A→B와 B→A 직접 항목 요청 → 동일한 404 | `evidence/08-cross-account-isolation.md` |
| 쓴 질문 재사용 | 최초 검증된 assertion → 200 | 같은 ceremony/assertion 재사용 → 400 | `evidence/06-passkey-authentication.md` |
| 삭제 뒤 로그인 | 남은 예비 키 로그인 → 성공 | 삭제한 주 기기 키 로그인 → 거절 | `evidence/07-second-passkey-recovery.md` |

## ⑤ AI와 나

AI에게는 요구사항 구조화, 서버·브라우저 구현 초안, 자동화 테스트와 증거 문서 작성을
맡겼습니다. 사람은 T07의 비밀번호/JWT 구성을 복사하지 않고 패스키 전용 경계를 유지할
것, 마지막 패스키 삭제를 허용하지 않을 것, 합성 데이터만 사용할 것을 결정했습니다.
AI가 제안할 수 있는 비밀번호 복구나 브라우저 토큰 방식은 과제 의도와 공격면 때문에
채택하지 않았습니다. Claude는 별도의 트러블슈팅 역할로 빌드·라이브러리 경고를 확인합니다.

## ⑥ 아직 못 막은 것

- 공개 등록에는 초대나 관리자 승인이 없어 자동 계정 생성을 악용할 수 있습니다.
- 모든 패스키 기기를 잃으면 비밀번호·이메일 복구가 없어 계정을 되찾을 수 없습니다.
- 동기화형 패스키가 sign counter를 항상 0으로 보고하면 복제 의심 신호를 얻기 어렵습니다.
- Supabase 세션 저장소가 중단되면 로그인 상태를 확인할 수 없어 비공개 접근도 실패합니다.
- 실제 브라우저·기기 조합과 HTTPS PostgreSQL 배포는 아직 최종 확인 전입니다.
