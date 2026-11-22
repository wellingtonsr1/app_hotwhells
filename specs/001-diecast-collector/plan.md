# Implementation Plan: Diecast Collector — Aplicativo Android offline-first

**Branch**: `001-diecast-collector` | **Date**: 2026-11-16 | **Spec**: [`spec.md`](./spec.md)

**Input**: Feature specification from `/specs/001-diecast-collector/spec.md` (172 FRs, 11 user stories, 14 success criteria) e constituição v1.2.0.

## Summary

Aplicativo Android nativo, de instalação local e uso individual, que cataloga miniaturas 1:64 distinguindo **modelo → variante → exemplar**, controla coleção, desejos, duplicados, finanças, localização física, fotos e relatórios — tudo calculado no aparelho, sem servidor. A abordagem técnica é: Kotlin + Jetpack Compose para interface, Room/SQLite como fonte de verdade local com migrações versionadas, ML Kit on-device para código de barras e OCR, importação/exportação por arquivos via Storage Access Framework, PDF gerado com a API de plataforma e backup em pacote ZIP com manifesto de integridade.

A decisão de arquitetura que sustenta o resto: **o banco local é a coleção**. Toda leitura e escrita acontece primeiro no aparelho; nenhuma funcionalidade essencial atravessa a rede, o que elimina a maior fonte de perda de dados e de latência do domínio.

## Technical Context

**Language/Version**: Kotlin 2.1 (JVM target 17), Gradle Kotlin DSL, Android Gradle Plugin 8.x

**Primary Dependencies**: Jetpack Compose + Material3, Navigation Compose, Lifecycle/ViewModel com SavedStateHandle, Room 2.7 (KSP), DataStore Preferences, ML Kit `barcode-scanning` e `text-recognition`, Kotlin Coroutines + Flow, Hilt (injeção de dependência), Coil (carregamento de imagens), WorkManager (operações longas/retomáveis), AndroidX DocumentFile (SAF), `android.graphics.pdf.PdfDocument` (PDF), gerador OOXML próprio (XLSX)

**Storage**: Room/SQLite com `exportSchema = true` e migrações explícitas; imagens no armazenamento privado (`filesDir/images/…`); preferências em DataStore; arquivos de importação/exportação/backup via Storage Access Framework (sem permissões amplas de armazenamento)

**Testing**: JUnit4 + Truth (domínio), Robolectric (ViewModel), Compose UI Test em emulador (fluxos P1), Room `MigrationTestHelper` (ida e reversão), Turbine (Flows), testes de contrato para CSV/XLSX e pacote de backup

**Observabilidade**: logs estruturados no aparelho (nível, timestamp UTC, identificador de correlação por operação, sem dado sensível), retidos localmente e exportáveis no pacote de diagnóstico — requisito do Art. V.7, implementado por T140

**Target Platform**: Android 8.0+ (API 26, `minSdk 26`), `compileSdk`/`targetSdk 36`; orientação retrato e paisagem; telefone como alvo primário

**Project Type**: mobile-app (offline, single-user, sem backend)

**Performance Goals**: abrir listas com 10.000 exemplares em < 1 s; busca/filtro em < 300 ms; abrir detalhe de exemplar em < 200 ms; startup em < 2 s; PDF de 1.000 itens em < 10 s; backup de 1.000 exemplares + 2.000 fotos em < 5 min; rolagem em 60 fps nas listas

**Constraints**: funcionamento integral em modo avião; nenhuma conta ou login; sem telemetria por padrão; fotos comprimidas e sem EXIF de localização; migração destrutiva proibida; acessibilidade 48 dp, TalkBack e fonte do sistema até 200%; distribuição por APK assinado com AAB e requisitos de loja preparados, sem publicar

**Scale/Scope**: 10.000 exemplares, 50.000 fotos, ~22 telas (painel, catálogo, detalhe, cadastro, séries, coleções, desejos, duplicados, finanças, localizações, relatórios, importação, backup, reconhecimento, ajustes, sobre), 1 colecionador por aparelho

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Portão (constituição v1.2.0) | Como o plano atende | Status |
|---|---|---|
| **I.1–I.4** modelo, variante e exemplar distintos | Três tabelas com FKs obrigatórias (`variant.model_id`, `exemplar.variant_id`); IDs UUID gerados no app; nenhuma entidade fundida por conveniência | PASS |
| **I.5** integridade referencial | `PRAGMA foreign_keys = ON` na abertura do banco; FKs declaradas no Room; `onDelete` explícito, nunca `CASCADE` em entidade com histórico | PASS |
| **I.6–I.7** sem perda silenciosa; arquivamento | Exclusão bloqueada quando há dependentes; `archived_at` em entidades com histórico; confirmação com descrição do efeito (FR-028, FR-093) | PASS |
| **I.8** auditoria | Tabela `history_entry` alimentada nas transações de preço, avaliação, condição, localização e vínculo de variante (FR-027) | PASS |
| **I.9** desconhecido ≠ vazio | Campos opcionais `nullable` no schema; nenhum sentinela `0`/`"N/A"`; dinheiro ausente é `NULL`, não zero (FR-015, FR-129) | PASS |
| **I.10** migrações não destrutivas | Room com `exportSchema`, migrações explícitas testadas ida/volta; `fallbackToDestructiveMigration` **proibido** (R.8) | PASS |
| **I.11** dinheiro exato | Valores em `Long` de centavos + código de moeda ISO; nenhum `Double` em preço ou avaliação (FR-076) | PASS |
| **I.12** escrita concorrente | Escritas em transação Room; versão otimista (`updated_at`/`row_version`) em exemplar | PASS |
| **II.1–II.7** modularidade | Módulos Gradle `:core:domain` (Kotlin puro), `:core:data`, `:core:ui`, `:app`; regra de negócio no domínio; dependências unidirecionais; sem ciclo | PASS |
| **III.1–III.11** segurança e privacidade | Sem credenciais nem segredos; armazenamento privado; sem rede por padrão; política de privacidade in-app (FR-172); sem analytics (FR-162) | PASS |
| **IV.1–IV.9** usabilidade e acessibilidade | Strings 100% pt-BR em `strings.xml`; Material3 com tema claro/escuro; TalkBack, 48 dp, fonte dinâmica; formulários com `SavedStateHandle` (FR-023) | PASS |
| **V.1–V.9** qualidade e testes | Pirâmide com JUnit/Truth, Robolectric, Compose UI Test, `MigrationTestHelper`; meta de cobertura no domínio; nenhum teste pulado para liberar entrega | PASS |
| **VI.1–VI.11** evolução e offline | Nenhuma dependência de serviço externo em runtime; fabricante/série/coleção como entidades de primeira classe; UUID + timestamps desde a v1; exportação integral aberta (FR-142) | PASS |
| **R.14** stack nativa | Kotlin + Compose + Room + ML Kit on-device; nenhuma camada cross-platform | PASS |
| **R.15** distribuição | APK assinado na v1; AAB, keystore fora do versionamento e metadados de loja preparados | PASS |
| **R.2–R.3** imagens fora do banco | Fotos em arquivo privado; banco guarda caminho, dimensões, bytes, checksum e ângulo | PASS |
| **R.12** operações longas | Importação, backup, geração de PDF e compressão de fotos em WorkManager/Dispatchers.IO com progresso e retomada | PASS |

**Resultado do portão**: nenhuma violação. `Complexity Tracking` registra apenas duas decisões que precisam de justificativa explícita (módulos Gradle e limitação do reconhecimento visual).

*Ajuste pós-G5 (`/speckit-analyze`, 2026-11-16)*: os achados C1 (log estruturado, Art. V.7), C3 (exclusão/arquivamento, Art. I.6), C4 (união e separação de variantes, Art. I.3/V.3) e C5 (controle de dados do usuário, Art. III.7) foram fechados com as tarefas T140–T144 na Fase 15 do `tasks.md`; C2 (divergência 1 s vs 300 ms) foi corrigido em FR-121 e SC-002.
## Project Structure

### Documentation (this feature)

```text
specs/001-diecast-collector/
├── plan.md              # Este arquivo
├── research.md          # Fase 0 — decisões técnicas com alternativas e trade-offs
├── data-model.md        # Fase 1 — entidades, relacionamentos, índices, DER
├── quickstart.md        # Fase 1 — como construir, testar e validar no aparelho
├── contracts/           # Fase 1 — contratos de arquivo (CSV/XLSX, pacote de backup)
├── checklists/          # checklist de qualidade da spec
├── spec.md              # Especificação funcional
└── tasks.md             # Fase 2 — gerado por /speckit-tasks
```

### Source Code (repository root)

```text
app/                                  # :app — composição, navegação, DI, tema
├── src/main/java/br/com/diecastcollector/
│   ├── DiecastApp.kt               # Application + Hilt
│   ├── MainActivity.kt               # host Compose, edge-to-edge
│   ├── navigation/                   # grafo de rotas tipadas
│   ├── feature/
│   │   ├── dashboard/                # M1 — painel e indicadores
│   │   ├── catalog/                  # M2, M3 — lista, detalhe, cadastro modelo/variante/exemplar
│   │   ├── organization/             # M4 — séries, coleções, progresso, faltantes
│   │   ├── wishlist/                 # M5 — desejos e conversão em aquisição
│   │   ├── duplicates/               # M6 — duplicados e negociação
│   │   ├── finance/                  # M7 — aquisições, avaliações, custos
│   │   ├── locations/                # M8 — hierarquia física e movimentações
│   │   ├── recognition/              # M9, M10 — foto, OCR, código de barras
│   │   ├── search/                   # M11 — busca global e filtros
│   │   ├── reports/                  # M12 — geração e compartilhamento
│   │   ├── transfer/                 # importação, exportação, backup e restauração
│   │   └── settings/                 # preferências, privacidade, sobre
│   └── di/                           # módulos Hilt
├── src/main/res/                     # strings pt-BR, temas, ícones
├── src/test/                         # Robolectric (ViewModels)
└── src/androidTest/                  # Compose UI Test dos fluxos P1 e P2

core/domain/                          # :core:domain — Kotlin puro, sem Android
└── src/main/kotlin/.../domain/
    ├── model/                        # CarModel, Variant, Exemplar, Collection, …
    ├── repository/                   # interfaces de repositório
    ├── usecase/                      # regras: duplicidade, progresso, custos, conversão de desejo
    └── validation/                   # validação de entradas e campos desconhecidos

core/data/                            # :core:data — Room, arquivos, ML Kit, import/export
└── src/main/kotlin/.../data/
    ├── db/                           # entidades, DAOs, migrações, FTS
    ├── mapper/                       # entidade ↔ domínio
    ├── repository/                   # implementações
    ├── storage/                      # imagens, SAF, pacote de backup, checksums
    ├── importexport/                 # leitores CSV/XLSX, escritores CSV/XLSX, PDF
    └── recognition/                  # ML Kit barcode/OCR + RecognitionProvider

core/ui/                              # :core:ui — design system compartilhado
└── src/main/kotlin/.../ui/
    ├── theme/                        # cores, tipografia, formas
    ├── components/                   # card de miniatura, badges de condição, chips de filtro
    └── format/                       # formatação de moeda, data e rótulos pt-BR

core/data/schemas/                    # schema JSON exportado pelo Room (versionado)
```

**Structure Decision**: quatro módulos Gradle (`:app`, `:core:domain`, `:core:data`, `:core:ui`) com as features organizadas como pacotes dentro de `:app`. O domínio fica isolado de Android para permitir teste unitário puro (Art. II.6) e para que uma eventual sincronização futura não exija reescrever regra de negócio. Cada pacote de feature pode ser promovido a módulo próprio sem mover código de domínio, o que atende ao Art. II.3 sem pagar hoje o custo de build de ~14 módulos.

## Complexity Tracking

| Decisão | Por que é necessária | Alternativa mais simples rejeitada porque |
|---|---|---|
| Quatro módulos Gradle em vez de um único módulo | Art. II.1–II.3 exige fronteira explícita entre domínio e infraestrutura; o domínio precisa ser testável sem emulador | Módulo único com pacotes permitiria dependências invertidas (UI acessando DAO), o que já ocorreu em apps deste porte e viola a proibição de SQL fora da camada de dados (Art. II.4) |
| Reconhecimento visual de modelo tratado como **sugestão limitada** na v1 | ML Kit entrega código de barras e OCR de forma confiável, mas não identifica modelo de veículo; a spec exige sugestão com confiança e proíbe inventar dado (FR-105, VI.4) | Prometer identificação visual por foto na v1 exigiria treinar e manter um classificador próprio sem dados rotulados; a alternativa honesta é abstrair `RecognitionProvider`, entregar OCR + código de barras + sugestão por texto e registrar a limitação |

## Riscos e mitigações

| Risco | Impacto | Mitigação no plano |
|---|---|---|
| Reconhecimento visual de modelo não atende FR-102 no nível "reconhecer o carro pela foto" | Alto (expectativa do usuário) | Abstração de provedor; na v1 a foto alimenta OCR/rótulos e comparação textual; limitação declarada no app e no quickstart; classificador próprio vira fase posterior |
| FTS4 e tolerância a acentos dependem do tokenizer `unicode61` com `remove_diacritics=1` | Médio (busca "skyline r34" falhar) | Tabela FTS criada em migração explícita com opções do tokenizer; teste de contrato com acentos e caixa variados |
| Backup com 50.000 fotos pode estourar tempo e espaço | Médio | Pacote em streaming, compactação por arquivo, checksum incremental, progresso e retomada; aviso de espaço antes de iniciar |
| PDF/XLSX gerados à mão (sem biblioteca pesada) têm risco de formato inválido | Médio | Teste de contrato abrindo o arquivo gerado com biblioteca de leitura independente na suíte de testes |
| Room + 22 telas em módulo único de app pode crescer demais | Baixo | Fronteiras por pacote de feature; promoção a módulo prevista sem mover domínio |
| Android Auto Backup tem limite de 25 MB e pode incluir fotos | Médio | Regras de `dataExtractionRules` incluindo banco e preferências, excluindo imagens; pacote manual é o backup completo (R.6) |

## Roadmap por fases (alinhado ao prompt mestre)

| Fase | Escopo | User stories | Portão de saída |
|---|---|---|---|
| **1 — Fundação** | Projeto Gradle, módulos, tema, Room v1 com modelo/variante/exemplar, navegação, painel básico | US1, US2 (parte), US3 | Migração v1 testada; cadastro e painel funcionando offline |
| **2 — Catálogo** | Cadastro completo, fotos, busca FTS, filtros, visualizações | US2, US4 | SC-001, SC-002, SC-011 verificados |
| **3 — Colecionador** | Séries, coleções, progresso, desejos, duplicados, localização | US5, US6, US7, US9 | SC-005 verificado (variante ≠ duplicata) |
| **4 — Gestão** | Aquisições, avaliações, finanças, negociação, relatórios | US8, US7 (conclusão), US10 (relatórios) | SC-007, SC-013, SC-014 verificados |
| **5 — Inteligência** | OCR, código de barras, sugestões e fluxo de aceite | US11 | SC-010 verificado (nada gravado sem confirmação) |
| **6 — Consolidação** | Importação/exportação, backup/restauração, acessibilidade, desempenho, AAB | US10, transversais | SC-003, SC-004, SC-006, SC-008, SC-009, SC-012 verificados |

## Artefatos gerados por esta fase

- `plan.md` (este documento)
- `research.md` — decisões com alternativas, critérios e consequências (Art. R.4)
- `data-model.md` — entidades, campos, relacionamentos, índices, DER e regras de integridade
- `contracts/import-format.md` — contrato de importação CSV/XLSX
- `contracts/backup-package.md` — contrato do pacote de backup e exportação integral
- `quickstart.md` — construção, testes e cenários de validação no aparelho

**Reavaliação do Constitution Check após a Fase 1 de design**: nenhuma violação introduzida. O modelo de dados mantém as três entidades separadas com FKs obrigatórias (Art. I.1–I.4), dinheiro em centavos (I.11), fotos fora do banco (R.2) e nenhuma tabela nova exige migração destrutiva (I.10).

**Próximo passo**: `/speckit-tasks` para decompor em tarefas executáveis por fase, e `/speckit-analyze` para o portão G5.
