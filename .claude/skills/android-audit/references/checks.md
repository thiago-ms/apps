# Checks da auditoria

Cada check: o que lê, o valor esperado, **as exceções conhecidas hoje** (para o
relatório não ser ruído) e o comando de remediação — que o **usuário** roda, nunca a
skill.

Estado de referência levantado em **2026-08-06**. Um check cuja "exceção conhecida"
deixou de existir é uma boa notícia: reportar como resolvido e sugerir tirar a exceção
daqui.

---

## Severidade A — bloqueia entrega

### A1 · Versão entregue sem tag

Lê: `versionName` em `<app>/app/build.gradle.kts` e `git -C <app> tag --list`.
Espera: existe tag bare igual ao `versionName` atual.

**Exceções conhecidas:** `watch-up` 1.15 (buildada, presente em `dist/`, **nunca**
publicada nem tagueada — estado consistente, não erro); `notes` 1.0 e 1.1 (sem tag).

Remediação (só se o usuário quiser fechar retroativamente):

```bash
git -C <app> tag <ver> <sha>
```

**Nunca rodar.** Escolher o commit certo de um build antigo é julgamento, e no caso da
1.15 do `watch-up` taguear seria errado — a versão não virou release.

### A2 · Hub da raiz desatualizado

Lê: `ls .dist/*.apk` e o `versionName` de cada app.
Espera: exatamente **um** APK release por app, com versão igual ao `versionName` atual.

**Não é achado:** versão antiga ausente (o hub é "a versão de agora", não arquivo
histórico). `watchup-1.9-release.apk` não estar lá é por design.

**É achado:** hub com versão anterior à atual; dois APKs do mesmo app; app com release
já feito e sem APK no hub.

Remediação: `/android-release <app>` (o passo 7 dele conserta), ou o `cp` manual.

### A3 · Hub do app desatualizado

Lê: `ls <app>/.dist/*.apk`. Mesma regra do A2, mas no `.dist/` versionado do submódulo.
Os dois hubs precisam ter o **mesmo** arquivo.

### A4 · `index.html` do hub fora de sincronia

Lê: `.dist/index.html` e `ls .dist/*.apk`.
Espera: todo APK presente aparece como link no índice.

Remediação: rodar `./server.sh` na raiz (regenera o índice antes de servir) ou o loop
inline do `android-release`.

### A5 · `token-map.md` dessincronizado do `.sample/`

O check mais urgente da suíte: se falhar, o próximo `/android-new-app` **falha no
meio da substituição**.

Lê: as contagens de token no `.sample/` e as declaradas em
`../../android-new-app/references/token-map.md`.
Espera: **15 / 16 / 16**.

```bash
cd .sample
a=0; b=0; c=0
while IFS= read -r f; do
  [ "$f" = "./README.md" ] && continue
  x=$(grep -Io 'br\.com\.sample' "$f" 2>/dev/null | wc -l); a=$((a+x))
  y=$(grep -Io 'Sample' "$f" 2>/dev/null | wc -l);          b=$((b+y))
  z=$(grep -Io 'sample' "$f" 2>/dev/null | wc -l);           c=$((c+z-x))
done < <(find . -type f)
printf 'PASS A: %s (esperado 15)\nPASS B: %s (esperado 16)\nPASS C: %s (esperado 16)\n' "$a" "$b" "$c"
```

O `-I` é obrigatório (sem ele o `gradlew` casa por conter "example"). O pass C desconta
o A porque `br.com.sample` contém `sample` minúsculo.

Remediação: refazer as tabelas do `token-map.md` a partir do `.sample/` atual, e validar
com um scaffold de teste que builde.

### A6 · `.sample/` poluído com artefato de build

Lê: existência de `.sample/build/`, `.sample/.gradle/`, `.sample/dist/`,
`.sample/keystore/`, `.sample/.adb/`.
Espera: nenhum. Todos ausentes hoje.

Por que bloqueia: o `cp -r .sample <dir>` levaria isso para o app novo — inclusive
`.gradle/` com caminhos absolutos e `dist/` com APK de outro app.

Remediação: pedir ao usuário para limpar. **Nunca apagar** (lei do não-apagar-artefato);
o `android-new-app` já sabe excluir na cópia como paliativo.

---

## Severidade B — drift

### B1 · Makefile chamando `distApk` sem `:app:`

Lê: `grep -E '^\t.*gradlew.*distApk' <app>/Makefile`.
Espera: `:app:distApk`.

**Exceção conhecida: `site-blocker`** usa a forma sem prefixo — e ali é **inofensivo**,
porque o app é single-module (`include(":app")` só) e não há ambiguidade possível.
Reportar como informativo, não como problema a resolver. Vira problema real só se o
`site-blocker` ganhar um segundo módulo.

Nota: `utilities` e `watch-up` **usam** a forma qualificada. Se alguma auditoria antiga
disse o contrário, estava errada.

### B2 · `docker/Dockerfile` divergente

Lê: `md5sum .sample/docker/Dockerfile */docker/Dockerfile`.
Espera: **um** hash único nos 7. Verdadeiro hoje.

Essa uniformidade é o que faz "build 100% via Docker" valer na frota inteira. Divergência
é achado real — mostrar o diff.

### B3 · Níveis de SDK / toolchain divergentes

Lê: `minSdk`, `targetSdk`, `compileSdk`, `buildToolsVersion` em cada
`app/build.gradle.kts`.
Espera: 26 / 35 / 35 / `35.0.0` nos 7. Uniforme hoje.

Também conferir `JavaVersion.VERSION_17` + `jvmTarget = "17"`, e
`gradle-8.10.2-bin.zip` em `gradle/wrapper/gradle-wrapper.properties`.

### B4 · R8 desligado

Lê: `isMinifyEnabled = true` e `isShrinkResources = true` no bloco `release`.
Espera: ambos nos 7. Verdadeiro hoje.

Desligado é achado: o release perde a razão de existir (~1–2 MB vs ~17 MB do debug).
Pode ser desativação temporária de debug — perguntar em vez de afirmar.

### B5 · Arquivo never-touch editado

Lê: diff de `gradle.properties`, `gradlew`, `gradle/wrapper/*`, `proguard-rules.pro`
contra o `.sample`.
Espera: só as divergências conhecidas abaixo.

**Exceções conhecidas:** `site-blocker/gradle.properties` não tem
`org.gradle.configureondemand=true`; os catálogos de `utilities` e `site-blocker`
divergem bastante (sem room/ksp/work/coil; `utilities` adiciona `osmdroid`,
`site-blocker` adiciona `datastore`). São apps pré-template — divergência esperada, não
drift a consertar.

### B6 · `:app` sem declarar `:core:data` existente

Lê: `grep 'project(":core:data")' <app>/app/build.gradle.kts`.
**Exceção conhecida: `gastos`** — alcança transitivamente pelo `api()` do `:core:ui`.
As duas formas funcionam. Informativo, nunca acionável.

---

## Severidade C — higiene

### C1 · App sem `adb.sh`

**Exceção conhecida: `utilities`.** Instalar por USB não existe lá; o caminho é
`./server.sh`. Não criar o script sem o usuário pedir.

Remediação, se pedirem: copiar do `.sample` e trocar `IMAGE`, `CONTAINER` e `PKG` — 3
linhas. Atenção: `PKG` usa a folha do package (`br.com.utils`), `IMAGE`/`CONTAINER` usam
o slug (`utilitarios`).

### C2 · App sem `build.sh`

**Exceção conhecida: `site-blocker`.** Puramente cosmético — o `make apk` faz o mesmo.

### C3 · `server.sh` na versão antiga (sem gerar índice)

**Exceções conhecidas: `site-blocker` e `utilities`** têm o stub de 4 linhas, sem gerar
`dist/index.html`. Os outros 5 geram. Cosmético: o stub serve o diretório e funciona.

### C4 · Senha de keystore `<slug>123`

Vale nos 7 (`watchup123`, `notes123`, …; `utilities` usa `utils123` com alias
`utilitarios`).

**Mencionar uma vez, informativo, e explicitamente NÃO sugerir rotação.** Todo APK já
publicado está assinado com essa keystore; trocar quebra a continuidade de assinatura
para quem tem o app instalado. `keystore/` é gitignorado nos 7, então nada vaza.

### C5 · App sem backlog

**Exceções conhecidas:** `utilities` usa `docs/` (deliberado); `gastos`,
`site-blocker`, `apps-hub` não têm nada.
Remediação, se quiserem: `/android-backlog <app> create`.

### C6 · Registro do `apps-hub` incompleto

Lê: `HUB_APPS` em `apps-hub/feature/home/.../HubApps.kt` e o bloco `<queries>` em
`apps-hub/app/src/main/AndroidManifest.xml`.
Espera: **as duas listas idênticas**, e um app do monorepo em cada.

Estado hoje: 5 apps nos dois (`notes`, `siteblocker`, `utils`, `watchup`, `gastos`).
Sem o `<queries>`, no Android 11+ o hub vê o app como não instalado mesmo instalado.

App do monorepo ausente dos dois é achado (informativo — pode ser escolha). Presente em
**um só** é bug real.

### C7 · `apps.code-workspace` desatualizado

Lê: as pastas listadas em `apps.code-workspace`.
Estado hoje: lista só `site-blocker`, `utilities`, `watch-up` — falta `gastos`, `notes`,
`apps-hub`. Cosmético, mas trivial de consertar.

### C8 · `server.sh` sem porta configurável

Lê — **com `-F`, obrigatoriamente**:

```bash
grep -Fc 'port="${1:-8000}"' <app>/server.sh
```

Espera: `1` nos 8 (raiz + `.sample` + 6 apps).

O `-F` não é preferência: sem ele o padrão casa **zero**. O `$` seguido de `"` é lido como
âncora de fim de linha pelo GNU grep, então `port="$` nunca casa. Testado — `{1:-8000}`
sozinho casa bem; o problema é só o `$"`. Vale para qualquer grep por interpolação de
shell nesta base.

**Corrigido em 2026-08-06** nos 8 de uma vez. Antes, a porta 8000 era literal e a dica de
URL apontava um arquivo inexistente (`url: $ip:8000/<slug>-x.x-debug.apk`); hoje imprime
`http://$ip:$port/`. Se um app aparecer sem o `port=`, ficou para trás numa cópia velha —
remediação é copiar as três linhas do `.sample/server.sh`.

Efeito colateral que a auditoria deve conhecer: essa mudança **tirou** o único site de
pass C do `server.sh`, e é por isso que o **A5** espera 16 e não 17 no pass C.

---

## Formato do relatório

Agrupar por severidade, e dentro de cada uma **omitir os checks que passaram** — o valor
está no que divergiu. Cada achado em uma linha:

```
A1 · watch-up: versão 1.16 tagueada ✅ (1.15 buildada sem tag — conhecido, ver nota)
B1 · site-blocker: ./gradlew distApk sem :app: — inofensivo (single-module)
C1 · utilities: sem adb.sh — instalar via ./server.sh
```

Terminar com: quantos checks rodaram, quantos passaram, quantos achados por severidade,
e **os comandos de remediação em bloco**, deixando claro que a skill não roda nenhum.

Se nada divergiu além das exceções conhecidas, dizer isso em uma frase — relatório
limpo é resultado, não falta de conteúdo.
