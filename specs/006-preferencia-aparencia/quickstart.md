# Quickstart: validar a preferência de aparência

**Feature**: `006-preferencia-aparencia` | **Data**: 2026-11-25

Roteiro manual dos 12 critérios de aceitação do pedido. Cada cenário indica comando, o que observar e onde registrar a evidência. O que é automatizado está em `tasks.md`; aqui ficam apenas as verificações que dependem de gesto, de reinício de aparelho ou de inspeção visual.

**Pacote de debug**: `br.com.diecastcollector.debug`
**Activity**: `br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity`

## Preparação

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear br.com.diecastcollector.debug   # instalação limpa (remove coleção; use só no emulador)
adb shell cmd uimode night no                     # Android em claro
```

> `pm clear` apaga os dados do app no emulador. Em aparelho com coleção real, não execute; use outro perfil/usuário ou um AVD separado.

## 1. Instalação inicial com preferência Sistema

1. Abrir o app e tocar em **Ajustes**.
2. Observar a seção **Aparência** com três opções e **Sistema** marcada.
3. `adb shell cmd uimode night yes` e voltar ao app.

**Esperado**: paleta clara com o Android claro; paleta escura em ≤ 1 s após o Android mudar, sem reiniciar.
**Evidência**: captura de Ajustes nos dois modos + vídeo curto ou anotação de tempo.

## 2. Seleção manual do tema Claro

1. Com o Android em escuro, tocar em **Claro**.
2. Percorrer Painel, Catálogo, Desejos, Financeiro e Ajustes.

**Esperado**: tudo claro imediatamente, marca em **Claro**, nenhuma superfície escura.
**Evidência**: capturas das cinco abas em `build/responsive/aparencia/claro/`.

## 3. Seleção manual do tema Escuro

1. Tocar em **Escuro**.
2. Abrir ficha com fotos, busca, um diálogo e uma folha.

**Esperado**: paleta escura idêntica à atual (fundo `InkBlack`, cartões `Graphite`, destaques `FlameRedDark`), sem mudança de layout.
**Evidência**: capturas em `build/responsive/aparencia/escuro/`.

## 4. Retorno ao modo Sistema

1. Tocar em **Sistema** com o Android em claro; depois mudar o Android para escuro.

**Esperado**: após o toque, paleta clara; após `cmd uimode night yes`, paleta escura em ≤ 1 s.
**Evidência**: anotação de tempo + captura.

## 5. Persistência após encerramento

```bash
# escolha Escuro em Ajustes
adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity
```

**Esperado**: abre em escuro. Repetir com **Claro**.
**Evidência**: captura após cada reabertura. O `ActivityScenario.recreate` é automatizado em `AppearancePersistenceTest` (T411); o encerramento real com `force-stop` é **manual** (T413).

## 6. Persistência após reinício do aparelho

```bash
# escolha Escuro em Ajustes
adb reboot
# após o boot
adb shell am start -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity
```

**Esperado**: abre em escuro.
**Evidência**: data/hora do reboot e captura. **Não** é teste automatizado; registrar como execução manual.

## 7. Android muda de tema com o app em Sistema

1. Abrir o app (modo Sistema) e deixá-lo em Painel.
2. `adb shell cmd uimode night yes` e, depois, `no`.

**Esperado**: as duas trocas aparecem sem reiniciar e sem perder a aba/rolagem; em ≥ 1 s a tela já está na paleta nova.
**Evidência**: gravação de tela curta (`adb shell screenrecord`).

## 8. Alteração imediata da aparência

1. Rolar o catálogo até o meio.
2. Abrir Ajustes (a rolagem não precisa persistir entre abas) e alternar Claro ↔ Escuro dez vezes.

**Esperado**: cada toque aplica a paleta na hora; nenhum "flash" branco/escuro persistente; nenhuma tela em branco.
**Evidência**: gravação de tela; anotar se houver piscada perceptível.

## 9. Comportamento consistente entre telas

Percorrer o inventário de 37 linhas da Spec 005 (`build/responsive/checklist.md`) nos modos **Claro** e **Escuro**.

**Esperado**: nenhuma tela com superfície da paleta oposta, texto ilegível ou elemento sumido; mesmo layout nas duas aparências.
**Evidência**: uma linha por tela no checklist de aparência, com o arquivo de captura de cada modo.

## 10. Cartões de coleção e editor de fotografias

1. Modo **Claro**: abrir uma coleção com cartões com foto e sem foto; abrir o editor de uma foto.
2. Repetir em **Escuro**.

**Esperado**: cartões com fundo de superfície do modo, miniatura não tingida, marcador de "sem foto" visível; editor com área de edição neutra (a imagem não muda de cor) e ferramentas legíveis.
**Evidência**: par de capturas por modo; comparar a área da imagem entre Claro e Escuro (deve ser idêntica).

## 11. Contraste de textos e elementos gráficos

Verificações automatizadas cobrem os pares do tema e os elementos renderizados (`RenderedContrastTest`). Manualmente:

1. Em **Claro**, abrir uma foto muito clara com controles sobrepostos na grade e na revisão de captura.
2. Em **Escuro**, repetir com uma foto muito escura.

**Esperado**: ícones e rótulos sobre a imagem continuam legíveis (branco sobre *scrim*).
**Evidência**: capturas + resultado do teste. Não substituir o teste por julgamento visual.

## 12. Ausência de regressões

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

**Esperado**: suíte verde; nenhum teste removido, ignorado ou afrouxado.
**Evidência**: saída dos comandos em `docs/validacao.md`.

## Classes de janela e acessibilidade (V.10)

```bash
# janela compacta
adb shell wm size 1080x2400; adb shell wm density 420   # ≈ 411 × 914 dp
# janela expandida
adb shell wm size 2625x2400; adb shell wm density 420   # ≈ 1.000 × 914 dp
```

1. Abrir Ajustes → Aparência em 360 dp e em 1.000 dp, nos dois modos.
2. Ativar fonte em 200% (`adb shell settings put system font_scale 2.0`) e repetir.

**Esperado**: três opções inteiras, alvos ≥ 48 dp, sem truncamento; seção alinhada à coluna de leitura em 1.000 dp.
**Evidência**: capturas por classe e fonte.

## Registro

Cada execução manual deve ser anotada em `docs/validacao.md` com: data, aparelho/AVD, comando usado, resultado e caminho das capturas. Cenário não executado é declarado como **não verificado** — nunca como aprovado.
