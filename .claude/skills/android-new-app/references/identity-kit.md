# Kit de identidade

O que o `.sample/` deixa **de propósito** como placeholder a ser substituído por
julgamento — não é substituição de token, é decisão de design. Os quatro itens:
paleta, ícone, permissões, README.

---

## 1. Paleta — `core/ui/src/main/kotlin/br/com/{{leaf}}/core/ui/theme/Theme.kt`

O `.sample` traz índigo/violeta com o comentário "Ajuste as cores ao criar um app
real". São **6 slots por esquema**, dois esquemas. Substituir os 12 valores e manter a
estrutura (`private val LightColors = lightColorScheme(...)`, idem `DarkColors`, e o
`{{Pascal}}Theme` que escolhe entre os dois por `isSystemInDarkTheme()`).

Regras que os 6 slots seguem no `.sample` e que as paletas abaixo respeitam:

- `primary` claro é **escuro e saturado**; `primary` escuro é **claro e dessaturado**
  (invertem, não repetem).
- `onPrimary` contrasta com `primary` do mesmo esquema.
- `primaryContainer`/`onPrimaryContainer` são o par invertido do anterior.
- `secondary` é neutro puxando a matiz do primary.
- `tertiary` é a cor de **destaque**, numa matiz deliberadamente distante (no `.sample`,
  âmbar contra índigo).
- Não mexer em `background`/`surface` — o Material 3 deriva defaults bons.
- Sem dynamic color em nenhum app deste repo. Não introduzir.

**O `primary` do tema claro precisa casar com o `fillColor` do fundo do ícone** (§2) —
no `.sample` os dois são `#5B4BE8`.

### Paletas prontas

**Índigo/violeta** (o padrão do `.sample` — usar só se o app não pedir identidade
própria)

```kotlin
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BE8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3DEFF), onPrimaryContainer = Color(0xFF17004A),
    secondary = Color(0xFF625B71), tertiary = Color(0xFFE8A13B),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF), onPrimary = Color(0xFF2A1A6B),
    primaryContainer = Color(0xFF43349E), onPrimaryContainer = Color(0xFFE3DEFF),
    secondary = Color(0xFFCBC2DB), tertiary = Color(0xFFF3C27A),
)
```

**Verde/teal** — dinheiro, saúde, hábitos, produtividade

```kotlin
private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F3E4), onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635D), tertiary = Color(0xFFE07A5F),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDBC7), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045), onPrimaryContainer = Color(0xFFA7F3E4),
    secondary = Color(0xFFB1CCC5), tertiary = Color(0xFFFFB59D),
)
```

**Âmbar/terracota** — comida, viagem, leitura, mídia

```kotlin
private val LightColors = lightColorScheme(
    primary = Color(0xFFB4531A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8), onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFF76574A), tertiary = Color(0xFF4C6B9A),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68F), onPrimary = Color(0xFF5E2600),
    primaryContainer = Color(0xFF883C05), onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE6BEAD), tertiary = Color(0xFFAFC8FF),
)
```

**Azul/aço** — ferramentas, utilitários, rede, sistema

```kotlin
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E9B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF54606E), tertiary = Color(0xFF7A5296),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCAFF), onPrimary = Color(0xFF003259),
    primaryContainer = Color(0xFF00497E), onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7D6), tertiary = Color(0xFFDDB8F5),
)
```

**Vermelho/carmim** — bloqueio, alarme, foco, limites

```kotlin
private val LightColors = lightColorScheme(
    primary = Color(0xFFAE2C2C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5), onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF775654), tertiary = Color(0xFF5B6236),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AA), onPrimary = Color(0xFF690006),
    primaryContainer = Color(0xFF8C1A1C), onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB9), tertiary = Color(0xFFC2CB8E),
)
```

Se o usuário passou um hex semente, montar a paleta a partir dele seguindo as regras
acima: `primary` claro = a semente (escurecer se for clara demais), `primary` escuro =
a semente clareada e dessaturada, containers = versões bem clara/bem escura da matiz,
`tertiary` = matiz a ~150° de distância.

---

## 2. Ícone — dois vetores, sem PNG

O `.sample` usa adaptive icon puro em vetor: `drawable/ic_launcher_background.xml`
(quadrado 108dp chapado) + `drawable/ic_launcher_foreground.xml` (símbolo branco).
Os dois wrappers em `mipmap-anydpi-v26/` (`ic_launcher.xml`, `ic_launcher_round.xml`)
são idênticos e **não precisam de edição**. Não existem buckets de densidade PNG — o
minSdk é 26, então o adaptive icon basta.

**Fundo** — trocar só o `fillColor` para o `primary` do tema claro:

```xml
    <path
        android:fillColor="#{{PRIMARY_HEX}}"
        android:pathData="M0,0h108v108h-108z" />
```

**Frente** — o `.sample` traz um losango genérico marcado "Troque ao criar um app
real". A safe zone do ícone adaptativo é o círculo central de ~66dp num viewport de
108: **manter todo desenho entre x,y = 27 e 81**, com folga.

Modelos verificados nessa faixa:

```xml
<!-- Losango (o padrão do .sample) -->
android:pathData="M54,32 L64,50 L54,68 L44,50 Z"

<!-- Bloco de notas: página com canto dobrado + 3 linhas -->
android:pathData="M38,30 h24 l10,10 v38 h-34 z M62,30 v10 h10 M44,52 h20 M44,60 h20 M44,68 h12"

<!-- Círculo (usar como base de qualquer glifo centralizado) -->
android:pathData="M54,30 a24,24 0 1,0 0.1,0 Z"

<!-- Barras de gráfico -->
android:pathData="M38,68 h8 v-14 h-8 z M50,68 h8 v-26 h-8 z M62,68 h8 v-20 h-8 z"

<!-- Escudo (bloqueio/proteção) -->
android:pathData="M54,30 l18,7 v16 c0,12 -8,20 -18,25 c-10,-5 -18,-13 -18,-25 v-16 z"

<!-- Marca de check -->
android:pathData="M38,54 l10,10 l22,-24 l-6,-6 l-16,18 l-4,-4 z"
```

Para linhas (`M44,52 h20`) o path precisa de `android:strokeColor` +
`android:strokeWidth` em vez de `fillColor`, ou virar retângulo fino preenchido. O
modelo do bloco de notas mistura os dois — na prática é mais simples usar dois `<path>`
separados: um preenchido para a silhueta, um com stroke para as linhas.

**Quando o app merece um ícone de verdade, dizer isso.** Path data escrito à mão
produz ícone tosco; oferecer os modelos e deixar claro que trocar por um asset real
depois é o caminho normal.

---

## 3. Permissões — `app/src/main/AndroidManifest.xml`

O ponto de inserção é o comentário acima de `<application>`:

```xml
    <!-- Declare aqui as permissões que o app usar (ex.: INTERNET). -->
```

Substituir o comentário por um que explique **por que** o app precisa, seguido das
declarações — é o padrão do `watch-up`:

```xml
    <!-- Busca de mídias na API do TMDB. -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Sem permissão nenhuma: **manter o comentário original** intacto.

Permissões que exigem pedido em runtime (não só o manifest) — avisar o usuário de que
o código de request ainda não existe: `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`
(API 33+), `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` (API 33+), `CAMERA`,
`RECORD_AUDIO`.

---

## 4. README — reescrever, nunca substituir token

O README do `.sample` tem 13 ocorrências de token, mas é um documento **sobre o
template** ("esqueleto de app Android", "Como instanciar um app novo a partir daqui").
Substituir token nele produz texto sem sentido — o app novo não é um esqueleto e
ninguém instancia app a partir dele.

Escrever do zero, na estrutura que os 6 apps compartilham:

```markdown
# {{dir}} — <uma linha dizendo o que o app faz>

<2 a 4 linhas: o problema que resolve e o comportamento principal.>

- **Linguagem/UI:** Kotlin + Jetpack Compose (Material 3)
- **Build:** Docker (Makefile / docker compose) — não precisa de JDK/Gradle/SDK no host
- **Package:** `br.com.{{leaf}}` · minSdk 26 · targetSdk 35 · compileSdk 35 · JDK 17

## Módulos

```
:app             # host: MainActivity + {{Pascal}}App (NavHost), tema/ícone
:core:ui         # tema ({{Pascal}}Theme) + componentes compartilhados
:feature:home    # <o que a tela inicial faz>
```

Regra de dependências: **features dependem apenas de `:core:*`** — nunca do `:app` nem
umas das outras. A navegação entre features vive no `:app`.

## Build

```bash
make image     # constrói a imagem Docker de build (1ª vez; baixa SDK/Gradle)
make apk           # APK debug em dist/{{slug}}-<versão>-debug.apk
make dist-release  # APK release (assinado + R8) em dist/{{slug}}-<versão>-release.apk
make dist-all      # debug + release de uma vez
```

Instalar no aparelho físico (adb via Docker):

```bash
./adb.sh authorize        # 1ª vez
./adb.sh build-install    # gera o APK e instala
./adb.sh logcat           # segue os logs
```

## Conformidade

Não trata dados de clientes (CPF/e-mail/telefone) e usa apenas imagens Docker de
registries públicos.
```

A seção `## Conformidade` existe em 5 dos 6 apps e no `.sample` — **incluir sempre**.
O `gastos` é o único sem ela, e isso é um esquecimento, não um padrão.
