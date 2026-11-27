# Research: negociar qualquer exemplar da coleção

**Feature**: `007-negociar-qualquer-exemplar` | **Data**: 2026-11-27

Investigação feita **antes** de escrever o plano, lendo o código em `main`. Cada achado tem arquivo e símbolo; nada aqui é suposição sobre o que "deveria" existir.

## 1. Onde a duplicidade está acoplada à negociação

**Achado**: só em dois lugares.

| Lugar | Evidência | Consequência |
|---|---|---|
| Consulta de listagem | `DuplicateDao.observeDuplicates()` — subconsulta `GROUP BY variant_id HAVING COUNT(*) > 1` | Define quem aparece em Duplicados; **não** restringe gravação |
| Caminho de interface | `AppNavHost`: único `onOpenNegotiation` vem de `DuplicatesScreen` | É o que impede negociar exemplar único |

**Não existe** vínculo no banco: `duplicate_offer.exemplar_id` é único e aceita qualquer exemplar; `NegotiationRepositoryImpl.open` valida apenas `status = OWNED` e ausência de negociação ativa.

**Decisão**: a feature é de **acesso + apresentação**. Nenhuma migração, nenhum índice novo.

**Alternativas descartadas**: (a) mover a negociação para uma tabela própria de "itens negociáveis" — exigiria migração e duplicaria dado (Art. I.12/II.5); (b) relaxar a consulta de duplicados — destruiria a identificação de repetidos (FR-503).

## 2. Reaproveitar a folha de disponibilidade ou criar outra

**Achado**: `DuplicateActionsSheet` já contém tudo o que RN-004 pede (código do exemplar, quatro opções, condição, valor, observações, **Salvar**), mas recebe `DuplicateExemplar` — que só existe dentro de um grupo de duplicados.

**Decisão**: extrair para `ExemplarOfferSheet(subject: OfferSheetSubject, …)`, com o mínimo que a folha usa (código, título, condição do exemplar, oferta atual). `DuplicateExemplar` continua existindo para Duplicados.

**Alternativas descartadas**: (a) construir `DuplicateExemplar` sintético na ficha/lista — obrigaria a inventar `manufacturerName`/`modelName` e acoplaria as telas ao modelo de duplicados; (b) folha nova para a ficha — duplicação com a mesma regra (proibido por II.5) e dois lugares para corrigir.

## 3. Onde colocar o aviso de exemplar único (D-01/D-02 revisado)

**Achado**: RN-003 fala em alertar "quando o usuário tenta negociar". A spec aprovada define **dois** momentos: confirmar a disponibilidade para venda/troca/ambas e abrir a negociação.

**Decisão**: um único diálogo (`UniqueExemplarDialog`) disparado por uma regra pura (`UniqueExemplarPolicy`) a partir das duas telas. Nunca em `Indisponível`, nunca com 2+ `OWNED`, nunca com negociação ativa.

**Alternativas descartadas**:
- Avisar apenas na abertura da negociação — foi a D-01 original; revisada porque quem marca disponibilidade e só negocia semanas depois não veria o alerta no momento em que a intenção nasce.
- Avisar em toda edição de disponibilidade, inclusive `Indisponível` — alerta sem consequência, ruído; o usuário desaprende o aviso.
- "Não mostrar de novo" persistido — esconderia decisão patrimonial (Art. I.6).

## 4. Quando nada é gravado

**Achado**: `setOffer` é transacional e o `open` também; nenhum dos dois grava fora da chamada.

**Decisão**: o aviso é exibido **antes** de chamar o repositório. O ViewModel guarda a intenção (`pendingOffer`) em memória e só executa no **Continuar**. **Cancelar** limpa a intenção; o banco não é tocado. Teste verifica o banco depois do cancelamento (SC-505).

**Alternativa descartada**: gravar e "desfazer" no cancelar — produziria oferta fantasma e auditoria falsa.

## 5. Como detectar "exemplar único" sem nova regra de banco

**Achado**: `ExemplarDao.countByVariant(variantId)` já existe (`COUNT(*) WHERE variant_id = ? AND status = 'OWNED'`) e é usado pelo cadastro rápido (FR-039).

**Decisão**: usar essa contagem, reconsultada no instante da ação. Nenhum `SELECT ... HAVING` novo, nenhuma subconsulta duplicada da de duplicados.

**Alternativa descartada**: derivar de `observeByVariant(...).size` na ficha — o Flow da ficha não observa a variante inteira; assinar a lista inteira só para contar seria custo sem ganho.

## 6. Leituras por linha (N+1) na lista de exemplares

**Achado**: `ExemplarListViewModel` já combina `observeByVariant` + `observeActive` + `observePrimaryPathsByVariant`, cada uma **uma** consulta por variante. Não há padrão de consulta por linha no projeto.

**Decisão**: somar duas leituras por variante — `offersByVariant` e `activeNegotiationsByVariant` — e juntar em memória por `exemplar_id`. Custo: duas consultas indexadas por variante, independentes do número de unidades.

**Alternativa descartada**: `offerFor`/`observeByExemplar` dentro do `items {}` — N+1, proibido por NFR-503.

## 7. Pré-selecionar o tipo de negociação sem mudar a rota

**Achado**: `NegotiationViewModel` lê `exemplarId` do `SavedStateHandle` e já resolve o exemplar; `type` começa em `SALE`. A rota `duplicates/{exemplarId}/negotiation` não tem outros argumentos.

**Decisão**: o ViewModel consulta a oferta do exemplar e define o tipo inicial (`SALE`/`TRADE`; `BOTH`/ausente mantêm o padrão). Zero mudança de navegação (FR-523).

**Alternativas descartadas**: (a) argumento de rota `?tipo=` — amplia contrato de navegação e quebra deep links/testes existentes; (b) pré-selecionar na tela de origem e passar por `savedStateHandle` — estado escondido, difícil de testar.

## 8. Onde hospedar o aviso antes de navegar

**Achado**: a navegação é feita só no `AppNavHost`; as telas recebem callbacks.

**Decisão**: o aviso é hospedado pela tela de origem (ficha/lista), porque só ela conhece a contagem e o estado; o `Continuar` chama o callback que já navega. A tela de negociação não ganha lógica de aviso — ela pode ser aberta sem aviso hoje (Duplicados) e continua assim.

**Alternativa descartada**: mostrar o aviso dentro da tela de negociação na primeira composição — depois de navegar, **Cancelar** teria de desempilhar a rota, criando efeito colateral e histórico inconsistente.

## 9. O que **não** muda

| Item | Evidência |
|---|---|
| Esquema Room v4 | `AppDatabase` com versão 4; nenhuma entidade nova |
| Consulta de duplicados | `DuplicateDao.observeDuplicates` intocada |
| `setOffer`/`open`/conclusão/cancelamento | `DuplicateRepositoryImpl`/`NegotiationRepositoryImpl` sem alteração de regra |
| Rota de negociação | `Destination.Negotiation` igual |
| Backup | tabelas já exportadas; nenhum formato novo |
| Dependências | nenhuma nova |

## 10. ADR

**Achado**: os ADRs existentes (`docs/adr-005-window-size-class.md`, `docs/adr-006-preferencia-aparencia.md`) registram escolhas de **tecnologia/persistência**.

**Decisão**: nenhum ADR novo. Não há tecnologia, dependência, esquema ou contrato externo novo; as escolhas relevantes já estão registradas como D-01…D-10 na spec e R-01…R-10 aqui. Registrar ADR para reuso de componente seria burocracia sem alternativa tecnológica a comparar.

## 11. Riscos confirmados na leitura do código

- `DuplicateActionsSheet` guarda estado por `remember(selected.exemplar.id)`; ao trocar a chave, a seleção reinicia. Na extração, a chave passa a ser `subject.internalCode` — manter o mesmo comportamento e **não** recriar a folha ao abrir o aviso.
- `ExemplarDetailScreenTest` chama `ExemplarDetailContent` com a lista completa de callbacks; acrescentar parâmetros exige atualizar o teste (assinatura, não asserção).
- `NegotiationDao` está declarado em `CollectionsDao.kt` — a consulta nova entra nesse arquivo, não em um DAO fictício.
