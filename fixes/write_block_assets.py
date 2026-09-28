#!/usr/bin/env python3
"""Writes blockstates/models/textures/lang for space_scanner, ground_comm, monitor, tars items."""
from pathlib import Path
import json, struct, zlib

ROOT = Path("src/main/resources/assets/simplespace")
BLOCK = ROOT / "textures" / "block"
ITEM = ROOT / "textures" / "item"
MODELS_B = ROOT / "models" / "block"
MODELS_I = ROOT / "models" / "item"
STATES = ROOT / "blockstates"
LANG = ROOT / "lang"

for p in (BLOCK, ITEM, MODELS_B, MODELS_I, STATES, LANG):
    p.mkdir(parents=True, exist_ok=True)


def write_png(path: Path, w: int, h: int, fn):
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw += bytes(fn(x, y, w, h))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def metal(x, y, w, h):
    c = 90 + (x + y) % 20
    return (c, c + 5, c + 10, 255)


def panel(x, y, w, h, base, accent):
    if x in (0, w - 1) or y in (0, h - 1):
        return (accent[0] // 2, accent[1] // 2, accent[2] // 2, 255)
    if 4 <= x <= 11 and 4 <= y <= 11:
        return (*accent, 255)
    n = 1.0 if (x + y) % 3 else 0.85
    return (int(base[0] * n), int(base[1] * n), int(base[2] * n), 255)


# Textures
tex_map = {
    "space_scanner": lambda x, y, w, h: panel(x, y, w, h, (30, 50, 70), (40, 200, 220)),
    "ground_comm": lambda x, y, w, h: panel(x, y, w, h, (40, 45, 55), (80, 140, 255)),
    "monitor": lambda x, y, w, h: panel(x, y, w, h, (20, 22, 26), (180, 185, 190)),
    "scanner_body": metal,
    "scanner_front": metal,
    "scanner_base": metal,
    "scanner_dish": metal,
    "comm_casing": metal,
    "comm_panel": metal,
    "comm_screen": metal,
    "monitor_screen": lambda x, y, w, h: (14, 15, 17, 255),
    "monitor_frame": metal,
    "monitor_back": metal,
}
for name, fn in tex_map.items():
    p = BLOCK / f"{name}.png"
    if not p.exists():
        write_png(p, 16, 16, fn)

for name in [
    "space_scanner",
    "ground_comm",
    "monitor",
    "tars_core",
    "tars_manual",
    "tars_dock",
    "flash_l1",
    "flash_l2",
    "flash_l3",
    "flash_l4",
    "flash_l5",
]:
    write_png(ITEM / f"{name}.png", 16, 16, metal)

# Dock blockstate
(STATES / "tars_dock.json").write_text(
    json.dumps({"variants": {"": {"model": "simplespace:block/tars_dock"}}}) + "\n"
)

# Facing blocks
for b in ("space_scanner", "ground_comm", "monitor"):
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        entry = {"model": f"simplespace:block/{b}"}
        if y:
            entry["y"] = y
        variants[f"facing={facing}"] = entry
    # monitor also needs up/down/left/right — fallback simple facing if multipart missing
    if b == "monitor":
        # simple cube until gen_tars_blocks provides multipart
        pass
    (STATES / f"{b}.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    (MODELS_B / f"{b}.json").write_text(
        json.dumps(
            {"parent": "minecraft:block/cube_all", "textures": {"all": f"simplespace:block/{b}"}}
        )
        + "\n"
    )
    (MODELS_I / f"{b}.json").write_text(
        json.dumps({"parent": f"simplespace:block/{b}"}) + "\n"
    )

(MODELS_I / "tars_dock.json").write_text(
    json.dumps({"parent": "simplespace:block/tars_dock"}) + "\n"
)

ru = {
    "item.simplespace.tars_core": "Монолит TARS",
    "item.simplespace.tars_manual": "Пособие TARS",
    "item.simplespace.tars_dock": "Порт TARS",
    "block.simplespace.tars_dock": "Порт TARS",
    "block.simplespace.space_scanner": "Космический сенсор",
    "item.simplespace.space_scanner": "Космический сенсор",
    "block.simplespace.ground_comm": "Наземная станция связи",
    "item.simplespace.ground_comm": "Наземная станция связи",
    "block.simplespace.monitor": "Монитор",
    "item.simplespace.monitor": "Монитор",
    "item.simplespace.flash_l1": "Флешка TARS L1",
    "item.simplespace.flash_l2": "Флешка TARS L2",
    "item.simplespace.flash_l3": "Флешка TARS L3",
    "item.simplespace.flash_l4": "Флешка TARS L4",
    "item.simplespace.flash_l5": "Флешка TARS L5",
}
en = {
    "item.simplespace.tars_core": "TARS Monolith",
    "block.simplespace.space_scanner": "Space Scanner",
    "item.simplespace.space_scanner": "Space Scanner",
    "block.simplespace.ground_comm": "Ground Comm",
    "item.simplespace.ground_comm": "Ground Comm",
    "block.simplespace.monitor": "Monitor",
    "item.simplespace.monitor": "Monitor",
    "block.simplespace.tars_dock": "TARS Port",
}
(LANG / "ru_ru.json").write_text(json.dumps(ru, ensure_ascii=False, indent=2) + "\n")
(LANG / "en_us.json").write_text(json.dumps(en, indent=2) + "\n")
print("write_block_assets ok")
