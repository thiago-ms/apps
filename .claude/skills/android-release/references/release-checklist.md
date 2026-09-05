# O ritual de release, na ordem, com as asserções

Dez passos. A ordem é carregada — trocar 7 por 8 deixa o superprojeto apontando pra
um commit que não existe no remoto; trocar 5 por 6 publica um APK que ninguém abriu.

`<app>` = diretório. `<slug>` = slug de artefato (**não** é o diretório: `watch-up` →
`watchup`, `utilities` → `utilitarios` — tabela em [../../README.md](../../README.md)).
`<ver>` = novo `versionName`.

---

## 1. Preflight — read-only

```bash
ROOT=$(git rev-parse --show-superproject-working-tree 2>/dev/null || git rev-parse --show-toplevel)
[ -d "$ROOT/.sample" ] && [ -f "$ROOT/.gitmodules" ] && [ -f "$ROOT/server.sh" ] || exit 1
cd "$ROOT/<app>"

grep -nE 'versionName|versionCode' app/build.gradle.kts   # versão atual
git tag --list                                            # tags existentes (bare: 1.16)
git log -1 --format=%s                                    # estilo da mensagem do submódulo
git status --short                                         # árvore suja?
ls dist/ .dist/ "$ROOT/.dist/"                            # artefatos existentes
ls specs/backlog-*.md docs/*.md 2>/dev/null               # tem backlog?
grep -m1 -E 'gradlew.*distApk' Makefile                   # forma do target (:app: ou não)
docker info >/dev/null 2>&1                                # Docker vivo?
git -C "$ROOT" log -1 --format=%s                          # estilo da mensagem do superprojeto
```

**Nunca** `rm`, `git clean`, `make clean`, `git stash` — em nenhum passo, nem aqui.

## 2–3. Interview + resumo

Ver [interview-flow.md](interview-flow.md). O resumo (≤20 linhas) inclui o
`git status --short` do passo 1: árvore suja faz o hook `warn-before-changes`
perguntar depois, e isso não deve ser surpresa.

## 4. Bump — as 4 asserções

Editar **as duas** linhas em `<app>/app/build.gradle.kts`:

```kotlin
        versionCode = <novo-code>
        versionName = "<ver>"
```

Antes de gravar, todas têm que passar:

| # | Asserção | Comando |
|---|---|---|
| 1 | `<ver>` > versão atual | comparar numericamente minor a minor |
| 2 | `<novo-code>` > code atual | idem |
| 3 | `<ver>` não é tag existente | `git tag --list \| grep -qx '<ver>'` → tem que falhar |
| 4 | nenhum artefato `<ver>` já existe | `ls dist/ .dist/ $ROOT/.dist/ \| grep -q '<slug>-<ver>-'` → tem que falhar |

Qualquer uma falhando: **parar e perguntar**. Nunca sobrescrever uma versão.

## 5. Build

```bash
cd "$ROOT/<app>" && make dist-all
```

Longo (~3–8 min; mais se a imagem Docker não existir ainda). Rodar em background com
status "compilando debug + release…". Depois, assertar os dois artefatos **pelo slug**:

```bash
ls dist/<slug>-<ver>-debug.apk dist/<slug>-<ver>-release.apk
```

Se o release saiu com tamanho parecido com o do debug (~17 MB em vez de ~1–2 MB), o
R8 não rodou — investigar antes de seguir, não publicar.

## 6. Gate de smoke no device

**Este é o único passo que não pode subir pro Round 2 do interview: o artefato ainda
não existe quando o Round 2 acontece.** Não "otimizar" isso.

R8 (`isMinifyEnabled` + `isShrinkResources`, ligados nos 7 apps) quebra reflexão só em
runtime. Um release que compila pode abrir e crashar.

**O caminho padrão é o server — baixar pelo celular.** O APK **já existe** do passo 5;
nada aqui rebuilda.

```bash
cd "$ROOT/<app>" && ./server.sh
```

Três detalhes que fazem isso funcionar de verdade:

- **`server.sh` bloqueia** (`python3 -m http.server` em foreground). Rodar **em
  background**, capturar o `ip:` que ele imprime, e passar a URL ao usuário:
  `http://<ip>:8000/` — PC e celular na mesma rede.
- **Dizer o nome exato do arquivo.** O `index.html` que ele gera lista *todos* os APKs de
  `dist/` (debug e release, todas as versões). O usuário precisa tocar em
  `<slug>-<ver>-release.apk`, não no debug nem numa versão velha.
- **Matar o server depois de responder o gate** — **por porta, nunca por padrão de texto**:
  ```bash
  fuser -k 8000/tcp        # ou a porta que você passou
  ```
  Isso não é opcional num lote multi-app: sem isso o release seguinte colide.

  **Não use `pkill -f 'http.server'`.** Duas armadilhas, as duas verificadas:
  matar o PID do `./server.sh` **não mata o `python3` filho** (fica órfão segurando a
  porta), e o padrão `http.server` **casa a própria linha de comando de quem chama**, então
  o `pkill` mata o próprio shell no meio do lote. `fuser -k <porta>/tcp` não tem nenhum dos
  dois problemas.
- **A porta é sobrescrevível:** `./server.sh <porta>`. Serve para quando a 8000 já está
  ocupada por outra coisa (outra sessão, outro servidor). **Não** use isso para abrir dois
  gates ao mesmo tempo — o teste é num aparelho só, e duas URLs simultâneas só confundem
  qual APK você instalou.

**Alternativa por USB**, se o usuário pedir — e note que é `install`, **não**
`build-install-release`, porque o artefato já existe e rebuildar aqui é desperdício:

```bash
./adb.sh install "dist/<slug>-<ver>-release.apk"
```

Primeira vez no aparelho: `./adb.sh authorize` e `./adb.sh devices`. O `adb.sh`
desinstala+reinstala sozinho se a assinatura divergir. `utilities` **não tem `adb.sh`** —
lá só existe o caminho do server.

Depois perguntar (Round 3). Resposta "não": **parar aqui**. O bump fica — é
deliberado, a versão queimada evita reusar um número que já gerou artefato. Dizer isso
ao usuário.

## 7. Publicar nos dois hubs

Dois destinos, semânticas diferentes — ver [artifact-safety.md](artifact-safety.md).

```bash
cp dist/<slug>-<ver>-release.apk .dist/            # hub do app (versionado no submódulo)
cp dist/<slug>-<ver>-release.apk "$ROOT/.dist/"    # hub da raiz (versionado no superprojeto)
```

Cada hub guarda **exatamente um** APK por app. O superado precisa sair:
**perguntar primeiro, nomeando o arquivo exato, nunca em lote.**

Depois regenerar o `index.html` da raiz — byte-compatível com o que o `server.sh`
produz (mesmo loop, para o diff do commit ficar limpo):

```bash
cd "$ROOT"
{
  echo '<!DOCTYPE html>'
  echo '<html><head>'
  echo '<meta name="viewport" content="width=device-width, initial-scale=1">'
  echo '</head><body>'
  echo "<h3>Apps</h3>"
  echo '<table border="1">'
  for f in .dist/*.apk; do
    [ -e "$f" ] || continue
    name=$(basename "$f")
    printf '<tr><td><a href="%s">%s</a></td></tr>\n' "$name" "$name"
  done
  echo '</table>'
  echo '</body></html>'
} > .dist/index.html
```

Asserção final: `ls "$ROOT/.dist/<slug>"*.apk | wc -l` == 1.

## 8. Git — submódulo antes do superprojeto

**Ordem obrigatória.** Pushar o superprojeto primeiro deixa o ponteiro apontando pra
um commit que o remoto não tem.

```bash
cd "$ROOT/<app>"
git add -A
git commit -m "Release <ver>"          # estilo confirmado no passo 1
git tag <ver>                          # bare, sem prefixo v
git push --follow-tags

cd "$ROOT"
git add <app> .dist/
git commit -m "App <Nome> <ver>"       # <Nome> = rootProject.name; ver ../../README.md
git push
```

`<Nome>` é o `rootProject.name`, não o diretório: `App WatchUp 1.16`, `App Notes 1.6`.
`apps-hub` tem espaço no nome (`Apps Hub`).

Forma verificada do commit de release do superprojeto (`git show HEAD` no dia da
1.16) — é exatamente isto que deve aparecer:

```
App WatchUp 1.16
 .dist/index.html               |   2 +-
 .dist/watchup-1.14-release.apk | Bin -> 0 bytes      (o superado sai)
 .dist/watchup-1.16-release.apk | Bin 0 -> ... bytes  (o novo entra)
 watch-up                       |   2 +-              (ponteiro do submódulo)
```

Se o escopo git escolhido no interview foi "só local", parar antes dos `push` e dizer
o que ficou pendente.

## 9. Sincronizar o backlog

Só se o app tiver `specs/backlog-*.md`. Ver [backlog-sync.md](backlog-sync.md).

## 10. Reportar

Dizer, nesta ordem: versão publicada, os dois APKs em `dist/`, o que entrou nos dois
hubs e o que saiu, tag criada, commits e pushes feitos, itens de backlog fechados.

E as formas de instalar, **na ordem em que o usuário usa**:

- **Pelo celular (padrão):** `./server.sh` no diretório do app → abrir
  `http://<ip>:8000/` no navegador do celular (PC e celular na mesma rede) → tocar em
  `<slug>-<ver>-release.apk`. Se o server do gate do passo 6 já foi morto, rodar de novo.
- **Por USB (só se pedirem):** `./adb.sh install "dist/<slug>-<ver>-release.apk"` —
  instala o artefato que já existe. Primeira vez: `./adb.sh authorize` e
  `./adb.sh devices`; o `adb.sh` desinstala+reinstala sozinho se a assinatura divergir.
  `build-install-release` também funciona, mas **rebuilda à toa** nesta altura.

Para `utilities`, mencionar só o caminho do `server.sh` — não tem `adb.sh`.

---

## A divergência do Makefile

`site-blocker` chama `./gradlew --no-daemon distApk` **sem** o prefixo `:app:`; os
outros seis usam `:app:distApk`. Verificado.

Isso **não é um problema** lá: `site-blocker` é single-module (`include(":app")` só),
então não existe ambiguidade possível. Não "consertar" durante um release — mexer no
Makefile no meio de uma entrega é risco sem ganho. `make dist-all` funciona igual nos
sete.
