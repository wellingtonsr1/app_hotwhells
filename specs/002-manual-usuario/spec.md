# Feature Specification: Manual de Usuário Completo

**Feature Branch**: `002-manual-usuario`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "Produzir um manual profissional de usuário do Diecast Collector, em pt-BR, cobrindo todas as telas, botões, campos, ícones, filtros, indicadores e procedimentos realmente implementados, com capturas reais, diagramas, PDF e matriz de cobertura. A atividade é exclusivamente documental; nenhum código, banco, configuração ou interface pode ser alterado."

## Contexto

O aplicativo **Diecast Collector** (versão instalada `0.1.0`, versionCode 1) organiza uma
coleção pessoal de miniaturas inteiramente no aparelho. A documentação existente
(`docs/operacao.md`, `docs/validacao.md`) cobre instalação e backup no nível operacional,
mas não explica tela por tela o que o usuário vê e faz.

O manual é um produto documental derivado do código. Ele **não pode** descrever
funcionalidades planejadas, telas de outros aplicativos ou comportamentos inferidos apenas
pelo nome de componentes. Quando não for possível confirmar ou executar algo, a ausência
deve ser registrada explicitamente, nunca preenchida por suposição.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Aprender a usar o aplicativo do zero (Priority: P1)

Uma pessoa instala o Diecast Collector pela primeira vez, sem conhecer a diferença entre
modelo, variante e exemplar, e precisa chegar a uma pequena coleção cadastrada: primeiro
fabricante, modelo, variante, exemplar, fotos, aquisição, localização, coleção, leitura do
painel e primeiro backup.

**Why this priority**: é o critério final de aceitação do manual. Sem esse caminho, o
documento não serve para quem nunca viu o app, por mais completo que seja o restante.

**Independent Test**: uma pessoa que nunca usou o app recebe apenas o manual e monta a
coleção de demonstração do tutorial inicial, sem ajuda de terceiros e sem consultar o
código.

**Acceptance Scenarios**:

1. **Given** um aparelho com o app recém-instalado e uma coleção vazia, **When** o leitor
   segue o capítulo «Primeiros passos» do início ao fim, **Then** ele conclui o cadastro de
   fabricante, modelo, variante, exemplar, fotos, aquisição, localização, coleção, consulta
   ao painel e backup, usando somente os nomes e caminhos escritos no manual.
2. **Given** o leitor diante de uma tela desconhecida, **When** ele consulta o índice pelo
   nome visível na tela, **Then** encontra a seção correspondente sem precisar adivinhar
   termos internos.
3. **Given** uma instrução do manual, **When** o leitor a executa no app, **Then** o
   resultado descrito acontece e nenhum botão citado está ausente ou com outro rótulo.

---

### User Story 2 - Consultar cada tela e cada elemento (Priority: P1)

Um usuário já familiarizado com o app precisa de referência para uma tela específica:
entender o significado de um indicador, o efeito de um filtro, a validação de um campo ou
as opções de um diálogo, sem ler o manual inteiro.

**Why this priority**: o pedido exige explicação individual de botões, campos, ícones,
opções, filtros e indicadores; um texto por fluxo deixaria lacunas.

**Independent Test**: para cada tela implementada, o manual contém descrição de finalidade,
acesso, elementos visíveis, procedimento, regras, mensagens, dicas e problemas comuns, e
todos os elementos interativos da tela aparecem nomeados.

**Acceptance Scenarios**:

1. **Given** a lista de destinos de navegação do aplicativo, **When** a matriz de cobertura
   é conferida, **Then** cada destino tem seção própria ou seção de tela-filha identificada,
   sem destinos ausentes.
2. **Given** uma tela com botões, campos, ícones e diálogos, **When** a seção dessa tela é
   lida, **Then** cada elemento visível tem nome, localização, finalidade, ação e resultado
   esperado.
3. **Given** uma mensagem de erro ou confirmação exibida pelo app, **When** o usuário a
   encontra, **Then** o manual explica quando ela aparece e o que fazer.

---

### User Story 3 - Reproduzir um exemplo completo de coleção (Priority: P2)

Um novo usuário quer ver o app em uso antes de cadastrar seu acervo real e segue um exemplo
com um Nissan Skyline GT-R R34 azul, ano 2025, escala 1:64, acompanhando como o mesmo
registro aparece no catálogo, no financeiro, na coleção e nos relatórios.

**Why this priority**: exemplos concretos reduzem erro de interpretação, mas dependem de o
manual já estar correto e completo.

**Independent Test**: o leitor reproduz o exemplo em uma instalação limpa e obtém os mesmos
tipos de resultado descritos, ainda que os valores numéricos variem conforme os dados que
ele digitar.

**Acceptance Scenarios**:

1. **Given** uma instalação limpa, **When** o leitor executa o exemplo completo, **Then**
   fabricante, modelo, variante, exemplar, fotos, aquisição, localização e coleção ficam
   cadastrados e visíveis nas telas indicadas.
2. **Given** o exemplo concluído, **When** o leitor abre painel e financeiro, **Then**
   entende por que os valores aparecem daquela forma e consegue refazer o cálculo.
3. **Given** um recurso usado no exemplo que não existe no app, **When** o capítulo foi
   escrito, **Then** a etapa não foi inventada: o texto explica a alternativa real ou
   registra a ausência.

---

### User Story 4 - Ler e imprimir o manual em PDF (Priority: P3)

Um usuário prefere ler no tablet, imprimir ou arquivar o manual; ele recebe um PDF com capa,
sumário, numeração, capturas legíveis e diagramas, coerente com a versão Markdown.

**Why this priority**: é formato de entrega exigido, mas o valor principal está no conteúdo
já coberto pelas histórias anteriores.

**Independent Test**: o PDF é aberto em um leitor comum, o sumário leva às seções e todas as
imagens referenciadas existem e são legíveis.

**Acceptance Scenarios**:

1. **Given** o PDF entregue, **When** o leitor usa o sumário, **Then** chega ao capítulo
   correspondente e a paginação está correta.
2. **Given** uma captura citada no Markdown, **When** o PDF é conferido, **Then** a mesma
   captura aparece no capítulo e nenhum caminho de imagem está quebrado.
3. **Given** uma tela que não pôde ser capturada, **When** o capítulo é lido, **Then** há
   aviso explícito da ausência, sem imagem genérica apresentada como captura real.

---

### Edge Cases

- Tela implementada sem caminho de navegação alcançável na versão atual: documentar o
  código e registrar que a captura/execução não foi possível.
- Funcionalidade parcial (por exemplo, relatórios que geram somente o inventário):
  documentar apenas o que existe, sem herdar promessas de documentos antigos.
- Fluxo que fecha o banco (restauração de backup): descrever a necessidade de reabrir o app
  e registrar se a execução manual foi feita ou não.
- Recurso que depende de permissão do sistema negada (câmera, notificações): explicar o
  comportamento observado e a alternativa.
- Tela que exibe dados de exemplo: usar somente registros fictícios, nunca dados pessoais.
- Rótulo que muda com plural, tamanho de fonte ou estado vazio: registrar o texto real e a
  condição em que aparece.
- Elemento sem `contentDescription` ou ícone sem rótulo visível: descrever forma e posição,
  e registrar a limitação de acessibilidade quando aplicável.
- Funcionalidade encontrada no código mas inacessível pela interface: não apresentá-la como
  disponível; listar em `LIMITACOES_DOCUMENTACAO.md`.

## Requirements *(mandatory)*

### Escopo e fidelidade ao aplicativo

- **FR-001**: O manual MUST ser escrito em português brasileiro e refletir exclusivamente o
  aplicativo implementado na versão documentada, identificada por versão, versionCode,
  commit e data.
- **FR-002**: O manual MUST cobrir os 30 destinos de navegação existentes: Painel; Catálogo;
  Busca; Cadastro inicial (fabricante/modelo/escala); Formulário de variante e primeiro
  exemplar; Incompletos; Edição de modelo; Adicionar exemplar; Lista de exemplares; Detalhe
  do exemplar; Coleções; Detalhe de coleção; Formulário de coleção; Séries; Leitura de
  código de barras; Duplicados; Negociação; Lista de desejos; Formulário de desejo;
  Financeiro; Edição de variante; Localizações; Conteúdo da localização; Ajustes; Backup;
  Relatórios; Importação; Dados da coleção; Sobre; Privacidade.
- **FR-003**: O manual MUST explicar a estrutura do domínio — fabricante, modelo, variante,
  exemplar, série, cor, coleção, localização, aquisição, avaliação e fotografia — com
  diagrama de relacionamentos e exemplos Hot Wheels.
- **FR-004**: O manual MUST conter um capítulo de primeiros passos que leve uma instalação
  vazia até uma coleção pequena com backup, na ordem em que o app realmente permite.
- **FR-005**: O manual MUST conter um exemplo completo com Nissan Skyline GT-R R34, ano
  2025, cor azul, escala 1:64, mostrando o mesmo registro nas telas em que ele aparece.
- **FR-006**: O manual MUST documentar o menu inferior de cinco abas e cada entrada de
  Ajustes, sem tratar os submenus como se fossem acessíveis apenas pela aba principal.

### Detalhamento por tela e elemento

- **FR-007**: Cada tela MUST seguir o padrão: finalidade; como acessar; identificação visual;
  descrição de cada elemento; como utilizar; exemplos; regras e validações; mensagens e
  situações especiais; dicas; problemas comuns.
- **FR-008**: Cada botão, campo, ícone, opção, filtro, seletor, indicador e diálogo visível
  MUST ter nome, localização, aparência, finalidade, ação, dados necessários, resultado
  esperado e relação com outras funções.
- **FR-009**: Os rótulos documentados MUST ser os textos exibidos ao usuário (recursos de
  idioma), não nomes de funções, variáveis ou rotas internas.
- **FR-010**: O manual MUST documentar os estados vazio, carregando, erro e sucesso de cada
  tela em que eles existam, incluindo textos de confirmação e de erro.
- **FR-011**: O manual MUST explicar as regras de validação e duplicidade realmente
  aplicadas, incluindo campos obrigatórios, limites, normalização de texto e critérios de
  identificação de duplicados.
- **FR-012**: O manual MUST distinguir reconhecimento automático (código de barras/OCR) de
  identificação efetiva do carrinho, explicando o que o app sugere, o que o usuário confirma
  e quais são os limites.
- **FR-013**: O manual MUST documentar os cálculos financeiros reais (aquisição, frete,
  descontos/custos existentes, total investido, avaliação e valor estimado) com exemplos
  numéricos conferíveis.
- **FR-014**: O manual MUST documentar backup, restauração, exportação aberta, relatórios e
  importação com os formatos, conteúdos, validações e consequências reais, sem prometer
  tipos de relatório ou dados que não sejam gerados.
- **FR-015**: O manual MUST registrar recursos ausentes ou parciais encontrados durante a
  investigação, sem inventar telas, campos ou opções para completar capítulos.

### Evidências visuais

- **FR-016**: As capturas MUST vir do app em execução, em emulador ou aparelho, com dados
  fictícios de demonstração; imagens genéricas não podem ser apresentadas como capturas.
- **FR-017**: Cada captura MUST ter legenda, indicar a tela e o estado representado, e ser
  referenciada por caminho relativo a partir dos documentos.
- **FR-018**: Botões e áreas relevantes das capturas MUST ser identificados com números ou
  legendas quando a legibilidade permitir; diagramas MUST ser usados para relacionamentos e
  fluxos.
- **FR-019**: Quando uma tela ou estado não puder ser executado/capturado, o documento MUST
  dizer isso no ponto de uso e em `LIMITACOES_DOCUMENTACAO.md`, explicando o motivo.

### Entregáveis e controle de qualidade

- **FR-020**: A entrega MUST conter `MANUAL_USUARIO_COMPLETO.md`,
  `MANUAL_USUARIO_COMPLETO.pdf`, `INDICE_FUNCIONALIDADES.md`, `MATRIZ_COBERTURA.md`,
  `LIMITACOES_DOCUMENTACAO.md` e uma pasta de capturas com nomes estáveis.
- **FR-021**: `INDICE_FUNCIONALIDADES.md` MUST listar telas, elementos, procedimentos e
  entidades documentados, com vínculo para o capítulo correspondente.
- **FR-022**: `MATRIZ_COBERTURA.md` MUST relacionar cada destino e funcionalidade
  identificada ao capítulo/seção que a documenta, marcando explicitamente o que não foi
  documentado e por quê; nenhuma linha pode ficar sem destino.
- **FR-023**: O PDF MUST ter capa com nome e versão documentada, sumário navegável,
  numeração de páginas, cabeçalho/rodapé, títulos hierarquizados, tabelas, diagramas e
  capturas legíveis; o conteúdo MUST ser coerente com o Markdown.
- **FR-024**: Antes da conclusão, MUST haver revisão técnica (fidelidade ao código e aos
  textos), editorial (clareza, consistência e ortografia) e de validação (execução dos
  procedimentos possíveis), com os resultados registrados.
- **FR-025**: A atividade MUST ser somente documental: nenhum arquivo de código, recurso,
  banco, migração, dependência ou configuração do app pode ser alterado; defeitos
  encontrados vão para relatório separado, não são corrigidos nesta entrega.

### Key Entities *(include if feature involves data)*

- **Tela**: destino navegável ou estado de tela com identidade própria; possui finalidade,
  caminho de acesso, elementos e regras.
- **Elemento de interface**: botão, campo, ícone, opção, filtro, indicador, diálogo ou aba;
  possui rótulo visível, posição, ação e resultado.
- **Procedimento**: sequência numerada de passos que produz um resultado no app; possui
  pré-condições, validações e mensagens.
- **Entidade de domínio**: fabricante, modelo, variante, exemplar, série, cor, coleção,
  localização, aquisição, avaliação e fotografia, com seus relacionamentos reais.
- **Evidência visual**: captura ou diagrama com origem, data, tela/estado e legenda.
- **Lacuna documental**: funcionalidade não confirmada, não executável ou não ilustrável,
  com motivo e impacto para o leitor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos destinos de navegação implementados aparecem na matriz de cobertura
  com uma seção correspondente; uma conferência automatizada da lista de rotas acusa zero
  destinos sem vínculo.
- **SC-002**: Para cada tela documentada, 100% dos elementos interativos identificados na
  inspeção do código aparecem na tabela de elementos da seção, sem item «a definir».
- **SC-003**: Um leitor sem conhecimento prévio conclui o roteiro de primeiros passos em uma
  instalação limpa usando apenas o manual, sem bloqueios e sem precisar de explicação oral.
- **SC-004**: O exemplo do Nissan Skyline GT-R R34 é reproduzível e os valores exibidos no
  painel/financeiro podem ser recalculados a partir dos dados do exemplo.
- **SC-005**: 100% das referências de imagem nos documentos apontam para arquivos existentes;
  capturas ausentes estão justificadas em `LIMITACOES_DOCUMENTACAO.md`.
- **SC-006**: Os cinco arquivos obrigatórios e a pasta de capturas existem, e o PDF contém
  todos os capítulos do Markdown, com sumário e numeração corretos.
- **SC-007**: Nenhuma afirmação do manual descreve funcionalidade que não exista no código
  da versão documentada; a amostra de revisão técnica não encontra promessas de recursos
  apenas planejados.
- **SC-008**: A entrega final não altera nenhum arquivo do aplicativo fora da pasta de
documentação e das capturas; a conferência de alterações mostra apenas arquivos
  documentais e imagens.

## Assumptions

- A versão documentada é a instalada no momento da captura (`0.1.0`, versionCode 1); se o
  código mudar durante a produção, o manual registra o commit usado e a diferença.
- As capturas serão feitas no emulador `diecast_test` (API 36) com dados fictícios, salvo se um
  aparelho físico estiver disponível; a ausência de aparelho físico não impede a entrega,
  mas deve ser registrada quando afetar uma evidência.
- O manual cobre também telas de recursos declarados fora do escopo da v1 (importação,
  relatórios e reconhecimento), porque elas existem no app e são alcançáveis.
- «Manual completo» significa cobertura de todas as telas e elementos implementados, não
  cobertura de funcionalidades planejadas em especificações antigas.
- O PDF será gerado a partir do Markdown com ferramenta local disponível; se a ferramenta
  não reproduzir sumário ou paginação, isso será registrado como limitação, não omitido.
- Registros de demonstração usam nomes e valores fictícios; nenhuma captura expõe dados
  pessoais do desenvolvedor ou de terceiros.
