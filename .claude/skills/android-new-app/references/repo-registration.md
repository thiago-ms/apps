# Registro do app novo no git e no hub

Quatro coisas, em ordem. A primeira tem uma armadilha que suja o superprojeto se
invertida.

---

## 1. `git init` **dentro** do novo dir, antes de qualquer staging no superprojeto

A armadilha: o superprojeto (`thiago-ms/apps`) não tem `.gitignore` próprio, e
`.sample/` é rastreado direto nele. Uma cópia de `.sample/` para `<dir>/` fica, aos
olhos do superprojeto, como ~35 arquivos novos não rastreados. Se algo stagear o
superprojeto antes de `<dir>/` ser um repo próprio, os arquivos do app entram no
superprojeto — e desfazer isso depois exige `git rm --cached -r` e um commit de
conserto.

```bash
cd "$ROOT/<dir>"
git init -b main
git add -A
git commit -m "First commit"
```

`First commit` é a mensagem que os 6 apps usam no commit inicial. Verificado.

Conferir antes de seguir: `git -C "$ROOT" status --short` deve mostrar `<dir>/` como
**um único** item não rastreado (ou nada, se o `.gitignore` do superprojeto o cobrir),
nunca 35 arquivos soltos.

---

## 2. Criar o repo remoto

Todos os 6 apps ficam em `thiago-ms`, privados, com o mesmo nome do diretório.
Verificado em `.gitmodules`.

```bash
cd "$ROOT/<dir>"
gh repo create thiago-ms/<dir> --private --source=. --push
```

Se o `gh` não estiver autenticado, `gh auth status` diz. Não tentar contornar com
`git remote add` + push por HTTPS — o repo remoto precisa existir primeiro.

O superprojeto usa SSH nas URLs de submódulo (`git@github.com:thiago-ms/<dir>.git`),
então conferir que o remote criado é SSH:

```bash
git remote -v   # tem que ser git@github.com:..., não https://
git remote set-url origin git@github.com:thiago-ms/<dir>.git   # se veio HTTPS
```

---

## 3. `git submodule add` num diretório que já existe

O `submodule add` normalmente clona; aqui o diretório já está lá com conteúdo. O jeito
que funciona é apontar para o dir existente — o git reconhece que é um repo e só
registra:

```bash
cd "$ROOT"
git submodule add git@github.com:thiago-ms/<dir>.git <dir>
```

Se reclamar que `<dir>` já existe no índice, é sintoma da armadilha do passo 1 (os
arquivos do app entraram no superprojeto). Consertar antes:

```bash
git rm -r --cached <dir>
```

Depois conferir o `.gitmodules`, que deve ganhar exatamente:

```
[submodule "<dir>"]
	path = <dir>
	url = git@github.com:thiago-ms/<dir>.git
```

E commitar no superprojeto:

```bash
git add .gitmodules <dir>
git commit -m "App {{Pascal}} 1.0"
```

`App <rootProject.name> <ver>` é o padrão de mensagem do superprojeto (`App WatchUp
1.16`, `App Notes 1.6`). Para o primeiro commit de um app o `gastos` usou só
`App gastos` — as duas formas existem; preferir a com versão.

---

## 4. Registrar no `apps-hub` — dois lugares, sempre juntos

O `apps-hub` é o app que lista e abre os outros. Um app novo só aparece nele se **os
dois** registros forem editados; o README do `apps-hub` avisa isso explicitamente.

**4a.** `apps-hub/feature/home/src/main/kotlin/br/com/appshub/feature/home/HubApps.kt`:

```kotlin
val HUB_APPS = listOf(
    HubApp("br.com.notes", "Notas"),
    // ...
    HubApp("br.com.{{leaf}}", "{{Pascal}}"),   // ← adicionar
)
```

O segundo argumento é o rótulo de **fallback**, usado só quando o app não está
instalado; instalado, o hub lê o label real do `PackageManager`.

**4b.** `apps-hub/app/src/main/AndroidManifest.xml`, no bloco `<queries>`:

```xml
    <queries>
        <package android:name="br.com.notes" />
        <!-- ... -->
        <package android:name="br.com.{{leaf}}" />   <!-- ← adicionar -->
    </queries>
```

Sem o `<queries>`, no Android 11+ (API 30+) o `getLaunchIntentForPackage` e o
`getApplicationIcon` retornam null e o app aparece esmaecido como "não instalado"
mesmo estando instalado.

Os dois usam o **`applicationId`** (`br.com.{{leaf}}`), não o slug nem o diretório.

**Isso é uma alteração no `apps-hub`, que é outro submódulo** — ou seja, gera um
release dele. Não fazer isso silenciosamente: perguntar, e se o usuário aceitar, dizer
que o `apps-hub` precisa de `/android-release apps-hub` depois para o registro chegar
ao aparelho. Se recusar, o app novo simplesmente não aparece no hub até alguém
registrar.

---

## Ordem completa, resumida

```
1. git init + First commit dentro de <dir>        ← nunca depois do passo 3
2. gh repo create thiago-ms/<dir> --private --source=. --push
3. git submodule add + commit no superprojeto
4. (opcional, gated) registrar no apps-hub → exige release do apps-hub
```

O passo 4 é o único que mexe em código de outro app. Os passos 2 e 3 podem ser
recusados pelo usuário ("só local, registro depois") — nesse caso parar depois do 1 e
dizer o que ficou pendente.
