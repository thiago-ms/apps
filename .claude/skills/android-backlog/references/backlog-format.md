# Formato do backlog — spec canônica

**Esta é a fonte de verdade do formato.** O
[../../android-release/references/backlog-sync.md](../../android-release/references/backlog-sync.md)
cobre só a mutação de hora de release e aponta para cá.

Arquivo: `<app>/specs/backlog-<assunto>.md`. Hoje existe em 2 dos 6 apps:
`watch-up/specs/backlog-ajustes.md` e `notes/specs/backlog-ajustes.md`.

---

## A lei da numeração estável

> Escolha um número e a gente conversa e executa.

O número de um item é **permanente**. Isso é o que faz o backlog servir de vocabulário
entre pessoa e agente ("faz o 7") ao longo de meses.

Consequências, todas duras:

- **nunca** renumerar, nem para fechar buracos, nem para reordenar prioridade.
- **nunca** reusar um número, nem depois do item entregue.
- **nunca** apagar a seção de um item feito — ela é o registro do que foi decidido.
- item novo é sempre `max(#) + 1`, mesmo que existam números "livres" por conta de
  itens abandonados.
- reordenar prioridade se faz movendo a **linha na tabela de índice**, nunca trocando
  números.

---

## Estrutura do arquivo, na ordem

````markdown
# <Nome do app> — Backlog de ajustes, melhorias e bugs

Lista numerada de itens para resolver **um a um**. A numeração é estável: escolha
um número e a gente conversa e executa. `Status` começa tudo em `pendente`.

Legenda de tipo: 🐛 bug · ✨ melhoria · 🔧 ajuste

| # | Tipo | Título | Status |
|---|------|--------|--------|
| 1 | 🔧 | <título> | <status> |

## Histórico de entregas

Mapa item → versão em que foi entregue (APK debug+release em `dist/` e release no
hub `../.dist/`). **Manter atualizado a cada entrega:** ao concluir um item, subir a
linha correspondente com a versão (`versionName`) usada no build.

| Versão | Itens | Resumo |
|--------|-------|--------|
| 1.3 | 1 | <resumo denso> |

<linha "Versão atual" — só no dialeto com versão, ver abaixo>

---

## 1. 🔧 <título, igual ao da tabela de índice>
<parágrafo dizendo o problema>

- <bullet com arquivos afetados, caminhos em backtick>
- <bullet com a decisão de comportamento>
````

O `---` entre o histórico e a primeira seção de item é obrigatório; separa metadados de
conteúdo.

## A tabela de índice

`| # | Tipo | Título | Status |` com alinhamento `|---|------|--------|--------|`.

- **Tipo** é um dos três emoji da legenda: 🐛 bug, ✨ melhoria, 🔧 ajuste. Só esses.
- **Título** é curto, em português, e **idêntico** ao título da seção `## N.`
  correspondente. Se divergirem, a tabela é a que manda para busca — corrigir a seção.
- **Status** começa `pendente`. Ver dialetos abaixo. Não inventar vocabulário novo
  (`em andamento`, `wontfix`, `blocked` não existem aqui).

## A tabela de histórico

`| Versão | Itens | Resumo |`, uma linha por release, em ordem crescente de versão.

- **Versão** é o `versionName` (2 casas: `1.6`), sem prefixo.
- **Itens** são números separados por vírgula: `5, 6, 7`.
- **Resumo** é uma frase densa em português, sem ponto final, com sub-cláusulas
  separadas por `;` dentro de parênteses ou `·` entre temas distintos. Descreve o
  **comportamento entregue**, não o commit.

**Um item pode aparecer em várias versões.** No `watch-up` o item 4 está nas 1.13, 1.15
e 1.16, e o item 3 nas 1.11 e 1.16. Não é duplicação a consertar: é a mesma área mexida
de novo. Nunca remover a linha antiga.

Release que não fechou item nenhum **não entra** — o histórico mapeia item → versão.

## As seções de item

`## N. <emoji> <título>`, na ordem numérica, depois do `---`.

Corpo: um parágrafo dizendo o problema, depois bullets. Os bullets dos arquivos reais
carregam caminhos em backtick e, no `notes`, links relativos:

```markdown
- Arquivos: `app/src/main/res/drawable/ic_launcher_foreground.xml` (o símbolo, hoje
  um losango em `M54,32 L64,50 L54,68 L44,50 Z`)
```

Os dois apps divergem no vocabulário de anotação de decisão — **preservar o do app**:

| app | forma |
|---|---|
| `notes` | `- Decisões tomadas (entregue na v1.6):` |
| `watch-up` | `**Definido:**` / `**Refinamento v1.15 — <tema>:**` |

---

## Os dois dialetos de status

| dialeto | pendente | feito | app |
|---|---|---|---|
| **com versão** (default para arquivo novo) | `pendente` | `feito (v1.6, validar no device)` | `notes` |
| **simples** | `pendente` | `feito (validar no device)` | `watch-up` |

O dialeto com versão é estritamente mais informativo: dá para saber em que release cada
item caiu sem cruzar com a tabela de histórico. **É o default para arquivo novo.**

**Nunca converter o dialeto de um arquivo existente.** O `watch-up` tem mais de 20
células na forma simples; reescrever todas por consistência é risco de dessincronizar o
histórico por zero ganho visível.

Detecção, sempre por leitura:

```bash
grep -m1 '| feito' <arquivo>
```

### A linha "Versão atual" — acompanha o dialeto com versão

Logo depois da tabela de histórico:

```markdown
Versão atual em `app/build.gradle.kts`: **1.6** (`versionCode` 7).
```

Existe só no `notes`. Duplica o `app/build.gradle.kts` de propósito (referência rápida
ao ler o backlog); a proteção contra ficar velha é que só um caminho a escreve — o
passo 9 do `/android-release`.

O `watch-up` não tem essa linha; em vez dela mantém uma nota de rodapé sobre migrações
de schema Room acumuladas. **Não adicionar a linha do `notes` no `watch-up`** e não
mexer na nota de rodapé.

---

## Onde o arquivo mora — e onde não mora

- `watch-up`, `notes` → `specs/backlog-ajustes.md`. O `watch-up` tem outros `specs/*.md`
  (`busca.md`, `google-drive.md`, um PDF de spec) que **não** são backlog.
- `utilities` → usa `docs/` com planos (`docs/plano-mapa-simples.md`), não backlog
  numerado. **Organização deliberada — não criar `specs/` lá.**
- `gastos`, `site-blocker`, `apps-hub` → nada. Criar só se o usuário pedir.

## Invariantes verificáveis

Depois de qualquer edição, todas têm que valer:

1. nº de linhas na tabela de índice == nº de seções `## N.`
2. os números são contíguos e estritamente crescentes, começando em 1
3. nenhum número aparece duas vezes na coluna `#`
4. todo número citado em `Itens` do histórico existe na tabela de índice
5. as versões do histórico são crescentes e nenhuma repete
6. a linha de legenda existe e lista os três emoji
7. cada `Tipo` é um dos três emoji da legenda
8. cada `Status` casa o dialeto do arquivo
9. o título na tabela == o título da seção `## N.`
10. no dialeto com versão, a linha "Versão atual" bate com o `app/build.gradle.kts`
