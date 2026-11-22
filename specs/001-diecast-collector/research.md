# Phase 0 — Research: Diecast Collector

**Data**: 2026-11-16 · **Spec**: [`spec.md`](./spec.md) · **Plano**: [`plan.md`](./plan.md)

Nenhum item do Technical Context ficou como `NEEDS CLARIFICATION`: as duas decisões que estavam abertas (stack e distribuição) foram fechadas em `/speckit-clarify` e emendadas na constituição (v1.2.0, R.14/R.15). O que segue são as decisões de arquitetura com alternativas, critérios, trade-offs e consequências, no formato exigido pelo Art. R.4.

---

## D1 — Stack nativa Android

- **Decisão**: Kotlin 2.1 + Jetpack Compose (Material3), JVM 17, `minSdk 26`, `targetSdk 36`.
- **Justificativa**: a constituição fixa a stack (R.14, R.16). Tecnicamente, o app precisa de ML Kit on-device, ciclo de vida controlado, Room com migrações testáveis e desempenho em listas de 10.000 itens — todos recursos de primeira classe só no nativo.
- **Alternativas consideradas**: Flutter (um código para iOS, mas plugins de ML Kit e ciclo de vida ficam em camada intermediária e o artefato cresce); React Native (ecossistema JS, dependências nativas de terceiros para câmera/ML Kit); TWA (mais rápido de publicar, inviabiliza offline-first robusto e ML Kit).
- **Consequência**: um app só para Android; suporte a iOS exigirá emenda constitucional e nova implementação.

## D2 — Persistência em Room (SQLite)

- **Decisão**: Room sobre SQLite, `exportSchema = true`, migrações explícitas, `fallbackToDestructiveMigration` proibido.
- **Justificativa**: gera schema verificável, integra Flow para UI reativa, permite testar migração com `MigrationTestHelper` e manter FKs ativas — requisito direto do Art. I.5/I.10 e R.8.
- **Alternativas consideradas**: SQLDelight (ótimo em KMP, mas adiciona complexidade sem iOS no escopo); SQLite puro com `SQLiteOpenHelper` (mais controle, sem verificação de schema e com mais código manual de cursor); Realm (banco próprio, migração pouco transparente para dado patrimonial).
- **Consequência**: migrações são código de produção, testadas ida e volta a cada versão.

## D3 — Busca com FTS4 e normalização de acentos

- **Decisão**: tabela virtual FTS4 (`tokenizer=unicode61` com `remove_diacritics=1`) sobre uma visão de busca que concatena nome do modelo, veículo real, série, coleção e código do fabricante.
- **Justificativa**: atende FR-115 (maiúsculas, acentos, ordem parcial) e FR-121 (< 300 ms com 10.000 itens) sem full scan; FTS4 é o que o SQLite do Android suporta de forma confiável.
- **Alternativas consideradas**: `LIKE '%termo%'` (simples, mas não escala e não tolera acento); FTS5 (indisponível em versões antigas do SQLite embarcado); busca em memória com cache (memória cresce com a coleção e não é persistente).
- **Consequência**: a tabela FTS precisa ser mantida por gatilhos ou pela camada de repositório; teste dedicado com acentos e caixa variados.

## D4 — Injeção de dependência com Hilt

- **Decisão**: Hilt com módulos por camada.
- **Justificativa**: verificação em tempo de compilação, integração com ViewModel e WorkManager, sem reflexão em runtime (impacto em startup).
- **Alternativas consideradas**: Koin (mais simples, falhas só em runtime); injeção manual por construtor (viável no domínio, custoso em 22 telas).
- **Consequência**: `@HiltAndroidApp` obrigatório; módulos declarados por camada para não vazar implementação de dados para a UI.

## D5 — Navegação com Navigation Compose e rotas tipadas

- **Decisão**: Navigation Compose com rotas type-safe (serialização de argumentos) e um grafo por feature agregado no `:app`.
- **Justificativa**: back stack previsível, argumentos verificados em compilação, integração com `SavedStateHandle` (FR-023).
- **Alternativas consideradas**: fragments + XML (mais boilerplate em Compose); navegação manual com estado próprio (perde back stack do sistema e deep link).
- **Consequência**: nenhuma tela recebe `Context` de navegação diretamente; navegação exposta por callbacks do ViewModel.

## D6 — Fotos em arquivo privado, não em BLOB

- **Decisão**: imagens em `filesDir/images/<exemplarId>/<uuid>.jpg`; banco guarda caminho relativo, ângulo, ordem, flag de principal, largura, altura, bytes, checksum e data.
- **Justificativa**: Art. R.2 proíbe BLOB no banco principal; arquivos permitem streaming, backup incremental e carregamento eficiente com Coil.
- **Alternativas consideradas**: BLOB (banco cresce para gigabytes, backup e migração inviáveis); MediaStore/galeria pública (EXIF e privacidade piores, depende de permissões amplas).
- **Consequência**: restauração de backup precisa reescrever caminhos; exclusão de exemplar precisa remover o diretório de imagens em transação compensatória.

## D7 — Importação/exportação via Storage Access Framework

- **Decisão**: `ACTION_OPEN_DOCUMENT` para ler CSV/XLSX/backup e `ACTION_CREATE_DOCUMENT` para escrever; nenhuma permissão de armazenamento ampla.
- **Justificativa**: atende R.9 (permissões mínimas) e FR-130 (compartilhar via app externo), funcionando em qualquer provedor de arquivos do aparelho.
- **Alternativas consideradas**: `WRITE_EXTERNAL_STORAGE` e pasta fixa (obsoleto em versões recentes, ruim para privacidade); salvar sempre em cache interno e exigir compartilhamento (impede o usuário de escolher destino).
- **Consequência**: todo acesso a arquivo é por `Uri`, com tratamento de provedor indisponível e permissão revogada.

## D8 — PDF com a API de plataforma

- **Decisão**: `android.graphics.pdf.PdfDocument` com paginação manual (cabeçalho, tabela, totais, número de página).
- **Justificativa**: zero dependências, funciona offline, suficiente para relatórios tabulares (FR-126, FR-127); bibliotecas de PDF trazem licença e tamanho.
- **Alternativas consideradas**: iText (licença AGPL/comercial); PdfBox (grande, lento no Android); imprimir para PDF pelo framework de impressão (menos controle de layout e sem geração direta de arquivo).
- **Consequência**: código de layout e quebra de página é nosso; teste de contrato deve abrir o PDF gerado e conferir número de páginas e texto.

## D9 — XLSX gerado como OOXML próprio

- **Decisão**: escrever `.xlsx` como ZIP de XML (workbook, planilha, estilos mínimos, cabeçalho congelado) usando `ZipOutputStream`.
- **Justificativa**: Apache POI no Android é pesado e problemático; o subconjunto necessário (uma planilha por relatório) é pequeno e testável.
- **Alternativas consideradas**: Apache POI e portas Android (tamanho de APK, dependências Java não suportadas); CSV com extensão `.xls` (arquivo inválido, abre com aviso e perde acentuação); bibliotecas comerciais (custo).
- **Consequência**: teste de contrato abre o arquivo com uma biblioteca de leitura independente; formatação limitada a texto, número e data.

## D10 — Reconhecimento on-device com limitação declarada

- **Decisão**: ML Kit `barcode-scanning` e `text-recognition` no aparelho; sugestão de modelo por OCR/rótulos e comparação textual com o catálogo local; `RecognitionProvider` como interface substituível.
- **Justificativa**: atende FR-106 (offline), FR-107 (OCR), FR-109 (código de barras) e R.11 (nenhum envio de foto por padrão); a interface permite trocar de provedor sem tocar no domínio (VI.5).
- **Alternativas consideradas**: API de visão em nuvem (custo por chamada, fotos saem do aparelho, quebra offline); classificador TFLite próprio (exigiria dataset rotulado e treinamento, fora do escopo da v1); OCR caseiro (precisão insuficiente em embalagem).
- **Consequência e limitação**: ML Kit **não identifica modelo de veículo por foto**. A v1 entrega código de barras, OCR e sugestões textuais com confiança, declarando "não identificado" quando inconclusivo (FR-105); identificação visual de modelo fica para fase posterior, registrada como risco no plano.

## D11 — Backup em pacote ZIP com manifesto, mais Auto Backup restrito

- **Decisão**: pacote `.zip` com `manifest.json` + `data/*.json` (exportação integral) + `images/`; SHA-256 por arquivo e hash do manifesto; Android Auto Backup habilitado apenas para banco e preferências.
- **Justificativa**: R.6 exige cópia portátil e backup do sistema; Auto Backup tem cota de ~25 MB, insuficiente para 50.000 fotos, então o pacote manual é o backup completo (SC-004, SC-013).
- **Alternativas consideradas**: apenas Auto Backup (não restaurável em outro app, cota pequena); cópia direta do arquivo SQLite (inconsistente se copiada durante escrita, sem imagens); backup em nuvem próprio (exige backend, proibido por VI.1).
- **Consequência**: restauração valida manifesto e hashes antes de gravar; conflito de IDs é resolvido por decisão do usuário (substituir/manter ambos).

## D12 — Dinheiro em centavos inteiros

- **Decisão**: `Long` de centavos + código ISO da moeda; formatação pt-BR na camada de UI.
- **Justificativa**: Art. I.11 exige exatidão decimal; centavos inteiros eliminam erro de ponto flutuante sem o custo de `BigDecimal` em listas grandes.
- **Alternativas consideradas**: `Double` (perde centavos em somas, inaceitável para patrimônio); `BigDecimal` (exato, mas mais lento e verboso em agregações por série/fabricante).
- **Consequência**: toda conversão de entrada passa por parser único testado com vírgula e ponto.

## D13 — Identificadores UUID em texto, datas separadas por natureza

- **Decisão**: `TEXT` com UUID v4 gerado no app; timestamps de auditoria em epoch millis UTC; datas de aquisição/avaliação como `LocalDate` ISO (`YYYY-MM-DD`).
- **Justificativa**: VI.11 exige IDs gerados no cliente e estáveis após exportar/reimportar; separar data de evento (dia) de timestamp de auditoria evita deslocamento por fuso (edge case do fuso horário na spec).
- **Alternativas consideradas**: autoincremento (colide ao mesclar backups e ao unir variantes); ULID (ordenável, mas sem ganho prático aqui).
- **Consequência**: mais bytes por linha; aceitável em 10.000 registros e indispensável para restauração sem colisão.

## D14 — Operações longas com WorkManager

- **Decisão**: importação, geração de backup, restauração e compressão em lote executam em `WorkManager` (com progresso observável) ou em `Dispatchers.IO` quando estritamente interativas; nada na thread principal.
- **Justificativa**: R.12 e FR-150 exigem sobrevivência a segundo plano e retomada; WorkManager dá estado persistente e política de retry.
- **Alternativas consideradas**: corrotina apenas na tela (morre com o processo); `ForegroundService` manual (mais controle, notificação obrigatória e mais código).
- **Consequência**: resultado de trabalho é lido do banco, não de memória; UI observa progresso por Flow.

## D15 — Testes: JUnit4 + Robolectric + Compose UI Test + MigrationTestHelper

- **Decisão**: JUnit4 com Truth, Robolectric para ViewModel, Compose UI Test em emulador para fluxos P1/P2, `MigrationTestHelper` para cada migração, Turbine para Flows, testes de contrato para arquivos gerados.
- **Justificativa**: Art. V.2 exige integração com banco real e interface instrumentada; é o conjunto com melhor suporte oficial no ecossistema Android.
- **Alternativas consideradas**: JUnit5 (suporte Android ainda por plugin de terceiros); apenas instrumentados (lento, roda só em emulador); mocks de banco (proibido por V.2).
- **Consequência**: suíte unitária rápida no JVM e suíte instrumentada obrigatória antes de cada fase.

## D16 — Versão mínima Android 8.0 (API 26)

- **Decisão**: `minSdk 26`, `compileSdk`/`targetSdk 36`.
- **Justificativa**: cobre praticamente todo o parque relevante, permite `java.time` sem desugaring completo, ML Kit e APIs modernas de arquivo; simplifica o código de data e permissões.
- **Alternativas consideradas**: `minSdk 24` (mais alcance, mais desugaring e caminhos alternativos); `minSdk 30` (menos aparelhos antigos, sem ganho funcional).
- **Consequência**: aparelhos muito antigos ficam fora; declarado no "Sobre" e na política de privacidade.

---

## Itens deliberadamente adiados

| Item | Motivo | Quando decidir |
|---|---|---|
| Classificador visual de modelo (TFLite) | Exige dataset rotulado e treinamento; ML Kit não cobre | Fase 6 ou posterior, com emenda se alterar R.11 |
| Sincronização entre aparelhos | Fora do escopo v1 (VI.10, A13); UUID e timestamps já preparam o caminho | Após v1, com ADR de resolução de conflito |
| Catálogo de referência mundial compartilhado | Fora do escopo v1 (A3); modelo já aceita coleções e séries personalizadas | Produto separado, com curadoria |
