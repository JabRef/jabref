package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.UrlBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A generic [UrlBasedFetcher] which creates a `@Misc` entry holding the provided URL.
///
/// As a best-effort enhancement, it tries to download the referenced page and extract its HTML `<title>` to use as
/// the entry's title. If the page cannot be downloaded (for instance, because of a missing network connection), the
/// resulting entry still contains the URL so that no information entered by the user is lost.
@NullMarked
public class GenericUrlBasedFetcher implements UrlBasedFetcher {

    public static final String NAME = "URL";

    /// Only the leading portion of a page is read when looking for the `<title>`, which always resides in the
    /// `<head>`. Reading a bounded number of bytes keeps memory usage in check even if the server streams a very
    /// large (or effectively unbounded) body.
    private static final int MAX_TITLE_LOOKUP_BYTES = 512 * 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericUrlBasedFetcher.class);

    private static final Pattern TITLE_TAG = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /// Seam used to obtain the (bounded) page content. Production code downloads it over the network; tests can
    /// supply fixed content without any network access.
    @FunctionalInterface
    interface PageContentDownloader {
        Optional<String> download(URL url) throws FetcherException;
    }

    private final ImportFormatPreferences importFormatPreferences;
    private final PageContentDownloader pageContentDownloader;

    public GenericUrlBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, GenericUrlBasedFetcher::downloadBoundedContent);
    }

    GenericUrlBasedFetcher(ImportFormatPreferences importFormatPreferences, PageContentDownloader pageContentDownloader) {
        this.importFormatPreferences = importFormatPreferences;
        this.pageContentDownloader = pageContentDownloader;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<BibEntry> performSearch(String url) throws FetcherException {
        if (StringUtil.isBlank(url)) {
            return List.of();
        }

        String trimmedUrl = url.trim();

        URL validatedUrl;
        try {
            validatedUrl = URLUtil.create(trimmedUrl);
        } catch (MalformedURLException e) {
            throw new FetcherException(Localization.lang("Invalid URL: '%0'.", trimmedUrl), e);
        }

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.URL, validatedUrl.toExternalForm());

        // Best-effort: enrich the entry with the page title. A failed download must not discard the URL.
        try {
            pageContentDownloader.download(validatedUrl)
                                 .flatMap(GenericUrlBasedFetcher::extractTitle)
                                 .ifPresent(title -> entry.setField(StandardField.TITLE, title));
        } catch (FetcherException e) {
            LOGGER.debug("Could not download '{}' to extract a title; creating a bare @Misc entry.", validatedUrl, e);
        }

        return List.of(entry);
    }

    /// Reads at most [#MAX_TITLE_LOOKUP_BYTES] from the page, which is sufficient to locate the `<title>` in the
    /// `<head>` while keeping memory usage bounded.
    private static Optional<String> downloadBoundedContent(URL url) throws FetcherException {
        try (InputStream stream = new URLDownload(url).asInputStream()) {
            byte[] content = stream.readNBytes(MAX_TITLE_LOOKUP_BYTES);
            return Optional.of(new String(content, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new FetcherException(url, "Could not download the page to extract its title.", e);
        }
    }

    private static Optional<String> extractTitle(String htmlContent) {
        if (StringUtil.isBlank(htmlContent)) {
            return Optional.empty();
        }

        Matcher matcher = TITLE_TAG.matcher(htmlContent);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String title = WHITESPACE.matcher(matcher.group(1)).replaceAll(" ").trim();
        return StringUtil.isBlank(title) ? Optional.empty() : Optional.of(title);
    }
}
