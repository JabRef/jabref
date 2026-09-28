---
parent: Requirements
---
# Distribution

## Open `.bib` files with JabRef after installing a package
`req~distribution.bib-file-association~1`

The deb, rpm, and msi packages register JabRef as an application for `text/x-bibtex`, so that opening a `.bib` file from the file manager starts JabRef.

## Match the macOS application icon to icon appearance
`req~distribution.macos-dark-app-icon~1`

The macOS application icon provides light and dark artwork with the same JabRef logo, allowing the system to use the dark version when dark icons are selected.

## Push citations to external applications from the Flatpak
`req~distribution.flatpak-host-editor~1`

Users should be able to push citations from the sandboxed Flatpak to supported external applications installed on the host. The Flatpak manifest must grant session-bus access to `org.freedesktop.Flatpak` to launch those applications on the host.

<!-- markdownlint-disable-file MD022 -->
