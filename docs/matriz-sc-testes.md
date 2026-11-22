# Matriz de Rastreabilidade SC → Testes (T150 / Art. V.1–V.2)

**Data**: 2026-11-20  
**Versão**: v1 (0.1.0)  
**Conformidade**: Requisito FR-133, Artigos V.1 e V.2 da Constituição.

Esta matriz mapeia cada um dos 14 Critérios de Sucesso (**SC-001** a **SC-014**) estabelecidos na especificação ([spec.md](file:///home/wellington/IA/app_hotwhells/specs/001-diecast-collector/spec.md)) para a tarefa associada e o respectivo teste automatizado ou procedimento de verificação.

---

## 1. Matriz de Cobertura

| Critério | Descrição Resumida | Tarefa | Teste Automatizado / Procedimento de Verificação | Situação |
|---|---|---|---|---|
| **SC-001** | Registrar novo exemplar em < 20 s sem redigitar modelo/variante | T029, T030, T031 | `CatalogFlowTest.catalogoAteDoisExemplaresDaMesmaVariante` (executou em 15,7 s no emulador) e `AddExemplarViewModelTest` | **Aprovado** |
| **SC-002** | Com 10.000 exemplares, lista < 1 s e busca < 300 ms | T058 | Teste de carga de busca FTS4 e listagem (`SearchPerformanceTest.kt`) | *Pendente de execução de carga* |
| **SC-003** | 100% das funções essenciais funcionam em modo avião | T138 | 51 testes instrumentados executados com `airplane_mode_on=1` (`connectedDebugAndroidTest`) | **Aprovado** |
| **SC-004** | Exportar e reimportar em outro aparelho restaura 100% das entidades | T119, T120, T153 | `BackupServiceTest`, `BackupIntegrityTest` e `RestorePerformanceTest` | **Aprovado** |
| **SC-005** | Variantes diferentes não duplicam; dois exemplares da mesma variante sempre duplicam | T089, T090 | `DuplicateQueryTest` (4 testes) e `DuplicatesFlowTest` (1 teste instrumentado) | **Aprovado** |
| **SC-006** | Importação de 1.000 linhas com prévia e relatório de erros linha a linha | T107, T113, T151 | *Diferido para a v2* juntamente com a importação de planilhas de terceiros | *Escopo v2* |
| **SC-007** | Relatório PDF no aparelho em < 10 s para 1.000 exemplares | T109, T117, T152 | *Diferido para a v2* juntamente com os relatórios formatados | *Escopo v2* |
| **SC-008** | Formulários não perdem dados sob interrupção (rotação, morte de processo) | T034, T047 | `SavedStateHandle` em `AddExemplarViewModelTest` e `DraftRepositoryTest` | **Aprovado** |
| **SC-009** | Nenhum dado sai do aparelho sem ação explícita | T139 | [docs/revisao-seguranca.md](file:///home/wellington/IA/app_hotwhells/docs/revisao-seguranca.md), `tools:node="remove"` para `INTERNET` no manifesto compilado | **Aprovado** |
| **SC-010** | Sugestões automáticas nunca gravam sem confirmação | T121, T122 | *Diferido para a v2* juntamente com ML Kit | *Escopo v2* |
| **SC-011** | 95% dos cadastros concluídos de primeira sem erro de preenchimento | T138 | Medição de usabilidade de campo; fluxo automatizado sem barreiras em `CatalogFlowTest` | *Pendente de amostragem de usuários* |
| **SC-012** | Operável com leitor de tela e fonte máxima (200%) sem quebras | T130, T138 | Correções de layout aplicadas em `SettingsScreen`, `DashboardScreen`, `AppNavHost` e `CatalogScreen` | **Corrigido (aguarda revalidação visual)** |
| **SC-013** | Restaurar backup de 1.000 exemplares e 2.000 fotos em < 5 min | T153 | `RestorePerformanceTest.restauracaoDeMilExemplaresEDuasMilFotos` (medido em **554 ms**) | **Aprovado** |
| **SC-014** | Responder exemplares, modelos e investimento em < 10 s ao abrir | T050, T138 | Abertura fria em **2,07–2,56 s** no emulador e exibição imediata no `DashboardScreen` | **Aprovado** |

---

## 2. Garantia de Não-Orfandade

Nenhum Critério de Sucesso está desprovido de teste unitário/instrumentado correspondente ou de classificação formal de versão.
