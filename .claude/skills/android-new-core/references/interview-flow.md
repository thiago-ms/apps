# Payloads de `AskUserQuestion` — `android-new-core`

Copiar verbatim. Um round para `prefs`/`location`/`backup`; dois para `data` (a
entidade só faz sentido perguntar depois de saber que é Room).

---

## Round 1

Se o app-alvo é `utilities` ou `site-blocker`, a opção `:core:data` **muda de
description** para avisar da edição de catálogo — as duas variantes estão abaixo.

```jsonc
{
  "questions": [
    {
      "question": "Qual módulo :core:*?",
      "header": "Módulo",
      "multiSelect": false,
      "options": [
        { "label": ":core:data — Room (Recommended p/ persistência)", "description": "Espelha watch-up/core/data: alias(libs.plugins.ksp), SEM kotlin-compose, pacotes model/ db/ repo/ domain/, room-runtime + room-ktx + ksp(room.compiler) + junit + coroutines-test, e um test sourceSet." },
        { "label": ":core:prefs — SharedPreferences",                 "description": "Espelha utilities/core/prefs: 2 plugins, 1 dependência, 1 arquivo. Singleton por nome com StateFlow para o Compose reagir. Nada a acrescentar no catálogo nem na raiz." },
        { "label": ":core:<outro>",                                   "description": "Módulo de domínio próprio, como o :core:location e o :core:backup do gastos. Usa o build.gradle.kts do :core:prefs como base (o mais enxuto) e o conteúdo fica por escrever." }
      ]
    },
    {
      "question": "Como as features vão ver esse módulo?",
      "header": "Exposição",
      "multiSelect": false,
      "options": [
        { "label": "api() através do :core:ui (Recommended)", "description": "Uma linha em core/ui/build.gradle.kts (o .sample já traz comentada na linha 34). Toda feature ganha os tipos transitivamente, sem declarar nada. É o que watch-up, gastos e notes fazem." },
        { "label": "Só no :app",                              "description": "Declara em app/build.gradle.kts e não no :core:ui. Para infraestrutura que a UI não deve alcançar — quem orquestra é o :app." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Cria o módulo, aplica o wiring e roda make apk — é no build que falha de KSP aparece." },
        { "label": "Rever escolhas",       "description": "Volta ao início." },
        { "label": "Cancelar",             "description": "Encerra sem criar nada." }
      ]
    }
  ]
}
```

**Variante da opção `:core:data` em `utilities` e `site-blocker`** (os dois não têm
Room/KSP no catálogo nem o alias na raiz):

```jsonc
{ "label": ":core:data — Room (exige editar o catálogo)", "description": "Este app não tem Room/KSP em gradle/libs.versions.toml nem o alias do KSP na raiz. Seguir aqui adiciona 6 entradas ao catálogo e 1 linha na raiz, copiadas verbatim do .sample. É o único caminho — se der para resolver com liga/desliga, :core:prefs não exige nada disso." }
```

Quando essa variante for escolhida, **perguntar a edição do catálogo explicitamente**
antes de aplicar (não fundir na confirmação geral):

```jsonc
{
  "questions": [
    {
      "question": "Adicionar Room e KSP ao gradle/libs.versions.toml deste app?",
      "header": "Catálogo",
      "multiSelect": false,
      "options": [
        { "label": "Sim, adicionar as 6 entradas", "description": "versions ksp + room, libraries room-runtime/room-ktx/room-compiler/coroutines-test, e o plugin ksp — verbatim do .sample, inseridas nas seções existentes em ordem alfabética." },
        { "label": "Não — usar :core:prefs",       "description": "Troca para SharedPreferences, que não precisa de KSP, Room, nem edição de catálogo." },
        { "label": "Cancelar",                     "description": "Encerra sem tocar em nada." }
      ]
    }
  ]
}
```

---

## Round 2 — só para `:core:data`

```jsonc
{
  "questions": [
    {
      "question": "Nome da primeira entidade? (PascalCase, singular)",
      "header": "Entidade",
      "multiSelect": false,
      "options": [
        { "label": "Digite o nome em 'Other'", "description": "Vira a @Entity, o nome da tabela em minúsculo, e o tipo em todo o Dao e Repository. Uma entidade com id + nome + duas datas — o schema de verdade vem depois." },
        { "label": "Item",                     "description": "Nome genérico para começar. Renomear depois é refactor de IDE, mas mexe no schema — se você já sabe o nome real, use ele agora." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Cria os 6 arquivos (model, dao, database, repository, logic, teste), aplica o wiring e roda make apk + make test." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem criar nada." }
      ]
    }
  ]
}
```

O **schema não é perguntado além do nome**. Uma entidade com `id`, `nome`,
`dataCriacao`, `dataAtualizacao` e paramos: desenhar schema é trabalho de domínio, não
de scaffold, e campo inventado agora é migração de Room depois.

---

## Como tratar respostas "Other"

| Pergunta | Tratamento do "Other" |
|---|---|
| Módulo | Aceitar se casar `^:core:[a-z][a-z0-9]*$` ou for só o nome (`location` → `:core:location`). Se `core/<x>/` já existir, parar. Para nome que não seja `data` nem `prefs`, seguir pela receita genérica (base do `:core:prefs`) e dizer que o conteúdo fica por escrever. |
| Exposição | Recusar. As duas cobrem o que existe no repo. |
| Catálogo | Recusar terceira via. Não existe forma de usar Room sem as coordenadas. |
| Entidade | Aceitar. Validar `^[A-Z][A-Za-z0-9]*$`, singular. Se vier plural, sugerir o singular e confirmar — o nome da tabela vem daqui. |
| Confirmar | Tratar como Cancelar. |
