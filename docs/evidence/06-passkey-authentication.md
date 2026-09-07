# Card 3 — passkey authentication and logout evidence

Captured on 2026-09-07 with synthetic accounts and credentials. Cookie, session ID,
CSRF token, raw challenge, authenticator signature and collected client data are
redacted.

## Fresh username-less options

Two consecutive `POST /api/webauthn/authenticate/options` requests in the same
anonymous session returned 200 and distinct server-held challenges:

| Attempt | SHA-256 fingerprint (first 12 hex) | Bytes |
| --- | --- | --- |
| 1 | `34a490f2b6a0` | 32 |
| 2 | `172b9ceb53e7` | 32 |

Both responses set `userVerification=required`, returned an empty
`allowCredentials` list for username-less discovery, and used
`Cache-Control: no-store`.

## Paired signature result

The automated authenticator fixture registered a generated P-256 public key, then
signed `authenticatorData || SHA-256(clientDataJSON)` with its matching private key.

| Case | Result | Server state |
| --- | --- | --- |
| Matching signature, Origin, RP ID, challenge, userHandle and UP/UV | accepted | new authenticated session; counter 0 → 1 |
| Same shape with one signature byte changed | rejected | no principal returned; counter stayed 0 |

The application reconstructs WebAuthn4J's authenticator from the database-held COSE
public key. The private key exists only in the synthetic test authenticator and is
never sent in the assertion or stored by the application.

Counter-capable credentials must advance their sign counter. Credentials that
report zero both before and after authentication are accepted as counter-unsupported.

## Single use, session and logout

The first finish attempt consumes the authentication ceremony before signature
verification. Reusing the challenge after a failed signature was rejected. A
successful assertion replaced the anonymous session and installed only a
`SessionPrincipal(accountId, displayName)` plus fresh CSRF state.

`POST /api/session/logout` returned 204 and invalidated the server session. The next
private-data request returned 401. Submitting the already-used assertion after
logout from a fresh anonymous session returned 400; it did not recreate access.

No evidence value contains a cookie, session identifier, CSRF token, raw challenge
or signature. No password input or password column exists.

## Verification

`./gradlew.bat clean test build` passed with 30 tests, 0 failures, 0 errors and 0
skipped. JavaScript syntax checks passed for both `passkey.js` and `session.js`.
