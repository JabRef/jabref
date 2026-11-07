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
The script derives `JabRef-dark.svg` and the Icon Composer logo layer from the existing JabRef logo,
and embeds its rendered iconset as the dark variant in `JabRef.icns` and `launcher.icns`.

macOS Tahoe uses `Resources/Assets.car` and the `CFBundleIconName` plist key for the app icon's
appearance variants. Regenerate the checked-in asset catalog from the repository root with Xcode 26:

```bash
mkdir -p jabgui/build/icon-composer
xcrun actool jabgui/buildres/macos/JabRef.icon \
  --compile jabgui/build/icon-composer \
  --app-icon JabRef --target-device mac --minimum-deployment-target 10.11 --platform macosx \
  --output-partial-info-plist jabgui/build/icon-composer/partial.plist
cp jabgui/build/icon-composer/Assets.car jabgui/buildres/macos/Resources/Assets.car
```

The `.icns` remains the fallback for older macOS versions.
The DMG volume icon remains unchanged.

## Clearing macOS icon cache

When testing local packages or changing bundle icons, macOS LaunchServices may serve previously cached icons. To reset:

```bash
# Clear quarantine and re-register bundle
xattr -cr /path/to/JabRef.app
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f /path/to/JabRef.app
killall Dock
```
