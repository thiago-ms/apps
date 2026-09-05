# Ledger de lote

`.claude/work/<YYYY-MM-DD>-<slug>.md` — o mapa **1 lote → N apps → N itens**.

Existe porque o backlog de cada app não tem como carregar isso: um lote atravessa apps, e
nenhum dos 6 backlogs é o lugar certo para a ordem e o estado de um trabalho que toca três
repositórios.

O `<slug>` é kebab-case, curto, tirado do assunto dominante do lote (`ajustes-watchup-notes`,
`app-leitura`, `bugs-de-data`). Data no início para ordenar por listagem.

---

## Rastreabilidade dupla

Dois registros paralelos, com papéis diferentes — não é duplicação:

| | ledger do lote | backlog do app |
|---|---|---|
| escopo | este lote, atravessando apps | um app, para sempre |
| vida | descartável — arquiva em `.claude/work/done/` ao fechar | permanente |
| numeração | `#1..N` **local ao lote** | `#N` **global e permanente** no app |
| responde | "onde está o lote agora?" | "o que já foi feito neste app, e em que versão?" |

Cruzam-se pela coluna `Backlog` da tabela de itens. Um item aparece nos dois: como linha do
lote e como item numerado do app.

**Nunca** renumere itens de backlog para casar com a numeração do lote. São eixos
diferentes: o item `#2` do lote pode ser o `#13` do backlog do `watch-up`.

---

## O arquivo

````markdown
# Lote <slug> — <YYYY-MM-DD>

**Estado:** em execução | concluído

**Origem:**

> <o pedido do usuário, verbatim, sem reescrever>

## Itens

| # | App | Tipo | Item | Natureza | Backlog | Estado |
|---|-----|------|------|----------|---------|--------|
| 1 | watch-up | 🐛 | Progresso ignora temporada | edicao | #12 | ✅ entregue v1.17 |
| 2 | notes | ✨ | Aba de busca | feature-nova | #5 | 🔄 implementando |
| 3 | notes | 🔧 | Busca filtra por data | edicao | #6 | ⏳ na fila (depende de #2) |
| 4 | gastos | 🔧 | Filtro de categoria | edicao | #1 | ⏳ na fila |
| 5 | — | — | App de leitura | app-novo | — | ⏳ na fila |
| 6 | utilities | 🔧 | Ordem das unidades | edicao | — (usa `docs/`) | ⏳ na fila |

## Ordem

<Por app, a sequência e o motivo de cada dependência. Entre apps não há ordem — correm em
paralelo. Exemplo:>

- **notes**: #2 antes de #3 — o filtro precisa do módulo de busca existir.
- **watch-up**, **gastos**: independentes, um item cada.
- **app novo** por último: não bloqueia ninguém e é o item mais longo.

## Releases

| App | Versão | Estado |
|-----|--------|--------|
| watch-up | 1.17 | ✅ publicado (tag + hubs + commit) |
| notes | 1.7 | ⏳ aguardando código |
| gastos | 1.2 | ⏳ aguardando código |

## Fora do lote

<O que foi pedido e não entrou, com o motivo. "_Nada._" se tudo entrou. Nunca omitir.>

## Notas

<Desvios que o implementador reportou, decisões tomadas no meio, o que descobriu que muda
os próximos itens. É o que faz a retomada a frio funcionar.>
````

---

## Estados — "de quem é a bola", não "que etapa é"

Escolha deliberada, herdada do `sdd-automation`: o estado diz **o que fazer em seguida**,
então dá para retomar o lote sem memória de sessão.

| estado | significa | próxima ação |
|---|---|---|
| `⏳ na fila` | escopado e filado, não começou | despachar quando o app estiver livre |
| `🔄 implementando` | implementador rodando ou item em skill | aguardar o retorno |
| `⚠️ bloqueado: <motivo>` | precisa de decisão sua | perguntar, resolver, voltar para a fila |
| `✅ verde` | código pronto, build passou, **não** entregue | entra na fila de release do app |
| `✅ entregue v<x>` | saiu no release | nada |
| `❌ fora: <motivo>` | não entrou ou saiu do lote | nada — o motivo fica registrado |

Estado de item **nunca** volta atrás sem motivo escrito nas Notas. `✅ verde` que virou
`🔄` de novo significa que algo foi desfeito, e isso precisa estar explicado.

Na tabela de releases: `⏳ aguardando código` → `🔄 em release` → `✅ publicado` ou
`⚠️ reprovado no aparelho`.

**`⚠️ reprovado no aparelho` é um estado terminal legítimo do release, não um erro a
limpar.** O bump fica (a versão está queimada porque já existe artefato com aquele nome em
`dist/`), nada foi publicado, e a correção vira a versão seguinte. Foi exatamente o que
aconteceu com o `watch-up` 1.15.

---

## Retomada a frio

`/android-batch` sem argumento varre `.claude/work/*.md` com `**Estado:** em execução`.
Achou um, a retomada é só leitura:

1. Ler o ledger inteiro — a seção **Notas** primeiro, é onde está o contexto que se perdeu.
2. Confirmar contra a realidade, porque o ledger pode estar velho:
   ```bash
   git -C <ROOT>/<app> status --short     # tem trabalho não commitado de um item 🔄?
   grep -E 'versionName' <ROOT>/<app>/app/build.gradle.kts
   git -C <ROOT>/<app> tag --list | tail -3
   ```
   Item `🔄 implementando` com árvore limpa = o implementador não terminou e não deixou
   nada; volta para `⏳ na fila`. Item `✅ verde` com a versão já tagueada = foi entregue;
   corrija para `✅ entregue`.
3. Seguir do primeiro `⏳ na fila`, respeitando a seção **Ordem**.

**Divergência entre ledger e realidade: a realidade ganha.** Corrija o ledger e registre a
correção nas Notas.

---

## Fechamento

Fecha quando todo item está em estado terminal (`✅ entregue`, `❌ fora`) e toda linha de
release está `✅ publicado` ou `⚠️ reprovado`.

1. `**Estado:** concluído`.
2. `mkdir -p .claude/work/done` e mover o arquivo para lá.
3. Não apagar. O ledger arquivado é o histórico do lote, e é barato.

Item que ficou `⚠️ bloqueado` sem resolução **impede o fechamento**. Ou resolve, ou vira
`❌ fora: <motivo>` — e nesse caso o item de backlog fica `pendente`, que é exatamente para
isso que o backlog serve. Lote não fecha com item em limbo.
