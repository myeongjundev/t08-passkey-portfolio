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

## D-004 · Implementation stack

**Status:** open · 2026-09-07

Not yet decided. Constraints that bear on it:

- The T01 page is plain HTML/CSS/JS with no build step, so no frontend framework is
  required by the existing code.
- T07 (`t07-plando-see-diary`) used Flask 3 + SQLAlchemy + PostgreSQL on Render with
  Neon. Reusing that stack means the deployment path is already proven.
- WebAuthn needs a maintained server-side verification library. Candidates:
  `py_webauthn` (Python), `webauthn4j` (Java/Spring), `@simplewebauthn/server` (Node).
- The author's stated focus is Java · Spring Boot · React.

Record the choice here with reasoning before writing server code — T08-C48 requires
the submission to state what was used and why.

---

## D-005 · Two accounts without passwords

**Status:** open · 2026-09-07

R7 requires two separate accounts, but T08 forbids passwords, so there is no
conventional signup. Options: registration creates an account keyed only by a
user handle plus the first passkey; or accounts are pre-seeded and a passkey is
attached to each.

Whatever is chosen must keep the public first screen open to anyone with no
registration at all (T08-C10).
