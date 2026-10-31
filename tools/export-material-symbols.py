#!/usr/bin/env python3
"""Rebuild the bundled catalogs from Google's Material Symbols variable fonts.

Install fonttools==4.68.0, then run:
  python tools/export-material-symbols.py --fonts-dir <variablefont-directory> \
      --codepoints <outlined-codepoints-file> --license <Google-LICENSE>

Inputs come from google/material-design-icons at --commit. The three TTF names
are MaterialSymbols{Outlined,Rounded,Sharp}[FILL,GRAD,opsz,wght].ttf.
"""

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

import fontTools
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont


ROOT = Path(__file__).resolve().parents[1]
COMMIT = "10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7"
STYLES = {"outline": "Outlined", "round": "Rounded", "sharp": "Sharp"}
AXES = {"FILL": 0, "GRAD": 0, "opsz": 24, "wght": 400}
ZIP_TIMESTAMP = (2026, 10, 31, 0, 0, 0)


def export_catalog(font_path, names):
    with TTFont(font_path) as font:
        scale = 24 / font["head"].unitsPerEm
        cmap = font.getBestCmap()
        glyphs = font.getGlyphSet(location=AXES)
        drawings = {}
        catalog = {}
        for name, code in sorted(names.items()):
            glyph_name = cmap[int(code, 16)]
            if glyph_name not in drawings:
                pen = SVGPathPen(glyphs, ntos=lambda value: format(round(value, 4), "g"))
                glyphs[glyph_name].draw(TransformPen(pen, (scale, 0, 0, -scale, 0, 24)))
                path = pen.getCommands()
                if not path:
                    raise ValueError(f"Empty glyph: {font_path.name}/{name}")
                drawings[glyph_name] = (
                    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" '
                    f'viewBox="0 0 24 24"><path d="{path}"/></svg>'
                )
            catalog[name] = drawings[glyph_name]
        return catalog


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fonts-dir", type=Path, required=True)
    parser.add_argument("--codepoints", type=Path, required=True)
    parser.add_argument("--license", type=Path, required=True)
    parser.add_argument("--commit", default=COMMIT)
    parser.add_argument("--output-dir", type=Path, default=ROOT / "app/src/main/assets/icons")
    args = parser.parse_args()
    names = dict(line.split() for line in args.codepoints.read_text(encoding="utf-8").splitlines() if line.strip())
    fonts = {
        style: args.fonts_dir / f"MaterialSymbols{family}[FILL,GRAD,opsz,wght].ttf"
        for style, family in STYLES.items()
    }
    catalogs = {style: export_catalog(path, names) for style, path in fonts.items()}
    with ZipFile(ROOT / "app/src/main/assets/icons/icon_pack.zip") as classic:
        classic_names = {name.split("/")[1] for name in classic.namelist() if name.endswith(".svg")}
    sources = {
        "source": "https://github.com/google/material-design-icons",
        "commit": args.commit,
        "license": "Apache-2.0 (see LICENSE)",
        "material_icons_count": len(classic_names),
        "material_symbols_count": len(names),
        "styles": STYLES,
        "axes": AXES,
        "font_sha256": {style: hashlib.sha256(path.read_bytes()).hexdigest() for style, path in fonts.items()},
        "generator": f"fontTools {fontTools.__version__}; SVGPathPen, scaled to a 24x24 viewport",
        "note": "Original Material Icons SVG files are unchanged. Material Symbols SVGs are exported from the official variable fonts at the axes above.",
        "archive_layout": "symbols.json maps icon names to unmodified exported SVG strings; name sets are verified during the app build.",
    }
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for style, catalog in catalogs.items():
        destination = args.output_dir / f"material_symbols_{style}.zip"
        temporary = destination.with_suffix(".zip.tmp")
        try:
            with ZipFile(temporary, "w") as archive:
                for name, content in (
                    ("symbols.json", json.dumps(catalog, sort_keys=True, separators=(",", ":")).encode()),
                    ("LICENSE", args.license.read_bytes()),
                    ("SOURCES.json", (json.dumps(sources, indent=2) + "\n").encode()),
                ):
                    entry = ZipInfo(name, ZIP_TIMESTAMP)
                    entry.compress_type = ZIP_DEFLATED
                    archive.writestr(entry, content, compresslevel=9)
            temporary.replace(destination)
        finally:
            temporary.unlink(missing_ok=True)
        print(f"{destination.name}: {len(catalog)} icons, {destination.stat().st_size} bytes")


if __name__ == "__main__":
    main()
