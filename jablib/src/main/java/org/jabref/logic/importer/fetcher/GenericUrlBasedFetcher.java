package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GenericUrlBasedFetcher implements WebFetcher {

    @Override
    public String getName() {
        return "Generic URL Fetcher";
    }

    public List<BibEntry> fetchEntryFromUrl(String url) throws FetcherException {
        if (StringUtil.isBlank(url)) {
            return List.of();
        }

        String normalizedUrl = normalizeUrl(url);

        if (!URLUtil.isValidHttpUrl(normalizedUrl)) {
            throw new FetcherException("Invalid URL: " + normalizedUrl);
        }

        BibEntry entry = new BibEntry(StandardEntryType.Online)
                .withField(StandardField.URL, normalizedUrl)
                .withField(
                        StandardField.URLDATE,
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                );

        String title = scrapeTitle(normalizedUrl);
        if (StringUtil.isNotBlank(title)) {
            entry.setField(StandardField.TITLE, title);
        }

        return List.of(entry);
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private String scrapeTitle(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            return document.title();
        } catch (IOException e) {
            return null;
        }
    }
}
