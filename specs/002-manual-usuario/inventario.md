# Inventário de telas (30 destinos)

Fonte: `app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt` (commit `424e4fb`).
Títulos são os textos exibidos; telas sem título próprio usam o identificador do exemplar.

| # | Rota | Título visível | Arquivo | ViewModel | Acesso | Cap. |
|---|---|---|---|---|---|---|
| 1 | `dashboard` | Painel | DashboardScreen.kt | DashboardViewModel | aba Painel | 5 |
| 2 | `catalog` | Coleção | CatalogScreen.kt | CatalogViewModel | aba Catálogo | 6 |
| 3 | `catalog/search` | Busca | SearchScreen.kt | SearchViewModel | ícone «Buscar no acervo» no catálogo | 7 |
| 4 | `catalog/create` | Nova miniatura | CreateCatalogEntryScreen.kt | CreateCatalogEntryViewModel | botão «+ Mini» | 8 |
| 5 | `catalog/variant/{modelId}` | Cadastrar variante | VariantFormScreen.kt | VariantFormViewModel | após «Salvar miniatura»; item de incompletos | 9 |
| 6 | `catalog/incomplete` | Cadastros incompletos | IncompleteEntriesScreen.kt | IncompleteEntriesViewModel | «Incompletos (N)» no catálogo | 10 |
| 7 | `catalog/model/{modelId}` | Modelo e veículo real | ModelFormScreen.kt | ModelFormViewModel | «Editar modelo» nos incompletos | 11 |
| 8 | `catalog/variant/{variantId}/edit` | Editar variante | VariantFormScreen.kt (`VariantEditScreen`) | VariantFormViewModel | «Editar variante» nos incompletos | 12 |
| 9 | `exemplar/add/{variantId}` | Novo exemplar | AddExemplarScreen.kt | AddExemplarViewModel | toque na variante no catálogo; «+ Exemplar» | 13 |
| 10 | `catalog/variant/{variantId}/exemplars` | Exemplares | ExemplarListScreen.kt | ExemplarListViewModel | «Ver exemplares» no catálogo | 14 |
| 11 | `exemplar/{exemplarId}` | código interno (ex.: HW-000001) | ExemplarDetailScreen.kt | ExemplarDetailViewModel | item da lista de exemplares | 15 |
| 12 | `catalog/collections` | Coleções | CollectionsScreen.kt | CollectionsViewModel | «Coleções» no catálogo | 16 |
| 13 | `collections/{collectionId}` | nome da coleção | CollectionDetailScreen.kt | CollectionDetailViewModel | item da lista de coleções | 17 |
| 14 | `collections/form` | Nova coleção / Editar coleção | CollectionFormScreen.kt | CollectionFormViewModel | «+ Coleção»; «Editar» no detalhe | 18 |
| 15 | `catalog/series` | Séries | SeriesScreen.kt | SeriesViewModel | «Séries» em Coleções | 19 |
| 16 | `catalog/barcode` | Código de barras | BarcodeScanScreen.kt | RecognitionViewModel | «Código de barras» no catálogo | 20 |
| 17 | `duplicates` | Duplicados | DuplicatesScreen.kt | DuplicatesViewModel | «Duplicados» no catálogo | 21 |
| 18 | `duplicates/{exemplarId}/negotiation` | Negociação | NegotiationScreen.kt | NegotiationViewModel | «Negociar» em Duplicados, na lista de exemplares ou na ficha do exemplar | 22 |
| 19 | `wishlist` | Lista de desejos | WishlistScreen.kt | WishlistViewModel | aba Desejos | 23 |
| 20 | `wishlist/form` | Novo desejo / Editar desejo | WishlistFormScreen.kt | WishlistFormViewModel | «Novo desejo»; item da lista | 24 |
| 21 | `finance` | Financeiro | FinanceScreen.kt | FinanceViewModel | aba Financeiro | 25 |
| 22 | `locations` | Localizações | LocationsScreen.kt | LocationsViewModel | Ajustes → Localizações | 26 |
| 23 | `locations/{locationId}` | Inventário | LocationContentsScreen.kt | LocationContentsViewModel | item da árvore de localizações | 27 |
| 24 | `settings` | Ajustes | SettingsScreen.kt | SettingsViewModel | aba Ajustes | 28 |
| 25 | `settings/backup` | Backup e restauração | BackupScreen.kt | BackupViewModel | Ajustes → Backup e restauração | 29 |
| 26 | `settings/reports` | Relatórios | ReportsScreen.kt | ReportsViewModel | Ajustes → Relatórios | 30 |
| 27 | `settings/import` | Importar | ImportScreen.kt | ImportViewModel | Ajustes → Importar planilha | 31 |
| 28 | `settings/data` | Dados da coleção | DataControlScreen.kt | DataControlViewModel | Ajustes → Dados da coleção | 32 |
| 29 | `settings/about` | Sobre | AboutScreen.kt | — (BuildConfig) | Ajustes → Sobre o app | 33 |
| 30 | `settings/privacy` | Privacidade | PrivacyPolicyScreen.kt | — (texto fixo) | Ajustes → Política de privacidade | 34 |

## Estados por tela (resumo)

| Tela | Vazio | Carregando | Erro | Sucesso |
|---|---|---|---|---|
| Painel | «Coleção vazia» | indicador circular | «Não foi possível ler a coleção» | cartões de indicadores |
| Catálogo | «Nenhuma miniatura ainda» | lista | erro de leitura | lista de variantes |
| Busca | «Nenhum exemplar encontrado.» | leitura | — | grade/lista/galeria |
| Incompletos | «Nenhum cadastro incompleto…» | — | — | lista |
| Exemplares | «Nenhum exemplar ativo» | — | — | lista |
| Detalhe do exemplar | «Exemplar não encontrado…» | — | — | seções |
| Coleções | «Nenhuma coleção ainda» | «Carregando coleções…» | — | lista com % |
| Séries | «Nenhuma série cadastrada…» | «Carregando coleções…» | — | árvore |
| Duplicados | «Nenhum duplicado» | «Carregando duplicados…» | — | grupos |
| Negociação | «Nenhuma negociação registrada…» | «Carregando negociação…» | — | ativa/histórico |
| Desejos | «Nada na lista de desejos» | «Carregando desejos…» | — | cartões |
| Financeiro | «Nenhuma venda ou troca concluída.» | — | — | cartões |
| Localizações | «Nenhuma localização ainda» | «Carregando localizações…» | — | árvore |
| Inventário | «Nenhum exemplar nesta localização.» | «Carregando inventário…» | — | lista |
| Backup | — | «Trabalhando…» | mensagens específicas | «Backup criado» |
| Relatórios | — | «Gerando relatório…» | «Não foi possível gerar o relatório» | «Relatório gerado» |
| Importação | — | «Lendo o arquivo…» | «Não foi possível importar» | «Importação concluída» |
| Dados da coleção | «Não há dados no aparelho.» | «Trabalhando…» | «Não foi possível concluir» | exportação/exclusão |
