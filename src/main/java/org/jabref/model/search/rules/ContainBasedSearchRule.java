package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;

/**
 * Search rule for contain-based search.
 */
public class ContainBasedSearchRule implements SearchRule {

    private final boolean caseSensitive;
    private final boolean fulltext;

    private String lastQuery;
    private List<SearchResult> lastSearchResults;
    private BibDatabaseContext databaseContext;

    public ContainBasedSearchRule(boolean caseSensitive, boolean fulltext, BibDatabaseContext databaseContext) {
        this.caseSensitive = caseSensitive;
        this.fulltext = fulltext;
        this.lastQuery = "";
        lastSearchResults = new Vector<>();
        this.databaseContext = databaseContext;
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

        return false; // Didn't match all words.
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {

        if (!fulltext) {
            return new PdfSearchResults(List.of());
        }

        if (!query.equals(this.lastQuery)) {
            this.lastQuery = query;
            lastSearchResults = List.of();
            try {
                PdfSearcher searcher = PdfSearcher.of(databaseContext);
                PdfSearchResults results = searcher.search(query, 100);
                lastSearchResults = results.getSortedByScore();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PdfSearchResults(lastSearchResults.stream().filter(searchResult -> searchResult.isResultFor(bibEntry)).collect(Collectors.toList()));
    }
}
