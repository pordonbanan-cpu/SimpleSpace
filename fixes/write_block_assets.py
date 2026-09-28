#!/usr/bin/env python3
"""Writes blockstates/models/textures/lang. Does not overwrite existing model files from gens."""
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


tex_map = {
    "space_scanner": lambda x, y, w, h: panel(x, y, w, h, (30, 50, 70), (40, 200, 220)),
    "ground_comm": lambda x, y, w, h: panel(x, y, w, h, (40, 45, 55), (80, 140, 255)),
    "monitor": lambda x, y, w, h: panel(x, y, w, h, (20, 22, 26), (180, 185, 190)),
    "airlock_door": lambda x, y, w, h: panel(x, y, w, h, (220, 222, 226), (60, 62, 66)),
}
for name, fn in tex_map.items():
    p = BLOCK / f"{name}.png"
    if not p.exists():
        write_png(p, 16, 16, fn)

for name in [
    "space_scanner", "ground_comm", "monitor", "airlock_door",
    "tars_core", "tars_manual", "tars_dock",
    "flash_l1", "flash_l2", "flash_l3", "flash_l4", "flash_l5",
]:
    write_png(ITEM / f"{name}.png", 16, 16, metal)

if not (STATES / "tars_dock.json").exists():
    (STATES / "tars_dock.json").write_text(
        json.dumps({"variants": {"": {"model": "simplespace:block/tars_dock"}}}) + "\n"
    )

for b in ("space_scanner", "ground_comm", "monitor"):
    if (STATES / f"{b}.json").exists():
        continue
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        entry = {"model": f"simplespace:block/{b}"}
        if y:
            entry["y"] = y
        variants[f"facing={facing}"] = entry
    (STATES / f"{b}.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    if not (MODELS_B / f"{b}.json").exists():
        (MODELS_B / f"{b}.json").write_text(
            json.dumps({"parent": "minecraft:block/cube_all", "textures": {"all": f"simplespace:block/{b}"}}) + "\n"
        )
    if not (MODELS_I / f"{b}.json").exists():
        (MODELS_I / f"{b}.json").write_text(json.dumps({"parent": f"simplespace:block/{b}"}) + "\n")

# Airlock: if gen did not run, simple cube variants for all properties
if not (STATES / "airlock_door.json").exists():
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for part in ("bottom_right", "bottom_left", "top_right", "top_left"):
            for status in ("closed", "open", "moving"):
                key = f"facing={facing},part={part},status={status}"
                entry = {"model": "simplespace:block/airlock_door"}
                if y:
                    entry["y"] = y
                variants[key] = entry
    (STATES / "airlock_door.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    if not (MODELS_B / "airlock_door.json").exists():
        (MODELS_B / "airlock_door.json").write_text(
            json.dumps({"parent": "minecraft:block/cube_all", "textures": {"all": "simplespace:block/airlock_door"}}) + "\n"
        )
    if not (MODELS_I / "airlock_door.json").exists():
        (MODELS_I / "airlock_door.json").write_text(
            json.dumps({"parent": "simplespace:block/airlock_door"}) + "\n"
        )

if not (MODELS_I / "tars_dock.json").exists():
    (MODELS_I / "tars_dock.json").write_text(json.dumps({"parent": "simplespace:block/tars_dock"}) + "\n")

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
    "block.simplespace.airlock_door": "Шлюзовой люк",
    "item.simplespace.airlock_door": "Шлюзовой люк",
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
    "block.simplespace.airlock_door": "Airlock Hatch",
    "item.simplespace.airlock_door": "Airlock Hatch",
    "block.simplespace.tars_dock": "TARS Port",
}
# merge with existing lang from gens
for path, data in ((LANG / "ru_ru.json", ru), (LANG / "en_us.json", en)):
    existing = {}
    if path.exists():
        try:
            existing = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            pass
    existing.update(data)
    path.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("write_block_assets ok")
