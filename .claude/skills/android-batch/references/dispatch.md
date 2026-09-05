# Despacho de subagentes e paralelismo

Os dois agentes moram em [`.claude/agents/`](../../../agents/):
[`android-scout.md`](../../../agents/android-scout.md) (read-only) e
[`android-implementer.md`](../../../agents/android-implementer.md) (escreve).
Eles já carregam as próprias instruções — **os prompts abaixo passam só o recorte**, não
repetem o método. Se um prompt estiver explicando ao agente como fazer o trabalho dele, a
instrução está no lugar errado.

---

## Regra de ouro do paralelismo

| O quê | Paralelo? | Por quê |
|---|---|---|
| scouts | **sim, todos de uma vez** | read-only; nada colide; é o ganho de graça |
| implementadores, apps **diferentes** | **sim** | dirs, repos git e imagens Docker separados |
| implementadores, **mesmo** app | **não, nunca** | mesmo `build/`, mesmo `.gradle/`, mesmo índice git |
| builds | **não** — serializados por `flock` | contenção de CPU/disco, não correção |
| releases | **não** — um por vez, no loop principal | o gate de aparelho é humano |

Concorrência de build entre apps seria **correta** (cada app tem imagem
`<slug>-android-build` própria, `.gradle/` próprio dentro do seu `/workspace`, e projeto
compose derivado do diretório). O lock existe por **recurso**, não por corretude — o que
significa que uma violação deixa lento, não corrompe. Daí a solução ser um `flock` inline
e não uma máquina de coordenação.

### A propriedade que isso compra

Com release por app conforme cada um termina, o **gate de aparelho do app A acontece
enquanto B e C ainda implementam**. O tempo humano sobrepõe o de máquina em vez de
bloquear a fila. Não desfaça isso "simplificando" para tudo sequencial.

### Teto de concorrência

Máximo **4 agentes simultâneos**. Acima disso a saída fica ilegível e a fila do `flock`
cresce mais que o ganho. Com mais apps que isso, despache em ondas.

---

## O hook `warn-before-changes` — resolvido, mas com uma ordem a respeitar

O hook `PreToolUse` em `Edit|Write|NotebookEdit`
([warn-before-changes.sh](/var/home/shopper/.claude/skills/.hooks/warn-before-changes.sh))
bloqueia (exit 2) quando a árvore está suja e ainda não houve reconhecimento na sessão.
Libera pelo marker `/tmp/.claude-wbc-${SESSION_ID}`, gravado quando a árvore está limpa
ou quando o usuário aceita "Proceed for session". Subagente não tem interlocutor, então um
bloqueio dentro dele viraria falha do item.

**Sondado em 2026-08-06, com a árvore suja de propósito:** um subagente com `Write`
escreveu **sem ser bloqueado** e **nenhum marker novo** apareceu. Isso descarta o cenário
ruim — `session_id` próprio + árvore suja teria bloqueado *sem* gravar marker. Sobram duas
explicações, ambas favoráveis: o subagente herda o `session_id` (e pegou o fast path pelo
marker existente), ou o hook não se aplica a tool call de subagente.

**A ordem que decorre disso, e que o passo 5 do workflow já garante:** o orquestrador
escreve o ledger e os itens de backlog **no loop principal, antes** de despachar qualquer
implementador. Se a sessão começou com árvore suja, é aí que o hook pergunta — uma vez, com
você presente. Depois disso os implementadores passam.

**Fallback**, se um implementador ainda reportar bloqueio de hook: não retente às cegas.
Aplique as edições daquele item no loop principal, a partir do relatório dele.

---

## Prompt-modelo do `android-scout`

Um por item, todos despachados na mesma mensagem. `subagent_type: "android-scout"`.

```
Raiz do superprojeto (CWD): <ROOT>
App-alvo: <app>            (diretório; o package base está no namespace do :app)

Item a escopar:
"<o enunciado do item, verbatim como o usuário escreveu>"

Contexto que já tenho:
- versionName atual: <ver>
- módulos do app: <lista de settings.gradle.kts>
- backlog: <caminho, ou "não tem">

Escope este item conforme suas instruções e devolva o relatório no formato definido.
Orçamento: 5–15 arquivos. Não builde.
```

Nada além disso. O agente já sabe as heurísticas de busca, o orçamento e o formato.

**Item de `app-novo` não recebe scout** — não há código para investigar. Vai direto para o
plano como `natureza: app-novo`.

### Como consumir o retorno

- `Natureza` decide o roteamento na execução (ver abaixo).
- `Complexidade` **alta** merece aparecer no resumo do plano — é o sinal de que o item pode
  virar release próprio ou pedir decisão sua.
- **`Dúvidas que impedem implementar` não é opcional.** Item que voltou com dúvida não
  entra na fila: ou você resolve com o usuário no passo 4, ou o item sai do lote. Despachar
  implementador com dúvida aberta produz código que precisa ser refeito.
- `Lacunas` não bloqueia, mas se o scout não achou o território, o item é candidato a sair
  do lote em vez de virar tentativa às cegas.

---

## Prompt-modelo do `android-implementer`

Um por item, **um item por app de cada vez**. `subagent_type: "android-implementer"`.

```
Raiz do superprojeto (CWD): <ROOT>
App: <app>

Item: <enunciado> (backlog #<n>)

Mapa do scout:
<cole o relatório do scout inteiro>

Implemente conforme suas instruções. Builde com:
  cd "<ROOT>/<app>" && flock -w 1800 /tmp/other-projects-build.lock make apk

Não bumpe versão, não commite, não toque em dist/ ou .dist/, não saia de <app>/.
Devolva o relatório no formato definido.
```

O mapa do scout vai **inteiro**. Resumir aqui desperdiça a investigação que já foi paga.

### Roteamento por natureza — quem executa o quê

| natureza | executor | por quê |
|---|---|---|
| `edicao` | **`android-implementer`** | é o caso comum e não precisa de você |
| `feature-nova` | **loop principal** via `/android-new-feature` | a skill entrevista (destino, estado, testes) |
| `core-novo` | **loop principal** via `/android-new-core` | a skill entrevista, e em `utilities`/`site-blocker` decide edição de catálogo |
| `app-novo` | **loop principal** via `/android-new-app` | a skill entrevista (tokens, paleta, permissões, git) |

A regra por trás: **nenhuma skill deste repo sobrevive dentro de subagente**, porque todas
usam `AskUserQuestion`. Item que precisa de skill volta para o loop principal, sempre.

Item misto — "cria a aba de busca e já corrige o filtro" — se resolve em dois itens: a
skill cria o módulo, e o implementador ajusta depois. Isso é decidido na triagem
(ver [triage-rules.md](triage-rules.md)), não aqui.

### Como consumir o retorno

- `Estado: verde` → marque o item no ledger e siga. Confira de graça:
  `git -C <ROOT>/<app> diff --stat` mostra só o que o item pedia?
- `Estado: bloqueado` → **não redespache**. Leia `Bloqueado por`: se é decisão de produto,
  pergunte ao usuário (é um dos casos legítimos de parada); se é pré-requisito (precisa de
  módulo novo), o item volta para a triagem com outra natureza.
- `Estado: falhou` → leia `Saída do build`. Erro do item → um redespacho com o erro no
  prompt é razoável, **uma vez**. Erro que não é do item (Docker fora, build já quebrado,
  rede) → pare o app e reporte; não gaste tentativas.
- `Desvios do mapa do scout` → sempre leia. É a correção do seu modelo do código, e afeta
  os itens seguintes do mesmo app.

---

## Ordem de despacho num lote multi-app

```
onda 1  │ scouts: todos os itens, em paralelo                    (read-only)
        │
gate    │ plano + aprovação única do usuário                     (loop principal)
        │
onda 2  │ ledger + backlogs                                      (loop principal — aquece o hook)
        │
onda 3  │ app A: item 1 ─→ item 2 ─→ …   ┐
        │ app B: item 1 ─→ item 2 ─→ …   ├ paralelo entre apps
        │ app C: item 1 ─→ …             ┘  builds serializados pelo flock
        │
        │ app que terminou → /android-release <app>              (loop principal, gate humano)
        │ (os outros seguem implementando durante o gate)
```

Dentro de um app a seta é **estrita**: o item 2 só sai depois do item 1 verde. Dois itens
do mesmo app em paralelo corrompem o build e o índice git.
