# Lote menus-e-backup — 2026-08-08

**Estado:** concluído

> Os 6 itens entregues e **as duas versões validadas em aparelho**. MyApps 1.4 e Pessoas
> 1.4 publicados nos dois hubs. Continua **sem git**: nenhum dos dois apps foi
> inicializado como repositório.

**Origem:**

> Ajustes no MyApps:
> - Nas telas de reordenação tem que ter atalho para ordenar em ordem alfabetica ascedente e descendente
> - Quero que o backup fique no estilo do WatchUp, ou seja, escolher uma pasta no Drive e ter os botões fazer backup, restaurar backup e apagar backup, além de mostrar a data e hora do último backup
>
> Ajustes no app Pessoas:
> - Quero que as opções do contato (3 pontinhos) tenha ícones e também tenha as opções mover para outra lista e remover da lista, como no app MyApps. Também tem que ter opção apagar contato com confirmação
> - Também quero que o adicionar contato seja um + lá no topo como no MyApps
> - Aquele botão pra mostrar arquivados tem que estar dentro de um menu 3 pontinhos que vai entrar no lugar dele e vai ter outras opções (parecido com o MyApps também). Assim como no MyApps esse menu 3 pontinhos tem que ter as mesmas opções de nova lista, renomear, apagar, reordenar listas, reordenar contatos da lista, backup e ajustes, sendo que o backup deve seguir o estilo do WatchUp, ou seja, escolher uma pasta no Drive e ter os botões fazer backup, restaurar backup e apagar backup, além de mostrar a data e hora do último backup
> - Com o ajuste acima o 3 pontinhos ao lado da lista no carrossel perde o sentido, pode tirar. Além disso, o "+" do carrossel deve ficar fixo, como fica no MyApps

## Itens

| # | App | Tipo | Item | Natureza | Backlog | Estado |
|---|-----|------|------|----------|---------|--------|
| 1 | apps-hub-lists | ✨ | Ordenação alfabética ↑↓ nas duas telas de reordenação | edicao | #5 | ✅ entregue v1.4 |
| 2 | apps-hub-lists | ✨ | Backup estilo WatchUp + automático diário | edicao | #2 | ✅ entregue v1.4 |
| 3 | people | ✨ | Menu ⋮ do contato com ícones + apagar com confirmação | edicao | #7 | ✅ entregue v1.4 |
| 4 | people | ✨ | `:feature:reorder` — reordenar listas | **feature-nova** | #3 | ✅ entregue v1.4 |
| 5 | people | ✨ | `:feature:settings` — backup estilo WatchUp + automático | **feature-nova** | #2 | ✅ entregue v1.4 |
| 6 | people | 🔧 | Chrome da Home: `+` no topo, ⋮ com **seis** entradas, carrossel | edicao | #8 | ✅ entregue v1.4 |

**Dois pedidos caíram em itens já filados**, então executam aqueles números em vez de
duplicar: o backup do MyApps é o `#2` do backlog dele, e no Pessoas o backup é o `#2` e a
reordenação é o `#3`. Numeração de backlog é permanente; ela não acompanha a numeração do
lote.

## Ordem

- **apps-hub-lists**: #5 antes de #2 — independentes, o mais simples primeiro.
- **people**: #7 → #3 → #2 → **#8 obrigatoriamente por último**. O menu ⋮ do #8 aponta
  para as telas que o #3 (`:feature:reorder`) e o #2 (`:feature:settings`) criam; antes
  delas existirem, o #8 **não compila**. Essa é dependência real, não preferência.
- Entre os dois apps não há ordem: correm em paralelo, builds serializados por `flock`.

## Releases

| App | Versão | Estado |
|-----|--------|--------|
| apps-hub-lists (MyApps) | 1.4 | ✅ publicado nos hubs · gate **aprovado** · ⚠️ sem git |
| people (Pessoas) | 1.4 | ✅ publicado nos hubs · gate **aprovado** · ⚠️ sem git |

## Pendências e correções ao fechar

1. **Erro meu de contagem, pego pelo implementador.** Eu escrevi "**oito** entradas" no
   menu ⋮ do Pessoas, no backlog e no prompt. A enumeração dá **seis**: os sete itens
   pedidos, menos "reordenar contatos da lista" (que o usuário tirou), com "backup e
   ajustes" contando como um, dá cinco — mais "Arquivados", seis. O implementador
   implementou as seis e **sinalizou em vez de inventar duas**. Backlog corrigido.
2. **Tabela de histórico quebrada por linha em branco**, nos dois backlogs — defeito meu,
   de quando acrescentei as linhas da 1.3. Corrigido nos dois.
3. **Nenhum dos dois apps é repositório git.** Os quatro implementadores desta rodada
   esbarraram nisso de forma independente e reportaram: sem `.git`, não dá para conferir
   diff por app (todos conferiram por mtime) e o passo de commit + tag do
   `/android-release` não tem onde rodar. O superprojeto acumula **seis rodadas** sem
   commit.
4. **`apps-hub-lists/app/proguard-rules.pro` está vazio e com comentário obsoleto** (diz
   "sem minificação por padrão" enquanto o release tem `isMinifyEnabled = true`). É o
   mesmo defeito que a revisão da v1.2 corrigiu no Pessoas e que ninguém corrigiu no
   MyApps. Relevante porque o `BackupWorker` é instanciado por reflexão pelo WorkManager —
   passou no gate, então as regras `consumer` da lib cobrem, mas o comentário segue
   mentindo.
5. **Backlogs após esta rodada**: MyApps tem 2 pendentes (#1 arrasto, #3 teste do
   serializador); Pessoas tem 1 (#4 tags em minúsculas).

## Desvios dos implementadores que valem registro

- **`salvarContato` não serve para restaurar backup** (achado no item #2 do Pessoas). O
  scout o indicou como ponto de entrada do import, mas ele força `criadoEm = agora` —
  restaurar zeraria a data de criação da agenda inteira, calado. O implementador escreveu
  a inserção direto na transação, preservando `criadoEm`/`atualizadoEm`/`arquivado`, e
  extraiu `regravarTags()` para reusar só a parte que queria.
- **Foto com nome igual não é reenviada** no backup do Pessoas: os nomes são UUIDs de
  arquivos que `ArmazenamentoDeFotos` nunca reescreve, então nome igual é conteúdo igual.
  É o que torna o automático diário viável — o segundo backup sobe só as fotos novas em
  vez de reenviar a agenda inteira ao Drive todo dia.
- **Fotos primeiro, JSON depois** (mesmo item): o JSON é o índice, e índice citando
  arquivo ainda não enviado é pior que índice velho. Se o processo morrer no meio, o
  backup anterior continua consistente.
- **`ContatoNaLista.ordem` vai no backup** mesmo sem uso na UI — o `@Relation` da junção
  devolve `Lista`, não a linha do cross-ref, então sem uma query extra a coluna se
  perderia no round-trip.
- **Reconciliação do agendamento na abertura do app**, nos dois apps: o WatchUp só agenda
  pelo `Switch`, o que deixa o toggle ligado sem nada agendado se o banco do WorkManager
  for limpo por fora ("limpar dados", restauração do sistema, força-parada agressiva). Uma
  linha no `MainActivity` cura os dois lados.
- **`Lista : Ordenavel`** no Pessoas (item #3): o item dizia "não toque em `@Entity`", mas
  acrescentar supertipo e `override` é schema-neutro — mesmas colunas, mesmo PK, mesmo
  índice. O implementador explicou a leitura e ofereceu a alternativa sem tocar em
  `Model.kt`. Aceito.

Aprovado **com gate de aparelho**. Não há migração de Room desta vez, mas o backup mexe
em SAF sobre pasta do Drive, cujo `DocumentsProvider` é historicamente irregular com
`createFile`/`findFile` — é o que mais merece teste real.

## Fora do lote

- **Reordenar contatos dentro da lista.** O usuário tirou do escopo depois que o scout
  mostrou que não teria efeito visual nenhum: a Home ordena sempre por nome
  (`Contatos.kt:84`, `filtrarContatos` fecha em `ordenarPorNome`) e **ninguém lê**
  `ContatoNaLista.ordem`. Fazer a tela sem mexer na listagem seria gravar no banco e não
  mudar nada na tela. O campo continua existindo e populado, sem uso.
- **MyApps #1** (arrasto para reordenar) e **#3** (teste do serializador de backup) —
  seguem pendentes.
- **Pessoas #4** (tags exibidas em minúsculas) — segue pendente.

## Notas

### Decisões de produto tomadas na aprovação (2026-08-08)

- **Fotos no backup do Pessoas: arquivos irmãos na pasta** (`backup.json` + subpasta
  `fotos/`). O scout provou que base64 inline é inviável: `ArmazenamentoDeFotos.copiar` é
  um `copyTo` cru, sem redimensionar, então os arquivos são as fotos originais da câmera.
  10 fotos × 4 MB × 1,33 do base64 ≈ 53 MB de JSON **para um contato**, e o `org.json`
  monta tudo em `String` na heap antes do `toByteArray()` — ~150 MB de pico. OOM garantido.
- **Reordenar: só listas.** Ver "Fora do lote".
- **Arquivados vira a oitava entrada do ⋮.** Do jeito literal do pedido (⋮ no lugar do
  botão, com sete opções e nenhuma delas sendo arquivados), a `ArchiveScreen` ficaria
  **inalcançável**.
- **Ordenar alfabeticamente grava direto, com confirmação.** Coerente com a tela, onde todo
  toque já persiste — mas com diálogo, porque destrói de uma vez uma ordem manual e não há
  desfazer.
- **Backup automático diário entra nos dois apps.** Fecha o `#2` do MyApps por inteiro.
- **Restaurar: os dois apps só substituem.** ⚠️ **Isto é uma regressão deliberada no
  MyApps**, que hoje oferece mesclar × substituir (`SettingsScreen.kt:196`, decisão
  documentada da v1.1). Registrado a pedido do usuário, que preferiu simetria com o
  WatchUp.
- **"Backup e ajustes" é uma entrada só** no menu do Pessoas, como no MyApps — um destino,
  uma tela com seções.

### Decisões técnicas que tomei sem consultar

- **MyApps**: apps sem nome real (desinstalados e nunca cacheados) ordenam pelo **package
  cru**, que é o que a tela mostra — não vão para o fim. `Collator` pt-BR com a mesma força
  já usada em `AppsInstalados.kt:47`. **Dois itens de menu** (A→Z e Z→A) em vez de um botão
  que alterna, porque um botão alternante não deixa óbvio qual direção sai do próximo toque.
- **Pessoas**: "Remover da lista" fica **visível e desabilitado** quando o carrossel está em
  "Todos", em vez de sumir — é o padrão do MyApps (`enabled = false` com texto alternativo).
- **Pessoas**: o título da `TopAppBar` passa a ser o nome da lista selecionada, caindo em
  "Pessoas" no chip "Todos" — espelha o `listaAtual?.nome ?: "MyApps"` do MyApps.
- **Pessoas**: o chip "Todos" **rola junto** com os demais; só o `+` é fixo, que é o que o
  pedido diz.
- **Pessoas**: ícones também no menu ⋮ do `DetailScreen`, senão fica inconsistente dentro
  do próprio app.
- **Pessoas**: apagar contato pela Home dá snackbar **sem desfazer** — nenhum apagar do app
  tem desfazer hoje (nem lista, nem contato pelo Detail/Archive).
- **Pessoas**: "apagar backup" varre também a subpasta `fotos/`, senão ela vira depósito de
  órfãos no Drive.

### Achados dos scouts que valem para a execução

- **A técnica do `+` fixo** (MyApps `HomeScreen.kt:417-441`): `Row` externa **não rolável**;
  a parte rolável é uma `LazyRow` com `Modifier.weight(1f)`; o `+` é **irmão** da `LazyRow`
  dentro da `Row`. O padding horizontal migra do `Row` para o `contentPadding` da `LazyRow`.
  **Atenção ao inset**: o `windowInsetsPadding(safeDrawing.only(Bottom))` do Pessoas
  (`HomeScreen.kt:331`) tem que ficar na **`Row` externa**, não migrar para a `LazyRow` —
  ele vale para a barra inteira. Brinde a copiar: `rememberLazyListState()` +
  `animateScrollToItem` para o chip selecionado não ficar fora da viewport.
- **O FAB some e os 72dp de `contentPadding` viram espaço morto.** O slot
  `floatingActionButton` do `Scaffold` M3 **não** contribui para o `innerPadding`; os 72dp
  (`HomeScreen.kt:202`) são compensação manual e explícita pelo FAB, ajustada na v1.3. É o
  bug #8 daquela revisão reaparecendo pelo outro lado.
- **`Ordenacao.alfabetica` cabe como função pura** no `:core:data` do MyApps: `Collator` é
  JDK, roda em teste JVM sem Android, e `renumerar` já garante contiguidade e devolve só o
  que mudou (se já estiver alfabético, não escreve nada).
- **Ordenar apps tem que ser resolvido no repositório, não na UI.** A UI conhece os nomes
  via `rememberExibicaoDoApp`, mas eles chegam **assíncronos por linha** (`produceState`) —
  ordenar a partir de estado parcialmente resolvido dá ordem instável. O repo já tem
  `Context` (desde o cache de ícone da v1.3) e resolve tudo em IO.
- **O Pessoas não tem equivalente de `Ordenacao.kt`** — precisa nascer, copiado do MyApps,
  com uma adaptação: `Ordenavel` exige `id: Long` e `ContatoNaLista` tem **PK composta**
  sem `id`. Para reordenar **listas** (que é o escopo aprovado) isso não é problema:
  `Lista` tem `id`.
- **`PessoasDao` só expõe `Flow` para ler contatos** — não há query `suspend` de snapshot.
  Exportar backup exige DAO novo (`@Transaction @Query ... suspend fun todosOsContatos()`),
  e `contato_na_lista` hoje **não tem leitura nenhuma**, só insert/delete.
- **Ordem não-contígua herdada no Pessoas**: `criarLista` usa `MAX(ordem)+1` e nem
  `apagarLista` nem `removerDaLista` renumeram, então o banco pode ter buracos.
  `Ordenacao.mover` conserta de graça no primeiro toque (`renumerar` é idempotente).
- **`work` e `documentfile` já estão nos dois catálogos** (`work = 2.9.1`,
  `documentfile = 1.0.1`), com os mesmos aliases do WatchUp. **Nenhum catálogo precisa ser
  editado** — o que é bom, porque implementador não pode.
- **`apagarBackup` do WatchUp apaga o arquivo, não esquece a pasta.** A pasta continua
  configurada; não existe ação "esquecer pasta", só "Trocar pasta".
- **O nome do arquivo é fixo** (`backup.json`, escrito com `"wt"` que trunca), não versiona
  por data. Isso responde a pendência que o `#2` do MyApps deixava em aberto.

### Estado da árvore no início do lote

`apps-hub-lists` e `people` **continuam sem repositório git** — sem `git init`, sem
remoto, sem submódulo; aparecem como diretórios untracked no superprojeto. O superprojeto
acumula **cinco rodadas** sem commit. O usuário foi consultado na aprovação e escolheu
seguir assim.
