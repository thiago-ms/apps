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

## Ao finalizar alterações (rotina de entrega — sempre executar)

**Sempre que terminar as alterações pedidas em um app** (depois de compilar/testar
e o código estar verde), executar esta rotina no diretório do app alterado, sem
precisar o usuário pedir:

1. **Bump de versão** — subir `versionName` **e** `versionCode` em
   `app/build.gradle.kts` (nunca sobrescrever a mesma versão; ver "Versionamento é
   manual" abaixo). Este passo é o que torna o APK um entregável.
2. **Gerar debug + release** — `make dist-all` (ou `make dist` + `make dist-release`).
   Saem em `dist/<app>-<versão>-debug.apk` e `dist/<app>-<versão>-release.apk`.
   Fazer smoke-test do release quando possível (R8 pode quebrar só em runtime).
3. **Copiar o release para o hub raiz** — copiar `dist/<app>-<versão>-release.apk`
   para o `.dist/` da raiz de `other-projects` (`cp dist/<app>-<versão>-release.apk
   ../.dist/`), que é o hub agregador servido pelo `server.sh` da raiz (lista os
   releases de todos os apps). Não apagar os releases anteriores sem perguntar.
4. **Avisar o usuário** que as duas versões foram geradas em `dist/` e lembrar as
   **duas formas de instalar no aparelho**:
   - **Via server (baixar pelo celular):** rodar `./server.sh` no diretório do app
     (sobe um `http.server` na porta 8000 servindo `dist/`) e abrir no navegador do
     celular a URL que o script imprime (`http://<ip-do-pc>:8000/`) — PC e celular na
     mesma rede. Baixar o APK desejado e instalar.
   - **Via USB (aparelho conectado no PC):** `./adb.sh build-install` (debug) ou
     `./adb.sh build-install-release` (release) — gera e instala direto. Na 1ª vez,
     autorizar o device com `./adb.sh authorize` (aceitar o prompt no celular) e
     conferir com `./adb.sh devices`. O `adb.sh` roda o adb via Docker e
     desinstala+reinstala sozinho se a assinatura divergir.

Se o app não tiver `adb.sh`/`server.sh`, mencionar só os caminhos disponíveis.

Se o item entregue veio de um backlog/spec do app (ex.: `specs/backlog-*.md`),
**atualizar esse arquivo**: marcar o item como feito e registrar a versão na tabela
de "Histórico de entregas" (item → `versionName`).

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
