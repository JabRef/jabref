# Mac Resources

## Modifying DMG Setup scpt

Rename `JabRef-dmg-setup.scpt` script to  `JabRef-dmg-setup.applescript`.
Only modify the `JabRef-dmg-setup.applescript` in the macOS Script Editor.
Afterwards, copy over the file and rename it to `JabRef-dmg-setup.scpt`.
Normally the `scpt` file is a binary compiled variant and the `.applescript` the uncompiled format but jpackage expects the sctp in uncompiled format

## Generate iconsets

To generate icns files use the script under `src/main/resources/icons`
Install [svg2png](https://formulae.brew.sh/formula/svg2png) and call the script with the svg filename as first argument.

To regenerate the macOS application icon's light/dark `.icns`, install `librsvg` and run
`python3 jabgui/src/main/resources/icons/generate-dynamic-icns.py` from the repository root.
The script derives `JabRef-dark.svg` from the existing JabRef logo and embeds its rendered iconset
as the dark variant in `JabRef.icns` and `launcher.icns`. The DMG volume icon remains unchanged.
