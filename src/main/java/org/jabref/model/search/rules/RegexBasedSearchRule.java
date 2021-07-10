package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.gui.Globals;
import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search rule for regex-based search.
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public class RegexBasedSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarBasedSearchRule.class);

    private final EnumSet<SearchFlags> searchFlags;

    private String lastQuery;
    private List<SearchResult> lastSearchResults;

    private final BibDatabaseContext databaseContext;

    public RegexBasedSearchRule(EnumSet<SearchFlags> searchFlags) {
        this.searchFlags = searchFlags;

        databaseContext = Globals.stateManager.getActiveDatabase().orElse(null);
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        String searchString = query;
        if (!searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            searchString = searchString.toLowerCase(Locale.ROOT);
        }

        try {
            Pattern.compile(searchString, searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        Pattern pattern;

        try {
            pattern = Pattern.compile(query, searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE);
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
        return getFulltextResults(query, bibEntry).numSearchResults() > 0;
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {

        if (!searchFlags.contains(SearchRules.SearchFlags.FULLTEXT) || databaseContext == null) {
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
