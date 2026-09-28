---
parent: Requirements
---
# Distribution

## Open `.bib` files with JabRef after installing a package
`req~distribution.bib-file-association~1`

The deb, rpm, and msi packages register JabRef as an application for `text/x-bibtex`, so that opening a `.bib` file from the file manager starts JabRef.

## Push citations to external applications from the Flatpak
`req~distribution.flatpak-host-editor~1`

Users should be able to push citations from the sandboxed Flatpak to supported external applications installed on the host. The Flatpak manifest must grant session-bus access to `org.freedesktop.Flatpak` to launch those applications on the host.

<!-- markdownlint-disable-file MD022 -->
