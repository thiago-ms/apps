---
name: android-new-core
description: |
  Use this skill when the user wants to add a `:core:*` module — persistence,
  preferences, or other shared infrastructure — to one of the Android apps in the
  other-projects monorepo (the repo holding `.sample/` plus the app submodules
  watch-up, utilities, site-blocker, gastos, notes, apps-hub). Trigger phrases:
  "adiciona Room no gastos", "preciso de banco nesse app", "cria um :core:data",
  "guardar preferências", "add persistence to notes", "/android-new-core".

  Behavior:
  - Scaffolds `:core:data` (Room + KSP, packages model/db/repo/domain) or
    `:core:prefs` (SharedPreferences + StateFlow), distilled from the real
    implementations in watch-up/core/data and utilities/core/prefs.
  - Wires it correctly: settings include, the `api(project(":core:data"))` line in
    `core/ui/build.gradle.kts` (the `.sample` ships it commented), and the root
    `alias(libs.plugins.ksp) apply false` only when missing.
  - Verifies with `make apk` — KSP failures surface only at build time, never in the
    Gradle files.

  Do not invoke when:
  - The user wants a screen or feature module — that is `/android-new-feature`.
  - The user wants a whole new app — that is `/android-new-app`.
  - The user wants to design a real database schema — this scaffolds ONE placeholder
    entity and stops.

  `:core:*` NEVER depends on `:feature:*` or `:app`. A feature NEVER declares
  `:core:data` directly — it sees it only through `api()` in `core/ui`. NEVER use
  `fallbackToDestructiveMigration()`. This is the ONLY skill in the suite that may
  edit `gradle/libs.versions.toml`, and only in `utilities` and `site-blocker`, only
  after asking.
version: 0.1.0
allowed-tools: [Bash, Read, Write, Edit, Grep, Glob, AskUserQuestion]
argument-hint: <app>/<core-name>
---

# android-new-core

Adds one `:core:<x>` module to one app and exposes it correctly.

**The invariant:** `:core:*` sits at the end of the dependency chain — it never depends
on `:feature:*` or `:app`. Features reach `:core:data` **only** through
`api(project(":core:data"))` in `core/ui/build.gradle.kts`, never by declaring the
dependency themselves. If you find yourself wanting that line in a feature module, the
`:core:ui` wiring is missing.

This is the lowest-frequency skill in the suite (`:core:data` exists in 3 of 6 apps,
`:core:prefs` in 1) and the highest value per use: the wiring is exactly the forgettable
part, and KSP failures don't show up until the build.

---

## When to use

- **"esse app precisa guardar dados"**, **"adiciona Room"** → `:core:data`.
- **"guardar um liga/desliga"**, **"preferências"** → `:core:prefs`.
- A domain-specific shared module like `gastos`'s `:core:location` / `:core:backup`.

## Do not invoke when

- The target is a screen → `/android-new-feature`.
- The target is a new app → `/android-new-app`.
- The user wants a real schema designed — this stops at one placeholder entity.
- The cwd resolves outside this monorepo → stop, see Step 1.

---

## Workflow

### Step 1 — Preflight

Resolve the root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

Then, read-only, for the target app:

- `settings.gradle.kts` — existing `:core:*` includes; stop if `core/<x>/` exists.
- `app/build.gradle.kts` — the `namespace` (this is `{{pkg}}`).
- `settings.gradle.kts` — `rootProject.name` (this is `{{Pascal}}`).
- `core/ui/build.gradle.kts` — is the `api(project(":core:data"))` line present,
  commented (as in `.sample:34`), or absent?
- `build.gradle.kts` (root) — does it already declare `alias(libs.plugins.ksp) apply false`?
- `gradle/libs.versions.toml` — **do the Room/KSP coordinates exist?**
  ```bash
  grep -cE 'room-runtime|room-ktx|room-compiler|^ksp' <app>/gradle/libs.versions.toml
  ```
  Present in `.sample`, `notes`, `watch-up`, `gastos`, `apps-hub`. **Absent in
  `utilities` and `site-blocker`** — for those, `:core:data` requires a catalog edit,
  which changes the interview (see Step 2).
- `git status --short` — surface in the plan summary.

**Do NOT** plan a catalog edit for an app that already has the coordinates. **Do NOT**
touch `gradle.properties`. **Do NOT** build yet.

### Step 2 — Round 1 interview

Module kind, exposure, merged confirm. Payload verbatim in
[references/interview-flow.md](references/interview-flow.md#round-1).

If the app is `utilities` or `site-blocker` **and** the kind is `:core:data`, use the
variant description that names the catalog edit, and then ask the catalog question
**separately** — not folded into the general confirm. Always offer `:core:prefs` as the
alternative that needs no catalog change.

### Step 3 — Round 2, only for `:core:data`

First entity name + confirm
([#round-2](references/interview-flow.md#round-2--só-para-coredata)).

Print a ≤20-line plan summary: files to create, wiring edits, whether the catalog and
root plugin are being touched, and the dirty-tree note.
**Do not touch the filesystem until "Seguir" has been picked.**

### Step 4 — Write the module

Per [references/core-recipes.md](references/core-recipes.md). For `:core:data`, six
files across four packages:

```
core/data/build.gradle.kts
core/data/src/main/kotlin/<pkg-path>/core/data/model/Model.kt
                                              /db/<Pascal>Dao.kt
                                              /db/<Pascal>Database.kt
                                              /repo/<Pascal>Repository.kt
                                              /domain/<Pascal>Logic.kt
core/data/src/test/kotlin/<pkg-path>/core/data/domain/<Pascal>LogicTest.kt
```

For `:core:prefs`, two files (`build.gradle.kts` + one store). For any other name, use
the `:core:prefs` build file as the base and say the content is left to write.

### Step 5 — Wire it

Per [references/wiring-rules.md](references/wiring-rules.md):

| # | File | Edit | When |
|---|---|---|---|
| 1 | `settings.gradle.kts` | `include(":core:<x>")` | always |
| 2 | `core/ui/build.gradle.kts` | `api(project(":core:<x>"))` — uncomment the `.sample` line or add it | exposure = via `:core:ui` |
| 3 | `app/build.gradle.kts` | `implementation(project(":core:<x>"))` | exposure = `:app` only; also the default for `:core:data`, following notes and watch-up |
| 4 | `build.gradle.kts` (root) | `alias(libs.plugins.ksp) apply false` | `:core:data` **and** the line is missing |
| 5 | `gradle/libs.versions.toml` | the 6 Room/KSP entries | `:core:data` in `utilities` / `site-blocker` **only**, after the explicit yes |

Each edit is check-then-insert. Duplicating the root KSP alias is a Gradle
configuration error, so the "only when missing" guard is load-bearing.

### Step 6 — Verify

```bash
cd "$ROOT/<app>" && make apk
make test    # only if the module ships junit (:core:data does)
```

**This is where KSP failures appear** — the Gradle files stay green while
`@Dao`/`@Database` silently generate nothing. The symptom is
`Unresolved reference: <Pascal>Database_Impl`. The error table in
[references/wiring-rules.md](references/wiring-rules.md#os-erros-e-o-que-cada-um-produz)
maps each mistake to its message.

Long-running (~2–5 min with a warm image). Run in background with a status.

### Step 7 — Report and hand off

State the files created, the wiring applied (naming the catalog and root edits
explicitly if they happened), and what a feature should import — for `:core:data`, the
repository, never the Dao or the Database.

Then: build the screen on top with `/android-new-feature <app>/<x>`.

Do not commit.

---

## Reference files

- [references/core-recipes.md](references/core-recipes.md) — verbatim
  `build.gradle.kts` and file skeletons per kind, distilled from `watch-up/core/data`
  and `utilities/core/prefs`.
- [references/wiring-rules.md](references/wiring-rules.md) — `api()` vs
  `implementation`, the root KSP rule, the catalog exception per app, and the table of
  what each mistake produces.
- [references/interview-flow.md](references/interview-flow.md) — verbatim payloads,
  including the catalog-edit question and the "Other" handling table.
- [../README.md](../README.md) — the stable per-app table.

---

## Silent quality checklist (run before showing the plan summary)

1. Root resolved; the three markers exist.
2. `core/<x>/` does not already exist.
3. `{{pkg}}` came from the app's real `namespace`; `{{Pascal}}` from
   `rootProject.name`.
4. The catalog was **checked**, and a catalog edit is in the plan **only** for
   `utilities`/`site-blocker` with `:core:data`, **only** after an explicit yes.
5. The root KSP alias is in the plan only if `:core:data` **and** it's missing.
6. For `:core:data`, the plan includes `alias(libs.plugins.ksp)` in the module and
   **excludes** `kotlin.compose` and `buildFeatures { compose }`.
7. For `:core:prefs`, the plan has exactly 2 plugins, 1 dependency, no test sourceSet.
8. The `api()` edit targets `core/ui/build.gradle.kts`, never a feature module.
9. The repository uses no `fallbackToDestructiveMigration()`.
10. Exactly one entity, with `id` + `nome` + two dates. No invented columns.
11. The dirty-tree state is in the summary.

If any item fails, fix the plan before showing it.

---

## Anti-patterns

- Don't design a schema. One placeholder entity, then stop.
- Don't add `alias(libs.plugins.kotlin.compose)` or `buildFeatures { compose }` to a
  `:core:data` — it has no UI.
- Don't add Room coordinates to a catalog that already has them (5 of 7 apps).
- Don't edit the catalog at all without the explicit yes.
- Don't declare `:core:data` from a feature module.
- Don't duplicate the root `alias(libs.plugins.ksp) apply false`.
- Don't use `fallbackToDestructiveMigration()` — it silently wipes user data on the
  first schema bump, and these apps do real migrations (`watch-up` went v4 → v8).
- Don't expose the Dao or the Database to features — the repository is the API.
- Don't introduce Hilt, Dagger, or Koin. No app here uses dependency injection;
  singletons take `applicationContext`.
- Don't run `make dist` or commit.

---

## Limits

- One module per invocation.
- Scaffolds structure, not domain logic: one entity, a CRUD Dao, a repository
  singleton, and one trivial pure function with its test.
- Doesn't write migrations. The first schema is version 1; the second version needs a
  hand-written `Migration`.
- For `:core:location`, `:core:backup` and other domain-specific modules it produces
  only the build file and the package skeleton — the content is left to write.
- Doesn't touch `gradle.properties`, the Dockerfile, or the Makefile.
- Doesn't work on `site-blocker` without a warning: it is single-module
  (`include(":app")` only), so adding any `:core:*` there is a real architectural
  change and needs the user's explicit go-ahead.
