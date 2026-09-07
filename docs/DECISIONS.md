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
