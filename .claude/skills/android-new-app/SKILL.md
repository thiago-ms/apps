---
name: android-new-app
description: |
  Use this skill when the user wants to create a brand-new Android app inside the
  other-projects monorepo — the repo holding `.sample/` (the official skeleton) plus
  the app submodules watch-up, utilities, site-blocker, gastos, notes, apps-hub.
  Trigger phrases: "cria um app Android novo", "new Android app", "start an app for
  X", "instancia o .sample", "app novo aqui", "/android-new-app".

  Behavior:
  - Copies `.sample/` and applies 4 renames + 3 ordered substitution passes with
    hit-count assertions (15 + 16 + 16 occurrences, verified against the template).
  - Then the judgment pass: color palette, adaptive-icon vectors, manifest
    permissions, and a from-scratch README.
  - Validates with a real Docker build (`make apk`) before reporting done.
  - Optionally registers the app: git init inside the dir, `gh repo create
    thiago-ms/<dir> --private`, `git submodule add`.

  Do not invoke when:
  - The app already exists and needs a screen or module — that is
    `/android-new-feature` or `/android-new-core`.
  - The user wants a Node/TypeScript/web project — that is `/project-starter`.
  - The user wants an Android project outside this monorepo — there is no template
    for that; say so instead of improvising.

  NEVER derive the package leaf or the artifact slug from the directory name —
  `utilities` is `br.com.utils` with slug `utilitarios`. NEVER substitute inside
  `docker/Dockerfile`, `gradle/`, `libs.versions.toml`, `gradle.properties`, the
  root `build.gradle.kts`, `proguard-rules.pro` or the mipmap XMLs. NEVER stage the
  new dir in the superproject before running `git init` inside it. NEVER print the
  keystore password.
version: 0.1.0
allowed-tools: [Bash, Read, Write, Edit, Grep, Glob, AskUserQuestion]
argument-hint: <optional app name>
---

# android-new-app

Instantiates `.sample/` into a new Android app that builds green and is registered as
a submodule.

**The invariant:** the three tokens — `{{leaf}}` (package leaf), `{{Pascal}}` (display
name), `{{slug}}` (artifact slug) — are **supplied by the user, never derived
silently**. They diverge from the directory name in 3 of the 6 existing apps.
Directory and file renames happen **before** any text substitution, and the three
substitution passes run in a fixed order because pass C would corrupt the package if
it ran before pass A. Nine paths are never touched.

The template is the source of truth: everything comes from `.sample/` by copy. This
skill owns the *transformation*, not the content.

---

## When to use

- **"cria um app Android novo pra X"**, **"new app for tracking Y"** — the whole
  instantiation.
- The user describes an app idea and expects a working skeleton in this monorepo.

## Do not invoke when

- The app exists → `/android-new-feature`, `/android-new-core`.
- It's a Node/TS/web project → `/project-starter`.
- It's an Android project outside this monorepo → no template exists; say so (**E15**).
- The user only wants to change the palette or icon of an existing app → that's a
  direct edit, no skill needed.

---

## Workflow

### Step 1 — Preflight

Resolve the root and hard-fail if this isn't the right repo:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

Then, in parallel and read-only:

- `grep -h 'applicationId' "$ROOT"/*/app/build.gradle.kts` — collision check (**E3**).
- `grep -h -o '"[a-z]*-\$version-debug\.apk"' "$ROOT"/*/app/build.gradle.kts` — slug
  collision (**E4**).
- `docker info` — if down, still generate; the build validation becomes pending (**E5**).
- `git status --short` at the root — surface in the plan summary (**E7**).
- `ls "$ROOT/.sample"` — confirm no `build/`, `.gradle/`, `dist/` polluting the
  template (**E16**).
- The token counts, asserted against
  [references/token-map.md](references/token-map.md) (**E9**).

**Do NOT** check for host `java`, `gradle`, `sdkmanager`, `keytool` or `adb` —
everything runs in Docker. **Do NOT** read `docker/Dockerfile` with intent to change
it.

### Step 2 — Round 1 interview

Name, tokens, home shape, palette. Payload verbatim in
[references/interview-flow.md](references/interview-flow.md#round-1).

### Step 3 — Round 1b, only if "Customizar" was picked

Directory, package leaf, slug as three separate questions
([#round-1b](references/interview-flow.md#round-1b--só-quando-customizar-foi-escolhido)).
This split is forced by the 4-question cap, not a design accident.

### Step 4 — Round 2 interview + plan summary

Permissions, git registration, merged confirm
([#round-2](references/interview-flow.md#round-2)).

Print a ≤20-line summary first: the three resolved tokens, the palette, the
permissions, the git scope, and the dirty-tree note.
**Do not touch the filesystem until "Seguir" has been picked.**

### Step 5 — Copy and rename

`cp -r .sample <dir>` (or the `rsync --exclude` form if the template is polluted,
**E16**). Then the 4 renames — three source directories plus
`SampleApp.kt` → `{{Pascal}}App.kt` — per
[references/token-map.md](references/token-map.md#passo-2--renomeações-4).

Renames before substitutions. This ordering is load-bearing: a leftover
`br/com/sample/` directory makes the residual sweep report false positives.

### Step 6 — The three substitution passes, in order

Per [references/token-map.md](references/token-map.md), asserting the count after each:

| pass | de → para | ocorrências | arquivos |
|---|---|---|---|
| A | `br.com.sample` → `br.com.{{leaf}}` (ancorado) | **15** | 10 |
| B | `Sample` → `{{Pascal}}` (case-sensitive) | **16** | 11 |
| C | `sample` → `{{slug}}` | **16** | 4 |

A before C is mandatory. Pass B is a single pass on purpose — it resolves
`SampleTheme`, `SampleApp`, `Theme.Sample` and `rootProject.name` at once.

Count mismatch → stop (**E9**). Failure mid-pass → **E8**, and never `rm -rf` to
"start clean".

### Step 7 — Identity pass (judgment)

Per [references/identity-kit.md](references/identity-kit.md): the 12 palette values in
`Theme.kt`, the two adaptive-icon vectors (background `fillColor` = the light
`primary`), `strings.xml`, permissions at the comment anchor, and
`versionName = "1.0"` / `versionCode = 1` — fixed defaults, never asked.

Then rewrite `README.md` from scratch. **Never** substitute tokens in it: it's a
document *about the template*, and substitution produces nonsense.

### Step 8 — Verify

Residual sweep with `grep -rI` (the `-I` matters — without it `gradlew` matches on the
word "example"), then build:

```bash
cd "$ROOT/<dir>" && make apk
```

Long-running. `make apk` pulls in `image` through the dependency chain
(`apk → dist → wrapper → image`), so on a fresh app it also downloads the Android SDK
(~2 GB) and takes far longer than a normal build. Warn first, and offer the legitimate
shortcut of tagging an existing image since `docker/Dockerfile` is byte-identical
across all 7 apps (**E6**). Run in background with a status.

### Step 9 — Register (gated on the Round 2 answer)

Per [references/repo-registration.md](references/repo-registration.md), in order:
`git init` + `First commit` **inside** `<dir>` first (the trap: staging the
superproject first pulls ~35 app files into it), then `gh repo create
thiago-ms/<dir> --private --source=. --push`, then `git submodule add` + a superproject
commit `App {{Pascal}} 1.0`.

### Step 10 — Report and hand off

State: the three tokens, files generated, palette chosen, the APK path, and what was
registered. Never print the keystore password.

**Say plainly, first line of the report: this is the skeleton, and the app does nothing
yet.** The home screen is a placeholder with a title and one paragraph. Do not describe
the app in the present tense as if the described features exist — the `README.md` written
in Step 7 must say the same thing.

Then, **before** anything else in the handoff, close the gap this skill leaves open:

> **Se o pedido descrevia funcionalidades** (listas, backup, contatos, o que for), elas
> **não** foram construídas e **não** existem em lugar nenhum ainda. Nesse caso, ainda
> aqui:
>
> 1. `/android-backlog <dir> create` — o backlog do app nasce junto com o app.
> 2. File cada funcionalidade descrita como item, com o texto do usuário.
> 3. Se veio de um lote, **devolva ao orquestrador a lista desses itens** — ele os põe na
>    fila em série depois do esqueleto. Não é sugestão de fim de relatório: é o retorno da
>    skill.
>
> Entregar o esqueleto e parar, quando o pedido descrevia comportamento, é o modo de falha
> conhecido desta skill — aconteceu em 2026-08-07 com `apps-hub-lists` e `people`.

Then ask separately about registering in `apps-hub` (**E12**) — it edits another
submodule and needs its own release. And hand off:

- persistence → `/android-new-core <dir>/data` (quase sempre o primeiro passo real)
- more screens → `/android-new-feature <dir>/<x>`
- shipping → `/android-release <dir>`
- device: `./adb.sh authorize` → `./adb.sh devices` → `./adb.sh build-install`

---

## Reference files

- [references/token-map.md](references/token-map.md) — the exhaustive per-file site
  table with verified hit counts, the pass ordering and why, the 9 never-touch paths,
  and the residual-sweep allowlist.
- [references/identity-kit.md](references/identity-kit.md) — 5 ready light/dark
  palettes, the icon vector templates with safe-zone rules, the permissions anchor,
  and the README skeleton.
- [references/repo-registration.md](references/repo-registration.md) — the
  `git init`-before-staging trap, `gh repo create`, `submodule add` on an existing
  dir, and the `apps-hub` dual registry.
- [references/edge-cases.md](references/edge-cases.md) — **E1**–**E16**.
- [../README.md](../README.md) — the stable per-app table proving leaf ≠ slug ≠ dir.

---

## Silent quality checklist (run before showing the Step 4 summary)

1. Root resolved; the three markers exist.
2. `<dir>` does not already exist (**E1**).
3. `{{leaf}}` matches `^[a-z][a-z0-9]*$` and `br.com.{{leaf}}` collides with no
   existing app (**E3**).
4. `{{slug}}` matches `^[a-z][a-z0-9]*$` and collides with no existing slug (**E4**).
5. `{{leaf}}` and `{{slug}}` came from answers, not from the directory name.
6. Token counts in `.sample/` match the map: 15 / 16 / 16 (**E9**).
7. `.sample/` has no `build/`, `.gradle/`, `dist/` (**E16**).
8. The plan lists renames **before** substitutions, and pass A before pass C.
9. The plan touches none of the 9 never-touch paths.
10. The palette's light `primary` equals the icon background `fillColor`.
11. `versionName = "1.0"`, `versionCode = 1`.
12. The plan rewrites the README rather than substituting it.
13. No keystore password appears in the summary.
14. The dirty-tree state is in the summary.

If any item fails, fix the plan before showing it.

---

## Anti-patterns

- Don't derive `{{leaf}}` or `{{slug}}` from the directory name.
- Don't run the passes out of order, and don't split pass B by symbol.
- Don't `sed` the README — rewrite it.
- Don't substitute in `docker/Dockerfile`, `gradle.properties`, `gradlew*`,
  `gradle/wrapper/*`, `libs.versions.toml`, the root `build.gradle.kts`,
  `proguard-rules.pro`, `.gitignore`, `.dockerignore`, or the mipmap XMLs.
- Don't stage the superproject before `git init` inside the new dir.
- Don't `rm -rf` the half-built dir to start over without asking (**E8**).
- Don't scaffold `:core:*` modules or extra features here (**E13**, **E14**).
- Don't run gradle, keytool or adb on the host.
- Don't print the keystore password, and don't "fix" the `<slug>123` convention.
- Don't edit `apps-hub` without asking, and never edit only one of its two registries.
- Don't build `.sample/` itself to test — that pollutes the template with `build/`,
  `.gradle/` and `dist/`, which the next `cp -r` would copy into a new app.

---

## Limits

- One app per invocation.
- Delivers the `.sample` skeleton with identity applied — the home screen is a
  placeholder with a title and one paragraph, not the real app. **This limit is the
  skill's known failure mode when the request described features:** the skeleton looks
  like a delivery and isn't one. Step 10 exists to convert those features into filed
  items instead of letting them vanish.
- Doesn't design a database, a network client, or navigation beyond what `.sample`
  ships.
- Hand-written icon path data produces a crude icon. The templates are geometric
  placeholders; say plainly that a real asset is better.
- Doesn't publish to the `apps-hub` registry on its own (that's a change to another
  submodule) and doesn't ship a release.
- Can't validate the build when Docker is down; it says so rather than claiming green.
