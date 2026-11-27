# Implementation Plan: Negociar qualquer exemplar da coleção

**Branch**: `007-negociar-qualquer-exemplar` | **Date**: 2026-11-27 | **Spec**: [`spec.md`](./spec.md)

**Input**: especificação `007-negociar-qualquer-exemplar` (aprovada em 2026-11-27, com D-01/D-02 revisadas para avisar em **dois** momentos) e Constituição v1.3.0.

> **Natureza desta fase**: planejamento. Nenhum arquivo de produção é alterado aqui. As tarefas detalhadas virão em `tasks.md`; este documento fixa abordagem técnica, arquivos, ordem e critérios de verificação.

## Summary

Hoje a negociação só é alcançável por **Catálogo → Duplicados**, e a tela Duplicados só lista variantes com mais de um exemplar `OWNED` — o vínculo com duplicidade existe **apenas na consulta de listagem e no único caminho de interface**, não no banco. A ampliação é, portanto, de **acesso**, não de modelo.

A abordagem técnica é:

1. **Domínio**: uma regra pura nova, `UniqueExemplarPolicy`, decide quando avisar (variante com exatamente um `OWNED`, exemplar `OWNED`, oferta diferente de `Indisponível`, sem negociação ativa). Nenhum repositório é redefinido; nenhuma entidade, coluna ou migração.
2. **Dados**: três leituras em lote por variante (oferta por exemplar, oferta de um exemplar, negociação ativa por exemplar) — uma consulta por lista, nunca por linha. O `setOffer` e o `open` atuais **não mudam**.
3. **Apresentação**: extrair a folha de disponibilidade para um componente com sujeito próprio (`OfferSheetSubject`), reaproveitada por Duplicados, ficha e lista; extrair o aviso de exemplar único para um diálogo compartilhado; adicionar as ações **Disponibilidade** e **Negociar** nas duas superfícies do exemplar.
4. **Navegação**: nenhuma rota nova. `AppNavHost` passa a ligar as duas novas origens à rota existente `duplicates/{exemplarId}/negotiation` (dívida de nome registrada em C-03/S-02). O tipo da negociação é pré-selecionado pela oferta **dentro** do `NegotiationViewModel`, sem parâmetro de rota (FR-514).
5. **Duplicados**: a tela e a consulta (`HAVING COUNT(*) > 1`) permanecem **intocadas**; o aviso não aparece nela porque o grupo só existe com 2+ exemplares.

## Technical Context

**Language/Version**: Kotlin 2.1.0, JVM 17

**Primary Dependencies atuais**: Compose BOM `2025.01.00` (material3, ui), `activity-compose 1.10.0`, `navigation-compose 2.8.5`, Hilt 2.53.1, Room 2.6.1, Coil 2.7.0

**Dependência pretendida**: **nenhuma nova**. Tudo é Compose, Room, Hilt e corrotinas já presentes (D-10).

**Storage**: Room, esquema **v4**, **nenhuma migração** (D-06). Reaproveita `duplicate_offer` (uma linha por exemplar, `exemplar_id` único) e `negotiation` (índices por `exemplar_id`/`status`). Sem nova tabela, coluna, índice ou mudança no pacote de backup.

**Testing**: JUnit4 + Robolectric (`app/src/test`), testes de dados com Room real (`core/data/src/test`), Compose UI Test + Espresso (`app/src/androidTest`), reuso de `WindowSizeRule`/`AdaptiveGrid` e das capturas da Spec 005.

**Target Platform**: Android `minSdk 26`, `targetSdk 36`; celular, tablet e dobrável

**Project Type**: aplicativo Android multi-módulo (`app`, `core:domain`, `core:data`, `core:ui`, `benchmark`)

**Performance Goals**: abrir lista de exemplares e ficha < 1 s com 10.000 exemplares (NFR-504/R.7); salvar disponibilidade refletida em < 300 ms nas superfícies abertas (NFR-505); nenhuma consulta por linha (NFR-503)

**Constraints**: offline total (VI.10); nenhuma rota nova (FR-523); nenhum `applicationId`/`app_name`/esquema alterado (FR-525); Duplicados com comportamento e consulta idênticos (FR-503/FR-527); pt-BR em toda string nova (IV.1); classes de janela compacta/média/expandida (FR-521, VII.2)

**Scale/Scope**: 1 regra de domínio pura, 3 consultas de leitura, 1 componente de folha extraído, 1 diálogo novo, 2 telas alteradas, 3 ViewModels alterados, 1 navegação ligada, **7 strings pt-BR**, **~17 arquivos de teste** (6 de unidade/dados — 4 novos e 2 estendidos — e 11 instrumentados — 10 novos e 1 ajustado)

## Constitution Check

*GATE: passa antes da Fase 0 e é revalidado após o desenho.*

| Princípio | Como o plano atende | Situação |
|---|---|---|
| I.3 (duplicidade por variante) | Nenhuma consulta de duplicidade é alterada; `HAVING COUNT(*) > 1` por `variant_id` permanece a única definição | PASSA |
| I.6/I.7/I.9 (sem perda silenciosa, histórico, `NULL` honesto) | Disponibilidade e negociação não apagam nada; `UNAVAILABLE` mantém a linha; campos opcionais continuam `NULL` | PASSA |
| I.10/I.12 (migração, concorrência) | Nenhuma migração; `setOffer`/`open` continuam transacionais e com `rowVersion` no exemplar | PASSA |
| II.1–II.4 (modularidade, regra fora da UI) | A decisão de avisar vira regra pura no domínio; ViewModels só orquestram; SQL só nos DAOs, com parâmetros vinculados | PASSA |
| II.5 (sem duplicação) | Folha de disponibilidade e aviso passam a ter **um** componente cada, usados por três e duas origens | PASSA |
| II.6 (regra testável sem UI) | `UniqueExemplarPolicy` é função pura, testada sem Android/banco | PASSA |
| IV.1/IV.4/IV.9 (pt-BR, feedback, confirmação) | Strings em `strings.xml`; erro mantém a folha aberta; o aviso é a confirmação que explica o efeito | PASSA |
| IV.6/VII.10 (acessibilidade) | Diálogo com `AlertDialog`/foco padrão, alvos ≥ 48 dp, rótulo semântico por linha, fonte 200% prevista nos testes | PASSA |
| V.1/V.2/V.4/V.10 (testes, janelas) | Regra pura + dados + instrumentados nas três origens e nas classes de janela; suíte existente mantida (testes adaptados, nunca afrouxados) | PASSA |
| VI.10 (offline) | Tudo local; nenhuma chamada externa | PASSA |
| VII.2/VII.5/VII.9/VII.11 (responsividade, identidade, sem mudar regra) | Seção **Negociação** entra na coluna de leitura existente da ficha; ações por linha na lista usam `FlowRow`/botões já padronizados; nenhum token novo | PASSA |
| R.4/R.5 (decisão registrada, dependências) | Sem dependência nova; sem decisão tecnológica — ver "ADRs" abaixo | PASSA |
| R.7/R.8 (desempenho, SQLite) | Leituras em lote por variante; medição prevista; esquema não muda | PASSA |

Sem violações a justificar em *Complexity Tracking*.

## Project Structure

### Documentação (esta feature)

```text
specs/007-negociar-qualquer-exemplar/
├── spec.md              # requisitos aprovados (5 histórias, FR-501…FR-528)
├── plan.md              # este documento
├── research.md          # Fase 0: investigação do código e alternativas descartadas
├── data-model.md        # Fase 1: leituras derivadas, invariantes e máquina de estados
├── quickstart.md        # Fase 1: roteiro manual dos SC-501…SC-513
├── contracts/
│   ├── offer-contract.md        # contrato da folha de disponibilidade compartilhada
│   └── navigation-contract.md   # origens, rotas e semântica do aviso
├── checklists/
│   └── requirements.md  # validação de qualidade da spec
└── tasks.md             # Fase 2 (/speckit-tasks — não criado aqui)
```

### Código (estrutura existente, sem reorganização)

```text
core/domain/src/main/kotlin/br/com/diecastcollector/domain/
├── model/DuplicateModels.kt                 # inalterado (KDoc corrigido: C-05)
├── model/Enums.kt                           # inalterado
├── repository/DuplicateRepositories.kt      # +2 leituras em DuplicateRepository
├── repository/ItemRepositories.kt           # inalterado (ExemplarRepository já basta)
├── usecase/DuplicateUseCases.kt             # inalterado
└── usecase/UniqueExemplarPolicy.kt          # NOVO: regra pura do aviso

core/data/src/main/kotlin/br/com/diecastcollector/data/
├── db/dao/DuplicateDao.kt                   # +2 consultas de oferta (variante e exemplar)
├── db/dao/CollectionsDao.kt                 # +1 consulta em NegotiationDao (ativa por variante)
├── db/dao/ItemDao.kt                        # inalterado (countByVariant já existe)
└── repository/DuplicateRepositories.kt      # implementa as leituras novas; setOffer/open intocados

app/src/main/java/br/com/diecastcollector/
├── feature/duplicates/ExemplarOfferSheet.kt     # renomeado de DuplicateActionsSheet.kt; o antigo é removido (II.5)
├── feature/duplicates/UniqueExemplarDialog.kt   # NOVO: aviso compartilhado (Continuar/Cancelar)
├── feature/duplicates/DuplicatesScreen.kt       # só a chamada passa a usar o novo assunto
├── feature/duplicates/NegotiationViewModel.kt   # pré-seleciona o tipo pela oferta (FR-514)
├── feature/catalog/ExemplarDetailScreen.kt      # seção Negociação + folha + aviso
├── feature/catalog/ExemplarDetailViewModel.kt   # oferta/negociação ativa, unicidade, intenções
├── feature/catalog/ExemplarListScreen.kt        # indicador + duas ações por linha
├── feature/catalog/ExemplarListViewModel.kt     # leituras em lote, seleção, intenções
├── navigation/AppNavHost.kt                     # liga as origens novas à rota existente
└── res/values/strings.xml                       # 7 strings novas (bloco "Duplicados e negociação")

app/src/test/java/br/com/diecastcollector/feature/catalog/   # testes de ViewModel (padrão existente)
app/src/androidTest/java/br/com/diecastcollector/feature/    # ficha, lista, aviso, responsividade
core/data/src/test/kotlin/br/com/diecastcollector/data/repository/DuplicateQueryTest.kt  # estendido
core/domain/src/test/kotlin/br/com/diecastcollector/domain/usecase/UniqueExemplarPolicyTest.kt # novo
```

**Structure Decision**: mantém a arquitetura atual (II.1–II.4, VII.13). Nada é movido de módulo; as únicas extrações são dentro de `app/feature/duplicates`, que já é dono da folha e do fluxo de negociação.

## Estratégia técnica

### 1. Regra do aviso no domínio (II.2, II.6)

`core/domain/.../usecase/UniqueExemplarPolicy.kt`:

```kotlin
public object UniqueExemplarPolicy {
    /** RN-003 na confirmação da disponibilidade (FR-515a). */
    public fun warnsOnAvailability(
        ownedInVariant: Int,
        status: ExemplarStatus,
        availability: DuplicateAvailability,
        hasActiveNegotiation: Boolean,
    ): Boolean

    /** RN-003 na abertura da negociação (FR-515b). */
    public fun warnsOnNegotiation(
        ownedInVariant: Int,
        status: ExemplarStatus,
        hasActiveNegotiation: Boolean,
    ): Boolean
}
```

Verdadeiro **somente** quando: `ownedInVariant == 1` **e** `status == OWNED` **e** `!hasActiveNegotiation` **e** (na disponibilidade) `availability != UNAVAILABLE`. `SOLD`/`TRADED`/`ARCHIVED` nunca avisam porque nem chegam à ação (FR-506); a checagem dupla evita aviso inútil em corrida.

Por que no domínio e não na tela: é regra de negócio derivada de RN-003 e das definições de acervo; precisa ser testável sem Android (II.6) e a mesma regra é usada por duas telas (II.5).

### 2. Leituras em lote (NFR-503)

| Necessidade | Onde | Forma |
|---|---|---|
| Oferta de um exemplar, reativa | `DuplicateDao.observeOffer(exemplarId)` | `Flow<DuplicateOfferEntity?>` — mesmo filtro de `findOffer` |
| Ofertas de todos os exemplares da variante | `DuplicateDao.observeOffersByVariant(variantId)` | `JOIN exemplar` por `variant_id`; `Flow<List<DuplicateOfferEntity>>` |
| Negociação ativa de todos os exemplares da variante | `NegotiationDao.observeActiveByVariant(variantId)` | `status IN ('OPEN','RESERVED')` + `JOIN exemplar`; `Flow<List<NegotiationEntity>>` |

O DAO emite `List`; o repositório indexa por `exemplarId` e expõe **`Flow<Map<String, …>>`**, que é o que as telas consomem (T506): `observeOffersByVariant(variantId): Flow<Map<String, DuplicateOffer>>` e `observeActiveByVariant(variantId): Flow<Map<String, Negotiation>>`. Exemplar sem registro simplesmente não aparece no mapa.
| Quantos `OWNED` a variante tem | `ExemplarDao.countByVariant` (já existe) | `suspend`, chamado na ação |

`DuplicateRepository` ganha **duas** leituras (`observeOffer`, `observeOffersByVariant`) e `NegotiationRepository` ganha **uma** (`observeActiveByVariant`). Nenhuma assinatura existente muda; `setOffer` e `open` continuam idênticos — o que garante FR-501/FR-513 sem tocar em regra.

Para a ficha, a contagem é reconsultada **no momento da ação** (`countByVariant`), não apenas no carregamento: a unicidade precisa valer no instante em que o usuário confirma (premissa da spec), não no instante em que abriu a tela.

### 3. Folha de disponibilidade compartilhada (D-03, II.5)

A folha (renomeada de `DuplicateActionsSheet` para `ExemplarOfferSheet`) deixa de receber `DuplicateExemplar` e passa a receber um assunto mínimo:

```kotlin
public data class OfferSheetSubject(
    val internalCode: String,
    val title: String,          // "Fabricante · Modelo · Cor", quando houver
    val condition: Condition,
    val offer: DuplicateOffer?,
)
```

Assinatura: `ExemplarOfferSheet(subject, saving, onDismiss, onSave)`. O estado local (opção escolhida, condição, valor, observações) continua `remember(subject.internalCode)`, o que preserva a seleção quando o aviso abre por cima. `DuplicatesScreen` é o único chamador atual e passa a montar o assunto — **nenhuma mudança de comportamento** (FR-503/FR-527).

O arquivo é renomeado para `ExemplarOfferSheet.kt` — o antigo **não** permanece, nem como wrapper (II.5) — mantendo `DuplicateAvailability.labelRes()` no mesmo lugar; nenhum teste referencia o composable hoje (verificado por busca), apenas `DuplicatesContent`.

### 4. Aviso de exemplar único (FR-515, D-01/D-02)

`UniqueExemplarDialog(titulo, corpo, onContinue, onCancel)` em `AlertDialog`, com o texto da consequência e os botões **Continuar** / **Cancelar**. Não há "não mostrar de novo" e nada é persistido.

- **Disponibilidade**: `onSave` da folha → valida valor → consulta contagem/negociação ativa → se `warnsOnAvailability`, guarda `pendingOffer` e mostra o diálogo **acima** da folha (a folha continua montada, com a seleção). **Continuar** grava e fecha a folha; **Cancelar** fecha só o aviso e devolve o usuário à folha. Nada é gravado em nenhum dos caminhos até o Continuar.
- **Negociação**: `onNegotiate` na ficha/lista → se `warnsOnNegotiation`, mostra o diálogo **antes** de navegar; **Continuar** navega para a rota atual; **Cancelar** fecha o aviso e não navega (a tela de origem fica exatamente como estava).
- **Duplicados**: não usa o aviso — a variante do grupo tem 2+ `OWNED` por construção (FR-503/FR-516).

O aviso nunca aparece com negociação ativa (o caso de borda da spec): a própria condição `!hasActiveNegotiation` cobre os dois momentos.

### 5. Ficha do exemplar

`ExemplarDetailUiState` ganha `offer`, `activeNegotiation`, `ownedInVariant`, `sheetOpen`, `pendingOffer`, `uniqueWarning`. O ViewModel injeta `DuplicateRepository`, `NegotiationRepository` e passa a observar oferta e negociação do exemplar (dois `Flow` já indexados por `exemplar_id`).

A seção **Negociação** entra na coluna de leitura existente (antes de Localização, depois de Detalhes), com: estado atual (oferta + **Em negociação** como dominante, FR-511), botão **Disponibilidade** e botão **Negociar**, e explicação no lugar dos botões quando `status != OWNED` (FR-506). Nada do formulário de condição/localização se move (risco registrado na spec).

`ExemplarDetailScreen` ganha `onOpenNegotiation: (String) -> Unit`; o `navController` é quem navega (a tela não conhece rotas).

### 6. Lista de exemplares

`ExemplarListUiState` ganha `offers: Map<String, DuplicateOffer?>`, `activeNegotiations: Map<String, Negotiation>` e `selectedId`. As três leituras da variante são combinadas no `combine` que já existe (agora com cinco fontes) — mantém `SharingStarted.WhileSubscribed` e não cria consulta por linha.

Cada cartão passa a ter uma linha de estado (`Venda`, `Troca`, `Troca e venda`, `Indisponível`, **Em negociação**) e duas ações em `FlowRow`, abaixo do cartão e fora da área clicável que abre o detalhe (evita ação dentro de `clickable`). Com 1 exemplar `OWNED` na variante, o aviso pode aparecer daqui também.

### 7. Navegação e pré-seleção de tipo (FR-514, FR-523)

`AppNavHost`: `ExemplarListScreen` e `ExemplarDetailScreen` recebem `onOpenNegotiation = { id -> navController.navigate(Destination.Negotiation.routeFor(id)) }`. Nenhuma rota nova, nenhum argumento novo.

`NegotiationViewModel` lê `duplicateRepository.offerFor(exemplarId)` no `init` e, **se não houver negociação ativa**, define `type` inicial por `availability` (`SALE → SALE`, `TRADE → TRADE`, `BOTH`/`UNAVAILABLE`/ausente → padrão atual `SALE`). Continua editável, como antes. Fazer isso no ViewModel evita parâmetro de rota e mantém a decisão num único ponto.

### 8. Textos e acessibilidade

Strings novas (bloco "Duplicados e negociação", reutilizando o que existe):

```xml
<string name="exemplar_negotiation_section">Negociação</string>
<string name="exemplar_negotiation_blocked">Só exemplar no acervo pode ser disponibilizado ou negociado.</string>
<string name="exemplar_negotiation_offer_now">Disponível: %1$s</string>
<string name="unique_exemplar_warning_title">Último exemplar da variante</string>
<string name="unique_exemplar_warning_body">Este é o único exemplar desta variante no acervo. Concluída a venda ou a troca, ele deixa de fazer parte da coleção ativa. A disponibilidade não muda o acervo; a saída acontece só na conclusão.</string>
<string name="unique_exemplar_warning_continue">Continuar</string>
<string name="unique_exemplar_warning_cancel">Cancelar</string>
```

Rótulos de ação reutilizam `duplicates_offer_action` (**Disponibilidade**) e `duplicates_negotiate_action` (**Negociar**), garantindo o mesmo vocabulário nas três superfícies (FR-520). Estado nunca depende só de cor: sempre há texto (FR-520, IV.6).

### 9. ADRs

**Nenhum ADR novo.** R.4 exige decisão registrada para escolha **tecnológica**; aqui não há tecnologia nova, dependência nova, esquema novo nem mudança de rota. As escolhas são de reuso de componente e de ponto de aviso, já registradas como D-01…D-10 na spec. `docs/dependencias.md` não muda.

## Contratos de apresentação

Ver [`contracts/offer-contract.md`](./contracts/offer-contract.md) e [`contracts/navigation-contract.md`](./contracts/navigation-contract.md). Resumo da máquina de estados da disponibilidade:

```text
Fechada ──abrir──▶ Editando ──Salvar (sem aviso)──▶ Gravando ──ok──▶ Fechada (mensagem)
                      │                              └──erro──▶ Editando (mensagem, valores mantidos)
                      └──Salvar (com aviso)──▶ Avisando ──Continuar──▶ Gravando
                                                   └──Cancelar──▶ Editando (seleção preservada)
```

Invariante: entre **Salvar** e **Continuar** nada é gravado no banco.

## Fases de implementação

### Fase 1 — Domínio e dados (sem interface)

1. `UniqueExemplarPolicy` + `UniqueExemplarPolicyTest` (tabela-verdade dos dois gatilhos e dos não-gatilhos).
2. Consultas de leitura nos DAOs e as três assinaturas nos repositórios; implementação em `DuplicateRepositoryImpl`/`NegotiationRepositoryImpl`.
3. Estender `DuplicateQueryTest` com: oferta por variante, oferta por exemplar, ativa por variante, e a garantia de que a consulta de duplicados é **idêntica** (mesmas massas, mesma composição).
4. KDoc de `DuplicateAvailability`/`DuplicateOffer` corrigido (C-05) — sem mudança de tipo.

**Saída**: regra e leituras prontas, suíte de dados verde, nenhuma tela alterada.

### Fase 2 — Componentes compartilhados

1. `ExemplarOfferSheet` a partir de `DuplicateActionsSheet` + `OfferSheetSubject`, com o arquivo antigo removido (II.5); `DuplicatesScreen` adaptada.
2. `UniqueExemplarDialog`.
3. Rodar `DuplicatesFlowTest` e `DuplicatesResponsiveTest` para provar que a extração não mudou comportamento (SC-503).

**Saída**: folha e aviso reutilizáveis, Duplicados comprovadamente igual.

### Fase 3 — Ficha e lista

1. `ExemplarDetailViewModel`/`Screen`: estado de negociação, seção, ações, aviso, bloqueio explicado.
2. `ExemplarListViewModel`/`Screen`: leituras em lote, indicador por linha, ações, aviso.
3. `AppNavHost`: callbacks novos → rota existente.
4. `NegotiationViewModel`: pré-seleção do tipo pela oferta.
5. Strings pt-BR e semântica.

**Saída**: qualquer exemplar `OWNED` negociável pelas três origens.

### Fase 4 — Testes, medição e documentação

1. Instrumentados: três origens, aviso nos dois momentos, cancelamentos, consistência entre telas, bloqueios, classes de janela e fonte 200%.
2. Medições: abertura da lista/ficha, latência da atualização; registro em `docs/validacao.md`.
3. Documentação FR-528: Spec 001 (FR-065/FR-066), `specs/002-manual-usuario/inventario.md`, `inventario-regras.md`, `docs/manual/MANUAL_USUARIO_COMPLETO.md` (§21, §22 e capítulo do detalhe).
4. Suíte completa.

**Saída**: feature verificada, medida e documentada.

## Estratégia de testes (V.1, V.2, V.10)

| Camada | Arquivo previsto | Cobertura |
|---|---|---|
| Unidade (domínio) | `core/domain/.../usecase/UniqueExemplarPolicyTest.kt` | 1 vs 2+ `OWNED`; `UNAVAILABLE`; negociação ativa; `SOLD`/`TRADED`/`ARCHIVED`; os dois gatilhos |
| Dados | `core/data/.../repository/DuplicateQueryTest.kt` (estendido) | oferta por variante e por exemplar; ativa por variante; `setOffer` em variante de 1 exemplar; oferta preservada ao marcar `UNAVAILABLE`; consulta de duplicados inalterada (1/2/3 unidades) |
| Unidade (app) | `app/src/test/.../feature/catalog/ExemplarDetailNegotiationTest.kt` | estado inicial por status; aviso ao salvar disponibilidade; cancelar não grava; continuar grava; bloqueio `SOLD` |
| Unidade (app) | `app/src/test/.../feature/catalog/ExemplarListNegotiationTest.kt` | unicidade pela contagem da variante; ação por linha; aviso |
| Unidade (app) | `app/src/test/.../feature/duplicates/NegotiationTypePreselectTest.kt` | `SALE`/`TRADE`/`BOTH`/ausente e com negociação ativa |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarDetailNegotiationTest.kt` | seção Negociação, folha, aviso (Continuar/Cancelar), estado após salvar |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarListNegotiationTest.kt` | indicador e ações por linha; aviso; variante repetida sem aviso |
| Instrumentado | `app/src/androidTest/.../feature/duplicates/UniqueExemplarWarningTest.kt` | aviso antes de navegar; Cancelar mantém a origem; Continuar abre a negociação; negociação ativa não oferece segunda abertura (SC-506) |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarAvailabilityFlowTest.kt` | disponibilidade pelas duas superfícies; aviso no salvar; campos preservados ao reabrir; Duplicados vazio (SC-501, SC-502) |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarNegotiationFlowTest.kt` | abrir e concluir pela ficha; histórico e estado ao voltar (SC-507) |
| Instrumentado | `app/src/androidTest/.../feature/duplicates/DuplicatesUnchangedTest.kt` | massas A–D; atalho do Catálogo; ações preservadas (SC-503, FR-524) |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarStateConsistencyTest.kt` | mesmo estado nas três superfícies em 6 combinações (SC-508) |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarNegotiationLifecycleTest.kt` | venda, troca e cancelamento pela ficha (SC-507) |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarNegotiationResponsiveTest.kt` | 360/700/1.000 dp, paisagem e fonte 200% |
| Instrumentado | `app/src/androidTest/.../feature/catalog/ExemplarStabilityTest.kt` | recriação com folha e aviso abertos (disponibilidade e negociação); duas janelas; nada gravado |
| Regressão | `DuplicatesFlowTest`, `DuplicatesResponsiveTest`, `ExemplarDetailScreenTest`, `MigrationTest`, suíte de unidade | comportamento atual preservado (SC-511) |

Não existe `ExemplarListScreenTest` no repositório (verificado em `app/src/androidTest`): a lista é coberta pelos testes instrumentados novos de T515, T519 e T523. Apenas `ExemplarDetailContent` recebe parâmetros novos e seu teste é ajustado **na assinatura**, não na asserção (V.4).

## Arquivos que provavelmente precisarão de alteração

| Arquivo | Mudança |
|---|---|
| `core/domain/.../usecase/UniqueExemplarPolicy.kt` | novo: regra pura |
| `core/domain/.../repository/DuplicateRepositories.kt` | +`observeOffer`, +`observeOffersByVariant`, +`observeActiveByVariant` |
| `core/data/.../db/dao/DuplicateDao.kt` | +2 consultas de oferta |
| `core/data/.../db/dao/CollectionsDao.kt` | +1 consulta em `NegotiationDao` (o DAO vive neste arquivo) |
| `core/data/.../repository/DuplicateRepositories.kt` | implementa as leituras; `setOffer`/`open` intocados |
| `core/domain/.../model/DuplicateModels.kt` | KDoc (C-05) |
| `app/.../feature/duplicates/ExemplarOfferSheet.kt` | novo nome do arquivo da folha, com `OfferSheetSubject` |
| `app/.../feature/duplicates/UniqueExemplarDialog.kt` | novo |
| `app/.../feature/duplicates/DuplicatesScreen.kt` | monta o assunto; sem mudança visual |
| `app/.../feature/duplicates/NegotiationViewModel.kt` | tipo inicial pela oferta |
| `app/.../feature/catalog/ExemplarDetail{Screen,ViewModel}.kt` | seção, ações, aviso, bloqueio |
| `app/.../feature/catalog/ExemplarList{Screen,ViewModel}.kt` | indicador, ações por linha, aviso |
| `app/.../navigation/AppNavHost.kt` | dois callbacks → rota existente |
| `app/src/main/res/values/strings.xml` | 7 strings novas (reuso das 4 de disponibilidade) |
| `app/src/test`, `app/src/androidTest`, `core/data/src/test`, `core/domain/src/test` | testes da tabela acima |
| `docs/validacao.md`, `docs/manual/*`, `specs/001/.../spec.md`, `specs/002-manual-usuario/{inventario,inventario-regras}.md` | FR-528 |

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Extração da folha mudar o comportamento de Duplicados | `DuplicatesContent` mantém assinatura e testes; `DuplicatesFlowTest`/`DuplicatesResponsiveTest` rodam após a extração (Fase 2) |
| Aviso duplicado (folha + negociação) soar repetitivo | Aviso só nas duas intenções; sem "não mostrar de novo"; com negociação ativa nunca aparece |
| Gravação acidental antes do **Continuar** | `pendingOffer` só em memória; gravação exclusivamente no `onContinue`; teste de banco após **Cancelar** (SC-505) |
| Contagem desatualizada decidindo o aviso | Reconsulta de `countByVariant` no momento da ação, não só no carregamento |
| Custo de duas leituras novas na lista | Uma consulta por variante em cada uma, combinadas no `combine`; medição de abertura < 1 s (SC-510) |
| Ações por linha dentro de cartão clicável | Ações em `FlowRow` fora da área de `clickable`; teste de toque |
| Diálogo sobre a folha perder a seleção | Estado da folha por `remember(subject.internalCode)`, sem `key` mutável; teste do caminho Cancelar |
| Rotação, segunda janela ou morte de processo com o aviso aberto | Estado sempre recomposto do repositório e intenção pendente só em memória; `ExemplarStabilityTest` prova que recriar não grava; morte de processo fica como evidência manual no quickstart §13 |
| Pré-seleção de tipo virar regra divergente | Uma única derivação no `NegotiationViewModel`, testada; continua editável |
| Nomes herdados (`duplicate_offer`, rota `duplicates/…`) confundirem manutenção | Registrado em C-03/S-02, KDoc corrigido |
| Testes existentes assumirem assinaturas antigas das telas | Parâmetros novos com valor padrão onde possível; atualização explícita de `ExemplarDetailScreenTest` (o único teste de tela de exemplar existente) |

## Verificação (comandos previstos)

```bash
export JAVA_HOME="C:/Users/wellingtonrodrigues/AppData/Local/Android/Jdk/jdk-17.0.21+9"

# domínio + dados
./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest

# unidade do app + lint
./gradlew :app:testDebugUnitTest :app:lintDebug

# instrumentados das telas alteradas (emulador API 36)
./gradlew :app:connectedDebugAndroidTest --tests '*UniqueExemplar*'
./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarStability*'
./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarListNegotiation*'
./gradlew :app:connectedDebugAndroidTest --tests '*ExemplarDetailNegotiation*'
./gradlew :app:connectedDebugAndroidTest --tests '*Duplicates*'

# suíte instrumentada completa (regressão)
./gradlew :app:connectedDebugAndroidTest

# classes de janela
adb shell wm size 1080x2400; adb shell wm density 420     # ≈ 411 × 914 dp
adb shell wm size 2625x2400; adb shell wm density 420     # ≈ 1.000 × 914 dp
adb shell settings put system font_scale 2.0
```

## Decisões que a implementação deve respeitar

- Nenhuma migração; esquema permanece **v4** (D-06, NFR-501).
- `setOffer` e `open` **não** mudam de assinatura nem de regra; a ampliação é de acesso (D-07/D-08).
- A rota continua `duplicates/{exemplarId}/negotiation` (D-05, C-03).
- Duplicados mantém consulta, agrupamento e contagem (FR-503/FR-527).
- O aviso aparece na disponibilidade e na negociação, nunca em `Indisponível`, nunca com 2+ `OWNED`, nunca com negociação ativa (FR-515 revisado).

## Próximos passos

1. `/speckit-tasks` detalha `tasks.md` por fase, com critério de aceite e teste por tarefa (G4).
2. `/speckit-analyze` valida Constituição ↔ spec ↔ plano ↔ tarefas (G5).
3. `/speckit-implement` começa pela Fase 1 (domínio e dados), sem tocar em interface.

**Dependências para iniciar**: nenhuma pendente. A spec está aprovada e o plano não abre decisão nova.
