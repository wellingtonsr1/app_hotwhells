# Demo — Painel com Investido / Estimado / Diferença preenchidos

Objetivo: vídeo (ou captura) do app já aberto no **Painel (US3)** mostrando
`Investido`, `Estimado` e `Diferença` preenchidos, com um exemplar cadastrado
com preço pago.

## Estado entregue

- `docs/demo_dashboard.png` — captura do Painel com os três valores preenchidos
  (Investido R$ 49,90, Estimado R$ 75,00, Diferença R$ 25,10), além de
  `2` exemplares, `1` modelo, `1` variante, `Twin Mill / Mattel` nas seções de
  aquisições e cadastros, e o selo de estimativa.
- `docs/simulacao-app.mp4` — vídeo de simulação existente, não é o demo do
  painel (manter separado).

O vídeo de demonstração em movimento (`screenrecord`) **não foi reproduzível**
neste ambiente: o container MP4 gerado pelo `screenrecord --time-limit` tem
`ftyp + moov` válidos, mas `mdat` de 1 byte (zero frames reais) — o emulador
em modo headless com `-gpu swiftshader_indirect` não fornece frames ao gravador
("recorded 2 frames in N seconds"). Por isso entrega-se a captura estática que
mostra exatamente o que o vídeo deveria mostrar: o Painel com os três valores.

## Dados do banco (para reprodução)

O estado do banco que gera essa tela está no aparelho/emulador
(`br.com.diecastcollector.debug`, SQLite `diecast.db`). Para reproduzir
apenas a tela (sem gravar), basta iniciar o app; os dados já estão lá se o teste
`DemoRecordingTest` rodou antes (ele popula e não apaga ao sair — mas o
`freshInstallRule` rodeia cada teste e reconstrói o banco a cada execução).

Linhas relevantes (após o teste popular):

```sql
-- 1 fabricante, 1 modelo, 1 variante, 2 exemplares
SELECT COUNT(*) FROM manufacturer;  --> 1
SELECT COUNT(*) FROM car_model;    --> 1
SELECT COUNT(*) FROM variant;      --> 1
SELECT COUNT(*) FROM exemplar;     --> 2

-- 1 aquisição de R$ 49,90 e 1 avaliação de R$ 75,00
SELECT price_cents, exemplar_id FROM acquisition;  --> 4990
SELECT value_cents, exemplar_id FROM valuation;    --> 7500
```

Resultado no Painel:
- Investido = R$ 49,90 (soma das aquisições, `AcquisitionDao.observeTotalInvestedCents`)
- Estimado = R$ 75,00 (soma da avaliação mais recente por exemplar,
  `ValuationDao.observeCurrentEstimatedCents`)
- Diferença = R$ 25,10 (Estimado − Investido)

Se quiser regenerar os dados sem rodar o teste, insere manualmente no aparelho:

```bash
# abre um shell com acesso ao banco do app (precisa de run-as)
adb shell run-as br.com.diecastcollector.debug sqlite3 databases/diecast.db
```

Uma sequência mínima que gera a tela (IDs são UUIDs reais do teste; substitua
se quiser):

```sql
-- exemplar já existente (criado pelo teste): 5f3d9ee6-4bb0-447d-8341-e4a017e236a5
-- inserir a avaliação que faz o Estimado aparecer (se não houver)
INSERT INTO valuation (id, exemplar_id, value_cents, currency, date, source, notes, created_at)
VALUES ('val-1', '5f3d9ee6-4bb0-447d-8341-e4a017e236a5', 7500, 'BRL', '2026-11-18',
        'Estimativa de mercado', 'Valor estimado', 1795015614759);
```

Se o banco estiver vazio, rode o teste de populate (ele faz todo o fluxo):
`app/src/androidTest/java/br/com/diecastcollector/DemoRecordingTest.kt`.

## Commands exatos usados nesta sessão

Toda a sessão usa **JDK 17 do Android SDK** (a `java` do PATH é Java 8 e não
compila). Variáveis:

```bash
# no Git Bash (MSYS_NO_PATHCONV=1 evita quebra de caminho no adb shell)
export JAVA_HOME="$LOCALAPPDATA/Android/Jdk/jdk-17.0.21+9"
export PATH="$LOCALAPPDATA/Android/Sdk/platform-tools:$PATH"
export ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
```

### 1. Subir o emulador (API 36, x86_64, diecast_test)

```bash
# em background; o adb devices só confirma quando o boot completa (~40s)
"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" \
  -avd diecast_test \
  -no-window -no-audio -no-boot-anim \
  -gpu swiftshader_indirect &
sleep 45
adb devices   # deve mostrar emulator-5554 device
```

Para parar depois:

```bash
adb shell ps -A | awk '/screenrecord/{print $2}'   # se estiver gravando
adb shell am broadcast -a android.media.action.STOP_RECORDING
# ou, para o emuladorinteiro:
taskkill //F //PID <pid-do-emulator.exe>
```

### 2. Instalar os APKs (debug + androidTest)

```bash
MSYS_NO_PATHCONV=1 adb install -r app/build/outputs/apk/debug/app-debug.apk
MSYS_NO_PATHCONV=1 adb install -r \
  app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

Se os APKs não existirem, build antes:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

(com `JAVA_HOME` apontando para o JDK 17 do SDK).

### 3. Rodar o teste que popula o Painel (catálogo → variante → exemplar com preço)

```bash
MSYS_NO_PATHCONV=1 adb shell am instrument -w -r \
  -e class br.com.diecastcollector.DemoRecordingTest \
  br.com.diecastcollector.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Esperado: `OK (1 test)`, tempo ~17s. Ao final, o app fica no Painel com
Investido/Estimado/Diferença preenchidos (o teste termina com o Painel visível e
aguarda alguns segundos).

### 4. Capturar a tela (PNG) — usada como evidência nesta entrega

```bash
adb shell screencap -p /sdcard/demo_dashboard.png
adb shell cat /sdcard/demo_dashboard.png > docs/demo_dashboard.png
```

(usado `adb shell cat > arquivo` porque `adb pull` falhou neste ambiente com erro
de caminho; `screencap` é o que funcionou para capturar o frame.)

### 5. Vídeo (screenrecord) — tentado, não reproduzível aqui

```bash
# inicia gravação em background (não funciona direito neste ambiente)
adb shell "rm -f /sdcard/DemoPainel.mp4 && screenrecord \
  --size 1280x720 --bit-rate 8000000 --time-limit=30 \
  /sdcard/DemoPainel.mp4" > /dev/null 2>&1 &

# ... aguardar ...

# parar: o próprio --time-limit corta; fallback:
adb shell pkill -9 -f screenrecord
adb shell am broadcast -a android.media.action.STOP_RECORDING

# tentar puxar (se houver frames)
adb pull /sdcard/DemoPainel.mp4 docs/demo_recording.mp4
```

Resultado observado: arquivo sai com `ftyp + moov` válidos, mas `mdat` de 1 byte
(sem frames) — `recorded 2 frames in N seconds` no verbose. Por isso não entregue
como vídeo; a captura PNG substitui.

## O que falta para entregar o vídeo em movimento

- Um ambiente onde o `screenrecord` capture frames do emulador (ex.: emulador
  com display visível e não headless, ou aparelho físico com `adb` e permissão de
  tela). Neste ambiente o emulador headless não fornece frames ao gravador.
- Alternativa sem emulador: um APK instalado em aparelho conectado via USB e
  `screenrecord` (ou `scrcpy`) funcionando — fora do alcance desta máquina.

Se o vídeo em movimento for obrigatório, rodar a mesma sequência em um
ambiente com display visível e confirmar que o `mdat` tem mais que alguns bytes
antes de puxar.
