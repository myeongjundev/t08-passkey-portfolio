# Production startup verification — 2026-09-07

## Local verification

- Official Eclipse Temurin JDK 25 downloaded outside the repository; SHA-256 checked
  against the Adoptium package metadata.
- `gradlew.bat clean test build --console=plain`: BUILD SUCCESSFUL.
- JUnit XML totals: 37 tests, 0 failures, 0 errors, 0 skipped.
- Local JDK: `C:\Users\dora2\.jdks\t08-temurin-25\jdk-25.0.4.1+1`.
  `JAVA_HOME` was set only for the verification process; global settings were not changed.

## Production attempt 1: correct PORT

- Confirmed Production `PORT` was `8080`; changed it to `80`.
- Redeployed source commit `abede2c` as `dpl_9GUMNMkvwjU51XvaqVhtCsgLi9uP`.
- Vercel build completed, but `/health` returned HTTP 500 with
  `INTERNAL_FUNCTION_INVOCATION_FAILED`.
- Runtime logs showed Java started as `appuser`, Hikari started successfully and
  Flyway identified PostgreSQL 17.6. This proves the database connection succeeded;
  it does not prove migration, application readiness or authentication succeeded.
- Runtime terminated before opening an HTTP listener: `startup timeout (15s)`.

## Production attempt 2: co-locate function and database

- Confirmed Function Region was Washington, D.C. (`iad1`).
- Changed the sole function region to Seoul (`icn1`), matching the Supabase project.
- Deployment `dpl_1dvseVnkULaierwCR9L2hLr1HZGD` built successfully, but `/health`
  still returned 500. Logs progressed through JPA EntityManagerFactory initialization
  before timeout. Region co-location alone was insufficient.

## Production attempt 3: reduce startup compilation overhead

- Preserved the memory flag and set `JAVA_TOOL_OPTIONS` to
  `-XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1`.
- The same packaged application started locally in 5.846 seconds and returned
  `200 {"status":"UP"}` on port 18080. The local verification server was stopped.
- Deployment `dpl_BPaTSyNfYxDCWAcxvjo8Uykymn1U` still returned 500. Runtime logs
  confirmed the flag was active and the app started in 9.449 seconds (JVM process
  10.435 seconds), then received shutdown. Container provisioning adds time before
  the JVM starts, so application-only startup time was not sufficient.
- Trade-off: limiting JIT compilation prioritizes startup over peak long-running
  throughput. No authentication or schema validation was disabled.
- Source: [Spring startup guidance](https://spring.io/blog/2018/12/12/how-fast-is-spring/).

Do not treat Vercel's build `Ready` status alone as application readiness.

## Production attempt 4: defer optional initialization

- Added JVM system properties `-Dspring.main.lazy-initialization=true` and
  `-Dspring.data.jpa.repositories.bootstrap-mode=lazy` to the existing options.
- Local packaged startup: 3.839 seconds. Flyway and JPA schema validation still
  completed; repository query preparation can occur on the first request.
- Added `scripts/Verify-Deployment.ps1`, tested on local port 18080 with the
  configured localhost origin. Public/health/access 200, private APIs 401,
  registration options 200 with different challenges, cancellation 204,
  foreign Origin and missing CSRF 403.
- Script debugging corrected its expected cancellation status from 200 to 204
  and cleared PowerShell session headers before each request so the missing-CSRF
  case could not inadvertently reuse a previous header. Anonymous logout was
  removed from the script because this endpoint requires authentication.
- No account or physical passkey is created by the script. It creates short-lived
  anonymous sessions and synthetic ceremonies, which expire or are consumed.

- Deployment `dpl_HpSDFqfeThPjst7ZmVnRiMVTgVnD` still returned 500. Logs showed
  application startup in 7.487 seconds but Vercel had already timed out initialization.

## Production attempt 5: early TCP binding with explicit HTTP startup gate

- Added `ContainerStartupConfiguration`, enabled only through the production
  `t08.server.bind-on-init=true` property (or explicit test configuration).
- Tomcat reserves its socket on initialization. Before `ApplicationReadyEvent`,
  an engine Valve returns fixed HTTP 503, `Cache-Control: no-store`, `Retry-After: 2`.
  After readiness, requests use the existing authentication and security pipeline.
- An actual-socket startup regression test verifies the pre-ready 503 response and
  the post-ready unauthenticated `/private` 401. Early binding alone was not enough
  to guarantee the pre-ready HTTP boundary; the explicit gate was added after that
  initial test exposed the behavior.

No database password, session cookie, CSRF token or credential payload is included.
