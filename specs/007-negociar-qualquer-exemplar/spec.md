# Feature Specification: Negociar qualquer exemplar da coleção

**Feature Branch**: `007-negociar-qualquer-exemplar`

**Created**: 2026-11-27

**Status**: Draft — aguardando revisão e aprovação

**Input**: User description: "Ampliar as funcionalidades de venda e troca do Diecast Collector: hoje só é possível negociar pelo fluxo Catálogo → Duplicados → Negociar, e a tela Duplicados lista apenas variantes com mais de um exemplar OWNED. A evolução deve permitir disponibilizar para venda ou troca qualquer exemplar da coleção, mesmo quando for a única unidade da variante, mantendo o módulo Duplicados existente e a regra de duplicidade (mesma variante com mais de um exemplar OWNED)."

**Constituição aplicável**: `.specify/memory/constitution.md` v1.3.0 — Art. I.1–I.3 (modelo/variante/exemplar e duplicidade por variante), I.6–I.9 (sem perda silenciosa, arquivamento, desconhecido é `NULL`, trilha auditável), II.1–II.6 (modularidade, regra de negócio no domínio), IV.1/IV.3/IV.4/IV.9 (idioma, distinção exemplar × modelo, feedback, confirmação de ação destrutiva), V.1–V.5 (verificação, banco real, migração testada), VI.7/VI.10 (exportação e offline), VII.2–VII.14 (classes de janela, consistência, testes adaptativos), R.8 (migração não destrutiva), A1 (instância pessoal única).

> **Âmbito desta especificação**: independência entre *duplicidade* e *negociação*. Nenhum requisito cria marketplace, publicação pública, comunicação entre usuários ou sincronização. Nenhum requisito altera o modelo de dados, o esquema Room, o `applicationId`, o nome comercial ou a tela Duplicados como recurso de identificação de repetidos.

## Contexto

### O que existe hoje (verificado no código em 2026-11-27)

| Camada | Situação real | Evidência |
|---|---|---|
| Disponibilidade | `duplicate_offer` tem **uma linha por exemplar** (`exemplar_id` único), com `availability` (`TRADE`, `SALE`, `BOTH`, `UNAVAILABLE`), condição, valor pretendido e observações | `core/data/.../db/entity/DuplicateOfferEntity.kt` |
| Regra de gravação | `setOffer` recusa apenas exemplar que **não** esteja `OWNED`; **não verifica duplicidade** | `core/data/.../repository/DuplicateRepositories.kt` (`DuplicateRepositoryImpl.setOffer`) |
| Listagem de duplicados | `observeDuplicates` filtra `status = 'OWNED'` **e** `variant_id IN (… GROUP BY variant_id HAVING COUNT(*) > 1)`; agrupa por variante | `core/data/.../db/dao/DuplicateDao.kt` |
| Negociação | `negotiation.exemplar_id` é FK para o exemplar; `open` recusa exemplar não `OWNED` e recusa segunda negociação ativa | `DuplicateRepositories.kt` (`NegotiationRepositoryImpl.open`), `NegotiationEntity.kt` |
| Ponto de entrada da negociação | rota `duplicates/{exemplarId}/negotiation`, alcançada **somente** pelo botão **Negociar** da tela Duplicados | `navigation/AppNavHost.kt`, `feature/duplicates/DuplicatesScreen.kt` |
| Detalhe do exemplar | mostra condição, embalagem, notas, localização, aquisição, avaliações, fotos e arquivar/excluir — **nenhuma ação de disponibilidade ou negociação** | `feature/catalog/ExemplarDetailScreen.kt`, `ExemplarDetailViewModel.kt` |
| Lista de exemplares da variante | cartões com código/condição que abrem o detalhe; **nenhum indicador de disponibilidade** | `feature/catalog/ExemplarListScreen.kt` |
| Estados do exemplar | `OWNED`, `SOLD`, `TRADED`, `ARCHIVED` | `core/domain/.../model/Enums.kt` |
| Estados da negociação | `OPEN`, `RESERVED`, `CLOSED`, `CANCELLED`; unicidade de ativa verificada na transação (não há índice parcial único) | `Enums.kt`, `NegotiationEntity.kt`, `NegotiationRepositoryTest` |
| Esquema | Room **v4**; `duplicate_offer` entrou na v2 junto com `negotiation.received_exemplar_id` | `core/data/.../db/AppDatabase.kt` |

### Conclusão do diagnóstico

O vínculo entre negociação e duplicidade **não existe no modelo de dados**: existe na *consulta de listagem* (que é a definição correta de duplicado) e no *único caminho de interface* até a negociação. Ou seja, a ampliação é de **superfície e de requisito**, não de esquema.

Isso responde às perguntas da análise técnica do pedido:

1. **A estrutura atual já permite negociar qualquer exemplar?** Sim, na camada de dados e domínio: `setOffer` e `open` só exigem `status = OWNED` e ausência de negociação ativa. Não há verificação de quantidade de exemplares da variante.
2. **Existe vínculo obrigatório entre negociação e duplicidade?** Não no banco (FK de `negotiation` aponta para `exemplar`, não para grupo; `duplicate_offer` idem). O vínculo é de interface (única rota de entrada) e de documentação (FR-066, manual §21/§22 e `inventario.md` falam em "exemplar duplicado").
3. **O modelo identifica individualmente o exemplar negociado?** Sim: `negotiation.exemplar_id` e `duplicate_offer.exemplar_id`, ambos com unicidade/índice próprio.
4. **Quais estados existem antes, durante e depois?** Exemplar: `OWNED` antes e durante; `SOLD`/`TRADED` depois (ou `ARCHIVED` por decisão independente). Negociação: `OPEN` ou `RESERVED` durante; `CLOSED`/`CANCELLED` depois. Disponibilidade: `TRADE`/`SALE`/`BOTH`/`UNAVAILABLE`, independente dos anteriores.
5. **A negociação altera o status do exemplar?** Só na conclusão: venda → `SOLD`, troca → `TRADED`. Abrir e cancelar **não** alteram `exemplar.status`; a reserva vive na negociação (decisão registrada no código, FR-072).
6. **Como são tratadas venda, troca e cancelamento?** Venda exige valor recebido e grava valor/data/contraparte no histórico do exemplar. Troca marca o negociado como `TRADED`, vincula opcionalmente um exemplar recebido (que precisa estar `OWNED` e ser diferente do negociado) e registra evento. Cancelamento mantém o registro, grava evento e não mexe no status nem na oferta.
7. **É possível ampliar sem alterar o banco?** **Sim.** Nenhuma coluna, tabela, índice ou migração é necessária. Entram no máximo **três consultas de leitura** — oferta de um exemplar, ofertas dos exemplares de uma variante e negociações ativas de uma variante —, cada uma por exemplar ou por variante, para evitar consulta por linha (ver §Impacto na arquitetura).
8. **Se houver migração, qual seria?** **Nenhuma.** O esquema v4 já atende. Renomear `duplicate_offer`/`DuplicateOffer` para refletir o novo escopo não traz valor ao usuário e exigiria migração com risco — decisão: manter os nomes e registrar a dívida de nomenclatura (ver C-03).

## Objetivos e escopo

**Objetivos**

1. Permitir marcar para **venda**, **troca**, **ambas** ou **indisponível** qualquer exemplar com `status = OWNED`, independentemente de a variante ter um ou vários exemplares.
2. Abrir a negociação de qualquer exemplar a partir do **detalhe do exemplar** e da **lista de exemplares da variante**, mantendo o caminho por **Duplicados**.
3. Alertar, sem bloquear, quando o exemplar for a **única unidade `OWNED` da variante** no momento de abrir a negociação.
4. Manter a tela **Duplicados** exatamente com o significado atual (mesma variante, mais de um exemplar `OWNED`), como recurso independente de identificação de repetidos.
5. Garantir que o estado do exemplar (disponibilidade e negociação ativa) seja o mesmo em todos os pontos de acesso.

**Dentro do escopo**

- Ações **Disponibilidade** e **Negociar** no detalhe do exemplar e na lista de exemplares da variante.
- Reaproveitamento da folha de disponibilidade e da tela de negociação já existentes.
- Aviso de preservação da coleção para exemplar único.
- Indicador textual de disponibilidade/negociação nas superfícies de exemplar.
- Ajuste de textos, testes e documentação.

**Fora do escopo**

- Tela nova de "central de negociações" (avaliação em §4.4 — decisão: adiada, ver **S-01**).
- Marketplace, anúncio público, compartilhamento, chat, integração com serviços externos.
- Alteração do modelo de dados, do esquema Room, de migrações, do formato de backup ou do `applicationId`.
- Mudança nas regras de conclusão de venda/troca e de cancelamento.
- Movimento de exemplar entre coleções, exclusão física, edição em massa.
- Renomear rotas, tabelas, classes ou chaves existentes (dívida registrada em C-03).
- Reorganizar a tela Duplicados ou o detalhe do exemplar além da adição das ações.

### Avaliação do ponto 4.4 do pedido — central de negociações

Inventário: **não existe hoje** nenhuma tela que liste exemplares por estado de negociação. As telas existentes são Catálogo (variantes), Duplicados (variantes repetidas), Lista de exemplares (unidades de uma variante) e Negociação (um exemplar). Uma central exigiria **rota nova**, agregando todos os exemplares `OWNED` com oferta ou negociação, com filtros por estado (disponíveis para venda/troca/ambas, reservados, concluídos, cancelados).

**Decisão**: **fora do escopo desta feature**, por três motivos registrados: (a) o objetivo central — negociar qualquer exemplar — é atingido sem ela; (b) uma central é uma tela nova com filtros e estados, com custo e risco de regressão maiores do que a correção de acesso; (c) o pedido proíbe criar recursos não comprovados como parte do escopo. A descoberta é atendida no que já existe (§FR-520: indicador nas listas e no detalhe). Fica registrada como próxima especificação candidata (**S-01**), sem requisito nesta.

## Decisões e suposições

| ID | Decisão | Justificativa |
|---|---|---|
| D-01 | O aviso de exemplar único aparece em **dois momentos**: ao confirmar a disponibilidade para venda/troca/ambas e ao abrir a negociação | RN-003 alerta quando o usuário tenta negociar o único exemplar; disponibilizar é a primeira manifestação dessa intenção e é onde nasce a expectativa de desapego. Avisar só na abertura pegaria o usuário já decidido; avisar só ao disponibilizar esconderia o risco de quem marca a disponibilidade e volta meses depois para negociar. Nos dois momentos o aviso informa sem impedir |
| D-02 | O aviso é **diálogo não bloqueante** (Continuar / Cancelar), mostrado a cada disponibilização e a cada abertura de negociação de exemplar único | RN-003 exige alerta sem impedir; não persistir o "não mostrar de novo" evita esconder decisão patrimonial, e repetir nas duas intenções mantém a consequência visível sem travar o fluxo |
| D-03 | A folha de disponibilidade existente **é** a confirmação exigida por RN-004; o aviso de exemplar único é **alerta de consequência**, não uma segunda confirmação da oferta | A folha mostra o exemplar, as quatro opções, condição, valor e observações e exige **Salvar**. O diálogo de FR-515 não coleta nem revisa nenhum dado da oferta: apenas informa a saída do acervo ativo na conclusão e pede ciência (**Continuar**/**Cancelar**). Por isso não duplica a confirmação de RN-004 — e, sem ele, a consequência de vender a única unidade ficaria invisível |
| D-04 | Disponibilidade pode ser editada mesmo com negociação ativa; o estado exibido continua sendo **Em negociação** | A exclusividade é da negociação (FR-068), não da oferta; proibir a edição criaria regra nova sem necessidade |
| D-05 | A rota continua `duplicates/{exemplarId}/negotiation` | Mudar a rota não traz valor, quebraria testes e capturas existentes; dívida registrada em C-03 |
| D-06 | Nenhuma migração Room | O esquema v4 já suporta; qualquer mudança seria desnecessária (Art. I.10 / R.8) |
| D-07 | O tipo da negociação é pré-selecionado pela disponibilidade quando ela for `SALE` ou `TRADE` | Reduz erro do usuário sem criar regra: continua editável |
| D-08 | Exemplar `SOLD`, `TRADED` ou `ARCHIVED` não oferece ações de disponibilidade/negociação | Já é a regra do domínio (`setOffer`/`open` recusam); a interface passa a refletir isso |
| D-09 | A lista de exemplares da variante passa a mostrar a disponibilidade | É o "pela coleção" do pedido no nível de unidade, sem tela nova |
| D-10 | Nenhuma dependência nova | Tudo é Compose/Room/Hilt já presentes |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Disponibilizar a única unidade de uma variante para venda ou troca (Priority: P1)

O colecionador decidiu desapegar de um carro que tem em unidade única. Abre a ficha do exemplar, toca em **Disponibilidade**, escolhe **Venda**, informa condição e valor pretendido e salva. O exemplar passa a aparecer como disponível na própria ficha e na lista de exemplares da variante, sem que ele apareça na tela Duplicados.

**Why this priority**: é o defeito central que a feature corrige — hoje esse usuário não tem caminho nenhum para registrar a intenção de venda.

**Independent Test**: instalação limpa, um exemplar em uma variante, abrir o detalhe, marcar **Venda**, confirmar o aviso de preservação ao salvar, e verificar que o estado aparece na ficha e na lista e que Duplicados continua vazio.

**Acceptance Scenarios**:

1. **Given** uma variante com exatamente um exemplar `OWNED` e nenhuma oferta registrada, **When** o usuário abre o detalhe e toca em **Disponibilidade**, **Then** a folha abre com as quatro opções e **Venda** pré-selecionada (padrão atual preservado), enquanto a ficha e a lista exibem **Indisponível** para aquele exemplar.
2. **Given** a folha aberta em um exemplar único, **When** o usuário escolhe **Venda** e toca em **Salvar**, **Then** o aviso de preservação aparece antes de gravar; com **Continuar**, a ficha e a lista de exemplares passam a mostrar **Venda** para aquele exemplar, sem exigir segundo exemplar na variante.
3. **Given** o aviso aberto a partir da folha, **When** o usuário escolhe **Cancelar**, **Then** a folha permanece aberta com a seleção feita e nada é gravado; a disponibilidade anterior continua valendo.
4. **Given** o mesmo exemplar, **When** o usuário volta à tela Duplicados, **Then** ele **não** aparece (a variante tem um único exemplar `OWNED`).
5. **Given** uma variante com três exemplares, **When** o usuário marca apenas o segundo como **Troca**, **Then** só esse exemplar fica disponível, os outros dois continuam **Indisponível** e o aviso **não** aparece.

---

### User Story 2 - Abrir a negociação a partir do exemplar e ser avisado sobre a coleção (Priority: P1)

Na ficha do exemplar, o usuário toca em **Negociar**. Se aquele exemplar é a única unidade da variante, aparece um aviso explicando que, concluída a operação, o exemplar pode deixar de fazer parte da coleção ativa. Ele pode continuar ou cancelar; continuando, preenche contraparte, canal e valor e abre a negociação, como já fazia pelos Duplicados.

**Why this priority**: sem o aviso, a ampliação aumentaria o risco de o colecionador tirar do acervo a única unidade por engano — exatamente o que a constituição chama de perda insubstituível.

**Independent Test**: com uma variante de um exemplar, tocar **Negociar**, ver o aviso, cancelar e confirmar que nada foi criado; repetir escolhendo **Continuar** e abrir a negociação.

**Acceptance Scenarios**:

1. **Given** um exemplar único `OWNED` e sem negociação ativa, **When** o usuário toca em **Negociar**, **Then** o aviso de preservação aparece antes da tela de negociação, com **Continuar** e **Cancelar** — ainda que o usuário já o tenha visto ao disponibilizar o exemplar.
2. **Given** o aviso aberto, **When** o usuário escolhe **Cancelar**, **Then** volta à ficha sem criar negociação, sem alterar disponibilidade e sem alterar status.
3. **Given** o aviso aberto, **When** o usuário escolhe **Continuar**, **Then** a tela de negociação abre para aquele exemplar com tipo pré-selecionado pela disponibilidade.
4. **Given** uma variante com dois exemplares `OWNED`, **When** o usuário toca em **Negociar**, **Then** o aviso **não** aparece e a negociação abre direto.
5. **Given** um exemplar com negociação ativa, **When** o usuário toca em **Negociar**, **Then** a tela abre mostrando a negociação ativa e o histórico, sem oferecer uma segunda abertura.

---

### User Story 3 - Continuar usando Duplicados como identificação de repetidos (Priority: P2)

O usuário abre **Catálogo → Duplicados** e encontra exatamente os mesmos grupos de antes: variantes com mais de uma unidade. Marca disponibilidade e negocia ali também, como sempre fez.

**Why this priority**: o módulo não pode regredir nem mudar de significado; é a base de comparação para provar que a ampliação não quebrou a regra de duplicidade.

**Independent Test**: com duas variantes (uma com 1 exemplar e outra com 2), confirmar que só a segunda aparece, com contagem correta, e que as ações continuam funcionando.

**Acceptance Scenarios**:

1. **Given** uma variante com dois exemplares `OWNED`, **When** o usuário abre Duplicados, **Then** o grupo aparece com "2 exemplares nesta variante" e cada unidade com **Disponibilidade** e **Negociar**.
2. **Given** duas variantes diferentes do mesmo modelo (cores distintas), **When** o usuário abre Duplicados, **Then** nenhuma das duas aparece por esse motivo.
3. **Given** um exemplar de variante repetida vendido, **When** o usuário abre Duplicados, **Then** o grupo passa a ter uma unidade e **deixa de aparecer** enquanto a variante tiver um único exemplar `OWNED`.
4. **Given** a tela Duplicados, **When** o usuário negocia por ali, **Then** o comportamento é idêntico ao do caminho pela ficha (mesma tela, mesmas regras).

---

### User Story 4 - Ver o estado da negociação em qualquer ponto de acesso (Priority: P2)

O usuário percorre a lista de exemplares da variante, a ficha e os Duplicados e vê sempre a mesma informação: o que está disponível, para quê e o que está em negociação. Um exemplar reservado nunca aparece como disponível numa tela e reservado em outra.

**Why this priority**: RN-010 — informação contraditória entre telas destrói a confiança no registro e é a falha mais provável de uma ampliação com três entradas.

**Independent Test**: marcar disponibilidade pela ficha, abrir a lista de exemplares e os Duplicados e comparar os rótulos; abrir negociação e conferir **Em negociação** nas três superfícies.

**Acceptance Scenarios**:

1. **Given** um exemplar com disponibilidade **Troca**, **When** o usuário abre lista, ficha e Duplicados, **Then** as três mostram **Troca** com o mesmo rótulo e o mesmo valor pretendido, quando houver.
2. **Given** um exemplar com negociação ativa, **When** o usuário observa as três superfícies, **Then** todas indicam **Em negociação**, além da disponibilidade registrada.
3. **Given** a ficha aberta com disponibilidade salva em outra superfície, **When** o usuário retorna à ficha, **Then** o valor exibido é o atual, sem recarregar manualmente.
4. **Given** fonte do sistema em 200% e janela compacta, **When** as superfícies são abertas, **Then** os rótulos de estado ficam inteiros e legíveis.

---

### User Story 5 - Encerrar o ciclo: venda, troca e cancelamento (Priority: P3)

Concluída a negociação, o exemplar sai do acervo ativo por venda ou troca, ou volta ao estado anterior se a negociação for cancelada; o histórico permanece consultável em qualquer caso. A disponibilidade continua sendo um registro separado e não apaga nada.

**Why this priority**: são as regras já existentes que a ampliação não pode quebrar; entram como regressão explícita, não como novidade.

**Independent Test**: repetir os fluxos de venda, troca e cancelamento a partir do detalhe (e não só dos Duplicados) e comparar com o comportamento anterior.

**Acceptance Scenarios**:

1. **Given** uma venda concluída a partir da ficha de um exemplar único, **When** o usuário volta ao catálogo, **Then** o exemplar não conta mais no acervo ativo e o histórico da negociação continua acessível pelos registros financeiros/histórico do exemplar.
2. **Given** uma troca concluída, **When** o usuário consulta a negociação, **Then** o exemplar negociado está `TRADED` e o recebido, se informado, continua `OWNED` e vinculado ao evento.
3. **Given** uma negociação cancelada, **When** o usuário reabre a ficha, **Then** o exemplar continua `OWNED`, a disponibilidade anterior está preservada e o cancelamento consta no histórico.
4. **Given** um exemplar marcado **Indisponível** depois de já ter negociações encerradas, **When** o usuário abre o histórico, **Then** as negociações anteriores continuam listadas.

---

### Edge Cases

- **Exemplar arquivado**: ficha mostra o selo de arquivado e **não** oferece disponibilidade nem negociação; se já existia oferta, ela permanece gravada e volta a valer se o exemplar for reativado.
- **Exemplar `SOLD`/`TRADED`**: nenhuma ação de negociação; disponibilidade não editável.
- **Aviso de exemplar único com negociação ativa**: não aparece, nem na edição da disponibilidade nem ao tocar em **Negociar** (a negociação já existe e o aviso foi dado na abertura); a superfície mostra o estado ativo.
- **Cancelar no aviso disparado pela disponibilidade**: a folha continua aberta com a seleção feita, nada é gravado e a ficha/lista mantêm o estado anterior.
- **Exemplar único marcado Indisponível**: nenhum aviso — a operação reduz, não amplia, o risco de saída do acervo.
- **Último exemplar vendido de uma variante com dois**: o segundo passa a ser único; a próxima negociação dele mostra o aviso.
- **Disponibilidade `Indisponível` com negociação ativa**: permitido salvar; a superfície continua mostrando **Em negociação** e o exemplar continua bloqueado para outra negociação.
- **Falha ao salvar disponibilidade**: mensagem em pt-BR, estado anterior mantido, nenhuma tela perde conteúdo.
- **Duas janelas (freeform/dobra) com a mesma ficha**: alterar a disponibilidade numa janela reflete na outra, sem cópia de estado; a folha ou o aviso abertos na outra janela não gravam nada por conta própria e, ao fechar essa folha ou esse aviso, a superfície volta a exibir o estado atual do repositório.
- **Rotação/processo encerrado no meio do aviso**: nenhuma oferta ou negociação é criada pela metade (sem escrita parcial); o aviso é descartado e a folha reabre no padrão, sem restaurar a intenção pendente.
- **Restauração de backup antigo** (sem ofertas para variantes únicas): nenhuma oferta é inventada; exemplares aparecem **Indisponível**, como hoje.
- **Duplicados com exemplar em negociação**: continua mostrando **Em negociação** e a contagem não muda por causa da oferta.
- **Lista de exemplares com muitos itens**: nenhuma consulta por linha; rolagem preservada.
- **Fonte ampliada + janela compacta**: folha de disponibilidade rola e mantém **Salvar**/**Cancelar** alcançáveis.

## Requirements *(mandatory)*

> Numeração a partir de FR-501 para não colidir com as especificações 001–006 (a 005 usa FR-3xx e a 006 usa FR-4xx).

### Independência entre duplicidade e negociação

- **FR-501**: O aplicativo MUST permitir marcar para **venda**, **troca**, **venda e troca** ou **indisponível** qualquer exemplar com `status = OWNED`, independentemente de a variante ter um ou vários exemplares `OWNED`.
- **FR-502**: O aplicativo MUST oferecer as ações **Disponibilidade** e **Negociar** em três pontos de acesso: (a) **lista de exemplares da variante**; (b) **detalhe do exemplar**; (c) **tela Duplicados** (mantida).
- **FR-503**: A tela **Duplicados** MUST continuar listando **somente** variantes com mais de um exemplar `OWNED`, agrupadas por variante, com a contagem do grupo; o critério MUST permanecer por `variant_id`, nunca por modelo.
- **FR-504**: Um exemplar cuja variante tenha um único exemplar `OWNED` MUST NOT aparecer na tela Duplicados em razão da disponibilidade ou da negociação.
- **FR-505**: A disponibilidade e a negociação ativa MUST ter uma única fonte de verdade; todas as superfícies MUST exibir o mesmo estado no mesmo momento, sem cópia local divergente.
- **FR-506**: Exemplar com `status` diferente de `OWNED` MUST NOT oferecer criação ou edição de disponibilidade nem abertura de negociação; a ficha MUST explicar o motivo.

### Disponibilidade

- **FR-507**: A disponibilidade MUST ter exatamente um valor por exemplar — `TRADE`, `SALE`, `BOTH` ou `UNAVAILABLE` —, gravada por upsert, sem criar segunda linha para o mesmo exemplar (preserva FR-066/FR-067).
- **FR-508**: A marcação MUST ocorrer na folha de disponibilidade existente, que apresenta o código do exemplar, a disponibilidade atual, as quatro opções, condição física, valor pretendido e observações, e só grava após **Salvar** explícito — essa folha é a confirmação exigida por RN-004.
- **FR-509**: Condição, valor pretendido e observações MUST continuar opcionais, com `NULL` quando não informados (Art. I.9), e MUST ser preservados entre edições.
- **FR-510**: Marcar **Indisponível** MUST manter o registro da oferta e MUST NOT apagar negociação, evento ou histórico (RN-007).
- **FR-511**: A disponibilidade MUST poder ser editada enquanto existir negociação ativa; nesse caso a superfície MUST exibir **Em negociação** como estado dominante, junto da disponibilidade registrada, e o exemplar MUST NOT entrar em outra negociação (FR-068 preservado).
- **FR-512**: Falha ao gravar a disponibilidade MUST manter o valor anterior, informar em pt-BR e não fechar a folha sem confirmação.

### Negociação

- **FR-513**: Abrir negociação MUST exigir `status = OWNED` e ausência de negociação `OPEN`/`RESERVED` para o mesmo exemplar, com mensagens em pt-BR distintas para cada recusa (FR-068 preservado).
- **FR-514**: O tipo da negociação MUST ser pré-selecionado a partir da disponibilidade quando ela for `SALE` ou `TRADE`, permanecendo editável; com `BOTH` ou `UNAVAILABLE` mantém-se o padrão atual.
- **FR-515**: Para um exemplar que seja a **única unidade `OWNED` da variante**, o aplicativo MUST exibir aviso de preservação da coleção — explicando que, concluída a operação, o exemplar pode deixar de integrar a coleção ativa, com **Continuar** e **Cancelar** — em **dois momentos**: (a) antes de gravar a oferta de **Venda**, **Troca** ou **Venda e troca** confirmada na folha de disponibilidade; (b) antes de abrir a negociação. O aviso MUST NOT impedir a operação, MUST NOT ser exibido para variantes com dois ou mais exemplares `OWNED`, MUST NOT ser exibido ao marcar **Indisponível** e MUST NOT se repetir quando já existir negociação ativa para o exemplar (RN-003). **Cancelar** MUST NOT gravar nada e MUST devolver o usuário ao ponto de origem com o estado anterior intacto.
- **FR-516**: Concluir venda MUST exigir valor recebido, marcar o exemplar como `SOLD`, retirá-lo do acervo ativo e mantê-lo consultável no histórico, como hoje (FR-045/FR-070 preservados).
- **FR-517**: Concluir troca MUST marcar o exemplar negociado como `TRADED`, vincular o exemplar recebido quando informado (que MUST estar `OWNED` e ser diferente do negociado) e registrar o evento, sem alterar o status do recebido (FR-071 preservado).
- **FR-518**: Cancelar negociação MUST manter o registro, gravar o evento e devolver o exemplar ao estado anterior, sem alterar `exemplar.status` nem a disponibilidade (FR-072 preservado).
- **FR-519**: Nenhuma alteração de disponibilidade, conclusão ou cancelamento MUST apagar histórico de negociação ou eventos (FR-073, RN-007).

### Apresentação, consistência e acessibilidade

- **FR-520**: Lista de exemplares, detalhe e Duplicados MUST indicar o estado com os mesmos rótulos pt-BR já usados (**Troca**, **Venda**, **Troca e venda**, **Indisponível**, **Em negociação**) e MUST NOT depender apenas de cor para distinguir os estados (IV.6, VII.9).
- **FR-521**: As superfícies alteradas MUST declarar e cumprir o comportamento por classe de janela: **compacta** (< 600 dp) em coluna única com ações visíveis; **média** (600–839 dp) e **expandida** (≥ 840 dp) com a mesma hierarquia, sem esticar botões pela largura total e sem rolagem horizontal; altura compacta (paisagem) com conteúdo rolável e ações alcançáveis (VII.2, VII.5, VII.7).
- **FR-522**: Alvos de toque MUST ser ≥ 48 × 48 dp, com rótulo semântico e ordem de foco coerente; com fonte do sistema em 200% os rótulos de estado e as ações MUST permanecer inteiros e alcançáveis (VII.10).
- **FR-523**: A feature MUST NOT criar rota de tela nova nem alterar rotas existentes; a negociação continua acessível pela rota atual, agora a partir de três origens.
- **FR-524**: O ponto de entrada **Duplicados** no Catálogo MUST permanecer visível e inalterado.

### Preservação e escopo

- **FR-525**: A implementação MUST NOT alterar o modelo de domínio, o esquema Room (v4), migrações, o formato do pacote de backup, o `applicationId`, o `app_name` ou a identidade visual do produto (Art. I.10, IP.4).
- **FR-526**: A feature MUST NOT introduzir marketplace, anúncio público, compartilhamento, comunicação entre usuários, sincronização ou telemetria.
- **FR-527**: Os fluxos existentes de catálogo, coleções, Duplicados, negociação, financeiro e histórico MUST continuar funcionando com o mesmo comportamento; nenhum teste MUST ser removido, ignorado ou afrouxado (V.4).
- **FR-528**: A documentação MUST ser atualizada ao final da implementação: `specs/001-diecast-collector/spec.md` (FR-065/FR-066), `specs/002-manual-usuario/inventario.md`, `specs/002-manual-usuario/inventario-regras.md` e `docs/manual/MANUAL_USUARIO_COMPLETO.md` (§21, §22 e capítulo do detalhe do exemplar), deixando explícito que **duplicidade não é requisito para negociação** e ensinando a negociar exemplar único, uma unidade de duplicata, vários exemplares, exemplar disponível para venda e para troca.

### Requisitos não funcionais

- **NFR-501**: Nenhuma migração de banco MUST ser necessária; a versão do esquema permanece **v4** e o teste de migração existente MUST continuar verde.
- **NFR-502**: Nenhuma dependência nova MUST ser adicionada (R.4, R.5).
- **NFR-503**: A lista de exemplares MUST obter a disponibilidade de todas as unidades da variante em **uma** consulta e as negociações ativas em **outra**, sem consulta por linha; a ficha MUST obter oferta e negociação ativa com, no máximo, **duas** consultas (uma por assunto). Nenhuma consulta de duplicidade MUST ser alterada para acomodar essas leituras.
- **NFR-504**: A abertura da lista de exemplares e da ficha MUST continuar abaixo de **1 segundo** com 10.000 exemplares e 50.000 fotos no aparelho (meta provisória de R.7, medida no aparelho de referência).
- **NFR-505**: Salvar a disponibilidade MUST refletir o novo estado nas superfícies já abertas em menos de **300 ms** após a confirmação, sem recarregar a tela.
- **NFR-506**: Tudo MUST funcionar offline, incluindo marcar disponibilidade e abrir negociação (VI.10).
- **NFR-507**: A cobertura MUST incluir testes de domínio/dados (regra de disponibilidade para exemplar único e para repetido, unicidade de negociação ativa), instrumentados de interface (três pontos de acesso, aviso de exemplar único, consistência entre telas) e a regressão da suíte existente (V.2, V.10).
- **NFR-508**: A arquitetura MUST ser preservada: regra de negócio no domínio/repositório, `core:ui` sem dependência de `core:data`/`app`, ViewModels expondo estado e intenção (II.1–II.6, VII.13).

### Key Entities

- **Exemplar**: unidade física; atributos relevantes aqui são `id`, `internalCode`, `variantId`, `condition` e `status` (`OWNED`, `SOLD`, `TRADED`, `ARCHIVED`).
- **Disponibilidade (`duplicate_offer`)**: decisão atual de oferta de **um** exemplar (`TRADE`/`SALE`/`BOTH`/`UNAVAILABLE`), com condição, valor pretendido e observações; uma linha por exemplar; não é histórico.
- **Negociação**: tentativa de venda ou troca de um exemplar (`SALE`/`TRADE`; `OPEN`/`RESERVED`/`CLOSED`/`CANCELLED`), com contraparte, canal, datas, valor e exemplar recebido (na troca).
- **Evento de negociação**: registro cronológico append-only de abertura, reserva, conclusão e cancelamento; nunca apagado.
- **Grupo de duplicados**: conjunto derivado, não persistido, de exemplares `OWNED` da mesma variante quando há mais de um.

## Regras de negócio

- **BR-501 (RN-001)**: Exemplar único pode ser disponibilizado e negociado; a quantidade de exemplares da variante não é pré-condição.
- **BR-502 (RN-002)**: Com mais de um exemplar da variante, a disponibilidade é decidida **por unidade**, nunca pelo grupo.
- **BR-503 (RN-003)**: Exemplar único avisa antes de ser disponibilizado para venda/troca e antes de abrir negociação, explicando a saída do acervo ativo na conclusão; o aviso não impede e não aparece para `Indisponível` nem com negociação ativa.
- **BR-504 (RN-004)**: Disponibilizar exige confirmação explícita na folha (exemplar, tipo de oferta, dados adicionais e condições).
- **BR-505 (RN-005)**: Um exemplar não pode ter duas negociações ativas; a segunda tentativa é recusada com mensagem acionável.
- **BR-506 (RN-006)**: O usuário pode retirar a disponibilidade; com negociação ativa, a retirada é permitida mas não encerra a negociação nem libera o exemplar.
- **BR-507 (RN-007)**: Histórico de negociação e eventos é append-only; mudar disponibilidade, concluir ou cancelar nunca apaga registros.
- **BR-508 (RN-008)**: Venda concluída → `SOLD`; troca concluída → `TRADED`; cancelamento não altera `exemplar.status`; arquivamento continua sendo decisão separada.
- **BR-509 (RN-009)**: Duplicidade continua sendo "mesma variante com mais de um exemplar `OWNED`"; disponibilidade e negociação não criam nem removem duplicatas.
- **BR-510 (RN-010)**: O estado exibido é único em todas as superfícies; divergência entre telas é defeito, não escolha de apresentação.
- **BR-511**: A disponibilidade é intenção atual, não histórico: sobrescrever a oferta não gera trilha própria, mas as negociações já realizadas permanecem.
- **BR-512**: Exemplar fora de `OWNED` não entra em negociação nem tem disponibilidade editada; após venda/troca ele permanece consultável no histórico.

## Fluxos de navegação

| Origem | Caminho | Ação | Destino |
|---|---|---|---|
| Catálogo → variante → **exemplares** | `catalog/variant/{variantId}/exemplars` | **Disponibilidade** na linha | Folha de disponibilidade; aviso (se único) ao salvar |
| Catálogo → variante → **exemplares** | idem | **Negociar** na linha | Aviso (se único) → `duplicates/{exemplarId}/negotiation` |
| Catálogo → variante → exemplares → cartão | `exemplar/{exemplarId}` | **Disponibilidade** | Folha de disponibilidade; aviso (se único) ao salvar |
| `exemplar/{exemplarId}` | ficha | **Negociar** | Aviso (se único) → tela de negociação |
| Catálogo → **Duplicados** | `duplicates` | **Disponibilidade** / **Negociar** | Folha (sem aviso: variante já repetida) / tela de negociação |
| Tela de negociação | `duplicates/{exemplarId}/negotiation` | Concluir venda/troca, cancelar | Permanece, com histórico atualizado |

Regras de fluxo: o aviso de exemplar único é uma etapa **antes da gravação da disponibilidade** e **antes da tela de negociação**, não cria rota nova; **Cancelar** no aviso volta exatamente à origem (a folha continua aberta, sem gravar); a folha de disponibilidade nunca navega.

## Success Criteria *(mandatory)*

- **SC-501**: Em 100% das tentativas, um exemplar único `OWNED` pode ser marcado como **Venda**, **Troca** ou **Ambas** a partir da ficha e da lista de exemplares, e o estado aparece nas duas superfícies ao voltar.
- **SC-502**: Em 100% das tentativas, um exemplar único **não** aparece na tela Duplicados por causa da disponibilidade ou da negociação.
- **SC-503**: A tela Duplicados reproduz exatamente os grupos do comportamento atual para as mesmas massas de dados (contagem e composição idênticas), verificado por comparação antes/depois em três massas: 1 exemplar, 2 exemplares e 3 exemplares na mesma variante.
- **SC-504**: O aviso de preservação aparece em 100% das confirmações de disponibilidade (**Venda**, **Troca**, **Venda e troca**) e em 100% das aberturas de negociação de exemplar único, e em 0% das variantes com 2+ exemplares `OWNED`, das marcações **Indisponível** e das edições com negociação ativa.
- **SC-505**: Cancelar no aviso não grava disponibilidade, não cria negociação e não altera status, verificado no banco após a ação nos dois momentos em que o aviso aparece; interromper o aviso (recriar a janela, dispensar o diálogo ou encerrar o processo) tem o mesmo efeito — nada é gravado e a superfície volta a exibir o estado do repositório.
- **SC-506**: A segunda tentativa de negociação do mesmo exemplar é recusada em 100% dos casos, com mensagem em pt-BR, e o número de negociações ativas por exemplar permanece ≤ 1.
- **SC-507**: A venda concluída remove o exemplar do acervo ativo e mantém o histórico consultável em 100% dos casos; a troca e o cancelamento mantêm o comportamento atual.
- **SC-508**: As três superfícies exibem o mesmo estado em 100% das combinações verificadas (sem oferta, `TRADE`, `SALE`, `BOTH`, `UNAVAILABLE`, com negociação ativa).
- **SC-509**: Marcar disponibilidade é percebido como imediato: estado atualizado em < 300 ms, sem recarregar a tela (NFR-505).
- **SC-510**: A abertura da lista de exemplares e da ficha permanece abaixo de 1 s com 10.000 exemplares, medido com comando reproduzível.
- **SC-511**: A suíte existente permanece verde (0 falhas; nenhum teste removido, ignorado ou afrouxado) e os novos testes passam.
- **SC-512**: `git diff` comprova que `core/domain` mudou **apenas** com a regra pura do aviso (`UniqueExemplarPolicy`) e com os contratos de leitura previstos — nenhuma regra de disponibilidade, abertura, conclusão ou cancelamento alterada — e que não houve alteração em esquema Room, migrações, `applicationId` e `app_name`.
- **SC-513**: As superfícies alteradas passam em janela **compacta** (≥ 360 dp), **média** (700 dp) e **expandida** (1.000 dp), em retrato e paisagem, sem corte, sobreposição ou rolagem horizontal, e com fonte 200%.

## Impacto na arquitetura e necessidade de migração

**Módulos afetados**

| Módulo | Alteração prevista | Tipo |
|---|---|---|
| `core:domain` | Uma **regra pura** nova (`UniqueExemplarPolicy`, sem Android) decide o aviso de exemplar único; `DuplicateRepository` ganha duas leituras (`observeOffer`, `observeOffersByVariant`) e `NegotiationRepository` uma (`observeActiveByVariant`). `setOffer`, `open`, conclusão e cancelamento não mudam. | Regra + contrato de leitura |
| `core:data` | **Três consultas de leitura**: oferta de um exemplar; ofertas de todos os exemplares de uma variante; negociações ativas de uma variante. Nenhuma entidade, coluna, índice ou migração. | Leitura |
| `app` (duplicados) | Extrair a folha de disponibilidade e o diálogo de aviso para componentes reutilizáveis; manter a tela e o ViewModel atuais. | Apresentação |
| `app` (catálogo) | `ExemplarListScreen`/`ViewModel`: expor disponibilidade e negociação ativa por exemplar e emitir as duas ações. `ExemplarDetailScreen`/`ViewModel`: seção **Negociação** com estado e ações. | Apresentação |
| `app` (navegação) | `AppNavHost` passa os callbacks de **Negociar** para a lista de exemplares e o detalhe, reutilizando a rota existente. | Navegação |
| `app` (recursos) | Strings pt-BR novas (aviso, rótulos de ação na ficha/lista, motivos de bloqueio). | Recursos |
| Documentação | Spec 001 (FR-065/066), `inventario.md`, `inventario-regras.md`, manual §21/§22 e capítulo do detalhe. | Documentação |

**Banco de dados**: **nenhuma migração**. Justificativa: as tabelas `duplicate_offer` (uma linha por exemplar, `exemplar_id` único) e `negotiation` (FK para `exemplar`, índices por `exemplar_id`/`status`) já expressam tudo o que a feature precisa. Criar tabela ou coluna nova seria duplicação de dado (Art. I.12/II.5). As três consultas de leitura usam os índices que já existem (`duplicate_offer.exemplar_id`, `negotiation.exemplar_id`/`status`); nenhum índice novo entra sem medição que o justifique (NFR-504).

**Compatibilidade com instalações existentes**: nenhum dado é reinterpretado. Ofertas já gravadas continuam válidas; exemplares sem oferta aparecem como **Indisponível**; negociações existentes permanecem com o mesmo status e histórico; o backup atual (que já inclui `duplicate_offer` e `negotiation`) não muda de formato.

## Contradições encontradas entre documentação e código

| ID | Contradição | Evidência | Encaminhamento |
|---|---|---|---|
| **C-01** | FR-066 diz "marcar cada exemplar **duplicado**", mas a gravação aceita qualquer exemplar `OWNED` | `specs/001-diecast-collector/spec.md` FR-066 × `DuplicateRepositoryImpl.setOffer` | **Resolvida por esta spec**: FR-501/FR-528; texto da 001 atualizado na implementação |
| **C-02** | Manual e inventário dizem que a negociação "pertence a um exemplar duplicado" | `docs/manual/MANUAL_USUARIO_COMPLETO.md` (tabela de entidades, linha 410), `specs/002-manual-usuario/inventario.md` (rotas 18) | **Resolvida por esta spec**: FR-528 |
| **C-03** | Nomes `DuplicateOffer`/`duplicate_offer`/rota `duplicates/{id}/negotiation` pressupõem duplicidade | `DuplicateOfferEntity.kt`, `DuplicateModels.kt`, `AppNavHost.kt` | **Mantida como dívida consciente** (D-05): renomear exigiria migração e mudança de rota sem valor ao usuário. Registrado como melhoria futura (S-02) |
| **C-04** | `docs/continua.md` afirma "ainda sem tela: coleções, vendas/trocas separadas do investido (T097)" | `docs/continua.md` linhas 25 e 107 × `T097` fechada em `specs/001-diecast-collector/tasks.md` (2026-11-19) e `FinanceScreenTest` | **Fora do escopo desta feature**; registrado para correção documental independente |
| **C-05** | KDoc de `DuplicateAvailability`/`DuplicateOffer` fala em "exemplar repetido" | `core/domain/.../model/DuplicateModels.kt` | **Resolvida na implementação** por ajuste de KDoc (sem mudança de tipo) |

## Riscos e possíveis regressões

| Risco | Impacto | Mitigação prevista |
|---|---|---|
| Regressão na tela Duplicados (contagem, agrupamento, ações) | Perda do recurso de identificação de repetidos | Nenhuma alteração na consulta; teste de comparação com massas fixas (SC-503) |
| Divergência de estado entre três superfícies | Usuário vê informação contraditória | Fonte única por consulta reativa; teste de consistência (SC-508) |
| Aviso de exemplar único irritante ou ignorado | Usuário desativa o alerta mentalmente e vende a única unidade | Aviso apenas nos dois momentos de intenção (disponibilizar e negociar), com texto curto que explica a consequência; sem opção "não mostrar de novo" e sem repetição com negociação ativa |
| Consulta por linha na lista de exemplares | Lista lenta com muitas unidades | Duas consultas por variante — ofertas e negociações ativas —, nenhuma por linha (NFR-503), e medição (SC-510) |
| Ações novas na ficha competindo com edição de condição/localização | Confusão de fluxo | Seção própria e curta, sem mover o que já existe |
| Exemplar arquivado com oferta antiga | Oferta reaparece ao reativar | Comportamento documentado no caso de borda; sem exclusão de dado (Art. I.7) |
| Nomes herdados (`DuplicateOffer`) confundirem manutenção | Dívida técnica | Registro em C-03 e KDoc ajustado |
| Textos novos fora do padrão pt-BR de colecionador | IV.1 | Revisão de strings na implementação e no manual |
| Backups antigos sem ofertas | Usuário estranha "Indisponível" | Comportamento já atual; documentado nos casos de borda |

**Dependências**: nenhuma dependência externa ou nova biblioteca. Depende das telas/rotas existentes (`ExemplarList`, `ExemplarDetail`, `Duplicates`, `Negotiation`), do repositório de duplicados e do repositório de negociação.

## Matriz de rastreabilidade

| Cenário do pedido | Regra | Requisitos | Critérios | Verificação prevista |
|---|---|---|---|---|
| 1 — Exemplar único | RN-001 / BR-501 | FR-501, FR-502, FR-507, FR-508, FR-515 | SC-501, SC-504 | Unitário de dados (`setOffer` em variante de 1 exemplar) + instrumentado na ficha e na lista, incluindo o aviso ao salvar disponibilidade |
| 2 — Exemplar único com aviso | RN-003 / BR-503 | FR-515 | SC-504, SC-505 | Instrumentado nos dois momentos: aviso ao salvar disponibilidade e ao abrir negociação; Cancelar não grava, Continuar prossegue |
| 3 — Duplicatas (3 unidades) | RN-002 / BR-502 | FR-502, FR-507, FR-509 | SC-501 | Instrumentado: marcar só a segunda unidade e conferir as outras duas |
| 4 — Variantes diferentes do mesmo modelo | Art. I.3 / BR-509 | FR-503, FR-504 | SC-503 | Unitário de consulta (`DuplicateQueryTest`) |
| 5 — Negociação simultânea | RN-005 / BR-505 | FR-513 | SC-506 | Unitário de repositório (já existe) + instrumentado de mensagem |
| 6 — Cancelamento | RN-006/007 / BR-506, BR-507 | FR-510, FR-518, FR-519 | SC-507 | Unitário de repositório + instrumentado |
| 7 — Venda concluída | RN-008 / BR-508 | FR-516 | SC-507 | Unitário de repositório + instrumentado (estado e financeiro) |
| 8 — Troca concluída | RN-008 / BR-508 | FR-517 | SC-507 | Unitário de repositório + instrumentado |
| 9 — Duplicados preservados | RN-009 / BR-509 | FR-503, FR-504, FR-524 | SC-503 | `DuplicateQueryTest` + `DuplicatesFlowTest` |
| 10 — Regressão | FR-527 | FR-525–FR-527 | SC-511, SC-512 | Suíte completa + `git diff` |
| Extra — Consistência entre telas | RN-010 / BR-510 | FR-505, FR-520 | SC-508 | Instrumentado nas três superfícies |
| Extra — Exemplar arquivado/vendido | BR-512 | FR-506 | SC-512 | Instrumentado de bloqueio |
| Extra — Acessibilidade e responsividade | VII.2/VII.10 | FR-521, FR-522 | SC-513 | Instrumentado em 360/700/1.000 dp + fonte 200% |
| Extra — Sem migração | Art. I.10 / R.8 | NFR-501 | SC-512 | `MigrationTest` inalterado + `git diff` do esquema |

**Inventário de testes existentes que precisam continuar verdes**: `core/data/.../DuplicateQueryTest`, `core/data/.../NegotiationRepositoryTest`, `core/data/.../MigrationTest`, `app/src/androidTest/.../DuplicatesFlowTest`, `DuplicatesResponsiveTest`, `ExemplarDetailScreenTest`, `FinanceScreenTest`, `AppShellResponsiveTest`, além da suíte completa de unidade e instrumentada.

## Plano preliminar de implementação *(detalhamento na fase `/speckit-plan`)*

1. **Leitura de dados**: três consultas em lote — oferta de um exemplar, ofertas dos exemplares da variante e negociações ativas da variante —, com `countByVariant` reutilizado para a contagem de `OWNED`; nenhuma mudança de esquema.
2. **Contratos de apresentação**: extrair a folha de disponibilidade e o diálogo de aviso para componentes compartilhados, sem alterar a tela Duplicados.
3. **Ficha do exemplar**: seção **Negociação** com estado (disponibilidade + **Em negociação**), **Disponibilidade** e **Negociar**; bloqueio explicado para `SOLD`/`TRADED`/`ARCHIVED`.
4. **Lista de exemplares**: indicador de estado por unidade e as duas ações por linha, com a mesma folha.
5. **Navegação**: `AppNavHost` liga as novas origens à rota de negociação existente, com o aviso de exemplar único antes de navegar; na folha de disponibilidade o mesmo aviso entra antes de gravar.
6. **Aviso de preservação**: diálogo com **Continuar**/**Cancelar**, disparado quando a variante tiver um único exemplar `OWNED`, tanto na confirmação da disponibilidade quanto antes de abrir a negociação; cancelar preserva o estado anterior.
7. **Strings e acessibilidade**: rótulos pt-BR, semântica, alvos ≥ 48 dp, fonte 200%.
8. **Testes**: dados (oferta única × repetida, unicidade ativa), interface (três origens, aviso, consistência, bloqueios), responsividade (compacta/média/expandida) e regressão completa.
9. **Documentação**: Spec 001 (FR-065/FR-066), `inventario.md`, `inventario-regras.md`, manual §21/§22 e capítulo do detalhe; nota de que duplicidade não é requisito para negociar.

## Itens registrados para depois (fora do escopo)

- **S-01**: central de negociações com filtros por estado (disponíveis para venda/troca/ambas, em negociação, concluídas, canceladas) — avaliar em spec própria, com rota e agregador próprios.
- **S-02**: renomear `DuplicateOffer`/`duplicate_offer`/rota `duplicates/{id}/negotiation` para refletir o escopo ampliado — exige migração e mudança de rota, sem valor direto ao usuário.
- **S-03**: correção de `docs/continua.md` quanto ao estado de T097 (C-04).

## Assumptions

- Instância pessoal única no aparelho (A1): não há permissões, contas ou visibilidade entre usuários a definir.
- "Exemplar único" significa exatamente **um exemplar `OWNED`** na variante no momento da confirmação (da disponibilidade ou da abertura da negociação); `SOLD`, `TRADED` e `ARCHIVED` não contam.
- A disponibilidade é intenção atual e reversível; a consequência patrimonial acontece na conclusão da venda ou da troca.
- O preço pretendido é estimativa do colecionador, não preço de mercado (IV.8); a interface mantém a redação atual.
- Os caminhos e rótulos atuais (Duplicados, Negociar, Disponibilidade, Troca, Venda) são mantidos; a ampliação adiciona origens, não vocabulário novo.
- O usuário continua sem poder negociar exemplar fora do acervo ativo, e essa restrição é comunicada quando a superfície permitir a tentativa.
- Nenhuma alteração de schema, backup ou `applicationId` será necessária; se a implementação descobrir o contrário, a spec MUST ser revista antes do código (G2/G3).

## Critérios de conclusão

1. Qualquer exemplar `OWNED` pode ser disponibilizado para venda, troca, ambas ou indisponível, a partir de três origens, sem exigir duplicata.
2. Exemplar único recebe aviso de preservação ao confirmar disponibilidade para venda/troca e antes da negociação, pode continuar nos dois momentos, e o aviso não aparece para variantes com 2+ exemplares, para **Indisponível** nem com negociação ativa.
3. A tela Duplicados mantém exatamente a regra e o comportamento atuais (mesma variante, mais de um `OWNED`).
4. Estado consistente nas três superfícies, verificado por teste.
5. Regras de negociação ativa, conclusão de venda/troca, cancelamento e histórico preservadas sem alteração.
6. Nenhuma migração Room, nenhuma dependência nova, nenhuma rota nova, `applicationId` e identidade intactos.
7. Suíte existente verde e novos testes cobrindo os dez cenários do pedido e os extras.
8. Documentação atualizada, deixando explícito que duplicidade não é requisito para negociar.
