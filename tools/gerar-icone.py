#!/usr/bin/env python3
"""Gera os recursos de ícone do app a partir da arte original.

Arte: docs/icone-novo.png (quadrada, com moldura prateada).
- PNGs legados em mipmap-<densidade>/ic_launcher.png, com a arte inteira.
- Camada de frente do ícone adaptativo em drawable-nodpi/ic_launcher_foreground.png:
  a arte ocupa 56dp do canvas de 108dp, dentro da área visível de 72dp, para a
  moldura aparecer inteira mesmo com máscara circular. O preenchimento preto ao
  redor se funde ao fundo sólido definido em @color/ic_launcher_background.
"""

from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent
ORIGEM = RAIZ / "docs" / "icone-novo.png"
RES = RAIZ / "app" / "src" / "main" / "res"

DENSIDADES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# 108dp de camada adaptativa em xxxhdpi; 72dp é a área visível.
CANVAS = 432
# 56dp deixa a moldura inteira dentro da máscara circular (a 60dp as quinas
# são cortadas) e ainda preenche bem a área visível de 72dp.
ARTE_DP = 56
ARTE = CANVAS * ARTE_DP // 108


def main() -> None:
    arte = Image.open(ORIGEM).convert("RGB")

    for densidade, lado in DENSIDADES.items():
        destino = RES / f"mipmap-{densidade}"
        destino.mkdir(parents=True, exist_ok=True)
        arte.resize((lado, lado), Image.LANCZOS).save(destino / "ic_launcher.png")

    frente = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    reduzida = arte.resize((ARTE, ARTE), Image.LANCZOS).convert("RGBA")
    frente.paste(reduzida, ((CANVAS - ARTE) // 2, (CANVAS - ARTE) // 2))

    destino = RES / "drawable-nodpi"
    destino.mkdir(parents=True, exist_ok=True)
    frente.save(destino / "ic_launcher_foreground.png", optimize=True)


if __name__ == "__main__":
    main()
