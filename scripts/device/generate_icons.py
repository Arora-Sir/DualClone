"""Generate pixel-perfect DualClone application icons across all densities.

Creates:
- 512x512 store asset: app/src/main/res/drawable/app_icon_512.png
- Adaptive raster mipmaps: mdpi (48), hdpi (72), xhdpi (96), xxhdpi (144), xxxhdpi (192)
- Adaptive foreground: app/src/main/res/drawable/ic_launcher_foreground.png (432x432)
- Standard and round launcher mipmaps.
Uses 4x supersampling with PIL Lanczos antialiasing for maximum sharpness.
Glyph strictly occupies 52% of the canvas, ensuring ample padding inside the One UI squircle.
"""

from __future__ import annotations

import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_foreground_glyph(canvas_size: int) -> Image.Image:
    """Draw the centered DualClone glyph with transparent outer margins."""
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    cx, cy = canvas_size / 2, canvas_size / 2

    # Card dimensions: scaled to fit comfortably inside 52% of canvas
    card_w = canvas_size * 0.30
    card_h = canvas_size * 0.30
    card_r = canvas_size * 0.07

    # Card 1: Primary System Card (Bottom-left offset, User 0)
    c1_x = cx - canvas_size * 0.20
    c1_y = cy - canvas_size * 0.08

    # Card 1 drop shadow
    c1_shadow = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    c1_shadow_draw = ImageDraw.Draw(c1_shadow)
    c1_shadow_draw.rounded_rectangle(
        [c1_x, c1_y + canvas_size * 0.02, c1_x + card_w, c1_y + card_h + canvas_size * 0.02],
        radius=card_r,
        fill=(0, 0, 0, 150),
    )
    c1_shadow = c1_shadow.filter(ImageFilter.GaussianBlur(radius=canvas_size * 0.025))
    img = Image.alpha_composite(img, c1_shadow)

    # Card 1 body (Dark indigo glassmorphism)
    c1_tile = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    c1_tile_draw = ImageDraw.Draw(c1_tile)
    c1_tile_draw.rounded_rectangle(
        [c1_x, c1_y, c1_x + card_w, c1_y + card_h],
        radius=card_r,
        fill=(32, 24, 54, 235),
        outline=(98, 74, 152, 255),
        width=int(canvas_size * 0.007),
    )
    c1_cx = c1_x + card_w / 2
    c1_cy = c1_y + card_h / 2
    c1_tile_draw.ellipse(
        [c1_cx - card_w * 0.18, c1_cy - card_h * 0.18, c1_cx + card_w * 0.18, c1_cy + card_h * 0.18],
        outline=(150, 130, 200, 160),
        width=int(canvas_size * 0.006),
    )
    img = Image.alpha_composite(img, c1_tile)

    # Card 2: Cloned Card (Top-right offset, User 95 / Secondary)
    c2_x = cx - canvas_size * 0.06
    c2_y = cy - canvas_size * 0.17

    # Card 2 drop shadow
    c2_shadow = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    c2_shadow_draw = ImageDraw.Draw(c2_shadow)
    c2_shadow_draw.rounded_rectangle(
        [c2_x, c2_y + canvas_size * 0.025, c2_x + card_w, c2_y + card_h + canvas_size * 0.025],
        radius=card_r,
        fill=(0, 0, 0, 180),
    )
    c2_shadow = c2_shadow.filter(ImageFilter.GaussianBlur(radius=canvas_size * 0.03))
    img = Image.alpha_composite(img, c2_shadow)

    # Card 2 body with electric gradient (Purple to Samsung Dual Messenger Orange)
    c2_mask = Image.new("L", (canvas_size, canvas_size), 0)
    c2_mask_draw = ImageDraw.Draw(c2_mask)
    c2_mask_draw.rounded_rectangle(
        [c2_x, c2_y, c2_x + card_w, c2_y + card_h],
        radius=card_r,
        fill=255,
    )

    c2_grad = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    for px in range(int(c2_x), int(c2_x + card_w)):
        for py in range(int(c2_y), int(c2_y + card_h)):
            tx = (px - c2_x) / card_w
            ty = (py - c2_y) / card_h
            t = (tx + ty) / 2.0
            r = int(124 * (1 - t) + 255 * t)
            g = int(58 * (1 - t) + 109 * t)
            b = int(237 * (1 - t) + 0 * t)
            c2_grad.putpixel((px, py), (r, g, b, 255))

    img.paste(c2_grad, (0, 0), c2_mask)

    # Card 2 inner highlight border
    c2_border = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    c2_border_draw = ImageDraw.Draw(c2_border)
    c2_border_draw.rounded_rectangle(
        [c2_x, c2_y, c2_x + card_w, c2_y + card_h],
        radius=card_r,
        fill=(0, 0, 0, 0),
        outline=(255, 255, 255, 190),
        width=int(canvas_size * 0.006),
    )
    img = Image.alpha_composite(img, c2_border)

    # Card 2 center glyph: Dual interlocking parallel rings
    c2_cx = c2_x + card_w / 2
    c2_cy = c2_y + card_h / 2
    ring_r = card_w * 0.16
    ring_w = int(canvas_size * 0.009)

    c2_glyph = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    c2_glyph_draw = ImageDraw.Draw(c2_glyph)
    # Left ring
    c2_glyph_draw.ellipse(
        [c2_cx - ring_r * 1.3, c2_cy - ring_r, c2_cx + ring_r * 0.3, c2_cy + ring_r],
        outline=(255, 255, 255, 245),
        width=ring_w,
    )
    # Right ring
    c2_glyph_draw.ellipse(
        [c2_cx - ring_r * 0.3, c2_cy - ring_r, c2_cx + ring_r * 1.3, c2_cy + ring_r],
        outline=(255, 255, 255, 245),
        width=ring_w,
    )
    img = Image.alpha_composite(img, c2_glyph)

    # Dual Messenger Orange circular badge with twin white connector dots
    badge_r = canvas_size * 0.058
    badge_cx = c2_x + card_w - canvas_size * 0.015
    badge_cy = c2_y + canvas_size * 0.015

    badge = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    badge_draw = ImageDraw.Draw(badge)
    # Badge shadow
    badge_draw.ellipse(
        [badge_cx - badge_r, badge_cy - badge_r + canvas_size * 0.008, badge_cx + badge_r, badge_cy + badge_r + canvas_size * 0.008],
        fill=(0, 0, 0, 160),
    )
    badge = badge.filter(ImageFilter.GaussianBlur(radius=canvas_size * 0.012))
    img = Image.alpha_composite(img, badge)

    # Badge body: Solid neon orange
    badge_body = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    badge_b_draw = ImageDraw.Draw(badge_body)
    badge_b_draw.ellipse(
        [badge_cx - badge_r, badge_cy - badge_r, badge_cx + badge_r, badge_cy + badge_r],
        fill=(255, 109, 0, 255),
        outline=(255, 255, 255, 230),
        width=int(canvas_size * 0.005),
    )
    # Twin connector dots inside badge
    dot_r = badge_r * 0.28
    badge_b_draw.ellipse(
        [badge_cx - dot_r * 1.5, badge_cy - dot_r, badge_cx - dot_r * 0.2, badge_cy + dot_r],
        fill=(255, 255, 255, 255),
    )
    badge_b_draw.ellipse(
        [badge_cx + dot_r * 0.2, badge_cy - dot_r, badge_cx + dot_r * 1.5, badge_cy + dot_r],
        fill=(255, 255, 255, 255),
    )
    img = Image.alpha_composite(img, badge_body)
    return img


def draw_master_icon(size: int = 1024, is_round: bool = False) -> Image.Image:
    """Generate composed standalone icon for legacy launchers and store assets."""
    canvas_size = size * 2
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))

    cx, cy = canvas_size / 2, canvas_size / 2
    r_outer = canvas_size * 0.44

    # Base background shape (Circle for round, Squircle for standard)
    bg_mask = Image.new("L", (canvas_size, canvas_size), 0)
    bg_mask_draw = ImageDraw.Draw(bg_mask)
    if is_round:
        bg_mask_draw.ellipse(
            [cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
            fill=255,
        )
    else:
        # One UI style squircle (rounded rect with 24% radius)
        corner_r = canvas_size * 0.22
        bg_mask_draw.rounded_rectangle(
            [cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
            radius=corner_r,
            fill=255,
        )

    # Background gradient: Deep obsidian (#0D0B16) to rich midnight violet (#1F1538)
    bg_layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    for y in range(canvas_size):
        t = y / canvas_size
        r = int(13 * (1 - t) + 26 * t)
        g = int(11 * (1 - t) + 18 * t)
        b = int(22 * (1 - t) + 48 * t)
        bg_layer.paste((r, g, b, 255), [0, y, canvas_size, y + 1])

    img.paste(bg_layer, (0, 0), bg_mask)

    # Ambient radial purple glow in center
    glow = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse(
        [cx - r_outer * 0.65, cy - r_outer * 0.65, cx + r_outer * 0.65, cy + r_outer * 0.65],
        fill=(124, 58, 237, 50),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(radius=canvas_size * 0.08))
    img = Image.alpha_composite(img, glow)

    # Composite centered foreground glyph
    glyph = draw_foreground_glyph(canvas_size)
    img = Image.alpha_composite(img, glyph)

    return img.resize((size, size), Image.Resampling.LANCZOS)


def main():
    print("Generating DualClone refined launcher icon suite...", flush=True)

    # 1. 512x512 Store Asset & high-res preview
    icon_512 = draw_master_icon(512, is_round=False)
    store_path = RES / "drawable" / "app_icon_512.png"
    icon_512.save(store_path, "PNG")
    print(f"Saved 512x512 store icon: {store_path}")

    art_dir = Path(__file__).resolve().parents[2] / ".e2e" / "icons"
    art_dir.mkdir(parents=True, exist_ok=True)

    # 2. Adaptive Foreground PNG (432x432 for xxxhdpi)
    fg_canvas = draw_foreground_glyph(864)
    fg_432 = fg_canvas.resize((432, 432), Image.Resampling.LANCZOS)
    fg_path = RES / "drawable" / "ic_launcher_foreground.png"
    fg_432.save(fg_path, "PNG")
    print(f"Saved adaptive foreground: {fg_path}")

    # 3. Generate Mipmap densities
    for folder, px in DENSITIES.items():
        density_dir = RES / folder
        density_dir.mkdir(parents=True, exist_ok=True)

        # Standard squircle
        icon = draw_master_icon(px, is_round=False)
        std_path = density_dir / "ic_launcher.png"
        icon.save(std_path, "PNG")

        # Round icon
        icon_round = draw_master_icon(px, is_round=True)
        round_path = density_dir / "ic_launcher_round.png"
        icon_round.save(round_path, "PNG")

        print(f"Generated {folder} ({px}x{px}): {std_path.name}, {round_path.name}")

    # 4. Generate One UI squircle preview for verification
    squircle_preview = Image.new("RGBA", (512, 512), (32, 33, 36, 255))
    squircle_mask = Image.new("L", (384, 384), 0)
    sq_draw = ImageDraw.Draw(squircle_mask)
    sq_draw.rounded_rectangle([0, 0, 384, 384], radius=88, fill=255)

    full_icon = draw_master_icon(384, is_round=False)
    squircle_preview.paste(full_icon, (64, 64), squircle_mask)
    squircle_preview.save(art_dir / "refined_squircle_preview.png", "PNG")
    print(f"Saved One UI squircle preview: {art_dir / 'refined_squircle_preview.png'}")

    print("All density icons generated successfully.", flush=True)


if __name__ == "__main__":
    main()
