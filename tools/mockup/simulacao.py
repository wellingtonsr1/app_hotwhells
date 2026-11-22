#!/usr/bin/env python3
"""Gera a simulação em vídeo do app pronto (maquete animada, não gravação de tela).

Desenha cada tela com PIL e codifica com ffmpeg. As telas seguem as telas e as
strings reais do projeto (app/src/main/res/values/strings.xml) e a paleta de
core/ui/.../theme/Color.kt; o painel e o backup seguem US3/US10 da spec.

Uso: python3 tools/mockup/simulacao.py
Saída: docs/simulacao-app.mp4 e docs/simulacao-app-poster.png
"""

import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, ImageFont

S = 2                        # 1 dp -> 2 px
W, H = 360 * S, 640 * S      # tela em dp
FPS = 30
FONT_DIR = "/usr/share/fonts/truetype/dejavu"

BG = (18, 18, 18)
SURFACE = (30, 30, 30)
SURFACE2 = (44, 44, 44)
LINE = (58, 58, 58)
TEXT = (237, 237, 237)
MUTED = (158, 158, 158)
RED = (211, 36, 43)
ORANGE = (242, 107, 33)
AMBER = (178, 106, 0)
GREEN = (46, 125, 50)
WHITE = (255, 255, 255)

_fonts = {}


def font(dp, bold=False):
    key = (dp, bold)
    if key not in _fonts:
        name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
        _fonts[key] = ImageFont.truetype(os.path.join(FONT_DIR, name), int(dp * S))
    return _fonts[key]


def ease(p):
    p = max(0.0, min(1.0, p))
    return p * p * (3 - 2 * p)


def seg(p, start, end):
    if end <= start:
        return 1.0 if p >= end else 0.0
    return ease((p - start) / (end - start))


def canvas():
    img = Image.new("RGB", (W, H), BG)
    return img, ImageDraw.Draw(img)


def rect(d, x, y, w, h, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(
        [x * S, y * S, (x + w) * S, (y + h) * S],
        radius=r * S,
        fill=fill,
        outline=outline,
        width=int(width * S),
    )


def text(d, x, y, s, size, bold=False, color=TEXT, anchor="la"):
    d.text((x * S, y * S), s, font=font(size, bold), fill=color, anchor=anchor)


def width_of(s, size, bold=False):
    return font(size, bold).getlength(s) / S


def status_bar(d, color=TEXT):
    text(d, 16, 8, "9:41", 11, True, color)
    for i in range(3):
        rect(d, 300 + i * 4, 15 - i * 3, 3, 4 + i * 3, 1, color)
    rect(d, 317, 10, 18, 8, 2, None, color, 1)
    rect(d, 320, 12.5, 12, 3, 1, color)


def appbar(d, title, back=False, action=None):
    if back:
        d.line([(28 * S, 27 * S), (20 * S, 33 * S)], fill=TEXT, width=int(2 * S))
        d.line([(28 * S, 39 * S), (20 * S, 33 * S)], fill=TEXT, width=int(2 * S))
        tx = 38
    else:
        tx = 16
    text(d, tx, 25, title, 19, True)
    if action:
        text(d, 344, 30, action, 11, True, ORANGE, anchor="rm")


def field(d, x, y, w, label, value, active=False, caret=False):
    text(d, x, y - 12, label, 9.5, False, MUTED if not active else ORANGE)
    rect(d, x, y, w, 34, 8, SURFACE2 if not active else (52, 40, 34),
         ORANGE if active else LINE, 1.2)
    shown = value
    cw = width_of(shown, 12.5)
    text(d, x + 10, y + 10.5, shown, 12.5, False, TEXT)
    if caret:
        d.line([((x + 11 + cw) * S, (y + 10) * S), ((x + 11 + cw) * S, (y + 24) * S)],
               fill=ORANGE, width=int(1.5 * S))


def chip(d, x, y, label, selected=False, small=False):
    size = 10.5 if small else 11.5
    w = width_of(label, size, selected) + 22
    h = 24 if small else 27
    rect(d, x, y, w, h, h / 2,
         ORANGE if selected else None,
         None if selected else LINE, 1.2)
    text(d, x + w / 2, y + h / 2, label, size, selected,
         (26, 16, 8) if selected else MUTED, anchor="mm")
    return w


def button(d, x, y, w, label, fill=RED, fg=WHITE, h=44):
    rect(d, x, y, w, h, h / 2, fill)
    text(d, x + w / 2, y + h / 2, label, 13, True, fg, anchor="mm")


def car(d, x, y, w, h, color, kind="profile"):
    """Silhueta estilizada da miniatura (não é foto real)."""
    light = tuple(min(255, c + 45) for c in color)
    dark = tuple(int(c * 0.55) for c in color)
    if kind == "front":
        rect(d, x + w * 0.12, y + h * 0.22, w * 0.76, h * 0.56, 10, color)
        rect(d, x + w * 0.2, y + h * 0.34, w * 0.6, h * 0.2, 5, light)
        rect(d, x + w * 0.05, y + h * 0.16, w * 0.16, h * 0.18, 4, dark)
        rect(d, x + w * 0.79, y + h * 0.16, w * 0.16, h * 0.18, 4, dark)
        for fx in (0.28, 0.72):
            rect(d, x + w * fx - w * 0.05, y + h * 0.52, w * 0.1, h * 0.07, 2, (250, 240, 200))
    elif kind == "rear":
        rect(d, x + w * 0.12, y + h * 0.24, w * 0.76, h * 0.52, 10, color)
        rect(d, x + w * 0.22, y + h * 0.32, w * 0.56, h * 0.18, 5, light)
        for fx in (0.22, 0.66):
            rect(d, x + w * fx, y + h * 0.5, w * 0.12, h * 0.09, 3, (240, 90, 80))
    else:
        rect(d, x + w * 0.06, y + h * 0.42, w * 0.88, h * 0.26, 8, color)
        rect(d, x + w * 0.3, y + h * 0.2, w * 0.4, h * 0.24, 7, light)
        rect(d, x + w * 0.33, y + h * 0.24, w * 0.34, h * 0.14, 4, (60, 70, 85))
        rect(d, x + w * 0.07, y + h * 0.44, w * 0.86, h * 0.05, 2, light)
    for fx in (0.26, 0.72):
        cy = y + h * (0.68 if kind != "profile" else 0.68)
        d.ellipse([((x + w * fx - w * 0.1) * S), ((cy - h * 0.1) * S),
                   ((x + w * fx + w * 0.1) * S), ((cy + h * 0.1) * S)], fill=(24, 24, 24))
        d.ellipse([((x + w * fx - w * 0.04) * S), ((cy - h * 0.04) * S),
                   ((x + w * fx + w * 0.04) * S), ((cy + h * 0.04) * S)], fill=(140, 140, 140))


import math


def photo_tile(d, x, y, size, color, kind, label):
    rect(d, x, y, size, size, 10, SURFACE2)
    car(d, x + size * 0.06, y + size * 0.16, size * 0.88, size * 0.6, color, kind)
    text(d, x, y + size + 4, label, 9, False, MUTED)
    rect(d, x + 4, y + 4, 14, 12, 4, (0, 0, 0, 0), None, 0) if False else None


CATALOG = [
    ("Twin Mill III", "Hot Wheels · 2023 · 2 exemplares", RED, "profile"),
    ("Bone Shaker", "Hot Wheels · 2019 · 1 exemplar", ORANGE, "front"),
    ("'70 Ford Bronco", "Matchbox · 2020 · 1 exemplar", (46, 130, 90), "rear"),
    ("Skyline GT-R R34", "Mini GT · 2022 · nenhum exemplar", (150, 60, 170), "profile"),
]


def sc_title(p):
    img, d = canvas()
    status_bar(d)
    a = seg(p, 0.05, 0.35)
    car(d, 40, 240, 280, 140, blend(BG, RED, a), "profile")
    text(d, 180, 412, "Diecast Collector", 18, True, blend(BG, TEXT, a), anchor="mm")
    text(d, 180, 438, "modelo · variante · exemplar", 10, False,
         blend(BG, MUTED, a), anchor="mm")
    b = seg(p, 0.3, 0.5)
    rect(d, 122, 466, 116, 24, 12, blend(BG, SURFACE2, b), blend(BG, ORANGE, b), 1.2)
    text(d, 180, 478, "100% offline", 9.5, True, blend(BG, ORANGE, b), anchor="mm")
    return img


def card(d, x, y, w, h, title, subtitle, color, kind, alpha=1.0):
    if alpha <= 0.01:
        return
    rect(d, x, y, w, h, 12, SURFACE if alpha >= 0.99 else blend(BG, SURFACE, alpha))
    rect(d, x + 10, y + (h - 48) / 2, 48, 48, 10, SURFACE2)
    car(d, x + 13, y + (h - 48) / 2 + 8, 42, 32, color, kind)
    text(d, x + 68, y + 14, title, 13.5, True)
    text(d, x + 68, y + 33, subtitle, 10, False, MUTED)
    d.line([((x + w - 16) * S, (y + h / 2 - 5) * S), ((x + w - 12) * S, (y + h / 2) * S)],
           fill=MUTED, width=int(1.5 * S))
    d.line([((x + w - 16) * S, (y + h / 2 + 5) * S), ((x + w - 12) * S, (y + h / 2) * S)],
           fill=MUTED, width=int(1.5 * S))


def blend(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


CONV = 588  # fim da área de conteúdo (dp)


def sc_catalog(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Coleção", action="Buscar")
    rect(d, 16, 62, 328, 32, 16, SURFACE2)
    text(d, 34, 72, "Buscar modelo, fabricante, ano…", 10.5, False, MUTED)
    x = 16
    for i, label in enumerate(["Todos", "Fabricante", "Ano", "Série"]):
        w = chip(d, x, 104, label, i == 0)
        x += w + 8
    if p > 0.35:
        a = seg(p, 0.35, 0.5)
        rect(d, 250, 142, 94, 22, 11, blend(SURFACE, (60, 42, 10), a))
        text(d, 297, 153, "Incompletos (2)", 9.5, True, blend(MUTED, (255, 190, 60), a), anchor="mm")
    for i, (title, sub, color, kind) in enumerate(CATALOG):
        a = seg(p, 0.1 + i * 0.1, 0.3 + i * 0.1)
        card(d, 16, 174 + i * 84 - (1 - a) * 10, 328, 76, title, sub, color, kind, a)
    fa = seg(p, 0.62, 0.78)
    if fa > 0.01:
        rect(d, 244, 512, 100, 46, 23, ORANGE)
        text(d, 294, 535, "+ Mini", 13, True, (26, 16, 8), anchor="mm")
    return img


def sc_new_model(p):
    """Fabricante digitado, sugestão escolhida e depois o modelo (FR-024 nos dois campos)."""
    img, d = canvas()
    status_bar(d)
    appbar(d, "Nova miniatura", back=True)
    text(d, 16, 62, "Só fabricante e modelo são obrigatórios.", 9.5, False, MUTED)
    text(d, 16, 74, "A variante e o exemplar entram na próxima tela.", 9.5, False, MUTED)

    # Cartão de fabricantes aparece ao digitar "mat" e colapsa ao escolher "Matchbox".
    # O deslocamento do layout começa antes da opacidade, senão o cartão nasce em cima
    # do rótulo do campo seguinte.
    ca = seg(p, 0.16, 0.26) * (1 - seg(p, 0.30, 0.38))
    ca_layout = seg(p, 0.12, 0.20) * (1 - seg(p, 0.34, 0.44))
    cb = seg(p, 0.56, 0.66)
    cb_layout = seg(p, 0.52, 0.60)

    typed = "mat"
    n = int(len(typed) * seg(p, 0.06, 0.16))
    fab_typing = 0.06 <= p < 0.30
    fab_value = typed[:n] if p < 0.30 else "Matchbox"
    field(d, 16, 104, 328, "Fabricante *", fab_value, fab_typing, fab_typing)

    if ca > 0.01:
        rect(d, 16, 146, 328, 46, 10, blend(BG, SURFACE, ca))
        text(d, 26, 152, "Fabricantes já cadastrados:", 8.5, True, blend(BG, ORANGE, ca))
        text(d, 26, 168, "Matchbox", 12, False, blend(BG, TEXT, ca))

    y_model = 154 + 54 * ca_layout
    model_typing = 0.40 <= p < 0.56
    mn = int(len("Ford Bronco") * seg(p, 0.40, 0.54))
    field(d, 16, y_model, 328, "Modelo *", "Ford Bronco"[:mn], model_typing, model_typing)

    if cb > 0.01:
        cy = y_model + 44
        rect(d, 16, cy, 328, 56, 10, blend(BG, SURFACE, cb))
        text(d, 26, cy + 6, "Modelos existentes com nome semelhante:", 8.5, True,
             blend(BG, ORANGE, cb))
        text(d, 26, cy + 22, "Ford Bronco", 11.5, False, blend(BG, TEXT, cb))
        text(d, 26, cy + 38, "Escala 1:64", 9, False, blend(BG, MUTED, cb))

    y_esc = y_model + 50 + 70 * cb_layout
    sn = int(len("1:64") * seg(p, 0.66, 0.72))
    esc_typing = 0.66 <= p < 0.74
    field(d, 16, y_esc, 328, "Escala", "1:64"[:sn], esc_typing, esc_typing)

    ia = seg(p, 0.72, 0.8)
    if ia > 0.01:
        rect(d, 16, y_esc + 46, 328, 46, 10, blend(BG, SURFACE, ia))
        text(d, 28, y_esc + 56, "Cor, ano e código do fabricante", 9.5, False,
             blend(BG, TEXT, ia))
        text(d, 28, y_esc + 70, "entram na tela da variante.", 9.5, False, blend(BG, TEXT, ia))
    ba = seg(p, 0.82, 0.9)
    if ba > 0.01:
        button(d, 16, 500, 328, "Salvar miniatura", blend(BG, RED, ba))
    return img


def sc_variant(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Cadastrar variante", back=True)
    text(d, 16, 60, "Twin Mill III", 11, False, MUTED)
    rows = [("Ano *", "2023"), ("Cor principal *", "Vermelho"), ("Série", "HW Mainline")]
    for i, (label, val) in enumerate(rows):
        n = int(len(val) * seg(p, 0.1 + i * 0.1, 0.34 + i * 0.1))
        field(d, 16, 92 + i * 56, 328, label, val[:n], p < 0.5 and i == 0, 0 < n < len(val))
    ca = seg(p, 0.42, 0.52)
    if ca > 0.01:
        text(d, 16, 258, "Condição do exemplar", 9.5, False, MUTED)
        x = 16
        for i, label in enumerate(["Lacrado", "Solto", "Danificado"]):
            w = chip(d, x, 272, label, i == 0, small=True)
            x += w + 6
    ga = seg(p, 0.5, 0.6)
    if ga > 0.01:
        text(d, 16, 312, "Fotografias", 11, True)
        tiles = [("Frente", RED, "front"), ("Traseira", RED, "rear"), ("Lateral", RED, "profile")]
        for i, (label, color, kind) in enumerate(tiles):
            a = seg(p, 0.54 + i * 0.08, 0.66 + i * 0.08)
            if a > 0.01:
                photo_tile(d, 16 + i * 82, 330, 72, color, kind, label)
        aa = seg(p, 0.74, 0.84)
        if aa > 0.01:
            rect(d, 16 + 3 * 82, 330, 72, 72, 10, blend(BG, SURFACE2, aa), LINE, 1.2)
            text(d, 16 + 3 * 82 + 36, 366, "+", 20, False, blend(MUTED, TEXT, aa), anchor="mm")
            text(d, 16 + 3 * 82, 406, "Adicionar", 9, False, MUTED)
    sa = seg(p, 0.8, 0.9)
    if sa > 0.01:
        button(d, 16, 512, 328, "Salvar variante e exemplar", blend(BG, RED, sa))
    return img


def sc_incomplete(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Cadastros incompletos", back=True)
    text(d, 16, 62, "Modelos que ainda não têm variante cadastrada", 10, False, MUTED)
    rows = [("Rodger Dodger", "Hot Wheels · 2021"), ("'70 Ford Bronco", "Matchbox · 2020")]
    for i, (title, sub) in enumerate(rows):
        a = seg(p, 0.15 + i * 0.12, 0.35 + i * 0.12)
        if a <= 0.01:
            continue
        y = 94 + i * 82
        rect(d, 16, y, 328, 72, 12, SURFACE)
        rect(d, 26, y + 14, 44, 44, 10, SURFACE2)
        car(d, 29, y + 22, 38, 28, (150, 60, 170) if i == 0 else RED,
            "rear" if i == 0 else "profile")
        text(d, 82, y + 12, title, 12.5, True)
        text(d, 82, y + 30, sub, 9.5, False, MUTED)
        rect(d, 82, y + 46, 132, 17, 8, (60, 42, 10))
        text(d, 148, y + 54.5, "Sem variante cadastrada", 8.5, True, (255, 190, 60), anchor="mm")
    ea = seg(p, 0.5, 0.62)
    if ea > 0.01:
        text(d, 180, 286, "Cadastre a variante para completar", 9.5, False,
             blend(BG, MUTED, ea), anchor="mm")
        button(d, 16, 306, 328, "Cadastrar variante", blend(BG, RED, ea), h=40)
    return img


def sc_dashboard(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Painel")
    text(d, 16, 60, "Coleção completa · atualizado no aparelho", 9.5, False, MUTED)
    stats = [("128", "exemplares"), ("47", "modelos"), ("9", "fabricantes")]
    for i, (num, label) in enumerate(stats):
        a = seg(p, 0.1 + i * 0.08, 0.3 + i * 0.08)
        if a <= 0.01:
            continue
        x = 16 + i * 111
        rect(d, x, 82, 106, 64, 12, SURFACE)
        text(d, x + 12, 92, num, 22, True, blend(BG, TEXT, a))
        text(d, x + 12, 124, label, 9, False, MUTED)
    va = seg(p, 0.32, 0.45)
    if va > 0.01:
        rect(d, 16, 158, 328, 76, 12, SURFACE)
        text(d, 28, 168, "Valor investido", 9.5, False, MUTED)
        text(d, 28, 184, "R$ 4.120,00", 18, True, blend(BG, TEXT, va))
        text(d, 200, 168, "Valor avaliado", 9.5, False, MUTED)
        text(d, 200, 184, "R$ 6.850,00", 18, True, blend(BG, GREEN, va))
        text(d, 28, 212, "+ R$ 2.730,00 de valorização", 9.5, True, GREEN)
    ba = seg(p, 0.5, 0.62)
    if ba > 0.01:
        text(d, 16, 250, "Por fabricante", 11, True)
        bars = [("Hot Wheels", 96, RED), ("Matchbox", 22, ORANGE), ("Mini GT", 10, (46, 110, 200))]
        for i, (name, val, color) in enumerate(bars):
            g = seg(p, 0.52 + i * 0.08, 0.74 + i * 0.08)
            y = 274 + i * 42
            text(d, 16, y, name, 10)
            text(d, 344, y, str(val), 10, True, MUTED, anchor="ra")
            rect(d, 16, y + 16, 328, 8, 4, SURFACE2)
            if g > 0.01:
                rect(d, 16, y + 16, max(4, 328 * val / 100 * g), 8, 4, color)
    na = seg(p, 0.8, 0.9)
    if na > 0.01:
        rect(d, 16, 402, 328, 40, 10, SURFACE)
        text(d, 28, 415, "Funciona em modo avião — dados só no aparelho", 9.5, False,
             blend(BG, MUTED, na))
    return img


def sc_backup(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Relatórios e backup", back=True)
    rows = [
        ("Exportar inventário (CSV)", "1.284 exemplares · 47 modelos"),
        ("Relatório em PDF", "Coleção, financeiro e faltantes"),
        ("Pacote de backup", "Dados + fotos, restaurável em outro aparelho"),
    ]
    for i, (title, sub) in enumerate(rows):
        a = seg(p, 0.1 + i * 0.1, 0.3 + i * 0.1)
        if a <= 0.01:
            continue
        y = 66 + i * 70
        rect(d, 16, y, 328, 60, 12, SURFACE)
        text(d, 28, y + 12, title, 12, True)
        text(d, 28, y + 30, sub, 9.5, False, MUTED)
    pa = seg(p, 0.42, 0.55)
    if pa > 0.01:
        text(d, 16, 286, "Gerando pacote de backup…", 10.5, True, ORANGE)
        rect(d, 16, 304, 328, 10, 5, SURFACE2)
        prog = min(1.0, max(0.0, (p - 0.45) / 0.4))
        if prog > 0.01:
            rect(d, 16, 304, max(5, 328 * prog), 10, 5, ORANGE)
        text(d, 344, 322, f"{int(prog * 100)}%", 9.5, False, MUTED, anchor="ra")
    da = seg(p, 0.8, 0.92)
    if da > 0.01:
        rect(d, 16, 344, 328, 56, 12, blend(BG, (26, 46, 30), da))
        d.ellipse([(32 * S, 362 * S), (50 * S, 380 * S)], outline=GREEN, width=int(2 * S))
        d.line([(37 * S, 371 * S), (41 * S, 375 * S)], fill=GREEN, width=int(2 * S))
        d.line([(41 * S, 375 * S), (46 * S, 366 * S)], fill=GREEN, width=int(2 * S))
        text(d, 60, 356, "Backup concluído", 11.5, True, GREEN)
        text(d, 60, 374, "Último backup: hoje, 9:41", 9.5, False, MUTED)
    return img


SLIDES = [
    ("Frente", "front"),
    ("Traseira", "rear"),
    ("Lateral esquerda", "profile"),
]


def carousel(img, x, y, w, h, off):
    """Fatia horizontal de fotos: `off` avança o carrossel em telas inteiras.

    Cada slide é desenhado num quadro próprio e recortado pelo seu cartão, então
    o conteúdo nunca sangra para fora da foto vizinha durante a transição.
    """
    gap = 8
    mask = Image.new("L", (int(w * S), int(h * S)), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, int(w * S) - 1, int(h * S) - 1], radius=int(14 * S), fill=255)
    view = Image.new("RGB", (int(w * S), int(h * S)), BG)
    for i, (label, kind) in enumerate(SLIDES):
        slide = Image.new("RGB", (int(w * S), int(h * S)), SURFACE2)
        dl = ImageDraw.Draw(slide)
        car(dl, w * 0.14, h * 0.24, w * 0.72, h * 0.5, RED, kind)
        bw = width_of(label, 9, True) + 16
        rect(dl, 10, 10, bw, 18, 9, (10, 10, 10))
        text(dl, 10 + bw / 2, 19, label, 9, True, WHITE, anchor="mm")
        sx = (i - off) * (w + gap)
        view.paste(slide, (int(sx * S), 0), mask)
    img.paste(view, (int(x * S), int(y * S)))


HISTORY = [
    ("Compra", "R$ 89,90 · Loja Modelismo SP", "12/03/2024", ORANGE),
    ("Avaliação", "R$ 150,00 · conservação lacrado", "05/2026", GREEN),
    ("Último backup", "incluído no pacote de hoje", "9:41", MUTED),
]


def sc_detail(p):
    img, d = canvas()
    status_bar(d)
    appbar(d, "Twin Mill III", back=True, action="Editar")
    text(d, 16, 56, "Hot Wheels · 2023 · HW Mainline", 9.5, False, MUTED)
    fa = seg(p, 0.05, 0.2)
    if fa > 0.01:
        off = seg(p, 0.3, 0.44) * 1.0 + seg(p, 0.5, 0.64) * 1.0
        carousel(img, 16, 78, 328, 196, off)
        idx = min(len(SLIDES) - 1, round(off))
        for i in range(len(SLIDES)):
            cx = 180 + (i - idx) * 12
            d.ellipse([((cx - (3 if i == idx else 2)) * S), ((286 - (3 if i == idx else 2)) * S),
                       ((cx + (3 if i == idx else 2)) * S), ((286 + (3 if i == idx else 2)) * S)],
                      fill=ORANGE if i == idx else SURFACE2)
    ca = seg(p, 0.2, 0.32)
    if ca > 0.01:
        w1 = chip(d, 16, 300, "Lacrado", True, True)
        w2 = chip(d, 16 + w1 + 6, 300, "1 exemplar", False, True)
        chip(d, 16 + w1 + w2 + 12, 300, "1:64", False, True)
    ia = seg(p, 0.28, 0.4)
    if ia > 0.01:
        rect(d, 16, 334, 160, 46, 10, blend(BG, SURFACE, ia))
        text(d, 28, 342, "Preço pago", 8.5, False, MUTED)
        text(d, 28, 355, "R$ 89,90", 12, True, blend(BG, TEXT, ia))
        rect(d, 184, 334, 160, 46, 10, blend(BG, SURFACE, ia))
        text(d, 196, 342, "Localização", 8.5, False, MUTED)
        text(d, 196, 355, "Caixa 3 · armário", 12, True, blend(BG, TEXT, ia))
    ha = seg(p, 0.42, 0.52)
    if ha > 0.01:
        text(d, 16, 394, "Histórico", 11, True, blend(BG, TEXT, ha))
        d.line([(24 * S, 420 * S), (24 * S, 496 * S)], fill=LINE, width=int(1.5 * S))
        for i, (title, sub, when, color) in enumerate(HISTORY):
            a = seg(p, 0.5 + i * 0.08, 0.66 + i * 0.08)
            if a <= 0.01:
                continue
            y = 414 + i * 32
            d.ellipse([(20 * S, (y + 3) * S), (28 * S, (y + 11) * S)], fill=color)
            text(d, 38, y, title, 10.5, True, blend(BG, TEXT, a))
            text(d, 38, y + 14, sub, 9, False, blend(BG, MUTED, a))
            text(d, 344, y, when, 9, False, blend(BG, MUTED, a), anchor="ra")
    return img


SCENES = [
    (sc_title, 2.6, "Diecast Collector — qualquer fabricante, tudo no aparelho"),
    (sc_catalog, 4.6, "Coleção offline: busca, filtros e exemplares por variante"),
    (sc_detail, 5.6, "Detalhe da variante: fotos por ângulo, dados do exemplar e histórico"),
    (sc_new_model, 4.2, "Cadastro em dois passos: primeiro o modelo"),
    (sc_variant, 5.2, "Depois a variante, o primeiro exemplar e as fotos"),
    (sc_incomplete, 3.6, "Cadastros incompletos ficam à vista, sem sumir"),
    (sc_dashboard, 4.4, "Painel: acervo, investimento e avaliação — tudo local"),
    (sc_backup, 4.4, "Relatórios e backup em CSV, PDF e pacote completo"),
]

# toques por cena: (x, y, início da animação em progresso da cena)
TAPS = {
    1: [(180, 212, 0.66)],
    3: [(90, 176, 0.29), (180, 522, 0.92)],
    4: [(180, 534, 0.90)],
}


def with_tap(img, scene_i, p):
    taps = TAPS.get(scene_i, ())
    if not taps:
        return img
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dd = ImageDraw.Draw(layer)
    drew = False
    for x, y, t0 in taps:
        if not (t0 <= p <= t0 + 0.2):
            continue
        q = (p - t0) / 0.2
        radius = 8 + 26 * q
        dd.ellipse([((x - radius) * S), ((y - radius) * S),
                    ((x + radius) * S), ((y + radius) * S)],
                   fill=(255, 255, 255, int(80 * (1 - q))))
        drew = True
    if not drew:
        return img
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def caption(img, text_str, alpha):
    if alpha <= 0.01:
        return img
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    a = int(255 * alpha)
    d.rectangle([0, (CONV - 34) * S, W, H], fill=(8, 8, 8, int(235 * alpha)))
    d.text((180 * S, (CONV + 10) * S), text_str, font=font(10.5, False),
           fill=(236, 236, 236, a), anchor="mm")
    d.text((180 * S, (CONV + 10) * S), "", font=font(10.5))
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def wrap_caption(img, s, alpha):
    if alpha <= 0.01:
        return img
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    a = int(255 * alpha)
    d.rectangle([0, (CONV - 30) * S, W, H], fill=(8, 8, 8, int(240 * alpha)))
    words = s.split()
    lines, cur = [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if width_of(trial, 10.5) <= 320:
            cur = trial
        else:
            lines.append(cur)
            cur = w
    lines.append(cur)
    y = CONV + 4 if len(lines) == 1 else CONV + 1
    for i, line in enumerate(lines[:2]):
        d.text((180 * S, (y + i * 13) * S), line, font=font(10.5, False),
               fill=(236, 236, 236, a), anchor="ma")
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def build(out_dir):
    xs = []
    for i, (fn, dur, cap) in enumerate(SCENES):
        n = int(dur * FPS)
        frames = []
        for k in range(n):
            p = k / max(1, n - 1)
            img = with_tap(fn(p), i, p)
            ca = min(seg(p, 0.0, 0.1), 1 - seg(p, 0.82, 0.95))
            img = wrap_caption(img, cap, ca)
            frames.append(img)
        xs.append(frames)
    # Transições: escurece até o fundo e clareia na cena seguinte. Crossfade entre telas
    # diferentes sobrepõe dois textos e fica ilegível; o escurecer lê como corte.
    out = []
    fade = 5
    black = Image.new("RGB", (W, H), BG)
    for i, frames in enumerate(xs):
        out.extend(frames)
        if i < len(xs) - 1:
            for j in range(1, fade + 1):
                out.append(Image.blend(frames[-1], black, j / (fade + 1)))
            for j in range(fade, 0, -1):
                out.append(Image.blend(xs[i + 1][0], black, j / (fade + 1)))
    os.makedirs(out_dir, exist_ok=True)
    for idx, img in enumerate(out):
        img.save(os.path.join(out_dir, f"f{idx:04d}.png"))
    return len(out)


def main():
    tmp = tempfile.mkdtemp(prefix="simulacao-")
    total = build(tmp)
    print(f"{total} quadros em {tmp}")
    out = "docs/simulacao-app.mp4"
    os.makedirs("docs", exist_ok=True)
    ffmpeg = os.environ.get("FFMPEG")
    if not ffmpeg:
        try:
            sys.path.insert(0, "/tmp/imgff")
            import imageio_ffmpeg
            ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
        except Exception:
            ffmpeg = "ffmpeg"
    cmd = [ffmpeg, "-y", "-framerate", str(FPS), "-i", os.path.join(tmp, "f%04d.png"),
           "-c:v", "libx264", "-preset", "slow", "-crf", "20",
           "-pix_fmt", "yuv420p", "-movflags", "+faststart", out]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    # Pôster fixo: catálogo montado e depois do toque (7,4 s). Fração do total caía em
    # cima de uma transição; 6 s caía em cima do próprio toque.
    poster_idx = min(total - 1, int(7.4 * FPS))
    Image.open(os.path.join(tmp, f"f{poster_idx:04d}.png")).save(
        "docs/simulacao-app-poster.png")
    print("gerado:", out, "|", total / FPS, "s")


if __name__ == "__main__":
    main()
