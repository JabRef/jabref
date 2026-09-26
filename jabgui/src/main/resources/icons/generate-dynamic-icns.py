#!/usr/bin/env python3

"""Add a dark appearance to the existing macOS application icon."""

import shutil
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path


SVG = "{http://www.w3.org/2000/svg}"
ET.register_namespace("", "http://www.w3.org/2000/svg")
DARK_ICON_TYPE = bytes.fromhex("fdd92fa8")
ICON_SIZES = {
    "16x16": 16,
    "16x16@2x": 32,
    "32x32": 32,
    "32x32@2x": 64,
    "48x48": 48,
    "128x128": 128,
    "128x128@2x": 256,
    "256x256": 256,
    "256x256@2x": 512,
    "512x512": 512,
    "512x512@2x": 1024,
}


def icon_data(icon):
    if icon[:4] != b"icns" or struct.unpack(">I", icon[4:8])[0] != len(icon):
        raise ValueError("Invalid icns header")

    offset = 8
    while offset < len(icon):
        length = struct.unpack(">I", icon[offset + 4:offset + 8])[0]
        if length < 8 or offset + length > len(icon):
            raise ValueError("Invalid icns element length")
        yield icon[offset:offset + length]
        offset += length


def add_logo(root, source, color):
    logo = ET.parse(source).getroot()
    original_group = logo.find(f".//{SVG}g")
    artwork = ET.SubElement(root, f"{SVG}g", {"transform": "translate(227 223) scale(2.7804878)"})
    pages = ET.SubElement(artwork, f"{SVG}g", {"transform": original_group.attrib["transform"]})
    for original in original_group.findall(f"{SVG}path"):
        ET.SubElement(pages, f"{SVG}path", {
            "d": original.attrib["d"],
            "transform": original.attrib["transform"],
            "fill": color,
        })


def dark_artwork(source, destination):
    root = ET.Element(f"{SVG}svg", {"width": "1024", "height": "1024", "viewBox": "0 0 1024 1024"})
    ET.SubElement(root, f"{SVG}rect", {
        "x": "100", "y": "100", "width": "824", "height": "824", "rx": "195", "fill": "#202b4b",
    })
    add_logo(root, source, "#c4d0f0")
    ET.indent(root, space="  ")
    ET.ElementTree(root).write(destination, encoding="utf-8", xml_declaration=True)


def logo_artwork(source, destination):
    root = ET.Element(f"{SVG}svg", {"width": "1024", "height": "1024", "viewBox": "0 0 1024 1024"})
    add_logo(root, source, "#ffffff")
    ET.indent(root, space="  ")
    destination.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(destination, encoding="utf-8", xml_declaration=True)


def main():
    icons = Path(__file__).resolve().parent
    macos = icons.parents[3] / "buildres" / "macos"
    light_icon = macos / "JabRef.icns"
    artwork = macos / "JabRef-dark.svg"
    dark_artwork(icons / "jabref.svg", artwork)
    logo_artwork(icons / "jabref.svg", macos / "JabRef.icon" / "Assets" / "JabRef-logo.svg")

    with tempfile.TemporaryDirectory(prefix="jabref-dark-icon-") as work:
        work = Path(work)
        iconset = work / "JabRef-dark.iconset"
        iconset.mkdir()
        for label, size in ICON_SIZES.items():
            subprocess.run([
                "rsvg-convert", "-w", str(size), "-h", str(size),
                "-o", str(iconset / f"icon_{label}.png"), str(artwork),
            ], check=True)

        dark_icon = work / "JabRef-dark.icns"
        subprocess.run(["iconutil", "-c", "icns", str(iconset), "-o", str(dark_icon)], check=True)
        dark_elements = b"".join(icon_data(dark_icon.read_bytes()))

    light_elements = b"".join(
        element for element in icon_data(light_icon.read_bytes())
        if element[:4] != DARK_ICON_TYPE
    )
    nested_dark = DARK_ICON_TYPE + struct.pack(">I", 8 + len(dark_elements)) + dark_elements
    elements = light_elements + nested_dark
    light_icon.write_bytes(b"icns" + struct.pack(">I", 8 + len(elements)) + elements)
    shutil.copyfile(light_icon, macos / "launcher.icns")


if __name__ == "__main__":
    main()
