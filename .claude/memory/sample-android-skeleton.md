---
name: sample-android-skeleton
description: .sample é o esqueleto oficial para criar novos apps Android nesta pasta
metadata:
  type: project
---

`/var/home/shopper/core/other-projects/.sample/` é o esqueleto (template) oficial
para novos apps Android neste diretório. Ao pedir "cria um app Android X", copiar
`.sample` para uma pasta nova e renomear os placeholders `br.com.sample` / `Sample`
(package, `namespace`/`applicationId`, `rootProject.name`, `SampleTheme`, `SampleApp`,
`Theme.Sample`, imagem Docker `sample-android-build`, container `sample-adb`, APK
`sample-<versão>-debug.apk`). O guia de instanciação está no `.sample/README.md`.

Estrutura: multi-módulo mínimo e compilável (`:app` + `:core:ui` + `:feature:home`),
Kotlin + Compose (Material 3), build 100% via Docker, minSdk 26 / target/compile 35 /
JDK 17. Extraído do denominador comum dos projetos existentes (`watch-up`, `utilities`
são multi-módulo; `site-blocker` é single-module mais antigo). Validado com
`assembleDebug` (BUILD SUCCESSFUL). Ganchos comentados para plugar `:core:data` (Room)
ou `:core:prefs` e novas features/abas.

**Why:** Padroniza o scaffold e evita recriar config de Gradle/Docker a cada app novo.
**How to apply:** Copiar `.sample`, renomear tokens conforme o README, ajustar paleta/ícone/telas.
