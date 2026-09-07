# T08 Requirements

Source of truth: `docs/T08-TASK.md` (full transcription of the assignment cards) and
`docs/T08-ACCEPTANCE-MATRIX.md` (the fixed checklist). This file states the shaped
requirements the implementation must satisfy.

## Reconciliation status

⚠️ **Preliminary.** The requirements below are transcribed from the assignment card
images under `docs/task-source/`. Criteria **T08-C04 – T08-C09 are missing from those
images.** Before implementation is called complete, reconcile against the official
assignment page and fill the gap in the acceptance matrix.

## R1 · Public surface is unchanged and stays open

- The first screen of the deployed result is the T01 intro page.
- T01's public content is carried over intact. The T01 source is vendored under
  `public/` from `myeongjundev/myeongjundev.github.io` at commit `0ec47fc`.
- It opens in a fresh incognito window with no account, login, invite, password,
  OAuth, or CAPTCHA. (T08-C03, T08-C10, T08-C11)

## R2 · A private area exists and is visibly separate

- A newly added private area holds **at least three** items — synthetic content such
  as project notes, a target-companies list, or personal retrospectives. (T08-C14)
- The boundary between public and private is legible on screen at a glance.
  (T08-C13)
- Real personal data is never used. The submission states that the content is
  synthetic. (T08-C12)

## R3 · Private content is server-gated

- Hiding is done by the server, not by CSS or client-side routing.
- An unauthenticated page response contains **no private content anywhere in its
  source**. (T08-C15, T08-C18)
- A direct unauthenticated request to a private data endpoint is rejected with
  **401 or 403**. (T08-C16, T08-C17)

## R4 · Passkey registration (WebAuthn `navigator.credentials.create`)

- The server generates a registration challenge and holds it until it verifies the
  response. Each registration request gets a different challenge value.
  (T08-C19, T08-C20)
- On success the server stores the **public key** — never a password, never a private
  key. The registration request body is captured as proof that no private key is
  transmitted. (T08-C21, T08-C22, T08-C23)
- Each passkey carries a human-readable nickname. (T08-C24)
- Cancelling mid-registration shows a message and stores nothing server-side.
  (T08-C25)
- The submission records where the passkey was stored: Google Password Manager, the
  device itself, or a security key. (T08-C26)

## R5 · Passkey sign-in (WebAuthn `navigator.credentials.get`)

- Every sign-in gets a freshly generated, server-held challenge; values differ per
  request. (T08-C27, T08-C28)
- The server verifies the signature against the stored public key before granting
  access. (T08-C29)
- Replaying an already-used challenge is rejected, and the rejection is recorded.
  (T08-C31)
- The submission states what identifies the user after sign-in — session or token.
  (T08-C32)
- After logout, the same credential value is rejected. (T08-C33)
- Session and token values are redacted in all committed records. (T08-C34)
- **There is no password input anywhere in the deliverable.** (T08-C35)

## R6 · Account recovery via a second passkey

- One account holds **two** registered passkeys. (T08-C42)
- A management screen lists them with nickname and registration date. (T08-C43)
- After deleting one, the remaining passkey still signs in; the deleted one no longer
  does. (T08-C44, T08-C45)
- What happens when zero passkeys remain is shown on screen and stated in the
  submission. (T08-C46)

## R7 · Cross-account isolation

- **Two** accounts each register a passkey and each hold different private content.
  (T08-C36)
- Signed in as account A, reading account B's private data is rejected — and the
  reverse direction is rejected identically. (T08-C37, T08-C38)
- The other account's record count is unchanged before and after the rejection.
  (T08-C39)
- Naming another account in the URL or request body still returns only the caller's
  own data. (T08-C40)
- The submission points at the source location that produces these rejections.
  (T08-C41)

## R8 · Written deliverables

| Document | Contents | Criteria |
| --- | --- | --- |
| `docs/T08-AUTH-GUIDE.md` | Six sections: ① what it was built with, ② why that choice, ③ what was changed where, ④ the lock-verification records, ⑤ AI and me, ⑥ what is still not locked down | C47 – C51 |
| `docs/T08-SUBMISSION.md` | The 4-line verification recipe and the 3-line AI/judgement split, plus both HTTPS URLs | C01, C02, C52, C53 |

- ③ must trace all four flows — register, sign in, sign out, private-data read —
  through the source. (T08-C49)
- ④ must contain all four checks, each as a paired passing/blocked request:
  open without login · open with someone else's passkey · reuse a spent challenge ·
  sign in after passkey deletion. (T08-C50)
- ⑥ must name **at least one concrete** remaining weakness. "Nothing" does not pass.
  (T08-C51)

## R9 · SKT ALeph security and network baseline

- Production has one canonical HTTPS origin. WebAuthn expected origin and RP ID come
  from trusted configuration, never from request forwarding headers.
- Every state-changing endpoint checks JSON content type, exact Origin and session
  CSRF. Passkey challenges are cryptographically random, database-held, short-lived
  and consumed once even when verification fails.
- Private repositories select ownership only from the server session account ID.
- Session cookies are Secure, HttpOnly and SameSite=Lax in production; authentication
  rotates the session ID and logout invalidates it server-side.
- Render connects to Neon with PostgreSQL TLS. Secrets remain server environment
  variables and never enter source, frontend bundles, responses or committed evidence.
- Security events and evidence share central redaction. Raw challenge, credential
  JSON, signature, session/CSRF value, DB secret and original IP are prohibited.
- Response headers, rate limits, payload limits, negative authorization paths and
  secret-pattern audits are automated gates, not final manual checks.

Detailed control matrix: `docs/T08-SECURITY-NETWORK.md`.

## Non-goals

- No password authentication of any kind.
- No collection of real contact details or ID numbers.
- No change to T01's public content beyond adding the entry point to the private
  area.
