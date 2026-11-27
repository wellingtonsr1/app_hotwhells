# Contrato: origens, rotas e aviso

**Feature**: `007-negociar-qualquer-exemplar` | **Tipo**: contrato de navegação | **Data**: 2026-11-27

## 1. Rotas (nenhuma nova)

| Destino | Rota | Argumento | Situação |
|---|---|---|---|
| `Destination.ExemplarList` | `catalog/variant/{variantId}/exemplars` | `variantId` | existente |
| `Destination.ExemplarDetail` | `exemplar/{exemplarId}` | `exemplarId` | existente |
| `Destination.Duplicates` | `duplicates` | — | existente |
| `Destination.Negotiation` | `duplicates/{exemplarId}/negotiation` | `exemplarId` | existente, **nome mantido** (D-05/C-03) |

FR-523: a feature não cria rota e não altera argumento. A ampliação é apenas de **quem chama** a rota de negociação.

## 2. Origens e o que cada uma faz

| Origem | Ação | Efeito |
|---|---|---|
| Lista de exemplares (linha) | **Disponibilidade** | abre `ExemplarOfferSheet` na própria tela |
| Lista de exemplares (linha) | **Negociar** | aviso (se único) → `navigate(Negotiation.routeFor(id))` |
| Ficha do exemplar | **Disponibilidade** | abre `ExemplarOfferSheet` na própria tela |
| Ficha do exemplar | **Negociar** | aviso (se único) → `navigate(Negotiation.routeFor(id))` |
| Duplicados (linha) | **Disponibilidade** / **Negociar** | como hoje; sem aviso |

Assinaturas de tela:

```kotlin
ExemplarListScreen(
    onAddExemplar: (variantId: String) -> Unit,
    onOpenExemplar: (exemplarId: String) -> Unit,
    onOpenNegotiation: (exemplarId: String) -> Unit,   // novo
)

ExemplarDetailScreen(
    onDone: () -> Unit,
    onOpenNegotiation: (exemplarId: String) -> Unit,   // novo
)
```

## 3. Aviso antes de navegar (FR-515b)

| Passo | Estado | Banco |
|---|---|---|
| Toque em **Negociar** | avaliar `warnsOnNegotiation` | intocado |
| Aviso aberto | **Continuar** / **Cancelar** | intocado |
| **Cancelar** | diálogo fecha; tela de origem inalterada (mesma rolagem/seleção) | intocado |
| **Continuar** | `navigate(Negotiation.routeFor(id))` | intocado (a negociação só nasce no **Abrir negociação** da tela de destino) |

O aviso **não** é mostrado dentro da tela de negociação: ali **Cancelar** exigiria desempilhar a rota (research §8).

## 4. Pré-seleção de tipo (FR-514)

A tela de negociação resolve o tipo inicial sozinha, a partir de `DuplicateRepository.offerFor(exemplarId)`:

| Oferta | Tipo inicial |
|---|---|
| `SALE` | `SALE` |
| `TRADE` | `TRADE` |
| `BOTH`, `UNAVAILABLE`, ausente | padrão atual (`SALE`) |
| com negociação ativa | inalterado (a tela abre no estado ativo) |

Continua editável em todos os casos. Nenhum argumento de rota é acrescentado.

## 5. Volta ao destino

- Concluir venda, concluir troca ou cancelar: a tela de negociação **permanece** com o histórico atualizado (comportamento atual).
- Voltar da negociação: retorna à origem exata (lista, ficha ou Duplicados), que reemite estado pelos `Flow` — sem cópia local (FR-505).
- Venda concluída: o exemplar deixa o acervo ativo; a lista de exemplares e o detalhe refletem o novo status na próxima emissão; Duplicados recalcula o grupo pela consulta existente.
