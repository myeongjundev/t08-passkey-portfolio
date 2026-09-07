# Server-held single-use ceremony evidence

Captured from the H2/PostgreSQL-compatible integration environment on 2026-09-07.
No raw challenge, owner key or session identifier is included.

## Schema and persistence boundary

Flyway migration `V1__create_passkey_domain.sql` creates:

```text
accounts
passkey_credentials
webauthn_ceremonies
private_items
security_events
```

JPA starts with `ddl-auto=validate`; it does not silently create or repair these
tables. An information-schema test found zero column names containing `PASSWORD` or
`PRIVATE_KEY`.

The ceremony table enforces a 32-byte unique challenge, 32-byte opaque owner key,
five-minute application TTL and one `ACTIVE` slot per owner and kind. Credential ID,
account nickname, item ordering and ownership foreign keys also have database
constraints.

## Two options requests produce different values

Only the first 12 hexadecimal characters of SHA-256 are shown:

```text
first challenge fingerprint  b3d231794a0d
second challenge fingerprint 38f3125246f5
same raw value                false
active CREATE_ACCOUNT rows    1
```

Issuing the second request consumes the older active row before inserting the new
one. This prevents one anonymous owner and ceremony kind from accumulating usable
questions while still giving every options request a fresh value.

## Paired consume outcomes

```text
first consume with correct owner and kind       SUCCESS
second consume of the same ceremony             REPLAYED

consume with another owner key                  OWNER_MISMATCH
then consume with the original owner            SUCCESS

first consume through the wrong ceremony kind   KIND_MISMATCH
retry through the correct kind                  REPLAYED

first consume after the five-minute deadline    EXPIRED
retry the same expired ceremony                  REPLAYED

two simultaneous consume transactions           1 SUCCESS + 1 REPLAYED
```

The row is locked pessimistically and `consumed_at` is saved in an independent
transaction before later WebAuthn verification. A failed kind/expiry check therefore
cannot make the challenge usable again. An owner mismatch is checked before burning
the row so a caller from another session cannot invalidate it.

## Automated result

```text
CeremonyServiceTests       8 passed
SchemaSecurityTests        2 passed
Existing application tests 4 passed
failures/errors/skips      0 / 0 / 0
./gradlew.bat clean test build -> BUILD SUCCESSFUL
```

This proves the persistence and replay boundary. It does not yet claim that browser
registration or assertion verification is complete; those are the next slices.
