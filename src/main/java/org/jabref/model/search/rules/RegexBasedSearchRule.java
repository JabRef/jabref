package org.jabref.model.search.rules;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.model.entry.BibEntry;

/**
 * Search rule for regex-based search.
 */
public class RegexBasedSearchRule implements SearchRule {

    private final boolean caseSensitive;

    public RegexBasedSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
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
    public boolean applyRule(String query, BibEntry bibEntry) {
        Pattern pattern;

        try {
            pattern = Pattern.compile(query, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }

        for (String field : bibEntry.getFieldNames()) {
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

}
