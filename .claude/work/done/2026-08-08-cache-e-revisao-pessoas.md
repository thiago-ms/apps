# Lote cache-e-revisao-pessoas — 2026-08-08

**Estado:** concluído

> Os 3 itens entregues e **as duas versões validadas em aparelho** — primeira vez nesta
> sessão. As duas migrações de Room passaram por cima de bancos v1 com dados reais.
> Continua **sem git**: os dois apps nunca foram inicializados como repositório.

**Origem:**

> Quero que o MyApps guarde "cache" do nome do nome e ícone dos apps para usar de fallback quando os apps não estiverem mais instalado.
>
> Revise o app Pessoas, tem muita coisa estranha, inclusive com navegação cortando no bottom do app. Outra coisa, eu pedi para poder adicionar fotos ao contato, não só a foto do contato.

## Itens

| # | App | Tipo | Item | Natureza | Backlog | Estado |
|---|-----|------|------|----------|---------|--------|
| 1 | apps-hub-lists | ✨ | Cache de nome e ícone dos apps, com fallback | edicao | #4 | ✅ entregue v1.3 |
| 2 | people | ✨ | Fotos múltiplas + persistência durável (fecha o #1) | edicao | #6 | ✅ entregue v1.3 |
| 3 | people | 🐛 | Revisão da v1.2 — os 11 achados do scout | edicao | #5 | ✅ entregue v1.3 |

## Ordem

- **apps-hub-lists**: item único.
- **people**: **#6 antes do #5**, apesar da numeração do backlog. O #5 inclui trocar
  `remember` por `rememberSaveable` no formulário do detalhe, e o #6 muda quais campos
  esse formulário tem (galeria de fotos no lugar de uma foto só). Na ordem inversa o
  `Saver` customizado seria escrito duas vezes, a segunda desfazendo a primeira.
  Numeração de backlog e ordem de execução são eixos diferentes — isso é esperado.
- Entre os dois apps não há ordem: correm em paralelo, builds serializados por `flock`.

## Releases

| App | Versão | Estado |
|-----|--------|--------|
| apps-hub-lists (MyApps) | 1.3 | ✅ publicado nos hubs · gate **aprovado** · ⚠️ sem git |
| people (Pessoas) | 1.3 | ✅ publicado nos hubs · gate **aprovado** · ⚠️ sem git |

## Pendências ao fechar

1. **Os dois apps continuam sem repositório git.** Não houve `git init`, não existe
   repositório remoto, não são submódulos — aparecem como diretórios untracked no
   superprojeto. **Os dois implementadores esbarraram nisso de forma independente** e
   reportaram: o passo de commit + tag no submódulo do `/android-release` não tem onde
   rodar. Enquanto isso não for resolvido, `git -C <app> status/diff` não diz nada sobre o
   app e a conferência de escopo é feita por mtime.
2. **O superprojeto acumula cinco rodadas sem commit**: site-blocker 2.4, notes 1.7,
   watch-up 1.17, e os dois apps novos até a 1.3.
3. **Os hubs guardam várias versões por app**, não uma — `pessoas` e `appslists` têm 1.0,
   1.1, 1.2 e 1.3; `watch-up`, `notes` e `site-blocker` têm duas cada. Foi escolha do
   usuário nas primeiras rodadas e nunca foi revertida.
4. **Nenhuma das duas `Migration` tem teste automatizado.** `exportSchema = false` nos dois
   apps e não há `room-testing`/`MigrationTestHelper` no catálogo. A validação foi manual,
   no aparelho — e passou, mas a próxima migração terá o mesmo ponto cego. Ligar
   `exportSchema` e adicionar a coordenada é item próprio, e vale para os dois.

## Notas de execução

### Colisão de porta no gate

O gate do Pessoas falhou na primeira tentativa: a porta 8000 estava ocupada por um
`./server.sh` **da raiz** do monorepo (servindo o hub `.dist/`), rodando desde 00:27 e
iniciado pelo usuário — não por esta sessão. Resolvido usando `./server.sh 8001`, que é
exatamente o caso de uso do parâmetro de porta. **O server da raiz não foi morto.**

Consequência que confundiu o usuário: ele abriu a URL da 8000 (o hub) e não achou o
`pessoas-1.3-release.apk`, porque o hub só recebe o APK **depois** do gate. A ordem está
certa — testar antes de publicar — mas vale saber que durante um gate o hub da raiz está
sempre uma versão atrás.

Aprovado **com gate de aparelho**, ao contrário das rodadas anteriores. O motivo é
concreto: são **duas migrações de Room**, e migração incorreta não falha no build — falha
no primeiro boot de quem já tem dados gravados.

## Fora do lote

- **Visualizador de foto em tela cheia** no Pessoas. A galeria entrega adicionar, eleger
  e remover; ver a foto ampliada com swipe é feature própria.
- **Receiver de `PACKAGE_REPLACED`** no MyApps para revalidar o cache quando um app é
  atualizado e troca de ícone. Fica **revalidação oportunista** (quando a home nota que o
  `PackageManager` diverge do cache, regrava em background), sem componente novo no
  manifesto.
- **`exportSchema = true` + `MigrationTestHelper`** nos dois apps. Hoje `exportSchema` é
  `false`, então não há JSON de schema versionado e **não dá para testar migração
  automaticamente**. O gate das duas migrações é manual, no aparelho. Ligar isso é item
  próprio e vale para os dois.

## Notas

### Decisões de produto tomadas na aprovação (2026-08-08)

- **Revisão do Pessoas: os 11 achados entram**, incluindo o #3 (rotação), que é o mais
  caro — exige `Saver` customizado para a lista de links e o conjunto de listas.
- **Fotos múltiplas e o bug da foto que some (#1) são resolvidos JUNTOS.** Uma galeria de
  N fotos com URIs que expiram degrada mais que uma foto que expira, e separado custaria
  duas migrações de Room e dois toques no mesmo ponto de gravação.
- **Cache de ícone em arquivo (`filesDir`)**, não BLOB no Room. O `.db` fica pequeno e a
  leitura sob demanda casa com o `produceState` que o `IconeApp` já usa. O preço é
  invalidação manual.
- **O backup JSON leva nome e ícone (base64)**. É o único jeito de o fallback funcionar em
  aparelho novo — que é justamente o cenário "o app não está instalado".

### Decisões técnicas que tomei sem consultar

- **Pessoas #4 do scout (ordem)**: editar contato **preserva** a ordem dele nas listas.
  Hoje `salvarContato` faz `limparListasDoContato` + recalcula `MAX(ordem)`, jogando o
  contato para o fim de todas as listas a cada edição.
- **Pessoas #7 (edição descartada)**: sair com alteração pendente **pergunta** antes de
  descartar (`BackHandler` + diálogo). Não autosalva.
- **Pessoas #10 (nomes duplicados)**: bloqueado ignorando caixa, por **validação no
  repositório** — não por índice único, que exigiria uma segunda migração.
- **Pessoas #11 (paleta)**: o índigo **fica**. Só saem os comentários obsoletos do
  `Theme.kt` e do `proguard-rules.pro`.
- **Fotos**: remover a foto eleita **elege a próxima** da galeria; teto de **10 fotos** por
  contato; galeria é faixa de miniaturas com toque para eleger.
- **MyApps**: o item indisponível passa a mostrar nome e ícone do cache, mas **mantém** o
  aviso "não está mais instalado" e o clique desabilitado — senão fica visualmente
  idêntico a um app instalado que não abre.
- **MyApps**: escopo do cache é só os pacotes que estão em alguma lista, não todos os apps
  já vistos no picker (isso seria outra feature).

### Achados dos scouts que valem para a execução

- **A armadilha do MyApps**: `AppsInstalados.rotulo()` é hoje, ao mesmo tempo, "qual nome
  mostrar" **e** o teste de "está instalado?" (`HomeScreen.kt:316`, `ReorderScreen.kt:118`
  derivam `instalado` do `rotulo != null`). Se o cache for consultado dentro do próprio
  `rotulo()`, as duas telas **quebram semanticamente sem erro de compilação**: param de
  mostrar o aviso e reabilitam o clique num app que não abre. As duas responsabilidades
  têm que ser separadas.
- **MyApps**: `MyAppsDao.observarApps` faz `SELECT *`. É o argumento decisivo contra pôr o
  BLOB do ícone em `app_na_lista` — toda emissão do Flow da home carregaria todos os
  ícones para a heap. (Decidido por arquivo, então não se aplica, mas fica registrado.)
- **MyApps**: `RoomMyAppsRepository` **não guarda `Context`** hoje; precisa passar a
  guardar para conseguir perguntar nome/ícone ao `PackageManager` na hora de gravar o
  cache. A leitura do `PackageManager` tem que acontecer **antes** de abrir a transação de
  `definirApps`.
- **MyApps**: o `IconeApp` pede o ícone em três tamanhos (44/40/36dp) e a chave do cache em
  memória inclui o `px`. O cache **persistido** deve guardar **um** tamanho canônico
  (192px) e reescalar na leitura, senão o armazenamento triplica.
- **Pessoas**: o `Scaffold` do M3 **não** aplica inset no slot `bottomBar` — quem aplica
  são `NavigationBar`/`BottomAppBar` internamente. Como ali é um `Surface { Row }` cru e o
  `MainActivity` chama `enableEdgeToEdge()`, o carrossel de 48dp fica sob a barra de 3
  botões de 48dp. Cobertura praticamente total.
- **Pessoas**: **não existe uma única ocorrência** de `imePadding`, `navigationBarsPadding`,
  `windowInsetsPadding`, `WindowInsets` **nem de `rememberSaveable`** no app inteiro.
  Evidência negativa de dois greys que varreram todo `--include=*.kt`.
- **Pessoas**: o KDoc de `Contatos.kt:26-28` **mente** — diz que a foto quebrada "cai no
  avatar com as iniciais", mas o `AsyncImage` não tem `error`/`fallback`, então sobra um
  círculo colorido vazio. O texto do backlog #1 herdou essa suposição errada.
- **Pessoas**: `PickMultipleVisualMedia(maxItems)` existe e está disponível
  (`activityCompose = 1.9.2`). Mesmo comportamento de permissão do single — acesso só de
  sessão, `takePersistableUriPermission` lança `SecurityException`. Por isso a cópia para
  armazenamento próprio é o que torna a galeria durável.

### Estado da árvore no início do lote

`apps-hub-lists` e `people` **não são repositórios git** — não houve `git init`, não há
repositório remoto, não são submódulos; aparecem como diretórios untracked no
superprojeto. O superprojeto acumula **quatro rodadas** sem commit (site-blocker 2.4,
notes 1.7, watch-up 1.17, e os dois apps novos até a 1.2), com os APKs já publicados nos
hubs. Hook `warn-before-changes` já reconhecido nesta sessão.
