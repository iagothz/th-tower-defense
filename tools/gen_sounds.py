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
# One merge chime per result size: the same arpeggio, two semitones higher per pip, so a big merge sounds bigger.
for pips in range(2, 7):
    k = 2 ** ((pips - 2) * 2 / 12)
    save(f"merge_{pips}", sum((tone(0.055, f * k, shape=triangle, volume=0.6) for f in (523, 659, 784, 1047)), []))
# Seven pips: a short fanfare with a held top note.
save("max", sum((tone(0.07, f, shape=triangle, volume=0.55) for f in (523, 659, 784, 1047, 1319)), [])
     + [a + b for a, b in zip(tone(0.3, 1568, shape=triangle, volume=0.3, release=0.2), tone(0.3, 1047, shape=triangle, volume=0.25, release=0.2))])
save("power", tone(0.22, 400, 1200, shape=lambda p: square(p, 0.25), volume=0.3))
save("hit", noise(0.03, volume=0.18))
save("kill", tone(0.12, 620, 180, volume=0.3))
save("leak", tone(0.3, 140, 55, shape=triangle, volume=0.8, release=0.1))
save("wave", tone(0.12, 440, volume=0.3) + tone(0.16, 660, volume=0.3))
save("boss", [a + b for a, b in zip(tone(0.7, 110, 98, volume=0.3, release=0.2),
                                     tone(0.7, 116, 104, volume=0.3, release=0.2))])
print("wrote", sorted(p.name for p in OUT.glob("*.wav")))


# --- Attack sounds per die family (see SoundPlanner.family) ---
def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] if i < len(t) else 0.0 for t in tracks) for i in range(n)]


save("hit_fire", mix(noise(0.06, volume=0.16, release=0.04), tone(0.06, 220, 90, shape=triangle, volume=0.25)))
save("hit_ice", tone(0.05, 1900, 1500, shape=triangle, volume=0.28, release=0.03))
save("hit_zap", tone(0.045, 1400, 500, shape=lambda p: square(p, 0.2), volume=0.16))
save("hit_wind", noise(0.035, volume=0.10, release=0.03))
save("hit_heavy", tone(0.09, 130, 55, shape=triangle, volume=0.55, release=0.05))
save("hit_magic", tone(0.03, 660, shape=triangle, volume=0.25) + tone(0.035, 990, shape=triangle, volume=0.25))
# A boss going down: a rumble under a burst of noise.
save("bosskill", mix(noise(0.4, volume=0.45, release=0.3), tone(0.45, 170, 35, shape=triangle, volume=0.7, release=0.25)))


# --- Background loop: a calm minor arpeggio, four chords, 19.2 s at 100 BPM ---
def note_hz(semitones_from_a4):
    return 440.0 * 2 ** (semitones_from_a4 / 12)


def music():
    eighth = 0.3
    # Chord tones as semitones from A4: Am, F, C, G (roots an octave down for the bass).
    chords = [(-12, -9, -5), (-16, -12, -9), (-21, -17, -14), (-14, -10, -7)]
    out = []
    for rep in range(2):
        for ci, chord in enumerate(chords):
            bar = [0.0] * int(8 * eighth * RATE)
            bass = tone(8 * eighth, note_hz(chord[0] - 12), shape=triangle, volume=0.11, attack=0.02, release=0.3)
            bar = mix(bar, bass)
            pattern = [0, 1, 2, 1, 0, 2, 1, 2] if rep == 0 else [2, 1, 0, 1, 2, 0, 1, 0]
            for step, idx in enumerate(pattern):
                octave = 12 if step % 4 == 2 else 0
                n = tone(eighth * 0.95, note_hz(chord[idx] + octave), shape=triangle, volume=0.075, attack=0.01, release=0.12)
                start = int(step * eighth * RATE)
                for i, s in enumerate(n):
                    if start + i < len(bar):
                        bar[start + i] += s
                if step % 2 == 1:
                    hat = noise(0.02, volume=0.03, release=0.015)
                    for i, s in enumerate(hat):
                        if start + i < len(bar):
                            bar[start + i] += s
            out.extend(bar)
    return out


save("music", music())
print("wrote", sorted(p.name for p in OUT.glob("*.wav")))
