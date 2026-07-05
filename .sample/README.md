# .sample — esqueleto de app Android (multi-módulo)

Template base para criar novos apps Android neste diretório. Reflete a estrutura
comum dos projetos existentes (`watch-up`, `utilities`): Kotlin + Jetpack Compose
(Material 3), navegação por bottom navigation, build 100% via Docker (não precisa
instalar JDK/Gradle/Android SDK no host).

- **Linguagem/UI:** Kotlin + Jetpack Compose (Material 3)
- **Navegação:** Navigation Compose (bottom navigation com abas)
- **Build:** Docker (Makefile / docker compose)
- **Package base:** `br.com.sample` · minSdk 26 · targetSdk 35 · compileSdk 35 · JDK 17

## Arquitetura de módulos

```
:app             # host: MainActivity, NavHost (SampleApp), bottom nav, tema/ícone
:core:ui         # tema (SampleTheme) + componentes compartilhados (PushScreenScaffold, SectionHeader, EmptyState)
:feature:home    # tela inicial de exemplo
```

Regra de dependências: **features dependem apenas de `:core:*`** — nunca do `:app`
nem umas das outras. A navegação entre features vive no `:app` (cada tela recebe
callbacks como `onOpenDetail`, `onBack`). As versões de dependências ficam
centralizadas no version catalog [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

Módulos comuns a acrescentar conforme a necessidade (já preparados nos comentários
dos `.gradle.kts`):
- `:core:data` — Room + repositório + domínio (ver `watch-up`), ou
- `:core:prefs` — armazenamento leve de preferências (ver `utilities`)
- `:feature:<tela>` — um módulo Android Library por tela

## Como instanciar um app novo a partir daqui

1. Copie `.sample/` para uma nova pasta com o nome do app.
2. Substitua os placeholders `sample` / `Sample` pelos nomes reais. Os tokens são:
   - **`br.com.sample`** — package base (renomeie os diretórios `.../br/com/sample/`
     e o `namespace`/`applicationId` nos `build.gradle.kts`).
   - **`Sample`** — nome de exibição (`strings.xml`), `rootProject.name`
     (`settings.gradle.kts`), `SampleTheme`, `SampleApp`, `Theme.Sample`.
   - **`sample-android-build`** / **`sample-adb`** / **`sample-<versão>-debug.apk`** —
     imagem Docker, container adb e nome do APK em `docker-compose.yml`, `adb.sh`,
     `app/build.gradle.kts` (`distApk`), `server.sh`.
3. Ajuste `versionCode`/`versionName`, a paleta em `core/ui/.../theme/Theme.kt` e o
   ícone em `app/src/main/res/`.
4. Adicione novas abas em `navigation/Routes.kt` (`TabDestination`) e registre os
   destinos no `NavHost` de `navigation/SampleApp.kt`.

## Build

### Com `make`

```bash
make image     # constrói a imagem Docker de build (1ª vez; baixa SDK/Gradle)
make wrapper   # gera o Gradle wrapper (1ª vez)
make test          # roda os testes unitários
make apk           # APK debug versionado em dist/ (alias de dist)
make dist          # APK debug em dist/sample-<versão>-debug.apk
make apk-release   # APK release versionado em dist/ (alias de dist-release)
make dist-release  # APK release (assinado + R8) em dist/sample-<versão>-release.apk
make dist-all      # debug + release de uma vez
```

> `assembleDebug`/`assembleRelease` (Gradle puro) sempre escrevem a cópia bruta em
> `app/build/outputs/apk/<tipo>/`; as tasks `:app:distApk` / `:app:distReleaseApk`
> copiam para `dist/` já com a versão no nome. Os alvos `apk*`/`dist*`, o `build.sh`
> e o `./adb.sh build-install[-release]` usam essas tasks — o artefato final sai em `dist/`.

## Debug vs. release

| | debug | release |
|---|---|---|
| Assinatura | keystore de debug (automática) | keystore de release, ou debug como fallback |
| R8 (minify) + shrink | não | sim (APK bem menor) |
| Debuggável | sim | não |
| Task | `distApk` | `distReleaseApk` |

**Baixar/instalar o release igual ao debug:** funciona, desde que o APK esteja
**assinado**. O `assembleRelease` puro gera um `app-release-unsigned.apk` que o
Android recusa instalar; por isso o template já assina o release (com a keystore de
debug por padrão), e o APK cai em `dist/` — então aparece no `server.sh` e instala
pelo `adb.sh` exatamente como o debug.

Para uma keystore de release dedicada (recomendado antes de publicar):

```bash
make keystore        # gera keystore/release.jks + keystore/keystore.properties (fora do git)
make dist-release    # passa a assinar com essa keystore automaticamente
```

> **Atenção à troca debug ↔ release no mesmo aparelho:** se as assinaturas
> divergirem (ex.: release com keystore própria vs. debug), o Android bloqueia o
> upgrade (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). O `adb.sh` detecta isso e
> desinstala/reinstala (apaga os dados do app) automaticamente.

### Sem `make` (docker compose direto)

```bash
docker compose build
RUN="docker compose run --rm --user $(id -u):$(id -g) android"

$RUN gradle wrapper --gradle-version 8.10.2   # 1ª vez
$RUN ./gradlew --no-daemon testDebugUnitTest  # testes
$RUN ./gradlew --no-daemon assembleDebug      # APK
```

APK gerado em: `app/build/outputs/apk/debug/app-debug.apk`

> **Nota (Fedora/RHEL + SELinux):** o volume é montado com `:Z` no
> `docker-compose.yml` para o relabel do SELinux.

## Instalar no aparelho físico

`adb.sh` roda o `adb` via Docker (sem instalar nada no host):

```bash
./adb.sh authorize        # autoriza o aparelho (1ª vez)
./adb.sh build-install    # gera o APK e instala
./adb.sh logcat           # segue os logs do app
```

## Distribuir por HTTP local

```bash
./build.sh     # gera o APK versionado em dist/
./server.sh    # serve dist/ em http://<ip>:8000 (abra no navegador do celular)
```

## Conformidade

O esqueleto não trata dados de clientes (CPF/e-mail/telefone) e usa apenas imagens
Docker de registries públicos. Mantenha essa premissa nos apps derivados.
