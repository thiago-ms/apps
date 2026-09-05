# Wiring de navegação — as 4 edições

Um módulo de feature novo é **2 arquivos criados + 3 a 5 edições ancoradas**. As
edições são o modo de falha padrão: esquecer o `include` dá "project not found";
esquecer o `composable` compila verde e a tela simplesmente nunca abre.

Placeholders: `{{feature}}` (minúsculo), `{{Feature}}` (PascalCase), `{{Label}}`
(rótulo da aba, português, curto), `{{Icon}}` (nome em `Icons.Filled.*`),
`{{AppFile}}` (o arquivo de nav do app — `NotesApp.kt`, `WatchUpApp.kt`, … ver a
tabela no [README da suíte](../../README.md)).

**Toda edição é check-then-insert:** conferir se a linha já existe antes de inserir,
para que rodar a skill de novo seja no-op em vez de duplicar.

---

## A lei de direção de dependência

```
:app  →  :feature:*  →  :core:*
```

- uma feature depende **só** de `:core:*`. Nunca de `:app`, nunca de outra feature.
- a navegação vive no `:app`. A tela recebe callbacks (`onBack`, `onOpenDetail`,
  `onSelecionar`) como parâmetro e **nunca** recebe um `NavController`.
- se duas features precisam conversar, o `:app` faz a ponte: a feature A expõe
  `onAbrirX: (Long) -> Unit`, e o `:app` liga isso num `navController.navigate(...)`
  que leva à feature B.

Violação típica a recusar: "a feature de busca precisa abrir o detalhe, então importo
`DetailScreen` na busca". Não — a busca expõe `onOpenDetail: (Long) -> Unit`.

---

## Edição 1 — `<app>/settings.gradle.kts`

Âncora: a última linha `include(":feature:...")` existente. Inserir depois dela,
mantendo a ordem em que as features aparecem no app.

```kotlin
include(":feature:{{feature}}")
```

Checagem prévia: `grep -q 'include(":feature:{{feature}}")' <app>/settings.gradle.kts`

Cuidado: nos apps derivados do `.sample` existem linhas **comentadas** de exemplo
(`// include(":core:data")`). Não descomentar nada aqui — só adicionar a linha nova.

---

## Edição 2 — `<app>/app/build.gradle.kts`

Âncora: a última linha `implementation(project(":feature:...")))` no bloco
`dependencies`. Inserir depois dela.

```kotlin
    implementation(project(":feature:{{feature}}"))
```

Checagem prévia: `grep -q 'project(":feature:{{feature}}")' <app>/app/build.gradle.kts`

---

## Edição 3 — `<app>/app/src/main/kotlin/<pkg>/navigation/Routes.kt`

**3a. A constante de rota** — sempre. Dentro de `object Routes`, depois da última
`const val`, antes do bloco de comentário do exemplo `DETAIL`:

```kotlin
    const val {{FEATURE}} = "{{feature}}"
```

(`{{FEATURE}}` em SCREAMING_CASE, seguindo `HOME = "home"`.)

**3b. A entrada de aba** — só quando destino=aba. Dentro de
`enum class TabDestination`, depois da última entrada, **antes** do comentário
`// Adicione novas abas aqui`:

```kotlin
    {{FEATURE}}(Routes.{{FEATURE}}, "{{Label}}", Icons.Filled.{{Icon}}),
```

E o import do ícone, junto dos outros `androidx.compose.material.icons.filled.*`,
em ordem alfabética:

```kotlin
import androidx.compose.material.icons.filled.{{Icon}}
```

`material-icons-extended` já vem via `api()` do `:core:ui`, então qualquer ícone de
`Icons.Filled.*` está disponível sem tocar em dependência. Escolher um nome que
exista de fato — na dúvida, os seguros: `Home`, `Search`, `Settings`, `List`,
`Add`, `Person`, `Star`, `Info`, `Menu`, `Favorite`.

**3c. Rota com argumento** — só quando destino inclui `detail/{id}`. O `Routes.kt`
já traz o bloco pronto **comentado**; descomentar e renomear:

```kotlin
    const val {{FEATURE}}_DETAIL = "{{feature}}/detail/{id}"
    const val ARG_ID = "id"
    fun {{feature}}Detail(id: Long) = "{{feature}}/detail/$id"
```

`ARG_ID` é compartilhado — se já existir (outra feature já criou), **reusar** em vez
de declarar de novo.

`TAB_ROUTES` é derivado do enum (`TabDestination.entries.map { it.route }`) —
**não** precisa de edição. Destino empilhado não entra no enum, e é justamente isso
que faz a barra inferior desaparecer nele.

---

## Edição 4 — `<app>/app/src/main/kotlin/<pkg>/navigation/{{AppFile}}`

**4a. O import da tela**, junto dos outros `import <pkg>.feature.*`:

```kotlin
import {{pkg}}.feature.{{feature}}.{{Feature}}Screen
```

**4b. O destino no `NavHost`.** Âncora: dentro do bloco `NavHost { }`, depois do
último `composable(...)`, antes do comentário `// Registre aqui os destinos
empilhados`.

Aba (sem callback de voltar — a barra inferior é a navegação):

```kotlin
            composable(Routes.{{FEATURE}}) {
                {{Feature}}Screen()
            }
```

Tela empilhada (recebe `onBack`):

```kotlin
            composable(Routes.{{FEATURE}}) {
                {{Feature}}Screen(onBack = { navController.popBackStack() })
            }
```

Tela empilhada com argumento — o `{{AppFile}}` já traz esse bloco **comentado**
como modelo; descomentar e adaptar. Exige dois imports adicionais:

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
```

```kotlin
            composable(
                route = Routes.{{FEATURE}}_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_ID) ?: return@composable
                {{Feature}}DetailScreen(id = id, onBack = { navController.popBackStack() })
            }
```

**Não** mexer em `enterTransition`/`exitTransition`/`popEnterTransition`/
`popExitTransition` — as quatro são `None` por decisão de projeto (troca de tela
seca) e valem para todo o `NavHost`.

**Não** mexer no `modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())`
do `NavHost`. Ele reserva só a barra inferior de propósito: o topo é tratado pela
`TopAppBar` de cada tela, e usar `padding(innerPadding)` inteiro produz padding-top
duplicado.

---

## Verificação pós-wiring (antes de buildar)

```bash
cd <ROOT>/<app>
grep -q 'include(":feature:{{feature}}")'          settings.gradle.kts      && echo "ok include"
grep -q 'project(":feature:{{feature}}")'          app/build.gradle.kts     && echo "ok dep"
grep -q 'const val {{FEATURE}} '                   app/src/main/kotlin/*/*/*/navigation/Routes.kt && echo "ok rota"
grep -q 'composable(Routes.{{FEATURE}})'           app/src/main/kotlin/*/*/*/navigation/{{AppFile}} && echo "ok destino"
```

A asserção que mais pega erro real: **a string dentro de `composable(Routes.X)` tem
que ser a mesma constante declarada em `Routes.kt`**. Rota escrita à mão
(`composable("busca")`) compila e funciona, mas quebra no dia em que a constante
mudar — sempre referenciar a constante.

Só então buildar: `make apk`.
