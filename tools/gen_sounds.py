"""Generates the game's retro sound effects into assets/sounds/.

Run from the repo root:  python tools/gen_sounds.py
Pure standard library; every sound is synthesized, so there is nothing to license.
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "assets" / "sounds"


def square(phase, duty=0.5):
    return 1.0 if (phase % 1.0) < duty else -1.0


def triangle(phase):
    p = phase % 1.0
    return 4 * p - 1 if p < 0.5 else 3 - 4 * p


def tone(duration, f_start, f_end=None, shape=square, volume=0.5, attack=0.005, release=0.04):
    """A note sweeping linearly from f_start to f_end with a short attack/release envelope."""
    f_end = f_end or f_start
    n = int(duration * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        freq = f_start + (f_end - f_start) * (i / max(1, n - 1))
        phase += freq / RATE
        env = min(1.0, t / attack) if attack else 1.0
        env *= min(1.0, (duration - t) / release) if release else 1.0
        out.append(shape(phase) * volume * env)
    return out


def noise(duration, volume=0.3, release=0.02):
    n = int(duration * RATE)
    rnd = random.Random(42)
    return [(rnd.random() * 2 - 1) * volume * min(1.0, (duration - i / RATE) / release) for i in range(n)]


def save(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32000)) for s in samples))


save("buy", tone(0.05, 880, volume=0.35) + tone(0.07, 1320, volume=0.35))
save("merge", sum((tone(0.055, f, shape=triangle, volume=0.6) for f in (523, 659, 784, 1047)), []))
save("power", tone(0.22, 400, 1200, shape=lambda p: square(p, 0.25), volume=0.3))
save("hit", noise(0.03, volume=0.18))
save("kill", tone(0.12, 620, 180, volume=0.3))
save("leak", tone(0.3, 140, 55, shape=triangle, volume=0.8, release=0.1))
save("wave", tone(0.12, 440, volume=0.3) + tone(0.16, 660, volume=0.3))
save("boss", [a + b for a, b in zip(tone(0.7, 110, 98, volume=0.3, release=0.2),
                                     tone(0.7, 116, 104, volume=0.3, release=0.2))])
print("wrote", sorted(p.name for p in OUT.glob("*.wav")))
