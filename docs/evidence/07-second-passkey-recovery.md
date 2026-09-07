# Card 4 — second passkey and device-loss recovery evidence

Captured on 2026-09-07 using one synthetic account and two generated P-256
authenticators. Cookie, session, CSRF, raw challenge, credential ID and signature
values are redacted.

## Two-passkey state

The authenticated account registered these credentials:

| Nickname | Registered date | State |
| --- | --- | --- |
| `주 기기` | shown by the server-rendered management screen | registered |
| `예비 보안 키` | shown by the server-rendered management screen | registered |

The second registration options response included the first credential in
`excludeCredentials`, preventing the same authenticator credential from being
registered twice. `GET /api/passkeys` returned exactly two account-scoped entries,
each with its nickname and registration timestamp.

## Delete one and use the survivor

The service locked the account's credential rows, deleted `주 기기`, and left
`예비 보안 키`. A WebAuthn assertion signed by the remaining authenticator passed
stored-public-key verification and identified the same account.

An assertion from the deleted `주 기기` credential was rejected because its
credential ID no longer resolved to a server-held public key. The failure did not
restore the credential.

## Zero-passkey policy

Deleting the remaining `예비 보안 키` returned HTTP 409:

```json
{"error":"final_passkey_required"}
```

The credential count remained one. The screen states that the final passkey cannot
be removed until another is registered and that losing every registered device has
no password or email recovery route. This demonstrates the zero-passkey outcome
without deliberately making an account unreachable.

Unauthenticated access to `/api/passkeys` returned 401. Delete and registration
operations require the authenticated server session, exact Origin, JSON content
type and CSRF header.

## Verification

`./gradlew.bat clean test build` passed with 32 tests, 0 failures, 0 errors and 0
skipped. JavaScript syntax checks passed for registration, login, logout and passkey
management scripts.
