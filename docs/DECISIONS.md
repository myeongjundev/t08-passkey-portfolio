# Decision Log

Append-only. Each entry records the decision, the reasoning, and what would reverse
it. Do not delete entries; supersede them with a later one.

---

## D-001 · Build T08 as a new repository, not a commit on `myeongjundev.github.io`

**Status:** decided · 2026-09-07

T08-C11 requires the result to continue the T01 intro page with its public content
intact. T08 also requires a server that generates and holds WebAuthn challenges,
verifies signatures, and gates private data behind 401/403 (R3–R5). GitHub Pages
serves static files only and cannot do any of that.

So T08 lives in a new repository that vendors the T01 public page under `public/` and
serves it from an application server. `myeongjundev.github.io` stays as the T01
deliverable, untouched.

**Reverses if:** the assignment turns out to require the same URL as T01, in which
case the T01 repo would need to become a redirect or the domain would need to point
at the T08 deployment.

---

## D-002 · Vendor the T01 page rather than iframe or fetch it

**Status:** decided · 2026-09-07

`public/` holds a copy of `index.html`, `script.js`, `styles.css`, `favicon.svg`,
`assets/`, and `fonts/` taken from `myeongjundev/myeongjundev.github.io` at commit
`0ec47fc79e4e6a57382d9d4e6ef4413725e6c135`.

An iframe or a runtime fetch would make the public first screen depend on another
origin being up, and would complicate proving T08-C18 (no private content in the
unauthenticated source). A vendored copy makes the served HTML directly inspectable.

**Cost:** T01 changes do not propagate. If T01 is edited, re-vendor and note the new
commit here.

---

## D-003 · Criteria T08-C04 – T08-C09 are unknown and must be reconciled

**Status:** open · 2026-09-07

The assignment card images under `docs/task-source/` cover C01–C03, C10–C53. The
range C04–C09 does not appear in any captured image. Judging by their position among
the submission-URL criteria, they are likely further general submission rules.

**Action:** check the official assignment page and fill
`docs/T08-ACCEPTANCE-MATRIX.md` before final submission. Do not assume they are
satisfied.

---

## D-004 · Implementation stack — Spring Boot 4.1.1 + webauthn4j, Java 25

**Status:** decided · 2026-09-07

| Piece | Choice |
| --- | --- |
| Language / runtime | Java 25 (LTS) |
| Framework | Spring Boot 4.1.1, Spring MVC |
| View | Thymeleaf (server-side rendering) |
| WebAuthn | `com.webauthn4j:webauthn4j-core:0.29.1.RELEASE` |
| Persistence | Spring Data JPA — H2 in dev, PostgreSQL in production |

Reasoning, which also answers T08-C48 (② why that choice):

- **Java · Spring Boot is the author's primary stack.** T08 is a portfolio-visible
  piece; building it in the stack being presented is worth more than reusing T07's
  Flask setup.
- **webauthn4j is the mature JVM WebAuthn library** and is FIDO-conformance tested.
  Implementing signature verification, attestation parsing, and challenge validation
  by hand would be the wrong kind of ambition for a task graded on whether the lock
  actually holds.
- **Thymeleaf, not a JSON API plus a client-side framework.** T08-C18 requires that
  an unauthenticated page response contain no private content anywhere in its source.
  Server-side rendering makes that a property of the code rather than something to
  test for afterwards. The T01 page is plain HTML with no build step, so it drops
  into a Thymeleaf template unchanged.
- **H2 in dev, PostgreSQL in production** mirrors the T07 deployment path.

**Layout:** the Spring Boot project sits at the repository root. The vendored T01
page splits into `src/main/resources/templates/index.html` (the rendered page) and
`src/main/resources/static/` (css, js, fonts, images).

**Verified:** `./gradlew build` passes and `./gradlew bootRun` serves the T01 page at
`http://localhost:8080/` with `styles.css` and `script.js` resolving (200).

---

## D-006 · No Spring Security

**Status:** decided · 2026-09-07

`webauthn4j-core` is used directly rather than `webauthn4j-spring-security`, and
Spring Security is not on the classpath at all.

- Spring Security's default configuration serves a **form login page with a password
  field**. T08-C35 requires that no password input exists anywhere in the
  deliverable. Leaving that default on would fail the task outright, and carrying a
  dependency whose defaults contradict the assignment is a standing risk.
- T08-C41 requires the submission to point at the source location that produces the
  authorization rejections. A plain `HandlerInterceptor` guarding the private routes
  is a single file to point at; a Spring Security filter chain is not.
- The authorization model here is trivial — a session either carries a verified
  passkey identity or it does not.

**Cost:** session fixation protection, CSRF tokens, and security headers must be
handled deliberately instead of arriving by default. Track this; it is a strong
candidate for the "what is still not locked down" section (T08-C51).

**Reverses if:** the private area grows roles or shared access, at which point
hand-rolled authorization stops being the simpler option.

---

## D-005 · Two accounts without passwords

**Status:** open · 2026-09-07

R7 requires two separate accounts, but T08 forbids passwords, so there is no
conventional signup. Options: registration creates an account keyed only by a
user handle plus the first passkey; or accounts are pre-seeded and a passkey is
attached to each.

Whatever is chosen must keep the public first screen open to anyone with no
registration at all (T08-C10).

---

## D-007 · A successful first passkey registration creates the account

**Status:** decided · 2026-09-07 · supersedes the open part of D-005

There is no pre-seeded account and no account row before WebAuthn registration
passes. The server creates an opaque 32-byte user handle and a pending ceremony;
after WebAuthn4J verifies the response, one transaction creates the account, its
first credential and three synthetic private items.

The user supplies only a synthetic display label and a passkey nickname. There is
no email, phone number, invite code or password. A cancelled or failed ceremony
leaves no account or credential row. A second test account follows the same flow.

**Reason:** pre-seeded empty accounts would let the first visitor claim them, while
an invite code would become another shared secret and conflict with the public URL
requirements. Open passkey-first registration is the smallest complete passwordless
bootstrap and makes the two-account isolation test reproducible.

**Cost:** public registration can be abused to create synthetic accounts. Bound
active ceremonies and request sizes now; record stronger enrollment control as a
known limitation rather than disguising an invite or password as a passkey flow.

---

## D-008 · Use discoverable credentials and require user verification

**Status:** decided · 2026-09-07

Registration uses `residentKey=required`, `requireResidentKey=true` and
`userVerification=required`. Authentication leaves `allowCredentials` empty. The
credential ID and opaque `userHandle` returned by the authenticator identify the
account after server verification.

**Reason:** the user can sign in without first typing an account identifier, and
the public authentication options do not reveal registered credential IDs. W3C
defines this as the discoverable-credential flow.

**Cost:** authenticators without discoverable credentials or local user verification
cannot use the private area. The screen must explain that limitation without adding
a password fallback.

---

## D-009 · Persist and consume WebAuthn ceremonies in the database

**Status:** decided · 2026-09-07

Every registration and authentication options request creates a database ceremony
with a 32-byte server-generated challenge, a five-minute expiry and a kind. The
first finish attempt consumes it under a row lock in a separate transaction before
cryptographic verification. Failed verification does not make the challenge usable
again.

**Reason:** a browser-only challenge would not prevent replay. A database row makes
expiry, kind, cancellation and concurrent replay testable and keeps the rule valid
if the application process restarts.

**Cost:** a transient table and cleanup job are required. Raw challenges must never
appear in committed evidence; compare redacted fingerprints instead.

---

## D-010 · Use server HttpSession with explicit Origin and CSRF guards

**Status:** decided · 2026-09-07

After assertion verification the server invalidates the anonymous session and
creates a new session containing only `SessionPrincipal(accountId)` and a fresh CSRF
token. `JSESSIONID` is HttpOnly, SameSite=Lax and Secure in production. Every state
change, including pre-login ceremonies, requires configured Origin, JSON content
type and the session CSRF header.

**Reason:** T08 asks the submission to say whether a session or token identifies the
user. A server session is smaller than a custom JWT lifecycle and can be invalidated
immediately on logout. D-006 excluded Spring Security, so fixation protection, CSRF
and cookie attributes must be explicit application responsibilities.

**Cost:** the default in-process session disappears on a Render restart and the user
must sign in again. That is acceptable for this single-instance assignment and is
documented as a limitation.

---

## D-011 · Refuse deletion of the final passkey

**Status:** decided · 2026-09-07

A credential can be deleted only when its account has at least two. Deleting the
last one returns 409 and the screen says to register another passkey first. Losing
access to every registered authenticator still leaves no recovery route and is
stated on the same screen and in the submission.

**Reason:** deliberately creating an unreachable account to demonstrate the zero-key
case would be an unsafe product rule. The rejection gives T08-C46 a visible,
testable behavior while preserving the two-passkey recovery exercise.

**Reverses if:** the assignment's missing official text explicitly requires a stored
zero-credential account rather than asking what the product does in that case.

---

## D-012 · Keep private rendering and ownership on the server

**Status:** decided · 2026-09-07

`/private` is server-rendered only after an authentication interceptor accepts the
session. Private repositories require account ID and controllers obtain it only
from the session principal. URL or JSON account IDs never select ownership. A
missing session is 401; a resource outside the current account is 404.

**Reason:** this makes T08-C15 through C18 and C36 through C41 properties of the
server response and query, rather than CSS or client state.

---

## D-013 · Use WebAuthn4J for verification and keep ceremony ownership local

**Status:** decided · 2026-09-07

WebAuthn4J verifies registration and authentication objects, expected origin,
RP ID, challenge, user presence, user verification and signatures. Application code
owns HTTP JSON, challenge lifetime, credential persistence, session creation,
authorization and evidence redaction, matching the library's documented scope.

The repository pins 0.31.8.RELEASE after a 2026-09-07 compatibility spike against
Spring Boot 4.1.1 and its Jackson 3.1.5 dependency. The full build and a minimal
registration/authentication parsing adapter loaded successfully. Keep verification
behind that adapter and do not implement signature verification manually.

---

## D-014 · Carry T07's security method into T08, not its credentials

**Status:** decided · 2026-09-07

T08 uses the same SKT ALeph method as T07: explicit trust boundaries, server-side
authorization, paired success/rejection evidence, central redaction, persistent
rate/replay state, TLS-only production data paths and honest remaining-risk notes.
It does not copy T07's password, JWT or refresh-token design. Those are replaced by
WebAuthn public-key verification, database-held single-use challenges and an opaque
server session.

**Reason:** copying credential machinery would add forbidden password fields and
unneeded bearer secrets. Reusing the threat and verification discipline preserves
the useful security work while keeping T08 genuinely passkey-only.

---

## D-015 · Deploy the Java container on Vercel with Supabase PostgreSQL

**Status:** decided · 2026-09-07 · supersedes the Render/Neon deployment target and
the in-process-session cost recorded in D-010

Vercel detects `Dockerfile.vercel` and runs the Spring Boot Java 25 image as a
stateless container function. Supabase supplies the persistent PostgreSQL database
through its TLS Session pooler on port 5432; the transaction pooler is excluded
because Hibernate uses prepared statements.

Because Vercel container instances can scale down or serve different requests, the
default in-process `HttpSession` is no longer adequate. The production profile uses
Spring Session JDBC, and Flyway owns the two session tables. Only the opaque session
ID remains in the Secure, HttpOnly, SameSite cookie. Account identity, anonymous
ceremony owner and CSRF value are server-side database attributes.

**Cost:** application data and session availability now share the Supabase failure
domain. Vercel preview domains cannot use production passkeys because WebAuthn is
bound to the exact configured origin; only the canonical production domain is a
submission target.
