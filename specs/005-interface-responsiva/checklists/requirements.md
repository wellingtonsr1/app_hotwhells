# Specification Quality Checklist: Interface responsiva e adaptável

**Purpose**: validar completude e qualidade da especificação antes do planejamento e das tarefas
**Created**: 2026-11-24
**Feature**: [`spec.md`](../spec.md) · auditoria: [`audit.md`](../audit.md) · plano: [`plan.md`](../plan.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs) — o corpo da spec descreve comportamento; APIs do Material 3 aparecem apenas como dependência/restrição, não como requisito de usuário
- [x] Focused on user value and business needs — cada história descreve o colecionador usando o app em celular, tablet, dobrável e paisagem
- [x] Written for non-technical stakeholders — vocabulário de tela, controle e tamanho de janela, sem nomes de classes
- [x] All mandatory sections completed — User Scenarios, Requirements, Success Criteria, Assumptions, Dependências

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — as três dúvidas abertas foram registradas na auditoria (Q1–Q3) com proposta padrão, sem bloquear a revisão
- [x] Requirements are testable and unambiguous — FR-301–FR-333 e NFR-301–NFR-304 com sujeito e condição verificáveis
- [x] Success criteria are measurable — SC-301–SC-310 com percentuais, limites em dp, tempos e contagens de arquivo
- [x] Success criteria are technology-agnostic — falam de janela, coluna, alvo de toque e tela; não citam biblioteca
- [x] All acceptance scenarios are defined — 5 histórias com cenários Given/When/Then (24 cenários)
- [x] Edge cases are identified — 11 casos (fonte 200%, paisagem com teclado, dobra, duas janelas, janela redimensionável, insets, tema, valores longos)
- [x] Scope is clearly bounded — seção "Fora de escopo" + aviso de âmbito no topo + FR-329/330/331
- [x] Dependencies and assumptions identified — 8 premissas e 3 dependências

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria — cada FR se mapeia a um cenário de história ou a um item de SC
- [x] User scenarios cover primary flows — navegação, leitura de listas, formulários, busca/filtros, fotos/editor
- [x] Feature meets measurable outcomes defined in Success Criteria — SC-301…SC-310 verificáveis pelo plano de testes
- [x] No implementation details leak into specification — nomes de arquivo e componentes ficam em `plan.md`

## Notas

- A spec é de **apresentação**: FR-329 a FR-331 garantem que domínio, banco, offline e identidade do produto não mudam.
- As três dúvidas (Q1 navegação na classe média, Q2 grade × lista-detalhe, Q3 trilho × gaveta) estão em `audit.md` §6 com proposta padrão; podem ser fechadas em `/speckit-clarify` sem retrabalho da spec.
- Limitação registrada: a auditoria é estática, sem execução em tablet/dobrável; a verificação por classe de janela está prevista no plano (fase 1 e fase 7).
- Pronto para `/speckit-tasks` (a spec não depende de esclarecimento bloqueante).
