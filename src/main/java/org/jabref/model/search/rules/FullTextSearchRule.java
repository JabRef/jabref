package org.jabref.model.search.rules;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jabref.gui.Globals;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.pdf.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All classes providing full text search results inherit from this class.
 * <p>
 * Some kind of caching of the full text search results is implemented.
 */
public abstract class FullTextSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullTextSearchRule.class);

    protected final EnumSet<SearchRules.SearchFlags> searchFlags;

    protected String lastQuery;
    protected List<SearchResult> lastSearchResults;

    private final BibDatabaseContext databaseContext;

    public FullTextSearchRule(EnumSet<SearchRules.SearchFlags> searchFlags) {
        this.searchFlags = searchFlags;
        this.lastQuery = "";
        lastSearchResults = Collections.emptyList();

         databaseContext = Globals.stateManager.getActiveDatabase().orElse(null);
    }

    public EnumSet<SearchRules.SearchFlags> getSearchFlags() {
        return searchFlags;
    }
}
