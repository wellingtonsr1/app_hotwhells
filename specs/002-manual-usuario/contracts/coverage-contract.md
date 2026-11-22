# Contract — Matriz de cobertura

**Feature**: `002-manual-usuario` | **Aplica-se a**: `docs/manual/MATRIZ_COBERTURA.md`
| **Requisitos**: FR-021, FR-022, SC-001, SC-002, SC-005

A matriz é a prova de que nenhuma tela ou funcionalidade implementada ficou de fora. Ela é
derivada de `AppNavHost.kt` (lista fechada de destinos) e da inspeção das telas
(elementos e funcionalidades), nunca da memória de quem escreveu.

## 1. Colunas obrigatórias

| Coluna | Conteúdo | Exemplo |
|---|---|---|
| **#** | número sequencial | 12 |
| **Destino / funcionalidade** | nome visível + rota estável | Detalhe do exemplar (`exemplar/{exemplarId}`) |
| **Tipo** | `destino`, `subestado`, `funcionalidade`, `entidade`, `entregável` | `destino` |
| **Capítulo / seção** | link âncora para o manual | `[7.2](#72-detalhe-do-exemplar)` |
| **Elementos documentados** | `documentados / total` contados na tabela de elementos | `18/18` |
| **Evidência** | caminho da captura ou `lac-XX` justificado | `capturas/05-exemplar-detalhe.png` |
| **Status** | `DOCUMENTADO`, `PARCIAL`, `AUSENTE` | `DOCUMENTADO` |
| **Observação** | motivo obrigatório quando status ≠ `DOCUMENTADO` | — |

## 2. Regras de preenchimento

1. **Zero órfãos**: todo `data object` que estende `Destination` tem uma linha; todo item de
   Ajustes (Backup, Relatórios, Importação, Dados da coleção, Sobre, Privacidade) também.
2. **Nenhuma linha sem vínculo**: `Capítulo / seção` nunca fica vazio; se a seção ainda não
   existe, o status é `AUSENTE` e a observação diz o que falta.
3. **Status `DOCUMENTADO`** exige: seção existente, tabela de elementos com 100% dos
   interativos, ao menos uma evidência ou lacuna justificada, mensagens e estados tratados.
4. **Status `PARCIAL`** exige observação nomeando exatamente o que falta (elemento, estado,
   captura, regra) — não serve como “quase pronto”.
5. **Status `AUSENTE`** exige motivo e prazo/condição de resolução; entra também em
   `LIMITACOES_DOCUMENTACAO.md`.
6. **Funcionalidades transversais** (backup, importação CSV, relatórios PDF/CSV/XLSX,
   código de barras, duplicados, negociação, financeiro) têm linha própria além das telas.
7. **Entidades** de domínio têm linha quando exigidas por FR-003 (fabricante, modelo,
   variante, exemplar, série, cor, coleção, localização, aquisição, avaliação, fotografia).
8. A contagem de elementos da coluna 5 precisa bater com as linhas da tabela de elementos do
   capítulo; divergência é falha de cobertura.

## 3. Conjunto mínimo de linhas

- 30 linhas de `destino` (uma por `Destination`).
- 1 linha de `subestado` para cada diálogo/sheet/estado com texto próprio relevante
  (ex.: confirmação de exclusão, restauração de backup, pré-visualização de importação).
- 1 linha por funcionalidade transversal listada em FR-002/FR-014.
- 11 linhas de `entidade` (FR-003).
- 6 linhas de `entregável` (FR-020): manual MD, manual PDF, índice, matriz, limitações,
  pasta de capturas.

## 4. Verificação automatizada

A conferência roda antes da entrega e o resultado é registrado no próprio documento.

```bash
# 1. Destinos declarados no código
grep -oP 'data object \K\w+' app/src/main/java/br/com/diecastcollector/navigation/AppNavHost.kt \
  | sort > /tmp/destinos-codigo.txt

# 2. Destinos citados na matriz (coluna Destino/funcionalidade)
grep -oP '`[a-z][^`]*`' docs/manual/MATRIZ_COBERTURA.md \
  | tr -d '`' | sort -u > /tmp/destinos-matriz.txt

# 3. Diferença precisa ser vazia nos dois sentidos
comm -23 /tmp/destinos-codigo.txt /tmp/destinos-matriz.txt   # sem linha na matriz
```

- A verificação de rotas compara o conjunto de `Destination` com as rotas citadas na matriz.
- A verificação de elementos compara `documentados / total`; qualquer `total` menor que a
  contagem da tabela do capítulo é falha.
- A verificação de imagens extrai caminhos `capturas/...` dos documentos e testa existência;
  caminho quebrado ou captura ausente sem `lac-XX` é falha.
- Resultado esperado: **zero** destinos sem vínculo, **zero** elementos sem linha,
  **zero** referências de imagem quebradas (SC-001/SC-002/SC-005).

## 5. Critérios de aceite da matriz

- [ ] Cabeçalho com versão, versionCode, commit e data da conferência.
- [ ] 30 destinos presentes, sem duplicata e sem rota inventada.
- [ ] Nenhuma célula de vínculo vazia.
- [ ] `DOCUMENTADO` sem observação contraditória; `PARCIAL`/`AUSENTE` com motivo.
- [ ] Contagens de elementos conferem com os capítulos.
- [ ] Resultado da verificação automatizada anexado com data e saída.
