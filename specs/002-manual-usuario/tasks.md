# Tasks: Manual de Usuário Completo

**Input**: Documentos de design em `/specs/002-manual-usuario/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Restrição transversal**: nenhuma tarefa altera `app/`, `core/`, banco, migração,
dependência ou configuração. Toda escrita fica em `docs/manual/`, `specs/002-manual-usuario/`
e nas capturas (FR-025/SC-008). Defeitos encontrados viram lacuna/achado, não correção.

**Organization**: tarefas agrupadas por história de usuário (US1–US4) para permitir
entrega e validação independentes. Os capítulos são escritos em um único arquivo
(`docs/manual/MANUAL_USUARIO_COMPLETO.md`), por isso as tarefas de redação são sequenciais
entre si; capturas, índice, matriz e limitações são arquivos separados e podem correr em
paralelo.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivo diferente, sem dependência pendente)
- **[Story]**: US1, US2, US3, US4
- Todo caminho de arquivo é relativo à raiz do repositório

## Path Conventions

Documentação entregue: `docs/manual/`. Artefatos internos de inventário:
`specs/002-manual-usuario/`. Código do app: somente leitura.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: criar a estrutura dos seis entregáveis e a folha de estilo do PDF.

- [X] T001 Criar `docs/manual/MANUAL_USUARIO_COMPLETO.md` com capa (nome, versão `0.1.0`, versionCode 1), sumário com marcadores de capítulo e apêndice «Registro de evidências» (tabela: arquivo, tela/estado, origem, data, commit, dados fictícios)
- [X] T002 [P] Criar `docs/manual/manual.css` com capa, sumário, cabeçalho/rodapé, numeração de páginas, largura máxima de imagem e `page-break-inside: avoid` para figuras e tabelas
- [X] T003 [P] Criar `docs/manual/INDICE_FUNCIONALIDADES.md` com as tabelas Telas, Elementos, Procedimentos e Entidades, todas com coluna «Capítulo/seção»
- [X] T004 [P] Criar `docs/manual/MATRIZ_COBERTURA.md` com cabeçalho (versão, versionCode, commit, data) e as colunas de `contracts/coverage-contract.md`: `#`, Destino/funcionalidade, Tipo, Capítulo/seção, Elementos documentados, Evidência, Status, Observação
- [X] T005 [P] Criar `docs/manual/LIMITACOES_DOCUMENTACAO.md` com a tabela `id`, item afetado, motivo, impacto, alternativa real, destino no manual
- [X] T006 [P] Criar `docs/manual/capturas/LEIA-ME.md` e `docs/manual/diagramas/LEIA-ME.md` documentando a convenção `NN-descricao-estado.png`, origem, data e commit obrigatórios
- [X] T007 Registrar em `docs/manual/MANUAL_USUARIO_COMPLETO.md` a âncora de versão documentada (`0.1.0`, versionCode 1, commit `424e4fb`, 2026-11-21) e a declaração de que a obra é somente documental

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: inventário rastreável, numeração e ambiente de captura — base de todas as histórias.

**⚠️ CRITICAL**: nenhuma história começa antes desta fase.

- [X] T008 Inventariar os 30 destinos em `specs/002-manual-usuario/inventario.md` a partir de `app/src/main/java/br/com/hotwheelscollector/navigation/AppNavHost.kt`, com colunas: rota, título visível, arquivo `*Screen.kt`, ViewModel, acesso, estados (vazio/carregando/erro/sucesso), capítulo previsto
- [X] T009 Inventariar os elementos interativos por tela em `specs/002-manual-usuario/inventario-elementos.md`, com rótulo literal de `app/src/main/res/values/strings.xml`, `contentDescription` (ou ausência declarada), posição, ação e resultado
- [X] T010 Inventariar regras em `specs/002-manual-usuario/inventario-regras.md`: validações de `core/domain/.../validation/CatalogValidation.kt`, duplicidade por variante, cálculo financeiro de `FinanceDao.observeSummary()`, progresso de coleção, limites de backup/relatórios/importação e estados de reconhecimento
- [X] T011 Definir numeração e ordem editorial dos capítulos no sumário de `docs/manual/MANUAL_USUARIO_COMPLETO.md` conforme o inventário (navegação principal → primeiros passos → catálogo → organização → financeiro → ajustes)
- [X] T012 Preparar o ambiente de captura: `./gradlew :app:assembleDebug`, subir `hotwheels_test`, instalar limpo com `.tools/android-sdk/platform-tools/adb` e registrar commit/versão no «Registro de evidências»
- [X] T013 Criar o conjunto de dados fictícios de demonstração (fabricante, modelo, variante, exemplar, coleção, localização, aquisição, avaliação, item de desejo) e registrar os valores e o roteiro de criação no «Registro de evidências» de `docs/manual/MANUAL_USUARIO_COMPLETO.md`
- [X] T014 Registrar em `docs/manual/LIMITACOES_DOCUMENTACAO.md` as divergências encontradas entre `docs/operacao.md`/`docs/continua.md` e o código (relatórios, importação, reconhecimento, coleções), com status AUSENTE/PARCIAL

**Checkpoint**: inventário, numeração e emulador prontos — as histórias podem começar.

---

## Phase 3: User Story 1 — Aprender a usar o aplicativo do zero (Priority: P1) 🎯 MVP

**Goal**: um leitor sem conhecimento prévio instala o app e chega à primeira coleção com fotos, aquisição, localização, coleção, painel e backup seguindo apenas o manual.

**Independent Test**: instalação limpa, apenas o manual, executar o capítulo «Primeiros passos» de ponta a ponta sem ajuda e sem consultar o código.

- [X] T015 [US1] Escrever o capítulo «Antes de começar» em `docs/manual/MANUAL_USUARIO_COMPLETO.md`: conceitos de modelo, variante e exemplar, requisitos da versão `0.1.0`, instalação resumida e o que o app não faz
- [X] T016 [P] [US1] Capturar em `docs/manual/capturas/01-*.png` a `12-*.png` as telas do roteiro: catálogo vazio, cadastro inicial, formulário de variante, exemplar salvo, detalhe do exemplar, coleção, painel e backup
- [X] T017 [US1] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Primeiros passos» com as 10 etapas numeradas na ordem real do app (fabricante → modelo → variante → exemplar → fotos → aquisição → localização → coleção → painel → backup), citando rótulos literais
- [X] T018 [US1] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` a subseção «Primeiro backup» com o caminho Ajustes → Backup e restauração, o que conferir no pacote e o aviso de que restaurar fecha e reabre o banco
- [X] T019 [US1] Executar o roteiro de primeiros passos em instalação limpa usando somente o manual e registrar data, emulador, resultado e divergências no «Registro de evidências» de `docs/manual/MANUAL_USUARIO_COMPLETO.md`
- [X] T020 [US1] Corrigir o capítulo de primeiros passos em `docs/manual/MANUAL_USUARIO_COMPLETO.md` conforme as divergências da execução e refazer as capturas afetadas em `docs/manual/capturas/`
- [X] T021 [US1] Registrar no `docs/manual/INDICE_FUNCIONALIDADES.md` e em `docs/manual/MATRIZ_COBERTURA.md` as telas cobertas pelo roteiro, com status `DOCUMENTADO` ou `PARCIAL` + motivo

**Checkpoint**: o caminho do zero ao backup está escrito e validado por execução.

---

## Phase 4: User Story 2 — Consultar cada tela e cada elemento (Priority: P1)

**Goal**: referência completa, tela por tela, de todos os 30 destinos, com cada botão, campo, ícone, filtro, indicador, diálogo e mensagem.

**Independent Test**: para cada destino do inventário existe seção no contrato de 10 itens; a tabela de elementos cobre 100% dos interativos da `Screen.kt` correspondente.

- [X] T022 [P] [US2] Criar `docs/manual/diagramas/dominio.svg` com os relacionamentos fabricante → modelo → variante → exemplar → foto e escrever o capítulo «Estrutura do domínio» em `docs/manual/MANUAL_USUARIO_COMPLETO.md` (FR-003)
- [X] T023 [P] [US2] Capturar as 11 telas do catálogo em `docs/manual/capturas/13-*.png` a `23-*.png`: painel, catálogo, busca, cadastro inicial, formulário de variante, incompletos, edição de modelo, edição de variante, adicionar exemplar, lista e detalhe do exemplar
- [X] T024 [P] [US2] Capturar as 11 telas de organização/financeiro em `docs/manual/capturas/24-*.png` a `34-*.png`: coleções, detalhe, formulário, séries, financeiro, duplicados, negociação, lista de desejos, formulário de desejo, localizações e conteúdo da localização
- [X] T025 [P] [US2] Capturar as 8 telas de ajustes/transferência/reconhecimento em `docs/manual/capturas/35-*.png` a `42-*.png`: ajustes, backup, relatórios, importação, dados da coleção, sobre, privacidade e código de barras
- [X] T026 [US2] Escrever o capítulo «Painel» em `docs/manual/MANUAL_USUARIO_COMPLETO.md` no contrato de 10 seções, começando pela seção «Navegação principal» (as 5 abas inferiores e seus ícones/rótulos, a indicação de aba ativa e a regra de que a barra só aparece em rotas de primeiro nível), explicando cada indicador e o estado vazio
- [X] T027 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Catálogo» com tabela de elementos, estados de carregamento/erro e o que cada ação do cabeçalho abre
- [X] T028 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Busca» com todos os filtros inventariados em T010 (contagem confirmada e registrada no capítulo), chips ativos, contagem, grade/lista/galeria e agrupamentos
- [X] T029 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Cadastro inicial» com fabricante/modelo/escala, validação de escala `1:N`, limite de 120 caracteres e sanitização de campos opcionais
- [X] T030 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Formulário de variante e primeiro exemplar» com cor principal/secundárias, série, ano, código do fabricante, fotos e condição
- [X] T031 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Incompletos» com campos faltantes por modelo/variante e os atalhos de correção
- [X] T032 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Edição de modelo» com campos editáveis, validações e efeito no catálogo
- [X] T033 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Edição de variante» com cor e ano, a confirmação «variante atualizada» e o retorno à lista de incompletos
- [X] T034 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Adicionar exemplar» com código interno obrigatório/único, condição, embalagem, localização e notas (FR-011/FR-012)
- [X] T035 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Lista de exemplares» com contagem, filtros/ordenação existentes e acesso ao detalhe
- [X] T036 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Detalhe do exemplar» com foto, aquisição editável, avaliações append-only (explicando que o valor é estimativa, com fonte e data — Art. IV.8), histórico, movimentação, arquivamento e exclusão bloqueada
- [X] T037 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Coleções» com progresso por unidades, itens não colecionáveis e o estado vazio
- [X] T038 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Detalhe de coleção» com percentual, faltantes, itens e ação de editar
- [X] T039 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Formulário de coleção» com criação/edição, hierarquia e fabricante opcional
- [X] T040 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Séries» com criação, renomeação, hierarquia e arquivamento
- [X] T041 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Leitura de código de barras» separando leitura, sugestão de correspondência, confirmação humana e cadastro manual com o código lido (FR-012)
- [X] T042 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Duplicados» com agrupamento por variante, disponibilidade (troca/venda/ambas/indisponível) e o que conta como duplicado
- [X] T043 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Negociação» com venda/troca, status, histórico, valor e efeito no financeiro e no status do exemplar
- [X] T044 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Lista de desejos» com prioridade alta/média/baixa, status e ordenação
- [X] T045 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Formulário de desejo» com prioridade 1–3, valores máximo/encontrado, loja, link, vínculo opcional e conversão em exemplar
- [X] T046 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Financeiro» com as fórmulas reais (investido = preço + frete − desconto de exemplares `OWNED`; estimado = última avaliação; diferença; custo médio), um exemplo numérico conferível e o aviso de que o valor estimado é estimativa com fonte e data, nunca preço oficial (Art. IV.8)
- [X] T047 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Localizações» com árvore, criação, renomeação, arquivamento com política de movimentação e contagem por subárvore
- [X] T048 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Conteúdo da localização» com inventário, sublocalizações e movimentação de exemplares
- [X] T049 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Ajustes» com as 7 entradas (Localizações, Backup, Relatórios, Importação, Dados da coleção, Sobre, Privacidade), o lembrete de backup e a nota de funcionamento offline
- [X] T050 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Backup» com criação do pacote, contagens exibidas, restauração, consequência de fechar o banco e recuperação de falha
- [X] T051 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Relatórios» com o inventário em PDF, CSV e XLSX, destino do arquivo e ausência de outros tipos de relatório (FR-014)
- [X] T052 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Importação» com CSV, pré-visualização, validações e relatório de erros em CSV, sem prometer planilha XLSX
- [X] T053 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Dados da coleção» com exportação aberta e exclusão total, incluindo confirmação e consequências
- [X] T054 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Sobre» com versão, versionCode e pacote lidos da instalação
- [X] T055 [US2] Escrever em `docs/manual/MANUAL_USUARIO_COMPLETO.md` o capítulo «Privacidade» com a política offline e o que fica no aparelho
- [X] T056 [US2] Fazer revisão técnica por capítulo (mínimo 10 capítulos, incluindo Financeiro, Backup, Importação e Código de barras) contra `Screen.kt`, `ViewModel` e `strings.xml`, registrando o resultado no «Registro de evidências»
- [X] T057 [US2] Preencher `docs/manual/INDICE_FUNCIONALIDADES.md` com telas, elementos, procedimentos e entidades documentados e seus vínculos
- [X] T058 [US2] Preencher `docs/manual/MATRIZ_COBERTURA.md` com `documentados/total` de elementos por tela, evidência e status de cada um dos 30 destinos

**Checkpoint**: os 30 destinos têm seção no contrato e a matriz não tem órfãos.

---

## Phase 5: User Story 3 — Reproduzir um exemplo completo de coleção (Priority: P2)

**Goal**: o leitor reproduz o Nissan Skyline GT-R R34 azul, ano 2025, escala 1:64, e vê o mesmo registro no catálogo, no financeiro, na coleção e no painel.

**Independent Test**: instalação limpa, executar o capítulo do exemplo e obter os mesmos tipos de resultado, recalculando os valores do painel/financeiro a partir dos dados informados.

- [X] T059 [US3] Escrever o capítulo «Exemplo completo: Nissan Skyline GT-R R34» em `docs/manual/MANUAL_USUARIO_COMPLETO.md`, com os valores de cadastro, aquisição (preço, frete, desconto), avaliação, localização e coleção
- [X] T060 [US3] Capturar o mesmo registro no catálogo, na lista e no detalhe de exemplar, no financeiro e no painel em `docs/manual/capturas/43-*.png` a `47-*.png`
- [X] T061 [US3] Executar o exemplo em instalação limpa, registrar data/emulador no «Registro de evidências» de `docs/manual/MANUAL_USUARIO_COMPLETO.md` e conferir que painel e financeiro são recalculáveis a partir dos valores do capítulo (SC-004)
- [X] T062 [US3] Ajustar o exemplo e o capítulo «Financeiro» em `docs/manual/MANUAL_USUARIO_COMPLETO.md` até os números do manual e da tela coincidirem, registrando a conferência no «Registro de evidências»

**Checkpoint**: o exemplo é reproduzível e os cálculos fecham.

---

## Phase 6: User Story 4 — Ler e imprimir o manual em PDF (Priority: P3)

**Goal**: PDF com capa, sumário navegável, numeração, capturas legíveis e coerência com o Markdown.

**Independent Test**: abrir o PDF em leitor comum, navegar pelo sumário até um capítulo e conferir que todas as imagens citadas aparecem e estão legíveis.

- [X] T063 [US4] Gerar `docs/manual/MANUAL_USUARIO_COMPLETO.pdf` com o comando de `quickstart.md` (python-markdown + WeasyPrint + `docs/manual/manual.css`)
- [X] T064 [US4] Conferir capa, sumário navegável, paginação, cabeçalho/rodapé, tabelas e figuras sem quebra; ajustar `docs/manual/manual.css` e regerar
- [X] T065 [US4] Verificar que todo capítulo do Markdown existe no PDF e que nenhuma imagem está quebrada; registrar eventuais limitações de geração em `docs/manual/LIMITACOES_DOCUMENTACAO.md`

**Checkpoint**: Markdown e PDF coerentes, entregáveis completos.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: fechar as três revisões exigidas e provar a cobertura.

- [X] T066 [P] Concluir `docs/manual/LIMITACOES_DOCUMENTACAO.md`: toda lacuna citada no manual e na matriz, com motivo, impacto e alternativa real
- [X] T067 [P] Concluir `docs/manual/MATRIZ_COBERTURA.md`: 30 destinos, subestados, funcionalidades transversais, 11 entidades e 6 entregáveis, sem vínculo vazio e com resultado da conferência datado
- [X] T068 [P] Concluir `docs/manual/INDICE_FUNCIONALIDADES.md` sem item sem capítulo
- [X] T069 Executar a conferência de rotas × matriz de `contracts/coverage-contract.md` (seção 4) e anexar a saída ao `docs/manual/MATRIZ_COBERTURA.md`
- [X] T070 Executar a verificação de referências de imagem de `quickstart.md` (seção 5.2) e corrigir todo caminho quebrado ou captura ausente sem `lac-XX`
- [X] T071 Fazer a revisão editorial de `docs/manual/MANUAL_USUARIO_COMPLETO.md`: ortografia, terminologia (modelo/variante/exemplar), rótulos literais e consistência de nomes de tela; registrar o resultado no «Registro de evidências» de `docs/manual/MANUAL_USUARIO_COMPLETO.md`
- [X] T072 Fazer a revisão de validação: executar os procedimentos possíveis e registrar em `docs/manual/LIMITACOES_DOCUMENTACAO.md` o que não pôde ser executado, com motivo, e registrar o resultado da revisão no «Registro de evidências» de `docs/manual/MANUAL_USUARIO_COMPLETO.md`
- [X] T073 Conferir `git status --porcelain` e garantir que só há alterações em `docs/manual/` e `specs/002-manual-usuario/` (SC-008)
- [X] T074 Regerar `docs/manual/MANUAL_USUARIO_COMPLETO.pdf` após as correções e validar o checklist de pronto de `quickstart.md` (seção 6)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependência — pode começar já
- **Foundational (Phase 2)**: depende do Setup; **bloqueia** todas as histórias
- **US1 (Phase 3)**: depende da Foundational; não depende de US2
- **US2 (Phase 4)**: depende da Foundational; a redação dos capítulos pode começar depois do inventário, mas as capturas T023–T025 dependem de T013
- **US3 (Phase 5)**: depende de US1 (dados do exemplo) e do capítulo Financeiro de US2 (T046)
- **US4 (Phase 6)**: depende de US1 + US2 + US3 (o PDF cobre o manual inteiro)
- **Polish (Phase 7)**: depende de todas as histórias desejadas

### User Story Dependencies

- **US1 (P1)**: independente; entrega o caminho do zero ao backup
- **US2 (P1)**: independente de US1 na estrutura, mas compartilha o arquivo do manual — executar depois de US1 para evitar conflito
- **US3 (P2)**: consome os capítulos de US1/US2 e precisa do Financeiro correto
- **US4 (P3)**: consolida o que já foi escrito; não introduz conteúdo novo

### Within Each User Story

- Inventário antes de redigir; captura antes de citar figura
- Capítulo com tabela de elementos antes de preencher índice/matriz
- Execução de validação depois da redação e antes do checkpoint

### Parallel Opportunities

- Setup: T002–T006 em paralelo (arquivos diferentes)
- Foundational: T008 e T012 em paralelo; T009/T010 dependem de T008
- US1: T016 (capturas) em paralelo com T015/T017/T018
- US2: T023–T025 (capturas, arquivos distintos) em paralelo; T022 (diagrama/capítulo) em paralelo com as capturas
- Polish: T066–T068 em paralelo; T069–T073 dependem do conteúdo fechado

---

## Parallel Example: User Story 2

```bash
# Capturas de áreas diferentes, arquivos diferentes:
Task: "Capturar as 11 telas do catálogo em docs/manual/capturas/13-*.png a 23-*.png"
Task: "Capturar as 11 telas de organização/financeiro em docs/manual/capturas/24-*.png a 34-*.png"
Task: "Capturar as 8 telas de ajustes/transferência/reconhecimento em docs/manual/capturas/35-*.png a 42-*.png"

# Fechamento em arquivos separados:
Task: "Preencher docs/manual/INDICE_FUNCIONALIDADES.md"
Task: "Preencher docs/manual/MATRIZ_COBERTURA.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Setup (T001–T007) + Foundational (T008–T014)
2. US1 (T015–T021)
3. **PARAR e VALIDAR**: instalação limpa, somente o manual, roteiro até o backup
4. O MVP prova que o texto é executável, mas a entrega exige US2 + US4

### Incremental Delivery

1. Fundação pronta → inventário e ambiente confiáveis
2. US1 → roteiro validado por execução (MVP)
3. US2 → referência completa das 30 telas e matriz fechada
4. US3 → exemplo Skyline reproduzível e números conferidos
5. US4 → PDF com sumário, paginação e figuras
6. Polish → três revisões, cobertura e `git status` limpo

### Parallel Team Strategy

Com mais de uma pessoa: fundação em conjunto; depois capturas (US2) em paralelo com a
redação de US1; a redação de capítulos é sequencial por causa do arquivo único do manual;
índice, matriz, limitações e PDF podem ser feitos por outra pessoa ao final de cada fase.

---

## Notes

- [P] = arquivo diferente e sem dependência pendente
- Toda tarefa de redação edita o mesmo `docs/manual/MANUAL_USUARIO_COMPLETO.md` — não paralelizar entre si
- Nenhuma tarefa altera `app/`, `core/`, `gradle`, banco ou configuração (FR-025)
- Rótulo documentado é sempre o texto de `strings.xml`, nunca nome de função ou rota
- Captura ausente vira `lac-XX` em `LIMITACOES_DOCUMENTACAO.md`, nunca mockup
- Verificar os checkpoints antes de avançar de fase

## Estado da execução

Executado em 22/11/2026 no emulador `hotwheels_test` (API 36), APK de debug do commit `424e4fb`.

- 74 tarefas concluídas; 35 capítulos; 61 capturas reais, todas citadas e embutidas no manual.
- Desvios registrados: T060 usou as capturas já existentes (`05b`, `15`, `25`) em vez de
  `43-*.png` a `47-*.png`, porque o conjunto de dados do exemplo foi fotografado nessas telas.
- Lacunas de captura fechadas na mesma data: edição de variante (via importação de CSV com
  variante incompleta), foto de exemplar (Photo Picker com imagem fictícia) e restauração de
  backup (pacote de 3 exemplares · 1 foto restaurado com **Substituir**).
- Relatórios PDF/CSV/XLSX gerados no app, extraídos com `adb pull` e abertos; conteúdo idêntico
  nos três formatos, incluindo a condição como código interno (`SEALED`).
- Lacuna restante em `docs/manual/LIMITACOES_DOCUMENTACAO.md`: `lac-01` (leitura real de
  código de barras).
- PDF: 128 páginas, gerado com WeasyPrint 70.0 a partir de `docs/manual/MANUAL_USUARIO_COMPLETO.md`.
