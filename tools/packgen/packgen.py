import argparse
import hashlib
import json
import struct
import shutil
import zlib
import zipfile
from pathlib import Path


def hex_to_rgb(value):
    value = value.lstrip("#")
    if len(value) != 6:
        return (255, 255, 255, 255)
    rgb = tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))
    return (rgb[0], rgb[1], rgb[2], 255)


def shade(color, factor):
    if len(color) == 4:
        r, g, b, a = color
    else:
        r, g, b = color
        a = 255
    return (
        max(0, min(255, int(r * factor))),
        max(0, min(255, int(g * factor))),
        max(0, min(255, int(b * factor))),
        a,
    )


def parse_items_yaml(path):
    items = []
    current = None
    in_lore = False
    lore = []

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.rstrip()
        if not line or line.lstrip().startswith("#"):
            continue

        indent = len(line) - len(line.lstrip(" "))
        content = line.strip()

        if indent == 0 and content.endswith(":"):
            if current:
                if lore:
                    current["lore"] = lore
                items.append(current)
            current = {"_key": content[:-1]}
            in_lore = False
            lore = []
            continue

        if current is None:
            continue

        if indent == 2 and content.startswith("lore:"):
            in_lore = True
            lore = []
            continue

        if indent == 2 and ":" in content:
            in_lore = False
            key, val = content.split(":", 1)
            val = val.strip()
            if val.startswith("\"") and val.endswith("\""):
                val = val[1:-1]
            elif val.startswith("'") and val.endswith("'"):
                val = val[1:-1]
            current[key] = val
            continue

        if in_lore and content.startswith("- "):
            lore.append(content[2:])

    if current:
        if lore:
            current["lore"] = lore
        items.append(current)

    return items


def load_items(items_dir):
    items = []
    for path in items_dir.glob("*.yml"):
        for raw in parse_items_yaml(path):
            item_id = raw.get("id") or raw.get("_key")
            if not item_id:
                continue
            items.append({
                "id": item_id,
                "material": (raw.get("material") or "PAPER").upper(),
                "cmd": int(raw.get("custom_model_data") or 0),
                "color": raw.get("color") or "#FFFFFF",
            })
    return items


def assign_cmd(items):
    used = {i["cmd"] for i in items if i["cmd"] > 0}
    counters = {"ui": 9000, "rune": 1000, "item": 2000}
    for item in items:
        if item["cmd"] > 0:
            continue
        kind = guess_kind(item["id"])
        while counters[kind] in used:
            counters[kind] += 1
        item["cmd"] = counters[kind]
        used.add(item["cmd"])
        counters[kind] += 1


def guess_kind(item_id):
    if item_id.startswith("ui_"):
        return "ui"
    if any(x in item_id for x in ["_element", "_shape", "_effect", "_rune"]):
        return "rune"
    return "item"


def new_canvas(color=(0, 0, 0, 0)):
    return [[color for _ in range(16)] for _ in range(16)]


def fill_rect(px, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[y][x] = color


def draw_line(px, x0, y0, x1, y1, color):
    if x0 == x1:
        for y in range(min(y0, y1), max(y0, y1) + 1):
            px[y][x0] = color
        return
    if y0 == y1:
        for x in range(min(x0, x1), max(x0, x1) + 1):
            px[y0][x] = color
        return


def draw_circle(px, cx, cy, r, color):
    r2 = r * r
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                if (x - cx) ** 2 + (y - cy) ** 2 <= r2:
                    px[y][x] = color


def draw_triangle(px, color):
    for y in range(4, 12):
        w = (y - 4) // 2 + 1
        for x in range(8 - w, 8 + w + 1):
            px[y][x] = color


def draw_symbol(px, item_id, base):
    light = shade(base, 1.2)
    dark = shade(base, 0.7)
    if "fire" in item_id:
        draw_triangle(px, light)
    elif "ice" in item_id:
        draw_line(px, 8, 3, 8, 12, light)
        draw_line(px, 3, 8, 12, 8, light)
    elif "arcane" in item_id:
        draw_circle(px, 8, 8, 4, light)
    elif "lightning" in item_id:
        draw_line(px, 5, 3, 9, 7, light)
        draw_line(px, 9, 7, 7, 7, light)
        draw_line(px, 7, 7, 11, 12, light)
    elif "shadow" in item_id:
        draw_line(px, 8, 3, 3, 8, dark)
        draw_line(px, 3, 8, 8, 13, dark)
        draw_line(px, 8, 13, 13, 8, dark)
        draw_line(px, 13, 8, 8, 3, dark)
    elif "projectile" in item_id:
        draw_circle(px, 8, 8, 2, light)
    elif "wave" in item_id:
        draw_line(px, 3, 10, 6, 7, light)
        draw_line(px, 6, 7, 9, 10, light)
        draw_line(px, 9, 10, 12, 7, light)
    elif "nova" in item_id:
        draw_line(px, 8, 3, 8, 12, light)
        draw_line(px, 3, 8, 12, 8, light)
    elif "slow" in item_id:
        draw_line(px, 8, 5, 8, 12, light)
        draw_line(px, 5, 10, 8, 13, light)
        draw_line(px, 11, 10, 8, 13, light)
    elif "shield_break" in item_id:
        fill_rect(px, 5, 4, 10, 11, light)
        draw_line(px, 5, 10, 10, 5, dark)
    elif "none" in item_id:
        draw_circle(px, 8, 8, 3, light)
    else:
        draw_circle(px, 8, 8, 3, light)


def render_item_texture(item_id, color_hex, kind):
    base = hex_to_rgb(color_hex)
    px = new_canvas()

    if kind == "ui":
        bg = shade(base, 0.9)
        fill_rect(px, 0, 0, 15, 15, bg)
        for x in range(0, 16, 2):
            draw_line(px, x, 0, x, 15, shade(base, 0.8))
        fill_rect(px, 0, 0, 15, 0, shade(base, 0.6))
        fill_rect(px, 0, 15, 15, 15, shade(base, 0.6))
        fill_rect(px, 0, 0, 0, 15, shade(base, 0.6))
        fill_rect(px, 15, 0, 15, 15, shade(base, 0.6))
        return px

    border = shade(base, 0.5)
    fill = shade(base, 0.85)
    fill_rect(px, 0, 0, 15, 15, border)
    fill_rect(px, 1, 1, 14, 14, fill)
    draw_symbol(px, item_id, base)
    return px


def write_png(path, px):
    width = 16
    height = 16
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            r, g, b, a = px[y][x]
            raw.extend([r, g, b, a])
    compressed = zlib.compress(bytes(raw))

    def chunk(chunk_type, data):
        return (struct.pack(">I", len(data)) + chunk_type + data +
                struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    path.write_bytes(png)


def write_models_and_textures(items, out_root):
    assets = out_root / "assets"
    tex_dir = assets / "minecraft" / "textures" / "magicacademy" / "item"
    model_item_dir = assets / "minecraft" / "models" / "magicacademy" / "item"
    model_base_dir = assets / "minecraft" / "models" / "item"

    tex_dir.mkdir(parents=True, exist_ok=True)
    model_item_dir.mkdir(parents=True, exist_ok=True)
    model_base_dir.mkdir(parents=True, exist_ok=True)

    overrides = {}
    for item in items:
        item_id = item["id"]
        material = item["material"].lower()
        cmd = item["cmd"]

        overrides.setdefault(material, []).append({
            "predicate": {"custom_model_data": cmd},
            "model": f"magicacademy/item/{item_id}"
        })

        model = {
            "parent": "item/generated",
            "textures": {"layer0": f"magicacademy:item/{item_id}"}
        }
        (model_item_dir / f"{item_id}.json").write_text(
            json.dumps(model, indent=2), encoding="utf-8"
        )

        kind = guess_kind(item_id)
        px = render_item_texture(item_id, item["color"], kind)
        write_png(tex_dir / f"{item_id}.png", px)

    for material, ov in overrides.items():
        ov_sorted = sorted(ov, key=lambda x: x["predicate"]["custom_model_data"])
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"minecraft:item/{material}"},
            "overrides": ov_sorted,
        }
        (model_base_dir / f"{material}.json").write_text(
            json.dumps(model, indent=2), encoding="utf-8"
        )


def write_pack_meta(out_root, cfg):
    pack = cfg.get("pack", {})
    pack_format = int(pack.get("format", 34))
    description = pack.get("description", "Magic Academy Resource Pack")
    meta = {"pack": {"pack_format": pack_format, "description": description}}
    (out_root / "pack.mcmeta").write_text(json.dumps(meta, indent=2), encoding="utf-8")

    pack_png = out_root / "pack.png"
    px = new_canvas((90, 40, 120, 255))
    write_png(pack_png, px)


def zip_pack(out_root, zip_path):
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in out_root.rglob("*"):
            if path.is_file():
                zf.write(path, path.relative_to(out_root))


def sha1_file(path):
    h = hashlib.sha1()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def load_or_create_pack_cfg(cfg_path):
    if cfg_path.exists():
        return parse_pack_cfg(cfg_path)
    cfg = {
        "pack": {
            "name": "MagicAcademy",
            "format": 34,
            "description": "Magic Academy Resource Pack",
            "url": "",
            "required": False,
            "prompt": "Magic Academy Resource Pack",
        }
    }
    cfg_path.parent.mkdir(parents=True, exist_ok=True)
    cfg_path.write_text(serialize_pack_cfg(cfg), encoding="utf-8")
    return cfg


def parse_pack_cfg(path):
    data = {"pack": {}}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.endswith(":"):
            continue
        if ":" in line:
            key, val = line.split(":", 1)
            key = key.strip()
            val = val.strip().strip("\"").strip("'")
            if val.lower() in ["true", "false"]:
                val = val.lower() == "true"
            data["pack"][key] = val
    return data


def serialize_pack_cfg(cfg):
    pack = cfg.get("pack", {})
    lines = [
        "pack:",
        f"  name: {pack.get('name', 'MagicAcademy')}",
        f"  format: {pack.get('format', 34)}",
        f"  description: \"{pack.get('description', 'Magic Academy Resource Pack')}\"",
        f"  url: \"{pack.get('url', '')}\"",
        f"  required: {str(pack.get('required', False)).lower()}",
        f"  prompt: \"{pack.get('prompt', 'Magic Academy Resource Pack')}\"",
    ]
    return "\n".join(lines) + "\n"


def update_server_properties(server_props, cfg, sha1):
    pack = cfg.get("pack", {})
    url = str(pack.get("url") or "").strip()
    if not url:
        return False

    required = "true" if pack.get("required", False) else "false"
    prompt = str(pack.get("prompt", "Magic Academy Resource Pack"))

    lines = []
    seen = set()
    for line in server_props.read_text(encoding="utf-8").splitlines():
        if line.startswith("resource-pack="):
            lines.append(f"resource-pack={url}")
            seen.add("resource-pack")
        elif line.startswith("resource-pack-sha1="):
            lines.append(f"resource-pack-sha1={sha1}")
            seen.add("resource-pack-sha1")
        elif line.startswith("resource-pack-required="):
            lines.append(f"resource-pack-required={required}")
            seen.add("resource-pack-required")
        elif line.startswith("resource-pack-prompt="):
            lines.append(f"resource-pack-prompt={prompt}")
            seen.add("resource-pack-prompt")
        else:
            lines.append(line)

    if "resource-pack" not in seen:
        lines.append(f"resource-pack={url}")
    if "resource-pack-sha1" not in seen:
        lines.append(f"resource-pack-sha1={sha1}")
    if "resource-pack-required" not in seen:
        lines.append(f"resource-pack-required={required}")
    if "resource-pack-prompt" not in seen:
        lines.append(f"resource-pack-prompt={prompt}")

    server_props.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=None, help="Magic Academy root dir")
    args = parser.parse_args()

    root = Path(args.root).resolve() if args.root else Path(__file__).resolve().parents[2]
    items_dir = root / "server" / "plugins" / "MagicAcademy" / "items"
    pack_cfg_path = root / "server" / "resource_pack" / "pack.yml"
    pack_cfg = load_or_create_pack_cfg(pack_cfg_path)

    pack_name = pack_cfg.get("pack", {}).get("name", "MagicAcademy")
    out_root = root / "server" / "resource_pack" / pack_name
    zip_path = root / "server" / "resource_pack" / f"{pack_name}.zip"

    items = load_items(items_dir)
    assign_cmd(items)

    if out_root.exists():
        shutil.rmtree(out_root)

    out_root.mkdir(parents=True, exist_ok=True)
    write_models_and_textures(items, out_root)
    write_pack_meta(out_root, pack_cfg)
    zip_pack(out_root, zip_path)

    sha1 = sha1_file(zip_path)
    server_props = root / "server" / "server.properties"
    updated = False
    if server_props.exists():
        updated = update_server_properties(server_props, pack_cfg, sha1)

    print(f"Pack: {zip_path}")
    print(f"SHA1: {sha1}")
    if not updated:
        print("Set pack URL in server/resource_pack/pack.yml to update server.properties.")


if __name__ == "__main__":
    main()
