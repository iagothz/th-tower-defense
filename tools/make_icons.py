"""Draws the launcher icon (a red die with five pips, in the game's bevel style) at every Android density.

    python tools/make_icons.py

Writes android/res/mipmap-*/ic_launcher.png, ic_launcher_round.png (legacy, API < 26) and
ic_launcher_foreground.png (adaptive icon layer, 108dp with the die inside the 72dp safe zone).
Needs Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "android" / "res"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
BACKGROUND = (0x1A, 0x1C, 0x30, 255)
RED = (235, 77, 56)
SUPERSAMPLE = 4


def die(canvas: Image.Image, cx: float, cy: float, size: float) -> None:
    """A bevelled die centred on (cx, cy), [size] wide, drawn like DieDraw.die."""
    d = ImageDraw.Draw(canvas)
    half = size / 2
    r = size * 0.19
    lip = size * 0.052
    dark = tuple(int(c * 0.62) for c in RED)
    shadow = (0, 0, 0, 90)
    d.rounded_rectangle((cx - half + size * 0.02, cy - half + size * 0.05, cx + half + size * 0.02, cy + half + size * 0.05), r, fill=shadow)
    d.rounded_rectangle((cx - half, cy - half, cx + half, cy + half), r, fill=dark)
    d.rounded_rectangle((cx - half, cy - half, cx + half, cy + half - lip), r, fill=RED)
    light = tuple(int(c + (255 - c) * 0.22) for c in RED)
    d.rectangle((cx - half + r, cy - half, cx + half - r, cy - half + size * 0.03), fill=light)
    pip_r = size * 0.0885
    step = size * 0.25
    fy = cy - lip / 2
    for dx, dy in [(-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)]:
        px, py = cx + dx * step, fy + dy * step
        d.ellipse((px - pip_r, py - pip_r, px + pip_r, py + pip_r), fill=(255, 255, 255))


def render(px: int, draw_background: bool, die_fraction: float) -> Image.Image:
    s = px * SUPERSAMPLE
    img = Image.new("RGBA", (s, s), BACKGROUND if draw_background else (0, 0, 0, 0))
    die(img, s / 2, s / 2, s * die_fraction)
    return img.resize((px, px), Image.LANCZOS)


def main() -> None:
    for name, scale in DENSITIES.items():
        out = RES / f"mipmap-{name}"
        out.mkdir(parents=True, exist_ok=True)
        legacy = int(48 * scale)
        render(legacy, True, 0.78).save(out / "ic_launcher.png")
        # Round variant: same picture under a circular mask.
        round_img = render(legacy, True, 0.70)
        mask = Image.new("L", (legacy * SUPERSAMPLE,) * 2, 0)
        ImageDraw.Draw(mask).ellipse((0, 0, legacy * SUPERSAMPLE - 1, legacy * SUPERSAMPLE - 1), fill=255)
        round_img.putalpha(mask.resize((legacy, legacy), Image.LANCZOS))
        round_img.save(out / "ic_launcher_round.png")
        # Adaptive foreground: 108dp canvas, content inside the central 72dp (2/3).
        render(int(108 * scale), False, 0.56).save(out / "ic_launcher_foreground.png")
    print("icons written under", RES)


if __name__ == "__main__":
    main()
