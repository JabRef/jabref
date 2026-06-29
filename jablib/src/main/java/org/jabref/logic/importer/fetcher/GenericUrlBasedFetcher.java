package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URL;
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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A generic {@link UrlBasedFetcher} which creates a `@Misc` entry holding the provided URL.
///
/// As a best-effort enhancement, it tries to download the referenced page and extract its HTML `<title>` to use as
/// the entry's title. If the page cannot be downloaded (for instance, because of a missing network connection), the
/// resulting entry still contains the URL so that no information entered by the user is lost.
public class GenericUrlBasedFetcher implements UrlBasedFetcher {

    public static final String NAME = "URL";

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericUrlBasedFetcher.class);

    private static final Pattern TITLE_TAG = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ImportFormatPreferences importFormatPreferences;

    public GenericUrlBasedFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<BibEntry> performSearch(@NonNull String url) throws FetcherException {
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

        // Best-effort: enrich the entry with the page title. Failures here must not discard the URL.
        try {
            String content = new URLDownload(validatedUrl).asString();
            extractTitle(content).ifPresent(title -> entry.setField(StandardField.TITLE, title));
        } catch (FetcherException | RuntimeException e) {
            LOGGER.debug("Could not download '{}' to extract a title; creating a bare @Misc entry.", validatedUrl, e);
        }

        return List.of(entry);
    }

    private static Optional<String> extractTitle(String htmlContent) {
        if (StringUtil.isBlank(htmlContent)) {
            return Optional.empty();
        }

        Matcher matcher = TITLE_TAG.matcher(htmlContent);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String title = matcher.group(1).replaceAll("\\s+", " ").trim();
        return StringUtil.isBlank(title) ? Optional.empty() : Optional.of(title);
    }
}
