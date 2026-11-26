# Inventário de Dependências Externas (R.5)

**Data**: 2026-11-20  
**Versão da aplicação**: 0.1.0 (v1 de uso pessoal)  
**Conformidade**: Princípio R.5 e Artigo III.10 da Constituição.

Todas as dependências externas utilizadas no projeto estão listadas com sua finalidade, versão, licença de software, custo financeiro e plano de contingência/substituição.

---

## 1. Bibliotecas de Produção (Runtime)

| Dependência | Versão | Licença | Finalidade | Custo | Plano de Substituição |
|---|---|---|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Apache 2.0 | Extensões Kotlin essenciais para APIs Android | Gratuito | Componente padrão Google Android. Insubstituível, mas de código aberto e padrão da plataforma. |
| `androidx.lifecycle:*` | 2.8.7 | Apache 2.0 | Gerenciamento de ciclo de vida e integração ViewModel no Compose | Gratuito | Componente padrão AndroidX. |
| `androidx.activity:activity-compose` | 1.10.0 | Apache 2.0 | Ponto de entrada da Activity com Jetpack Compose | Gratuito | Padrão do ecossistema Compose. |
| `androidx.navigation:navigation-compose` | 2.8.5 | Apache 2.0 | Navegação tipada entre telas e suporte a `SavedStateHandle` | Gratuito | Substituível por navegação baseada em estado próprio ou Voyager/Decompose se necessário. |
| `androidx.compose.*` (BOM 2025.01.00) | BOM | Apache 2.0 | Framework declarativo de interface de usuário (Material 3) | Gratuito | Core da interface do aplicativo. Código aberto Google. |
| `androidx.compose.material3:material3-window-size-class` | BOM 2025.01.00 | Apache 2.0 | Classificação da janela em compacta/média/expandida a partir do contêiner (spec 005) | Gratuito | Substituível por limiares próprios sobre `BoxWithConstraints`; decisão e alternativas em [`adr-005-window-size-class.md`](./adr-005-window-size-class.md). |
| `com.google.dagger:hilt-android` | 2.53.1 | Apache 2.0 | Injeção de dependências em tempo de compilação | Gratuito | Substituível por Koin ou injeção manual pura via Factory pattern. |
| `androidx.room:*` | 2.6.1 | Apache 2.0 | Camada de persistência relacional SQLite local com DAOs e Flow | Gratuito | Substituível por SQLDelight ou SQLiteOpenHelper nativo do Android SDK. |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Apache 2.0 | Preferências simples do app. Uso real: aparência (tema Sistema/Claro/Escuro) em `app/.../appearance/AppearancePreferences.kt`, com um único dono `@Singleton` em `app/.../di/PreferencesModule.kt` (spec 006, ADR-006). O lembrete de backup permanece em `SharedPreferences`. | Gratuito | Substituível por tabela de configuração no Room ou SharedPreferences nativo. |
| `io.coil-kt:coil-compose` | 2.7.0 | Apache 2.0 | Carregamento assíncrono e exibição de imagens locais | Gratuito | Substituível por Glide ou carregamento manual via `BitmapFactory` + `remember`. |
| `androidx.work:work-runtime-ktx` | 2.10.0 | Apache 2.0 | Agendamento e execução de tarefas em background resilientes | Gratuito | Substituível por `JobScheduler` nativo da plataforma Android. |
| `androidx.hilt:hilt-work` | 1.2.0 | Apache 2.0 | Injeta dependências nos `Worker`s do WorkManager | Gratuito | Substituível por `WorkerFactory` manual, sem Hilt. |
| `androidx.documentfile:documentfile` | 1.0.1 | Apache 2.0 | Operações seguras com Storage Access Framework (SAF) para backup | Gratuito | Substituível por ContentResolver / DocumentContract nativo. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-*` | 1.9.0 | Apache 2.0 | Concorrência estruturada, Flow e assincronismo em Kotlin | Gratuito | Core da linguagem Kotlin. |

---

## 2. Bibliotecas de Teste e Ferramentas (Test & Tooling)

| Dependência | Versão | Licença | Finalidade | Custo | Plano de Substituição |
|---|---|---|---|---|---|
| `junit:junit` | 4.13.2 | EPL 2.0 | Executor de testes unitários | Gratuito | Migrável para JUnit 5. |
| `com.google.truth:truth` | 1.4.4 | Apache 2.0 | Asserções fluentes e legíveis em testes | Gratuito | Substituível por asserções nativas `kotlin.test` ou AssertJ. |
| `org.robolectric:robolectric` | 4.14.1 | Apache 2.0 | Execução de testes Android na JVM sem emulador | Gratuito | Substituível por testes instrumentados rodando em emulador/aparelho físico. |
| `app.cash.turbine:turbine` | 1.2.0 | Apache 2.0 | Testes de fluxos reativos Kotlin `Flow` | Gratuito | Substituível por coleta manual com lista buffer em `runTest`. |
| `androidx.test.espresso:*` | 3.6.1 | Apache 2.0 | Testes instrumentados de interface e interações no dispositivo | Gratuito | Padrão AndroidX Test. |
| `androidx.compose.ui:ui-test-junit4` | BOM | Apache 2.0 | Testes de semântica e nós Compose | Gratuito | Padrão do Compose. |

---

## 3. Plugins de Build

| Plugin | Versão | Licença | Finalidade |
|---|---|---|---|
| `com.android.application` | 8.9.2 | Apache 2.0 | Android Gradle Plugin para geração do APK/AAB |
| `org.jetbrains.kotlin.android` | 2.1.0 | Apache 2.0 | Compilador Kotlin para Android |
| `com.google.devtools.ksp` | 2.1.0-1.0.29 | Apache 2.0 | Kotlin Symbol Processing para Room e Hilt |

---

## 4. Avaliação de Custo e Licenciamento

- **Custo Total**: R$ 0,00 (zero custo financeiro recorrente ou pontual).
- **Compatibilidade de Licenças**: 100% das dependências adotam licenças permissivas e de código aberto (`Apache 2.0` e `EPL 2.0`). Nenhuma dependência utiliza licença copyleft viral (como GPL v3) incompatível com distribuição proprietária ou móvel.
- **Isolamento de Fornecedor**: Nenhuma dependência vincula o aplicativo a serviços em nuvem proprietários pagos (Firebase, AWS, Azure, Google Cloud APIs pagas).
