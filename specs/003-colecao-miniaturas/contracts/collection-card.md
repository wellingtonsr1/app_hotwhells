# Contrato de Interface — Cartão da Coleção

**Versão**: 1.0 — 2026-11-23
**Escopo**: contrato visível de cada item da listagem da tela Coleção. Não descreve
implementação; fixa o que o usuário vê e o que o teste pode afirmar.

## Estrutura

```text
┌──────────────────────────────────────────────┐
│ ┌────────┐  Nome do modelo                   │
│ │ FOTO   │  Fabricante · Ano · N exemplares  │
│ │ 64 dp  │                                   │
│ └────────┘                    Ver exemplares │
└──────────────────────────────────────────────┘
```

| Elemento | Regra |
|---|---|
| Miniatura | Quadrada, 64 dp, cantos 10 dp, à esquerda, centralizada verticalmente, 12 dp de afastamento |
| Título | Nome do modelo, no máximo 2 linhas com reticências |
| Subtítulo | Fabricante, ano (se houver) e contagem, separados por « · », no máximo 2 linhas com reticências |
| Atalho | «Ver exemplares», alinhado ao fim, abaixo do bloco de texto |
| Cartão | Fundo de superfície do tema em uso (claro ou escuro), cantos arredondados, largura total |

## Estados da miniatura

| Estado | Entrada | Renderização | Descrição acessível |
|---|---|---|---|
| Foto | Caminho não vazio e arquivo decodificável | Imagem com recorte central | «Foto de {modelo} · {cor} · {ano}» (partes ausentes omitidas) |
| Sem foto | Caminho nulo ou vazio | Marcador quadrado com ícone de carrinho no fundo `surfaceVariant` | «Sem foto do carrinho» |
| Foto inválida | Arquivo ausente ou falha de decodificação | Mesmo marcador do estado sem foto | «Sem foto do carrinho» |
| Ampliação | Toque na foto | Diálogo de imagem inteira com botão fechar | Mantém a descrição da foto |

## Interações

| Gesto | Onde | Efeito |
|---|---|---|
| Toque | Qualquer área do cartão fora da foto | Abre o cadastro rápido da variante (comportamento atual) |
| Toque | Miniatura com foto | Abre a ampliação; não navega e não altera a variante |
| Toque | Miniatura sem foto/inválida | Não amplia |
| Toque | «Ver exemplares» | Abre a lista de exemplares da variante do próprio cartão |

## Conteúdo do subtítulo

- Fabricante sempre que existir.
- Ano somente se cadastrado; ausente não gera texto, «N/A» nem zero.
- Contagem: `1 exemplar`, `N exemplares`, ou «nenhum exemplar» quando zero.
- Partes vazias não deixam separador solto.

## Identidade visual

- Segue a aparência escolhida pelo usuário (Spec 006):
  - **Escuro / Sistema escuro**: `InkBlack` de fundo, `SmokyGrey` nas superfícies, destaque `FlameRedDark`.
  - **Claro / Sistema claro**: `Chalk` de fundo, `PureWhite` nas superfícies, destaque `FlameRed`.
- Tipografia Material 3: título em `titleMedium`, subtítulo em `bodySmall` com
  `onSurfaceVariant`.
- Nenhuma cor, fonte ou espaçamento novo fora dos tokens existentes; a miniatura não recebe tingimento do tema.

## Acessibilidade

- A foto tem descrição; o marcador também (nunca `null`).
- O cartão é acionável por leitor de tela com o texto visível.
- Funciona com fonte ampliada: o texto quebra até duas linhas e a miniatura não encolhe.
- Alvo de toque do cartão e do atalho ≥ 48 dp.

## Critérios de conformidade (testáveis)

1. Variante sem foto ⇒ existe nó com «Sem foto do carrinho».
2. Variante com foto principal ⇒ existe nó com «Foto de {modelo} · {cor} · {ano}».
3. Variante sem ano ⇒ subtítulo sem o campo e sem separador sobrando.
4. «Ver exemplares» aciona a variante do cartão, não outra.
5. Toque no título abre o cadastro rápido da variante.
6. Arquivo inválido ⇒ marcador, sem exceção e sem área vazia.
