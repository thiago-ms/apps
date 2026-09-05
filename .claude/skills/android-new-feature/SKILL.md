---
name: android-new-feature
description: |
  Use this skill when the user wants to add a new screen or feature module to one
  of the Android apps in the other-projects monorepo (the repo holding `.sample/`
  plus the app submodules watch-up, utilities, site-blocker, gastos, notes,
  apps-hub). Trigger phrases: "add a feature to notes", "nova tela no watch-up",
  "cria um módulo de feature", "new screen in gastos", "/android-new-feature".

  Behavior:
  - Creates `feature/<x>/build.gradle.kts` + `<X>Screen.kt` and applies the 3–5
    navigation wiring edits (settings include, :app dependency, Routes constant,
    TabDestination entry, NavHost composable).
  - Copies the module build file from `.sample/feature/home/build.gradle.kts`
    rather than authoring it, so the 38-line boilerplate never drifts.
  - Every wiring edit is check-then-insert, so re-running is a no-op.
  - Verifies with `make apk` in Docker before reporting done.

  Do not invoke when:
  - The user wants a `:core:*` module (Room, prefs, location) — that is
    `/android-new-core`.
  - The user wants a whole new app — that is `/android-new-app`.
  - The user wants to add a screen *inside* an existing feature module — that is
    one file next to its siblings, no wiring, no skill needed.
  - The user wants to ship a version — that is `/android-release`.

  NEVER edit `gradle/libs.versions.toml` — every coordinate this skill needs
  already exists in all 7 catalogs. NEVER let a feature module import `:app` or
  another feature. NEVER put a NavController inside a feature.
version: 0.1.0
allowed-tools: [Bash, Read, Write, Edit, Grep, Glob, AskUserQuestion]
argument-hint: <app>/<feature-name>
---

# android-new-feature

Adds one `:feature:<x>` module to one app and wires it into navigation.

**The invariant:** a feature module depends on `:core:*` and nothing else — never
`:app`, never another feature. Navigation lives in `:app`; screens receive
callbacks (`onBack`, `onOpenDetail`) as parameters and never touch a
`NavController`. Everything this skill writes either comes from `.sample/` by copy
or from [references/module-templates.md](references/module-templates.md); nothing is
improvised.

---

## When to use

- **"add a feature/screen/tab to \<app\>"** — the common case.
- **"nova tela de busca no watch-up"**, **"aba de configurações no notes"** — same
  thing in Portuguese.
- The user names a module that doesn't exist yet and expects it wired into the
  bottom navigation or reachable by push.

## Do not invoke when

- The target is a `:core:*` module → `/android-new-core`.
- The target is a whole new app → `/android-new-app`.
- The screen belongs **inside** an existing feature module (no new Gradle module,
  no wiring) → just write the file next to its siblings.
- The user is shipping → `/android-release`.
- The cwd resolves outside this monorepo → stop, see Step 1.

---

## Workflow

### Step 1 — Preflight

Resolve the superproject root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

If the assertion fails, say *"this skill only runs inside the other-projects Android
monorepo"* and end. Do not try to guess a root.

Then, in parallel and read-only, for the target app:

- `settings.gradle.kts` — existing `:feature:*` includes, and where to insert.
- `app/build.gradle.kts` — the `namespace` (this is `{{pkg}}`), and the existing
  `project(":feature:...")` lines.
- `app/src/main/kotlin/<pkg>/navigation/Routes.kt` — existing constants and tabs.
- the nav shell file (`NotesApp.kt`, `WatchUpApp.kt`, … — see the table in
  [../README.md](../README.md)) — existing `composable(...)` blocks.
- `.sample/feature/home/build.gradle.kts` — the byte-template.
- `git status --short` — capture it; a dirty tree makes the `warn-before-changes`
  hook prompt later, so surface it in the Step 3 summary instead of letting it
  surprise the user.

**Do NOT** read `gradle/libs.versions.toml` with intent to change it. **Do NOT**
check for host `java`, `gradle`, or `adb` — all builds run in Docker. **Do NOT**
build yet.

If the app diverges from `.sample` (different `compileSdk`/`buildToolsVersion`),
use a sibling `feature/*/build.gradle.kts` from the same app as the template
instead, and say so.

### Step 2 — Round 1 interview

One `AskUserQuestion` call, 4 questions: feature name, destination, state holder,
tests. Payload verbatim in
[references/interview-flow.md](references/interview-flow.md#round-1).

Sanitize the name (lowercase, no accents, no hyphens) and stop if
`feature/<x>/` already exists — never merge into an existing module.

### Step 3 — Round 2 interview + plan summary

If the destination includes a tab: one call with label, icon, and the merged
confirm ([#round-2](references/interview-flow.md#round-2--só-quando-destino-inclui-aba)).
Otherwise: the confirm alone ([#round-1b](references/interview-flow.md#round-1b--quando-o-destino-não-é-aba)).

Print a ≤20-line summary first: files to create, edits to apply, and the dirty-tree
note from Step 1. **Do not touch the filesystem until "Seguir" has been picked.**

### Step 4 — Write the module files

Per [references/module-templates.md](references/module-templates.md):

1. `cp .sample/feature/home/build.gradle.kts feature/<x>/build.gradle.kts`, then swap
   the `namespace` line only. Add the two test lines *only* when tests=yes, and the
   `lifecycle-viewmodel-compose` dep *only* when state=ViewModel.
2. The screen file(s) under
   `feature/<x>/src/main/kotlin/<pkg-path>/feature/<x>/<X>Screen.kt` — tab screen
   copied from `.sample`'s `HomeScreen.kt`, push and detail screens from the
   templates.
3. `<X>ViewModel.kt` when state=ViewModel; `<X>Logic.kt` +
   `src/test/kotlin/.../<X>LogicTest.kt` when tests=yes.

### Step 5 — Apply the wiring edits

Per [references/nav-wiring.md](references/nav-wiring.md), each one check-then-insert:

| # | File | Edit |
|---|---|---|
| 1 | `settings.gradle.kts` | `include(":feature:<x>")` |
| 2 | `app/build.gradle.kts` | `implementation(project(":feature:<x>"))` |
| 3 | `navigation/Routes.kt` | `const val <X>` + tab entry + icon import (tabs only) |
| 4 | nav shell file | screen import + `composable(Routes.<X>) { ... }` |

Do not touch the four `*Transition = None` lines or the `NavHost`'s
`padding(bottom = ...)` — both are deliberate.

### Step 6 — Verify

Run the grep assertions from
[references/nav-wiring.md](references/nav-wiring.md#verificação-pós-wiring-antes-de-buildar),
then build:

```bash
cd "$ROOT/<app>" && make apk
```

Long-running (~2–5 min with a warm image; the first build in a fresh app also builds
the Docker image and takes longer). Run in background and surface a
"compilando…" status.

If it fails on an unresolved icon reference, go back to the icon question — that's
the one interview answer this skill can't validate up front. Any other failure:
report the Gradle output verbatim, don't guess a fix.

### Step 7 — Report and hand off

State the files created, the edits applied, and the APK path. Then:

- if this closed a backlog item → `/android-backlog <app> done`
- when it's time to ship → `/android-release <app>`

Do not commit. Do not run `make dist` — that is a release concern.

---

## Reference files

- [references/module-templates.md](references/module-templates.md) — the screen,
  detail, and ViewModel skeletons, plus the test-variant lines. **Only what
  `.sample/` doesn't carry** — the module `build.gradle.kts` and the tab-screen
  skeleton come from `.sample/` by copy.
- [references/nav-wiring.md](references/nav-wiring.md) — the 4 anchored edits, the
  dependency-direction law, and the post-wiring assertions.
- [references/interview-flow.md](references/interview-flow.md) — verbatim
  `AskUserQuestion` payloads and the "Other" handling table.
- [../README.md](../README.md) — the stable per-app table (nav file name, package,
  which apps have which tooling).

---

## Silent quality checklist (run before showing the Step 3 summary)

1. The superproject root resolved and the three markers exist.
2. The target app exists and is one of the six (or `.sample` — allowed, but say it
   will change the template itself).
3. `feature/<x>/` does not already exist.
4. `<x>` matches `^[a-z][a-z0-9]*$`.
5. `{{pkg}}` came from the app's real `namespace`, not from the directory name.
6. The nav shell file name came from the app's actual file, not guessed from the
   directory (`utilities` → `UtilitariosApp.kt`, not `UtilitiesApp.kt`).
7. The number of wiring edits matches the destination kind (3 for push, 5 for tab).
8. Nothing in the plan touches `libs.versions.toml`, `docker/`, `gradle/`, or the
   Makefile.
9. The dirty-tree state from Step 1 is in the summary.
10. For state=ViewModel, the 4th dependency line is in the plan; for tests=no, the
    test sourceSet line is not.

If any item fails, fix the plan before showing it.

---

## Anti-patterns

- Don't add a dependency beyond the module's three (`:core:ui`,
  `lifecycle-runtime-compose`, `coroutines-android`) — plus the ViewModel one when
  asked for.
- Don't edit `gradle/libs.versions.toml`. Ever.
- Don't pass a `NavController` into a feature, and don't let one feature import
  another.
- Don't hand-roll a `TopAppBar` in a push screen — `PushScreenScaffold` exists.
- Don't write `composable("literal")` — reference the `Routes` constant.
- Don't add the test sourceSet when tests=no.
- Don't renumber, reorder, or reformat the existing entries in `Routes.kt` or the
  nav shell while inserting into them.
- Don't run `make dist`, `make clean`, or any git command that writes.
- Don't create the module in `.sample/` unless the user explicitly asked to change
  the template.

---

## Limits

- One feature module per invocation. Two features = two runs.
- Doesn't design the screen's real content — it produces a working skeleton with a
  title and a placeholder body.
- Doesn't work on `site-blocker`, which is single-module (`include(":app")` only):
  adding a feature module there is a real architectural change, so it stops and says
  so rather than silently restructuring the app.
- Doesn't know whether an `Icons.Filled.*` name exists; the compiler decides.
- Doesn't commit, tag, or bump versions.
