# Tasks: Negociar qualquer exemplar da coleção

**Input**: [`spec.md`](./spec.md) (aprovada em 2026-11-27, D-01/D-02 revisadas, D-03 reescrita), [`plan.md`](./plan.md), [`research.md`](./research.md), [`data-model.md`](./data-model.md), [`contracts/`](./contracts/) e Constituição v1.3.0

**Pré-requisitos**: nenhuma aprovação pendente. Nenhuma migração, nenhuma dependência nova, nenhuma rota nova (D-05, D-06, D-10).

**Testes**: obrigatórios (V.1, V.2, V.4). Cada tarefa de interface declara a verificação por classe de janela (V.10) quando aplicável.

**Estado nesta fase**: documento apenas. Nenhum arquivo de produção foi alterado.

**Revisão 2 (2026-11-27)**: fecha as lacunas C6–C11 do `/speckit-analyze` — estabilidade a recriação/duas janelas (T525), SC-506 pelas origens novas (T519), FR-524 (T521), FR-509 (T511), FR-512 (T513) e diff de dependências (T531). Tarefas de T525 em diante renumeradas.

## Formato: `[ID] [P?] [Story] Descrição — arquivo`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1–US5, conforme `spec.md`
- Cada tarefa tem **Aceite** (critério verificável) e **Teste** (como provar)
- **Janela**: C = compacta < 600 dp, M = média, E = expandida ≥ 840 dp, F = fonte 200%, P = paisagem compacta

> Numeração a partir de **T501** para não colidir com as specs 001–006 (a 006 usa T4xx).

---

## Phase 1: Setup (base compartilhada)

**Propósito**: baseline medido e recursos de texto prontos, antes de qualquer comportamento novo.

- [ ] T501 Confirmar o baseline antes de tocar em código: `git status` limpo, `./gradlew :app:assembleDebug` e suíte de unidade verdes, e massas de teste A–D descritas no [`quickstart.md`](./quickstart.md) (§Preparação) disponíveis em banco de emulador — sem massa, os cenários de Duplicados não são verificáveis. **Aceite**: build e testes passam **antes** de qualquer alteração; as quatro massas existem e foram conferidas na tela Duplicados. **Teste**: saída dos comandos arquivada em `docs/validacao.md` (seção desta feature). **Janela**: build apenas.

- [ ] T502 [P] Acrescentar as strings pt-BR em `app/src/main/res/values/strings.xml`, no bloco "Duplicados e negociação": `exemplar_negotiation_section` ("Negociação"), `exemplar_negotiation_blocked` ("Só exemplar no acervo pode ser disponibilizado ou negociado."), `exemplar_negotiation_offer_now` ("Disponível: %1$s"), `unique_exemplar_warning_title` ("Último exemplar da variante"), `unique_exemplar_warning_body` (consequência: o exemplar pode deixar de integrar a coleção ativa na conclusão; a disponibilidade não altera o acervo), `unique_exemplar_warning_continue` ("Continuar"), `unique_exemplar_warning_cancel` ("Cancelar"). Reutilizar `duplicates_offer_action` e `duplicates_negotiate_action`; **não** criar sinônimos para **Disponibilidade**, **Negociar** ou **Em negociação**. **Aceite**: sete strings novas e nenhum texto literal nas telas; as mesmas palavras nas três superfícies (FR-520, IV.1); nenhuma string nova fora deste bloco. **Teste**: `:app:lintDebug` sem `HardcodedText`; revisão de que `duplicates_reserved` continua sendo a única fonte de **Em negociação**. **Janela**: C, F.

- [ ] T503 [P] Corrigir o KDoc de `DuplicateAvailability` e `DuplicateOffer` em `core/domain/src/main/kotlin/br/com/diecastcollector/domain/model/DuplicateModels.kt`, que hoje dizem "exemplar repetido": a disponibilidade vale para **qualquer exemplar `OWNED`** da coleção (C-05). **Aceite**: comentário descreve o comportamento real; **nenhum** tipo, campo, assinatura ou `enum` muda; `git diff` do arquivo mostra apenas comentários. **Teste**: `./gradlew :core:domain:compileDebugKotlin` + `git diff --stat` do arquivo conferido. **Janela**: não se aplica.

**Checkpoint**: strings e KDoc prontos; nenhum comportamento alterado.

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Propósito**: a regra do aviso, as três leituras e os dois componentes reutilizáveis — tudo o que US1–US5 consomem.

**⚠️ CRÍTICO**: nenhuma história começa antes do checkpoint desta fase.

- [ ] T504 [P] Criar `core/domain/src/main/kotlin/br/com/diecastcollector/domain/usecase/UniqueExemplarPolicy.kt` com duas funções puras: `warnsOnAvailability(ownedInVariant, status, availability, hasActiveNegotiation)` e `warnsOnNegotiation(ownedInVariant, status, hasActiveNegotiation)`. Verdadeiro **somente** com `ownedInVariant == 1` **e** `status == OWNED` **e** `!hasActiveNegotiation` **e**, na disponibilidade, `availability != UNAVAILABLE` — exatamente a tabela-verdade de `data-model.md` §3 (FR-515, BR-503). **Aceite**: sem Android, sem banco, sem `Flow`; nenhuma regra de disponibilidade/negociação existente alterada; nenhuma dependência de módulo nova. **Teste**: `core/domain/src/test/kotlin/br/com/diecastcollector/domain/usecase/UniqueExemplarPolicyTest.kt` cobrindo as 7 linhas da tabela e os dois gatilhos. **Janela**: não se aplica.

- [ ] T505 [P] Acrescentar as três consultas de leitura: em `core/data/src/main/kotlin/br/com/diecastcollector/data/db/dao/DuplicateDao.kt`, `observeOffer(exemplarId): Flow<DuplicateOfferEntity?>` (`WHERE exemplar_id = :exemplarId LIMIT 1`) e `observeOffersByVariant(variantId): Flow<List<DuplicateOfferEntity>>` (`JOIN exemplar` por `variant_id`); em `core/data/src/main/kotlin/br/com/diecastcollector/data/db/dao/CollectionsDao.kt` (onde vive `NegotiationDao`), `observeActiveByVariant(variantId): Flow<List<NegotiationEntity>>` com `status IN ('OPEN','RESERVED')` e `JOIN exemplar` (plan §2, NFR-503). **Aceite**: SQL parametrizado (II.4); nenhuma consulta toca `observeDuplicates`; nenhum índice novo; os DAOs emitem `List`. **Teste**: consultas exercitadas em T507; `./gradlew :core:data:compileDebugKotlin`. **Janela**: não se aplica.

- [ ] T506 Expor e implementar as leituras: em `core/domain/src/main/kotlin/br/com/diecastcollector/domain/repository/DuplicateRepositories.kt`, `DuplicateRepository.observeOffer(exemplarId): Flow<DuplicateOffer?>` e `observeOffersByVariant(variantId): Flow<Map<String, DuplicateOffer>>` e `NegotiationRepository.observeActiveByVariant(variantId): Flow<Map<String, Negotiation>>`; implementar em `core/data/src/main/kotlin/br/com/diecastcollector/data/repository/DuplicateRepositories.kt` mapeando com `toDomain()` e indexando por `exemplarId` (o DAO entrega `List`, o repositório entrega `Map`). `setOffer`, `open`, `closeSale`, `closeTrade` e `cancel` **não podem** mudar (depende de T505). **Aceite**: assinaturas existentes intactas; nenhum método removido; mapeamento sem consulta por linha; exemplar sem oferta simplesmente ausente do mapa. **Teste**: `./gradlew :core:data:compileDebugKotlin` + T507. **Janela**: não se aplica.

- [ ] T507 Estender `core/data/src/test/kotlin/br/com/diecastcollector/data/repository/DuplicateQueryTest.kt`: oferta por exemplar (com e sem oferta), ofertas por variante (variante com 1, 2 e 3 unidades, só ofertas da variante), ativa por variante (OPEN/RESERVED sim, CLOSED/CANCELLED não), `setOffer` funcionando em variante de **1** exemplar, `UNAVAILABLE` preservando a linha, e a **mesma** composição de `observeGroups()` nas três massas (depende de T506). **Aceite**: cobre FR-501, FR-507, FR-510, FR-513 e SC-503 no nível de dados; nenhuma asserção existente afrouxada; todos os casos passam. **Teste**: `./gradlew :core:data:testDebugUnitTest --tests '*DuplicateQueryTest*'`. **Janela**: não se aplica.

- [ ] T508 Extrair a folha de disponibilidade: criar `app/src/main/java/br/com/diecastcollector/feature/duplicates/ExemplarOfferSheet.kt` a partir de `DuplicateActionsSheet.kt` e **remover** o arquivo antigo (`git mv` ou `mv` + ajuste de imports), sem deixar wrapper nem cópia (II.5), com `OfferSheetSubject(internalCode, title, condition, offer)` e `ExemplarOfferSheet(subject, saving, onDismiss, onSave)`, mantendo o conteúdo, a ordem visual e o `remember(subject.internalCode)` (contrato: [`contracts/offer-contract.md`](./contracts/offer-contract.md)); adaptar `app/src/main/java/br/com/diecastcollector/feature/duplicates/DuplicatesScreen.kt` para montar o assunto (depende de T502). **Aceite**: `grep -r "DuplicateActionsSheet" app/src` sem resultados (nenhum wrapper remanescente); Duplicados exibe os mesmos campos e botões; `DuplicatesUiState`/`DuplicatesViewModel` **não** mudam de assinatura; nenhum texto literal novo; a folha não navega nem grava. **Teste**: T510 + `./gradlew :app:assembleDebug`. **Janela**: C, F, P.

- [ ] T509 Criar `app/src/main/java/br/com/diecastcollector/feature/duplicates/UniqueExemplarDialog.kt`: `AlertDialog` com título, corpo e os botões **Continuar**/**Cancelar** (`unique_exemplar_warning_*`), sem "não mostrar de novo" e sem persistir nada (FR-515, D-02) (depende de T502). **Aceite**: alvos ≥ 48 dp; foco inicial no texto do aviso; **Cancelar** é a ação de descarte; sem chamada a repositório no componente; nenhum estado do aviso sobrevive a recriação (é intenção em memória). **Teste**: exercitado em T519 e T520. **Janela**: C/M/E, F, P.

- [ ] T510 Rodar a regressão de Duplicados **imediatamente após** a extração: `DuplicatesFlowTest`, `DuplicatesResponsiveTest` e `DuplicateQueryTest` (depende de T508). **Aceite**: mesma contagem, mesmo agrupamento, mesmas ações e mesmas mensagens de antes da extração; nenhum teste alterado para passar. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*Duplicates*'` e `./gradlew :core:data:testDebugUnitTest`. **Janela**: C, E.

**Checkpoint**: regra pura, três leituras e dois componentes prontos — as histórias podem começar.

---

## Phase 3: US1 (P1) — Disponibilizar a única unidade de uma variante 🎯 MVP

**Goal**: marcar disponibilidade em **qualquer** exemplar `OWNED`, a partir da lista e da ficha, com o aviso de preservação na confirmação.

**Independent Test**: instalação limpa, um exemplar em uma variante; abrir a ficha, marcar **Venda**, confirmar o aviso, salvar; conferir o estado na ficha e na lista e Duplicados vazio.

- [ ] T511 [US1] `app/src/main/java/br/com/diecastcollector/feature/catalog/ExemplarListViewModel.kt`: incluir `offers`, `activeNegotiations` e `selectedId` no estado, somando `observeOffersByVariant` e `observeActiveByVariant` ao `combine` existente; adicionar `onSelectExemplar`, `onDismissSheet`, `onSaveOffer(availability, condition, priceText, notes)` com validação de valor e `pendingOffer`/`uniqueWarning` vindos de `UniqueExemplarPolicy.warnsOnAvailability`, usando `state.exemplars.size` como contagem (FR-501, FR-507, FR-515). **Aceite**: nenhuma consulta por linha; gravação só no **Continuar**; falha mantém a folha e o valor anterior; mensagens pt-BR; ao reabrir a folha de um exemplar que já tem oferta, condição, valor pretendido e observações salvos aparecem preenchidos (FR-509). **Teste**: `app/src/test/java/br/com/diecastcollector/feature/catalog/ExemplarListNegotiationTest.kt` (unitário, repositórios falsos): aviso com 1 unidade, sem aviso com 2+, sem aviso em `Indisponível`, cancelar não grava, oferta salva reaberta com os três campos preservados. **Janela**: não se aplica.

- [ ] T512 [US1] `app/src/main/java/br/com/diecastcollector/feature/catalog/ExemplarListScreen.kt`: mostrar o rótulo de estado por linha (**Venda**, **Troca**, **Troca e venda**, **Indisponível**, **Em negociação**) e as ações **Disponibilidade**/**Negociar** em `FlowRow` **fora** da área `clickable` do cartão; montar `ExemplarOfferSheet` e `UniqueExemplarDialog` a partir do estado (FR-502, FR-520) (depende de T508, T509, T511). **Aceite**: tocar em qualquer ação não abre o detalhe; rótulo inteiro em 360 dp e fonte 200%; sem cor como único indicador; nenhum parâmetro antigo removido (`onAddExemplar`, `onOpenExemplar`). **Teste**: T515. **Janela**: C/M/E, F, P.

- [ ] T513 [US1] `app/src/main/java/br/com/diecastcollector/feature/catalog/ExemplarDetailViewModel.kt`: injetar `DuplicateRepository` e `NegotiationRepository`, observar `observeOffer` e negociação do exemplar, acrescentar `ownedInVariant` (via `countByVariant`, **reconsultado na ação**), `sheetOpen`, `pendingOffer` e `uniqueWarning`, e implementar `onOpenOfferSheet`, `onDismissOfferSheet`, `onSaveOffer`, `onContinueUniqueWarning`, `onCancelUniqueWarning` (FR-507, FR-511, FR-515, FR-506). **Aceite**: nenhuma gravação antes do Continuar; `SOLD`/`TRADED`/`ARCHIVED` recusam com mensagem; contagem reconsultada no instante da ação; estado de oferta vem do `Flow`, sem cópia local; falha ao gravar mantém a folha aberta, os valores digitados e a mensagem em pt-BR (FR-512); reabrir a folha mostra condição, valor e observações salvos (FR-509). **Teste**: `app/src/test/java/br/com/diecastcollector/feature/catalog/ExemplarDetailNegotiationTest.kt` (oferta, aviso, cancelar sem gravar, bloqueio, falha de gravação, reabertura preservando os três campos). **Janela**: não se aplica.

- [ ] T514 [US1] `app/src/main/java/br/com/diecastcollector/feature/catalog/ExemplarDetailScreen.kt`: acrescentar a seção **Negociação** na coluna de leitura existente (estado atual + botão **Disponibilidade** + explicação quando `status != OWNED`), renderizando `ExemplarOfferSheet` e `UniqueExemplarDialog`; passar os callbacks novos por `ExemplarDetailContent` mantendo os existentes (depende de T508, T509, T513). **Aceite**: nenhum formulário existente movido; sem sobreposição/corte em C/M/E; ações alcançáveis em paisagem com fonte 200%; textos vêm de `strings.xml`; erro de gravação aparece sem fechar a folha. **Teste**: T515 + `ExemplarDetailScreenTest` atualizado apenas na assinatura. **Janela**: C/M/E, F, P.

- [ ] T515 [US1] Teste instrumentado de disponibilidade nas duas superfícies — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarAvailabilityFlowTest.kt`: marcar **Venda** pela ficha e **Troca** pela lista em variante de 1 exemplar, com o aviso; conferir o estado na outra superfície e Duplicados vazio; reabrir a folha e conferir condição/valor/observações preservados (FR-509, FR-501, FR-502, SC-501, SC-502). **Aceite**: os dois caminhos funcionam sem segundo exemplar; estado consistente após voltar; campos salvos reaparecem; Duplicados continua vazio. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarAvailabilityFlowTest*'`. **Janela**: C.

**Checkpoint**: US1 entregue — disponibilizar exemplar único funciona pelas duas superfícies novas.

---

## Phase 4: US2 (P1) — Avisar sobre a coleção e abrir a negociação do exemplar

**Goal**: negociar a partir da ficha e da lista, com o aviso de preservação nos dois momentos e o tipo pré-selecionado pela oferta.

**Independent Test**: com uma variante de um exemplar, tocar **Negociar**, ver o aviso, cancelar (nada criado); repetir com **Continuar** e abrir a negociação.

- [ ] T516 [US2] Acrescentar a intenção de negociar às duas telas: `onNegotiate` em `ExemplarListScreen`/`ExemplarDetailScreen` e `warnsOnNegotiation` nos ViewModels (`ownedInVariant` + `status` + negociação ativa), guardando `pendingNegotiationId` em memória e expondo `onContinueNegotiation`/`onCancelNegotiation` (FR-515b, FR-513) (depende de T511, T513). **Aceite**: **Cancelar** não navega e não grava; **Continuar** entrega o id ao callback de navegação; com negociação ativa não há aviso nem segunda abertura; exemplar não `OWNED` não oferece a ação. **Teste**: casos novos nos dois testes unitários de T511/T513 cobrindo os 5 cenários de US2 no nível de estado. **Janela**: não se aplica.

- [ ] T517 [US2] Ligar a navegação em `app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt`: passar `onOpenNegotiation = { id -> navController.navigate(Destination.Negotiation.routeFor(id)) }` para `ExemplarListScreen` e `ExemplarDetailScreen`; **não** criar destino, rota ou argumento (FR-523, D-05) (depende de T516). **Aceite**: `Destination.Negotiation.route` inalterada; Duplicados continua com o mesmo caminho; nenhum `navArgument` novo. **Teste**: T519 e T520; `./gradlew :app:assembleDebug`. **Janela**: não se aplica.

- [ ] T518 [US2] `app/src/main/java/br/com/diecastcollector/feature/duplicates/NegotiationViewModel.kt`: definir o `type` inicial por `duplicateRepository.offerFor(exemplarId)` — `SALE → SALE`, `TRADE → TRADE`, `BOTH`/`UNAVAILABLE`/ausente mantêm `SALE`; com negociação ativa nada muda (FR-514) (depende de T506). **Aceite**: continua editável; nenhum parâmetro de rota novo; nenhuma regra de abertura alterada. **Teste**: `app/src/test/java/br/com/diecastcollector/feature/duplicates/NegotiationTypePreselectTest.kt` com os quatro casos + negociação ativa. **Janela**: não se aplica.

- [ ] T519 [US2] Teste instrumentado do aviso nas duas origens — `app/src/androidTest/java/br/com/diecastcollector/feature/duplicates/UniqueExemplarWarningTest.kt`: **Negociar** na ficha e na lista com 1 unidade (aviso aparece), com 2+ (não aparece), **Cancelar** (origem intacta, nenhuma negociação), **Continuar** (tela de negociação abre com tipo pré-selecionado); e, com negociação ativa, **Negociar** abre o estado ativo sem oferecer uma segunda abertura nem exibir a mensagem de erro em fluxo normal (FR-513, SC-504b, SC-505b, SC-506). **Aceite**: 100% dos casos de 1 unidade avisam e 0% dos de 2+; nenhuma negociação criada no cancelamento (conferido na tela de histórico); a recusa de segunda negociação continua comprovada por `NegotiationRepositoryTest`, citada na evidência de SC-506. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*UniqueExemplarWarningTest*'` + `./gradlew :core:data:testDebugUnitTest --tests '*NegotiationRepositoryTest*'`. **Janela**: C, F.

- [ ] T520 [US2] Teste instrumentado da negociação aberta pela ficha — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarNegotiationFlowTest.kt`: abrir negociação pela ficha, abrir e concluir, voltar e conferir a lista/duplicados (FR-513, FR-516, SC-507). **Aceite**: nenhuma rota nova no fluxo; histórico e estado corretos ao voltar. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarNegotiationFlowTest*'`. **Janela**: C.

**Checkpoint**: US2 entregue — negociar funciona pelas três origens, com aviso nos dois momentos.

---

## Phase 5: US3 (P2) — Duplicados continua sendo duplicados

**Goal**: provar, com massa fixa, que o módulo não mudou de significado nem de comportamento, e que o atalho continua no lugar.

**Independent Test**: com A, B, C e D, Duplicados mostra apenas B e C, com contagem correta, e as ações continuam funcionando.

- [ ] T521 [US3] Teste instrumentado de comparação — `app/src/androidTest/java/br/com/diecastcollector/feature/duplicates/DuplicatesUnchangedTest.kt`: com as massas A (1), B (2), C (3) e D (variante do mesmo modelo), conferir grupos, contagem e ausência de A/D; conferir que o atalho **Duplicados** continua visível no Catálogo e navega para a tela; marcar disponibilidade e negociar por ali (FR-503, FR-504, FR-524, SC-503). **Aceite**: composição idêntica à do comportamento anterior; atalho presente e funcional, sem texto ou posição novos; criar oferta em A (pela ficha) **não** faz A aparecer; vender uma unidade de B reduz o grupo e o remove ao chegar a 1. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*DuplicatesUnchangedTest*'`. **Janela**: C, E.

- [ ] T522 [US3] Confirmar no código que a consulta de duplicidade não mudou: revisar `git diff` de `DuplicateDao.observeDuplicates` e do trecho de agrupamento em `DuplicateRepositoryImpl.observeGroups`. **Aceite**: nenhuma alteração no `HAVING COUNT(*) > 1` nem no agrupamento por `variant_id`; se houver alteração, ela é revertida — não justificada. **Teste**: `git diff` do arquivo anexado à evidência; `DuplicateQueryTest` verde. **Janela**: não se aplica.

**Checkpoint**: US3 entregue — Duplicados comprovadamente intacto, inclusive a entrada pelo Catálogo.

---

## Phase 6: US4 (P2) — Estado consistente e estável nas três superfícies

**Goal**: mesma informação em lista, ficha e Duplicados, sem cópia divergente — inclusive quando a janela é recriada no meio do fluxo.

**Independent Test**: marcar **Troca** pela ficha, abrir lista e Duplicados, comparar rótulos e valor; abrir negociação e conferir **Em negociação** nas três; girar o aparelho com a folha e o aviso abertos.

- [ ] T523 [US4] Teste instrumentado de consistência — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarStateConsistencyTest.kt`: percorrer as três superfícies para as combinações sem oferta, `TRADE`, `SALE`, `BOTH`, `UNAVAILABLE` e com negociação ativa (FR-505, FR-511, FR-520, SC-508). **Aceite**: rótulo e valor iguais nas três em todas as combinações; **Em negociação** domina e coexiste com a disponibilidade; nenhuma superfície exige recarregar. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarStateConsistencyTest*'`. **Janela**: C.

- [ ] T524 [US4] Teste instrumentado de responsividade e acessibilidade — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarNegotiationResponsiveTest.kt`: ficha, lista, folha e aviso em 360/700/1.000 dp, retrato e paisagem, fonte 200%, alvos ≥ 48 dp e semântica de TalkBack (FR-521, FR-522, SC-513). **Aceite**: sem corte, sobreposição ou rolagem horizontal; **Salvar**/**Cancelar** e **Continuar**/**Cancelar** alcançáveis em todas as combinações; cada linha lê código + estado. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarNegotiationResponsiveTest*'`. **Janela**: C/M/E, F, P.

- [ ] T525 [US4] Teste instrumentado de estabilidade a recriação e a duas janelas — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarStabilityTest.kt`: com a folha preenchida e o aviso de disponibilidade aberto, e depois com o aviso de **Negociar** aberto, executar `ActivityScenario.recreate()`; conferir que nenhuma oferta/negociação foi gravada, que o aviso não reaparece e que a folha reabre no padrão; em configuração de janela dividida/freeform (ou duas instâncias da mesma rota), conferir que ambas exibem o estado do repositório e que fechar a folha numa não grava na outra (FR-505, FR-515, SC-505). **Aceite**: 0% de escrita em interrupção; nenhum estado duplicado entre janelas; a intenção pendente não é restaurada; nenhum dado da coleção alterado. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarStability*'`; a morte real de processo (`am force-stop`) fica como evidência manual no `quickstart.md` §13. **Janela**: C, E, P.

**Checkpoint**: US4 entregue — informação única, adaptável e resistente a recriação de janela.

---

## Phase 7: US5 (P3) — Ciclo de vida preservado

**Goal**: venda, troca e cancelamento mantêm as regras atuais, agora também a partir da ficha.

**Independent Test**: repetir os três desfechos a partir do detalhe e comparar com o comportamento anterior.

- [ ] T526 [US5] Teste instrumentado do ciclo — `app/src/androidTest/java/br/com/diecastcollector/feature/catalog/ExemplarNegotiationLifecycleTest.kt`: venda (exemplar sai do acervo ativo, histórico e financeiro registrados), troca (negociado `TRADED`, recebido segue `OWNED` e vinculado) e cancelamento (segue `OWNED`, disponibilidade anterior intacta, evento no histórico) (FR-516–FR-519, SC-507). **Aceite**: comportamento idêntico ao do caminho por Duplicados; nenhum registro apagado. **Teste**: `./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarNegotiationLifecycleTest*'`. **Janela**: C.

- [ ] T527 [US5] Reforçar nos dados que oferta e histórico sobrevivem: casos novos em `core/data/src/test/kotlin/br/com/diecastcollector/data/repository/NegotiationRepositoryTest.kt` para `UNAVAILABLE` durante negociação ativa, cancelamento preservando oferta, conclusão sem apagar evento e recusa de segunda negociação ativa com mensagem acionável (FR-510, FR-511, FR-513, FR-518, FR-519, SC-506, BR-507). **Aceite**: nenhuma assinatura de repositório mudou; todos os casos passam; nenhuma asserção existente enfraquecida. **Teste**: `./gradlew :core:data:testDebugUnitTest --tests '*NegotiationRepositoryTest*'`. **Janela**: não se aplica.

**Checkpoint**: US5 entregue — nenhuma regra de ciclo de vida regrediu.

---

## Phase 8: Polish (transversal)

**Propósito**: medir, documentar e fechar os portões G6.

- [ ] T528 Medir desempenho com 10.000 exemplares: abrir lista e ficha (meta < 1 s) e tempo entre **Salvar** e o rótulo atualizado (< 300 ms), usando o roteiro do [`quickstart.md`](./quickstart.md) §10 (NFR-504, NFR-505, SC-509, SC-510). **Aceite**: número medido e registrado com comando e aparelho; sem medição, a meta é declarada **não verificada** (V.8). **Teste**: medição reproduzível + registro em `docs/validacao.md`. **Janela**: C, E.

- [ ] T529 Atualizar a documentação (FR-528): `specs/001-diecast-collector/spec.md` (FR-065/FR-066 — duplicidade não é pré-condição), `specs/002-manual-usuario/inventario.md`, `specs/002-manual-usuario/inventario-regras.md` e `docs/manual/MANUAL_USUARIO_COMPLETO.md` (§21, §22 e capítulo do detalhe do exemplar), ensinando a negociar exemplar único, uma unidade de duplicata, vários exemplares, e a diferença entre disponibilidade e conclusão. **Aceite**: nenhum trecho afirma que negociar exige duplicata (C-02); vocabulário igual ao das telas (IV.1); nenhum requisito aprovado das specs 001/002 removido. **Teste**: busca textual por "duplicado" + leitura dos capítulos alterados. **Janela**: não se aplica.

- [ ] T530 Rodar o [`quickstart.md`](./quickstart.md) completo, incluindo §13 (interrupção durante o aviso), e registrar evidências manuais (aviso nos dois momentos, Duplicados comparado, consistência, ciclo, classes de janela, TalkBack, `am force-stop`). **Aceite**: cada cenário com resultado e caminho de captura, ou declarado **não verificado** — nunca aprovado sem evidência. **Teste**: registro em `docs/validacao.md` com data, aparelho e comando. **Janela**: C/M/E, F, P.

- [ ] T531 Fechamento de regressão (G6): suíte completa de unidade e instrumentada + `git diff` comprovando que `core/domain` mudou **apenas** com `UniqueExemplarPolicy` e os contratos de leitura, sem alteração de esquema, migração, `applicationId` ou `app_name`, e **sem dependência nova** (`gradle/libs.versions.toml` e `app/build.gradle.kts` incluídos no diff conferido) (FR-525, FR-526, FR-527, NFR-502, SC-511, SC-512). **Aceite**: 0 falhas; nenhum teste removido, ignorado ou afrouxado; `MigrationTest` verde; `git diff --stat` dos arquivos proibidos vazio, build files inclusive. **Teste**: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest :core:domain:testDebugUnitTest :core:data:testDebugUnitTest`; `git diff` de `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AppDatabase.kt` e manifest. **Janela**: não se aplica.

---

## Dependências & ordem de execução

### Dependências de fase

- **Phase 1 (Setup)**: sem dependência — T501 primeiro, T502/T503 em paralelo.
- **Phase 2 (Foundational)**: depende de T501/T502 — **bloqueia** todas as histórias.
  - T504 e T505 em paralelo; T506 depois de T505; T507 depois de T506; T508 e T509 depois de T502; T510 depois de T508.
- **Phase 3 (US1)**: depois do checkpoint da Fase 2. T511 e T513 em paralelo (ViewModels diferentes); T512 depois de T511; T514 depois de T513; T515 por último.
- **Phase 4 (US2)**: depois de US1 (T516 reusa estado de T511/T513). T517 depois de T516; T518 independente (só depende de T506); T519/T520 depois de T517.
- **Phase 5 (US3)**: depois de T510; T522 independe de T521.
- **Phase 6 (US4)**: depois de US1 e US2. T523, T524 e T525 são independentes entre si.
- **Phase 7 (US5)**: depois de US2.
- **Phase 8 (Polish)**: depois das histórias que se decidir entregar.

### Dependências entre histórias

| História | Depende de | Observação |
|---|---|---|
| US1 (P1) | Fase 2 | entrega valor sozinha: disponibilizar exemplar único |
| US2 (P1) | Fase 2 + US1 | reusa estado de oferta criado em US1; sem ele, a pré-seleção fica em `SALE` |
| US3 (P2) | Fase 2 | só verificação; não altera produção |
| US4 (P2) | US1, US2 | compara e estressa as três superfícies já existentes |
| US5 (P3) | US2 | exercita o ciclo a partir da origem nova |

### Oportunidades de paralelismo

- Fase 2: T504 (domínio) ∥ T505 (DAOs) ∥ T508/T509 (componentes), com T506→T507 em série.
- Fase 3: T511 (lista) ∥ T513 (ficha).
- Fase 4: T518 (pré-seleção) ∥ T516/T517 (aviso + navegação).
- Fase 6: T523 ∥ T524 ∥ T525.
- Fase 8: T528 (medição) ∥ T529 (documentação).

---

## Exemplo de execução paralela

```bash
# Fase 2 — três trilhos independentes
Trilho A: T504 (UniqueExemplarPolicy + teste de domínio)
Trilho B: T505 → T506 → T507 (DAOs → repositórios → testes de dados)
Trilho C: T508 + T509 (folha compartilhada + diálogo), depois T510 (regressão)

# Fase 3 — duas telas em paralelo
Trilho A: T511 → T512 (lista)
Trilho B: T513 → T514 (ficha)
Depois: T515 (instrumentado das duas)

# Fase 6 — três verificações independentes
Trilho A: T523 (consistência)   Trilho B: T524 (responsividade)   Trilho C: T525 (estabilidade)
```

---

## Estratégia de implementação

### MVP primeiro (US1 apenas)

1. Fase 1 + Fase 2 completas.
2. Fase 3 (T511–T515).
3. **Parar e validar**: exemplar único disponível pelas duas superfícies, Duplicados intacto, aviso ao salvar, campos preservados ao reabrir.
4. O MVP já corrige o defeito central (hoje não existe caminho para disponibilizar a única unidade).

### Entrega incremental

1. Setup + Foundational → base pronta.
2. US1 → validar → marcar disponibilidade é possível em qualquer exemplar (MVP).
3. US2 → validar → negociar a partir do exemplar com aviso nos dois momentos.
4. US3/US4 → validar → Duplicados provado igual (atalho incluído), estado consistente e estável à recriação.
5. US5 → validar → ciclo de vida sem regressão.
6. Polish → medir, documentar, fechar G6.

### Regras de execução

- Nenhuma tarefa altera esquema, migração, rota, dependência, `applicationId` ou `app_name`.
- Toda tarefa termina com `./gradlew :app:assembleDebug` (ou o módulo afetado) verde e sem `lintDebug` novo.
- Teste que falha é corrigido na causa; nenhum `@Ignore` ou asserção enfraquecida (V.4).
- Commit por tarefa ou grupo lógico, apenas quando o usuário pedir.

---

## Notas

- `[P]` = arquivos diferentes, sem dependência pendente.
- `[USn]` mapeia a tarefa à história da spec, para rastreabilidade no G5.
- Cada história tem checkpoint próprio: dá para parar e validar sem esperar as seguintes.
- Evidência não registrada é **não verificada** — nunca "aprovada".
- Duplicados só é tocado em T508 (chamada da folha) e sai provado por T510/T521/T522.
