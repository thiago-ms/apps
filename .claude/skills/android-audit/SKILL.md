---
name: android-audit
description: |
  Use this skill when the user wants a health/drift report across the Android apps in
  the other-projects monorepo (`.sample/` plus the submodules watch-up, utilities,
  site-blocker, gastos, notes, apps-hub) and the `.dist/` hub. Trigger phrases:
  "audita os apps", "o que está fora do padrão", "tudo publicado?", "check the
  fleet", "algum app sem tag", "/android-audit".

  Behavior:
  - Read-only. Collects ~10 facts per app plus the root hub state, diffs them against
    the fleet invariants, and reports findings grouped by severity.
  - Every check carries its KNOWN current exceptions, so the report shows real drift
    instead of noise.
  - Also checks that `.sample/`'s token counts still match what
    `android-new-app/references/token-map.md` declares — the finding that matters most,
    because a mismatch means the next scaffold fails mid-substitution.
  - Ships every finding with the command to fix it. The user runs it.

  Do not invoke when:
  - The user wants something fixed — this skill only reports. Point at
    `/android-release`, `/android-backlog`, or a manual command.
  - The user wants to know one specific fact about one app (current version, module
    list) — just read the file and answer.

  This skill REPORTS and NEVER REPAIRS. It runs no build, no git write, no delete, no
  file edit.
version: 0.1.0
allowed-tools: [Bash, Read, Grep, Glob, AskUserQuestion]
argument-hint: [app | "all"]
---

# android-audit

Read-only drift report across the 7 apps and the `.dist/` hub.

**The invariant:** this skill reports and never repairs. Every finding ships with the
command that would fix it, and the user decides whether to run it. That's not timidity —
several of the known drifts are *correct as they stand* (`site-blocker`'s unqualified
`distApk` is harmless in a single-module app; `watch-up` 1.15 was deliberately never
released), and a skill that "fixed" them would be doing damage.

Note there is no `Write` tool here. Nothing this skill can do touches a file.

---

## When to use

- **"audita os apps"**, **"o que está fora do padrão?"** — the full sweep.
- Before or after a batch of releases, to confirm every hub and tag lines up.
- After editing `.sample/`, to confirm the token map still matches (check **A5**).
- Periodically, because nobody notices this kind of drift by hand.

## Do not invoke when

- The user wants a fix applied → `/android-release`, `/android-backlog`, or a manual
  command.
- The user wants one fact about one app → read the file and answer directly.
- The cwd resolves outside this monorepo → stop, see Step 1.

---

## Workflow

### Step 1 — Preflight

Resolve the root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
git -C "$ROOT" submodule status
```

Enumerate the apps to audit — every submodule plus `.sample`, or just the one named in
the argument. **Do NOT** check host tooling; nothing here builds.

### Step 2 — Collect

Run the read-only commands in [references/collect.md](references/collect.md), in
parallel. ~10 reads per app, none dependent on another.

Compare `versionName` as an **integer pair**, never as a decimal: this repo's 2-place
scheme rolls past 9 (`1.9` → `1.10` → `1.16`), so float comparison inverts the order.
The `sort -t. -k1,1n -k2,2n` recipe is in
[references/collect.md](references/collect.md#comparar-versionname-como-par-de-inteiros).

### Step 3 — Diff against the invariants

Run the checks in [references/checks.md](references/checks.md) — **A1–A6** (blocks
delivery), **B1–B6** (drift), **C1–C8** (hygiene).

Each check has its known current exceptions recorded. Honor them: `utilities` having no
`adb.sh` is a documented gap with a documented fallback, not a finding to raise every
run. A check whose known exception has *disappeared* is good news — report it as
resolved and suggest removing the exception from `checks.md`.

### Step 4 — Report by severity

Group by severity and **omit the checks that passed** — the value is in what diverged.
Format in
[references/checks.md](references/checks.md#formato-do-relatório). Close with the
counts and the remediation commands in a block, stating plainly that this skill ran
none of them.

If nothing diverged beyond the known exceptions, say so in one sentence. A clean report
is a result.

### Step 5 — Offer, don't apply

For each finding, name what would fix it and who owns it:

| finding | owner |
|---|---|
| hub stale, version untagged going forward | `/android-release <app>` |
| app without a backlog | `/android-backlog <app> create` |
| `token-map.md` out of sync (**A5**) | rewrite the tables from the current `.sample/`, then validate with a throwaway scaffold that builds |
| `.sample/` polluted (**A6**) | ask the user to clean it — never delete artifacts |
| retroactive tag, `apps.code-workspace`, `apps-hub` registry | manual, command provided |

Then stop. Do not offer to run any of it in this invocation — a report the user trusts
is worth more than a fix bundled into it.

---

## Reference files

- [references/checks.md](references/checks.md) — every check with its expected value,
  known current exceptions, and remediation command; plus the report format.
- [references/collect.md](references/collect.md) — the read-only commands per app, at
  the root, in `.sample/` and in `apps-hub`, and the integer-pair version comparison.
- [../README.md](../README.md) — the stable per-app table this audit validates against.

---

## Silent quality checklist (run before showing the report)

1. Root resolved; the three markers exist.
2. Every app in `git submodule status` was collected, plus `.sample`.
3. Versions were compared as integer pairs, not decimals.
4. Each slug came from `app/build.gradle.kts`, not from the directory name.
5. Every finding was matched against the known-exceptions list before being raised.
6. Check **A5** actually ran the token count against `.sample/` — not read the numbers
   out of `token-map.md` and repeated them.
7. No finding claims something is broken when it is documented as deliberate
   (`site-blocker` single-module, `utilities` without `adb.sh`, `gastos` transitive
   `:core:data`, `<slug>123` keystores).
8. No remediation suggests rotating a keystore or deleting an artifact.
9. Passing checks are omitted from the report.
10. No command in the transcript wrote, deleted, or built anything.

If any item fails, fix the report before showing it.

---

## Anti-patterns

- Don't fix anything. Not "while I'm here", not the one-line ones.
- Don't run `make`, `git add/commit/tag/push/checkout/stash/clean`, `rm`, `mv`, or `cp`.
- Don't retroactively tag an old version — report the candidate sha and stop.
- Don't suggest rotating the `<slug>123` keystores: every published APK is signed with
  them, and rotating breaks signature continuity for installed users.
- Don't suggest deleting anything from `dist/`, `.dist/`, or `.sample/`.
- Don't report a missing old version in the hub as a finding — the hub holds the current
  version, by design.
- Don't report `site-blocker`'s unqualified `distApk` as a problem; it is single-module.
- Don't report pre-template catalog divergence (`utilities`, `site-blocker`) as drift to
  normalize.
- Don't pad the report with checks that passed.
- Don't infer a fact you didn't read — if a command failed, say the check didn't run.

---

## Limits

- Static analysis of files and git metadata only. It can't tell whether an app *works*,
  whether a release APK actually installs, or whether R8 broke something at runtime.
- Doesn't build, so it can't detect a broken build — only a suspicious configuration.
- Can't tell whether a shipped-but-untagged version *should* have been tagged. It reports
  the gap; the judgment is the user's.
- The known-exceptions list is a snapshot (2026-08-06) kept by hand in `checks.md`. When
  the fleet legitimately changes, that file needs updating — the audit will say so by
  reporting a "resolved" exception.
- Reads `apps-hub`'s two registries but can't tell whether an app is *meant* to be in the
  hub.
