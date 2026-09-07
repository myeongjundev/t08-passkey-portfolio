# Evidence 09 — Public source and deployment readiness

Date: 2026-09-07 (Asia/Seoul)

## Public source URL

- URL: `https://github.com/myeongjundev/t08-passkey-portfolio`
- Verification: the repository opened without authentication and was labelled
  public after commit `05a85ae` was pushed to `main`.
- Result: T08-C02 is complete. The source link is recorded in
  `docs/T08-SUBMISSION.md`.

## Repeatable build

```text
./gradlew.bat clean test build
BUILD SUCCESSFUL
tests=34 failures=0 errors=0
```

The added test checks that `/health` reaches the configured database and returns
only `{"status":"UP"}` with `Cache-Control: no-store`.

## Container verification

```text
docker build -t t08-passkey-portfolio:verify .
image build: success

GET http://localhost:18080/health
200 {"status":"UP"}

container identity
uid=10001(appuser) gid=999(appuser) groups=999(appuser)
```

The verification container was stopped after the check. No credential or connection
string was placed in the command output. This proves that the deployable artifact
builds and runs; it does not claim the still-pending public HTTPS deployment.
