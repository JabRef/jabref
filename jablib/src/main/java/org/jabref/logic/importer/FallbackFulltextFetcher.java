package org.jabref.logic.importer;

import org.jspecify.annotations.NullMarked;

/// Marker interface for {@link FulltextFetcher}s that are consulted only as a fallback, after the
/// regular fetchers have found nothing.
///
/// Intended for fetchers that are expensive or user-visible — for example a browser-extension
/// companion that opens a browser tab to fetch a PDF through the user's session. Racing such a
/// fetcher alongside the cheap direct fetchers wastes work (and opens tabs) whenever a direct
/// fetcher already yields a PDF, so {@link FulltextFetchers} defers it until the direct fetchers
/// come up empty.
@NullMarked
public interface FallbackFulltextFetcher extends FulltextFetcher {
}
