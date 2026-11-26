# Feature Specification: Miniatura dos carrinhos na tela Coleção

**Feature Branch**: `003-colecao-miniaturas`

**Created**: 2026-11-23

**Status**: Implemented (v1) — SC-003 atestado só na metade «abre em menos de 1 s» · Esclarecida em 2026-11-26 (CL-003-01/CL-003-02): identidade visual por modo de aparência, na spec e no contrato do cartão

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

## Clarifications

### Session 2026-11-26

- Q: A FR-013 exige "tema escuro, fundo preto, cartões em cinza escuro" como identidade visual. A Spec 006 introduz a escolha entre Sistema, Claro e Escuro. A identidade deixa de valer no modo Claro? → A: **A identidade é o conjunto de tokens, tipografia, formas e hierarquia, não a paleta escura como obrigação permanente.** No modo **Escuro** (e em **Sistema** com o Android escuro), a FR-013 MUST ser cumprida exatamente como está: fundo `InkBlack`, cartões em `Graphite`/`SmokyGrey`, textos `Chalk`, destaques `FlameRedDark`/`HotOrangeLight`. No modo **Claro**, MUST usar os tokens claros da mesma paleta (`Chalk`, `PureWhite`, `InkBlack`, `MutedGrey`, `FlameRed`, `HotOrange`), com a mesma tipografia, formas, espaçamentos e hierarquia — **sem cor nova**.
- Q: A FR-013 precisa ser renumerada ou reescrita? → A: **Não.** O número é preservado e o texto recebe apenas o esclarecimento de modo; nenhum critério de aceite (SC-001–SC-005) muda.

**Consequências registradas**: nenhuma mudança de layout, conteúdo, acessibilidade ou desempenho no modo escuro; o modo claro passa a ser explicitamente permitido, com contraste verificado pela Spec 006 (FR-422/FR-423).

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
- **FR-013**: A aparência MUST preservar a identidade visual do produto — paleta,
  tipografia, formas, espaçamentos e componentes Material 3 — em todas as opções de
  aparência suportadas.
  - **No modo Escuro** (e em **Sistema** quando o Android está escuro): fundo preto
    (`InkBlack`), cartões em cinza escuro (`Graphite`/`SmokyGrey`), textos claros
    (`Chalk`), destaques em vermelho/coral (`FlameRedDark`/`HotOrangeLight`), navegação
    inalterada.
  - **No modo Claro**: os tokens claros da mesma paleta (`Chalk` de fundo, `PureWhite`
    de superfície, `InkBlack` de texto, `MutedGrey` secundário, `FlameRed`/`HotOrange`
    de destaque), com a mesma tipografia, formas, espaçamentos, hierarquia e navegação.

  Nenhuma cor fora dos tokens existentes MUST ser introduzida por este esclarecimento,
  e a troca de aparência MUST NOT alterar layout, conteúdo ou regra de negócio.
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
  *(A abertura está medida — 83 ms de mediana; a fluidez da rolagem segue pendente de
  aparelho físico, ver «Estado da implementação».)*
- **SC-004**: Nenhuma informação hoje visível no cartão desaparece e nenhum caminho
  atual da tela (pesquisa, atalhos, cadastro, exemplares) deixa de funcionar.
- **SC-005**: Com 10% das variantes sem foto e 5% com arquivo de foto inválido, nenhuma
  falha ou aviso de erro é apresentado ao usuário; nesses cartões aparece o espaço
  reservado.

## Estado da implementação (2026-11-23)

As três histórias estão implementadas e verificadas neste workspace; a evidência de cada
comando e cada limitação de análise estão em [analysis.md](./analysis.md), e a marcação
tarefa a tarefa em [tasks.md](./tasks.md).

| Critério | Situação | Prova |
|---|---|---|
| SC-001 | Atendido | `amostraDoSc002MantemCadaMiniaturaNoProprioCartao` (20 cartões) e `listaLongaNaoCompoeCartoesForaDaTela` (60) |
| SC-002 | Atendido | `VariantRepositoryTest` (seleção da foto) + teste de cartão da amostra fixa |
| SC-003 | **Parcial** | Abertura medida por macrobenchmark: mediana **83 ms**, p90 127 ms em 10 aberturas. «Rolagem fluida» sem prova |
| SC-004 | Atendido | `CatalogFlowTest` + `SearchScreenTest` + testes do cartão |
| SC-005 | Atendido | `carrinhoSemFotoMostraMarcadorAcessivel`, `arquivoInvalidoCaiNoMarcadorSemQuebrarOCartao`; semente na proporção |

A medição de desempenho roda pelo Gradle (`./gradlew
:benchmark:connectedBenchmarkBenchmarkAndroidTest`, que instala o `:app:benchmark`) ou pelo
roteiro manual de [quickstart.md](./quickstart.md).

### Limitações que seguem abertas

1. **SC-003 — «rolagem fluida» sem prova.** A medição é do AVD `hw_test` (API 36, x86_64,
   GPU por software): o P90 de 40,5 ms por quadro (~25 fps) descreve a renderização do
   emulador, não um aparelho. Falta repetir a medição em hardware físico para atender a
   Constituição V.8; até lá o SC-003 não pode ser declarado atendido por inteiro.
2. **Números absolutos não são meta de aparelho.** A mediana de 83 ms serve para comparar
   versões do app no mesmo ambiente; a primeira abertura (1.230 ms) inclui instalação do
   perfil e não representa a abertura típica.
3. **Sem cobertura de aparelho de entrada.** Nem a fluidez nem o consumo de memória foram
   exercitados em hardware modesto (2 GB de RAM, tela de 60 Hz), que é onde a rolagem com
   500 miniaturas tem o pior caso.

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
