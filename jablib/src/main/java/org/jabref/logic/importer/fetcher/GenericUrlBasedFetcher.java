package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.UrlBasedFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jsoup.Jsoup;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Wraps an arbitrary URL into a minimal `@Misc` entry.
///
/// This is the fallback for any URL that no more specific [UrlBasedFetcher] recognizes — it does not try to
/// interpret the URL's shape (e.g. a DOI or Semantic Scholar link), it only records the link itself, when it was
/// added, and (best-effort) the target page's title.
@NullMarked
public class GenericUrlBasedFetcher implements UrlBasedFetcher {

    public static final String NAME = "URL";

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericUrlBasedFetcher.class);
    private static final int CONNECT_TIMEOUT_MILLIS = 30_000;

    @Override
    public List<BibEntry> performSearch(String url) throws FetcherException {
        String trimmedUrl = url.trim();
        if (!URLUtil.isURL(trimmedUrl)) {
            throw new FetcherException("Invalid URL: " + url);
        }

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.URL, trimmedUrl)
                .withField(StandardField.TITLE, fetchTitle(trimmedUrl).orElse(trimmedUrl))
                .withField(StandardField.URLDATE, new Date(LocalDate.now()).getNormalized());

        return List.of(entry);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /// Best-effort fetch of the target page's `<title>`. A failure here (unreachable host, timeout, no
    /// title tag, ...) should never prevent the entry from being created, so this swallows the error and lets the
    /// caller fall back to using the URL itself as the title.
    ///
    /// Only attempted for `http`/`https` URLs — [Jsoup.connect] only supports those schemes and throws
    /// `IllegalArgumentException` for anything else (e.g. `ftp://`, which [URLUtil.isURL] otherwise accepts).
    private Optional<String> fetchTitle(String url) {
        String scheme = url.toLowerCase(Locale.ROOT);
        if (!scheme.startsWith("http://") && !scheme.startsWith("https://")) {
            return Optional.empty();
        }

        try {
            String title = Jsoup.connect(url)
                                .userAgent(URLDownload.USER_AGENT)
                                .timeout(CONNECT_TIMEOUT_MILLIS)
                                .get()
                                .title();
            return title.isBlank() ? Optional.empty() : Optional.of(title);
        } catch (IOException e) {
            LOGGER.debug("Could not fetch title for '{}', falling back to the URL as title.", url, e);
            return Optional.empty();
        }
    }
}
