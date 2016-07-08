package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.importer.fetcher.CrossRef;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {
    private static final Log LOGGER = LogFactory.getLog(FindFullText.class);

    private final List<FullTextFinder> finders = new ArrayList<>();

    public FindFullText() {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new DoiResolution());
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        finders.add(new ArXiv());
        finders.add(new IEEE());
        // Meta search
        finders.add(new GoogleScholar());
    }

    public FindFullText(List<FullTextFinder> fetcher) {
        finders.addAll(fetcher);
    }

    public Optional<URL> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = (BibEntry) entry.clone();
        String doi = clonedEntry.getField("doi");

        if (doi == null || !DOI.build(doi).isPresent()) {
            CrossRef.findDOI(clonedEntry).ifPresent(e -> clonedEntry.setField("doi", e.getDOI()));
        }

        for (FullTextFinder finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(clonedEntry);

                if (result.isPresent() && MimeTypeDetector.isPdfContentType(result.get().toString())) {
                    return result;
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
        }
        return Optional.empty();
    }
}
