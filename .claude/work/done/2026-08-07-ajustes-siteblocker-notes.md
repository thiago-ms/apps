# Lote ajustes-siteblocker-notes — 2026-08-07

**Estado:** concluído

> ⚠️ **Fechado com pendência deliberada: NENHUM commit foi feito.** Os dois apps estão
> com bump, código e backlog inteiramente não commitados, e nenhum dos dois releases
> passou por gate de aparelho. Ver "Pendências ao fechar" no fim do arquivo.

**Origem:**

> site-blocker:
> - incluir opção pra informar quantidade de dias que o app deve ficar travado sem permitir que o usuário desative o bloqueio
> - verificar se dá pra iniciar o app junto com o celular
>
> notes:
> - tirar a opção de copiar o clipboard que usa recurso de acessibilidade do Android, acho que com o botão flutuante já fica bom e sem precisar disso
> - tem um bug que ao ativar o botão flutuante no primeiro clique ao botão ele desativa automaticamente, verificar porque isso acontece

## Itens

| # | App | Tipo | Item | Natureza | Backlog | Estado |
|---|-----|------|------|----------|---------|--------|
| 1 | site-blocker | ✨ | Iniciar junto com o celular via Always-on VPN (atalho + explicação) | edicao | #1 | ✅ entregue v2.4 |
| 2 | site-blocker | ✨ | Travar o bloqueio por N dias (sem desativar/remover domínio/importar) | edicao | #2 | ✅ entregue v2.4 |
| 3 | notes | 🐛 | Botão flutuante se desativa sozinho no primeiro toque | edicao | #5 | ✅ entregue v1.7 |
| 4 | notes | 🔧 | Remover o modo de captura por acessibilidade | edicao | #6 | ✅ entregue v1.7 |

## Ordem

- **site-blocker**: #1 antes de #2 — sem dependência real, o mais simples primeiro (o #1
  virou baixa complexidade depois que o scout descobriu que o app já é elegível a
  Always-on VPN). Entrega progresso enquanto o #2, que mexe em três camadas, roda.
- **notes**: #3 antes de #4 — sem dependência real e sem colisão de arquivos (#3 é só
  `CaptureGuardService.kt`; #4 é manifest + `SettingsScreen` + `CapturePrefs`). Bug de
  complexidade baixa primeiro.
- Entre os dois apps não há ordem: correm em paralelo, builds serializados por `flock`.

## Releases

| App | Versão | Estado |
|-----|--------|--------|
| site-blocker | 2.4 | ✅ publicado nos hubs · ⚠️ **sem git** e ⚠️ **gate pulado** |
| notes | 1.7 | ✅ publicado nos hubs · ⚠️ **sem git** e ⚠️ **gate pulado** |

## Pendências ao fechar

Nada disto é erro: são escolhas suas registradas para não se perderem.

1. **Nenhum commit, nenhuma tag, nenhum push** — nos 3 repositórios (os 2 submódulos e o
   superprojeto). O que está solto:
   - `site-blocker`: bump 2.4/code 7, 4 arquivos Kotlin, `strings.xml`, `Trava.kt` e
     `TravaTest.kt` untracked, `specs/` untracked.
   - `notes`: bump 1.7/code 8, 8 arquivos modificados, 3 deletados, `specs/` modificado.
   - superprojeto: `.dist/` com 2 APKs novos + `index.html` regenerado, e os ponteiros dos
     2 submódulos ainda nos commits antigos.
   - Tags que **não** existem: `2.4` no site-blocker, `1.7` no notes.
2. **Nenhum dos dois APKs de release foi aberto em aparelho.** R8 ligado nos dois. A lista
   do que precisa ser testado está na seção "Verificações de aparelho" acima.
3. **Hubs com 2 APKs por app** (2.3+2.4 e 1.6+1.7), a pedido. Se quiser voltar ao
   invariante de 1 por app, apagar os superados e regenerar o `index.html`.
4. **Backlog #3 do site-blocker** (endurecer o lock) fica `pendente`, fora deste lote.

### Ressalvas do release do site-blocker 2.4 (decisões do usuário, não drift)

- **Gate de aparelho pulado** a pedido. O APK 2.4 nunca foi aberto. R8 está ligado, então
  um crash de reflexão em runtime continua possível — e a verificação que fecharia o item
  #1 (reiniciar o celular com Always-on VPN marcado) não aconteceu.
- **Nenhuma operação de git**: sem commit, sem tag `2.4`, sem push, nos dois repositórios.
  O bump `2.4`/`versionCode 7` está no `app/build.gradle.kts` **não commitado**, junto com
  todo o código dos itens #1 e #2 e o `specs/` untracked.
- **O APK 2.3 foi mantido** nos dois hubs a pedido, então cada hub tem 2 APKs do
  site-blocker em vez de 1. A asserção "um APK por app" falha por escolha, não por erro.
- Consequência a resolver: o `.dist/` da raiz agora tem `siteblocker-2.4-release.apk` e um
  `index.html` regenerado. Se o release do `notes` fizer commit do superprojeto, ele
  **arrasta junto** o APK e o index do site-blocker, com o ponteiro do submódulo
  site-blocker ainda apontando para a 2.3.

## Fora do lote

- **Endurecer o lock do site-blocker contra fugas por fora do app** — re-subir após
  reboot, retomar após revogar a VPN nas Configurações, desabilitar `allowBackup`
  (hoje `true`, permite zerar o lock por restore). O usuário escolheu o nível
  "compromisso honesto" na aprovação do plano; fica como item pendente no backlog do
  site-blocker, sem execução neste lote.

## Notas

### Decisões de produto tomadas na aprovação (2026-08-07)

- **Escopo do lock (#2)**: congela desativar a proteção **+** remover domínios da lista
  **+** importar backup. Adicionar domínio novo continua liberado.
- **Dureza do lock (#2)**: "compromisso honesto" — barra o caminho normal (botão de UI
  **e** `SiteBlockerVpnService.onStartCommand` recusando `ACTION_STOP`). Não cobre
  force-stop, desinstalação nem revogação de VPN.
- **Boot (#1)**: Always-on VPN do sistema, **não** BootReceiver próprio. Vira UX — botão
  que abre `Settings.ACTION_VPN_SETTINGS` + texto explicando. Sem receiver, sem
  `RECEIVE_BOOT_COMPLETED`.
- **UI do notes (#4)**: o seletor de modo some por completo; fica só o switch do botão
  flutuante.

### Decisões técnicas que tomei sem consultar

- #2: lock pode ser **estendido**, nunca reduzido nem cancelado. Dias em campo livre com
  teto. Ativar o lock liga a proteção. Fonte de tempo é `System.currentTimeMillis()`
  (relógio de parede) — recuar o relógio do aparelho encurta o lock, aceitável no nível
  escolhido.
- #3: correção no **guard** (`CaptureGuardService.onTaskRemoved` filtrando a task pelo
  `rootIntent?.component`), não na `ClipboardReadActivity`. Preserva o
  `finishAndRemoveTask()` e o retorno ao app anterior documentado no KDoc.
- #4: o enum `CaptureMode` e o par `getModo`/`setModo` saem de vez do `:core:data`.

### Achados dos scouts que valem para os próximos itens

- **site-blocker** tem uma única `preferencesDataStore(name = "site_blocker_prefs")`
  declarada como delegate top-level em `BlockedSitesRepository.kt:12`. Um segundo
  delegate com o mesmo `name` estoura em runtime — o `lockUntil` e qualquer chave nova
  entram nesta classe.
- **site-blocker**: `VpnState` é só memória (`MutableStateFlow(false)`). Se o processo
  morrer e a UI reabrir com o serviço vivo, o toggle mostra `false`. Bug latente
  adjacente, **não** introduzido por este lote e **não** corrigido nele.
- **notes**: `ModoOpcao` (`SettingsScreen.kt:510`) continua em uso pela `SecaoLinks`
  (linhas 495 e 501) — **não apagar** ao remover o seletor de modo.
- **notes**: o default de `getModo` é `ACESSIBILIDADE`, então quem nunca escolheu modo
  está hoje nesse modo. Depois do #4, ligar a captura passa a pedir permissão de
  sobreposição para todo mundo — quebra visível na primeira execução da 1.7, e é o
  efeito pretendido.

### Desvios reportados pelos implementadores (todos os 4 itens verdes, build 1ª tentativa)

- **#1 (site-blocker)**: nenhum desvio. Strings novas via `stringResource` contrariando o
  vizinho (o resto do `HomeScreen.kt` tem texto inline) — seguiu o item, não o padrão
  local; strings antigas não foram migradas, seria escopo alheio. `openVpnSettings` com
  `try/catch ActivityNotFoundException` porque `Settings.ACTION_VPN_SETTINGS` é opcional
  na prática e alguns fabricantes removem a Activity.
- **#3 (notes)**: implementou o filtro como **denylist** (`classe != ClipboardReadActivity`)
  e não allowlist (`== MainActivity`), pelo caso de `rootIntent`/`component` nulo — com
  allowlist um `rootIntent` nulo deixaria a captura ligada, quebrando em silêncio o
  contrato de `SettingsScreen.kt:176-177`. Denylist faz o desconhecido cair no lado
  seguro. Aceito.
- **#2 (site-blocker)**: criou `data/Trava.kt` — arquivo **não previsto pelo scout**, que
  punha a aritmética no ViewModel/Screen. Como o serviço também consome a regra e não vê
  a UI, a extração se justifica; de quebra ficou testável sem `Context` (8 testes novos em
  `TravaTest.kt`, 23 no total verdes). Também mudou assinaturas além do previsto:
  `remove` → `Boolean` e `addAll` → `Int?`, e `importBackup` → sealed
  `ResultadoImportacao`, porque `Int?` não distinguia "arquivo inválido" de "recusado pela
  trava". Gate duplo (UI desabilita **e** repo recusa) fecha a corrida de trava iniciada
  com a tela já aberta. Teto de 365 dias **por operação** — estender várias vezes soma
  além disso, coerente com "só cresce".
- **#4 (notes)**: removeu também `strings.xml/acessibilidade_desc` e o diretório
  `res/xml/` (órfãos após apagar o config), fora da lista do scout. Ajustou KDoc obsoleto
  em `FloatingButtonService`. Renomeou a seção para **"Captura por toque"** e o composable
  privado `SecaoAutoCaptura` → `SecaoCaptura`. **`AutoCaptureState` não foi renomeado** —
  é contrato do `:core:data` e o item não pedia; fica dissonância leve entre o nome do
  estado e o rótulo da UI. Lacunas do scout checadas e vazias: `proguard-rules.pro` só
  tem comentários, e **o notes não tem `src/test/` em nenhum módulo**.

### Verificações de aparelho que o smoke-test do release PRECISA cobrir

Nenhum implementador pôde fechar estas — são afirmações sobre runtime:

- **site-blocker #1**: marcar "VPN sempre ativa" nas configurações de VPN do Android,
  **reiniciar o celular** e confirmar que a notificação de foreground sobe sozinha. Se não
  subir, o item #1 não está entregue de verdade.
- **site-blocker #2**: travar por 1 dia e confirmar que "Desativar proteção" fica
  desabilitado, a lixeira dos domínios some e "Importar" no backup recusa. R8 no release +
  DataStore/reflexão é justamente o risco que só aparece em runtime.
- **notes #5**: tocar o botão flutuante **várias vezes seguidas** e conferir que o switch
  de Configurações não cai; depois fechar o app pelos recentes e conferir que o botão some
  e o switch desliga.
- **notes #6**: ligar a captura numa instalação que já existia — vai pedir permissão de
  sobreposição pela primeira vez (efeito pretendido, mas é a quebra visível da 1.7).

### Estado da árvore no início do lote

`server.sh` modificado em `site-blocker`, `notes` e na raiz, mais `.sample/server.sh`,
`CLAUDE.md`, e os untracked `.claude/agents/` + `.claude/skills/`. Trabalho pré-existente
do usuário, não tocado por este lote. Hook `warn-before-changes` reconhecido com
"Proceed for session" em 2026-08-07T03:25:57Z.
