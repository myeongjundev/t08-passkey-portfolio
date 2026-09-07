# Status

**Last updated:** 2026-09-08

> 학원에서 이어서 작업할 때는 `docs/process/HANDOFF-2026-09-08-ACADEMY.md`부터
> 읽으세요. 실행 순서, 완료 조건, 복구 절차가 최신 상태로 정리돼 있습니다.

> **Current deployment:** healthy at `https://t08-passkey-portfolio.vercel.app`.
> See `docs/evidence/11-production-startup.md`. Physical passkey verification,
> C26, the author's C53 wording and missing official criteria C04–C09 remain.

## 2026-09-08 — Vercel production deployment healthy

Commit `eebe09e` adds an opt-in production startup boundary: Tomcat reserves port 80
early, but a Valve returns only fixed 503/no-store until Spring publishes
`ApplicationReadyEvent`. The first cold request received this safe 503; five seconds
later the complete deployment script passed health, public/private boundaries,
fresh challenge issuance, cancellation, Origin and CSRF checks against the canonical
HTTPS URL.

The full JDK 25 build passes with **38 tests, 0 failures, 0 errors, 0 skipped**.
T08-C01, C03, C10, C11 and C52 are now done. Remaining captured criterion: C26,
which requires an actual device/provider observation. C04–C09 still require the
official assignment source, and the author must confirm the personal C53 wording.

## 2026-09-07 evening — deployment resumed and tests re-run

Prepared the official Temurin JDK 25 outside the repository, verified its download
checksum and ran `gradlew.bat clean test build --console=plain`: **37 tests passed,
0 failures, errors or skips**. The previous local JDK blocker is resolved.

Changed the existing Vercel Production `PORT=8080` to `80` and redeployed `abede2c`.
The image builds, Java starts as `appuser` and Supabase connects successfully, but
Flyway startup exceeds Vercel's 15-second container startup window before an HTTP
listener opens. This is new evidence; the earlier port-only diagnosis is incomplete.

The function was running in Washington (`iad1`) while Supabase is in Seoul.
Changed the function region to Seoul (`icn1`) and started a second deployment.
HTTP verification is pending. Evidence: `docs/evidence/11-production-startup.md`.

Remaining work: obtain a healthy public deployment, verify physical passkeys and
storage provider, reconcile C04–C09 with the official source, and have the author
confirm the personal-judgement section. No acceptance criterion was marked done
solely because a container build passed.

## 2026-09-07 Vercel container boot fixed in three steps

Deployment stalled on container startup, not on application code. Three causes,
each fixed and verified locally before pushing:

1. `cfe44c0` — `exec: "java": executable file not found in $PATH`. Vercel starts the
   container with its own environment, so the base image PATH was gone. ENTRYPOINT now
   uses the absolute path `/opt/java/openjdk/bin/java`.
2. `0debc37` — moved to port 8080 on the theory that Vercel honours the `PORT`
   variable. It does not; the proxy kept routing to 80. Superseded.
3. `2cdd299` — granted the JVM launcher `cap_net_bind_service` so the non-root image
   can bind port 80, rather than running as root. Verified with
   `net.ipv4.ip_unprivileged_port_start=1024` enforced so the bind could not succeed
   by accident: `uid=10001`, `Tomcat started on port 80`, `/health` → 200.

**Pending manual step:** the `PORT=8080` Vercel environment variable added during
step 2 must be deleted before redeploying, or the app will listen on 8080 while
Vercel routes to 80 again.

Supabase is provisioned in Seoul with the Data API disabled, and the production
domain `t08-passkey-portfolio.vercel.app` matches the configured RP ID and origin.
The database connection has not been exercised yet — that is the next gate after
the container boots.
**Phase:** Vercel + Supabase deployment configuration and persistent sessions ready.

## 2026-09-07 duplicate passkey nickname no longer returns 500

Review of the Cards 1-5 implementation found one defect. `issueOptions` did not
check the per-account nickname uniqueness constraint, so a second passkey reusing
the first one's nickname failed only at `save()` — after the browser ceremony had
already created the credential on the device — as an unhandled
`DataIntegrityViolationException`, i.e. HTTP 500.

The nickname is now checked before any ceremony is issued, re-checked at finish,
and answered with 409 `nickname_taken`; the manage screen names the collision.
`DataIntegrityViolationException` maps to 409 `conflict` as a safety net. D-016
records the reasoning.

Verified by removing the pre-check and watching the new HTTP test fail with
`expected 409 but was 200` at the options call. Full build: **37 tests, 0
failures, 0 errors, 0 skipped**; `manage-passkeys.js` passes syntax check.

No acceptance criterion changed status: C42 and C43 already passed with distinct
nicknames. This was quality of the graded flow, not a gate.

The review also re-verified, live rather than from claims: 35 (now 37) tests pass;
`/private`, `/api/private-items` and `/api/passkeys` return 401 unauthenticated;
three consecutive challenges differ; a consumed ceremony is rejected on replay;
missing/foreign CSRF, wrong Origin and non-JSON bodies are refused (403/403/415);
the T01 public diff is additive only; and no secret is committed.

## 2026-09-07 Vercel and Supabase target adopted

Superseded the Render/Neon deployment target with Vercel container functions and
Supabase PostgreSQL. Added `Dockerfile.vercel`, removed `render.yaml`, and rewrote the
deployment handoff with the Supabase Session pooler TLS settings. D-015 records the
decision and its security trade-offs.

Vercel containers are stateless, so the production profile now replaces in-process
`HttpSession` with Spring Session JDBC. Flyway V2 creates the two session tables and
`SessionPrincipal` supports safe Java serialization. The focused production-profile
test persisted and restored the synthetic principal, CSRF value and ceremony-owner
bytes successfully. Evidence: `docs/evidence/10-vercel-supabase-session-readiness.md`.

The complete build passed with 35 tests. `Dockerfile.vercel` built successfully and
its production-profile container returned 200 from `/health` and `/access`, issued
the opaque `SESSION` cookie, and ran as UID 10001. The local verification container
was stopped afterward.

Next external work: create the Supabase project, enter its Session pooler values in
Vercel, deploy the canonical production URL and perform physical passkey verification.
C26 and the deployment URL criteria remain pending; C04 through C09 still require
the missing official assignment source.

## 2026-09-07 source published and deployment handoff prepared

Committed the complete Cards 1–5 implementation as `05a85ae` and pushed `main` to
the public HTTPS repository. The source URL is now fixed in `T08-SUBMISSION.md`, so
T08-C02 is done.

Added a Java 25 multi-stage Docker build, a non-root runtime, a Render Blueprint and
strict production properties. Production now requires PostgreSQL credentials plus
the exact WebAuthn RP ID and HTTPS origin instead of silently using local H2 and
localhost. `/health` checks database readiness without returning connection details.
`T08-DEPLOYMENT.md` records the secret-free setup and physical passkey verification
sequence.

Verification passed with 34 tests and no failures or errors. The Docker image also
built successfully, ran as UID 10001 rather than root, and returned 200 with
`{"status":"UP"}` from its database-backed health check. Evidence:
`docs/evidence/09-public-source-and-deployment-readiness.md`.

Remaining external work: connect Render to PostgreSQL, enter deployment-only
environment values, verify the public HTTPS origin and record the physical passkey
storage provider for T08-C26. The deployed URL is still required for C01, C03, C10,
C11 and completion of C52. C04 through C09 still require the missing official source.

## 2026-09-07 cross-account isolation and write-up complete

Two synthetic accounts now receive visibly distinct private bodies. Each account
registered and authenticated with a separate P-256 passkey before the isolation
checks. A→B and B→A direct item requests both returned the same 404, total row count
stayed 6 before and after, and an A request carrying B's account ID still returned
only A's three rows.

Added `docs/evidence/08-cross-account-isolation.md`, the six-section
`docs/T08-AUTH-GUIDE.md`, and `docs/T08-SUBMISSION.md` with the four-line verification
recipe and AI/judgement split. T08-C36 through C41, C47 through C51 and C53 are done.
C52 remains wip until its destination is replaced with the deployed URL.

Verification: full build passed with 33 tests, 0 failures, 0 errors and 0 skipped.
JavaScript syntax and diff whitespace checks passed. A focused scan found zero
password-input, password-hash or PEM private-key patterns under `src/` and `docs/`.
Remaining external gates are the actual passkey storage-provider note (C26), public
GitHub/result HTTPS URLs, production PostgreSQL/HTTPS verification, and reconciliation
of the missing official criteria C04 through C09.

Troubleshooting note for Claude: the first combined PowerShell secret-scan command
failed before execution because nested quote characters were parsed incorrectly.
It was split into a build command and a fixed-pattern scan; both then passed. No
application change was needed.

## 2026-09-07 Card 4 second-passkey recovery complete

The authenticated private screen now lists passkeys by human-readable nickname and
registration date and can register a second discoverable credential. Existing
credential IDs are placed in `excludeCredentials`. All management APIs are behind
the authenticated session plus the shared Origin, JSON and CSRF checks.

Deletion locks every credential row for the session account, hides cross-account
targets as 404 and refuses to remove the final credential with 409. Tests register
two independent P-256 credentials, delete the first, authenticate with the survivor,
reject an assertion from the deleted credential, and verify the final credential
and private data remain intact.

Verification: `./gradlew.bat clean test build` passed with 32 tests, 0 failures,
0 errors and 0 skipped. All three passkey/session browser scripts passed syntax
checking. Evidence: `docs/evidence/07-second-passkey-recovery.md`. T08-C42 through
C46 are done.

Next target: T08-C36 through C41 cross-account isolation, followed by the two final
submission documents. T08-C26 still needs the actual device/provider name from a
manual physical passkey registration.

## 2026-09-07 Card 3 authentication and logout complete

Added username-less authentication options and browser `navigator.credentials.get()`.
The finish path consumes its database ceremony first, looks up the credential and
account from the returned credential ID, compares the discoverable credential's
userHandle, reconstructs WebAuthn4J verification from the stored COSE public key,
and verifies Origin, RP ID, challenge, UP/UV and signature before creating a session.

Successful authentication advances supported sign counters and replaces the
anonymous session and CSRF. Both-zero counters remain supported. Logout invalidates
the server session; private access then returns 401 and the spent assertion cannot
restore it. Live option fingerprints were `34a490f2b6a0` and `172b9ceb53e7`.

Evidence: `docs/evidence/06-passkey-authentication.md`. T08-C27 through C35 are done.
Final verification passed with 30 tests, 0 failures, 0 errors and 0 skipped; both
browser scripts passed syntax checking. Credential authentication also holds a row
lock while checking and updating the sign counter so concurrent assertions cannot
silently overwrite one another.
The next implementation target is Card 4: register a second passkey, list both with
nickname/date, delete one, reject the deleted credential and prevent deleting the
final credential.

## 2026-09-07 Card 2 registration implementation complete

The `/access` page now performs `navigator.credentials.create()` with no password
field. The server verifies the returned registration with WebAuthn4J against the
exact configured Origin, RP ID, server-held challenge, required user presence and
required user verification. Only then does one transaction create the account,
public-key credential and three account-scoped synthetic private items.

Successful registration replaces the anonymous session and CSRF value before
opening `/private`. Invalid Origin creates no account or credential and burns the
challenge. Browser cancellation calls a dedicated endpoint, leaves account,
credential and private-item counts at zero, and makes the ceremony unusable.

Verification: `./gradlew.bat clean test build` passed with 25 tests, 0 failures,
0 errors and 0 skipped. Evidence: `docs/evidence/05-registration-finish.md`.
T08-C19 through C25 are done. T08-C26 remains pending because the physical storage
provider must be recorded from an actual browser/authenticator ceremony.

## 2026-09-07 registration options HTTP boundary complete

The first Card 2 endpoint is live: `POST /api/webauthn/register/options`. `/access`
bootstraps an anonymous server session; the endpoint requires JSON, the configured
exact Origin and its session CSRF header before creating a row. It returns ES256 and
RS256 options with discoverable credential and user verification required,
attestation none, a 32-byte opaque user handle, and `Cache-Control: no-store`.

Live local evidence produced two different challenge fingerprints
(`402943e98cbe`, `f131b46e0fa0`), plus 403 for an attacker Origin and 415 for
`text/plain`. Cookie and CSRF values were redacted. Evidence:
`docs/evidence/04-registration-options-http.md`.

Current automated total: 21 tests passed (8 ceremony, 3 registration HTTP,
2 schema, 3 WebAuthn property and 5 application); 0 failures/errors/skips. Full
build passes.

Additional troubleshooting note for Claude: Flyway 12.4 reports that Boot's H2
2.4.240 is newer than its latest verified H2 2.3.232. The migration and validation
tests pass. Keep the warning visible until the dependency set moves or PostgreSQL
integration supersedes the development check.

## 2026-09-07 Slice 2 complete — schema and single-use ceremony

Flyway now owns an H2/PostgreSQL-compatible V1 schema for accounts, passkey public
credentials, WebAuthn ceremonies, private items and redacted security events. JPA
runs with `ddl-auto=validate`; schema inspection confirms there is no password or
private-key column.

`CeremonyService` creates independent 32-byte values with `SecureRandom`, expires
them after five minutes, keeps one active row per opaque session owner and ceremony
kind, and consumes the first finish attempt in a `REQUIRES_NEW` transaction under a
pessimistic row lock. Replay, expiry, kind mismatch, owner mismatch and two-thread
concurrent consumption are covered. Evidence:
`docs/evidence/03-server-held-ceremony.md`.

Verification: 14 tests passed, 0 failed/error/skipped, and
`./gradlew.bat clean test build` completed successfully.

Troubleshooting notes for Claude:

- Spring Boot 4 splits Flyway auto-configuration into its own module. Depending on
  `flyway-core` alone left Hibernate validation ahead of migration and reported
  `missing table [accounts]`. Replacing it with
  `spring-boot-starter-flyway` loaded the Boot 4 initializer.
- Replacing an active ceremony initially dirtied the old entity and inserted the
  new row in one flush; Hibernate issued the INSERT before the UPDATE and the active
  slot UNIQUE constraint rejected it. Saving and flushing the old consumed row
  before the new insert fixed the order without weakening the constraint.

## 2026-09-07 SKT ALeph security/network baseline adopted

Added `docs/T08-SECURITY-NETWORK.md` and D-014. T08 now carries forward T07's trust
boundary, paired negative evidence, centralized redaction, persistent replay/rate
state and TLS deployment discipline without copying its password/JWT/refresh-token
credentials. The concrete T08 boundaries are browser → Render edge → Spring Boot →
Neon TLS, plus the separate authenticator boundary.

The first executable control is also in place: every response now receives CSP,
clickjacking, MIME sniffing, referrer, opener and passkey Permissions-Policy headers.
The public integration test fixes these headers as a regression gate. HSTS remains
production-only and will not be claimed until the HTTPS deployment is tested.
Local response evidence: `docs/evidence/02-security-response-headers.md`.

## 2026-09-07 WebAuthn4J compatibility gate passed

Upgraded `webauthn4j-core` from 0.29.1.RELEASE to 0.31.8.RELEASE. Spring Boot's
dependency management selects Jackson 3.1.5 over WebAuthn4J's requested 3.1.4, so
the earlier Jackson 2/3 split is gone. A small adapter compiles registration and
authentication parsing paths, loads in the full Spring context, and the build
passes. Evidence: `docs/evidence/00-webauthn4j-compatibility.md`.

## 2026-09-07 Card 1 complete

Implemented the first vertical slice for T08-C13 through T08-C18. The T01 public
portfolio remains open, now ending in a visually distinct passkey boundary and a
public `/access` explanation page. `/private`, `/private/**`,
`/api/private-items`, and `/api/private-items/**` are protected by one server-side
session interceptor and return `401` with `Cache-Control: no-store` before a
controller can render private data.

The authenticated rendering path contains three clearly labelled synthetic items.
The current data service is intentionally an in-memory seam for Card 1 and will be
replaced by account-scoped persistence in the next slices. It does not accept an
account ID from the route or request.

Verification completed:

- `./gradlew.bat clean test build`: **BUILD SUCCESSFUL**.
- 3 Card 1 integration tests and 1 compatibility test passed; 0 failures,
  errors or skips.
- Live HTTP: `/` 200, `/access` 200, `/private` 401,
  `/api/private-items` 401.
- Public response contained none of the three private titles and no password input.
- Browser rendered the new boundary and the `/access` page successfully.
- Evidence: `docs/evidence/01-public-private-boundary.md`.

Troubleshooting note for Claude: the initial test compile used the pre-Boot-4 import
`org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` and
failed. Spring Boot 4.1.1 provides the annotation at
`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`; changing
that import resolved the failure. No runtime implementation change was required.

## 2026-09-07 full implementation design complete

Added `docs/T08-ARCHITECTURE.md` and `docs/T08-IMPLEMENTATION-PLAN.md`. The design now
fixes the whole path from the public T01 page through passkey-first account creation,
discoverable credential registration, database-held single-use ceremonies,
username-less assertion, server sessions, CSRF/Origin guards, account-scoped private
queries, second-passkey management, evidence and Render/Neon deployment.

D-007 resolves D-005: a verified first passkey creates the account; there are no
pre-seeded accounts, invites or passwords. D-008 through D-013 record the discoverable
credential policy, challenge consumption, session boundary, final-passkey deletion
guard, server ownership and WebAuthn4J scope. No application code or fixed criterion
changed. T08-C04 through C09 remain an explicit official-source blocker, and the
WebAuthn4J 0.31.8 compatibility spike is the first dependency gate before Card 2.

## 2026-09-07 Codex readiness verification

The repository is ready for Card 1 implementation. No application file changed in
this check.

- Compared `C:\gov\SKT_ALEPH\T08\T08.md` with `docs/T08-TASK.md`: the content diff
  reports them identical.
- Compared all seven source card images by SHA-256: every file matches its copy in
  `docs/task-source/`.
- Ran `.\gradlew.bat clean test build`: **BUILD SUCCESSFUL**, 8 tasks executed.
- Started the application and checked `/`, `/styles.css`, and `/script.js`: all
  returned 200. `/private` and `/api/private` returned 404 because Card 1 has not
  created those routes yet.
- The rendered public HTML contains no password input. Its existing Korean word
  `비공개` belongs to the T01 public privacy statement, not to T08 private content.
- Reconfirmed the official-source gate: D-003 still needs the official source for
  missing criteria T08-C04 through C09. D-005 was later resolved by D-007 without
  guessing any missing criterion text.

Handoff target remains Card 1: choose at least three synthetic private items, then
add the server-side `/private/**` boundary and its unauthenticated response tests.

## Historical scaffold snapshot (superseded)

This section records the repository state before implementation began. The current
state is described at the top of this file.

## Done

- [x] Assignment cards transcribed from images → `docs/T08-TASK.md`
- [x] Card images preserved → `docs/task-source/`
- [x] Acceptance matrix built from all captured criteria → `docs/T08-ACCEPTANCE-MATRIX.md`
- [x] Shaped requirements written → `docs/REQUIREMENTS.md`
- [x] Agent working agreement → `AGENTS.md`, `CLAUDE.md`
- [x] Stack decided → D-004 (Spring Boot 4.1.1 · Java 25 · webauthn4j · Thymeleaf)
- [x] Spring Security deliberately excluded → D-006
- [x] Spring Boot project generated, `webauthn4j-core` added, `./gradlew build` passes
- [x] T01 page vendored (`myeongjundev.github.io` @ `0ec47fc`) and split into
      `src/main/resources/templates/index.html` + `src/main/resources/static/`
- [x] `HomeController` serves `/`; verified `200` for `/`, `/styles.css`, `/script.js`

## Open decisions

| ID | Question |
| --- | --- |
| D-003 | Criteria T08-C04 – T08-C09 are not in the captured images and must be reconciled against the official assignment page. |

D-003 blocks the final completeness claim, not Card 1 implementation. D-005 is
resolved by D-007: successful first-passkey registration creates the account.

## Next steps

1. Run the WebAuthn4J compatibility spike, then **Card 2 — registration**
   (T08-C19 – C26): challenge issuance and storage,
   `navigator.credentials.create`, `webauthn4j` registration verification,
   public key persistence, nickname, cancel handling.
2. **Card 3 — authentication** (T08-C27 – C35): per-request challenge,
   `navigator.credentials.get`, signature verification, session establishment,
   replay rejection, logout.
3. **Card 4 — second passkey** (T08-C42 – C46): credential list screen, deletion,
   sign-in with the survivor, zero-passkey state.
4. **Card 5 — isolation and write-up** (T08-C36 – C41, C47 – C53): second account,
   cross-account rejection in both directions, `docs/T08-AUTH-GUIDE.md`,
   `docs/T08-SUBMISSION.md`.
5. Resolve D-003 against the official assignment page before submitting.
6. Deploy over HTTPS. **Note:** `t08.webauthn.rp-id` and `t08.webauthn.origin` in
   `application.properties` are currently `localhost` and must be overridden per
   environment — WebAuthn rejects an origin mismatch.

## Commands run

```bash
./gradlew build      # passes
./gradlew bootRun    # serves http://localhost:8080/
```

## Evidence captured

Card 1 request/response and source-isolation record:
`docs/evidence/01-public-private-boundary.md`.

## Handoff target

Next session: run the WebAuthn4J compatibility spike, then start Card 2 registration.
