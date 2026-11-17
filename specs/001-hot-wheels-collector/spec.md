# Feature Specification: Hot Wheels Collector — Aplicativo Android de Gestão de Coleções 1:64

**Feature Branch**: `001-hot-wheels-collector`

**Created**: 2026-11-16

**Status**: Draft

**Input**: Aplicativo Android offline-first para catalogação, organização e gestão profissional de coleções de miniaturas em escala 1:64 (Hot Wheels e outros fabricantes), cobrindo os 12 módulos: dashboard, cadastro de miniaturas, modelo/variante/exemplar, séries e coleções, lista de desejos, duplicados e trocas, controle financeiro, localização física, fotografias com reconhecimento assistido, leitura de código de barras, pesquisa e filtros, e relatórios — sem servidor, com importação/exportação por arquivo e PDF gerado no aparelho.

**Plataforma**: Android (uso primário em celular), funcionamento integral sem conexão.

**Constituição aplicável**: v1.1.0 ratificada (`.specify/memory/constitution.md`).

## Clarifications

### Session 2026-11-16

- Q: Com qual stack o aplicativo Android será construído? → A: Nativo Kotlin + Jetpack Compose, com Room para persistência local e ML Kit on-device para OCR e leitura de código de barras.
- Q: Como o aplicativo será distribuído na versão 1? → A: Sideload de arquivo assinado na v1, mantendo o pacote de publicação (AAB) e os requisitos de loja prontos para publicação futura, sem publicar automaticamente.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar mais um exemplar de um modelo já catalogado (Priority: P1)

O colecionador chega da loja com uma miniatura repetida ou uma nova variante de um modelo que já existe no catálogo. Ele abre o aplicativo, encontra o modelo, toca em "Adicionar exemplar" e registra apenas o que é específico daquela unidade física: condição, localização, preço, data e observações.

**Why this priority**: é a ação mais frequente da vida real e a que falha em quase todo app de coleção. Se registrar um exemplar repetido exigir redigitar todos os dados do carro, o usuário para de usar o sistema e a coleção volta para a planilha.

**Independent Test**: com um modelo e uma variante já cadastrados, é possível adicionar um segundo exemplar sem digitar nome, série ou cor, e o resultado aparece corretamente no inventário com dois exemplares e um modelo distinto.

**Acceptance Scenarios**:

1. **Given** uma variante cadastrada com um exemplar, **When** o usuário toca em "Adicionar exemplar" e confirma, **Then** o sistema cria um segundo exemplar, mantém a contagem em 1 modelo distinto e 2 exemplares, e não exige nenhum dado herdado da variante.
2. **Given** o formulário de exemplar aberto com dados preenchidos, **When** o processo do aplicativo é encerrado pelo sistema operacional e o usuário retorna, **Then** os dados digitados são recuperados sem perda.
3. **Given** um exemplar em que o usuário não conhece o preço pago, **When** ele deixa o campo em branco, **Then** o exemplar é salvo com o valor registrado como "não informado" (nunca R$ 0,00).

---

### User Story 2 - Catalogar uma miniatura nova do zero, com fotos (Priority: P1)

Ao cadastrar um carro que ainda não existe no catálogo, o colecionador informa fabricante, modelo, série, ano, cores, condição e fotografias. Alguns dados podem ser desconhecidos no momento da compra e ficam explicitamente marcados como desconhecidos para preenchimento posterior.

**Why this priority**: sem o cadastro inicial completo não existe coleção para organizar; e o cadastro é a única porta de entrada de dados — se for frágil, tudo a jusante nasce errado.

**Independent Test**: partindo de uma instalação vazia, é possível cadastrar um modelo, sua primeira variante e um exemplar com duas fotos, com o aplicativo em modo avião, e o item aparece corretamente no dashboard e na busca.

**Acceptance Scenarios**:

1. **Given** o aplicativo sem conexão, **When** o usuário conclui o cadastro de uma miniatura com dados mínimos (fabricante, nome do modelo, escala), **Then** o item é salvo e aparece na lista, marcado como "cadastro incompleto" enquanto faltarem campos desejáveis.
2. **Given** um cadastro com seis fotos de ângulos diferentes, **When** o usuário reabre o item, **Then** as fotos aparecem na ordem dos ângulos e a foto principal é exibida no card da coleção.
3. **Given** uma foto de 8 MB tirada pela câmera, **When** ela é anexada ao exemplar, **Then** é armazenada de forma comprimida, sem dados de localização embutidos e sem travar a interface.

---

### User Story 3 - Ver o estado da coleção em um painel (Priority: P2)

O colecionador abre o aplicativo e vê, de imediato, quantos exemplares físicos possui, quantos modelos distintos, quanto investiu, quanto vale a coleção segundo suas próprias avaliações, quais foram as últimas aquisições e quais coleções estão mais perto de serem completadas.

**Why this priority**: é o motivo pelo qual o usuário abre o aplicativo sem ter uma tarefa específica, mas depende de dados já cadastrados para ter valor.

**Independent Test**: com dados de exemplo já cadastrados, o painel é comparado com uma contagem manual e todos os números coincidem, incluindo a separação entre exemplares e modelos distintos.

**Acceptance Scenarios**:

1. **Given** uma coleção com 120 exemplares e 87 modelos distintos, **When** o painel é aberto, **Then** os dois números aparecem em cartões separados, com rótulos que não permitem confundi-los.
2. **Given** exemplares sem avaliação registrada, **When** o valor estimado é exibido, **Then** o painel informa que o total cobre apenas os itens avaliados, indicando a quantidade restante.
3. **Given** que o usuário acabou de registrar uma compra, **When** volta ao painel, **Then** a aquisição aparece em "últimas aquisições" e o total investido é atualizado.

---

### User Story 4 - Encontrar uma miniatura na coleção (Priority: P2)

O colecionador precisa responder "eu tenho esse?" ou "onde está aquele Skyline azul?". Ele pesquisa por nome, filtra por fabricante, série, cor, condição ou localização, e alterna entre grade, lista, galeria e agrupamentos por coleção, fabricante ou série.

**Why this priority**: uma coleção grande é inútil se não for consultável; é a segunda função mais usada depois do cadastro.

**Independent Test**: com 500 itens cadastrados, uma busca por parte do nome retorna o item correto em menos de 300 milissegundos e os filtros combinados reduzem o resultado sem perder itens válidos.

**Acceptance Scenarios**:

1. **Given** um modelo chamado "Nissan Skyline GT-R (R34)", **When** o usuário digita "skyline r34", **Then** o item é encontrado, ignorando diferenças de maiúsculas, acentos e ordem parcial das palavras.
2. **Given** filtros de fabricante "Hot Wheels" e condição "lacrado" ativos, **When** o usuário adiciona o filtro de série "Car Culture", **Then** o resultado contém apenas itens que atendem aos três critérios e a contagem é exibida.
3. **Given** resultados exibidos em grade, **When** o usuário alterna para "agrupado por coleção", **Then** os itens são reorganizados com cabeçalhos por coleção e a contagem de cada grupo.

---

### User Story 5 - Acompanhar o progresso de uma série ou coleção (Priority: P2)

O colecionador quer completar a série "Car Culture 2024". Ele cadastra os itens conhecidos daquela coleção, vê o percentual concluído, a lista do que falta e quais variantes já possui.

**Why this priority**: transforma o catálogo em instrumento de decisão de compra; depende de série/coleção e de cadastro já existentes.

**Independent Test**: com uma coleção de 10 itens conhecidos e 6 possuídos, o progresso é exibido como 60% e a lista de faltantes contém exatamente 4 itens.

**Acceptance Scenarios**:

1. **Given** uma coleção com 10 itens conhecidos e 6 adquiridos, **When** o usuário abre a coleção, **Then** vê 60% de conclusão, 4 faltantes e 6 adquiridos, com a contagem de exemplares separada da de itens.
2. **Given** uma variante adquirida de um item da coleção, **When** o item é exibido, **Then** o sistema mostra "adquirido" com a variante possuída, e não confunde com outra variante do mesmo item.
3. **Given** um item que o usuário decide não colecionar, **When** ele marca o item como "não colecionável", **Then** o item sai do cálculo de percentual sem ser apagado.

---
### User Story 6 - Transformar um desejo em aquisição (Priority: P3)

O colecionador mantém uma lista de miniaturas que quer comprar, com prioridade, valor máximo disposto, preço encontrado e link de referência. Quando compra, converte o item em aquisição sem perder o histórico de quando passou a desejar aquele item nem o preço que havia registrado.

**Why this priority**: organiza a intenção de compra e fecha o ciclo com o financeiro; depende de catálogo e aquisição já existentes.

**Independent Test**: um item da lista de desejos pode ser convertido em exemplar adquirido, e o registro resultante contém data de aquisição, preço pago e a data em que o desejo foi criado.

**Acceptance Scenarios**:

1. **Given** um desejo com prioridade alta e valor máximo de R$ 80,00, **When** o usuário o converte em aquisição pagando R$ 55,00, **Then** o exemplar é criado, o desejo é marcado como adquirido e o histórico preserva a prioridade e o valor máximo original.
2. **Given** um desejo sem variante especificada, **When** o usuário compra uma variante específica, **Then** é possível vinculá-la na conversão sem criar um segundo modelo.
3. **Given** um item desejado, **When** o usuário consulta o dashboard, **Then** esse item não é contado como exemplar possuído.

---

### User Story 7 - Gerenciar duplicados e negociar (Priority: P3)

Com o tempo o colecionador acumula exemplares repetidos da mesma variante. Ele precisa vê-los como duplicados, decidir quais ficam disponíveis para troca ou venda, reservar os já negociados e registrar o histórico da negociação.

**Why this priority**: evita vender o exemplar que deveria ficar na coleção e é a base de credibilidade em negociações; depende da separação modelo/variante/exemplar estar correta.

**Independent Test**: com três exemplares da mesma variante e um de outra variante do mesmo modelo, a lista de duplicados mostra apenas os três primeiros.

**Acceptance Scenarios**:

1. **Given** dois exemplares da mesma variante e um de outra variante do mesmo modelo, **When** o usuário abre "duplicados", **Then** apenas os dois exemplares da mesma variante são listados, com indicação de que a outra variante não é duplicata.
2. **Given** um exemplar marcado como disponível para venda por R$ 45,00, **When** o usuário o reserva para uma negociação, **Then** o exemplar passa a "reservado" e não pode ser marcado como disponível para outra negociação sem liberar a primeira.
3. **Given** uma venda concluída por R$ 45,00, **When** o usuário confirma a conclusão, **Then** o exemplar sai do inventário ativo, o valor entra como venda no histórico financeiro e o registro permanece consultável.

---

### User Story 8 - Saber quanto a coleção custou e quanto vale (Priority: P3)

O colecionador registra, para cada exemplar, data, preço pago, frete, desconto, loja, vendedor e forma de aquisição; e registra avaliações com valor, data e fonte. O sistema calcula investimento total, custo médio, gastos por período, por série e por fabricante, e compara o investido com o estimado.

**Why this priority**: é o módulo de maior valor percebido no longo prazo, mas só faz sentido com aquisições registradas de forma consistente.

**Independent Test**: com cinco aquisições conhecidas, o investimento total, o custo médio e o gasto por série coincidem com o cálculo manual, incluindo frete e desconto.

**Acceptance Scenarios**:

1. **Given** um exemplar comprado por R$ 20,00 com R$ 8,00 de frete, **When** o total investido é consultado, **Then** o custo considerado é R$ 28,00.
2. **Given** duas avaliações do mesmo exemplar (R$ 60,00 em maio e R$ 75,00 em agosto), **When** o valor estimado é exibido, **Then** o sistema usa R$ 75,00 como estimativa atual e mantém as duas no histórico.
3. **Given** qualquer tela com valor estimado, **When** o número é exibido, **Then** ele vem acompanhado de identificação como estimativa do colecionador, com data e fonte, nunca como preço oficial de mercado.

---

### User Story 9 - Saber onde cada miniatura está fisicamente (Priority: P3)

O colecionador organiza a coleção em ambientes, armários, estantes, prateleiras, caixas, gavetas e expositores, com estruturas que ele mesmo cria. Cada exemplar tem uma localização atual e todo movimento fica registrado.

**Why this priority**: resolve a dor prática de "não sei onde guardei"; depende de exemplares cadastrados.

**Independent Test**: um exemplar pode ser atribuído a uma caixa, movido para outra e o histórico exibe as duas posições com datas.

**Acceptance Scenarios**:

1. **Given** um exemplar sem localização, **When** o usuário o atribui à "Caixa 12", **Then** o exemplar passa a aparecer na consulta por localização e a Caixa 12 lista esse exemplar.
2. **Given** um exemplar na "Estante A > Prateleira 3", **When** o usuário o move para a "Caixa 12", **Then** a localização atual muda e o histórico registra a origem, o destino e a data.
3. **Given** uma caixa com 30 exemplares, **When** o usuário tenta excluir a caixa, **Then** o sistema exige uma decisão explícita (mover os exemplares ou manter a caixa arquivada) e não apaga nada silenciosamente.

---

### User Story 10 - Gerar relatórios, exportar e fazer backup (Priority: P4)

O colecionador extrai o inventário completo, o investimento, a avaliação patrimonial e a lista de faltantes em CSV, Excel e PDF, tudo gerado no aparelho. Ele também gera um pacote de backup com dados e imagens, e consegue restaurá-lo em um aparelho novo.

**Why this priority**: é a garantia de que a coleção não fica presa ao aplicativo, mas depende de todo o resto estar consistente.

**Independent Test**: gerar um backup, apagar os dados do aplicativo, importar o pacote e verificar que exemplares, fotos, aquisições, avaliações e localizações voltam com os mesmos relacionamentos.

**Acceptance Scenarios**:

1. **Given** uma coleção cadastrada, **When** o usuário exporta o inventário em PDF, **Then** o arquivo é gerado no aparelho, sem rede, com cabeçalho, data de geração, totais e paginação.
2. **Given** um arquivo CSV com 1.000 linhas e 12 inválidas, **When** o usuário importa, **Then** ele vê uma pré-visualização com o resumo (válidas, inválidas, possíveis duplicatas) antes de confirmar, e recebe um relatório com o motivo de cada erro por linha.
3. **Given** um backup gerado, **When** ele é restaurado em aparelho novo, **Then** as fotos continuam vinculadas aos exemplares corretos e o total de exemplares e modelos coincide com o original.

---

### User Story 11 - Identificar uma miniatura por foto ou código de barras (Priority: P4)

Ao fotografar um carro ou ler o código de barras da embalagem, o aplicativo sugere fabricante, modelo, série, ano e possíveis variantes, com nível de confiança. O colecionador aceita ou rejeita campo a campo; nada é gravado sem confirmação.

**Why this priority**: reduz o atrito de cadastro e é o diferencial do produto, mas é opcional e depende de catálogo existente para comparar.

**Independent Test**: com uma foto de um carro já cadastrado, o sistema apresenta sugestões e, ao recusar todas, o cadastro permanece exatamente como estava antes da sugestão.

**Acceptance Scenarios**:

1. **Given** uma foto de um modelo já cadastrado, **When** o reconhecimento termina, **Then** o sistema lista até cinco correspondências com confiança e permite aplicar apenas os campos aceitos pelo usuário.
2. **Given** uma foto sem correspondência confiável, **When** o resultado é inconclusivo, **Then** o sistema informa "não identificado" e não preenche nenhum campo com estimativa inventada.
3. **Given** a permissão de câmera negada, **When** o usuário usa o aplicativo, **Then** todas as outras funções continuam disponíveis e o reconhecimento por foto fica indisponível com explicação clara.

---

### Edge Cases

- Duas variantes do mesmo modelo com cores diferentes: devem contar como um modelo distinto e duas variantes, nunca como duplicata.
- Mesma variante com cinco exemplares: cinco exemplares, um modelo, uma variante; o sistema não deve sugerir mesclar.
- Variante cadastrada por engano em duplicidade (mesmos atributos): o sistema avisa e oferece unir, preservando todos os exemplares e históricos.
- Exemplar cuja variante precisa ser corrigida: a troca de vínculo mantém aquisição, avaliações, fotos e movimentações.
- Exclusão de modelo com variantes e exemplares: bloqueada até decisão explícita, nunca em cascata silenciosa.
- Campo desconhecido durante o cadastro: permanece "não informado" e não entra em cálculos como zero.
- Aquisição sem preço: não deve distorcer custo médio nem total investido; o sistema informa quantos itens estão sem valor.
- Avaliação de item não possuído (desejo): entra no desejo, não no patrimônio.
- Compra de item que já existia na lista de desejos com variante diferente: não deve gerar duplicidade de desejo nem de modelo.
- Importação de arquivo com código de referência repetido em linhas diferentes: aviso de possível duplicidade, com opção de pular, criar mesmo assim ou mesclar.
- Importação de arquivo malformado ou com codificação inesperada: rejeitada com explicação, sem gravar nada parcialmente.
- Armazenamento do aparelho cheio durante o cadastro de fotos: mensagem acionável e cadastro preservado sem fotos incompletas.
- Aplicativo encerrado pelo sistema no meio de uma importação longa: estado deve ser retomável ou revertido, sem base pela metade.
- Rotação de tela com formulário preenchido: nenhum campo perdido.
- Fuso horário e datas: aquisições registradas como data local do colecionador, exibidas de forma consistente após viagem.
- Duas fotos iguais anexadas ao mesmo exemplar: aviso de duplicidade, sem bloqueio.
- Código de barras da embalagem que corresponde a uma caixa com vários carros iguais: o código não identifica a unidade física; o sistema não deve criar exemplar automaticamente.
- Coleção com 10.000 exemplares e 50.000 fotos: listas, filtros e abertura de item continuam responsivos.
- Restauração de backup gerado em versão anterior do aplicativo: migração preserva dados e informa o que não pôde ser convertido.
- Usuário que deseja apagar tudo: exclusão exige dupla confirmação e oferece exportação antes.

---
## Requirements *(mandatory)*

Legenda: **MUST** = obrigatório e verificável; **MUST NOT** = proibido. Cada requisito é verificável por observação do comportamento no aparelho, sem conhecer a implementação.

### Functional Requirements

**Módulo 1 — Dashboard**

- **FR-001**: O painel MUST exibir a quantidade de exemplares físicos e a quantidade de modelos distintos em cartões separados, com rótulos que impeçam confusão entre os dois.
- **FR-002**: O painel MUST exibir a quantidade de variantes distintas registradas.
- **FR-003**: O painel MUST exibir a quantidade de fabricantes distintos presentes na coleção.
- **FR-004**: O painel MUST exibir a quantidade de séries e de coleções, com a contagem de itens em cada uma.
- **FR-005**: O painel MUST exibir a quantidade de exemplares duplicados (mesma variante com mais de um exemplar).
- **FR-006**: O painel MUST exibir a quantidade de itens na lista de desejos, separada de qualquer contagem de itens possuídos.
- **FR-007**: O painel MUST exibir o valor total investido, calculado como soma de preço pago mais frete menos desconto de cada aquisição registrada.
- **FR-008**: O painel MUST exibir o valor estimado da coleção com base na avaliação mais recente de cada exemplar e MUST informar quantos exemplares não possuem avaliação.
- **FR-009**: O painel MUST exibir a diferença entre valor investido e valor estimado, com sinal explícito e identificação de que se trata de estimativa.
- **FR-010**: O painel MUST listar as últimas aquisições e os últimos cadastros, com data e vínculo para abrir o item.
- **FR-011**: O painel MUST listar as coleções ordenadas por percentual de conclusão, no mínimo as cinco mais próximas de completar.
- **FR-012**: Todos os indicadores do painel MUST ser calculados a partir dos dados locais do aparelho, sem depender de conexão.

**Módulo 2 — Cadastro de miniaturas**

- **FR-013**: O cadastro MUST exigir apenas um conjunto mínimo de campos (fabricante, nome do modelo e escala) e MUST permitir salvar com os demais campos não informados.
- **FR-014**: O cadastro MUST aceitar os campos: fabricante, marca da miniatura, nome do modelo, nome do veículo real, fabricante do veículo real, escala, série, coleção, subcoleção, ano de lançamento, ano do veículo representado, número de coleção, número na série, código do fabricante, cor principal, cores secundárias, tipo de carroceria, material, tipo de rodas, tipo de base, país de fabricação, condição da embalagem, condição da miniatura e observações.
- **FR-015**: Cada campo MUST poder assumir o estado "não informado", distinto de vazio, zero ou texto genérico.
- **FR-016**: A escala MUST ter 1:64 como valor padrão e MUST permitir outras escalas.
- **FR-017**: O cadastro MUST permitir uma cor principal e múltiplas cores secundárias.
- **FR-018**: A condição da miniatura MUST admitir, no mínimo: lacrada, solta, danificada, restaurada e incompleta.
- **FR-019**: A condição da embalagem MUST admitir, no mínimo: lacrada, aberta, danificada e sem embalagem.
- **FR-020**: O cadastro MUST permitir anexar múltiplas fotografias por exemplar, categorizadas por ângulo: frente, traseira, lateral esquerda, lateral direita, superior, base, embalagem e detalhe.
- **FR-021**: O cadastro MUST permitir indicar uma foto principal por exemplar, usada nos cartões e relatórios.
- **FR-022**: O sistema MUST validar os dados no momento de salvar e MUST apresentar mensagens de erro em português que indiquem o campo e a correção necessária.
- **FR-023**: O sistema MUST preservar o conteúdo de um formulário em preenchimento quando o aplicativo for interrompido, encerrado pelo sistema ou tiver a tela rotacionada.
- **FR-024**: Ao digitar um nome de modelo já existente, o sistema MUST sugerir modelos e variantes semelhantes antes de criar um novo registro.
- **FR-025**: Quando o usuário tentar criar um modelo com mesmo nome e fabricante de um existente, o sistema MUST avisar e oferecer usar o existente ou criar mesmo assim.
- **FR-026**: O cadastro MUST funcionar integralmente sem conexão à internet.
- **FR-027**: Toda edição de preço pago, avaliação, condição, localização ou vínculo de variante MUST registrar data, valor anterior e valor novo em histórico consultável.
- **FR-028**: A exclusão de exemplar ou variante MUST exigir confirmação explícita que descreva o efeito e MUST oferecer arquivamento como alternativa.
- **FR-029**: O sistema MUST permitir identificar cadastros incompletos e listá-los para complementação posterior.
- **FR-030**: Quando informado, o código do fabricante MUST ser exibido no item e considerado na busca.
- **FR-031**: O sistema MUST permitir cadastrar o mesmo código do fabricante em itens diferentes apenas com aviso de possível duplicidade, sem bloqueio.
- **FR-032**: O cadastro MUST registrar a data de criação e a data da última alteração de cada entidade, visíveis ao usuário.

**Módulo 3 — Modelos, variantes e exemplares**

- **FR-033**: O sistema MUST tratar modelo, variante e exemplar como entidades distintas, com telas e contagens próprias.
- **FR-034**: Um exemplar MUST pertencer a exatamente uma variante; uma variante MUST pertencer a exatamente um modelo.
- **FR-035**: Uma variante MUST ser definida por atributos que a distinguem das demais do mesmo modelo, no mínimo: cor principal, tipo de rodas, tipo de base, decoração e edição/série.
- **FR-036**: O sistema MUST permitir cadastrar modelo sem variante e MUST criar a primeira variante no momento do primeiro exemplar.
- **FR-037**: Cada exemplar MUST ter condição, localização, histórico financeiro e fotos próprios, independentes de outros exemplares da mesma variante.
- **FR-038**: O sistema MUST atribuir a cada exemplar um identificador interno exibível, utilizável como etiqueta física.
- **FR-039**: O sistema MUST identificar duplicidade exclusivamente por variante, e MUST NOT classificar como duplicata duas variantes diferentes do mesmo modelo.
- **FR-040**: Ao cadastrar variante com todos os atributos idênticos a uma existente, o sistema MUST avisar e oferecer reutilizar a variante existente.
- **FR-041**: O sistema MUST permitir unir duas variantes cadastradas em duplicidade, movendo todos os exemplares e preservando históricos e fotos.
- **FR-042**: O sistema MUST permitir separar de uma variante os exemplares que pertencem a outra variante, criando o novo vínculo sem perder histórico.
- **FR-043**: O sistema MUST permitir renomear modelo, variante e exemplar sem quebrar vínculos, fotos, aquisições ou relatórios.
- **FR-044**: As contagens de exemplares, variantes e modelos MUST permanecer corretas após união, separação, arquivamento e restauração.
- **FR-045**: Exemplares vendidos ou trocados MUST sair do inventário ativo e MUST permanecer consultáveis no histórico.
- **FR-046**: O sistema MUST permitir registrar exemplar restaurado ou incompleto, com descrição do que falta ou foi alterado.

**Módulo 4 — Séries e coleções**

- **FR-047**: O sistema MUST permitir cadastrar fabricantes, com nome e país de origem opcional.
- **FR-048**: O sistema MUST permitir cadastrar séries vinculadas a um fabricante, incluindo ao menos Mainline, Basic Cars, Treasure Hunt, Super Treasure Hunt, Premium, Car Culture, Boulevard, Fast & Furious, Team Transport, RLC, Convention e edições especiais.
- **FR-049**: O sistema MUST permitir criar séries e coleções personalizadas, com nome definido pelo usuário.
- **FR-050**: O sistema MUST organizar coleções e subcoleções em hierarquia, sem presumir que todos os fabricantes usam a taxonomia da Mattel.
- **FR-051**: Cada miniatura MUST poder ser vinculada a uma série e a uma coleção, e MUST poder existir sem vínculo.
- **FR-052**: O sistema MUST exibir, por coleção, o percentual de conclusão calculado sobre os itens conhecidos e não marcados como não colecionáveis.
- **FR-053**: O sistema MUST listar os itens faltantes de cada coleção.
- **FR-054**: O sistema MUST distinguir, em cada item de coleção, "item conhecido", "item possuído" e "variante possuída".
- **FR-055**: O sistema MUST permitir marcar item como "não colecionável", retirando-o do cálculo de conclusão sem apagá-lo.
- **FR-056**: O sistema MUST permitir renomear, reorganizar e arquivar coleções sem perder vínculos de itens nem históricos.

**Módulo 5 — Lista de desejos**

- **FR-057**: O sistema MUST permitir registrar um desejo vinculado a um modelo existente, a uma variante existente ou a uma descrição livre ainda não catalogada.
- **FR-058**: O desejo MUST aceitar prioridade, com ao menos três níveis ordenáveis.
- **FR-059**: O desejo MUST aceitar valor máximo disposto a pagar, distinto do preço encontrado.
- **FR-060**: O desejo MUST aceitar preço encontrado, loja ou vendedor e data da observação.
- **FR-061**: O desejo MUST aceitar link de referência e MUST permitir abri-lo em navegador externo.
- **FR-062**: O desejo MUST aceitar observações livres.
- **FR-063**: O sistema MUST permitir converter um desejo em aquisição, criando o exemplar e registrando a aquisição, preservando data de criação do desejo, prioridade e valores originais no histórico.
- **FR-064**: Itens da lista de desejos MUST NOT ser contados como exemplares possuídos em nenhum indicador, relatório ou cálculo patrimonial.

**Módulo 6 — Duplicados e trocas**

- **FR-065**: O sistema MUST listar os exemplares duplicados, agrupados por variante, com a quantidade em cada grupo.
- **FR-066**: O sistema MUST permitir marcar cada exemplar duplicado como disponível para troca, para venda, para ambos ou indisponível.
- **FR-067**: O sistema MUST registrar, para exemplar disponível, condição física, valor pretendido e observações de negociação.
- **FR-068**: O sistema MUST permitir reservar exemplar para negociação, impedindo que ele seja comprometido em outra negociação ativa.
- **FR-069**: O sistema MUST registrar negociação com contraparte, canal, data de início, valores discutidos e resultado.
- **FR-070**: A conclusão de uma venda MUST registrar o valor recebido, a data e a contraparte no histórico financeiro do exemplar.
- **FR-071**: A conclusão de uma troca MUST registrar a saída do exemplar negociado e a entrada do exemplar recebido, quando houver.
- **FR-072**: Cancelar uma negociação MUST devolver o exemplar ao estado anterior, mantendo o registro do cancelamento.
- **FR-073**: O histórico de negociação MUST ser consultável e MUST NOT ser apagado pela conclusão ou pelo cancelamento.

---
**Módulo 7 — Controle financeiro**

- **FR-074**: A aquisição MUST registrar data, preço pago, frete, desconto, loja, vendedor e forma de aquisição.
- **FR-075**: O custo de aquisição MUST ser calculado como preço pago mais frete menos desconto, e MUST ser exibido separadamente das parcelas que o compõem.
- **FR-076**: Valores monetários MUST ser tratados com precisão de centavos, sem arredondamento acumulado, e MUST indicar a moeda utilizada (real brasileiro como padrão).
- **FR-077**: O sistema MUST calcular e exibir o investimento total da coleção.
- **FR-078**: O sistema MUST calcular e exibir gastos por período selecionável (mês, ano e intervalo livre).
- **FR-079**: O sistema MUST calcular e exibir gastos por série e por coleção.
- **FR-080**: O sistema MUST calcular e exibir gastos por fabricante.
- **FR-081**: O sistema MUST calcular e exibir custo médio por exemplar, considerando apenas exemplares com valor informado e indicando quantos foram excluídos do cálculo.
- **FR-082**: O sistema MUST permitir registrar avaliação de valor com valor, data e fonte (por exemplo: catálogo, loja, avaliação própria).
- **FR-083**: O sistema MUST permitir múltiplas avaliações por exemplar e MUST manter o histórico completo com data e fonte.
- **FR-084**: O valor estimado atual de um exemplar MUST ser o da avaliação mais recente.
- **FR-085**: O sistema MUST exibir a diferença entre custo de aquisição e avaliação atual, por exemplar.
- **FR-086**: Todo valor estimado MUST ser apresentado como estimativa do colecionador, com data e fonte, e MUST NOT ser descrito como preço oficial ou garantido de mercado.
- **FR-087**: Vendas e trocas concluídas MUST ser contabilizadas separadamente do investimento, sem alterar o custo histórico de aquisição.

**Módulo 8 — Localização física**

- **FR-088**: O sistema MUST permitir criar estruturas físicas hierárquicas com, no mínimo: ambiente, armário, estante, prateleira, caixa, gaveta, expositor e compartimento.
- **FR-089**: O sistema MUST permitir criar níveis e nomes personalizados de localização.
- **FR-090**: Cada exemplar MUST possuir zero ou uma localização atual.
- **FR-091**: A movimentação de um exemplar MUST registrar origem, destino e data, e MUST ser consultável como histórico.
- **FR-092**: Uma localização MUST poder conter qualquer quantidade de exemplares e MUST exibir sua contagem.
- **FR-093**: A exclusão de localização que contenha exemplares MUST exigir decisão explícita entre mover os exemplares e arquivar a localização; MUST NOT haver remoção em cascata.
- **FR-094**: O sistema MUST permitir consultar e filtrar exemplares por localização, em qualquer nível da hierarquia.
- **FR-095**: O sistema MUST permitir gerar inventário por localização, listando o conteúdo de uma caixa, estante ou ambiente.

**Módulo 9 — Fotografias e reconhecimento assistido**

- **FR-096**: O sistema MUST permitir capturar fotografia pela câmera do aparelho e escolher imagem da galeria do usuário.
- **FR-097**: O sistema MUST categorizar cada foto por ângulo ou como "detalhe", e MUST permitir mais de uma foto do mesmo ângulo.
- **FR-098**: O sistema MUST permitir definir uma foto como principal e alterar essa escolha depois.
- **FR-099**: Toda foto anexada MUST ser redimensionada e comprimida com limite de tamanho configurável e MUST ter metadados de localização removidos.
- **FR-100**: O sistema MUST avisar quando a foto anexada for semelhante a uma já existente no mesmo exemplar, sem bloquear.
- **FR-101**: A exclusão de foto MUST exigir confirmação e MUST NOT afetar o exemplar.
- **FR-102**: O reconhecimento por foto MUST apresentar sugestões de fabricante, modelo, série, ano e variantes, cada uma com nível de confiança.
- **FR-103**: O sistema MUST apresentar até cinco possíveis correspondências no catálogo local, ordenadas por confiança.
- **FR-104**: O sistema MUST permitir aplicar as sugestões campo a campo, com aceite ou rejeição individual, e MUST NOT gravar qualquer dado sem confirmação do usuário.
- **FR-105**: Quando o reconhecimento for inconclusivo, o sistema MUST declarar "não identificado" e MUST NOT preencher campos por inferência.
- **FR-106**: O reconhecimento MUST funcionar sem conexão quando o recurso estiver disponível no aparelho, e o envio de imagens a serviço externo MUST ser opcional, explícito e desativado por padrão.
- **FR-107**: O sistema MUST permitir ler texto da embalagem (OCR) e usá-lo como sugestão de nome, série, ano e código, sempre sujeito a confirmação.
- **FR-108**: Todas as funções não relacionadas a câmera MUST permanecer disponíveis quando a permissão de câmera for negada.

**Módulo 10 — Leitura de código de barras**

- **FR-109**: O sistema MUST permitir ler código de barras da embalagem usando a câmera.
- **FR-110**: Quando o código existir no catálogo local, o sistema MUST apresentar o item correspondente com seus dados e a origem da correspondência.
- **FR-111**: O sistema MUST permitir aproveitar os dados encontrados no cadastro somente após conferência e confirmação do usuário.
- **FR-112**: Quando o código não existir, o sistema MUST informar a ausência de correspondência e MUST permitir cadastro manual, guardando o código lido no item criado.
- **FR-113**: O sistema MUST NOT criar exemplar automaticamente a partir de um código, pois o mesmo código pode corresponder a vários carros de uma mesma caixa.

**Módulo 11 — Pesquisa e filtros**

- **FR-114**: O sistema MUST oferecer pesquisa por nome do modelo, nome do veículo real, código do fabricante, série e coleção.
- **FR-115**: A pesquisa MUST ser tolerante a diferenças de maiúsculas, acentos e ordem parcial de palavras.
- **FR-116**: O sistema MUST oferecer filtros combináveis por fabricante, modelo, série, coleção, ano, cor, escala, condição, variante, status de aquisição, localização e faixa de preço.
- **FR-117**: Filtros MUST combinar por interseção (E) entre categorias e MUST permitir múltipla escolha dentro de uma mesma categoria.
- **FR-118**: O sistema MUST permitir ordenação por nome, ano, data de aquisição e valor.
- **FR-119**: O sistema MUST exibir os filtros e a pesquisa ativos de forma visível e MUST permitir limpá-los em uma ação.
- **FR-120**: O sistema MUST exibir a quantidade de resultados para a combinação atual.
- **FR-121**: A pesquisa e os filtros MUST responder em menos de 300 milissegundos com 10.000 exemplares cadastrados (alinhado a R.7 da constituição).
- **FR-122**: Pesquisa, filtros e ordenação MUST funcionar integralmente sem conexão.

**Módulo 12 — Relatórios**

- **FR-123**: O sistema MUST gerar os relatórios: inventário completo, coleções, séries, modelos distintos, variantes, duplicados, lista de desejos, aquisições, investimento, avaliação patrimonial e localização física.
- **FR-124**: O sistema MUST exportar relatórios em CSV.
- **FR-125**: O sistema MUST exportar relatórios em planilha (formato Excel).
- **FR-126**: O sistema MUST gerar relatórios em PDF no próprio aparelho, sem servidor e sem conexão.
- **FR-127**: O PDF gerado MUST conter título, data e hora de geração, filtros aplicados, totais e numeração de páginas.
- **FR-128**: Relatórios MUST respeitar os filtros ativos quando o usuário optar por isso, e MUST indicar no arquivo quais filtros foram aplicados.
- **FR-129**: Relatórios MUST exibir "não informado" para dados ausentes, e MUST NOT exibir zero ou texto inventado em seu lugar.
- **FR-130**: O sistema MUST permitir compartilhar o arquivo gerado com outros aplicativos do aparelho.
- **FR-131**: O nome do arquivo gerado MUST conter o tipo de relatório e a data de geração.
- **FR-132**: O sistema MUST informar o progresso de geração de relatórios extensos e MUST permitir cancelar a operação sem corromper dados.
- **FR-133**: Os totais dos relatórios MUST coincidir com os indicadores do painel para o mesmo conjunto de dados e filtros.

---
**Transversal — Importação, exportação e backup**

- **FR-134**: O sistema MUST importar dados de arquivo CSV.
- **FR-135**: O sistema MUST importar dados de arquivo de planilha (formato Excel).
- **FR-136**: A importação MUST exibir pré-visualização com quantidade de linhas válidas, inválidas e possíveis duplicatas, antes de qualquer gravação.
- **FR-137**: A importação MUST produzir relatório de erros por linha, com número da linha, campo e motivo.
- **FR-138**: A importação MUST detectar possíveis duplicidades e oferecer, por item, pular, criar mesmo assim ou mesclar com o existente.
- **FR-139**: A importação MUST ser atômica por lote confirmado: ou todas as linhas válidas do lote entram, ou nenhuma entra, e o resultado parcial MUST ser informado explicitamente.
- **FR-140**: O sistema MUST gerar pacote de backup contendo dados e imagens.
- **FR-141**: O sistema MUST restaurar pacote de backup preservando relacionamentos entre fabricantes, modelos, variantes, exemplares, fotos, aquisições, avaliações e localizações.
- **FR-142**: O sistema MUST oferecer exportação integral dos dados em formato aberto, suficiente para reconstruir a coleção fora do aplicativo, incluindo metadados das imagens.
- **FR-143**: A restauração MUST verificar a integridade do pacote antes de importar e MUST informar de forma acionável quando o pacote estiver corrompido ou incompleto.
- **FR-144**: A restauração MUST NOT sobrescrever dados existentes sem confirmação explícita que descreva o efeito.
- **FR-145**: O sistema MUST lembrar o usuário de gerar backup quando o último backup for mais antigo que o intervalo escolhido por ele, sem bloquear o uso.

**Transversal — Operação offline e plataforma**

- **FR-146**: Todas as funções essenciais (cadastro, edição, consulta, filtros, fotos, financeiro, relatórios e backup) MUST funcionar sem conexão à internet.
- **FR-147**: Nenhuma operação essencial MUST ser bloqueada, adiada ou degradada pela ausência de rede.
- **FR-148**: Os dados locais do aparelho MUST ser a fonte de verdade da coleção.
- **FR-149**: Todo registro MUST receber identificador único gerado no aparelho, estável mesmo após exportação e reimportação.
- **FR-150**: Operações longas (importação, geração de backup, processamento de imagens) MUST continuar ou ser retomáveis quando o aplicativo for para segundo plano.
- **FR-151**: A interface MUST permanecer utilizável durante operações longas, com indicação de progresso.
- **FR-152**: O aplicativo MUST iniciar sem exigir criação de conta, login ou conexão.
- **FR-153**: O aplicativo MUST informar claramente quando o espaço de armazenamento do aparelho for insuficiente, sem gravar registro incompleto.
- **FR-154**: O aplicativo MUST informar a versão em uso e MUST preservar os dados ao ser atualizado.
- **FR-155**: O aplicativo MUST suportar as versões do Android declaradas oficialmente como suportadas, sem funcionalidade essencial exclusiva de uma única versão.

**Transversal — Segurança e privacidade**

- **FR-156**: O sistema MUST coletar apenas dados pessoais necessários ao uso (no máximo nome de exibição opcional e dados de negociação que o usuário inserir).
- **FR-157**: O sistema MUST permitir exportar e excluir todos os dados do usuário a pedido dele.
- **FR-158**: Nenhum dado da coleção MUST sair do aparelho sem ação explícita do usuário (exportar, compartilhar ou ativar recurso opcional).
- **FR-159**: Fotos e banco de dados MUST ficar no armazenamento privado do aplicativo, inacessíveis a outros aplicativos sem ação do usuário.
- **FR-160**: O aplicativo MUST NOT armazenar credenciais, chaves ou segredos embutidos.
- **FR-161**: O aplicativo MUST solicitar apenas as permissões necessárias e MUST solicitar cada uma no momento do uso correspondente.
- **FR-162**: O aplicativo MUST NOT enviar telemetria, analytics ou dados de uso por padrão; qualquer envio MUST ser opcional e explícito.
- **FR-163**: A exclusão de todos os dados MUST exigir dupla confirmação e MUST oferecer exportação prévia.

**Transversal — Idioma e acessibilidade**

- **FR-164**: Toda a interface e todos os relatórios MUST estar em português brasileiro, com termos de colecionador (exemplar, variante, série, lacrado, solto).
- **FR-165**: O aplicativo MUST ser navegável por leitor de tela, com rótulos descritivos em botões, campos e imagens.
- **FR-166**: A interface MUST respeitar o tamanho de fonte definido no sistema, até o limite máximo disponível, sem cortar conteúdo essencial.
- **FR-167**: A interface MUST manter contraste mínimo de 4.5:1 em textos e elementos essenciais.
- **FR-168**: Os alvos de toque MUST ter no mínimo 48 dp e MUST ser operáveis com uma mão na orientação retrato.
- **FR-169**: Toda ação destrutiva MUST exigir confirmação que explique consequência e alternativa.

**Transversal — Instalação e transparência**

- **FR-170**: O aplicativo MUST poder ser instalado em aparelho compatível por meio de arquivo de instalação assinado, sem exigir conta, loja de aplicativos ou conexão.
- **FR-171**: O aplicativo MUST exibir, em uma tela "Sobre", a versão instalada, a data de compilação e o identificador do pacote.
- **FR-172**: O aplicativo MUST disponibilizar, dentro do próprio aplicativo e sem conexão, uma política de privacidade legível que descreva quais dados ficam no aparelho e quais só saem por ação do usuário.

### Key Entities *(include if feature involves data)*

- **Fabricante**: quem produz a miniatura (Hot Wheels, Matchbox, Mini GT, Tarmac Works, outros). Tem nome, país e séries associadas.
- **Marca**: marca comercial exibida na miniatura, quando distinta do fabricante.
- **Veículo real**: veículo representado, com marca, modelo, ano e país de origem. Reutilizável entre miniaturas.
- **Modelo**: identidade do veículo em miniatura (ex.: "Nissan Skyline GT-R R34"), independente de cor, rodas ou edição. Agrupa variantes.
- **Variante**: versão específica do modelo definida por cor principal, cores secundárias, rodas, base, decoração, série/edição e ano de lançamento. É a unidade de comparação para duplicidade.
- **Exemplar**: unidade física possuída, com condição, identificador interno, localização atual, fotos, aquisição, avaliações e histórico próprios.
- **Série**: agrupamento editorial do fabricante (Mainline, Premium, Car Culture, RLC etc.), extensível e personalizável.
- **Coleção**: agrupamento curado pelo colecionador, com hierarquia e subcoleções, usado para calcular conclusão e faltantes. Pode reunir itens de fabricantes diferentes.
- **Categoria**: classificação auxiliar do item (tipo de carroceria, tema, categoria de série).
- **Fotografia**: imagem vinculada a um exemplar, com ângulo, ordem, indicação de principal e data de captura.
- **Aquisição**: evento de compra ou recebimento, com data, preço, frete, desconto, loja, vendedor e forma.
- **Avaliação**: estimativa de valor de um exemplar, com valor, data e fonte; histórico preservado.
- **Item de desejo**: miniatura ainda não possuída, com prioridade, valor máximo, preço encontrado, referência e status (desejado, adquirido, descartado).
- **Negociação**: processo de venda ou troca de um exemplar, com contraparte, canal, datas, valores e resultado; histórico permanente.
- **Localização**: nó de uma hierarquia física criada pelo usuário (ambiente, armário, estante, prateleira, caixa, gaveta, expositor).
- **Movimentação**: registro de transferência de um exemplar entre localizações, com origem, destino e data.
- **Reconhecimento**: resultado de sugestão por foto, OCR ou código de barras, com origem, confiança e status (pendente, aceito, rejeitado).
- **Registro de histórico**: trilha de alterações relevantes por entidade, com data, campo, valor anterior e valor novo.
- **Backup**: pacote exportável com dados e imagens, com versão de formato e verificação de integridade.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Registrar um novo exemplar de um modelo já catalogado leva menos de 20 segundos e não exige redigitar dados do modelo ou da variante.
- **SC-002**: Com 10.000 exemplares e 50.000 fotos, abrir listas leva menos de 1 segundo e pesquisar/filtrar responde em menos de 300 milissegundos em aparelho de referência de médio porte.
- **SC-003**: 100% das funções essenciais (cadastro, edição, consulta, filtros, fotos, financeiro, relatórios, backup) funcionam com o aparelho em modo avião.
- **SC-004**: Exportar e reimportar a coleção em outro aparelho restaura 100% dos exemplares, variantes, modelos, fotos vinculadas, aquisições, avaliações e localizações, com contagens idênticas.
- **SC-005**: Duas variantes diferentes do mesmo modelo nunca são contadas como duplicata, e dois exemplares da mesma variante sempre são, em 100% dos casos testados.
- **SC-006**: Importação de arquivo com 1.000 linhas apresenta pré-visualização completa e relatório de erros por linha antes de gravar qualquer dado.
- **SC-007**: Relatório em PDF é gerado no aparelho, sem conexão, em menos de 10 segundos para 1.000 exemplares, com totais iguais aos do painel.
- **SC-008**: O aplicativo nunca perde dados de formulário em 20 interrupções simuladas (rotação, segundo plano, encerramento pelo sistema).
- **SC-009**: Nenhum dado sai do aparelho sem ação explícita do usuário, verificado em 100% dos fluxos que envolvem rede.
- **SC-010**: Sugestões automáticas nunca gravam registros sem confirmação: 0 gravações automáticas em testes com reconhecimento inconclusivo.
- **SC-011**: 95% dos cadastros iniciados são concluídos na primeira tentativa, sem erro de preenchimento que obrigue reinício.
- **SC-012**: Os fluxos principais (cadastro, busca, adicionar exemplar, gerar relatório) são totalmente operáveis com leitor de tela e com fonte do sistema no tamanho máximo.
- **SC-013**: Restaurar um backup completo em aparelho novo leva menos de 5 minutos para uma coleção de 1.000 exemplares com 2.000 fotos.
- **SC-014**: O usuário consegue responder "quantos exemplares eu tenho", "quantos modelos distintos" e "quanto investi" em menos de 10 segundos após abrir o aplicativo.

## Assumptions

- O produto é um aplicativo Android de instalação local; não há servidor, conta de usuário nem sincronização entre aparelhos na versão 1.
- Um único colecionador por aparelho; dados de outras pessoas só aparecem se o usuário registrar negociações.
- A moeda padrão é o real brasileiro (BRL); valores históricos são preservados na moeda em que foram registrados.
- O catálogo de referência mundial de miniaturas (base compartilhada de modelos conhecidos) não faz parte da versão 1; a arquitetura apenas não deve impedir sua inclusão futura.
- O reconhecimento por imagem e o OCR são recursos assistivos opcionais; sem eles o cadastro manual continua completo.
- O código de barras pode não existir em todas as miniaturas e pode identificar uma caixa com várias unidades, não uma unidade individual.
- Valores estimados são avaliações do próprio colecionador; o sistema não afirma preço de mercado.
- O usuário tem aparelho Android com câmera e espaço de armazenamento suficiente para fotos da coleção.
- A distribuição da v1 é por instalação direta do arquivo assinado (sideload), sem conta em loja; o pacote de publicação e os requisitos de loja ficam preparados para uma publicação futura, fora do escopo desta versão.
- A stack nativa (Kotlin + Jetpack Compose, Room, ML Kit on-device) está registrada em Clarifications e na constituição v1.2.0; troca de stack exige emenda constitucional, não decisão de implementação.
- A identidade visual é inspirada no universo das miniaturas (vermelho, laranja, preto e branco), sem uso de logotipos ou ativos registrados de terceiros.
- A escala 1:64 é o padrão, mas o sistema aceita outras escalas sem alteração de estrutura.
- Textos de interface e relatórios são apenas em português brasileiro na versão 1.

## Out of Scope *(v1)*

- Sincronização entre aparelhos, conta em nuvem e compartilhamento público da coleção.
- Catálogo de referência mundial compartilhado com curadoria.
- Aplicativo para iOS, web ou desktop.
- Integração com marketplaces, cotações automáticas de preço ou leilões.
- Impressão de etiquetas por impressora dedicada (o identificador interno é exibível, não impresso pelo sistema).
- Multiusuário com permissões e auditoria por usuário.
- Tradução para outros idiomas e outras moedas além do real na interface.
- Publicação do aplicativo no Google Play nesta versão (o pacote de publicação e os requisitos de loja ficam preparados, sem envio).
