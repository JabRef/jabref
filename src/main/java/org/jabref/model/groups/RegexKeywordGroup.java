package org.jabref.model.groups;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;

/**
 * Matches entries if the content of a given field is matched by a regular expression.
 */
public class RegexKeywordGroup extends KeywordGroup {
    private Pattern pattern;

    public RegexKeywordGroup(String name, GroupHierarchyType context, String searchField,
                             String searchExpression, boolean caseSensitive) {
        super(name, context, searchField, searchExpression, caseSensitive);
        this.pattern = compilePattern(searchExpression, caseSensitive);
    }

    private static Pattern compilePattern(String searchExpression, boolean caseSensitive) {

        return caseSensitive ? Pattern.compile("\\b" + searchExpression + "\\b") : Pattern.compile(
                "\\b" + searchExpression + "\\b", Pattern.CASE_INSENSITIVE);

    }

    @Override
    public boolean contains(BibEntry entry) {

        Optional<String> content = entry.getField(searchField);
        return content.map(value -> pattern.matcher(value).find()).orElse(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegexKeywordGroup)) {
            return false;
        }
        RegexKeywordGroup other = (RegexKeywordGroup) o;
        return getName().equals(other.getName())
                && (getHierarchicalContext() == other.getHierarchicalContext())
                && searchField.equals(other.searchField)
                && searchExpression.equals(other.searchExpression)
                && (caseSensitive == other.caseSensitive);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new RegexKeywordGroup(getName(), getHierarchicalContext(), searchField, searchExpression,
                caseSensitive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(),
                getHierarchicalContext(),
                searchField,
                searchExpression,
                caseSensitive);
    }
}
