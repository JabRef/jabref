# Mac Resources

## DMG Setup scpt

Only modify the `JabRef-dmg-setup.applescript` in the OS X Script Editor. Afterwards copy over the file and rename it to `JabRef-dmg-setup.scpt`.
Normally the `scpt` file is a binary compiled variant and the `.applescript` but jpackage does not like it in binary format.

## Generate iconsets

To generate icns files use the script under `src/main/resources/icons`
Install [svg2png](https://formulae.brew.sh/formula/svg2png) and call the script with the svg filename as first argument.
