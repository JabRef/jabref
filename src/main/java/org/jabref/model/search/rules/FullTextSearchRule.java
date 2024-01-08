package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.gui.Globals;
import org.jabref.logic.pdf.search.PdfIndexer;
import org.jabref.logic.pdf.search.PdfIndexerManager;
import org.jabref.logic.pdf.search.PdfSearcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All classes providing full text search results inherit from this class.
 * <p>
 * Some kind of caching of the full text search results is implemented.
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public abstract class FullTextSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullTextSearchRule.class);

    protected final EnumSet<SearchRules.SearchFlags> searchFlags;

    protected String lastQuery;
    protected List<SearchResult> lastSearchResults;

    public FullTextSearchRule(EnumSet<SearchRules.SearchFlags> searchFlags) {
        this.searchFlags = searchFlags;
        this.lastQuery = "";
        lastSearchResults = Collections.emptyList();
    }

    public EnumSet<SearchRules.SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {
        if (!searchFlags.contains(SearchRules.SearchFlags.FULLTEXT)) {
            LOGGER.debug("Fulltext search results called even though fulltext search flag is missing.");
            return new PdfSearchResults();
        }

        if (query.equals(this.lastQuery)) {
            LOGGER.debug("Reusing fulltext search results.");
            return new PdfSearchResults(lastSearchResults.stream()
                                                         .filter(searchResult -> searchResult.isResultFor(bibEntry))
                                                         .toList());
        }

        PdfIndexer pdfIndexer;
        try {
            pdfIndexer = PdfIndexerManager.getIndexer(Globals.stateManager.getActiveDatabase().get(), Globals.prefs.getFilePreferences());
        } catch (IOException e) {
            LOGGER.error("Could not access full text index.", e);
            return new PdfSearchResults();
        }
        this.lastQuery = query;
        lastSearchResults = Collections.emptyList();
        try {
            PdfSearcher searcher = PdfSearcher.of(pdfIndexer);
            PdfSearchResults results = searcher.search(query, 5);
            lastSearchResults = results.getSortedByScore();
            return results;
        } catch (IOException e) {
            LOGGER.error("Could not retrieve search results.", e);
            return new PdfSearchResults();
        }
    }
}
