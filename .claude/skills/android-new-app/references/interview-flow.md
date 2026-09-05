# Payloads de `AskUserQuestion` — `android-new-app`

Copiar verbatim. Dois rounds, mais um Round 1b condicional quando o usuário quer
customizar os tokens (o cap de 4 perguntas força o split).

`<x>` = versão sanitizada do nome dado, em minúsculas com hífen.

---

## Round 1

```jsonc
{
  "questions": [
    {
      "question": "Como o app se chama? (nome de exibição, PascalCase)",
      "header": "Nome",
      "multiSelect": false,
      "options": [
        { "label": "<do-argumento-se-houver>", "description": "Vira o app_name em strings.xml, o rootProject.name, o {{Pascal}}Theme, o {{Pascal}}App e o Theme.{{Pascal}}." },
        { "label": "Digite o nome em 'Other'",  "description": "PascalCase, sem espaço se possível — apps-hub usa 'Apps Hub' com espaço e isso funciona, mas complica a mensagem de commit do superprojeto." }
      ]
    },
    {
      "question": "Usar os nomes derivados?",
      "header": "Tokens",
      "multiSelect": false,
      "options": [
        { "label": "Usar derivados (Recommended)", "description": "diretório <x>, applicationId br.com.<x-sem-hifen>, slug de artefato <x-sem-hifen>. É o caso de watch-up, notes, gastos e site-blocker." },
        { "label": "Customizar",                   "description": "Abre um round extra para diretório, folha do package e slug separadamente. Necessário quando divergem — utilities é o dir 'utilities' com applicationId br.com.utils e slug 'utilitarios'." }
      ]
    },
    {
      "question": "Qual a forma da tela inicial?",
      "header": "Home",
      "multiSelect": false,
      "options": [
        { "label": "Uma aba Início (Recommended)", "description": "Exatamente o que o .sample entrega: bottom navigation com uma aba só, pronta para receber mais. Nada a mais é gerado." },
        { "label": "Sem bottom navigation",        "description": "Remove o NavigationBar do {{Pascal}}App e o enum TabDestination fica com uma entrada usada só como destino inicial. Para app de tela única, como o apps-hub." }
      ]
    },
    {
      "question": "Identidade visual?",
      "header": "Paleta",
      "multiSelect": false,
      "options": [
        { "label": "Escolher pelo propósito do app (Recommended)", "description": "Uma das 5 paletas prontas (índigo, verde/teal, âmbar/terracota, azul/aço, vermelho/carmim) conforme o que o app faz, com o fundo do ícone casando com o primary." },
        { "label": "Manter o índigo do .sample",                   "description": "primary 0xFF5B4BE8. Todo app novo nasce igual ao template — só faz sentido se a identidade vem depois." },
        { "label": "Eu dou o hex semente (digite em 'Other')",     "description": "A paleta de 12 valores é montada a partir dele: primary claro = a semente, escuro = clareada e dessaturada, tertiary a ~150° de distância." }
      ]
    }
  ]
}
```

---

## Round 1b — só quando "Customizar" foi escolhido

```jsonc
{
  "questions": [
    {
      "question": "Nome do diretório dentro do monorepo?",
      "header": "Diretório",
      "multiSelect": false,
      "options": [
        { "label": "<x>",                        "description": "Minúsculas; hífen é permitido e usado por watch-up, site-blocker e apps-hub." },
        { "label": "Outro (digite em 'Other')",  "description": "Sem espaço, sem maiúscula, sem ponto, sem underscore. Vira também o nome do repo em thiago-ms." }
      ]
    },
    {
      "question": "Folha do package? (o applicationId fica br.com.<folha>)",
      "header": "Package",
      "multiSelect": false,
      "options": [
        { "label": "<x-sem-hifen>",              "description": "Vira namespace e applicationId no :app, os namespaces do :core:ui e :feature:home, o PKG do adb.sh e os diretórios src/main/kotlin/br/com/<folha>/." },
        { "label": "Outra (digite em 'Other')",  "description": "Só letras minúsculas, sem hífen — é identificador Kotlin e caminho de diretório. utilities usa 'utils' aqui." }
      ]
    },
    {
      "question": "Slug de artefato? (nome do APK, da imagem Docker e do container adb)",
      "header": "Slug",
      "multiSelect": false,
      "options": [
        { "label": "<x-sem-hifen>",              "description": "Gera dist/<slug>-<versão>-debug.apk, a imagem <slug>-android-build:latest, o container <slug>-adb e a senha de keystore <slug>123." },
        { "label": "Outro (digite em 'Other')",  "description": "Minúsculas sem hífen. Precisa ser único na frota — slug repetido faz um app buildar com a imagem Docker do outro. utilities usa 'utilitarios'." }
      ]
    }
  ]
}
```

---

## Round 2

```jsonc
{
  "questions": [
    {
      "question": "Quais permissões o app vai precisar?",
      "header": "Permissões",
      "multiSelect": true,
      "options": [
        { "label": "Nenhuma (Recommended)",   "description": "Mantém o comentário do .sample intacto no AndroidManifest. A maioria dos apps daqui começa assim e adiciona depois." },
        { "label": "INTERNET",                "description": "Adiciona INTERNET + ACCESS_NETWORK_STATE, como o watch-up faz para consultar a API do TMDB. Não exige pedido em runtime." },
        { "label": "POST_NOTIFICATIONS",      "description": "Notificações. No API 33+ exige pedido em runtime, e o código desse pedido NÃO é gerado — fica como pendência." },
        { "label": "ACCESS_FINE_LOCATION",    "description": "Localização precisa. Exige pedido em runtime, não gerado aqui. Precedente: gastos tem um :core:location dedicado." }
      ]
    },
    {
      "question": "Registrar no git?",
      "header": "Git",
      "multiSelect": false,
      "options": [
        { "label": "Repo privado em thiago-ms + submodule add (Recommended)", "description": "git init + 'First commit' dentro do dir, gh repo create thiago-ms/<dir> --private --source=. --push, e git submodule add no superprojeto. É como os 6 apps existentes estão registrados." },
        { "label": "Só git local, registro depois",                          "description": "Faz git init + First commit dentro do dir e para. Nada é criado no GitHub e o superprojeto não ganha submódulo." },
        { "label": "Não tocar no git",                                        "description": "Só os arquivos. Atenção: sem git init dentro do dir, os ~35 arquivos do app aparecem soltos no status do superprojeto." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Copia o .sample, aplica os 3 passes de substituição, escreve identidade e README, e roda make apk para validar." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem criar nada." }
      ]
    }
  ]
}
```

O registro no `apps-hub` **não** é perguntado aqui — ele edita outro submódulo e gera
um release dele. Perguntar separadamente no fim (E12), depois do build verde.

---

## Como tratar respostas "Other"

| Pergunta | Tratamento do "Other" |
|---|---|
| Nome | Aceitar. Se vier com espaço, avisar que o `rootProject.name` fica com espaço (como `Apps Hub`) e seguir. Se vier minúsculo, capitalizar e confirmar. |
| Tokens | Recusar. É binário: derivados ou customizados. |
| Diretório | Aceitar. Sanitizar e validar `^[a-z][a-z0-9-]*$`. Se `<dir>` já existe → **E1**. |
| Package | Aceitar. Validar `^[a-z][a-z0-9]*$` (sem hífen — é identificador Kotlin). Colisão de `applicationId` → **E3**. |
| Slug | Aceitar. Validar `^[a-z][a-z0-9]*$`. Colisão → **E4**. |
| Home | Recusar. As duas formas cobrem o que o `.sample` suporta; qualquer outra é reescrever o `NavHost`. |
| Paleta | Aceitar hex (`#RRGGBB` ou `0xFFRRGGBB`). Inválido: mostrar o formato e perguntar de novo. |
| Permissões | Aceitar nomes de permissão Android. Para cada uma que exige runtime, avisar que o código do pedido não é gerado. Não validar contra a lista completa do Android — só avisar se não começar com `android.permission.` nem for um nome conhecido. |
| Git | Recusar. As três opções cobrem o espectro. |
| Confirmar | Tratar como Cancelar. |
