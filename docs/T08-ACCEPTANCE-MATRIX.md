# T08 Acceptance Matrix

Fixed checklist transcribed from the assignment cards. **Do not edit the criterion
text.** Only `Status` and `Evidence` columns change as work lands.

Status values: `todo` · `wip` · `done` · `blocked`

Evidence paths are relative to `docs/evidence/`.

---

## Card 1 — Split public from private

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C13 | 공개 영역과 비공개 영역이 화면에서 구분되어 보인다. | todo | |
| T08-C14 | 비공개 영역에 들어 있는 항목이 세 개 이상이다. | todo | |
| T08-C15 | 패스키로 들어가지 않은 상태에서는 비공개 영역의 내용이 화면에 보이지 않는다. | todo | |
| T08-C16 | 패스키로 들어가지 않은 상태에서 비공개 자료를 서버에 직접 요청하면 거절된다. | todo | |
| T08-C17 | 그 거절이 401 또는 403으로 이루어진다. | todo | |
| T08-C18 | 로그인하지 않은 상태로 받은 페이지의 소스 어디에도 비공개 내용이 들어 있지 않다. | todo | |

## Card 2 — Register a passkey

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C19 | 서버가 등록용 질문(challenge)을 만들어 보내고, 그 값을 서버가 확인할 때까지 보관한다. | todo | |
| T08-C20 | 등록 요청마다 질문 값이 서로 다르다는 기록이 제출문에 있다. | todo | |
| T08-C21 | 등록이 끝나면 서버에 공개키가 저장된다. | todo | |
| T08-C22 | 서버에 저장된 값이 제출문에 있고, 그 값이 공개키이며 비밀번호가 아니라는 설명이 함께 적혀 있다. | todo | |
| T08-C23 | 개인키가 서버로 전송되지 않는다는 사실이, 등록 요청 본문을 적은 기록으로 확인된다. | todo | |
| T08-C24 | 등록한 패스키에 사람이 알아볼 수 있는 이름이 붙는다. | todo | |
| T08-C25 | 등록을 중간에 취소하면 화면에 안내가 나오고, 서버에 아무것도 저장되지 않는다. | todo | |
| T08-C26 | 패스키를 저장한 곳이 어디인지(구글 비밀번호 관리자·기기 자체·보안 키 중 무엇인지)가 제출문에 적혀 있다. | todo | |

## Card 3 — Sign in with a passkey

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C27 | 로그인할 때도 서버가 매번 새 질문을 만들어 보낸다. | todo | |
| T08-C28 | 로그인 요청마다 질문 값이 서로 다르다는 기록이 제출문에 있다. | todo | |
| T08-C29 | 서버가 저장해 둔 공개키로 서명을 확인한 뒤에만 통과시킨다. | todo | |
| T08-C30 | 서명 확인에 성공한 요청과 실패한 요청이 나란히 적혀 있다. | todo | |
| T08-C31 | 이미 한 번 쓴 질문으로 다시 로그인하려는 요청과 그 거절 응답이 적혀 있다. | todo | |
| T08-C32 | 로그인 뒤 무엇으로 사람을 알아보는지(세션·토큰 중 무엇인지)가 제출문에 적혀 있다. | todo | |
| T08-C33 | 로그아웃한 뒤 같은 값으로 다시 요청했을 때의 거절 응답이 적혀 있다. | todo | |
| T08-C34 | 적어 둔 기록에서 세션·토큰 값이 가려져 있다. | todo | |
| T08-C35 | 제출물 어디에도 비밀번호를 입력하는 칸이 없다. | todo | |

## Card 4 — Losing a device

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C42 | 한 계정에 패스키가 두 개 등록되어 있다. | todo | |
| T08-C43 | 등록된 패스키 목록을 화면에서 볼 수 있고, 각각의 이름과 등록한 날짜가 보인다. | todo | |
| T08-C44 | 패스키 하나를 지운 뒤 남은 하나로 들어갈 수 있다. | todo | |
| T08-C45 | 지운 패스키로는 더 이상 들어갈 수 없다는 기록이 적혀 있다. | todo | |
| T08-C46 | 패스키가 하나도 남지 않았을 때 어떻게 되는지가 화면과 제출문에 적혀 있다. | todo | |

## Card 5 — Submission, isolation checks, and the write-up

### Submission URLs and public surface

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C01 | 결과물 URL 필드에 HTTPS URL 한 개가 제출되어 있다. | todo | |
| T08-C02 | 소스 URL 필드에 HTTPS URL 한 개가 제출되어 있다. | todo | |
| T08-C03 | 제출한 모든 URL(결과물·소스)은 계정 생성·로그인·인증·초대·비밀번호·OAuth·CAPTCHA 없이 새 시크릿 창에서 열린다. | todo | |
| T08-C10 | 결과물 주소의 첫 화면은 공개 소개 페이지이고, 심사하는 사람이 아무것도 등록하지 않아도 열린다. | todo | |
| T08-C11 | 1번 과제에서 만든 소개 페이지에 이어 붙였고, 1번의 공개 내용이 그대로 남아 있다. | todo | |
| T08-C12 | 제출물에 실제 개인정보가 없고, 넣은 내용이 만들어 넣은 것이라는 사실이 적혀 있다. | todo | |

> **T08-C04 – T08-C09 are not captured in the source images.** They are presumably
> further general submission rules. Confirm against the official assignment page
> before final submission and fill them in here. See `docs/DECISIONS.md` D-003.

### Cross-account isolation

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C36 | 패스키를 등록한 계정이 두 개이고, 각각에 서로 다른 비공개 내용이 들어 있다. | todo | |
| T08-C37 | 한쪽 패스키로 들어가 다른 쪽의 비공개 자료를 읽으려는 요청과 그 거절 응답이 적혀 있다. | todo | |
| T08-C38 | 반대 방향(다른 쪽에서 첫 쪽으로)의 요청도 똑같이 거절된 기록이 적혀 있다. | todo | |
| T08-C39 | 거절 앞뒤로 반대편의 자료 건수가 같다는 기록이 적혀 있다. | todo | |
| T08-C40 | 주소나 요청 본문에 다른 계정을 적어 보낸 요청, 그래도 내 자료만 돌아온 응답이 적혀 있다. | todo | |
| T08-C41 | 위 거절을 만들어 내는 소스의 위치가 제출문에 적혀 있다. | todo | |

### Write-up

| ID | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| T08-C47 | 인증 구현 설명서의 여섯 항목(① 무엇으로 붙였나, ② 왜 그걸 골랐나, ③ 어디를 어떻게 고쳤나, ④ 안 열리는 것을 확인한 기록, ⑤ AI와 나, ⑥ 아직 못 막은 것)이 각각 나뉘어 적혀 있다. | todo | |
| T08-C48 | ①에 직접 구현·라이브러리·인증 서비스 중 무엇을 썼는지와 그 이름이 적혀 있다. | todo | |
| T08-C49 | ③에 등록·로그인·로그아웃·비공개 자료 조회 네 흐름이 소스의 어디를 지나는지 적혀 있다. | todo | |
| T08-C50 | ④에 확인 네 가지(로그인 없이 열기, 남의 패스키로 열기, 이미 쓴 질문 재사용, 패스키 삭제 뒤 로그인)가 모두 있고, 각각 성공한 요청과 거절된 요청이 나란히 적혀 있다. | todo | |
| T08-C51 | ⑥에 아직 못 막은 것이 최소 하나 구체적으로 적혀 있다. 없다고만 적으면 통과하지 않는다. | todo | |
| T08-C52 | 짧은 확인 방법에 ① 어디로 가나요, ② 세 단계 안에 무엇을 하나요, ③ 무엇이 보이면 통과인가요, ④ 안 될 때는 무엇이 보이나요가 각각 나뉘어 적혀 있다. | todo | |
| T08-C53 | 제출문에 ① AI에게 맡긴 일, ② 내가 직접 판단한 일, ③ AI 제안을 따르지 않은 일(없다면 왜 없었는지)이 각각 나뉘어 적혀 있다. | todo | |

---

## Completion checklist (과제 완주 체크리스트)

- [ ] 공개 영역과 비공개 영역을 화면에서 갈라 두었습니다.
- [ ] 패스키를 등록했고, 서버에 저장된 것이 공개키라는 것을 보였습니다.
- [ ] 매번 새 질문이 오고, 이미 쓴 질문은 다시 통하지 않는 것을 확인했습니다.
- [ ] 패스키를 두 개 등록해 하나를 지운 뒤에도 들어갔습니다.
- [ ] 남의 패스키로는 열리지 않는 것을 요청과 응답으로 남겼습니다.
- [ ] 제출물 어디에도 실제 개인정보와 비밀값이 없습니다.

## Deliverables (제출물)

- [ ] 공개 결과물 주소 (HTTPS)
- [ ] 소스 주소 (HTTPS)
- [ ] 인증 구현 설명서 여섯 항목 → `docs/T08-AUTH-GUIDE.md`
- [ ] 짧은 확인 방법 4줄 → `docs/T08-SUBMISSION.md`
- [ ] AI와 내 판단 3줄 → `docs/T08-SUBMISSION.md`
