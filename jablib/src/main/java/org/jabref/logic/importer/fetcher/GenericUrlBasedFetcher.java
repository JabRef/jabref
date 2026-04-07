package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jabref.logic.importer.WebFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.StandardEntryType;
import org.jabref.model.entry.field.StandardField;

public class GenericUrlBasedFetcher implements WebFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericUrlBasedFetcher.class);

    @Override
    public String getName() {
        return "Generic URL Fetcher";
    }

    @Override
    public List<BibEntry> performSearch(String url) {
        if ((url == null) || url.isBlank()) {
            return Collections.emptyList();
        }

        return fetchEntryFromUrl(url.trim())
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    public Optional<BibEntry> fetchEntryFromUrl(String urlString) {
        if ((urlString == null) || urlString.isBlank()) {
            return Optional.empty();
        }

        BibEntry entry = new BibEntry(StandardEntryType.Online);
        entry.setField(StandardField.URL, urlString);
        entry.setField(StandardField.URLDATE, LocalDate.now().toString());

        try {
            Document document = Jsoup.connect(urlString).get();
            String title = document.title();

            if (!title.isBlank()) {
                entry.setField(StandardField.TITLE, title);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not fetch title from URL: {}", urlString, e);
        }

        return Optional.of(entry);
    }
}
