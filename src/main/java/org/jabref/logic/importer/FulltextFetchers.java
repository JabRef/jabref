package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.ACS;
import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.DoiResolution;
import org.jabref.logic.importer.fetcher.GoogleScholar;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.ScienceDirect;
import org.jabref.logic.importer.fetcher.SpringerLink;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FulltextFetchers {
    private static final Log LOGGER = LogFactory.getLog(FulltextFetchers.class);

    private final List<FulltextFetcher> finders = new ArrayList<>();

    public FulltextFetchers(ImportFormatPreferences importFormatPreferences) {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new DoiResolution());
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        finders.add(new ArXiv(importFormatPreferences));
        finders.add(new IEEE());
        // Meta search
        finders.add(new GoogleScholar(importFormatPreferences));
    }

    public FulltextFetchers(List<FulltextFetcher> fetcher) {
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
