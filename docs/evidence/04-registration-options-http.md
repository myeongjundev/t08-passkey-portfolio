# Registration options HTTP evidence

Captured from `http://localhost:8080` on 2026-09-07. Cookie, CSRF and raw challenge
values are not recorded.

## Bootstrap

```http
GET /access HTTP/1.1
Host: localhost:8080

HTTP/1.1 200 OK
Cache-Control: no-store
Set-Cookie: [redacted]
```

The response creates an anonymous server session with an opaque 32-byte ceremony
owner key and a separate 32-byte CSRF value. Neither value is an account identifier.

## Two successful options requests

Request shape used twice in the same anonymous session:

```http
POST /api/webauthn/register/options HTTP/1.1
Host: localhost:8080
Origin: http://localhost:8080
Content-Type: application/json
Cookie: [redacted]
X-CSRF-Token: [redacted]

{"displayName":"합성 계정 A","nickname":"노트북 패스키"}
```

Observed results:

```text
first status                  200
second status                 200
first challenge fingerprint  402943e98cbe
second challenge fingerprint f131b46e0fa0
fingerprints differ           true
challenge bytes               32
user handle bytes             32
residentKey                   required
requireResidentKey            true
userVerification              required
attestation                   none
password field/value          absent
```

Each response returned a ceremony UUID. The integration test verified that its row
exists in `webauthn_ceremonies` and that only the latest row remains active for the
same anonymous owner and `CREATE_ACCOUNT` kind. Only SHA-256 prefixes are shown;
the raw challenges remain server/browser protocol values.

## Paired security rejections

Same URL and JSON body, but an untrusted origin:

```http
Origin: https://attacker.invalid
Cookie: [redacted]
X-CSRF-Token: [redacted]

HTTP/1.1 403 Forbidden
{"error":"request_rejected"}
```

Same URL and origin, but the wrong media type:

```http
Origin: http://localhost:8080
Content-Type: text/plain
Cookie: [redacted]
X-CSRF-Token: [redacted]

HTTP/1.1 415 Unsupported Media Type
{"error":"json_required"}
```

Automated tests also cover missing Origin, missing CSRF, wrong CSRF and invalid or
blank labels. All are rejected before a ceremony row is created.

This is evidence for the registration options half of T08-C19/T08-C20. Those matrix
items remain `wip` until the finish endpoint verifies a real WebAuthn response.
