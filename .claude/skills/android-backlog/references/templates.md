# Templates de backlog

Placeholders: `{{Nome}}` (`rootProject.name` do app), `{{assunto}}` (sufixo do
arquivo — `ajustes` nos dois apps que têm), `{{ver}}`, `{{code}}`.

Formato completo em [backlog-format.md](backlog-format.md).

---

## Esqueleto vazio — dialeto com versão (default)

Arquivo: `<app>/specs/backlog-{{assunto}}.md`

````markdown
# {{Nome}} — Backlog de ajustes, melhorias e bugs

Lista numerada de itens para resolver **um a um**. A numeração é estável: escolha
um número e a gente conversa e executa. `Status` começa tudo em `pendente`.

Legenda de tipo: 🐛 bug · ✨ melhoria · 🔧 ajuste

| # | Tipo | Título | Status |
|---|------|--------|--------|

## Histórico de entregas

Mapa item → versão em que foi entregue (APK debug+release em `dist/` e release no
hub `../.dist/`). **Manter atualizado a cada entrega:** ao concluir um item, subir a
linha correspondente com a versão (`versionName`) usada no build.

| Versão | Itens | Resumo |
|--------|-------|--------|

Versão atual em `app/build.gradle.kts`: **{{ver}}** (`versionCode` {{code}}).

---
````

## Esqueleto vazio — dialeto simples

Idêntico, **sem** a linha "Versão atual em `app/build.gradle.kts`".

---

## Exemplo preenchido — dialeto com versão

````markdown
# Gastos — Backlog de ajustes, melhorias e bugs

Lista numerada de itens para resolver **um a um**. A numeração é estável: escolha
um número e a gente conversa e executa. `Status` começa tudo em `pendente`.

Legenda de tipo: 🐛 bug · ✨ melhoria · 🔧 ajuste

| # | Tipo | Título | Status |
|---|------|--------|--------|
| 1 | 🐛 | Valor negativo aceito no lançamento manual | feito (v1.2, validar no device) |
| 2 | ✨ | Filtro por categoria na tela de histórico | pendente |
| 3 | 🔧 | Formato de data curto na lista (dd/MM) | pendente |

## Histórico de entregas

Mapa item → versão em que foi entregue (APK debug+release em `dist/` e release no
hub `../.dist/`). **Manter atualizado a cada entrega:** ao concluir um item, subir a
linha correspondente com a versão (`versionName`) usada no build.

| Versão | Itens | Resumo |
|--------|-------|--------|
| 1.2 | 1 | Bloqueia valor negativo no lançamento manual (validação no campo; mensagem inline em vez de toast) |

Versão atual em `app/build.gradle.kts`: **1.2** (`versionCode` 3).

---

## 1. 🐛 Valor negativo aceito no lançamento manual
O campo de valor aceita número negativo, e o lançamento entra somando ao total em vez
de subtrair — o saldo fica errado sem aviso nenhum.

- Arquivos: `feature/capture/.../CaptureScreen.kt` (o campo) e
  `core/data/.../repo/GastoRepository.kt` (a gravação).
- Validar no campo, não no repositório: mensagem inline abaixo do input, botão de
  salvar desabilitado enquanto inválido.
- Decisões tomadas (entregue na v1.2): negativo é rejeitado em vez de virar despesa;
  zero continua permitido (serve para registrar visita sem gasto).

## 2. ✨ Filtro por categoria na tela de histórico
A lista de histórico cresce rápido e não tem como isolar uma categoria.

- Arquivos: `feature/history/.../HistoryScreen.kt`.
- Chips de filtro no topo, seleção única, com "Todas" como padrão.
- Preservar o filtro na rotação (`rememberSaveable`), mas não persistir entre sessões.

## 3. 🔧 Formato de data curto na lista (dd/MM)
A data completa ocupa espaço demais em cada linha da lista.

- Arquivos: `feature/history/.../HistoryScreen.kt`.
- `dd/MM` na lista; data completa só na tela de detalhe.
````

---

## Exemplo preenchido — dialeto simples

Mesma estrutura, com duas diferenças. Status sem versão:

```markdown
| 1 | 🐛 | Valor negativo aceito no lançamento manual | feito (validar no device) |
```

E sem a linha "Versão atual". Se o app acumula migrações de schema, o `watch-up` usa
uma nota de rodapé depois da tabela de histórico — copiar essa forma quando fizer
sentido:

```markdown
> Migrações de banco acumuladas até a v1.14: schema Room v4 → v8 (favorito,
> arquivada, cadência+data-base, intenção).
```

---

## Semear o histórico a partir das tags

Ao criar backlog num app que já tem releases, a tabela de histórico pode nascer
preenchida. As tags são bare (`1.6`) e o commit de release é `Release <ver>`:

```bash
cd <app>
git tag --list | sort -t. -k1,1n -k2,2n | while read -r t; do
  echo "| $t |  | $(git log -1 --format=%s "$t") |"
done
```

Isso dá uma linha por tag com a mensagem do commit no lugar do resumo. **A coluna
`Itens` fica vazia de propósito** — não existe como inferir retroativamente qual item
de backlog cada release fechou, e chutar isso corrompe justamente o mapeamento que a
tabela existe para guardar.

Apresentar assim e dizer ao usuário que os `Itens` das linhas semeadas ficaram em
branco por não serem recuperáveis. Buracos de tag (`watch-up` sem 1.15, `notes` sem 1.0
e 1.1) simplesmente não aparecem — mencionar.
