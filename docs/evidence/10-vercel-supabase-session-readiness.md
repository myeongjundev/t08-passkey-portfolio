# Evidence 10 — Vercel and Supabase session readiness

Date: 2026-09-07 (Asia/Seoul)

## Why the session changed

Vercel container functions are stateless and may scale down or route later requests
to another instance. Keeping `HttpSession` only in one Java process could therefore
lose the passkey-authenticated identity between requests.

The production profile now enables Spring Session JDBC. Flyway migration V2 creates
`spring_session` and `spring_session_attributes` in the same PostgreSQL database as
the account, credential and single-use ceremony rows. `SessionPrincipal` is explicitly
serializable. No password or private key is added.

## Focused persistence verification

```text
PersistentSessionConfigurationTests
productionSessionSurvivesSerializationInTheDatabase: PASS
```

The test starts the production session configuration against an isolated
PostgreSQL-compatible H2 database, writes a synthetic principal, CSRF value and
32-byte ceremony owner, reloads them through a separate repository read and confirms
one `spring_session` row. Raw runtime session IDs are not printed or committed.

## Deployment artifact

- `Dockerfile.vercel` builds the Java 25 bootJar in a multi-stage image.
- Runtime UID is 10001 and the public process listens on Vercel's container port 80.
- Supabase Session pooler port 5432 is required; transaction pooler port 6543 is not
  used with Hibernate prepared statements.

This is local deployment readiness evidence. The public HTTPS URL, Supabase TLS
connection and physical authenticator still require post-deployment verification.

## Full build and production container

```text
./gradlew.bat clean test build
BUILD SUCCESSFUL
tests=35 failures=0 errors=0

docker build -f Dockerfile.vercel -t t08-passkey-portfolio:vercel-verify .
image build: success

prod-profile container
GET /health -> 200 {"status":"UP"}
GET /access -> 200, cookie name SESSION (value redacted)
uid=10001(appuser)
```

The verification container used an isolated local H2 database in PostgreSQL mode;
no Supabase credential was needed or exposed. It was stopped after the check.
