package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;

/**
 * Search rule for regex-based search.
 */
public class RegexBasedSearchRule implements SearchRule {

    private final boolean caseSensitive;
    private final boolean fulltext;

    private String lastQuery;
    private List<SearchResult> lastSearchResults;

    public RegexBasedSearchRule(boolean caseSensitive, boolean fulltext) {
        this.caseSensitive = caseSensitive;
        this.fulltext = fulltext;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isFulltext() {
        return fulltext;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase(Locale.ROOT);
        }

        try {
            Pattern.compile(searchString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean applyRule(String query, BibDatabaseContext databaseContext, BibEntry bibEntry) {
        Pattern pattern;

        try {
            pattern = Pattern.compile(query, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }

        for (Field field : bibEntry.getFields()) {
            Optional<String> fieldOptional = bibEntry.getField(field);
            if (fieldOptional.isPresent()) {
                String fieldContentNoBrackets = bibEntry.getLatexFreeField(field).get();
                Matcher m = pattern.matcher(fieldContentNoBrackets);
                if (m.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibDatabaseContext databaseContext, BibEntry bibEntry) {

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
