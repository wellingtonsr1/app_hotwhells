# Feature Specification: Preferência de aparência (tema claro, escuro e sistema)

**Feature Branch**: `006-preferencia-aparencia`

**Created**: 2026-11-25

**Status**: Draft — aguardando revisão e aprovação

**Input**: User description: "Implementar a escolha de aparência pelo usuário no Diecast Collector, com as opções Sistema, Claro e Escuro, persistente e aplicada imediatamente, corrigindo a divergência entre o KDoc de `Theme.kt` e o comportamento real; preservar a identidade visual e a arquitetura (Specs 001, 003, 004 e 005)."

**Constituição aplicável**: `.specify/memory/constitution.md` v1.3.0 — Art. I (integridade dos dados), II (modularidade e ponto único de verdade), IV.1/IV.6/IV.7 (idioma, acessibilidade, preservação de entrada), V.1/V.2/V.4 (testes e verificação), VII.9–VII.11 (consistência visual, acessibilidade, apresentação sem mudança de negócio), R.4/R.5 (decisão tecnológica e inventário de dependências), R.13 (tamanho e startup), R.14/R.16 (stack nativa).

> **Âmbito desta especificação**: aparência (tema). Nenhum requisito altera o modelo de domínio, o esquema Room, a persistência da coleção, o funcionamento offline, o `applicationId` ou o nome comercial. A preferência é um estado de apresentação do aplicativo, não um dado da coleção.

## Contexto

O aplicativo já tem duas paletas Material 3 completas em `core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/Theme.kt` (`LightColors` a partir de `Chalk`/`PureWhite`/`InkBlack` e `DarkColors` a partir de `InkBlack`/`Graphite`/`Chalk`) e escolhe entre elas por `isSystemInDarkTheme()`. Também já existe um tema mínimo de janela em `app/src/main/res/values/themes.xml`, com `windowBackground` fixo em `#121212`.

O KDoc de `DiecastTheme` afirma que "Segue a preferência do sistema por padrão e permite escolha explícita nas configurações (tema claro/escuro/sistema)". A segunda metade **não existe**: não há opção em Ajustes, não há preferência persistida e `MainActivity` chama `DiecastTheme { … }` sem qualquer parâmetro. A documentação descreve um comportamento que o usuário não consegue obter.

Levantamento do código atual (verificado em 2026-11-25):

| Ponto | Situação real |
|---|---|
| Seleção de paleta | `DiecastTheme(darkTheme: Boolean = isSystemInDarkTheme())` em `core/ui/.../theme/Theme.kt` |
| Instalação | `MainActivity.onCreate` → `setContent { DiecastTheme { ProvideWindowSizeClass { AppNavHost() } } }` |
| Preferência de aparência | **não existe** (nenhuma chave, nenhum enum, nenhuma tela) |
| Persistência de preferências | `BackupReminderStore` usa `SharedPreferences` (`backup_reminder`), fora do banco |
| DataStore Preferences | dependência declarada em `gradle/libs.versions.toml` e presente em `app` e `core:data`, **sem nenhum uso em código**; `docs/dependencias.md` a descreve como "lembrete de backup", finalidade que o código não cumpre |
| Tema de janela | `themes.xml` com `window_background` escuro fixo e sem `values-night` |
| Barras do sistema | `enableEdgeToEdge()` sem estilo explícito: ícones seguem o modo escuro **do sistema**, não a preferência |
| Identidade declarada em specs anteriores | Spec 003 FR-013 e `contracts/collection-card.md` ("tema escuro, fundo preto, cartões cinza escuro"); Spec 004, premissa "a interface segue o tema escuro" |
| Contraste já medido | `app/src/androidTest/.../ContrastAssert.kt` mede apenas as **cores declaradas** do tema (claro e escuro), não o que as telas renderizam |

Consequência: um usuário com o Android em modo claro recebe hoje a paleta clara, mas as premissas das Specs 003 e 004 pressupõem escuro; e um usuário que queira o app escuro com o sistema claro não tem como obtê-lo. O objetivo desta feature é fechar exatamente essa lacuna, sem criar um segundo mecanismo de tema.

## Objetivos e escopo

**Objetivos**

1. Oferecer as três opções de aparência — **Sistema**, **Claro** e **Escuro** — em Ajustes, com seleção única e visível.
2. Persistir a escolha localmente, sobrevivendo a fechamento, reinício do app, reinício do aparelho e atualização que preserve os dados.
3. Aplicar a escolha imediatamente, sem reiniciar o app e sem recarregar dados.
4. Ligar a preferência ao mecanismo de tema existente, preservando as duas paletas atuais e a identidade visual.
5. Garantir que a aparência escolhida valha para **todas** as telas e superfícies que usam o tema global — navegação, cartões, formulários, diálogos, folhas, menus, visualizadores de foto, editor e barras do sistema.
6. Corrigir a divergência entre documentação e comportamento, incluindo o KDoc de `Theme.kt`, o inventário de dependências e os manuais.

**Dentro do escopo**

- Opção de aparência em Ajustes, com as três alternativas.
- Preferência local, leitura assíncrona e escrita atômica.
- Resolução de tema em um único ponto, reaproveitando `LightColors`/`DarkColors`.
- Ajuste de superfícies especiais (fundo de janela, barras do sistema, sobreposições sobre fotos) para não dependerem do modo do sistema.
- Testes de persistência, de estado e de aplicação do tema, incluindo elementos efetivamente renderizados.

**Fora de escopo**

- Paleta dinâmica (Material You), temas personalizados, escolha de cor de destaque.
- Tema por tela, por coleção ou por usuário; multiusuário e sincronização.
- Mudança de tipografia, formas, ícones ou hierarquia visual.
- Novas rotas, novas telas ou reorganização de Ajustes além da seção de aparência.
- Correção do comportamento do lembrete de backup (o uso de `SharedPreferences` ali permanece).
- Alteração das Specs 001, 003, 004 e 005 nesta fase de especificação: as recomendações estão registradas sem edição; o esclarecimento da 003/004 foi **aprovado e aplicado em 2026-11-26** (CL-003-01, CL-003-02, CL-004-01 — ver `clarifications-g2.md`).

## Clarifications

### Session 2026-11-25

- Q: Onde a preferência deve ser persistida, considerando que o app já usa `SharedPreferences` para o lembrete de backup e tem DataStore declarado mas sem uso? → A: **DataStore Preferences**, na camada de aplicação (`app`), por ser assíncrono, atômico e já declarado em `gradle/libs.versions.toml` (nenhuma dependência nova). `SharedPreferences` continua apenas no lembrete de backup; não haverá um segundo mecanismo para a mesma finalidade. O inventário `docs/dependencias.md` será corrigido para refletir a finalidade real.
- Q: A escolha deve abrir uma tela nova ou ficar dentro de Ajustes? → A: **dentro de Ajustes**, em uma seção **Aparência** com as três opções em linha (uma seleção única, com marca visível). Nenhuma rota nova, nenhum destino novo na navegação (Spec 005, FR-303/FR-331).
- Q: Como conciliar a escolha do usuário com a "identidade escura" exigida pela Spec 003 FR-013 e pela premissa da Spec 004? → A: a identidade é o **conjunto de tokens, tipografia, formas e hierarquia**, não a paleta escura como obrigação permanente. Em **Escuro** (e em Sistema com o Android escuro), a identidade escura de 003/004 é preservada exatamente como hoje; em **Claro**, aplicam-se os tokens claros já existentes, da mesma família cromática (FlameRed, HotOrange, InkBlack, Chalk), sem cor nova. O esclarecimento foi aprovado e aplicado em 2026-11-26 à Spec 003, ao contrato do cartão e à Spec 004 (ver [`clarifications-g2.md`](./clarifications-g2.md) e *Impacto nas especificações anteriores*).

**Consequências registradas**: uma única fonte de verdade para a aparência (enum + resolvedor no tema + store de preferência); nenhuma paleta duplicada; nenhum `AppCompatDelegate`/`UiModeManager` introduzido; as preferências de aparência não fazem parte do pacote de backup da coleção.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Escolher a aparência do aplicativo em Ajustes (Priority: P1)

O colecionador prefere ler o acervo no claro durante o dia e no escuro à noite. Em **Ajustes**, ele encontra a seção **Aparência**, vê qual opção está ativa e toca em **Claro** ou **Escuro**. A tela inteira muda imediatamente, sem fechar o app e sem perder o que estava fazendo; pode voltar para **Sistema** a qualquer momento e o app passa a acompanhar o Android.

**Why this priority**: é o valor central da feature e a correção da divergência entre o KDoc e o comportamento real. Sem isso, as demais garantias (persistência, consistência) não têm sobre o que incidir.

**Independent Test**: abrir Ajustes em uma instalação limpa, confirmar **Sistema** selecionado, tocar em **Claro** e depois em **Escuro**, verificando que a superfície inteira do app muda a cada toque e que a marca de seleção acompanha.

**Acceptance Scenarios**:

1. **Given** uma instalação nova, **When** o usuário abre Ajustes, **Then** a seção **Aparência** mostra as três opções e **Sistema** está marcada.
2. **Given** a seção Aparência aberta, **When** o usuário toca em **Claro**, **Then** todas as superfícies visíveis passam à paleta clara imediatamente, a opção fica marcada e nenhum dado é recarregado.
3. **Given** a seção Aparência aberta com **Claro** ativo, **When** o usuário toca em **Escuro**, **Then** a paleta escura é aplicada imediatamente e a marca de seleção passa para **Escuro**.
4. **Given** uma aparência forçada ativa, **When** o usuário toca em **Sistema**, **Then** o app passa a refletir o modo do Android naquele instante, sem reiniciar.
5. **Given** um formulário com texto digitado e a lista rolada, **When** o usuário troca a aparência, **Then** o texto digitado, a rolagem, os filtros e a aba atual permanecem.

---

### User Story 2 - A escolha sobrevive ao tempo e ao aparelho (Priority: P1)

O usuário escolhe **Escuro**, fecha o app, reinicia o aparelho e abre o Diecast Collector de novo: continua escuro. Atualiza o app por cima da instalação existente: a escolha continua. Se nunca escolheu nada, o app segue o Android.

**Why this priority**: uma preferência que não persiste é pior do que não existir — ela promete e não cumpre. Persistência e padrão correto são pré-requisito de confiança.

**Independent Test**: escolher **Escuro**, encerrar o processo do app (`adb shell am force-stop`), reabrir e verificar a paleta escura; repetir com **Claro**; reinstalar por cima (`adb install -r`) e verificar que a escolha permanece.

**Acceptance Scenarios**:

1. **Given** a aparência **Escuro** escolhida, **When** o processo do app é encerrado e reaberto, **Then** o app abre em escuro desde a primeira tela composta.
2. **Given** a aparência **Claro** escolhida, **When** o aparelho é reiniciado e o app reaberto, **Then** o app abre em claro.
3. **Given** a aparência escolhida e o app atualizado por cima (`install -r`) preservando os dados, **When** o app abre, **Then** a aparência escolhida continua ativa.
4. **Given** uma preferência ausente, ilegível ou com valor desconhecido (versão futura, arquivo corrompido), **When** o app abre, **Then** usa **Sistema**, sem erro visível, sem travar e sem apagar a coleção.
5. **Given** um backup da coleção restaurado, **When** o app reabre, **Then** a aparência não mudou por causa da restauração.

---

### User Story 3 - Aparência consistente e legível em todas as telas (Priority: P2)

Com qualquer uma das três opções, o usuário percorre catálogo, coleções, busca, ficha do exemplar, fotos e editor e encontra a mesma linguagem visual: superfícies coerentes, textos legíveis, botões e ícones visíveis sobre as fotografias, barras do sistema com ícones contrastantes. Nada fica "meio claro, meio escuro", nada desaparece e nenhuma foto é tingida pelo tema.

**Why this priority**: a feature só está pronta quando a aparência vale de ponta a ponta. Uma tela que ignora a preferência, ou texto ilegível sobre foto, desfaz a confiança construída nas histórias anteriores.

**Independent Test**: percorrer as 37 linhas do inventário de telas/diálogos da Spec 005 nos modos Claro e Escuro, verificando paleta coerente, texto sobre foto legível e barras do sistema com ícones visíveis.

**Acceptance Scenarios**:

1. **Given** o app em **Claro**, **When** o usuário percorre as cinco abas, uma tela de detalhe, um formulário, um diálogo e uma folha, **Then** todas usam a mesma paleta clara, sem superfície da paleta oposta.
2. **Given** o app em **Escuro**, **When** o usuário abre a ficha de um exemplar com fotos, **Then** superfícies, textos e destaques correspondem à identidade escura atual, sem alteração de layout ou de hierarquia.
3. **Given** uma foto clara exibida com controles sobrepostos, **When** o app está em **Claro** ou em **Escuro**, **Then** ícones e rótulos sobre a foto continuam legíveis, com contraste medido no que é renderizado.
4. **Given** o editor de fotografias, **When** a aparência é trocada, **Then** a área de edição continua neutra (sem tingir a imagem) e as ferramentas mantêm contraste.
5. **Given** o app em **Claro** com o Android em escuro (ou o inverso), **When** o usuário observa as barras de status e navegação, **Then** os ícones do sistema contrastam com o conteúdo do app.
6. **Given** fonte do sistema em 200% e janela compacta, **When** a seção Aparência é aberta, **Then** os três rótulos ficam inteiros e tocáveis, com alvo ≥ 48 dp.

---

### Edge Cases

- **Android muda de tema com o app aberto em Sistema**: o app acompanha sem reiniciar e sem perder estado.
- **Android muda de tema com o app em Claro/Escuro forçado**: nada muda; a escolha do usuário prevalece.
- **Primeiro quadro após processo encerrado**: a preferência é aplicada sem bloquear a interface e sem exibir a paleta oposta além de um quadro (≤ 16,7 ms), conforme NFR-403.
- **Preferência gravada por versão futura** (valor desconhecido): cai em **Sistema**, preserva o arquivo e não apaga nada.
- **Falha de escrita da preferência** (disco cheio, I/O): o app continua utilizável, informa falha de forma discreta e mantém a aparência anterior.
- **Troca de aparência durante uma captura/edição de foto**: nenhum arquivo, rascunho ou parâmetro de edição é perdido.
- **Troca de aparência com diálogo aberto**: o diálogo acompanha a paleta e permanece funcional.
- **Duas janelas (spec 005, freeform/split)**: cada janela usa a mesma preferência; a troca se reflete nas duas.
- **Tema de janela na abertura**: em Sistema, o fundo da janela corresponde ao modo do Android; em modo forçado, qualquer diferença de primeiro quadro é limitada e documentada.
- **Restauração de backup da coleção**: não altera a aparência.
- **Acessibilidade**: TalkBack anuncia o grupo como seleção única e o estado de cada opção; a marca de seleção não depende só de cor.

## Requirements *(mandatory)*

> Numeração a partir de FR-401 para não colidir com as especificações anteriores (001–005).

### Opções de aparência

- **FR-401**: O aplicativo MUST oferecer exatamente três opções de aparência — **Sistema**, **Claro** e **Escuro** —, mutuamente exclusivas, com rótulos em português brasileiro.
- **FR-402**: As opções MUST ficar em uma seção **Aparência** dentro de Ajustes, sem criar rota, destino de navegação ou tela nova; a seção MUST ser alcançável pela aba Ajustes existente. Em janela **compacta** (< 600 dp) a seção ocupa a largura disponível; em janela **média** (600–839 dp) e **expandida** (≥ 840 dp) MUST permanecer na coluna de leitura de até **720 dp**, centralizada, sem esticar as três opções pela largura restante (Spec 005, FR-311; VII.2).
- **FR-403**: A opção MUST ser apresentada com marca visível de seleção que não dependa apenas de cor, e a seleção MUST refletir em todos os momentos o valor efetivamente aplicado (o requisito de semântica correspondente está em FR-424).
- **FR-404**: Quando o usuário nunca tiver escolhido, ou quando o valor armazenado estiver ausente, ilegível ou desconhecido, o aplicativo MUST usar **Sistema**.

### Aplicação e integração com o tema

- **FR-405**: A resolução da aparência MUST ocorrer em um único ponto do mecanismo de tema existente (`DiecastTheme`), reaproveitando as paletas `LightColors` e `DarkColors`; MUST NOT haver segunda tabela de cores, segunda função de tema ou paleta duplicada.
- **FR-406**: A troca de aparência MUST ser aplicada imediatamente a todas as superfícies já compostas, sem exigir encerramento, reinício do app ou recriação manual pelo usuário (o limite quantitativo único está em NFR-401).
- **FR-407**: No modo **Sistema**, o aplicativo MUST acompanhar alterações de tema do Android ocorridas com o app em execução, sem reiniciar e sem perder estado.
- **FR-408**: A aparência escolhida MUST valer para todas as telas e superfícies que usam o tema global: navegação (barra/trilho), painel, catálogo, listas, formulários, ficha, busca, financeiro, desejos, coleções, localizações, duplicados, importação, relatórios, backup, ajustes, diálogos, folhas, menus, cartões, estados vazios, mensagens e *snackbars*.
- **FR-409**: As barras de status e navegação do sistema MUST exibir ícones com contraste adequado ao conteúdo do app em todas as três opções, inclusive quando a aparência forçada divergir do tema do Android.
- **FR-410**: O fundo da janela exibido antes do primeiro quadro MUST corresponder à aparência em uso no modo **Sistema**; em modo forçado, a diferença eventual MUST durar no máximo **um quadro** (≤ 16,7 ms a 60 Hz), comprovada por captura de quadro, e registrada como limitação medida (NFR-403).
- **FR-411**: A aparência MUST NOT alterar layout, navegação, rotas, textos, regras de validação, fluxo de tela ou qualquer regra de negócio (VII.11).

### Persistência

- **FR-412**: A escolha MUST ser persistida localmente de forma atômica e sobreviver a: encerramento do app, reinício do app, reinício do dispositivo e atualização que preserve os dados (FR-004 do pedido).
- **FR-413**: A leitura e a escrita da preferência MUST ser assíncronas e MUST NOT bloquear a thread principal; a interface MUST permanecer interativa durante a leitura inicial.
- **FR-414**: Falha de leitura ou escrita da preferência MUST NOT impedir o uso do app nem apagar dados; o comportamento de fallback é **Sistema** (leitura) e manutenção do valor anterior com aviso **não bloqueante em pt-BR** (escrita), sem diálogo modal e sem perder o estado da tela.
- **FR-415**: A preferência de aparência MUST ficar fora do pacote de backup da coleção e MUST NOT ser alterada pela restauração de um backup (mesma regra já aplicada ao lembrete de backup).
- **FR-416**: A preferência MUST NOT exigir migração de esquema Room nem alteração de tabela, entidade ou repositório de domínio.

### Identidade visual

- **FR-417**: No modo **Escuro** (e em **Sistema** com o Android escuro), a identidade escura das Specs 003 e 004 MUST ser preservada: fundo `InkBlack`, superfícies `Graphite`/`SmokyGrey`, textos `Chalk`, destaques `FlameRedDark`/`HotOrangeLight`.
- **FR-418**: No modo **Claro**, a aparência MUST usar os tokens claros existentes (`Chalk`, `PureWhite`, `InkBlack`, `MutedGrey`, `FlameRed`, `HotOrange`), com a mesma tipografia, formas, espaçamentos e hierarquia do modo escuro; MUST NOT introduzir cor fora da paleta existente.
- **FR-419**: Fotografias e miniaturas MUST NOT ser tingidas, filtradas ou recoloridas pela aparência; molduras, marcadores de ausência de foto e fundos de imagem MUST usar tokens do tema com contraste visível nos dois modos. O não tingimento MUST ser verificado comparando a mesma imagem **renderizada** em Claro e em Escuro, não apenas pela leitura do código.
- **FR-420**: Sobreposições sobre fotografias (ícones, rótulos, *scrims* e controles do editor) MUST manter legibilidade independentemente do tema, e a área de edição fotográfica MUST permanecer neutra.
- **FR-421**: A troca de aparência MUST NOT alterar a identidade do produto: nome **Diecast Collector**, ícone, `applicationId`, tipografia base e iconografia permanecem como estão.

### Acessibilidade

- **FR-422**: Textos e componentes MUST atender contraste ≥ 4,5:1 para texto normal e ≥ 3:1 para texto grande e elementos de interface, **medidos sobre o que é efetivamente renderizado**, nos dois modos — não apenas sobre as cores declaradas no tema.
- **FR-423**: Textos sobre fotografias, imagens de fundo e sobreposições MUST atender ao mesmo critério de contraste, considerando duas imagens de teste determinísticas — uma totalmente branca e uma totalmente preta (pior caso de fundo).
- **FR-424**: O grupo de aparência MUST ser anunciado por leitor de tela como seleção única, com nome do grupo, rótulo de cada opção e estado selecionado/não selecionado.
- **FR-425**: Cada opção MUST ter alvo de toque ≥ 48 × 48 dp e MUST permanecer inteira e alcançável com fonte do sistema em 200% e em janela compacta (Spec 005, FR-324/FR-327).
- **FR-426**: A troca de aparência MUST preservar o conteúdo em preenchimento, a rolagem, os filtros e a seleção atuais (IV.7 e Spec 001, FR-023).

### Preservação e escopo

- **FR-427**: A implementação MUST NOT alterar domínio, esquema Room, repositórios, casos de uso, offline, `applicationId`, `app_name` ou rotas existentes.
- **FR-428**: A implementação MUST NOT introduzir dependência nova sem ADR e registro de finalidade, versão, licença, custo e substituição (R.4, R.5); o store de preferência MUST usar a dependência de DataStore já declarada.
- **FR-429**: A API pública de `DiecastTheme` MUST permanecer compatível com os usos existentes (previews e testes) por meio de valor padrão, sem obrigar alteração em chamadas que não lidam com aparência.
- **FR-430**: Nenhuma funcionalidade existente MUST ser removida, escondida, desabilitada ou ter comportamento alterado por causa da aparência.

### Requisitos não funcionais

- **NFR-401**: A mudança de aparência MUST ser percebida como imediata: o novo tema MUST estar aplicado em menos de **100 ms** após o toque, sem recriação de atividade exigida do usuário.
- **NFR-402**: A leitura inicial da preferência MUST NOT adicionar mais de **50 ms** à abertura fria medida na Spec 005 (baseline de **993 ms** de mediana) e MUST NOT bloquear a thread principal.
- **NFR-403**: Em modo **Sistema**, nenhum quadro composto do app MUST exibir a paleta oposta, nem na abertura nem quando o Android troca de tema; em modo forçado, o descompasso de primeiro quadro MUST ser de no máximo **um quadro** (≤ 16,7 ms a 60 Hz), medido por captura de quadro, ou registrado como limitação com o valor medido.
- **NFR-404**: O tamanho do APK MUST NOT crescer mais de **100 KB** com a feature (DataStore já está no classpath; medir antes/depois) e MUST NOT haver regressão mensurável de startup (R.13).
- **NFR-405**: Escritas concorrentes sobre a preferência MUST resultar em um valor íntegro (uma das escolhas), nunca em estado parcial ou ilegível.
- **NFR-406**: A arquitetura MUST ser mantida: `core:ui` continua sem depender de `core:data`/`app`; a preferência é lida na camada de aplicação e o tema recebe apenas o modo resolvido.
- **NFR-407**: A suíte existente MUST continuar verde; nenhum teste MUST ser removido, ignorado ou afrouxado (V.4).
- **NFR-408**: A acessibilidade existente MUST ser preservada: TalkBack, fonte ampliada, ordem de foco, D-pad e alvos mínimos, nos dois modos.

### Key Entities

Esta especificação **não envolve entidade de domínio persistida**. As entidades são de apresentação e de preferência local:

- **Modo de aparência**: valor único entre `Sistema`, `Claro`, `Escuro`; determina a paleta resolvida.
- **Modo resolvido**: resultado booleano "escuro?" aplicado ao tema; derivado do modo de aparência e, em `Sistema`, do tema do Android.
- **Preferência de aparência**: registro local isolado, com chave versionada e valor textual; fora do banco da coleção e fora do pacote de backup.
- **Seção Aparência**: superfície de Ajustes com as três opções e a marca de seleção.

## Regras de negócio

- **BR-401**: Existe um único valor de aparência por instalação; não há valor por tela, por coleção ou por janela.
- **BR-402**: Valor ausente, ilegível ou desconhecido equivale a **Sistema**; nunca a "claro" nem a "escuro" por conveniência.
- **BR-403**: Em **Sistema**, a fonte de verdade é o tema do Android no momento da composição; em **Claro**/**Escuro**, a escolha do usuário prevalece sobre o Android.
- **BR-404**: A aparência não é dado da coleção: não entra no pacote de backup, não é afetada por restauração e não aparece em relatórios/exportações.
- **BR-405**: A escolha é preservada em atualizações do app que preservem dados; desinstalar remove a preferência junto com os demais dados do app.
- **BR-406**: Nenhuma tela pode sobrescrever a aparência global; exceções locais (fundo neutro do editor, *scrim* sobre foto) são de legibilidade, não de tema.

## Arquitetura proposta *(resumo; detalhamento em `plan.md`)*

1. **Uma única resolução de tema**: `ThemeMode` (enum de apresentação) em `core:ui`, e `DiecastTheme(mode: ThemeMode = ThemeMode.SYSTEM)` resolvendo `escuro?` a partir de `isSystemInDarkTheme()` apenas no modo Sistema. Nenhuma paleta nova.
2. **Preferência na camada de aplicação**: store `@Singleton` em `app` sobre DataStore Preferences (dependência já declarada), expondo `StateFlow<ThemeMode>` e uma escrita suspensa; DataStore é lido em `Dispatchers.IO`.
3. **Ligação no shell**: `MainActivity` observa o `StateFlow`, passa o modo ao `DiecastTheme` e mantém `ProvideWindowSizeClass`/`AppNavHost` inalterados; o estilo das barras do sistema acompanha o modo resolvido.
4. **UI em Ajustes**: seção **Aparência** com três opções de seleção única, reutilizando os componentes e a coluna de leitura já usados por Ajustes (Spec 005), com `SettingsViewModel` expondo estado e ação.
5. **Superfícies especiais**: `windowBackground` claro/escuro por recurso `values`/`values-night`; sobreposições sobre foto mantêm *scrim* próprio; nenhuma foto é tingida.
6. **Testes**: unidade (store e resolução), instrumentados de UI (seleção, persistência entre recriações, aplicação por tela, contraste renderizado, classes de janela e fonte 200%) e evidência visual por captura nos dois modos, reaproveitando a infraestrutura da Spec 005.

## Estratégia de persistência

- **Mecanismo**: DataStore Preferences, já declarado em `gradle/libs.versions.toml` e no classpath de `app`; sem dependência nova e sem ADR de nova tecnologia.
- **Arquivo**: um `DataStore` próprio do app, em armazenamento privado; gravado fora do banco da coleção e fora do pacote de backup.
- **Formato**: uma chave textual versionada (`appearance_mode`) com valores estáveis `system`/`light`/`dark`; qualquer outro valor é tratado como `system`.
- **Atomicidade**: escritas pelo mecanismo transacional do DataStore; leitura por `Flow` com primeiro valor padrão `system`; nenhum estado parcial.
- **Compatibilidade**: sem migração Room; sem migração de dados de versões anteriores (não existe preferência anterior); instalações existentes passam a exibir **Sistema**, que preserva o comportamento atual.
- **Isolamento**: restaurar um pacote de backup da coleção não toca no arquivo de preferências (FR-415).

## Estratégia de testes

**Unidade (JVM/Robolectric, `app/src/test`)**

- Store de aparência com DataStore isolado em diretório temporário: ausência ⇒ `Sistema`; escrita ⇒ leitura subsequente; valor desconhecido ⇒ `Sistema`; escrita concorrente ⇒ valor íntegro; falha de I/O ⇒ fallback sem exceção propagada.
- Resolução de tema: `Sistema` delega ao tema do Android; `Claro`/`Escuro` independem dele.
- ViewModel de Ajustes: estado inicial, seleção e persistência da ação.

**Instrumentados (Compose UI Test, `app/src/androidTest`)**

- Seção Aparência: três opções, padrão **Sistema**, marca de seleção, semântica de seleção única, alvos ≥ 48 dp, fonte 200% e classes compacta/média/expandida (V.10).
- Aplicação global: telas representativas (painel, catálogo com cartão e foto, ficha, formulário, busca, diálogos, folhas, editor) renderizadas em `Claro` e `Escuro`, verificando paleta coerente e ausência de superfícies da paleta oposta.
- Persistência: escolher e recriar a atividade (`ActivityScenario.recreate`); encerramento real do processo, reinício de aparelho e atualização por cima como evidência manual registrada (não automatizável no emulador). Falha de **escrita** MUST ter teste próprio com store que falha, provando o aviso em pt-BR e a manutenção do valor anterior.
- Barras do sistema: ícones em contraste com o conteúdo nos dois modos, inclusive com aparência forçada divergente do Android.
- Contraste renderizado: captura de elementos reais (`captureToImage`) com amostragem de texto e sobreposições — textos sobre fotos claras e escuras, cartões de coleção e ferramentas do editor — além da verificação das cores declaradas já existente em `ContrastAssert`.
- Neutralidade da foto: a mesma imagem renderizada nos modos Claro e Escuro MUST ser comparada (região da foto no cartão e no editor), comprovando que o tema não a tinge (FR-419).
- Regressão: suíte atual (327 unitários + 208 instrumentados) permanece verde, sem teste removido ou afrouxado.

**Evidência visual**

- Caminhada de capturas nos modos Claro e Escuro nas classes compacta/média/expandida e paisagem, reaproveitando o percurso da Spec 005, com registro em `build/responsive/` e checklist próprio por linha de tela.

## Success Criteria *(mandatory)*

- **SC-401**: Em instalação nova, 100% das aberturas de Ajustes mostram **Sistema** selecionado e a paleta correspondente ao modo do Android naquele momento.
- **SC-402**: A seleção de **Claro** aplica a paleta clara a todas as superfícies visíveis em menos de 1 segundo, sem reinício, sem recarga de dados e sem perda de estado.
- **SC-403**: A seleção de **Escuro** aplica a paleta escura nas mesmas condições de SC-402.
- **SC-404**: O retorno a **Sistema** passa a acompanhar o Android imediatamente; uma mudança de tema do Android com o app aberto em Sistema é refletida em menos de 1 segundo, sem reinício.
- **SC-405**: A escolha sobrevive à recriação da atividade (verificado por teste automatizado) e ao encerramento real do processo, ao reinício do dispositivo e à atualização por cima (evidência manual registrada), em 100% das tentativas.
- **SC-406**: A escolha sobrevive a uma atualização por cima que preserve dados (`adb install -r`), em 100% das tentativas registradas.
- **SC-407**: As 37 linhas do inventário de telas/diálogos da Spec 005 abrem nos modos Claro e Escuro sem superfície da paleta oposta, sem conteúdo ilegível e sem perda de função.
- **SC-408**: Cartões de coleção com foto/sem foto e o editor de fotografias passam nos dois modos, preservando contraste sobre a imagem e a neutralidade da área de edição.
- **SC-409**: O contraste medido sobre elementos renderizados atinge ≥ 4,5:1 (texto normal) e ≥ 3:1 (texto grande/UI) em 100% dos pares verificados, nos dois modos, incluindo texto sobre foto clara e escura.
- **SC-410**: A suíte existente permanece verde (0 falhas, nenhum teste removido, ignorado ou afrouxado), com os novos testes aprovados.
- **SC-411**: `git diff` de `core/domain`, `core/data` (exceto o que for estritamente do store, se ali alocado), esquema Room, `applicationId` e `app_name` comprova ausência de mudança fora da apresentação.
- **SC-412**: A troca de aparência é percebida em < 100 ms e a abertura fria não regride além de +50 ms sobre o baseline de 993 ms, medido com comando reproduzível.

## Impacto nas especificações anteriores e rastreabilidade

| Spec | Relação | Situação |
|---|---|---|
| **001 — Estrutura e funcionalidades fundamentais** | FR-023 exige preservar formulário em preenchimento sob rotação/processo encerrado; a troca de tema é mais um evento de ciclo de vida que não pode perder entrada. Também fixa idioma pt-BR e identidade do produto. | **Complementa** (FR-426 desta spec); nenhuma alteração de requisito da 001 é necessária. |
| **003 — Coleção de miniaturas** | FR-013 e `contracts/collection-card.md` declaravam "tema escuro, fundo preto, cartões em cinza escuro" como identidade; com tema claro, a leitura literal falhava. | **Esclarecido em 2026-11-26** (CL-003-01/CL-003-02): a identidade passa a ser declarada por modo, com os tokens escuros preservados no escuro e os claros no claro. |
| **004 — Editor de fotografias** | Premissa: "a interface segue o tema escuro". O editor tem sobreposições e área neutra que precisam funcionar nos dois modos. | **Esclarecido em 2026-11-26** (CL-004-01): a premissa passa a declarar a aparência escolhida e a neutralidade da edição; FR-419/FR-420 seguem complementando com contraste e ausência de tingimento. |
| **005 — Interface responsiva** | Já prevê "tema escuro e claro" em casos de borda (linha 131) e exige contraste/legibilidade por classe de janela (FR-326). A seção Aparência deve respeitar colunas, alvos e fonte ampliada. | **Estende**: a seção Aparência é mais uma superfície sujeita a V.10; nenhuma mudança nos requisitos da 005. |
| **002 — Manual do usuário** | Descreve Ajustes e a identidade visual; passa a estar incompleto sem a seção Aparência. | **Atualização documental** (não é requisito de produto). |

> Rascunho completo para aprovação (texto pronto para as Specs 003/004, opções, impacto e registro): [`clarifications-g2.md`](./clarifications-g2.md).

**Esclarecimento aplicado à Spec 003 (CL-003-01, 2026-11-26)**

> Texto final: FR-013 da [`Spec 003`](../003-colecao-miniaturas/spec.md) e §3.3 de [`clarifications-g2.md`](./clarifications-g2.md); o contrato do cartão está em §3.4 do mesmo documento. Registro da execução em [`docs/validacao.md`](../../docs/validacao.md) §10.

**Rastreabilidade resumida**

| Pedido do usuário | Requisitos | Critérios |
|---|---|---|
| FR-001 (opções) | FR-401–FR-403 | SC-401 |
| FR-002 (seção em Ajustes) | FR-402, FR-424, FR-425 | SC-401, SC-407 |
| FR-003 (padrão Sistema) | FR-404, BR-402 | SC-401 |
| FR-004 (persistência) | FR-412–FR-416 | SC-405, SC-406 |
| FR-005 (aplicação imediata) | FR-406, NFR-401 | SC-402, SC-403, SC-412 |
| FR-006 (integração Material 3) | FR-405, FR-417, FR-418, FR-429 | SC-407, SC-411 |
| FR-007 (acompanhar o Android) | FR-407, BR-403 | SC-404 |
| FR-008 (consistência visual) | FR-408–FR-411, FR-421 | SC-407, SC-411 |
| FR-009 (identidade da coleção) | FR-417–FR-421 | SC-408 |
| FR-010 (acessibilidade) | FR-422–FR-426, NFR-408 | SC-409 |

## Riscos e dependências

| Risco | Impacto | Mitigação prevista |
|---|---|---|
| Divergência com a "identidade escura" da Spec 003/004 | Interpretação ambígua de requisito aprovado | Esclarecimento proposto acima, registrado sem alterar as specs; testes provam que o modo escuro permanece idêntico ao atual |
| Primeiro quadro com paleta oposta em modo forçado | Piscada perceptível na abertura | Fundo de janela por recurso claro/escuro para Sistema; medir o resíduo por captura de quadro (limite ≤ 16,7 ms) e registrar limitação com o valor medido (NFR-403) |
| Barras do sistema seguindo o Android e não a preferência | Ícones ilegíveis sobre o conteúdo | Estilo das barras derivado do modo resolvido (FR-409), com teste de contraste |
| Contraste de texto sobre fotos | Texto some sobre imagem clara | *Scrim* sobre a imagem e teste de contraste renderizado com pior caso (FR-423) |
| Custo de leitura assíncrona no startup | Regressão de abertura fria | Primeiro valor padrão `system`, leitura em I/O, medição com `am start -W` contra 993 ms (NFR-402) |
| `docs/dependencias.md` descreve DataStore com finalidade que o código não cumpre | Inventário incorreto (R.5) | Corrigir a finalidade para "preferências simples (aparência e lembrete, quando migrado)" e registrar a decisão no plano |
| DataStore declarado mas não usado hoje | Medição de tamanho pode surpreender | Medir APK antes/depois; limite de +100 KB (NFR-404) |
| Preferência ser confundida com dado da coleção | Backup/restauração alterando aparência | Exclusão explícita do pacote e teste de restauração (FR-415) |

**Dependências**: DataStore Preferences 1.1.1 (já declarada; sem ADR de nova tecnologia), Hilt (store injetável), Compose Material 3 (tema), infraestrutura de testes e capturas da Spec 005.

## Assumptions

- O usuário é único no aparelho (A1 da constituição); a preferência é por instalação, não por conta.
- O Android tem suporte a tema claro/escuro do sistema a partir do `minSdk 26`; em versões que não exponham a preferência, o modo **Sistema** usa o valor efetivo do sistema.
- O ícone do app, o splash do sistema e a identidade do produto não mudam.
- A preferência não é exportada nem sincronizada; desinstalar remove a escolha.
- O `values-night` já é o mecanismo do sistema para recursos; a feature usa esse caminho apenas para o fundo de janela, sem duplicar o tema Compose.
- Não há requisito de tema por horário (automático claro/escuro por relógio).

## Critérios de conclusão

1. As três opções existem em Ajustes, com seleção única, marca visível e semântica correta.
2. A escolha persiste após fechamento, reinício do app, reinício do aparelho e atualização com preservação de dados.
3. A troca é imediata e não perde estado de tela.
4. O modo **Sistema** acompanha o Android em execução; modos forçados prevalecem.
5. Todas as telas e superfícies do inventário da Spec 005 abrem nos dois modos sem superfície da paleta oposta.
6. Cartões de coleção, fotos e editor mantêm contraste e neutralidade nos dois modos, comprovados por contraste renderizado.
7. A suíte existente permanece verde, sem remoção ou afrouxamento de teste.
8. Nenhum arquivo de domínio, banco, esquema, `applicationId` ou `app_name` é alterado.
9. KDoc de `Theme.kt`, manual do usuário, `docs/validacao.md`, `docs/dependencias.md` e `docs/revisao-dependencias.md` refletem o comportamento implementado.
10. Medições de startup e tamanho do APK registradas contra o baseline da Spec 005.
