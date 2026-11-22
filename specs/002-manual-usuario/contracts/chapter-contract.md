# Contract — Capítulo do manual

**Feature**: `002-manual-usuario` | **Aplica-se a**: cada capítulo de tela em
`docs/manual/MANUAL_USUARIO_COMPLETO.md` | **Requisitos**: FR-007 a FR-013, FR-017, FR-018

Um capítulo é a unidade de documentação de **uma tela** (ou de uma tela e seus subestados
imediatos). O contrato é obrigatório: capítulo que não o cumpre não pode ser marcado como
concluído na matriz de cobertura.

## 1. Estrutura obrigatória (10 seções, nesta ordem)

| # | Seção (título literal) | Conteúdo mínimo |
|---|---|---|
| 1 | **Finalidade** | uma frase sobre o problema que a tela resolve e o que o usuário consegue fazer nela |
| 2 | **Como acessar** | caminho completo desde a abertura (aba → botão → item), com os rótulos visíveis |
| 3 | **Identificação visual** | título exibido, ícone da aba, posição na navegação, captura de referência |
| 4 | **Elementos da tela** | tabela de elementos (seção 3 deste contrato), cobrindo todos os interativos |
| 5 | **Como utilizar** | procedimento numerado do uso típico, citando os rótulos da tabela |
| 6 | **Exemplos** | pelo menos um exemplo concreto com dados fictícios; o exemplo Skyline usa os mesmos valores em todas as telas |
| 7 | **Regras e validações** | campos obrigatórios, limites, normalização, duplicidade e o que bloqueia a ação |
| 8 | **Mensagens e situações especiais** | tabela de mensagens (seção 4) + estados vazio, carregando, erro e sucesso |
| 9 | **Dicas** | atalhos e boas práticas reais, sem recurso inexistente |
| 10 | **Problemas comuns** | sintoma → causa provável → o que fazer no app |

Telas sem estado vazio/erro próprio declaram “Esta tela não tem estado vazio próprio” em vez
de omitir a seção (FR-010).

## 2. Cabeçalho do capítulo

```markdown
## <N>. <Título visível da tela>

> **Tela**: `<rota>` · **Arquivo**: `feature/<área>/<Nome>Screen.kt` · **Acesso**: <origem>
> **Capturas**: `capturas/NN-nome.png` (ou “sem captura — ver lac-XX”)
```

- Numeração `N` segue a ordem editorial do manual (primeiros passos → referência → ajustes).
- A rota e o arquivo são rastreabilidade interna; o leitor não precisa deles, mas a revisão
  técnica usa para conferir fidelidade.
- A seção de primeiro nível usa `##`; subseções do padrão de 10 itens usam `###`.

## 3. Tabela de elementos (obrigatória)

| Elemento | Tipo | Onde fica | O que faz | Pré-condição | Resultado |
|---|---|---|---|---|---|
| **Salvar** | botão | barra superior, à direita | grava o cadastro | campos obrigatórios preenchidos | volta à tela anterior e mostra “...” |
| **Cor principal** | campo/seletor | corpo, após “Série” | define a cor que identifica a variante | — | passa a compor o nome exibido da variante |

Regras:
- Coluna **Elemento** traz o rótulo literal em negrito; ícone sem texto é descrito entre
  parênteses: `**(ícone de lupa)**`.
- Todos os itens interativos da tela aparecem; campos somente-leitura e indicadores entram
  quando o usuário precisa interpretá-los.
- Elemento sem `contentDescription` recebe nota: “ícone sem rótulo acessível — identificado
  pela posição; ver `lac-XX`”.
- Nenhuma linha “a definir” (SC-002).

## 4. Tabela de mensagens

| Mensagem (texto exibido) | Quando aparece | O que fazer |
|---|---|---|
| “Informe o fabricante.” | nome em branco ao salvar | preencher o campo e salvar de novo |

- Texto literal de `strings.xml`, com placeholders resolvidos em um exemplo.
- Mensagens de confirmação e de erro entram; mensagens genéricas do sistema Android não.
- Mensagem sem string própria (exceção técnica) é descrita pelo texto observado na execução.

## 5. Figuras e diagramas

```markdown
![Tela de detalhe do exemplar com os blocos Condição, Localização e Financeiro](capturas/05-exemplar-detalhe.png)

*Figura 5 — Detalhe do exemplar: (1) condição e embalagem; (2) localização atual;
(3) ações de editar e mover.*
```

- Caminho **relativo** ao documento; arquivo precisa existir (FR-017/SC-005).
- Legenda obrigatória com número, tela/estado e, se houver, legenda das anotações.
- Diagramas de relacionamento e de fluxo usam o mesmo padrão de legenda; não contam como
  captura de tela.

## 6. Regras de redação

- Português brasileiro, terminologia do produto: **modelo**, **variante**, **exemplar**,
  **série**, **coleção**, **localização**, **aquisição**, **avaliação** (Art. IV.1).
- Rótulo sempre igual ao exibido; não traduzir, não “melhorar” texto do app.
- Instrução no imperativo (“Toque em **Salvar**”), uma ação por passo.
-Não usar nomes de função, variável, tabela ou rota na parte voltada ao leitor.
- Não prometer recurso planejado; ausência vai para a seção 8 e para
  `LIMITACOES_DOCUMENTACAO.md` (FR-015/FR-019).
- Estimativas são identificadas como estimativa, com origem e data (Art. IV.8).
- Nenhum dado pessoal real em exemplo, tabela ou captura (FR-016).

## 7. Capítulos que não são de tela

Capítulos transversais (estrutura do domínio, primeiros passos, exemplo Skyline, backup e
restauração, importação/exportação, privacidade) seguem as 10 seções quando fizer sentido,
substituindo “Elementos da tela” por uma tabela de **conceitos** ou **entidades** e mantendo
exemplos, regras, mensagens, dicas e problemas comuns. O capítulo de estrutura do domínio
exige diagrama de relacionamentos (FR-003).

## 8. Critérios de aceite do capítulo

- [ ] As 10 seções existem, na ordem, com os títulos do contrato.
- [ ] Cabeçalho com rota, arquivo, acesso e capturas (ou lacuna justificada).
- [ ] Tabela de elementos cobre 100% dos interativos da `Screen.kt`.
- [ ] Pelo menos um exemplo numérico ou de cadastro conferível.
- [ ] Mensagens literais e estados vazio/carregando/erro/sucesso tratados.
- [ ] Toda figura existe, tem legenda e está no capítulo correto.
- [ ] Nenhuma afirmação sem lastro no código ou em execução registrada.
- [ ] Capítulo vinculado na matriz e no índice funcional.
