# -*- coding: utf-8 -*-
"""
Genere les textures tuilables (seamless) du labo — style industriel brut.
Sortie : assets/Textures/labo/*.png (512x512, repetables sans couture).
Usage : python scripts/gen_textures.py
"""
import os
import numpy as np
from PIL import Image, ImageFilter

S = 512
OUT = os.path.join(os.path.dirname(__file__), "..", "assets", "Textures", "labo")
rng = np.random.default_rng(27)


def noise(cell, amp=1.0):
    """Value-noise tuilable : grille basse resolution tuilee 3x3 puis lissee."""
    n = max(2, S // cell)
    g = rng.random((n, n))
    big = np.tile(g, (3, 3))
    img = Image.fromarray((big * 255).astype(np.uint8)).resize((3 * S, 3 * S), Image.BICUBIC)
    out = np.asarray(img, dtype=np.float32)[S:2 * S, S:2 * S] / 255.0
    return (out - 0.5) * 2.0 * amp  # -amp..+amp


def fbm(cells=(64, 32, 16, 8), amps=(0.5, 0.25, 0.15, 0.10)):
    """Bruit multi-octaves tuilable."""
    a = np.zeros((S, S), np.float32)
    for c, m in zip(cells, amps):
        a += noise(c, m)
    return a


def blobs(count, rmin, rmax, strength):
    """Taches sombres periodiques (distance toroïdale → seamless)."""
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    a = np.zeros((S, S), np.float32)
    for _ in range(count):
        cx, cy = rng.random() * S, rng.random() * S
        r = rng.uniform(rmin, rmax)
        dx = np.minimum(np.abs(x - cx), S - np.abs(x - cx))
        dy = np.minimum(np.abs(y - cy), S - np.abs(y - cy))
        d2 = dx * dx + dy * dy
        a -= strength * np.exp(-d2 / (r * r))
    return a


def cracks(count, strength):
    """Fissures fines : marches aleatoires avec rebouclage (seamless)."""
    a = np.zeros((S, S), np.float32)
    for _ in range(count):
        x, y = rng.random() * S, rng.random() * S
        ang = rng.random() * 2 * np.pi
        for _ in range(rng.integers(120, 320)):
            x = (x + np.cos(ang)) % S
            y = (y + np.sin(ang)) % S
            ang += rng.normal(0, 0.18)
            a[int(y) % S, int(x) % S] -= strength
    img = Image.fromarray(((a - a.min()) / max(1e-6, a.max() - a.min()) * 255).astype(np.uint8))
    img = img.filter(ImageFilter.GaussianBlur(0.6))
    f = np.asarray(img, np.float32) / 255.0
    return (f - f.max()) * strength * 3.0


def save(name, lum, tint=(1.0, 1.0, 1.0)):
    """lum : luminance 0..1 -> PNG RGB teinte."""
    lum = np.clip(lum, 0, 1)
    rgb = np.stack([lum * tint[0], lum * tint[1], lum * tint[2]], axis=-1)
    Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8)).save(os.path.join(OUT, name))
    print("OK", name)


os.makedirs(OUT, exist_ok=True)

# ── 1. Beton mur : gris nuance + banches horizontales + taches + fissures ────
base = 0.58 + fbm() * 0.16
for jy in range(0, S, 128):  # joints de banches horizontaux
    base[jy:jy + 2, :] -= 0.18
    base[jy + 2:jy + 4, :] += 0.06
# trous de banche (cones de coffrage) tous les 128 px
for jy in range(64, S, 128):
    for jx in range(64, S, 128):
        yy, xx = np.ogrid[-6:7, -6:7]
        m = xx * xx + yy * yy <= 25
        base[(jy - 6) % S:(jy + 7) % S or S, (jx - 6) % S:(jx + 7) % S or S][m] -= 0.16
base += blobs(14, 30, 90, 0.10)   # taches d'humidite
base += cracks(5, 0.05)
save("beton_mur.png", base, (1.0, 1.0, 1.02))

# ── 2. Beton sol : plus sombre, plus use ─────────────────────────────────────
sol = 0.42 + fbm() * 0.14
sol += blobs(22, 24, 110, 0.12)   # taches d'usure / huile
sol += cracks(8, 0.05)
# joints de dalle (grille 256 px)
for j in range(0, S, 256):
    sol[j:j + 2, :] -= 0.20
    sol[:, j:j + 2] -= 0.20
save("beton_sol.png", sol, (1.0, 1.0, 1.0))

# ── 3. Acier peint (poutres/piliers) : brosse vertical + plaques + rivets ────
acier = 0.50 + fbm((64, 8), (0.08, 0.05))
stries = noise(4, 0.05)
acier += np.roll(stries, 1, axis=0) * 0.5 + stries * 0.5  # brossage vertical
for j in range(0, S, 256):  # joints de plaques
    acier[:, j:j + 3] -= 0.22
    acier[j:j + 3, :] -= 0.22
for j in range(0, S, 256):  # rivets le long des joints
    for k in range(32, S, 64):
        for (ry, rx) in ((j + 10, k), (k, j + 10)):
            yy, xx = np.ogrid[-3:4, -3:4]
            m = xx * xx + yy * yy <= 9
            acier[(ry - 3) % S:(ry + 4) % S or S, (rx - 3) % S:(rx + 4) % S or S][m] += 0.18
acier += blobs(8, 18, 50, 0.08)  # ecaillures sombres
save("acier_peint.png", acier, (0.96, 0.99, 1.05))

# ── 4. Metal rouille (tuyaux) : fond brun + mouchetures oxydees ──────────────
r = 0.40 + fbm((64, 16, 8), (0.20, 0.12, 0.08))
rouille = np.clip(r, 0, 1)
g = rouille * 0.55 + blobs(18, 14, 60, 0.10)
b = rouille * 0.35
rgb = np.stack([np.clip(rouille * 1.05, 0, 1), np.clip(g, 0, 1), np.clip(b, 0, 1)], axis=-1)
Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8)).save(os.path.join(OUT, "metal_rouille.png"))
print("OK metal_rouille.png")

# ── 5. Tole galvanisee (gaines) : gris clair, paillettes de zinc ─────────────
galva = 0.62 + fbm((32, 12), (0.07, 0.06)) + noise(6, 0.05)
save("tole_galva.png", galva, (0.97, 1.0, 1.04))

# ── 6. Metal de porte : acier sombre brosse + rayures ────────────────────────
porte = 0.40 + fbm((64, 8), (0.07, 0.04))
stries_h = noise(4, 0.05)
porte += np.roll(stries_h, 1, axis=1) * 0.5 + stries_h * 0.5  # brossage horizontal
porte += blobs(10, 14, 40, 0.09)
porte += cracks(3, 0.04)
save("metal_porte.png", porte, (0.95, 0.98, 1.06))

print("Textures generees dans", os.path.abspath(OUT))
