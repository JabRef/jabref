package org.jabref.logic.importer.fetcher;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;

/// Discovered browser-extension fulltext provider as published in JabRef's
/// well-known discovery directory.
///
/// See `docs/requirements/browser-extension-fulltext.md`.
///
/// [impl->req~bxf.discovery-schema~1]
@NullMarked
public record BrowserExtensionProvider(
        String name,
        String displayName,
        int port,
        Path tokenFile,
        int protocolVersion) {
}
