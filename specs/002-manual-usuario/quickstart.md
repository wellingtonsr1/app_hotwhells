# Quickstart — validação do Manual de Usuário

**Feature**: `002-manual-usuario` | **Data**: 2026-11-21 | **Commit de referência**: `424e4fb`
| **Plano**: [plan.md](./plan.md) | **Contrato de capítulo**:
[contracts/chapter-contract.md](./contracts/chapter-contract.md) | **Matriz**:
[contracts/coverage-contract.md](./contracts/coverage-contract.md)

Guia operacional para produzir as evidências e provar que a entrega atende à spec. Não
contém código do aplicativo: nada aqui altera `app/`, `core/`, banco ou configuração.

## 1. Pré-requisitos (verificados no ambiente)

| Item | Valor |
|---|---|
| JDK | 17 (Temurin) — `jvmTarget = 17` |
| Android SDK | `sdk.dir=.tools/android-sdk` (`platforms;android-36`, `platform-tools`, `emulator`) |
| Emulador | `hotwheels_test` (API 36, x86_64) |
| Python | `python3` com `markdown` 3.10.3 e `weasyprint` 70.0 |
| Versão documentada | `0.1.0`, versionCode 1 |

Atalho para as ferramentas (o `adb` não está no `PATH`):

```bash
export SDK="$PWD/.tools/android-sdk"
export ADB="$SDK/platform-tools/adb"
export EMU="$SDK/emulator/emulator"
```

## 2. Preparar o app para captura

```bash
# build de debug (único com ABI x86/x86_64 para emulador)
./gradlew :app:assembleDebug

# subir o AVD sem snapshot e esperar o boot
"$EMU" -avd hotwheels_test -no-snapshot-load &
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done

# instalar do zero (instalação limpa para o roteiro de primeiros passos)
"$ADB" uninstall br.com.hotwheelscollector.debug || true
"$ADB" install app/build/outputs/apk/debug/app-debug.apk
```

**Dados fictícios**: o app não tem semente automática; os registros de demonstração são
criados pela interface e seguem o exemplo do manual (fabricante “Hot Wheels”, modelo
“Nissan Skyline GT-R (R34)”, ano 2025, escala 1:64, cor azul). Nenhum dado pessoal real
pode entrar em captura, backup de demonstração ou log.

Registrar no início, para o cabeçalho do manual:

```bash
git rev-parse --short HEAD       # commit usado
git log -1 --format=%cI          # data
adb shell dumpsys package br.com.hotwheelscollector.debug | grep versionName
```

## 3. Capturar as telas

```bash
mkdir -p docs/manual/capturas

# uma captura por tela/estado, nome estável
"$ADB" exec-out screencap -p > docs/manual/capturas/01-painel-vazio.png
```

Checklist por captura:

- [ ] App em execução, dados fictícios, estado nomeado (vazio, preenchido, erro, diálogo).
- [ ] Nome `NN-descricao-estado.png`; `NN` na ordem editorial do manual.
- [ ] Legenda escrita no capítulo com tela e estado representados.
- [ ] Anotações numéricas, quando houver, com legenda correspondente.
- [ ] Nenhum dado pessoal visível (notificações, nome de conta, caminho de arquivo).
- [ ] Registro de origem/data/commit na tabela de evidências.

**Ordem de execução**: capturar primeiro os fluxos normais; deixar para o fim a
**restauração de backup**, porque ela fecha e reabre o banco. Depois de restaurar, reabrir o
app e conferir a lista antes de encerrar a sessão.

**Tela não capturável**: não usar mockup nem imagem genérica. Registrar `lac-XX` com motivo
e citar a lacuna no ponto de uso e em `LIMITACOES_DOCUMENTACAO.md` (FR-019).

## 4. Gerar o PDF

O Markdown é a fonte; o PDF é derivado com `docs/manual/manual.css`.

```bash
python3 - <<'PY'
import pathlib, markdown
from weasyprint import HTML, CSS

base = pathlib.Path("docs/manual")
texto = (base / "MANUAL_USUARIO_COMPLETO.md").read_text(encoding="utf-8")
html = markdown.markdown(
    texto,
    extensions=["tables", "toc", "attr_list", "sane_lists"],
    extension_configs={"toc": {"permalink": False}},
)
documento = f"""<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8">
<title>Manual do Usuário — Hot Wheels Collector 0.1.0</title></head>
<body><article class="manual">{html}</article></body></html>"""
HTML(string=documento, base_url=str(base.resolve())).write_pdf(
    base / "MANUAL_USUARIO_COMPLETO.pdf",
    stylesheets=[CSS(filename=str(base / "manual.css"))],
)
print("PDF gerado")
PY
```

Conferir no PDF: capa com nome e versão, sumário navegável, numeração de páginas,
cabeçalho/rodapé, títulos hierarquizados, tabelas e figuras sem quebra no meio, imagens
legíveis. Se sumário ou paginação não funcionarem, registrar a limitação — não omitir.

## 5. Validações de aceite

### 5.1 Cobertura de destinos (SC-001)

```bash
grep -oP 'data object \K\w+' \
  app/src/main/java/br/com/hotwheelscollector/navigation/AppNavHost.kt | sort -u
```

Comparar com as 30 linhas da matriz; toda diferença é falha. O conjunto esperado tem
Painel, Catálogo, Busca, Cadastro inicial, Formulário de variante, Incompletos, Edição de
modelo, Adicionar exemplar, Lista de exemplares, Detalhe do exemplar, Coleções, Detalhe de
coleção, Formulário de coleção, Séries, Código de barras, Duplicados, Negociação, Lista de
desejos, Formulário de desejo, Financeiro, Edição de variante, Localizações, Conteúdo da
localização, Ajustes, Backup, Relatórios, Importação, Dados da coleção, Sobre e Privacidade.

### 5.2 Referências de imagem (SC-005)

```bash
grep -rhoP '\]\(\Kcapturas/[^)]+' docs/manual/*.md | sort -u | while read -r img; do
  [ -f "docs/manual/$img" ] || echo "QUEBRADA: $img"
done
```

Saída esperada: nenhuma linha `QUEBRADA`.

### 5.3 Contrato de capítulo

Para cada seção de tela, conferir as 10 seções obrigatórias, a tabela de elementos completa
e as mensagens literais (`contracts/chapter-contract.md`, seção 8).

### 5.4 Fidelidade ao app

- Rótulos conferem com `app/src/main/res/values/strings.xml`.
- Cálculos do capítulo financeiro reproduzem `FinanceDao.observeSummary()`
  (investido = preço + frete − desconto de exemplares `OWNED`; estimado = última avaliação).
- Nenhuma promessa de recurso planejado; divergências de `docs/operacao.md` e
  `docs/continua.md` tratadas como legado, não como fonte.

### 5.5 Execução dos roteiros (SC-003/SC-004)

Em instalação limpa, executar o capítulo «Primeiros passos» e o exemplo Skyline **usando
somente o manual**. Registrar data, emulador e resultado. O leitor deve concluir cadastro de
fabricante, modelo, variante, exemplar, fotos, aquisição, localização, coleção, painel e
backup sem consultar o código.

### 5.6 Entrega documental (SC-006/SC-008)

```bash
ls docs/manual/MANUAL_USUARIO_COMPLETO.md \
   docs/manual/MANUAL_USUARIO_COMPLETO.pdf \
   docs/manual/INDICE_FUNCIONALIDADES.md \
   docs/manual/MATRIZ_COBERTURA.md \
   docs/manual/LIMITACOES_DOCUMENTACAO.md
ls docs/manual/capturas | head

git status --porcelain   # deve listar apenas specs/002-manual-usuario e docs/manual
```

Saída esperada: os cinco arquivos existem, a pasta de capturas não está vazia e o `git
status` não mostra arquivo de aplicativo alterado.

## 6. Definição de pronto

- [ ] `MANUAL_USUARIO_COMPLETO.md` com todos os capítulos no contrato.
- [ ] `MANUAL_USUARIO_COMPLETO.pdf` gerado, com sumário, numeração e imagens.
- [ ] `INDICE_FUNCIONALIDADES.md` e `MATRIZ_COBERTURA.md` sem órfãos.
- [ ] `LIMITACOES_DOCUMENTACAO.md` com toda lacuna e motivo.
- [ ] `capturas/` com nomes estáveis e todas as referências válidas.
- [ ] Revisões técnica, editorial e de validação registradas (FR-024).
- [ ] `git status` sem alteração fora de `specs/002-manual-usuario` e `docs/manual`.
