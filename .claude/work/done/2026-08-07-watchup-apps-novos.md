# Lote watchup-apps-novos — 2026-08-07

**Estado:** concluído

> Os 8 itens ficaram **✅ verde** (código pronto, build passou) e **nenhum foi entregue** —
> a aprovação foi "seguir, sem release". Ver "Pendências ao fechar" no fim do arquivo.

**Origem:**

> Novo app - apps-hub-lists
> - permitir adicionar qualquer app instalado e separar em listas, a ideia é selecionar apps instalados e criar listas com nome que o usuário preferir, então uma navegação no estilo carrossel de tags com os nome das listas e quando um nome for clicado vai praquela lista de apps.
> - permitir fazer backup
> - pode usar o o apps-hub como referência pra não sair do zero em relação a layout
> - importante o carrossel ser no bottom e a lista também ser do bottom ao top então mesmo quando tiver scroll a rolagem vai ser pra cima
> - tem que ser fácil remover ou mover apps de listas e também de criar novas listas
> - tem que poder reordenar as listas e reordenar os apps
>
> WatchUp:
> - ajuste no backup pra tmdb key ficar no backup
> - bug: uma série após ser lançada e ter a quantidade de episódios atualizada pela estimativa não habilitou o atualizar progresso e nem a quantidade de temporadas. Nesses casos também o dia da semana que novos episódios serão lançados deve ser assumido como sendo o mesmo dia da semana da data de lançamento
> - verificar se tmdb retorna data de estreia, se a série está completa ou não, quantidade de temporadas e episódios dispoíveis, dia e hora de lançamento e onde que pode ser assistada, ou seja, tentar preencher o máximo possível as informações do form. Não precisa trazer logo na busca, ela pode ficar como está, mas, ao ir para o form talvez tenha que ter um loading ou algo assim e ir buscar na API. Quero que tenha uma configuração para habilitar e desabilitar preenchimento automático usando API, quando estiver desabilitado o form funciona como é hoje
> - ainda falando de form, percebi que estamos misturando infos que são da série com infos que são pessoais minhas, então quero que tente jogar essas infos pessoais pra uma última etapa do form. São elas: contexto de consumo, onde vai assistir
> - na home separar episódicas de não episódicas, hoje o em cartaz inclui séries, o que não é legal, deve ter um bloco lançando para elas, o mesmo para o próximas datas, melhor que os blocos de episódicas venham depois dos de não episódicas
>
> Novo app - people
> - app lista de contatos
> - nome, número, tags, observação, links, fotos (escolher uma foto de contato)
> - ações de abrir conversa no wpp e ir para detalhe do contato
> - opção de "arquivar" e ir pra uma lista de arquivados
> - poder criar listas e que seja muito prático de remover e mover contatos entre listas

## Itens

| # | App | Tipo | Item | Natureza | Backlog | Estado |
|---|-----|------|------|----------|---------|--------|
| 1 | watch-up | 🔧 | Home: 4 blocos, não-episódicas antes das episódicas | edicao | #12 | ✅ verde |
| 2 | watch-up | 🔧 | Chave TMDB no backup (texto puro, `VERSION` 1→2) | edicao | #13 | ✅ verde |
| 3 | watch-up | 🐛 | Promoção da série lançada + dia da semana + correção retroativa | edicao | #14 | ✅ verde |
| 4 | watch-up | 🔧 | Form: etapa única "Sobre você" (6→5 etapas) | edicao | #15 | ✅ verde |
| 5 | watch-up | 🔧 | Módulo `:core:tmdb` | core-novo | #16 | ✅ verde |
| 6 | watch-up | ✨ | Autopreenchimento do form via TMDB + toggle | edicao | #17 | ✅ verde |
| 7 | apps-hub-lists | ✨ | Esqueleto do app novo | app-novo | — | ✅ verde |
| 8 | people | ✨ | Esqueleto do app novo | app-novo | — | ✅ verde |

## Ordem

- **watch-up** (estrita, um item por vez):
  1. **#12** primeiro por ser `baixa` e totalmente isolado (`:feature:home` só) — progresso rápido.
  2. **#13** e **#14** independentes entre si.
  3. **#15 antes de #17** — o #15 reestrutura o enum de etapas do form; o autopreenchimento
     precisa aterrissar na estrutura final, não na antiga.
  4. **#16 antes de #17** — dependência real: o autopreenchimento não compila sem o módulo.
  5. **#17** por último: é o único `alta` e depende de dois itens anteriores.
- **apps-hub-lists** e **people** por último e no loop principal (`/android-new-app`
  entrevista). São os itens mais longos — cópia + 3 passes + identidade + build de imagem
  Docker — e não bloqueiam ninguém.

## Releases

| App | Versão | Estado |
|-----|--------|--------|
| watch-up | — | ❌ **sem release nesta rodada** (escolha do usuário na aprovação) |
| apps-hub-lists | — | ❌ sem release — esqueleto não tem o que entregar |
| people | — | ❌ sem release — esqueleto não tem o que entregar |

O WatchUp para em **código verde**, sem bump (fica em 1.16/`versionCode` 17), sem
`make dist-all`, sem tag, sem commit. `/android-release watch-up` fica pendente.

## Fora do lote

- **As funcionalidades dos dois apps novos.** `/android-new-app` gera só o esqueleto
  (`:app` + `:core:ui` + `:feature:home`, paleta, ícone, README). Nada do que foi descrito
  é construído nesta rodada: no `apps-hub-lists`, a seleção de apps instalados, o carrossel
  de tags no bottom, a lista bottom-to-top, o backup e a reordenação de listas/apps; no
  `people`, o CRUD de contatos, tags, fotos, links, abrir WhatsApp, arquivar e listas com
  mover/remover. Tudo isso é lote próprio depois, e foi explicitado antes da aprovação.
- **Persistir `tmdbId` + migração de Room v8→v9** — descartado com o escopo "só cadastro
  novo vindo da busca". Sem isso não existe "atualizar do TMDB" em mídia já cadastrada.
- **`horarioLancamento` automático** — impossível: o TMDB não expõe horário de exibição em
  nenhum endpoint. O campo continua manual para sempre.

## Notas

### Correção de ordem feita durante a execução (depois do #12)

**#16 (`:core:tmdb`) passou para antes do #13 (chave no backup).** Os dois mexem em onde a
chave do TMDB mora: o scout do #13 propunha extrair a pref para `:core:data`, e o #16 cria
o `:core:tmdb`. Na ordem original a extração seria feita duas vezes, a segunda desfazendo
a primeira. Ordem efetiva: **#12 → #16 → #13 → #14 → #15 → #17**. As restrições originais
seguem respeitadas (#15 antes de #17, #16 antes de #17).

### Desvio aceito no #12

O implementador cumpriu "nenhum bloco tem placeholder", mas em vez de remover a mensagem
"Nenhum item neste filtro." de vez, passou a emiti-la **uma vez** abaixo dos chips de
filtro quando `radar.isEmpty()` — os quatro blocos vazios ao mesmo tempo. O argumento:
sem isso, um filtro sem resultado nenhum deixaria os chips sobre um vazio mudo, parecendo
tela quebrada. Aceito. Para remover de vez é apagar o `if (radar.isEmpty())` em
`HomeScreen.kt` (~linhas 172-179).

### Decisões de produto tomadas na aprovação (2026-08-07)

- **#16/#17**: o cliente TMDB vai para um **`:core:tmdb` novo**, não para `:core:data` —
  mantém `:core:data` só com Room e não obriga `buildConfig` lá.
- **#17**: escopo é **só cadastro novo vindo da busca**. Não persiste `tmdbId`, logo
  **nenhuma migração de Room** (schema fica na v8).
- **#14**: distinguir os **dois casos** do botão — série nova (0 temporadas → assume 1) e
  temporada nova (N → N+1, `temporadaAtual` = N+1, episódios **recomeçam do zero** em vez
  de somar aos da temporada anterior, que é o bug de brinde que o scout achou).
- **#14**: **não mexer em `statusUsuario`**. Se a série estiver em "Quero assistir" o card
  de progresso continua escondido por design — o usuário troca na mão.
- **#14**: **corrigir também os registros já gravados** em estado inconsistente
  (`LANCANDO` com 0 temporadas), não só o comportamento novo. Sem isso a série que o
  usuário reportou continuaria quebrada.
- **#15**: etapa única **"Sobre você"** juntando contexto de consumo + onde assistir; a
  etapa `ONDE_ASSISTIR` deixa de existir e o form vai de 6 para **5 etapas**.
- **#12**: ordem **agrupada por tipo** — `Em cartaz` (filmes) → `Próximas datas` (filmes) →
  `No ar` (séries) → `Próximos episódios` (séries).
- **#13**: chave TMDB em **texto puro** no `backup.json`. Chave de leitura e gratuita,
  impacto de vazamento baixo.

### Decisões técnicas que tomei sem consultar

- **#13**: restaurar só grava a chave se o backup trouxer valor **não-vazio** — restaurar
  um backup v1 (sem o campo) não apaga a chave atual. E salva **só a chave digitada pelo
  usuário**, nunca a efetiva, para não vazar a `BuildConfig.TMDB_API_KEY` embutida no build
  para dentro do JSON.
- **#14**: o `diaLancamento` derivado é **persistido**, não usado só como fallback de
  cálculo — é o que o pedido diz ("deve ser assumido"). E **não sobrescreve** um
  `diaLancamento` que já tenha valor.
- **#15**: a etapa `DETALHES`, ao perder o contexto de consumo, passa a se chamar
  **"Gênero"**.
- **#12**: "episódica" usa `TipoMidia.episodica`, que inclui `SERIE`, `ANIME`, `REALITY` e
  `PROGRAMA` — não só `SERIE`. Blocos vazios somem (nenhum dos quatro mantém placeholder).

### Achados dos scouts que valem para os próximos itens

- **`DetailScreen.kt:82-84`** tem três `const` (`ETAPA_DETALHES = 2`,
  `ETAPA_ONDE_ASSISTIR = 3`, `ETAPA_DATAS_STATUS = 4`) que espelham `PassoCadastro` **por
  número literal**. Mudar a ordem do enum **não quebra compilação** — a ficha só passa a
  abrir a etapa errada. O #15 tem que renumerar isso junto.
- **`TmdbResultado` (`TmdbClient.kt:11`) não carrega `id` nem `media_type`**, embora o
  `parsear()` já leia os dois e os descarte. Sem propagá-los por `Routes.registrationPrefill`
  até a tela de cadastro, o #17 é impossível. É o bloqueio nº 1 daquele item.
- **Nomes de streaming divergem**: TMDB devolve `Amazon Prime Video`, `Disney Plus`,
  `Paramount Plus`; `STREAMINGS_DISPONIVEIS` (`Enums.kt:73-91`) usa `Prime`, `Disney+`,
  `Paramount+`. Precisa de mapa de normalização, **não validado contra resposta real da
  API** — o scout não executou rede.
- **`horarioLancamento` não é obtenível** em nenhum endpoint do TMDB. O `diaLancamento`
  sai por inferência do dia da semana de `next_episode_to_air.air_date`, frágil para série
  com drop irregular ou lançamento em bloco.
- **`:feature:registration` não tem `src/test/`**; o único teste do projeto é
  `core/data/src/test/.../MidiaLogicTest.kt`. Nada cobre o form nem a composição da Home.
- **A chave TMDB mora em `:feature:search`** (`TmdbConfig.kt`, prefs `watchup_tmdb`), e
  `:feature:settings` **não depende** de `:feature:search` — é o bloqueio estrutural do #13.
  O #16, ao criar o `:core:tmdb`, muda esse terreno; por isso o #13 roda depois do #12 mas a
  ordem relativa a #16 importa se o implementador quiser reusar o módulo novo.

### Achados dos implementadores que merecem virar item futuro

- **`MidiaRepository.kt:81` tem `fallbackToDestructiveMigration()`** (achado no #14).
  Pré-existente e fora do item, mas contraria a regra do monorepo e é risco real: o
  WatchUp já fez migrações v4→v8, e essa chamada **apaga a biblioteca inteira** se um
  schema subir sem `Migration` escrita. Candidato a bug de prioridade alta.
- **O caminho "temporada nova" do botão de estimativa provavelmente é inalcançável hoje**
  (achado no #14). `novosEpisodiosEstimados` só aceita `VAI_LANCAR` com
  `dataBaseContagem == null`, e o repositório reancora essa data a cada edição de
  episódios; e mesmo com âncora nula o ramo desconta `episodiosDispTempAtual`, que na
  temporada nova guarda os episódios da temporada anterior. O `onAtualizar` ficou correto
  para os dois casos, mas destravar o caminho exige mexer numa função que `HomeScreen` e
  `LibraryScreen` também leem. Item próprio.
- **`BackupSerializer` não tem teste** (achado no #13) e não pode ter, do jeito atual: ele
  usa `org.json`, e o classpath de unit test do `:core:data` não o mocka (sem Robolectric,
  sem coordenada de `org.json` no catálogo). Cobrir exige mexer no `libs.versions.toml`,
  que implementador não pode fazer. Item próprio se o formato de backup crescer mais.
- **O mapa de normalização de streaming do #17 nunca foi validado contra resposta real da
  API** — nem os nomes de `status`. Está isolado num `mapOf` no topo do `TmdbMapa` e
  coberto por um teste de vocabulário (todo nome normalizado existe em
  `STREAMINGS_DISPONIVEIS`), mas o teste não prova que o TMDB devolve aqueles nomes.
  Primeiro smoke-test real deve conferir dia da semana e streamings.

### Desvios aceitos nos apps novos

- `versionName` nasceu **1.0**, não `0.1`. O `.sample/app/build.gradle.kts` traz `0.1`,
  mas a skill `/android-new-app` declara `1.0`/`versionCode 1` como default fixo. Segui a
  skill. **Divergência real entre o template e a skill** — vale reconciliar num
  `/android-audit`.
- A imagem Docker dos dois apps foi **tagueada a partir da `appshub-android-build`** em
  vez de construída do zero, o que é o atalho legítimo que a skill oferece: os
  `docker/Dockerfile` são byte-idênticos (md5 `63adaa56…` conferido em 5 apps). Evitou
  ~2 GB de download do SDK por app.
- `people` ficou com a paleta índigo do template, **igual ao `apps-hub` e ao
  `AppsLists`**. Foi escolha do usuário depois de eu apontar que os três ficariam
  parecidos na gaveta.
- `apps-hub-lists` recebeu `QUERY_ALL_PACKAGES`, e não `<queries>`. Motivo registrado no
  manifesto: o usuário escolhe **qualquer** app instalado, então não há como enumerar
  pacotes de antemão — o `apps-hub` consegue usar `<queries>` só porque tem lista fixa
  de 5.

## Pendências ao fechar

1. **Nenhum commit, nenhuma tag, nenhum push** — nem no `watch-up`, nem nos dois apps
   novos, nem no superprojeto. Os dois dirs novos estão **untracked**: não houve
   `git init`, não há repositório no GitHub, não são submódulos.
2. **WatchUp continua em 1.16/`versionCode` 17.** `/android-release watch-up` pendente —
   e quando rodar, precisa fechar os itens #12 a #17 do backlog, que seguem `pendente`.
3. **Nenhum dos 6 itens do WatchUp foi testado em aparelho.** O #17 em especial merece
   smoke-test: entrar pela busca numa série lançando e conferir dia da semana e
   streamings.
4. **Os dois apps novos não estão registrados no `apps-hub`** (nem no `HUB_APPS` nem no
   `<queries>`). É outro submódulo e gera release próprio — precisa de decisão à parte.
5. **Lote anterior (`2026-08-07-ajustes-siteblocker-notes`) também segue sem commit** —
   `site-blocker` 2.4 e `notes` 1.7, com APKs já publicados nos hubs.
6. **4 divergências de título entre tabela e seção** no `specs/backlog-ajustes.md` do
   WatchUp, nos itens 3, 4, 5 e 6. **Pré-existentes**, não introduzidas por este lote e
   deliberadamente não reparadas.

### Estado da árvore no início do lote

`watch-up` limpa fora do `server.sh`. Mas **`site-blocker` e `notes` continuam
inteiramente não commitados do lote anterior** (`2026-08-07-ajustes-siteblocker-notes`,
já arquivado em `done/`): bumps 2.4 e 1.7, todo o código, os backlogs e os `specs/`
untracked; mais `.dist/siteblocker-2.4-release.apk` e `.dist/notes-1.7-release.apk`
untracked na raiz e o `index.html` modificado. Hook `warn-before-changes` já reconhecido
nesta sessão.
