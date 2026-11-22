# Phase 0 — Research: Manual de Usuário Completo

**Feature**: `002-manual-usuario` | **Data**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

## Âncora de versão documentada

| Item | Valor verificado |
|---|---|
| Versão / versionCode | `0.1.0` / `1` (`app/build.gradle.kts`) |
| Commit no início da produção | `424e4fb` (2026-11-21 19:52:50 −03:00) |
| Pacote | `br.com.hotwheelscollector` (debug: `.debug`) |
| Emulador disponível | `hotwheels_test`, `target=android-36`, imagem `android-36/google_apis/x86_64` |
| Ferramentas | `python3` + `markdown` 3.10.3 + `weasyprint` 70.0 |
| SDK/ADB | `sdk.dir=.tools/android-sdk` (`platform-tools/adb`, `emulator/emulator`) |
| Strings exibidas | 677 entradas em `app/src/main/res/values/strings.xml` |

> Correção em relação ao texto inicial do plano: o AVD existente chama-se `hotwheels_test`
> (não `hw_test`). `adb` não está no `PATH`; os comandos usam o caminho do `sdk.dir`.

## Decisões

### D1 — Fonte de verdade em camadas

**Decisão**: toda afirmação do manual é rastreada na ordem
`AppNavHost.kt` (destinos reais) → `feature/<área>/*Screen.kt` (o que aparece) →
`*ViewModel.kt` (estado, mensagens, ações) → `core/domain` e `core/data` (regras,
validações, cálculos) → `res/values/strings.xml` (texto exibido).

**Racional**: a navegação é a única lista fechada de telas; o par tela + ViewModel explica
o comportamento sem inferência pelo nome do componente; a string é a única fonte legítima
do rótulo (FR-009).

**Alternativas consideradas**:
- *Ler apenas as telas*: não revela validações, cálculos e mensagens de erro.
- *Confiar em `docs/operacao.md` e `docs/continua.md`*: desatualizados — `continua.md`
  afirma que coleções, relatórios/backup e reconhecimento “ainda não têm tela”, o que
  contradiz as rotas implementadas. Documentação histórica não é fonte (FR-015, SC-007).

### D2 — Inventário canônico dos destinos

**Decisão**: gerar a lista de telas a partir dos `data object` que estendem `Destination`
em `navigation/AppNavHost.kt` e conferir com os `composable(...)` do `NavHost`.

**Racional**: os objetos de destino são a definição canônica de rota; a contagem isolada
de arquivos `*Screen.kt` não serve, porque `NegotiationHistoryScreen.kt` é um bloco
interno de `NegotiationScreen`, não um destino próprio.

**Resultado verificado**: 30 destinos — Painel, Catálogo, Busca, Cadastro inicial,
Formulário de variante, Incompletos, Edição de modelo, Adicionar exemplar, Lista de
exemplares, Detalhe do exemplar, Coleções, Detalhe de coleção, Formulário de coleção,
Séries, Código de barras, Duplicados, Negociação, Lista de desejos, Formulário de desejo,
Financeiro, Edição de variante, Localizações, Conteúdo da localização, Ajustes, Backup,
Relatórios, Importação, Dados da coleção, Sobre, Privacidade.

**Barra inferior**: cinco abas fixas — Painel (`dashboard_tab`), Catálogo (`catalog_tab`),
Lista de desejos (`wishlist_tab`), Financeiro (`finance_tab`), Ajustes (`settings_tab`) —
exibida apenas quando a rota atual é de primeiro nível.

**Alternativas consideradas**: listar telas pela árvore de pastas `feature/` — inclui
arquivos auxiliares e omite destinos cuja tela é compartilhada (ex.: `ModelFormScreen`
serve criação e edição).

### D3 — Capturas reais no emulador

**Decisão**: instalar o APK de debug (`br.com.hotwheelscollector.debug`, com ABIs x86/x86_64),
subir o AVD `hotwheels_test` sem snapshot, semear dados fictícios e capturar com
`adb exec-out screencap -p > docs/manual/capturas/<nome>.png`.

**Racional**: o APK de debug é o único com ABI de emulador; capturas de tela real atendem
FR-016 e evitam imagens de mockup de `tools/mockup` (que existem no repositório e **não**
podem ser apresentadas como captura).

**Convenção de nome**: `NN-descricao-curta-estado.png` (ex.: `05-exemplar-detalhe.png`),
com `NN` seguindo a ordem editorial do manual; nomes estáveis, sem data, para que a
referência não quebre entre revisões. Data e commit ficam no registro de evidências e na
legenda.

**Estados que exigem captura própria**: vazio, carregando, erro, sucesso, diálogo de
confirmação, filtro aplicado, dark/light se o app suportar, fonte ampliada (quando usado
para falar de acessibilidade).

**Alternativas consideradas**: mockup em HTML (`tools/mockup/simulacao.py`) — rejeitado
como evidência (viola FR-016), embora sirva para diagramas.

### D4 — Pipeline do PDF

**Decisão**: Markdown → HTML (python-markdown, extensões `tables`, `toc`, `attr_list`) →
PDF (WeasyPrint 70) com `docs/manual/manual.css` definindo capa, sumário, cabeçalho e
rodapé, numeração e quebras.

**Racional**: ferramentas já instaladas no ambiente, sem dependência nova no app; o CSS de
impressão controla `page-break-inside: avoid` em figuras/tabelas e largura máxima de
imagem, resolvendo as páginas quebradas listadas em riscos.

**Alternativas consideradas**:
- *Pandoc + LaTeX*: não instalado; exigiria toolchain pesada.
- *Gerador de PDF do Android/app*: alteraria o aplicativo — proibido por FR-025.
- *Impressão do navegador*: sem sumário automático e sem paginação controlada.

**Registro**: se a ferramenta não reproduzir sumário ou paginação, a limitação é anotada em
`LIMITACOES_DOCUMENTACAO.md` (Assumptions da spec), nunca omitida.

### D5 — Validação de cobertura e de referências

**Decisão**: conferência automatizada em três verificações independentes:
1. **Destinos × matriz**: extrair `Destination.*` de `AppNavHost.kt` e comparar com as
   linhas de `MATRIZ_COBERTURA.md`; qualquer destino sem linha = falha (SC-001).
2. **Elementos × seção**: cada tela documentada tem tabela de elementos; a inspeção da
   `Screen.kt` correspondente lista os componentes interativos e a tabela precisa
   contemplá-los (SC-002).
3. **Imagens**: extrair todo caminho de imagem dos documentos e verificar existência em
   `docs/manual/` (SC-005).

**Racional**: os três critérios são verificáveis por comando, sem julgamento subjetivo; a
matriz e o índice são os artefatos que provam cobertura.

**Alternativas consideradas**: revisão manual apenas — não é reproduzível e não prova
zero lacunas.

### D6 — Validação por execução

**Decisão**: executar no emulador, com instalação limpa, o roteiro de primeiros passos e o
exemplo Skyline antes de fechar os capítulos; registrar data, commit e resultado.

**Racional**: FR-004/FR-005 e SC-003/SC-004 exigem procedimento reproduzível; o app fecha
o banco ao restaurar backup, então a restauração é a última execução e o app é reaberto em
seguida.

**Alternativas consideradas**: validar só por leitura de código — não detecta caminho
inacessível nem rótulo divergente.

### D7 — Tratamento de recursos parciais e legado

**Decisão**: documentar como implementado somente o que a versão `0.1.0` executa; recursos
declarados fora do escopo da v1 mas presentes e alcançáveis (importação CSV, relatórios,
código de barras) entram no manual com a ressalva de estado real. Divergências entre
documentos antigos e código viram item em `LIMITACOES_DOCUMENTACAO.md`/relatório de achados,
sem correção nesta feature.

**Racional**: FR-014/FR-015 e a restrição de atividade somente documental; `docs/operacao.md`
nega relatórios e reconhecimento, mas `settings/reports`, `settings/import` e
`catalog/barcode` existem no `NavHost`.

### D8 — Cálculos financeiros a documentar

**Decisão**: descrever exatamente as agregações de `FinanceDao`:

- **Investido** = `Σ (preço + frete − desconto)` das aquisições de exemplares com
  `status = OWNED`; aquisição sem preço contribui com 0.
- **Estimado** = `Σ` da **avaliação mais recente** de cada exemplar `OWNED` (por `date`,
  desempate `created_at`).
- **Diferença** = estimado − investido; **custo médio** = investido ÷ exemplares com valor
  (nulo quando não há exemplar com valor).
- **Exemplares sem avaliação** e **última data de avaliação** são informados à parte.
- Valores em `MoneyCents` (inteiro de centavos), moeda `BRL`.

**Racional**: FR-013 e SC-004 exigem números conferíveis; a fórmula está no SQL, não numa
coluna, e vendidos/trocados/arquivados ficam fora do investimento.

### D9 — Reconhecimento como assistente

**Decisão**: explicar o código de barras separando (a) leitura e sugestão de correspondência,
(b) confirmação humana e (c) gravação; registrar que ler o código não identifica o carrinho
sozinho e que o resultado pode ser inconclusivo.

**Racional**: FR-012 e Art. VI.4; `RecognitionResult` guarda `source`, `confidence` e
`appliedFields`, e o cadastro manual recebe o código lido como código do fabricante.

### D10 — Escopo documental e integridade do código

**Decisão**: toda escrita fica em `docs/manual/`; nenhuma alteração em `app/`, `core/`,
`gradle`, `local.properties`, banco ou configuração. A conferência final usa `git status`
e deve mostrar apenas arquivos documentais e imagens (SC-008).

**Racional**: FR-025 e Art. I; a auditoria é somente leitura.

## Unknowns resolvidos

| Questão em aberto | Resolução |
|---|---|
| Qual nome de emulador usar | `hotwheels_test` (API 36) |
| Como chamar `adb` | `.tools/android-sdk/platform-tools/adb` |
| Quantos destinos existem | 30 `Destination`; 5 abas; `Settings` abre 4 subáreas |
| Relatórios existentes | Inventário em PDF, CSV e XLSX (`ReportFormat`); não há os “11 relatórios” de documentos antigos |
| Importação existente | CSV com pré-visualização e relatório de erros em CSV |
| Backup | Pacote manual + restauração; restaurar fecha/reabre o banco |
| Fórmula do financeiro | `FinanceDao.observeSummary()` (D8) |
| Texto de referência dos rótulos | 677 entradas de `strings.xml` |

## Riscos herdados do plano

Confirmados em `plan.md` (mudança de rótulo, tela inacessível, imagens no PDF, promessas
antigas, restauração fechando o banco, dados pessoais). Nenhum risco novo identificado.
