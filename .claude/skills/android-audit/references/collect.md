# Coleta — os comandos read-only

Tudo aqui é leitura. **Nenhum comando desta skill escreve, apaga ou builda.**

Rodar em paralelo quando possível: são ~10 leituras por app e nenhuma depende da outra.

---

## Por app

```bash
cd "$ROOT/<app>"

# versão e toolchain
grep -nE 'versionName|versionCode|applicationId|minSdk|targetSdk|compileSdk|buildToolsVersion' app/build.gradle.kts
grep -nE 'isMinifyEnabled|isShrinkResources' app/build.gradle.kts

# identidade
grep -n 'rootProject.name' settings.gradle.kts
grep -o '"[a-z]*-\$version-debug\.apk"' app/build.gradle.kts     # o slug

# módulos
grep -n '^include' settings.gradle.kts

# git
git tag --list
git log -1 --format='%s'
git status --short

# artefatos
ls dist/ 2>/dev/null
ls .dist/ 2>/dev/null

# ferramental
ls adb.sh server.sh build.sh 2>/dev/null
grep -E '^\t.*gradlew.*distApk' Makefile | head -1
grep -c 'help image wrapper apk dist' Makefile                    # contrato de targets
md5sum docker/Dockerfile

# keystore
grep -n 'alias\|storePassword\|keyAlias' Makefile | head -4

# backlog
ls specs/backlog-*.md docs/*.md 2>/dev/null
grep -m1 '| feito' specs/backlog-*.md 2>/dev/null                 # o dialeto

# catálogo (para saber se :core:data é possível sem edição)
grep -cE 'room-runtime|room-ktx|room-compiler' gradle/libs.versions.toml
grep -c 'plugins.ksp' build.gradle.kts
```

## Na raiz

```bash
cd "$ROOT"
git submodule status
ls .dist/
cat .dist/index.html | grep -o 'href="[^"]*"'
md5sum .sample/docker/Dockerfile */docker/Dockerfile | awk '{print $1}' | sort -u
cat apps.code-workspace
git log --format='%s' -8
git status --short
```

## No `.sample/`

O check A5 (sincronia com o `token-map.md`) e o A6 (poluição) — comandos completos em
[checks.md](checks.md#a5--token-mapmd-dessincronizado-do-sample).

```bash
ls -d .sample/build .sample/.gradle .sample/dist .sample/keystore .sample/.adb 2>/dev/null
```

Saída vazia é o esperado.

## No `apps-hub`

As duas listas do check C6 têm que ser idênticas. **Ancorar o grep na sintaxe**, não só
no prefixo `br.com.` — o `HubApps.kt` começa com `package br.com.appshub.feature.home`,
e um grep solto casa isso e reporta um achado que não existe (o hub não se lista):

```bash
diff <(grep -o 'HubApp("br\.com\.[a-z]*"' \
         apps-hub/feature/home/src/main/kotlin/br/com/appshub/feature/home/HubApps.kt \
       | grep -o 'br\.com\.[a-z]*' | sort) \
     <(grep -o '<package android:name="br\.com\.[a-z]*"' \
         apps-hub/app/src/main/AndroidManifest.xml \
       | grep -o 'br\.com\.[a-z]*' | sort)
```

Saída vazia = as duas coincidem (hoje: 5 apps).

Para saber se algum app do monorepo ficou fora do hub, comparar pelo `applicationId`
de cada um — nunca pelo diretório:

```bash
for app in watch-up utilities site-blocker gastos notes; do
  id=$(grep -m1 'applicationId' "$app/app/build.gradle.kts" | sed 's/.*"\(.*\)".*/\1/')
  grep -q "HubApp(\"$id\"" apps-hub/feature/home/src/main/kotlin/br/com/appshub/feature/home/HubApps.kt \
    || echo "$app ($id) ausente do hub"
done
```

---

## Comparar versionName como par de inteiros

O esquema é de 2 casas e a segunda passa de 9 sem virar (`1.9` → `1.10` → `1.16`).
Comparar como número decimal dá errado: `1.10 < 1.9` em float, e aqui é o contrário.

```bash
# maior versão entre duas, no esquema deste repo
printf '%s\n%s\n' "$v1" "$v2" | sort -t. -k1,1n -k2,2n | tail -1
```

Usar o mesmo `sort -t. -k1,1n -k2,2n` para ordenar listas de tag.

---

## Nunca fazer nesta skill

- `git checkout`, `git stash`, `git clean`, `git add`, `git commit`, `git tag`, `git push`
- `rm`, `mv`, `cp`, `make clean`, `make apk`, `make dist*`
- editar qualquer arquivo, **exceto** oferecer o refresh do índice de skills quando o
  usuário pedir explicitamente

A skill reporta e para. Todo achado vem com o comando; quem roda é o usuário.
