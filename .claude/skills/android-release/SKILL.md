---
name: android-release
description: |
  Use this skill when the user wants to ship / roll out a version of one of the
  Android apps in the other-projects monorepo (the repo holding `.sample/` plus the
  app submodules watch-up, utilities, site-blocker, gastos, notes, apps-hub).
  Trigger phrases: "ship notes 1.7", "roll out a version", "sobe a versão do
  watch-up", "gera o release do gastos", "entrega essa versão",
  "/android-release".

  Behavior:
  - Bumps versionName AND versionCode, runs `make dist-all`, gates on a human
    confirming the release APK ran on a device, then publishes to both `.dist/`
    hubs, tags the submodule, and commits the superproject pointer.
  - Reads the current version, tags, and commit-message style from the repo
    instead of assuming them.
  - The device gate serves the APK over Wi-Fi with `./server.sh` (the default — no cable
    needed); `./adb.sh install <apk>` over USB only if asked.

  Do not invoke when:
  - The user wants to add a feature or module — that is `/android-new-feature` or
    `/android-new-core`.
  - The user only wants a commit message (`/write-commit-message`) or a pull
    request (`/create-pr`) — these repos are trunk-based and this skill pushes
    directly.
  - The user only wants to build or install without shipping — that is `make apk`
    or `./adb.sh build-install`, no skill needed.

  NEVER overwrite an existing versionName. NEVER delete any `dist/`, `build/` or
  APK without asking first, and NEVER run `make clean` or `git clean` — a real
  incident on 2026-07-05 wiped three apps' `dist/`. NEVER push the superproject
  before the submodule. NEVER skip the device gate silently.
version: 0.1.0
allowed-tools: [Bash, Read, Edit, Grep, Glob, AskUserQuestion]
argument-hint: <app> [versionName]
---

# android-release

Ships one version of one app: bump → build → device gate → both hubs → tag →
superproject commit → backlog history row.

**The invariant:** versioning is manual and monotonic — an existing `versionName` is
never overwritten — and **nothing is deleted without asking first**. The release APK
is not published until a human confirms it ran on a device: `isMinifyEnabled` and
`isShrinkResources` are on in all seven apps, and R8 breaks reflection only at
runtime, so a release that compiles can still crash on open.

Note this skill has **no `Write` tool**. A release edits `app/build.gradle.kts` and a
backlog file and copies APKs; it never authors a file. If an app has no backlog, this
skill does not create one.

---

## When to use

- **"ship \<app\> \<version\>"**, **"roll out"**, **"entrega a versão"** — the whole
  ritual end to end.
- The user finished a change, it builds, and they want the deliverable.
- The user asks "what's missing to ship X?" — run Step 1 only and report.

## Do not invoke when

- Adding features or modules → `/android-new-feature`, `/android-new-core`.
- The user wants only a commit message or a PR → `/write-commit-message`,
  `/create-pr`.
- The user wants only to build or install → `make apk`, `./adb.sh build-install`.
- Grooming the backlog outside a release → `/android-backlog`.
- The cwd resolves outside this monorepo → stop, see Step 1.

---

## Workflow

### Step 1 — Preflight (read-only, parallel)

Resolve the root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

Then collect everything at once — the full command list is in
[references/release-checklist.md](references/release-checklist.md#1-preflight--read-only):
current `versionName`/`versionCode`, `git tag --list`, `git log -1 --format=%s` in
**both** submodule and superproject (to mirror the message style rather than assume
it), `ls dist/ .dist/ $ROOT/.dist/`, `git status --short`, the backlog file if any,
`docker info`, and the Makefile's `distApk` form.

Resolve the app's **slug** from `app/build.gradle.kts`'s `distApk` block — it is not
the directory name (`watch-up` → `watchup`, `utilities` → `utilitarios`). Table in
[../README.md](../README.md).

**NEVER** `rm`, `git clean`, `make clean`, or `git stash` — not here, not anywhere in
this skill. **Do NOT** build yet. **Do NOT** check host `java`/`gradle`/`adb`.

If the user asked "what's missing to ship?", report and stop here.

### Step 2 — Round 1 interview

Version, code, summary source. Payload verbatim in
[references/interview-flow.md](references/interview-flow.md#round-1--a-versão).
Compare versions as **integer pairs** — `1.10 > 1.9` is true in this 2-place scheme
and false in decimal arithmetic. See
[references/versioning-rules.md](references/versioning-rules.md).

### Step 3 — Round 2 interview + plan summary

Backlog items closed (only if the app has a backlog with pending items), git scope,
and the merged confirm
([#round-2](references/interview-flow.md#round-2--escopo--confirmação)).

Print a ≤20-line summary first, including the dirty-tree state from Step 1.
**Do not touch the filesystem until "Seguir" has been picked.**

### Step 4 — Bump, with four assertions

Edit both lines in `<app>/app/build.gradle.kts`. All four must pass first: new
version strictly greater; new code strictly greater; version is not an existing tag;
no artifact for that version already in `dist/`, `.dist/`, or `$ROOT/.dist/`. Any
failure → stop and ask.

### Step 5 — Build

```bash
cd "$ROOT/<app>" && make dist-all
```

Long-running (~3–8 min). Run in background with a "compilando debug + release…"
status. Then assert both artifacts exist **by slug**. If the release APK is
debug-sized (~17 MB rather than ~1–2 MB), R8 didn't run — investigate, don't publish.

### Step 6 — Device smoke gate (Round 3)

Serve the release APK so the user can install it from the phone — the default path, and
the one that needs no cable:

```bash
cd "$ROOT/<app>" && ./server.sh      # em background: ele bloqueia
```

Give the user `http://<ip>:8000/` **and the exact filename** to tap
(`<slug>-<ver>-release.apk`) — the generated index lists every APK in `dist/`, debug and
old versions included. Then ask
([#round-3](references/interview-flow.md#round-3--o-gate-de-smoke-no-aparelho)), and
**kill the server after the answer by port** — `fuser -k 8000/tcp` (or whichever port was
used). A batch with sequential releases collides otherwise. Never
`pkill -f 'http.server'`: it leaves the `python3` child orphaned holding the port, and the
pattern matches the calling shell's own command line, so it kills the caller.

USB only if asked: `./adb.sh install "dist/<slug>-<ver>-release.apk"` — the artifact
already exists from Step 5, so `build-install-release` would rebuild for nothing.
`utilities` has no `adb.sh` at all.

This round **cannot** move earlier: it asks about an artifact that doesn't exist until
Step 5. Don't "optimize" it into Round 2.

Answer "no" → stop. The bump stays (the version is deliberately burned; an artifact
with that name exists). Nothing is deleted. See
[references/artifact-safety.md](references/artifact-safety.md#o-que-fazer-quando-o-gate-do-device-reprova).

### Step 7 — Publish to both hubs

Copy the release APK to `<app>/.dist/` and `$ROOT/.dist/`. Each holds **exactly one**
APK per app, so the superseded file must go — **ask first, name the exact file, never
batch it**. Then regenerate `$ROOT/.dist/index.html` with the inline loop from
[references/release-checklist.md](references/release-checklist.md#7-publicar-nos-dois-hubs)
so the diff matches what `server.sh` would produce. Assert one APK per app in the hub.

### Step 8 — Git, submodule before superproject

Submodule: `git add -A`, commit `Release <ver>`, `git tag <ver>` (bare, no `v`), push
`--follow-tags`. Superproject: stage `<app>` + `.dist/`, commit
`App <rootProject.name> <ver>`, push. **This order is mandatory** — pushing the
superproject first leaves a dangling pointer.

Honor the git scope from Round 2; if it was local-only, stop before the pushes and say
what's pending.

### Step 9 — Backlog sync

Only if the app has `specs/backlog-*.md` (today: `watch-up`, `notes`). Flip the chosen
items to the app's **own dialect**, append one `Histórico de entregas` row, and for
`notes` update the trailing "Versão atual" line in the same edit. Rules in
[references/backlog-sync.md](references/backlog-sync.md).

### Step 10 — Report

Version shipped, both APKs, what entered and left each hub, tag, commits and pushes,
backlog items closed. Then both install paths (per
[references/release-checklist.md](references/release-checklist.md#10-reportar)) — for
`utilities`, mention only `./server.sh`. Also mention any untagged-version gap found
in Step 1, with the command to close it, without running it.

---

## Reference files

- [references/release-checklist.md](references/release-checklist.md) — the ordered
  ritual with every assertion, the verified shape of the superproject release commit,
  and the Makefile `distApk` divergence.
- [references/versioning-rules.md](references/versioning-rules.md) — the 2-place
  scheme, the `versionCode` = minor+1 rule (6 of 7), and how to ship when the
  *previous* version was never tagged.
- [references/artifact-safety.md](references/artifact-safety.md) — what `dist/` vs
  app `.dist/` vs root `.dist/` each mean, the never-delete law and the 2026-07-05
  incident, and what to do when the device gate fails.
- [references/backlog-sync.md](references/backlog-sync.md) — the three release-time
  edits and the two "done" dialects.
- [references/interview-flow.md](references/interview-flow.md) — verbatim payloads for
  all three rounds and the "Other" handling table.
- [../README.md](../README.md) — the stable per-app table (slug, `rootProject.name`,
  which apps have `adb.sh`).

---

## Silent quality checklist (run before showing the Step 3 summary)

1. Root resolved; the three markers exist.
2. The slug came from `app/build.gradle.kts`, not from the directory name.
3. `<rootProject.name>` for the commit message came from `settings.gradle.kts`
   (`apps-hub` → `Apps Hub`, with a space).
4. The proposed version compares as an integer pair, not a float.
5. The proposed version is not an existing tag and has no artifact anywhere.
6. The proposed `versionCode` is strictly greater than current.
7. Commit-message style was read from `git log`, not assumed.
8. The plan contains no `rm`, `clean`, `stash`, or `checkout`.
9. Every backlog item chosen exists and is not already `feito`.
10. The dirty-tree state from Step 1 is in the summary.
11. The plan pushes the submodule before the superproject.
12. The gate plan serves over `./server.sh` in the background, names the exact APK
    filename, and kills the server afterwards — never rebuilds the APK to install it.

If any item fails, fix the plan before showing it.

---

## Anti-patterns

- Don't compute and apply a version without confirmation.
- Don't reuse a `versionName`, ever.
- Don't `rm -rf dist/`, `make clean`, `git clean`, or `git stash` — including "to get
  a clean build".
- Don't delete the superseded hub APK without naming it and asking.
- Don't push the superproject before the submodule.
- Don't tag with a `v` prefix — tags are bare (`1.16`).
- Don't open a PR or create a branch.
- Don't skip the device gate silently, and don't move it earlier.
- Don't create a backlog file, and don't create `specs/` in `utilities`.
- Don't touch other apps' entries in `$ROOT/.dist/`.
- Don't retroactively tag an old untagged version — report it instead.
- Don't "fix" `site-blocker`'s unqualified `distApk` during a release.

---

## Limits

- One app per invocation.
- Can't verify the app works — only a human can answer the device gate. "Skip the
  gate" is offered but named as not recommended.
- Doesn't write code or fix a failed build; it reports Gradle's output verbatim.
- Doesn't create or restructure a backlog — `/android-backlog` does.
- Doesn't rotate keystores. The `<slug>123` passwords are weak but every published
  APK is signed with them, so rotating breaks signature continuity for installed
  users.
- Doesn't touch `apps-hub`'s `HUB_APPS` / `<queries>` registry. A **new app** needs
  registering there (`/android-new-app` handles it); a version bump does not.
