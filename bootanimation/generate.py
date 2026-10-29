#!/usr/bin/env python3
"""
Generates the Abyss boot animation (bootanimation.zip) plus a preview GIF.

  python3 generate.py            ->  bootanimation.zip, preview.gif (next to this file)

Needs: Python 3 + Pillow (pip install pillow). Edit the CODE list, colours, texts or timings below
and run it again to make your own version.

The animation has four parts (it is only an animation, nothing gets launched):
  part0  fake coding screen (typing code in a terminal window)
  part1  generic "game client" splash (invented logo, spinner, progress bar)
  part2  Abyss Home appears (tiles slide in, first tile gets focus)
  part3  idle loop: the focused tile gently glows until Android finishes booting
"""
import math
import os
import shutil
import sys
import zipfile

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

W, H, FPS = 1280, 720, 15
HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")

# ---- palette (same as the apps) ----
BG = (5, 7, 13)
SURF = (17, 23, 41)
ACC = (122, 162, 255)
ACC2 = (183, 148, 246)
TEXT = (230, 234, 245)
DIM = (138, 148, 178)
OK = (110, 231, 183)

# ---- fonts (falls back gracefully if a path doesn't exist) ----
def font(candidates, size):
    for p in candidates:
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()

MONO = ["/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", "DejaVuSansMono.ttf", "consola.ttf", "Menlo.ttc"]
MONO_B = ["/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", "DejaVuSansMono-Bold.ttf", "consolab.ttf"]
SANS = ["/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "DejaVuSans.ttf", "arial.ttf"]
SANS_B = ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "DejaVuSans-Bold.ttf", "arialbd.ttf"]


def ease(t):
    t = max(0.0, min(1.0, t))
    return 1 - (1 - t) ** 3


def mix(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def make_backdrop():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=mix((7, 11, 24), BG, y / H))
    glow = Image.new("RGB", (W, H), (0, 0, 0))
    ImageDraw.Draw(glow).ellipse([W - 520, -380, W + 380, 420], fill=(54, 41, 92))
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return ImageChops.add(img, glow)


BACK = make_backdrop()


def fade(img, a):
    return Image.blend(Image.new("RGB", (W, H), BG), img, max(0.0, min(1.0, a)))


# =====================================================================
# part0: fake coding screen
# =====================================================================
CODE = [
    "# abyss boot sequence",
    "import tegra, storage, net, media",
    "",
    "def mount_all():",
    "    for vol in storage.scan():",
    "        vol.mount(rw=True)",
    "        log('mounted', vol.name)",
    "",
    "def start_services():",
    "    gpu.init(clock=921)",
    "    audio.init(rate=48000)",
    "    net.up('wlan0')",
    "    media.index('/storage')",
    "",
    "mount_all()",
    "start_services()",
    "[ ok ] gpu driver ready",
    "[ ok ] storage mounted",
    "[ ok ] network online",
    "[ ok ] media library indexed",
    ">>> starting game client",
]


def line_color(line):
    s = line.lstrip()
    if s.startswith("#"):
        return DIM
    if s.startswith(("def ", "import ", "for ")):
        return ACC2
    if s.startswith("[ ok ]"):
        return OK
    if s.startswith(">>>"):
        return ACC
    return TEXT


def scene_code(n):
    frames = []
    fm = font(MONO, 19)
    ft = font(SANS, 15)
    total = sum(len(l) for l in CODE)
    lh, max_lines, tx, ty = 26, 16, 160, 150
    for f in range(n):
        img = BACK.copy()
        d = ImageDraw.Draw(img)
        d.rounded_rectangle([100, 80, 1180, 640], 18, fill=(10, 14, 26), outline=(40, 52, 92), width=2)
        d.rounded_rectangle([100, 80, 1180, 128], 18, fill=SURF)
        d.rectangle([100, 110, 1180, 128], fill=SURF)
        for i, c in enumerate([(255, 95, 109), (255, 200, 87), OK]):
            d.ellipse([124 + i * 26, 96, 140 + i * 26, 112], fill=c)
        d.text((640, 104), "abyss  -  boot.sh", font=ft, fill=DIM, anchor="mm")

        prog = min(1.0, (f + 1) / (n - 6))
        left = int(total * prog)
        shown = []
        for line in CODE:
            if left <= 0:
                break
            shown.append((line[:left], line_color(line)))
            left -= len(line)
        vis = shown[-max_lines:]
        first_no = len(shown) - len(vis) + 1
        for i, (txt, col) in enumerate(vis):
            y = ty + i * lh
            d.text((tx - 20, y), str(first_no + i), font=fm, fill=(60, 70, 105), anchor="ra")
            d.text((tx, y), txt, font=fm, fill=col)
        if vis and (f // 4) % 2 == 0 and prog < 1.0:
            last_txt = vis[-1][0]
            cx = tx + fm.getlength(last_txt) + 2
            cy = ty + (len(vis) - 1) * lh
            d.rectangle([cx, cy + 2, cx + 11, cy + 22], fill=ACC)

        d.text((tx, 592), "building boot image", font=ft, fill=DIM)
        d.rounded_rectangle([tx, 618, tx + 600, 624], 3, fill=(28, 38, 70))
        if prog > 0:
            d.rounded_rectangle([tx, 618, tx + int(600 * prog), 624], 3, fill=ACC)
        d.text((tx + 620, 621), f"{int(prog * 100)}%", font=ft, fill=DIM, anchor="lm")
        frames.append(img)
    return frames


# =====================================================================
# part1: generic game-client splash (invented logo, not a real brand)
# =====================================================================
def spaced_text(d, cx, y, text, fnt, fill, spacing):
    widths = [fnt.getlength(c) for c in text]
    total = sum(widths) + spacing * (len(text) - 1)
    x = cx - total / 2
    for c, w in zip(text, widths):
        d.text((x, y), c, font=fnt, fill=fill, anchor="lm")
        x += w + spacing


def draw_logo(d, cx, cy, r, color):
    pts = [(cx + r * math.cos(math.radians(60 * i - 30)), cy + r * math.sin(math.radians(60 * i - 30))) for i in range(6)]
    d.polygon(pts, outline=color, width=7)
    t = r * 0.42
    d.polygon([(cx - t * 0.55, cy - t), (cx - t * 0.55, cy + t), (cx + t * 0.95, cy)], fill=color)


def scene_splash(n):
    frames = []
    ftitle = font(SANS_B, 44)
    fsub = font(SANS, 20)
    cx, cy = 640, 290
    steps = ["Starting game client", "Loading library", "Ready"]
    for f in range(n):
        img = BACK.copy()
        d = ImageDraw.Draw(img)
        p = (f + 1) / n
        draw_logo(d, cx, cy, 82, ACC)
        a = f * 16
        d.arc([cx - 135, cy - 135, cx + 135, cy + 135], a, a + 110, fill=ACC2, width=6)
        d.arc([cx - 135, cy - 135, cx + 135, cy + 135], a + 180, a + 215, fill=(70, 90, 160), width=6)
        spaced_text(d, 640, 470, "ABYSS PLAY", ftitle, TEXT, 12)
        d.text((640, 525), steps[min(2, int(p * 3))] + ("..." if p < 0.9 else ""), font=fsub, fill=DIM, anchor="mm")
        d.rounded_rectangle([440, 570, 840, 576], 3, fill=(28, 38, 70))
        d.rounded_rectangle([440, 570, 440 + int(400 * ease(p)), 576], 3, fill=ACC)
        frames.append(img)
    return frames


# =====================================================================
# part2 + part3: Abyss Home appears, then idles
# =====================================================================
TILES = [
    ("Jellyfin", (170, 92, 195)),
    ("Kodi", (23, 178, 231)),
    ("Plex", (229, 160, 13)),
    ("Files", ACC),
    ("Settings", DIM),
]
TW, TH, GAP = 210, 120, 24
TILE_Y = 330
LEFT = (W - (len(TILES) * TW + (len(TILES) - 1) * GAP)) // 2
_tile_cache = {}


def tile_image(label, tint, focus):
    key = (label, focus)
    if key in _tile_cache:
        return _tile_cache[key]
    tile = Image.new("RGB", (TW, TH))
    d = ImageDraw.Draw(tile)
    d.rectangle([0, 0, TW, TH], fill=mix(SURF, tint, 0.34))  # flat fill: PNG palettes can't hold smooth gradients
    rgba = tile.convert("RGBA")
    mask = Image.new("L", (TW, TH), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, TW - 1, TH - 1], 20, fill=255)
    rgba.putalpha(mask)
    od = ImageDraw.Draw(rgba)
    od.rounded_rectangle([0, 0, TW - 1, TH - 1], 20, outline=ACC if focus else (45, 55, 90), width=3 if focus else 1)
    od.text((18, TH - 18), label, font=font(SANS_B, 22), fill=TEXT, anchor="ls")
    _tile_cache[key] = rgba
    return rgba


def paste_alpha(img, layer, pos, alpha):
    if alpha < 1.0:
        layer = layer.copy()
        layer.putalpha(layer.getchannel("A").point(lambda v: int(v * alpha)))
    img.paste(layer, pos, layer)


def draw_glow(img, x, y, w, h, intensity):
    if intensity <= 0:
        return
    pad = 120
    layer = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    ImageDraw.Draw(layer).rounded_rectangle([pad - 4, pad - 4, pad + w + 4, pad + h + 4], 26, fill=ACC + (255,))
    layer = layer.filter(ImageFilter.GaussianBlur(26))
    layer.putalpha(layer.getchannel("A").point(lambda v: int(v * intensity)))
    img.paste(layer, (x - pad, y - pad), layer)


def home_frame(f, intro_frames, focus_start, glow):
    """One frame of the Abyss Home layout. f counts from 0 in part2; part3 passes a large f."""
    img = BACK.copy()
    d = ImageDraw.Draw(img)
    head = ease(f / 10)
    d.text((80, 70 - int((1 - head) * 14)), "Abyss Home", font=font(SANS_B, 44), fill=mix(BG, TEXT, head))
    d.text((80, 128), "your media, no ads", font=font(SANS, 20), fill=mix(BG, DIM, head))

    focus_t = ease((f - focus_start) / 8)
    for i, (label, tint) in enumerate(TILES):
        t = ease((f - (3 + i * 4)) / 12)
        if t <= 0:
            continue
        x = LEFT + i * (TW + GAP)
        y = TILE_Y + int((1 - t) * 70)
        if i == 0 and focus_t > 0:
            sf = 1 + 0.08 * focus_t
            tile = tile_image(label, tint, True)
            w2, h2 = int(TW * sf), int(TH * sf)
            tile = tile.resize((w2, h2), Image.LANCZOS)
            gx, gy = x - (w2 - TW) // 2, y - (h2 - TH) // 2
            draw_glow(img, gx, gy, w2, h2, glow * focus_t)
            paste_alpha(img, tile, (gx, gy), t)
        else:
            paste_alpha(img, tile_image(label, tint, False), (x, y), t)

    # dummy "all apps" row
    rowt = ease((f - 24) / 10)
    if rowt > 0:
        d = ImageDraw.Draw(img)
        d.text((LEFT, 520), "All apps", font=font(SANS, 15), fill=mix(BG, DIM, rowt))
        for i in range(9):
            x = LEFT + i * 84
            d.rounded_rectangle([x, 552, x + 56, 608], 14, fill=mix(BG, (28, 38, 70), rowt))
    return img


def scene_home(n):
    return [home_frame(f, n, 24, 0.55) for f in range(n)]


def scene_idle(n):
    return [home_frame(999, n, 0, 0.55 + 0.2 * math.sin(2 * math.pi * i / n)) for i in range(n)]


# =====================================================================
# assemble
# =====================================================================
def save_part(name, frames):
    folder = os.path.join(BUILD, name)
    os.makedirs(folder, exist_ok=True)
    for i, im in enumerate(frames):
        im.quantize(colors=256, method=Image.MEDIANCUT, dither=Image.FLOYDSTEINBERG).save(
            os.path.join(folder, f"{i:04d}.png"), optimize=True
        )


def main():
    if os.path.isdir(BUILD):
        shutil.rmtree(BUILD)
    os.makedirs(BUILD)

    p0 = scene_code(66)
    p1 = scene_splash(38)
    p2 = scene_home(40)
    p3 = scene_idle(20)

    for i in range(6):  # cross-fades between parts
        p0[-1 - i] = fade(p0[-1 - i], i / 6 + 0.05)
        p1[i] = fade(p1[i], (i + 1) / 6)
        p1[-1 - i] = fade(p1[-1 - i], i / 6 + 0.05)
        p2[i] = fade(p2[i], (i + 1) / 6)

    parts = [("part0", p0), ("part1", p1), ("part2", p2), ("part3", p3)]
    for name, frames in parts:
        save_part(name, frames)

    with open(os.path.join(BUILD, "desc.txt"), "w", newline="\n") as fh:
        fh.write(f"{W} {H} {FPS}\n")
        fh.write("p 1 0 part0\np 1 0 part1\np 1 0 part2\np 0 0 part3\n")

    zip_path = os.path.join(HERE, "bootanimation.zip")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_STORED) as z:  # must be stored, not compressed
        z.write(os.path.join(BUILD, "desc.txt"), "desc.txt")
        for name, frames in parts:
            for i in range(len(frames)):
                z.write(os.path.join(BUILD, name, f"{i:04d}.png"), f"{name}/{i:04d}.png")

    gif = [im.resize((640, 360), Image.LANCZOS).quantize(colors=96) for _, fr in parts for im in fr]
    gif[0].save(os.path.join(HERE, "preview.gif"), save_all=True, append_images=gif[1:], duration=int(1000 / FPS), loop=0, optimize=True)
    print("zip:", os.path.getsize(zip_path) // 1024, "KB   frames:", sum(len(f) for _, f in parts))


if __name__ == "__main__":
    sys.exit(main())
