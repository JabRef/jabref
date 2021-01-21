package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.field.Field;

/**
 * Matches entries based on a search phrase relative to the content in a specified field.
 */
public abstract class KeywordGroup extends AbstractGroup {
    protected final Field searchField;
    protected final String searchExpression;
    protected final boolean caseSensitive;

    public KeywordGroup(String name, GroupHierarchyType context, Field searchField, String searchExpression, boolean caseSensitive) {
        super(name, context);
        this.caseSensitive = caseSensitive;
        this.searchField = searchField;
        this.searchExpression = searchExpression;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public Field getSearchField() {
        return searchField;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        KeywordGroup that = (KeywordGroup) o;
        return isCaseSensitive() == that.isCaseSensitive() && Objects.equals(getSearchField(), that.getSearchField()) && Objects.equals(getSearchExpression(), that.getSearchExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getSearchField(), getSearchExpression(), isCaseSensitive());
    }
}
