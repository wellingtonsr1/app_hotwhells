# G2 — Rascunho de esclarecimento: identidade visual e aparência (Specs 003 e 004)

**Status**: **aprovado e aplicado em 2026-11-26** — CL-003-01, CL-003-02 e CL-004-01 nas Specs 003/004 e no contrato do cartão
**Data**: 2026-11-26
**Origem**: análise de consistência da Spec 006 (`spec.md` §Impacto; achados H1/M7)
**Constituição**: G2 (esclarecimentos), G.4 (conflito deve ser corrigido, não virar dívida), VII.9 (identidade preservada), IV.6 (contraste ≥ 4,5:1), V.4 (não afrouxar teste)
**Feature relacionada**: [`006-preferencia-aparencia`](./spec.md)

## 1. Por que este esclarecimento existe

A Spec 003 (FR-013 e `contracts/collection-card.md`) e a Spec 004 (premissa em *Assumptions*) afirmam que a interface **segue o tema escuro**. A Spec 006 permite que o usuário escolha **Sistema**, **Claro** ou **Escuro** e, no modo Claro, aplica os tokens claros já existentes. Lida ao pé da letra, a redação atual das Specs 003/004 impede o modo Claro e transforma um requisito aprovado em contradição com a entrega seguinte.

O conflito é **de redação, não de produto**: o modo Escuro permanece exatamente como hoje (mesmos tokens, tipografia, formas e hierarquia). O que falta é declarar que "identidade visual" não é sinônimo de "paleta escura obrigatória em todos os modos".

## 2. Opções submetidas ao G2

| Opção | Descrição | Consequência | Recomendação |
|---|---|---|---|
| **A** | Identidade = conjunto de tokens, tipografia, formas e hierarquia. No Escuro (e em Sistema com Android escuro) vale a identidade escura atual; no Claro valem os tokens claros da mesma paleta. | Nenhuma mudança visual no modo escuro; modo claro passa a ter respaldo documental; Spec 006 fica consistente. | **Recomendada** |
| B | Manter a identidade exclusivamente escura e retirar o modo Claro do escopo da Spec 006. | Contraria o pedido do responsável (três opções) e a expectativa de FR-001; reduz a entrega. | Não recomendada |
| C | Manter a identidade escura também no modo Claro (fundo preto em "Claro"). | Torna as duas opções indistinguíveis e viola a expectativa do usuário; risco de contraste/legibilidade. | Rejeitada |

A opção **A** é a única que preserva simultaneamente a Spec 003, a Spec 004, a Spec 005 (que já prevê "tema escuro e claro") e a Spec 006.

## 3. Texto proposto para a Spec 003

### 3.1 Registro de clarificação (novo bloco, após `## Contexto`)

> ## Clarifications
>
> ### Session 2026-11-26
>
> - Q: A FR-013 exige "tema escuro, fundo preto, cartões em cinza escuro" como identidade visual. A Spec 006 introduz a escolha entre Sistema, Claro e Escuro. A identidade deixa de valer no modo Claro? → A: **A identidade é o conjunto de tokens, tipografia, formas e hierarquia, não a paleta escura como obrigação permanente.** No modo **Escuro** (e em **Sistema** com o Android escuro), a FR-013 MUST ser cumprida exatamente como está: fundo `InkBlack`, cartões em `Graphite`/`SmokyGrey`, textos `Chalk`, destaques `FlameRedDark`/`HotOrangeLight`. No modo **Claro**, MUST usar os tokens claros da mesma paleta (`Chalk`, `PureWhite`, `InkBlack`, `MutedGrey`, `FlameRed`, `HotOrange`), com a mesma tipografia, formas, espaçamentos e hierarquia — **sem cor nova**.
> - Q: A FR-013 precisa ser renumerada ou reescrita? → A: **Não.** O número é preservado e o texto recebe apenas o esclarecimento de modo; nenhum critério de aceite (SC-001–SC-005) muda.
>
> **Consequências registradas**: nenhuma mudança de layout, conteúdo, acessibilidade ou desempenho no modo escuro; o modo claro passa a ser explicitamente permitido, com contraste verificado pela Spec 006 (FR-422/FR-423).

### 3.2 FR-013 — texto atual

> **FR-013**: A aparência MUST respeitar a identidade visual atual: tema escuro, fundo
> preto, cartões em cinza escuro, textos claros, destaques em vermelho/coral,
> tipografia e componentes Material 3, navegação inferior inalterada.

### 3.3 FR-013 — texto proposto (substituição integral, mesmo número)

> **FR-013**: A aparência MUST preservar a identidade visual do produto — paleta, tipografia, formas, espaçamentos e componentes Material 3 — em todas as opções de aparência suportadas.
>
> - **No modo Escuro** (e em **Sistema** quando o Android está escuro): fundo preto (`InkBlack`), cartões em cinza escuro (`Graphite`/`SmokyGrey`), textos claros (`Chalk`), destaques em vermelho/coral (`FlameRedDark`/`HotOrangeLight`), navegação inalterada.
> - **No modo Claro**: os tokens claros da mesma paleta (`Chalk` de fundo, `PureWhite` de superfície, `InkBlack` de texto, `MutedGrey` secundário, `FlameRed`/`HotOrange` de destaque), com a mesma tipografia, formas, espaçamentos, hierarquia e navegação.
>
> Nenhuma cor fora dos tokens existentes MUST ser introduzida por este esclarecimento, e a troca de aparência MUST NOT alterar layout, conteúdo ou regra de negócio.

### 3.4 Contrato `contracts/collection-card.md` — §Identidade visual

**Texto atual**

> - Tema escuro (`InkBlack` de fundo, `SmokyGrey` nas superfícies, destaque `FlameRedDark`).
> - Tipografia Material 3: título em `titleMedium`, subtítulo em `bodySmall` com
>   `onSurfaceVariant`.
> - Nenhuma cor, fonte ou espaçamento novo fora dos tokens existentes.

**Texto proposto**

> - Segue a aparência escolhida pelo usuário (Spec 006):
>   - **Escuro / Sistema escuro**: `InkBlack` de fundo, `SmokyGrey` nas superfícies, destaque `FlameRedDark`.
>   - **Claro / Sistema claro**: `Chalk` de fundo, `PureWhite` nas superfícies, destaque `FlameRed`.
> - Tipografia Material 3: título em `titleMedium`, subtítulo em `bodySmall` com `onSurfaceVariant`.
> - Nenhuma cor, fonte ou espaçamento novo fora dos tokens existentes; a miniatura não recebe tingimento do tema.

E, na tabela de elementos, a linha do cartão:

**Atual**: `| Cartão | Fundo de superfície do tema escuro, cantos arredondados, largura total |`

**Proposto**: `| Cartão | Fundo de superfície do tema em uso (claro ou escuro), cantos arredondados, largura total |`

### 3.5 O que **não** muda na Spec 003

- FR-001 a FR-012, FR-014 e FR-015: intocadas.
- SC-001 a SC-005: intocados (nenhum mede cor de tema; SC-005 trata de foto inválida).
- Contrato: estrutura, estados da miniatura, interações e critérios de conformidade 1–6 permanecem.
- Nenhum teste existente precisa mudar: os testes da 003 afirmam estrutura, textos e acessibilidade, não a paleta escura.

## 4. Texto proposto para a Spec 004

### 4.1 Registro de clarificação (novo bloco, após `## Assumptions` ou antes dele)

> ## Clarifications
>
> ### Session 2026-11-26
>
> - Q: A premissa "A interface segue o tema escuro" continua válida depois da escolha de aparência da Spec 006? → A: **A interface segue a aparência escolhida pelo usuário (Sistema, Claro ou Escuro); a área de edição fotográfica permanece neutra em todos os modos.** No modo Escuro, nada muda em relação ao comportamento atual. As sobreposições sobre a foto (ícones, rótulos, *scrims* e alças de recorte) MUST manter legibilidade independentemente do tema, e o tema MUST NOT tingir a imagem exibida ou salva.

### 4.2 Premissa — texto atual

> - A interface segue o tema escuro e os componentes Material 3 já usados; a documentação técnica da funcionalidade é entregue em `docs/`.

### 4.3 Premissa — texto proposto

> - A interface segue a aparência escolhida pelo usuário (Spec 006, FR-401–FR-409) e os componentes Material 3 já usados; no modo Escuro o comportamento atual é preservado. A área de edição permanece neutra e a imagem não é tingida pelo tema em nenhum modo (Spec 006, FR-419/FR-420). A documentação técnica da funcionalidade é entregue em `docs/`.

### 4.4 O que **não** muda na Spec 004

- FR-201 a FR-226: intocadas.
- Nenhuma mudança de fluxo, arquivo, parâmetro de edição ou teste.
- A exigência de sobreposições legíveis já está coberta por FR-422/FR-423 da 006 e pelos testes `PhotoEditorScreenTest`/`PhotoReviewDialogTest`.

## 5. Impacto e rastreabilidade

| Item | Efeito |
|---|---|
| Spec 003 FR-013 | Redação esclarecida; nenhum comportamento do modo escuro alterado |
| Contrato do cartão | Passa a declarar superfície por modo; estrutura e testes intactos |
| Spec 004 (premissa) | Passa a declarar aparência escolhida + neutralidade da edição |
| Spec 005 (FR-326/FR-328 e caso de borda "tema escuro e claro") | Passa a ter base explícita nas specs de origem |
| Spec 006 (FR-417–FR-420) | Ganha o respaldo documental que hoje só existe como recomendação |
| Testes existentes | Nenhum precisa ser alterado; suíte permanece verde |
| Backlog | Nenhuma tarefa nova; as tarefas T401–T424 da 006 já cobrem a execução |

**Compatibilidade**: no modo Escuro, o resultado visual é idêntico ao atual — a alteração é exclusivamente de redação. Por isso a aprovação **não bloqueia** a implementação da 006, mas é necessária para fechar o G5.

## 6. Registro de aprovação

| ID | Documento | Alteração | Aprovador | Data | Situação |
|---|---|---|---|---|---|
| CL-003-01 | `specs/003-colecao-miniaturas/spec.md` | bloco *Clarifications* + FR-013 | aprovação registrada na solicitação de aplicação | 2026-11-26 | **Aplicado** |
| CL-003-02 | `specs/003-colecao-miniaturas/contracts/collection-card.md` | §Identidade visual + linha do cartão | aprovação registrada na solicitação de aplicação | 2026-11-26 | **Aplicado** |
| CL-004-01 | `specs/004-editor-fotografias/spec.md` | bloco *Clarifications* + premissa | aprovação registrada na solicitação de aplicação | 2026-11-26 | **Aplicado** |

**Ao aprovar**: atualizar também o `**Status**` de cada spec com a data do esclarecimento (ex.: "Esclarecida em 2026-11-26 — CL-003-01") e registrar em `docs/validacao.md` que a 006 passou a ter base nas specs de origem. Nenhuma numeração de FR/SC muda.

## 7. Checklist de aplicação

- [x] Aplicar CL-003-01: inserir o bloco *Clarifications* e substituir FR-013 pelo texto de §3.3.
- [x] Aplicar CL-003-02: substituir a §Identidade visual e a linha do cartão em §3.4.
- [x] Aplicar CL-004-01: inserir o bloco *Clarifications* e substituir a premissa por §4.3.
- [x] Atualizar os `**Status**` das duas specs com a data e o ID.
- [x] Registrar em `docs/validacao.md` a aprovação e a referência à 006 (§10).
- [ ] Reexecutar `/speckit-analyze` na 006 para confirmar o fechamento de H1/M7.
- [x] Nenhuma alteração em código, testes ou no manual por causa **deste** esclarecimento (o manual muda pela T419 da 006).
