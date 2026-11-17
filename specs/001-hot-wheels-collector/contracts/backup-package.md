# Contrato — Pacote de backup e exportação integral

**Requisitos**: FR-140 a FR-145, FR-142, R.6 · **Modelo**: [`../data-model.md`](../data-model.md)

O pacote é a garantia de que a coleção sobrevive ao aplicativo. Ele é autocontido, verificável e legível fora do app, sem depender de serviço externo.

## Estrutura do arquivo

```text
hotwheels-collector-backup-YYYYMMDD-HHmmss.zip
├── manifest.json
├── data/
│   ├── manufacturers.json
│   ├── brands.json
│   ├── real_vehicles.json
│   ├── car_models.json
│   ├── series.json
│   ├── collections.json
│   ├── collection_items.json
│   ├── colors.json
│   ├── variants.json
│   ├── variant_secondary_colors.json
│   ├── exemplars.json
│   ├── photos.json
│   ├── acquisitions.json
│   ├── valuations.json
│   ├── wishlist_items.json
│   ├── negotiations.json
│   ├── negotiation_events.json
│   ├── locations.json
│   ├── movements.json
│   ├── recognition_results.json
│   └── settings.json
├── images/
│   └── <exemplar_id>/<photo_id>.jpg
└── README.txt
```

- `data/*.json`: um array JSON por tabela, UTF-8, chaves iguais aos nomes de coluna do modelo (`price_cents`, `date`, …).
- `images/`: cópia dos arquivos referenciados por `photos.path`, já sem EXIF sensível.
- `README.txt`: explica em português como abrir os arquivos sem o aplicativo — parte do compromisso de não prender o colecionador (Art. VI.7).

## manifest.json

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `format_version` | inteiro | sim | versão deste contrato (inicia em `1`) |
| `schema_version` | inteiro | sim | versão do banco Room de origem |
| `app_version` | texto | sim | `versionName` + `versionCode` |
| `created_at` | texto | sim | ISO-8601 UTC |
| `created_on_device` | texto | não | modelo do aparelho |
| `counts` | objeto | sim | contagem por tabela e total de imagens |
| `total_bytes` | inteiro | sim | soma dos arquivos descompactados |
| `files` | array | sim | `{ "path", "bytes", "sha256" }` para cada arquivo, inclusive imagens |
| `manifest_sha256` | texto | não | hash do conteúdo semântico, para conferência rápida |

## Validação antes de restaurar (FR-143)

1. O arquivo é ZIP legível e contém `manifest.json`.
2. `format_version` é conhecida e suportada (senão: recusar com mensagem clara).
3. Todo arquivo listado em `files` existe no pacote e tem o mesmo tamanho.
4. O SHA-256 de cada arquivo confere.
5. As contagens declaradas coincidem com o número de registros lidos.
6. As FKs internas resolvem (exemplar → variante → modelo; foto → exemplar).

Qualquer falha interrompe a restauração **sem gravar nada**.

## Modos de restauração

| Modo | Comportamento | Quando usar |
|---|---|---|
| **Substituir** | Apaga o banco atual e grava o backup, após confirmação dupla e oferta de exportação prévia (FR-163) | primeiro uso em aparelho novo |
| **Mesclar** | Mantém os dois lados; IDs iguais são comparados campo a campo e o usuário decide por item (manter atual, usar backup, manter ambos com novo ID) | fusão de duas instalações |

O modo mesclar nunca sobrescreve silenciosamente (FR-144) e registra cada decisão em `history_entry`.

## Exportação integral (FR-142)

A exportação "dados abertos" usa o mesmo pacote, com uma diferença: inclui `CSV/` com os mesmos campos em planilhas por tabela, para leitura fora do ecossistema do app. Imagens continuam em `images/`.

## Garantias

- Nenhum segredo, token ou credencial entra no pacote (não existem no app).
- Senhas não se aplicam: o app não tem conta (A1).
- Backup de 1.000 exemplares e 2.000 fotos conclui em menos de 5 minutos (SC-013), com progresso visível e possibilidade de cancelar sem corromper o arquivo gerado.
- O pacote é gerado em streaming; nunca é montado inteiro em memória (risco de OOM em coleções grandes).
- Restauração é transacional: ou o banco resultante está completo e íntegro, ou permanece exatamente como estava.
- Android Auto Backup cobre apenas banco e preferências (cota ~25 MB); fotos só existem no pacote manual — o app avisa isso na tela de backup.
