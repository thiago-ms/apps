# other-projects

Diretório de apps Android pessoais (`watch-up`, `utilities`, `site-blocker`, ...).

## Esqueleto para novos apps: `.sample/`

`.sample/` é o **template oficial** para criar um app Android novo nesta pasta.
Ao pedir "cria um app Android X":

1. Copiar `.sample/` para uma pasta nova.
2. Renomear os placeholders `br.com.sample` / `Sample` (package, `namespace`,
   `applicationId`, `rootProject.name`, `SampleTheme`, `SampleApp`, `Theme.Sample`,
   imagem Docker `sample-android-build`, container `sample-adb`, APK
   `sample-<versão>-debug.apk`). Guia completo em `.sample/README.md`.
3. Ajustar paleta (`core/ui/.../theme/Theme.kt`), ícone e telas.

Estrutura: multi-módulo Kotlin + Compose (Material 3), build 100% via Docker,
minSdk 26 / target+compile 35 / JDK 17 — `:app` + `:core:ui` + `:feature:home`.
Ganchos comentados para plugar `:core:data` (Room) ou `:core:prefs` e novas features.

O APK final sai em `dist/` (via `make dist` / `make apk` / `./adb.sh build-install`).

## Build debug e release (padrão de todos os apps)

Todo app aqui (`.sample` + `watch-up`, `utilities`, `site-blocker`) tem os dois
fluxos, sempre entregando em `dist/`:

| | debug | release |
|---|---|---|
| Make | `make apk` / `make dist` | `make apk-release` / `make dist-release` · `make dist-all` (os dois) |
| adb (quando há adb.sh) | `./adb.sh build-install` | `./adb.sh build-install-release` |
| Artefato | `dist/<app>-<versão>-debug.apk` | `dist/<app>-<versão>-release.apk` |

- **Release = assinado + R8/shrink** (APK ~1 MB vs ~17 MB do debug). Assina com
  `keystore/keystore.properties` se existir (`make keystore` gera uma dedicada,
  fora do git); senão cai na keystore de debug — então já sai instalável.
- **R8 pode quebrar código via reflexão só em runtime** → sempre fazer smoke-test
  do release no aparelho. Toggle rápido: `isMinifyEnabled = false` no
  `app/build.gradle.kts` do app.
- Trocar debug↔release no mesmo device com assinaturas diferentes dá
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; o `adb.sh` desinstala+reinstala sozinho.

## Regras de trabalho

- **Nunca apagar artefatos (`dist/`, `build/`, APKs) sem perguntar antes**, mesmo
  gitignored/regeneráveis. Limpar só o que foi criado no passo atual e, na dúvida,
  confirmar.
- **Versionamento é manual.** Antes de gerar APK em `dist/` como entregável, subir o
  `versionName`/`versionCode` em `app/build.gradle.kts` (não sobrescrever a mesma
  versão). Rebuilds de validação durante o dev podem reusar a versão.

## Memória local

Memória desta pasta em `.claude/memory/` (ver `.claude/memory/MEMORY.md`) — não fica
na memória global; carregada por este `CLAUDE.md`.
