# Security response-header evidence

Captured from the local Spring Boot server on 2026-09-07.

Request:

```http
GET / HTTP/1.1
Host: localhost:8080
```

Observed response:

```http
HTTP/1.1 200 OK
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: https://cdn.simpleicons.org; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'
Referrer-Policy: no-referrer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Cross-Origin-Opener-Policy: same-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(), publickey-credentials-create=(self), publickey-credentials-get=(self)
```

The same CSP was present on the unauthenticated `GET /private` rejection, which
returned `401` and `Cache-Control: no-store`.

`Strict-Transport-Security` was intentionally absent because this capture used
`http://localhost:8080`. HSTS remains unclaimed until it is verified on the final
HTTPS deployment.

The integration suite asserts these headers so a later endpoint or refactor cannot
silently remove the baseline.
