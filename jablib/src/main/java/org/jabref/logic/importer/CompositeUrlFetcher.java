package org.jabref.logic.importer;

import java.util.List;

import org.jabref.logic.importer.fetcher.GenericUrlBasedFetcher;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Resolves a user-supplied URL to bibliographic entries by delegating to specialized fetchers.
///
/// If the URL contains a known identifier (DOI, arXiv, …), the fetcher extracts it
/// and delegates to the matching [IdBasedFetcher] via [CompositeIdFetcher].
/// Otherwise it delegates to [GenericUrlBasedFetcher], which wraps the URL into a `@Misc` entry.
@NullMarked
public class CompositeUrlFetcher implements UrlBasedFetcher {

    public static final String NAME = "CompositeUrlFetcher";

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeUrlFetcher.class);

    private final GenericUrlBasedFetcher genericUrlBasedFetcher;
    private final CompositeIdFetcher compositeIdFetcher;

    public CompositeUrlFetcher(ImportFormatPreferences importFormatPreferences) {
        this.genericUrlBasedFetcher = new GenericUrlBasedFetcher();
        this.compositeIdFetcher = new CompositeIdFetcher(importFormatPreferences);
    }

    @Override
    public List<BibEntry> performSearch(String url) throws FetcherException {
        String trimmedUrl = url.trim();
        if (!URLUtil.isURL(trimmedUrl)) {
            throw new FetcherException("Invalid URL: " + FetcherException.getRedactedUrl(url));
        }

        List<BibEntry> entriesFromIdentifier = fetchByIdentifier(trimmedUrl);
        if (!entriesFromIdentifier.isEmpty()) {
            return entriesFromIdentifier;
        }

        return genericUrlBasedFetcher.performSearch(trimmedUrl);
    }

    /// Attempts to fetch bibliographic metadata using an identifier embedded in the URL.
    /// If the identifier lookup fails (or yields no entry), returns an empty list so that
    /// the caller can fall back to creating a Misc entry with the URL and title (no error dialog is shown).
    private List<BibEntry> fetchByIdentifier(String url) {
        if (!CompositeIdFetcher.containsValidId(url)) {
            return List.of();
        }

        try {
            return compositeIdFetcher.performSearchById(url)
                                     .map(List::of)
                                     .orElseGet(List::of);
        } catch (FetcherException e) {
            LOGGER.debug("Could not fetch entry by identifier from URL '{}', falling back to GenericUrlBasedFetcher", FetcherException.getRedactedUrl(url), e);
            return List.of();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
