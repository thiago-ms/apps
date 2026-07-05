---
name: bump-versao-antes-do-dist
description: Bump manual do versionName antes de gerar dist como entregável
metadata:
  type: feedback
---

Antes de gerar APK em `dist/` como **entregável** (`make dist` / `make dist-release` /
`./adb.sh build-install-release`), fazer o **bump manual** do `versionName` (e
`versionCode`) em `app/build.gradle.kts`. O versionamento é manual em todos os projetos
aqui (watch-up, utilities, site-blocker, notes) — não há auto-incremento.

**Why:** Gerei o `notes` várias vezes na mesma versão 1.0, sobrescrevendo o mesmo
arquivo em `dist/`. O usuário quer um arquivo novo por versão, sem sobrescrever.
**How to apply:** Rebuilds de validação durante o desenvolvimento podem reusar a
versão; mas quando o build é para o usuário instalar/distribuir, subir a versão antes.
Confirmar a nova versão com o usuário se não estiver claro. Ver [[sample-android-skeleton]].
