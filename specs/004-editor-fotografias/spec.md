# Feature Specification: Editor Integrado de Fotografias

**Feature Branch**: `004-editor-fotografias`

**Created**: 2026-11-23

**Status**: Draft — esclarecida em 2026-11-26 (CL-004-01): aparência escolhida e neutralidade da edição

**Input**: User description: "Preparar um editor de fotografias completo integrado ao aplicativo, permitindo capturar, editar, visualizar, organizar e salvar fotografias dos carrinhos sem sair do aplicativo."

## Clarifications

### Session 2026-11-26

- Q: A premissa "A interface segue o tema escuro" continua válida depois da escolha de aparência da Spec 006? → A: **A interface segue a aparência escolhida pelo usuário (Sistema, Claro ou Escuro); a área de edição fotográfica permanece neutra em todos os modos.** No modo Escuro, nada muda em relação ao comportamento atual. As sobreposições sobre a foto (ícones, rótulos, *scrims* e alças de recorte) MUST manter legibilidade independentemente do tema, e o tema MUST NOT tingir a imagem exibida ou salva.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fotografar, revisar e editar sem sair do aplicativo (Priority: P1)

O colecionador está cadastrando ou editando um exemplar, escolhe a categoria da fotografia (frente, traseira, lateral etc.), toca em "Câmera", tira a foto e, em vez de só "OK/Repetir", recebe uma revisão com as opções **Editar fotografia**, **Salvar sem editar**, **Tirar novamente** e **Cancelar**. Ao escolher editar, o editor abre na hora, ele enquadra o carrinho, corrige cor e luz, confirma e a fotografia editada fica associada à categoria escolhida.

**Why this priority**: é o fluxo que elimina a saída para outro aplicativo — o problema central relatado. Sem ele, nada mais da funcionalidade entrega valor.

**Independent Test**: cadastrar um exemplar, tirar uma foto e concluir a edição no mesmo fluxo; verificar que a imagem editada aparece na grade na categoria correta e que o exemplar continua íntegro.

**Acceptance Scenarios**:

1. **Given** um exemplar em edição com a categoria "Frente" selecionada, **When** o usuário toca em "Câmera" e conclui a captura, **Then** o aplicativo apresenta as quatro opções de revisão e não associa a foto antes de uma escolha.
2. **Given** a revisão de uma captura, **When** o usuário toca em "Salvar sem editar", **Then** a foto é associada à categoria selecionada com a imagem original, sem abrir o editor.
3. **Given** a revisão de uma captura, **When** o usuário toca em "Editar fotografia", **Then** o editor abre com a captura carregada e todas as ferramentas disponíveis.
4. **Given** o editor aberto, **When** o usuário ajusta o enquadramento e confirma, **Then** a versão editada é salva na categoria correta e a original permanece recuperável.
5. **Given** a revisão de uma captura, **When** o usuário toca em "Tirar novamente", **Then** a câmera reabre e a captura anterior é descartada sem criar registro.
6. **Given** a revisão de uma captura, **When** o usuário toca em "Cancelar", **Then** nenhum registro é criado e os arquivos temporários são removidos.

---

### User Story 2 - Editar ferramentas de enquadramento e orientação (Priority: P1)

No editor, o usuário corta a fotografia em proporção livre, quadrada, retrato (4:5), paisagem (16:9) ou original, reposiciona e amplia a área de corte, gira 90° para os dois lados, espelha horizontalmente e corrige a inclinação. O objetivo é remover o excesso ao redor do carrinho e deixá-lo reto.

**Why this priority**: enquadramento é a correção mais frequente em fotos de miniatura e a que mais depende de o usuário permanecer no aplicativo.

**Independent Test**: abrir o editor com uma foto conhecida, aplicar cada proporção, girar, espelhar e confirmar; verificar dimensões e orientação do arquivo resultante.

**Acceptance Scenarios**:

1. **Given** o editor aberto, **When** o usuário escolhe a proporção 1:1, **Then** a área de corte passa a ser quadrada sem deformar a imagem.
2. **Given** uma área de corte ativa, **When** o usuário arrasta um canto, **Then** a área de corte é redimensionada respeitando a proporção escolhida.
3. **Given** uma área de corte ativa, **When** o usuário arrasta o interior, **Then** a área se move sobre a imagem sem sair dos limites.
4. **Given** o editor, **When** o usuário gira 90° à direita duas vezes, **Then** a imagem aparece girada 180° e a confirmação salva essa orientação.
5. **Given** o editor, **When** o usuário espelha horizontalmente, **Then** o texto e os decalques aparecem invertidos na pré-visualização e na imagem salva.
6. **Given** o editor com ajustes aplicados, **When** o usuário restaura o original, **Then** todos os ajustes são descartados na sessão sem tocar no arquivo original.

---

### User Story 3 - Corrigir brilho, contraste, saturação e nitidez (Priority: P2)

O usuário ajusta brilho, contraste, saturação e nitidez por controles deslizantes, vê o efeito em tempo real, pode voltar cada controle ao valor original e comparar o antes e o depois antes de confirmar.

**Why this priority**: é o que transforma uma foto aceitável em uma foto que mostra rodas, decalques e inscrições com clareza — objetivo declarado do colecionador.

**Independent Test**: aplicar valores extremos e neutros em cada controle e verificar mudança visível na pré-visualização, retorno ao neutro com o botão de restaurar e persistência dos valores na imagem salva.

**Acceptance Scenarios**:

1. **Given** o editor aberto, **When** o usuário move o controle de brilho, **Then** a pré-visualização reflete a mudança sem travar a interface.
2. **Given** um controle alterado, **When** o usuário toca em restaurar naquele controle, **Then** apenas aquele ajuste volta ao neutro.
3. **Given** a ferramenta de comparação, **When** o usuário pressiona e segura, **Then** a imagem original é exibida enquanto o toque durar e a versão editada volta ao soltar.
4. **Given** nitidez aumentada, **When** o usuário confirma, **Then** os detalhes ficam mais definidos sem artefatos evidentes e a imagem não perde resolução útil.
5. **Given** o editor, **When** o usuário altera vários controles, **Then** desfazer e refazer percorrem as alterações na ordem inversa e na ordem original.

---

### User Story 4 - Editar e restaurar fotografias já cadastradas (Priority: P2)

Na tela do exemplar, o usuário abre uma fotografia existente no editor, ajusta e salva. A categoria, o vínculo com o exemplar e a indicação de foto principal são preservados. Depois, ele pode comparar com o original ou restaurar a fotografia original.

**Why this priority**: fecha o ciclo de vida da fotografia e evita a recompra de fotos já registradas.

**Independent Test**: abrir foto salva, editar, salvar, conferir que não surgiu um segundo registro nem trocou a categoria, e restaurar o original.

**Acceptance Scenarios**:

1. **Given** um exemplar com foto cadastrada, **When** o usuário escolhe editar essa foto, **Then** o editor abre com a imagem original (não com uma versão já reeditada).
2. **Given** a edição de uma foto cadastrada, **When** o usuário confirma, **Then** a contagem de fotografias do exemplar não muda e a categoria permanece a mesma.
3. **Given** uma foto editada, **When** o usuário restaura o original, **Then** a imagem exibida e a miniatura voltam a ser as originais e a versão editada é removida do aparelho.
4. **Given** uma foto principal editada, **When** a edição é salva, **Then** ela continua sendo a principal.
5. **Given** a confirmação de exclusão, **When** o usuário exclui a foto, **Then** o comportamento atual do aplicativo é mantido (registro e arquivos removidos).

---

### User Story 5 - Trazer imagem da galeria e editá-la na hora (Priority: P3)

O usuário escolhe uma imagem da galeria pela categoria selecionada e o editor abre automaticamente, sem exigir edição em outro aplicativo. A imagem selecionada na galeria não é alterada.

**Why this priority**: atende quem já tem fotos boas no aparelho e completa a paridade com o fluxo da câmera.

**Independent Test**: escolher uma imagem da galeria, editar, salvar e verificar que o arquivo original da galeria permanece intacto.

**Acceptance Scenarios**:

1. **Given** a categoria "Lateral direita" selecionada, **When** o usuário escolhe uma imagem da galeria, **Then** o editor abre com essa imagem.
2. **Given** uma imagem escolhida da galeria, **When** o usuário confirma sem editar, **Then** a imagem é salva como está na categoria correta.
3. **Given** permissão de câmera recusada, **When** o usuário usa a galeria, **Then** o fluxo continua funcionando e o aviso atual é exibido.

---

### Edge Cases

- Captura com arquivo ilegível ou vazio: mensagem compreensível em pt-BR e retorno à revisão, sem criar registro.
- Falta de espaço durante a gravação: nenhuma foto ou registro fica pela metade; o erro explica a causa.
- Foto muito grande (acima do limite de memória): processada com redução de amostragem, sem travar a interface.
- Rotação de tela ou chamada recebida com o editor aberto: ajustes da sessão são preservados.
- Tentativa de salvar duas vezes seguidas: segundo toque é ignorado enquanto o salvamento está em andamento.
- Descartar o editor com alterações não salvas: confirmação antes de perder os ajustes.
- Foto editada cujo arquivo original sumiu do aparelho: a foto atual é usada como base e o usuário é informado de que não há original recuperável.
- Backup antes e depois de editar: a restauração recupera original e versão editada com os mesmos vínculos.
- Exclusão do exemplar com fotos: regra atual permanece (exclusão bloqueada enquanto houver dependentes).

## Requirements *(mandatory)*

### Functional Requirements

**Captura e revisão**

- **FR-201**: Após qualquer captura, o sistema MUST apresentar a revisão com as opções Editar fotografia, Salvar sem editar, Tirar novamente e Cancelar, mantendo a categoria selecionada.
- **FR-202**: O sistema MUST permitir "Salvar sem editar" em um toque, sem abrir o editor.
- **FR-203**: O sistema MUST descartar a captura e seus arquivos temporários quando o usuário escolher Tirar novamente ou Cancelar.
- **FR-204**: O sistema MUST abrir a imagem escolhida da galeria no editor e MUST NOT alterar o arquivo original da galeria.

**Editor**

- **FR-205**: O editor MUST oferecer corte livre, 1:1, 4:5, 16:9 e proporção original, com reposição da área, ampliação e redução do enquadramento.
- **FR-206**: O editor MUST permitir giro de 90° à esquerda e à direita, espelhamento horizontal e correção manual de inclinação entre -15° e +15°.
- **FR-207**: O editor MUST oferecer controles deslizantes de brilho, contraste, saturação e nitidez, com restauração individual ao valor neutro.
- **FR-208**: O editor MUST mostrar a pré-visualização dos ajustes sem bloquear a interface, processando fora da thread principal.
- **FR-209**: O editor MUST oferecer zoom por pinça, movimentação da imagem ampliada e ajuste à tela.
- **FR-210**: O editor MUST oferecer comparação entre original e editada por pressionar e segurar, com identificação visual de qual versão está sendo exibida.
- **FR-211**: O editor MUST oferecer desfazer, refazer e restaurar todos os ajustes da sessão.
- **FR-212**: O editor MUST permitir escolher a qualidade de exportação (alta, equilibrada, reduzida), preservando detalhes de inscrições, rodas, decalques e cores.
- **FR-213**: O editor MUST avisar antes de descartar alterações não salvas.

**Preservação e integração**

- **FR-214**: A edição MUST ser não destrutiva: o arquivo original MUST permanecer recuperável enquanto a fotografia existir.
- **FR-215**: O sistema MUST garantir no máximo uma versão derivada ativa por fotografia, removendo a derivada anterior ao salvar uma nova edição e nunca recomprimindo sobre o original.
- **FR-216**: O sistema MUST permitir restaurar o original, apagando a versão derivada e voltando a exibir a original.
- **FR-217**: A versão editada MUST ser a exibida na grade, na miniatura e na visualização do exemplar, mantendo o original recuperável.
- **FR-218**: A edição MUST preservar categoria, vínculo com o exemplar e indicação de foto principal, sem criar registros duplicados.
- **FR-219**: O sistema MUST permitir abrir o editor a partir de uma fotografia já cadastrada.
- **FR-220**: O sistema MUST registrar data da última edição e os parâmetros aplicados, quando armazenados, na trilha auditável.
- **FR-221**: O sistema MUST manter as categorias de fotografia existentes e MUST NOT limitar o editor a um subconjunto delas.

**Robustez**

- **FR-222**: O sistema MUST tratar imagem inválida, imagem muito grande, falta de espaço, falha de gravação e permissão recusada com mensagem em pt-BR e sem corromper dados.
- **FR-223**: O sistema MUST impedir cliques repetidos durante o salvamento e indicar progresso.
- **FR-224**: O sistema MUST preservar o estado do editor em rotação de tela e recriação do processo.
- **FR-225**: O backup MUST incluir fotografias originais e editadas, e a restauração MUST preservar seus vínculos.
- **FR-226**: O sistema MUST remover arquivos temporários de edição cancelada ou com falha.

### Key Entities *(include if feature involves data)*

- **Fotografia**: imagem associada a um exemplar, com categoria, caminho do arquivo exibido, caminho opcional do original preservado, checksum, dimensões, tamanho, ordem, indicação de principal, data de captura, data da última edição e parâmetros de edição.
- **Edição de fotografia**: conjunto de parâmetros não destrutivos — enquadramento (retângulo normalizado), proporção, giro, espelhamento, inclinação, brilho, contraste, saturação, nitidez e qualidade de exportação.
- **Exemplar**: unidade física dona das fotografias; a edição nunca troca o vínculo.
- **Categoria de fotografia**: ângulo/categoria existente (frente, traseira, lateral esquerda, lateral direita, superior, base, embalagem, detalhe).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-201**: Um colecionador conclui captura, edição e salvamento de uma foto sem sair do aplicativo em 100% dos fluxos de câmera e galeria.
- **SC-202**: A revisão pós-captura aparece em menos de 1 segundo após a captura em um aparelho de referência de 2020 ou mais novo.
- **SC-203**: A pré-visualização de um ajuste de cor aparece em menos de 300 ms após o toque, sem congelar a interface.
- **SC-204**: Salvar uma edição leva menos de 3 segundos para uma foto de 1600 px no aparelho de referência.
- **SC-205**: Nenhum cenário de teste perde a fotografia original, mesmo após cinco edições sucessivas.
- **SC-206**: A edição não altera a contagem de fotografias do exemplar, a categoria nem a foto principal em 100% dos casos.
- **SC-207**: A restauração de um backup criado após edições recupera original e editada em 100% dos casos testados.
- **SC-208**: As ferramentas de corte, giro, espelho, cor e nitidez produzem mudança verificável no arquivo salvo em 100% dos testes automatizados.

## Assumptions

- A captura continua usando a câmera do sistema, aberta em contexto (R.9), e o processamento/edição ocorre integralmente no aplicativo; a escolha evita nova dependência de câmera e preserva o comportamento atual em aparelhos sem câmera.
- Os limites atuais de armazenamento de imagem (aresta máxima de 1600 px e ~1,5 MB por arquivo) continuam válidos; a edição trabalha dentro deles.
- O editor opera sobre fotos guardadas no armazenamento privado do aplicativo; a imagem escolhida na galeria é copiada pelo fluxo atual, que já remove EXIF sensível.
- Não há migração destrutiva: os campos novos da fotografia são opcionais e as fotos existentes permanecem válidas sem original separado até a primeira edição.
- O backup em ZIP atual já copia a pasta de imagens; apenas a coleta de caminhos precisa considerar o original.
- A interface segue a aparência escolhida pelo usuário (Spec 006, FR-401–FR-409) e os componentes Material 3 já usados; no modo Escuro o comportamento atual é preservado. A área de edição permanece neutra e a imagem não é tingida pelo tema em nenhum modo (Spec 006, FR-419/FR-420). A documentação técnica da funcionalidade é entregue em `docs/`.
