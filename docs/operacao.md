# Operação — Diecast Collector

Guia de instalação, backup, restauração e solução de problemas da **v1 de uso pessoal**
(0.1.0, Android 8.0 / API 26 ou superior). Ele descreve o aplicativo como ele está, sem
prometer importação de planilha, relatórios em PDF/XLSX ou reconhecimento por câmera —
esses blocos estão fora do escopo desta v1 (ver `specs/001-diecast-collector/tasks.md`,
seção «Revisão de fechamento»).

> **Regra de ouro:** o acervo vive num único banco SQLite dentro do aparelho. Antes de
> desinstalar, trocar de celular ou limpar os dados do app, gere um **pacote de backup** e
> guarde-o fora do aparelho.

## 1. Onde os dados ficam

| O quê | Onde |
|---|---|
| Coleção (modelos, variantes, exemplares, fotos, finanças, desejos, coleções, localizações) | banco privado do app, `databases/diecast.db` |
| Fotos | `files/images/<idDoExemplar>/` no armazenamento privado do app |
| Lembrete de backup (último pacote e intervalo) | `SharedPreferences` do app, fora do banco |

O pacote de release usa o pacote `br.com.diecastcollector`; o de debug,
`br.com.diecastcollector.debug`. **São dois apps distintos** — instalar um não atualiza o
outro e os dados de um não aparecem no outro sem backup/restauração.

O app **não faz chamadas de rede**: nenhum fluxo lê ou envia dados pela internet. O
manifesto final, porém, herda permissões das bibliotecas: `INTERNET` de
`com.google.android.datatransport:transport-backend-cct` (transporte do ML Kit) e
`ACCESS_NETWORK_STATE` de `androidx.work:work-runtime`. A revisão que comprova a ausência
de tráfego é a T139, ainda aberta. A câmera só é usada quando você escolhe tirar uma foto;
a galeria usa o seletor do sistema (Photo Picker), sem permissão ampla de armazenamento.

## 2. Instalação

### 2.1 APK de release (uso normal)

1. Copie `app-release.apk` para o aparelho (cabo, nuvem, cartão).
2. Abra o arquivo e permita «instalar apps desconhecidos» para o app usado na abertura
   (Arquivos, navegador etc.).
3. Confirme a instalação.

Por `adb`, com o aparelho conectado e depuração USB ativa:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

O APK de release é assinado com a chave local (`keystore/release.jks` +
`keystore.properties`, ambos fora do versionamento). **Guarde uma cópia dos dois e da
senha**: é essa chave que permite instalar uma versão nova por cima, preservando os dados.
Sem esses arquivos, o build gera `app-release-unsigned.apk`, que o Android não instala.

A chave existe nesta máquina desde 2026-11-23 (`CN=Diecast Collector`, alias `diecast`,
SHA-256 `a7daef55…484f`) e a senha está somente em `keystore.properties`. Ela não é a
mesma chave citada em `specs/001-diecast-collector/tasks.md` (T136): aparelho que já tem o
app assinado pela outra chave precisa desinstalar antes de receber um APK gerado aqui.

### 2.2 APK de debug (desenvolvimento)

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O debug inclui as ABIs x86/x86_64, necessárias em emulador; o release é filtrado para
`arm64-v8a` e `armeabi-v7a`.

### 2.3 Atualizar sem perder a coleção

1. Gere um backup no app que está instalado (seção 3).
2. Instale o APK novo **com a mesma chave e o mesmo `applicationId`**.\
   Trocar de debug para release (ou vice-versa) conta como outro app: exporte no antigo,
   instale o novo e restaure lá.
3. Se a instalação reclamar de assinatura ou versão incompatível, veja a seção 7.

O banco tem migrações versionadas e **nenhuma migração destrutiva**: uma atualização que
não consiga migrar aborta e mantém o banco anterior intacto, em vez de recriar as tabelas.

## 3. Backup

### 3.1 Fazer o pacote

**Ajustes → Backup e restauração → Criar backup**. O seletor do sistema pede onde salvar
(nome sugerido `colecao-backup-<data>.zip`). Ao final, o app mostra data, contagens
(exemplares e fotos) e tamanho do arquivo.

O que vai no ZIP:

- `data/diecast.db` — o banco SQLite completo (snapshot após checkpoint do WAL);
- `images/...` — as fotos referenciadas por `photo.path`;
- `data/<tabela>.json` e `CSV/<tabela>.csv` — a mesma informação em formato aberto, uma
  linha por registro, para abrir em planilha ou editor de texto;
- `manifest.json` — versões, contagens, `missing_images` e o SHA-256 de cada arquivo;
- `README.txt` — o mesmo roteiro de restauração, dentro do pacote.

O pacote **não tem senha** e não sai do aparelho sozinho: copie-o para um computador, um
cartão ou uma nuvem de sua escolha. Guarde mais de uma cópia em locais diferentes.

Se alguma foto estiver ausente do disco, o pacote é criado mesmo assim, e a tela avisa
quantas ficaram de fora (`missing_images`). O resto do acervo continua íntegro.

O backup roda em segundo plano pelo WorkManager: você pode sair da tela ou trocar de app e
voltar depois para ver o resultado. No Android 13 ou mais novo, se a permissão de
notificações não tiver sido concedida, a notificação de andamento pode não aparecer — o
backup continua mesmo assim.

### 3.2 Lembrete

Em **Ajustes**, a linha «Lembrete de backup» mostra o intervalo e a situação («em dia»,
«atrasado» ou «nenhum backup»). Ao tocar, ela abre o último backup, o aviso de atraso, a
escolha do intervalo (7, 30, 90 dias ou desligado) e o atalho «Fazer backup agora».

O lembrete em si é **dentro do app**: ele não agenda notificação de sistema. A execução em
segundo plano existe apenas enquanto um backup, restauração ou importação está em andamento. A data considerada é a do último pacote gravado com sucesso — restaurar um
pacote não reescreve essa data de propósito.

### 3.3 Backup automático do Android (não substitui o pacote)

O app permite o Auto Backup do Android, que copia banco, preferências e arquivos, mas
**exclui `images/`** (as fotos passam da cota e só existem por completo no pacote manual).
Ele também depende de o backup do aparelho estar ativo e de uma conta configurada.
Portanto: o Auto Backup pode salvar o catálogo, mas não as fotos; use sempre o pacote
manual para trocar de aparelho.

### 3.4 Abrir o pacote sem o aplicativo

O ZIP é autocontido:

- `data/diecast.db` abre em qualquer leitor de SQLite (por exemplo, DB Browser for
  SQLite);
- `CSV/*.csv` e `data/*.json` abrem em planilha ou editor de texto;
- para conferir a integridade, compare cada arquivo com o SHA-256 listado em
  `manifest.json`. No Linux/macOS: `sha256sum data/diecast.db`. No Windows:
  `certutil -hashfile data\diecast.db SHA256`.

Não edite o ZIP e não o descompacte/recompacte antes de restaurar: a validação confere
tamanho e hash de cada arquivo e recusará o pacote alterado.

## 4. Restauração

**Ajustes → Backup e restauração → Restaurar backup** → escolha o `.zip`.

O app **valida antes de tocar nos dados**: ZIP legível, manifesto presente, versão de
formato conhecida, todo arquivo listado presente com tamanho e SHA-256 corretos,
`PRAGMA integrity_check = ok`, vínculos de FK íntegros e contagens por tabela iguais às do
manifesto. Só depois disso aparece o resumo (data de criação, aparelho de origem,
contagens, tamanho e fotos ausentes, se houver).

1. Confira o resumo. Nada foi gravado ainda.
2. Toque em **Restaurar** — esta é a confirmação explícita que substitui os dados atuais.
3. Ao terminar, toque em **Reabrir o app**. A restauração fecha o banco; o processo novo é
   o que o reabre.

Antes de substituir, o estado atual é guardado no armazenamento privado como
`diecast.db.pre-restore-<data>` e `images.pre-restore-<data>`. Em caso de erro no meio da
gravação, o app faz rollback e mantém o estado anterior. Essas cópias não aparecem na
interface e não são removidas automaticamente.

> **Se a restauração falhar depois de começar**, feche e reabra o app: os dados anteriores
> foram preservados, mas o processo pode estar com o banco fechado. Para tentar de novo,
> abra a tela de backup novamente.

## 5. Exportar tudo e excluir tudo

Em **Ajustes → Dados da coleção**:

- **Exportar tudo** grava o mesmo pacote do backup (banco, fotos, JSON e CSV). É a
  exportação integral em formato aberto; não existe um segundo formato.
- **Excluir todos os dados** apaga do aparelho os registros e as fotos do acervo. A
  exclusão exige **duas confirmações**: a primeira explica o que vai embora e oferece
  **«Exportar antes de excluir»**; a segunda é a definitiva. Não há lixeira nem desfazer.
- Os arquivos que você já exportou **fora** do app não são tocados. O lembrete de backup
  também é preservado: ele se refere a pacotes que continuam existindo.
- Com o acervo vazio, os dois botões ficam desabilitados.

## 6. Trocar de aparelho — roteiro

1. No aparelho antigo: **Ajustes → Backup e restauração → Criar backup**.
2. Copie o ZIP para fora (computador, nuvem, cartão) e confira o tamanho.
3. Instale o app no aparelho novo (mesmo tipo de build: release com release).
4. No aparelho novo: **Ajustes → Backup e restauração → Restaurar backup**, escolha o ZIP,
   confira o resumo e confirme.
5. Toque em **Reabrir o app** e confira no Painel as contagens de exemplares e fotos.
6. Só então desinstale o app do aparelho antigo.

Se você usa o app de release e o de debug na mesma máquina, lembre-se de que são pacotes
diferentes: o backup feito em um restaura no outro, mas a instalação não é uma atualização.

## 7. Solução de problemas

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ao instalar | APK assinado com outra chave ou `applicationId` diferente (debug × release) | Gere um backup no app instalado, desinstale-o e instale o novo; depois restaure. Não desinstale antes do backup. |
| «App não instalado» / instalação bloqueada | Origem desconhecida não permitida, APK corrompido ou Android abaixo de 8.0 | Permita a instalação para o app de arquivos; copie o APK de novo; confira a versão do Android (mínimo 8.0). |
| O app fecha ao abrir depois de atualizar | Migração de banco não aplicada, arquivo do banco danificado ou armazenamento cheio | Reabra o app. Se persistir, **não limpe os dados**: o banco anterior é preservado pela migração; restaure a cópia de backup feita antes da atualização (seção 4). |
| «Não foi possível abrir o arquivo como pacote de backup» | Arquivo não é o ZIP do app, está truncado ou foi recompactado | Escolha o `.zip` original gerado pelo app; não edite nem recompacte. |
| «Este pacote é de uma versão mais nova do app» | Backup feito em versão mais recente | Atualize o app antes de restaurar. |
| «Um arquivo do pacote está corrompido (hash diferente do manifesto)» | Cópia incompleta, arquivo alterado ou mídia com defeito | Recupere outra cópia do pacote (a validação recusa o arquivo justamente para não gravar dados quebrados). |
| «As contagens do pacote não conferem com o banco» | Pacote montado/editado por fora | Use um pacote gerado pelo próprio app. |
| Aviso de fotos ausentes no backup | Alguma foto referenciada não estava no disco (limpeza do sistema, falha antiga) | O backup foi criado sem essas fotos; o restante está íntegro. Reaproveite o pacote mais antigo que tenha a foto ou anexe-a de novo depois de restaurar. |
| Restauração concluída, mas a coleção continua vazia | O resumo não foi confirmado, o arquivo escolhido era de outra instalação, ou você abriu o outro pacote (debug × release) | Repita a restauração confirmando no diálogo; confira o nome do pacote instalado em **Sobre**. |
| Depois de uma restauração falha, as telas não carregam | O banco foi fechado para a troca e o rollback manteve os dados antigos | Feche o app pelo seletor de apps e abra de novo; os dados anteriores estão lá. |
| «Não foi possível gravar a exportação. Nada foi apagado.» | Destino sem permissão, sem espaço ou seletor cancelado | Escolha outro local com espaço livre e repita. A coleção não é alterada por uma exportação. |
| «A exclusão não terminou. Tente de novo…» | Falha ao remover as fotos ou o banco | Tente de novo; se persistir, reinicie o aparelho e repita. Os registros já apagados não voltam, e o que restou aparece na contagem da própria tela. |
| Lembrete diz que o backup está atrasado, mesmo após restaurar | A data do lembrete é a do último pacote **gravado**, não a do pacote restaurado | Faça um backup novo ou ajuste o intervalo em Ajustes. |
| Backup demora ou parece parado | Pacotes grandes (fotos) são gravados com a tela aberta; não há trabalho em segundo plano nesta versão | Mantenha o app em primeiro plano até a mensagem «Backup criado». Não bloqueie a tela no meio. |
| Erro ao anexar foto | Sem espaço no aparelho, imagem inválida ou limite de 1,5 MB por foto após compressão | Libere espaço e salve de novo: a captura pendente é mantida e o exemplar já criado não é duplicado. |
| A busca não encontra um item recém-cadastrado | Índice de busca desatualizado após substituição manual do banco | Feche e reabra o app: o índice é reconstruído na abertura quando a contagem diverge. |
| Não sei onde está o arquivo do backup | O seletor do sistema grava onde você escolheu; o app não mantém uma lista | Procure por `colecao-backup-*.zip` no local escolhido (Arquivos, Downloads, Drive etc.). |
| Perdi a chave de assinatura (`keystore`) | O `release.jks`/`keystore.properties` não estava guardado | Sem a chave não há atualização por cima: restaure o backup, desinstale e instale a versão nova, depois restaure os dados. **A chave desta máquina está em `keystore/release.jks` + `keystore.properties` e precisa de backup** — ela não vem do repositório. |

### Se nada resolver

1. Gere um backup (se o app ainda abrir) e copie-o para fora do aparelho.
2. Anote a versão mostrada em **Ajustes → Sobre** e o aparelho/versão do Android.
3. Guarde o ZIP: ele contém o banco e as fotos, e pode ser aberto sem o app (seção 3.4).

**Não** use «Limpar dados» nem desinstale antes de ter uma cópia: o pacote é a única forma
de reconstruir a coleção com as fotos.
