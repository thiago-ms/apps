# Mapa de tokens do `.sample/`

A transformação, **não** o conteúdo. Os corpos de arquivo vivem no `.sample/` e chegam
por `cp -r` — nada aqui duplica o template.

Três tokens de entrada, todos **fornecidos pelo usuário**, nunca derivados em silêncio:

| token | é | exemplo real |
|---|---|---|
| `{{leaf}}` | folha do package (`br.com.<leaf>`) | `utilities` → **`utils`** |
| `{{Pascal}}` | nome de exibição em PascalCase | `utilities` → **`Utilitarios`** |
| `{{slug}}` | slug de artefato (APK, imagem Docker, container) | `utilities` → **`utilitarios`** |

Os três podem divergir do nome do diretório e **divergem de fato** em 3 dos 6 apps.
Tabela completa em [../../README.md](../../README.md).

**Contagens verificadas** rodando a transformação inteira numa cópia do `.sample/`
(tokens de teste `leaf=zeta`, `Pascal=Zeta`, `slug=zetinha`), com build
`BUILD SUCCESSFUL` e resíduo zero. Se alguma contagem não bater, **parar** — o
`.sample/` mudou e este mapa precisa ser refeito antes de gerar app.

---

## Ordem

```
1. cp -r .sample <dir>
2. renomear diretórios e arquivo          ← ANTES de qualquer texto
3. PASS A: br.com.sample → br.com.{{leaf}}
4. PASS B: Sample        → {{Pascal}}
5. PASS C: sample        → {{slug}}       ← DEPOIS do A, obrigatoriamente
6. reescrever README.md do zero
```

**Por que o A antes do C:** o pass C casa `sample` minúsculo cru. Se rodasse antes,
transformaria `br.com.sample` em `br.com.{{slug}}` — errado sempre que o slug diverge
da folha, que é o caso do `utilities` (`br.com.utils` com slug `utilitarios`).

**Por que os renames antes de tudo:** enquanto existir o diretório
`src/main/kotlin/br/com/sample/`, o pass C acha `sample` no caminho e o sweep de
resíduo dá falso positivo.

**Por que o B é um pass só:** `Sample` → `{{Pascal}}` já resolve `SampleTheme` →
`{{Pascal}}Theme`, `SampleApp` → `{{Pascal}}App`, `Theme.Sample` → `Theme.{{Pascal}}`
e `rootProject.name` de uma vez. Não precisa de passes separados por símbolo — e
tentar separar é que gera bug. Case-sensitive, sempre.

---

## Passo 2 — renomeações (4)

```bash
mv <dir>/app/src/main/kotlin/br/com/sample          <dir>/app/src/main/kotlin/br/com/{{leaf}}
mv <dir>/core/ui/src/main/kotlin/br/com/sample      <dir>/core/ui/src/main/kotlin/br/com/{{leaf}}
mv <dir>/feature/home/src/main/kotlin/br/com/sample <dir>/feature/home/src/main/kotlin/br/com/{{leaf}}
mv <dir>/app/src/main/kotlin/br/com/{{leaf}}/navigation/SampleApp.kt \
   <dir>/app/src/main/kotlin/br/com/{{leaf}}/navigation/{{Pascal}}App.kt
```

O rename do arquivo vem **depois** dos três de diretório (o caminho já mudou).

---

## PASS A — `br.com.sample` → `br.com.{{leaf}}`

**15 ocorrências em 10 arquivos** (fora do README). Padrão ancorado: `br\.com\.sample`.

| arquivo | linhas | n |
|---|---|---|
| `adb.sh` | 23 (`PKG=`) | 1 |
| `app/build.gradle.kts` | 20 (`namespace`), 25 (`applicationId`) | 2 |
| `app/.../MainActivity.kt` | 1 (package), 7, 8 (imports) | 3 |
| `app/.../navigation/Routes.kt` | 1 | 1 |
| `app/.../navigation/{{Pascal}}App.kt` | 1 (package), 20 (import do HomeScreen) | 2 |
| `core/ui/build.gradle.kts` | 8 (`namespace`) | 1 |
| `core/ui/.../component/Components.kt` | 1 | 1 |
| `core/ui/.../theme/Theme.kt` | 1 | 1 |
| `feature/home/build.gradle.kts` | 8 (`namespace`) | 1 |
| `feature/home/.../HomeScreen.kt` | 1 (package), 16 (import do SectionHeader) | 2 |

Note que `MainActivity.kt:7` é `import br.com.sample.core.ui.theme.SampleTheme` — o
pass A mexe só no prefixo e deixa `SampleTheme` para o pass B. Os dois são
substrings ortogonais; é por isso que a sequência funciona.

## PASS B — `Sample` → `{{Pascal}}`

**16 ocorrências em 11 arquivos.** Case-sensitive.

| arquivo | linhas | n |
|---|---|---|
| `settings.gradle.kts` | 23 (`rootProject.name`) | 1 |
| `app/src/main/AndroidManifest.xml` | 12, 18 (`android:theme`) | 2 |
| `app/src/main/res/values/strings.xml` | 3 (`app_name`) | 1 |
| `app/src/main/res/values/themes.xml` | 9 (`Theme.Sample`) | 1 |
| `app/src/main/res/values-night/themes.xml` | 4 (`Theme.Sample`) | 1 |
| `app/.../MainActivity.kt` | 7, 8 (imports), 15 (`SampleTheme {`), 16 (`SampleApp()`) | 4 |
| `app/.../navigation/{{Pascal}}App.kt` | 28 (`fun SampleApp()`) | 1 |
| `core/ui/.../theme/Theme.kt` | 31 (`fun SampleTheme(`) | 1 |
| `feature/home/.../HomeScreen.kt` | 27 (`TopAppBar(title = { Text("Sample") })`) | 1 |
| `server.sh` | 5 (`app=`) | 1 |
| `Makefile` | 55 (`CN=Sample, … O=Sample`) | **2** |

Duas que o README do `.sample` **não** menciona e são fáceis de perder: o
`Text("Sample")` do `TopAppBar` no `HomeScreen.kt:27` e o `-dname` da keystore no
`Makefile:55`.

## PASS C — `sample` → `{{slug}}`

**16 ocorrências em 4 arquivos.** Minúsculo, e só depois do pass A.

| arquivo | linhas | n |
|---|---|---|
| `Makefile` | 13, 15 (texto do help), 52 (`-alias sample`), 54 (`-storepass`/`-keypass sample123` ×2), 56 (`storePassword`/`keyAlias`/`keyPassword` ×3) | **8** |
| `app/build.gradle.kts` | 109, 110 (debug), 120, 121 (release) — os `rename {}` e `println` do distApk | 4 |
| `adb.sh` | 6 (comentário), 21 (`IMAGE=`), 22 (`CONTAINER=sample-adb`) | 3 |
| `docker-compose.yml` | 14 (`image: sample-android-build:latest`) | 1 |

`server.sh` **não** tem site de pass C. Tinha, até 2026-08-06: a linha 4 era
`echo "url: $ip:8000/sample-x.x-debug.apk"`. Quando o script ganhou porta configurável, a
dica passou a apontar o índice (`http://$ip:$port/`) e o slug saiu — o arquivo
`<slug>-x.x-debug.apk` nunca existiu de verdade. O `server.sh` continua com **um** site de
pass B (`app="Sample"`, linha 6).

`sample123` → `{{slug}}123` sai de graça, porque a substituição é de substring — é o
que faz o `Makefile` ter 8 sites em 5 linhas.

**Cuidado ao contar:** `core/ui/build.gradle.kts` e `feature/home/build.gradle.kts`
aparecem se você listar arquivos com `grep -l sample`, mas **não têm site de pass C** —
casam só pelo `br.com.sample` do `namespace`, que o pass A já resolveu. Para conferir
por arquivo, contar `sample` e subtrair `br\.com\.sample`.

Senha de keystore fica `{{slug}}123` — fraca de propósito e **nunca versionada**
(`keystore/` é gitignorado). **Não imprimir a senha em resumo nenhum.**

---

## Nunca tocar — 9 caminhos

Verificado byte-idêntico entre `.sample/` e os 6 apps:

1. `docker/Dockerfile` — byte-idêntico nos 7. É o que faz "build 100% via Docker" ser
   verdade na frota.
2. `gradle.properties`
3. `gradlew`, `gradlew.bat` — executáveis
4. `gradle/wrapper/gradle-wrapper.jar` — binário, 43.583 bytes
5. `gradle/wrapper/gradle-wrapper.properties` — pina `gradle-8.10.2-bin.zip`
6. `gradle/libs.versions.toml` — já traz room/ksp/work/coil/documentfile/junit como
   ganchos para `:core:data` futuro
7. `build.gradle.kts` (raiz) — só aliases de plugin, todos `apply false`
8. `app/proguard-rules.pro`
9. `.gitignore`, `.dockerignore`, e os XML de `mipmap-anydpi-v26/`

Nenhum deles contém token. Se o sweep de resíduo apontar para um destes, algo saiu
errado nos passes.

---

## Sweep de resíduo

```bash
grep -rIl 'sample\|Sample' <dir> | sort
```

**Allowlist — o que pode aparecer:**

- `<dir>/README.md`, se ainda não foi reescrito (passo 6). Depois de reescrito, nada.

Qualquer outro arquivo = bug. Ação: mostrar o arquivo e a linha, **parar**, e não
prosseguir para o build.

O `-I` importa: sem ele o `gradlew` casa (contém a palavra `example` em texto de ajuda)
e vira falso positivo.

Sweep completo, contando:

```bash
cd <dir>
for pat in 'br\.com\.sample' 'Sample' 'sample'; do
  n=$(grep -rIo "$pat" . --exclude=README.md | wc -l)
  printf '%-18s residual: %s (esperado 0)\n' "$pat" "$n"
done
```

---

## Verificação final

```bash
cd <dir>
grep -n 'namespace\|applicationId' app/build.gradle.kts   # br.com.{{leaf}}
grep -n 'rootProject.name'         settings.gradle.kts    # "{{Pascal}}"
grep -n 'IMAGE=\|CONTAINER=\|PKG=' adb.sh                 # slug, slug, leaf
grep -n 'image:'                   docker-compose.yml     # {{slug}}-android-build
make apk                                                   # tem que ficar verde
```

A checagem que pega o erro mais traiçoeiro é o `adb.sh`: `PKG` usa a **folha**
(`br.com.{{leaf}}`) enquanto `IMAGE`/`CONTAINER` usam o **slug**. Se os três
estiverem iguais num app onde folha ≠ slug, um pass rodou fora de ordem.
