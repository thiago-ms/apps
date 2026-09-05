# Wiring de um `:core:*` — as regras e os erros que cada engano produz

## A lei de direção

```
:app  →  :feature:*  →  :core:*
```

`:core:*` está no fim da cadeia: **nunca** depende de `:feature:*`, **nunca** de
`:app`. Um `:core:` pode depender de outro `:core:` (o `gastos/core/backup` faz
`api(project(":core:data"))`).

## `api()` via `:core:ui` — o padrão da casa

Os três apps com `:core:data` expõem o módulo **através do `:core:ui`**:

```kotlin
// <app>/core/ui/build.gradle.kts
dependencies {
    api(project(":core:data"))     // watch-up:34, gastos:35, notes:35
    api(platform(libs.androidx.compose.bom))
    // ...
}
```

O `.sample` já traz a linha pronta, comentada, em `core/ui/build.gradle.kts:34`:

```kotlin
    // Se houver módulo de domínio/dados, exponha-o via `api` para as features:
    // api(project(":core:data"))
```

Descomentar essa linha é o wiring. Por que funciona: toda feature já faz
`implementation(project(":core:ui"))`, e `api` é transitivo — então cada feature ganha
os tipos do `:core:data` sem declarar nada. É o mesmo mecanismo que já distribui o BOM
do Compose e o Material 3 pela frota.

**A regra que sai disso:** uma feature **nunca** declara
`implementation(project(":core:data"))`. Se você se pegou querendo adicionar essa
linha num módulo de feature, o wiring do `:core:ui` está faltando.

### `api` vs `implementation`

| | vaza para quem depende? | usar quando |
|---|---|---|
| `api(project(":core:data"))` | sim | os **tipos** do módulo aparecem em assinaturas que a feature usa — o caso normal de `:core:data` (a feature recebe `List<Midia>`) |
| `implementation(project(":core:x"))` | não | o módulo é infraestrutura consumida só por dentro |

Default: `api` no `:core:ui` para `:core:data`; `implementation` para o resto.

### Exposição alternativa: só no `:app`

Para módulo que a UI não deve alcançar (um cliente de rede, um agendador), declarar em
`app/build.gradle.kts` e **não** no `:core:ui`. Aí quem orquestra é o `:app`.

## Declarar também no `:app`? Os apps discordam

`notes/app/build.gradle.kts` e `watch-up/app/build.gradle.kts` declaram
`implementation(project(":core:data"))` explicitamente. O `gastos` **não** — alcança
transitivamente pelo `api()` do `:core:ui`.

As duas formas funcionam. Declarar explícito é mais legível quando o `:app` usa os tipos
direto (é o caso: os dois apps instanciam o repositório na `MainActivity`). **Default:
declarar**, seguindo a maioria; não é erro se faltar.

---

## O alias do KSP na raiz

O `:core:data` aplica `alias(libs.plugins.ksp)`, e para isso o plugin precisa estar
declarado na raiz com `apply false`:

```kotlin
// <app>/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    // ...
    alias(libs.plugins.ksp) apply false
}
```

Estado verificado:

| app | alias ksp na raiz | coordenadas Room no catálogo |
|---|---|---|
| `.sample`, `notes`, `watch-up`, `gastos`, `apps-hub` | ✅ | ✅ |
| `utilities`, `site-blocker` | ❌ | ❌ |

`.sample` e `apps-hub` declaram o alias sem ter `:core:data` — é gancho de propósito.

**Adicionar a linha só se faltar.** Duplicar dá erro de configuração do Gradle.

---

## O catálogo — a exceção da suíte

As outras skills nunca editam `gradle/libs.versions.toml`. **Esta pode precisar**, e
só nos dois apps pré-template.

Nos 5 apps com o catálogo do `.sample`, Room e KSP já estão lá (declarados como gancho,
sem uso) — nada a fazer.

Em `utilities` e `site-blocker` **não estão**. Adicionar `:core:data` neles exige as 6
entradas abaixo, copiadas verbatim do `.sample/gradle/libs.versions.toml`. Isso é uma
edição de catálogo — **perguntar antes**, e dizer que é o único caminho.

```toml
[versions]
ksp = "2.0.21-1.0.28"
room = "2.6.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Inserir cada entrada na seção que ela pertence, mantendo a ordem alfabética já existente
— não recriar as seções. `coroutines` e `junit` já existem nos dois; conferir antes de
adicionar `version.ref`.

Alternativa a oferecer: `:core:prefs` em vez de `:core:data`, que não precisa de nada
disso. Para app que só guarda liga/desliga, é a escolha certa de qualquer forma.

---

## Os erros e o que cada um produz

| engano | sintoma |
|---|---|
| esqueceu `include(":core:data")` | `Project with path ':core:data' could not be found` na configuração |
| esqueceu o `api()` no `:core:ui` | a feature não compila: `Unresolved reference` no import do model |
| declarou `:core:data` na feature em vez do `:core:ui` | compila, mas cada feature nova repete a linha — o wiring "funciona" e a convenção quebra em silêncio |
| esqueceu `alias(libs.plugins.ksp)` no módulo | `@Dao`/`@Database` não geram nada: `Unresolved reference: {{Pascal}}Database_Impl` |
| esqueceu `apply false` na raiz | `Plugin [id: 'com.google.devtools.ksp'] was not found` |
| duplicou o alias na raiz | erro de configuração do Gradle apontando plugin declarado duas vezes |
| coordenada Room ausente no catálogo | `Unresolved reference: libs` no `build.gradle.kts` do módulo |
| adicionou `kotlin.compose` ao `:core:data` | compila, mas puxa o compilador Compose num módulo sem UI — build mais lento sem motivo |
| usou `fallbackToDestructiveMigration()` | funciona no dev e apaga os dados do usuário no primeiro bump de schema |

A falha de KSP só aparece no `make apk` — o `build.gradle.kts` fica verde. Por isso o
build é a verificação obrigatória do passo final.
