# Payloads de `AskUserQuestion` — `android-backlog`

Copiar verbatim. O Round 1 roteia por modo; o Round 2 depende do modo escolhido.

---

## Round 1 — modo

Quando o `argument-hint` já trouxe o modo (`<app> add`, `<app> done`), **pular a
pergunta de modo** e ir direto ao round do modo.

```jsonc
{
  "questions": [
    {
      "question": "O que fazer no backlog?",
      "header": "Modo",
      "multiSelect": false,
      "options": [
        { "label": "Adicionar item (Recommended)", "description": "Cria a seção ## N. e a linha na tabela de índice, com N = maior número atual + 1. Numeração nunca é reusada." },
        { "label": "Marcar item como feito",       "description": "Troca o status na tabela de índice para o dialeto do próprio app. Fora de um release — em release isso é trabalho do /android-release." },
        { "label": "Criar o backlog deste app",    "description": "Só para app sem backlog (gastos, site-blocker, apps-hub). Cria specs/backlog-<assunto>.md com a estrutura da casa e opcionalmente semeia o histórico das tags existentes." },
        { "label": "Reordenar prioridade",         "description": "Move linhas na tabela de índice sem tocar nos números. As seções ## N. continuam em ordem numérica — a tabela é que expressa prioridade." }
      ]
    }
  ]
}
```

---

## Round 2a — modo "adicionar item"

```jsonc
{
  "questions": [
    {
      "question": "Tipo do item?",
      "header": "Tipo",
      "multiSelect": false,
      "options": [
        { "label": "✨ melhoria (Recommended)", "description": "Comportamento novo ou ampliado. É o tipo mais comum nos backlogs existentes." },
        { "label": "🐛 bug",                    "description": "Algo que funciona errado hoje. A seção descreve o comportamento observado antes do desejado." },
        { "label": "🔧 ajuste",                 "description": "Refinamento de algo que já funciona — formato, ícone, texto, ordenação." }
      ]
    },
    {
      "question": "Título do item? (português, curto — vai na tabela e no cabeçalho da seção)",
      "header": "Título",
      "multiSelect": false,
      "options": [
        { "label": "Digite o título em 'Other'", "description": "Imperativo ou substantivo, sem ponto final. Precisa ser idêntico na tabela de índice e na seção ## N." },
        { "label": "Vários itens de uma vez",    "description": "Liste os títulos em 'Other', um por linha. Cada um vira uma seção com número próprio, em sequência." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Adiciona a linha de índice e a seção ## N. no fim do arquivo." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem tocar no arquivo." }
      ]
    }
  ]
}
```

O corpo da seção (parágrafo do problema + bullets de arquivos e decisões) **não** é
perguntado: é escrito a partir do que o usuário já contou na conversa, e do que a
leitura do código mostra. Se não houver informação para bullets, escrever só o
parágrafo — seção magra é melhor que bullet inventado.

---

## Round 2b — modo "marcar feito"

As opções de item vêm da tabela de índice: uma por item com status **diferente** de
`feito`, no formato `#<n> <emoji> <título truncado>`. Com mais de 3 pendentes, usar
"Other" para o usuário listar números.

```jsonc
{
  "questions": [
    {
      "question": "Quais itens marcar como feito?",
      "header": "Itens",
      "multiSelect": true,
      "options": [
        { "label": "#<n> <emoji> <título>", "description": "Status vira o dialeto do app: 'feito (validar no device)' ou 'feito (v<ver>, validar no device)'." }
      ]
    },
    {
      "question": "Em que versão foi entregue?",
      "header": "Versão",
      "multiSelect": false,
      "options": [
        { "label": "<versionName atual> (Recommended)", "description": "Lida de app/build.gradle.kts. Entra no status quando o app usa o dialeto com versão." },
        { "label": "Ainda não entregue",                "description": "Marca como feito sem versão — o código está pronto mas o release não saiu. Nenhuma linha entra no Histórico de entregas." },
        { "label": "Outra versão (digite em 'Other')",  "description": "Para registrar retroativamente um item entregue numa versão anterior." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Troca as células de status. A linha do Histórico de entregas NÃO é adicionada aqui — isso é do /android-release." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem tocar no arquivo." }
      ]
    }
  ]
}
```

---

## Round 2c — modo "criar o backlog"

```jsonc
{
  "questions": [
    {
      "question": "Qual dialeto de status?",
      "header": "Dialeto",
      "multiSelect": false,
      "options": [
        { "label": "Com versão (Recommended)", "description": "'feito (v1.6, validar no device)' mais a linha 'Versão atual em app/build.gradle.kts'. Registra em que release cada item caiu sem cruzar com a tabela de histórico. É o do notes." },
        { "label": "Simples",                  "description": "'feito (validar no device)', sem linha de versão atual. É o do watch-up — a versão de cada item sai só da tabela de histórico." }
      ]
    },
    {
      "question": "Semear o Histórico de entregas com as tags que já existem?",
      "header": "Histórico",
      "multiSelect": false,
      "options": [
        { "label": "Sim (Recommended)",  "description": "Uma linha por tag, com a mensagem do commit de release no Resumo. A coluna Itens fica vazia — não há como inferir retroativamente qual item cada release fechou." },
        { "label": "Não, começar vazio", "description": "Tabela de histórico só com o cabeçalho. O histórico passa a valer a partir do próximo release." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Cria specs/backlog-ajustes.md com a estrutura da casa." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem criar nada." }
      ]
    }
  ]
}
```

---

## Como tratar respostas "Other"

| Pergunta | Tratamento do "Other" |
|---|---|
| Modo | Recusar. Os quatro modos cobrem o que se faz num backlog. Pedido de "apagar item": explicar que a numeração é estável e item não se apaga — se foi abandonado, o caminho é registrar isso no corpo da seção. |
| Tipo | Recusar. Só os três emoji da legenda existem. Inventar um quarto quebra o invariante 7. |
| Título | Aceitar. Normalizar: uma linha, sem ponto final, escapar `\|` (vai dentro de célula markdown). Múltiplos títulos: um por linha. |
| Itens | Aceitar lista de números. Validar que cada um existe e que nenhum já está `feito`. Número inexistente: parar e perguntar — nunca criar item novo aqui. |
| Versão | Aceitar. Validar `^\d+\.\d+$`. Se for maior que o `versionName` atual, avisar que o release ainda não saiu e sugerir "Ainda não entregue". |
| Dialeto | Recusar. São os dois que existem no repo. |
| Histórico | Recusar. É binário. |
| Confirmar | Tratar como Cancelar. |
