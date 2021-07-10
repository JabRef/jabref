package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search rule for contain-based search.
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public class ContainBasedSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final boolean caseSensitive;
    private final boolean fulltext;

    private String lastQuery;
    private List<SearchResult> lastSearchResults;

    private final BibDatabaseContext databaseContext;

    public ContainBasedSearchRule(boolean caseSensitive, boolean fulltext) {
        this.caseSensitive = caseSensitive;
        this.fulltext = fulltext;
        this.lastQuery = "";
        lastSearchResults = new Vector<>();

        databaseContext = Globals.stateManager.getActiveDatabase().orElse(null);
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isFulltext() {
        return fulltext;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {

        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase(Locale.ROOT);
        }

        List<String> unmatchedWords = new SentenceAnalyzer(searchString).getWords();

        for (Field fieldKey : bibEntry.getFields()) {
            String formattedFieldContent = bibEntry.getLatexFreeField(fieldKey).get();
            if (!caseSensitive) {
                formattedFieldContent = formattedFieldContent.toLowerCase(Locale.ROOT);
            }

            Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
            while (unmatchedWordsIterator.hasNext()) {
                String word = unmatchedWordsIterator.next();
                if (formattedFieldContent.contains(word)) {
                    unmatchedWordsIterator.remove();
                }
            }

            if (unmatchedWords.isEmpty()) {
                return true;
            }
        }

        return getFulltextResults(query, bibEntry).numSearchResults() > 0; // Didn't match all words.
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {

        if (!fulltext || databaseContext == null) {
            return new PdfSearchResults(List.of());
        }

        if (!query.equals(this.lastQuery)) {
            this.lastQuery = query;
            lastSearchResults = List.of();
            try {
                PdfSearcher searcher = PdfSearcher.of(databaseContext);
                PdfSearchResults results = searcher.search(query, 5);
                lastSearchResults = results.getSortedByScore();
            } catch (IOException e) {
                LOGGER.error("Could not retrieve search results!", e);
            }
        }

        return new PdfSearchResults(lastSearchResults.stream().filter(searchResult -> searchResult.isResultFor(bibEntry)).collect(Collectors.toList()));
    }
}
