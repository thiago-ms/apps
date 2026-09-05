# Payloads de `AskUserQuestion` — `android-new-feature`

Copiar verbatim — **não parafrasear labels nem descriptions**. As descriptions citam
os arquivos e deps concretos que cada escolha produz, de propósito: é o que deixa a
escolha informada sem o usuário abrir o código.

Dois rounds. O Round 2 só existe quando o destino é uma aba (precisa de rótulo e
ícone); nos outros casos a pergunta de confirmação sobe para o fim do Round 1.

---

## Round 1

O `<feature>` derivado do `argument-hint` (`<app>/<feature-name>`) entra como label
da primeira opção da Q1. Se o argumento não foi passado, a Q1 vira só "Other".

```jsonc
{
  "questions": [
    {
      "question": "Nome do módulo de feature? (minúsculo, uma palavra)",
      "header": "Feature",
      "multiSelect": false,
      "options": [
        { "label": "<derivado-do-argumento>", "description": "Cria feature/<x>/ com namespace <pkg>.feature.<x>. Vira :feature:<x> no settings.gradle.kts." },
        { "label": "Outro nome (digite em 'Other')",             "description": "Minúsculo, sem hífen nem underscore — o nome entra num namespace Kotlin e num path de diretório." }
      ]
    },
    {
      "question": "Onde a tela vive na navegação?",
      "header": "Destino",
      "multiSelect": false,
      "options": [
        { "label": "Tela empilhada (Recommended)",  "description": "Usa PushScreenScaffold(title, onBack) do :core:ui — TopAppBar com seta de voltar de graça. Recebe onBack do :app; a barra inferior desaparece nela. 3 edições de wiring." },
        { "label": "Aba do bottom navigation",      "description": "Scaffold + TopAppBar próprios, com @OptIn(ExperimentalMaterial3Api::class). Entra no enum TabDestination e aparece na barra inferior. 5 edições de wiring." },
        { "label": "Aba + filho detail/{id}",       "description": "As duas telas: a aba mais uma tela de detalhe navegada por push, com navArgument(NavType.LongType). Gera 2 arquivos de tela." }
      ]
    },
    {
      "question": "Como a tela guarda estado?",
      "header": "Estado",
      "multiSelect": false,
      "options": [
        { "label": "Estado local no composable (Recommended)", "description": "remember / rememberSaveable dentro da própria tela. Nenhuma dependência extra — fica nas 3 deps padrão do módulo." },
        { "label": "ViewModel",                                "description": "Cria <Feature>ViewModel.kt com StateFlow + <Feature>UiState, e adiciona a 4ª dep implementation(libs.androidx.lifecycle.viewmodel.compose). Precedente: utilities/feature/altimeter." }
      ]
    },
    {
      "question": "Criar teste unitário?",
      "header": "Testes",
      "multiSelect": false,
      "options": [
        { "label": "Não (Recommended)",  "description": "Só a tela. Nenhum app deste repo testa Composable — os testes existentes cobrem lógica pura extraída em arquivo separado." },
        { "label": "Sim",                "description": "Adiciona sourceSets[\"test\"].kotlin.srcDir(\"src/test/kotlin\") + testImplementation(libs.junit) e um <Feature>Logic.kt com o <Feature>LogicTest.kt espelhado. Precedente: utilities/feature/converter." }
      ]
    }
  ]
}
```

---

## Round 2 — só quando destino inclui aba

Rótulo e ícone são propostos a partir do nome da feature; o usuário confirma ou troca.
A confirmação está fundida aqui como última pergunta.

```jsonc
{
  "questions": [
    {
      "question": "Rótulo da aba na barra inferior? (português, curto — cabe em ~10 caracteres)",
      "header": "Rótulo",
      "multiSelect": false,
      "options": [
        { "label": "<proposto-do-nome>",              "description": "Aparece embaixo do ícone na NavigationBar e como contentDescription do ícone." },
        { "label": "Outro rótulo (digite em 'Other')", "description": "Rótulo longo quebra a barra quando há 4+ abas." }
      ]
    },
    {
      "question": "Ícone da aba?",
      "header": "Ícone",
      "multiSelect": false,
      "options": [
        { "label": "Icons.Filled.<proposto>",  "description": "Escolhido pelo nome da feature. material-icons-extended já vem via api() do :core:ui — qualquer Icons.Filled.* funciona sem tocar em dependência." },
        { "label": "Outro ícone (digite em 'Other')", "description": "Nome exato de Icons.Filled.* — nome inexistente só quebra na compilação. Seguros: Home, Search, Settings, List, Add, Person, Star, Info, Menu, Favorite." }
      ]
    },
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Cria os arquivos do módulo, aplica as edições de wiring e roda make apk." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem tocar em nada." }
      ]
    }
  ]
}
```

---

## Round 1b — quando o destino NÃO é aba

Sem rótulo nem ícone a perguntar, então só a confirmação. Fazer numa chamada própria
depois do Round 1 (não dá pra fundir: o resumo do plano depende das respostas do
Round 1).

```jsonc
{
  "questions": [
    {
      "question": "Seguir com esse plano?",
      "header": "Confirmar",
      "multiSelect": false,
      "options": [
        { "label": "Seguir (Recommended)", "description": "Cria os arquivos do módulo, aplica as edições de wiring e roda make apk." },
        { "label": "Rever escolhas",       "description": "Volta ao Round 1." },
        { "label": "Cancelar",             "description": "Encerra sem tocar em nada." }
      ]
    }
  ]
}
```

---

## Como tratar respostas "Other"

`AskUserQuestion` sempre oferece "Other", então cada pergunta precisa de uma regra.

| Pergunta | Tratamento do "Other" |
|---|---|
| Feature | Aceitar. Sanitizar: minúsculas, remover acentos, trocar hífen/underscore/espaço por nada. Se o resultado não casar `^[a-z][a-z0-9]*$`, mostrar o valor sanitizado e perguntar de novo. Se `feature/<x>/` já existir, parar e dizer — não mesclar num módulo existente. |
| Destino | Recusar. É uma das três formas de navegação que o `.sample` suporta; qualquer outra exige mudar o `NavHost`, que está fora do escopo desta skill. |
| Estado | Recusar. "Estado local" e "ViewModel" cobrem o que existe no repo. Pedido de Hilt/Koin/etc: dizer que nenhum app aqui usa injeção de dependência e que isso é uma decisão de arquitetura, não uma feature nova. |
| Testes | Recusar. É booleano. |
| Rótulo | Aceitar qualquer string. Avisar se passar de 12 caracteres. |
| Ícone | Aceitar. Validar que casa `^[A-Z][A-Za-z0-9]*$`; **não** tentar validar que o ícone existe (a lista é enorme) — a compilação valida. Se `make apk` falhar com "unresolved reference" no ícone, voltar e perguntar. |
| Confirmar | Tratar como Cancelar. Não seguir. |
