# Templates de módulo de feature

**Só o que o `.sample/` não carrega.** O `build.gradle.kts` e o skeleton de tela-aba
vêm do `.sample/` por cópia — não estão duplicados aqui. Ver
[nav-wiring.md](nav-wiring.md) para as edições de wiring.

Placeholders: `{{pkg}}` (package base do app, ex. `br.com.notes`), `{{feature}}`
(nome do módulo, minúsculo, ex. `busca`), `{{Feature}}` (PascalCase, ex. `Busca`),
`{{Titulo}}` (título exibido na tela, português).

---

## `build.gradle.kts` — vem do `.sample`, não daqui

```bash
cp <ROOT>/.sample/feature/home/build.gradle.kts <app>/feature/{{feature}}/build.gradle.kts
```

Depois **uma única** substituição:

```
namespace = "{{pkg}}.feature.home"   →   namespace = "{{pkg}}.feature.{{feature}}"
```

O arquivo tem 38 linhas e as dependências são exatamente estas três — não adicionar
nada:

```kotlin
dependencies {
    // :core:ui reexporta o BOM do Compose e o Material 3 (e o :core:data, quando houver).
    implementation(project(":core:ui"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
}
```

**Fallback:** se o app-alvo divergir do `.sample` (catálogo de versões diferente, ou
`compileSdk` diferente), copiar de um `feature/*/build.gradle.kts` irmão **do próprio
app** em vez do `.sample`. Divergência detectável comparando
`grep -E 'compileSdk|buildToolsVersion' ` nos dois. `site-blocker` é single-module e
não tem irmão nenhum — ali usar o `.sample` e avisar.

### Variante com testes (só quando tests=yes)

Duas adições ao arquivo copiado. Dentro do bloco `android { }`, logo depois da linha
`sourceSets["main"]`:

```kotlin
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
```

E no bloco `dependencies { }`:

```kotlin
    testImplementation(libs.junit)
```

`libs.junit` já existe no catálogo de todos os apps derivados do `.sample` — **não
editar `libs.versions.toml`**. Referência real: `utilities/feature/converter`
(`Measures.kt` + `src/test/kotlin/.../MeasuresTest.kt`).

---

## Tela-aba — vem do `.sample`, não daqui

```bash
cp <ROOT>/.sample/feature/home/src/main/kotlin/br/com/sample/feature/home/HomeScreen.kt \
   <app>/feature/{{feature}}/src/main/kotlin/<pkg-path>/feature/{{feature}}/{{Feature}}Screen.kt
```

Substituir: `package`, o import de `SectionHeader`, `fun HomeScreen()` →
`fun {{Feature}}Screen()`, o `Text("Sample")` do `TopAppBar` → `Text("{{Titulo}}")`, e
o KDoc. Estrutura preservada: `@OptIn(ExperimentalMaterial3Api::class)` +
`Scaffold(topBar = { TopAppBar(...) })` + `Column` com `padding(innerPadding)`.

---

## Tela empilhada (push) — template

Usa `PushScreenScaffold` do `:core:ui`, que já resolve `TopAppBar` + seta de voltar
com `contentDescription = "Voltar"`. **Não escrever `TopAppBar` à mão numa tela
empilhada** — é exatamente o que esse componente existe pra evitar.

```kotlin
package {{pkg}}.feature.{{feature}}

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import {{pkg}}.core.ui.component.PushScreenScaffold

/**
 * {{Titulo}}. Tela empilhada — recebe [onBack] do :app e nunca navega direto.
 */
@Composable
fun {{Feature}}Screen(onBack: () -> Unit) {
    PushScreenScaffold(title = "{{Titulo}}", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                "Conteúdo de {{Titulo}}.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
```

Note que `PushScreenScaffold` **não** precisa de `@OptIn(ExperimentalMaterial3Api::class)`
na tela — o opt-in já está dentro do componente no `:core:ui`.

---

## Tela de detalhe com argumento (`detail/{id}`) — template

Para o par aba + filho. O `id` chega resolvido pelo `:app`; a feature só o recebe.

```kotlin
package {{pkg}}.feature.{{feature}}

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import {{pkg}}.core.ui.component.PushScreenScaffold

/**
 * Detalhe de um item de {{Titulo}}. O [id] é resolvido no NavHost do :app a partir
 * do argumento da rota; esta tela não conhece navegação.
 */
@Composable
fun {{Feature}}DetailScreen(id: Long, onBack: () -> Unit) {
    PushScreenScaffold(title = "{{Titulo}}", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text("Item #$id", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

---

## ViewModel — template (só quando state=ViewModel)

Vai no mesmo módulo da feature, ao lado da tela. Sem Hilt/Dagger — **nenhum app
deste repo usa injeção de dependência**; o ViewModel é instanciado por
`viewModel()` e as dependências entram por construtor via factory quando
necessário.

```kotlin
package {{pkg}}.feature.{{feature}}

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado da tela de {{Titulo}}. */
data class {{Feature}}UiState(
    val carregando: Boolean = false,
    val erro: String? = null,
)

/**
 * Estado de {{Titulo}}. Sobrevive à recomposição e à rotação; a tela observa
 * [uiState] via collectAsStateWithLifecycle.
 */
class {{Feature}}ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow({{Feature}}UiState())
    val uiState: StateFlow<{{Feature}}UiState> = _uiState.asStateFlow()
}
```

Na tela, o consumo — `collectAsStateWithLifecycle` vem de
`libs.androidx.lifecycle.runtime.compose`, que já está nas 3 deps do módulo:

```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun {{Feature}}Screen(viewModel: {{Feature}}ViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

`viewModel()` exige `libs.androidx.lifecycle.viewmodel.compose`, que **existe no
catálogo dos 7 apps mas não está nas 3 deps do módulo de feature**. Quando
state=ViewModel, adicionar essa quarta linha ao `dependencies` — e só ela:

```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
```

Precedente exato: `utilities/feature/altimeter/build.gradle.kts:37`. Como a chave já
existe em todo catálogo, **nunca há motivo pra editar `libs.versions.toml` nesta
skill**.
