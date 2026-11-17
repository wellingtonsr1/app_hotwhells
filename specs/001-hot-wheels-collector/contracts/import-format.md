# Contrato — Importação de dados (CSV / XLSX)

**Requisitos**: FR-134 a FR-139 · **Modelo**: [`../data-model.md`](../data-model.md)

Este contrato descreve o que o aplicativo aceita ler. É a única interface pela qual dados entram no app sem digitação, então ele é fechado, versionado e testado por contrato.

## Formatos aceitos

| Item | Regra |
|---|---|
| Extensões | `.csv`, `.xlsx` (primeira planilha do arquivo) |
| Codificação | UTF-8 (com ou sem BOM); Latin-1 aceito apenas com aviso na pré-visualização |
| Delimitador CSV | `,` ou `;`, detectado pela primeira linha |
| Cabeçalho | obrigatório, na primeira linha |
| Limite | 10.000 linhas por arquivo; 20 MB |
| Planilha | o app lê a primeira aba; abas adicionais são ignoradas com aviso |

## Colunas

Nomes de cabeçalho em português, sem distinção de caixa, acentos ou espaços extras. Colunas desconhecidas são ignoradas e listadas no relatório como aviso.

| Coluna | Tipo | Obrigatória | Exemplo | Observação |
|---|---|---|---|---|
| `fabricante` | texto | sim | Hot Wheels | cria fabricante se não existir (com confirmação) |
| `marca` | texto | não | Hot Wheels | marca da miniatura |
| `modelo` | texto | sim | Nissan Skyline GT-R R34 | chave de agrupamento com fabricante e escala |
| `veiculo_real` | texto | não | Nissan Skyline GT-R | marca + modelo do veículo real |
| `ano_veiculo_real` | inteiro | não | 1999 | 1900–2100 |
| `escala` | texto | não | 1:64 | padrão `1:64` |
| `serie` | texto | não | Car Culture | criada se não existir (com confirmação) |
| `colecao` | texto | não | JDM Favoritos | `>` indica subcoleção: `Premium > Car Culture` |
| `ano_lancamento` | inteiro | não | 2024 | |
| `numero_colecao` | texto | não | 3/5 | mantido como texto |
| `codigo_fabricante` | texto | não | HKC25 | usado na busca e no código de barras |
| `cor_principal` | texto | não | azul | cria cor se não existir |
| `cores_secundarias` | texto | não | branco; vermelho | separadas por `;` |
| `tipo_carroceria` | texto | não | cupê | |
| `material` | texto | não | zamac | |
| `rodas` | texto | não | 5SP | atributo de variante |
| `base` | texto | não | plástica | atributo de variante |
| `decoracao` | texto | não | racing stripes | atributo de variante |
| `condicao` | enum | não | lacrado | `lacrado`, `solto`, `danificado`, `restaurado`, `incompleto`; padrão `solto` |
| `condicao_embalagem` | enum | não | lacrada | `lacrada`, `aberta`, `danificada`, `sem embalagem` |
| `codigo_interno` | texto | não | HW-0042 | se presente e já existir, conta como duplicata |
| `data_aquisicao` | data | não | 2024-05-18 | ISO `YYYY-MM-DD` ou `dd/MM/yyyy` |
| `preco` | decimal | não | 19,90 | vírgula ou ponto; vazio = não informado |
| `frete` | decimal | não | 8,00 | |
| `desconto` | decimal | não | 2,00 | |
| `moeda` | texto | não | BRL | padrão BRL |
| `loja` | texto | não | Ri Happy | |
| `vendedor` | texto | não | — | |
| `localizacao` | texto | não | Armário A > Estante 2 > Caixa 12 | `>` cria hierarquia |
| `observacoes` | texto | não | etiqueta amassada | ≤ 2.000 caracteres |

## Fases da importação (obrigatórias, nesta ordem)

1. **Leitura**: abre o arquivo via SAF, detecta formato, codificação e delimitador.
2. **Parse**: converte cada linha em um registro tipado; nunca grava.
3. **Validação**: aplica as regras de `data-model.md` (seção "Regras de validação").
4. **Duplicidade**: compara com o banco — modelo por (`fabricante`, `modelo`, `escala`); variante por impressão digital dos atributos; exemplar por `codigo_interno`.
5. **Pré-visualização**: mostra total de linhas, válidas, inválidas, duplicatas, entidades que serão criadas (fabricantes, séries, coleções, cores, localizações) e as 20 primeiras linhas.
6. **Confirmação**: exige ação explícita; por duplicata o usuário escolhe **pular**, **criar mesmo assim** ou **mesclar**.
7. **Aplicação**: transação única para o lote; erro inesperado reverte tudo e informa a linha.

## Relatório de erros

CSV separado, com cabeçalho `linha,campo,codigo,mensagem,valor_informado`.

| Código | Significado | Ação sugerida ao usuário |
|---|---|---|
| `E001` | campo obrigatório vazio | preencher `fabricante` e `modelo` |
| `E002` | número inválido | usar vírgula ou ponto decimal, sem símbolo de moeda |
| `E003` | data inválida | usar `AAAA-MM-DD` ou `DD/MM/AAAA` |
| `E004` | valor fora do domínio (enum) | usar um dos valores listados |
| `E005` | duplicata de variante | escolher pular, criar ou mesclar |
| `E006` | texto acima do limite | reduzir para 120/2.000 caracteres |
| `E007` | hierarquia de localização inválida | usar `>` entre níveis, sem nível vazio |
| `E008` | valor monetário negativo | corrigir o sinal |

## Garantias

- Nenhuma linha é gravada antes da confirmação na pré-visualização (FR-136).
- Arquivo malformado, vazio ou com cabeçalho ausente é rejeitado com mensagem acionável, sem gravação parcial (edge case da spec).
- Campos desconhecidos entram como `NULL`, nunca como zero ou texto vazio (Art. I.9).
- A mesma importação executada duas vezes com `codigo_interno` presente não duplica exemplares (FR-138).
