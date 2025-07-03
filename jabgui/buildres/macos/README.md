# Mac Resources

## Modifying DMG Setup scpt

Rename `JabRef-dmg-setup.scpt script` to  `JabRef-dmg-setup.applescript`.
Only modify the `JabRef-dmg-setup.applescript` in the macOS Script Editor. Afterwards copy over the file and rename it to `JabRef-dmg-setup.scpt`.
Normally the `scpt` file is a binary compiled variant and the `.applescript` the uncompiled format but jpackage expects the sctp in uncompiled format

## Generate iconsets

To generate icns files use the script under `src/main/resources/icons`
Install [svg2png](https://formulae.brew.sh/formula/svg2png) and call the script with the svg filename as first argument.
