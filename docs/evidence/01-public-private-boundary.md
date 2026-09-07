# Card 1 evidence — public/private boundary

Captured locally on 2026-09-07. All names and private items below are synthetic.
No cookie or session identifier is recorded.

## Public response contains no private item

Request:

```http
GET / HTTP/1.1
Host: localhost:8080
```

Observed response and source checks:

```text
HTTP 200
public boundary text present: true
"합성 프로젝트 알파 출시 메모" present: false
"합성 지원 기업 베타 목록" present: false
"합성 주간 회고 감마" present: false
password input present: false
```

The in-app browser also rendered the public-to-private boundary as a separate dark
section headed `공개 소개는 여기까지. 그다음은 패스키로.` and opened `/access`
without authentication.

## Direct unauthenticated requests are rejected

Request/response pair 1:

```http
GET /private HTTP/1.1
Host: localhost:8080

HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store

{"error":"passkey_session_required"}
```

Request/response pair 2:

```http
GET /api/private-items HTTP/1.1
Host: localhost:8080

HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store

{"error":"passkey_session_required"}
```

Neither rejection body contains a private title or body.

## Authenticated rendering test

The server integration test injects a synthetic `SessionPrincipal` directly into a
test-only mock session. This does not claim that passkey authentication is complete;
it proves only the post-authentication Card 1 boundary while Card 2 and Card 3 are
still pending.

```text
GET /private             -> 200, all three synthetic cards rendered
GET /api/private-items   -> 200, JSON array length = 3
session identifier       -> [redacted; not captured]
```

Automated result:

```text
T08PasskeyPortfolioApplicationTests
Card 1 tests: 3
full suite: 4, failures: 0, errors: 0, skipped: 0
./gradlew.bat clean test build -> BUILD SUCCESSFUL
```

Relevant enforcement source:

- `src/main/java/dev/myeongjun/passkey/config/WebMvcConfiguration.java`
- `src/main/java/dev/myeongjun/passkey/session/AuthenticationInterceptor.java`
- `src/main/java/dev/myeongjun/passkey/privatearea/PrivateAreaController.java`
- `src/test/java/dev/myeongjun/passkey/T08PasskeyPortfolioApplicationTests.java`
