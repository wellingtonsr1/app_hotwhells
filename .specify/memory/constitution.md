# Constituição do Projeto — Hot Wheels Collector

**Sistema de gerenciamento profissional de coleções de miniaturas em escala 1:64**

| Campo | Valor |
|---|---|
| Versão da constituição | 1.2.0 |
| Data de criação | 2026-11-16 |
| Data de ratificação | **2026-11-16** |
| Última emenda | 2026-11-16 — Emenda 2: stack nativa **Kotlin + Jetpack Compose** e distribuição por **sideload preparada para o Play** (resolve A11 e A12) |
| Status | **RATIFICADA** (v1.2.0) — portões G0 e G1 autorizados; plano (G3) liberado |
| Plataforma alvo | **Android** (uso primário em celular, offline-first) |
| Escopo | Todo o sistema: catálogo, coleção pessoal, gestão financeira, relatórios, importação/exportação, reconhecimento assistido — entregue como aplicativo Android nativo instalável |
| Idioma do produto e da documentação | Português brasileiro (código e identificadores em inglês) |

Esta constituição é o documento de maior autoridade do projeto. Ela prevalece sobre especificações, planos, tarefas, convenções locais e preferências individuais. Qualquer artefato que a contradiga é considerado inválido e MUST ser corrigido antes de avançar de fase.

---

## Preâmbulo

O Hot Wheels Collector não é um CRUD de carrinhos. É um sistema de registro patrimonial e histórico de uma coleção, no qual a **precisão do dado** importa mais do que a velocidade de cadastro, e no qual a distinção entre *modelo*, *variante* e *exemplar físico* é a base de toda a modelagem. Um erro de agrupamento (tratar duas variantes como duplicata, ou dois exemplares como um só) destrói informação que o colecionador não consegue reconstruir depois.

Toda decisão subsequente de requisito, arquitetura, banco de dados, interface, teste e implantação deve ser rastreável a um princípio desta constituição.

---

## Artigo I — Integridade dos dados (princípio não negociável)

O dado do colecionador é insubstituível. Perder ou corromper registro é falha crítica, não bug cosmético.

### Modelo de domínio obrigatório

I.1. O sistema MUST distinguir três entidades conceitualmente distintas, em todas as camadas (banco, API, interface, relatórios):

| Entidade | Definição | Exemplo |
|---|---|---|
| **Modelo** | Identidade do veículo em miniatura, independente de pintura, rodas ou edição | "Nissan Skyline GT-R (R34)" |
| **Variante** | Versão específica do modelo, definida por atributos que a tornam um item distinto para o colecionador | Skyline R34 azul, rodas 5SP, base plástica, Mainline 2023 |
| **Exemplar** | Unidade física que o colecionador possui, com condição e histórico próprios | O Skyline azul que está na caixa 12, comprado em 2024 por R$ 19,90 |

I.2. Um Modelo MUST poder ter N Variantes; uma Variante MUST poder ter N Exemplares. Nenhuma das três entidades MUST ser fundida com outra para simplificar a implementação.

I.3. A detecção de duplicidade MUST ocorrer no nível de **Variante + Exemplar**, nunca apenas pelo nome comercial do modelo. Dois exemplares da mesma variante são duplicados; duas variantes do mesmo modelo não são.

I.4. Toda entidade persistida MUST possuir identificador único estável (surrogate key), independente de nomes exibidos, que podem ser corrigidos sem quebrar relacionamentos.

### Regras de persistência

I.5. O banco de dados MUST garantir integridade referencial por meio de chaves estrangeiras declaradas e ativas. Integridade referencial não pode depender exclusivamente de validação na aplicação.

I.6. Nenhuma operação MUST provocar perda silenciosa de registro. Exclusões de entidades com dependentes MUST exigir decisão explícita do usuário (bloquear, reatribuir ou arquivar), nunca cascata invisível.

I.7. O sistema MUST preferir **arquivamento/inativação** (soft delete com carimbo de data) à exclusão física para entidades que compõem histórico (exemplares, aquisições, avaliações, movimentações).

I.8. Alterações relevantes MUST deixar trilha auditável: o quê mudou, quando, por quem (quando houver usuário identificado) e o valor anterior, no mínimo para: preço pago, avaliação, condição, localização e vínculo de variante.

I.9. Campos desconhecidos MUST ser representados como desconhecidos (`NULL` ou estado explícito "não informado"), nunca como valor inventado, vazio ambíguo ou sentinela arbitrária (`"N/A"`, `0`, `""`).

I.10. Migrações de esquema MUST ser versionadas, determinísticas, reproduzíveis e não destrutivas por padrão. Migração que remove coluna ou tabela com dados MUST ser acompanhada de plano de preservação/exportação e aprovação explícita.

I.11. Valores monetários MUST ser armazenados em tipo decimal exato (nunca ponto flutuante), com moeda e data de referência explícitas.

I.12. Um mesmo registro MUST NOT existir em duas versões conflitantes: escritas concorrentes sobre o mesmo exemplar MUST ser detectadas (controle de concorrência) em vez de sobrescrever silenciosamente.

### Portão de qualidade

I.13. Nenhuma entrega é aceita sem teste que prove: (a) integridade referencial sob tentativa de exclusão indevida; (b) comportamento correto de modelo→variante→exemplar com múltiplas variantes e múltiplos exemplares; (c) ausência de duplicidade indevida em cadastros concorrentes.

---

## Artigo II — Modularidade e separação de responsabilidades

II.1. O sistema MUST ser organizado em módulos alinhados aos domínios funcionais (catálogo, coleção, organização, desejos, negociação, financeiro, localização, mídia, busca, relatórios, identidade e acesso), com fronteiras explícitas.

II.2. Cada módulo MUST possuir uma única razão para mudar; regra de negócio MUST residir no domínio/serviço, não em componentes de interface nem em controladores de rota.

II.3. Dependências entre módulos MUST ser unidirecionais e declaradas. Dependência circular entre módulos é proibida.

II.4. Acesso a dados MUST ser isolado atrás de repositórios/serviços de persistência; consultas SQL MUST NOT ser construídas por concatenação de entrada do usuário em nenhuma camada.

II.5. Código duplicado MUST ser extraído para um único ponto de verdade assim que a duplicação for identificada em duas ocorrências com a mesma regra.

II.6. Toda regra de negócio não trivial MUST ser testável sem interface gráfica, sem rede e sem banco real (ou com banco isolado).

II.7. A estrutura de pastas e as convenções de nomenclatura MUST ser documentadas antes da primeira implementação e mantidas estáveis.

---

## Artigo III — Segurança e privacidade

III.1. Senhas MUST ser armazenadas apenas como hash produzido por algoritmo de derivação de chave apropriado (bcrypt, scrypt ou Argon2id), com salt único por usuário. Senha em texto puro, hash reversível ou criptografia simétrica de senha são proibidos.

III.2. Credenciais, chaves de API, tokens e segredos MUST NOT residir no código-fonte, em arquivos versionados ou em artefatos de build. Devem vir de variáveis de ambiente/cofre de segredos, com arquivo de exemplo documentado.

III.3. Toda validação de entrada MUST ser repetida no servidor. Validação apenas no cliente é considerada ausência de validação.

III.4. O sistema MUST ser protegido contra, no mínimo: SQL/NoSQL injection, XSS (com escaping por padrão no template/renderização), CSRF em operações autenticadas por cookie, controle de acesso quebrado (IDOR), upload de arquivo malicioso, path traversal, force brute/credential stuffing (com limitação de tentativas) e exposição de dados em mensagens de erro.

III.5. Toda consulta e todo acesso a arquivo MUST ser autorizado no servidor, derivando o proprietário da sessão autenticada — nunca de identificador enviado pelo cliente.

III.6. Sessões MUST expirar; tokens de sessão MUST ser revogáveis; alteração de senha MUST invalidar sessões existentes.

III.7. Dados pessoais MUST ser coletados no mínimo necessário (minimização), com finalidade declarada e base legal identificada. O sistema MUST permitir exportação e exclusão dos dados pessoais do titular, em conformidade com a LGPD.

III.8. Dados sensíveis MUST NOT aparecer em logs, mensagens de erro devolvidas ao cliente, URLs ou telemetria. Logs MUST registrar identificadores, não conteúdo de segredo.

III.9. Comunicação em produção MUST ocorrer sobre TLS. Cookies de sessão MUST ser `HttpOnly`, `Secure` e com política `SameSite` adequada.

III.10. Dependências de terceiros MUST ser verificadas quanto a vulnerabilidades conhecidas antes de entrar em produção; vulnerabilidade crítica sem correção disponível MUST bloquear a entrega.

III.11. Nenhum mecanismo de segurança MUST ser desabilitado, afrouxado ou contornado para fazer teste ou build passar. Se um controle impede o avanço, o controle está correto e o código está errado.

---

## Artigo IV — Usabilidade, idioma e acessibilidade

IV.1. Toda a interface, mensagens de erro, rótulos, relatórios exportados e documentação de usuário MUST estar em português brasileiro, com terminologia de colecionador (exemplar, variante, série, mainline, lacrado, solto).

IV.2. O cadastro de um exemplar a partir de um modelo/variante já existente MUST ser rápido: o fluxo mínimo MUST exigir apenas os campos que identificam o exemplar físico, herdando o restante da variante.

IV.3. A interface MUST diferenciar visualmente "exemplares" de "modelos distintos" em todos os indicadores, sem rótulos ambíguos como "total de carros".

IV.4. Toda operação que altera dados MUST produzir feedback explícito de sucesso ou falha, com causa acionável em caso de erro.

IV.5. O produto MUST ser um aplicativo **Android** instalável, projetado para uso primário em celular: alvos de toque de no mínimo 48 dp, uso confortável com uma mão, telas a partir de 360 dp de largura, e funcionamento correto em orientação retrato e paisagem. Layout de tablet/desktop é opcional e não MUST bloquear a v1.

IV.6. A interface MUST atender, no mínimo, o equivalente a WCAG 2.1 nível AA na plataforma Android: suporte a TalkBack, respeito ao tamanho de fonte do sistema, contraste ≥ 4.5:1, rótulos semânticos em campos e imagens, foco visível e navegação por D-pad/teclado externo.

IV.7. Nenhuma entrada de formulário MUST ser perdida em interrupção do ciclo de vida Android (rotação de tela, processo encerrado em segundo plano, chamada recebida, troca de tema).

IV.8. Informação estimada (valor de mercado, sugestão de IA, correspondência de código de barras) MUST ser visualmente identificada como estimativa/sugestão, com a fonte e a data, e MUST NOT ser apresentada como valor oficial ou garantido.

IV.9. Nenhuma ação destrutiva MUST ocorrer em um toque sem confirmação que explique o efeito.


---

## Artigo V — Qualidade, testes e verificabilidade

V.1. Uma funcionalidade MUST NOT ser considerada concluída por ter interface implementada. Conclusão exige comportamento verificado por teste automatizado ou evidência de execução registrada.

V.2. A pirâmide de testes MUST conter: testes unitários do domínio, testes de integração com banco real no aparelho/emulador (não mock de banco), testes de interface instrumentados no Android (Compose UI Test/Espresso) para os fluxos críticos (cadastro, edição, exclusão, foto, importação) e testes de migração de esquema local.

V.3. Cenários obrigatórios de teste de domínio: um modelo com múltiplas variantes; uma variante com múltiplos exemplares; exemplar lacrado vs. solto vs. danificado; transformação de item da lista de desejos em aquisição preservando histórico; movimentação de localização com histórico; duplicidade detectada por variante e não por nome.

V.4. Falha de teste MUST NOT ser ignorada, marcada como "skip" ou afrouxada para liberar entrega. Corrigir a causa é obrigatório.

V.5. Toda migração de banco MUST ser exercitada em teste automatizado, incluindo ida (up) e, quando aplicável, reversão (down).

V.6. Erros MUST ser tratados de forma tipada e explícita; exceção genérica capturada e silenciada é proibida. Todo erro exposto ao usuário MUST ter mensagem em pt-BR e código correlacionável no log.

V.7. Logs MUST ser estruturados o suficiente para diagnóstico (nível, timestamp, contexto, identificador de correlação), sem dados sensíveis.

V.8. Nenhum número de desempenho MUST ser afirmado sem medição reproduzível registrada.

V.9. Os limites não funcionais (latência, volume, disponibilidade) MUST ser quantificados em especificação antes de serem declarados atendidos; até lá, são metas provisórias, não garantias.

---

## Artigo VI — Evolução, extensibilidade e independência de terceiros

VI.1. O catálogo MUST ser autossuficiente: nenhuma funcionalidade essencial pode depender, em tempo de execução, de serviço externo que possa sair do ar, mudar termos ou cobrar por acesso. Integrações externas são enriquecimento opcional, com degradação graciosa.

VI.2. O modelo de dados MUST NOT assumir que todos os fabricantes usam a taxonomia da Mattel. Fabricante, série, coleção e subcoleção MUST ser entidades de primeira classe, com hierarquia flexível e coleções personalizadas.

VI.3. O sistema MUST suportar a inclusão de novos fabricantes e novas escalas sem migração destrutiva e sem alteração de código de domínio.

VI.4. Reconhecimento de imagem, OCR e leitura de código de barras MUST ser implementados como **assistentes**: produzem sugestões com nível de confiança e origem, exigem confirmação humana e MUST NOT gravar dados no catálogo sem aceite explícito. Quando inconclusivos, MUST declarar "não identificado" em vez de inventar atributo.

VI.5. Os módulos de reconhecimento MUST ser acessados por uma interface de abstração (provedor substituível), permitindo trocar ou desligar o provedor sem afetar o domínio.

VI.6. A API MUST ser documentada (contrato legível por máquina, ex.: OpenAPI) e versionada, com política de compatibilidade declarada.

VI.7. Exportação integral dos dados MUST existir e ser suficiente para reconstruir a coleção em outro sistema, incluindo metadados das imagens — o colecionador não pode ficar preso ao sistema.

VI.8. A arquitetura MUST ser **mobile-first** e permitir evolução posterior para web/desktop ou sincronização em nuvem sem reescrita do domínio.

VI.9. Cada fase do roadmap MUST entregar valor utilizável isoladamente, sem exigir que a fase seguinte exista.

VI.10. O aplicativo MUST funcionar integralmente **offline** (cadastro, edição, fotos, busca, filtros, relatórios e cálculo financeiro) sem conexão à internet. Qualquer sincronização, catálogo remoto ou chamada a serviço externo MUST ser opcional, não bloqueante e com degradação explícita.

VI.11. O armazenamento local MUST ser tratado como fonte de verdade da coleção, com identificadores gerados no cliente (UUID) e carimbos de tempo, de forma que uma futura sincronização entre aparelhos possa ser adicionada sem migração destrutiva.

---

## Restrições técnicas adicionais

R.1. Banco de dados relacional normalizado (3FN), com esquema versionado por migrações. Desnormalização MUST ser justificada por medição e documentada.
R.2. Imagens MUST ser armazenadas no armazenamento privado do aplicativo (ou escolhidas via Photo Picker), com o banco local guardando apenas metadados e referência de caminho; imagens MUST NOT ser embutidas como BLOB no banco principal.
R.3. Fotos, vindas de câmera ou galeria, MUST ser processadas antes de persistir: redimensionamento/compressão com limite de tamanho, remoção de EXIF sensível (geolocalização), validação de tipo real por conteúdo (não por extensão) e correção de orientação.
R.4. Nenhuma decisão tecnológica MUST ser tomada por preferência pessoal: a escolha MUST ser registrada como decisão de arquitetura com alternativas, critérios, trade-offs e consequências (formato ADR).
R.5. Toda dependência externa MUST ser registrada com finalidade, versão, licença, custo e plano de substituição.
R.6. Backup MUST ser possível de duas formas: (a) cópia portátil gerada pelo próprio aplicativo (pacote de dados + imagens) exportável e reimportável em outro aparelho; e (b) backup do sistema operacional (Android Auto Backup/Google Drive), quando disponível. A restauração MUST preservar relacionamentos e integridade referencial, e MUST ser testada, não apenas documentada.
R.7. O aplicativo MUST operar com 10.000 exemplares e 50.000 fotografias no aparelho sem degradação perceptível nos fluxos principais; abertura de listas em menos de 1 segundo e busca em menos de 300 milissegundos são as metas provisórias a confirmar em especificação.
R.8. Persistência local MUST usar banco relacional embarcado (SQLite) com transações, chaves estrangeiras ativas e migrações versionadas e testadas (ida e reversão). Migração destrutiva de dados do usuário é proibida sem exportação prévia obrigatória.
R.9. As permissões Android solicitadas MUST ser mínimas e solicitadas em contexto (câmera ao fotografar, por exemplo), com recusa tratada como caminho válido — o aplicativo MUST continuar utilizável sem câmera ou sem galeria.
R.10. O aplicativo MUST declarar `targetSdk` atualizado e atender aos requisitos de publicação do Google Play quando distribuído por lá, incluindo política de privacidade, formulário de segurança de dados e assinatura de release. Nenhuma chave de assinatura MUST ser versionada.
R.11. Reconhecimento de imagem, OCR e código de barras MUST priorizar processamento **no aparelho** (ex.: ML Kit on-device) para preservar privacidade e funcionar offline. Envio de fotos a serviço em nuvem MUST ser opcional, explícito, documentado e desativado por padrão.
R.12. O aplicativo MUST respeitar o ciclo de vida Android: operações longas (importação, geração de backup, processamento de imagem) MUST sobreviver a segundo plano ou ser retomáveis, e MUST NOT bloquear a thread principal.
R.13. O tamanho do artefato instalável e o consumo de memória MUST ser monitorados; nenhuma dependência MUST entrar sem avaliação de impacto em tamanho e startup.
R.14. A implementação MUST ser **nativa Android**: Kotlin com Jetpack Compose para interface, Room (SQLite) para persistência local e ML Kit on-device para OCR e leitura de código de barras. Frameworks multiplataforma (Flutter, React Native) e invólucros web (TWA/WebView) estão fora de escopo; adoção de qualquer um deles exige emenda a esta constituição.
R.15. Na v1 a distribuição MUST ser por arquivo assinado (APK, sideload), sem conta em loja. O projeto MUST manter prontos, sem publicar automaticamente: pacote de publicação (AAB), assinatura de release fora do versionamento, `targetSdk` atualizado, política de privacidade acessível no aplicativo e informações para o formulário de segurança de dados do Google Play.
R.16. A stack tecnológica não é decisão de implementação discricionária: está fixada nesta constituição por decisão do responsável pelo produto em 2026-11-16, e substituições MUST ser registradas como emenda, não como ADR isolado.

---

## Fluxo de desenvolvimento e portões de qualidade

O desenvolvimento MUST seguir a ordem abaixo. Nenhum portão MUST ser saltado.

| Portão | Artefato | Critério de saída |
|---|---|---|
| **G0 — Constituição** | `constitution.md` ratificada | Aprovação explícita; ambiguidades bloqueantes resolvidas ou registradas com decisão padrão aceita |
| **G1 — Especificação** | `spec.md` (requisitos funcionais e não funcionais, critérios de aceitação, casos de borda) | Requisitos rastreáveis, sem "TBD" em item de escopo aprovado |
| **G2 — Esclarecimentos** | `clarifications` incorporadas à spec | Toda ambiguidade de escopo, dado ou regra de negócio resolvida |
| **G3 — Plano e modelo de dados** | `plan.md`, ADRs, DER, dicionário de dados, estratégia de testes | Arquitetura justificada; DER sem entidade órfã; restrições documentadas |
| **G4 — Tarefas** | `tasks.md` por módulo e dependência | Cada tarefa com critério de aceite e teste associado |
| **G5 — Análise de consistência** | `analysis.md` | Constituição ↔ spec ↔ plano ↔ tarefas sem contradição; lacunas de teste identificadas |
| **G6 — Implementação por fase** | Código + testes + documentação | Suíte verde, migrações aplicadas, documentação atualizada, limitações declaradas |

### Disciplina de alteração (obrigatória antes de qualquer mudança relevante)

1. Identificar arquivos e módulos afetados.
2. Verificar dependências e consumidores.
3. Avaliar risco de regressão e impacto em dados existentes.
4. Definir a alteração mínima suficiente.
5. Executar os testes afetados e os de regressão.
6. Registrar o resultado e atualizar a documentação.

### Proibições absolutas

- Remover ou alterar funcionalidade existente sem autorização explícita.
- Implementar qualquer coisa fora do escopo da tarefa em execução.
- Executar migração destrutiva.
- Apagar dados para facilitar teste.
- Armazenar senha em texto puro ou credencial no código.
- Desabilitar controle de segurança, teste ou verificação para "destravar" o build.
- Declarar conclusão sem evidência (comando executado, saída, teste).
- Apresentar estimativa como preço oficial de mercado.

### Definição de concluído (por incremento)

Um incremento é considerado concluído quando, cumulativamente: comportamento implementado conforme spec aprovada; testes automatizados cobrindo o caminho feliz e os casos de borda relevantes, verdes; migrações versionadas e exercitadas; validação de entrada no servidor; autorização verificada; documentação do módulo e da API atualizada; limitações conhecidas registradas; ausência de regressão nos testes existentes.

---

## Governança

G.1. Esta constituição MUST ser ratificada pelo responsável pelo produto antes de qualquer especificação.

G.2. Emendas MUST ser propostas por escrito, com justificativa, impacto nos artefatos existentes e plano de conformidade; entram em vigor após aprovação e incremento de versão.

G.3. Versionamento semântico: **MAJOR** para remoção ou redefinição de princípio; **MINOR** para novo princípio, artigo ou restrição; **PATCH** para esclarecimento sem mudança de obrigação.

G.4. Todo artefato de especificação, plano e tarefa MUST ser verificado contra esta constituição no portão G5. Violação MUST ser corrigida, não registrada como dívida.

G.5. Exceções MUST ser explícitas, temporais, justificadas, aprovadas e registradas com prazo de expiração. Exceção permanente equivale a emenda.

G.6. Conflito entre constituição e solicitação pontual resolve-se em favor da constituição; se a solicitação for legítima, o caminho correto é emendar a constituição.

### Histórico de emendas

| Versão | Data | Mudança | Classificação |
|---|---|---|---|
| 1.2.0 | 2026-11-16 | Stack nativa fixada em Kotlin + Jetpack Compose + Room + ML Kit on-device (R.14, R.16), distribuição por sideload com preparo para Play (R.15); A11 e A12 resolvidos em `/speckit-clarify` e registrados na especificação | MINOR — novas restrições técnicas, nenhuma obrigação de qualidade removida |
| 1.1.0 | 2026-11-16 | Plataforma alvo definida como aplicativo Android; offline promovido a requisito (VI.10); identidade local e preparação para sincronização (VI.11); usabilidade reexpressa para Android (IV.5–IV.9); testes instrumentados (V.2); restrições R.8–R.13; Anexo A revisado (A1, A2, A6, A10 resolvidos/repropostos; A11–A13 novos) | MINOR — nenhuma obrigação foi removida; obrigações de plataforma foram acrescentadas |
| 1.0.0 | 2026-11-16 | Criação da constituição | — |

---

## Anexo A — Decisões pendentes que bloqueiam a especificação

Estas decisões alteram escopo, arquitetura ou conformidade. As marcadas como **G1** MUST ser respondidas (ou aceitas na forma proposta) antes do portão G1; as marcadas como **G3** podem aguardar o plano, desde que registradas em ADR.

| ID | Decisão | Impacto | Proposta padrão |
|---|---|---|---|
| A1 (G1) | Multiusuário real (contas isoladas) ou instância pessoal única? | Autenticação, autorização, LGPD, sincronização | Instância pessoal única no aparelho, sem conta obrigatória; modelo preparado para sincronização multiusuário futura (VI.11) |
| A2 (G1) | Implantação: web, local, ou aplicativo Android? | Stack inteira, backup, distribuição | **RESOLVIDO na Emenda 1: aplicativo Android offline-first** |
| A3 | Catálogo de referência mundial (modelos conhecidos) entra na v1 ou em fase futura? | Volume de dados, curadoria, esforço de importação | Fora da v1; arquitetura preparada para receber |
| A4 | Reconhecimento por IA: provedor pago já na v1 ou apenas arquitetura de abstração na Fase 5? | Custo recorrente, privacidade das fotos, dependência externa | Apenas abstração e fluxo de sugestão na v1 |
| A5 | Leitura de código de barras depende de base de produtos externa ou apenas de base local do usuário? | Dependência externa, cobertura real | Base local + preenchimento manual assistido |
| A6 (G1) | Metas numéricas de desempenho e volume (exemplares, fotos, latência, RPO/RTO de backup) | Critérios de aceitação e testes de carga | 10.000 exemplares, 50.000 fotos, lista < 1 s, busca < 300 ms (ver R.7); confirmar na spec |
| A7 | Moeda: apenas BRL ou múltiplas moedas com cotação histórica? | Modelo financeiro, relatórios | BRL na v1, com campo de moeda preparado |
| A8 | Coleção compartilhável/publicável (link público, vitrine) na v1? | Privacidade, rotas públicas, cache | Fora da v1 |
| A9 | Uso de marca: identidade "inspirada", sem logotipos ou ativos registrados da Mattel? | Propriedade intelectual | Apenas inspiração cromática/tipográfica; nenhum ativo oficial |
| A10 (G1) | Sincronização offline-first é requisito da v1? | Complexidade de arquitetura e conflitos | **RESOLVIDO na Emenda 1: offline é requisito obrigatório (VI.10); sincronização entre aparelhos fica fora da v1** |
| A11 | Stack Android: nativo, Flutter, React Native ou wrapper web? | Todo o plano, desempenho, acesso a câmera/ML Kit, custo de manutenção, tamanho do APK | **RESOLVIDO na Emenda 2: nativo Kotlin + Jetpack Compose + Room + ML Kit on-device (R.14)** |
| A12 | Distribuição: Google Play ou sideload? | Assinatura, `targetSdk`, política de privacidade, formulário de segurança de dados, atualizações | **RESOLVIDO na Emenda 2: sideload de APK assinado na v1, pacote e requisitos de Play preparados, sem publicar (R.15)** |
| A13 (G3) | Haverá conta/sincronização em nuvem mais adiante? | Modelo de identidade, IDs, resolução de conflito | Sem conta na v1; UUID no cliente e timestamps desde já (VI.11) |

---

## Anexo B — Rastreabilidade

Toda especificação, decisão de arquitetura, tarefa e teste MUST referenciar o artigo desta constituição que o justifica (ex.: `[Art. I.3]` para detecção de duplicidade por variante). O portão G5 MUST produzir essa matriz de rastreabilidade e apontar qualquer requisito sem princípio, princípio sem verificação e teste sem requisito.

---

*Documento criado em 2026-11-16, ratificado no mesmo dia (v1.1.0) e emendado duas vezes: v1.1.0 fixou Android como plataforma e v1.2.0 fixou stack e distribuição. Portões G0 e G1 fechados; nenhum código escrito até aqui.*
