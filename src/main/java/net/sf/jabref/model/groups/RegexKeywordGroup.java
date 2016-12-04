package net.sf.jabref.model.groups;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Matches entries if the content of a given field is matched by a regular expression.
 */
public class RegexKeywordGroup extends KeywordGroup {
    private Pattern pattern;

    private static Pattern compilePattern(String searchExpression, boolean caseSensitive) throws IllegalArgumentException {
        try {
            return caseSensitive ? Pattern.compile("\\b" + searchExpression + "\\b") : Pattern.compile(
                    "\\b" + searchExpression + "\\b", Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Syntax error in regular-expression pattern: " + searchExpression);
        }
    }

    @Override
    public boolean contains(BibEntry entry) {

            Optional<String> content = entry.getField(searchField);
            return content.map(value -> pattern.matcher(value).find()).orElse(false);
        }
}
