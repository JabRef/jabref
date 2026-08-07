package org.jabref.logic.search.inmemory;

import java.util.List;

import org.jabref.logic.search.LibrarySearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// In-memory implementation of [LibrarySearcher] that evaluates the Search.g4 grammar
/// directly against each [BibEntry] of the library. No external infrastructure required
/// (no Postgres, no Lucene).
///
/// **Limitation:** linked-file content search ([SearchFlags#FULLTEXT]) is not supported.
/// If a query carries the FULLTEXT flag, this implementation logs a warning and falls
/// back to metadata-only matching.
public class InMemoryLibrarySearcher implements LibrarySearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryLibrarySearcher.class);

    private final BibDatabaseContext databaseContext;
    private final Character keywordSeparator;

    public InMemoryLibrarySearcher(BibDatabaseContext databaseContext, BibEntryPreferences bibEntryPreferences) {
        this.databaseContext = databaseContext;
        this.keywordSeparator = bibEntryPreferences.getKeywordSeparator();
    }

    @Override
    public List<BibEntry> getMatches(SearchQuery query) {
        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression '{}'", query.getSearchExpression());
            return List.of();
        }
        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT)) {
            LOGGER.warn("In-memory searcher does not support FULLTEXT search; matching against metadata only");
        }
        return databaseContext.getDatabase().getEntries().stream()
                              .filter(entry -> matchesParsedQuery(entry, query))
                              .toList();
    }

    /// Test a single entry against a query without iterating the library.
    /// Returns `false` for invalid queries.
    public boolean matches(BibEntry entry, SearchQuery query) {
        if (!query.isValid()) {
            return false;
        }
        return matchesParsedQuery(entry, query);
    }

    private boolean matchesParsedQuery(BibEntry entry, SearchQuery query) {
        return Boolean.TRUE.equals(
                new BibEntryMatchVisitor(entry, query.getSearchFlags(), keywordSeparator).visit(query.getContext()));
    }
}
