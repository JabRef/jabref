package net.sf.jabref.logic.search.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.logic.layout.format.RemoveLatexCommands;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Search rule for regex-based search.
 */
public class RegexBasedSearchRule implements SearchRule {

    private static final RemoveLatexCommands REMOVE_LATEX_COMMANDS = new RemoveLatexCommands();

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
            if (bibEntry.hasField(field)) {
                String fieldContent = RegexBasedSearchRule.REMOVE_LATEX_COMMANDS.format(bibEntry.getField(field));
                String fieldContentNoBrackets = RegexBasedSearchRule.REMOVE_LATEX_COMMANDS.format(fieldContent);
                Matcher m = pattern.matcher(fieldContentNoBrackets);
                if (m.find()) {
                    return true;
                }
            }

        }
        return false;
    }

}
