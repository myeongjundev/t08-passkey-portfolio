# Status

**Last updated:** 2026-09-07
**Phase:** setup — repository prepared, no implementation yet.

## Where things stand

The repository is scaffolded with the assignment transcribed, the acceptance matrix
laid out, and the T01 public page vendored. No server code exists yet. No stack has
been chosen.

## Done

- [x] Assignment cards transcribed from images → `docs/T08-TASK.md`
- [x] Card images preserved → `docs/task-source/`
- [x] Acceptance matrix built from all captured criteria → `docs/T08-ACCEPTANCE-MATRIX.md`
- [x] Shaped requirements written → `docs/REQUIREMENTS.md`
- [x] Agent working agreement → `AGENTS.md`, `CLAUDE.md`
- [x] T01 public page vendored under `public/` (from `myeongjundev.github.io` @ `0ec47fc`)

## Open decisions (blocking implementation)

| ID | Question |
| --- | --- |
| D-003 | Criteria T08-C04 – T08-C09 are not in the captured images and must be reconciled against the official assignment page. |
| D-004 | Implementation stack and WebAuthn library not chosen. |
| D-005 | How two passwordless accounts get created. |

## Next steps

1. Resolve D-004 (stack + WebAuthn library) and record it in `docs/DECISIONS.md`.
2. Resolve D-003 by checking the official assignment page.
3. Card 1 — carve the private area out of the page and prove it is server-gated
   (T08-C13 – C18). This is the only card that does not need WebAuthn working, so it
   is the right first slice.
4. Card 2 — registration ceremony (T08-C19 – C26).
5. Card 3 — authentication ceremony (T08-C27 – C35).
6. Card 4 — second passkey and deletion (T08-C42 – C46).
7. Card 5 — isolation checks and the write-up (T08-C36 – C41, C47 – C53).

## Commands run

None yet — no build or test tooling exists.

## Evidence captured

None yet. `docs/evidence/` is empty and awaits the paired request/response records
required by T08-C50.

## Handoff target

Next session: pick up at D-004, then start Card 1.
