# Cross-account isolation evidence

Captured on 2026-09-07 using two synthetic accounts. Each account registered an
independent generated P-256 passkey and completed WebAuthn signature verification
before the authorization checks. Credential, challenge, signature, cookie, session
and CSRF values are redacted.

## Starting state

| Account | Own private rows | Distinguishing content |
| --- | ---: | --- |
| 합성 계정 A | 3 | every body contains `합성 계정 A 전용` |
| 합성 계정 B | 3 | every body contains `합성 계정 B 전용` |

The two accounts and their passkeys were created through the same registration and
verification flow used by the application. Both login assertions were verified
against their separately stored public keys.

## Paired direct-object rejection

| Authenticated session | Requested item owner | Response | Total rows before → after |
| --- | --- | --- | ---: |
| A | B | `404 {"error":"resource_not_found"}` | 6 → 6 |
| B | A | `404 {"error":"resource_not_found"}` | 6 → 6 |

The same response hides both absence and foreign ownership. Neither rejected read
changed either account's data.

## Supplied account ID is ignored

Account A requested:

```text
GET /api/private-items?accountId=<account-B-redacted>
```

The response was 200 with exactly A's three rows. It contained `합성 계정 A 전용`
and no `합성 계정 B 전용` value. The controller never accepts a request account ID
as its ownership selector.

## Enforcement source

- `PrivateAreaController`: obtains the account only from `SessionPrincipal`.
- `PrivateAreaService`: passes that account ID into every private-item lookup.
- `PrivateItemRepository.findByIdAndAccountId`: requires both item and owner in one
  database query.
- `AuthenticationInterceptor`: rejects missing authenticated sessions before the
  private controller executes.

## Verification

The full build passed with 33 tests, 0 failures, 0 errors and 0 skipped.
