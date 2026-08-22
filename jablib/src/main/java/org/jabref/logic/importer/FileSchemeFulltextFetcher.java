package org.jabref.logic.importer;

/// Marker interface for {@link FulltextFetcher}s that are trusted to return local `file:` URLs.
///
/// A `file:` result triggers a local file read and the GUI's move/attach pipeline, so it must only
/// be honored for fetchers that produce the file themselves (e.g. a browser-extension companion that
/// already wrote the PDF to disk). Fetchers that build URLs from remote content must not implement
/// this interface, otherwise a malicious page could point JabRef at an arbitrary local file.
///
/// {@link FulltextFetchers} rejects any `file:` URL returned by a fetcher that does not implement
/// this interface.
public interface FileSchemeFulltextFetcher extends FulltextFetcher {
}
