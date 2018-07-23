package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.GroupSearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This group matches entries by a complex search pattern, which might include conditions about the values of
 * multiple fields.
 */
public class SearchGroup extends AbstractGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchGroup.class);
    private final GroupSearchQuery query;

    public SearchGroup(String name, GroupHierarchyType context, String searchExpression, boolean caseSensitive,
                       boolean isRegEx) {
        super(name, context);
        this.query = new GroupSearchQuery(searchExpression, caseSensitive, isRegEx);
    }

    public String getSearchExpression() {
        return query.getSearchExpression();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchGroup)) {
            return false;
        }
        SearchGroup other = (SearchGroup) o;
        return getName().equals(other.getName())
                && getSearchExpression().equals(other.getSearchExpression())
                && (isCaseSensitive() == other.isCaseSensitive())
                && (isRegularExpression() == other.isRegularExpression())
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return query.isMatch(entry);
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(getName(), getHierarchicalContext(), getSearchExpression(), isCaseSensitive(),
                    isRegularExpression());
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            LOGGER.error("Internal error in SearchGroup.deepCopy(). "
                    + "Please report this on https://github.com/JabRef/jabref/issues", t);
            return null;
        }
    }

    public boolean isCaseSensitive() {
        return query.isCaseSensitive();
    }

    public boolean isRegularExpression() {
        return query.isRegularExpression();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext(), getSearchExpression(), isCaseSensitive(), isRegularExpression());
    }
}
