#!/usr/bin/env bash
# Semeia o build `benchmark` do app com 500 variantes para medir SC-003 (spec 003).
#
# Pré-requisitos: emulador x86_64 ligado, `adb` no PATH, e `python tools/benchmark_seed.py`
# já executado (gera `build/benchmark-seed/seed.sql` e as 450 miniaturas).
#
# O build `benchmark` não é depurável (exigência do MacrobenchmarkRule), então `run-as` não
# funciona: o script usa `adb root`, disponível nos emuladores do projeto.
set -euo pipefail

ADB="${ADB:-adb}"
PKG=br.com.diecastcollector
SEED=build/benchmark-seed

"$ADB" root >/dev/null
sleep 2

"$ADB" install -r app/build/outputs/apk/benchmark/app-benchmark.apk >/dev/null
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null

# Room cria o arquivo na primeira leitura; sem ele não há esquema para inserir.
for _ in $(seq 1 30); do
    if "$ADB" shell "[ -f /data/data/$PKG/databases/diecast.db ]" && \
       "$ADB" shell "test -e /data/data/$PKG/databases/diecast.db"; then
        break
    fi
    sleep 1
done
"$ADB" shell am force-stop "$PKG"

"$ADB" shell "sqlite3 /data/data/$PKG/databases/diecast.db" < "$SEED/seed.sql"

"$ADB" shell "rm -rf /data/data/$PKG/files/seed-tmp"
"$ADB" push "$SEED/images/seed" "/data/data/$PKG/files/seed-tmp" >/dev/null

# O dono é o uid do pacote, lido do diretório de dados: `run-as` não existe aqui porque o
# build `benchmark` não é depurável, e uma instalação nova pode trocar o uid (u0_a217 → a218).
OWNER=$("$ADB" shell "stat -c '%u:%g' /data/data/$PKG" | tr -d '\r')
"$ADB" shell "cd /data/data/$PKG && \
    mkdir -p files/images && rm -rf files/images/seed && \
    mv files/seed-tmp files/images/seed && \
    chown -R '$OWNER' files databases && \
    chmod 771 files databases && chmod 660 databases/* && \
    restorecon -R files databases"

"$ADB" shell "sqlite3 /data/data/$PKG/databases/diecast.db \
    'SELECT \"variantes: \" || COUNT(*) FROM variant; SELECT \"fotos: \" || COUNT(*) FROM photo;'"
