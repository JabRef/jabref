package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FulltextFetchers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextFetchers.class);

    private final List<FulltextFetcher> finders = new ArrayList<>();

    public FulltextFetchers(ImportFormatPreferences importFormatPreferences) {
        this(WebFetchers.getFullTextFetchers(importFormatPreferences));
    }

    FulltextFetchers(List<FulltextFetcher> fetcher) {
        finders.addAll(fetcher);
    }

    public Optional<URL> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = (BibEntry) entry.clone();
        Optional<DOI> doi = clonedEntry.getField(FieldName.DOI).flatMap(DOI::parse);

        if (!doi.isPresent()) {
            try {
                WebFetchers.getIdFetcherForIdentifier(DOI.class)
                        .findIdentifier(clonedEntry)
                        .ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
            } catch (FetcherException e) {
                LOGGER.debug("Failed to find DOI", e);
            }
        }

        for (FulltextFetcher finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(clonedEntry);

                if (result.isPresent() && new URLDownload(result.get().toString()).isPdf()) {
                    return result;
                }
            } catch (IOException | FetcherException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
        }
        return Optional.empty();
    }
}
