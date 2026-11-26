# Research: Preferência de aparência

**Data**: 2026-11-25 | **Feature**: `006-preferencia-aparencia`

Este documento registra o que foi **verificado no código** antes da especificação, as alternativas consideradas e por que cada uma foi descartada ou escolhida. Nenhuma conclusão aqui se baseia em suposição sobre nomes de classes, caminhos ou bibliotecas.

## 1. Levantamento do código

### 1.1 Como o tema é escolhido hoje

`core/ui/src/main/kotlin/br/com/diecastcollector/ui/theme/Theme.kt`:

```kotlin
@Composable
fun DiecastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
)
```

As paletas `LightColors` e `DarkColors` são privadas, completas (primary, secondary, background, surface, surfaceVariant, error e `on*`) e derivadas de `Color.kt`. O KDoc afirma existir "escolha explícita nas configurações", que não existe.

**Busca por usos**: `DiecastTheme(` aparece em produção apenas em `MainActivity.kt` (e em um `@Preview` no mesmo arquivo); ~30 testes instrumentados chamam `DiecastTheme { … }` sem argumento. Nenhum chamador passa `darkTheme`, então alterar o parâmetro para `mode: ThemeMode = ThemeMode.SYSTEM` é compatível.

**Busca por `isSystemInDarkTheme`**: apenas a declaração do parâmetro em `Theme.kt`. Não há outro ponto de decisão de tema.

### 1.2 Onde a preferência poderia morar

- `app/src/main/java/.../feature/settings/BackupReminderStore.kt`: `@Singleton`, `SharedPreferences("backup_reminder")`, expõe `StateFlow`, comentário explica por que fica fora do banco (restauração de pacote não pode "enganar" o lembrete) — padrão arquitetural a seguir.
- `app/src/main/java/.../di/`: módulos Hilt (`CoroutinesModule`, `UseCaseModule`, `WorkModule`) no escopo `SingletonComponent`.
- `core/data`: contém storage (imagens, backup), repositórios Room, importação; **nenhum** store de preferências. Colocar a preferência ali criaria dependência de `core:data` para algo que é estado de apresentação, e `core:ui` não pode depender de `core:data` (II.3). Decisão: o store fica em `app`, e `core:ui` recebe apenas o enum.

### 1.3 DataStore: declarado, não usado

- `gradle/libs.versions.toml`: `datastore = "1.1.1"`, `datastore-preferences = androidx.datastore:datastore-preferences`.
- `app/build.gradle.kts` linha 145 e `core/data/build.gradle.kts` linha 55 incluem `libs.datastore.preferences`.
- **Nenhum arquivo `.kt` importa `androidx.datastore`** (grep em `app` e `core` sem resultados).
- `docs/dependencias.md` descreve o DataStore como "Armazenamento de preferências simples (lembrete de backup)", mas `BackupReminderStore` usa `SharedPreferences`. O inventário está incorreto (R.5).

Consequência: usar DataStore **não** é dependência nova; é usar o que já está no classpath. Ainda assim, o uso real pode aumentar o APK (R8 deixa de remover a biblioteca) — daí o limite de +100 KB e a medição obrigatória.

### 1.4 Superfícies que dependem do modo do sistema

| Superfície | Situação | Efeito com aparência forçada |
|---|---|---|
| `MainActivity.enableEdgeToEdge()` | sem argumentos; ícones derivados do modo escuro do sistema | ícones errados quando a aparência forçada divergir do Android |
| `res/values/themes.xml` | `windowBackground = @color/window_background` = `#121212`, sem `values-night` | piscada escura ao abrir com o Android no claro (já ocorre hoje) |
| `PhotoGrid` | `colorScheme.scrim.copy(alpha = 0.6f)` + `Color.White` sobre a miniatura | funciona nos dois modos; precisa de prova renderizada |
| `PhotoEditorScreen` | fundo `Color.Black`; *scrims* 0,55–0,7; branco nos controles; alças de recorte brancas | área neutra intencional; precisa de prova renderizada |
| `PhotoReviewDialog` | `scrim` + `Color.White` | precisa de prova renderizada |
| `ItemThumbnail` | placeholder com `surfaceVariant` e ícone | depende de token, funciona nos dois modos |

Nenhum componente fora de `theme/` usa `Color(0x…)` (grep sem resultados); as cores fixas existentes são `Color.White`/`Color.Black` **sobre imagem**, justificadas por legibilidade.

### 1.5 Backup e preferências

`core/data/.../storage/FileBackupService.kt` monta o pacote com: snapshot do banco (+`-wal`), caminhos de foto válidos relativos a `filesDir` e tabelas exportadas. Não há varredura de `filesDir` inteiro, nem de `shared_prefs`, nem de DataStore. Portanto restaurar um pacote **não** altera a preferência de aparência; a regra FR-415 é atendida sem mudança no backup. (O mesmo raciocínio já vale hoje para o lembrete em `SharedPreferences`.)

### 1.6 Conflitos com specs anteriores

- **Spec 003, FR-013** e `specs/003-colecao-miniaturas/contracts/collection-card.md` (§Identidade visual): "tema escuro, fundo preto, cartões em cinza escuro". Leitura literal impede o modo claro.
- **Spec 004, Assumptions** (linha 184): "A interface segue o tema escuro e os componentes Material 3 já usados."
- **Spec 005, Edge Cases** (linha 131): "Tema escuro e claro: contraste e legibilidade preservados em todas as classes." Já pressupõe os dois modos.
- **KDoc de `Theme.kt`**: promete escolha explícita inexistente.

Interpretação adotada (registrada em `spec.md`, sem alterar as specs): a identidade é o conjunto de tokens/tipografia/formas; o escuro é a expressão dessa identidade no modo escuro e no Sistema escuro, e o claro usa os tokens claros da mesma paleta. O modo escuro permanece byte a byte igual ao atual, o que torna a mudança retrocompatível e não bloqueia a implementação.

## 2. Alternativas consideradas

### 2.1 Persistência

| Alternativa | Prós | Contras | Decisão |
|---|---|---|---|
| **DataStore Preferences** (escolhida) | Assíncrono, atômico, baseado em Flow; **já declarado**; sem dependência nova; tolerante a escrita concorrente | Primeiro valor chega após alguns ms; uso real pode aumentar o APK | **Escolhida** — decisão registrada em [`docs/adr-006-preferencia-aparencia.md`](../../docs/adr-006-preferencia-aparencia.md) (R.4) |
| `SharedPreferences` (como o lembrete) | Leitura síncrona disponível (evita piscada no primeiro quadro) | API bloqueante/desencorajada; exigiria segundo mecanismo de preferência; `apply` não é atômico em falha de processo | Descartada: a spec proíbe duplicar mecanismo e o padrão moderno já está no classpath |
| Tabela de configuração no Room | Um único armazenamento | Migração de esquema para um dado que não é da coleção; leitura mais lenta; contraria FR-416 e a separação preferência × coleção | Descartada |
| Arquivo próprio (JSON/properties) | Controle total | Reimplementa o que o DataStore já faz; risco de corrupção e de escrita não atômica | Descartada |

### 2.2 Mecanismo de troca de tema

| Alternativa | Prós | Contras | Decisão |
|---|---|---|---|
| **Estado Compose + `DiecastTheme(mode)`** (escolhida) | Immediato, sem recriação, sem dependência, mantém o tema existente | Exige sincronizar barras do sistema manualmente | **Escolhida** |
| `AppCompatDelegate.setDefaultNightMode` | Simples, também muda recursos `values-night` | Exige AppCompat (não usado); recria atividades; segundo mecanismo de tema | Descartada |
| `UiModeManager.setApplicationNightMode` | API oficial de modo noturno do app, muda recursos | API 31+ (app tem `minSdk 26`); comportamento divergente entre versões; recria atividade | Descartada para esta feature; registrada como possível evolução se o `minSdk` subir |
| `Configuration`/`createConfigurationContext` manual | Sem libs | Duplica o mecanismo Compose e não atualiza composições | Descartada |

### 2.3 Estratégia para o primeiro quadro

| Alternativa | Prós | Contras | Decisão |
|---|---|---|---|
| **Inicial `SYSTEM`, leitura assíncrona, sem bloquear** (escolhida) | Simples, sem bloqueio (NFR-402), sem segundo mecanismo | Em modo forçado pode haver resíduo de um quadro com a paleta do sistema | **Escolhida**, com medição (≤ 16,7 ms) e limitação registrada |
| `runBlocking` na primeira composição | Zero piscada | Bloqueia a thread principal; viola NFR-402/IV.6 | Descartada |
| Splash retendo até a leitura | Zero piscada | Exige `core-splashscreen` (dependência nova) e atraso artificial; R.4/R.13 | Descartada |
| Cache síncrono da última paleta em `SharedPreferences` só para abertura | Zero piscada | Dois armazenamentos para o mesmo valor; risco de divergência | Descartada |

### 2.4 Onde colocar a UI

| Alternativa | Prós | Contras | Decisão |
|---|---|---|---|
| **Seção dentro de Ajustes** (escolhida) | Sem rota nova; descoberta óbvia; mantém V.10 da Spec 005 | Nenhum relevante | **Escolhida** |
| Tela dedicada «Aparência» | Espaço para futuras opções (paleta, tamanho) | Rota nova, item novo de navegação, mais superfícies para testar sem necessidade atual | Descartada |
| Diálogo a partir do cartão de Ajustes | Poucos toques | Padrão da tela hoje é entrada → tela; diálogo esconderia as opções | Descartada |

### 2.5 Controle de seleção

`RadioButton` + `Modifier.selectable(role = Role.RadioButton)` dentro de `Column.selectableGroup()`: semântica correta para TalkBack, alvo de 48 dp, estado anunciado, funciona com fonte 200% e quebra de linha. `FlowChipGroup` foi descartado porque um grupo de opções exclusivas e persistentes pede semântica de rádio, não de chip (o componente existente é para filtros/atributos).

## 3. Decisões

| ID | Decisão |
|---|---|
| D1 | `ThemeMode` (`SYSTEM`/`LIGHT`/`DARK`) em `core:ui`; resolução pura e única no tema |
| D2 | DataStore Preferences na camada de aplicação, com `@Singleton` Hilt; registrada em `docs/adr-006-preferencia-aparencia.md` (R.4) |
| D3 | Chave textual `appearance_mode` com `system`/`light`/`dark`; desconhecido ⇒ `system` |
| D4 | `MainActivity` observa o modo, alimenta `DiecastTheme` e o estilo das barras do sistema |
| D5 | Seção **Aparência** em Ajustes com seleção única (rádio), dentro da `FormColumn` |
| D6 | Fundo de janela por `values`/`values-night`; modo forçado medido e documentado |
| D7 | Modo escuro permanece exatamente como hoje; claro usa tokens existentes |
| D8 | Backup/restauração não tocam na preferência; nenhuma migração de banco |

## 4. Dúvidas remanescentes

1. **Contraste renderizado sobre foto**: a amostragem de pixels com `captureToImage()` precisa de regiões determinísticas e tolerância. O plano usa pior caso controlado (foto branca e foto preta) em superfície de teste, além de capturas do app real. Se a amostragem se mostrar instável no emulador por software, a tarefa deve registrar a limitação e manter a asserção sobre os pares declarados — **sem** declarar que elementos renderizados foram verificados.
2. **Reinício do aparelho**: não é automatizável no emulador de forma confiável; fica como evidência manual com comando registrado. Não será apresentado como teste.
3. **Esclarecimento da Spec 003**: recomendação redigida em `spec.md`; não foi aplicada para respeitar a restrição de não alterar specs anteriores. Não bloqueia a implementação.
