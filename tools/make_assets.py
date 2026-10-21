#!/usr/bin/env python3
"""Hand-placed pixel art for the Soldier Mod.

Everything here is drawn pixel by pixel (no image generation). Run:

    python3 tools/make_assets.py

It writes the 16x16 item icons, six 64x64 soldier skins and the 256x256 mod icon.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "soldier-mod")

SKIN = (226, 178, 140, 255)
SKIN_DARK = (196, 146, 110, 255)
EYE = (44, 38, 34, 255)
GLOVE = (58, 50, 44, 255)
BOOT = (42, 38, 34, 255)
WEB = (34, 32, 30, 255)
GOLD = (232, 186, 74, 255)
RED = (176, 52, 44, 255)
STEEL = (150, 158, 164, 255)
STEEL_DARK = (92, 100, 106, 255)
OUTLINE = (20, 22, 24, 255)
PAPER = (238, 232, 214, 255)
PAPER_DARK = (198, 190, 168, 255)
RADIO = (54, 60, 58, 255)
RADIO_LIGHT = (84, 92, 88, 255)
SCREEN = (108, 196, 128, 255)

# variant palettes: (base, dark, light, helmet)
PALETTES = {
    1: ((86, 102, 66, 255), (58, 72, 44, 255), (112, 128, 88, 255), (52, 62, 40, 255)),    # woodland
    2: ((178, 158, 110, 255), (140, 124, 82, 255), (204, 186, 138, 255), (120, 104, 70, 255)),  # desert
    3: ((110, 114, 118, 255), (78, 82, 86, 255), (142, 146, 150, 255), (66, 70, 74, 255)),   # urban
    4: ((50, 54, 56, 255), (30, 34, 36, 255), (70, 76, 80, 255), (26, 28, 30, 255)),         # black ops
    5: ((198, 206, 212, 255), (152, 162, 170, 255), (228, 234, 238, 255), (130, 140, 150, 255)),  # arctic
    6: ((58, 92, 54, 255), (36, 64, 34, 255), (84, 120, 72, 255), (34, 52, 30, 255)),        # jungle
}


def canvas(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, color)


def box_regions(u, v, w, h, d):
    """Standard Minecraft box-UV unwrap -> (x, y, w, h) per face."""
    return {
        "down": (u + d, v, w, d),
        "up": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def paint_box(img, u, v, w, h, d, color):
    for (rx, ry, rw, rh) in box_regions(u, v, w, h, d).values():
        rect(img, rx, ry, rx + rw - 1, ry + rh - 1, color)


def camo(x, y, base, dark, light):
    """Deterministic three-tone camouflage blotches."""
    h = ((x * 73856093) ^ (y * 19349663) ^ ((x + y) * 83492791)) & 0x7FFFFFFF
    r = h % 100
    if r < 26:
        return dark
    if r < 52:
        return light
    return base


def fill_camo(img, u, v, w, h, d, base, dark, light):
    for name, (rx, ry, rw, rh) in box_regions(u, v, w, h, d).items():
        for y in range(ry, ry + rh):
            for x in range(rx, rx + rw):
                # Keep the blotch pattern continuous across the seam by offsetting
                # each face with its own UV origin.
                px(img, x, y, camo(x + u, y + v, base, dark, light))


# ----------------------------------------------------------------------
# soldier skins (64x64 player layout)
# ----------------------------------------------------------------------

def soldier_skin(variant):
    base, dark, light, helmet = PALETTES[variant]
    img = canvas(64, 64)

    # --- head (8x8x8 at 0,0) ---
    fill_camo(img, 0, 0, 8, 8, 8, base, dark, light)
    hf = box_regions(0, 0, 8, 8, 8)["north"]  # front face at (8,8)
    fx, fy = hf[0], hf[1]
    rect(img, fx, fy + 2, fx + 7, fy + 7, SKIN)          # face
    rect(img, fx, fy + 2, fx + 7, fy + 2, SKIN_DARK)     # brow shadow
    rect(img, fx, fy + 3, fx + 7, fy + 3, helmet)        # helmet brow
    # eyes
    rect(img, fx + 1, fy + 4, fx + 2, fy + 4, EYE)
    rect(img, fx + 5, fy + 4, fx + 6, fy + 4, EYE)
    # camo face paint under the eyes
    px(img, fx + 1, fy + 5, dark)
    px(img, fx + 6, fy + 5, dark)
    # mouth
    rect(img, fx + 3, fy + 6, fx + 4, fy + 6, SKIN_DARK)
    # chin strap
    px(img, fx, fy + 7, WEB)
    px(img, fx + 7, fy + 7, WEB)

    # --- body (8x12x4 at 16,16) ---
    fill_camo(img, 16, 16, 8, 12, 4, base, dark, light)
    bf = box_regions(16, 16, 8, 12, 4)["north"]  # front at (20,20)
    bx, by = bf[0], bf[1]
    rect(img, bx, by, bx + 7, by + 6, dark)              # tactical vest
    rect(img, bx + 1, by + 7, bx + 2, by + 8, WEB)       # pouches
    rect(img, bx + 5, by + 7, bx + 6, by + 8, WEB)
    rect(img, bx + 3, by, bx + 4, by + 1, GOLD)          # rank patch
    rect(img, bx, by + 9, bx + 7, by + 9, WEB)           # belt
    # webbing on the back
    bb = box_regions(16, 16, 8, 12, 4)["south"]
    rect(img, bb[0], bb[1] + 2, bb[0] + 7, bb[1] + 4, dark)

    # --- arms (4x12x4) ---
    for (u, v) in ((40, 16), (32, 48)):
        fill_camo(img, u, v, 4, 12, 4, base, dark, light)
        f = box_regions(u, v, 4, 12, 4)["north"]
        rect(img, f[0], f[1] + 8, f[0] + 3, f[1] + 11, GLOVE)  # gloves
        rect(img, f[0], f[1] + 2, f[0] + 3, f[1] + 2, dark)    # shoulder seam

    # --- legs (4x12x4) ---
    for (u, v) in ((0, 16), (16, 48)):
        fill_camo(img, u, v, 4, 12, 4, base, dark, light)
        f = box_regions(u, v, 4, 12, 4)["north"]
        rect(img, f[0], f[1] + 9, f[0] + 3, f[1] + 11, BOOT)   # boots
        rect(img, f[0], f[1] + 8, f[0] + 3, f[1] + 8, WEB)     # boot top

    # --- helmet overlay (hat layer, 8x8x8 at 32,0) ---
    hat = box_regions(32, 0, 8, 8, 8)
    for name, (rx, ry, rw, rh) in hat.items():
        if name == "north":
            rect(img, rx, ry, rx + rw - 1, ry + 3, helmet)      # only the brow
            rect(img, rx, ry + 3, rx + rw - 1, ry + 3, dark)
        else:
            rect(img, rx, ry, rx + rw - 1, ry + rh - 1, helmet)
    # highlight + strap on the helmet
    for (rx, ry, rw, rh) in (hat["down"], hat["up"], hat["south"], hat["west"], hat["east"]):
        rect(img, rx + 1, ry + 1, rx + rw - 2, ry + 1, light)
    return img


# ----------------------------------------------------------------------
# item icons (16x16)
# ----------------------------------------------------------------------

def icon_papers():
    img = canvas(16, 16)
    rect(img, 3, 2, 12, 13, PAPER)
    rect(img, 3, 2, 12, 2, PAPER_DARK)
    for y in (5, 7, 9):
        rect(img, 5, y, 10, y, PAPER_DARK)
    # gold star
    px(img, 6, 3, GOLD)
    px(img, 5, 4, GOLD)
    px(img, 6, 4, GOLD)
    px(img, 7, 4, GOLD)
    px(img, 6, 5, GOLD)
    # red wax seal
    rect(img, 10, 11, 12, 12, RED)
    px(img, 11, 10, RED)
    px(img, 11, 13, RED)
    # outline
    for x in range(3, 13):
        px(img, x, 1, OUTLINE)
        px(img, x, 14, OUTLINE)
    for y in range(1, 15):
        px(img, 2, y, OUTLINE)
        px(img, 13, y, OUTLINE)
    return img


def icon_radio():
    img = canvas(16, 16)
    # antenna
    rect(img, 3, 1, 3, 4, STEEL_DARK)
    px(img, 3, 1, STEEL)
    # body
    rect(img, 2, 5, 12, 14, RADIO)
    rect(img, 2, 5, 12, 5, RADIO_LIGHT)
    # screen
    rect(img, 4, 6, 10, 9, SCREEN)
    rect(img, 5, 7, 9, 8, (40, 90, 56, 255))
    # grille
    for y in (11, 12):
        rect(img, 4, y, 10, y, RADIO_LIGHT)
    # buttons
    px(img, 11, 12, RED)
    px(img, 12, 12, GOLD)
    # outline
    for x in range(2, 13):
        px(img, x, 4, OUTLINE)
        px(img, x, 15, OUTLINE)
    for y in range(4, 16):
        px(img, 1, y, OUTLINE)
        px(img, 13, y, OUTLINE)
    return img


# ----------------------------------------------------------------------
# mod icon (256x256)
# ----------------------------------------------------------------------

def line(img, x0, y0, x1, y1, color, thickness=1):
    """Bresenham-ish line with a square brush."""
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    half = thickness // 2
    while True:
        rect(img, x0 - half, y0 - half, x0 - half + thickness - 1, y0 - half + thickness - 1, color)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


def make_icon():
    img = canvas(256, 256)
    # vignetted olive background
    for y in range(256):
        for x in range(256):
            d = (((x - 128) ** 2 + (y - 120) ** 2) ** 0.5) / 180.0
            d = min(1.0, d)
            r = int(58 - 30 * d)
            g = int(82 - 40 * d)
            b = int(44 - 24 * d)
            px(img, x, y, (r, g, b, 255))

    # a soft diagonal light band across the middle
    band = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    for i in range(34):
        line(band, 0, 78 + i, 256, 30 + i, (255, 255, 255, 8), 1)
    img.alpha_composite(band)

    # helmet dome (half ellipse) + brim
    dome = (154, 164, 152, 255)
    dome_dark = (104, 114, 104, 255)
    dome_light = (196, 204, 192, 255)
    cx, cy, rx, ry = 128, 128, 74, 66
    for y in range(cy - ry, cy):
        t = (y - cy) / float(ry)
        half = int(rx * (1 - t * t) ** 0.5)
        rect(img, cx - half, y, cx + half, y, dome)
    for y in range(cy - ry, cy - ry + 8):
        t = (y - cy) / float(ry)
        half = int(rx * (1 - t * t) ** 0.5)
        rect(img, cx - half + 6, y, cx + half - 6, y, dome_light)
    # brim
    rect(img, cx - rx - 12, cy, cx + rx + 12, cy + 12, dome)
    rect(img, cx - rx - 12, cy + 13, cx + rx + 12, cy + 15, dome_dark)
    rect(img, cx - rx - 12, cy, cx + rx + 12, cy, dome_light)
    # chin strap under the brim
    rect(img, cx - 6, cy + 16, cx - 1, cy + 52, dome_dark)
    rect(img, cx + 2, cy + 16, cx + 7, cy + 52, dome_dark)

    # three gold chevrons pointing up
    for row in range(3):
        top = 196 + row * 20
        line(img, cx - 44, top + 16, cx, top, GOLD, 7)
        line(img, cx, top, cx + 44, top + 16, GOLD, 7)
    return img


def main():
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)

    icon_papers().save(os.path.join(ASSETS, "textures", "item", "enlistment_papers.png"))
    icon_radio().save(os.path.join(ASSETS, "textures", "item", "squad_radio.png"))

    for variant in range(1, 7):
        soldier_skin(variant).save(os.path.join(ASSETS, "textures", "entity", f"soldier_{variant}.png"))

    make_icon().save(os.path.join(ASSETS, "icon.png"))
    print("wrote assets to", ASSETS)


if __name__ == "__main__":
    main()
