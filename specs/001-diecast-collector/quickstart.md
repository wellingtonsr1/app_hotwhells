# Quickstart — construir, testar e validar no aparelho

**Plano**: [`plan.md`](./plan.md) · **Modelo**: [`data-model.md`](./data-model.md) · **Contratos**: [`contracts/`](./contracts/)

> Este guia descreve **como validar**, não como implementar. Os comandos pressupõem o esqueleto Gradle da Fase 1 (ainda não criado — a implementação começa após `/speckit-tasks` e `/speckit-analyze`).

## Pré-requisitos

- JDK 17 (o Android Studio embarca um).
- Android Studio com SDK Platform 36 e Build Tools correspondentes.
- `ANDROID_HOME` configurado e `platform-tools` no PATH.
- Aparelho ou emulador com Android 8.0+ (API 26+).

## Construir e instalar

```bash
./gradlew :app:assembleDebug          # APK de desenvolvimento
./gradlew :app:installDebug           # instala no aparelho conectado
./gradlew :app:assembleRelease        # APK assinado (keystore fora do versionamento)
./gradlew :app:bundleRelease          # AAB pronto para publicação futura
```

## Testes

```bash
./gradlew test                        # unitários: domínio, ViewModel (Robolectric), contratos de arquivo
./gradlew :core:data:testDebugUnitTest # migrações Room (MigrationTestHelper) e DAOs
./gradlew connectedAndroidTest        # Compose UI Test dos fluxos P1/P2 em emulador
./gradlew lint                        # lint Android + checagem de strings pt-BR
```

Regra de aceite: `test` e `connectedAndroidTest` verdes antes de fechar qualquer fase (Art. V.1, V.4).

## Roteiro de validação manual (o que cada critério exige)

| # | Cenário | Passos | Resultado esperado | Critério |
|---|---|---|---|---|
| 1 | Offline total | Ativar modo avião; cadastrar modelo, variante e exemplar com foto; buscar; gerar PDF; gerar backup | Nenhuma tela bloqueia, nenhuma mensagem de rede | SC-003, FR-146 |
| 2 | Cadastro rápido | Com variante existente, tocar "Adicionar exemplar" e salvar | Menos de 20 s, sem redigitar dados do modelo | SC-001 |
| 3 | Contagens do painel | Comparar painel com contagem manual de 20 itens | Exemplares e modelos distintos em cartões separados, números corretos | SC-014, FR-001 |
| 4 | Variante ≠ duplicata | Duas variantes do mesmo modelo + dois exemplares de uma delas | 1 modelo, 2 variantes, 2 exemplares; só os dois da mesma variante listados como duplicados | SC-005, FR-039 |
| 5 | Busca tolerante | Pesquisar `skyline r34`, `SKYLINE`, `skyline` | Mesmo item encontrado nos três casos | FR-115 |
| 6 | Volume | Importar planilha de 10.000 exemplares | Lista abre < 1 s; filtro responde < 300 ms | SC-002, FR-121 |
| 7 | Importação segura | Importar CSV com 12 linhas inválidas | Pré-visualização com resumo antes de gravar; relatório com erro por linha | SC-006, FR-136 |
| 8 | PDF local | Gerar inventário em PDF em modo avião | Arquivo abre, tem cabeçalho, data, totais e paginação | SC-007, FR-127 |
| 9 | Backup ida e volta | Gerar pacote → limpar dados do app → restaurar | Fotos vinculadas corretamente; contagens idênticas | SC-004, FR-141 |
| 10 | Interrupção | Preencher formulário, girar a tela, mandar app para segundo plano e matar o processo | Dados recuperados sem perda | SC-008, FR-023 |
| 11 | Acessibilidade | Ativar TalkBack e fonte no máximo | Cadastro, busca e relatório operáveis; nada cortado | SC-012, FR-165 |
| 12 | Sem permissão de câmera | Negar câmera na primeira solicitação | App segue utilizável; reconhecimento indisponível com explicação | FR-108 |
| 13 | Código sem correspondência | Ler código inexistente | Mensagem de ausência; cadastro manual possível; nenhum exemplar criado | FR-112–FR-113 |
| 14 | Armazenamento cheio | Preencher o aparelho e anexar foto | Erro acionável, cadastro preservado sem foto parcial | FR-153 |

## Verificações de conformidade estrutural

Antes de considerar um incremento pronto:

1. `PRAGMA foreign_keys` ativo e FKs presentes no schema exportado (`core/data/schemas/`).
2. Nenhuma migração destrutiva (`fallbackToDestructiveMigration` ausente do código).
3. Todo campo monetário em `Long` de centavos; nenhuma ocorrência de `Double` em preço.
4. Strings de interface apenas em `res/values/strings.xml` (pt-BR), sem texto fixo em Composable.
5. Nenhuma chamada de rede no código de produção.
6. Nenhuma credencial ou chave em repositório; `keystore`/`keystore.properties` no `.gitignore`.
7. Relatórios e painel com os mesmos totais para o mesmo conjunto de dados (FR-133).

## Limitações conhecidas declaradas neste plano

- **Reconhecimento visual de modelo por foto** não é entregue na v1: ML Kit cobre código de barras e OCR. O app declara "não identificado" quando não há correspondência textual confiável (FR-105). Ver `research.md` D10 e `plan.md` (Complexity Tracking).
- **Publicação no Google Play** não acontece na v1; o AAB e os metadados ficam prontos (R.15).
- **Sincronização entre aparelhos** e **catálogo de referência mundial** estão fora do escopo (A3, A13).
