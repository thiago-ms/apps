---
name: android-batch
description: |
  Use this skill when the user hands over a BATCH of mixed requests about the Android
  apps in the other-projects monorepo — several bugs, adjustments, new features, or a
  new app, all in one message. Trigger phrases: "no watch-up arruma X, no notes
  adiciona Y", "faz essas coisas", "tenho várias demandas", "toca esses ajustes",
  "continua o lote", "/android-batch".

  Behavior:
  - Splits the text into items, infers app and type, then fans out one read-only
    `android-scout` subagent per item IN PARALLEL to scope them against real code.
  - Presents one plan and asks for approval ONCE; after that it runs the whole queue,
    stopping only for product decisions, odd build failures, and each release's device
    gate.
  - Files every item into the target app's backlog via `/android-backlog` (creating one
    where missing) and tracks the batch in a ledger at `.claude/work/`.
  - Executes in parallel ACROSS apps, one item at a time WITHIN an app, with all builds
    serialized through `flock`. Releases per app as each finishes, via
    `/android-release`.
  - Resumable: `/android-batch` with no argument picks up an open ledger.

  Do not invoke when:
  - There is a single, clear task — call the specific skill (`/android-new-feature`,
    `/android-release`, …) or just do it.
  - The user is asking a question rather than requesting work.
  - The request is a broad refactor ("reescreve a navegação") — that is a project, not a
    batch. Say so.

  NEVER write anything before the plan is approved. NEVER let a subagent invoke a skill
  (they all interview the user) or commit, tag, bump a version, or touch `dist/`.
  NEVER run two items of the SAME app in parallel. NEVER run a build outside `flock`.
version: 0.1.0
allowed-tools: [Bash, Read, Write, Edit, Grep, Glob, AskUserQuestion, Agent, Skill]
argument-hint: <o lote em texto livre, ou nada para retomar>
---

# android-batch

Recebe um lote de demandas mistas, organiza, e executa até a entrega.

**O invariante:** o plano é aprovado **uma vez** e depois a fila corre sozinha — mas
nenhum item é implementado antes de estar **escopado por um scout e filado no backlog**.
Item mal entendido custa muito mais caro depois de escrito do que antes, e é justamente
por isso que a investigação é paralela: ela é a parte que pode ser barata.

Esta skill não sabe fazer nada sozinha. Ela roteia: as 6 skills `android-*` fazem o
trabalho que precisa de você, e dois subagentes fazem o que é caro em contexto e não
precisa.

---

## When to use

- **Um lote:** "no watch-up o progresso tá errado, no notes queria uma aba de busca, e o
  ícone do gastos tá ruim" — três apps, três naturezas, uma mensagem.
- **Uma lista:** você cola 6 itens e espera que sejam organizados e executados.
- **Retomar:** `/android-batch` sem argumento, para continuar um lote de outra sessão.
- Vale para **um único item** também, se você quiser o fluxo completo (scout → backlog →
  implementação → release) em vez de mexer no código direto.

## Do not invoke when

- A tarefa é única e clara → a skill específica, ou o trabalho direto.
- É pergunta, não pedido de trabalho → responda.
- É refactor amplo ("reescreve a navegação do watch-up") → é projeto, não lote. Diga e
  proponha tratar separado.
- Só falta entregar código que já está pronto → `/android-release <app>`.
- A CWD resolvida está fora deste monorepo → pare, ver Step 1.

---

## Workflow

### Step 1 — Preflight

Resolva a raiz e aborte se não for este repositório:

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
```

Falhou → *"esta skill só roda dentro do monorepo Android other-projects"* e encerre.

Depois, read-only e em paralelo:

- **Ledgers abertos** — `grep -l 'Estado:.*em execução' "$ROOT"/.claude/work/*.md`. Achou
  e o usuário não passou lote novo → é retomada (ver [ledger-format.md](references/ledger-format.md#retomada-a-frio)).
  Achou **e** veio lote novo → diga que existe lote aberto e pergunte se retoma ou abre outro.
- **Estado da frota** — por app: `versionName`, slug, módulos de `settings.gradle.kts`,
  existência de backlog. A tabela de [../README.md](../README.md) é dica; confira ao vivo.
- `git status --short` no superprojeto e em cada app.
- `docker info` — sem Docker não há build, e portanto não há verificação nem release.
- `command -v flock` — sem ele os builds não podem ser serializados.

**Não** builde. **Não** escreva nada. **Não** chame subagente ainda.

Se a árvore de algum app está suja, diga no resumo do plano — o hook
`warn-before-changes` vai perguntar no primeiro write, e isso não deve ser surpresa.

### Step 2 — Quebrar o lote em itens

Por [references/triage-rules.md](references/triage-rules.md): separar em itens, inferir o
app, classificar o tipo (🐛/✨/🔧) e palpitar a natureza.

App não inferido → `app: ?`, e resolva **agora**, antes dos scouts
([interview-flow.md](references/interview-flow.md#resolver-o-app-de-um-item-ambíguo--antes-dos-scouts)) —
mandar um scout ao repositório errado desperdiça a investigação inteira.

### Step 3 — Triagem em paralelo (scouts)

Um `android-scout` por item, **todos despachados na mesma mensagem**. Prompt-modelo em
[references/dispatch.md](references/dispatch.md#prompt-modelo-do-android-scout). Teto de 4
simultâneos; com mais itens, despache em ondas.

Item `app-novo` **não** tem scout — não há código para investigar.

O que voltar decide o resto: `Natureza` roteia o executor, `Complexidade` alta entra no
resumo, e **`Dúvidas que impedem implementar` bloqueia o item** até ser resolvida.

### Step 4 — O plano, e a única aprovação

Imprima um resumo **≤20 linhas**: itens agrupados por app, o achado de cada scout em uma
linha, números de backlog previstos, versões previstas por app, o que ficou de fora e por
quê, e a árvore suja do Step 1.

Então a confirmação
([interview-flow.md](references/interview-flow.md#a-aprovação-do-plano--a-única-parada-obrigatória)).
**Nada é escrito antes do "Seguir".** Os scouts são read-only, então cancelar aqui deixa o
repositório intacto.

### Step 5 — Filar: ledger e backlogs

**Este é o primeiro write, e ele é no loop principal de propósito** — é o que resolve o
hook `warn-before-changes` uma vez, com você presente, antes de qualquer subagente escrever
([por quê](references/dispatch.md#o-hook-warn-before-changes--resolvido-mas-com-uma-ordem-a-respeitar)).

1. Escreva `.claude/work/<YYYY-MM-DD>-<slug>.md` no formato de
   [references/ledger-format.md](references/ledger-format.md).
2. Para cada app sem backlog (hoje: `gastos` e `apps-hub`): `/android-backlog <app> create`. **`utilities` não** — usa `docs/` deliberadamente; o
   item vive só no ledger e o plano deve dizer isso.
3. Fila os itens: `/android-backlog <app> add`. A skill atribui o número real (N = max+1);
   se divergir do previsto no Step 4, corrija o ledger.

App **novo** é a exceção de tempo, não de regra: o backlog dele não existe agora, então
crie-o e file as funcionalidades **assim que o esqueleto ficar pronto**, no Step 6 — não
deixe para "depois", que é como elas se perdem.

### Step 6 — Executar

Paralelo **entre** apps, um item por vez **dentro** de cada app, builds serializados por
`flock`. Regras completas em [references/dispatch.md](references/dispatch.md#regra-de-ouro-do-paralelismo).

Roteamento por natureza:

| natureza | executor |
|---|---|
| `edicao` | `android-implementer` (subagente) |
| `feature-nova` | **loop principal** → `/android-new-feature <app>/<x>` |
| `core-novo` | **loop principal** → `/android-new-core <app>/<x>` |
| `app-novo` | **loop principal** → `/android-new-app` |

A razão é sempre a mesma: essas três skills entrevistam, e subagente não tem interlocutor.

**`app-novo` não é um item terminal.** `/android-new-app` devolve o esqueleto — e é só
isso que ela promete. Ao receber esse retorno: crie o backlog do app novo
(`/android-backlog <dir> create`), file as funcionalidades que a triagem separou, e siga
para elas em série. Só marque o app como entregue quando os itens de funcionalidade também
estiverem verdes. Esqueleto entregue sozinho é item **incompleto**, não item pronto.

Atualize o estado do item no ledger a cada transição. Retorno `bloqueado` → não redespache;
leia `Bloqueado por` e trate (dúvida de produto vira pergunta; pré-requisito volta para a
triagem com outra natureza). Retorno `falhou` → distinga erro do item (um redespacho com o
erro no prompt) de erro que não é do item
([pergunta](references/interview-flow.md#build-quebrado-por-motivo-não-óbvio)).

### Step 7 — Entregar, app por app

Quando o **último** item de um app fica verde, `/android-release <app>` no loop principal —
gate de aparelho, bump, `dist-all`, hubs, tag, commit, sync do backlog.

Os outros apps **seguem implementando durante o gate**. Essa sobreposição é deliberada: o
tempo humano do gate não deve bloquear a fila de máquina.

**Um release por vez, e a porta é o motivo.** O gate serve o APK com `./server.sh`, que
roda em background e fica preso na porta **8000 por default** — a mesma em todos os apps.
Dois gates abertos colidem, ou pior: o segundo serve o `dist/` do app errado e você
instala o APK errado achando que testou o certo. Então:

- nunca abra o gate de um app com o gate de outro ainda de pé;
- depois de cada resposta de gate, o `/android-release` mata o server
  (`fuser -k 8000/tcp` — por porta, nunca `pkill -f 'http.server'`, que deixa órfão e
  mata o próprio shell) — se você estiver conduzindo à mão, confirme que morreu antes de
  chamar o release seguinte.

`./server.sh <porta>` sobrescreve o default, mas isso serve para **desviar de algo que já
ocupa a 8000**, não para abrir gates em paralelo: o teste é num aparelho só, e duas URLs
simultâneas tornam impossível saber qual APK você instalou.

App que terminou o código enquanto outro está em gate fica `✅ verde` no ledger e espera a
vez na fila de release. Isso não é atraso: é o que impede a colisão.

Se a aprovação do Step 4 foi "sem release", pare no código verde e diga quais
`/android-release <app>` ficaram pendentes.

Release reprovado no aparelho é estado terminal legítimo, não erro a limpar: o bump fica, o
item permanece `✅ verde` no ledger, e a correção vira um lote novo.

### Step 8 — Fechar

Todo item em estado terminal e toda linha de release resolvida → `**Estado:** concluído` e
mova para `.claude/work/done/`. Item `⚠️ bloqueado` sem resolução **impede o fechamento**:
resolva ou marque `❌ fora` com o motivo.

Reporte: o que entrou em cada app, versões publicadas, itens de backlog fechados, desvios
que os implementadores acharam, e **o que ficou de fora e por quê** — nunca em silêncio.

---

## Reference files

- [references/triage-rules.md](references/triage-rules.md) — quebrar o texto em itens,
  inferir app pelo território de domínio, classificar tipo, quando dividir um item, o que
  vira backlog, e como ordenar.
- [references/dispatch.md](references/dispatch.md) — prompts-modelo verbatim dos dois
  agentes, regra de ouro do paralelismo, o `flock`, o achado da sonda do hook, e como
  consumir cada retorno.
- [references/ledger-format.md](references/ledger-format.md) — o formato do ledger, os
  estados "de quem é a bola", rastreabilidade dupla ledger↔backlog, retomada a frio e
  fechamento.
- [references/interview-flow.md](references/interview-flow.md) — as quatro paradas
  legítimas, com payloads verbatim.
- [../README.md](../README.md) — a tabela estável da frota (slug, `rootProject.name`, nav,
  backlog, ferramental).
- [`.claude/agents/`](../../agents/) — [android-scout](../../agents/android-scout.md) e
  [android-implementer](../../agents/android-implementer.md).

---

## Silent quality checklist (rodar antes de mostrar o plano do Step 4)

1. Raiz resolvida; os três marcadores existem.
2. Nenhum item ficou com `app: ?`.
3. Todo item que não é `app-novo` tem relatório de scout.
4. Nenhum item entra na fila com dúvida de produto aberta.
5. Itens que atravessam apps foram divididos; itens com executores diferentes também.
6. A ordem dentro de cada app respeita dependência real (`feature-nova` antes do `edicao`
   que a usa).
7. Nenhum par de itens do mesmo app está marcado para rodar em paralelo.
8. `app-novo`, se existe, tem o **esqueleto** por último — e **todas as funcionalidades
   descritas no pedido viraram itens próprios depois dele** (`/android-new-app` entrega só
   o esqueleto; o que não virou item some). Nenhum comportamento pedido pelo usuário ficou
   sem item.
9. Números de backlog previstos são max+1 do app, sem preencher buraco.
10. `utilities` não aparece com plano de criar `specs/`.
11. Toda invocação de build no plano passa por `flock`.
12. O plano não contém `make clean`, `rm`, `git clean`, `git stash`, nem apagar artefato.
13. A árvore suja do Step 1 está no resumo.
14. O que ficou de fora está listado com motivo.

Falhou algum item, corrija o plano antes de mostrar.

---

## Anti-patterns

- Não escreva nada antes do "Seguir" — nem ledger, nem backlog, nem código.
- Não despache implementador para item sem scout, ou com dúvida aberta.
- Não deixe subagente invocar skill: todas as 6 entrevistam e nenhuma sobrevive lá.
- Não deixe subagente commitar, taguear, bumpar versão, tocar `dist/`/`.dist/` ou rodar
  `make dist*`.
- Não rode build fora do `flock`, e não deixe um item gastar mais de 3 tentativas.
- Não paralelize dois itens do mesmo app.
- Não chute o app de um item — pergunte.
- Não invente item que o usuário não pediu, e não descarte item em silêncio.
- **Não trate `app-novo` como item único quando o pedido descreve funcionalidades.** O
  esqueleto não é o app. Avisar no plano que "isso é só o esqueleto" **não** substitui
  criar os itens: sinalizar não é entregar, e o usuário aprovou o pedido dele, não a sua
  redução dele.
- Não renumere backlog para casar com a numeração do lote: são eixos diferentes.
- Não crie `specs/` no `utilities`.
- Não pule o gate de aparelho, e não trate release reprovado como erro a limpar.
- Não redespache item `bloqueado` sem tratar o motivo.
- Não pergunte a cada item — agrupe as dúvidas de um app numa chamada.

---

## Limits

- Um lote por vez. Lote aberto tem que ser fechado ou retomado antes de abrir outro.
- Não decide produto. Ambiguidade que muda o resultado sempre volta para você.
- Não valida se o app funciona — só que compila. Quem valida é o gate de aparelho.
- Não faz refactor amplo, migração de dependência (AGP/Kotlin/Compose BOM), nem mudança de
  arquitetura. Esses não são itens de lote.
- Não trabalha fora deste monorepo, e não toca `apps-hub` para registrar app novo sem
  perguntar (é outro submódulo e gera release próprio).
- Paralelismo tem teto de 4 agentes; lote grande roda em ondas.
- Se o Docker estiver fora, escopa e fila normalmente, mas não implementa nem entrega —
  e diz isso em vez de fingir progresso.
