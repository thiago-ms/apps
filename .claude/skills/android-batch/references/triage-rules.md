# Triagem — do texto do lote para itens executáveis

O lote chega como você fala: um parágrafo, uma lista, um áudio transcrito. A triagem
transforma isso em itens com app, tipo e natureza — **sem inventar e sem perder nada**.

---

## 1. Quebrar em itens

Um item = **uma mudança de comportamento observável**. Os separadores que funcionam na
prática, em ordem de confiabilidade:

1. **marcador de lista** (`-`, `1.`, linha nova) — o usuário já separou; respeite.
2. **troca de app** — "no watch-up… no notes…" fecha o item anterior.
3. **`e também` / `outra coisa` / `ah, e`** — separador explícito de assunto.
4. **mudança de verbo sobre alvo diferente** — "arruma X e adiciona Y" são dois itens.

**Não** separe por:

- vírgula dentro da mesma frase sobre o mesmo alvo ("arruma o filtro de data, tá vindo
  errado" é **um** item — a segunda parte é sintoma, não item novo);
- detalhe de aceitação ("e que ordene por data") — isso é parte do item, não item.

### Quando dividir um item em dois

Divida quando as partes têm **executores diferentes** — é o único critério que importa,
porque decide o despacho:

> "cria uma aba de busca no notes e faz ela filtrar por data"

Vira `feature-nova` (a skill `/android-new-feature` cria o módulo, entrevistando você) +
`edicao` (o implementador escreve o filtro). Dois itens, dois números de backlog, o segundo
depende do primeiro.

Também divida quando um item toca **dois apps** — "o ícone do notes e do gastos estão
ruins" são dois itens, porque são dois repos, dois releases.

**Não** divida por conveniência de implementação. Um item que mexe em 4 arquivos do mesmo
módulo é um item.

### `app-novo` com funcionalidades descritas — divida SEMPRE

O caso que mais custa caro, e o único que já falhou de verdade
([2026-08-07](../../../work/done/2026-08-07-watchup-apps-novos.md)): o usuário pede
"cria um app que faz A, B e C" e recebe só o esqueleto, porque A, B e C nunca viraram
item.

A razão é que `/android-new-app` **entrega o esqueleto e para** — é o limite declarado
dela: a home é um placeholder com título e um parágrafo, não o app. Se o pedido descreve
comportamento e a triagem cria um item só, o comportamento descrito **não é adiado, ele
some**: não há item, não há backlog, não há nada que o traga de volta.

Então um `app-novo` que vem com funcionalidades vira **N + 1 itens**:

> "cria um app de listas de apps instalados, com carrossel de tags no rodapé, backup e
> reordenação"

| # | item | natureza | executor |
|---|---|---|---|
| 1 | esqueleto do app | `app-novo` | `/android-new-app` |
| 2 | módulo de dados (Room) | `core-novo` | `/android-new-core` |
| 3 | carrossel + listagem na home | `edicao` | implementador |
| 4 | seleção de apps instalados | `feature-nova` | `/android-new-feature` |
| 5 | reordenação | `feature-nova` | `/android-new-feature` |
| 6 | backup | `feature-nova` | `/android-new-feature` |

Os itens 2..N **dependem** do 1 (não há onde escrever antes do diretório existir), então
correm depois dele e em série dentro do app novo — é o mesmo app.

Quase todo app novo com funcionalidade real precisa de persistência, então **assuma um
item `core-novo` logo após o esqueleto** salvo evidência do contrário. Sem ele os itens de
tela não têm onde gravar nada.

Se o pedido do app novo for genuinamente só "quero o esqueleto para começar", aí é um
item só — mas isso tem que estar dito, não presumido.

---

## 2. Inferir o app

Sinais, em ordem de força:

1. **nome explícito** — "no watch-up", "no notes". Aceite hífen ou não, e aceite o nome de
   exibição (`WatchUp` → `watch-up`, `Utilitários` → `utilities`).
2. **domínio do assunto** — cada app tem território próprio e isso resolve a maioria dos
   casos sem nome:

   | assunto | app |
   |---|---|
   | filme, série, temporada, episódio, assistir | `watch-up` |
   | nota, bloco, texto, captura | `notes` |
   | gasto, despesa, categoria, valor, saldo | `gastos` |
   | bloqueio, site, DNS, foco, distração | `site-blocker` |
   | conversor, altímetro, bandeira, whatsapp, mapa, unidade | `utilities` |
   | hub, lista de apps, abrir outro app | `apps-hub` |

3. **item anterior** — numa lista sob "no watch-up:", os itens seguintes herdam o app até
   que outro seja nomeado.

**Nenhum sinal fecha? Marque `app: ?`** e leve para a pergunta do passo 4 do workflow.
Chutar o app é o pior erro possível da triagem: manda um scout investigar o repositório
errado e polui um backlog que não era o alvo.

Cuidado com o que **não** é sinal de app: o nome de um módulo genérico ("na home", "nas
configurações") existe em vários apps.

---

## 3. Classificar o tipo (o emoji do backlog)

A legenda é fixa — três tipos, e a escolha muda o texto da seção no backlog:

| tipo | quando | vocabulário do usuário |
|---|---|---|
| 🐛 bug | funciona **errado** hoje | "tá errado", "não funciona", "quebrou", "bugado", "devia mostrar X e mostra Y" |
| ✨ melhoria | comportamento **novo** ou ampliado | "adiciona", "quero que também", "seria bom se", "falta" |
| 🔧 ajuste | refinamento do que **já funciona** | "muda o formato", "deixa mais", "troca o ícone", "reordena", "tira daqui" |

Fronteira que mais confunde, e a regra que resolve: se o comportamento atual **nunca foi
pretendido**, é 🐛. Se era pretendido e você mudou de ideia, é 🔧.

---

## 4. Determinar a natureza (quem executa)

O scout devolve isso, mas a triagem já faz um palpite para montar a ordem antes dos scouts
retornarem:

| natureza | pista no texto |
|---|---|
| `app-novo` | "cria um app", "queria um app pra…", nenhum app existente serve — **e quase nunca é um item só: ver [Quando dividir](#app-novo-com-funcionalidades-descritas--divida-sempre)** |
| `feature-nova` | "nova aba", "nova tela", "uma seção pra…" — área que não existe hoje |
| `core-novo` | "salvar", "guardar", "persistir", "lembrar entre sessões" **em app sem `:core:data`/`:core:prefs`** |
| `edicao` | todo o resto — o caso comum |

O palpite **cede ao scout**. Se o scout diz que a "nova aba" já existe como tela empilhada,
a natureza vira `edicao`.

Sinal de `core-novo` que vale checar antes de assumir: `grep -c 'core:data\|core:prefs'
<app>/settings.gradle.kts`. `watch-up`, `notes` e `gastos` têm `:core:data`; `utilities` tem
`:core:prefs`; `site-blocker` é single-module e `apps-hub` só tem `:core:ui`.

---

## 5. O que vira item de backlog

**Praticamente tudo** — a decisão do usuário foi filar tudo, criando backlog nos apps que
não têm. O backlog é o registro permanente e a tabela de `Histórico de entregas` mapeia
item → versão, então itens entregues também moram lá.

| item | vai para o backlog? |
|---|---|
| bug/ajuste/melhoria em app existente | **sim** |
| `feature-nova` / `core-novo` em app existente | **sim** — a entrega é rastreável como qualquer outra |
| `app-novo` (o esqueleto em si) | **não** — não há backlog quando o item entra na fila |
| **funcionalidades do app novo** (os itens 2..N acima) | **sim** — ver abaixo |
| item que atravessa apps já dividido | um item de backlog **por app** |

**O backlog do app novo é criado no fim do item 1, não é uma oferta opcional.** Assim que
`/android-new-app` devolve o diretório, rode `/android-backlog <dir> create` e file ali os
itens 2..N. Sem isso as funcionalidades ficam só no ledger, que é descartável — e quando o
lote fecha, o registro do que falta construir se perde junto. Foi exatamente assim que o
`apps-hub-lists` e o `people` nasceram vazios em 2026-08-07.

Apps sem backlog hoje: `gastos` e `apps-hub` → `/android-backlog <app> create` antes de
filar o primeiro item. (`site-blocker`, `apps-hub-lists` e `people` ganharam os deles em
2026-08-07.) `utilities` usa `docs/` deliberadamente — **não crie `specs/` lá**; o
item existe no ledger do lote e não no backlog, e diga isso no plano.

Numeração vem do backlog e é **permanente**: N = max + 1, nunca preenchendo buraco. Quem
atribui é a skill `/android-backlog`, não a triagem — a triagem só **prevê** o número para
o resumo do plano.

---

## 6. Ordenar

Dentro de um app, a ordem sai de duas regras:

1. **Dependência real primeiro** — `feature-nova` antes do `edicao` que mexe nessa feature;
   `core-novo` antes da tela que consome os dados.
2. **Sem dependência, o mais simples primeiro** — complexidade `baixa` antes de `alta`. Um
   app entrega mais cedo e você vê progresso enquanto o item difícil ainda roda.

Entre apps não há ordem: eles correm em paralelo. O que existe é **ordem de release**, que é
a ordem em que cada app termina o código.

`app-novo` é caso especial: o **esqueleto** vai por último se o lote também tem itens em
apps existentes. Ele é o item mais longo (cópia + 3 passes + identidade + build de imagem
Docker que pode baixar 2 GB) e não bloqueia ninguém.

**Mas "por último" vale só para o esqueleto.** Os itens de funcionalidade do app novo
(2..N) vêm **depois dele**, em série, e são trabalho real — não são apêndice. Um lote com
app novo funcional é longo por natureza; se isso não couber na rodada, diga no plano e
proponha dividir em duas, **nunca** entregue o esqueleto sozinho fingindo que o item
acabou.

---

## 7. O que não entra no lote

Diga isso no plano em vez de silenciar:

- **Item que não é trabalho de código** — "por que o release ficou grande?" é pergunta,
  responda direto.
- **Item que o scout não conseguiu escopar** (lacuna, território não encontrado) — vira
  candidato a ficar de fora, com o que falta esclarecido.
- **Item com dúvida de produto aberta** que você não conseguiu resolver com o usuário.
- **Item que é claramente refactor amplo** ("reescreve a navegação do watch-up") — não é
  lote, é projeto. Diga e proponha tratar separado.

Nada é descartado em silêncio. Item que não entra é listado no relatório final com o motivo,
e pode virar item de backlog `pendente` sem execução — o que é exatamente para que serve um
backlog.
