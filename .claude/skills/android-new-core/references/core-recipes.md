# Receitas de módulo `:core:*`

O `.sample/` **não tem** `:core:data` nem `:core:prefs` — só três ganchos comentados.
Então aqui os corpos são transcritos, **destilados** de implementações reais: entidade e
DAO genéricos, nunca o schema de produção de um app existente.

Placeholders: `{{pkg}}` (package base, ex. `br.com.gastos`), `{{Pascal}}`
(`rootProject.name`, ex. `Gastos`), `{{Entidade}}` / `{{entidade}}` (nome da primeira
entidade, PascalCase / minúsculo).

Referências reais: `watch-up/core/data`, `notes/core/data`, `utilities/core/prefs`.

---

## `:core:data` — Room + KSP

### `build.gradle.kts`

Transcrito de `watch-up/core/data/build.gradle.kts`. Diferenças em relação a um módulo
de feature, todas deliberadas: **sem** `kotlin.compose`, **sem** `buildFeatures
{ compose }`, **com** `alias(libs.plugins.ksp)`, **com** o `sourceSets["test"]`.

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "{{pkg}}.core.data"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

O `gastos` usa `api(...)` em vez de `implementation(...)` no room-runtime/ktx, porque
seu `:core:backup` precisa dos tipos do Room. Começar com `implementation` — trocar
depois é uma linha, e `api` desnecessário vaza a dependência para toda a árvore.

### Estrutura de pacotes — 4 diretórios

Layout de `watch-up/core/data` e `notes/core/data`:

```
src/main/kotlin/{{pkg-path}}/core/data/
├── model/       # @Entity + enums — os dados
├── db/          # @Dao, @Database, @TypeConverters — o Room
├── repo/        # a API que as features consomem
└── domain/      # lógica pura, testável sem Android
src/test/kotlin/{{pkg-path}}/core/data/domain/   # os testes de domain/
```

`domain/` existe separado justamente para ser testável sem instrumentação — é onde
mora a lógica que os testes de `MidiaLogicTest` (watch-up) cobrem. **Uma entidade com
dois campos placeholder e paramos** — não desenhar schema.

### `model/Model.kt`

```kotlin
package {{pkg}}.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * <O que esta entidade representa.> [dataCriacao] e [dataAtualizacao] são epoch
 * millis; a atualização é carimbada sempre que o conteúdo muda.
 */
@Entity(tableName = "{{entidade}}")
data class {{Entidade}}(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val dataCriacao: Long,
    val dataAtualizacao: Long,
)
```

`@PrimaryKey(autoGenerate = true) val id: Long = 0` com default 0 é o padrão dos dois
apps: insere com 0 e o Room atribui o id.

### `db/{{Pascal}}Dao.kt`

```kotlin
package {{pkg}}.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import {{pkg}}.core.data.model.{{Entidade}}
import kotlinx.coroutines.flow.Flow

@Dao
interface {{Pascal}}Dao {

    @Query("SELECT * FROM {{entidade}} ORDER BY dataAtualizacao DESC")
    fun observar(): Flow<List<{{Entidade}}>>

    @Query("SELECT * FROM {{entidade}} WHERE id = :id")
    fun observarUm(id: Long): Flow<{{Entidade}}?>

    @Query("SELECT * FROM {{entidade}} WHERE id = :id")
    suspend fun buscar(id: Long): {{Entidade}}?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: {{Entidade}}): Long

    @Update
    suspend fun atualizar(item: {{Entidade}})

    @Delete
    suspend fun apagar(item: {{Entidade}})
}
```

Convenção observada: leitura reativa devolve `Flow` e **não** é `suspend`; leitura
pontual e escrita são `suspend`. Nomes de método em português.

### `db/{{Pascal}}Database.kt`

```kotlin
package {{pkg}}.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import {{pkg}}.core.data.model.{{Entidade}}

@Database(
    entities = [{{Entidade}}::class],
    version = 1,
    exportSchema = false,
)
abstract class {{Pascal}}Database : RoomDatabase() {
    abstract fun {{pascal-lower}}Dao(): {{Pascal}}Dao
}
```

`exportSchema = false` nos dois apps — sem isso o build avisa a cada compilação.

### `repo/{{Pascal}}Repository.kt`

O ponto de entrada das features. Singleton por `applicationContext`, sem injeção de
dependência (nenhum app deste repo usa Hilt/Dagger/Koin — verificado).

```kotlin
package {{pkg}}.core.data.repo

import android.content.Context
import androidx.room.Room
import {{pkg}}.core.data.db.{{Pascal}}Database
import {{pkg}}.core.data.model.{{Entidade}}
import kotlinx.coroutines.flow.Flow

/**
 * Ponto de entrada dos dados. As features usam só esta classe — nunca o Dao nem o
 * Database direto. Singleton por processo via [get].
 */
class {{Pascal}}Repository private constructor(private val db: {{Pascal}}Database) {

    fun observar(): Flow<List<{{Entidade}}>> = db.{{pascal-lower}}Dao().observar()

    suspend fun buscar(id: Long): {{Entidade}}? = db.{{pascal-lower}}Dao().buscar(id)

    suspend fun salvar(item: {{Entidade}}): Long = db.{{pascal-lower}}Dao().inserir(item)

    suspend fun apagar(item: {{Entidade}}) = db.{{pascal-lower}}Dao().apagar(item)

    companion object {
        @Volatile
        private var instancia: {{Pascal}}Repository? = null

        fun get(context: Context): {{Pascal}}Repository =
            instancia ?: synchronized(this) {
                instancia ?: {{Pascal}}Repository(
                    Room.databaseBuilder(
                        context.applicationContext,
                        {{Pascal}}Database::class.java,
                        "{{pascal-lower}}.db",
                    ).build(),
                ).also { instancia = it }
            }
    }
}
```

**Sem `fallbackToDestructiveMigration()`** — de propósito. Os apps daqui acumulam
migração de verdade (o `watch-up` foi de schema v4 a v8), e o modo destrutivo apaga
dados do usuário no primeiro bump de versão. Quando a segunda versão do schema chegar,
escrever a `Migration`.

### `domain/{{Pascal}}Logic.kt` + teste

```kotlin
package {{pkg}}.core.data.domain

import {{pkg}}.core.data.model.{{Entidade}}

/**
 * Lógica pura sobre [{{Entidade}}] — sem Android, sem Room, sem coroutine. É o que os
 * testes unitários cobrem.
 */
object {{Pascal}}Logic {

    /** Ordena por atualização mais recente primeiro. */
    fun maisRecentesPrimeiro(itens: List<{{Entidade}}>): List<{{Entidade}}> =
        itens.sortedByDescending { it.dataAtualizacao }
}
```

```kotlin
package {{pkg}}.core.data.domain

import {{pkg}}.core.data.model.{{Entidade}}
import org.junit.Assert.assertEquals
import org.junit.Test

class {{Pascal}}LogicTest {

    private fun item(id: Long, atualizacao: Long) =
        {{Entidade}}(id = id, nome = "n$id", dataCriacao = 0, dataAtualizacao = atualizacao)

    @Test
    fun `ordena por atualizacao desc`() {
        val ordenado = {{Pascal}}Logic.maisRecentesPrimeiro(
            listOf(item(1, 100), item(2, 300), item(3, 200)),
        )
        assertEquals(listOf(2L, 3L, 1L), ordenado.map { it.id })
    }
}
```

---

## `:core:prefs` — SharedPreferences

O mais simples da frota: **2 plugins, 1 dependência, 1 arquivo**. Transcrito de
`utilities/core/prefs/build.gradle.kts`.

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "{{pkg}}.core.prefs"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
```

Sem KSP, sem Room, sem compose, sem test sourceSet. Nada a acrescentar no catálogo nem
na raiz.

O arquivo, destilado do `ToggleStore` do `utilities` — o padrão importante é
**singleton por nome** + `StateFlow`, para que duas telas compartilhem a mesma
instância e reajam na hora:

```kotlin
package {{pkg}}.core.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Nomes dos "arquivos" de preferências usados no app. */
object PrefStores {
    const val CONFIG = "config"
}

/**
 * Guarda um conjunto de liga/desliga (chave -> booleano) em [SharedPreferences] — sem
 * dependência externa — e expõe o estado como [StateFlow] para o Compose reagir na
 * hora. É um singleton **por nome** ([get]), então telas diferentes compartilham a
 * mesma instância e o mesmo fluxo.
 */
class ToggleStore private constructor(private val prefs: SharedPreferences) {

    private val _state = MutableStateFlow(readAll())
    val state: StateFlow<Map<String, Boolean>> = _state.asStateFlow()

    fun isEnabled(key: String, default: Boolean = true): Boolean =
        _state.value[key] ?: default

    fun setEnabled(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
        _state.update { it + (key to enabled) }
    }

    private fun readAll(): Map<String, Boolean> =
        prefs.all.entries
            .filter { it.value is Boolean }
            .associate { it.key to (it.value as Boolean) }

    companion object {
        private val instances = mutableMapOf<String, ToggleStore>()

        fun get(context: Context, name: String): ToggleStore =
            synchronized(instances) {
                instances.getOrPut(name) {
                    val prefs = context.applicationContext
                        .getSharedPreferences(name, Context.MODE_PRIVATE)
                    ToggleStore(prefs)
                }
            }
    }
}
```

---

## `:core:location` e `:core:backup`

Só o `gastos` tem esses dois, e são específicos do domínio dele. Não existe receita
genérica: usar o `build.gradle.kts` do `:core:prefs` como base (o mais enxuto),
ajustar o `namespace`, e escrever o conteúdo conforme o caso. Se o módulo precisar dos
tipos do `:core:data`, expor com `api(project(":core:data"))` dentro dele — é o que o
`gastos/core/backup` faz.
