# Limitações da Documentação — Hot Wheels Collector

**Versão documentada**: 0.1.0 (versionCode 1) · **Commit**: `424e4fb`
**Data da apuração**: 22/11/2026 · **Ambiente**: emulador `hotwheels_test` (API 36, headless)

Este documento declara o que **não** pôde ser comprovado por captura ou execução real. Nenhuma
tela, botão ou comportamento foi inventado: onde não há evidência, o manual aponta para a
limitação correspondente.

## Resumo

| ID | Item | Tipo | Impacto no manual | Status |
|---|---|---|---|---|
| lac-01 | Leitura real de código de barras | hardware ausente | tela documentada; decodificação não concluída | aberta |

As lacunas anteriores desta documentação foram **fechadas** em 22/11/2026:

| Antiga lacuna | Como foi fechada | Evidência |
|---|---|---|
| Edição de variante sem captura | variante incompleta criada por importação de CSV sem cor/ano; acesso por **Incompletos** → **Editar variante** | `capturas/10c-incompletos-com-variante.png`, `capturas/12-edicao-variante.png` |
| Nenhuma foto de exemplar | foto fictícia anexada pela **Galeria** no cadastro da variante | `capturas/06b-seletor-de-fotos.png`, `capturas/06c-variante-com-foto.png`, `capturas/07b-catalogo-com-foto.png`, backup com «1 foto» |
| Restauração de backup não executada | dois pacotes criados e o segundo restaurado com **Substituir** e **Reabrir o app** | `capturas/29c-salvar-backup.png` a `capturas/29f-restauracao-concluida.png` |
| Relatórios não abertos | PDF, CSV e XLSX gerados no app, extraídos por `adb pull` e abertos: os três traziam as mesmas 3 linhas e as colunas Código/Modelo/Variante/Condição/Fotos | `capturas/30b` a `capturas/30e` |

## lac-01 — Leitura de código de barras não concluída

- **O que falta**: leitura efetiva de um código e o preenchimento automático resultante.
- **Motivo**: o emulador headless não tem câmera útil; a tela de leitura foi capturada, mas a
  decodificação não ocorreu.
- **O que o manual faz**: documenta a tela, o botão de entrada manual e a mensagem de erro
  prevista, sem afirmar que a leitura foi testada.
- **Como fechar**: executar em aparelho físico com uma embalagem real.

## Observações de comportamento comprovadas na captura

- O detalhe do exemplar **não** exibe as fotos anexadas no cadastro; elas existem, são contadas
  no backup («1 foto»), mas não há galeria nessa tela.
- A edição de variante só é acessível por **Incompletos**, quando falta cor principal ou ano.
- A importação de uma linha sem `cor_principal` e sem `ano_lancamento` grava a variante e a
  marca como incompleta, sem impedir a gravação.
- A pré-visualização da importação mostra apenas os totais; os motivos de cada erro só saem no
  CSV de erros. Colunas desconhecidas são ignoradas sem aviso na tela.

## O que **não** é limitação

- Rótulos, rotas, campos, ícones e mensagens: conferidos em `strings.xml`, no `AppNavHost.kt` e
  nas telas reais.
- Valores do exemplo (R$ 44,30 investidos, R$ 45,00 estimados, R$ 0,70 de diferença): conferidos
  no emulador com os dados fictícios do Apêndice B.
- Contagem de destinos (30) e de abas (5): conferida no código e na interface.
- Criação e restauração de backup, incluindo contagem de fotos: executadas e capturadas.
- Conteúdo dos relatórios PDF, CSV e XLSX: gerado no app, aberto fora dele e conferido.
- Importação com erros: CSV com 7 linhas inválidas e 1 duplicata, prévia capturada e relatório
  de erros (`linha,campo,codigo,mensagem,valor_informado`) extraído e conferido.
- Nomes internos de condição nos relatórios (`SEALED`, `LOOSE`, …): verificados no PDF, no CSV e
  no XLSX e documentados no capítulo 30.

## Registro de revisões

| Data | Revisor | Escopo | Resultado |
|---|---|---|---|
| 22/11/2026 | Buffy (Codebuff) | técnica: rotas, rótulos, cálculos | aprovado com lac-01 |
| 22/11/2026 | Buffy (Codebuff) | editorial: estrutura de 10 seções, português | aprovado |
| 22/11/2026 | Buffy (Codebuff) | validação: links, imagens, PDF | aprovado com lacunas declaradas |
| 22/11/2026 | Buffy (Codebuff) | fechamento de lacunas de captura (variante, foto, restauração) | 3 lacunas fechadas |
| 22/11/2026 | Buffy (Codebuff) | abertura dos relatórios PDF/CSV/XLSX | 1 lacuna fechada |
