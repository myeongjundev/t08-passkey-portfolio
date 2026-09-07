# Status

**Last updated:** 2026-09-07
**Phase:** setup complete — the app runs and serves the public page. No passkey code yet.

## Where things stand

The repository is scaffolded, the assignment is transcribed, the stack is chosen, and
a Spring Boot application serves the T01 intro page at `/`. Nothing about
authentication is implemented yet — no private area, no WebAuthn endpoints, no
persistence model.

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

## Open decisions (blocking implementation)

| ID | Question |
| --- | --- |
| D-003 | Criteria T08-C04 – T08-C09 are not in the captured images and must be reconciled against the official assignment page. |
| D-005 | How two passwordless accounts get created. |

## Next steps

1. **Card 1 — carve out the private area** (T08-C13 – C18). The only card that does
   not need WebAuthn working, so it is the right first slice.
   - Decide the three-plus synthetic private items.
   - Add a `HandlerInterceptor` guarding `/private/**`, returning 401 or 403.
   - Render the private block in `index.html` only when the session is authenticated,
     and capture the unauthenticated response body as evidence for C18.
2. Resolve D-005, then **Card 2 — registration** (T08-C19 – C26): challenge issuance
   and storage, `navigator.credentials.create`, `webauthn4j` registration
   verification, public key persistence, nickname, cancel handling.
3. **Card 3 — authentication** (T08-C27 – C35): per-request challenge,
   `navigator.credentials.get`, signature verification, session establishment,
   replay rejection, logout.
4. **Card 4 — second passkey** (T08-C42 – C46): credential list screen, deletion,
   sign-in with the survivor, zero-passkey state.
5. **Card 5 — isolation and write-up** (T08-C36 – C41, C47 – C53): second account,
   cross-account rejection in both directions, `docs/T08-AUTH-GUIDE.md`,
   `docs/T08-SUBMISSION.md`.
6. Resolve D-003 against the official assignment page before submitting.
7. Deploy over HTTPS. **Note:** `t08.webauthn.rp-id` and `t08.webauthn.origin` in
   `application.properties` are currently `localhost` and must be overridden per
   environment — WebAuthn rejects an origin mismatch.

## Commands run

```bash
./gradlew build      # passes
./gradlew bootRun    # serves http://localhost:8080/
```

## Evidence captured

None yet. `docs/evidence/` awaits the paired request/response records required by
T08-C50.

## Handoff target

Next session: start Card 1 — the private area and its server-side gate.
