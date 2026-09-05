---
name: android-backlog
description: |
  Use this skill when the user wants to create or maintain the numbered backlog of an
  Android app in the other-projects monorepo (`specs/backlog-*.md`, as in
  watch-up/specs and notes/specs). Trigger phrases: "adiciona um item no backlog do
  notes", "marca o item 7 como feito", "cria o backlog do gastos", "add a bug to the
  backlog", "reordena as prioridades", "/android-backlog".

  Behavior:
  - Adds items with N = max + 1, marks items done in the app's own status dialect,
    creates a backlog from the house template, or reorders priority without
    renumbering.
  - Detects the app's dialect by reading the file instead of assuming it, and never
    converts an existing file from one dialect to the other.
  - Verifies 10 structural invariants after every edit.
  - Reads git tags read-only to seed the delivery-history table.

  Do not invoke when:
  - The status change is part of shipping a version — that is `/android-release`
    step 9, which also appends the delivery-history row.
  - The user wants to implement a backlog item — that is normal work, or
    `/android-new-feature` if it needs a module.
  - The app uses `docs/` for plans instead of a numbered backlog (`utilities`) —
    respect that organization.

  Item numbering is PERMANENT: NEVER renumber, NEVER reuse a number, NEVER delete a
  completed item's section. This skill never builds, never bumps a version, and never
  commits.
version: 0.1.0
allowed-tools: [Bash, Read, Write, Edit, Grep, Glob, AskUserQuestion]
argument-hint: <app> [add | done | create | groom]
---

# android-backlog

Creates and maintains an app's `specs/backlog-*.md` in the house format.

**The invariant:** item numbering is permanent. Numbers are never reused, never
renumbered, and never closed by deletion — an item that comes back gets a new
`Histórico de entregas` row under a new version while keeping its original number.
That permanence is the whole point: it lets "faz o 7" mean the same thing months
later.

This skill's `Bash` is read-only (`git tag`, `git log`, `ls`). It never builds and
never commits.

---

## When to use

- **"adiciona um item no backlog do \<app\>"** — the common case.
- **"marca o 3 como feito"** outside a release.
- **"cria o backlog do gastos"** — an app that has none.
- **"reordena as prioridades"** — moving rows, never numbers.

## Do not invoke when

- Shipping a version → `/android-release` (its step 9 does the release-time status
  flip *and* the history row; this skill does not).
- Implementing an item → normal work, or `/android-new-feature`.
- The app is `utilities` and the user wants `specs/` → it uses `docs/` for plans
  deliberately; say so and don't create `specs/`.
- The cwd resolves outside this monorepo → stop, see Step 1.

---

## Workflow

### Step 1 — Preflight

Resolve the root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

Then, read-only:

- `ls "$ROOT/<app>"/specs/backlog-*.md "$ROOT/<app>"/docs/*.md 2>/dev/null` — locate
  the backlog, or establish there is none. Today only `watch-up` and `notes` have one;
  `utilities` has `docs/`; `gastos`, `site-blocker`, `apps-hub` have nothing.
- If it exists: parse the index table (numbers, types, titles, statuses), the max
  number, the **dialect** (`grep -m1 '| feito'`), and the last history row.
- `grep -E 'versionName|versionCode' <app>/app/build.gradle.kts` — for the version in
  the status and the "Versão atual" line.
- `git -C <app> tag --list` — only if the mode may seed history.
- `git status --short` — surface in the plan summary.

**Do NOT** create the file yet. **Do NOT** renumber anything, ever. **Do NOT** read the
whole `specs/` folder as backlog — `watch-up` has other `specs/*.md` (`busca.md`,
`google-drive.md`, a PDF) that are specs, not backlog.

### Step 2 — Round 1 interview

Mode. Payload verbatim in
[references/interview-flow.md](references/interview-flow.md#round-1--modo). If the
argument already carried the mode (`<app> done`), skip this and go straight to Step 3.

If the mode is "create" and a backlog already exists → stop and switch to "add".
If the mode is anything but "create" and no backlog exists → stop, say so, and offer
"create".

### Step 3 — Round 2 interview + plan summary

Branch by mode:

| mode | payload |
|---|---|
| add | [#round-2a](references/interview-flow.md#round-2a--modo-adicionar-item) — type, title, confirm |
| done | [#round-2b](references/interview-flow.md#round-2b--modo-marcar-feito) — items, version, confirm |
| create | [#round-2c](references/interview-flow.md#round-2c--modo-criar-o-backlog) — dialect, seed history, confirm |
| groom | reuse 2b's shape for "which items", then confirm |

Print a ≤20-line summary first — for "add", include the number each item will get.
**Do not touch the filesystem until "Seguir" has been picked.**

### Step 4 — Apply

Per [references/backlog-format.md](references/backlog-format.md) and
[references/templates.md](references/templates.md):

- **add** — append the index row and the `## N. <emoji> <title>` section, N = max + 1.
  Write the body from what the conversation and the code already show; a thin section
  beats an invented bullet.
- **done** — flip the status cells to the app's own dialect. **No history row here** —
  that belongs to `/android-release`.
- **create** — write the file from the template in the chosen dialect, seeding history
  from tags if asked. Seeded rows get an **empty `Itens` column**: which item each past
  release closed is not recoverable, and guessing corrupts the exact mapping the table
  exists to hold. Say that.
- **groom** — reorder rows in the index table only. Section order stays numeric.

### Step 5 — Verify the 10 invariants

Run all of
[references/backlog-format.md](references/backlog-format.md#invariantes-verificáveis).
The ones that catch real damage: index-row count == section count, numbers contiguous
and unique, every number cited in `Itens` exists, and each title matching between table
and section.

If any fails, fix it before reporting — and if the failure predates this edit, say so
rather than silently repairing unrelated drift.

### Step 6 — Report

What changed, with the numbers assigned. Then state plainly that release-time status
changes and the `Histórico de entregas` row are `/android-release`'s job, so the user
doesn't do it twice. Do not commit.

---

## Reference files

- [references/backlog-format.md](references/backlog-format.md) — **the canonical
  spec**: the stable-numbering law, file structure, both tables, the two dialects, and
  the 10 verifiable invariants.
- [references/templates.md](references/templates.md) — empty skeletons in both
  dialects, a filled 3-item example, and the tag-seeding command.
- [references/interview-flow.md](references/interview-flow.md) — verbatim payloads per
  mode and the "Other" handling table.
- [../README.md](../README.md) — which apps have a backlog and in which dialect.

---

## Silent quality checklist (run before showing the Step 3 summary)

1. Root resolved; the three markers exist.
2. The backlog file was located, or its absence established.
3. The dialect came from `grep -m1 '| feito'`, not from the app's name.
4. The max number came from parsing the table, not from counting rows (a gap makes
   those differ).
5. For "add", the new number is max + 1 — never filling a gap.
6. For "done", every chosen number exists and none is already `feito`.
7. For "create", no backlog already exists, and the app isn't `utilities`.
8. No plan step renumbers, reorders sections, or deletes a section.
9. The version in a status came from `app/build.gradle.kts`, read live.
10. The plan contains no build, no commit, no version bump.
11. The dirty-tree state is in the summary.

If any item fails, fix the plan before showing it.

---

## Anti-patterns

- Don't renumber. Not to close gaps, not to reorder priority.
- Don't reuse a number, and don't fill a gap left by an abandoned item.
- Don't delete a completed item's section.
- Don't convert an existing file's dialect.
- Don't add the `notes` "Versão atual" line to `watch-up`, and don't touch
  `watch-up`'s schema-migration footnote.
- Don't invent status vocabulary — `pendente` and the two `feito` forms are all there is.
- Don't invent a fourth type emoji.
- Don't guess the `Itens` column when seeding history from tags.
- Don't add the `Histórico de entregas` row here — that's `/android-release` step 9.
- Don't create `specs/` in `utilities`.
- Don't bump a version, touch `app/build.gradle.kts`, build, or commit.

---

## Limits

- One app per invocation.
- Writes the item's *statement*, not its solution — no code, no estimates, no
  priorities beyond row order.
- Can't recover which item an old release closed; seeded history rows leave `Itens`
  blank.
- Doesn't reconcile a backlog whose invariants were already broken before this run — it
  reports the breakage and asks.
- Doesn't touch `watch-up`'s other `specs/*.md`, which are specs rather than backlog.
