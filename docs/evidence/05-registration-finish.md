# Card 2 — registration finish evidence

Captured on 2026-09-07 from the local Spring Boot test environment. All identities
and credentials below are synthetic. Session, CSRF and raw challenge values are
redacted.

## Verified success path

1. `POST /api/webauthn/register/options` returned a server-held 32-byte challenge.
2. A standards-shaped `webauthn.create` response was generated with a P-256 key.
3. `POST /api/webauthn/register/finish` returned `201` and `{ "redirectTo": "/private" }`.
4. The anonymous session ID and CSRF value were both replaced.
5. The authenticated session could read exactly three account-scoped synthetic items.

The finish request body had this redacted shape:

```json
{
  "ceremonyId": "<uuid>",
  "credential": {
    "id": "<base64url credential id>",
    "rawId": "<base64url credential id>",
    "type": "public-key",
    "authenticatorAttachment": "platform",
    "clientExtensionResults": {},
    "response": {
      "clientDataJSON": "<redacted>",
      "attestationObject": "<redacted>",
      "transports": ["internal"]
    }
  }
}
```

There is no password or private-key field. WebAuthn keeps the private key in the
authenticator; the server receives attested public credential material. The adapter
also rejects a parsed COSE key if private-key material is present.

## Stored server values

After verification the database contained:

- accounts: 1
- passkey credentials: 1
- private items: 3
- human-readable credential nickname: `이 노트북`
- public credential fields: credential ID, CBOR-encoded COSE public key, algorithm
  metadata, sign counter, transports and backup flags

The stored metadata was limited to `{"formatVersion":1,"algorithm":-7}`. It did not
contain the challenge, collected client data or private-key material. The raw
challenge is retained only by the single-use ceremony row and is consumed before
verification begins.

## Paired rejection and replay proof

The same valid structure with Origin changed to `https://attacker.example` failed
WebAuthn verification. Row counts remained accounts 0, credentials 0 and private
items 0. Repeating the finish request with that ceremony was rejected as replay,
because the first finish attempt had already consumed it.

## Cancellation proof

`POST /api/webauthn/register/cancel` returned `204` with `Cache-Control: no-store`.
Accounts, credentials and private items all remained at 0, and the cancelled
ceremony could not be consumed again. The `/access` screen handles the browser's
`NotAllowedError`, calls this endpoint and displays a cancellation message.

## Automated verification

`./gradlew.bat clean test build` completed successfully: 25 tests, 0 failures,
0 errors and 0 skipped.

The built application was also started locally: `/access` returned 200 with
`Cache-Control: no-store`, the registration form and no password field;
`/passkey.js` returned 200 and contained the cancellation handler. The server was
stopped after the check.

The physical passkey storage provider (device, Google Password Manager or security
key) still requires a manual browser ceremony and is intentionally not claimed here.
