# Contrato: folha de disponibilidade compartilhada

**Feature**: `007-negociar-qualquer-exemplar` | **Tipo**: contrato de interface (Compose) | **Data**: 2026-11-27

Contrato de apresentação, não de API externa: o aplicativo é offline e não expõe serviço. Documenta o que a folha promete a **três** chamadores (Duplicados, ficha, lista), para que a extração não mude comportamento de ninguém.

## 1. Assunto da folha

```kotlin
public data class OfferSheetSubject(
    val internalCode: String,      // "HW-000123"
    val title: String,             // "Fabricante · Modelo · Cor"; "" quando não houver
    val condition: Condition,      // condição registrada do exemplar (padrão da proposta)
    val offer: DuplicateOffer?,    // null = nunca marcado (aparece como Indisponível)
)
```

Sem `exemplarId` no assunto: a folha **não** grava nada; ela devolve a intenção para quem a abriu.

## 2. Composable

```kotlin
@Composable
public fun ExemplarOfferSheet(
    subject: OfferSheetSubject,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (DuplicateAvailability, Condition?, String, String) -> Unit,
)
```

`onSave` recebe `availability`, `condition`, `priceText` **cru** (pt-BR, como digitado) e `notes`. Conversão de valor e validação ficam no ViewModel chamador, que é quem conhece o repositório (II.2).

## 3. O que a folha garante

| Garantia | Detalhe |
|---|---|
| Conteúdo | Código do exemplar, linha de título, as **quatro** opções (`Troca`, `Venda`, `Troca e venda`, `Indisponível`), condição, valor pretendido, observações |
| Estado inicial | Oferta atual do exemplar; sem oferta, `Indisponível` como estado exibido e `Venda` como opção pré-selecionada (comportamento atual preservado) |
| Gravação | **Somente** pelo `onSave`, disparado por **Salvar** explícito; esta folha é a confirmação de RN-004 (D-03) |
| Cancelar | Fecha sem gravar; nenhum estado do chamador muda |
| Erro do chamador | O chamador mantém a folha aberta e envia mensagem; os valores digitados permanecem |
| Rolagem | Coluna vertical rolável; **Salvar**/**Cancelar** alcançáveis em paisagem e com fonte 200% |
| Semântica | Cada opção é um `FilterChip` com rótulo pt-BR; alvos ≥ 48 dp |
| Sem navegação | A folha nunca navega nem abre rota |

## 4. Estados do chamador (obrigações)

```text
Fechada ──abrir──▶ Editando ──Salvar (sem aviso)──▶ Gravando ──ok──▶ Fechada + mensagem
                      │                              └──erro──▶ Editando + mensagem
                      └──Salvar (com aviso)──▶ Avisando ──Continuar──▶ Gravando
                                                   └──Cancelar──▶ Editando (seleção preservada)
```

1. O chamador **não** pode gravar antes do `onSave`.
2. Com aviso pendente, **nada** é gravado até **Continuar** (FR-515, SC-505).
3. **Cancelar** no aviso fecha apenas o diálogo; a folha continua montada e mantém a seleção (a chave do `remember` é `subject.internalCode`).
4. O aviso é um `Dialog` sobre a folha; fechar a folha descarta a intenção pendente.
5. Duplicados **não** usa o aviso: a variante do grupo tem 2+ `OWNED` por construção.

## 5. Compatibilidade com o comportamento atual

| Situação | Antes | Depois |
|---|---|---|
| Duplicados → Disponibilidade | `DuplicateActionsSheet(selected = DuplicateExemplar)` | `ExemplarOfferSheet(subject)` com os mesmos campos exibidos |
| Salvar oferta | `DuplicatesViewModel.onSaveOffer` | idêntico (mesma assinatura e mesma validação) |
| Mensagens | `availability_updated` / `duplicates_error_save` | idênticas |
| Ordem visual | título, código, oferta, condição, valor, observações, Salvar/Cancelar | idêntica |

Qualquer diferença visual entre as três origens é defeito (FR-520).
