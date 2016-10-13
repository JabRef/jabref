package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.importer.fetcher.ACS;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.CrossRef;
import net.sf.jabref.logic.importer.fetcher.DoiResolution;
import net.sf.jabref.logic.importer.fetcher.GoogleScholar;
import net.sf.jabref.logic.importer.fetcher.IEEE;
import net.sf.jabref.logic.importer.fetcher.ScienceDirect;
import net.sf.jabref.logic.importer.fetcher.SpringerLink;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

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
        Optional<String> doi = clonedEntry.getField(FieldName.DOI);

        if (!doi.isPresent() || !DOI.build(doi.get()).isPresent()) {
            CrossRef.findDOI(clonedEntry).ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
        }

        for (FulltextFetcher finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(clonedEntry);

                if (result.isPresent() && MimeTypeDetector.isPdfContentType(result.get().toString())) {
                    return result;
                }
            } catch (IOException | FetcherException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
        }
        return Optional.empty();
    }
}
