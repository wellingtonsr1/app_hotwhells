# Revisão de Vulnerabilidades de Dependências (Art. III.10)

**Data da análise**: 2026-11-20  
**Versão avaliada**: v1 (0.1.0)  
**Conformidade**: Artigo III.10 da Constituição.

---

## 1. Verificação Automatizada no Build

O projeto utiliza o **Android Lint** com verificação ativa de dependências embutida no ciclo de compilação:
- Configuração em `app/build.gradle.kts`:
  ```kotlin
  lint {
      warningsAsErrors = false
      abortOnError = true
      checkDependencies = true
  }
  ```
- O build de release executa `:app:lintRelease` (ou `:app:lint`), que inspeciona o grafo de dependências transitivas e falha caso sejam detectadas violações de segurança conhecidas.

---

## 2. Auditoria das Dependências Atuais

| Dependência | Versão | CVEs Críticos Conhecidos | Situação |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Nenhum | Conforme |
| `androidx.lifecycle:*` | 2.8.7 | Nenhum | Conforme |
| `androidx.activity:activity-compose` | 1.10.0 | Nenhum | Conforme |
| `androidx.navigation:navigation-compose` | 2.8.5 | Nenhum | Conforme |
| `androidx.compose.*` | BOM 2025.01.00 | Nenhum | Conforme |
| `androidx.compose.material3:material3-window-size-class` | BOM 2025.01.00 | Nenhum | Conforme |
| `com.google.dagger:hilt-android` | 2.53.1 | Nenhum | Conforme |
| `androidx.room:*` | 2.6.1 | Nenhum | Conforme |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Nenhum | Conforme |
| `io.coil-kt:coil-compose` | 2.7.0 | Nenhum | Conforme |
| `androidx.work:work-runtime-ktx` | 2.10.0 | Nenhum | Conforme |
| `androidx.documentfile:documentfile` | 1.0.1 | Nenhum | Conforme |
| `kotlinx-coroutines-*` | 1.9.0 | Nenhum | Conforme |

Nenhuma dependência em uso possui vulnerabilidade de severidade Alta ou Crítica registrada nas bases NVD (National Vulnerability Database) ou GitHub Security Advisory.

---

## 3. Política de Bloqueio de Release

Em conformidade com o Artigo III.10 da Constituição:
> *Art. III.10. Dependências de terceiros MUST ser verificadas quanto a vulnerabilidades conhecidas antes de entrar em produção; vulnerabilidade crítica sem correção disponível MUST bloquear a entrega.*

Qualquer versão de release com vulnerabilidade crítica identificada deve ser imediatamente bloqueada, devendo a dependência ser:
1. Atualizada para uma versão corrigida no `gradle/libs.versions.toml`; OU
2. Substituída por biblioteca equivalente conforme o plano em [docs/dependencias.md](file:///home/wellington/IA/app_hotwhells/docs/dependencias.md); OU
3. Removida, caso a funcionalidade afetada seja dispensável.
