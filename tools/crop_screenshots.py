"""Crops raw emulator captures (1080×2400 with a 130 px black cutout on top) to 1080×2160, the 2:1 the store allows.

Run from the repo root:  python tools/crop_screenshots.py   (needs Pillow)
Overwrites every PNG under store/screenshots/ that is still 2400 tall.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "store" / "screenshots"
TOP = 130
HEIGHT = 2160

for png in sorted(ROOT.glob("*/*.png")):
    img = Image.open(png)
    if img.height == HEIGHT:
        continue
    cropped = img.crop((0, TOP, img.width, TOP + HEIGHT))
    cropped.save(png)
    print("cropped", png.relative_to(ROOT.parent.parent), cropped.size)
