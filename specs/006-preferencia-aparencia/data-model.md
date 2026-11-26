# Data Model: Preferência de aparência

**Feature**: `006-preferencia-aparencia` | **Data**: 2026-11-25

Este modelo descreve **apenas** a preferência de apresentação. Nenhuma entidade de domínio, tabela Room, migração ou repositório da coleção é criada ou alterada.

## 1. Valor persistido

| Campo | Chave | Tipo | Obrigatório | Padrão | Domínio de valores |
|---|---|---|---|---|---|
| Modo de aparência | `appearance_mode` | String | não (ausência = padrão) | `system` | `system`, `light`, `dark` |

**Armazenamento**: DataStore Preferences, arquivo próprio do app (diretório privado), fora do banco da coleção.

**Por que String e não enum serializado**: um valor textual estável permite que uma versão futura leia o arquivo sem depender de ordinal/`name` de enum, e torna o fallback de valores desconhecidos trivial. A conversão para `ThemeMode` acontece na borda (store).

### Codificação

| `ThemeMode` | Valor gravado |
|---|---|
| `SYSTEM` | `system` |
| `LIGHT` | `light` |
| `DARK` | `dark` |

### Decodificação tolerante

| Entrada | Resultado |
|---|---|
| chave ausente | `SYSTEM` |
| `system` | `SYSTEM` |
| `light` | `LIGHT` |
| `dark` | `DARK` |
| qualquer outro texto (incluindo vazio, caixa diferente, valor de versão futura) | `SYSTEM` |
| falha de leitura do arquivo | `SYSTEM` (emitido no `catch` do fluxo), sem exceção para a UI |

A decodificação é uma função pura, testável sem Android.

## 2. Estado em memória

```text
ThemeMode            (SYSTEM | LIGHT | DARK)   ← preferência
systemInDark: Bool   (do Android)              ← só consultado em SYSTEM
dark: Bool           (paleta resolvida)        ← LightColors | DarkColors
```

**Invariantes**

- I-1: existe um único `ThemeMode` por instalação.
- I-2: `dark == false` para `LIGHT` e `dark == true` para `DARK`, independentemente do Android.
- I-3: `dark == systemInDark` quando o modo é `SYSTEM`, reavaliado a cada mudança de configuração.
- I-4: o valor exibido como selecionado em Ajustes é o mesmo `ThemeMode` aplicado ao tema (sem cópia divergente).
- I-5: o valor persistido é sempre um dos três canônicos; nunca uma parcial ou um estado intermediário.
- I-6: a preferência não é dado da coleção — não entra em backup/exportação e não é alterada por restauração.

## 3. Transições

| Estado atual | Evento | Próximo estado | Efeito |
|---|---|---|---|
| ausente | primeira leitura | `SYSTEM` | paleta segue o Android |
| `SYSTEM` | usuário escolhe **Claro** | `LIGHT` | grava `light`; aplica paleta clara |
| `SYSTEM` | usuário escolhe **Escuro** | `DARK` | grava `dark`; aplica paleta escura |
| `LIGHT` | usuário escolhe **Sistema** | `SYSTEM` | grava `system`; aplica paleta do Android agora |
| `LIGHT` | Android muda para escuro | `LIGHT` (inalterado) | nada muda no app |
| `SYSTEM` | Android muda para escuro | `SYSTEM` (inalterado) | paleta escura em ≤ 1 s |
| qualquer | falha de gravação | estado anterior | aviso não bloqueante em pt-BR; tema atual mantido |
| qualquer | arquivo ilegível | `SYSTEM` | app continua utilizável |
| qualquer | restauração de backup | inalterado | pacote não contém a preferência |

## 4. Persistência e ciclo de vida

| Situação | Comportamento esperado |
|---|---|
| Processo encerrado e reaberto | valor lido do DataStore; aplicado assim que a primeira emissão chega |
| Reinício do aparelho | mesmo comportamento (arquivo privado persiste) |
| Atualização `install -r` preservando dados | mesmo comportamento |
| Desinstalação | preferência removida com os dados do app |
| Backup do sistema Android (Auto Backup) | pode preservar o arquivo; não é requisito desta feature e não faz parte do pacote manual da coleção |
| Troca de aparência durante edição/captura | nenhum rascunho, arquivo ou parâmetro é perdido |

## 5. O que **não** muda

- Entidades `Modelo`, `Variante`, `Exemplar` e demais tabelas Room.
- `AppDatabase`, versão de esquema e migrações.
- Repositórios e casos de uso.
- Pacote de backup (`FileBackupService`) e formato do manifesto.
- `applicationId`, `app_name`, rotas e destinos de navegação.
