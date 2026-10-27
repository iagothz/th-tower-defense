"""Draws the Play Store feature graphic (1024×500) into store/feature.png.

Run from the repo root:  python tools/make_feature.py   (needs Pillow)
Night sky, a row of dice in the game's colours, the title and a tagline.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "store" / "feature.png"
FONT = ROOT / "assets" / "fonts" / "Roboto-Medium.ttf"

W, H = 1024, 500
# (fill, pip colour) per die, in the game's palette.
DICE = [
    ((0xE2, 0x4A, 0x3B), "white"),  # fire
    ((0x7E, 0xC8, 0xF0), (0x20, 0x20, 0x30)),  # ice
    ((0x5E, 0xB8, 0x4F), "white"),  # poison
    ((0xF2, 0xC2, 0x2E), (0x20, 0x20, 0x30)),  # electric
    ((0x3D, 0xB8, 0xA8), "white"),  # wind
]
PIPS = {1: [(0, 0)], 3: [(-1, 1), (0, 0), (1, -1)], 5: [(-1, 1), (1, 1), (0, 0), (-1, -1), (1, -1)], 6: [(-1, 1), (1, 1), (-1, 0), (1, 0), (-1, -1), (1, -1)], 7: [(-1, 1), (1, 1), (-1, 0), (0, 0), (1, 0), (-1, -1), (1, -1)]}


def die(draw, cx, cy, size, fill, pip, pips, gold=False):
    r = size * 0.19
    if gold:
        draw.rounded_rectangle((cx - size / 2 - 6, cy - size / 2 - 6, cx + size / 2 + 6, cy + size / 2 + 6), radius=r + 6, fill=(0xF2, 0xC2, 0x2E))
    shade = tuple(int(c * 0.72) for c in fill)
    draw.rounded_rectangle((cx - size / 2, cy - size / 2 + 6, cx + size / 2, cy + size / 2 + 6), radius=r, fill=shade)
    draw.rounded_rectangle((cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2), radius=r, fill=fill)
    step = size * 0.25
    pr = size * 0.0885
    for dx, dy in PIPS[pips]:
        x, y = cx + dx * step, cy - dy * step
        draw.ellipse((x - pr, y - pr, x + pr, y + pr), fill=pip)


def main():
    img = Image.new("RGB", (W, H), (0x10, 0x11, 0x1C))
    draw = ImageDraw.Draw(img)
    # Sky gradient and stars.
    for y in range(H):
        t = y / H
        draw.line((0, y, W, y), fill=(int(0x10 + 0x08 * t), int(0x11 + 0x06 * t), int(0x1C + 0x14 * t)))
    import random
    rng = random.Random(7)
    for _ in range(160):
        x, y = rng.randrange(W), rng.randrange(H)
        s = rng.choice((1, 1, 1, 2))
        draw.ellipse((x, y, x + s, y + s), fill=(0xC8, 0xCC, 0xE0))
    # Path ribbon.
    draw.rounded_rectangle((60, 300, 964, 344), radius=22, fill=(0x3A, 0x2E, 0x22))
    draw.rounded_rectangle((68, 306, 956, 338), radius=18, fill=(0x55, 0x45, 0x30))
    # Dice marching up in pips.
    pips = [1, 3, 5, 6, 7]
    for i, ((fill, pip), p) in enumerate(zip(DICE, pips)):
        size = 86 + i * 10
        cx = 150 + i * 180
        die(draw, cx, 322 - (size - 86) * 0.5, size, fill, pip, p, gold=(p == 7))
    # Title and tagline.
    title = ImageFont.truetype(str(FONT), 96)
    small = ImageFont.truetype(str(FONT), 34)
    for dx, dy in ((4, 4), (0, 0)):
        draw.text((W / 2 + dx, 118 + dy), "Merge TD", font=title, anchor="mm", fill=(0, 0, 0) if dx else (0xFF, 0xFF, 0xFF))
    draw.text((W / 2, 196), "Merge dice. Hold the waves. Offline.", font=small, anchor="mm", fill=(0xF2, 0xC2, 0x2E))
    draw.text((W / 2, 452), "25 dice · 8 synergies · 4 maps · 4 modes · daily and weekly challenges", font=ImageFont.truetype(str(FONT), 24), anchor="mm", fill=(0xC8, 0xCC, 0xE0))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
