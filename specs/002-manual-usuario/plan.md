# Implementation Plan: Manual de Usuário Completo

**Branch**: `main` (spec em `002-manual-usuario`) | **Date**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-manual-usuario/spec.md`

**Versão documentada**: `0.1.0`, versionCode 1, commit `424e4fb` (2026-11-21 19:52:50 −03:00)

## Summary

Produzir um manual de usuário profissional, em pt-BR, que permita a uma pessoa sem
conhecimento prévio operar o Diecast Collector `0.1.0` do primeiro cadastro ao backup.
O manual será derivado de uma auditoria somente-leitura dos **30 destinos de navegação**, das
strings exibidas, dos ViewModels/regras e das telas executadas no emulador, e entregue em
Markdown e PDF, acompanhado de índice funcional, matriz de cobertura e registro de lacunas.

A produção é dividida em quatro passos verificáveis: (1) inventário rastreável de telas,
elementos e regras; (2) captura de telas reais com dados fictícios; (3) redação dos capítulos
no contrato de 10 seções; (4) validação de cobertura, referências de imagem e PDF. Nenhum
arquivo do aplicativo será alterado.

## Technical Context

**Language/Version**: Markdown (CommonMark + tabelas) e HTML/CSS para o PDF; português
brasileiro.

**Primary Dependencies**: Python 3 com `markdown` 3.10.3 e `weasyprint` 70.0 (já instalados
no ambiente); `adb`/emulador API 36 para capturas; fontes do sistema. Nenhuma dependência
entra no app.

**Storage**: Arquivos em `docs/manual/` — manual, índice, matriz, limitações, `capturas/` e
folha de estilo de impressão.

**Testing**: conferência automatizada de rotas × matriz de cobertura, verificação de todos os
caminhos de imagem, checklist do contrato de capítulo, revisão técnica contra o código e
execução do roteiro de primeiros passos no emulador.

**Target Platform**: leitores humanos (Markdown e PDF A4); aplicativo Android 0.1.0
(versionCode 1), emulador `diecast_test` API 36 (AVD existente; `adb` em
`.tools/android-sdk/platform-tools`).

**Project Type**: documentação de produto sobre aplicativo móvel existente.

**Performance Goals**: gerar o PDF completo em menos de 60 s; PDF abrível em leitor comum sem
avisos de imagem ausente; navegação pelo sumário em um toque.

**Constraints**: somente leitura no código do app; nenhuma alteração de código, recurso,
banco, migração, dependência ou configuração; capturas reais com dados fictícios; toda
afirmação precisa ser rastreável ao código ou a uma execução registrada; telas não
executáveis devem ser declaradas, nunca simuladas.

**Scale/Scope**: 30 destinos, 5 abas inferiores, 7 entradas em Ajustes (Localizações, Backup,
Relatórios, Importação, Dados da coleção, Sobre, Privacidade), 30 arquivos `*Screen.kt`
(um deles compõe a tela de Negociação), 677 strings exibidas, manual com 15+ capítulos e 6
entregáveis.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio / restrição | Aplicação nesta entrega | Situação |
|---|---|---|
| Art. I — integridade dos dados | Nenhum dado, esquema ou código é tocado; capturas usam somente registros fictícios | PASSA |
| Art. II — modularidade | A documentação não altera módulos; o inventário segue as fronteiras reais de feature | PASSA |
| Art. III — segurança e privacidade | Nenhum segredo ou dado pessoal em capturas/logs; ambiente offline | PASSA |
| Art. IV.1 — idioma | Manual e artefatos em pt-BR, com terminologia do produto | PASSA |
| Art. IV.3 — modelo ≠ variante ≠ exemplar | Capítulo de estrutura e exemplos usam a distinção real | PASSA |
| Art. IV.8 — estimativa identificada | O manual explica que valor estimado é estimativa, não preço oficial | PASSA |
| Art. V.1/V.8 — evidência | Nenhuma funcionalidade ou número será afirmado sem teste/execução registrada | PASSA |
| Art. VI.7/VI.10 — exportação e offline | Backup/exportação e funcionamento offline documentados como implementados | PASSA |
| R.1–R.16 — stack e distribuição | O plano não muda stack, banco, permissões nem artefatos de release | PASSA |
| G3 — plano e modelo de dados | Este plano, `data-model.md`, `research.md`, contratos e quickstart compõem o G3 | PASSA |

**Sem violações.** A entrega é documental; qualquer defeito encontrado vai para
`LIMITACOES_DOCUMENTACAO.md`/relatório de achados, não é corrigido nesta feature.

## Project Structure

### Documentation (this feature)

```text
specs/002-manual-usuario/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── chapter-contract.md
│   └── coverage-contract.md
└── tasks.md              # próxima fase (/speckit-tasks)

docs/manual/
├── MANUAL_USUARIO_COMPLETO.md
├── MANUAL_USUARIO_COMPLETO.pdf
├── INDICE_FUNCIONALIDADES.md
├── MATRIZ_COBERTURA.md
├── LIMITACOES_DOCUMENTACAO.md
├── manual.css            # capa, sumário, cabeçalho/rodapé e paginação
├── capturas/
│   ├── 01-painel.png
│   └── ...
└── diagramas/
    └── dominio.svg       # relacionamentos fabricante → modelo → variante → exemplar
```

### Source Code (repository root, somente leitura)

```text
app/src/main/java/br/com/diecastcollector/
├── navigation/AppNavHost.kt          # fonte da lista de destinos
├── feature/<área>/*Screen.kt         # telas e componentes visuais
├── feature/<área>/*ViewModel.kt      # estados, validações e mensagens
├── di/                               # grafo consultado para entender dependências
└── res/values/strings.xml            # rótulos e mensagens reais

core/domain/...                       # regras e entidades
core/data/...                         # Room, DAOs, repositórios e serviços
docs/                                 # documentação existente (contexto, não fonte de verdade)
```

**Structure Decision**: a documentação entregue fica em `docs/manual/`; os artefatos de
planejamento ficam na spec. O código é consultado em modo leitura apenas para rastrear
comportamento.

## Phase 0 — Research (ver `research.md`)

Decisões fixadas:

1. Fonte de verdade em camadas: navegação → tela → ViewModel → domínio/dados → strings.
2. Inventário canônico dos 30 destinos e dos subestados (diálogos, sheets, estados vazios).
3. Capturas reais no emulador, com semente fictícia e anotação por sobreposição numericamente
   identificada, sem alterar o app.
4. PDF via Markdown → HTML → WeasyPrint, com capa, sumário, cabeçalho/rodapé e numeração.
5. Validação de cobertura por lista de rotas × matriz e verificação de referências de imagem.
6. Alegações históricas de docs antigos não são fonte: cada afirmação é reconfirmada no código
   da versão documentada.

## Phase 1 — Design & Contracts

- `data-model.md`: modelo de informação do manual (Tela, Elemento, Procedimento, Mensagem,
  Evidência, Lacuna) e mapa das entidades reais do app.
- `contracts/chapter-contract.md`: estrutura obrigatória de capítulo e regras de redação.
- `contracts/coverage-contract.md`: colunas e critérios da matriz de cobertura.
- `quickstart.md`: preparação do emulador, captura, geração do PDF e validações de aceite.

## Implementation Sequence

1. **Inventário** — gerar a lista de 30 destinos a partir de `AppNavHost.kt`, mapear cada tela
   ao arquivo/ViewModel, extrair rótulos de `strings.xml` e listar diálogos/sheets.
2. **Regras** — ler ViewModels, casos de uso, DAOs e serviços para confirmar validações,
   cálculos, duplicidade, arquivamento, reconhecimento e limites de relatórios/importação.
3. **Capturas** — subir o emulador `diecast_test`, criar dados fictícios, capturar cada
tela/estado com `adb exec-out screencap`, nomear e registrar data/commit; anotar elementos
quando necessário.
4. **Redação** — escrever os capítulos na ordem editorial, aplicando o contrato de capítulo e
   mantendo o índice funcional atualizado.
5. **Exemplo e tutoriais** — validar o roteiro de primeiros passos e o exemplo Skyline no
   emulador antes de fechar os capítulos.
6. **PDF** — converter Markdown em HTML/CSS e gerar o PDF com capa, sumário, paginação e
   imagens; conferir visualmente.
7. **Auditoria** — rodar as verificações de cobertura, referências, contrato e `git status`;
   registrar lacunas e revisar ortografia/terminologia.

## Dependencies & Risks

| Risco | Mitigação |
|---|---|
| Rótulo ou fluxo mudar durante a redação | Fixar commit e versão no início; registrar commit no manual e refazer captura afetada |
| Tela inacessível ou estado difícil de reproduzir | Registrar em `LIMITACOES_DOCUMENTACAO.md` e documentar pelo código, sem inventar captura |
| PDF com imagens gigantes ou páginas quebradas | CSS de impressão com largura máxima, `page-break-inside: avoid` em figuras/tabelas e revisão visual |
| Documentação repetir promessas antigas (ex.: 11 relatórios) | Reconfirmar cada afirmação no código; a evidência antiga não é fonte |
| Backups/restauração fecharem o banco durante capturas | Executar restauração por último e reabrir o app; registrar o comportamento |
| Dados pessoais nas capturas | Semente 100% fictícia; conferir cada imagem antes de publicar |

## Complexity Tracking

Nenhuma violação de constituição. Não há necessidade de justificativa de complexidade.

## Post-Design Constitution Re-check

Após `research.md`, `data-model.md`, contratos e `quickstart.md`: a entrega continua
documental, sem alteração de código, com rastreabilidade e limites explícitos. **PASSA.**
