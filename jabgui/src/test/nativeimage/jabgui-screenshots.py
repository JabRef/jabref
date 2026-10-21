#!/usr/bin/env python3

"""Take screenshots of a running JabRef with OculiX (see JabRef/jabref#6089).

JabRef must already show a library with at least one entry. The script drives
it like a user (keyboard shortcuts, OCR for menus without a shortcut) and saves
one screenshot per view. A view that cannot be opened is reported and skipped,
so one broken dialog does not hide the others.
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path
from typing import Callable

from oculix import OCR, Key, Region, Screen

# Time for a dialog or view to appear and finish rendering
SETTLE_SECONDS = 3


def press(screen: Screen, modifier: object, key: object) -> None:
    screen.keyDown(modifier)
    screen.type(key)
    screen.keyUp()


def open_entry_editor(screen: Screen) -> None:
    press(screen, Key.ALT, "1")
    screen.type(Key.DOWN)
    press(screen, Key.CTRL, "e")


def close_entry_editor(screen: Screen) -> None:
    press(screen, Key.CTRL, "e")


def open_preferences(screen: Screen) -> None:
    press(screen, Key.CTRL, ",")


def open_cleanup(screen: Screen) -> None:
    press(screen, Key.ALT, Key.F8)


def click_text(screen: Screen, area: Region, text: str, timeout: float = 10.0) -> object:
    # existsText is SikuliX's OCR search; the Python wrapper does not list it, so call it directly.
    # Searching a small area: on the whole screen OCR misses small menu labels it reads fine in a strip.
    # existsText waits up to timeout and returns None instead of throwing, so we can report what OCR saw.
    match = area._call("existsText", text, timeout)
    if match is None:
        seen = OCR.readText(area._remote)
        raise RuntimeError(f"text {text!r} not found, OCR read in the search area: {seen!r}")
    screen.click(match)
    return match


def open_about(screen: Screen) -> None:
    # No shortcut and no mnemonic for About: open the Help menu by its text (OCR).
    # About JabRef is the last entry; Up selects the last entry of an opened menu.
    # OCR reads "About JabRef" in the dropdown on CI, but existsText does not match the two words there.
    menu_bar = Region(0, 0, screen.getW(), 40)
    click_text(screen, menu_bar, "Help")
    time.sleep(1)
    screen.type(Key.UP)
    screen.type(Key.ENTER)


def close_dialog(screen: Screen) -> None:
    screen.type(Key.ESC)


def take(screen: Screen, output: Path, name: str) -> None:
    time.sleep(SETTLE_SECONDS)
    screen.capture().save(str(output), name)
    print(f"saved {name}.png")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="Directory for the PNG files")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    screen = Screen()
    take(screen, args.output, "01-main-window")

    views: list[tuple[str, Callable[[Screen], None], Callable[[Screen], None]]] = [
        ("02-entry-editor", open_entry_editor, close_entry_editor),
        ("03-preferences", open_preferences, close_dialog),
        ("04-cleanup", open_cleanup, close_dialog),
        ("05-about", open_about, close_dialog),
    ]
    failed = []
    for name, open_view, close_view in views:
        try:
            open_view(screen)
            take(screen, args.output, name)
            close_view(screen)
            time.sleep(1)
        except Exception as error:  # noqa: BLE001 (OculiX reports Java exceptions as plain exceptions)
            print(f"could not take {name}: {error}", file=sys.stderr)
            screen.capture().save(str(args.output), f"{name}-failed")
            screen.type(Key.ESC)
            failed.append(name)

    if failed:
        print(f"missing screenshots: {', '.join(failed)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
