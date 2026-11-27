# Inventário de regras confirmadas no código

Commit `424e4fb`. Cada regra é rastreável ao arquivo citado; nenhuma foi inferida por nome.

## Catálogo e validação (`core/domain/.../validation/CatalogValidation.kt`)

| Regra | Mensagem exibida |
|---|---|
| Escala `1:N`, N de 1 a 999 | «Escala deve estar no formato 1:N, como 1:64.» |
| Nome obrigatório não vazio, ≤ 120 caracteres | «Informe o …» / «O … excede 120 caracteres.» |
| Ano entre 1886 e ano atual + 2, ou nulo | «Ano deve estar entre 1886 e <ano+2>.» |
| Prioridade do desejo 1–3 | «Prioridade deve estar entre 1 (alta) e 3 (baixa).» |
| Texto opcional vazio/`N/A`/`NA`/`null`/`none`/«não informado» | vira desconhecido (`NULL`) |
| Código interno do exemplar obrigatório e único | bloqueia o cadastro |
| Foto exige caminho e checksum | falha de gravação |
| `rowVersion` ≥ 0 | detecção de escrita concorrente |

## Duplicidade (`core/domain/.../usecase/DuplicateUseCases.kt`, `DuplicateModels`)

- Grupo = mesma **variante** (`variantId`), nunca pelo nome do modelo.
- Disponibilidade por exemplar: `TRADE`, `SALE`, `BOTH`, `UNAVAILABLE`; «Em negociação» quando existe negociação ativa.
- Dois exemplares da mesma variante contam como duplicados; duas variantes do mesmo modelo, não.
- Duplicidade não é pré-condição para disponibilizar nem negociar: a oferta e a negociação valem para qualquer exemplar `OWNED`. Quando a variante tem **um único** exemplar, o aviso «Último exemplar da variante» aparece antes de gravar a disponibilidade e antes de abrir a negociação, e não aparece com negociação ativa (`UniqueExemplarPolicy`).

## Progresso de coleção (`core/domain/.../model/CollectionProgress.kt`)

- `units` = 0 se não colecionável; senão `expected`.
- `ownedUnits` = `min(ownedExemplars, expected)`; `missingUnits` = `max(units − ownedUnits, 0)`.
- Percentual = unidades atendidas ÷ unidades esperadas; item não colecionável não conta.

## Financeiro (`core/data/.../db/dao/FinanceDao.kt`)

- Investido = Σ(preço + frete − desconto) das aquisições de exemplares `OWNED`; sem preço, 0.
- Estimado = Σ da **avaliação mais recente** de cada exemplar `OWNED` (data, depois criação).
- Diferença = estimado − investido; custo médio = investido ÷ exemplares com valor.
- Vendas/trocas concluídas ficam fora do investido; gastos por fabricante/série/coleção/mês usam a mesma fórmula.
- Valores em centavos (`MoneyCents`), moeda BRL.

## Backup e restauração (`feature/transfer`, `core/domain/backup`)

- Pacote: banco, fotos, manifesto com hash e cópias CSV/JSON.
- Restauração valida: pacote reconhecível, formato suportado, arquivo presente, tamanho, checksum, contagens, banco válido, esquema não mais novo, chaves estrangeiras.
- Restaurar **substitui** os dados (com cópia do estado anterior) ou **mescla** sem sobrescrever; o app precisa ser reaberto.
- Backup automático do Android não leva fotos.

## Importação CSV (`core/data/.../importexport/CsvImportPipeline.kt`)

- Colunas conhecidas: fabricante, marca, modelo, veiculo_real, ano_veiculo_real, escala, serie, colecao, ano_lancamento, numero_colecao, codigo_fabricante, cor_principal, cores_secundarias, tipo_carroceria, material, rodas, base, decoracao, condicao, condicao_embalagem, codigo_interno, data_aquisicao, preco, frete, desconto, moeda, loja, vendedor, localizacao, observacoes.
- Obrigatórios: fabricante e modelo; condição aceita lacrado/solto/danificado/restaurado/incompleto.
- Pré-visualização mostra total, válidas, inválidas e duplicatas antes de gravar; erros podem ser salvos em CSV.

## Relatórios (`core/data/.../importexport/ReportWriters.kt`)

- Formato: `PDF`, `CSV`, `XLSX`; conteúdo = inventário de exemplares.
- PDF informa contagem de exemplares e páginas; CSV/XLSX não têm páginas.

## Reconhecimento (`feature/recognition/BarcodeScanScreen.kt`)

- Leitura por câmera ou código digitado; resultado é sugestão com confiança.
- «Não identificado» não preenche campo algum; correspondência no catálogo permite «Adicionar exemplar desta variante».
- Cadastro manual leva o código lido como código do fabricante.
