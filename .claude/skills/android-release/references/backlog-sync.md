# Sincronizar o backlog na hora do release

**Só a mutação de release.** A spec canônica do formato — numeração, legenda, seções
de item, semântica do histórico — está em
[../../android-backlog/references/backlog-format.md](../../android-backlog/references/backlog-format.md).
Se as duas divergirem, aquela vale.

Aplica-se apenas a apps com `specs/backlog-*.md`: hoje **`watch-up` e `notes`**.
`utilities` tem `docs/` (planos, não backlog numerado) e `gastos`, `site-blocker` e
`apps-hub` não têm nada. **Se não existe, não criar** — esta skill não autora arquivo.

---

## As três edições

### 1. Status dos itens entregues, no dialeto do próprio app

Na tabela de índice `| # | Tipo | Título | Status |`, trocar a célula de status dos
itens fechados. **Cada app tem seu dialeto — detectar lendo o arquivo, nunca assumir:**

| app | forma |
|---|---|
| `watch-up` | `feito (validar no device)` |
| `notes` | `feito (v<ver>, validar no device)` |

```
watch-up:  | 1 | 🔧 | Tirar mídias vistas da home | feito (validar no device) |
notes:     | 2 | ✨ | Botão na lista para mesclar blocos | feito (v1.6, validar no device) |
```

Detecção: `grep -m1 '| feito' <arquivo>` e copiar a forma encontrada.
**Não converter o dialeto de um arquivo existente** — o `watch-up` tem 20+ células
nessa forma e reescrever todas por consistência é risco puro.

### 2. Uma linha no `Histórico de entregas`

Tabela `| Versão | Itens | Resumo |`, uma linha por release, apendada no fim:

```
| <ver> | <itens separados por vírgula> | <resumo denso de uma frase> |
```

Exemplo real (`notes`):

```
| 1.6 | 2 | Mesclar blocos de mesmo nome na lista (ação por item só em nomes repetidos; novo bloco "(mesclado)", origem por nome+data de criação, ordem por criação, originais preservados) |
```

Regras do resumo, lidas dos arquivos existentes:

- uma frase, densa, em português, sem ponto final.
- sub-cláusulas separadas por `;` dentro de parênteses, ou por `·` entre temas
  distintos.
- descreve o **comportamento entregue**, não o commit ("Mesclar blocos de mesmo nome
  na lista", não "refatora BlocosViewModel").

**Um item pode reaparecer em várias versões** e isso é normal: no `watch-up`, o item 4
aparece nas 1.13, 1.15 e 1.16, e o item 3 nas 1.11 e 1.16. Não é duplicação a
consertar — é a mesma área mexida de novo. Nunca remover a linha antiga.

Se um release não fechou item de backlog nenhum (ajuste avulso), **não** inventar
uma linha. O histórico mapeia item → versão; release sem item não entra.

### 3. A linha "Versão atual" — só no `notes`

O `notes` mantém, logo depois da tabela de histórico:

```
Versão atual em `app/build.gradle.kts`: **1.6** (`versionCode` 7).
```

Atualizar **na mesma edição** que a linha do histórico. Ela duplica o
`app/build.gradle.kts` de propósito (serve de referência rápida ao ler o backlog), e a
única proteção contra ela ficar velha é este passo ser o único caminho que a escreve.

O `watch-up` não tem essa linha — em vez dela mantém uma nota de rodapé sobre
migrações de schema Room acumuladas. **Não adicionar a linha do `notes` no
`watch-up`**, e não mexer na nota de rodapé.

---

## Verificação depois de editar

1. O número de itens com status `feito` cresceu exatamente pelo tanto que foi fechado.
2. A tabela de histórico ganhou **uma** linha, no fim.
3. Todo número de item citado na linha nova existe na tabela de índice.
4. Nenhum número de item foi renumerado (comparar a coluna `#` antes/depois).
5. No `notes`, a linha "Versão atual" bate com o `app/build.gradle.kts`.

## Anti-padrões

- Não criar `specs/backlog-*.md` num app que não tem.
- Não criar `specs/` no `utilities` — ele usa `docs/` deliberadamente.
- Não renumerar, reordenar ou apagar seções de itens.
- Não marcar como feito um item que o usuário não escolheu no interview.
- Não converter dialeto.
- Não commitar aqui — o commit do backlog entra junto com o do release (passo 8).
