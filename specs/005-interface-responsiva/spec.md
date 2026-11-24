# Feature Specification: Interface responsiva e adaptável

**Feature Branch**: `005-interface-responsiva`

**Created**: 2026-11-24

**Status**: Draft — aguardando revisão e aprovação

**Input**: User description: "Realizar uma auditoria completa da interface atual do Diecast Collector e elaborar uma especificação técnica para torná-la responsiva, adaptável e consistente em diferentes tamanhos de tela, respeitando a Constituição v1.3.0 (Artigo VII)."

**Constituição aplicável**: `.specify/memory/constitution.md` v1.3.0 — Artigo VII (responsividade), IV.5 (Android, 360 dp, 48 dp, retrato e paisagem), IV.6/VII.10 (acessibilidade), II (modularidade), V.1/V.10 (testes e verificação por classe de janela), IV.9 (LGPD), R.17 e R.18.

> **Âmbito desta especificação**: apenas apresentação. Nenhum requisito desta spec altera regra de negócio, modelo de dados, esquema do banco, persistência, funcionamento offline ou contratos de domínio (VII.11). A auditoria que a originou está em `audit.md`; o plano de execução, em `plan.md`.

## Contexto

O aplicativo foi construído e verificado em celular (janela compacta, ~411 dp de largura, emulador API 36). A interface usa Jetpack Compose + Material 3, mas **nenhuma decisão de layout consulta o tamanho da janela**: não há uso de classes de janela, a navegação principal é sempre uma barra inferior de cinco abas, as grades têm sempre duas colunas fixas, os formulários ocupam a largura inteira sem limite de leitura e vários grupos de opções são organizados em `Row` sem quebra de linha. Em janelas médias e expandidas (tablets, dobráveis abertos, celular em paisagem) e em fonte ampliada, isso produz telas esticadas, cartões gigantes, fileiras de controles que saem da tela e diálogos que dependem de altura fixa.

A auditoria (`audit.md`) registra os problemas comprovados no código atual, separados de riscos e de oportunidades. Esta especificação define o comportamento esperado e os critérios verificáveis.

## Clarifications

### Session 2026-11-24

- Q: Em janela média (600–839 dp), a navegação deve ir direto para trilho lateral ou manter a barra inferior até 840 dp? → A: **trilho lateral já na classe média**. A barra inferior fica restrita à janela compacta (< 600 dp). Os cinco destinos, os identificadores de rota e o estado por aba permanecem exatamente como hoje (FR-302, FR-303).
- Q: Em janela expandida, listas de cartões devem virar grade de 2–3 colunas ou lista-detalhe (lista com painel ao lado)? → A: **grade responsiva** — colunas adicionais por largura mínima confortável — nesta feature. **Lista-detalhe lado a lado fica fora do escopo**: as telas de detalhe continuam sendo destinos próprios (por exemplo, catálogo → exemplar) e adotar painel duplo exige especificação própria, porque muda o fluxo de navegação e não apenas a apresentação (FR-307, FR-331).
- Q: A barra inferior em janela expandida pode virar gaveta, ou o trilho deve permanecer fixo com os cinco destinos atuais? → A: **trilho fixo com os cinco destinos atuais**. Gaveta só entra se o número de destinos crescer, o que exige decisão e especificação posteriores; esta feature não acrescenta destino novo (FR-302).

**Consequências registradas**: nenhuma rota nova, nenhuma tela reconstruída, nenhum destino escondido em qualquer classe de janela; a adaptação continua sendo de apresentação (FR-329) e a navegação por trilho é o único acréscimo estrutural ao shell.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navegar e ler o acervo com conforto em qualquer tamanho de tela (Priority: P1)

O colecionador abre o Diecast Collector no celular em retrato, no celular em paisagem, no tablet ou no dobrável aberto. A navegação principal se reposiciona conforme o espaço (barra inferior no celular, trilho lateral nas janelas maiores), as listas e grades aproveitam a largura disponível mostrando mais itens por linha, e nada precisa ser "esticado" ou "espremido" para caber. Ao girar o aparelho ou redimensionar a janela, a tela continua utilizável sem perder o ponto em que ele estava.

**Why this priority**: a navegação é a moldura de todas as telas. Enquanto ela não se adapta, nenhuma tela resolve sozinha o desconforto em tela grande, e o app continua parecendo "um celular esticado" no tablet.

**Independent Test**: em uma janela compacta, média e expandida, abrir as cinco abas, percorrer catálogo, busca, desejos e finanças e girar a tela; verificar que a navegação muda de forma, que as listas mostram mais de uma coluna quando há espaço e que nenhum item fica cortado.

**Acceptance Scenarios**:

1. **Given** uma janela compacta (largura < 600 dp), **When** o usuário abre o aplicativo, **Then** a navegação principal aparece como barra inferior com as cinco abas e o conteúdo ocupa a largura disponível sem rolagem horizontal.
2. **Given** uma janela média (600–839 dp) ou expandida (≥ 840 dp), **When** o usuário abre o aplicativo, **Then** a navegação principal aparece como trilho lateral (ou gaveta persistente) e a área de conteúdo usa a largura restante.
3. **Given** qualquer tela com lista ou grade, **When** a janela é expandida, **Then** a lista passa a mostrar mais de uma coluna de itens (grade responsiva) em vez de alargar indefinidamente o mesmo cartão.
4. **Given** o catálogo em tela expandida, **When** o usuário percorre os itens, **Then** os cartões mantêm largura de leitura confortável e alinhamento consistente, sem um único cartão ocupando a tela toda.
5. **Given** uma tela aberta em retrato, **When** o usuário gira o aparelho para paisagem, **Then** o layout se reorganiza, o estado da tela é preservado e nenhuma ação fica inacessível.
6. **Given** um dobrável aberto com a dobra no meio, **When** o usuário usa uma tela de lista com detalhe ou um formulário, **Then** os controles e o conteúdo não ficam sobre a dobra nem sob a barra de navegação do sistema.

---

### User Story 2 - Nenhum controle cortado, sobreposto ou fora de alcance (Priority: P1)

O usuário com fonte do sistema ampliada, ou em uma janela estreita, consegue tocar em todos os controles: os grupos de opções quebram em mais linhas em vez de sair da tela, os botões continuam inteiros e legíveis, e o rodapé de ações nunca fica embaixo dos botões do aparelho.

**Why this priority**: um controle inacessível é uma funcionalidade perdida. É o mesmo defeito que já apareceu no rodapé do editor de fotografia (spec 004) e reaparece em outros grupos de opções.

**Independent Test**: com fonte do sistema em 200% e janela compacta, abrir cada tela com grupos de opções (condição do exemplar, disponibilidade de duplicado, filtros de busca, veículo real) e verificar que todos os chips estão visíveis e tocáveis, com pelo menos 48 dp de área de toque.

**Acceptance Scenarios**:

1. **Given** a tela de cadastro rápido com fonte ampliada, **When** o usuário vê o grupo de condições do exemplar, **Then** os chips quebram para novas linhas (ou rolam dentro do próprio grupo) e nenhum fica parcialmente fora da tela.
2. **Given** a folha de disponibilidade de duplicado, **When** ela é aberta em janela compacta, **Then** todas as opções de disponibilidade e de condição são alcançáveis por toque.
3. **Given** a ficha do exemplar, **When** o usuário percorre os grupos de condição e de embalagem, **Then** todos os chips ficam visíveis e selecionáveis.
4. **Given** qualquer botão de ícone da grade de fotos, **When** o usuário tenta tocar, **Then** a área de toque é de pelo menos 48 × 48 dp.
5. **Given** qualquer tela com ações no rodapé, **When** a barra de navegação do sistema é de 3 botões ou por gestos, **Then** as ações terminam acima da barra, com folga, e não recebem toque do sistema.

---

### User Story 3 - Formulários e diálogos confortáveis em tablet e paisagem (Priority: P2)

Em tablet ou celular em paisagem, o formulário não estica um campo de texto por 1.000 dp; ele mantém uma coluna de leitura confortável, aproveita o espaço extra para colocar campos relacionados lado a lado quando fizer sentido, e os diálogos cabem na altura disponível com rolagem própria.

**Why this priority**: os formulários são o caminho principal de cadastro. Em tela grande eles hoje parecem "esticados"; em paisagem, diálogos com altura fixa podem ultrapassar a tela.

**Independent Test**: abrir cadastro de miniatura, variante, exemplar, coleção, desejo e ficha do exemplar em janela expandida e em paisagem compacta (altura ~360 dp); verificar largura de leitura limitada, campos alinhados e diálogos roláveis.

**Acceptance Scenarios**:

1. **Given** uma janela expandida, **When** o usuário abre um formulário longo, **Then** o conteúdo respeita uma largura máxima de leitura e o restante do espaço permanece como margem, sem campos de 1.000 dp.
2. **Given** um formulário com pares de campos lado a lado, **When** a janela é compacta ou a fonte é ampliada, **Then** os campos se empilham em uma coluna em vez de truncar rótulos e valores.
3. **Given** a janela em paisagem com pouca altura, **When** o usuário abre o seletor de item de coleção, **Then** o diálogo cabe na altura disponível e a lista interna rola.
4. **Given** um diálogo de confirmação com texto longo, **When** a fonte é ampliada, **Then** o texto rola e os botões permanecem visíveis e tocáveis, inteiros.
5. **Given** um formulário preenchido, **When** o usuário gira a tela ou redimensiona a janela, **Then** os valores digitados são preservados (IV.7).

---

### User Story 4 - Busca, filtros e coleções adaptados ao espaço (Priority: P2)

Na busca, o usuário escolhe entre lista, grade, galeria e agrupações. Em tela grande, as grades mostram mais colunas e os filtros podem ocupar um painel lateral fixo em vez de uma folha que cobre tudo; em tela estreita, continuam em folha modal.

**Why this priority**: busca e filtros são a porta de entrada do acervo grande; grades de duas colunas fixas em tablet mostram menos itens do que o espaço permitiria.

**Independent Test**: em cada classe de janela, alternar as visões de lista, grade e galeria e abrir os filtros; verificar número de colunas coerente com a largura, painel lateral em expandida e folha modal em compacta.

**Acceptance Scenarios**:

1. **Given** a busca em janela compacta, **When** o usuário abre os filtros, **Then** eles aparecem em folha modal ocupando a largura da janela.
2. **Given** a busca em janela expandida, **When** o usuário abre os filtros, **Then** eles aparecem em painel lateral persistente e os resultados permanecem visíveis ao lado.
3. **Given** a visão em grade ou galeria, **When** a janela cresce, **Then** o número de colunas aumenta e as miniaturas mantêm proporção e legenda sem corte.
4. **Given** a busca com filtros ativos, **When** o usuário gira a tela, **Then** filtros, consulta e ordem escolhidos são preservados.

---

### User Story 5 - Fotografias e editor proporcionais em qualquer tela (Priority: P3)

As grades de fotos aproveitam a largura da janela, as miniaturas não viram tijolos de 500 dp e o editor de fotografias calcula sua área útil a partir do espaço real da janela, mantendo as ferramentas e o rodapé alcançáveis em tablet, paisagem e dobrável.

**Why this priority**: já funciona em celular retrato (spec 004 resolvida); o ganho é de conforto e de prevenção de regressão em telas grandes. Não bloqueia as demais entregas.

**Independent Test**: abrir a grade de fotos e o editor em janela expandida e em paisagem compacta; verificar proporção das miniaturas, painel de ferramentas rolável e rodapé acima da barra do sistema.

**Acceptance Scenarios**:

1. **Given** a grade de fotos de um exemplar em janela expandida, **When** ela é exibida, **Then** o número de colunas é coerente com a largura e as fotos mantêm a proporção 3:4 sem distorção.
2. **Given** o editor aberto em paisagem compacta, **When** o usuário troca para a ferramenta de cor, **Then** o painel rola dentro do próprio espaço e o rodapé permanece acima da barra do sistema.
3. **Given** o editor aberto em janela expandida, **When** o usuário ajusta a imagem, **Then** a prévia usa o espaço disponível sem deslocar as ferramentas para fora da tela.
4. **Given** o visualizador de foto em tela cheia, **When** o aparelho tem recorte de câmera ou barra de gestos, **Then** a imagem e o botão de fechar não ficam sob os insets.

---

### Edge Cases

- Fonte do sistema em 200% (2.0×) em janela compacta de 360 dp: todo texto essencial continua legível e todo controle continua alcançável.
- Paisagem de celular (altura útil ~360 dp) com teclado aberto: o campo em edição permanece visível e o conteúdo rola.
- Dobrável fechado (janela estreita) → aberto (janela expandida) durante o uso: a tela se reorganiza e o estado é preservado.
- Dobrável com duas janelas do app lado a lado: cada janela se comporta pela própria classe de largura, não pela tela física.
- Dobra/separador no meio da janela: nenhum controle essencial sobre a dobra.
- Janela redimensionável livre (tablet em modo desktop / ChromeOS): qualquer largura intermediária entre 320 dp e 1.400 dp permanece utilizável.
- Barra de navegação de 3 botões, por gestos e transparente sobre o conteúdo: nenhuma ação sob a barra.
- Tema escuro e claro: contraste e legibilidade preservados em todas as classes.
- Listas vazias, com erro e em carregamento: mensagens centralizadas e legíveis, sem depender de largura fixa.
- Tabela de métricas do painel com valores monetários longos (ex.: "R$ 1.234.567,89") em fonte ampliada.
- Nome de fabricante/série muito longo em linha com valor à direita (finanças, coleções): o texto quebra ou trunca sem empurrar o valor para fora.

## Requirements *(mandatory)*

> Numeração a partir de FR-301 para não colidir com as especificações anteriores (001–004).

### Apresentação global e navegação

- **FR-301**: O aplicativo MUST classificar a janela pelas classes de largura compacta (< 600 dp), média (600–839 dp) e expandida (≥ 840 dp), derivadas do contêiner da tela, e MUST reavaliar a classificação quando a janela muda de tamanho, de orientação ou de dobra.
- **FR-302**: A navegação principal MUST se adaptar: barra inferior na compacta; trilho lateral na média e na expandida, mantendo as cinco abas existentes e os mesmos destinos. Gaveta MUST NOT ser introduzida nesta feature (Clarifications, Q3); ela só será considerada se o número de destinos crescer, em especificação própria.
- **FR-303**: O conteúdo MUST ocupar a área restante após a navegação, sem sobreposição, sem corte e sem rolagem horizontal da página inteira.
- **FR-304**: O estado das telas (rolagem, filtros, texto digitado, seleção) MUST ser preservado ao girar, redimensionar ou dobrar/desdobrar, reutilizando o mesmo mecanismo de estado já existente.
- **FR-305**: As telas de detalhe MUST continuar sem a barra de navegação principal e MUST manter a ação de voltar alcançável em qualquer classe de janela.

### Listas, grades e conteúdo

- **FR-306**: Listas e grades MUST ajustar o número de colunas à largura disponível, com largura mínima de item entre **160 dp e 180 dp** para cartões de acervo (valor declarado no componente de grade), em vez de fixar duas colunas ou uma coluna única.
- **FR-307**: Em janelas médias e expandidas, telas de lista MUST aproveitar a largura com colunas adicionais (grade responsiva), sem exigir reconstrução da tela. Apresentação em dois painéis (lista com detalhe ao lado) MUST NOT ser introduzida nesta feature: as telas de detalhe continuam sendo destinos próprios, e o modo lista-detalhe exige especificação própria (ver Clarifications, 2026-11-24).
- **FR-308**: Nenhum item de lista ou cartão MUST ultrapassar uma largura de leitura confortável; o espaço excedente MUST virar margem ou colunas adicionais.
- **FR-309**: Cabeçalhos com título e ações à direita MUST quebrar ou reduzir as ações a ícones quando a largura não comportar título e botões na mesma linha.
- **FR-310**: Textos que dividem linha com valores numéricos (valores monetários, contagens, percentuais) MUST quebrar ou truncar sem empurrar o valor para fora da tela.

### Formulários

- **FR-311**: Formulários MUST respeitar largura máxima de leitura de **720 dp de conteúdo** em janelas médias e expandidas (SC-306) e MUST alinhar os campos ao centro dessa coluna.
- **FR-312**: Pares de campos em linha MUST se empilhar em uma coluna quando a largura não comportar dois campos legíveis, incluindo com fonte ampliada.
- **FR-313**: Grupos de opções (chips, filtros, seleções) MUST quebrar em linhas adicionais (ou rolar dentro do próprio grupo) quando não couberem na largura, sem cortar opções.
- **FR-314**: Toda ação primária de um formulário MUST permanecer visível e alcançável com o teclado aberto e em janela de pouca altura.
- **FR-315**: O comportamento de validação, os campos, a ordem e a regra de negócio dos formulários MUST permanecer idênticos em todas as classes de janela.

### Diálogos, folhas e menus

- **FR-316**: Diálogos MUST dimensionar-se pela altura e largura disponíveis, com rolagem interna quando o conteúdo exceder o espaço, e MUST NOT depender de altura fixa em dp.
- **FR-317**: Folhas modais com conteúdo longo (filtros de busca, disponibilidade de duplicado, revisão de reconhecimento) MUST manter rolagem própria e botões de ação sempre alcançáveis.
- **FR-318**: Em janelas expandidas, painéis densos (filtros de busca) MUST ser apresentados como painel lateral persistente; em compacta, como folha modal.
- **FR-319**: Menus suspensos e seletores MUST abrir dentro dos limites da janela, sem sair da área visível.
- **FR-320**: O editor de fotografias MUST calcular sua área útil a partir do espaço real da janela, mantendo as ferramentas roláveis e o rodapé de ações acima dos insets do sistema.

### Imagens

- **FR-321**: Miniaturas e fotos MUST manter a proporção original (recorte por `Crop` ou ajuste por `Fit`, nunca distorção), em qualquer coluna ou classe de janela.
- **FR-322**: A grade de fotos MUST ajustar o número de colunas à largura, preservando a proporção 3:4 das miniaturas.
- **FR-323**: Visualizadores de foto em tela cheia MUST respeitar os insets do sistema e manter os controles de fechar e navegar alcançáveis.

### Acessibilidade e insets

- **FR-324**: Toda área de toque MUST ter pelo menos 48 × 48 dp, inclusive botões de ícone sobre miniaturas.
- **FR-325**: O conteúdo MUST respeitar os insets do sistema (status, navegação, gestos, recorte, teclado) e a dobra do dispositivo; nenhuma ação essencial MUST ficar sob eles.
- **FR-326**: Contraste ≥ 4.5:1, rótulos semânticos, ordem de foco coerente e respeito ao tamanho de fonte do sistema MUST ser mantidos em todas as classes de janela.
- **FR-327**: Rótulos essenciais MUST NOT ser truncados a ponto de perder sentido por falta de espaço; quando truncar, MUST haver descrição acessível completa.

### Consistência e preservação

- **FR-328**: A identidade visual MUST ser preservada: mesmos tokens de tema, paleta, tipografia base, ícones e padrões de interação em todas as classes.
- **FR-329**: As mudanças MUST ser de apresentação: nenhuma alteração de modelo de dados, banco, repositório, caso de uso, regra de negócio ou funcionamento offline.
- **FR-330**: O nome comercial **Diecast Collector**, o `applicationId` e os identificadores técnicos MUST permanecer inalterados.
- **FR-331**: Nenhuma funcionalidade existente MUST ser removida, escondida ou desabilitada em qualquer classe de janela.
- **FR-332**: A adaptação MUST ser implementada em componentes compartilhados (shell de navegação, grade adaptativa, grupo de opções que quebra linha, coluna de formulário, contêiner de diálogo) e reutilizada pelas telas, em vez de duplicada tela a tela.
- **FR-333**: O aplicativo MUST NOT bloquear orientação nem restringir redimensionamento como forma de evitar problemas de layout (R.18).

### Requisitos não funcionais

- **NFR-301**: A troca de classe de janela (rotação, redimensionamento, dobra) MUST reorganizar a tela em menos de 1 segundo em aparelho de referência, sem recarregar dados.
- **NFR-302**: A adaptação MUST NOT degradar os fluxos medidos na spec 003 (abertura de listas, rolagem, abertura de detalhe); as metas provisórias de R.7 continuam valendo.
- **NFR-303**: O tamanho do artefato instalável MUST NOT crescer mais de 1 MB com a adaptação, avaliado antes e depois (R.13).
- **NFR-304**: Nenhuma dependência nova MUST entrar sem registro de finalidade, versão e plano de substituição (R.5); bibliotecas adaptativas exigem ADR (R.4, R.17).

### Fora de escopo

- Novas telas, novas funcionalidades e novos dados.
- Sincronização, nuvem, multiusuário.
- Versão web, desktop ou iOS.
- Redesenho visual da identidade (cores, tipografia, ícones) — apenas adaptação de layout.
- Reescrita de telas sem necessidade comprovada; correções cirúrgicas são a regra.
- Mudança de `applicationId`, `package name`, banco, esquema ou nomes técnicos.

### Key Entities *(include if feature involves data)*

Esta especificação **não envolve modelo de dados**: nenhuma entidade persistida é criada, alterada ou migrada. As "entidades" aqui são de apresentação:

- **Classe de janela**: compacta, média, expandida; derivada da largura do contêiner; determina forma de navegação, número de colunas e apresentação de painéis.
- **Shell de navegação**: moldura comum (barra inferior, trilho, gaveta) com os cinco destinos atuais.
- **Componentes adaptativos compartilhados**: grade responsiva de itens, grupo de opções que quebra linha, coluna de formulário com largura máxima, contêiner de diálogo com rolagem, visualizador de mídia com insets.
- **Tela**: cada uma das **30 telas/rotas de UI** e **11 diálogos/folhas** inventariados em `audit.md` §2 — 30 linhas de tela mais 7 linhas que agrupam os diálogos e componentes, **37 linhas no total** —, com o comportamento esperado por classe de janela.

## Success Criteria *(mandatory)*

- **SC-301**: 100% das **30 telas/rotas de UI e 11 diálogos/folhas** (as **37 linhas** do inventário de `audit.md` §2) são verificados nas três classes de janela e em paisagem compacta, sem nenhum elemento cortado, sobreposto ou inacessível (0 ocorrências nas 37 linhas do checklist de T356).
- **SC-302**: Em janela expandida, todas as telas de lista/grade exibem mais de uma coluna de itens e nenhum cartão ocupa mais de 70% da largura útil.
- **SC-303**: Em janela compacta com fonte do sistema em 200%, todas as ações principais de cada tela continuam alcançáveis por toque em um teste de verificação por tela.
- **SC-304**: 100% dos alvos de toque medidos em teste têm pelo menos 48 × 48 dp.
- **SC-305**: Nenhuma ação fica sob a barra de navegação ou sob a dobra: 0 falhas nas asserções de insets nas telas com rodapé de ações.
- **SC-306**: Todos os formulários mantêm largura de leitura de no máximo 600–720 dp de conteúdo em janelas expandidas.
- **SC-307**: A suíte existente (unitários + instrumentados) continua verde após a mudança, com no máximo os ajustes de asserção exigidos pela nova forma de navegação — nenhum teste é removido ou afrouxado.
- **SC-308**: Nenhuma alteração em banco, esquema, dados ou regra de negócio: 0 arquivos de `core/domain`, `core/data` ou de migração modificados por esta feature.
- **SC-309**: O usuário consegue concluir o cadastro de um exemplar em tablet e em celular em paisagem sem sair do fluxo, com o mesmo número de toques do fluxo em celular retrato (ou menos).
- **SC-310**: A troca de classe de janela reorganiza a tela em menos de 1 segundo (NFR-301), sem perder estado.

## Assumptions

- A Constituição v1.3.0 (Artigo VII) é a referência de responsividade; em conflito, ela prevalece.
- A stack permanece Kotlin + Jetpack Compose + Material 3 com o BOM já usado (2025.01.00); a adaptação usa as APIs de classe de janela do Material 3, sem trocar a stack.
- `minSdk 26`, `targetSdk 36`: as APIs de classe de janela do Compose funcionam em todas as versões suportadas.
- Tablets e dobráveis são suportados por adaptação de layout, não por telas separadas; desktop/ChromeOS continua opcional (IV.5).
- As telas em janela compacta mantêm a aparência atual sempre que possível; a mudança perceptível concentra-se em janelas médias/expandidas e em fonte ampliada.
- A adaptação é incremental: cada fase entrega valor isolado e pode ir ao ar sozinha.
- Não há dados de layout salvos como preferência do usuário; nada precisa ser migrado.
- Os testes instrumentados atuais rodam em emulador de celular; a verificação por classe de janela exige configuração de tamanho no teste ou emuladores adicionais (detalhado em `plan.md`).

## Dependências

- Constituição v1.3.0; especificações 001–004 (telas existentes e editor de fotografia).
- APIs de classe de janela do Material 3 para Compose e, se aprovado em ADR, os componentes adaptativos de navegação/lista-detalhe (R.17).
- Suíte de testes instrumentados existente (30 classes) e o emulador API 36 já usado no projeto.
