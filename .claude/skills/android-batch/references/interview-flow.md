# Payloads de `AskUserQuestion` — `android-batch`

Copiar verbatim. Esta skill fala pouco de propósito: a decisão do usuário foi **aprovação
única do plano**, depois corre a fila. As paradas legítimas são quatro, e só quatro.

| parada | quando |
|---|---|
| resolver `app: ?` | a triagem não conseguiu inferir o app de um item |
| **a aprovação do plano** | uma vez, no passo 4 — a única obrigatória |
| dúvida de produto | scout ou implementador devolveu dúvida que muda o resultado |
| build quebrado por motivo não óbvio | falha que não é do item |

O gate de aparelho **não está nesta lista** — ele é do `/android-release`, que roda no loop
principal e tem o payload dele.

---

## Resolver o app de um item ambíguo — antes dos scouts

Item cujo app não foi inferido não vai para scout: mandar investigar o repo errado é o pior
desperdício da triagem. Uma pergunta por item ambíguo, até 4 por chamada.

```jsonc
{
  "questions": [
    {
      "question": "Em qual app é \"<enunciado do item>\"?",
      "header": "App",
      "multiSelect": false,
      "options": [
        { "label": "<palpite-mais-forte>",  "description": "<o sinal que sugere este app, ex.: 'o assunto é episódio/temporada, território do watch-up'>" },
        { "label": "<segundo-palpite>",     "description": "<o sinal alternativo>" },
        { "label": "App novo",              "description": "Nenhum app existente cobre isso — o item vira um /android-new-app no fim do lote." },
        { "label": "Tirar do lote",         "description": "Fica registrado em 'Fora do lote' no ledger, com o motivo. Não é descartado em silêncio." }
      ]
    }
  ]
}
```

Com mais de 4 itens ambíguos, o lote está mal enunciado — mostre a lista inteira e peça para
o usuário dizer os apps de uma vez em texto, em vez de encadear 4 chamadas.

---

## A aprovação do plano — a única parada obrigatória

Vem **depois** dos scouts, com o resumo ≤20 linhas já impresso. Uma pergunta só: o plano
inteiro já está na tela, e opção com descrição longa aqui é redundante.

```jsonc
{
  "questions": [
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)",       "description": "Fila os itens no backlog de cada app, escreve o ledger, e executa a fila inteira. A partir daqui só paro em dúvida de produto, build quebrado por motivo estranho, e no gate de aparelho de cada release." },
        { "label": "Seguir, mas sem release",    "description": "Faz tudo até o código verde e para. Bump, tag, hubs e commit ficam para você rodar /android-release <app> depois. Útil quando você não vai estar com o celular à mão." },
        { "label": "Ajustar o plano",            "description": "Diga o que muda — tirar item, trocar app, mudar ordem, dividir um item. Volto com o plano revisado." },
        { "label": "Cancelar",                   "description": "Encerra sem escrever nada: nenhum ledger, nenhum item de backlog, nenhuma edição." }
      ]
    }
  ]
}
```

**Nada é escrito antes do "Seguir".** Os scouts que rodaram são read-only, então cancelar
aqui deixa o repositório exatamente como estava.

A opção "sem release" existe porque o gate de aparelho depende de você ter o celular por
perto — não é uma variação de rigor, é uma restrição física.

---

## Dúvida de produto — durante a execução

Disparada quando um scout devolve `Dúvidas que impedem implementar`, ou um implementador
devolve `Estado: bloqueado` por decisão de produto. Agrupe as dúvidas de um mesmo app numa
chamada; não interrompa a cada item.

```jsonc
{
  "questions": [
    {
      "question": "<a dúvida, exatamente como o scout ou o implementador formulou>",
      "header": "<substantivo curto do assunto: Ordem, Formato, Escopo, Limite>",
      "multiSelect": false,
      "options": [
        { "label": "<opção A>",       "description": "<o que isso produz concretamente na tela ou no dado>" },
        { "label": "<opção B>",       "description": "<idem>" },
        { "label": "Pular este item", "description": "O item vira '❌ fora' no ledger com o motivo, e fica 'pendente' no backlog do app. Os outros itens do lote seguem." }
      ]
    }
  ]
}
```

Regras que fazem esta pergunta valer a pena:

- **as opções vêm do scout/implementador**, não de você. Se as duas leituras do item estão
  no relatório, elas são as opções.
- **"Pular este item" sempre presente.** Uma dúvida que trava um item não deve travar o lote.
- **descrição concreta**: "ordena pela data de atualização, então bloco editado sobe para o
  topo" e não "usa a data de atualização".

---

## Build quebrado por motivo não óbvio

Só quando o erro **não é do item**: Docker fora, build que já estava quebrado antes do lote,
falha de rede baixando dependência, ou 3 tentativas gastas sem convergir.

Erro de compilação do próprio item **não** vira pergunta — é trabalho do implementador, e um
redespacho com o erro no prompt é a resposta certa.

```jsonc
{
  "questions": [
    {
      "question": "O build do <app> falhou por motivo que não parece ser do item. Como seguir?",
      "header": "Build",
      "multiSelect": false,
      "options": [
        { "label": "Parar só este app",           "description": "Os itens deste app ficam '⏳ na fila' no ledger e os outros apps do lote seguem normalmente. É o default seguro." },
        { "label": "Tentar mais uma vez",         "description": "Redespacha o item com a saída do erro no prompt. Vale quando o erro parece transitório (rede, timeout do lock)." },
        { "label": "Parar o lote",                "description": "Interrompe tudo. O ledger fica com o estado de cada item para retomar depois com /android-batch." }
      ]
    }
  ]
}
```

**Nunca** ofereça `make clean` ou apagar `dist/` como saída — a lei do não-apagar-artefato
vale aqui, e um build sujo não é diagnóstico suficiente para justificar isso.

---

## Como tratar respostas "Other"

| Pergunta | Tratamento do "Other" |
|---|---|
| App | Aceitar nome de app existente (normalizar: minúsculas, hífen, aceitar nome de exibição — `WatchUp` → `watch-up`). Nome que não existe: mostrar os 6 e perguntar de novo. **Nunca** criar app aqui — isso é um item `app-novo` explícito. |
| Confirmar | Texto livre é tratado como "Ajustar o plano": leia o que o usuário pediu, revise e volte. Só "Cancelar" encerra. |
| Dúvida de produto | Aceitar. É a resposta mais informativa possível — o usuário conhece o produto. Repasse verbatim ao implementador no prompt de redespacho. |
| Build | Aceitar se for uma instrução concreta e verificável ("roda `make image` primeiro"). Recusar se for pedido de apagar artefato. |
