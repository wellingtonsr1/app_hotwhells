# Feature Specification: Miniatura dos carrinhos na tela Coleção

**Feature Branch**: `003-colecao-miniaturas`

**Created**: 2026-11-23

**Status**: Draft

**Input**: User description: "Melhoria visual da tela Coleção: cada cartão da listagem deve exibir uma miniatura fotográfica do carrinho à esquerda, com nome do modelo, fabricante, ano e quantidade de exemplares à direita, preservando pesquisa, filtros, navegação, botão de inclusão e a identidade visual atual."

## Contexto

A tela **Coleção** é a entrada do aplicativo e lista o acervo agrupado por
**variante** de modelo. Cada cartão hoje apresenta texto (nome do modelo, fabricante,
ano e quantidade de exemplares) e um atalho para os exemplares daquela variante.
Quem tem muitos carrinhos parecidos — o mesmo modelo em cores e edições diferentes —
precisa ler cada linha para saber de qual carrinho se trata, o que torna a rolagem
lenta e a conferência visual do acervo pouco confiável.

A melhoria consiste em mostrar, em cada cartão, uma **miniatura fotográfica** já
existente no acervo do próprio usuário, aproximando a leitura da listagem da forma
como o colecionador reconhece seus carrinhos: pela imagem, e não pelo texto.

A atividade é exclusivamente de apresentação. Ela não cria, altera, move nem remove
fotografias, cadastros, relacionamentos ou regras de negócio.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconhecer o carrinho pela imagem na listagem (Priority: P1)

Um colecionador abre a tela Coleção para conferir o que tem. Cada linha do acervo
mostra a foto de um dos exemplares daquela variante, ao lado do nome, fabricante, ano
e quantidade de exemplares, de modo que ele identifique o carrinho de imediato e
prossiga a rolagem sem precisar ler cada texto.

**Why this priority**: é o valor central do pedido — sem a miniatura correta, a
melhoria não existe. Entregue sozinha, ela já melhora a leitura da listagem.

**Independent Test**: cadastrar variantes com foto (uma e várias fotos) e sem foto;
abrir a Coleção e verificar, linha a linha, qual imagem aparece e qual placeholder
aparece — sem usar nenhuma outra tela.

**Acceptance Scenarios**:

1. **Given** uma variante com exemplar que possui fotografia principal, **When** o
   usuário abre a Coleção, **Then** o cartão dessa variante exibe a fotografia
   principal à esquerda das informações.
2. **Given** uma variante cujo exemplar possui várias fotos e nenhuma marcada como
   principal, **When** o usuário abre a Coleção, **Then** o cartão exibe uma única
   foto escolhida de forma determinística (a mesma em toda abertura, enquanto os dados
   não mudarem).
3. **Given** uma variante com vários exemplares, **When** o usuário abre a Coleção,
   **Then** o cartão exibe uma única foto representativa dessa variante e a contagem
   correta de exemplares possuídos.
4. **Given** uma variante sem nenhuma fotografia, **When** o usuário abre a Coleção,
   **Then** o cartão exibe um espaço reservado com ícone de carrinho, com a mesma
   dimensão dos cartões com foto, sem erro e sem espaço vazio desproporcional.
5. **Given** a lista aberta, **When** o usuário toca no cartão, **Then** o
   comportamento atual é preservado; **When** toca em «Ver exemplares», **Then** a
   listagem de exemplares daquela variante é aberta.

---

### User Story 2 - Manter as informações e a navegação existentes (Priority: P1)

Um colecionador que já usa o aplicativo continua encontrando no cartão tudo o que
encontrava antes — nome, fabricante, ano e quantidade de exemplares — e continua
chegando aos exemplares pelo mesmo atalho, agora com a foto ao lado.

**Why this priority**: a melhoria é visual; perder informação ou quebrar um caminho de
navegação existente seria regressão, não melhoria.

**Independent Test**: comparar o conteúdo de cada cartão antes e depois da mudança e
percorrer o caminho cartão → exemplares para uma variante de cada tipo (com e sem foto).

**Acceptance Scenarios**:

1. **Given** uma variante com fabricante e ano preenchidos, **When** o cartão é
   exibido, **Then** nome, fabricante, ano e contagem de exemplares continuam visíveis
   e legíveis.
2. **Given** uma variante sem ano cadastrado, **When** o cartão é exibido, **Then** o
   ano simplesmente não aparece, sem texto inventado, sem «null» e sem valor padrão.
3. **Given** uma variante sem exemplares, **When** o cartão é exibido, **Then** a
   indicação de ausência de exemplares segue o texto atual da aplicação.
4. **Given** qualquer cartão, **When** o usuário aciona «Ver exemplares», **Then** a
   tela de exemplares da variante correspondente é aberta, e a foto exibida não
   altera qual variante é selecionada.

---

### User Story 3 - Continuar usando busca, filtros e inclusão (Priority: P2)

Um colecionador procura um carrinho pela busca, abre os atalhos de incompletos,
coleções e duplicados e cadastra um novo item pelo botão flutuante, exatamente como
antes.

**Why this priority**: esses são os caminhos de uso diário da tela; a mudança de cartão
não pode afetá-los. É verificável de forma independente, percorrendo cada atalho.

**Independent Test**: abrir busca, incompletos, coleções e duplicados, aplicar um
termo de pesquisa e cadastrar um item pelo botão «+ Mini».

**Acceptance Scenarios**:

1. **Given** a Coleção aberta, **When** o usuário pesquisa por um termo, **Then** os
   resultados continuam corretos e os cartões resultantes seguem o novo formato.
2. **Given** a Coleção aberta, **When** o usuário aciona os atalhos de incompletos,
   coleções, duplicados ou leitura de código de barras, **Then** cada atalho abre a
   tela correspondente como antes.
3. **Given** a Coleção aberta, **When** o usuário toca no botão flutuante de inclusão,
   **Then** o fluxo de cadastro abre no mesmo ponto e posição atuais.

---

### Edge Cases

- **Foto registrada mas arquivo ausente ou ilegível**: o cartão não pode quebrar nem
  exibir área vazia; deve degradar para o espaço reservado, mantendo as dimensões.
- **Variante cujo único exemplar foi vendido ou arquivado**: a miniatura continua
  exibindo a foto desse exemplar, preservando a continuidade visual da variante entre
  idas e voltas ao acervo; a contagem permanece a de exemplares possuídos.
- **Fotos de exemplares diferentes na mesma variante**: a escolha deve ser estável e
  repetível, nunca alternar entre aberturas sem mudança nos dados.
- **Nome de modelo muito longo e fonte do sistema ampliada**: a miniatura não pode ser
  comprimida nem empurrar o texto para fora do cartão.
- **Lista vazia**: a tela mantém o estado vazio atual, sem cartões e sem miniaturas.
- **Lista muito grande**: a rolagem permanece fluida; miniaturas fora da área visível
  não são carregadas.
- **Vários cartões com a mesma foto**: cada cartão exibe somente a foto da própria
  variante; nenhuma imagem é copiada ou duplicada em disco.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Cada cartão da tela Coleção MUST exibir uma miniatura da fotografia do
  carrinho no lado esquerdo, com proporção quadrada, cantos arredondados, alinhamento
  vertical centralizado e espaçamento em relação ao bloco de informações.
- **FR-002**: A miniatura MUST usar apenas fotografias já cadastradas no aplicativo
  para a variante representada no cartão. Nenhuma imagem nova, fictícia ou de outro
  modelo pode ser gerada ou utilizada.
- **FR-003**: A escolha da fotografia MUST priorizar a fotografia que o usuário definiu
  como principal; não havendo principal, MUST usar outra fotografia válida vinculada
  à variante; havendo várias candidatas, a escolha MUST ser determinística e igual em
  toda abertura enquanto os dados não mudarem.
- **FR-004**: Uma variante que não possua nenhuma fotografia MUST exibir um espaço
  reservado com ícone de carrinho, mantendo as mesmas dimensões do cartão com foto.
- **FR-005**: O cartão MUST continuar exibindo nome do modelo, fabricante, ano (quando
  houver) e quantidade de exemplares, sem remover ou substituir informações atuais.
- **FR-006**: Campos ausentes MUST continuar ausentes, sem valor inventado, texto de
  preenchimento ou indicação de erro.
- **FR-007**: A miniatura MUST NOT interferir na identificação da variante: tocar no
  cartão e acionar «Ver exemplares» MUST manter o comportamento atual e abrir os
  exemplares da variante correta. Tocar na própria fotografia MUST abrir a ampliação da
  imagem, sem navegar, sem trocar a variante selecionada e sem alterar o cadastro.
- **FR-008**: Pesquisa, atalho de incompletos, coleções, duplicados, leitura de código
  de barras e ordenação atual da listagem MUST continuar funcionando como antes.
- **FR-009**: O botão flutuante de inclusão MUST permanecer na mesma posição e com o
  mesmo comportamento.
- **FR-010**: A listagem MUST obter a fotografia representativa junto com os dados de
  cada cartão, sem uma consulta adicional por cartão e sem leitura de arquivo na
  thread principal.
- **FR-011**: O carregamento das miniaturas MUST ser sob demanda (apenas cartões
  visíveis ou próximos da área visível) e MUST reutilizar o mecanismo de carregamento e
  cache de imagens já existente no aplicativo.
- **FR-012**: Falha ao ler ou decodificar um arquivo de foto MUST resultar no espaço
  reservado, nunca em exceção visível, cartão colapsado ou espaço desproporcional.
- **FR-013**: A aparência MUST respeitar a identidade visual atual: tema escuro, fundo
  preto, cartões em cinza escuro, textos claros, destaques em vermelho/coral,
  tipografia e componentes Material 3, navegação inferior inalterada.
- **FR-014**: O layout MUST manter altura consistente entre cartões, sem cartões
  excessivamente altos, e funcionar em telas Android a partir de 360 dp de largura,
  em retrato e paisagem, respeitando o tamanho de fonte configurado no sistema.
- **FR-015**: A miniatura MUST ter descrição acessível que permita ao leitor de tela
  identificar o carrinho a que a imagem pertence; o espaço reservado MUST ser
  anunciado como ausência de foto, sem poluir a leitura do cartão.

### Key Entities *(include if data involved)*

- **Modelo**: veículo em miniatura, independente de cor e edição; fornece o nome
  exibido no cartão e o fabricante.
- **Variante**: versão específica do modelo (cor, rodas, edição) e a unidade que a
  tela Coleção lista; determina quais fotos podem representar o cartão.
- **Exemplar**: unidade física possuída pelo colecionador, vinculada a uma variante;
  fornece a quantidade exibida e as fotografias candidatas.
- **Fotografia**: imagem já cadastrada, vinculada a um exemplar, com indicação de
  principal ou não e uma ordem; é a única origem possível da miniatura.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos cartões da Coleção exibem uma miniatura ou o espaço reservado
  com ícone de carrinho, sem cartão sem área de imagem.
- **SC-002**: Em uma amostra fixa de 20 variantes — 5 com foto principal, 5 com várias
  fotos sem principal, 5 sem foto e 5 com foto de exemplar vendido —, 100% das
  miniaturas correspondem à variante do próprio cartão e, quando existe, à foto
  principal escolhida pelo usuário.
- **SC-003**: Uma listagem de 500 variantes abre em menos de 1 segundo e permanece com
  rolagem fluida, com as miniaturas carregadas apenas conforme entram na tela.
- **SC-004**: Nenhuma informação hoje visível no cartão desaparece e nenhum caminho
  atual da tela (pesquisa, atalhos, cadastro, exemplares) deixa de funcionar.
- **SC-005**: Com 10% das variantes sem foto e 5% com arquivo de foto inválido, nenhuma
  falha ou aviso de erro é apresentado ao usuário; nesses cartões aparece o espaço
  reservado.

## Assumptions

- A tela chamada **Coleção** é a listagem de variantes do acervo, com contagem de
  exemplares possuídos; é a tela de entrada do aplicativo.
- As fotografias já são cadastradas por exemplar, com uma delas marcada como principal
  e as demais ordenadas; não há fotografia vinculada diretamente ao modelo ou à
  variante.
- A miniatura representa a variante por meio da foto de um de seus exemplares; a
  preferência é pela foto principal, e a estabilidade da escolha é garantida por uma
  ordenação determinística.
- O mecanismo de carregamento e cache de imagens existente é suficiente; nenhuma nova
  dependência é necessária.
- Nenhuma migração de banco, alteração de esquema, mudança de regra de cadastro,
  backup ou financeiro é necessária ou permitida por esta melhoria.
- Tocar no cartão, fora da miniatura, continua abrindo o cadastro rápido, e «Ver
  exemplares» continua sendo o atalho para a lista de exemplares; esses caminhos estão
  fora de discussão nesta feature.
- Modelos e exemplares já cadastrados continuam válidos sem qualquer conversão de
  dados.
- Uma fotografia pode vir de qualquer exemplar da variante, possuído ou não: a foto de
  um exemplar vendido ou arquivado continua representando a variante. Decisão tomada em
  2026-11-23; a contagem de exemplares é que distingue o que o usuário possui hoje.
- Tocar na miniatura amplia a imagem; é um comportamento intencional da Coleção e não
  substitui o toque no cartão. Decisão tomada em 2026-11-23.
