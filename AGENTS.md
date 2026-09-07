# T08 shared agent instructions

These rules apply to Claude, Codex, and any other coding agent working in this
repository.

1. Read `docs/STATUS.md`, `docs/REQUIREMENTS.md`, and `docs/DECISIONS.md` before
   changing implementation files.
2. `docs/T08-TASK.md` is the transcribed assignment and `docs/T08-ACCEPTANCE-MATRIX.md`
   is the fixed checklist. Do not weaken, remove, or rewrite an acceptance
   expectation to make code pass. If a criterion looks wrong, record the conflict in
   `docs/DECISIONS.md` and leave the criterion intact.
3. **No passwords.** T08 is a passwordless task. Do not add a password field, a
   password hash column, or a password reset flow anywhere in the product.
4. The private key never leaves the device. Only the public key, the credential id,
   and the sign counter may be stored server-side. If a request body would carry a
   private key, that is a bug, not a feature.
5. Every challenge is single-use and server-held. Generate it server-side, store it
   until verification, and reject replays. Never trust a challenge that only ever
   lived in the browser.
6. The public area stays public. The first screen of the deployed result is the T01
   intro page and must open with no account, no login, and no invite. Only the newly
   added private area is locked.
7. Private content is server-gated, not CSS-hidden. An unauthenticated page response
   must not contain private content anywhere in its source.
8. All data in the public deployment, screenshots, tests, and submissions is
   synthetic. No real contact details, no ID numbers, no real personal records.
9. Redact session and token values in every captured request/response log before
   committing it under `docs/evidence/`.
10. Do not commit chat transcripts, secrets, `.env` files, generated dependency
    folders, or private exports.
11. Before handoff, update `docs/STATUS.md` with commands run, evidence captured,
    remaining work, and the handoff target.

Detailed workflow and architecture are documented under `docs/`.
