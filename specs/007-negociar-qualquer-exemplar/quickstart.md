# Quickstart: validar a negociação de qualquer exemplar

**Feature**: `007-negociar-qualquer-exemplar` | **Data**: 2026-11-27

Roteiro manual dos critérios de sucesso. O que é automatizado está em `tasks.md`; aqui ficam as verificações que dependem de gesto, de inspeção visual ou de observação de tempo. Cada cenário indica o comando, o que observar e onde registrar a evidência.

**Pacote de debug**: `br.com.diecastcollector.debug`
**Activity**: `br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity`
**Banco**: `adb shell run-as br.com.diecastcollector.debug ls databases/` (para conferir as linhas depois das ações)

## Preparação

```bash
export JAVA_HOME="C:/Users/wellingtonrodrigues/AppData/Local/Android/Jdk/jdk-17.0.21+9"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# instalação limpa (apaga a coleção; use só em emulador)
adb shell pm clear br.com.diecastcollector.debug
```

Massas mínimas:

| Massa | Conteúdo | Para quê |
|---|---|---|
| A | 1 variante com **1** exemplar `OWNED` | unicidade, aviso, Duplicados vazio |
| B | 1 variante com **2** exemplares `OWNED` | sem aviso, Duplicados com grupo |
| C | 1 variante com **3** exemplares `OWNED` | disponibilidade por unidade |
| D | variante do mesmo modelo com cor diferente | não é duplicata (Art. I.3) |

## 1. Disponibilizar o único exemplar (SC-501, SC-502)

1. Massa A: abrir **Catálogo → variante → exemplares**; na linha, tocar **Disponibilidade**.
2. Escolher **Venda**, informar valor, tocar **Salvar**.
3. No aviso, tocar **Continuar**.
4. Voltar à lista, abrir a ficha do mesmo exemplar e tocar **Disponibilidade** de novo; escolher **Troca** e salvar.

**Esperado**: as duas superfícies mostram **Venda** e depois **Troca**; a tela **Duplicados** continua com "Nenhum duplicado".
**Evidência**: prints de lista, ficha e Duplicados; `adb shell run-as … sqlite3` não é necessário — a tela basta.

## 2. Aviso no momento do disponibilizar (SC-504a, SC-505a)

1. Massa A, ficha, **Disponibilidade**, escolher **Troca**, **Salvar** → deve aparecer o aviso **Último exemplar da variante**.
2. Tocar **Cancelar**.

**Esperado**: a folha continua aberta com **Troca** selecionada e nada foi gravado; ao fechar e reabrir a folha, o padrão volta a ser **Venda** pré-selecionada (comportamento atual, porque não há oferta) e a ficha/lista continuam exibindo **Indisponível**.
**Evidência**: print da folha com o aviso e print após o cancelamento.

## 3. Aviso no momento de negociar (SC-504b, SC-505b)

1. Massa A, ficha, **Negociar** → aviso; **Cancelar**.
2. Repetir com **Continuar** e preencher contraparte/canal; tocar **Abrir negociação**.
3. Repetir o mesmo caminho partindo da **lista de exemplares**.

**Esperado**: **Cancelar** não cria negociação e não muda disponibilidade nem status; **Continuar** abre a tela com o tipo pré-selecionado pela oferta; o caminho pela lista tem o mesmo comportamento.
**Evidência**: print de cada aviso, do estado das duas origens e da tela de negociação.

## 4. Sem aviso quando não deve haver (SC-504c)

| Caso | Comando/ação | Esperado |
|---|---|---|
| Variante com 2+ `OWNED` | Massa B, **Negociar** | sem aviso; abre direto |
| Marcar **Indisponível** | Massa A, folha, **Indisponível**, Salvar | sem aviso |
| Editar com negociação ativa | Massa A com negociação aberta, **Disponibilidade**, mudar valor, Salvar | sem aviso |
| **Negociar** com negociação ativa | Massa A, **Negociar** | sem aviso; abre no estado ativo |
| Exemplar `SOLD`/`TRADED`/`ARCHIVED` | Massa A após venda/arquivamento | ações ausentes com explicação |

**Evidência**: uma linha por caso no checklist, com print.

## 5. Duplicados inalterado (SC-503)

```bash
# antes e depois da feature, com as mesmas massas A–D
```

1. Abrir **Catálogo → Duplicados** com A, B, C e D.

**Esperado**: só B e C aparecem, com a contagem correta ("2 exemplares…", "3 exemplares…"); A e D nunca aparecem; as ações continuam funcionando; o texto do cartão é o mesmo de antes.
**Evidência**: comparação lado a lado (print antes/depois ou `git stash` + build anterior).

## 6. Disponibilidade por unidade (SC-501, RN-002)

1. Massa C: marcar **Troca** apenas no segundo exemplar.
2. Abrir a lista e a ficha dos três.

**Esperado**: só o segundo mostra **Troca**; os outros dois, **Indisponível**.
**Evidência**: print da lista.

## 7. Consistência entre superfícies (SC-508)

1. Marcar **Venda** pela ficha; conferir lista, ficha e Duplicados.
2. Abrir negociação; conferir as três de novo.

**Esperado**: mesmo rótulo e mesmo valor pretendido nas três; **Em negociação** aparece junto da disponibilidade registrada; nenhuma tela contradiz a outra.
**Evidência**: print das três na mesma rodada, sem recarregar manualmente.

## 8. Ciclo completo (SC-507)

1. Massa A: abrir negociação, concluir **venda** com valor.
2. Voltar ao catálogo e abrir o histórico do exemplar.
3. Repetir com **troca** (informando um exemplar recebido da massa B) e com **cancelamento**.

**Esperado**: venda → fora do acervo ativo, histórico e financeiro com o registro; troca → `TRADED` e o recebido segue `OWNED`; cancelamento → exemplar continua `OWNED`, disponibilidade anterior preservada, evento no histórico.
**Evidência**: prints das três conclusões + conferência na tela de financeiro.

## 9. Segunda negociação (SC-506)

1. Massa A com negociação aberta: voltar e tocar **Negociar**.

**Esperado**: a tela mostra a negociação ativa; nenhum caminho oferece uma segunda abertura.
**Evidência**: print.

## 10. Desempenho (SC-509, SC-510)

```bash
adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -W -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity
```

1. Com ~10.000 exemplares (massa gerada pelo roteiro de importação da Spec 003), abrir a lista de exemplares de uma variante grande e a ficha de um exemplar.
2. Cronometrar a percepção de abertura e a atualização do rótulo após **Salvar**.

**Esperado**: lista < 1 s; rótulo atualizado em < 300 ms sem recarregar.
**Evidência**: medição com o comando reproduzível registrada em `docs/validacao.md`. **Sem medição, o número não pode ser declarado atendido** (V.8).

## 11. Classes de janela, fonte e acessibilidade (SC-513)

```bash
adb shell wm size 1080x2400;  adb shell wm density 420   # ≈ 411 × 914 dp (compacta)
adb shell wm size 2625x2400; adb shell wm density 420   # ≈ 1.000 × 914 dp (expandida)
adb shell settings put system font_scale 2.0
```

1. Abrir ficha e lista nas duas larguras, em retrato e paisagem.
2. Com fonte 200%, abrir a folha e o aviso.

**Esperado**: rótulos de estado e ações inteiros; sem corte, sobreposição ou rolagem horizontal; **Salvar**/**Cancelar** e **Continuar**/**Cancelar** alcançáveis; TalkBack lê cada linha com o estado.
**Evidência**: capturas por classe/fonte e uma passagem com TalkBack.

## 12. Sem migração e sem regressão (SC-511, SC-512)

```bash
./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
git diff --stat -- app/src/main/java/br/com/diecastcollector/data \
                    core/data/src/main/kotlin/br/com/diecastcollector/data/db/AppDatabase.kt \
                    app/build.gradle.kts gradle/libs.versions.toml \
                    app/src/main/AndroidManifest.xml
```

**Esperado**: esquema v4 e `MigrationTest` verdes; nenhum teste removido, ignorado ou afrouxado; nenhuma dependência nova; `applicationId`/`app_name` intactos.
**Evidência**: saída dos comandos em `docs/validacao.md`.

## 13. Interrupção durante o aviso (rotação, segunda janela, processo encerrado)

```bash
# rotação forçada (a atividade é recriada)
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # paisagem
adb shell settings put system user_rotation 0   # retrato

# encerramento do processo
adb shell am force-stop br.com.diecastcollector.debug
adb shell am start -n br.com.diecastcollector.debug/br.com.diecastcollector.MainActivity
```

1. Massa A: abrir a folha com **Troca** escolhida, tocar **Salvar** e deixar o aviso aberto; girar o aparelho.
2. Repetir deixando o aviso de **Negociar** aberto e encerrar o processo pelo app recente.
3. Em janela dividida/freeform (tablet ou emulador grande), abrir a mesma ficha nas duas janelas.

**Esperado**: nenhuma oferta e nenhuma negociação criadas; após a rotação, o aviso não reaparece e a folha reabre no padrão (**Venda** pré-selecionada, sem oferta); a segunda janela reflete o estado atual do repositório, sem cópia própria.
**Evidência**: capturas antes/depois e conferência do estado na ficha. A recriação por rotação é automatizada em `ExemplarStabilityTest`; a morte de processo por `force-stop` é **manual** e declarada como tal.

## Registro

Cada execução manual deve ser anotada em `docs/validacao.md` com data, aparelho/AVD, comando, resultado e caminho das capturas. Cenário não executado é declarado **não verificado** — nunca aprovado.
