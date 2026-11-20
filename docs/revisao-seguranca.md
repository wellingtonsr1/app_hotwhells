# Revisão de Segurança e Privacidade — v1

**Data da revisão**: 2026-11-20  
**Escopo**: Módulos `:app`, `:core:domain`, `:core:data`, `:core:ui`  
**Conformidade**: Artigo III da Constituição (v1.2.0), Requisitos FR-156 a FR-163 e Critério de Sucesso SC-009.

---

## 1. Resumo Executivo

O aplicativo **Hot Wheels Collector** foi concebido e auditado como um aplicativo **100% offline-first**, de armazenamento exclusivamente local no dispositivo móvel. Esta auditoria comprova formalmente que:
1. **Zero tráfego de rede**: nenhuma chamada HTTP/HTTPS ou socket de rede existe no código-fonte.
2. **Ausência de permissões de rede**: o manifesto final do APK (`release` e `debug`) removeu explicitamente as permissões `INTERNET` e `ACCESS_NETWORK_STATE` via `tools:node="remove"`. O sistema operacional Android bloqueia fisicamente qualquer tentativa de comunicação externa.
3. **Zero segredos**: nenhuma chave de API, senha, token de autenticação ou credencial está embutida no repositório ou artefatos gerados.
4. **Privacidade e Minimização**: zero telemetria, zero bibliotecas de analytics/crash reporting (Firebase, Sentry, Mixpanel, etc.).
5. **Isolamento de dados e LGPD**: os dados do banco SQLite e as fotografias residem exclusivamente no armazenamento privado da aplicação. Há controle total do usuário sobre seus dados, com exportação integral aberta e exclusão com dupla confirmação (`DataControlScreen`).

---

## 2. Auditoria de Rede e Comunicação Externa (SC-009 / Art. III)

### 2.1 Análise de Código-Fonte
Busca exaustiva por classes e clientes HTTP/Socket no código:
- **Padrões verificados**: `HttpURLConnection`, `okhttp3`, `java.net.URL`, `WebView`, `HttpClient`, `Retrofit`, `Volley`.
- **Resultado**: `0` ocorrências encontradas em todos os módulos (`:app`, `:core:data`, `:core:domain`, `:core:ui`).

### 2.2 Permissões no Manifesto Final (`AndroidManifest.xml`)
No manifesto principal (`app/src/main/AndroidManifest.xml`), as permissões transitivas foram expressamente removidas:
```xml
<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
```

A inspeção do manifesto compilado final (`app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`) atesta:
- `android.permission.INTERNET`: **NÃO PRESENTE** (bloqueado).
- `android.permission.ACCESS_NETWORK_STATE`: **NÃO PRESENTE** (bloqueado).
- **Veredito SC-009**: **VERIFICADO INTEGRALMENTE**. Nenhum byte é transmitido ou recebido pela aplicação.

---

## 3. Gestão de Segredos e Credenciais (Art. III.2)

- **Verificação**: Varredura por termos `api_key`, `secret`, `password`, `auth_token`, `bearer` no código-fonte e recursos.
- **Resultado**: `0` credenciais ou chaves presentes.
- **Assinatura de Release**: O arquivo de chave `keystore/release.jks` e o arquivo de propriedades `keystore.properties` estão devidamente incluídos no `.gitignore`, não sendo versionados no repositório git.

---

## 4. Permissões de Dispositivo e Privacidade (FR-156 a FR-161)

| Permissão | Finalidade | Como é solicitada |
|---|---|---|
| `android.permission.CAMERA` | Captura de fotos dos exemplares | Solicitada em tempo de execução (`runtime permission`) apenas no clique do botão de câmera |
| *Armazenamento/Galeria* | Seleção de fotos existentes | Usa o **Photo Picker** moderno do sistema Android (`ActivityResultContracts.PickVisualMedia`), que **dispensa permissões amplas de leitura de disco** (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`) |

Nenhuma permissão intrusiva (como localização precisa, contatos, microfone ou ID do telefone) é solicitada ou declarada.

---

## 5. Armazenamento e Integridade Local (Art. III.7, FR-159)

1. **Localização do Banco de Dados**:
   - `databases/hotwheels.db` no armazenamento interno privado do aplicativo (`context.getDatabasePath`).
   - Inacessível por outros aplicativos sem permissão root.
2. **Localização das Fotos**:
   - `files/images/<exemplarId>/` no armazenamento interno privado do aplicativo (`context.filesDir`).
   - Remoção de metadados EXIF sensíveis (incluindo geolocalização GPS) no salvamento via `ImageStore`.
3. **Controle de Dados pelo Titular (LGPD / FR-157 / FR-163)**:
   - A tela `DataControlScreen` (T146) disponibiliza:
     - Exportação integral aberta (banco + imagens + manifesto em arquivo ZIP).
     - Exclusão total dos dados com **dupla confirmação dialogada**, exigindo confirmação explícita do usuário antes de expurgar o banco e diretórios de arquivos.

---

## 6. Telemetria e Rastreamento (Art. III.8 / FR-162)

- O arquivo `gradle/libs.versions.toml` e as dependências Gradle foram inspecionados:
  - Nenhuma biblioteca de rastreamento, métricas de engajamento ou analytics está presente.
  - O aplicativo opera com 100% de sigilo em relação aos padrões de uso do usuário.

---

## 7. Conclusão da Revisão

O Hot Wheels Collector cumpre rigorosamente todos os preceitos do **Artigo III da Constituição**, do requisito **SC-009** e dos requisitos **FR-156 a FR-163**, sendo classificado como uma aplicação **segura, estritamente local e com privacidade garantida por desenho**.
