# Payloads de `AskUserQuestion` — `android-release`

Copiar verbatim. **Três rounds**, e o terceiro é obrigatório e não pode ser fundido
nos anteriores — explicado abaixo.

Valores entre `<>` são computados no Preflight a partir dos arquivos reais.

---

## Round 1 — a versão

```jsonc
{
  "questions": [
    {
      "question": "Qual o novo versionName? (atual: <atual>)",
      "header": "Versão",
      "multiSelect": false,
      "options": [
        { "label": "<próximo-minor> (Recommended)", "description": "Próximo minor no esquema de 2 casas — cobre ajuste, bug e melhoria, que é praticamente toda entrega deste repo." },
        { "label": "<próximo-major>",               "description": "Vira a major e zera o minor. Só para mudança grande de comportamento ou arquitetura." },
        { "label": "Outra versão (digite em 'Other')", "description": "Duas casas, estritamente maior que <atual>. Versão já usada é recusada — nunca se sobrescreve uma versão." }
      ]
    },
    {
      "question": "Qual o novo versionCode? (atual: <code-atual>)",
      "header": "Code",
      "multiSelect": false,
      "options": [
        { "label": "<minor+1> (Recommended)",     "description": "Segue a relação versionCode = minor do versionName + 1, que vale em 6 dos 7 apps." },
        { "label": "<code-atual+1>",              "description": "Só incrementa o code atual. É o caminho certo para apps fora do padrão minor+1, como site-blocker." },
        { "label": "Outro code (digite em 'Other')", "description": "Precisa ser estritamente maior que <code-atual> — o Android recusa instalar APK com code menor ou igual ao instalado." }
      ]
    },
    {
      "question": "De onde vem o resumo da entrega? (vai na mensagem de commit e na linha do histórico)",
      "header": "Resumo",
      "multiSelect": false,
      "options": [
        { "label": "Derivar do git log desde a última tag (Recommended)", "description": "Lê os commits do submódulo desde a tag anterior e monta uma frase densa em português, no estilo das linhas já existentes no Histórico de entregas." },
        { "label": "Eu escrevo (digite em 'Other')",                      "description": "Texto livre, uma frase, sem ponto final — vai literalmente para a coluna Resumo do histórico." }
      ]
    }
  ]
}
```

---

## Round 2 — escopo + confirmação

A pergunta de itens de backlog só entra se o app tiver `specs/backlog-*.md` (hoje
`watch-up` e `notes`) **e** houver item pendente. Sem backlog, o round tem 2 perguntas.

As opções de item são montadas no Preflight a partir da tabela de índice — uma opção
por item cujo status **não** é `feito`, no formato `#<n> <emoji> <título truncado>`.
Como `AskUserQuestion` aceita no máximo 4 opções, com mais de 4 itens pendentes usar
"Other" para o usuário listar os números.

```jsonc
{
  "questions": [
    {
      "question": "Quais itens do backlog esta versão fecha?",
      "header": "Backlog",
      "multiSelect": true,
      "options": [
        { "label": "Nenhum",              "description": "Release sem item de backlog associado. Nenhuma linha entra no Histórico de entregas — o histórico mapeia item → versão, e release sem item não entra." },
        { "label": "#<n> <emoji> <título>", "description": "Marca como feito no dialeto do próprio app e entra na linha nova do Histórico de entregas." }
      ]
    },
    {
      "question": "Até onde levar o git?",
      "header": "Git",
      "multiSelect": false,
      "options": [
        { "label": "Commit + tag + push nos dois repos (Recommended)", "description": "Submódulo: commit 'Release <ver>' + tag <ver> bare + push --follow-tags. Superprojeto: stage do ponteiro e do .dist/, commit 'App <Nome> <ver>', push. Nesta ordem — superprojeto antes do submódulo deixa ponteiro pendurado." },
        { "label": "Só commit + tag local, sem push",                  "description": "Faz tudo localmente e para antes dos push. Útil para revisar antes de publicar; a skill diz o que ficou pendente." },
        { "label": "Não tocar no git",                                 "description": "Só bumpa, builda e copia para os hubs. Commit e tag ficam por sua conta." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Aplica o bump e roda make dist-all. A publicação nos hubs ainda vai depender do gate de verificação no aparelho." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem tocar em nada — nem o bump é aplicado." }
      ]
    }
  ]
}
```

---

## Round 3 — o gate de smoke no aparelho

**Por que este round não pode ser fundido no Round 2:** ele pergunta sobre um APK que
só existe depois do build (passo 5), e o build só roda depois da confirmação do Round 2.
Perguntar antes seria pedir que o usuário confirmasse ter testado um arquivo que ainda
não foi gerado. Isto é ordem essencial, não round desperdiçado — **não "otimizar"**.

Fazer a pergunta **depois** de subir o `./server.sh` (em background) e dar ao usuário a
URL `http://<ip>:8000/` **mais o nome exato do arquivo** — `<slug>-<ver>-release.apk`.
Pedir que ele baixe, instale, abra o app e exercite as telas que a entrega mexeu.

Depois da resposta, **matar o server por porta**: `fuser -k 8000/tcp` (ou a porta usada).
Num lote com releases em sequência o próximo app colide se ficar de pé. **Nunca**
`pkill -f 'http.server'` — deixa o `python3` filho órfão e casa a própria linha de comando
de quem chama. Se a 8000 estiver ocupada por outra coisa, `./server.sh <porta>` sobrescreve.

```jsonc
{
  "questions": [
    {
      "question": "O APK release rodou no aparelho? (abrir o app e exercitar as telas que mudaram)",
      "header": "Device",
      "multiSelect": false,
      "options": [
        { "label": "Sim, publicar (Recommended)", "description": "Segue para os hubs .dist/, a tag e os commits. R8 (isMinifyEnabled + isShrinkResources) quebra reflexão só em runtime, então essa é a única checagem que pega esse tipo de erro." },
        { "label": "Não — parar aqui",            "description": "Nada é publicado, nada é commitado. O bump permanece de propósito: já existe artefato com esse nome em dist/, então reusar o número produziria dois APKs diferentes com a mesma versão. A correção vira o release seguinte." },
        { "label": "Pular o gate (não recomendado)", "description": "Publica sem verificação no aparelho. Só faça isso se a entrega não mexeu em código — por exemplo, mudança apenas de ícone ou de string." }
      ]
    }
  ]
}
```

---

## Como tratar respostas "Other"

| Pergunta | Tratamento do "Other" |
|---|---|
| Versão | Aceitar. Validar: casa `^\d+\.\d+$`; comparar como **par de inteiros** (`1.10 > 1.9` é verdade neste esquema, e falso em aritmética decimal); recusar se já existe tag ou artefato com esse número. |
| Code | Aceitar. Validar inteiro estritamente maior que o atual. |
| Resumo | Aceitar qualquer texto. Normalizar: uma linha, sem ponto final, sem quebra (vai dentro de célula de tabela markdown — escapar `\|` se houver). |
| Backlog | Aceitar lista de números. Validar que cada um existe na tabela de índice e que nenhum já está `feito`. Número inexistente: parar e perguntar. |
| Git | Recusar. As três opções cobrem o espectro; qualquer outra coisa (rebase, branch, PR) está fora do escopo — esses repos são trunk-based com push direto, e PR é `/create-pr`. |
| Confirmar | Tratar como Cancelar. |
| Device | Tratar como "Não — parar aqui". Na dúvida sobre verificação, **não publicar**. |
