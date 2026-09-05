# Casos de borda — `android-new-app`

Referenciados por ID a partir dos passos do `SKILL.md`.

---

**E1 — o diretório já existe.**
Parar. Não mesclar, não sobrescrever, não sugerir `<dir>-2`. Mostrar o que tem lá
(`ls <dir>` + o `applicationId` se houver `app/build.gradle.kts`) e perguntar se o
usuário quer outro nome. Um diretório existente com conteúdo é quase sempre um app que
já existe.

**E2 — nome de diretório inválido.**
Sanitizar para minúsculas, sem acento, hífen permitido (`watch-up`, `site-blocker`,
`apps-hub` usam hífen). Recusar espaço, maiúscula, ponto e underscore. O diretório
**não** precisa casar com a folha do package nem com o slug — é justamente por isso que
os três tokens são perguntados.

**E3 — colisão de `applicationId`.**
Dois apps com o mesmo `applicationId` não coexistem num aparelho: instalar o segundo
substitui o primeiro. Checar antes de gerar:

```bash
grep -h 'applicationId' "$ROOT"/*/app/build.gradle.kts
```

Colidiu: parar e pedir outra folha.

**E4 — colisão de slug.**
Slug repetido colide em três lugares: nome do APK em `dist/` e nos dois hubs, tag da
imagem Docker (`<slug>-android-build:latest`) e nome do container adb (`<slug>-adb`).
O sintoma é confuso (o build de um app usa a imagem do outro), então checar:

```bash
grep -h -o '"[a-z]*-\$version-debug\.apk"' "$ROOT"/*/app/build.gradle.kts
```

**E5 — Docker desligado.**
`docker info` falhando: o build do passo 7 não vai rodar. Ainda **vale gerar o app** —
a transformação é toda de arquivo. Avisar que a validação por build ficou pendente e
dar o comando (`make apk`) para o usuário rodar quando o Docker subir. Não fingir que
está validado.

**E6 — a imagem Docker não existe ainda.**
`make apk` chama `make image` implicitamente pela cadeia de dependências? **Não** —
`dist` depende de `wrapper`, que depende de `image`. Então `make apk` constrói a imagem
sozinho, mas isso baixa o Android SDK inteiro (~2 GB) e leva bem mais que os 3–6 min de
um build normal. Avisar sobre isso antes de disparar.

Atalho legítimo, já que o `docker/Dockerfile` é byte-idêntico nos 7 apps: reusar uma
imagem existente por tag, em vez de reconstruir.

```bash
docker images --format '{{.Repository}}' | grep -m1 'android-build'   # achar uma
docker tag <existente>-android-build:latest <slug>-android-build:latest
```

Dizer ao usuário que fez isso e por que é seguro (Dockerfile idêntico).

**E7 — superprojeto com árvore suja.**
Não limpar, não stashear. Capturar `git status --short` no Preflight e mostrar no
resumo do plano — o hook `warn-before-changes` vai perguntar de qualquer forma, e o
usuário decide. Nunca `git stash`.

**E8 — falha no meio da substituição.**
O `<dir>` fica num estado híbrido (pass A aplicado, B não). **Não** apagar o diretório
para "começar limpo" — a lei do não-apagar-sem-perguntar vale para artefato que a
própria skill criou. Fazer:

1. Dizer exatamente qual pass falhou e em qual arquivo.
2. Rodar o sweep de resíduo e mostrar o que sobrou.
3. Oferecer duas saídas: continuar do pass que falhou (os passes são idempotentes —
   reaplicar um pass já aplicado casa 0 vezes e não faz nada), ou remover `<dir>` e
   recomeçar.
4. Só remover se o usuário escolher, nomeando o caminho.

**E9 — contagem de token não bate com o `token-map.md`.**
Parar antes de seguir. Significa que o `.sample/` mudou desde que o mapa foi escrito.
Mostrar esperado vs encontrado por pass, e dizer que o `token-map.md` precisa ser
refeito (`/android-audit` tem esse check). Não "adaptar na hora" e seguir — uma
substituição parcial gera app que compila e falha em runtime.

**E10 — resíduo de `sample` depois de todos os passes.**
Fora do `README.md`, é bug. Mostrar arquivo:linha e parar. Usar `grep -rI` (com o `-I`)
— sem ele o `gradlew` casa por conter a palavra `example` e vira falso positivo.

**E11 — `gh` não autenticado ou sem permissão em `thiago-ms`.**
Parar no passo de registro, manter o `git init` + commit local já feitos (são úteis
sozinhos), e dizer o que falta: `gh auth login`, depois `gh repo create` e
`git submodule add`. Não criar o repo em outra conta.

**E12 — o usuário pede pra registrar no `apps-hub`.**
Isso edita **outro submódulo** (`HubApps.kt` + `<queries>`) e portanto gera um release
do `apps-hub`. Perguntar antes. Se aceitar, fazer as duas edições juntas (nunca uma só)
e avisar que o registro só chega ao aparelho depois de `/android-release apps-hub`.

**E13 — o app novo precisa de Room / persistência.**
Não fazer aqui. `.sample` builda verde de fábrica; adicionar KSP+Room no mesmo passe
mistura as causas quando o build final falha. Terminar o app, confirmar verde, e
apontar `/android-new-core <dir>/data`.

**E14 — o usuário quer mais telas além da home.**
Uma feature além da home é `/android-new-feature <dir>/<x>`, depois do primeiro build
verde. Esta skill entrega o app com a `:feature:home` do `.sample` e nada mais.

**E15 — pedido para gerar o app fora do monorepo.**
Recusar. Toda a transformação depende do `.sample/` e do layout do superprojeto. Para
projeto Android fora daqui não existe template — dizer isso em vez de improvisar.

**E16 — o `.sample/` tem `build/`, `.gradle/` ou `dist/`.**
Alguém buildou o template no lugar. O `cp -r` levaria esse lixo para o app novo
(inclusive `dist/` com APK de outro app, e `.gradle/` com caminhos absolutos).
Excluir na cópia e **avisar**:

```bash
rsync -a --exclude build/ --exclude .gradle/ --exclude dist/ --exclude .adb/ \
      --exclude keystore/ "$ROOT/.sample/" "$ROOT/<dir>/"
```

Não apagar esses diretórios do `.sample/` — só não copiar. Mencionar no relatório para
o usuário limpar se quiser.
