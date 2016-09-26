package net.sf.jabref.model.search.rules;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.strings.LatexToUnicode;

/**
 * Search rule for regex-based search.
 */
public class RegexBasedSearchRule implements SearchRule {

    private static final LatexToUnicode LATEX_TO_UNICODE_FORMATTER = new LatexToUnicode();

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
            searchString = searchString.toLowerCase();
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
                String fieldContent = RegexBasedSearchRule.LATEX_TO_UNICODE_FORMATTER.format(fieldOptional.get());
                String fieldContentNoBrackets = RegexBasedSearchRule.LATEX_TO_UNICODE_FORMATTER.format(fieldContent);
                Matcher m = pattern.matcher(fieldContentNoBrackets);
                if (m.find()) {
                    return true;
                }
            }

        }
        return false;
    }

}
