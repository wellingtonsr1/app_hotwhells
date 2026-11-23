"""Semeia o banco da Coleção com 500 variantes para medir SC-003 (spec 003).

Gera, em `build/benchmark-seed/`:

- `images/seed/pNNN.png` — 450 miniaturas sintéticas de 256×256 (gradiente + ruído), para
  que o Coil tenha o que decodificar de verdade;
- `seed.sql` — 500 variantes, cada uma com um exemplar e a foto representativa, na
  proporção de SC-005: 450 com foto válida, 25 com arquivo ausente e 25 sem foto.

Uso: `python tools/benchmark_seed.py` e depois aplicar `seed.sql` e `images.tar` no app
(vide `specs/003-colecao-miniaturas/quickstart.md`).
"""

from __future__ import annotations

import random
import struct
import zlib
from pathlib import Path

SIZE = 256
VARIANTS = 500
WITH_PHOTO = 450
WITH_MISSING_FILE = 25
ROOT = Path(__file__).resolve().parent.parent / "build" / "benchmark-seed"
TS = 1_700_000_000_000


def png(path: Path, seed: int) -> None:
    """PNG RGB 256×256: gradiente entre duas cores sorteadas, com ruído leve."""
    rnd = random.Random(seed)
    start = [rnd.randrange(256) for _ in range(3)]
    end = [rnd.randrange(256) for _ in range(3)]
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)  # filtro "None"
        for x in range(SIZE):
            t = (x + y) / (2 * SIZE - 2)
            noise = rnd.randrange(12) - 6
            for channel in range(3):
                value = start[channel] * (1 - t) + end[channel] * t + noise
                raw.append(int(value) % 256)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 6))
        + chunk(b"IEND", b"")
    )


def sql_literal(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def main() -> None:
    images = ROOT / "images" / "seed"
    images.mkdir(parents=True, exist_ok=True)

    for index in range(WITH_PHOTO):
        png(images / f"p{index:03d}.png", index)

    lines: list[str] = [
        "-- Semente de SC-003: 500 variantes na Coleção (spec 003).",
        "PRAGMA foreign_keys=OFF;",
        "BEGIN;",
    ]

    colors = ["Vermelho", "Azul", "Verde", "Preto", "Branco"]
    for position, color in enumerate(colors):
        lines.append(
            "INSERT INTO color (id, name, hex) VALUES "
            f"({sql_literal(f'c{position}')}, {sql_literal(color)}, NULL);"
        )
    lines.append(
        "INSERT INTO manufacturer (id, name, country, archived_at, created_at, updated_at) "
        f"VALUES ('mfr1', 'Hot Wheels', 'EUA', NULL, {TS}, {TS});"
    )

    for index in range(VARIANTS):
        variant_id = f"v{index:03d}"
        model_id = f"m{index:03d}"
        exemplar_id = f"e{index:03d}"
        # A cada sete, um nome longo: o cartão precisa cortar em duas linhas (FR-014).
        name = f"Modelo {index:03d}" + (" — Edição Especial de Colecionador" if index % 7 == 0 else "")
        color_id = f"c{index % len(colors)}"
        created = TS + index

        lines.append(
            "INSERT INTO car_model (id, manufacturer_id, name, scale, brand_id, "
            "real_vehicle_id, body_type, material, country_of_manufacture, notes, "
            "archived_at, created_at, updated_at) VALUES "
            f"({sql_literal(model_id)}, 'mfr1', {sql_literal(name)}, '1:64', NULL, NULL, "
            f"NULL, NULL, NULL, NULL, NULL, {created}, {created});"
        )
        lines.append(
            "INSERT INTO variant (id, model_id, fingerprint, series_id, primary_color_id, "
            "wheels_type, base_type, decoration, release_year, collection_number, "
            "series_number, edition, manufacturer_code, packaging_type, archived_at, "
            "created_at, updated_at) VALUES "
            f"({sql_literal(variant_id)}, {sql_literal(model_id)}, "
            f"{sql_literal(f'fp-{index:03d}')}, NULL, {sql_literal(color_id)}, NULL, NULL, "
            f"NULL, {2024 - index % 10}, NULL, NULL, NULL, {sql_literal(f'HW{index:03d}')}, "
            f"NULL, NULL, {created}, {created});"
        )
        lines.append(
            "INSERT INTO exemplar (id, variant_id, internal_code, condition, "
            "packaging_condition, location_id, status, notes, row_version, created_at, "
            "updated_at) VALUES "
            f"({sql_literal(exemplar_id)}, {sql_literal(variant_id)}, "
            f"{sql_literal(f'HW-{index:05d}')}, 'LOOSE', NULL, NULL, 'OWNED', NULL, 0, "
            f"{created}, {created});"
        )

        if index < WITH_PHOTO:
            path = f"images/seed/p{index:03d}.png"
        elif index < WITH_PHOTO + WITH_MISSING_FILE:
            # Caminho registrado sem arquivo no disco: exercita o marcador (FR-012).
            path = f"images/seed/ausente{index:03d}.png"
        else:
            path = None  # 10% das variantes sem foto (SC-005)

        if path is not None:
            lines.append(
                "INSERT INTO photo (id, exemplar_id, angle, path, checksum, is_primary, "
                "sort_order, width, height, bytes, captured_at) VALUES "
                f"({sql_literal(f'p{index:03d}')}, {sql_literal(exemplar_id)}, 'FRONT', "
                f"{sql_literal(path)}, {sql_literal('seed')}, 1, 0, {SIZE}, {SIZE}, 0, "
                f"{created});"
            )

    lines.append("COMMIT;")
    (ROOT / "seed.sql").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{VARIANTS} variantes, {WITH_PHOTO} com foto, {WITH_MISSING_FILE} sem arquivo")
    print(f"Artefatos em {ROOT}")


if __name__ == "__main__":
    main()
