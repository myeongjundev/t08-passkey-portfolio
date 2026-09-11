# 증거 화면

과제 제출용 발표자료(`output/presentation/T08-evidence.pptx`)와 최종 제출 문서
(`output/pdf/T08-final-submission.pdf`)에 들어간 원본 화면이다. 2026-09-11 캡처.

## 어디서 찍었나

| 파일 | 어디 | 무엇 |
| --- | --- | --- |
| `01-public-first-screen.png` | 프로덕션 | 등록 없이 열리는 공개 첫 화면. PASSKEY ONLY 입구 |
| `02-public-private-boundary.png` | 프로덕션 | 공개가 끝나는 자리의 05 섹션 |
| `03-access-no-password-field.png` | 프로덕션 | 등록 화면. 비밀번호 입력 칸이 없다 |
| `06-error-page.png` | 프로덕션 | 오류 화면. Whitelabel이 아닌 제 화면 |
| `07-private-after-registration.png` | 로컬 | 등록 직후 비공개 자리 전체 |
| `08-two-passkeys.png` | 로컬 | 패스키 두 개와 각각의 저장 위치 배지 |
| `09-after-deleting-first.png` | 로컬 | 하나를 지운 뒤 |
| `10-last-passkey-refused.png` | 로컬 | 마지막 하나 삭제 거절 |
| `11-login-with-remaining-passkey.png` | 로컬 | 남은 패스키로 재로그인 성공 |
| `c-badges.png` · `c-last-refused.png` | 로컬 | 위 화면에서 목록 부분만 잘라낸 것 |

**공개 화면은 프로덕션에서 읽기만 했고, 로그인이 필요한 화면은 로컬 인스턴스에서 찍었다.**
로컬 쪽은 CDP 가상 인증기로 등록·추가·삭제·로그인을 실제로 수행한 결과이며, 프로덕션
데이터베이스에는 아무것도 쓰지 않았다. 실물 기기 확인은 따로 했고 기록은
`../12-real-device-registration.md`에 있다.

두 번째 패스키를 일부러 보안 키 transport로 등록했다. 저장 위치 표시가 `동기화됨 · 이 기기`와
`보안 키`로 갈리는 것을 한 화면에서 보이기 위해서다.

## 담긴 내용

계정 이름과 비공개 자료는 전부 합성값이다. 실제 개인정보는 없다. 세션·CSRF·challenge·서명
값은 화면에 나타나지 않는다.
