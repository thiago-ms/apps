# Skills deste repositório

Sete skills que automatizam os rituais mecânicos deste monorepo de apps Android.
Elas **só funcionam aqui** — cada uma resolve a raiz do superprojeto e aborta se
não encontrar `.sample/` + `.gitmodules` + `server.sh`.

Seis fazem **um** trabalho cada. A sétima (`android-batch`) é a camada de cima: recebe um
lote de demandas misturadas e roteia para as outras.

| Skill | Faz | Nunca faz |
|---|---|---|
| `android-batch` | um lote inteiro: triagem → backlog → execução → entrega por app | decidir produto, refactor amplo, migração de dependência |
| `android-new-app` | `.sample/` → app novo que builda verde, registrado como submódulo | módulos `:core:*`, features extras, release |
| `android-new-feature` | um `:feature:<x>` + o wiring de navegação | módulos `:core:*`, build além do `make apk` |
| `android-new-core` | um `:core:<x>` + exposição via `api()` + wiring do KSP | navegação, desenhar schema além de 1 entidade |
| `android-release` | bump → `dist-all` → gate no device → hubs `.dist/` → tag → commit do superprojeto | criar arquivo, apagar algo sem perguntar, abrir PR |
| `android-backlog` | criar/manter `specs/backlog-*.md` com numeração estável | build, commit, mudança de status em hora de release |
| `android-audit` | relatório read-only de drift na frota | consertar qualquer coisa |

## A camada de orquestração

`android-batch` + dois subagentes em [`../agents/`](../agents/):

| Agente | Papel | Ferramentas |
|---|---|---|
| [`android-scout`](../agents/android-scout.md) | escopa **um** item e devolve mapa destilado (arquivos, causa, raio de alcance, dúvidas) | read-only |
| [`android-implementer`](../agents/android-implementer.md) | implementa **um** item em **um** app e builda sob lock | escreve, mas nunca entrega |

Três restrições decidem essa divisão, e vale entendê-las antes de mexer:

1. **As 6 skills entrevistam** (`AskUserQuestion`). Subagente não tem interlocutor — logo
   **nenhuma skill roda dentro de subagente**. Tudo que precisa de skill (app novo, módulo
   novo, backlog, release) fica no loop principal.
2. **O gate de aparelho do release é humano** — R8 quebra reflexão só em runtime.
3. **Não há PR nestes repos** (trunk-based). O ledger de estado é o **backlog** (permanente,
   por app) mais um ledger de lote em `.claude/work/` (descartável, atravessa apps).

Sobra para subagente o que é caro em contexto e não precisa de você: **investigar** e
**escrever código**.

Paralelismo: scouts todos de uma vez; implementadores em paralelo **entre** apps e um por
vez **dentro** de cada app; **builds serializados** por `flock /tmp/other-projects-build.lock`
— builds concorrentes seriam corretos (imagem Docker e `.gradle/` por app), o lock existe
por contenção de CPU/disco.

## Por que ficam aqui e não em `~/.claude/skills/`

Deliberado: **sem symlink, sem nada fora deste repo.** Os invariantes que essas
skills carregam são fatos *deste* código — o mapa de tokens do `.sample/`, a tabela
de slugs, a ordem submódulo-antes-de-superprojeto. Morando aqui, eles mudam no mesmo
commit que o código que descrevem.

**Consequência:** skill de projeto é descoberta a partir da raiz do projeto da sessão.
Cada app é um repo git próprio, então uma sessão aberta *dentro* de um submódulo pode
não listar as `android-*`. **Abra a sessão na raiz do superprojeto** — é o modo natural,
já que todas operam em caminhos `<app>/...` relativos a ela.

## Mapeamento estável dos apps

Consultar antes de derivar qualquer nome. As três derivações ingênuas são **falsas**:
o `applicationId` não é o nome do diretório (`utilities` → `br.com.utils`), o slug de
artefato não é o nome do diretório (`utilities` → `utilitarios`, `watch-up` → `watchup`),
e o `rootProject.name` pode ter espaço (`apps-hub` → `Apps Hub`).

| dir | applicationId | slug de artefato | rootProject.name | arquivo de nav | backlog |
|---|---|---|---|---|---|
| `watch-up` | `br.com.watchup` | `watchup` | `WatchUp` | `WatchUpApp.kt` | `specs/backlog-ajustes.md` (dialeto simples) |
| `utilities` | `br.com.utils` | `utilitarios` | `Utilitarios` | `UtilitariosApp.kt` | `docs/` (sem backlog) |
| `site-blocker` | `br.com.siteblocker` | `siteblocker` | `SiteBlocker` | — (single-module) | — |
| `gastos` | `br.com.gastos` | `gastos` | `Gastos` | `GastosApp.kt` | — |
| `notes` | `br.com.notes` | `notes` | `Notes` | `NotesApp.kt` | `specs/backlog-ajustes.md` (dialeto com versão) |
| `apps-hub` | `br.com.appshub` | `appshub` | `Apps Hub` | `AppsHubApp.kt` | — |
| `.sample` | `br.com.sample` | `sample` | `Sample` | `SampleApp.kt` | — |

**`versionName` e `versionCode` não estão nesta tabela de propósito** — mudam a cada
release e qualquer cópia aqui estaria errada na maior parte do tempo. Ler sempre ao vivo:

```bash
grep -E 'versionName|versionCode' <app>/app/build.gradle.kts
```

Idem para a lista de módulos (`settings.gradle.kts`) e para as tags (`git -C <app> tag --list`).

## Ferramental por app — nem todos têm tudo

| | `adb.sh` | `server.sh` | `build.sh` |
|---|---|---|---|
| `watch-up`, `notes`, `gastos`, `apps-hub` | ✅ | ✅ | ✅ |
| `site-blocker` | ✅ | ✅ | ❌ |
| `utilities` | ❌ | ✅ | ✅ |

`utilities` não tem `adb.sh` — instalar por USB não existe lá; cair pro `./server.sh`.
Nenhuma skill deve *criar* esses scripts num app sem o usuário pedir.

**`./server.sh [porta]`** — porta **8000** por default, sobrescrevível
(`./server.sh 8123`). Vale nos 8 (raiz + `.sample` + 6 apps). O script **bloqueia** em
foreground, então quem chama roda em background e mata depois **por porta**
(`fuser -k 8000/tcp` — nunca `pkill -f 'http.server'`, que deixa o `python3` filho órfão
e casa a linha de comando de quem chama, matando o próprio shell). O override existe para desviar de algo que já ocupa a
porta — não para abrir dois gates de release ao mesmo tempo.

Alvos do Makefile são uniformes nos 7: `help image wrapper apk dist apk-release
dist-release dist-all keystore test clean shell`.

## Convenções de autoria

Formato igual às skills globais em `~/.claude/skills/` (inglês, `SKILL.md` +
`references/*.md`, sem `scripts/` nem `assets/`):

- frontmatter: `name` (== pasta == H1) → `description: |` → `version` → `allowed-tools` → `argument-hint`
- corpo: lede com o invariante → `## When to use` / `## Do not invoke when` → `## Workflow`
  (`### Step N — <Verbo>`, Step 1 sempre Preflight) → `## Reference files` →
  `## Silent quality checklist` → `## Anti-patterns` → `## Limits`
- `SKILL.md` entre 170 e 240 linhas; o que passar disso vai pra `references/`
- interview: ≤2 rounds de `AskUserQuestion`, payloads verbatim em
  `references/interview-flow.md`, `(Recommended)` dentro do **label**, gate de
  confirmação fundido como última pergunta do último round

**Os templates gerados ficam em português** (comentários, labels de aba, README dos
apps) — é o idioma do código existente.
