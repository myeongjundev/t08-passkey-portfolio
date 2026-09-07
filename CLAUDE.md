# Claude entrypoint for T08

Follow `AGENTS.md` as the shared working agreement. Before acting, read:

1. `docs/STATUS.md`
2. `docs/REQUIREMENTS.md`
3. `docs/DECISIONS.md`

Use the repository documents as memory instead of prior conversation. Work on one
explicit card or bounded slice at a time, preserve synthetic-only public data, and
finish by updating `docs/STATUS.md` with commands, evidence, remaining work, and the
handoff target.

## What this project is

T08 attaches a **passkey (WebAuthn)** lock to the personal intro page built in T01.
The intro page stays public. A newly added private area is opened only by a passkey.
There is no password anywhere in the system.

The five task cards, in order:

| Card | Theme | Criteria |
| --- | --- | --- |
| 1 | Split public from private | T08-C13 – C18 |
| 2 | Register a passkey | T08-C19 – C26 |
| 3 | Sign in with a passkey | T08-C27 – C35 |
| 4 | Losing a device (two passkeys) | T08-C42 – C46 |
| 5 | Prove it is really locked, and write it up | T08-C01 – C12, C36 – C41, C47 – C53 |

## Non-negotiables

- No password field, ever.
- Private key stays on the device; the server stores only the public key.
- Challenges are server-generated, server-held, and single-use.
- Unauthenticated responses must not contain private content in the source.
- Synthetic data only in anything public.

## Evidence is the deliverable

This task is graded on paired request/response records, not on claims. For each
check, capture both the blocked request and the passing request side by side, redact
session and token values, and file it under `docs/evidence/`. Keep
`docs/T08-ACCEPTANCE-MATRIX.md` updated as evidence lands.
